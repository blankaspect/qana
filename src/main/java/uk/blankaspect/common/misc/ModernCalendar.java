/*====================================================================*\

ModernCalendar.java

Modern calendar class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.misc;

//----------------------------------------------------------------------


// IMPORTS


import java.util.GregorianCalendar;

import java.util.stream.Collectors;

import uk.blankaspect.common.date.Date;

//----------------------------------------------------------------------


// MODERN CALENDAR CLASS


/**
 * This is a pure Gregorian calendar: there is no point at which it switches from Julian dates to Gregorian
 * dates.
 */

public class ModernCalendar
	extends GregorianCalendar
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public ModernCalendar()
	{
		_init();
	}

	//------------------------------------------------------------------

	public ModernCalendar(int year,
						  int month,
						  int day)
	{
		super(year, month, day);
		_init();
	}

	//------------------------------------------------------------------

	public ModernCalendar(int year,
						  int month,
						  int day,
						  int hour,
						  int minute,
						  int second)
	{
		super(year, month, day, hour, minute, second);
		_init();
	}

	//------------------------------------------------------------------

	public ModernCalendar(long milliseconds)
	{
		_init();
		setTimeInMillis(milliseconds);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Converts the specified string representation of a date to an instance of {@code ModernCalendar} and
	 * returns the result.  The format of the input string is flexible, but it must contain exactly eight
	 * decimal digit characters in the range U+0030 to U+0039.  Any non-digit characters in the input string
	 * are removed, and the resulting digits are parsed as <i>yyyymmdd</i>, where <i>yyyy</i> is the year,
	 * <i>mm</i> is the month and <i>dd</i> is the day of the month.
	 * @param  str  the string that will be parsed as a calendar date.
	 * @return the result of converting {@code str} to a calendar date.
	 */
	public static ModernCalendar fromDateString(String str)
	{
		// Validate argument
		if (str == null)
			throw new IllegalArgumentException();

		// Remove non-decimal-digit characters from string
		str = str.chars()
					.filter(ch -> (ch >= '0') && (ch <= '9'))
					.mapToObj(ch -> Character.toString((char)ch))
					.collect(Collectors.joining());

		// Convert string to date and thence to calendar, and return calendar
		return new Date(str).toCalendar();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private void _init()
	{
		setGregorianChange(new java.util.Date(Long.MIN_VALUE));
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
