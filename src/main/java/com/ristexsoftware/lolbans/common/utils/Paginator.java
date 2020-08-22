/* 
 *  LolBans - An advanced punishment management system made for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ristexsoftware.lolbans.common.utils; 
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

	/**
	 * Create a pagination of the object array
	 * @param objects array of objects to paginate
	 * @param max maximum number of objects per page
	 */
	public Paginator(T[] objects, Integer max) 
	{
		this.objects = objects;
		this.pagSize = new Double(max);
		this.amountOfPages = (int) Math.ceil(objects.length / pagSize);
	}

	/**
	 * Create a pagination of the list of objects
	 * @param objects an object list
	 * @param max maximum number of objects per page
	 */
	public Paginator(List<T> objects, Integer max) 
	{
		this(objects.toArray((T[]) new Object[0]), max);
	}

	/**
	 * Set the paginated objects to the list below
	 * @param objects objects to replace the existing object list for pagination
	 */
	public void SetElements(List<T> objects) 
	{
		this.objects = objects.toArray((T[]) new Object[0]);
		this.amountOfPages = (int) Math.ceil(objects.size() / pagSize);
	}

	/**
	 * Test if the paginator has another page.
	 * @return True if there's another page
	 */
	public boolean HasNext() 
	{
		return currentPage < amountOfPages;
	}

	/**
	 * Test if the paginator has a previous page
	 * @return True if there is a previous page
	 */
	public boolean HasPrev() 
	{
		return currentPage > 1;
	}

	/**
	 * Get the next page number
	 * @return Next page number
	 */
	public int GetNext() 
	{
		return currentPage + 1;
	}

	/**
	 * Get the previous page number
	 * @return Previous page number
	 */
	public int GetPrev() 
	{
		return currentPage - 1;
	}

	/**
	 * Get the current page number
	 * @return current page number
	 */
	public int GetCurrent()
	{
		return currentPage;
	}

	/**
	 * Get the total number of pages for the objects in the array
	 * @return total number of pages
	 */
	public int GetTotalPages()
	{
		return this.amountOfPages;
	}

	/**
	 * Get the objects for this page
	 * @param pageNum the page number.
	 * @return List of objects that make up this page
	 */
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
 