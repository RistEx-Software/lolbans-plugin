package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

import javax.net.ssl.HttpsURLConnection;
import javax.xml.soap.MessageFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Arrays;

import com.mrpowergamerbr.temmiewebhook.DiscordEmbed;
import com.mrpowergamerbr.temmiewebhook.DiscordMessage;
import com.mrpowergamerbr.temmiewebhook.TemmieWebhook;
import com.mrpowergamerbr.temmiewebhook.embed.FieldEmbed;
import com.mrpowergamerbr.temmiewebhook.embed.FooterEmbed;
import com.mrpowergamerbr.temmiewebhook.embed.ThumbnailEmbed;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/*
               _   ___      ___   _ 
              | | | \ \ /\ / / | | |
              | |_| |\ V  V /| |_| |
               \__,_| \_/\_/  \__,_|
*/

public class DiscordUtil 
{
    public static String Webhook;
    public static Boolean UseSimplifiedMessage;
    
    public static String SimpMessageBan;
    public static String SimpMessageUnban;
    public static String SimpMessageSilentBan;
    public static String SimpMessageSilentUnban;
    public static String SimpMessageMute;
    public static String SimpMessageUnmute;
    public static String SimpMessageSilentMute;
    public static String SimpMessageSilentUnmute;
    public static String SimpMessageKick;
    public static String SimpMessageSilentKick;
    public static String SimpMessageWarn;
    public static String SimpMessageSilentWarn;

    public static String WebhookProfilePicture;
    public static String ReportWebhook;

        //sender.getName().toString(), target.getName(), 
        //((Entity) sender).getUniqueId().toString(), target.getUniqueId().toString(), reason, silent

        public static void SendDiscord(String sender, String title, String target, String SenderUUID, String TargetUUID, String reason, String PunishID, Timestamp bantime, Boolean silent)
        {
            String st = silent ? " [Silent]" : "";
            TemmieWebhook temmie = new TemmieWebhook(Webhook);

            // https://crafatar.com/renders/head/e296a7d7-7c25-4d90-894b-feba23665a98?overlay
            String fuckingjava = "https://crafatar.com/renders/head/" + TargetUUID + "?overlay&default=MHF_Steve";        
            DiscordEmbed de = DiscordEmbed.builder()
                    .color(255)
                    .title(sender + " " + title + " " + target + st) // We are creating a embed with this title...
                    .description(reason) // with this description...
                    // Maybe a link to the ban id if a website is configured?
                    .footer(FooterEmbed.builder() // with a fancy footer...
                            .text(target) // this footer will have the text "TemmieWebhook!"...
                            .icon_url("https://crafatar.com/avatars/" + TargetUUID + "?overlay") // with this icon on the footer
                    .build()) // and now we build the footer...
                    .thumbnail(ThumbnailEmbed.builder() // with a fancy thumbnail...
                            .url(fuckingjava) // with this thumbnail...
                            //.height(64) // not too big because we don't want to flood the user chats with a huge image, right?
                            .build()) // and now we build the thumbnail...
                    //.url("https://github.com/MrPowerGamerBR/TemmieWebhook") // that, when clicked, goes to the TemmieWebhook repo...
                    .fields(Arrays.asList( // with fields...
                        FieldEmbed.builder()
                        .name("PunishID")
                        .value("#" + PunishID)
                        .build(),
                        FieldEmbed.builder()
                        .name("Expires")
                        .value(bantime != null ? TimeUtil.TimeString(bantime) : "Indefinite")
                        .build()))
                    .build(); // and finally, we build the embed
                
            DiscordMessage dm = DiscordMessage.builder()
                    .username(sender) // We are creating a message with the username "LolBans"...
                    .avatarUrl("https://crafatar.com/avatars/" + SenderUUID) // with this avatar...
                    .embeds(Arrays.asList(de)) // with the our embed...
                    .build(); // and now we build the message!
                
            temmie.sendMessage(dm);
        }

        public static void SendDiscord(String sender, String title, String target, String SenderUUID, String TargetUUID, String reason, String PunishID, Boolean silent)
        {
            String st = silent ? " [Silent]" : "";
            TemmieWebhook temmie = new TemmieWebhook(Webhook);

            // https://crafatar.com/renders/head/e296a7d7-7c25-4d90-894b-feba23665a98?overlay
            String fuckingjava = "https://crafatar.com/renders/head/" + TargetUUID + "?overlay&default=MHF_Steve";        
            DiscordEmbed de = DiscordEmbed.builder()
                    .color(255)
                    .title(sender + " " + title + " " + target + st) // We are creating a embed with this title...
                    .description(reason) // with this description...
                    // Maybe a link to the ban id if a website is configured?
                    .footer(FooterEmbed.builder() // with a fancy footer...
                            .text(target) // this footer will have the text "TemmieWebhook!"...
                            .icon_url("https://crafatar.com/avatars/" + TargetUUID + "?overlay") // with this icon on the footer
                    .build()) // and now we build the footer...
                    .thumbnail(ThumbnailEmbed.builder() // with a fancy thumbnail...
                            .url(fuckingjava) // with this thumbnail...
                            //.height(64) // not too big because we don't want to flood the user chats with a huge image, right?
                            .build()) // and now we build the thumbnail...
                    //.url("https://github.com/MrPowerGamerBR/TemmieWebhook") // that, when clicked, goes to the TemmieWebhook repo...
                    .fields(Arrays.asList( // with fields...
                        FieldEmbed.builder()
                        .name("PunishID")
                        .value("#" + PunishID)
                        .build()))
                    .build(); // and finally, we build the embed
                
            DiscordMessage dm = DiscordMessage.builder()
                    .username(sender) // We are creating a message with the username "LolBans"...
                    .avatarUrl("https://crafatar.com/avatars/" + SenderUUID) // with this avatar...
                    .embeds(Arrays.asList(de)) // with the our embed...
                    .build(); // and now we build the message!
                
            temmie.sendMessage(dm);
        }

