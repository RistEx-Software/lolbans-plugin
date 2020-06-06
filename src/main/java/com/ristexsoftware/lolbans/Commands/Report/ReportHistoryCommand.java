package com.ristexsoftware.lolbans.Commands.Report;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.Paginator;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.List;
import java.sql.*;

public class ReportHistoryCommand extends RistExCommand
{
    private Main self = (Main)this.getPlugin();

    public ReportHistoryCommand(Plugin owner)
    {
        super("reports", owner);
        this.setDescription("Get latest reports for a specific player or everyone");
        this.setPermission("lolbans.report.history");
        this.setAliases(Arrays.asList(new String[]{"rps"}));
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.ReportHistory", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.report.history"))
            return User.PermissionDenied(sender, "lolbans.report.history");

        try
        {
            // There are two ways this command can work, it can either specify a player, or show all reports.
            PreparedStatement pst = null;
            if (args.length < 1)
                pst = self.connection.prepareStatement("SELECT * FROM Reports ORDER BY Closed, TimeAdded");
            else 
            {
                OfflinePlayer target = User.FindPlayerByAny(args[0]);
                pst = self.connection.prepareStatement("SELECT * FROM Reports WHERE DefendantUUID = ?");
                pst.setString(1, target.getUniqueId().toString());
            }

            ResultSet result = pst.executeQuery();
            if (!result.next() || result.wasNull())
                return User.PlayerOnlyVariableMessage("History.NoHistory2", sender, args[0], true);


            // The page to use.
            int pageno = args.length > 1 ? Integer.valueOf(args[1]) : 1;
            
            // We use a do-while loop because we already checked if there was a result above.
            List<String> pageditems = new ArrayList<String>();
            do 
            {
                // First, we have to calculate our punishment type.
                //PunishmentType Type = PunishmentType.FromOrdinal(result.getInt("Type"));
                //Timestamp ts = result.getTimestamp("Expiry");

                pageditems.add(Messages.Translate("History.HistoryMessageReport", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("playername", result.getString("DefendantName"));
                        put("reason", result.getString("Reason"));
                        put("arbiter", result.getString("PlaintiffName"));
                        put("date", result.getTimestamp("TimeAdded").toString());
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
