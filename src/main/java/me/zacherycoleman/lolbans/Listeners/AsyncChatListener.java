package me.zacherycoleman.lolbans.Listeners;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.DatabaseUtil;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.Messages;
import me.zacherycoleman.lolbans.Utils.TimeUtil;

public class AsyncChatListener implements Listener 
{
    private static Main self = Main.getPlugin(Main.class);

    @EventHandler
    public void OnAsyncPlayerChat(AsyncPlayerChatEvent event) throws InterruptedException, ExecutionException 
    {
        try 
        {
            // Send a message to the user if the chat is muted globally
            if (self.ChatMuted)
            {
                if (event.getPlayer().hasPermission("lolbans.mute.bypass") || event.getPlayer().isOp())
                    return;
                event.setCancelled(true);
                event.getPlayer().sendMessage(Messages.GetMessages().GetConfig().getString("Mute.GlobalMuted"));
                return;
            }

            // Otherwise check if they're individually muted.
            PreparedStatement MuteStatement = self.connection.prepareStatement(
                    "SELECT * FROM MutedPlayers WHERE UUID = ? AND (Expiry IS NULL OR Expiry >= NOW())");
            MuteStatement.setString(1, event.getPlayer().getUniqueId().toString());

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
                    String YouAreMuted;
                    try 
                    {
                        YouAreMuted = Messages.GetMessages().Translate("Mute.YouAreMuted",
                            new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) 
                            {{
                                put("prefix", Messages.Prefix);
                                put("player", event.getPlayer().getName());
                                put("reason", result.getString("Reason"));
                                put("banner", result.getString("Executioner"));
                                put("muteid", result.getString("MuteID"));
                                put("fullexpiry",MuteTime != null ? String.format("%s (%s)",TimeUtil.TimeString(MuteTime), TimeUtil.Expires(MuteTime)) : "Never");
                                put("expiryduration", MuteTime != null ? TimeUtil.Expires(MuteTime) : "Never");
                                put("dateexpiry", MuteTime != null ? TimeUtil.TimeString(MuteTime) : "Never");
                            }});
                        event.getPlayer().sendMessage(YouAreMuted);
                    } 
                    catch (InvalidConfigurationException e) 
                    {
                        e.printStackTrace();
                    }
                    //(String message, String ColorChars, Map<String, String> Variables)
                    
                    return;
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}