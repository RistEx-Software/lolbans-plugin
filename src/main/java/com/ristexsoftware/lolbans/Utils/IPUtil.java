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


package com.ristexsoftware.lolbans.Utils;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
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
public class IPUtil 
{
	private static Main self = Main.getPlugin(Main.class);

	/**
	 * Check if an IP falls within a CIDR range
	 * @param ipAddress1 IP Address to check
	 * @param ipAddress The second IP Address to check
	 * @param prefix The prefix to check against
	 * @return true if IP is in prefix, otherwise false
	 */
	public static Boolean checkRange(String ipAddress1, String ipAddress2, String prefix) {
		
		String subnetStr = ipAddress1+"/"+prefix;
		IPAddress subnetAddress = new IPAddressString(subnetStr).getAddress();
		IPAddress subnet = subnetAddress.toPrefixBlock();
		IPAddress testAddress = new IPAddressString(ipAddress2).getAddress();
		return subnet.contains(testAddress);
	}

	/**
	 * Asynchronously check if an IP address is banned
	 * @param address The suspect banned address
	 * @return SQL ResultSet of the banned address, if banned this object will have the returned data from the database or be null.
	 */
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
							PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM lolbans_ipbans WHERE IPAddress = ? AND Appealed = false"); 

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
	/**
	 * Check if the UUID's currently connecting IP address matches one which has already
	 * been banned, making their account an alternate account.
	 * @param address The ip address of the suspect account
	 * @return A UUID of the banned account or null
	 */
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
					ResultSet results = self.connection.prepareStatement("SELECT UUID,IPAddress FROM lolbans_punishments WHERE Type = 0 AND Appealed = False").executeQuery();

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
	 * @param address The address to query for a reverse DNS result.
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

	public static void addIPAddr(String address) {
		IPAddressString addr = new IPAddressString(address);

		// Try and find our address.
		boolean found = false;
		for (IPAddressString cb : Main.BannedAddresses)
		{
			if (cb.compareTo(addr) == 0)
			{ 
				found = true;
				break;
			}
		}

		// Add our banned cidr range if not found.
		if (!found)
			Main.BannedAddresses.add(addr);
	}
}