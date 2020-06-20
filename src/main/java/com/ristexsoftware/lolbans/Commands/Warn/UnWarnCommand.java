package com.ristexsoftware.lolbans.Commands.Warn;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


public class UnWarnCommand extends RistExCommand
{
    public UnWarnCommand(Plugin owner)
    {
        super("unwarn", owner);
        this.setDescription("Remove a warning previously issued to a player");
        this.setPermission("lolbans.unwarn");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.UnWarn", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.unwarn"))
            return User.PermissionDenied(sender, "lolbans.unwarn");

        // /unwarn [-s] <PlayerName|PunishID>
        try 
		{
			ArgumentUtil a = new ArgumentUtil(args);
			a.OptionalFlag("Silent", "-s");
			a.RequiredString("PlayerName", 0);

			if (!a.IsValid())
                return false;

            boolean silent = a.get("Silent") != null;
            String PlayerName = a.get("PlayerName");
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

			Optional<Punishment> opunish = Punishment.FindPunishment(PunishmentType.PUNISH_WARN, target, false);
			if (!opunish.isPresent())
			{
				sender.sendMessage("Congratulations!! You've found a bug!! Please report it to the lolbans developers to get it fixed! :D");
                return true;
			}
			Punishment punish = opunish.get();

            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", target.getName());
                put("punishid", punish.GetPunishmentID());
                put("arbiter", sender.getName());
                put("silent", Boolean.toString(silent));
                put("appealed", Boolean.toString(punish.GetAppealed()));
			}};
			
			// We don't track punishments that were removed so we just delete them.
			punish.Delete();
                
            // If they're online, require acknowledgement immediately by freezing them and sending a message.
            if (target.isOnline())
            {
                User u = Main.USERS.get(target.getUniqueId());
                Player player = (Player) target;
                u.SetWarned(false, player.getLocation(), "unwarned");
                u.SendMessage(Messages.Translate("Warn.AcceptMessage", Variables));
            }
			
			sender.sendMessage(Messages.Translate("Warn.RemovedSuccess", Variables));
            if (Messages.Discord)
                DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }

        return true;
    }
}
