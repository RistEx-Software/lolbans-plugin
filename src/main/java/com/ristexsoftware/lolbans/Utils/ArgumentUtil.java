/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *   
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *   
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *   
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 */

package com.ristexsoftware.lolbans.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The purpose of this class is to parse the arguments of a command.
 * When the user enters a command, say:
 *  /ban -s Justasic * testing!
 * then we must parse all the elements of these commands and ensure
 * valid syntax is present. 
 * 
 * So the way this works is we gather our command line arguments in
 * an array of strings. We then expect to have all our arguments
 * in a usable map. This accounts for things like positional arguments
 * and other such nonsense things.
 */
public class ArgumentUtil 
{
	private Map<String, String> ParsedArgs = new HashMap<String, String>();
	private ArrayList<String> UnparsedArgs;
	private boolean IsValidArgs = true;

	/**
	 * Create an ArgumentUtil object and parse the existing arguments.
	 * @param args The argumenst to parse.
	 */
	public ArgumentUtil(String args[])
	{
		UnparsedArgs = new ArrayList<String>(Arrays.asList(args));
	}

	/**
	 * Parse an optional flag as long as it exists.
	 * @param Name Name to put the flag under
	 * @param Flag The flag itself to check for
	 * @return The ArgumentUtil to allow chaining calls.
	 */
	public ArgumentUtil OptionalFlag(String Name, String Flag)
	{
		if (UnparsedArgs.contains(Flag))
		{
			int index = UnparsedArgs.indexOf(Flag);
			this.ParsedArgs.put(Name, UnparsedArgs.get(index));
			UnparsedArgs.remove(index);
		}
		return this;
	}

	/**
	 * Parse an optional string from a position in a list
	 * @param Name Name to put the string under
	 * @param position Position in the parsed argument array
	 * @return The ArgumentUtil to allow chaining calls.
	 */
	public ArgumentUtil OptionalString(String Name, int position)
	{
		if (UnparsedArgs.size() > position)
		{
			this.ParsedArgs.put(Name, UnparsedArgs.get(position));
			UnparsedArgs.remove(position);
		}
		return this;
	}

	/**
	 * Parse an optional sentence from the argument list
	 * @param Name Name to put the sentence under.
	 * @param start Position the sentence starts at
	 * @param end Position the sentence ends at (maybe -1 if it's the rest of the arguments)
	 * @return The ArgumentUtil to allow chaining calls.
	 */
	public ArgumentUtil OptionalSentence(String Name, int start, int end)
	{
		System.out.println(start);
		System.out.println(end);
		if (start > end)
			throw new IllegalArgumentException("start cannot be greater than end");
		if (UnparsedArgs.size() > start)
		{
			// Put our parsed argument in the map
			this.ParsedArgs.put(Name, ConcatenateRest(UnparsedArgs.toArray(new String[UnparsedArgs.size()]), start, end, " "));
			// Remove the parsed arguments from the map.
			for (int s = start; s != end; s++)
				UnparsedArgs.remove(s);
		}
		return this;

	}

	/**
	 * Parse an optional sentence from the rest of an argument list
	 * @param Name Name to put the sentence under.
	 * @param start Start point in the list of where the sentence starts
	 * @return The ArgumentUtil to allow chaining calls.
	 */
	public ArgumentUtil OptionalSentence(String Name, int start)
	{
		return this.OptionalSentence(Name, start, UnparsedArgs.size());
	}

	/**
	 * Ensure a string is in a specific spot and if it is, put it in an array
	 * @param Name Name to put the string under
	 * @param position position to ensure the string is in
	 * @return The ArgumentUtil to allow chaining calls.
	 */
	public ArgumentUtil RequiredString(String Name, int position)
	{
		if (UnparsedArgs.size() > position)
			this.ParsedArgs.put(Name, UnparsedArgs.get(position));
		else
			this.IsValidArgs = false;
		return this;

	}

	/**
	 * Ensure a flag is specified in the command
	 * @param Name Name to put the flag under
	 * @param Flag Flag that should be expected
	 * @return The ArgumentUtil to allow chaining calls.
	 */
	public ArgumentUtil RequiredFlag(String Name, String Flag)
	{
		if (UnparsedArgs.contains(Flag))
		{
			int index = UnparsedArgs.indexOf(Flag);
			this.ParsedArgs.put(Name, UnparsedArgs.get(index));
		}
		else
			this.IsValidArgs = false;
		return this;
	}

	/**
	 * Ensure a sentence is specifed in the command
	 * @param Name Name to put the sentence under
	 * @param start Starting point of the sentence
	 * @param end Ending point of the sentence (or -1 if it's the rest of the arguments)
	 * @return The ArgumentUtil to allow chaining calls.
	 */
	public ArgumentUtil RequiredSentence(String Name, int start, int end)
	{
		// Sanity check
		if (start > end && end != -1)
			throw new IllegalArgumentException("start cannot be greater than end");

		// get our arguments.
		if (UnparsedArgs.size() > start)
			this.ParsedArgs.put(Name, ConcatenateRest(UnparsedArgs.toArray(new String[UnparsedArgs.size()]), start, end, " "));
		else
			this.IsValidArgs = false;

		return this;
	}

	/**
	 * Ensure a sentence is specifed in the command, using the rest of the argunments in the array.
	 * @param Name Name to put the sentence under
	 * @param start Starting point of the sentence
	 * @return The ArgumentUtil to allow chaining calls.
	 */
	public ArgumentUtil RequiredSentence(String Name, int start)
	{
		return this.RequiredSentence(Name, start, -1);
	}

	/**
	 * This actually validates the parsed arguments and ensures everything is what it should be.
	 * @return True if the command requirements were met.
	 */
	public boolean IsValid()
	{
		return IsValidArgs;
	}

	/**
	 * Get the internal unparsed arguments array.
	 * This is useful for commands which have sub-commands
	 * like /banwave
	 * @return the array of unparsed argumenst from the originally provided list.
	 */
	public String[] GetUnparsedArgs()
	{
		return this.UnparsedArgs.toArray(new String[0]);
	}

	/**
	 * Returns the named argument
	 * @param Name
	 * @return Value from command's arguments.
	 */
	public String get(String Name)
	{
		if (ParsedArgs.containsKey(Name))
			return ParsedArgs.get(Name);
		return null;
	}

	/**
	 * Concatenate an array of strings to a single string between a specified range, gluing together
	 * each string with the delim specified.
	 * @param args The array to concatenate
	 * @param offset The offsent inside the array, if applicable
	 * @param end The end point inside the array or -1 if the end of the array
	 * @param delim The character(s) used to glue together each string
	 * @return A string of all the concatenated elements
	 */
	public static String ConcatenateRest(String[] args, int offset, int end, String delim)
    {
        return args.length > 1 ? String.join(delim, Arrays.copyOfRange(args, offset, end == -1 ? args.length : end)) : args[1];
    }
}