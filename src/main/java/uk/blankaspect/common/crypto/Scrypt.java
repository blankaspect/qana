/*====================================================================*\

Scrypt.java

Class: scrypt password-based key-derivation function.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.stream.Stream;

import uk.blankaspect.common.exception.UnexpectedRuntimeException;

import uk.blankaspect.common.misc.IStringKeyed;

import uk.blankaspect.common.thread.DaemonFactory;

//----------------------------------------------------------------------


// CLASS: SCRYPT PASSWORD-BASED KEY-DERIVATION FUNCTION


/**
 * This class is an implementation of the <i>scrypt</i> password-based key derivation function (KDF).
 * <p style="margin-bottom: 0.25em">
 * The scrypt function is specified in <a href="https://tools.ietf.org/html/rfc7914">IETF RFC 7914</a>.  This
 * implementation differs from the specification in two respects:
 * </p>
 * <ul style="margin-top: 0.25em">
 *   <li>A core hash function of a suitable cipher other than Salsa20 (eg, ChaCha20) may be used.  The core hash
 *       function is a parameter of the constructor.</li>
 *   <li>The number of rounds of the core hash function is not fixed at 8: this implementation supports 8, 12, 16 and
 *       20 rounds.  The number of rounds is a parameter of the constructor.</li>
 * </ul>
 * @see ScryptChaCha20
 * @see ScryptSalsa20
 */

