package com.ristexsoftware.lolbans.Commands.Warn;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;

public class AcceptCommand implements CommandExecutor 
{    
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) 
    {
        if(!(sender instanceof Player))
            return User.PlayerOnlyVariableMessage("UserRequired", sender, sender.getName(), true);
        else
        {
            Player player = (Player) sender;

            // Getting command name
            if (Main.USERS.get(player.getUniqueId()) != null && Main.USERS.get(player.getUniqueId()).IsWarn()) 
            {
                try
                {
                    User u = Main.USERS.get(player.getUniqueId());
                    // Unset them warned locally
                    u.SetWarned(false, null, null);
                    // Preapre a statement
                    PreparedStatement pst3 = self.connection.prepareStatement("UPDATE Punishments SET WarningAck = true WHERE UUID = ? AND Type = 3");
                    pst3.setString(1, player.getUniqueId().toString());
                    DatabaseUtil.ExecuteUpdate(pst3);

                    User.PlayerOnlyVariableMessage("Warn.AcceptMessage", sender, sender.getName(), false);
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                    sender.sendMessage(Messages.ServerError);
                }
            }
        }
        return false;
    }
}