package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

/*
 * This class draws the box around the ban message (if enabled)
 * which should allow us to format our ban messages however we like
 * whether it be left-aligned, right-aligned, or split messages.
 * 
 * Minecraft by default will center-align everything but if every line
 * is identical in length there's no difference. We trick their alignment
 * code by thinking there really is all this space here.
 * 
 * A sample ban can look as follows:
 ┌─────────────────────────────────────────────────────────────┐
 │     You have been PERMANENTLY suspended from My Network     │
 ├─────────────────────────────────────────────────────────────┤
 │ Expiration: Fri, Nov 4th 2019 22:37:04 GMT                  │
 │ Ban ID:  #E745A27D8                                         │
 │ Banned By: iZachery                                         │
 │ Reason: You're a tool, bye.                                 │
 └─────────────────────────────────────────────────────────────┘
 * By making the ban use box-drawing characters, it center-aligns the box
 * and not the text within which is subject to alignment via padding
 * to ensure the box-drawing characters are aligned. This can be achieved
 * by measuring the maximum length of a line (maybe add 2 or 3 chars of pad)
 * and using that as the max length of the box. This is then used to calculate
 * how much padding we need on each line. This can also allow for complex ban
 * messages such as table-styleized bans. This depends on Minecraft supporting
 * the ASCII box-drawing chars (which I expect it should.)
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.lang.Math;

import org.bukkit.ChatColor;

public class BoxDrawer
{
    public static String PadRight(String s, int n)
    {
        return String.format("%-" + n * 4 + "s", s);  
    }

    public static String PadLeft(String s, int n)
    {
        return String.format("%" + n + "s", s);
    }

    public static String PadCenter(String s, int n)
    {
        return String.format("%"+ n/2 +"s", s) + String.join("", Collections.nCopies(n/2, " ")); 
    }

    // This function gets the displayed chars (not just the unicode sequences that add colors)
    public static int GetVisibleLength(String str)
    {
        // we do *2 here because there is always a code for the color after the color char
        return str.length() - ((int)str.chars().filter(ch -> ch == ChatColor.COLOR_CHAR).count() * 2);
    }

    private List<String> BoxStrings = new ArrayList<String>();
    public void SetTitle(String title)
    {
        this.BoxStrings.set(0, title);
    }

    public void AddString(String string)
    {
        BoxStrings.addAll(Arrays.asList(string.split("\n")));
    }

    public String RenderBox()
    {
        // First, get longest string in the pack + 2 for 1 char of pad on each side.
        int MaxLen = 0, DebugLen = 0;
        for (String str : this.BoxStrings)
            MaxLen = Math.max(BoxDrawer.GetVisibleLength(str), MaxLen);

        // Now start box drawing.
        String out = "\u250C " + String.join("", Collections.nCopies(MaxLen-2, "\u2500")) + " \u2510\n";
        DebugLen = BoxDrawer.GetVisibleLength(out);
        // First line is nothing but the top of the box.

        for (int i = 0; i < this.BoxStrings.size(); ++i)
        {
            // First iteration is our title.
            if (i == 0)
            {
                out += "\u2502 " + BoxDrawer.PadRight(this.BoxStrings.get(i), MaxLen) + " \u2502\n";
                // Append an additional line.
                out += "\u251C " + BoxDrawer.PadRight(String.join("", Collections.nCopies(MaxLen, "\u2500")), MaxLen-2) + " \u2524\n";
                continue;
            }

            // The rest of the lines are drawn normally.
            // TODO: Handle justification/alignment of text
            out += "\u2502 " + BoxDrawer.PadRight(this.BoxStrings.get(i), MaxLen) + " \u2502\n";
        }

        // Draw the last line at the bottom.
        out += "\u2514" + String.join("", Collections.nCopies(MaxLen-2, "\u2500")) + "\u2518\n";

        // Perform an idiot check here for debug.
        for (String str : out.split("\n"))
        {
            if (BoxDrawer.GetVisibleLength(str) != DebugLen)
                System.out.println("String lengths don't match in box!!");
        }

        return out;
    }
}