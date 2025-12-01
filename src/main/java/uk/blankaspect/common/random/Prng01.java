/*====================================================================*\

Prng01.java

Class: pseudo-random number generator 01.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.random;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.range.DoubleRange;
import uk.blankaspect.common.range.IntegerRange;

//----------------------------------------------------------------------


// CLASS: PSEUDO-RANDOM NUMBER GENERATOR 01


/**
 * This class implements a pseudo-random number generator using the Mersenne twister MT19937 algorithm.  The seed is an
 * array of unsigned int values (32 bits).  If the seed is {@code null}, a randomised seed is generated using a 30-cell
 * rule-30 cellular automaton and {@link System#nanoTime()}.
 * <p>
 * The algorithm was developed by Makoto Matsumoto and Takuji Nishimura.  For more information, see the
 * <a href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html">Mersenne twister home page</a>.
 * </p>
 * <p>
 * <i>Reference:</i><br/>
 *   Makoto Matsumoto and Takuji Nishimura, "Mersenne twister: a 623-dimensionally equidistributed uniform pseudo-random
 *   number generator", ACM Transactions on Modeling and Computer Simulation, Volume 8 Issue 1, January 1998, pages
 *   3-30.
 * </p>
 * <p>
 * This implementation of the algorithm is based on the following work:<br/>
 *   A C-program for MT19937, with initialization improved 2002/1/26.<br/>
 *   Coded by Takuji Nishimura and Makoto Matsumoto.<br/>
 *   Copyright (C) 1997-2002, Makoto Matsumoto and Takuji Nishimura<br/>
 *   All rights reserved.
 * </p>
 */

