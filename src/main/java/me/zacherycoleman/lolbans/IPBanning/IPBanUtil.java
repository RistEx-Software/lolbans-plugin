package me.zacherycoleman.lolbans.IPBanning;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.net.InetAddress;
import java.sql.*;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.sql.rowset.serial.SerialBlob;
import me.zacherycoleman.lolbans.Main;


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

					IPAddress addr = hn.asAddress();

					synchronized(Main.BannedAddresses)
					{
						for (IPAddressString cb : Main.BannedAddresses)
						{
							// They're a banned cidr, query for the reason and kick them.
							if (cb.contains(addr.toAddressString()))
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


}