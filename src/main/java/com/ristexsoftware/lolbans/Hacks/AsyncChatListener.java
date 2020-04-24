package com.ristexsoftware.lolbans.Hacks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

public class AsyncChatListener 
{
    private static Main self = Main.getPlugin(Main.class);

    public static void OnAsyncPlayerChat(AsyncPlayerChatEvent event) throws InterruptedException, ExecutionException 
    {
        try 
        {
            // Send a message to the user if the chat is muted globally
            if (self.ChatMuted)
            {
                if (event.getPlayer().hasPermission("lolbans.mute.bypass"))
                    return;
                event.setCancelled(true);
                event.getPlayer().sendMessage(Messages.Translate("Mute.GlobalMuted", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
                return;
            }

            // Otherwise check if they're individually muted.
            PreparedStatement MuteStatement = self.connection.prepareStatement(
                    "SELECT * FROM Punishments WHERE UUID = ? AND Type = ? AND (Expiry IS NULL OR Expiry >= NOW()) AND Appealed = False");
            MuteStatement.setString(1, event.getPlayer().getUniqueId().toString());
            MuteStatement.setInt(2, PunishmentType.PUNISH_MUTE.ordinal());

            Future<Optional<ResultSet>> MuteRecord = DatabaseUtil.ExecuteLater(MuteStatement);
            Optional<ResultSet> MuteResult = MuteRecord.get();
            // The query was successful.
            if (MuteResult.isPresent()) 
            {
                ResultSet result = MuteResult.get();
                // They're muted. don't let them speak...
                if (result.next()) 
                {
                    event.setCancelled(true);
                    Timestamp MuteTime = result.getTimestamp("Expiry");

                    event.getPlayer().sendMessage(Messages.Translate("Mute.YouAreMuted",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) 
                        {{
                            put("player", event.getPlayer().getName());
                            put("reason", result.getString("Reason"));
                            put("arbiter", result.getString("ArbiterName"));
                            put("punishid", result.getString("PunishID"));
                            put("expiry", MuteTime != null ? MuteTime.toString() : "Never");
                        }}
                    ));
                }
            }
        }
        catch (SQLException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }
}