public class Prng01
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	// Mersenne twister period parameters
	private static final	int	MT_N			= 624;
	private static final	int	MT_M			= 397;
	private static final	int	MT_MATRIX_A		= 0x9908B0DF;
	private static final	int	MT_UPPER_MASK	= 0x80000000;
	private static final	int	MT_LOWER_MASK	= 0x7FFFFFFF;

	private static final	int[]	MT_MAG01	= { 0, MT_MATRIX_A };

	// Mersenne twister tempering parameters
	private static final	int	MT_TEMPERING_SHIFT_U	= 11;
	private static final	int	MT_TEMPERING_SHIFT_L	= 18;
	private static final	int	MT_TEMPERING_SHIFT_S	= 7;
	private static final	int	MT_TEMPERING_SHIFT_T	= 15;
	private static final	int	MT_TEMPERING_MASK_B		= 0x9D2C5680;
	private static final	int	MT_TEMPERING_MASK_C		= 0xEFC60000;

	// Mersenne twister seed parameters
	private static final	int	MT_SEED_INIT_VALUE	= 19650218;
	private static final	int	MT_SEED_MULTIPLIER1	= 1812433253;
	private static final	int	MT_SEED_MULTIPLIER2	= 1664525;
	private static final	int	MT_SEED_MULTIPLIER3	= 1566083941;

	// Cellular automaton seed randomiser
	private static final	int	CA_NUM_SEED_BITS		= 64;
	private static final	int	CA_NUM_SEED_INTS		= (CA_NUM_SEED_BITS + 31) / 32;
	private static final	int	CA_NUM_SEED_CELLS		= 30;
	private static final	int	CA_SEED_SAMPLE_MASK		= 1 << CA_NUM_SEED_CELLS / 2;
	private static final	int	CA_SEED_INDEX_INCREMENT	= 5;

	private static final	int[]	CA_RULE	= { 0, 1, 1, 1, 1, 0, 0, 0 };

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	int	seedIndex	= ((int)System.nanoTime() & 0x7FFFFFFF) % CA_NUM_SEED_BITS;
	private static	int	seedCells;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	int[]	mt;
	private	int		mtIndex;
	private	int[]	seed;
	private	double	normalNextValue;
	private	boolean	hasNormalNextValue;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public Prng01()
	{
		this((int[])null);
	}

	//------------------------------------------------------------------

	public Prng01(Long seed)
	{
		this((seed == null) ? null : longToIntArray(seed));
	}

	//------------------------------------------------------------------

	public Prng01(int[] seed)
	{
		mt = new int[MT_N];
		init(seed);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static int[] longToIntArray(long value)
	{
		return new int[] { (int)(value >>> 32), (int)(value & 0xFFFFFFFFL) };
	}

	//------------------------------------------------------------------

	public static long intArrayToLong(int[] values)
	{
		return ((long)values[0] << 32) | ((long)values[1] & 0xFFFFFFFFL);
	}

	//------------------------------------------------------------------

	public static int[] getRandomisedSeed()
	{
		// Initialise seed cellular automaton
		if (seedCells == 0)
		{
			seedCells = (int)System.nanoTime() & 0x7FFFFFFE;
			for (int i = 0; i < CA_NUM_SEED_CELLS; i++)
				updateSeedCa();
		}

		// Generate seed
		int seed[] = null;
		boolean done = false;
		while (!done)
		{
			// Scramble bits from seed cellular automaton to create seed
			seed = new int[CA_NUM_SEED_INTS];
			int index = seedIndex;
			for (int i = 0; i < CA_NUM_SEED_BITS; i++)
			{
				int count = (int)System.nanoTime() & 0x03;
				++count;
				while (--count >= 0)
					updateSeedCa();

				if ((seedCells & CA_SEED_SAMPLE_MASK) != 0)
					seed[index >> 5] |= 1 << (index & 0x1F);
				index += CA_SEED_INDEX_INCREMENT;
				if (index >= CA_NUM_SEED_BITS)
					index -= CA_NUM_SEED_BITS;
			}

			// Update seed index
			seedIndex = index;

			// Test for non-zero seed
			for (int s : seed)
			{
				if (s != 0)
				{
					done = true;
					break;
				}
			}
		}

		// Return seed
		return seed;
	}

	//------------------------------------------------------------------

	private static void updateSeedCa()
	{
		int mask = 1 << 1;
		int value = 0;
		int cells = seedCells | seedCells >>> CA_NUM_SEED_CELLS | seedCells << CA_NUM_SEED_CELLS;
		for (int i = 0; i < CA_NUM_SEED_CELLS; i++)
		{
			if (CA_RULE[cells & 0x07] != 0)
				value |= mask;
			mask <<= 1;
			cells >>>= 1;
		}
		seedCells = value;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public int[] getSeed()
	{
		return seed;
	}

	//------------------------------------------------------------------

	public void setSeed(int[] seed)
	{
		this.seed = seed;

		mt[0] = MT_SEED_INIT_VALUE;
		for (mtIndex = 1; mtIndex < MT_N; mtIndex++)
			mt[mtIndex] = (MT_SEED_MULTIPLIER1 * (mt[mtIndex - 1] ^ (mt[mtIndex - 1] >>> 30)) + mtIndex);

		int i = 1;
		int j = 0;
		int k = (MT_N > seed.length) ? MT_N : seed.length;
		for (; k != 0; --k)
		{
			mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * MT_SEED_MULTIPLIER2)) + seed[j] + j;
			++i;
			++j;
			if (i >= MT_N)
			{
				mt[0] = mt[MT_N - 1];
				i = 1;
			}
			if (j >= seed.length)
				j = 0;
		}
		for (k = MT_N - 1; k != 0; k--)
		{
			mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * MT_SEED_MULTIPLIER3)) - i;
			++i;
			if (i >= MT_N)
			{
				mt[0] = mt[MT_N - 1];
				i = 1;
			}
		}

		mt[0] = 0x80000000;
	}

	//------------------------------------------------------------------

	public void init()
	{
		init((int[])null);
	}

	//------------------------------------------------------------------

	public void init(Long seed)
	{
		init((seed == null) ? null : longToIntArray(seed));
	}

	//------------------------------------------------------------------

	public void init(int[] seed)
	{
		// Initialise MT state
		setSeed((seed == null) ? getRandomisedSeed() : seed);

		// Initialise instance variables
		hasNormalNextValue = false;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the next positive (ie, 31-bit) integer value from the random sequence.
	 *
	 * @return the next positive (ie, 31-bit) integer value from the random sequence.
	 */

	public int nextInt()
	{
		return nextInt32() & 0x7FFFFFFF;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the next integer value from the random sequence in the interval [0, {@code bound-1}].
	 *
	 * @param  bound
	 *           the upper bound of the value.
	 * @return the next integer value from the random sequence in the interval [0, {@code bound-1}].
	 */

	public int nextInt(int bound)
	{
		long value = (long)nextInt() * (long)bound;
		return (int)(value >>> 31);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the next integer value from the random sequence in the half-open range specified by {@code range}.  The
	 * returned value is in the range [{@code range.lowerBound}..{@code range.upperBound-1}].
	 *
	 * @return the next integer value from the random sequence in the range [{@code
	 *         range.lowerBound}..{@code range.upperBound-1}].
	 */

	public int nextInt(IntegerRange range)
	{
		return range.lowerBound + nextInt(range.getInterval());
	}

	//------------------------------------------------------------------

	/**
	 * Returns the next 32-bit integer value from the random sequence.
	 *
	 * @return the next 32-bit integer value from the random sequence.
	 */

	public int nextInt32()
	{
		if (mtIndex >= MT_N)
			nextBlock();

		int y = mt[mtIndex++];
		y ^= y >>> MT_TEMPERING_SHIFT_U;
		y ^= (y << MT_TEMPERING_SHIFT_S) & MT_TEMPERING_MASK_B;
		y ^= (y << MT_TEMPERING_SHIFT_T) & MT_TEMPERING_MASK_C;
		y ^= y >>> MT_TEMPERING_SHIFT_L;

		return y;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the next 64-bit integer value from the random sequence as a long.
	 *
	 * @return the next 64-bit integer value from the random sequence.
	 */

	public long nextInt64()
	{
		return ((nextInt32() & 0xFFFFFFFFL) << 32) | (nextInt32() & 0xFFFFFFFFL);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the next positive (ie, 63-bit) long value from the random sequence.
	 *
	 * @return the next positive (ie, 63-bit) long value from the random sequence.
	 */

	public long nextLong()
	{
		return ((nextInt32() & 0x7FFFFFFFL) << 32) | (nextInt32() & 0xFFFFFFFFL);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the next double value in the interval [0, 1) from the pseudo-random sequence.  The value is obtained by
	 * converting a 64-bit integer value to an IEEE 754 double-format bit field.  The 64-bit value is deemed to
	 * represent a binary fraction of the form 0.b[63]...b[0].  It is normalised by shifting it to the left until the
	 * msb is 1.  The msb is then discarded because the significand has an implied leading '1' bit; the remaining bits
	 * are right-shifted into bits 51..0, and the exponent is set in bits 62..52.
	 *
	 * @return the next double value in the interval [0, 1).
	 */

	public double nextDouble()
	{
		// Get a random 64-bit integer
		long value = nextInt64();

		// Convert the integer to an 11-bit exponent and a normalised 52-bit significand
		if (value != 0)
		{
			long exponent = 1022;
			while (value > 0)
			{
				value <<= 1;
				--exponent;
			}
			value <<= 1;
			value >>>= 12;
			value |= exponent << 52;
		}

		// Convert the bit field to a double and return it
		return Double.longBitsToDouble(value);
	}

	//------------------------------------------------------------------

	public double nextDouble(DoubleRange range)
	{
		return range.lowerBound + nextDouble() * range.getInterval();
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	public void nextBytes(byte[] buffer)
	{
		nextBytes(buffer, 0, buffer.length);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	public void nextBytes(byte[] buffer,
						  int    offset,
						  int    length)
	{
		if (buffer == null)
			throw new IllegalArgumentException("Null buffer");
		if ((offset < 0) || (offset > buffer.length))
			throw new IllegalArgumentException("Offset out of bounds: " + offset);
		if ((length < 0) || (length > buffer.length - offset))
			throw new IllegalArgumentException("Length out of bounds: " + length);

		int value = 0;
		int i = 0;
		int endOffset = offset + length;
		while (offset < endOffset)
		{
			if (i == 0)
			{
				value = nextInt32();
				i = Integer.BYTES;
			}
			--i;
			buffer[offset++] = (byte)value;
			value >>>= Byte.SIZE;
		}
	}

	//------------------------------------------------------------------

	public boolean nextBoolean()
	{
		return ((nextInt32() >> 5 & 0x01) != 0);
	}

	//------------------------------------------------------------------

	public double nextNormal()
	{
		if (hasNormalNextValue)
		{
			hasNormalNextValue = false;
			return normalNextValue;
		}

		double v1 = 0.0;
		double v2 = 0.0;
		double s = 1.0;
		while (s >= 1.0)
		{
			v1 = nextDouble() * 2.0 - 1.0;
			v2 = nextDouble() * 2.0 - 1.0;
			s = v1 * v1 + v2 * v2;
		}
		double factor = StrictMath.sqrt(-2.0 * StrictMath.log(s) / s);
		normalNextValue = v1 * factor;
		hasNormalNextValue = true;
		return v2 * factor;
	}

	//------------------------------------------------------------------

	public double nextNormal(double mean)
	{
		return nextNormal() + mean;
	}

	//------------------------------------------------------------------

	public double nextNormal(double mean,
							 double sd)
	{
		return nextNormal() * sd + mean;
	}

	//------------------------------------------------------------------

	public double nextExponential(double lambda)
	{
		double x = 0.0;
		while (x == 0.0)
			x = nextDouble();
		return StrictMath.log(x) / -lambda;
	}

	//------------------------------------------------------------------

	public Prng01 createChild(int index)
	{
		for (int i = 0; i < index; i++)
			nextInt64();
		return new Prng01(longToIntArray(nextInt64()));
	}

	//------------------------------------------------------------------

	private void nextBlock()
	{
		int y = 0;
		int kk = 0;
		for (; kk < MT_N - MT_M; kk++)
		{
			y = (mt[kk] & MT_UPPER_MASK) | (mt[kk + 1] & MT_LOWER_MASK);
			mt[kk] = mt[kk + MT_M] ^ (y >>> 1) ^ MT_MAG01[y & 0x1];
		}
		for (; kk < MT_N - 1; kk++)
		{
			y = (mt[kk] & MT_UPPER_MASK) | (mt[kk + 1] & MT_LOWER_MASK);
			mt[kk] = mt[kk + (MT_M - MT_N)] ^ (y >>> 1) ^ MT_MAG01[y & 0x1];
		}
		y = (mt[MT_N - 1] & MT_UPPER_MASK) | (mt[0] & MT_LOWER_MASK);
		mt[MT_N - 1] = mt[MT_M - 1] ^ (y >>> 1) ^ MT_MAG01[y & 0x1];

		mtIndex = 0;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
