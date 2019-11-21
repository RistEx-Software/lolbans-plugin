package me.zacherycoleman.lolbans.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.data.BlockData;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.BanID;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.TimeUtil;
import me.zacherycoleman.lolbans.Utils.User;

import java.sql.*;
import java.util.Arrays;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;


public class BoxCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player p = (Player)sender;
            if (p.isOnline())
            {
                SpawnBox(p);
            }
        }
        return true;
    }

}