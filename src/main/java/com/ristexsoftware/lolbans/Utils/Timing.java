package com.ristexsoftware.lolbans.Utils;

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
}