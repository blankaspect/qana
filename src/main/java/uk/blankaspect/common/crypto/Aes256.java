/*====================================================================*\

Aes256.java

AES-256 cipher class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.util.Arrays;

import uk.blankaspect.common.exception2.UnexpectedRuntimeException;

//----------------------------------------------------------------------


// AES-256 CIPHER CLASS


/**
 * This class implements the Advanced Encryption Standard (AES) block cipher with a 256-bit key.
 * <p>
 * The Advanced Encryption Standard is specified in <a
 * href="https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.197.pdf">NIST FIPS 197</a> (PDF file).
 * </p>
 */

public class Aes256
	implements Cloneable
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/**
	 * The key size (in bits) of the cipher.
	 */
	public static final		int	KEY_SIZE_BITS		= 256;

	/**
	 * The key size (in bytes) of the cipher.
	 */
	public static final		int	KEY_SIZE			= KEY_SIZE_BITS / Byte.SIZE;

	/**
	 * The block size (in bits) of the cipher.
	 */
	public static final		int	BLOCK_SIZE_BITS		= 128;

	/**
	 * The block size (in bytes) of the cipher.
	 */
	public static final		int	BLOCK_SIZE			= BLOCK_SIZE_BITS / Byte.SIZE;

	private static final	int	NUM_KEY_COLUMNS		= KEY_SIZE / 4;
	private static final	int	NUM_BLOCK_COLUMNS	= BLOCK_SIZE / 4;
	private static final	int	NUM_ROUNDS			= 14;
	private static final	int	NUM_ROUND_KEY_WORDS	= (NUM_ROUNDS + 1) * NUM_BLOCK_COLUMNS;

	private static final	int	REDUCING_DIVISOR	= 0x011B;

	private static final	int	NUM_S_BOX_ROWS		= 16;
	private static final	int	NUM_S_BOX_COLUMNS	= 16;
	private static final	int	S_BOX_SIZE			= NUM_S_BOX_ROWS * NUM_S_BOX_COLUMNS;

	private static final	byte[]	S_BOX	=
	{
		(byte)0x63, (byte)0x7C, (byte)0x77, (byte)0x7B, (byte)0xF2, (byte)0x6B, (byte)0x6F, (byte)0xC5,
		(byte)0x30, (byte)0x01, (byte)0x67, (byte)0x2B, (byte)0xFE, (byte)0xD7, (byte)0xAB, (byte)0x76,
		(byte)0xCA, (byte)0x82, (byte)0xC9, (byte)0x7D, (byte)0xFA, (byte)0x59, (byte)0x47, (byte)0xF0,
		(byte)0xAD, (byte)0xD4, (byte)0xA2, (byte)0xAF, (byte)0x9C, (byte)0xA4, (byte)0x72, (byte)0xC0,
		(byte)0xB7, (byte)0xFD, (byte)0x93, (byte)0x26, (byte)0x36, (byte)0x3F, (byte)0xF7, (byte)0xCC,
		(byte)0x34, (byte)0xA5, (byte)0xE5, (byte)0xF1, (byte)0x71, (byte)0xD8, (byte)0x31, (byte)0x15,
		(byte)0x04, (byte)0xC7, (byte)0x23, (byte)0xC3, (byte)0x18, (byte)0x96, (byte)0x05, (byte)0x9A,
		(byte)0x07, (byte)0x12, (byte)0x80, (byte)0xE2, (byte)0xEB, (byte)0x27, (byte)0xB2, (byte)0x75,
		(byte)0x09, (byte)0x83, (byte)0x2C, (byte)0x1A, (byte)0x1B, (byte)0x6E, (byte)0x5A, (byte)0xA0,
		(byte)0x52, (byte)0x3B, (byte)0xD6, (byte)0xB3, (byte)0x29, (byte)0xE3, (byte)0x2F, (byte)0x84,
		(byte)0x53, (byte)0xD1, (byte)0x00, (byte)0xED, (byte)0x20, (byte)0xFC, (byte)0xB1, (byte)0x5B,
		(byte)0x6A, (byte)0xCB, (byte)0xBE, (byte)0x39, (byte)0x4A, (byte)0x4C, (byte)0x58, (byte)0xCF,
		(byte)0xD0, (byte)0xEF, (byte)0xAA, (byte)0xFB, (byte)0x43, (byte)0x4D, (byte)0x33, (byte)0x85,
		(byte)0x45, (byte)0xF9, (byte)0x02, (byte)0x7F, (byte)0x50, (byte)0x3C, (byte)0x9F, (byte)0xA8,
		(byte)0x51, (byte)0xA3, (byte)0x40, (byte)0x8F, (byte)0x92, (byte)0x9D, (byte)0x38, (byte)0xF5,
		(byte)0xBC, (byte)0xB6, (byte)0xDA, (byte)0x21, (byte)0x10, (byte)0xFF, (byte)0xF3, (byte)0xD2,
		(byte)0xCD, (byte)0x0C, (byte)0x13, (byte)0xEC, (byte)0x5F, (byte)0x97, (byte)0x44, (byte)0x17,
		(byte)0xC4, (byte)0xA7, (byte)0x7E, (byte)0x3D, (byte)0x64, (byte)0x5D, (byte)0x19, (byte)0x73,
		(byte)0x60, (byte)0x81, (byte)0x4F, (byte)0xDC, (byte)0x22, (byte)0x2A, (byte)0x90, (byte)0x88,
		(byte)0x46, (byte)0xEE, (byte)0xB8, (byte)0x14, (byte)0xDE, (byte)0x5E, (byte)0x0B, (byte)0xDB,
		(byte)0xE0, (byte)0x32, (byte)0x3A, (byte)0x0A, (byte)0x49, (byte)0x06, (byte)0x24, (byte)0x5C,
		(byte)0xC2, (byte)0xD3, (byte)0xAC, (byte)0x62, (byte)0x91, (byte)0x95, (byte)0xE4, (byte)0x79,
		(byte)0xE7, (byte)0xC8, (byte)0x37, (byte)0x6D, (byte)0x8D, (byte)0xD5, (byte)0x4E, (byte)0xA9,
		(byte)0x6C, (byte)0x56, (byte)0xF4, (byte)0xEA, (byte)0x65, (byte)0x7A, (byte)0xAE, (byte)0x08,
		(byte)0xBA, (byte)0x78, (byte)0x25, (byte)0x2E, (byte)0x1C, (byte)0xA6, (byte)0xB4, (byte)0xC6,
		(byte)0xE8, (byte)0xDD, (byte)0x74, (byte)0x1F, (byte)0x4B, (byte)0xBD, (byte)0x8B, (byte)0x8A,
		(byte)0x70, (byte)0x3E, (byte)0xB5, (byte)0x66, (byte)0x48, (byte)0x03, (byte)0xF6, (byte)0x0E,
		(byte)0x61, (byte)0x35, (byte)0x57, (byte)0xB9, (byte)0x86, (byte)0xC1, (byte)0x1D, (byte)0x9E,
		(byte)0xE1, (byte)0xF8, (byte)0x98, (byte)0x11, (byte)0x69, (byte)0xD9, (byte)0x8E, (byte)0x94,
		(byte)0x9B, (byte)0x1E, (byte)0x87, (byte)0xE9, (byte)0xCE, (byte)0x55, (byte)0x28, (byte)0xDF,
		(byte)0x8C, (byte)0xA1, (byte)0x89, (byte)0x0D, (byte)0xBF, (byte)0xE6, (byte)0x42, (byte)0x68,
		(byte)0x41, (byte)0x99, (byte)0x2D, (byte)0x0F, (byte)0xB0, (byte)0x54, (byte)0xBB, (byte)0x16
	};

	private static final	byte[]	INV_S_BOX	= new byte[S_BOX_SIZE];

	private static final	int[]	T1		= new int[S_BOX_SIZE];
	private static final	int[]	T2		= new int[S_BOX_SIZE];
	private static final	int[]	T3		= new int[S_BOX_SIZE];
	private static final	int[]	T4		= new int[S_BOX_SIZE];

	private static final	int[]	TI1		= new int[S_BOX_SIZE];
	private static final	int[]	TI2		= new int[S_BOX_SIZE];
	private static final	int[]	TI3		= new int[S_BOX_SIZE];
	private static final	int[]	TI4		= new int[S_BOX_SIZE];

	private static final	int[]	TKI1	= new int[S_BOX_SIZE];
	private static final	int[]	TKI2	= new int[S_BOX_SIZE];
	private static final	int[]	TKI3	= new int[S_BOX_SIZE];
	private static final	int[]	TKI4	= new int[S_BOX_SIZE];

	private static final	int	NUM_ROUND_CONSTANTS	= 30;

	private static final	byte[]	ROUND_CONSTANTS	= new byte[NUM_ROUND_CONSTANTS];

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an implementation of the AES-256 block cipher.
	 */

	public Aes256()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Pads a cipher key with trailing zeros to a length of 32 bytes, and returns the key.
	 * <p>
	 * If the length of {@code cipherKey} is less than 32 bytes, it is padded with trailing zeros to a
	 * length of 32 bytes and returned; otherwise, the key is returned unchanged.
	 * </p>
	 *
	 * @param  cipherKey  the key that will be padded.
	 * @return the key, padded with trailing zeros to a length of 32 bytes.
	 */

	public static byte[] padCipherKey(byte[] cipherKey)
	{
		byte[] key = cipherKey;
		if (cipherKey.length < KEY_SIZE)
		{
			key = new byte[KEY_SIZE];
			System.arraycopy(cipherKey, 0, key, 0, cipherKey.length);
		}
		return key;
	}

	//------------------------------------------------------------------

	/**
	 * Expands a specified cipher key into a key schedule of round keys for encryption, and returns the
	 * key schedule.
	 *
	 * @param  cipherKey  the cipher key that will be expanded into the key schedule.
	 * @return the key schedule for encryption that is created by the expansion of {@code cipherKey}.
	 * @throws IllegalArgumentException
	 *           if {@code cipherKey} is {@code null} or the length of {@code cipherKey} is not 32 bytes.
	 */

	public static int[][] createEncryptionKey(byte[] cipherKey)
	{
		return createRoundKeys(cipherKey, false);
	}

	//------------------------------------------------------------------

	/**
	 * Expands a specified cipher key into a key schedule of round keys for decryption, and returns the
	 * key schedule.
	 *
	 * @param  cipherKey  the cipher key that will be expanded into the key schedule.
	 * @return the key schedule for decryption that is created by the expansion of {@code cipherKey}.
	 * @throws IllegalArgumentException
	 *           if {@code cipherKey} is {@code null} or the length of {@code cipherKey} is not 32 bytes.
	 */

	public static int[][] createDecryptionKey(byte[] cipherKey)
	{
		return createRoundKeys(cipherKey, true);
	}

	//------------------------------------------------------------------

	/**
	 * Encrypts a specified 128-bit block of data with a specified key schedule.
	 *
	 * @param inData     the data that will be encrypted.
	 * @param inOffset   the offset of the start of the input data in {@code inData}.
	 * @param outBuffer  the buffer in which the encrypted data will be stored.
	 * @param outOffset  the offset in {@code outBuffer} at which the first byte of encrypted data will be
	 *                   stored.
	 * @param roundKeys  the key schedule of round keys that will be used for encryption.
	 * @see   #decryptBlock(byte[], int, byte[], int, int[][])
	 */

	public static void encryptBlock(byte[]  inData,
									int     inOffset,
									byte[]  outBuffer,
									int     outOffset,
									int[][] roundKeys)
	{
		// Apply first round key to plaintext
		int[] roundKey = roundKeys[0];
		int a0 = (inData[inOffset++]         << 24 |
				  (inData[inOffset++] & 0xFF) << 16 |
				  (inData[inOffset++] & 0xFF) <<  8 |
				  (inData[inOffset++] & 0xFF)       ) ^ roundKey[0];
		int a1 = (inData[inOffset++]         << 24 |
				  (inData[inOffset++] & 0xFF) << 16 |
				  (inData[inOffset++] & 0xFF) <<  8 |
				  (inData[inOffset++] & 0xFF)       ) ^ roundKey[1];
		int a2 = (inData[inOffset++]         << 24 |
				  (inData[inOffset++] & 0xFF) << 16 |
				  (inData[inOffset++] & 0xFF) <<  8 |
				  (inData[inOffset++] & 0xFF)       ) ^ roundKey[2];
		int a3 = (inData[inOffset++]         << 24 |
				  (inData[inOffset++] & 0xFF) << 16 |
				  (inData[inOffset++] & 0xFF) <<  8 |
				  (inData[inOffset++] & 0xFF)       ) ^ roundKey[3];

		// Apply transforms for each round except last
		int b0 = 0;
		int b1 = 0;
		int b2 = 0;
		int b3 = 0;
		for (int i = 1; i < NUM_ROUNDS; i++)
		{
			roundKey = roundKeys[i];
			b0 = (T1[(a0 >>> 24)       ] ^
				  T2[(a1 >>> 16) & 0xFF] ^
				  T3[(a2 >>>  8) & 0xFF] ^
				  T4[ a3         & 0xFF]) ^ roundKey[0];
			b1 = (T1[(a1 >>> 24)       ] ^
				  T2[(a2 >>> 16) & 0xFF] ^
				  T3[(a3 >>>  8) & 0xFF] ^
				  T4[ a0         & 0xFF]) ^ roundKey[1];
			b2 = (T1[(a2 >>> 24)       ] ^
				  T2[(a3 >>> 16) & 0xFF] ^
				  T3[(a0 >>>  8) & 0xFF] ^
				  T4[ a1         & 0xFF]) ^ roundKey[2];
			b3 = (T1[(a3 >>> 24)       ] ^
				  T2[(a0 >>> 16) & 0xFF] ^
				  T3[(a1 >>>  8) & 0xFF] ^
				  T4[ a2         & 0xFF]) ^ roundKey[3];
			a0 = b0;
			a1 = b1;
			a2 = b2;
			a3 = b3;
		}

		// Apply transforms for last round
		roundKey = roundKeys[NUM_ROUNDS];
		int keyWord = roundKey[0];
		outBuffer[outOffset++] = (byte)(S_BOX[(a0 >>> 24)       ] ^ (keyWord >>> 24));
		outBuffer[outOffset++] = (byte)(S_BOX[(a1 >>> 16) & 0xFF] ^ (keyWord >>> 16));
		outBuffer[outOffset++] = (byte)(S_BOX[(a2 >>>  8) & 0xFF] ^ (keyWord >>>  8));
		outBuffer[outOffset++] = (byte)(S_BOX[ a3         & 0xFF] ^  keyWord       );
		keyWord = roundKey[1];
		outBuffer[outOffset++] = (byte)(S_BOX[(a1 >>> 24)       ] ^ (keyWord >>> 24));
		outBuffer[outOffset++] = (byte)(S_BOX[(a2 >>> 16) & 0xFF] ^ (keyWord >>> 16));
		outBuffer[outOffset++] = (byte)(S_BOX[(a3 >>>  8) & 0xFF] ^ (keyWord >>>  8));
		outBuffer[outOffset++] = (byte)(S_BOX[ a0         & 0xFF] ^  keyWord       );
		keyWord = roundKey[2];
		outBuffer[outOffset++] = (byte)(S_BOX[(a2 >>> 24)       ] ^ (keyWord >>> 24));
		outBuffer[outOffset++] = (byte)(S_BOX[(a3 >>> 16) & 0xFF] ^ (keyWord >>> 16));
		outBuffer[outOffset++] = (byte)(S_BOX[(a0 >>>  8) & 0xFF] ^ (keyWord >>>  8));
		outBuffer[outOffset++] = (byte)(S_BOX[ a1         & 0xFF] ^  keyWord       );
		keyWord = roundKey[3];
		outBuffer[outOffset++] = (byte)(S_BOX[(a3 >>> 24)       ] ^ (keyWord >>> 24));
		outBuffer[outOffset++] = (byte)(S_BOX[(a0 >>> 16) & 0xFF] ^ (keyWord >>> 16));
		outBuffer[outOffset++] = (byte)(S_BOX[(a1 >>>  8) & 0xFF] ^ (keyWord >>>  8));
		outBuffer[outOffset++] = (byte)(S_BOX[ a2         & 0xFF] ^  keyWord       );
	}

	//------------------------------------------------------------------

	/**
	 * Decrypts a specified 128-bit block of data with a specified key schedule.
	 *
	 * @param inData     the data that will be decrypted.
	 * @param inOffset   the offset of the start of the input data in {@code inData}.
	 * @param outBuffer  the buffer in which the decrypted data will be stored.
	 * @param outOffset  the offset in {@code outBuffer} at which the first byte of decrypted data will be
	 *                   stored.
	 * @param roundKeys  the key schedule of round keys that will be used for decryption.
	 * @see   #encryptBlock(byte[], int, byte[], int, int[][])
	 */

	public static void decryptBlock(byte[]  inData,
									int     inOffset,
									byte[]  outBuffer,
									int     outOffset,
									int[][] roundKeys)
	{
		// Apply first round key to ciphertext
		int[] roundKey = roundKeys[0];
		int a0 = (inData[inOffset++]         << 24 |
				  (inData[inOffset++] & 0xFF) << 16 |
				  (inData[inOffset++] & 0xFF) <<  8 |
				  (inData[inOffset++] & 0xFF)       ) ^ roundKey[0];
		int a1 = (inData[inOffset++]         << 24 |
				  (inData[inOffset++] & 0xFF) << 16 |
				  (inData[inOffset++] & 0xFF) <<  8 |
				  (inData[inOffset++] & 0xFF)       ) ^ roundKey[1];
		int a2 = (inData[inOffset++]         << 24 |
				  (inData[inOffset++] & 0xFF) << 16 |
				  (inData[inOffset++] & 0xFF) <<  8 |
				  (inData[inOffset++] & 0xFF)       ) ^ roundKey[2];
		int a3 = (inData[inOffset++]         << 24 |
				  (inData[inOffset++] & 0xFF) << 16 |
				  (inData[inOffset++] & 0xFF) <<  8 |
				  (inData[inOffset++] & 0xFF)       ) ^ roundKey[3];

		// Apply transforms for each round except last
		int b0 = 0;
		int b1 = 0;
		int b2 = 0;
		int b3 = 0;
		for (int i = 1; i < NUM_ROUNDS; i++)
		{
			roundKey = roundKeys[i];
			b0 = (TI1[(a0 >>> 24)       ] ^
				  TI2[(a3 >>> 16) & 0xFF] ^
				  TI3[(a2 >>>  8) & 0xFF] ^
				  TI4[ a1         & 0xFF]) ^ roundKey[0];
			b1 = (TI1[(a1 >>> 24)       ] ^
				  TI2[(a0 >>> 16) & 0xFF] ^
				  TI3[(a3 >>>  8) & 0xFF] ^
				  TI4[ a2         & 0xFF]) ^ roundKey[1];
			b2 = (TI1[(a2 >>> 24)       ] ^
				  TI2[(a1 >>> 16) & 0xFF] ^
				  TI3[(a0 >>>  8) & 0xFF] ^
				  TI4[ a3         & 0xFF]) ^ roundKey[2];
			b3 = (TI1[(a3 >>> 24)       ] ^
				  TI2[(a2 >>> 16) & 0xFF] ^
				  TI3[(a1 >>>  8) & 0xFF] ^
				  TI4[ a0         & 0xFF]) ^ roundKey[3];
			a0 = b0;
			a1 = b1;
			a2 = b2;
			a3 = b3;
		}

		// Apply transforms for last round
		roundKey = roundKeys[NUM_ROUNDS];
		int keyWord = roundKey[0];
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a0 >>> 24)       ] ^ (keyWord >>> 24));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a3 >>> 16) & 0xFF] ^ (keyWord >>> 16));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a2 >>>  8) & 0xFF] ^ (keyWord >>>  8));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[ a1         & 0xFF] ^  keyWord       );
		keyWord = roundKey[1];
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a1 >>> 24)       ] ^ (keyWord >>> 24));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a0 >>> 16) & 0xFF] ^ (keyWord >>> 16));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a3 >>>  8) & 0xFF] ^ (keyWord >>>  8));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[ a2         & 0xFF] ^  keyWord       );
		keyWord = roundKey[2];
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a2 >>> 24)       ] ^ (keyWord >>> 24));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a1 >>> 16) & 0xFF] ^ (keyWord >>> 16));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a0 >>>  8) & 0xFF] ^ (keyWord >>>  8));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[ a3         & 0xFF] ^  keyWord       );
		keyWord = roundKey[3];
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a3 >>> 24)       ] ^ (keyWord >>> 24));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a2 >>> 16) & 0xFF] ^ (keyWord >>> 16));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[(a1 >>>  8) & 0xFF] ^ (keyWord >>>  8));
		outBuffer[outOffset++] = (byte)(INV_S_BOX[ a0         & 0xFF] ^  keyWord       );
	}

	//------------------------------------------------------------------

	/**
	 * Expands a specified cipher key into a key schedule of round keys, and returns the round keys in a
	 * two-dimensional array.
	 * <p>
	 * The returned array of round keys has the dimensions (number of rounds + 1) x (number of columns) =
	 * (14 + 1) x 4.
	 * </p>
	 *
	 * @param  cipherKey  the cipher key that will be expanded.
	 * @param  decrypt    if {@code true}, the cipher key will be expanded for decryption; if {@code false},
	 *                    the cipher key will be expanded for encryption.
	 * @return a key schedule (a two-dimensional array of round keys).
	 * @throws IllegalArgumentException
	 *           if {@code cipherKey} is {@code null} or the length of {@code cipherKey} is not 32 bytes.
	 */

	private static int[][] createRoundKeys(byte[]  cipherKey,
										   boolean decrypt)
	{
		// Validate arguments
		if ((cipherKey == null) || (cipherKey.length != KEY_SIZE))
			throw new IllegalArgumentException();

		// Copy cipher key into word array
		int[] keyWords = new int[NUM_KEY_COLUMNS];
		for (int i = 0, j = 0; i < NUM_KEY_COLUMNS;)
			keyWords[i++] = cipherKey[j++]         << 24 |
						   (cipherKey[j++] & 0xFF) << 16 |
						   (cipherKey[j++] & 0xFF) <<  8 |
						   (cipherKey[j++] & 0xFF);

		// Perform key expansion to generate round keys from cipher key
		int[][] key = new int[NUM_ROUNDS + 1][NUM_BLOCK_COLUMNS];
		int k = 0;
		for (int j = 0; j < NUM_KEY_COLUMNS; j++, k++)
		{
			int i = decrypt ? NUM_ROUNDS - (k / NUM_BLOCK_COLUMNS) : k / NUM_BLOCK_COLUMNS;
			key[i][k % NUM_BLOCK_COLUMNS] = keyWords[j];
		}

		int rcIndex = 0;
		while (k < NUM_ROUND_KEY_WORDS)
		{
			int keyWord = keyWords[NUM_KEY_COLUMNS - 1];
			keyWords[0] ^= (S_BOX[(keyWord >>> 16) & 0xFF] & 0xFF) << 24 ^
						   (S_BOX[(keyWord >>>  8) & 0xFF] & 0xFF) << 16 ^
						   (S_BOX[ keyWord         & 0xFF] & 0xFF) <<  8 ^
						   (S_BOX[(keyWord >>> 24)       ] & 0xFF)       ^
						   ROUND_CONSTANTS[rcIndex++] << 24;
			for (int j = 0, i = j + 1; i < NUM_KEY_COLUMNS / 2;)
				keyWords[i++] ^= keyWords[j++];
			keyWord = keyWords[NUM_KEY_COLUMNS / 2 - 1];
			keyWords[NUM_KEY_COLUMNS / 2] ^= (S_BOX[ keyWord         & 0xFF] & 0xFF)       ^
											 (S_BOX[(keyWord >>>  8) & 0xFF] & 0xFF) << 8  ^
											 (S_BOX[(keyWord >>> 16) & 0xFF] & 0xFF) << 16 ^
											  S_BOX[(keyWord >>> 24) & 0xFF]         << 24;
			for (int j = NUM_KEY_COLUMNS / 2, i = j + 1; i < NUM_KEY_COLUMNS;)
				keyWords[i++] ^= keyWords[j++];

			for (int j = 0; (j < NUM_KEY_COLUMNS) && (k < NUM_ROUND_KEY_WORDS); j++, k++)
			{
				int i = decrypt ? NUM_ROUNDS - (k / NUM_BLOCK_COLUMNS) : k / NUM_BLOCK_COLUMNS;
				key[i][k % NUM_BLOCK_COLUMNS] = keyWords[j];
			}
		}

		// Perform InvMixColumns on decryption key
		if (decrypt)
		{
			for (int i = 1; i < NUM_ROUNDS; i++)
			{
				for (int j = 0; j < NUM_BLOCK_COLUMNS; j++)
				{
					int keyWord = key[i][j];
					key[i][j] = TKI1[(keyWord >>> 24)       ] ^
								TKI2[(keyWord >>> 16) & 0xFF] ^
								TKI3[(keyWord >>>  8) & 0xFF] ^
								TKI4[ keyWord         & 0xFF];
				}
			}
		}

		return key;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a copy of this AES-256 cipher.
	 *
	 * @return a copy of this AES-256 cipher.
	 */

	@Override
	public Aes256 clone()
	{
		try
		{
			Aes256 copy = (Aes256)super.clone();

			if (encryptionKeys != null)
			{
				copy.encryptionKeys = new int[encryptionKeys.length][];
				for (int i = 0; i < encryptionKeys.length; i++)
					copy.encryptionKeys[i] = encryptionKeys[i].clone();
			}

			if (decryptionKeys != null)
			{
				copy.decryptionKeys = new int[decryptionKeys.length][];
				for (int i = 0; i < decryptionKeys.length; i++)
					copy.decryptionKeys[i] = decryptionKeys[i].clone();
			}

			return copy;
		}
		catch (CloneNotSupportedException e)
		{
			throw new UnexpectedRuntimeException(e);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if this cipher is equal to a specified object.
	 * <p>
	 * This cipher is considered to be equal to another object if the object is an instance of {@code
	 * Aes256} and the encryption keys of the two objects are equal and the decryption keys of the two
	 * objects are equal.
	 * </p>
	 *
	 * @return {@code true} if this cipher is equal to the specified object, {@code false} otherwise.
	 */

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Aes256)
		{
			Aes256 aes = (Aes256)obj;
			return (Arrays.deepEquals(encryptionKeys, aes.encryptionKeys) &&
					 Arrays.deepEquals(decryptionKeys, aes.decryptionKeys));
		}
		return false;
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
		return (Arrays.deepHashCode(encryptionKeys) ^ Arrays.deepHashCode(decryptionKeys));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Resets the encryption keys and decryption keys of this cipher.
	 */

	public void reset()
	{
		encryptionKeys = null;
		decryptionKeys = null;
	}

	//------------------------------------------------------------------

	/**
	 * Sets the encryption keys of this cipher by expanding a specified cipher key.
	 *
	 * @param  cipherKey  the cipher key that will be expanded to create the encryption keys.
	 * @throws IllegalArgumentException
	 *           if {@code cipherKey} is {@code null} or the length of {@code cipherKey} is not 32 bytes.
	 * @see    #setDecryptionKey(byte[])
	 */

	public void setEncryptionKey(byte[] cipherKey)
	{
		encryptionKeys = createRoundKeys(cipherKey, false);
	}

	//------------------------------------------------------------------

	/**
	 * Sets the decryption keys of this cipher by expanding a specified cipher key.
	 *
	 * @param  cipherKey  the cipher key that will be expanded to create the decryption keys.
	 * @throws IllegalArgumentException
	 *           if {@code cipherKey} is {@code null} or the length of {@code cipherKey} is not 32 bytes.
	 * @see    #setEncryptionKey(byte[])
	 */

	public void setDecryptionKey(byte[] cipherKey)
	{
		decryptionKeys = createRoundKeys(cipherKey, true);
	}

	//------------------------------------------------------------------

	/**
	 * Encrypts a specified 128-bit block of data with the encryption keys that have been set on this
	 * cipher.
	 *
	 * @param  inData     the data that will be encrypted.
	 * @param  inOffset   the offset of the start of the input data in {@code inData}.
	 * @param  outBuffer  the buffer in which the encrypted data will be stored.
	 * @param  outOffset  the offset in {@code outBuffer} at which the first byte of encrypted data will be
	 *                    stored.
	 * @throws IllegalStateException
	 *           if no encryption keys have been set on this cipher.
	 * @see    #decryptBlock(byte[], int, byte[], int)
	 */

	public void encryptBlock(byte[] inData,
							 int    inOffset,
							 byte[] outBuffer,
							 int    outOffset)
	{
		if (encryptionKeys == null)
			throw new IllegalStateException();

		encryptBlock(inData, inOffset, outBuffer, outOffset, encryptionKeys);
	}

	//------------------------------------------------------------------

	/**
	 * Decrypts a specified 128-bit block of data with the decryption keys that have been set on this
	 * cipher.
	 *
	 * @param  inData     the data that will be decrypted.
	 * @param  inOffset   the offset of the start of the input data in {@code inData}.
	 * @param  outBuffer  the buffer in which the decrypted data will be stored.
	 * @param  outOffset  the offset in {@code outBuffer} at which the first byte of decrypted data will be
	 *                    stored.
	 * @throws IllegalStateException
	 *           if no decryption keys have been set on this cipher.
	 * @see    #encryptBlock(byte[], int, byte[], int)
	 */

	public void decryptBlock(byte[] inData,
							 int    inOffset,
							 byte[] outBuffer,
							 int    outOffset)
	{
		if (decryptionKeys == null)
			throw new IllegalStateException();

		decryptBlock(inData, inOffset, outBuffer, outOffset, decryptionKeys);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Static initialiser
////////////////////////////////////////////////////////////////////////

	static
	{
		// Initialise inverted S-box and transforms
		for (int i = 0; i < S_BOX.length; i++)
		{
			int s = S_BOX[i] & 0xFF;
			INV_S_BOX[s] = (byte)i;

			int s2 = s << 1;
			if (s2 > 0xFF)
				s2 ^= REDUCING_DIVISOR;
			int s3 = s2 ^ s;
			int t = (s2 << 24) | (s << 16) | (s << 8) | s3;
			T1[i] = t;
			T2[i] = (t >>>  8) | (t << 24);
			T3[i] = (t >>> 16) | (t << 16);
			T4[i] = (t >>> 24) | (t <<  8);

			int i2 = i << 1;
			if (i2 > 0xFF)
				i2 ^= REDUCING_DIVISOR;
			int i4 = i2 << 1;
			if (i4 > 0xFF)
				i4 ^= REDUCING_DIVISOR;
			int i8 = i4 << 1;
			if (i8 > 0xFF)
				i8 ^= REDUCING_DIVISOR;
			int i9 = i8 ^ i;
			int iB = i9 ^ i2;
			int iD = i9 ^ i4;
			int iE = i8 ^ i4 ^ i2;
			t = (iE << 24) | (i9 << 16) | (iD << 8) | iB;
			TI1[s] = TKI1[i] = t;
			TI2[s] = TKI2[i] = (t >>>  8) | (t << 24);
			TI3[s] = TKI3[i] = (t >>> 16) | (t << 16);
			TI4[s] = TKI4[i] = (t >>> 24) | (t <<  8);
		}

		// Initialise round constants
		int rc = 1;
		ROUND_CONSTANTS[0] = 1;
		for (int i = 1; i < NUM_ROUND_CONSTANTS; i++)
		{
			rc <<= 1;
			if (rc > 0xFF)
				rc ^= REDUCING_DIVISOR;
			ROUND_CONSTANTS[i] = (byte)rc;
		}
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	int[][]	encryptionKeys;
	private	int[][]	decryptionKeys;

}

//----------------------------------------------------------------------
