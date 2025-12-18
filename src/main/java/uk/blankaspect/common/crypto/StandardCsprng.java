/*====================================================================*\

StandardCsprng.java

Standard cryptographically secure pseudo-random number generator class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.AWTEvent;
import java.awt.Toolkit;

import java.io.File;

import java.util.Map;

import uk.blankaspect.common.exception.AppException;

//----------------------------------------------------------------------


// STANDARD CRYPTOGRAPHICALLY SECURE PSEUDO-RANDOM NUMBER GENERATOR CLASS


/**
 * This class implements a standard cryptographically secure pseudo-random number generator (PRNG) comprising a Fortuna
 * PRNG and an entropy accumulator.
 */

public class StandardCsprng
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		DEFAULT_ENTROPY_SOURCE_MOUSE_INTERVAL	= 4;
	private static final	int		DEFAULT_ENTROPY_SOURCE_TIMER_INTERVAL	= 4;
	private static final	int		DEFAULT_TIMER_DIVISOR					= 1;

	private static final	String	SEED_FILE_NAME	= "fortunaSeed.dat";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	Fortuna				prng;
	private	EntropyAccumulator	entropyAccumulator;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a standard cryptographically secure PRNG with an entropy accumulator that has default sources of entropy
	 * and a value of 1 for its timer divisor.
	 * <p>
	 * The default entropy sources are keyboard, mouse and timer.  The bit mask for all three sources comprises the four
	 * low-order bits, and the sample interval of the mouse and timer sources is four milliseconds.
	 * </p>
	 * <p>
	 * The entropy accumulator is set as a listener for system-wide keyboard and mouse events.
	 * </p>
	 *
	 * @param  cipher
				 the kind of cipher that will be used by the pseudo-random number generator to generate a stream cipher
				 for encryption and decryption.
	 * @throws AppException
	 *           if an error occurs when setting the entropy accumulator as a listener for system-wide keyboard and
	 *           mouse events.
	 */

	public StandardCsprng(
		FortunaCipher	cipher)
		throws AppException
	{
		this(cipher, DEFAULT_TIMER_DIVISOR);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a standard cryptographically secure PRNG with an entropy accumulator that has default sources of entropy
	 * and a specified timer divisor.
	 * <p>
	 * The default entropy sources are keyboard, mouse and timer.  The bit mask for all three sources comprises the four
	 * low-order bits, and the sample interval of the mouse and timer sources is four milliseconds.
	 * </p>
	 * <p>
	 * The entropy accumulator is set as a listener for system-wide keyboard and mouse events.
	 * </p>
	 *
	 * @param  cipher
	 *           the kind of cipher that will be used by the pseudo-random number generator to generate a stream cipher
	 *           for encryption and decryption.
	 * @param  timerDivisor
	 *           the value that will divide values from the system high-resolution timer that are used by the keyboard
	 *           and timer sources of the entropy accumulator.
	 * @throws AppException
	 *           if an error occurs when setting the entropy accumulator as a listener for system-wide keyboard and
	 *           mouse events.
	 */

	public StandardCsprng(
		FortunaCipher	cipher,
		int				timerDivisor)
		throws AppException
	{
		this(cipher, EntropyAccumulator.getSourceParams(DEFAULT_ENTROPY_SOURCE_MOUSE_INTERVAL,
														DEFAULT_ENTROPY_SOURCE_TIMER_INTERVAL),
			 timerDivisor);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a standard cryptographically secure PRNG with an entropy accumulator that has specified sources of
	 * entropy and a value of 1 for its timer divisor.
	 * <p>
	 * The entropy accumulator is set as a listener for system-wide keyboard and mouse events.
	 * </p>
	 *
	 * @param  cipher
	 *           the kind of cipher that will be used by the pseudo-random number generator to generate a stream cipher
	 *           for encryption and decryption.
	 * @param  entropySourceParams
	 *           a map of the parameters of the sources of the entropy accumulator.  A source that does not have a map
	 *           entry is disabled.
	 * @throws AppException
	 *           if an error occurs when setting the entropy accumulator as a listener for system-wide keyboard and
	 *           mouse events.
	 */

	public StandardCsprng(
		FortunaCipher														cipher,
		Map<EntropyAccumulator.SourceKind, EntropyAccumulator.SourceParams>	entropySourceParams)
		throws AppException
	{
		this(cipher, entropySourceParams, DEFAULT_TIMER_DIVISOR);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a standard cryptographically secure PRNG with an entropy accumulator that has specified sources of
	 * entropy and a specified timer divisor.
	 * <p>
	 * The entropy accumulator is set as a listener for system-wide keyboard and mouse events.
	 * </p>
	 *
	 * @param  cipher
	 *           the kind of cipher that will be used by the pseudo-random number generator to generate a stream cipher
	 *           for encryption and decryption.
	 * @param  entropySourceParams
	 *           a map of the parameters of the sources of the entropy accumulator.  A source that does not have a map
	 *           entry is disabled.
	 * @param  timerDivisor
	 *           the value that will divide values from the system high-resolution timer that are used by the keyboard
	 *           and timer sources of the entropy accumulator.
	 * @throws AppException
	 *           if an error occurs when setting the entropy accumulator as a listener for system-wide keyboard and
	 *           mouse events.
	 */

	public StandardCsprng(
		FortunaCipher														cipher,
		Map<EntropyAccumulator.SourceKind, EntropyAccumulator.SourceParams>	entropySourceParams,
		int																	timerDivisor)
		throws AppException
	{
		// Create the PRNG
		prng = cipher.prng(CryptoUtils.getSeedFromNanoTime(cipher.keySize(), timerDivisor));

		// Create the entropy accumulator and set it as the supplier of entropy to the PRNG
		entropyAccumulator = new EntropyAccumulator(entropySourceParams, timerDivisor);
		entropyAccumulator.setMetricsEnabled(true);
		entropyAccumulator.addEntropyConsumer(prng);

		// Set the entropy accumulator to use system-wide keyboard and mouse events as sources of entropy
		try
		{
			long eventMask = AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK;
			Toolkit.getDefaultToolkit().addAWTEventListener(entropyAccumulator, eventMask);
		}
		catch (SecurityException e)
		{
			throw new AppException(ErrorId.FAILED_TO_SET_ENTROPY_SOURCES, e);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the PRNG of this standard cryptographically secure PRNG.
	 *
	 * @return the PRNG of this standard cryptographically secure PRNG.
	 */

	public Fortuna getPrng()
	{
		return prng;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the entropy accumulator of this standard cryptographically secure PRNG.
	 *
	 * @return the entropy accumulator of this standard cryptographically secure PRNG.
	 */

	public EntropyAccumulator getEntropyAccumulator()
	{
		return entropyAccumulator;
	}

	//------------------------------------------------------------------

	/**
	 * Generates and returns a random byte.
	 *
	 * @return a random byte.
	 * @throws IllegalStateException
	 *           if the PRNG has not been seeded.
	 */

	public byte getRandomByte()
	{
		return prng.getRandomBytes(1)[0];
	}

	//------------------------------------------------------------------

	/**
	 * Generates and returns a specified number of random bytes, which are stored in a buffer that is allocated by this
	 * method.
	 *
	 * @param  length
	 *           the number of bytes to generate.
	 * @return a buffer containing {@code length} random bytes.
	 * @throws IllegalArgumentException
	 *           if {@code length} is negative or greater than 2<sup>20</sup> (1048576).
	 * @throws IllegalStateException
	 *           if the PRNG has not been seeded.
	 */

	public byte[] getRandomBytes(
		int	length)
	{
		return prng.getRandomBytes(length);
	}

	//------------------------------------------------------------------

	/**
	 * Generates random bytes and stores them in a specified buffer.  The number of bytes generated is equal to the
	 * length of the buffer.
	 *
	 * @param  buffer
	 *           the buffer in which the random data will be stored.
	 * @throws IllegalArgumentException
	 *           if {@code buffer} is {@code null} or the length of {@code buffer} is greater than
	 *           2<sup>20</sup> (1048576).
	 * @throws IllegalStateException
	 *           if the PRNG has not been seeded.
	 */

	public void getRandomBytes(
		byte[]	buffer)
	{
		prng.getRandomBytes(buffer, 0, buffer.length);
	}

	//------------------------------------------------------------------

	/**
	 * Generates a specified number of random bytes and stores them in a specified buffer.
	 *
	 * @param  buffer
	 *           the buffer in which the random data will be stored.
	 * @param  offset
	 *           the offset in {@code buffer} at which the first byte of random data will be stored.
	 * @param  length
	 *           the number of bytes to generate.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code buffer} is {@code null}, or</li>
	 *             <li>{@code length} is negative or greater than 2<sup>20</sup> (1048576), or</li>
	 *             <li>{@code offset} + {@code length} is greater than the length of {@code buffer}.</li>
	 *           </ul>
	 * @throws IndexOutOfBoundsException
	 *           if {@code offset} is negative or greater than the length of {@code buffer}.
	 * @throws IllegalStateException
	 *           if the PRNG has not been seeded.
	 */

	public void getRandomBytes(
		byte[]	buffer,
		int		offset,
		int		length)
	{
		prng.getRandomBytes(buffer, offset, length);
	}

	//------------------------------------------------------------------

	/**
	 * Generates and returns a random 32-bit integer.
	 *
	 * @return a random 32-bit integer.
	 * @throws IllegalStateException
	 *           if the PRNG has not been seeded.
	 */

	public int getRandomInt()
	{
		return prng.getRandomInt();
	}

	//------------------------------------------------------------------

	/**
	 * Generates and returns a random 64-bit integer.
	 *
	 * @return a random 64-bit integer.
	 * @throws IllegalStateException
	 *           if the PRNG has not been seeded.
	 */

	public long getRandomLong()
	{
		return prng.getRandomLong();
	}

	//------------------------------------------------------------------

	/**
	 * Reads a file named {@code fortunaSeed.dat} in the directory at the specified location and adds the random data
	 * that it contains to the first entropy pool of this object's PRNG.  If, after adding the entropy, the PRNG can
	 * reseed, the seed file is replaced with new random data from the PRNG.
	 *
	 * @param  directory
	 *           the location of the directory that is expected to contain the seed file.
	 * @return the length of the random data that was extracted from the seed file.
	 * @throws AppException
	 *           if an error occurs when reading the file.
	 * @see    #writeSeedFile(File)
	 */

	public int readSeedFile(
		File	directory)
		throws AppException
	{
		int randomDataLength = 0;

		// Test for file
		RandomDataFile randomDataFile = new RandomDataFile();
		File file = new File(directory, SEED_FILE_NAME);
		if (file.isFile())
		{
			// Read file
			randomDataFile.read(file);

			// Get random data from file
			byte[] randomData = randomDataFile.getRandomData();
			randomDataLength = randomData.length;

			// Add random data to first entropy pool of PRNG
			prng.addRandomBytes(0, randomData, 0, randomDataLength);

			// If PRNG can reseed, update seed file ...
			if (prng.canReseed())
				writeSeedFile(directory);

			// ... otherwise, delete seed file
			else
				file.delete();
		}

		return randomDataLength;
	}

	//------------------------------------------------------------------

	/**
	 * Writes 64 bytes of random data from this object's PRNG to a file named {@code fortunaSeed.dat} in a directory at
	 * the specified location.
	 *
	 * @param  directory
	 *           the location of the directory to which the seed file will be written.  The directory must exist.
	 * @throws AppException
	 *           if an error occurs when writing the file.
	 * @see    #readSeedFile(File)
	 */

	public void writeSeedFile(
		File	directory)
		throws AppException
	{
		RandomDataFile randomDataFile = new RandomDataFile();
		randomDataFile.setRandomData(prng.getRandomBytes(Fortuna.RESEED_ENTROPY_THRESHOLD));
		randomDataFile.write(new File(directory, SEED_FILE_NAME));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FAILED_TO_SET_ENTROPY_SOURCES
		("Failed to set the keyboard and mouse as entropy sources.");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ErrorId(
			String	message)
		{
			this.message = message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : AppException.IId interface
	////////////////////////////////////////////////////////////////////

		@Override
		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
