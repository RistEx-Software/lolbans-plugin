package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

import java.lang.Character;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;

public class TranslationUtil
{
    // {VARIABLE|pluralize:"y,ies"}
    private static String Pluralize(String lvalue, String arg)
    {
        String singlar = "", plural = (arg.isEmpty() ? "s" : arg);
        if (arg.contains(","))
        {
            String values[] = arg.trim().split(",");
            singlar = values[0];
            plural = values[1];
        }

        if (lvalue == "1")
            return singlar;
        else
            return plural;
    }

    private static String yesno(String lvalue, String arg)
    {
        if (arg.isEmpty())
            return Boolean.valueOf(lvalue) ? "yes" : "no";
        else if (arg.contains(","))
            return arg.split(",")[Boolean.valueOf(lvalue) ? 0 : 1];
        else
            return Boolean.valueOf(lvalue) ? arg : "no";
    }

    // {VARIABLE|function:"HH:MM:SS"}
    public static TreeMap<String, BiFunction<String, String, String>> Functions = new TreeMap<String, BiFunction<String, String, String>>(String.CASE_INSENSITIVE_ORDER)
    {{
        put("pluralize", (String lvalue, String arg) -> { return Pluralize(lvalue, arg); });
        put("datetime", (String lvalue, String args) -> { return lvalue.isEmpty() ? "" : (new SimpleDateFormat(args)).format(Timestamp.valueOf(lvalue)); });
        put("duration", (String lvalue, String unused) -> { return TimeUtil.DurationString(Timestamp.valueOf(lvalue)); });
        put("expiry", (String lvalue, String unused) -> { return TimeUtil.Expires(Timestamp.valueOf(lvalue)); });
        put("cut", (String lvalue, String arg) -> { return lvalue.replace(arg, ""); });
        put("empty_if_false", (String lvalue, String arg) -> { return Boolean.valueOf(lvalue) ? arg : ""; });
        put("empty_if_none", (String lvalue, String arg) -> { return lvalue == null ? "" : arg; });
        put("default_if_none", (String lvalue, String arg) -> { return lvalue == null ? arg : lvalue; });
        put("lower", (String lvalue, String unused) -> { return lvalue.toLowerCase(); });
        put("upper", (String lvalue, String unused) -> { return lvalue.toUpperCase(); });
        put("yesno", (String lvalue, String arg) -> {return yesno(lvalue, arg); });
    }};

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
            if (ch >= 97)
                ch -= 32;

            // Minecraft uses some new special chars for formatting so we have
            // to account for those too.
            if (ch == 'R' || (ch < 80 && ch > 74))
                return true;

            // if they're greater than 70 (aka 'F') but less than 65 (aka 'A')
            // then it's not valid hexidecimal.
            if (ch > 71 || ch < 65)
                return false;
        }
        
        return true;
    }

    public static final char SPECIAL_CHAR = '\u00A7';
    // Used to translate colors
    public static String TranslateColors(String chars, String message)
    {
        if (message == null)
            return null;
        
        if (chars == null)
            return message;
            
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
            String replacement = null;
            
            // If the variable contains a | (verticle bar), then we tokenize on `|` and
            // treat the lvalue as a variable and the rvalue as a function name. The
            // functions are stored as a hashmap and only take one string argument
            // ("dereferenced" value of the lvalue map name.). This allows us to do things
            // like conditionally pluralize words and such in the config.
            if (variable.contains("|"))
            {
                String values[] = variable.split("\\|");
                String rvalue = values[1], lvalue = values[0];

                if (rvalue.contains(":"))
                {
                    int nextsplit = rvalue.indexOf(":");
                    rvalue = rvalue.substring(0, nextsplit);
                    String argument = values[1].substring(nextsplit + 2, values[1].length() - 1);
                    
                    replacement = Functions.get(rvalue.trim()).apply(Variables.get(lvalue.trim()), argument);
                }
                else //(Functions.containsKey(rvalue.trim()) && Variables.containsKey(lvalue.trim()))
                    replacement = Functions.get(rvalue.trim()).apply(Variables.get(lvalue.trim()), "");
            }
            else if (Variables.containsKey(variable))
            {
                // Now we replace it with our value from the map.
                replacement = Variables.get(variable);
            }

            if (replacement != null)
                retstr = retstr.substring(0, pos) + replacement + retstr.substring(pos2 + 1);
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