/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Kokumaji <kokumaji@outlook.com>
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

package com.ristexsoftware.lolbans.Objects;

import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public abstract class GUI implements Listener {

    private Inventory inv;
    private int s;
    private Plugin plugin;

    protected GUI(int pSize, Plugin pPlugin) {
        this.s = pSize;
        this.plugin = pPlugin;

    }

    public void RegisterSlot(ItemStack pItem, int pSlot) {
        if(pItem == null) 
            return;
        inv.setItem(pSlot, pItem);
    }

    public abstract void BuildGUI(Player player, String[] args, ArgumentUtil a);
    
    public boolean IsValidSlot(int pSlot) {
        return inv.getItem(pSlot) != null;
    }

    @EventHandler
    public abstract void onSlotClick(InventoryClickEvent e);

    protected Inventory getInventory() {
        return this.inv;
    }

    protected Plugin getPlugin() {
        return this.plugin;
    }

    protected Inventory MakeInventory(String pTitle) {
        return this.inv = Bukkit.createInventory(null, s, pTitle);
    }
}