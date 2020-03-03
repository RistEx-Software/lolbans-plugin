package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

import java.lang.Math;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("unchecked")
public class Paginator<T> 
{
	private T[] objects;
	private Double pagSize;
	private Integer currentPage;
	private Integer amountOfPages;

	public Paginator(T[] objects, Integer max) 
	{
		this.objects = objects;
		pagSize = new Double(max);
	}

	public Paginator(List<T> objects, Integer max) 
	{
		this(objects.toArray((T[]) new Object[0]), max);
	}

	public void SetElements(List<T> objects) 
	{
		this.objects = objects.toArray((T[]) new Object[0]);
	}

	public boolean HasNext() 
	{
		return currentPage < amountOfPages;
	}

	public boolean HasPrev() 
	{
		return currentPage > 1;
	}

	public int GetNext() 
	{
		return currentPage + 1;
	}

	public int GetPrev() 
	{
		return currentPage - 1;
	}

	public List<T> GetPage(Integer pageNum) 
	{
		List<T> page = new ArrayList<>();
		double total = objects.length / pagSize;
		amountOfPages = (int) Math.ceil(total);
		currentPage = pageNum;

		if (objects.length == 0)
			return page;

		double startC = pagSize * (pageNum - 1);
		double finalC = startC + pagSize;

		for (; startC < finalC; startC++)
		{
			if (startC < objects.length)
				page.add(objects[(int) startC]);
		}

		return page;
	}
}
 