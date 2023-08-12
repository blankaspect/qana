/*====================================================================*\

CryptoUtils.java

Cryptographic utility methods class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// CRYPTOGRAPHIC UTILITY METHODS CLASS


/**
 * This class contains methods that are intended for use in cryptography.
 */

public class CryptoUtils
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	DEFAULT_NANO_TIMER_DIVISOR	= 1;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private CryptoUtils()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns a supposedly random seed of a specified length that is derived from the Java virtual
	 * machine's high-resolution timer.
	 * <p>
	 * This method is equivalent to calling {@link #getSeedFromNanoTime(int, int)} with the specified value
	 * of {@code length} and a timer divisor of 1.
	 * </p>
	 * <p>
	 * The returned value consists of bits from the value of the high-resolution timer that are processed
	 * with a double-iteration SHA-256 hash function.  The high-resolution timer is sampled twice for each
	 * byte of the seed, and the four low-order bits are extracted from each sample.
	 * </p>
	 *
	 * @param  length  the length (in bytes) of the seed.
	 * @return a seed that was derived from the Java virtual machine's high-resolution timer.
	 */

	public static byte[] getSeedFromNanoTime(int length)
	{
		return getSeedFromNanoTime(length, DEFAULT_NANO_TIMER_DIVISOR);
	}

	//------------------------------------------------------------------

	/**
	 * Returns a supposedly random seed of a specified length that is derived from the Java virtual
	 * machine's high-resolution timer.
	 * <p>
	 * The returned value consists of bits from the value of the high-resolution timer that are processed
	 * with a double-iteration SHA-256 hash function.  The high-resolution timer is sampled twice for each
	 * byte of the seed; each sample is divided by {@code timerDivisor} and the four low-order bits are
	 * extracted.
	 * </p>
	 *
	 * @param  length        the length (in bytes) of the seed.
	 * @param  timerDivisor  the value that will divide the value from the high-resolution timer before
	 *                       extracting the four lower-order bits.
	 * @return a seed that was derived from the Java virtual machine's high-resolution timer.
	 */

	public static byte[] getSeedFromNanoTime(int length,
											 int timerDivisor)
	{
		synchronized (seedHash)
		{
			byte[] seed = new byte[length];
			for (int i = 0; i < seed.length; i++)
			{
				if (seedHashDataLength == 0)
				{
					if (seedHashData == null)
					{
						byte[] data = new byte[ShaD256.HASH_VALUE_SIZE];
						for (int j = 0; j < data.length; j++)
							data[j] = getNanoTimeByte(timerDivisor);
						seedHash.update(data);
					}
					seedHashData = seedHash.digest();
					seedHashDataLength = ShaD256.HASH_VALUE_SIZE;
				}
				seedHash.update(getNanoTimeByte(timerDivisor));
				seed[i] = seedHashData[--seedHashDataLength];
			}
			return seed;
		}
	}

	//------------------------------------------------------------------

	private static byte getNanoTimeByte(int timerDivisor)
	{
		int value1 = 0;
		try
		{
			value1 = (int)(System.nanoTime() / timerDivisor);
			Thread.sleep(value1 & 0x03);
		}
		catch (InterruptedException e)
		{
			// ignore
		}

		int value2 = 0;
		try
		{
			value2 = (int)(System.nanoTime() / timerDivisor);
			Thread.sleep(value2 & 0x03);
		}
		catch (InterruptedException e)
		{
			// ignore
		}

		return (byte)((value1 << 4) | (value2 & 0x0F));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	ShaD256	seedHash			= new ShaD256();
	private static	byte[]	seedHashData;
	private static	int		seedHashDataLength;

}

//----------------------------------------------------------------------
