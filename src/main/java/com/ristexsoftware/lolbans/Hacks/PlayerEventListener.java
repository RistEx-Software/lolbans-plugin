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

package com.ristexsoftware.lolbans.Hacks;

import java.util.TreeMap;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;

import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class PlayerEventListener implements Listener
{

    @EventHandler
    public void OnPlayerMove(PlayerMoveEvent event) 
    {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;

        PlayerMoveEvent E = (PlayerMoveEvent)event;
        Player p = E.getPlayer();
        // Put them in a box
        u.SpawnBox(false, null);
        
        Location from = E.getFrom();
        Location to = E.getTo();

        double x = Math.floor(from.getX());
        double z = Math.floor(from.getZ());
        double y = Math.floor(from.getY());

        Location loc = p.getLocation();
        loc.setY(loc.getY() - 2);

        // This will enforce their location server-side.
        if (Math.floor(to.getX()) != x || Math.floor(to.getZ()) != z || Math.floor(to.getY()) != y)
        {
            E.setCancelled(true);
            // TODO: Add {ISSUER}, {PLAYER}, and {REASON} to this message.
            try
            {
                p.sendMessage(Messages.Translate("Warn.WarnedOnAction", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{

                    }}
                ));
            }
            catch (InvalidConfigurationException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    // Don't mind this dumb stuff, can't register PlayerEvent or EntityEvent so this is the next best thing!
    @EventHandler
    public static void OnPlayerEvent2(AsyncPlayerChatEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
        u.SendMessage(u.GetWarnMessage());
    }

    @EventHandler
    public static void OnPlayerEvent3(PlayerInteractEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent4(PlayerDropItemEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
        u.SendMessage(u.GetWarnMessage());
    }

    @EventHandler
    public static void OnPlayerEvent5(PlayerItemConsumeEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent6(PlayerInteractEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent6(PlayerBedEnterEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent7(PlayerTeleportEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        if (event.getCause() != TeleportCause.PLUGIN)
            event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent8(PlayerGameModeChangeEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent9(PlayerToggleFlightEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent10(PlayerEditBookEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
        u.SendMessage(u.GetWarnMessage());
    }

    @EventHandler
    public static void OnEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity e = event.getEntity();

        // Ensure the player is not an entity of some kind. They're currently exempt.
        if (e instanceof Player)
        {
            User u = Main.USERS.get(e.getUniqueId());
        
            // Ignore players not warned
            if (u == null || !u.IsWarn())
                return;

            if (event instanceof Cancellable)
            {
                u.SendMessage(u.GetWarnMessage());
                ((Cancellable)event).setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e)
    {

        if (e.getEntity() instanceof Player)
        {
            User u = Main.USERS.get(e.getEntity().getUniqueId());
        
            // Ignore players not warned
            if (u == null || !u.IsWarn())
                return;

            if (e.getCause() == EntityDamageEvent.DamageCause.VOID || e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION ||
             e.getCause() == EntityDamageEvent.DamageCause.FALL || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || 
             e.getCause() == EntityDamageEvent.DamageCause.CONTACT || e.getCause() == EntityDamageEvent.DamageCause.DROWNING)
                e.setCancelled(true);
        }
    }
}