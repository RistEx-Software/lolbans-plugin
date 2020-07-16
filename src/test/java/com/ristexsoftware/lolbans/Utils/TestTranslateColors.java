/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *   
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *   
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *   
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 */

package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
// Javadoc: https://www.jacoco.org/jacoco/trunk/doc/api/index.html
import org.junit.jupiter.api.Test;

public class TestTranslateColors
{
    @DisplayName("Test TranslateColors can accept null")
    @Test
    public void TestTranslateNull()
    {
        // Make sure that given nulls, translate colors will return a null.
        assertEquals(null, TranslationUtil.TranslateColors(null, null));
    }

    @DisplayName("Test TranslateColors can accept a formatted string")
    @Test
    public void TestTranslateString()
    {
        // Make sure that given a string, it translates the colors
        assertEquals("bnyeh", TranslationUtil.TranslateColors("&", "bnyeh"));
    }

    @DisplayName("Test TranslateColors can accept color formatting")
    @Test
    public void TestTranslateColoredString()
    {
        // Make sure that given a string with colors, it returns a colored string
        assertEquals("\u00A74bnyeh", TranslationUtil.TranslateColors("&", "&4bnyeh"));
    }

    @DisplayName("Test TranslateColors can accept different format characters")
    @Test
    public void TestTranslateCharacter()
    {
        // Make sure that the color char can be changed to something else
        assertEquals("\u00A74bnyeh", TranslationUtil.TranslateColors("$", "$4bnyeh"));
    }
}