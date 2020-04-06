package com.ristexsoftware.lolbans.Commands.Misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.lang.Long;
import java.util.Optional;


public class StaffRollbackCommand extends RistExCommand
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.StaffRollback", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.staffrollback"))
            return User.PermissionDenied(sender, "lolbans.staffrollback");

        // Syntax: /staffrollback [-s] <staffmember> <duration>
        if (args.length < 2)
            return false;
        
        try 
        {
            String username = args[0];
            OfflinePlayer u = User.FindPlayerByAny(username);
            Optional<Long> amount = TimeUtil.Duration(args[1]);

            if (u == null)
                return User.NoSuchPlayer(sender, username, true);

            // Parse the duration to a usable time quantity
            // TODO: Maybe say that the time type is wrong.
            if (!amount.isPresent())
                return false;

            Timestamp starttime = new Timestamp(System.currentTimeMillis() - (amount.get() * 1000L));

            // We now delete these punishments. Yes, delete.
            // If they are maliciously banning people then those bans are not valid.
            // This may become a config option though.
            PreparedStatement ps = self.connection.prepareStatement("DELETE FROM Punishments WHERE ExecutionerUUID = ? AND TimePunished >= ?");
            ps.setString(1, u.getUniqueId().toString());
            ps.setTimestamp(2, starttime);
            Future<Integer> fores = DatabaseUtil.ExecuteUpdate(ps);

            Integer ores = fores.get();
            sender.sendMessage(Messages.Translate(ores > 0 ? "StaffRollback.RollbackComplete" : "StaffRollback.NoRollback", 
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("affected", String.valueOf(ores));
                    put("player", u.getName());
                }}
            ));
            // TODO: Discord notifs, email notifs, whatever else notifs?
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        
        return true;
    }
}