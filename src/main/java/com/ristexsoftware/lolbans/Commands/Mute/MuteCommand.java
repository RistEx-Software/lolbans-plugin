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


package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.sql.*;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.Map;

public class MuteCommand extends RistExCommand
{
    public MuteCommand(Plugin owner)
    {
        super("mute", owner);
        this.setDescription("Prevent a player from sending messages in chat");
        this.setPermission("lolbans.mute");
        this.setAliases(Arrays.asList(new String[] { "emute","tempmute" }));
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Mute", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.mute"))
            return User.PermissionDenied(sender, "lolbans.mute");

        // /mute [-s, -o] <PlayerName> <TimePeriod|*> <Reason>
        try 
        {
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.OptionalFlag("Overwrite", "-o");
            a.RequiredString("PlayerName", 0);
            a.OptionalString("TimePeriod", 1);
            a.RequiredSentence("Reason", a.get("TimePeriod")==null?0:1);

            if (!a.IsValid())
                return false;

            boolean silent = a.get("Silent") != null;
            boolean ow = a.get("Overwrite") != null;
            String PlayerName = a.get("PlayerName");
            Timestamp punishtime = TimeUtil.ParseToTimestamp(a.get("TimePeriod"));
            String reason = punishtime == null ? a.get("TimePeriod")+" "+ a.get("Reason") : a.get("Reason");
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);
            Punishment punish = new Punishment(PunishmentType.PUNISH_WARN, sender instanceof Player ? ((Player)sender).getUniqueId().toString() : null, target, reason, punishtime, silent);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);
                
            if(ow && !sender.hasPermission("lolbans.mute.overwrite")) {
                return User.PermissionDenied(sender, "lolbans.mute.overwrite");
            } else if(ow) {
                User.removePunishment(PunishmentType.PUNISH_MUTE, sender, target, "Overwritten by #" + punish.GetPunishmentID(), silent);
            }
            
            if (User.isPlayerMuted(target).get() && !ow)
                return User.PlayerOnlyVariableMessage("Mute.PlayerIsMuted", sender, target.getName(), true);
            
            if (punishtime == null && !PermissionUtil.Check(sender, "lolbans.mute.perm"))
                return User.PermissionDenied(sender, "lolbans.mute.perm");

            // If punishtime is null and they got past the check above, we don't need to check this
            if (punishtime != null && punishtime.getTime() > User.getTimeGroup(sender).getTime())
                punishtime = User.getTimeGroup(sender);
                //return User.PermissionDenied(sender, "lolbans.maxtime."+a.get("TimePeriod"));

            punish.Commit(sender);
            
            final Timestamp thisissodumb = punishtime;
            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("arbiter", sender.getName());
                    put("punishid", punish.GetPunishmentID());
                    put("expiry", thisissodumb == null ? "" : punish.GetExpiryString());
                    put("silent", Boolean.toString(silent));
                    put("appealed", Boolean.toString(punish.GetAppealed()));
                    put("expires", Boolean.toString(thisissodumb != null && !punish.GetAppealed()));
                }};

            if (target.isOnline()) {
                ((Player)target).sendMessage(Messages.Translate("Mute.YouWereMuted", Variables));
                User.playSound((Player)target, Main.getPlugin(Main.class).getConfig().getString("ChatSettings.MuteSettings.Sound"));
            }
            System.out.println(punish.GetPunishmentType());

            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Mute.MuteAnnouncement", Variables));
            DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}