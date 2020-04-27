package com.ristexsoftware.lolbans.Commands.Misc;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
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
    private Main self = (Main)this.getPlugin();

    public StaffRollbackCommand(Plugin owner)
    {
        super("staffrollback", owner);
        this.setDescription("Rollback a staff member's actions");
        this.setPermission("lolbans.staffrollback");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
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
    public boolean Execute(CommandSender sender, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.staffrollback"))
            return User.PermissionDenied(sender, "lolbans.staffrollback");

        // Syntax: /staffrollback [-s] <staffmember> <duration>
        
        try 
        {
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.RequiredString("PlayerName", 0);
            a.RequiredString("Time", 0);

            if (!a.IsValid())
                return false;

            boolean silent = a.get("-s") != null;
            String username = a.get("PlayerName");
            OfflinePlayer u = User.FindPlayerByAny(username);
            Optional<Long> amount = TimeUtil.Duration(a.get("Time"));

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
            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("affected", String.valueOf(ores));
                put("player", u.getName());
            }};
            
            sender.sendMessage(Messages.Translate(ores > 0 ? "StaffRollback.RollbackComplete" : "StaffRollback.NoRollback", Variables ));
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("StaffRollback.Announcement", Variables));
            // TODO: Discord
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        
        return true;
    }
}