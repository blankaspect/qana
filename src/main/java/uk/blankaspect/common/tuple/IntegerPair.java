/*====================================================================*\

IntegerPair.java

Class: pair of integers.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.tuple;

//----------------------------------------------------------------------


// CLASS: PAIR OF INTEGERS


/**
 * This class implements an immutable ordered pair of integers.
 */

public class IntegerPair
	implements Cloneable
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** A pair of integers whose elements are both zero. */
	public static final	IntegerPair	ZEROS	= new IntegerPair(0, 0);

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	/** The first element of this pair. */
	private	int	first;

	/** The second element of this pair. */
	private	int	second;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of a pair of integers with the specified elements.
	 *
	 * @param first
	 *          the first element of the pair.
	 * @param second
	 *          the second element of the pair.
	 */

	public IntegerPair(
		int	first,
		int	second)
	{
		// Initialise instance variables
		this.first = first;
		this.second = second;
	}

	//------------------------------------------------------------------

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

		if (obj instanceof IntegerPair)
		{
			IntegerPair pair = (IntegerPair)obj;
			return ((first == pair.first) && (second == pair.second));
		}
		return false;
	}

	//------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */

	@Override
	public int hashCode()
	{
		int sum = first + second;
		return (sum * (sum + 1) / 2 + first);
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

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the first element of this pair.
	 *
	 * @return the first element of this pair.
	 */

	public int getFirst()
	{
		return first;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the second element of this pair.
	 *
	 * @return the second element of this pair.
	 */

	public int getSecond()
	{
		return second;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
