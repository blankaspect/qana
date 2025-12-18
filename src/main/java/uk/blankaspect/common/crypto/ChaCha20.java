/*====================================================================*\

ChaCha20.java

Class: ChaCha20 stream cipher.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Arrays;

import uk.blankaspect.common.exception2.UnexpectedRuntimeException;

//----------------------------------------------------------------------


// CLASS: CHACHA20 STREAM CIPHER


/**
 * This class implements the ChaCha20 stream cipher with 20 rounds and a 256-bit key.
 */

public class ChaCha20
	implements Cloneable
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The number of bytes per word, as defined in the ChaCha20 specification. */
	public static final		int		BYTES_PER_WORD	= 4;

	/** The size (in bits) of the key of the cipher. */
	public static final		int		KEY_SIZE_BITS		= 256;

	/** The size (in bytes) of the key of the cipher. */
	public static final		int		KEY_SIZE			= KEY_SIZE_BITS / Byte.SIZE;

	/** The size (in 32-bit words) of the key of the cipher. */
	public static final		int		KEY_SIZE_WORDS		= KEY_SIZE / BYTES_PER_WORD;

	/** The size (in bytes) of the of the nonce of the cipher. */
	public static final		int		NONCE_SIZE			= 12;

	/** The size (in 32-bit words) of the of the nonce of the cipher. */
	public static final		int		NONCE_SIZE_WORDS	= NONCE_SIZE / BYTES_PER_WORD;

	/** The size (in bytes) of the counter of the cipher. */
	public static final		int		COUNTER_SIZE		= 4;

	/** The counter size (in 32-bit words) of the cipher. */
	public static final		int		COUNTER_SIZE_WORDS	= COUNTER_SIZE / BYTES_PER_WORD;

	/** The size (in bytes) of the blocks of the cipher. */
	public static final		int		BLOCK_SIZE			= 64;

	/** The size (in 32-bit words) of a block of the cipher. */
	public static final		int		BLOCK_SIZE_WORDS	= BLOCK_SIZE / BYTES_PER_WORD;

	/** The offset to the key in the input block. */
	private static final	int		KEY_OFFSET		= 4;

	/** The offset to the nonce in the input block. */
	private static final	int		NONCE_OFFSET	= 13;

	/** The offset to the block counter in the input block. */
	private static final	int		COUNTER_OFFSET	= 12;

	/** The constant values that are put into the input block. */
	private static final	int[]	CONSTANT_WORDS	=
	{
		0x61707865, 0x3320646E, 0x79622D32, 0x6B206574
	};

	/** The name of the hash function. */
	private static final	String	HASH_NAME	= "SHA-256";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	/** The number of rounds of the ChaCha20 core hash function. */
	private	int		numRounds;

	/** The block counter. */
	private	long	blockCounter;

	/** The input block. */
	private	int[]	inBlock;

	/** The output block. */
	private	int[]	outBlock;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of the ChaCha20 stream cipher with a 256-bit key and the specified number of rounds of the
	 * core hash function.
	 *
	 * @param numRounds
	 *          the number of rounds of the core hash function that will be performed.
	 */

	public ChaCha20(
		int	numRounds)
	{
		// Initialise instance variables
		this.numRounds = numRounds;
		inBlock = new int[BLOCK_SIZE_WORDS];
		outBlock = new int[BLOCK_SIZE_WORDS];
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of the ChaCha20 stream cipher with a 256-bit key and the specified number of rounds of the
	 * core hash function, and initialises the cipher with the specified key and nonce.
	 *
	 * @param  numRounds
	 *           the number of rounds of the core hash function that will be performed.
	 * @param  key
	 *           the key that will be used for the cipher.
	 * @param  nonce
	 *           the nonce that will be used in the cipher.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code key} is {@code null} or the length of {@code key} is not 32, or</li>
	 *             <li>{@code nonce} is {@code null} or the length of {@code nonce} is not 12.</li>
	 *           </ul>
	 */

	public ChaCha20(
		int	numRounds,
		int	key[],
		int	nonce[])
	{
		// Call alternative constructor
		this(numRounds);

		// Initialise input block
		init(key, nonce);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of the ChaCha20 stream cipher with a 256-bit key and the specified number of rounds of the
	 * core hash function, and initialises the cipher with the specified key and nonce.
	 *
	 * @param  numRounds
	 *           the number of rounds of the core hash function that will be performed.
	 * @param  key
	 *           the key that will be used for the cipher.
	 * @param  nonce
	 *           the nonce that will be used in the cipher.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code key} is {@code null} or the length of {@code key} is not 32, or</li>
	 *             <li>{@code nonce} is {@code null} or the length of {@code nonce} is not 12.</li>
	 *           </ul>
	 */

	public ChaCha20(
		int		numRounds,
		byte	key[],
		byte	nonce[])
	{
		// Call alternative constructor
		this(numRounds);

		// Initialise input block
		init(key, nonce);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Converts the specified string to a 32-byte sequence, and returns it.  The byte sequence, which is an SHA-256 hash
	 * of the UTF-8 encoding of {@code str}, is suitable for use as the key of a ChaCha20 cipher.
	 *
	 * @param  str
	 *           the string that will be converted.
	 * @return a SHA-256 hash of the UTF-8 encoding of {@code str}.
	 * @throws RuntimeException
	 *           if the {@link MessageDigest} class does not support the SHA-256 algorithm.  (Every implementation of
	 *           the Java platform is required to support the SHA-256 algorithm.)
	 */

	public static byte[] stringToKey(
		String	str)
	{
		try
		{
			return MessageDigest.getInstance(HASH_NAME).digest(str.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new UnexpectedRuntimeException(e);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Converts the specified sequence of four bytes in little-endian format to a 32-bit word, and returns it.
	 *
	 * @param  data
	 *           the sequence of four bytes that will be converted.
	 * @param  offset
	 *           the start offset of the sequence in {@code data}.
	 * @return the 32-bit word that resulted from converting from the input sequence.
	 */

	public static int bytesToWord(
		byte[]	data,
		int		offset)
	{
		return (data[offset++] & 0xFF) | (data[offset++] & 0xFF) << 8 | (data[offset++] & 0xFF) << 16
				| (data[offset++] & 0xFF) << 24;
	}

	//------------------------------------------------------------------

	/**
	 * Converts the specified 32-bit word to a sequence of four bytes and stores the byte sequence in the specified
	 * buffer.
	 *
	 * @param value
	 *          the word value that will be converted.
	 * @param buffer
	 *          the buffer in which the byte sequence will be stored.
	 * @param offset
	 *          the offset in {@code buffer} at which the first byte of the sequence will be stored.
	 */

	public static void wordToBytes(
		int		value,
		byte[]	buffer,
		int		offset)
	{
		buffer[offset++] = (byte)value;
		buffer[offset++] = (byte)(value >>> 8);
		buffer[offset++] = (byte)(value >>> 16);
		buffer[offset++] = (byte)(value >>> 24);
	}

	//------------------------------------------------------------------

	/**
	 * Performs the specified number of rounds of the ChaCha20 core hash function on the specified block of data.
	 *
	 * @param inData
	 *          the data that will be hashed.
	 * @param outBuffer
	 *          a buffer in which the hashed data will be stored.
	 * @param numRounds
	 *          the number of rounds of the ChaCha20 core hash function that will be performed.
	 */

	public static void hash(
		int[]	inData,
		int[]	outBuffer,
		int		numRounds)
	{
		// Initialise variables from the input data
		int x0  = inData[0];
		int x1  = inData[1];
		int x2  = inData[2];
		int x3  = inData[3];
		int x4  = inData[4];
		int x5  = inData[5];
		int x6  = inData[6];
		int x7  = inData[7];
		int x8  = inData[8];
		int x9  = inData[9];
		int x10 = inData[10];
		int x11 = inData[11];
		int x12 = inData[12];
		int x13 = inData[13];
		int x14 = inData[14];
		int x15 = inData[15];

		// Transform the input data
		int r = 0;
		numRounds >>= 1;
		for (int i = 0; i < numRounds; i++)
		{
			// Quarter-round 1.1
			x0 += x4;
			x12 ^= x0;
			r = 16;
			x12 = (x12 << r) | (x12 >>> (32 - r));

			x8 += x12;
			x4 ^= x8;
			r = 12;
			x4 = (x4 << r) | (x4 >>> (32 - r));

			x0 += x4;
			x12 ^= x0;
			r = 8;
			x12 = (x12 << r) | (x12 >>> (32 - r));

			x8 += x12;
			x4 ^= x8;
			r = 7;
			x4 = (x4 << r) | (x4 >>> (32 - r));

			// Quarter-round 1.2
			x1 += x5;
			x13 ^= x1;
			r = 16;
			x13 = (x13 << r) | (x13 >>> (32 - r));

			x9 += x13;
			x5 ^= x9;
			r = 12;
			x5 = (x5 << r) | (x5 >>> (32 - r));

			x1 += x5;
			x13 ^= x1;
			r = 8;
			x13 = (x13 << r) | (x13 >>> (32 - r));

			x9 += x13;
			x5 ^= x9;
			r = 7;
			x5 = (x5 << r) | (x5 >>> (32 - r));

			// Quarter-round 1.3
			x2 += x6;
			x14 ^= x2;
			r = 16;
			x14 = (x14 << r) | (x14 >>> (32 - r));

			x10 += x14;
			x6 ^= x10;
			r = 12;
			x6 = (x6 << r) | (x6 >>> (32 - r));

			x2 += x6;
			x14 ^= x2;
			r = 8;
			x14 = (x14 << r) | (x14 >>> (32 - r));

			x10 += x14;
			x6 ^= x10;
			r = 7;
			x6 = (x6 << r) | (x6 >>> (32 - r));

			// Quarter-round 1.4
			x3 += x7;
			x15 ^= x3;
			r = 16;
			x15 = (x15 << r) | (x15 >>> (32 - r));

			x11 += x15;
			x7 ^= x11;
			r = 12;
			x7 = (x7 << r) | (x7 >>> (32 - r));

			x3 += x7;
			x15 ^= x3;
			r = 8;
			x15 = (x15 << r) | (x15 >>> (32 - r));

			x11 += x15;
			x7 ^= x11;
			r = 7;
			x7 = (x7 << r) | (x7 >>> (32 - r));

			// Quarter-round 2.1
			x0 += x5;
			x15 ^= x0;
			r = 16;
			x15 = (x15 << r) | (x15 >>> (32 - r));

			x10 += x15;
			x5 ^= x10;
			r = 12;
			x5 = (x5 << r) | (x5 >>> (32 - r));

			x0 += x5;
			x15 ^= x0;
			r = 8;
			x15 = (x15 << r) | (x15 >>> (32 - r));

			x10 += x15;
			x5 ^= x10;
			r = 7;
			x5 = (x5 << r) | (x5 >>> (32 - r));

			// Quarter-round 2.2
			x1 += x6;
			x12 ^= x1;
			r = 16;
			x12 = (x12 << r) | (x12 >>> (32 - r));

			x11 += x12;
			x6 ^= x11;
			r = 12;
			x6 = (x6 << r) | (x6 >>> (32 - r));

			x1 += x6;
			x12 ^= x1;
			r = 8;
			x12 = (x12 << r) | (x12 >>> (32 - r));

			x11 += x12;
			x6 ^= x11;
			r = 7;
			x6 = (x6 << r) | (x6 >>> (32 - r));

			// Quarter-round 2.3
			x2 += x7;
			x13 ^= x2;
			r = 16;
			x13 = (x13 << r) | (x13 >>> (32 - r));

			x8 += x13;
			x7 ^= x8;
			r = 12;
			x7 = (x7 << r) | (x7 >>> (32 - r));

			x2 += x7;
			x13 ^= x2;
			r = 8;
			x13 = (x13 << r) | (x13 >>> (32 - r));

			x8 += x13;
			x7 ^= x8;
			r = 7;
			x7 = (x7 << r) | (x7 >>> (32 - r));

			// Quarter-round 2.4
			x3 += x4;
			x14 ^= x3;
			r = 16;
			x14 = (x14 << r) | (x14 >>> (32 - r));

			x9 += x14;
			x4 ^= x9;
			r = 12;
			x4 = (x4 << r) | (x4 >>> (32 - r));

			x3 += x4;
			x14 ^= x3;
			r = 8;
			x14 = (x14 << r) | (x14 >>> (32 - r));

			x9 += x14;
			x4 ^= x9;
			r = 7;
			x4 = (x4 << r) | (x4 >>> (32 - r));
		}

		// Set the output data to the sum of the input data and the transformed data
		outBuffer[0]  = inData[0]  + x0;
		outBuffer[1]  = inData[1]  + x1;
		outBuffer[2]  = inData[2]  + x2;
		outBuffer[3]  = inData[3]  + x3;
		outBuffer[4]  = inData[4]  + x4;
		outBuffer[5]  = inData[5]  + x5;
		outBuffer[6]  = inData[6]  + x6;
		outBuffer[7]  = inData[7]  + x7;
		outBuffer[8]  = inData[8]  + x8;
		outBuffer[9]  = inData[9]  + x9;
		outBuffer[10] = inData[10] + x10;
		outBuffer[11] = inData[11] + x11;
		outBuffer[12] = inData[12] + x12;
		outBuffer[13] = inData[13] + x13;
		outBuffer[14] = inData[14] + x14;
		outBuffer[15] = inData[15] + x15;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a copy of this ChaCha20 cipher.
	 *
	 * @return a copy of this ChaCha20 cipher.
	 */

	@Override
	public ChaCha20 clone()
	{
		try
		{
			ChaCha20 copy = (ChaCha20)super.clone();
			copy.inBlock = inBlock.clone();
			copy.outBlock = outBlock.clone();
			return copy;
		}
		catch (CloneNotSupportedException e)
		{
			throw new UnexpectedRuntimeException(e);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if this cipher is equal to the specified object.
	 * <p>
	 * This cipher is considered to be equal to another object if the object is an instance of {@code ChaCha20} and the
	 * keys and nonces of the two objects are equal.
	 * </p>
	 *
	 * @return {@code true} if this cipher is equal to the specified object, {@code false} otherwise.
	 */

	@Override
	public boolean equals(
		Object	obj)
	{
		if (this == obj)
			return true;

		return (obj instanceof ChaCha20 other) && Arrays.equals(getId(), other.getId());
	}

	//------------------------------------------------------------------

	/**
	 * Returns a hash code for this object.
	 *
	 * @return a hash code for this object.
	 */

	@Override
	public int hashCode()
	{
		return Arrays.hashCode(getId());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the key of this cipher.
	 *
	 * @return the key of this cipher.
	 * @see    #getNonce()
	 */

	public byte[] getKey()
	{
		// Allocate array for key
		byte[] key = new byte[KEY_SIZE];

		// Initialise offset
		int offset = 0;

		// Get key from input block
		for (int i = KEY_OFFSET; i < KEY_OFFSET + KEY_SIZE_WORDS / 2; i++)
		{
			wordToBytes(inBlock[i], key, offset);
			offset += BYTES_PER_WORD;
		}

		// Return key
		return key;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the nonce of this cipher.
	 *
	 * @return the nonce of this cipher.
	 * @see    #getKey()
	 */

	public byte[] getNonce()
	{
		// Allocate array for nonce
		byte[] nonce = new byte[NONCE_SIZE];

		// Initialise offset
		int offset = 0;

		// Get nonce from input block
		for (int i = NONCE_OFFSET; i < NONCE_OFFSET + NONCE_SIZE_WORDS; i++)
		{
			wordToBytes(inBlock[i], nonce, offset);
			offset += BYTES_PER_WORD;
		}

		// Return nonce
		return nonce;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the value of the block counter of this cipher.
	 *
	 * @return the value of the block counter of this cipher.
	 * @see    #setBlockCounter(long)
	 * @see    #getNextBlock(byte[], int)
	 */

	public long getBlockCounter()
	{
		return blockCounter;
	}

	//------------------------------------------------------------------

	/**
	 * Sets the block counter of this cipher to the specified value.
	 *
	 * @param counter
	 *          the value to which the block counter will be set.
	 * @see   #getBlockCounter()
	 * @see   #getNextBlock(byte[], int)
	 */

	public void setBlockCounter(
		long	counter)
	{
		blockCounter = counter;
	}

	//------------------------------------------------------------------

	/**
	 * Resets this cipher.
	 * <p>
	 * The block counter is reset to zero.
	 * </p>
	 */

	public void reset()
	{
		blockCounter = 0;
	}

	//------------------------------------------------------------------

	/**
	 * Initialises this cipher with the specified key and nonce.
	 *
	 * @param  key
	 *           the key that will be used for the cipher.
	 * @param  nonce
	 *           the nonce that will be used in the cipher.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code key} is {@code null} or the length of {@code key} is not 32, or</li>
	 *             <li>{@code nonce} is {@code null} or the length of {@code nonce} is not 12.</li>
	 *           </ul>
	 * @see    #init(int[], int[])
	 */

	public void init(
		byte[]	key,
		byte[]	nonce)
	{
		// Validate arguments
		if (key == null)
			throw new IllegalArgumentException("Null key");
		if (key.length != KEY_SIZE)
			throw new IllegalArgumentException("Incorrect key size");
		if (nonce == null)
			throw new IllegalArgumentException("Null nonce");
		if (nonce.length != NONCE_SIZE)
			throw new IllegalArgumentException("Incorrect nonce size");

		// Convert key to words
		int[] keyWords = new int[KEY_SIZE_WORDS];
		int offset = 0;
		for (int i = 0; i < KEY_SIZE_WORDS; i++)
		{
			keyWords[i] = bytesToWord(key, offset);
			offset += BYTES_PER_WORD;
		}

		// Convert nonce to words
		int[] nonceWords = new int[NONCE_SIZE_WORDS];
		offset = 0;
		for (int i = 0; i < NONCE_SIZE_WORDS; i++)
		{
			nonceWords[i] = bytesToWord(nonce, offset);
			offset += BYTES_PER_WORD;
		}

		// Initialise input block
		initBlock(keyWords, nonceWords);
	}

	//------------------------------------------------------------------

	/**
	 * Initialises this cipher with the specified key and nonce.
	 *
	 * @param  key
	 *           the key that will be used for the cipher.
	 * @param  nonce
	 *           the nonce that will be used in the cipher.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code key} is {@code null} or the length of {@code key} is not 32, or</li>
	 *             <li>{@code nonce} is {@code null} or the length of {@code nonce} is not 12.</li>
	 *           </ul>
	 * @see    #init(byte[], byte[])
	 */

	public void init(
		int[]	key,
		int[]	nonce)
	{
		// Validate arguments
		if (key == null)
			throw new IllegalArgumentException("Null key");
		if (key.length != KEY_SIZE_WORDS)
			throw new IllegalArgumentException("Incorrect key size");
		if (nonce == null)
			throw new IllegalArgumentException("Null nonce");
		if (nonce.length != NONCE_SIZE_WORDS)
			throw new IllegalArgumentException("Incorrect nonce size");

		// Initialise input block
		initBlock(key, nonce);
	}

	//------------------------------------------------------------------

	/**
	 * Generates a block of data with the specified counter value and stores the resulting data as a sequence of bytes
	 * in the specified buffer.
	 *
	 * @param blockCounter
	 *          the value of the block counter that will be used for generating the block.
	 * @param buffer
	 *          the buffer in which the generated data will be stored.
	 * @param offset
	 *          the offset in {@code buffer} at which the first byte of the generated data will be stored.
	 * @see   #getNextBlock(byte[], int)
	 */

	public void getBlock(
		long	blockCounter,
		byte[]	buffer,
		int		offset)
	{
		// Set block counter in input block
		inBlock[COUNTER_OFFSET] = (int)blockCounter;

		// Perform hash
		hash(inBlock, outBlock, numRounds);

		// Copy output block to buffer
		for (int i = 0; i < BLOCK_SIZE_WORDS; i++)
		{
			wordToBytes(outBlock[i], buffer, offset);
			offset += BYTES_PER_WORD;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Generates a block of data with the current block counter of this cipher and stores the resulting data as a
	 * sequence of bytes in the specified buffer.
	 * <p>
	 * The block counter is incremented after the block is generated.
	 * </p>
	 *
	 * @param buffer
	 *          the buffer in which the generated data will be stored.
	 * @param offset
	 *          the offset in {@code buffer} at which the first byte of the generated data will be stored.
	 * @see   #getBlock(long, byte[], int)
	 * @see   #getBlockCounter()
	 */

	public void getNextBlock(
		byte[]	buffer,
		int		offset)
	{
		getBlock(blockCounter++, buffer, offset);
	}

	//------------------------------------------------------------------

	/**
	 * Initialises the input block with the specified key and nonce.
	 *
	 * @param key
	 *          the key.
	 * @param nonce
	 *          the nonce.
	 */

	private void initBlock(
		int[]	key,
		int[]	nonce)
	{
		// Reset block counter
		blockCounter = 0;

		// Initialise index
		int i = 0;

		// Constants
		for (int j = 0; j < CONSTANT_WORDS.length; j++)
			inBlock[i++] = CONSTANT_WORDS[j];

		// Key
		for (int j = 0; j < KEY_SIZE_WORDS; j++)
			inBlock[i++] = key[j];

		// Counter
		for (int j = 0; j < COUNTER_SIZE_WORDS; j++)
			inBlock[i++] = 0;

		// Nonce
		for (int j = 0; j < NONCE_SIZE_WORDS; j++)
			inBlock[i++] = nonce[j];
	}

	//------------------------------------------------------------------

	/**
	 * Returns the identifier of this cipher: a concatenation of the key and nonce.
	 *
	 * @return the identifier of this cipher: a concatenation of the key and nonce.
	 */

	private int[] getId()
	{
		// Allocate array for ID
		int[] id = new int[KEY_SIZE_WORDS + NONCE_SIZE_WORDS];

		// Initialise index
		int j = 0;

		// Get key
		for (int i = KEY_OFFSET; i < KEY_OFFSET + KEY_SIZE_WORDS / 2; i++)
			id[j++] = inBlock[i];

		// Get nonce
		for (int i = NONCE_OFFSET; i < NONCE_OFFSET + NONCE_SIZE_WORDS; i++)
			id[j++] = inBlock[i];

		// Return ID
		return id;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
