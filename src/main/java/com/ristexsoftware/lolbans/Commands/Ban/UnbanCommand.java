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

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;
import com.ristexsoftware.lolbans.Utils.Timing;

import java.util.TreeMap;

public class UnbanCommand extends RistExCommandAsync
{
    public UnbanCommand(Plugin owner)
    {
        super("unban", owner);
        this.setDescription("Remove a player ban");
        this.setPermission("lolbans.unban");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Unban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.unban"))
            return true;
        
        // Syntax: /unban [-s] <PlayerName|PunishID> <Reason>
        try 
        {
            Timing t = new Timing();
            
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.RequiredString("PlayerName", 0);
            a.RequiredSentence("Reason", 1);

            if (a.get("PlayerName") == null)
                return false;

            boolean silent = a.get("Silent") != null;
            String PlayerName = a.get("PlayerName");
            String reason = a.get("Reason") == null ? Main.getPlugin(Main.class).getConfig().getString("BanSettings.DefaultUnban") : a.get("Reason");
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);
            
            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);
            
            if (!User.IsPlayerBanned(target))
                return User.PlayerOnlyVariableMessage("Ban.PlayerIsNotBanned", sender, target.getName(), true);

            // // Preapre a statement
            // // We need to get the latest banid first.
            // Optional<Punishment> op = Punishment.FindPunishment(PunishmentType.PUNISH_BAN, target, false);
            // if (!op.isPresent())
            // {
            //     sender.sendMessage("Congratulations!! You've found a bug!! Please report it to the lolbans developers to get it fixed! :D");
            //     return true;
            // }

            // Punishment punish = op.get();
            // punish.SetAppealReason(reason);
            // punish.SetAppealed(true);
            // punish.SetAppealTime(TimeUtil.TimestampNow());
            // punish.SetAppealStaff(sender);
            // punish.Commit(sender);
            Punishment punish = User.removePunishment(PunishmentType.PUNISH_BAN, sender, target, reason, silent);
            if (punish == null) return true;

            // Prepare our announce message
            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", target.getName());
                put("reason", reason);
                put("arbiter", sender.getName());
                put("punishid", punish.GetPunishmentID());
                put("silent", Boolean.toString(silent));
                put("appealed", Boolean.toString(punish.GetAppealed()));
                put("expires", Boolean.toString(punish.GetExpiry() != null && !punish.GetAppealed()));
            }};
            
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Ban.BanAnnouncement", Variables));
            DiscordUtil.GetDiscord().SendDiscord(punish, silent);
            t.Finish(sender);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}