/*====================================================================*\

Time.java

Class: time.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.time;

//----------------------------------------------------------------------


// IMPORTS


import java.util.Calendar;

import uk.blankaspect.common.number.NumberUtils;

//----------------------------------------------------------------------


// CLASS: TIME


public class Time
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		String	UTC_TIME_ZONE_STR	= "UTC";

	public static final	char	SEPARATOR_CHAR	= ':';

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	public	int	hour;
	public	int	minute;
	public	int	second;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public Time(int hour,
				int minute,
				int second)
	{
		this.hour = hour;
		this.minute = minute;
		this.second = second;
	}

	//------------------------------------------------------------------

	public Time(Time time)
	{
		hour = time.hour;
		minute = time.minute;
		second = time.second;
	}

	//------------------------------------------------------------------

	public Time(Calendar time)
	{
		hour = time.get(Calendar.HOUR_OF_DAY);
		minute = time.get(Calendar.MINUTE);
		second = time.get(Calendar.SECOND);
	}

	//------------------------------------------------------------------

	/**
	 * @param  str  a string representation of the time, which should have the form "hhmmss".
	 * @throws IllegalArgumentException
	 *           if {@code str} does not have 6 digits.
	 *         NumberFormatException
	 *           if one of the time components of {@code str} is not a valid number.
	 */

	public Time(String str)
	{
		if (str.length() != 6)
			throw new IllegalArgumentException();

		hour = Integer.parseInt(str.substring(0, 2));
		minute = Integer.parseInt(str.substring(2, 4));
		second = Integer.parseInt(str.substring(4));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Time)
		{
			Time Time = (Time)obj;
			return ((hour == Time.hour) && (minute == Time.minute) && (second == Time.second));
		}
		return false;
	}

	//------------------------------------------------------------------

	@Override
	public int hashCode()
	{
		return ((hour << 12) | (minute << 6) | second);
	}

	//------------------------------------------------------------------

	@Override
	public String toString()
	{
		return (NumberUtils.uIntToDecString(hour, 2, '0') + SEPARATOR_CHAR
					+ NumberUtils.uIntToDecString(minute, 2, '0') + SEPARATOR_CHAR
					+ NumberUtils.uIntToDecString(second, 2, '0'));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public boolean isValid()
	{
		return ((hour >= 0) && (hour <= 23) && (minute >= 0) && (minute <= 59) &&
				 (second >= 0) && (second <= 59));
	}

	//------------------------------------------------------------------

	public String toShortString()
	{
		return (NumberUtils.uIntToDecString(hour, 2, '0') + NumberUtils.uIntToDecString(minute, 2, '0')
					+ NumberUtils.uIntToDecString(second, 2, '0'));
	}

	//------------------------------------------------------------------

	public Calendar setInCalendar(Calendar calendar)
	{
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, second);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