        public static void SendDiscord(CommandSender sender, String title, OfflinePlayer target, String reason, String PunishID, Boolean silent)
        {
                DiscordUtil.SendDiscord(sender.getName().toString(), title, target.getName().toString(),
                (sender instanceof ConsoleCommandSender) ? "f78a4d8d-d51b-4b39-98a3-230f2de0c670" : ((OfflinePlayer)sender).getUniqueId().toString(),
                target.getUniqueId().toString(), reason, PunishID, silent);
        }

        public static void SendDiscord(CommandSender sender, String title, OfflinePlayer target, String reason, String PunishID, Timestamp expiry, Boolean silent)
        {
                DiscordUtil.SendDiscord(sender.getName().toString(), title, target.getName().toString(),
                (sender instanceof ConsoleCommandSender) ? "f78a4d8d-d51b-4b39-98a3-230f2de0c670" : ((OfflinePlayer)sender).getUniqueId().toString(),
                target.getUniqueId().toString(), reason, PunishID, expiry, silent);
        }


        public static void SendBanWave(String message, String ptbqa)
        {
                TemmieWebhook temmie = new TemmieWebhook(Webhook);
                DiscordEmbed de = DiscordEmbed.builder()
                        .title(message) // We are creating a embed with this title...
                        .description("The following users are being banned! \n" + ptbqa)
                        .color(255)
                        // Maybe a link to the ban id if a website is configured?
                        //.url("https://github.com/MrPowerGamerBR/TemmieWebhook") // that, when clicked, goes to the TemmieWebhook repo...
                        //ntbq

                        .footer(FooterEmbed.builder() // with a fancy footer...
                                .text("LolBans") // this footer will have the text "TemmieWebhook!"...
                        .build()) // and now we build the footer...
                        .build(); // and finally, we build the embed
                        
                        
                DiscordMessage dm = DiscordMessage.builder()
                .username("BanWave") // We are creating a message with the username "LolBans"...
                .avatarUrl("https://crafatar.com/avatars/" + "f78a4d8d-d51b-4b39-98a3-230f2de0c670" + "?&overlay") // with this avatar
                .embeds(Arrays.asList(de)) // with the our embed...
                .build(); // and now we build the message!

                temmie.sendMessage(dm);
        }

        public static void SendBanWaveAdd(String sender, String target, String TargetUUID, String SenderUUID, String reason, String BanID)
        {
                TemmieWebhook temmie = new TemmieWebhook(Webhook);
                String fuckingjava = "https://crafatar.com/renders/head/" + TargetUUID + "?overlay";
                String Color = BanID.replaceAll("[^0-9]","");

                DiscordEmbed de = DiscordEmbed.builder()
                        .title(sender + " has added " + target + " to the ban wave") // We are creating a embed with this title...
                        .color(Integer.parseInt(Color))
                        // Maybe a link to the ban id if a website is configured?
                        //.url("https://github.com/MrPowerGamerBR/TemmieWebhook") // that, when clicked, goes to the TemmieWebhook repo...
                        .footer(FooterEmbed.builder() // with a fancy footer...
                                .text(target) // this footer will have the text "TemmieWebhook!"...
                                .icon_url("https://crafatar.com/avatars/" + TargetUUID + "?overlay") // with this icon on the footer
                        .build()) // and now we build the footer...
                        .description(reason) // with this description...
                        // Maybe a link to the ban id if a website is configured?
                        .thumbnail(ThumbnailEmbed.builder() // with a fancy thumbnail...
                                .url(fuckingjava) // with this thumbnail...
                                //.height(64) // not too big because we don't want to flood the user chats with a huge image, right?
                                .build()) // and now we build the thumbnail...
                        //.url("https://github.com/MrPowerGamerBR/TemmieWebhook") // that, when clicked, goes to the TemmieWebhook repo...
                        .fields(Arrays.asList( // with fields...
                                FieldEmbed.builder()
                                .name("BanID")
                                .value("#" + BanID)
                                .build(),
                                FieldEmbed.builder()
                                .name("Expires")
                                .value("Indefinite")
                                .build() ))
                        .build(); // and finally, we build the embed
                        
                DiscordMessage dm = DiscordMessage.builder()
                        .username(sender) // We are creating a message with the username "LolBans"...
                        .avatarUrl("https://crafatar.com/avatars/" + SenderUUID) // with this avatar...
                        .embeds(Arrays.asList(de)) // with the our embed...
                        .build(); // and now we build the message!

                temmie.sendMessage(dm);
        }

        public static void SendFormatted(String message)
        {
                TemmieWebhook temmie = new TemmieWebhook(Webhook);
                DiscordMessage dm = DiscordMessage.builder()
                        .username("LolBans") // We are creating a message with the username "Temmie"...
                        .content(message) // with this content...
                        .avatarUrl(DiscordUtil.WebhookProfilePicture) // with this avatar...
                        .build(); // and now we build the message!
                        
                temmie.sendMessage(dm);
        }
}