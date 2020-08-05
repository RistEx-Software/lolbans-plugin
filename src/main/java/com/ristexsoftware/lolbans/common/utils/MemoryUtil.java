package com.ristexsoftware.lolbans.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class MemoryUtil {
    public enum Unit {
        BITS, BYTES, KILOBYTES, MEGABYTES
    }

    /**
     * Get the size of an object, specifying in what units the method should return.
     */
    public static Double getSizeOf(Object object, Unit units) {
        int bits = getSizeOf(object);
        return formatBits(bits, units);
    }

    public static Double formatBits(int bits, Unit units) {
        switch (units) {
            case BITS:
                return (double) bits;
            case BYTES:
                return (double) bits / 8;
            case KILOBYTES:
                return (double) bits / 8 / 1000;
            case MEGABYTES:
                return (double) bits / 8 / 1000 / 1000;
            default: 
                return 0.0;
        }  
    }

    /**
     * Get the approximate size of the given object.
     */
    public static int getSizeOf(Object object) {
        if (object == null) {
            return 0;
        }

        Class<?> clazz = object.getClass();
        int accumulator = 0;

        int size = getSizeOfBuiltin(object);
        if (size != 0) {
            return size;
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            accumulator += getFieldSize(object, field);
        }

        return accumulator;
    }

    /**
     * Get the size of a field on a given object.
     */
    private static int getFieldSize(Object object, Field field) {
        try {
            // Java complains about illegal reflective access... Too bad!
            return getSizeOf(ReflectionUtil.getProtectedValue(object, field.getName()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int getSizeOfBuiltin(Object object) {
        Class<?> clazz = object.getClass();

        // Because Java is literally fucking stupid, we can't use switch here
        // instead we have to pull a Yandere Dev and ABUSE if/else
        if (clazz == String.class) {
            return ((String) (object)).length() * 8;
        }

        if (clazz == Boolean.class) {
            return 1;
        }

        if (clazz == Byte.class) {
            return 8;
        }

        if (clazz == Short.class || clazz == Character.class) {
            return 16;
        }

        if (clazz == Integer.class || clazz == Float.class) {
            return 32;
        }

        if (clazz == Long.class || clazz == Double.class) {
            return 64;
        }

        // potential for stack overflow if a field is circular!
        return 0;
    }
}