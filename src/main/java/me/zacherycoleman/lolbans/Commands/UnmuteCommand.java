package me.zacherycoleman.lolbans.Commands;

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
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.TranslationUtil;
import me.zacherycoleman.lolbans.Utils.User;
import me.zacherycoleman.lolbans.Utils.Messages;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;

public class UnmuteCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean SenderHasPerms = (sender instanceof ConsoleCommandSender || 
                                 (!(sender instanceof ConsoleCommandSender) && (((Player)sender).hasPermission("lolbans.unmute") || ((Player)sender).isOp())));
        
        if (SenderHasPerms)
        {
            try 
            {
                // just incase someone, magically has a 1 char name........
                if (!(args.length < 2 || args == null))
                {
                    String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length )) : args[1];
                    reason = reason.replace(",", "");
                    // Because dumbfuck java and it's "ItS nOt FiNaL"
                    OfflinePlayer target = User.FindPlayerByBanID(args[0]);
                    
                    if (target == null)
                        return User.NoSuchPlayer(sender, args[0], true);
                    
                    if (!User.IsPlayerMuted(target))
                        return User.PlayerOnlyVariableMessage("Mute.PlayerIsNotMuted", sender, target.getName(), true);
                    
                    // Prepare our reason for unbanning
                    boolean silent = reason.contains("-s");
                    reason = reason.replace("-s", "").trim();
                    final String FuckingJava = new String(reason);
                    
                    // Preapre a statement
                    // We need to get the latest banid first.
                    PreparedStatement pst2 = self.connection.prepareStatement("UPDATE MutedHistory INNER JOIN (SELECT MuteID AS LatestMuteID, UUID as bUUID FROM MutedPlayers WHERE UUID = ?) tm SET UnmuteReason = ?, UnmuteExecutioner = ? WHERE UUID = tm.bUUID AND MuteID = tm.LatestMuteID");
                    pst2.setString(1, target.getUniqueId().toString());
                    pst2.setString(2, reason);
                    pst2.setString(3, sender.getName());
                    pst2.executeUpdate();
                    
                    PreparedStatement pst3 = self.connection.prepareStatement("SELECT MuteID FROM MutedPlayers WHERE UUID = ?");
                    pst3.setString(1, target.getUniqueId().toString());
    
                    ResultSet result = pst3.executeQuery();
                    result.next();
                    String MuteID = result.getString("MuteID");

                    // Preapre a statement
                    PreparedStatement pst = self.connection.prepareStatement("DELETE FROM MutedPlayers WHERE UUID = ?");
                    pst.setString(1, target.getUniqueId().toString());
                    pst.executeUpdate();

                    // Log to console.
                    Bukkit.getConsoleSender().sendMessage(String.format("\u00A7c%s \u00A77has unmuted \u00A7c%s\u00A77: \u00A7c%s\u00A77%s\u00A7r", 
                    sender.getName(), target.getName(), reason, (silent ? " [silent]" : "")));

                    // Post that to the database.
                    for (Player p : Bukkit.getOnlinePlayers())
                    {
                        if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp()))
                            continue;

                        //"&c%banner% &7has banned &c%player%&7: &c%reason%"
                
                        String UnbanAnnouncementMessage = Messages.GetMessages().Translate(silent ? "Mute.SilentUnmuteAnnouncment" : "Mute.UnmuteAnnouncment",
                            new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                            {{
                                put("prefix", Messages.Prefix);
                                put("player", target.getName());
                                put("reason", FuckingJava);
                                put("unmuter", sender.getName());
                                put("muteid", MuteID);
                            }}
                        );

                        p.sendMessage(UnbanAnnouncementMessage);
                    }

                    String YouWereUnMuted = Messages.GetMessages().Translate("Mute.YouWereUnMuted",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("prefix", Messages.Prefix);
                        put("player", target.getName());
                        put("reason", FuckingJava);
                        put("unmuter", sender.getName());
                        put("muteid", MuteID);
                    }}
                    );

                    if (target instanceof Player)
                    {
                        Player target2 = (Player) target;
                        target2.sendMessage(YouWereUnMuted);
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
                        String SimplifiedMessageUnban = TranslationUtil.Translate(self.getConfig().getString(silent ? "SimplifiedMessageSilentUnban" : "SimplifiedMessageUnban"), "&",
                            new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                            {{
                                put("player", target.getName());
                                put("reason", FuckingJava);
                                put("banner", sender.getName());
                                put("banid", MuteID);
                            }}
                        );

                        DiscordUtil.SendFormatted(SimplifiedMessageUnban, sender.getName());
                        return true;
                    }
                    else
                    {
                        DiscordUtil.SendUnmute(sender.getName().toString(), target.getName(),
                                // if they're the console, use a hard-defined UUID instead of the player's UUID.
                                (sender instanceof ConsoleCommandSender) ? "f78a4d8d-d51b-4b39-98a3-230f2de0c670" : ((OfflinePlayer) sender).getUniqueId().toString(),
                                target.getUniqueId().toString(), reason, MuteID, silent);
                        return true;
                    }
                }

                else
                {
                    sender.sendMessage(Messages.InvalidSyntax);
                    return false; // Show syntax.
                }
            }
            catch (SQLException | InvalidConfigurationException e)
            {
                e.printStackTrace();
                sender.sendMessage(Messages.ServerError);
                return true;
            }
        }
        // They're denied perms, just return.
        return true;
    }
}