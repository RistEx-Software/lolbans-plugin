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
		public void SendEmbed(String MessageNode, TreeMap<String, String> Variables) throws InvalidConfigurationException, InvalidKeyException
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

			// Send the message to discord.
			this.ModeratorWebhook.sendMessage(dm.build());
		}

		public void SendDiscord(Punishment p, boolean silent) throws InvalidConfigurationException
		{
				String ExecutionerName = p.IsConsoleExectioner() ? "CONSOLE" : p.GetExecutioner().getName();
				String ExecutionerUUID = p.IsConsoleExectioner() ? "CONSOLE" : p.GetExecutioner().getUniqueId().toString();
				String action = null;

				switch(p.GetPunishmentType())
				{
				   case PUNISH_BAN: action = "Discord.Embedded.BanTitle"; break;
				   case PUNISH_KICK: action = "Discord.Embedded.KickTitle"; break;
				   case PUNISH_MUTE: action = "Discord.Embedded.MuteTitle"; break;
				   case PUNISH_WARN: action = "Discord.Embedded.WarnTitle"; break;
				   default: action = "Discord.Embedded.UnknownTitle"; break;
				}

				try
				{
					this.SendEmbed(action, new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
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
						}}
					);
				}
				catch (InvalidKeyException ex)
				{
					ex.printStackTrace();
				}
				
				//this.SendDiscord(ExecutionerName, action, p.GetPlayerName(), ExecutionerUUID, p.GetUUID().toString(), p.GetReason(), p.GetPunishmentID(), silent);
		}


		public void SendBanWave(String message, String ptbqa)
		{
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
				.avatarUrl(this.WebhookProfilePicture) // with this avatar
				.embeds(Arrays.asList(de)) // with the our embed...
				.build(); // and now we build the message!

				this.ModeratorWebhook.sendMessage(dm);
		}

		public void SendBanWaveAdd(CommandSender sender, OfflinePlayer target, String reason, String PunishID, Timestamp Expiry)
		{
			try
			{
				this.SendEmbed("Discord.Embedded.BanwaveAdd", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
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
					}}
				);
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