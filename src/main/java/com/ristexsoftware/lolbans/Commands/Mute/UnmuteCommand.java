package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.util.Optional;
import java.util.TreeMap;

public class UnmuteCommand extends RistExCommand
{
    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        sender.sendMessage(Messages.InvalidSyntax);
        sender.sendMessage("Usage: /unmute [-s] <Player> <Reason>");
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.unmute"))
            return User.PermissionDenied(sender, "lolbans.unmute");

        // /unmute [-s] <PlayerName> <Reason>
        if (args.length < 2)
            return false;

        try 
        {
            boolean silent = args.length > 2 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = args[silent ? 1 : 0];
            String reason = Messages.ConcatenateRest(args, silent ? 2 : 1).trim();
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);
            
            if (!User.IsPlayerMuted(target))
                return User.PlayerOnlyVariableMessage("Mute.PlayerIsNotMuted", sender, target.getName(), true);

            Optional<Punishment> op = Punishment.FindPunishment(PunishmentType.PUNISH_MUTE, target, false);

            if (!op.isPresent())
            {
                sender.sendMessage("Congratulations!! You've found a bug!! Please report it to the lolbans developers to get it fixed! :D");
                return true;
            }

            Punishment punish = op.get();
            punish.SetAppealReason(reason);
            punish.SetAppealed(true);
            punish.SetAppealStaff((OfflinePlayer)sender);
            punish.Commit(sender);

            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("arbiter", sender.getName());
                    put("punishid", punish.GetPunishmentID());
                }};

            // Post that to the database.
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (!silent && (p.hasPermission("lolbans.alerts") || p.isOp()) && !p.equals(target))
                    p.sendMessage(Messages.Translate(silent ? "Mute.SilentUnmuteAnnouncment" : "Mute.UnmuteAnnouncment", Variables));
            }

            if (target.isOnline())
                ((Player)target).sendMessage(Messages.Translate("Mute.YouWereUnMuted", Variables));

            if (DiscordUtil.UseSimplifiedMessage == true)
                DiscordUtil.SendFormatted(Messages.Translate(silent ? "Discord.SimpMessageSilentUnmute" : "Discord.SimpMessageUnmute", Variables));
            else
                DiscordUtil.SendDiscord(punish, silent);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}