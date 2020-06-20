package com.ristexsoftware.lolbans.Commands.Report;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.TreeMap;
import java.util.Optional;

public class ReportCommand extends RistExCommand
{
    private Main self = (Main)this.getPlugin();

    public ReportCommand(Plugin owner)
    {
        super("report", owner);
        this.setDescription("Report a user to server administration");
        this.setPermission("lolbans.report");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Report", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.report"))
            return User.PermissionDenied(sender, "lolbans.report");

        // /report <type> <player> <reason>
        try 
        {
            ArgumentUtil a = new ArgumentUtil(args);
            // a.RequiredString("Type", 0);
            a.RequiredString("PlayerName", 0);
            a.RequiredString("Reason", 1);

            if (!a.IsValid())
                return false;

            String type = "General";
            String username = a.get("PlayerName");
            String reason = a.get("Reason");
            OfflinePlayer u = User.FindPlayerByAny(username);

            if (sender instanceof ConsoleCommandSender)
                return User.PlayerOnlyVariableMessage("UserRequired", sender, "CONSOLE", true);

            if (u == null)
                return User.NoSuchPlayer(sender, username, true);

/*             if (!Messages.CompareMany(type, self.getConfig().getStringList("ReportSettings.Types").toArray()))
                return false; // Show syntax. 
 */
            // They must have *something* in their report message.
            if (reason.isEmpty())
                return User.PlayerOnlyVariableMessage("Report.ReasonRequired", sender, username, false);

            // Make sure they're not already reported by this user.
            PreparedStatement ps = self.connection.prepareStatement("SELECT 1 FROM lolbans_reports WHERE DefendantUUID = ? AND PlaintiffUUID = ? AND NOT Closed = True");
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
            ps = self.connection.prepareStatement("INSERT INTO lolbans_reports (PlaintiffUUID, PlaintiffName, DefendantUUID, DefendantName, Reason, PunishID, Type) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setString(i++, ((Player)sender).getUniqueId().toString());
            ps.setString(i++, ((Player)sender).getName());
            ps.setString(i++, u.getUniqueId().toString());
            ps.setString(i++, u.getName());
            ps.setString(i++, reason);
            ps.setString(i++, ReportID);
            ps.setString(i++, "General");

            DatabaseUtil.ExecuteUpdate(ps);
            
            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", u.getName());
                put("arbiter", sender.getName());
                put("reason", reason);
                put("punishid", ReportID);
                put("type", type);
            }};

            sender.sendMessage(Messages.Translate("Report.ReportSuccess", Variables));
                
            BroadcastUtil.BroadcastEvent(false, Messages.Translate("Report.ReportAnnouncement", Variables), "lolbans.ReceiveReports");
            if (Messages.Discord)
                DiscordUtil.GetDiscord().SendReport(sender, u, reason, ReportID, type);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        
        return true;
    }
}