package me.zacherycoleman.lolbans.Utils; // Zachery's package owo

import java.lang.Character;
import java.util.Map;
import java.util.TreeMap;

public class TranslationUtil
{
    public static final char SPECIAL_CHAR = '\u00A7';
    // Used to translate colors
    public static String TranslateColors(String chars, String stuffs)
    {
        // Don't allocate if we don't have to.
        if (!stuffs.contains(chars))
            return stuffs;

        StringBuilder retstr = new StringBuilder(stuffs);
        for (int pos = stuffs.indexOf(chars); pos != -1; pos = stuffs.indexOf(chars, pos))
        {
            String substr = stuffs.substring(pos);
            // Make sure the next char is valid hex as Minecraft uses a hexidecimal number
            if (Character.digit(substr.charAt(pos + 1), 16) != -1)
            {
                // Now we replace the starting char with our special char.
                retstr.setCharAt(pos, SPECIAL_CHAR);
            }
        }

        return retstr.toString();
    }

    // Used to replace variables inside of strings.
    // {Player} has been banned by {Executioner}: {Reason}
    // {Player has been banned by CONSOLE: fuck you.
    public static String TranslateVariables(String stuffs, Map<String, String> Variables)
    {
        // If it doesn't have the starting char for variables, skip it.
        if (!stuffs.contains("{"))
            return stuffs;

        String retstr = stuffs;

        // Try and iterate over all our variables.
        for (int pos = stuffs.indexOf("{"); pos != -1; pos = stuffs.indexOf("{", pos))
        {
            // Find the end of the variable.
            int pos2 = stuffs.indexOf("}", pos);
            // If we don't find this char, this variable is an error. Continue.
            if (pos2 == -1)
                continue;

            // Substring.
            String variable = stuffs.substring(pos + 1, pos);
            
            if (Variables.containsKey(variable))
            {
                // Now we replace it with our value from the map.
                retstr = retstr.substring(0, pos) + Variables.get(variable) + retstr.substring(pos2 + 1);
            }
        }
        return retstr;
    }

    public static String Translate(String message, String ColorChars, Map<String, String> Variables)
    {
        String retstr = TranslationUtil.TranslateColors(message, ColorChars);
        retstr = TranslationUtil.TranslateVariables(retstr, Variables);
        return retstr;
    }

/*     String whatever()
    {
        String MyStr = "{PLAYER} &4has been banned by &7{Executioner}:&4 {reason}";

        String endstr = TranslationUtil.Translate(MyStr, "&", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
        {{
            put("Player", "Justasic");
            put("Executioner", "Zachery");
            put("Reason", "lmao fucking fag");
        }});

        return endstr;
    } */
}