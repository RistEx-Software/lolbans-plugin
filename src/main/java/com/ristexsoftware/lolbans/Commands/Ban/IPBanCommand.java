package com.ristexsoftware.lolbans.Commands.Ban;

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

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Hacks.IPBanning.IPBanUtil;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.TranslationUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.Arrays;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Future;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;

// FIXME: Hostname-based bans?

public class IPBanCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    /**
     * Check to make sure that the address is sane.
     * This prevents you from doing insane bans (like 0.0.0.0/0)
     * which is configurable in the config file.
     * 
     * Returns true if it is an insane (over-reaching) ban.
     */
    private boolean SanityCheck(IPAddress bnyeh, CommandSender sender)
    {
        // Checking IP masks for sanity has been disabled by the config.
        if (self.getConfig().getBoolean("BanSettings.insane.ipmasks"))
            return false;

        double sanepercent = self.getConfig().getDouble("BanSettings.insane.trigger");

        // Get the total number of affected players.
        int affected = 0;
        int TotalOnline = Bukkit.getOnlinePlayers().size();
        for (Player player : Bukkit.getOnlinePlayers())
        {
            HostName hn = new HostName(player.getAddress());
            if (bnyeh.contains(hn.asAddress()))
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

            // Because Java requries effective finality, we have to redeclare shit.

            // Format our messages.
            try {
                final int fuckingfinal = affected;
                String Insanity = Messages.Translate("IPBan.Insanity",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("ipaddress", String.valueOf(bnyeh));
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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.ipban"))
            return true;

        // /ipban <ip address>[/<cidr>] <time> <Reason here unlimited length>
        try
        {
            if (args.length < 3 || args == null)
            {
                sender.sendMessage(Messages.InvalidSyntax);
                return false;
            }

            boolean silent = args.length > 3 ? args[2].equalsIgnoreCase("-s") : false;
            String reason = Messages.ConcatenateRest(args, silent ? 3 : 2);
            
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
            
            // Is a future, needed != null for some reason.
            IPAddress thingy = new IPAddressString(args[0]).toAddress();
            // TODO: handle this better? Send the banned subnet string instead of the address they tried to ban?
            Optional<ResultSet> res = IPBanUtil.IsBanned(thingy.toInetAddress()).get();
            if (res.isPresent() && res.get().next())
                return User.PlayerOnlyVariableMessage("IPBan.IPIsBanned", sender, thingy.toString(), true);

            if (SanityCheck(thingy, sender))
                return true;

            String banid = PunishID.GenerateID(DatabaseUtil.GenID("IPBans"));

            int i = 1;
            PreparedStatement pst = self.connection.prepareStatement("INSERT INTO IPBans (IPAddress, Reason, Executioner, PunishID, Expiry) VALUES (?, ?, ?, ?, ?)");
            pst.setString(i++, thingy.toString());
            pst.setString(i++, reason);
            pst.setString(i++, sender.getName());
            pst.setString(i++, banid);
            pst.setTimestamp(i++, bantime);
            pst.executeUpdate();

            // Format our messages.
            // FIXME: Is this even the right message?
            String messagenode = silent ? (bantime != null ? "IPBan.SilentTempIPBanMessage" : "IPBan.SilentPermIPBanMessage") : (bantime != null ? "IPBan.TempIPBanMessage" : "IPBan.PermIPBanMessage");
            String IPBanAnnouncement = Messages.Translate(messagenode,
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", thingy.toString());
                    put("reason", reason);
                    put("banner", sender.getName());
                    put("banid", banid);
                    put("fullexpiry", FuckingJava2);
                    put("expiryduration", FuckingJava3);
                    put("dateexpiry", FuckingJava4);
                }}
            );

            // Send messages to all players (if not silent) or only to admins (if silent)
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp()))
                    continue;

                p.sendMessage(IPBanAnnouncement);
            }

            // Kick players who match the ban
            for (Player player : Bukkit.getOnlinePlayers())
            {
                HostName hn = new HostName(player.getAddress());

                // FIXME: Do we use a custom message? what's this func even doing?
                // "KickPlayer" sends the inputed strings into the function in the User class
                // there are multiple "KickPlayer" funcs but this one is for IPBans (hence why the IP is on the end)
                // Once the func gets the inputs, it'll kick the player with a message specified in the config
                if (thingy.contains(hn.asAddress()))
                    User.KickPlayer(sender.getName(), player, banid, reason, bantime, thingy);
            }
            
            // TODO: Global announcement

            // SendIP
            // Send to Discord. (New method)
            if (DiscordUtil.UseSimplifiedMessage == true)
            {
                // this is broken still, don't use it
                //DiscordUtil.SendFormatted(SimplifiedMessage);
            }
            else
            {
                DiscordUtil.SendDiscord(sender.getName().toString(), "IPBanned", thingy.toString(),
                        // if they're the console, use a hard-defined UUID instead of the player's UUID.
                        (sender instanceof ConsoleCommandSender) ? "f78a4d8d-d51b-4b39-98a3-230f2de0c670" : ((Entity) sender).getUniqueId().toString(), 
                        thingy.toString(), reason, banid, bantime, silent);
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