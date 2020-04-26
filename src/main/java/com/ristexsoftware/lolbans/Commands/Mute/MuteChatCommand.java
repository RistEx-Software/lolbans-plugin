package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

import java.util.TreeMap;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

public class MuteChatCommand extends RistExCommand
{
    private Main self = (Main)this.getPlugin();

    public MuteChatCommand(Plugin owner)
    {
        super("mutechat", owner);
        this.setDescription("Mute the chat for all players (toggleable)");
        this.setPermission("lolbans.mutechat");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.ChatMute", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.chatmute"))
            return User.PermissionDenied(sender, "lolbans.chatmute");
        
        // Nice toggle feature
        self.ChatMuted = !self.ChatMuted;

        try 
        {
            if (self.ChatMuted)
                Bukkit.broadcastMessage(Messages.Translate("Mute.GlobalMuted", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
            else
                Bukkit.broadcastMessage(Messages.Translate("Mute.GlobalUnmuted", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
            // TODO: Discord/BroadcastOps
        }
        catch (InvalidConfigurationException ex)
        {
            ex.printStackTrace();
        }

        return true;
    }
}