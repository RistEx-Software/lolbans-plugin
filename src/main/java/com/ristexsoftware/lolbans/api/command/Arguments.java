package com.ristexsoftware.lolbans.api.command;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.ristexsoftware.lolbans.api.utils.TimeUtil;
import com.ristexsoftware.lolbans.common.utils.Debug;

import lombok.Getter;

/**
 * Utility class for handling command arguments.
 */
public class Arguments {
    @Getter private ArrayList<String> unparsedArgs;
    private HashMap<String, String> parsedArgs = new HashMap<>();

    private int position = 0;
    private boolean valid = true;

    public void invalidate(String name) {
        debug.print("Invalidated by argument " + name);
    }

    public boolean valid() {
        return valid;
    }

    private Debug debug = new Debug(getClass());

    public Arguments(String[] args) {
        unparsedArgs = new ArrayList<String>(Arrays.asList(args));
    }

    /**
     * Create an optional flag.
     */
    public Arguments optionalFlag(String name, String flag) {
        debug.print("Looking for optional flag " + name + "...");
        int index = unparsedArgs.indexOf(flag);
        if (index == -1) {
            debug.print("Could not find flag");
            return this;
        }

        debug.print("Found flag at position " + String.valueOf(index) + " - new args size = " + String.valueOf(unparsedArgs.size()));

        parsedArgs.put(name, unparsedArgs.get(index));
        unparsedArgs.remove(index);

        return this;
    }

    /**
     * Create a required flag.
     */
    public Arguments requiredFlag(String name, String flag) {
        debug.print("Looking for required flag " + name + "...");
        int index = unparsedArgs.indexOf(flag);
        if (index == -1) {
            invalidate(name);
            debug.print("Could not find flag - marking as invalid");
            return this;
        }


        debug.print("Found flag at position " + String.valueOf(index));

        parsedArgs.put(name, unparsedArgs.get(index));
        unparsedArgs.remove(index);

        return this;
    }


    /**
     * Create a required string argument.
     */
    public Arguments optionalString(String name) {
        debug.print("Looking for optional string " + name + "...");
        if (unparsedArgs.size() > position) {
            parsedArgs.put(name, unparsedArgs.get(position));
            unparsedArgs.remove(position);
            debug.print("Found string at position " + String.valueOf(position) + " - new args size = " + String.valueOf(unparsedArgs.size()));
        } else 
            debug.print("Could not find string");

        return this;
    }

    /**
     * Create a required string argument.
     */
    public Arguments requiredString(String name) {
        debug.print("Looking for required string " + name + "...");

        if (unparsedArgs.size() > position) {
            parsedArgs.put(name, unparsedArgs.get(position));
            debug.print("Found string at position " + String.valueOf(position));
            position++;
        } else {
            debug.print("Could not find string - marking as invalid");
            invalidate(name);
        }

        return this;
    }

    /**
     * Create an optional sentence argument, with its length defaulting to the remaining length of
     * current unparsed arguments.
     */
    public Arguments optionalSentence(String name) {
        debug.print("Using default length: " + String.valueOf(unparsedArgs.size() - position));
        return optionalSentence(name, unparsedArgs.size() - position);
    }

    /**
     * Create an optional sentence with the given length.
     */
    public Arguments optionalSentence(String name, int length) {
        int end = position + length;
        debug.print("Looking for required sentence - start = " + String.valueOf(position) + ", end = " + String.valueOf(end) + ", length = " + String.valueOf(length));
    
        if (position >= end)
            throw new IllegalArgumentException("start cannot be greater than or equal to end");
        
        if (unparsedArgs.size() < position + length) {
            debug.print("Could not find sentence of appropriate length (args are size " + String.valueOf(position)
                    + ") - marking as invalid");
            invalidate(name);
            return this;
        }

        String concatenated = String.join(" ", Arrays.copyOfRange(unparsedArgs.toArray(new String[unparsedArgs.size()]), position, end));
        parsedArgs.put(name, concatenated);

        for (int s = position; s != end; s++)
            unparsedArgs.remove(position);
        
        debug.print("Found sentence of length " + String.valueOf(length) + " - new args size = " + String.valueOf(unparsedArgs.size()));

        return this;
    }

    /**
     * Create a required sentence argument, its length defaulting to that of any remaining unparsed arguments.
     */
    public Arguments requiredSentence(String name) {
        debug.print("Using default length: " + String.valueOf(unparsedArgs.size() - position));
        return requiredSentence(name, unparsedArgs.size() - position);
    }

    /**
     * Create a required sentence argument with the given length.
     */
    public Arguments requiredSentence(String name, int length) {
        int end = position + length;
        debug.print("Looking for required sentence - start = " + String.valueOf(position) + ", end = " + String.valueOf(end) + ", length = " + String.valueOf(length));

        // Usually means there aren't enough args left
        if (position >= end) {
            debug.print("Start cannot be greater than or equal to end");
            invalidate(name);
            return this;
        }
        
        if (unparsedArgs.size() < position + length) {
            invalidate(name);
            debug.print("Could not find sentence of appropriate length (args are size " + String.valueOf(unparsedArgs.size()) + ") - marking as invalid");
            return this;
        }

        String concatenated = String.join(" ", Arrays.copyOfRange(unparsedArgs.toArray(new String[unparsedArgs.size()]), position, end));
        parsedArgs.put(name, concatenated);

        position += length;
        
        debug.print("Found sentence of length " + String.valueOf(length));

        return this;
    }

    /**
     * Create an optional timestamp argument.
     */
    public Arguments optionalTimestamp(String name) {
        debug.print("Looking for optional timestamp " + name + "...");
        if (unparsedArgs.size() > position && TimeUtil.toTimestamp(unparsedArgs.get(position)) != null) {
            parsedArgs.put(name, String.valueOf(TimeUtil.toTimestamp(unparsedArgs.get(position)).getTime()));
            unparsedArgs.remove(position);
            debug.print("Found timestamp at position " + String.valueOf(position) + " - new args size = " + String.valueOf(unparsedArgs.size()));
        } else 
            debug.print("Could not find timestamp");

        return this;
    }

    /**
     * Create an optional timestamp argument.
     */
    public Arguments requiredTimestamp(String name) {
        debug.print("Looking for required timestamp " + name + "...");
        if (unparsedArgs.size() > position && TimeUtil.toTimestamp(unparsedArgs.get(position)) != null) {
            parsedArgs.put(name, String.valueOf(TimeUtil.toTimestamp(unparsedArgs.get(position)).getTime()));
            position++;
            debug.print("Found timestamp at position " + String.valueOf(position) + " - new args size = " + String.valueOf(unparsedArgs.size()));
        } else 
            debug.print("Could not find timestamp");

        return this;
    }

    /**
     * Fetch a parsed argument from this arguments object.
     */
    public String get(String name) {
        return parsedArgs.get(name);
    }

    /**
     * Fetch a timestamp.
     */
    public Timestamp getTimestamp(String name) {
        return TimeUtil.toTimestamp(name);
    }

    public Boolean exists(String name) {
        return parsedArgs.get(name) != null;
    }
}