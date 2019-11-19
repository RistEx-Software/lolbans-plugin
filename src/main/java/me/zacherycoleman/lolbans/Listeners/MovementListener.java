package me.zacherycoleman.lolbans.Listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.User;

public class MovementListener implements Listener
{

    @EventHandler
    public void OnPlayerMove(PlayerMoveEvent E)
    {
        Player p = E.getPlayer();
        User u = Main.USERS.get(p.getUniqueId());
        if (u.IsWarn())
        {
            Location from = E.getFrom();
            Location to = E.getTo();

            double x = Math.floor(from.getX());
            double z = Math.floor(from.getZ());
            double y = Math.floor(from.getY());

            Location loc = p.getPlayer().getLocation();
            loc.setY(loc.getY() - 2);

            if (Math.floor(to.getX()) != x || Math.floor(to.getZ()) != z || Math.floor(to.getY()) != y)
            {
                E.setCancelled(true);
                p.sendMessage(Configuration.Prefix + "§cYou have been warned, you may not move!\n" + Configuration.Prefix + " Please acknowledge that you've been warned by typing /accept.");

            }
            if (E.getTo().getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.AIR))
            {
                p.teleport(p.getPlayer().getWorld().getHighestBlockAt(p.getPlayer().getLocation().getBlockX(), p.getPlayer().getLocation().getBlockZ()).getLocation());
            }
        }
    }
}