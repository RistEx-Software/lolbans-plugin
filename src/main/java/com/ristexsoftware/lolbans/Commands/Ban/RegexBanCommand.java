package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.IPBanUtil;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.regex.*;
import java.lang.Long;
import java.util.Optional;
import java.util.TreeMap;

import inet.ipaddr.HostName;

public class RegexBanCommand extends RistExCommand
{
    private static Main self = Main.getPlugin(Main.class);

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
            String rDNS = IPBanUtil.rDNSQUery(hn.getHost());
            
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
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        sender.sendMessage(Messages.InvalidSyntax);
        sender.sendMessage("Usage: /regexban [-s] <Regex> <Time|*> <Reason>");
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.regexban"))
            return User.PermissionDenied(sender, "lolbans.regexban");

        if (args.length < 3 || args == null)
            return false;

        // /regexban <regex> <time> <Reason here unlimited length>
        // FIXME: How do we handle the <regex> if there's spaces in the regex?
        // Should spaces even be allowed? I don't think anything allows spaces...
        try
        {
            String reason = Messages.ConcatenateRest(args, 2);
            Pattern regex = null;
            try 
            {
                regex = Pattern.compile(args[0]);
            }
            catch (PatternSyntaxException ex)
            {
                // TODO: Maybe give more of a reason why this pattern failed?
                sender.sendMessage(Messages.InvalidSyntax);
                return false;
            }
            
            Timestamp bantime = null;
            // Parse ban time.
            if (!args[1].trim().contentEquals("0") && !args[1].trim().contentEquals("*"))
            {
                Optional<Long> dur = TimeUtil.Duration(args[1]);
                if (dur.isPresent())
                bantime = new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L);
                else
                {
                    sender.sendMessage(Messages.InvalidSyntax);
                    return false;
                }
            }

            // ItS gOtTa Be FiNaL thanks java.
            final String FuckingJava2 = new String(bantime != null ? String.format("%s (%s)", TimeUtil.TimeString(bantime), TimeUtil.Expires(bantime)) : "Never");
            final String FuckingJava3 = new String(bantime != null ? TimeUtil.Expires(bantime) : "Never");
            final String FuckingJava4 = new String(bantime != null ? TimeUtil.TimeString(bantime) : "Never");
            
            if (SanityCheck(regex, sender))
                return true;

            String banid = PunishID.GenerateID(DatabaseUtil.GenID("RegexBans"));

            int i = 1;
            PreparedStatement pst = self.connection.prepareStatement("INSERT INTO RegexBans (Regex, Reason, Executioner, PunishID, Expiry) VALUES (?, ?, ?, ?, ?)");
            pst.setString(i++, regex.pattern());
            pst.setString(i++, reason);
            pst.setString(i++, sender.getName());
            pst.setString(i++, banid);
            pst.setTimestamp(i++, bantime);
            pst.executeUpdate();

            // Add it to our pattern cache
            pst = self.connection.prepareStatement("SELECT id FROM RegexBans WHERE PunishID = ?");
            pst.setString(1, banid);
            ResultSet rst = pst.executeQuery();
            
            if (rst.next())
                Main.REGEX.put(rst.getInt("id"), regex);
            else
            {
                self.getLogger().severe("Cannot get ID for regex " + regex.pattern() + " punish id " + banid + ", this will not allow the regex to be enforced!");
                self.getLogger().severe("Please try restarting the server! If this error persists, please report it to the lolbans team.");
            }

            // Format our messages.
            // TODO: This.
            String messagenode = bantime != null ? "RegexBan.TempBanMessage" : "RegexBan.PermBanMessage";
            String ThanksJava = regex.pattern();
            String IPBanAnnouncement = Messages.Translate(messagenode,
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("regex", ThanksJava);
                    put("reason", reason);
                    put("arbiter", sender.getName());
                    put("banid", banid);
                    put("fullexpiry", FuckingJava2);
                    put("expiryduration", FuckingJava3);
                    put("dateexpiry", FuckingJava4);
                }}
            );

            // Send messages to all players (if not silent) or only to admins (if silent)
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (/*silent &&*/ (!p.hasPermission("lolbans.alerts") && !p.isOp()))
                    continue;

                p.sendMessage(IPBanAnnouncement);
            }

            // Kick players who match the ban
            for (Player player : Bukkit.getOnlinePlayers())
            {
                String rDNS = IPBanUtil.rDNSQUery(player.getAddress().getAddress().getHostAddress());
                // Matchers to make things more efficient.
                Matcher NameMatch = regex.matcher(player.getName());
                Matcher IPMatch = regex.matcher(player.getAddress().getAddress().getHostAddress());
                Matcher HostMatch = regex.matcher(rDNS);

                // If any of them match, we must query the database for the record
                // then disconnect them for matching something.
                if (NameMatch.matches() || IPMatch.matches() || HostMatch.matches())
                {
                    // FIXME: Do we use a custom message? what's this func even doing?
                    // "KickPlayer" sends the inputed strings into the function in the User class
                    // there are multiple "KickPlayer" funcs but this one is for IPBans (hence why the IP is on the end)
                    // Once the func gets the inputs, it'll kick the player with a message specified in the config
                    // FIXME: Is this message personalized for each banned player to describe what is matched?
                    User.KickPlayer(sender.getName(), player, banid, reason, bantime);
                }
                // TODO: Global announcement
            }
            
            // SendIP
            // Send to Discord. (New method)
            if (DiscordUtil.UseSimplifiedMessage == true)
            {
                // this is broken still, don't use it
                //DiscordUtil.SendFormatted(SimplifiedMessage);
            }
            else
            {
                DiscordUtil.SendDiscord(sender.getName().toString(), "Regex Banned", regex.pattern(),
                        // if they're the console, use a hard-defined UUID instead of the player's UUID.
                        (sender instanceof ConsoleCommandSender) ? "f78a4d8d-d51b-4b39-98a3-230f2de0c670" : ((Entity) sender).getUniqueId().toString(), 
                        regex.pattern(), reason, banid, bantime, false);
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