package me.zacherycoleman.lolbans.Commands.Misc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.BanID;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.DatabaseUtil;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.TimeUtil;
import me.zacherycoleman.lolbans.Utils.User;
import me.zacherycoleman.lolbans.Utils.Messages;
import me.zacherycoleman.lolbans.Utils.TranslationUtil;
import me.zacherycoleman.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Map;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;


public class ReportCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.report"))
            return true;

        try 
        {
            // /report <player> <reason>
            if (args.length > 2)
            {
                String username = args[1];
                String reason = Messages.ConcatenateRest(args, 2).trim();

                // They must have *something* in their report message.
                if (reason.isEmpty())
                {
                    sender.sendMessage(Messages.GetMessages().Translate("Report.ReasonRequired",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("prefix", Messages.Prefix);
                            put("player", username);
                        }}
                    ));
                    // SHow the syntax.
                    return false;
                }

                OfflinePlayer u = User.FindPlayerByBanID(username);
                if (u == null)
                    return User.NoSuchPlayer(sender, username, true);

                try
                {
                    int i = 1;
                    PreparedStatement ps = self.connection.prepareStatement("INSERT INTO Reports (PlaintiffUUID, PlaintiffName, DefendantUUID, DefendantName, Reason) VALUES (?, ?, ?, ?)");
                    ps.setString(i++, sender instanceof ConsoleCommandSender ? "console" : ((Player)sender).getUniqueId().toString());
                    ps.setString(i++, sender instanceof ConsoleCommandSender ? "console" : ((Player)sender).getName());
                    ps.setString(i++, u.getUniqueId().toString());
                    ps.setString(i++, u.getName());
                    ps.setString(i++, reason);

                    DatabaseUtil.ExecuteLater(ps);

                    sender.sendMessage(Messages.GetMessages().Translate("Report.ReportSuccess",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("prefix", Messages.Prefix);
                            put("player", u.getName());
                            put("reason", reason);
                        }}
                    ));

                    // TODO: Discord notifs, email notifs, whatever else notifs?

                    return true;
                }
                catch (SQLException ex)
                {
                    ex.printStackTrace();
                    sender.sendMessage(Messages.ServerError);
                    return true;
                }
            }
            else
            {
                sender.sendMessage(Messages.InvalidSyntax);
                return false; // Show syntax.
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        
        return true;
    }
}