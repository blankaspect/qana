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

	/** The number of digits in the <i>hour</i> component of the string representation of a time. */
	public static final		int		NUM_HOUR_DIGITS		= 2;

	/** The number of digits in the <i>minute</i> omponent of the string representation of a time. */
	public static final		int		NUM_MINUTE_DIGITS	= 2;

	/** The number of digits in the <i>second</i> omponent of the string representation of a time. */
	public static final		int		NUM_SECOND_DIGITS	= 2;

	/** The character that separates the components of the string representation of a time. */
	public static final		char	SEPARATOR_CHAR	= ':';

	/** Miscellaneous strings. */
	private static final	String	MALFORMED_TIME_STR	= "Malformed time";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	int	hour;
	private	int	minute;
	private	int	second;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public Time(
		int	hour,
		int	minute,
		int	second)
	{
		this.hour = hour;
		this.minute = minute;
		this.second = second;
	}

	//------------------------------------------------------------------

	public Time(
		Time	time)
	{
		hour = time.hour;
		minute = time.minute;
		second = time.second;
	}

	//------------------------------------------------------------------

	public Time(
		Calendar	time)
	{
		hour = time.get(Calendar.HOUR_OF_DAY);
		minute = time.get(Calendar.MINUTE);
		second = time.get(Calendar.SECOND);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of a time from the specified string representation, which should have the form
	 * <i>hhmmss</i> or <i>hh</i>:<i>mm</i>:<i>ss</i>, where <i>h</i>, <i>m</i> and <i>s</i> are decimal digits that
	 * denote the hour, minute and second respectively.
	 *
	 * @param  text
	 *           a string representation of the time.
	 * @throws IllegalArgumentException
	 *           if {@code text} is malformed.
	 *         NumberFormatException
	 *           if one of the time components of {@code text} is not a valid number.
	 */

	public Time(
		String	text)
	{
		// Test length of input string
		int inLength = text.length();
		int expectedLength = NUM_HOUR_DIGITS + NUM_MINUTE_DIGITS + NUM_SECOND_DIGITS;
		if ((inLength != expectedLength) && (inLength != expectedLength + 2))
			throw new IllegalArgumentException(MALFORMED_TIME_STR);

		// Parse hour
		int offset = 0;
		int length = NUM_HOUR_DIGITS;
		hour = Integer.parseInt(text.substring(offset, offset + length));
		offset += length;

		// Skip optional separator between hour and minute
		if ((inLength > expectedLength) && (text.charAt(offset++) != SEPARATOR_CHAR))
			throw new IllegalArgumentException(MALFORMED_TIME_STR);

		// Parse minute
		length = NUM_MINUTE_DIGITS;
		minute = Integer.parseInt(text.substring(offset, offset + length));
		offset += length;

		// Skip optional separator between minute and second
		if ((inLength > expectedLength) && (text.charAt(offset++) != SEPARATOR_CHAR))
			throw new IllegalArgumentException(MALFORMED_TIME_STR);

		// Parse second
		length = NUM_SECOND_DIGITS;
		second = Integer.parseInt(text.substring(offset));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public boolean equals(
		Object	obj)
	{
		if (this == obj)
			return true;

		return (obj instanceof Time other) && (hour == other.hour) && (minute == other.minute)
				&& (second == other.second);
	}

	//------------------------------------------------------------------

	@Override
	public int hashCode()
	{
		return (hour << 12) | (minute << 6) | second;
	}

	//------------------------------------------------------------------

	@Override
	public String toString()
	{
		return NumberUtils.uIntToDecString(hour, 2, '0') + SEPARATOR_CHAR
				+ NumberUtils.uIntToDecString(minute, 2, '0') + SEPARATOR_CHAR
				+ NumberUtils.uIntToDecString(second, 2, '0');
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the hour of this time.
	 *
	 * @return the hour of this time.
	 */

	public int getHour()
	{
		return hour;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the minute of this time.
	 *
	 * @return the minute of this time.
	 */

	public int getMinute()
	{
		return minute;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the second of this time.
	 *
	 * @return the second of this time.
	 */

	public int getSecond()
	{
		return second;
	}

	//------------------------------------------------------------------

	public boolean isValid()
	{
		return (hour >= 0) && (hour <= 23) && (minute >= 0) && (minute <= 59) && (second >= 0) && (second <= 59);
	}

	//------------------------------------------------------------------

	public String toShortString()
	{
		return NumberUtils.uIntToDecString(hour, 2, '0') + NumberUtils.uIntToDecString(minute, 2, '0')
				+ NumberUtils.uIntToDecString(second, 2, '0');
	}

	//------------------------------------------------------------------

	public Calendar setInCalendar(
		Calendar	calendar)
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
