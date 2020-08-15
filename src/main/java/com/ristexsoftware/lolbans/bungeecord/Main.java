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

package com.ristexsoftware.lolbans.bungeecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.UUID;

import com.ristexsoftware.knappy.util.Version;
import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.common.utils.CommandUtil;
import com.ristexsoftware.lolbans.bungeecord.Listeners.ConnectionListener;
import com.ristexsoftware.lolbans.bungeecord.provider.BungeeConfigProvider;
import com.ristexsoftware.lolbans.bungeecord.provider.BungeeUserProvider;
import com.ristexsoftware.lolbans.common.commands.ban.*;
import com.ristexsoftware.lolbans.common.commands.history.*;
import com.ristexsoftware.lolbans.common.commands.misc.*;
import com.ristexsoftware.lolbans.common.commands.mute.Mute;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class Main extends Plugin {
    public static boolean isEnabled;

    @Getter
    public static Main plugin;

    @Getter
    public static File folder;

    // !IMPORTANT TODO: Create config files and datafolder if it doesn't already exist inside the config providers, instead of in the LolBans class
    @Override
    public void onEnable() {
        plugin = this;
        isEnabled = true;
        try {
            new LolBans(new BungeeConfigProvider(), new BungeeUserProvider(), Version.getServerType());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Make sure our messages file exists
        Messages.getMessages();

        if (!Database.initDatabase())
            return;

            CommandUtil.BungeeCord.registerBungeeCommand(new Ban.BanCommand(LolBans.getPlugin()));
            CommandUtil.BungeeCord.registerBungeeCommand(new Ban.UnbanCommand(LolBans.getPlugin()));
            
            CommandUtil.BungeeCord.registerBungeeCommand(new Mute.MuteCommand(LolBans.getPlugin()));
            CommandUtil.BungeeCord.registerBungeeCommand(new Mute.UnmuteCommand(LolBans.getPlugin()));

            CommandUtil.BungeeCord.registerBungeeCommand(new IPBan.Ban(LolBans.getPlugin()));
            CommandUtil.BungeeCord.registerBungeeCommand(new IPBan.Unban(LolBans.getPlugin()));    
    
            CommandUtil.BungeeCord.registerBungeeCommand(new RegexBan.Ban(LolBans.getPlugin()));
            CommandUtil.BungeeCord.registerBungeeCommand(new RegexBan.Unban(LolBans.getPlugin()));
    
            CommandUtil.BungeeCord.registerBungeeCommand(new BanWave(LolBans.getPlugin()));
    
            CommandUtil.BungeeCord.registerBungeeCommand(new History(LolBans.getPlugin()));
            CommandUtil.BungeeCord.registerBungeeCommand(new PruneHistory(LolBans.getPlugin()));
    
            CommandUtil.BungeeCord.registerBungeeCommand(new Kick(LolBans.getPlugin()));
    
            CommandUtil.BungeeCord.registerBungeeCommand(new Warn.WarnCommand(LolBans.getPlugin()));
            CommandUtil.BungeeCord.registerBungeeCommand(new Warn.UnwarnCommand(LolBans.getPlugin()));
            CommandUtil.BungeeCord.registerBungeeCommand(new Warn.AcknowledgeWarnCommand(LolBans.getPlugin()));
            CommandUtil.BungeeCord.registerBungeeCommand(new Rollback(LolBans.getPlugin()));
        getProxy().getPluginManager().registerListener(this, new ConnectionListener());
    }

    @Override
    public void onDisable() {
        LolBans.getPlugin().destroy();
        isEnabled = false;
    }

    public static ProxiedPlayer getPlayer(String name) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }
    public static ProxiedPlayer getPlayer(UUID uuid) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.getUniqueId() == uuid) return player;
        }
        return null;
    }
}
