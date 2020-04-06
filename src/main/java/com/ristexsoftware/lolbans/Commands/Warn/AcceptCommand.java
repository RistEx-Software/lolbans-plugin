package com.ristexsoftware.lolbans.Commands.Warn;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

public class AcceptCommand extends RistExCommand
{    
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        sender.sendMessage(Messages.InvalidSyntax);
        sender.sendMessage("Usage: /warnaccept");
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args) 
    {
        if (!PermissionUtil.Check(sender, "lolbans.warnaccept"))
            return User.PermissionDenied(sender, "lolbans.warnaccept");

        // /accept [<PlayerName>]
        if(!(sender instanceof Player))
            return User.PlayerOnlyVariableMessage("UserRequired", sender, sender.getName(), true);

        User u = Main.USERS.get(((Player)sender).getUniqueId());
        if (u == null)
        {
            sender.sendMessage("Congratulations!!! YOU FOUND A BUG! One cookie for you! Please report this bug to the lolbans team!");
            return true;
        }

        // Getting command name
        if (u.IsWarn()) 
        {
            try
            {
                // Unset them warned locally
                u.SetWarned(false, null, null);
                // Preapre a statement
                PreparedStatement pst3 = self.connection.prepareStatement("UPDATE Punishments SET WarningAck = true WHERE UUID = ? AND Type = 3");
                pst3.setString(1, u.getPlayer().getUniqueId().toString());
                DatabaseUtil.ExecuteUpdate(pst3);

                User.PlayerOnlyVariableMessage("Warn.AcceptMessage", sender, sender.getName(), false);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                sender.sendMessage(Messages.ServerError);
            }
        }

        return true;
    }
}