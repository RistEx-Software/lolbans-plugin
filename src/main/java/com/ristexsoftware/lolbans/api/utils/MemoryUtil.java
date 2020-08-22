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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.ristexsoftware.lolbans.common.utils.ReflectionUtil;

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