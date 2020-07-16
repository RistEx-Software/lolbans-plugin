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


package com.ristexsoftware.lolbans.Commands.Warn;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.TreeMap;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

public class AcceptCommand extends RistExCommand
{
    private Main self = (Main)this.getPlugin();

    public AcceptCommand(Plugin owner)
    {
        super("acknowledge", owner);
        this.setDescription("Accept a warning");
        this.setPermission("lolbans.warnaccept");
    }    

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.WarnAccept", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.warnaccept"))
            return User.PermissionDenied(sender, "lolbans.warnaccept");

        // /accept [<PlayerName>]
        // TODO: What? This needs to handle args?
        if(!(sender instanceof Player))
            return User.PlayerOnlyVariableMessage("UserRequired", sender, sender.getName(), true);

        User u = Main.USERS.get(((Player)sender).getUniqueId());
        if (u == null)
        {
            sender.sendMessage("Congratulations!!! YOU FOUND A BUG! One cookie for you! Please report this bug to the lolbans team!");
            return true;
        }

        // Getting command name
        if (u.IsWarn()) 
        {
            try 
            {
                // Unset them warned locally
                u.SetWarned(false, null, null);
                // Preapre a statement
                PreparedStatement pst3 = self.connection
                        .prepareStatement("UPDATE lolbans_punishments SET WarningAck = true WHERE UUID = ? AND Type = ?");
                pst3.setString(1, u.getPlayer().getUniqueId().toString());
                pst3.setInt(2, PunishmentType.PUNISH_WARN.ordinal());
                DatabaseUtil.ExecuteUpdate(pst3);

                User.PlayerOnlyVariableMessage("Warn.AcceptMessage", sender, sender.getName(), false);
                // TODO: Discord/BroadcastOps
            } 
            catch (SQLException e)
            {
                e.printStackTrace();
                sender.sendMessage(Messages.ServerError);
            }
        }
        else
            User.PlayerOnlyVariableMessage("Warn.NotWarned", sender, sender.getName(), true);

        return true;
    }
}