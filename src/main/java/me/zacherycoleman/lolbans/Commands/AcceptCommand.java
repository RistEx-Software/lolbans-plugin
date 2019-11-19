package me.zacherycoleman.lolbans.Commands;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.Configuration;

public class AcceptCommand implements CommandExecutor 
{    
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) 
    {
        if(!(sender instanceof Player))
        {
            sender.sendMessage("" + Configuration.Prefix + ChatColor.RED + "You have to be a user to these commands.");
        }
        else
        {
            Player player = (Player) sender;

            // Getting command name
            if (command.getName().equalsIgnoreCase("accept") && Main.USERS.get(player.getUniqueId()) != null && Main.USERS.get(player.getUniqueId()).IsWarn()) 
            {
                
                try
                {
                    Main.USERS.get(player.getUniqueId()).SetWarned(false);
                    // Preapre a statement
                    PreparedStatement pst3 = self.connection.prepareStatement("UPDATE Warnings SET Accepted = true WHERE UUID = ?");
                    pst3.setString(1, player.getUniqueId().toString());
                    pst3.executeUpdate();

                    // Commit to the database.
                    pst3.executeUpdate();
                    sender.sendMessage(ChatColor.GREEN + "Thank you for accepting, you may move!");
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                    sender.sendMessage(ChatColor.RED + "An internal error occured while attempting to execute this command.");
                }

            }
        }
        return false;
    }
}