package com.ristexsoftware.lolbans.Commands.History;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.Paginator;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.List;
import java.sql.*;

public class HistoryCommand extends RistExCommand
{
    private Main self = (Main)this.getPlugin();

    public HistoryCommand(Plugin owner)
    {
        super("history", owner);
        this.setDescription("Get history of player punishments");
        this.setPermission("lolbans.history");
        this.setAliases(Arrays.asList(new String[]{"h"}));
    }

    private String GodForbidJava8HasUsableLambdaExpressionsSoICanAvoidDefiningSuperflouosFunctionsLikeThisOne(PunishmentType Type, Timestamp ts)
    {
        if (Type == PunishmentType.PUNISH_BAN)
        {
            // Check if ts is null.
            if (ts == null)
                return "Permanent Ban";
            else 
                return "Temporary Ban";
        }
        else 
            return Type.DisplayName();
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.History", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.history"))
            return User.PermissionDenied(sender, "lolbans.history");

        if (args.length < 1)
            return false; // Show syntax.

        try
        {
            OfflinePlayer target = User.FindPlayerByAny(args[0]);

            if (target == null)
                return User.NoSuchPlayer(sender, args[0], true);

            // Preapre a statement
            PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM Punishments WHERE UUID = ?");
            pst.setString(1, target.getUniqueId().toString());

            ResultSet result = pst.executeQuery();
            if (!result.next() || result.wasNull())
                return User.PlayerOnlyVariableMessage("History.NoHistory", sender, args[0], true);


            // The page to use.
            // TODO: WHat if args[1] is a string not an int?
            int pageno = args.length > 1 ? Integer.valueOf(args[1]) : 1;
            
            // We use a do-while loop because we already checked if there was a result above.
            List<String> pageditems = new ArrayList<String>();
            do 
            {
                // First, we have to calculate our punishment type.
                PunishmentType Type = PunishmentType.FromOrdinal(result.getInt("Type"));
                Timestamp ts = result.getTimestamp("Expiry");

                pageditems.add(Messages.Translate(ts == null ? "History.HistoryMessagePerm" : "History.HistoryMessageTemp", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("playername", result.getString("PlayerName"));
                        put("punishid", result.getString("PunishID"));
                        put("reason", result.getString("Reason"));
                        put("arbiter", result.getString("ArbiterName"));
                        put("type", GodForbidJava8HasUsableLambdaExpressionsSoICanAvoidDefiningSuperflouosFunctionsLikeThisOne(Type, ts));
                        put("date", result.getTimestamp("TimePunished").toString());
                        if (ts != null)
                            put("expiry", ts.toString());
                        // TODO: Add more variables for people who want more info?
                    }}
                ));
            }
            while(result.next());
            
            
            // This is several rendered things in one string
            // Minecraft's short window (when chat is closed) can hold 10 lines
            // their extended window can hold 20 lines
            Paginator<String> page = new Paginator<String>(pageditems, Messages.GetMessages().GetConfig().getInt("History.PageSize", 2));

            // Minecraft trims whitespace which can cause formatting issues
            // To avoid this, we have to send everything as one big message.
            String Message = "";
            for (Object str : page.GetPage(pageno))
                Message += (String)str;

            sender.sendMessage(Message);

            // Check if the paginator needs the page text or not.
            if (page.GetTotalPages() > 1)
            {
                sender.sendMessage(Messages.Translate("History.Paginator",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("current", String.valueOf(page.GetCurrent()));
                        put("total", String.valueOf(page.GetTotalPages()));
                    }}
                ));
            }
        }
        catch (SQLException | InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}