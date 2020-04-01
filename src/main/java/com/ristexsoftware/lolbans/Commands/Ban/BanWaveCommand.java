package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Runnables.BanWaveRunnable;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;

public class BanWaveCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    private boolean BanWaveAdd(CommandSender sender, boolean silent, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.banwave.add"))
            return User.PermissionDenied(sender, "lolbans.banwave.add");

        if (args.length < 2)
        {
            sender.sendMessage(Messages.InvalidSyntax);
            return false; // Show syntax.
        }

        // Syntax: /banwave add <playername> <reason>
        try 
        {
            // TODO: Fix this too.
            // TODO: Fix expiries.
            String reason = Messages.ConcatenateRest(args, 2).trim();
            OfflinePlayer target = User.FindPlayerByAny(args[0]);

            if (target == null)
                return User.NoSuchPlayer(sender, args[0], true);

            if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
            {
                sender.sendMessage(Messages.Translate("BanWave.CannotAddSelf", null));
                return true;
            }

            if (User.IsPlayerInWave(target))
                return User.PlayerOnlyVariableMessage("BanWave.PlayerIsInBanWave", sender, target.getName(), true);

            // Get the latest ID of the banned players to generate a PunishID form it.
            String banid = PunishID.GenerateID(DatabaseUtil.GenID("BanWave"));
                
            // TODO: Cleanup this query...
            int i = 1;
            PreparedStatement pst = self.connection.prepareStatement("INSERT INTO BanWave (UUID, PlayerName, IPAddress, Reason, ExecutionerName, ExecutionerUUID, PunishID) VALUES (?, ?, ?, ?, ?, ?, ?)");
            pst.setString(i++, target.getUniqueId().toString());
            pst.setString(i++, target.getName());
            pst.setString(i++, target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : "UNKNOWN");
            pst.setString(i++, reason);
            pst.setString(i++, sender.getName());
            pst.setString(i++, sender instanceof ConsoleCommandSender ? "CONSOLE" : ((Player)sender).getUniqueId().toString());
            pst.setString(i++, banid);

            // Commit to the database.
            DatabaseUtil.ExecuteUpdate(pst);

            // Log to console.
            // TODO: Log to everyone with the alert permission?
            // FIXME: Silents?
            // Format our messages.
            Bukkit.getConsoleSender().sendMessage(Messages.Translate(silent ? "BanWave.AddedToWave" : "BanWave.AddedToWave",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("banner", sender.getName());
                    put("punishid", banid);
                    put("silent", (silent ? " [silent]" : ""));
                }}
            ));

            // Send to Discord.
            // TODO: Uhhh this is different now?
            DiscordUtil.SendBanWaveAdd(sender.getName(), target.getName(), target.getUniqueId().toString(), "f78a4d8d-d51b-4b39-98a3-230f2de0c670", reason, banid);
            return true;
        }
        catch (SQLException | InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }

    private boolean BanWaveRemove(CommandSender sender, boolean silent, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.banwave.remove"))
            return User.PermissionDenied(sender, "lolbans.banwave.remove");

        // Check and make sure the user actually exists.
        OfflinePlayer target = User.FindPlayerByAny(args[0]);
        if (target == null)
            return User.NoSuchPlayer(sender, args[0], true);

        // Check if they're banned normally
        if (!User.IsPlayerInWave(target))
            return User.PlayerOnlyVariableMessage("BanWave.PlayerNotInBanWave", sender, target.getName(), true);

        try
        {
            // God forbid any fucking thing does `return this;` so we can chain calls together like a builder. 
            PreparedStatement fuckingdumb = self.connection.prepareStatement("DELETE FROM BanWave WHERE UUID = ?");
            fuckingdumb.setString(1, target.getUniqueId().toString());
            DatabaseUtil.ExecuteUpdate(fuckingdumb);

            User.PlayerOnlyVariableMessage("BanWave.RemovedFromWave", sender, target.getName(), false);
            return true;
            
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }

    private boolean BanWaveExecute(CommandSender sender, boolean silent, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.banwave.enforce"))
            return User.PermissionDenied(sender, "lolbans.banwave.enforce");

        User.PlayerOnlyVariableMessage("BanWave.BanWaveStart", sender, sender.getName(), false);
        BanWaveRunnable bwr = new BanWaveRunnable();
        bwr.sender = sender;
        bwr.runTaskAsynchronously(self);
        return true;
    }
        
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.banwave"))
            return User.PermissionDenied(sender, "lolbans.banwave");
        
        // Invalid arguments.
        if (args.length < 1)
            return false;

        boolean silent = args[0].equalsIgnoreCase("-s");
        String SubCommand = silent ? args[1] : args[0];
        String[] Subargs = Arrays.copyOfRange(args, silent ? 2 : 1, args.length);

        // TODO: Help commands with this which explains everything
        if (SubCommand.equalsIgnoreCase("add"))
            return this.BanWaveAdd(sender, silent, command, label, Subargs);
        else if (Messages.CompareMany(SubCommand, new String[]{"remove", "rm", "delete", "del"}))
            return this.BanWaveRemove(sender, silent, command, label, Subargs);
        else if (Messages.CompareMany(SubCommand, new String[]{"enforce", "run", "start", "exec", "execute"}))
            return this.BanWaveExecute(sender, silent, command, label, Subargs);
            
        // If they run a sub command we don't know
        return false;
    }
}