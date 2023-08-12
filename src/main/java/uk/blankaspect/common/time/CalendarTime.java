/*====================================================================*\

CalendarTime.java

Class: calendar time.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.time;

//----------------------------------------------------------------------


// IMPORTS


import java.util.Calendar;
import java.util.TimeZone;

import uk.blankaspect.common.misc.ModernCalendar;

import uk.blankaspect.common.number.NumberUtils;

//----------------------------------------------------------------------


// CLASS: CALENDAR TIME


public class CalendarTime
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		String	UTC_TIME_ZONE_STR	= "UTC";

	private static final	int	NUM_YEAR_DIGITS			= 4;
	private static final	int	NUM_MONTH_DIGITS		= 2;
	private static final	int	NUM_DAY_DIGITS			= 2;
	private static final	int	NUM_HOUR_DIGITS			= 2;
	private static final	int	NUM_MINUTE_DIGITS		= 2;
	private static final	int	NUM_SECOND_DIGITS		= 2;
	private static final	int	NUM_MILLISECOND_DIGITS	= 3;

	private static final	String	DEFAULT_SEPARATOR	= " ";

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private CalendarTime()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static String getTimeString()
	{
		return timeToString(-1, DEFAULT_SEPARATOR, false, false);
	}

	//------------------------------------------------------------------

	public static String getTimeString(char separator)
	{
		return timeToString(-1, Character.toString(separator), false, false);
	}

	//------------------------------------------------------------------

	public static String getTimeString(String separator)
	{
		return timeToString(-1, separator, false, false);
	}

	//------------------------------------------------------------------

	public static String getTimeString(boolean showMilliseconds)
	{
		return timeToString(-1, DEFAULT_SEPARATOR, showMilliseconds, false);
	}

	//------------------------------------------------------------------

	public static String getTimeString(boolean showMilliseconds,
									   boolean utc)
	{
		return timeToString(-1, DEFAULT_SEPARATOR, showMilliseconds, utc);
	}

	//------------------------------------------------------------------

	public static String getTimeString(char    separator,
									   boolean showMilliseconds)
	{
		return timeToString(-1, Character.toString(separator), showMilliseconds, false);
	}

	//------------------------------------------------------------------

	public static String getTimeString(String  separator,
									   boolean showMilliseconds)
	{
		return timeToString(-1, separator, showMilliseconds, false);
	}

	//------------------------------------------------------------------

	public static String timeToString(long time)
	{
		return timeToString(time, DEFAULT_SEPARATOR, false, false);
	}

	//------------------------------------------------------------------

	public static String timeToString(long    time,
									  boolean utc)
	{
		return timeToString(time, DEFAULT_SEPARATOR, false, utc);
	}

	//------------------------------------------------------------------

	public static String timeToString(long time,
									  char separator)
	{
		return timeToString(time, Character.toString(separator), false, false);
	}

	//------------------------------------------------------------------

	public static String timeToString(long   time,
									  String separator)
	{
		return timeToString(time, separator, false, false);
	}

	//------------------------------------------------------------------

	public static String timeToString(long    time,
									  boolean showMilliseconds,
									  boolean utc)
	{
		return timeToString(time, DEFAULT_SEPARATOR, showMilliseconds, utc);
	}

	//------------------------------------------------------------------

	/**
	 * Converts a specified time in UTC milliseconds to a string in the format yyyy-MM-dd HH:mm:ss[.SSS].
	 *
	 * @param  time              the time to be converted, in UTC milliseconds from 1 January 1970
	 *                           GMT+00:00:00.000.  If {@literal time < 0}, the current time is used.
	 * @param  separator         the character that is to be used to separate the date and the time.
	 * @param  showMilliseconds  {@code true} if the time string should include milliseconds; {@code false}
	 *                           otherwise.
	 * @param  utc               {@code true} if the date and time should be shown as UTC (ie, with a time
	 *                           zone of GMT+00:00); {@code false} if the date and time should be shown in
	 *                           the default time zone.
	 * @return a string representing the time in the format yyyy-MM-dd HH:mm:ss[.SSS].
	 */

	public static String timeToString(long    time,
									  char    separator,
									  boolean showMilliseconds,
									  boolean utc)
	{
		return timeToString(time, Character.toString(separator), showMilliseconds, utc);
	}

	//------------------------------------------------------------------

	/**
	 * Converts a specified time in UTC milliseconds to a string in the format yyyy-MM-dd HH:mm:ss[.SSS].
	 *
	 * @param  time              the time to be converted, in UTC milliseconds from 1 January 1970
	 *                           GMT+00:00:00.000.  If {@literal time < 0}, the current time is used.
	 * @param  separator         the string that is to be used to separate the date and the time.
	 * @param  showMilliseconds  {@code true} if the time string should include milliseconds; {@code false}
	 *                           otherwise.
	 * @param  utc               {@code true} if the date and time should be shown as UTC (ie, with a time
	 *                           zone of GMT+00:00); {@code false} if the date and time should be shown in
	 *                           the default time zone.
	 * @return a string representing the time in the format yyyy-MM-dd HH:mm:ss[.SSS].
	 */

	public static String timeToString(long    time,
									  String  separator,
									  boolean showMilliseconds,
									  boolean utc)
	{
		Calendar calendar = new ModernCalendar();
		if (utc)
			calendar.setTimeZone(TimeZone.getTimeZone(UTC_TIME_ZONE_STR));
		if (time >= 0)
			calendar.setTimeInMillis(time);

		StringBuilder buffer = new StringBuilder(32);
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.YEAR), NUM_YEAR_DIGITS, '0'));
		buffer.append('-');
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.MONTH) + 1, NUM_MONTH_DIGITS, '0'));
		buffer.append('-');
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.DAY_OF_MONTH), NUM_DAY_DIGITS, '0'));
		buffer.append(separator);
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.HOUR_OF_DAY), NUM_HOUR_DIGITS, '0'));
		buffer.append(':');
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.MINUTE), NUM_MINUTE_DIGITS, '0'));
		buffer.append(':');
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.SECOND), NUM_SECOND_DIGITS, '0'));
		if (showMilliseconds)
		{
			buffer.append('.');
			buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.MILLISECOND), NUM_MILLISECOND_DIGITS, '0'));
		}
		return buffer.toString();
	}

	//------------------------------------------------------------------

	public static String dateToString()
	{
		return dateToString(-1, null);
	}

	//------------------------------------------------------------------

	public static String dateToString(long time)
	{
		return dateToString(time, null);
	}

	//------------------------------------------------------------------

	public static String dateToString(long   time,
									  String separator)
	{
		Calendar calendar = new ModernCalendar();
		if (time >= 0)
			calendar.setTimeInMillis(time);

		StringBuilder buffer = new StringBuilder();
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.YEAR), NUM_YEAR_DIGITS, '0'));
		if (separator != null)
			buffer.append(separator);
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.MONTH) + 1, NUM_MONTH_DIGITS, '0'));
		if (separator != null)
			buffer.append(separator);
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.DAY_OF_MONTH), NUM_DAY_DIGITS, '0'));
		return buffer.toString();
	}

	//------------------------------------------------------------------

	public static String hoursMinsToString()
	{
		return hoursMinsToString(-1, null);
	}

	//------------------------------------------------------------------

	public static String hoursMinsToString(long time)
	{
		return hoursMinsToString(time, null);
	}

	//------------------------------------------------------------------

	public static String hoursMinsToString(long   time,
										   String separator)
	{
		Calendar calendar = new ModernCalendar();
		if (time >= 0)
			calendar.setTimeInMillis(time);

		StringBuilder buffer = new StringBuilder();
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.HOUR_OF_DAY), NUM_HOUR_DIGITS, '0'));
		if (separator != null)
			buffer.append(separator);
		buffer.append(NumberUtils.uIntToDecString(calendar.get(Calendar.MINUTE), NUM_MINUTE_DIGITS, '0'));
		return buffer.toString();
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
