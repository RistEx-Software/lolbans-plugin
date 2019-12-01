package me.zacherycoleman.lolbans.IPBanning;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.net.InetAddress;
import java.sql.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.sql.rowset.serial.SerialBlob;
import me.zacherycoleman.lolbans.Main;
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
						return Optional.ofNullable(null);

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

					return Optional.ofNullable(null);
				} 
				catch (SQLException e) 
				{
					e.printStackTrace();
					return Optional.ofNullable(null);
				}
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