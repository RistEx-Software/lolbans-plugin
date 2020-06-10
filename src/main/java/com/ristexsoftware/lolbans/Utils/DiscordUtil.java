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
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
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

		/**
		 * Create a DiscordUtil object against the webhooks to announce reports and punishments on.
		 * @param ModeratorWebhook The webhook to send reports to
		 * @param ReportWebhook The webhook to send punishment announcements to
		 */
		public DiscordUtil(String ModeratorWebhook, String ReportWebhook)
		{
			// TODO: handle /reload better?
			DiscordUtil.me = this;
			this.reload(ModeratorWebhook, ReportWebhook);
		}

		/**
		 * Get the DiscordUtil object currently allocated.
		 * @return DiscordUtil object
		 */
		public static DiscordUtil GetDiscord() { return DiscordUtil.me; }
		
		/**
		 * Change the webhooks in use with new ones.
		 * @param ModeratorWebhook The new webhook for punishment announcements
		 * @param ReportWebhook The new webhook for report announcements
		 */
		public void reload(String ModeratorWebhook, String ReportWebhook)
		{
			this.ModeratorWebhook = new TemmieWebhook(ModeratorWebhook);
			this.ReportWebhook = new TemmieWebhook(ReportWebhook);
		}

		// message.yml
		// - Title
		// - Expiry

		/**
		 * Send a discord embed in accordance to the variables specified
		 * <br>
		 * Variables must contain the following:
		 * - PlayerUUID  -- The UUID of the person being punished
		 * - ArbiterName -- The Name of the person who executes the command
		 * - ArbiterUUID -- The UUID of the person who executes the command
		 * - Reason      -- The primary text to make part of the embed.
		 * - FooterText  -- Text to show on the footer
		 * - Expiry      -- If present, will add the "expires" field
		 * - PunishID    -- If present, will add the "PunishID" field
		 * - ReportType  -- If present, will add the "Type" field
		 * @param MessageNode The title of the embed from messages.yml
		 * @param Variables Variables to use for both placeholders and for parts of the embed.
		 * @return A {@link com.mrpowergamerbr.temmiewebhook.DiscordMessage} generated object for sending to a Discord webhook.
		 * @throws InvalidConfigurationException When the messages.yml node does not exist
		 * @throws InvalidKeyException When a key is not provided but required to be in the Variables parameter.
		 */
	  private
		DiscordMessage SendEmbed(String MessageNode, TreeMap<String, String> Variables) throws InvalidConfigurationException,
			InvalidKeyException
		{
			if (!Variables.containsKey("PlayerUUID") || !Variables.containsKey("ArbiterName") || !Variables.containsKey("ArbiterUUID") || !Variables.containsKey("FooterText") || !Variables.containsKey("Reason"))
				throw new InvalidKeyException("Missing a required key for Discord Embeds.");

			// Dynamically add the various fields we may need
			List<FieldEmbed> Fields = new ArrayList<FieldEmbed>();

			// TODO: Find a reasonable way to messages.yml-ify this?
			if (Variables.containsKey("PunishID"))
				Fields.add(FieldEmbed.builder().name("PunishID").value("#" + Variables.get("PunishID")).build());
			if (Variables.containsKey("Expiry"))
				Fields.add(FieldEmbed.builder().name("Expires").value(Variables.get("Expiry")).build());
			if (Variables.containsKey("ReportType"))
				Fields.add(FieldEmbed.builder().name("Type").value(Variables.get("ReportType")).build());

			// Start a discord embed builder
			DiscordEmbedBuilder build = DiscordEmbed.builder();
			// Set the color based on the PunishID (if available)
			if (Variables.containsKey("PunishID"))
				build.color(Integer.parseInt(Variables.get("PunishID").replaceAll("[^0-9]", "")));

			build.title(Messages.Translate(MessageNode, Variables));
			build.description(Variables.get("Reason"));
			build.fields(Fields);
			build.thumbnail(ThumbnailEmbed.builder().url(Messages.TranslateNC("Discord.ThumbnailPictures", Variables)).build());
			build.footer(FooterEmbed.builder().text(Variables.get("FooterText")).icon_url(Messages.TranslateNC("Discord.AvatarPictures", Variables)).build());

			// Get the Avatar URL from the config. We provide the PlayerUUID and the ArbiterUUID
			String AvatarURL = "";
			if (!Variables.get("ArbiterUUID").equalsIgnoreCase("CONSOLE"))
			{
				AvatarURL = Messages.TranslateNC("Discord.AvatarPictures",
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

		/**
		 * Send a discord message using the data from the punishment object.
		 * @param p The punishment object to send to discord
		 * @param silent Whether this punishment was silent
		 * @throws InvalidConfigurationException If the discord messages cannot be found in messages.yml
		 */
		public void SendDiscord(Punishment p, boolean silent) throws InvalidConfigurationException
		{
			if (!Messages.Discord) return;
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
					put("Expiry", p.GetExpiry() == null ? "" : p.GetExpiry().toString()); 
					put("PunishID", p.GetPunishmentID());
					put("appealed", Boolean.toString(p.GetAppealed()));
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

		/**
		 * Send a discord message formatted for an object being punished (such as a Regular Expression or IP ban)
		 * @param sender Who is performing the action
		 * @param Object The object expression being punished
		 * @param Reason The reason for the punishment
		 * @param PunishID The ID of this punishment
		 * @param Expiry When this ban expires (if applicable, may be null if never expires)
		 */
		public void SendBanObject(CommandSender sender, String Object, String Reason, String PunishID, Timestamp Expiry)
		{
			if (!Messages.Discord) return;
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
						AvatarURL = Messages.TranslateNC("Discord.AvatarPictures",
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

		/**
		 * Send a message to discord saying that a banwave has been initiated.
		 * @param ArbiterName The person who started the ban wave
		 * @param UserList The formatted list of players being banned to include with the message
		 * @throws InvalidConfigurationException If the messages.yml node doesn't exist.
		 */
		public void SendBanWave(String ArbiterName, String UserList) throws InvalidConfigurationException
		{
			if (!Messages.Discord) return;
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

		/**
		 * Send a message to discord stating that someone was added to a ban wave.
		 * @param sender The person who executed the command
		 * @param target The person being added to the ban wave
		 * @param reason The reason they're being banned.
		 * @param PunishID The Punishment ID assigned to their ban
		 * @param Expiry The expiration time (if applicable, may be null of never)
		 */
		public void SendBanWaveAdd(CommandSender sender, OfflinePlayer target, String reason, String PunishID, Timestamp Expiry)
		{
			if (!Messages.Discord) return;
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

		/**
		 * Send a message to discord saying a report has been filed against a player.
		 * @param sender The person filing the report
		 * @param target The person the report is filed against
		 * @param Reason The reason for the report
		 * @param PunishID The punishment id of the report
		 * @param Type The type of report (based on the config)
		 */
		public void SendReport(CommandSender sender, OfflinePlayer target, String Reason, String PunishID, String Type)
		{
			if (!Messages.Discord) return;
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

		/**
		 * Send a basic formatted message to discord.
		 * @param message Message to send.
		 * @deprecated This should not be used.
		 */
		public void SendFormatted(String message)
		{
			if (!Messages.Discord) return;
			DiscordMessage dm = DiscordMessage.builder()
				.username("LolBans") // We are creating a message with the username "Temmie"...
				.content(message) // with this content...
				.avatarUrl(this.WebhookProfilePicture) // with this avatar...
				.build(); // and now we build the message!
					
			this.ModeratorWebhook.sendMessage(dm);
		}
}