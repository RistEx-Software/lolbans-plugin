package com.ristexsoftware.lolbans.IPBanning;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.net.InetAddress;
import java.sql.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.sql.rowset.serial.SerialBlob;
import com.ristexsoftware.lolbans.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// Javadocs for IPAddress: https://seancfoley.github.io/IPAddress/IPAddress/apidocs/
public class IPBanUtil 
{
	private static Main self = Main.getPlugin(Main.class);
	
	public static Future<Optional<ResultSet>> IsBanned(InetAddress address)
	{
		FutureTask<Optional<ResultSet>> t = new FutureTask<>(new Callable<Optional<ResultSet>>()
		{
			@Override
			public Optional<ResultSet> call()
			{
				//This is where you should do your database interaction
				try 
				{
					// Invalid IP address (unlikely but worth a shot)
					HostName hn = new HostName(address);
					if (!hn.isAddress())
						return Optional.empty();

					synchronized(Main.BannedAddresses)
					{
						for (IPAddressString cb : Main.BannedAddresses)
						{
							// They're a banned cidr, query for the reason and kick them.
							if (cb.contains(hn.asAddressString()))
							{
								PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM IPBans WHERE IPAddress = ?");
								pst.setString(1, cb.toString());
				
								ResultSet res = pst.executeQuery();
								return Optional.of(res);
							}
						}
					}

					return Optional.empty();
				} 
				catch (SQLException e) 
				{
					e.printStackTrace();
					return Optional.empty();
				}
			}
		});

		self.pool.execute(t);

		return t;
	}

	// Check if a UUID's currently connecting IP address matches one which has
	// already been banned, making their account an alternate account.
	// This can enforce additional bans or help identify alts
	public static Future<UUID> CheckAlts(InetAddress address)
	{
		FutureTask<UUID> t = new FutureTask<>(new Callable<UUID>()
		{
			@Override
			public UUID call()
			{
				HostName hn = new HostName(address);
				if (!hn.isAddress())
					return null;

				try
				{
					// Now query the database for ALL ip addresses and we have to check each one.
					// TODO: Left outter join BanWaves too?
					ResultSet results = self.connection.prepareStatement("SELECT UUID,IPAddress FROM BannedPlayers").executeQuery();

					// Iterate the results and check to see if anyone matches.
					while (results.next())
					{
						HostName h = new HostName(results.getString("IPAddress"));
						if (!h.isAddress())
							continue;

						// They match (either part of a CIDR or an exact address)
						if (h.asAddressString().contains(hn.asAddressString()))
							return UUID.fromString(results.getString("UUID"));
					}
				}
				catch (SQLException ex)
				{
					ex.printStackTrace();
				}

				return null;
			}
		});

		self.pool.execute(t);
		return t;
	}

	/**
	 * Calculate the percentage of users that will be banned using
	 * this CIDR. Useful for thresholds when a ban may be set but
	 * it would be wildly overreaching and should be avoided.
	 * @return
	 */
	public static float CalculateBanPercentage(IPAddressString address)
	{
		// Iterate the users and compare their addresses to the CIDR given to us
		int TotalAffected = 0;
		Collection<? extends Player> Players = Bukkit.getOnlinePlayers();
		for (Player p : Players)
		{
			HostName hn = new HostName(p.getAddress());

			if (address.contains(hn.asAddressString()))
				TotalAffected++;
		}

		float percentage = (TotalAffected / Players.size()) * 100.0f;
		return percentage;
	}
}