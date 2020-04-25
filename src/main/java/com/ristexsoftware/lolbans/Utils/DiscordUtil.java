package com.ristexsoftware.lolbans.Utils; // Zachery's package owo :spray:

import java.security.InvalidKeyException;
import java.sql.Timestamp;
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
import com.ristexsoftware.lolbans.Objects.Punishment;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

/*
			   _   ___      ___   _ 
			  | | | \ \ /\ / / | | |
			  | |_| |\ V  V /| |_| |
			   \__,_| \_/\_/  \__,_|
							   ᵈᵃᵈᵈʸ
*/

/**
 * Okay. Look. This file contains a lot of black magic.
 * I had no clue what the fuck was going on *even when I was writing the code*
 * and I really would rather not deal with the discord code anymore than I need
 * to. I tried my best to make it as abstracted as possible but it's really hard
 * to do. Heed my warning: Don't fucking try and change anything in here.
 * ✨ IT'S. BLACK. MAGIC. 🧙 ✨
 */

public class DiscordUtil 
{
		public Boolean UseSimplifiedMessage;
		public String WebhookProfilePicture;

		private TemmieWebhook ModeratorWebhook;
		private TemmieWebhook ReportWebhook;
		private static DiscordUtil me = null;

		public DiscordUtil(String ModeratorWebhook, String ReportWebhook)
		{
			// TODO: handle /reload better?
			DiscordUtil.me = this;
			this.reload(ModeratorWebhook, ReportWebhook);
		}

		public static DiscordUtil GetDiscord() { return DiscordUtil.me; }
		
		public void reload(String ModeratorWebhook, String ReportWebhook)
		{
			this.ModeratorWebhook = new TemmieWebhook(ModeratorWebhook);
			this.ReportWebhook = new TemmieWebhook(ReportWebhook);
		}

		// message.yml
		// - Title
		// - Expiry

		// Variables must contain the following:
		// - PlayerUUID  -- The UUID of the person being punished
		// - ArbiterName -- The Name of the person who executes the command
		// - ArbiterUUID -- The UUID of the person who executes the command
		// - Reason      -- The primary text to make part of the embed.
		// - FooterText  -- Text to show on the footer
		// - Expiry      -- If present, will add the "expires" field
		// - PunishID    -- If present, will add the "PunishID" field 
		// - ReportType  -- If present, will add the "Type" field
		private DiscordMessage SendEmbed(String MessageNode, TreeMap<String, String> Variables) throws InvalidConfigurationException, InvalidKeyException
		{
			if (!Variables.containsKey("PlayerUUID") || !Variables.containsKey("ArbiterName") || !Variables.containsKey("ArbiterUUID") || !Variables.containsKey("FooterText") || !Variables.containsKey("Reason"))
				throw new InvalidKeyException("Missing a required key for Discord Embeds.");

			// Dynamically add the various fields we may need
			List<FieldEmbed> Fields = new ArrayList<FieldEmbed>();

			// TODO: Find a reasonable way to messages.yml-ify this?
			if (Variables.containsKey("PunishID"))
				Fields.add(FieldEmbed.builder().name("PunishID").value("#" + Variables.get("PunishID")).build());
			if (Variables.containsKey("Expires"))
				Fields.add(FieldEmbed.builder().name("Expires").value(Variables.get("Expires")).build());
			if (Variables.containsKey("ReportType"))
				Fields.add(FieldEmbed.builder().name("Type").value(Variables.get("ReportType")).build());

			// Start a discord embed builder
			DiscordEmbedBuilder build = DiscordEmbed.builder();
			// Set the color based on the PunishID (if available)
			if (Variables.containsKey("PunishID"))
				build.color(Integer.parseInt(Variables.get("PunishID").replaceAll("[^0-9]", "")));

			// Set the title
			build.title(Messages.Translate(MessageNode, Variables));
			// Set the description
			build.description(Variables.get("Reason"));
			// Set the fields from above
			build.fields(Fields);
			// Set the thumbnail picture
			build.thumbnail(ThumbnailEmbed.builder().url(Messages.Translate("Discord.ThumbnailPictures", Variables)).build());
			// Build the footer with the avatar
			build.footer(FooterEmbed.builder().text(Variables.get("FooterText")).icon_url(Messages.Translate("Discord.AvatarPictures",
				new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
				{{
					put("PlayerUUID", Variables.get("PlayerUUID"));
				}}
			)).build());

			// Get the Avatar URL from the config. We provide the PlayerUUID and the ArbiterUUID
			String AvatarURL = "";
			if (!Variables.get("ArbiterUUID").equalsIgnoreCase("CONSOLE"))
			{
				AvatarURL = Messages.Translate("Discord.AvatarPictures",
					new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) 
					{{
						put("PlayerUUID", Variables.get("ArbiterUUID"));
					}});
			}
			else
				AvatarURL = Messages.GetMessages().GetConfig().getString("Discord.ConsoleProfilePicture");

