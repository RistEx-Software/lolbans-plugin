/* 
 *  LolBans - An advanced punishment management system made for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ristexsoftware.lolbans.bukkit;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.knappy.util.Version;
import com.ristexsoftware.lolbans.bukkit.Listeners.ConnectionListener;
import com.ristexsoftware.lolbans.bukkit.Listeners.PlayerEventListener;
import com.ristexsoftware.lolbans.bukkit.provider.BukkitConfigProvider;
import com.ristexsoftware.lolbans.bukkit.provider.BukkitUserProvider;
import com.ristexsoftware.lolbans.common.commands.ban.Ban;
import com.ristexsoftware.lolbans.common.commands.ban.BanWave;
import com.ristexsoftware.lolbans.common.commands.ban.IPBan;
import com.ristexsoftware.lolbans.common.commands.ban.RegexBan;
import com.ristexsoftware.lolbans.common.commands.history.History;
import com.ristexsoftware.lolbans.common.commands.history.PruneHistory;
import com.ristexsoftware.lolbans.common.commands.misc.Kick;
import com.ristexsoftware.lolbans.common.commands.misc.Rollback;
import com.ristexsoftware.lolbans.common.commands.misc.Warn;
import com.ristexsoftware.lolbans.common.commands.misc.Maintenance;
import com.ristexsoftware.lolbans.common.utils.CommandUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;

public class Main extends JavaPlugin {
    @Getter
    private static Main plugin;
    public static boolean isEnabled = false;
    List<Command> CommandList = new ArrayList<Command>();

    @Override
    public void onEnable() {
        plugin = this;
        isEnabled = true;
        try {
            new LolBans(new BukkitConfigProvider(), new BukkitUserProvider(), Version.getServerType());
        } catch (FileNotFoundException e) {
            return;
        }


        // This is dumb, plugman somehow breaks lolbans
        // but whatever, you shouldn't be using plugman anyway...
        if (getServer().getPluginManager().getPlugin("PlugMan") != null)
            getLogger().warning(
                    "PlugMan detected! This WILL cause issues with LolBans, please consider restarting the server to update plugins!");

        // Make sure our messages file exists
        Messages.getMessages();

        for (Player player : Bukkit.getOnlinePlayers())
            LolBans.getPlugin().registerUser(player);

        if (!Database.initDatabase())
            return;

        // System.out.println("aaaa ur gay");
        CommandUtil.Bukkit.registerBukkitCommand(new Ban.BanCommand(LolBans.getPlugin()));
        CommandUtil.Bukkit.registerBukkitCommand(new Ban.UnbanCommand(LolBans.getPlugin()));

        CommandUtil.Bukkit.registerBukkitCommand(new IPBan.Ban(LolBans.getPlugin()));
        CommandUtil.Bukkit.registerBukkitCommand(new IPBan.Unban(LolBans.getPlugin()));    

        CommandUtil.Bukkit.registerBukkitCommand(new RegexBan.Ban(LolBans.getPlugin()));
        CommandUtil.Bukkit.registerBukkitCommand(new RegexBan.Unban(LolBans.getPlugin()));

        CommandUtil.Bukkit.registerBukkitCommand(new BanWave(LolBans.getPlugin()));

        CommandUtil.Bukkit.registerBukkitCommand(new History(LolBans.getPlugin()));
        // CommandUtil.Bukkit.registerBukkitCommand(new PruneHistory(LolBans.getPlugin()));
        // CommandUtil.Bukkit.registerBukkitCommand(new Rollback(LolBans.getPlugin()));

        CommandUtil.Bukkit.registerBukkitCommand(new Kick(LolBans.getPlugin()));

        CommandUtil.Bukkit.registerBukkitCommand(new Warn.WarnCommand(LolBans.getPlugin()));
        CommandUtil.Bukkit.registerBukkitCommand(new Warn.UnwarnCommand(LolBans.getPlugin()));
        CommandUtil.Bukkit.registerBukkitCommand(new Warn.AcknowledgeWarnCommand(LolBans.getPlugin()));

        CommandUtil.Bukkit.registerBukkitCommand(new Maintenance(LolBans.getPlugin()));

        getServer().getPluginManager().registerEvents(new ConnectionListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);
    }

    @Override
    public void onDisable() {
        LolBans.getPlugin().destroy();
        isEnabled = false;
        reloadConfig();
    }
}