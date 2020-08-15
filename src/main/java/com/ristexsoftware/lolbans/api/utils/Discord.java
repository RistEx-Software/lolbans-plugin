package com.ristexsoftware.lolbans.api.utils;

import java.util.ArrayList;
import java.util.List;

import com.mrpowergamerbr.temmiewebhook.DiscordEmbed;
import com.mrpowergamerbr.temmiewebhook.TemmieWebhook;
import com.mrpowergamerbr.temmiewebhook.DiscordEmbed.DiscordEmbedBuilder;
import com.mrpowergamerbr.temmiewebhook.embed.FieldEmbed;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.Punishment;

public class Discord {
    TemmieWebhook punishmentWebhook;
    TemmieWebhook reportWebhook;

    public Discord(String punishmentWebhook, String reportWebhook) {
        reload();
    }

    public void reload() {
        this.punishmentWebhook = new TemmieWebhook(LolBans.getPlugin().getConfig().getString("discord.punishment-webhook"));
        this.reportWebhook = new TemmieWebhook(LolBans.getPlugin().getConfig().getString("discord.report-webhook"));
    }

    public void send(Punishment punishment) {
        DiscordEmbedBuilder builder = DiscordEmbed.builder();

        List<FieldEmbed> fields = new ArrayList<FieldEmbed>();
    }
}