package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddressString;

import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.IPBanUtil;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

public class UnIPBanCommand extends RistExCommandAsync
{
	private Main self = (Main)this.getPlugin();
	
	public UnIPBanCommand(Plugin owner)
    {
        super("unipban", owner);
        this.setDescription("Remove an IP or CIDR range ban");
		this.setPermission("lolbans.ban");
		this.setAliases(Arrays.asList(new String[] { "unbanip" }));
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.IPUnban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.ipunban"))
            return User.PermissionDenied(sender, "lolbans.ipunban");
        
        // Syntax: /unipban [-s] <CIDR|PunishID> <Reason>
        try 
		{
			ArgumentUtil a = new ArgumentUtil(args);
			a.OptionalFlag("Silent", "-s");
			a.RequiredString("CIDR", 0);
			a.RequiredSentence("Reason", 1);

			if (!a.IsValid())
				return false;

            boolean silent = a.get("-s") != null;
            String CIDR = a.get("CIDR");
            String reason = a.get("Reason");
			PreparedStatement ps = null;
			ResultSet res = null;


			if (PunishID.ValidateID(CIDR.replaceAll("#", "")))
			{
				ps = self.connection.prepareStatement("SELECT * FROM IPBans WHERE PunishID = ?");
				ps.setString(1, CIDR);
				Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(ps).get();
				if (!ores.isPresent())
					return User.PlayerOnlyVariableMessage("IPBan.IPIsNotBanned", sender, CIDR, true);

				res = ores.get();
			}
			else
			{
				HostName hs = new HostName(CIDR);
				Optional<ResultSet> ores =  IPBanUtil.IsBanned(hs.asInetAddress()).get();
				if (!ores.isPresent())
					return User.PlayerOnlyVariableMessage("IPBan.IPIsNotBanned", sender, CIDR, true);

				res = ores.get();
			}

            res.next();

            int i = 1;
            ps = self.connection.prepareStatement("UPDATE IPBans SET AppealReason = ?, AppelleeName = ?, AppelleeUUID = ?, AppealTime = CURRENT_TIMESTAMP, Appealed = TRUE WHERE id = ?");
            ps.setString(i++, reason);
            ps.setString(i++, sender.getName());
            ps.setString(i++, sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "CONSOLE");
            ps.setInt(i++, res.getInt("id"));
			DatabaseUtil.ExecuteUpdate(ps);


			// Remove the banned address from BannedAddresses
			IPAddressString addr = new IPAddressString(res.getString("IPAddress"));
			for (IPAddressString anotheraddress : Main.BannedAddresses)
			{
				if (anotheraddress.equals(addr))
				{
					Main.BannedAddresses.remove(anotheraddress);
					break;
				}
			}


			// Prepare our announce message
			final String IPAddress = res.getString("IPAddress");
			final String PunishID = res.getString("PunishID");
            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("ipaddress", IPAddress);
				put("arbiter", sender.getName());
				put("Reason", reason);
                put("punishid", PunishID);
                put("silent", Boolean.toString(silent));
			}};
			
			sender.sendMessage(Messages.Translate("IPBan.UnbanSuccess", Variables));
            
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("IPBan.UnbanAnnouncement", Variables));
            // TODO: DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        }
        catch (InvalidConfigurationException | SQLException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}