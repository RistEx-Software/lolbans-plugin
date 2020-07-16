/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *   
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *   
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *   
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 */

package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.IPUtil;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.regex.*;
import java.util.TreeMap;

import inet.ipaddr.HostName;

public class RegexBanCommand extends RistExCommandAsync
{
    private Main self = (Main) this.getPlugin();
    private boolean IsConsoleExectioner = false;
    
    public RegexBanCommand(Plugin owner)
    {
        super("regexban", owner);
        this.setDescription("Ban a username/hostname/ip address based on a Regular Expression");
        this.setPermission("lolbans.regexban");
    }

    /**
     * Check to make sure that the address is sane.
     * This prevents you from doing insane bans (like 0.0.0.0/0)
     * which is configurable in the config file.
     * 
     * Returns true if it is an insane (over-reaching) ban.
     */
    private boolean SanityCheck(Pattern bnyeh, CommandSender sender)
    {
        // Checking IP masks for sanity has been disabled by the config.
        if (self.getConfig().getBoolean("BanSettings.insane.regex"))
            return false;

        double sanepercent = self.getConfig().getDouble("BanSettings.insane.trigger");

        // Get the total number of affected players.
        int affected = 0;
        int TotalOnline = Bukkit.getOnlinePlayers().size();
        for (Player player : Bukkit.getOnlinePlayers())
        {
            HostName hn = new HostName(player.getAddress());
            self.getLogger().info(hn.getHost());
            String rDNS = IPUtil.rDNSQUery(hn.getHost());
            
            // Check their IP address
            if (bnyeh.matcher(hn.getHost()).matches())
                affected++;
            // Check their rDNS hostname
            else if (bnyeh.matcher(rDNS).matches())
                affected++;
            // Finally, check their player display name
            else if (bnyeh.matcher(player.getName()).matches())
                affected++;
        }

        // calculate percentage
        double percentage = affected == 0 ? 0 : (affected / TotalOnline) * 100.0;

        // TODO: Add confirmation command for people with override.
        // They have an override permission, let them execute
        if (percentage >= sanepercent)
        {
            // if the sender can override.
            if (sender.hasPermission("lolbans.insanityoverride"))
                return false;

            // Format our messages.
            try 
            {
                final int fuckingfinal = affected;
                String Insanity = Messages.Translate("RegexBan.Insanity",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("regex", bnyeh.pattern());
                        put("arbiter", sender.getName());
                        put("AFFECTEDPLAYERS", String.valueOf(fuckingfinal));
                        put("TOTALPLAYERS", String.valueOf(TotalOnline));
                        put("INSANEPERCENT", String.valueOf(percentage));
                        put("INSANETHRESHOLD", String.valueOf(sanepercent));
                    }}
                );
                sender.sendMessage(Insanity);
            }
            catch (InvalidConfigurationException e)
            {
                e.printStackTrace();
                sender.sendMessage(Messages.ServerError);
            }

            // They breach the insanity limit and are not allowed to continue.
            return true;
        }

        // They're not insane (according to above logic... lol)
        return false;
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.RegexBan", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.regexban"))
            return User.PermissionDenied(sender, "lolbans.regexban");

        // /regexban [-s] <regex> <time> <Reason here unlimited length>
        try
        {   
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.RequiredString("Regex", 0); 
            a.RequiredString("Time", 1);
            a.RequiredSentence("Reason", 2);

            if (!a.IsValid())
                return false;

            boolean silent = a.get("Silent") != null;
            String reason = a.get("Reason");
            Timestamp bantime = TimeUtil.ParseToTimestamp(a.get("Time"));
            Pattern regex = null;
            try 
            {
                regex = Pattern.compile(a.get("Regex"));
            }
            catch (PatternSyntaxException ex)
            {
                // TODO: Maybe give more of a reason why this pattern failed?
                sender.sendMessage(Messages.InvalidSyntax);
                return false;
            }
            
            if (SanityCheck(regex, sender))
                return true;

            String banid = PunishID.GenerateID(DatabaseUtil.GenID("lolbans_regexbans"));

            if (sender instanceof ConsoleCommandSender)
                IsConsoleExectioner = true;

            int i = 1;
            PreparedStatement pst = self.connection.prepareStatement("INSERT INTO lolbans_regexbans (Regex, Reason, ArbiterName, ArbiterUUID, PunishID, Expiry) VALUES (?, ?, ?, ?, ?, ?)");
            pst.setString(i++, regex.pattern());
            pst.setString(i++, reason);
            pst.setString(i++, sender.getName());
            pst.setString(i++, IsConsoleExectioner ? "CONSOLE" : ((Player) sender).getUniqueId().toString());
            pst.setString(i++, banid);
            pst.setTimestamp(i++, bantime);
            pst.executeUpdate();

            // Add it to our pattern cache
            pst = self.connection.prepareStatement("SELECT id FROM lolbans_regexbans WHERE PunishID = ?");
            pst.setString(1, banid);
            ResultSet rst = pst.executeQuery();
            
            if (rst.next())
                Main.REGEX.put(rst.getInt("id"), regex);
            else
            {
                self.getLogger().severe("Cannot get ID for regex " + regex.pattern() + " punish id " + banid + ", this will not allow the regex to be enforced!");
                self.getLogger().severe("Please try restarting the server! If this error persists, please report it to the lolbans team.");
                return true;
            }

            String ThanksJava = regex.pattern();
            // Send messages to all players (if not silent) or only to admins (if silent)
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("RegexBan.BanAnnouncement",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("regex", ThanksJava);
                    put("reason", reason);
                    put("arbiter", sender.getName());
                    put("punishid", banid);
                    put("expiry",  bantime == null ? "" : bantime.toString());
                    put("silent", String.valueOf(silent));
                }}
            ));
            // TODO: Make regex ban object for discord.
            //DiscordUtil.GetDiscord().SendBanObject(sender, regex.toString(), reason, banid, bantime);

            // Kick players who match the ban
            for (Player player : Bukkit.getOnlinePlayers())
            {
                String rDNS = IPUtil.rDNSQUery(player.getAddress().getAddress().getHostAddress());
                // Matchers to make things more efficient.
                Matcher NameMatch = regex.matcher(player.getName());
                Matcher IPMatch = regex.matcher(player.getAddress().getAddress().getHostAddress());
                Matcher HostMatch = regex.matcher(rDNS);

                // If any of them match, we must query the database for the record
                // then disconnect them for matching something.
                if (NameMatch.find() || IPMatch.find() || HostMatch.find())
                {
                    Bukkit.getScheduler().runTaskLater(self, () -> User.KickPlayerBan(sender.getName(), player, banid, reason, TimeUtil.TimestampNow(), bantime), 1L);
                }
            }
                
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
        
    }
}