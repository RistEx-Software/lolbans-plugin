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

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ClickableSlot {
    
    private ItemStack is;
    private int slot;

    public ClickableSlot(Material pMaterial, String pName, int pAmount, int pSlot) {
        this.is = new ItemStack(pMaterial, pAmount);

        ItemMeta isM = is.getItemMeta();
        isM.setDisplayName(pName);
        is.setItemMeta(isM);

        this.slot = pSlot;
    }

    public void Execute(Runnable r) {
        new Thread(r).start();
    }

    public ItemMeta GetMeta() {
        return is.getItemMeta();
    }

    public ItemStack GetItem() {
        return is;
    }

    public int GetSlotId() {
        return slot;
    }

    public void SetName(String s) {
        ItemMeta isM = this.is.getItemMeta();
        isM.setDisplayName(s);
        is.setItemMeta(isM);
    }

    public String GetName() {
        return is.getItemMeta().getDisplayName();
    }
}