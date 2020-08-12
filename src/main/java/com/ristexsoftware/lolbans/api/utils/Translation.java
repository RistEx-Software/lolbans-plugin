 /* 
 *  LolBans - An advanced punishment management system made for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ristexsoftware.lolbans.api.utils;

import java.lang.Character;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;

public class Translation {
    private static HashMap<String, String> leetReplace = new HashMap<>();

    static {
        leetReplace.put("0", "o");
        leetReplace.put("1", "i");
        leetReplace.put("3", "e");
        leetReplace.put("4", "a");
        leetReplace.put("5", "s");
        leetReplace.put("6", "d");
        leetReplace.put("7", "t");
        leetReplace.put("_", "");
    }

    // {VARIABLE|pluralize:"y,ies"}
    private static String pluralize(String lvalue, String arg) {
        String singlar = "", plural = (arg.isEmpty() ? "s" : arg);
        if (arg.contains(",")) {
            String values[] = arg.trim().split(",");
            singlar = values[0];
            plural = values[1];
        }

        if (lvalue == "1")
            return singlar;
        else
            return plural;
    }

    private static String yesno(String lvalue, String arg) {
        if (arg.isEmpty())
            return Boolean.valueOf(lvalue) ? "yes" : "no";
        else if (arg.contains(","))
            return arg.split(",")[Boolean.valueOf(lvalue) ? 0 : 1];
        else
            return Boolean.valueOf(lvalue) ? arg : "no";
    }

    /**
     * List of functions which are executed as part of the translation functions.
     * When the placeholder parser begins parsing a string, it will parse the
     * following format as below: {@code {VARIABLE|function:"HH:MM:SS"}} Where
     * `VARIABLE` would be the name of the variables passed to the
     * TranslateVariables function, `function` would be the name of the function in
     * this class to execute, and everything in quotes after the colon would be the
     * 2nd argument passed to the functions in this map.
     * 
     * Each function takes 2 arguments, the first being the dereferenced value of
     * the variable itself as a string and the 2nd being the additional arguments
     * provided in quotes after the colon in the placeholder string. For example a
     * string with `{TimeBanned|datetime:"HH:MM:SS"}` would execute the `datetime`
     * function in the map below as:
     * {@code datetime(Variables.get("TimeBanned"), "HH:MM:SS")}
     */
    public static TreeMap<String, BiFunction<String, String, String>> Functions = new TreeMap<String, BiFunction<String, String, String>>(String.CASE_INSENSITIVE_ORDER) {
        {
            put("pluralize", (String lvalue, String arg) -> {
                return pluralize(lvalue, arg);
            });
            put("datetime", (String lvalue, String args) -> {
                return lvalue == null || lvalue == "" ? "Never"
                        : (new SimpleDateFormat(args)).format(Timestamp.valueOf(lvalue));
            });
            put("duration", (String lvalue, String unused) -> {
                return lvalue == null || lvalue == "" ? "Never" : TimeUtil.expires(Timestamp.valueOf(lvalue));
            });
            put("expiry", (String lvalue, String unused) -> {
                return lvalue == null || lvalue == "" ? "Never" : TimeUtil.expires(Timestamp.valueOf(lvalue));
            });
            put("cut", (String lvalue, String arg) -> {
                return lvalue.replace(arg, "");
            });
            put("empty_if_false", (String lvalue, String arg) -> {
                return Boolean.valueOf(lvalue) ? arg : "";
            });
            put("empty_if_true", (String lvalue, String arg) -> {
                return Boolean.valueOf(lvalue) ? "" : arg;
            });
            put("default_if_none", (String lvalue, String arg) -> {
                return lvalue == null ? arg : lvalue;
            });
            put("lower", (String lvalue, String unused) -> {
                return lvalue.toLowerCase();
            });
            put("upper", (String lvalue, String unused) -> {
                return lvalue.toUpperCase();
            });
            put("yesno", (String lvalue, String arg) -> {
                return yesno(lvalue, arg);
            });
        }
    };

    // Java apparently has no capabiliy to do something even a simple language like
    // C can do
    // which is parse hexidecimal numbers and tell me if they're fucking valid. This
    // function
    // will do exactly what I need by checking if the char is A through F, 0 through
    // 9.
    /**
     * Checks if the character is a valid minecraft color code
     * 
     * @param ch The character to check for a valid color code char
     * @return True if the character is valid minecraft colorcode
     * @deprecated Since Minecraft 1.16 supports 32 bit colors, this function will
     *             be deprecated.
     */
    public static boolean isxdigit(char ch) {
        // First check if the char is a digit, Java can manage to do this one amazingly.
        if (!Character.isDigit(ch)) {
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

    /**
     * Replace the character sequence in `chars` to swap out with the minecraft
     * color char while also validating that the color code sequence is valid.
     * 
     * @param chars   Character sequence to replace with the section character
     *                minecraft uses for color codes
     * @param message Message containing sequences of `chars` in it
     * @return A color formatted message for Minecraft clients.
     */
    public static String translateColors(String chars, String message) {
        if (message == null)
            return null;

        if (chars == null)
            return message;

        // Don't allocate if we don't have to.
        if (!message.contains(chars))
            return message;

        StringBuilder retstr = new StringBuilder(message);
        for (int pos = message.indexOf(chars); pos != -1; pos = message.indexOf(chars, pos)) {
            if (pos + 1 > message.length())
                break;

            // Make sure the next char is valid hex as Minecraft uses a hexidecimal number
            if (Translation.isxdigit(message.charAt(pos + 1))) {
                // Now we replace the starting char with our special char.
                retstr.setCharAt(pos, SPECIAL_CHAR);
                pos += 2;
            } else // Skip 2 characters, invalid sequence.
                pos += 2;
        }

        return retstr.toString();
    }

    // Used to replace variables inside of strings.
    // {Player} has been banned by {Executioner}: {Reason}
    // {Player has been banned by CONSOLE: fuck you.
    /**
     * Replace all placeholders in a string, executing placeholder functions in the
     * process to format strings with variables provided.
     * 
     * @param message   The message to have placeholders replaced
     * @param Variables The variables to be utilized in this message for the
     *                  placeholders and their functions
     * @return Formatted string with all placeholders from Variables replaced.
     */
    public static String translateVariables(String message, Map<String, String> Variables) {
        // If it doesn't have the starting char for variables, skip it.
        if (!message.contains("{") || Variables == null)
            return message;

        String retstr = message;
        // Try and iterate over all our variables.
        for (int pos = retstr.indexOf("{"), pos2 = retstr.indexOf("}", pos); pos != -1
                && pos2 != -1; pos = retstr.indexOf("{", pos + 1), pos2 = retstr.indexOf("}", pos + 1)) {
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
            if (variable.contains("|")) {
                String values[] = variable.split("\\|");
                String rvalue = values[1], lvalue = values[0];

                if (rvalue.contains(":")) {
                    int nextsplit = rvalue.indexOf(":");
                    rvalue = rvalue.substring(0, nextsplit);
                    String argument = values[1].substring(nextsplit + 2, values[1].length() - 1);

                    replacement = Functions.get(rvalue.trim()).apply(Variables.get(lvalue.trim()), argument);
                } else // (Functions.containsKey(rvalue.trim()) &&
                       // Variables.containsKey(lvalue.trim()))
                    replacement = Functions.get(rvalue.trim()).apply(Variables.get(lvalue.trim()), "");
            } else if (Variables.containsKey(variable)) {
                // Now we replace it with our value from the map.
                replacement = Variables.get(variable);
            }

            if (replacement != null)
                retstr = retstr.substring(0, pos) + replacement + retstr.substring(pos2 + 1);
        }
        return retstr;
    }

    /**
     * Translate the preformatted string to a fully formatted string ready for
     * players to see, switching out color codes and placeholders.
     * 
     * @param message    The message containing placeholders and untranslated color
     *                   code sequences
     * @param ColorChars The character used as the prefix for color strings
     *                   (bukkit/spigot use `&amp;` and so do we most of the time)
     * @param Variables  A list of variables to be parsed by the placeholder
     * @return A string with color sequences and placeholders translated to their
     *         formatted message ready for the player.
     * @see TranslateVariables
     */
    public static String translate(String message, String ColorChars, Map<String, String> Variables) {
        String retstr = Translation.translateColors(ColorChars, message);
        retstr = Translation.translateVariables(retstr, Variables);
        return retstr;
    }
    /**
     * Replace a word with asterisks.
     * @param word The word to censor
     * @return The censored word
     */
    public static String censorWord(String word) {
        StringBuilder asterisks = new StringBuilder();

        for (int i = 0; i < word.length(); i++) {
            asterisks.append("*");
        }

        return asterisks.toString();
    }

    /**
     * Filter "Leet Speak" out of a message
     * <p>
     * Example:
     * 
     * <pre>
     * TranslationUtil.replaceLeet("50m3 1337 5p34k h3r3") = "some leet speak here"
     * </pre>
     * 
     * @param message The message to filter
     * @return The filtered message
     */
    public static String replaceLeet(String message) {
        if (message.trim().isEmpty())
            return message;

        for (Map.Entry<String, String> entry : leetReplace.entrySet())
            message = message.replaceAll(entry.getKey(), entry.getValue());

        return message;
    }
}