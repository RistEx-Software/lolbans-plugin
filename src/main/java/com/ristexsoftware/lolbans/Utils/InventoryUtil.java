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

package com.ristexsoftware.lolbans.Utils;

import java.util.HashMap;
import java.util.Map;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.GUI;
import com.ristexsoftware.lolbans.Objects.GUIs.HistoryGUI;

import org.bukkit.plugin.Plugin;

// There :3
// import pls
public class InventoryUtil {
    
    private static Map<String, GUI> guiMap = new HashMap<String, GUI>(); 
    private static Plugin self = Main.getPlugin(Main.class);

    public static void LoadGUIs() {
        guiMap.put("history", new HistoryGUI(45, self));
    }

    public static GUI GetGUI(String name) {
        if(guiMap.get(name) != null) 
            return guiMap.get(name);

        return null;
    }
}