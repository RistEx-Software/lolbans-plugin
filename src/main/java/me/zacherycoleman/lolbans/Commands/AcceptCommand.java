package me.zacherycoleman.lolbans.Commands;

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

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.User;
import me.zacherycoleman.lolbans.Utils.Messages;

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
                    PreparedStatement pst3 = self.connection.prepareStatement("UPDATE Warnings SET Accepted = true WHERE UUID = ?");
                    pst3.setString(1, player.getUniqueId().toString());
                    pst3.executeUpdate();

                    // Commit to the database.
                    pst3.executeUpdate();
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