package me.zacherycoleman.lolbans.Listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.User;

public class PlayerEventListener implements Listener
{
    public void SpawnBox(Player target)
    {
        Location loc = target.getLocation();
        // Create a barrier block.
        BlockData BlockType = Material.BARRIER.createBlockData();
        //BlockData BlockType = Material.GLASS.createBlockData();

        // We must first ensure the player is actually in a safe space to
        // lock them down on. We do this by finding the ground, then teleporting
        // them both to the ground and the center of the block.
        // If we don't find the ground and teleport them there, then the player is
        // considered to be "flying" and can be kicked as such.
        Location TeleportLoc = target.getWorld().getHighestBlockAt(loc.getBlockX(), loc.getBlockZ()).getLocation();
        // Preserve the player's pitch/yaw values
        TeleportLoc.setPitch(loc.getPitch());
        TeleportLoc.setYaw(loc.getYaw());
        // Add to get to center of the block
        TeleportLoc.add(0.5, 0, 0.5);
        // Teleport.
        target.teleport(TeleportLoc);

        /*
        // The blocks we need to spawn is as below. We need to spawn 2 blocks in the 
              | X |
          | X | P | X |
              | X |

              Location.add(x, y, z)

              Y - Up
              X - strafe
              Z - Walk
        */

        // Set the block under them
        target.sendBlockChange(TeleportLoc.subtract(0, 1, 0), BlockType);
        // set the block above them
        target.sendBlockChange(TeleportLoc.add(0, 3, 0), BlockType);

        // Reset our TeleportLoc.
        TeleportLoc.subtract(0, 2, 0);

        // Now set the blocks to all sides of them.
        target.sendBlockChange(TeleportLoc.add(1, 0, 0), BlockType);
        target.sendBlockChange(TeleportLoc.add(0, 1, 0), BlockType);

        target.sendBlockChange(TeleportLoc.subtract(2, 0, 0), BlockType);
        target.sendBlockChange(TeleportLoc.subtract(0, 1, 0), BlockType);

        target.sendBlockChange(TeleportLoc.add(1, 0, 1), BlockType);
        target.sendBlockChange(TeleportLoc.add(0, 1, 0), BlockType);

        target.sendBlockChange(TeleportLoc.subtract(0, 0, 2), BlockType);
        target.sendBlockChange(TeleportLoc.subtract(0, 1, 0), BlockType);
    }

    public void OnPlayerEvent(PlayerEvent event)
    {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        // Ignore players not warned
        if (!u.IsWarn())
            return;

        if (event instanceof PlayerMoveEvent)
        {
            PlayerMoveEvent E = (PlayerMoveEvent)event;
            Player p = E.getPlayer();

            // Put them in a box
            SpawnBox(p);
            
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
                p.sendMessage(Configuration.Prefix + "§cYou have been warned, you may not move!\n" + Configuration.Prefix + " Please acknowledge that you've been warned by typing /accept.");
            }
            // Exit
            return;
        }


        // If they're trying to do literally anything, cancel it.
        if (event instanceof Cancellable)
        {
            Cancellable c = (Cancellable)event;
            c.setCancelled(true);
        }
    }
}