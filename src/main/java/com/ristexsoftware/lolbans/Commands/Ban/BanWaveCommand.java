package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Runnables.BanWaveRunnable;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;

public class BanWaveCommand extends RistExCommand
{
    private Main self = (Main)this.getPlugin();

    public BanWaveCommand(Plugin owner)
    {
        super("banwave", owner);
        this.setDescription("Manage ban waves");
        this.setPermission("lolbans.banwave");
    }

    private boolean BanWaveAdd(CommandSender sender, boolean silent, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.banwave.add"))
            return User.PermissionDenied(sender, "lolbans.banwave.add");

        // Syntax: /banwave add <playername> <Time|*> <reason>
        try 
        {
            ArgumentUtil a = new ArgumentUtil(args);
            a.RequiredString("PlayerName", 0);
            a.RequiredString("Time", 1);
            a.RequiredSentence("reason", 2);

            if (!a.IsValid())
                return false;

            Timestamp Expiry = TimeUtil.ParseToTimestamp(a.get("Time"));
            String reason = a.get("reason");
            OfflinePlayer target = User.FindPlayerByAny(a.get("PlayerName"));

            if (target == null)
                return User.NoSuchPlayer(sender, a.get("PlayerName"), true);

            if (User.IsPlayerInWave(target))
                return User.PlayerOnlyVariableMessage("BanWave.PlayerIsInBanWave", sender, target.getName(), true);

            // Get the latest ID of the banned players to generate a PunishID form it.
            String banid = PunishID.GenerateID(DatabaseUtil.GenID("BanWave"));
                
            int i = 1;
            PreparedStatement pst = self.connection.prepareStatement("INSERT INTO BanWave (UUID, PlayerName, IPAddress, Reason, ArbiterName, ArbiterUUID, PunishID, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            pst.setString(i++, target.getUniqueId().toString());
            pst.setString(i++, target.getName());
            pst.setString(i++, target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : "UNKNOWN");
            pst.setString(i++, reason);
            pst.setString(i++, sender.getName());
            pst.setString(i++, sender instanceof ConsoleCommandSender ? "CONSOLE" : ((Player)sender).getUniqueId().toString());
            pst.setString(i++, banid);
            pst.setTimestamp(i++, Expiry);

            // Commit to the database.
            DatabaseUtil.ExecuteUpdate(pst);

            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", target.getName());
                put("reason", reason);
                put("arbiter", sender.getName());
                put("punishid", banid);
                put("expiry", Expiry.toString());
                put("silent", Boolean.toString(silent));
            }};

            sender.sendMessage(Messages.Translate("BanWave.AddedToWave", Variables));
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("BanWave.AddedToWaveAnnouncement", Variables));
            if (Messages.Discord)
                DiscordUtil.GetDiscord().SendBanWaveAdd(sender, target, reason, banid, Expiry);

            return true;
        }
        catch (SQLException | InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }

    private boolean BanWaveRemove(CommandSender sender, boolean silent, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.banwave.remove"))
            return User.PermissionDenied(sender, "lolbans.banwave.remove");

        ArgumentUtil a = new ArgumentUtil(args);
        a.RequiredString("PlayerName", 0);

        if (!a.IsValid())
            return false;

        // Check and make sure the user actually exists.
        OfflinePlayer target = User.FindPlayerByAny(a.get("PlayerName"));
        if (target == null)
            return User.NoSuchPlayer(sender, a.get("PlayerName"), true);

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
            // TODO: Discord/Broadcast
            return true;
            
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }

    private boolean BanWaveExecute(CommandSender sender, boolean silent, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.banwave.enforce"))
            return User.PermissionDenied(sender, "lolbans.banwave.enforce");

        User.PlayerOnlyVariableMessage("BanWave.BanWaveStart", sender, sender.getName(), false);
        BanWaveRunnable bwr = new BanWaveRunnable();
        bwr.sender = sender;
        bwr.silent = silent;
        bwr.runTaskAsynchronously(self);
        return true;
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.BanWave", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.banwave"))
            return User.PermissionDenied(sender, "lolbans.banwave");
        
        ArgumentUtil a = new ArgumentUtil(args);
        a.OptionalFlag("Silent", "-s");
        a.RequiredString("Command", 0);

        if (!a.IsValid())
            return false;

        boolean silent = a.get("Silent") != null;
        String SubCommand = a.get("Command");
        args = a.GetUnparsedArgs();
        String[] Subargs = Arrays.copyOfRange(args, 1, args.length);

        if (SubCommand.equalsIgnoreCase("add"))
            return this.BanWaveAdd(sender, silent, label, Subargs);
        else if (Messages.CompareMany(SubCommand, new String[]{"remove", "rm", "delete", "del"}))
            return this.BanWaveRemove(sender, silent, label, Subargs);
        else if (Messages.CompareMany(SubCommand, new String[]{"enforce", "run", "start", "exec", "execute"}))
            return this.BanWaveExecute(sender, silent, label, Subargs);
            
        // If they run a sub command we don't know
        return false;
    }
}