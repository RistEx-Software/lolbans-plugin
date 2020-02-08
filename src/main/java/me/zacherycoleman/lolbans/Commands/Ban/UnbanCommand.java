package me.zacherycoleman.lolbans.Commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.DatabaseUtil;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.TranslationUtil;
import me.zacherycoleman.lolbans.Utils.User;
import me.zacherycoleman.lolbans.Utils.Messages;
import me.zacherycoleman.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class UnbanCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.unban"))
            return true;
        
        try 
        {
            // just incase someone, magically has a 1 char name........
            if (!(args.length < 2 || args == null))
            {
                String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length )) : args[1];
                reason = reason.replace(",", "").trim();
                // Because dumbfuck java and it's "ItS nOt FiNaL"
                OfflinePlayer target = User.FindPlayerByBanID(args[0]);
                String euuid = null;

                if (sender instanceof ConsoleCommandSender)
                    euuid = "console";

                else if (sender instanceof Player)
                    euuid = ((Player) sender).getUniqueId().toString();
                
                if (target == null)
                    return User.NoSuchPlayer(sender, args[0], true);
                
                if (!User.IsPlayerBanned(target))
                    return User.PlayerOnlyVariableMessage("Ban.PlayerIsNotBanned", sender, target.getName(), true);
                
                // Prepare our reason for unbanning
                boolean silent = false;
                if (args.length > 2)
                    silent = args[1].equalsIgnoreCase("-s");

                final String FuckingJava = new String(reason);

                // Preapre a statement
                // We need to get the latest banid first.
                PreparedStatement pst3 = self.connection.prepareStatement("SELECT PunishID FROM BannedPlayers WHERE UUID = ?");
                pst3.setString(1, target.getUniqueId().toString());

                ResultSet result = pst3.executeQuery();
                result.next();
                String BanID = result.getString("PunishID");

                // Run the async task for the database
                Future<Boolean> UnBan = DatabaseUtil.UnBan(target.getUniqueId().toString(), target.getName(), reason, sender, euuid);

                // InsertBan(String UUID, String PlayerName, String Reason, String Executioner, String BanID, Timestamp BanTime)
                if (!UnBan.get())
                {
                    sender.sendMessage(Messages.ServerError);
                    return true;
                }

                // Log to console.
                Bukkit.getConsoleSender().sendMessage(String.format("\u00A7c%s \u00A77has unbanned \u00A7c%s\u00A77: \u00A7c%s\u00A77%s\u00A7r", 
                sender.getName(), target.getName(), reason, (silent ? " [silent]" : "")));

                // Post that to the database.
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp()))
                        continue;

                    //"&c%banner% &7has banned &c%player%&7: &c%reason%"
            
                    String UnbanAnnouncementMessage = Messages.GetMessages().Translate(silent ? "Ban.SilentUnbanAnnouncment" : "Ban.UnbanAnnouncment",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("player", target.getName());
                            put("reason", FuckingJava);
                            put("banner", sender.getName());
                            put("banid", BanID);
                        }}
                    );

                    p.sendMessage(UnbanAnnouncementMessage);
                }

                /* 
                // Send to Discord.
                if (sender instanceof ConsoleCommandSender)
                    DiscordUtil.SendUnban(sender.getName().toString(), target.getName(), "f78a4d8d-d51b-4b39-98a3-230f2de0c670", target.getUniqueId().toString(), reason, BanID, silent);
                else
                    DiscordUtil.SendUnban(sender.getName().toString(), target.getName(), ((OfflinePlayer) sender).getUniqueId().toString(), target.getUniqueId().toString(), reason, BanID, silent);
                */ 

                // ":hammer: **{BANNER}** un-banned **{PLAYER}** for **{REASON}** *[SILENT] {BANID}*"
                // Send to Discord. (New method)
                //String SimplifiedMessageSilentUnban = DiscordUtil.SimplifiedMessageSilentUnban;
                //String SimplifiedMessageUnban = DiscordUtil.SimplifiedMessageUnban;

                if (DiscordUtil.UseSimplifiedMessage == true)
                {
                    
                    String SimplifiedMessageUnban = Messages.GetMessages().Translate(silent ? "Discord.SimpMessageSilentUnban" : "Discord.SimpMessageUnban",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("player", target.getName());
                            put("reason", FuckingJava);
                            put("banner", sender.getName());
                            put("banid", BanID);
                        }}
                    );

                    DiscordUtil.SendFormatted(SimplifiedMessageUnban);
                    return true;
                }
                else
                {
                    DiscordUtil.SendUnban(sender.getName().toString(), target.getName(),
                            // if they're the console, use a hard-defined UUID instead of the player's UUID.
                            (sender instanceof ConsoleCommandSender) ? "f78a4d8d-d51b-4b39-98a3-230f2de0c670" : ((OfflinePlayer) sender).getUniqueId().toString(),
                            target.getUniqueId().toString(), reason, BanID, silent);
                    return true;
                }
            }

            else
            {
                sender.sendMessage(Messages.InvalidSyntax);
                return false; // Show syntax.
            }
        }
        catch (SQLException | InvalidConfigurationException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }
}