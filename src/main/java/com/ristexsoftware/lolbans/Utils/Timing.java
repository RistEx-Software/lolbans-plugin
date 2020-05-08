package com.ristexsoftware.lolbans.Utils;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import java.util.TreeMap;

/**
 * <h1>Timing Functions</h1>
 * The timing class will measure how long it takes between
 * the class being created to when the Finish() function is
 * called. This allows us to time how long certain operations
 * have taken.
 */
public class Timing 
{
	private Long start = System.currentTimeMillis();
	private Long later = 0L;
	public Timing()
	{
	}

	public Long Finish()
	{
		if (later == 0L)
			later = System.currentTimeMillis();
		return later - start;
	}

	public void Finish(CommandSender sender)
	{
		Timing self = this;
		try
		{
			sender.sendMessage(Messages.Translate("CommandComplete",
				new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
				{{
					put("Milliseconds", Long.toString(self.Finish()));
				}}
			));
		}
		catch (InvalidConfigurationException ex)
		{
			ex.printStackTrace();
		}
	}
}