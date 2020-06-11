package com.ristexsoftware.lolbans.Utils;



import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/*

        _____
       |    ||
       |\___/|
       |     |
       |     |
       |     |
       |     |
       |     |
       |     |
   ____||____|____
  /    |     |     \
 /     |     |    | \
|      |     |    |  |
|      |     |    |  |
|                 |  |
|                 |  |
|                    /
|                   /
 \                 /
  \               /
   |             |
   |             |

*/

import java.util.Optional;

public class TimeUtil
{
    private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d yyyy HH:mm:ss");

    /** A lookup table of values for multiplier characters used by
     * TimeUtil.Duration(). In this lookup table, the indexes for
     * the ascii values 'm' and 'M' have the value '60', the indexes
     * for the ascii values 'D' and 'd' have a value of '86400', etc.
     */
    private static final long duration_multi[] = 
    {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 86400, 0, 0, 0, 3600, 0, 0, 0, 0, 60, 0, 0,
        0, 0, 0, 1, 0, 0, 0, 604800, 0, 31557600, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 86400, 0, 0, 0, 3600, 0, 0, 0, 0, 60, 0, 0,
        0, 0, 0, 1, 0, 0, 0, 604800, 0, 31557600, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    /**
     * Convert the unix timestamp to a human readable duration string
     * @param t {@link java.sql.Timestamp}
     * @return a human readable duration
     */
    public static String DurationString(Timestamp t)
    {
        return TimeUtil.DurationString(t.getTime() / 1000L);
    }

    /**
     * Convert the unix timestamp to a human readable duration string
     * @param t unix timestamp
     * @return human readable duration
     */
    public static String DurationString(long t)
    {
        long years = t / 31449600;
        long weeks = (t / 604800) % 52;
        long days = (t / 86400) % 7;
        long hours = (t / 3600) % 24;
        long minutes = (t / 60) % 60;
        long seconds = t % 60;

        if (years == 0 && days == 0 && hours == 0 && minutes == 0)
            return String.format("%d %s", seconds, seconds != 1 ? "seconds" : "second");
        else
        {
            boolean need_comma = false;
            String buffer = "";
            if (years != 0)
            {
                buffer = String.format("%d %s", years, years != 1 ? "years" : "year");
                need_comma = true;
            }
            if (weeks != 0)
            {
                buffer += need_comma ? ", " : "";
                buffer += String.format("%d %s", weeks, weeks != 1 ? "weeks" : "week");
                need_comma = true;
            }
            if (days != 0)
            {
                buffer += need_comma ? ", " : "";
                buffer += String.format("%d %s", days, days != 1 ? "days" : "day");
                need_comma = true;
            }
            if (hours != 0)
            {
                buffer += need_comma ? ", " : "";
                buffer += String.format("%d %s", hours, hours != 1 ? "hours" : "hour");
                need_comma = true;
            }
            if (minutes != 0)
            {
                buffer += need_comma ? ", " : "";
                buffer += String.format("%d %s", minutes, minutes != 1 ? "minutes" : "minute");
                need_comma = true;
            }
            if (seconds != 0)
            {
                buffer += need_comma ? ", and ": "";
                buffer += String.format("%d %s", seconds, seconds != 1 ? "seconds" : "second");
            }
            return buffer;
        }
    }

    /**
     * Convert unix timestamp to a duration forward or backward to current system time
     * @param ts {@link java.sql.Timestamp}
     * @return A string stating the duration forward or backward in time.
     */
    public static String Expires(Timestamp ts)
    {
        if (ts == null)
            return "";
        return TimeUtil.Expires(ts.getTime() / 1000L);
    }

    /**
     * Convert unix timestamp to a duration forward or backward to current system time
     * @param expires A unix timestamp
     * @return A string stating the duration forward or backward in time.
     */
    public static String Expires(long expires)
    {
        long CurTime = TimeUtil.GetUnixTime();
        if (expires == 0)
            return "never expires";
        else if (expires < CurTime)
            return String.format("%s ago", TimeUtil.DurationString(CurTime - expires));
        else
            return String.format("%s from now", TimeUtil.DurationString(expires - CurTime));
    }

    /**
     * Parse a duration string to a length of time in seconds.
     * @param s the string of characters to convert to a quantity of seconds
     * @return The duration converted to seconds
     */
    public static Optional<Long> Duration(String s)
    {
        long total = 0;
        long subtotal = 0;

        for (int i = 0; i < s.length(); ++i)
        {
            char ch = s.charAt(i);
            if ((ch >= '0') && (ch <= '9'))
                subtotal = (subtotal * 10) + (ch - '0');
            else
            {
                /* Found something thats not a number, find out how much
                 * it multiplies the built up number by, multiply the total
                 * and reset the built up number.
                 */

                long multiplier = TimeUtil.duration_multi[ch];
                if (multiplier == 0)
                    return Optional.empty();

                total += subtotal * multiplier;

                // Next subtotal please!
                subtotal = 0;
            }
        }

        return Optional.of(total + subtotal);
    }

    /**
     * Convert a unix timestamp to a human readable date string
     * @param t Number of seconds since unix epoch
     * @return A string with the format "EEE, MMM d yyyy HH:mm:ss"
     * @deprecated Use the {@link com.ristexsoftware.lolbans.Utils.TranslationUtil} functions for converting timestamps to human readable strings.
     * {@link java.text.SimpleDateFormat}
     */
	@Deprecated
    public static String TimeString(long t)
    {
        return TimeUtil.TimeString(new Timestamp(t));
    }

	  /**
	   * Convert a SQL Timestamp to a human readable date string
	   * @param ts The time as a Timestamp object
	   * @return A string with the format "EEE, MMM d yyyy HH:mm:ss"
	   * @deprecated Use the {@link com.ristexsoftware.lolbans.Utils.TranslationUtil} functions for converting timestamps to human readable strings.
	   * {@link java.text.SimpleDateFormat}
	   */
	@Deprecated
	public static String TimeString(Timestamp ts)
    {
        if (ts == null)
            return "";
        return sdf.format(ts);
    }

    /**
     * Convert a human-readable duration to a SQL Timestamp object
     * @param TimePeriod A duration string (eg, "2y1w10d40m6s")
     * @return {@link java.sql.Timestamp}
     */
    // I spent 2 hours ensuring this method worked...
    // Turns out, I need to read documentation more, java.sql.Timestamp uses milliseconds and not seconds, I somehow forgot that.
    public static Timestamp ParseToTimestamp(String TimePeriod)
    {
        System.out.println(TimePeriod);
        // Parse ban time.
        // If it's numeric, lets do some extra checks!
        if (NumberUtil.isNumeric(TimePeriod)) {
            // Return null if it's greater 12 characters long
            if (TimePeriod.length() > 12) return null;
            Optional<Long> dur = TimeUtil.Duration(TimePeriod);
            if (dur.isPresent())
                return new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L).getTime() >= new java.sql.Timestamp(253402261199L * 1000L).getTime() ? null : new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L);
                // 253402261199 is the year 9999 in epoch, this ensures we don't have invalid timestamp exceptions.
        } 
        if (!Messages.CompareMany(TimePeriod, new String[]{"*", "0"}))
        {
            Optional<Long> dur = TimeUtil.Duration(TimePeriod);
            if (dur.isPresent())
                return new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L).getTime() >= new java.sql.Timestamp(253402261199L * 1000L).getTime() ? null : new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L);
        }
            
        return null;
    } 

    /**
     * Get the current system time as a Unix timestamp
     * @return Current number of seconds since Unix Epoch
     */
    public static long GetUnixTime()
    {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * Get the current system time as a SQL Timestamp object
     * @return {@link java.sql.Timestamp}
     */
    public static Timestamp TimestampNow()
    {
        return new Timestamp(TimeUtil.GetUnixTime() * 1000L);
    }
}
/* 

    public static Timestamp ParseToTimestamp(String TimePeriod)
    {
        System.out.println(TimePeriod);
        System.out.println("Fuck1");
        // Parse ban time.
        if (NumberUtil.isNumeric(TimePeriod)) {
            System.out.println("Fuck2");
            if (TimePeriod.length() > 12) return null;
            Optional<Long> dur = TimeUtil.Duration(TimePeriod);
            System.out.println("Fuck3");
            if (dur.isPresent())
                return new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L).getTime() >= new java.sql.Timestamp(253402261199L).getTime() ? null : new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L);
        } 
        if (!Messages.CompareMany(TimePeriod, new String[]{"*", "0"}))
        {
            System.out.println("Fuck4");
            Optional<Long> dur = TimeUtil.Duration(TimePeriod);
            if (dur.isPresent())
                return new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L).getTime() >= new java.sql.Timestamp(253402261199L).getTime() ? null : new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L);
        }
        System.out.println("Fuck5");
            
        return null;
    } */