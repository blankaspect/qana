/*====================================================================*\

Date.java

Class: calendar date.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.date;

//----------------------------------------------------------------------


// IMPORTS


import java.util.Calendar;

import uk.blankaspect.common.exception2.UnexpectedRuntimeException;

import uk.blankaspect.common.misc.ModernCalendar;

import uk.blankaspect.common.number.NumberUtils;

import uk.blankaspect.common.time.Time;

//----------------------------------------------------------------------


// CLASS: CALENDAR DATE


/**
 * This class encapsulates a calendar date that consists of a year, month and day of the month.
 */

public class Date
	implements Cloneable
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The number of digits in the <i>year</i> component of the string representation of a date. */
	public static final		int		NUM_YEAR_DIGITS		= 4;

	/** The number of digits in the <i>month</i> component of the string representation of a date. */
	public static final		int		NUM_MONTH_DIGITS	= 2;

	/** The number of digits in the <i>day of the month</i> component of the string representation of a date. */
	public static final		int		NUM_DAY_DIGITS		= 2;

	/** The character that separates the components of the string representation of a date. */
	public static final		char	SEPARATOR_CHAR	= '-';

	/** Miscellaneous strings. */
	private static final	String	MALFORMED_DATE_STR	= "Malformed date";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	/** The year of this date. */
	private	int	year;

	/** The zero-based month of this date. */
	private	int	month;

	/** The zero-based day of the month of this date. */
	private	int	day;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of a date with the specified year, month and day of the month.
	 *
	 * @param year
	 *          the year.
	 * @param month
	 *          the month, zero-based (eg, March = 2).
	 * @param day
	 *          the day of the month, zero-based.
	 */

	public Date(
		int	year,
		int	month,
		int	day)
	{
		// Initialise instance variables
		this.year = year;
		this.month = month;
		this.day = day;
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of a date that is a copy of the specified date.
	 *
	 * @param date
	 *          the date for which a copy will be created.
	 */

	public Date(
		Date	date)
	{
		// Call alternative constructor
		this(date.year, date.month, date.day);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of a date that is initialised from the specified {@link Calendar} date.
	 *
	 * @param date
	 *          the date from which the new instance of {@code Date} will be initialised.
	 */

	public Date(
		Calendar	date)
	{
		// Call alternative constructor
		this(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH) - 1);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of a date from the specified string representation, which should have the form
	 * <i>yyyymmdd</i> or <i>yyyy</i>-<i>mm</i>-<i>dd</i>, where <i>y</i>, <i>m</i> and <i>d</i> are decimal digits that
	 * denote the year, month and day of the month respectively.
	 *
	 * @param  text
	 *           a string representation of the date.
	 * @throws IllegalArgumentException
	 *           if {@code text} is malformed.
	 * @throws NumberFormatException
	 *           if one of the date components of {@code text} is not a valid number.
	 */

	public Date(
		String	text)
	{
		// Test length of input string
		int inLength = text.length();
		int expectedLength = NUM_YEAR_DIGITS + NUM_MONTH_DIGITS + NUM_DAY_DIGITS;
		if ((inLength != expectedLength) && (inLength != expectedLength + 2))
			throw new IllegalArgumentException(MALFORMED_DATE_STR);

		// Parse year
		int offset = 0;
		int length = NUM_YEAR_DIGITS;
		year = Integer.parseInt(text.substring(offset, offset + length));
		offset += length;

		// Skip optional separator between year and month
		if ((inLength > expectedLength) && (text.charAt(offset++) != SEPARATOR_CHAR))
			throw new IllegalArgumentException(MALFORMED_DATE_STR);

		// Parse month
		length = NUM_MONTH_DIGITS;
		month = Integer.parseInt(text.substring(offset, offset + length)) - 1;
		offset += length;

		// Skip optional separator between month and day of month
		if ((inLength > expectedLength) && (text.charAt(offset++) != SEPARATOR_CHAR))
			throw new IllegalArgumentException(MALFORMED_DATE_STR);

		// Parse day of month
		length = NUM_DAY_DIGITS;
		day = Integer.parseInt(text.substring(offset)) - 1;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates and returns a new instance of a date that is initialised with the current date.
	 *
	 * @return a new instance of {@link Date} that is initialised with the current date.
	 */

	public static Date currentDate()
	{
		return new Date(new ModernCalendar());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 */

	@Override
	public boolean equals(
		Object	obj)
	{
		if (this == obj)
			return true;

		return (obj instanceof Date other) && (year == other.year) && (month == other.month) && (day == other.day);
	}

	//------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */

	@Override
	public int hashCode()
	{
		return (year << 9) | (month << 5) | day;
	}

	//------------------------------------------------------------------

	/**
	 * Returns a string representation of this date with the form <i>yyyy-mm-dd</i>.
	 *
	 * @return a string representation of this date with the form <i>yyyy-mm-dd</i>.
	 */

	@Override
	public String toString()
	{
		return NumberUtils.uIntToDecString(year, NUM_YEAR_DIGITS, '0') + SEPARATOR_CHAR
				+ NumberUtils.uIntToDecString(month + 1, NUM_MONTH_DIGITS, '0') + SEPARATOR_CHAR
				+ NumberUtils.uIntToDecString(day + 1, NUM_DAY_DIGITS, '0');
	}

	//------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */

	@Override
	public Date clone()
	{
		try
		{
			return (Date)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new UnexpectedRuntimeException(e);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the year of this date.
	 *
	 * @return the year of this date.
	 */

	public int getYear()
	{
		return year;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the zero-based month of this date.
	 *
	 * @return the zero-based month of this date.
	 */

	public int getMonth()
	{
		return month;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the zero-based day of the month of this date.
	 *
	 * @return the zero-based day of the month of this date.
	 */

	public int getDay()
	{
		return day;
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if this date is valid.  The year is deemed valid if it lies within the specified bounds; the
	 * month and day of the month are deemed valid if they denote an actual day of the year of this date.
	 *
	 * @param  minYear
	 *           the minimum year.
	 * @param  maxYear
	 *           the maximum year.
	 * @return {@code true} if the year of this date is between {@code minYear} and {@code maxYear} inclusive, and the
	 *         month and day of the month denote an actual day of the year of this date.
	 */

	public boolean isValid(
		int	minYear,
		int	maxYear)
	{
		if ((year < minYear) || (year > maxYear))
			return false;

		Calendar calendar = new ModernCalendar(year, 0, 1);
		if ((month < calendar.getActualMinimum(Calendar.MONTH)) || (month > calendar.getActualMaximum(Calendar.MONTH)))
			return false;

		calendar = new ModernCalendar(year, month, 1);
		int calendarDay = day + 1;
		return (calendarDay >= calendar.getActualMinimum(Calendar.DAY_OF_MONTH))
				&& (calendarDay <= calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
	}

	//------------------------------------------------------------------

	/**
	 * Returns a short string representation of this date.  The string consists of eight decimal digits in the form
	 * <i>yyyymmdd</i>.
	 *
	 * @return a string representation of this date with the form <i>yyyymmdd</i>.
	 */

	public String toShortString()
	{
		return NumberUtils.uIntToDecString(year, NUM_YEAR_DIGITS, '0')
				+ NumberUtils.uIntToDecString(month + 1, NUM_MONTH_DIGITS, '0')
				+ NumberUtils.uIntToDecString(day + 1, NUM_DAY_DIGITS, '0');
	}

	//------------------------------------------------------------------

	/**
	 * Returns a new instance of {@link ModernCalendar} that has been initialised with this date.
	 *
	 * @return a new instance of {@link ModernCalendar} that has been initialised with this date.
	 */

	public ModernCalendar toCalendar()
	{
		return new ModernCalendar(year, month, day + 1);
	}

	//------------------------------------------------------------------

	/**
	 * Returns a new instance of {@link ModernCalendar} that has been initialised with this date and the specified time.
	 *
	 * @return a new instance of {@link ModernCalendar} that has been initialised with this date and the specified time.
	 */

	public ModernCalendar toCalendar(
		Time	time)
	{
		return new ModernCalendar(year, month, day + 1, time.getHour(), time.getMinute(), time.getSecond());
	}

	//------------------------------------------------------------------

	/**
	 * Returns this date (at 00:00:00) as the number of milliseconds after the Unix epoch, 1970-01-01T00:00:00Z.
	 *
	 * @return this date (at 00:00:00) as the number of milliseconds after the Unix epoch.
	 */

	public long toMilliseconds()
	{
		return toCalendar().getTimeInMillis();
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
