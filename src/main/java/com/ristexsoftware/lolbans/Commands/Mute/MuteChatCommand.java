package com.ristexsoftware.lolbans.Commands.Mute;

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

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.BanID;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

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
            //DiscordUtil.SendFormatted("%s has muted the chat.", sender.getName());
        }
        else
        {
            Bukkit.broadcastMessage(Messages.GetMessages().GetConfig().getString("Mute.GlobalUnmuted"));
            //DiscordUtil.SendFormatted("%s has un-muted the chat.", sender.getName());
        }
        return true;
    }
}