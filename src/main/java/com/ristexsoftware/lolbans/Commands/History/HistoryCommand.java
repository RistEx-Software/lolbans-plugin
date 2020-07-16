/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *   
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *   
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *   
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 */

package com.ristexsoftware.lolbans.Commands.History;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Objects.GUIs.HistoryGUI;
import com.ristexsoftware.lolbans.Objects.GUI;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.InventoryUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.NumberUtil;
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

        try
        {
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalString("PlayerOrPage", 0);
            a.OptionalString("Page", 0); // I have no clue how or why, but this Just Works:tm: The history classes are the only ones that require this weirdness
            OfflinePlayer target = null;

            // There are two ways this command can work, it can either specify a player, or show all punishments.
            PreparedStatement pst = null;
            if (args.length < 1 || args.length > 0 && NumberUtil.isInteger(a.get("PlayerOrPage")))
                pst = self.connection.prepareStatement("SELECT * FROM lolbans_punishments ORDER BY TimePunished DESC");
            if (args.length > 0 && a.get("PlayerOrPage").length() > 2 && !NumberUtil.isInteger(a.get("PlayerOrPage")))
            {
                target = User.FindPlayerByAny(a.get("PlayerOrPage"));
                pst = self.connection.prepareStatement("SELECT * FROM lolbans_punishments WHERE UUID = ?");
                pst.setString(1, target.getUniqueId().toString());
            }

            // Preapre a statement
            // PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM lolbans_punishments WHERE UUID = ?");
            // pst.setString(1, target.getUniqueId().toString());

            ResultSet result = pst.executeQuery(); // this line????
            if (!result.next() || result.wasNull())
                return User.PlayerOnlyVariableMessage("History.NoHistory", sender, args[0], true);


            // The page to use.
            // I spent a while figuring out the logic to this, and now it Just Works:tm:
            // This is dumb and I don't know if i want to keep this around.
            int pageno = args.length > 0 ? (args.length > 1 ? (NumberUtil.isInteger(a.get("Page")) ? Integer.valueOf(a.get("Page")) : 0) : NumberUtil.isInteger(a.get("PlayerOrPage")) ? Integer.valueOf(a.get("PlayerOrPage")) : 1 ) : 1;
            if (pageno == 0)
                return false;

            if (self.getConfig().getBoolean("General.HistoryGUI")) {
                HistoryGUI gui = new HistoryGUI(45, self);
                gui.BuildGUI((Player) sender, args, a);
                return true;
            }
            
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
                        put("expiry", ts == null ? "" : ts.toString());
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