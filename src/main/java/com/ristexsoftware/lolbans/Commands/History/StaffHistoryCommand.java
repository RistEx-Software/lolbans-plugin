package com.ristexsoftware.lolbans.Commands.History;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.Paginator;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.sql.*;

public class StaffHistoryCommand extends RistExCommand
{

    private static Main self = Main.getPlugin(Main.class);

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
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        sender.sendMessage(Messages.InvalidSyntax);
        sender.sendMessage("Usage: /staffhistory <staffmember>");
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args) 
    {
        if (!PermissionUtil.Check(sender, "lolbans.staffhistory"))
            return true;

        // Command runs as /staffhistory <staffmember>

        if (args.length < 1)
        {
            sender.sendMessage(Messages.InvalidSyntax);
            return false;
        }

        try 
        {
            // FIXME: What if args[0] == "Console"
            OfflinePlayer target = User.FindPlayerByAny(args[0]);
            if (target == null)
                return User.NoSuchPlayer(sender, args[0], true);

            // Preapre a statement
            // TODO: What about IP bans?
            PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM Punishments WHERE ExecutionerUUID = ?");
            pst.setString(1, target.getUniqueId().toString());

            ResultSet result = pst.executeQuery();
            if (!result.next() || result.wasNull())
                return User.PlayerOnlyVariableMessage("History.NoHistory", sender, args[0], true);


            // The page to use.
            // TODO: WHat if args[1] is a string not an int?
            int pageno = args.length > 1 ? Integer.valueOf(args[1]) : 1;

            // We use a do-while loop because we already checked if there was a result above.
            // NYE!... nye..... bye....go to bed bum or what I'll disconnect your inet bye
            List<String> pageditems = new ArrayList<String>();
            do 
            {
                // First, we have to calculate our punishment type.
                PunishmentType Type = PunishmentType.FromOrdinal(result.getInt("Type"));
                Timestamp ts = result.getTimestamp("TimePunished");

                pageditems.add(Messages.Translate(ts == null ? "History.StaffHistoryMessagePerm" : "History.StaffHistoryMessageTemp", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("playername", result.getString("PlayerName"));
                        put("punishid", result.getString("PunishID"));
                        put("reason", result.getString("Reason"));
                        put("moderator", result.getString("ExecutionerName"));
                        put("type", GodForbidJava8HasUsableLambdaExpressionsSoICanAvoidDefiningSuperflouosFunctionsLikeThisOne(Type, ts));
                        put("expirydate", TimeUtil.TimeString(ts));
                        put("expiryduration", TimeUtil.Expires(ts));
                        // TODO: Add more variables for people who want more info?
                    }}
                ));
            }
            while(result.next());

            // This is several rendered things in one string
            // Minecraft's short window (when chat is closed) can hold 10 lines
            // their extended window can hold 20 lines
            Paginator<String> page = new Paginator<String>(pageditems, Messages.GetMessages().GetConfig().getInt("History.StaffPageSize", 2));

            for (Object str : page.GetPage(pageno))
                sender.sendMessage((String)str);

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

            return true;
        }
        catch (SQLException | InvalidConfigurationException ex)
        {
            ex.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }

}