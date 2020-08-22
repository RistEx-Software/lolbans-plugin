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

package com.ristexsoftware.lolbans.api.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import com.mrpowergamerbr.temmiewebhook.DiscordEmbed;
import com.mrpowergamerbr.temmiewebhook.DiscordMessage;
import com.mrpowergamerbr.temmiewebhook.TemmieWebhook;
import com.mrpowergamerbr.temmiewebhook.DiscordEmbed.DiscordEmbedBuilder;
import com.mrpowergamerbr.temmiewebhook.DiscordMessage.DiscordMessageBuilder;
import com.mrpowergamerbr.temmiewebhook.embed.FieldEmbed;
import com.mrpowergamerbr.temmiewebhook.embed.FooterEmbed;
import com.mrpowergamerbr.temmiewebhook.embed.ThumbnailEmbed;
import com.ristexsoftware.knappy.configuration.Configuration;
import com.ristexsoftware.knappy.configuration.ConfigurationSection;
import com.ristexsoftware.knappy.translation.LocaleProvider;
import com.ristexsoftware.knappy.translation.Translation;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;

import lombok.Getter;
import lombok.Setter;

public class Discord {
    LocaleProvider locale = LolBans.getPlugin().getLocaleProvider();
    TemmieWebhook punishmentWebhook;
    TemmieWebhook reportWebhook;

    @Getter
    @Setter
    Boolean simplifiedMessage = false;

    public Discord(String punishmentWebhook, String reportWebhook) {
        reload();
    }

    public void reload() {
        this.punishmentWebhook = new TemmieWebhook(LolBans.getPlugin().getConfig().getString("discord.punishment-webhook"));
        this.reportWebhook = new TemmieWebhook(LolBans.getPlugin().getConfig().getString("discord.report-webhook"));
        this.simplifiedMessage = LolBans.getPlugin().getConfig().getBoolean("discord.simple.enabled");
    }
    
    public void send(Punishment punishment) {
        TreeMap<String, String> vars = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {{
            put("player", punishment.getTarget() == null ? null : punishment.getTarget().getName());
            put("ipaddress", punishment.getIpAddress() == null ? "#" : punishment.getIpAddress().toString());
            put("censroedipaddress", punishment.getIpAddress() == null ? "#" : Translation.censorWord(punishment.getIpAddress().toString()));
            put("reason", punishment.getAppealed() ? punishment.getAppealReason() : punishment.getReason());
            put("arbitername", punishment.getAppealed() ? punishment.getAppealedBy().getName() : punishment.getPunisher().getName());
            put("arbiteruuid", punishment.getAppealed() ? punishment.getAppealedBy().getUniqueId().toString() : punishment.getPunisher().getUniqueId().toString());
            put("expiry", punishment.getExpiresAt() == null ? "" : punishment.getExpiresAt().toString());
            put("silent", Boolean.toString(punishment.getSilent()));
            put("appealed",Boolean.toString(punishment.getAppealed()));
            put("expires",Boolean.toString(punishment.getExpiresAt() != null && !punishment.getAppealed()));
            put("punishid", punishment.getPunishID());
            put("warningack", Boolean.toString(punishment.getWarningAck()));
            put("regex", punishment.getRegex() == null ? "" : punishment.getRegex());
            put("type", punishment.getType().displayName());
            put("typeordinal", String.valueOf(punishment.getType().ordinal()));
        }};
    
        DiscordMessageBuilder discordMessage = DiscordMessage.builder();
        discordMessage.username(locale.translate("discord.username", vars));
        discordMessage.avatarUrl(locale.translateNoColor("discord.avatar", vars));
        
        PunishmentType type = PunishmentType.fromOrdinal(Integer.valueOf(vars.get("typeordinal")));
        if (simplifiedMessage) {
            String message = null;
            switch(type) {  
                case BAN:
                case MUTE:
                case KICK:
                case WARN:
                    message = locale.translate("discord.simple.message-" + type.name().toLowerCase(), vars);
                case IP:
                case REGEX: 
                    message = locale.translate("discord.simple.message-" + type.name().toLowerCase() + "-ban", vars);
                case BANWAVE: 
                    break;
            }
            discordMessage.content(locale.translate(message, vars));
            this.punishmentWebhook.sendMessage(discordMessage.build());
        } else {
            String title = null;
            switch (type) {
                case BAN:
                case MUTE:
                case KICK:
                case WARN:
                case BANWAVE:
                    title = locale.translate("discord.embed." + type.name().toLowerCase() + "-title", vars);
                case REGEX:
                case IP:
                    title = locale.translate("discord.embed." + type.name().toLowerCase() + "-ban-title", vars);
                default:
                    title = locale.translate("discord.embed.unknown-title", vars);
            }
    
            List<FieldEmbed> fields = new ArrayList<FieldEmbed>();
            for (String section : LolBans.getPlugin().getConfig().getConfigurationSection("discord.embed.fields").getKeys(false)) {
                fields.add(FieldEmbed.builder().name(locale.translate(LolBans.getPlugin().getConfig().getString("discord.embed.fields."+section+".title"), vars)).value(locale.translate(LolBans.getPlugin().getConfig().getString("discord.embed.fields."+section+".content"), vars)).build());
            }
    
            DiscordEmbedBuilder discordEmbed = DiscordEmbed.builder();
            discordEmbed.color(Integer.parseInt(vars.get("punishid").replaceAll("[^0-9]", "")));
            discordEmbed.title(title);
            discordEmbed.description(locale.translate("discord.embed.description", vars));
            discordEmbed.fields(fields);
            discordEmbed.thumbnail(ThumbnailEmbed.builder().url(locale.get("discord.thumbnail")).build());
            discordEmbed.footer(FooterEmbed.builder().text(locale.translate("discord.embed.footer", vars)).build());
            
            discordMessage.embed(discordEmbed.build());
        }
    }
}