			// Build an actual Discord message, set the avatar, and the embed.
			
			DiscordMessageBuilder dm = DiscordMessage.builder();
			dm.username(Variables.get("ArbiterName"));
			dm.avatarUrl(AvatarURL);
			dm.embeds(Arrays.asList(build.build()));

			return dm.build();
		}

		public void SendDiscord(Punishment p, boolean silent) throws InvalidConfigurationException
		{
				String ExecutionerName = p.IsConsoleExectioner() ? "CONSOLE" : p.GetExecutioner().getName();
				String ExecutionerUUID = p.IsConsoleExectioner() ? "CONSOLE" : p.GetExecutioner().getUniqueId().toString();
				String action = null;

				switch(p.GetPunishmentType())
				{
				   case PUNISH_BAN: action = this.UseSimplifiedMessage ? "Discord.Simple.MessageBan" : "Discord.Embedded.BanTitle"; break;
				   case PUNISH_KICK: action = this.UseSimplifiedMessage ? "Discord.Simple.MessageKick" : "Discord.Embedded.KickTitle"; break;
				   case PUNISH_MUTE: action = this.UseSimplifiedMessage ? "Discord.Simple.MessageMute" : "Discord.Embedded.MuteTitle"; break;
				   case PUNISH_WARN: action = this.UseSimplifiedMessage ? "Discord.Simple.MessageWarn" : "Discord.Embedded.WarnTitle"; break;
				   default: action = "Discord.Embedded.UnknownTitle"; break;
				}

				TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
					{{
						put("PlayerName", p.GetPlayerName());
						put("PlayerUUID", p.GetUUID().toString());
						put("ArbiterName", ExecutionerName);
						put("ArbiterUUID", ExecutionerUUID);
						put("Reason", p.GetReason());
						put("FooterText", p.GetPlayerName());
						if (p.GetExpiry() != null)
							put("Expiry", p.GetExpiry().toString());
						put("PunishID", p.GetPunishmentID());
						put("Applealed", Boolean.toString(p.GetAppealed()));
					}};

				try
				{
					// Send the message to discord.
					if (this.UseSimplifiedMessage)
						this.SendFormatted(Messages.Translate(action, Variables));
					else 
						this.ModeratorWebhook.sendMessage(this.SendEmbed(action, Variables));
				}
				catch (InvalidKeyException ex)
				{
					ex.printStackTrace();
				}		
		}

		public void SendBanObject(CommandSender sender, String Object, String Reason, String PunishID, Timestamp Expiry)
		{
			TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
			{{
				put("Object", Object);
				put("ArbiterName", sender.getName());
				put("ArbiterUUID", sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "CONSOLE");
				if (Expiry != null)
					put("Expiry", Expiry.toString());
				put("Reason", Reason);
				put("PunishID", PunishID);
			}};
			
			try
			{
				List<FieldEmbed> Fields = new ArrayList<FieldEmbed>();

				Fields.add(FieldEmbed.builder().name("PunishID").value("#" + PunishID).build());
				Fields.add(FieldEmbed.builder().name("Item").value(Object).build());

				if (Variables.containsKey("Expires"))
					Fields.add(FieldEmbed.builder().name("Expires").value(Variables.get("Expires")).build());

				if (this.UseSimplifiedMessage)
					this.SendFormatted(Messages.Translate("Discord.Simple.BanObject", Variables));
				else
				{
					// Get the Avatar URL from the config. We provide the PlayerUUID and the ArbiterUUID
					String AvatarURL = "";
					if (!Variables.get("ArbiterUUID").equalsIgnoreCase("CONSOLE"))
					{
						AvatarURL = Messages.Translate("Discord.AvatarPictures",
							new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) 
							{{
								put("PlayerUUID", Variables.get("ArbiterUUID"));
							}});
					}
					else
						AvatarURL = Messages.GetMessages().GetConfig().getString("Discord.ConsoleProfilePicture");

					DiscordEmbed de = DiscordEmbed.builder()
						.title(Messages.Translate("Discord.Embedded.BanObjectTitle", Variables)) // We are creating a embed with this title...
						.description(Reason)
						.color(Integer.parseInt(PunishID.replaceAll("[^0-9]", "")))
						.footer(FooterEmbed.builder().text(sender.getName()).icon_url(AvatarURL).build())
						.build(); // and finally, we build the embed

					DiscordMessage dm = DiscordMessage.builder()
							.username(sender.getName()) // We are creating a message with the username "LolBans"...
							.avatarUrl(AvatarURL) // with this avatar
							.embeds(Arrays.asList(de)) // with the our embed...
							.build(); // and now we build the message!

					this.ModeratorWebhook.sendMessage(dm);
				}
			}
			catch (InvalidConfigurationException ex)
			{
				ex.printStackTrace();
			}
		}


		public void SendBanWave(String ArbiterName, String UserList) throws InvalidConfigurationException
		{
			TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
			{{
				put("ARBITERNAME", ArbiterName);
				put("UserList", UserList);
			}};

			if (this.UseSimplifiedMessage)
				this.SendFormatted(Messages.Translate("Discord.Simple.BanwaveStart", Variables));
			else
			{
				
				DiscordEmbed de = DiscordEmbed.builder()
						.title(Messages.Translate("Discord.Embedded.BanwaveStartTitle", Variables)) // We are creating a embed with this title...
						.description(Messages.Translate("Discord.Embedded.BanwaveStartText", Variables)).color(255)
						// Maybe a link to the ban id if a website is configured?
						//.url("https://github.com/MrPowerGamerBR/TemmieWebhook") // that, when clicked, goes to the TemmieWebhook repo...
						//ntbq

						.footer(FooterEmbed.builder() // with a fancy footer...
								.text("LolBans") // this footer will have the text "TemmieWebhook!"...
								.build()) // and now we build the footer...
						.build(); // and finally, we build the embed

				DiscordMessage dm = DiscordMessage.builder()
						.username("BanWave") // We are creating a message with the username "LolBans"...
						.avatarUrl(this.WebhookProfilePicture) // with this avatar
						.embeds(Arrays.asList(de)) // with the our embed...
						.build(); // and now we build the message!

						this.ModeratorWebhook.sendMessage(dm);
			}
		}

		public void SendBanWaveAdd(CommandSender sender, OfflinePlayer target, String reason, String PunishID, Timestamp Expiry)
		{
			TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
			{{
				put("PlayerName", target.getName());
				put("PlayerUUID", target.getUniqueId().toString());
				put("ArbiterName", sender.getName());
				put("ArbiterUUID", sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "CONSOLE");
				put("Reason", reason);
				put("FooterText", target.getName());
				if (Expiry != null)
					put("Expiry", Expiry.toString());
				put("PunishID", PunishID);
				put("Applealed", Boolean.toString(false));
			}};
			try
			{
				if (this.UseSimplifiedMessage)
					this.SendFormatted(Messages.Translate("Discord.Simple.BanwaveAdd", Variables));
				else
					this.ModeratorWebhook.sendMessage(this.SendEmbed("Discord.Embedded.BanwaveAdd", Variables));
			}
			catch (InvalidKeyException | InvalidConfigurationException ex)
			{
				ex.printStackTrace();
			}
		}

		public void SendReport(CommandSender sender, OfflinePlayer target, String Reason, String PunishID, String Type)
		{
			TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
			{{
				put("PlayerName", target.getName());
				put("PlayerUUID", target.getUniqueId().toString());
				put("ArbiterName", sender.getName());
				put("ArbiterUUID", sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "CONSOLE");
				put("Reason", Reason);
				put("FooterText", target.getName());
				put("PunishID", PunishID);
				put("ReportType", Type);
			}};
			
			try
			{
				if (this.UseSimplifiedMessage)
				{
					DiscordMessage dm = DiscordMessage.builder()
						.username("LolBans") // We are creating a message with the username "Temmie"...
						.content(Messages.Translate("Discord.Simple.Report", Variables)) // with this content...
						.avatarUrl(this.WebhookProfilePicture) // with this avatar...
						.build(); // and now we build the message!
						
					this.ReportWebhook.sendMessage(dm);
				}
				else
					this.ReportWebhook.sendMessage(this.SendEmbed("Discord.Embedded.ReportTitle", Variables));
			}
			catch (InvalidKeyException | InvalidConfigurationException ex)
			{
				ex.printStackTrace();
			}
		}

		public void SendFormatted(String message)
		{
			DiscordMessage dm = DiscordMessage.builder()
				.username("LolBans") // We are creating a message with the username "Temmie"...
				.content(message) // with this content...
				.avatarUrl(this.WebhookProfilePicture) // with this avatar...
				.build(); // and now we build the message!
					
			this.ModeratorWebhook.sendMessage(dm);
		}
}