package com.ristexsoftware.lolbans.Commands.Misc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.TreeMap;
import java.util.Optional;

public class ReportCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.report"))
            return User.PermissionDenied(sender, "lolbans.report");

        // /report <type> <player> <reason>
        if (args.length < 3)
            return false;

        try 
        {
            String type = args[0];
            String username = args[1];
            String reason = Messages.ConcatenateRest(args, 2).trim();
            OfflinePlayer u = User.FindPlayerByAny(username);

            if (sender instanceof ConsoleCommandSender)
                return User.PlayerOnlyVariableMessage("Report.ConsoleDisallowed", sender, "CONSOLE", true);

            if (u == null)
                return User.NoSuchPlayer(sender, username, true);

            if (!Messages.CompareMany(type, (String[])self.getConfig().getStringList("ReportSettings.Types").toArray()))
            {
                sender.sendMessage(Messages.InvalidSyntax);
                return false; // Show syntax. 
            }

            // They must have *something* in their report message.
            if (reason.isEmpty())
                return User.PlayerOnlyVariableMessage("Report.ReasonRequired", sender, username, false);

            // Make sure they're not already reported by this user.
            PreparedStatement ps = self.connection.prepareStatement("SELECT 1 FROM Reports WHERE DefendantUUID = ? AND PlaintiffUUID = ? AND NOT Closed = True");
            ps.setString(1, u.getUniqueId().toString());
            ps.setString(2, ((Player)sender).getUniqueId().toString());

            Optional<ResultSet> bnyeh = DatabaseUtil.ExecuteLater(ps).get();
            if (bnyeh.isPresent())
            {
                if (bnyeh.get().next())
                    return User.PlayerOnlyVariableMessage("Report.TooManyTries", sender, u.getName(), true);
            }

            String ReportID = PunishID.GenerateID(DatabaseUtil.GenID("Reports"));

            int i = 1;
            ps = self.connection.prepareStatement("INSERT INTO Reports (PlaintiffUUID, PlaintiffName, DefendantUUID, DefendantName, Reason, PunishID, Type) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setString(i++, ((Player)sender).getUniqueId().toString());
            ps.setString(i++, ((Player)sender).getName());
            ps.setString(i++, u.getUniqueId().toString());
            ps.setString(i++, u.getName());
            ps.setString(i++, ReportID);
            ps.setString(i++, reason);

            DatabaseUtil.ExecuteUpdate(ps);

            sender.sendMessage(Messages.Translate("Report.ReportSuccess",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", u.getName());
                    put("reason", reason);
                    put("punishid", ReportID);
                }}
            ));

            String AnnounceMessage = Messages.Translate("Report.ReportAnnouncement",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", u.getName());
                    put("reporter", sender.getName());
                    put("reason", reason);
                    put("punishid", ReportID);
                }}
            );

            self.getLogger().warning(AnnounceMessage);

            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (PermissionUtil.Check(sender, "lolbans.ReceiveReports"))
                    p.sendMessage(AnnounceMessage);
            }

            DiscordUtil.SendDiscord(sender, "reported", u, reason, ReportID, false);

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