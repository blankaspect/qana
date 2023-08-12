/*====================================================================*\

MathUtils.java

Class: mathematics-related utility methods.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.math;

//----------------------------------------------------------------------


// CLASS: MATHEMATICS-RELATED UTILITY METHODS


/**
 * This class contains utility methods that relate to mathematics.
 */

public class MathUtils
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	NEGATIVE_VALUE_STR	= "Negative value";

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Prevents this class from being instantiated externally.
	 */

	private MathUtils()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the greatest common divisor of two non-negative integers.  The GCD is computed with the Euclidean
	 * algorithm.
	 *
	 * @param  value1
	 *           the first value.
	 * @param  value2
	 *           the second value.
	 * @return the greatest common divisor of {@code value1} and {@code value2}.
	 * @throws IllegalArgumentException
	 *           if either {@code value1} or {@code value2} is negative.
	 */

	public static int gcd(
		int	value1,
		int	value2)
	{
		// Validate arguments
		if (value1 < 0)
			throw new IllegalArgumentException(NEGATIVE_VALUE_STR + ": " + value1);
		if (value2 < 0)
			throw new IllegalArgumentException(NEGATIVE_VALUE_STR + ": " + value2);

		// Find greatest common divisor
		while (value2 != 0)
		{
			int remainder = value1 % value2;
			value1 = value2;
			value2 = remainder;
		}

		// Return result
		return value1;
	}

	//------------------------------------------------------------------

	/**
	 * Maps the specified input value to an output value using the specified lookup table and linear interpolation
	 * between adjacent values in the table.  The domain of the lookup table is the closed interval [0, 1].
	 *
	 * @param  table
	 *           the lookup table of output values.
	 * @param  x
	 *           the input value for which an output value is required.
	 * @return an output value for {@code x} that is obtained by interpolating between the values of the entries in
	 *         {@code table} that lie on either side of {@code x}.
	 * @throws IllegalArgumentException
	 *           if {@code x} is outside the interval [0, 1].
	 */

	public static double lookup(
		double[]	table,
		double		x)
	{
		// Validate arguments
		if ((x < 0.0) || (x > 1.0))
			throw new IllegalArgumentException("x out of bounds: " + x);

		// If x is at upper bound, return last value in table
		if (x == 1.0)
			return table[table.length - 1];

		// Find values in table that lie on either side of x, and interpolate between them
		double numIntervals = (double)(table.length - 1);
		int index = (int)Math.floor(x * numIntervals);
		double x0 = (double)index / numIntervals;
		double y0 = table[index];
		double y1 = table[index + 1];
		return (Double.isFinite(y0) && Double.isFinite(y1)) ? y0 + (x - x0) * numIntervals * (y1 - y0) : Double.NaN;
	}

	//------------------------------------------------------------------

	/**
	 * Maps the specified input value to an output value using the specified lookup table and linear interpolation
	 * between adjacent values in the table.  The domain of the lookup table is a closed interval with the specified
	 * endpoints.
	 *
	 * @param  table
	 *           the lookup table of output values.
	 * @param  minX
	 *           the lower endpoint of the interval of the domain of {@code table}.
	 * @param  maxX
	 *           the upper endpoint of the interval of the domain of {@code table}.
	 * @param  x
	 *           the input value for which an output value is required.
	 * @return an output value for {@code x} that is obtained by interpolating between the values of the entries in
	 *         {@code table} that lie on either side of {@code x}.
	 * @throws IllegalArgumentException
	 *           if {@code x} is outside the interval [0, 1].
	 */

	public static double lookup(
		double[]	table,
		double		minX,
		double		maxX,
		double		x)
	{
		// Validate arguments
		if ((x < minX) || (x > maxX))
			throw new IllegalArgumentException("x out of bounds: " + x);

		// If x is at upper bound, return last value in table
		int numIntervals = table.length - 1;
		if (x == maxX)
			return table[numIntervals];

		// Find values in table that lie on either side of x, and interpolate between them
		double intervalWidth = (maxX - minX) / (double)numIntervals;
		int index = (int)Math.floor((x - minX) / intervalWidth);
		double x0 = (double)index * intervalWidth;
		double y0 = table[index];
		double y1 = table[index + 1];
		return (Double.isFinite(y0) && Double.isFinite(y1)) ? y0 + (x - x0) / intervalWidth * (y1 - y0) : Double.NaN;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
