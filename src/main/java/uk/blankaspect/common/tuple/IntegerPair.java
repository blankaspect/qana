/*====================================================================*\

IntegerPair.java

Record: pair of integers.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.tuple;

//----------------------------------------------------------------------


// RECORD: PAIR OF INTEGERS


/**
 * This record implements an immutable ordered pair of integers.
 */

public record IntegerPair(
	int	first,
	int	second)
	implements Cloneable
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** A pair of integers whose elements are both zero. */
	public static final	IntegerPair	ZEROS	= new IntegerPair(0, 0);

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates and returns a new instance of a pair of integers with the specified elements.
	 *
	 * @param  first
	 *           the first element of the pair.
	 * @param  second
	 *           the second element of the pair.
	 * @return a new instance of a pair of integers whose elements are {@code first} and {@code second}.
	 */

	public static IntegerPair of(
		int	first,
		int	second)
	{
		return new IntegerPair(first, second);
	}

	//------------------------------------------------------------------

	/**
	 * Returns a new instance of a pair of integers that is represented by the specified string.
	 *
	 * @param  str
	 *           the string that will be parsed.
	 * @return the pair of integers that is represented by {@code str}.
	 * @throws NumberFormatException
	 *           if {@code str} is not a valid representation of a pair of integers.
	 * @throws IllegalArgumentException
	 *           if {@code str} is {@code null}.
	 */

	public static IntegerPair parse(
		String	str)
	{
		// Validate argument
		if (str == null)
			throw new IllegalArgumentException("Null string");

		// Split string into string representations of elements
		String[] strs = str.split("\\s*,\\s*", -1);
		if (strs.length != 2)
			throw new NumberFormatException();

		// Create pair from string representations of elements, and return pair
		return new IntegerPair(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : Cloneable interface
////////////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 */

	@Override
	public IntegerPair clone()
	{
		return new IntegerPair(first, second);
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

		return (obj instanceof IntegerPair other) && (first == other.first) && (second == other.second);
	}

	//------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */

	@Override
	public int hashCode()
	{
		int sum = first + second;
		return sum * (sum + 1) / 2 + first;
	}

	//------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */

	@Override
	public String toString()
	{
		return new String(Integer.toString(first) + ", " + Integer.toString(second));
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
