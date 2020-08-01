/* 
 *  LolBans - The advanced banning system for Minecraft
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@SuppressWarnings({"unchecked", "deprecation"})
public class ReflectionUtil {
    public static void setProtectedValue(Object o, String field, Object newValue) {
        setProtectedValue(o.getClass(), o, field, newValue);
    }

    public static void setProtectedValue(Class<?> c, String field, Object newValue) {
        setProtectedValue(c, null, field, newValue);
    }

    public static void setProtectedValue(Class<?> c, Object o, String field, Object newValue) {
        try {
            Field f = c.getDeclaredField(field);
            f.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            if (!modifiersField.isAccessible())
                throw new IllegalAccessException("Cannot set field as accessible");

            if (Modifier.isFinal(f.getModifiers())) {
                modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);

                if (Modifier.isFinal(f.getModifiers()))
                    throw new IllegalAccessException("Cannot set field as non-final. Is this assigned final?");
            }

            f.set(o, newValue);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            System.out.println("*** " + c.getName() + ":" + ex);
        }
    }

    public static <T> T getProtectedValue(Object obj, String fieldName) {
        try {
            Class<?> c = obj.getClass();
            while (c != Object.class) {
                Field[] fields = c.getDeclaredFields();
                for (Field f : fields) {
                    if (f.getName() == fieldName) {
                        f.setAccessible(true);
                        return (T) f.get(obj);
                    }
                }
                c = c.getSuperclass();
            }
            // System.out.println("*** " + obj.getClass().getName() + ":No such field");
            return null;
        } catch (Exception ex) {
            // System.out.println("*** " + obj.getClass().getName() + ":" + ex);
            return null;
        }
    }

    public static <T> T getProtectedValue(Class<?> c, String field) {
        try {
            Field f = c.getDeclaredField(field);
            f.setAccessible(true);
            return (T) f.get(c);
        } catch (Exception ex) {
            System.out.println("*** " + c.getName() + ":" + ex);
            return null;
        }
    }

    public static Object invokeProtectedMethod(Class<?> c, String method, Object... args) {
        return invokeProtectedMethod(c, null, method, args);
    }

    public static Object invokeProtectedMethod(Object o, String method, Object... args) {
        return invokeProtectedMethod(o.getClass(), o, method, args);
    }

    public static Object invokeProtectedMethod(Class<?> c, Object o, String method, Object... args) {
        try {
            Class<?>[] pTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Integer)
                    pTypes[i] = int.class;
                else
                    pTypes[i] = args[i].getClass();
            }

            Method m = c.getDeclaredMethod(method, pTypes);
            m.setAccessible(true);
            return m.invoke(o, args);
        } catch (Exception ex) {
            System.out.println("*** " + c.getName() + "." + method + "(): " + ex);
            return null;
        }
    }
}