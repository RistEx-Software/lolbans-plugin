package me.zacherycoleman.lolbans.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.BanID;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.TimeUtil;
import me.zacherycoleman.lolbans.Utils.User;
import me.zacherycoleman.lolbans.Utils.Messages;
import me.zacherycoleman.lolbans.Utils.DatabaseUtil;
import me.zacherycoleman.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Map;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;


public class MuteChatCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.chatmute"))
            return true;
        
        // Nice toggle feature
        self.ChatMuted = !self.ChatMuted;

        if (self.ChatMuted)
        {
            Bukkit.broadcastMessage(Messages.GetMessages().GetConfig().getString("Mute.GlobalMuted"));
            DiscordUtil.SendFormatted("%s has muted the chat.", sender.getName());
        }
        else
        {
            Bukkit.broadcastMessage(Messages.GetMessages().GetConfig().getString("Mute.GlobalUnmuted"));
            DiscordUtil.SendFormatted("%s has un-muted the chat.", sender.getName());
        }
        return true;
    }
}