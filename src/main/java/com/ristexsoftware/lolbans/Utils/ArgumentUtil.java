package com.ristexsoftware.lolbans.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

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
	private List<String> UnparsedArgs;
	private boolean IsValidArgs = true;

	public ArgumentUtil(String args[])
	{
		UnparsedArgs = Arrays.asList(args);
	}

	// Used for -s and the likes.
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

	public ArgumentUtil OptionalString(String Name, int position)
	{
		if (UnparsedArgs.size() > position)
		{
			this.ParsedArgs.put(Name, UnparsedArgs.get(position));
			UnparsedArgs.remove(position);
		}
		return this;
	}

	public ArgumentUtil OptionalSentence(String Name, int start, int end)
	{
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

	public ArgumentUtil OptionalSentence(String Name, int start)
	{
		return this.OptionalSentence(Name, start, -1);
	}

	public ArgumentUtil RequiredString(String Name, int position)
	{
		if (UnparsedArgs.size() > position)
			this.ParsedArgs.put(Name, UnparsedArgs.get(position));
		else
			this.IsValidArgs = false;
		return this;

	}

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

	public ArgumentUtil RequiredSentence(String Name, int start)
	{
		return this.RequiredSentence(Name, start, -1);
	}

	/**
	 * This actually validates the parsed arguments and ensures everything is what it should be.
	 * @return
	 */
	public boolean IsValid()
	{
		return IsValidArgs;
	}

	/**
	 * Get the internal unparsed arguments array.
	 * This is useful for commands which have sub-commands
	 * like /banwave
	 * @return
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

	public static String ConcatenateRest(String[] args, int offset, int end, String delim)
    {
        return args.length > 1 ? String.join(delim, Arrays.copyOfRange(args, offset, end == -1 ? args.length : end)) : args[1];
    }
}