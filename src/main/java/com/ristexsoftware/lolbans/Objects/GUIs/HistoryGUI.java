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

package com.ristexsoftware.lolbans.Objects.GUIs;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.ClickableSlot;
import com.ristexsoftware.lolbans.Objects.GUI;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.Paginator;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

public class HistoryGUI extends GUI {

    private Main self = Main.getPlugin(Main.class);
    private Paginator<ItemStack> pages;

    public HistoryGUI(int pSize, Plugin pPlugin) {
        super(pSize, pPlugin);

        Bukkit.getServer().getPluginManager().registerEvents(this, self);
    }

    private void GetResults(Player player, String[] args, ArgumentUtil a) {
        ItemStack historyItem;
        PreparedStatement pst = null;
        String type;

        try {
            // Are they not specifying a player? Cool, select e v e r y t h i n g!
            if(args.length < 1 ) {
                pst = self.connection.prepareStatement("SELECT * FROM lolbans_punishments ORDER BY TimePunished DESC");
                MakeInventory("Global Punishment History");
            }
            
            // Player? No problem.
            if (args.length > 0 ) {
                OfflinePlayer target = User.FindPlayerByAny(a.get("PlayerOrPage"));
                pst = self.connection.prepareStatement("SELECT * FROM lolbans_punishments WHERE UUID = ?");
                pst.setString(1, target.getUniqueId().toString());
                MakeInventory("History of " + target.getName());
            }
            
            ResultSet result = pst.executeQuery();
            if (!result.next() || result.wasNull())
                return;
            
            List<ItemStack> items = new ArrayList<ItemStack>();
            
            while(result.next()) {
                type = PunishmentType.DisplayName(PunishmentType.FromOrdinal(result.getInt("type"))); 
                switch(result.getInt("type")) {
                    default: 
                    historyItem = new ItemStack(Material.PAPER);
                    break;
                    case 0: 
                    historyItem = new ItemStack(Material.RED_TERRACOTTA, 1);
                    break;
                    case 1: 
                    historyItem = new ItemStack(Material.ORANGE_TERRACOTTA, 1);
                    break;
                    case 2: 
                    historyItem = new ItemStack(Material.YELLOW_TERRACOTTA, 1);
                    break;
                    case 3: 
                    historyItem = new ItemStack(Material.LIME_TERRACOTTA, 1);
                    break;
                }
                
                String title = String.format("§b%s §7#%2s", result.getString("playername"), result.getString("punishid"));
                ItemMeta meta = historyItem.getItemMeta();
                
                meta.setDisplayName(title);
                
                meta.setLore(Arrays.asList(new String[]{
                    "§7Type:    §r" + type,
                    "§7Arbiter: §r" + result.getString("arbitername"), 
                    "§7Reason: §r" + result.getString("reason")}));

                historyItem.setItemMeta(meta);
                
                items.add(historyItem);
            }

            this.pages = new Paginator<ItemStack>(items, 36);
                
            } catch(SQLException e) {
                //whatever
            }
            
        }

    @Override
    public void BuildGUI(Player player, String[] args, ArgumentUtil a) {
        ClickableSlot arrowNext = new ClickableSlot(Material.ARROW, "§6Next Page", 1, 44);
        ClickableSlot arrowBack = new ClickableSlot(Material.ARROW, "§6Previous Page", 1, 36);
        ClickableSlot clearHistory = new ClickableSlot(Material.BARRIER, "§cClear History", 1, 41);
        ClickableSlot pageInfo = new ClickableSlot(Material.BOOK, "§6Page ?/?", 1, 40);

        GetResults(player, args, a);
        // Maybe tell the user that there are no results
        if(pages.GetTotalPages() < 1)
            return;

        int i = 0;
        for(ItemStack is : pages.GetPage(1)) {
            RegisterSlot(is, i);
            i++;
        }

        String pageInfoName = String.format("§ePage %d/%2d", pages.GetCurrent(), pages.GetTotalPages());
        pageInfo.SetName(pageInfoName);

        RegisterClickable(arrowBack);
        RegisterClickable(pageInfo);
        RegisterClickable(clearHistory);
        RegisterClickable(arrowNext);

        player.openInventory(getInventory());
        //player.playSound( player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.5f);
    } 

    @Override
    @EventHandler
    public void onSlotClick(InventoryClickEvent e) {
        int slot = e.getSlot();

        if(e.getInventory() != getInventory()) 
            return;

        if(IsValidSlot(slot)) {
            e.setCancelled(true);
            if(IsClickable(slot)) {
                ItemStack pageInfo = e.getInventory().getItem(40);
                ItemMeta pageInfoMeta = pageInfo.getItemMeta();

                ClickableSlot cs = GetClickable(slot);
                if(cs == null) return;

                if(cs.GetName().contains("Next")) {
                    cs.Execute(() -> {
                        int i = 0;
                        if(pages.HasNext()) {
                            int next = pages.GetNext();
                            for(ItemStack isl : pages.GetPage(next)) {
                                e.getInventory().setItem(i, isl);
                                i++;
                            }
                        }
                    });
                } else if(cs.GetName().contains("Previous")) {
                    cs.Execute(() -> {
                        int i = 0;
                        if(pages.HasPrev()) {
                            int prev = pages.GetPrev();
                            for(ItemStack isl : pages.GetPage(prev)) {
                                e.getInventory().setItem(i, isl);
                                i++;
                            }
                        }
                    });
                }
                String pageInfoName = String.format("§ePage %d/%2d", pages.GetCurrent(), pages.GetTotalPages());
                pageInfoMeta.setDisplayName(pageInfoName);
                pageInfo.setItemMeta(pageInfoMeta);
            }
        }
    }
}