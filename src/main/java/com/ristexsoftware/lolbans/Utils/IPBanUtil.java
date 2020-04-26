package com.ristexsoftware.lolbans.Utils;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddressString;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import com.ristexsoftware.lolbans.Main;

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

					return Optional.empty();
				} 
				catch (SQLException e) 
				{
					e.printStackTrace();
					return Optional.empty();
				}
			}
		});

		Main.pool.execute(t);

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
					ResultSet results = self.connection.prepareStatement("SELECT UUID,IPAddress FROM Punishments WHERE Type = 0 AND Appealed = False").executeQuery();

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

		Main.pool.execute(t);
		return t;
	}

	/**
	 * Query the Reverse DNS of an address and return the DNS result
	 * as a string.
	 * @return String with the DNS result or null
	 */
	public static String rDNSQUery(String address)
	{
		try
		{
			InetAddress ia = InetAddress.getByName(address);
			return ia.getCanonicalHostName();
		}
		catch (UnknownHostException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
}