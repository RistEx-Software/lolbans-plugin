package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

import java.util.zip.CRC32;

class Luhn 
{
	/**
	 * Checks if the card is valid
	 * 
	 * @param card
	 *           {@link String} card number
	 * @return result {@link boolean} true of false
	 */
    public static boolean LuhnCheck(String card) 
    {
		if (card == null)
            return false;
            
		char checkDigit = card.charAt(card.length() - 1);
		String digit = CalculateCheckDigit(card.substring(0, card.length() - 1));
		return checkDigit == digit.charAt(0);
	}
	
	/**
	 * Calculates the last digits for the card number received as parameter
	 * 
	 * @param card
	 *           {@link String} number
	 * @return {@link String} the check digit
	 */
    public static String CalculateCheckDigit(String card)
    {
		if (card == null)
            return null;
            
		String digit;
		/* convert to array of int for simplicity */
        int[] digits = new int[card.length()];
        
        for (int i = 0; i < card.length(); i++)
			digits[i] = Character.getNumericValue(card.charAt(i));
		
		/* double every other starting from right - jumping from 2 in 2 */
        for (int i = digits.length - 1; i >= 0; i -= 2)	
        {
			digits[i] += digits[i];
			
			/* taking the sum of digits grater than 10 - simple trick by substract 9 */
			if (digits[i] >= 10) 
				digits[i] = digits[i] - 9;
        }
        
		int sum = 0;
		for (int i = 0; i < digits.length; i++) 
            sum += digits[i];
            
		/* multiply by 9 step */
		sum = sum * 9;
		
		/* convert to string to be easier to take the last digit */
		digit = sum + "";
		return digit.substring(digit.length() - 1);
    }
}

public class PunishID
{
    public static String GenerateID(int idstart)
    {
        CRC32 crc = new CRC32();

        // Time of the ban
        crc.update((int)(System.currentTimeMillis() / 1000L));
        // Some kind of semi-unique identifier (like last database row insertion)
        crc.update(idstart);

        // Get the hash
        String crc32hash = Long.toHexString(crc.getValue());
        // Calculate the Luhn check digit
        crc32hash += Luhn.CalculateCheckDigit(crc32hash);
        // return.
        return crc32hash.toUpperCase();
    }

    public static boolean ValidateID(String id)
    {
        return Luhn.LuhnCheck(id);
    }
}