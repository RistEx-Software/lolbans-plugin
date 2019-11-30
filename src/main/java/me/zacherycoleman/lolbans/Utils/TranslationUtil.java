package me.zacherycoleman.lolbans.Utils; // Zachery's package owo

import java.lang.Character;
import java.util.Map;
import java.util.TreeMap;

public class TranslationUtil
{
    // Java apparently has no capabiliy to do something even a simple language like C can do
    // which is parse hexidecimal numbers and tell me if they're fucking valid. This function
    // will do exactly what I need by checking if the char is A through F, 0 through 9.
    public static boolean isxdigit(char ch)
    {
        // First check if the char is a digit, Java can manage to do this one amazingly.
        if (!Character.isDigit(ch))
        {
            // If it's not a number between 0 through 9, check if it's A through F
            // If we are lower case, switch to upper and compare that way.
            if (ch > 97)
                ch -= 32;

            // if they're greater than 70 (aka 'F') but less than 65 (aka 'A')
            // then it's not valid hexidecimal.
            if (ch > 70 || ch < 65)
                return false;
        }
        
        return true;
    }

    public static final char SPECIAL_CHAR = '\u00A7';
    // Used to translate colors
    public static String TranslateColors(String chars, String message)
    {
        // Don't allocate if we don't have to.
        if (!message.contains(chars))
            return message;

        StringBuilder retstr = new StringBuilder(message);
        for (int pos = message.indexOf(chars); pos != -1; pos = message.indexOf(chars, pos))
        {
            if (pos + 1 > message.length())
                break;

            // Make sure the next char is valid hex as Minecraft uses a hexidecimal number
            if (TranslationUtil.isxdigit(message.charAt(pos + 1)))
            {
                // Now we replace the starting char with our special char.
                retstr.setCharAt(pos, SPECIAL_CHAR);
                pos += 2;
            }
            else // Skip 2 characters, invalid sequence.
                pos += 2;
        }

        return retstr.toString();
    }

    // Used to replace variables inside of strings.
    // {Player} has been banned by {Executioner}: {Reason}
    // {Player has been banned by CONSOLE: fuck you.
    public static String TranslateVariables(String message, Map<String, String> Variables)
    {
        // If it doesn't have the starting char for variables, skip it.
        if (!message.contains("{") || Variables == null)
            return message;

        String retstr = message;
        // Try and iterate over all our variables.
        for (int pos = retstr.indexOf("{"), pos2 = retstr.indexOf("}", pos);
            pos != -1 && pos2 != -1;
            pos = retstr.indexOf("{", pos + 1), pos2 = retstr.indexOf("}", pos + 1))
        {
            // If we're longer than we should be.
            if (pos + 1 > retstr.length() || pos2 + 1 > retstr.length())
                break;

            // Substring.
            String variable = retstr.substring(pos + 1, pos2);
            if (Variables.containsKey(variable))
            {
                // Now we replace it with our value from the map.
                //System.out.println("SUBSTRING BEGIN: " + retstr.substring(0, pos));
                retstr = retstr.substring(0, pos) + Variables.get(variable) + retstr.substring(pos2 + 1);
            }
        }
        return retstr;
    }

    public static String Translate(String message, String ColorChars, Map<String, String> Variables)
    {
        String retstr = TranslationUtil.TranslateColors(ColorChars, message);
        retstr = TranslationUtil.TranslateVariables(retstr, Variables);
        return retstr;
    }
}