public class Scrypt
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The minimum value of CPU/memory cost parameter, which is the binary logarithm of the <i>N</i> parameter
		(CPU/memory cost) of the scrypt algorithm. */
	public static final		int	MIN_COST	= 1;

	/** The maximum value of CPU/memory cost parameter, which is the binary logarithm of the <i>N</i> parameter
		(CPU/memory cost) of the scrypt algorithm. */
	public static final		int	MAX_COST	= 24;

	/** The minimum value of the <i>r</i> parameter (block size) of the scrypt algorithm. */
	public static final		int	MIN_NUM_BLOCKS	= 1;

	/** The maximum value of the <i>r</i> parameter (block size) of the scrypt algorithm. */
	public static final		int	MAX_NUM_BLOCKS	= 1024;

	/** The minimum value of the <i>p</i> parameter (parallelisation) of the scrypt algorithm. */
	public static final		int	MIN_NUM_SUPERBLOCKS	= 1;

	/** The maximum value of the <i>p</i> parameter (parallelisation) of the scrypt algorithm. */
	public static final		int	MAX_NUM_SUPERBLOCKS	= 64;

	/** The minimum number of threads that may be created to perform the parallel processing of superblocks at the
		highest level of the scrypt algorithm. */
	public static final		int	MIN_NUM_THREADS	= 1;

	/** The maximum number of threads that may be created to perform the parallel processing of superblocks at the
		highest level of the scrypt algorithm. */
	public static final		int	MAX_NUM_THREADS	= 64;

	/** The size (in bytes) of a block of the core hash function. */
	private static final	int	CORE_HASH_BLOCK_SIZE	= 64;

	/** The size (in bytes) of a block. */
	private static final	int	BLOCK_SIZE		= 2 * CORE_HASH_BLOCK_SIZE;

	/** The size (in ints) of a block. */
	private static final	int	BLOCK_SIZE_INTS	= BLOCK_SIZE / Integer.BYTES;

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	/** The index of the next thread that is created when deriving a key. */
	private static	int	threadIndex;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	/** The number of rounds of the core hash function. */
	private CoreHashNumRounds	coreHashNumRounds;

	/** The core hash function. */
	private ICoreHashFunction	coreHashFunction;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of a scrypt password-based key-derivation function with the specified core hash function
	 * and number of rounds of that function.
	 *
	 * @param coreHashNumRounds
	 *          the number of rounds of the core hash function.
	 * @param coreHashFunction
	 *          the core hash function.
	 */

	public Scrypt(
		CoreHashNumRounds	coreHashNumRounds,
		ICoreHashFunction	coreHashFunction)
	{
		// Validate arguments
		if (coreHashNumRounds == null)
			throw new IllegalArgumentException("Null core hash number of rounds");
		if (coreHashFunction == null)
			throw new IllegalArgumentException("Null core hash function");

		// Initialise instance variables
		this.coreHashNumRounds = coreHashNumRounds;
		this.coreHashFunction = coreHashFunction;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the number of rounds of the core hash function that will be performed by the scrypt key-derivation
	 * function.
	 * <p>
	 * In the scrypt specification, the number of rounds is fixed at 8, but this implementation supports 8, 12, 16 and
	 * 20 rounds.
	 * </p>
	 * <p>
	 * The default value is 8.
	 * </p>
	 *
	 * @return the number of rounds of the core hash function that will be performed by the scrypt key-derivation
	 *         function.
	 * @see    #setCoreHashNumRounds(CoreHashNumRounds)
	 */

	public CoreHashNumRounds getCoreHashNumRounds()
	{
		return coreHashNumRounds;
	}

	//------------------------------------------------------------------

	/**
	 * Derives a key from the specified key and salt using the scrypt key-derivation function with the specified
	 * parameters, and returns the derived key.
	 *
	 * @param  key
	 *           the key from which the key will be derived.
	 * @param  salt
	 *           the salt from which the key will be derived.
	 * @param  params
	 *           the parameters of the scrypt algorithm: CPU/memory cost, block size and parallelisation.
	 * @param  maxNumThreads
	 *           the maximum number of threads that will be created to perform the mixing of the parallel superblocks at
	 *           the highest level of the KDF.
	 * @param  outKeyLength
	 *           the length (in bytes) of the derived key, which must be a positive integral multiple of 32.
	 * @return a derived key of length {@code outKeyLength}.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code key} is {@code null}, or</li>
	 *             <li>{@code salt} is {@code null}, or</li>
	 *             <li>{@code params.cost} is less than 1 or greater than 24, or</li>
	 *             <li>{@code params.numBlocks} is less than 1 or greater than 1024, or</li>
	 *             <li>{@code params.numSuperblocks} is less than 1 or greater than 64, or</li>
	 *             <li>{@code maxNumThreads} is less than 1 or greater than 64, or</li>
	 *             <li>{@code outKeyLength} is not a positive integral multiple of 32.</li>
	 *           </ul>
	 * @see    #deriveKey(byte[], byte[], int, int, int, int, int)
	 * @see    Scrypt.Params
	 */

	public byte[] deriveKey(
		byte[]	key,
		byte[]	salt,
		Params	params,
		int		maxNumThreads,
		int		outKeyLength)
	{
		return deriveKey(key, salt, params.cost, params.numBlocks, params.numSuperblocks, maxNumThreads,
						 outKeyLength);
	}

	//------------------------------------------------------------------

	/**
	 * Derives a key from the specified key and salt using the scrypt key-derivation function with the specified
	 * parameters, and returns the derived key.
	 *
	 * @param  key
	 *           the key from which the key will be derived.
	 * @param  salt
	 *           the salt from which the key will be derived.
	 * @param  cost
	 *           the binary logarithm of the scrypt CPU/memory cost parameter, <i>N</i>.
	 * @param  numBlocks
	 *           the number of blocks: the scrypt block size parameter, <i>r</i>.
	 * @param  numSuperblocks
	 *           the number of parallel superblocks: the scrypt parallelisation parameter, <i>p</i>.
	 * @param  maxNumThreads
	 *           the maximum number of threads that will be created to perform the mixing of the parallel superblocks at
	 *           the highest level of the KDF.
	 * @param  outKeyLength
	 *           the length (in bytes) of the derived key, which must be a positive integral multiple of 32.
	 * @return a derived key of length {@code outKeyLength}.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code key} is {@code null}, or</li>
	 *             <li>{@code salt} is {@code null}, or</li>
	 *             <li>{@code cost} is less than 1 or greater than 24, or</li>
	 *             <li>{@code numBlocks} is less than 1 or greater than 1024, or</li>
	 *             <li>{@code numSuperblocks} is less than 1 or greater than 64, or</li>
	 *             <li>{@code maxNumThreads} is less than 1 or greater than 64, or</li>
	 *             <li>{@code outKeyLength} is not a positive integral multiple of 32.</li>
	 *           </ul>
	 * @see    #deriveKey(byte[], byte[], Params, int, int)
	 */

	public byte[] deriveKey(
		byte[]	key,
		byte[]	salt,
		int		cost,
		int		numBlocks,
		int		numSuperblocks,
		int		maxNumThreads,
		int		outKeyLength)
	{
		final	int	NUM_HMAC_ITERATIONS	= 1;

		// Validate arguments
		if (key == null)
			throw new IllegalArgumentException("Null key");
		if (salt == null)
			throw new IllegalArgumentException("Null salt");
		if ((cost < MIN_COST) || (cost > MAX_COST))
			throw new IllegalArgumentException("Cost out of bounds: " + cost);
		if ((numBlocks < MIN_NUM_BLOCKS) || (numBlocks > MAX_NUM_BLOCKS))
			throw new IllegalArgumentException("Number of blocks out of bounds: " + numBlocks);
		if ((numSuperblocks < MIN_NUM_SUPERBLOCKS) || (numSuperblocks > MAX_NUM_SUPERBLOCKS))
			throw new IllegalArgumentException("Number of superblocks out of bounds: " + numSuperblocks);
		if ((maxNumThreads < MIN_NUM_THREADS) || (maxNumThreads > MAX_NUM_THREADS))
			throw new IllegalArgumentException("Maximum number of threads out of bounds: " + maxNumThreads);
		if ((outKeyLength <= 0) || (outKeyLength % HmacSha256.HASH_VALUE_SIZE != 0))
			throw new IllegalArgumentException("Invalid output key length: " + outKeyLength);

		// Create container for local variables
		class Vars
		{
			boolean outOfMemory;
		}
		Vars vars = new Vars();

		// Generate key data from the input key and salt
		int superblockSize = numBlocks * BLOCK_SIZE;
		byte[] keyData = pbkdf2HmacSha256(key, salt, NUM_HMAC_ITERATIONS, numSuperblocks * superblockSize);

		// Mix the key data using a thread pool.
		// The superblocks of the scrypt KDF can be processed independently of each other, which makes the set of tasks
		// suitable for execution in parallel
		ExecutorService executor = Executors.newFixedThreadPool(Math.min(numSuperblocks, maxNumThreads), runnable ->
				DaemonFactory.create(getClass().getSimpleName() + "-" + threadIndex++, runnable));
		for (int offset = 0; offset < keyData.length; offset += superblockSize)
		{
			int offset0 = offset;
			executor.execute(() ->
			{
				try
				{
					// Get length of superblock
					int length = numBlocks * BLOCK_SIZE_INTS;

					// Convert the key data to integers
					int[] buffer = new int[length];
					int j = offset0;
					for (int i = 0; i < length; i++)
						buffer[i] = (keyData[j++] & 0xFF)
									| (keyData[j++] & 0xFF) << 8
									| (keyData[j++] & 0xFF) << 16
									| (keyData[j++] & 0xFF) << 24;

					// Mix the key data
					sMix(buffer, buffer, cost);

					// Convert the mixed key data back to bytes
					j = offset0;
					for (int i = 0; i < length; i++)
					{
						int value = buffer[i];
						keyData[j++] = (byte)value;
						keyData[j++] = (byte)(value >>> 8);
						keyData[j++] = (byte)(value >>> 16);
						keyData[j++] = (byte)(value >>> 24);
					}
				}
				catch (OutOfMemoryError e)
				{
					vars.outOfMemory = true;
				}
			});
		}

		// Shut down executor
		executor.shutdown();

		// Wait for threads to terminate
		try
		{
			executor.awaitTermination(1000, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			// ignore
		}
		finally
		{
			if (!executor.isTerminated())
				executor.shutdownNow();
		}

		// Throw an exception if there was an 'out of memory' error when mixing key data
		if (vars.outOfMemory)
			throw new OutOfMemoryError();

		// Return a key derived from the input key and the processed key data
		return pbkdf2HmacSha256(key, keyData, NUM_HMAC_ITERATIONS, outKeyLength);
	}

	//------------------------------------------------------------------

	/**
	 * Creates and returns a new instance of a {@link KeyGenerator} that uses the scrypt key-derivation function with
	 * the specified KDF parameters to derive a key from the specified key and salt.
	 *
	 * @param  key
	 *           the key from which the key will be derived.
	 * @param  salt
	 *           the salt from which the key will be derived.
	 * @param  params
	 *           the parameters of the KDF that will derive the key.
	 * @param  maxNumThreads
	 *           the maximum number of threads that should be allocated for the processing of parallel superblocks in
	 *           the KDF.
	 * @param  outKeyLength
	 *           the length (in bytes) of the derived key, which must be a positive integral multiple of 32.
	 * @return a new instance of {@code KeyGenerator}.
	 */

	public KeyGenerator createKeyGenerator(
		byte[]			key,
		byte[]			salt,
		Scrypt.Params	params,
		int				maxNumThreads,
		int				outKeyLength)
	{
		return new KeyGenerator(key, salt, params, maxNumThreads, outKeyLength);
	}

	//------------------------------------------------------------------

	/**
	 * Mixes the specified block of data at the intermediate level of the scrypt KDF.
	 *
	 * @param in
	 *          the data that will be mixed.
	 * @param out
	 *          the buffer in which the mixed output data will be stored.
	 */

	protected void blockMix(
		int[]	in,
		int[]	out)
	{
		final	int	BLOCK_LENGTH	= CORE_HASH_BLOCK_SIZE / Integer.BYTES;

		// Copy the last block of input data to the X array
		int length = in.length;
		int[] x = new int[BLOCK_LENGTH];
		System.arraycopy(in, in.length - BLOCK_LENGTH, x, 0, BLOCK_LENGTH);

		// Hash the input data with the core hash function
		int[] y = new int[length];
		int[] z = new int[BLOCK_LENGTH];
		int j = 0;
		for (int offset = 0; offset < length; offset += BLOCK_LENGTH)
		{
			for (int i = 0; i < BLOCK_LENGTH; i++)
				z[i] = x[i] ^ in[j++];

			coreHashFunction.hash(z, x, coreHashNumRounds.value);

			System.arraycopy(x, 0, y, offset, BLOCK_LENGTH);
		}

		// Move the processed input data to the output array
		int offset = 0;
		int offset0 = 0;
		int offset1 = length / 2;
		while (offset < length)
		{
			System.arraycopy(y, offset, out, offset0, BLOCK_LENGTH);
			offset += BLOCK_LENGTH;
			offset0 += BLOCK_LENGTH;

			System.arraycopy(y, offset, out, offset1, BLOCK_LENGTH);
			offset += BLOCK_LENGTH;
			offset1 += BLOCK_LENGTH;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Mixes the specified superblock of data at the highest level of the scrypt KDF.
	 *
	 * @param in
	 *          the data that will be mixed.
	 * @param out
	 *          a buffer in which the mixed output data will be stored.
	 * @param cost
	 *          the binary logarithm of the scrypt CPU/memory cost parameter, <i>N</i>.
	 */

	protected void sMix(
		int[]	in,
		int[]	out,
		int		cost)
	{
		// Copy the input data to the X array
		int length = in.length;
		int[] x = new int[length];
		System.arraycopy(in, 0, x, 0, length);

		// Mix the data in the costly V array
		int numIterations = 1 << cost;
		int[][] v = new int[numIterations][length];
		for (int i = 0; i < numIterations; i++)
		{
			System.arraycopy(x, 0, v[i], 0, length);

			blockMix(x, x);
		}

		// Perform further mixing
		int mask = numIterations - 1;
		int[] t = new int[length];
		for (int i = 0; i < numIterations; i++)
		{
			int[] u = v[x[length - (BLOCK_SIZE_INTS / 2)] & mask];
			for (int j = 0; j < length; j++)
				t[j] = x[j] ^ u[j];

			blockMix(t, x);
		}

		// Copy the mixed data to the output array
		System.arraycopy(x, 0, out, 0, length);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the HMAC-SHA256 hash value for the specified key and data.
	 * <p>
	 * HMAC-SHA256 is a hash-based message authentication code whose underlying function is the SHA-256 cryptographic
	 * hash function.
	 * </p>
	 *
	 * @param  key
	 *           the key for the HMAC.
	 * @param  data
	 *           the data that will be hashed by the HMAC function.
	 * @return the HMAC-SHA256 hash value for the specified key and data.
	 */

	protected byte[] hmacSha256(
		byte[]	key,
		byte[]	data)
	{
		return new HmacSha256(key).getValue(data);
	}

	//------------------------------------------------------------------

	/**
	 * Derives a key of the specified length from the specified key and salt using the PBKDF2 function whose underlying
	 * hash function is HMAC-SHA256 (hash-based message authentication code using the SHA-256 cryptographic hash
	 * function), and returns the derived key.
	 * <p>
	 * PBKDF2 is specified in <a href="https://tools.ietf.org/html/rfc2898">IETF RFC 2898</a>.
	 * </p>
	 *
	 * @param  key
	 *           the key from which the key will be derived.
	 * @param  salt
	 *           the salt from which the key will be derived.
	 * @param  numIterations
	 *           the number of iterations of the HMAC-SHA256 algorithm that will be applied.
	 * @param  outKeyLength
	 *           the length (in bytes) of the derived key, which must be a positive integral multiple of 32.
	 * @return a derived key of length {@code outKeyLength}.
	 * @throws IllegalArgumentException
	 *           if {@code outKeyLength} is not a positive integral multiple of 32.
	 */

	protected byte[] pbkdf2HmacSha256(
		byte[]	key,
		byte[]	salt,
		int		numIterations,
		int		outKeyLength)
	{
		final	int	INDEX_LENGTH	= 4;

		// Validate length of derived key
		if ((outKeyLength <= 0) || (outKeyLength % HmacSha256.HASH_VALUE_SIZE != 0))
			throw new IllegalArgumentException();

		// Copy salt to array to allow its extension with the block index
		byte[] extendedSalt = new byte[salt.length + INDEX_LENGTH];
		System.arraycopy(salt, 0, extendedSalt, 0, salt.length);

		// Repeatedly hash the input key and the salt extended with the block index
		byte[] outKey = new byte[outKeyLength];
		int index = 0;
		for (int offset = 0; offset < outKeyLength; offset += HmacSha256.HASH_VALUE_SIZE)
		{
			// Extend the salt with the block index
			int value = ++index;
			for (int i = extendedSalt.length - 1; i >= salt.length; i--)
			{
				extendedSalt[i] = (byte)value;
				value >>>= 8;
			}

			// Repeatedly hash the input key and extended salt
			byte[] result = new byte[HmacSha256.HASH_VALUE_SIZE];
			byte[] hashValue = extendedSalt;
			for (int i = 0; i < numIterations; i++)
			{
				hashValue = hmacSha256(key, hashValue);
				for (int j = 0; j < HmacSha256.HASH_VALUE_SIZE; j++)
					result[j] ^= hashValue[j];
			}

			// Copy the result to the output array
			System.arraycopy(result, 0, outKey, offset, HmacSha256.HASH_VALUE_SIZE);
		}

		// Return the derived key
		return outKey;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: NUMBER OF ROUNDS OF CORE HASH FUNCTION


	/**
	 * This is an enumeration of the possible values for the number of rounds of the core hash function, which performs
	 * the lowest level of mixing in the scrypt algorithm.
	 * <p>
	 * In the scrypt specification, the number of rounds of the core hash function is fixed at 8, but this
	 * implementation supports 8, 12, 16 and 20 rounds.
	 * </p>
	 */

	public enum CoreHashNumRounds
		implements IStringKeyed
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		/**
		 * 8 rounds.
		 */
		_8  (8),

		/**
		 * 12 rounds.
		 */
		_12 (12),

		/**
		 * 16 rounds.
		 */
		_16 (16),

		/**
		 * 20 rounds.
		 */
		_20 (20);

		//--------------------------------------------------------------

		/** The default number of rounds of the core hash function. */
		public static final	CoreHashNumRounds	DEFAULT	= CoreHashNumRounds._8;

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		/** The number of rounds. */
		private	int	value;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of an enumeration constant for the number of rounds of the core hash function.
		 *
		 * @param value
		 *          the number of rounds.
		 */

		private CoreHashNumRounds(
			int	value)
		{
			// Initialise instance variables
			this.value = value;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the enumeration constant corresponding to the specified number of rounds.
		 *
		 * @param  numRounds
		 *           the number of rounds for which the enumeration constant is sought.
		 * @return the enumeration constant corresponding to the specified number of rounds, or {@code null} if {@code
		 *         numRounds} does not correspond to a supported value.
		 */

		public static CoreHashNumRounds forNumRounds(
			int	numRounds)
		{
			return Stream.of(values())
							.filter(value -> value.value == numRounds)
							.findFirst()
							.orElse(null);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IStringKeyed interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the key of this enumeration constant.
		 *
		 * @return the key of this enumeration constant.
		 */

		@Override
		public String getKey()
		{
			return Integer.toString(value);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns a string representation of this enumeration constant.
		 *
		 * @return a string representation of this enumeration constant.
		 */

		@Override
		public String toString()
		{
			return getKey();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the number of rounds of the core hash function.
		 *
		 * @return the number of rounds of the core hash function.
		 */

		public int getValue()
		{
			return value;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member interfaces
////////////////////////////////////////////////////////////////////////


	// INTERFACE: CORE HASH FUNCTION


	/**
	 * This functional interface defines the method that must be implemented by a core hash function of the
	 * <i>scrypt</i> key-derivation function.
	 */

	@FunctionalInterface
	public interface ICoreHashFunction
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a hash of the specified input data and stores it in the specified array.
		 *
		 * @param inData
		 *          the input data that will be hashed.
		 * @param outData
		 *          a hash of the input data.
		 * @param numRounds
		 *          the number of rounds of the hash function.
		 */

		void hash(
			int[]	inData,
			int[]	outData,
			int		numRounds);

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: FUNCTION PARAMETERS


	/**
	 * This class encapsulates the three parameters of the scrypt algorithm: CPU/memory cost, block size and
	 * parallelisation.
	 * <p>
	 * In this class, the parameters are public fields, named as follows:
	 * </p>
	 * <ul>
	 *   <li>cost : CPU/memory cost, <i>N</i></li>
	 *   <li>numBlocks : block size, <i>r</i></li>
	 *   <li>numSuperblocks : parallelisation, <i>p</i></li>
	 * </ul>
	 * <p>
	 * <b>Note:</b><br>
	 * In this implementation of the scrypt algorithm, the CPU/memory cost parameter is the binary logarithm of the
	 * <i>N</i> parameter in the scrypt specification; that is, the value of <i>N</i> in the scrypt specification is 2
	 * raised to the power of the value of the parameter in this implementation.
	 * </p>
	 */

	public static class Params
		implements Cloneable
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		/** The binary logarithm of the scrypt CPU/memory cost parameter, <i>N</i>. */
		private	int	cost;

		/** The number of blocks: the scrypt block size parameter, <i>r</i>. */
		private	int	numBlocks;

		/** The number of parallel superblocks: the scrypt parallelisation parameter, <i>p</i>. */
		private	int	numSuperblocks;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a set of parameters of the scrypt algorithm with the specified values.
		 *
		 * @param cost
		 *          the binary logarithm of the scrypt CPU/memory cost parameter, <i>N</i>.
		 * @param numBlocks
		 *          the number of blocks: the scrypt block size parameter, <i>r</i>.
		 * @param numSuperblocks
		 *          the number of parallel superblocks: the scrypt parallelisation parameter, <i>p</i>.
		 */

		public Params(
			int	cost,
			int	numBlocks,
			int	numSuperblocks)
		{
			// Initialise instance variables
			this.cost = cost;
			this.numBlocks = numBlocks;
			this.numSuperblocks = numSuperblocks;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a copy of this set of scrypt parameters.
		 *
		 * @return a copy of this set of scrypt parameters.
		 */

		@Override
		public Params clone()
		{
			try
			{
				return (Params)super.clone();
			}
			catch (CloneNotSupportedException e)
			{
				throw new UnexpectedRuntimeException();
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the binary logarithm of the scrypt CPU/memory cost parameter, <i>N</i>.
		 *
		 * @return the binary logarithm of the scrypt CPU/memory cost parameter, <i>N</i>.
		 */

		public int getCost()
		{
			return cost;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the number of blocks: the scrypt block size parameter, <i>r</i>.
		 *
		 * @return the number of blocks: the scrypt block size parameter, <i>r</i>.
		 */

		public int getNumBlocks()
		{
			return numBlocks;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the number of parallel superblocks: the scrypt parallelisation parameter, <i>p</i>.
		 *
		 * @return the number of parallel superblocks: the scrypt parallelisation parameter, <i>p</i>.
		 */

		public int getNumSuperblocks()
		{
			return numSuperblocks;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: KEY GENERATOR


	/**
	 * This class implements a convenient mechanism for running the scrypt key-derivation function (KDF) on its own
	 * thread.
	 * <p>
	 * It is intended that the KDF be run by invoking the {@link KeyGenerator#run() run()} method of this class.  The
	 * {@link KeyGenerator#run() run()} method handles the two expected error conditions, {@code
	 * IllegalArgumentException} and {@code OutOfMemoryError}, by setting flags that can be tested by the caller.
	 * </p>
	 */

	public class KeyGenerator
		implements Runnable
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		/** The key from which the generated key is derived. */
		private	byte[]			key;

		/** The salt from which the generated key is derived. */
		private	byte[]			salt;

		/** The parameters of the key-derivation function. */
		private	Scrypt.Params	params;

		/** The maximum number of threads that should be allocated for the processing of parallel superblocks in the
			key-derivation function. */
		private	int				maxNumThreads;

		/** The length (in bytes) of the derived key. */
		private	int				outKeyLength;

		/** The derived key. */
		private	byte[]			derivedKey;

		/** Flag: if {@code true}, a parameter of the key-derivation function was invalid. */
		private	boolean			invalidParameterValue;

		/** Flag: if {@code true}, an {@code OutOfMemoryError} was thrown. */
		private	boolean			outOfMemory;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of {@code KeyGenerator} for deriving a key from the specified key and salt by means of
		 * the scrypt key-derivation function with the specified KDF parameters.
		 *
		 * @param key
		 *          the key from which the generated key will be derived.
		 * @param salt
		 *          the salt from which the generated key will be derived.
		 * @param params
		 *          the parameters of the key-derivation function.
		 * @param maxNumThreads
		 *          the maximum number of threads that should be allocated for the processing of parallel superblocks in
		 *          the key-derivation function.
		 * @param outKeyLength
		 *          the length (in bytes) of the derived key, which must be a positive integral multiple of 32.
		 */

		private KeyGenerator(
			byte[]			key,
			byte[]			salt,
			Scrypt.Params	params,
			int				maxNumThreads,
			int				outKeyLength)
		{
			// Initialise instance variables
			this.key = key;
			this.salt = salt;
			this.params = params;
			this.maxNumThreads = maxNumThreads;
			this.outKeyLength = outKeyLength;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Runs the scrypt key-derivation function using the key, salt and KDF parameters that were specified when this
		 * object was created.
		 * <p>
		 * The two expected error conditions, {@code IllegalArgumentException} and {@code OutOfMemoryError}, are handled
		 * by setting flags that can be tested by the caller with {@link #isInvalidParameterValue()} and {@code
		 * #isOutOfMemory()} respectively.
		 * </p>
		 * <p>
		 * If the KDF completes normally, the derived key can be accessed with {@link #getDerivedKey()}.
		 * </p>
		 */

		@Override
		public void run()
		{
			try
			{
				derivedKey = deriveKey(key, salt, params, maxNumThreads, outKeyLength);
			}
			catch (IllegalArgumentException e)
			{
				invalidParameterValue = true;
			}
			catch (OutOfMemoryError e)
			{
				outOfMemory = true;
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns {@code true} if a parameter of the scrypt KDF was invalid when the KDF was executed from this
		 * generator's {@link #run()} method.
		 *
		 * @return {@code true} if a parameter of the scrypt KDF was invalid when the KDF was executed from this
		 *         generator's {@link #run()} method, {@code false} otherwise.
		 * @see    #run()
		 */

		public boolean isInvalidParameterValue()
		{
			return invalidParameterValue;
		}

		//--------------------------------------------------------------

		/**
		 * Returns {@code true} if there was not enough memory for the scrypt KDF when it was executed from this
		 * generator's {@link #run()} method.
		 *
		 * @return {@code true} if there was not enough memory for the scrypt KDF when it was executed from this
		 *         generator's {@link #run()} method, {@code false} otherwise.
		 * @see    #run()
		 */

		public boolean isOutOfMemory()
		{
			return outOfMemory;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the key that was derived by the scrypt KDF when it was executed from this object's {@link #run()}
		 * method.
		 *
		 * @return the key that was derived by the scrypt KDF when it was executed from this object's {@link #run()}
		 *         method.  The returned value will be {@code null} if the KDF has not been executed or if it did not
		 *         complete normally.
		 */

		public byte[] getDerivedKey()
		{
			return derivedKey;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
