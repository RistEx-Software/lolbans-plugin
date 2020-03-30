package com.ristexsoftware.lolbans.Commands.Misc;

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
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.TranslationUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.Map;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;


public class StaffRollbackCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.report"))
            return true;

        // Syntax: /staffrollback <staffmember> <duration>

        if (args.length < 2)
            return false;
        
        try 
        {
            String username = args[0];

            OfflinePlayer u = User.FindPlayerByAny(username);
            if (u == null)
                return User.NoSuchPlayer(sender, username, true);

            // Parse the duration to a usable time quantity
            Optional<Long> amount = TimeUtil.Duration(args[1]);

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
            if (ores > 0)
            {
                sender.sendMessage(Messages.Translate("StaffRollback.RollbackComplete", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("affected", String.valueOf(ores));
                        put("player", u.getName());
                    }}
                ));
                // TODO: Discord notifs, email notifs, whatever else notifs?
            }
            else
            {
                // ¯\_(ツ)_/¯
                sender.sendMessage(Messages.Translate("StaffRollback.NoRollback", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", u.getName());
                    }}
                ));
            }

            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        
        return true;
    }
}