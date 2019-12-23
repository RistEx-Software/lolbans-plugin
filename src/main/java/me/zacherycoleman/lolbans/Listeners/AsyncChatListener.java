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

public class AsyncChatListener implements Listener {
    private static Main self = Main.getPlugin(Main.class);

    @EventHandler
    public void OnAsyncPlayerChat(AsyncPlayerChatEvent event) throws InterruptedException, ExecutionException 
    {
        try 
        {
            PreparedStatement ps2 = self.connection.prepareStatement("SELECT * FROM MutedPlayers WHERE Expiry IS NOT NULL AND Expiry <= NOW()");
            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next())
            {
                String name = rs2.getString("PlayerName"), id = rs2.getString("MuteID");
                self.getLogger().info(String.format("Expiring mute on %s (#%s)", name, id));
                DiscordUtil.Send2(name, id);
            }

            self.connection.prepareStatement("DELETE FROM MutedPlayers WHERE Expiry IS NOT NULL AND Expiry <= NOW()").executeUpdate();

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