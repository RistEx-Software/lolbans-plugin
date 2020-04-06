package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

public class MuteChatCommand extends RistExCommand
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        sender.sendMessage(Messages.InvalidSyntax);
        sender.sendMessage("Usage: /chatmute");
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.chatmute"))
            return User.PermissionDenied(sender, "lolbans.staffrollback");
        
        // Nice toggle feature
        self.ChatMuted = !self.ChatMuted;

        if (self.ChatMuted)
            Bukkit.broadcastMessage(Messages.GetMessages().GetConfig().getString("Mute.GlobalMuted"));
        else
            Bukkit.broadcastMessage(Messages.GetMessages().GetConfig().getString("Mute.GlobalUnmuted"));

        return true;
    }
}