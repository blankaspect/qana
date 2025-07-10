/*====================================================================*\

StreamEncrypter.java

Stream encrypter class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import uk.blankaspect.common.bitarray.BitUtils;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.TaskCancelledException;

import uk.blankaspect.common.exception2.ValueOutOfBoundsException;

import uk.blankaspect.common.function.IProcedure1;

import uk.blankaspect.common.misc.IProgressListener;

import uk.blankaspect.common.number.NumberCodec;

//----------------------------------------------------------------------


// CLASS: STREAM ENCRYPTER


/**
 * This class provides methods for encrypting and decrypting data with a stream cipher that is based on the {@link
 * Fortuna} cryptographically secure pseudo-random number generator (PRNG).
 * <p>
 * The kind of cipher that the PRNG uses to generate random numbers is specified when an instance of this class is
 * constructed.
 * </p>
 * <p>
 * The content-encryption key (CEK) for the stream cipher that is used for encryption and decryption may be either
 * provided directly or derived from a key and a salt with the {@link Scrypt} password-based key-derivation function
 * (KDF).  If the CEK is derived with a KDF, the salt and the parameters of the KDF are written as cleartext (ie,
 * unencrypted) to the encrypted output stream.
 * </p>
 * <p>
 * The encrypted stream may have a header comprising an identifier, version number and optional supplementary data.
 * </p>
 * <p>
 * The payload is compressed with the DEFLATE algorithm before encryption.
 * </p>
 * <p>
 * In the encrypted stream, the ciphertext is followed by an HMAC-SHA256 hash value generated from the timestamp and
 * payload before encryption.  (HMAC-SHA256 is a hash-based message authentication code whose underlying function is the
 * SHA-256 cryptographic hash function.)  The hash value is used in the decryption operation to verify the
 * content-encryption key and the integrity of the ciphertext.
 * </p>
 * <p style="margin-bottom: 0.25em;">
 * The output from the encryption operation consists of the blocks listed below, in the order given.  The leading
 * characters have the following meanings: '?' indicates that the block is optional, '#' indicates that the block is
 * encrypted with the CEK, and '~' indicates that the content of the block is random bytes.
 * </p>
 * <table style="margin-top: 0.25em; padding-left: 1em;">
 *   <tbody>
 *     <tr>
 *       <td>? </td>
 *       <td style="padding-left: 0.7em;">Header</td>
 *     </tr>
 *     <tr>
 *       <td>? </td>
 *       <td style="padding-left: 0.7em;">Salt (for deriving CEK)</td>
 *     </tr>
 *     <tr>
 *       <td>? </td>
 *       <td style="padding-left: 0.7em;">KDF parameters (for deriving CEK)</td>
 *     </tr>
 *     <tr>
 *       <td>&nbsp; </td>
 *       <td style="padding-left: 0.7em;">Cipher ID</td>
 *     </tr>
 *     <tr>
 *       <td># </td>
 *       <td style="padding-left: 0.7em;">Lengths of three blocks of random padding</td>
 *     </tr>
 *     <tr>
 *       <td>~ </td>
 *       <td style="padding-left: 0.7em;">Padding 1</td>
 *     </tr>
 *     <tr>
 *       <td># </td>
 *       <td style="padding-left: 0.7em;">Timestamp</td>
 *     </tr>
 *     <tr>
 *       <td># </td>
 *       <td style="padding-left: 0.7em;">Payload</td>
 *     </tr>
 *     <tr>
 *       <td>~ </td>
 *       <td style="padding-left: 0.7em;">Padding 2</td>
 *     </tr>
 *     <tr>
 *       <td># </td>
 *       <td style="padding-left: 0.7em;">HMAC-SHA256 hash value of timestamp and payload</td>
 *     </tr>
 *     <tr>
 *       <td>~ </td>
 *       <td style="padding-left: 0.7em;">Padding 3</td>
 *     </tr>
 *   </tbody>
 * </table>
 */

public class StreamEncrypter
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The size (in bytes) of the salt that is used to derive a content-encryption key. */
	public static final		int		SALT_SIZE			= 32;

	/** The size (in bytes) of the content-encryption key that is derived from a key and a salt with the {@linkplain
		Scrypt scrypt} key-derivation function. */
	public static final		int		DERIVED_KEY_SIZE	= 256;

	/** The minimum compression level for the compression with the DEFLATE algorithm that is applied to the payload of
		an encryption operation. */
	public static final		int		MIN_COMPRESSION_LEVEL	= Deflater.BEST_SPEED;

	/** The maximum compression level for the compression with the DEFLATE algorithm that is applied to the payload of
		an encryption operation. */
	public static final		int		MAX_COMPRESSION_LEVEL	= Deflater.BEST_COMPRESSION;

	private static final	int		NUM_PADDINGS	= 3;
	private static final	int		PADDING_SIZE	= 255;
	private static final	int		MIN_LENGTH		= 512;

	private static final	int		SALT_FIELD_SIZE				= SALT_SIZE;
	private static final	int		PARAMETERS_FIELD_SIZE		= 4;
	private static final	int		CIPHER_FIELD_SIZE			= 2;
	private static final	int		PADDING_LENGTH_FIELD_SIZE	= 1;
	private static final	int		TIMESTAMP_FIELD_SIZE		= Long.SIZE / Byte.SIZE;
	private static final	int		HASH_VALUE_FIELD_SIZE		= HmacSha256.HASH_VALUE_SIZE;

	private static final	int		METADATA1_SIZE	= CIPHER_FIELD_SIZE + 3 * PADDING_LENGTH_FIELD_SIZE
															+ TIMESTAMP_FIELD_SIZE + HASH_VALUE_FIELD_SIZE;
	private static final	int		METADATA2_SIZE	= SALT_FIELD_SIZE + PARAMETERS_FIELD_SIZE;

	private static final	int		BUFFER_LENGTH	= 1 << 13;  // 8192

	private static final	int		COMBINER_BLOCK_SIZE	= 1 << 12;  // 4096

	private static final	String	DATA_STR	= "data";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	FortunaCipher			cipher;
	private	KdfParams				kdfParams;
	private	Header					header;
	private	int						compressionLevel;
	private	byte[]					hashValue;
	private	List<IProgressListener>	progressListeners;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an instance of {@link StreamEncrypter} with the specified kind of cipher but no KDF parameters or header.
	 * <p>
	 * The absence of KDF parameters means that the {@link #encrypt(IInput, IOutput, long, long, byte[], byte[],
	 * IProcedure1)} and {@link #decrypt(IInput, IOutput, long, byte[], IProcedure1)} methods and their overloaded
	 * variants will not derive a content-encryption key (CEK) from their {@code key} argument but will use the {@code
	 * key} argument directly as the CEK.
	 * </p>
	 *
	 * @param cipher
	 *          the kind of cipher that will be used by the pseudo-random number generator to generate a stream cipher
	 *          for encryption.  {@code cipher} may be {@code null} if the encrypter will not be used for encryption.
	 */

	public StreamEncrypter(
		FortunaCipher	cipher)
	{
		this(cipher, null, null);
	}

	//------------------------------------------------------------------

	/**
	 * Creates an instance of {@link StreamEncrypter} with the specified kind of cipher and KDF parameters but no
	 * header.
	 *
	 * @param cipher
	 *          the kind of cipher that will be used by the pseudo-random number generator to generate a stream cipher
	 *          for encryption.  {@code cipher} may be {@code null} if the encrypter will not be used for encryption.
	 * @param kdfParams
	 *          the parameters that will be used by the key-derivation function to derive the content-encryption key
	 *          when encrypting and decrypting a stream.  If {@code kdfParams} is {@code null}, the {@link
	 *          #encrypt(IInput, IOutput, long, long, byte[], byte[], IProcedure1)} and {@link #decrypt(IInput, IOutput,
	 *          long, byte[], IProcedure1)} methods and their overloaded variants will not derive a content-encryption
	 *          key (CEK) from their {@code key} argument but will use the {@code key} argument directly as the CEK.
	 */

	public StreamEncrypter(
		FortunaCipher	cipher,
		KdfParams		kdfParams)
	{
		this(cipher, kdfParams, null);
	}

	//------------------------------------------------------------------

	/**
	 * Creates an instance of {@link StreamEncrypter} with the specified kind of cipher and header but no KDF
	 * parameters.
	 * <p>
	 * The absence of KDF parameters means that the {@link #encrypt(IInput, IOutput, long, long, byte[], byte[],
	 * IProcedure1)} and {@link #decrypt(IInput, IOutput, long, byte[], IProcedure1)} methods and their overloaded
	 * variants will not derive a content-encryption key (CEK) from their {@code key} argument but will use the {@code
	 * key} argument directly as the CEK.
	 * </p>
	 *
	 * @param cipher
	 *          the kind of cipher that will be used by the pseudo-random number generator to generate a stream cipher
	 *          for encryption.  {@code cipher} may be {@code null} if the encrypter will not be used for encryption.
	 * @param header
	 *          the header that will be included in the output stream, in the case of encryption, or that will be used
	 *          to check the identifier and version number against, in the case of decryption.
	 */

	public StreamEncrypter(
		FortunaCipher	cipher,
		Header			header)
	{
		this(cipher, null, header);
	}

	//------------------------------------------------------------------

	/**
	 * Creates an instance of {@link StreamEncrypter} with the specified kind of cipher, KDF parameters and header.
	 *
	 * @param cipher
	 *          the kind of cipher that will be used by the pseudo-random number generator to generate a stream cipher
	 *          for encryption.  {@code cipher} may be {@code null} if the encrypter will not be used for encryption.
	 * @param kdfParams
	 *          the parameters that will be used by the key-derivation function to derive the content-encryption key
	 *          when encrypting and decrypting a stream.  If {@code kdfParams} is {@code null}, the {@link
	 *          #encrypt(IInput, IOutput, long, long, byte[], byte[], IProcedure1)} and {@link #decrypt(IInput, IOutput,
	 *          long, byte[], IProcedure1)} methods and their overloaded variants will not derive a content-encryption
	 *          key (CEK) from their {@code key} argument but will use the {@code key} argument directly as the CEK.
	 * @param header
	 *          the header that will be included in the output stream, in the case of encryption, or that will be used
	 *          to check the identifier and version number against, in the case of decryption.
	 */

	public StreamEncrypter(
		FortunaCipher	cipher,
		KdfParams		kdfParams,
		Header			header)
	{
		this.cipher = cipher;
		if (kdfParams != null)
			this.kdfParams = kdfParams.clone();
		this.header = header;
		compressionLevel = MAX_COMPRESSION_LEVEL;
		progressListeners = new ArrayList<>();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Reads data from the specified input, and stores it in a buffer.
	 *
	 * @param  input
	 *           the input object from which data will be read.
	 * @param  buffer
	 *           the buffer in which the data will be stored.
	 * @throws InputException
	 *           if an error occurs when reading from the input.
	 */

	private static void read(
		IInput	input,
		byte[]	buffer)
		throws InputException
	{
		read(input, buffer, 0, buffer.length);
	}

	//------------------------------------------------------------------

	/**
	 * Reads data from the specified input up to a specifed length, and stores it in a buffer.
	 *
	 * @param  input
	 *           the input object from which data will be read.
	 * @param  buffer
	 *           the buffer in which the data will be stored.
	 * @param  offset
	 *           the offset in {@code buffer} at which the first byte of data will be stored.
	 * @param  length
	 *           the maximum number of bytes to read.
	 * @throws InputException
	 *           if an error occurs when reading from the input.
	 */

	private static void read(
		IInput	input,
		byte[]	buffer,
		int		offset,
		int		length)
		throws InputException
	{
		try
		{
			int endOffset = offset + length;
			while (offset < endOffset)
			{
				int readLength = input.read(buffer, offset, endOffset - offset);
				if (readLength < 0)
					throw new InputException(ErrorId.PREMATURE_END_OF_DATA);
				offset += readLength;
			}
		}
		catch (IOException e)
		{
			throw new InputException(ErrorId.ERROR_READING_DATA, e);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Writes data from an array of bytes to the specified output.
	 *
	 * @param  output
	 *           the output object to whicg the data will be written.
	 * @param  data
	 *           the array of data to be written.
	 * @throws IOException
	 *           if an error occurs when writing to the output.
	 */

	private static void write(
		IOutput	output,
		byte[]	data)
		throws OutputException
	{
		write(output, data, 0, data.length);
	}

	//------------------------------------------------------------------

	/**
	 * Writes data from an array of bytes to the specified output.
	 *
	 * @param  output
	 *           the output object to which the data will be written.
	 * @param  data
	 *           an array that contains the data to be written.
	 * @param  offset
	 *           the start offset of the data in {@code data}.
	 * @param  length
	 *           the number of bytes to write.
	 * @throws IOException
	 *           if an error occurs when writing to the output.
	 */

	private static void write(
		IOutput	output,
		byte[]	data,
		int		offset,
		int		length)
		throws OutputException
	{
		try
		{
			output.write(data, offset, length);
		}
		catch (IOException e)
		{
			throw new OutputException(ErrorId.ERROR_WRITING_DATA, e);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Creates and returns a new instance of a generator that uses the scrypt key-derivation function with the specified
	 * KDF parameters to derive a content-encryption key from the specified key and salt.
	 *
	 * @param  key
	 *           the key from which the content-encryption key will be derived.
	 * @param  salt
	 *           the salt from which the content-encryption key will be derived.
	 * @param  params
	 *           the parameters of the function that will derive the content-encryption key.
	 * @return a generator of a content-encryption key.
	 */

	private static Scrypt.KeyGenerator createKeyGenerator(
		byte[]		key,
		byte[]		salt,
		KdfParams	kdfParams)
	{
		return new ScryptSalsa20(kdfParams.numRounds)
								.createKeyGenerator(key, salt, kdfParams, kdfParams.getNumThreads(), DERIVED_KEY_SIZE);
	}

	//------------------------------------------------------------------

	/**
	 * Returns an array of bit indices that will be used for permuting the bits of the timestamp.
	 *
	 * @param  numIndices
	 *           the number of indices required.
	 * @param  prng
	 *           the pseudo-random number generator that will generate the permutation of indices.
	 * @return an array of bit indices whose length is {@code numIndices}.
	 */

	private static int[] getBitIndices(
		int		numIndices,
		Fortuna	prng)
	{
		int[] indices = new int[numIndices];
		for (int i = 0; i < numIndices; i++)
		{
			int j = ((prng.getRandomByte() & 0xFF) * (i + 1)) >>> 8;
			indices[i] = indices[j];
			indices[j] = i;
		}
		return indices;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns thecompression level of the DEFLATE algorithm that is applied to the payload before encryption.
	 * <p>
	 * The compression level can have values from 1 to 9.
	 * </p>
	 *
	 * @return the compression level of the DEFLATE algorithm that is applied to the payload before encryption.
	 * @see    #setCompressionLevel(int)
	 */

	public int getCompressionLevel()
	{
		return compressionLevel;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the HMAC-SHA256 hash value of the unencrypted timestamp and payload for the last encryption or decryption
	 * operation.
	 * <p>
	 * HMAC-SHA256 is a hash-based message authentication code whose underlying function is the SHA-256 cryptographic
	 * hash function.  The hash value is an array of 32 bytes.
	 * </p>
	 * <p>
	 * The value returned by this method is updated with each encryption and decryption operation that completes
	 * successfully.
	 * </p>
	 *
	 * @return the HMAC-SHA256 hash value of the unencrypted timestamp and payload.
	 */

	public byte[] getHashValue()
	{
		return hashValue;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the minimum size of the overhead of an encrypted stream.  The overhead is the content of the stream
	 * excluding the payload.
	 * <p>
	 * The base value of the size of the overhead is the size of the metadata and the minimum amount of padding.  If KDF
	 * parameters or a header were specified when this encrypter was created, their sizes are added to the base value.
	 * </p>
	 *
	 * @return the minimum size of the overhead of an encrypted stream.
	 * @see    #getMaxOverheadSize()
	 */

	public int getMinOverheadSize()
	{
		int size = METADATA1_SIZE + PADDING_SIZE;
		if (kdfParams != null)
			size += METADATA2_SIZE;
		if (header != null)
			size += header.getSize();
		return size;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the maximum size of the overhead of an encrypted stream.  The overhead is the content of the stream
	 * excluding the payload.
	 * <p>
	 * The base value of the size of the overhead is the size of the metadata and the maximum amount of padding.  If KDF
	 * parameters or a header were specified when this encrypter was created, their sizes are added to the base value.
	 * </p>
	 *
	 * @return the maximum size of the overhead of an encrypted stream.
	 * @see    #getMinOverheadSize()
	 */

	public int getMaxOverheadSize()
	{
		return (getMinOverheadSize() + MIN_LENGTH - PADDING_SIZE);
	}

	//------------------------------------------------------------------

	/**
	 * Sets the compression level of the DEFLATE algorithm that is applied to the payload before encryption.
	 * <p>
	 * The compression level can have values from 1 to 9.
	 * </p>
	 *
	 * @param  level
	 *           the compression level that will be set, in the range [1..9].
	 * @throws IllegalArgumentException
	 *           if {@code level} is less than 1 or greater than 9.
	 * @see    #getCompressionLevel()
	 */

	public void setCompressionLevel(
		int	level)
	{
		if ((level < MIN_COMPRESSION_LEVEL) || (level > MAX_COMPRESSION_LEVEL))
			throw new IllegalArgumentException();
		compressionLevel = level;
	}

	//------------------------------------------------------------------

	/**
	 * Adds the specified progress listener to this encrypter's list of listeners.
	 * <p>
	 * The progress listeners are notified during encryption and decryption operations after each block of the payload
	 * is processed.
	 * </p>
	 *
	 * @param listener
	 *          the progress listener that will be added to the list.
	 * @see   #removeProgressListener(IProgressListener)
	 * @see   #getProgressListeners()
	 */

	public void addProgressListener(
		IProgressListener	listener)
	{
		progressListeners.add(listener);
	}

	//------------------------------------------------------------------

	/**
	 * Removes the specified progress listener from this encrypter's list of listeners.
	 * <p>
	 * The removal operation has no effect if the specified listener is not in the list.
	 * </p>
	 *
	 * @param listener
	 *          the progress listener that will be removed from the list.
	 * @see   #addProgressListener(IProgressListener)
	 * @see   #getProgressListeners()
	 */

	public void removeProgressListener(
		IProgressListener	listener)
	{
		progressListeners.remove(listener);
	}

	//------------------------------------------------------------------

	/**
	 * Returns this encrypter's list of progress listeners.
	 *
	 * @return the list of progress listeners as an array.
	 * @see    #addProgressListener(IProgressListener)
	 * @see    #removeProgressListener(IProgressListener)
	 */

	public IProgressListener[] getProgressListeners()
	{
		return progressListeners.toArray(IProgressListener[]::new);
	}

	//------------------------------------------------------------------

	/**
	 * Encrypts data from the specified input stream and writes the resulting ciphertext to the specified output stream.
	 * <p>
	 * The output stream also contains metadata and blocks of padding of random lengths in addition to the ciphertext.
	 * The composition of the output data is described in the class comment for {@link StreamEncrypter}.
	 * </p>
	 *
	 * @param  inStream
	 *           the input stream from which the data to be encrypted will be read.
	 * @param  outStream
	 *           the output stream to which the ciphertext will be written, along with metadata and blocks of random
	 *           padding.
	 * @param  length
	 *           the length of the data to be encrypted.
	 * @param  timestamp
	 *           a timestamp that will be encrypted and written to the output stream.
	 * @param  key
	 *           if parameters of the key-derivation function were specified when this encrypter was created, the key
	 *           from which the content-encryption key will be derived; otherwise, the key that will be used as the
	 *           content-encryption key.
	 * @param  randomKey
	 *           the key that will be used as a seed for the pseudo-random number generator that will generate the
	 *           random data for the salt of the KDF and for padding.
	 * @param  kdfExecutor
	 *           the executor of the key-derivation function, which is ignored if it is {@code null}.
	 * @throws AppException
	 *           if there was not enough memory for the key-derivation function to generate the content-encryption key.
	 * @throws InputException
	 *           if an error occurs when reading from the input stream.
	 * @throws OutputException
	 *           if an error occurs when writing to the output stream.
	 * @throws TaskCancelledException
	 *           if the encryption operation was cancelled by the user.
	 * @see    #encrypt(IInput, IOutput, long, long, byte[], byte[], IProcedure1)
	 * @see    #decrypt(IInput, IOutput, long, byte[], IProcedure1))
	 * @see    #decrypt(InputStream, OutputStream, long, byte[], IProcedure1)
	 */

	public void encrypt(
		InputStream				inStream,
		OutputStream			outStream,
		long					length,
		long					timestamp,
		byte[]					key,
		byte[]					randomKey,
		IProcedure1<Runnable>	kdfExecutor)
		throws AppException, InputException, OutputException, TaskCancelledException
	{
		encrypt(new InputStreamAdapter(inStream), new OutputStreamAdapter(outStream), length, timestamp, key,
				randomKey, kdfExecutor);
	}

	//------------------------------------------------------------------

	/**
	 * Encrypts data from the specified input and writes the resulting ciphertext to the specified output.
	 * <p>
	 * The output data also contains metadata and blocks of padding of random lengths in addition to the ciphertext.
	 * The composition of the output data is described in the class comment for {@link StreamEncrypter}.
	 * </p>
	 *
	 * @param  input
	 *           the input from which the data to be encrypted will be read.
	 * @param  output
	 *           the output to which the ciphertext will be written, along with metadata and blocks of random padding.
	 * @param  length
	 *           the length of the data to be encrypted.
	 * @param  timestamp
	 *           a timestamp that will be encrypted and written to the output.
	 * @param  key
	 *           if parameters of the key-derivation function were specified when this encrypter was created, the key
	 *           from which the content-encryption key will be derived; otherwise, the key that will be used as the
	 *           content-encryption key.
	 * @param  randomKey
	 *           the key that will be used as a seed for the pseudo-random number generator that will generate the
	 *           random data for the salt of the KDF and for padding.
	 * @param  kdfExecutor
	 *           the executor of the key-derivation function, which is ignored if it is {@code null}.
	 * @throws AppException
	 *           if there was not enough memory for the key-derivation function to generate the content-encryption key.
	 * @throws InputException
	 *           if an error occurs when reading from the input.
	 * @throws OutputException
	 *           if an error occurs when writing to the output.
	 * @throws TaskCancelledException
	 *           if the encryption operation was cancelled by the user.
	 * @see    #encrypt(InputStream, OutputStream, long, long, byte[], byte[], IProcedure1)
	 * @see    #decrypt(IInput, IOutput, long, byte[], IProcedure1)
	 * @see    #decrypt(InputStream, OutputStream, long, byte[], IProcedure1)
	 */

	public void encrypt(
		IInput					input,
		IOutput					output,
		long					length,
		long					timestamp,
		byte[]					key,
		byte[]					randomKey,
		IProcedure1<Runnable>	kdfExecutor)
		throws AppException, InputException, OutputException, TaskCancelledException
	{
		final	int	RANDOM_DATA_POOL_LENGTH	= 256;

		// Generate random padding lengths
		byte[] randomDataPool = new byte[RANDOM_DATA_POOL_LENGTH];
		int randomDataPoolIndex = 0;
		Fortuna prng = cipher.createPrng(randomKey);
		int[] paddingLengths = new int[NUM_PADDINGS];
		int paddingIndex = 0;
		while (true)
		{
			// Test total padding length
			int paddingLength = 0;
			for (int i = 0; i < NUM_PADDINGS; i++)
				paddingLength += paddingLengths[i];
			if ((paddingLength >= PADDING_SIZE) && (paddingLength + length >= MIN_LENGTH))
				break;

			// Fill pool of random data
			if (randomDataPoolIndex == 0)
			{
				for (int i = 0; i < RANDOM_DATA_POOL_LENGTH; i++)
					randomDataPool[i] = prng.getRandomByte();
			}

			// Update padding length with next random value
			paddingLengths[paddingIndex] ^= randomDataPool[randomDataPoolIndex] & 0xFF;
			if (++paddingIndex >= NUM_PADDINGS)
				paddingIndex = 0;
			if (++randomDataPoolIndex >= RANDOM_DATA_POOL_LENGTH)
				randomDataPoolIndex = 0;
		}

		// Encode cipher ID
		randomDataPoolIndex = 0;
		int cipherId = cipher.getId();
		byte[] cipherData = new byte[CIPHER_FIELD_SIZE];
		CRC32 crc = new CRC32();
		while (true)
		{
			// Fill pool of random data
			if (randomDataPoolIndex == 0)
			{
				for (int i = 0; i < RANDOM_DATA_POOL_LENGTH; i++)
					randomDataPool[i] = prng.getRandomByte();
			}

			// Update cipher data
			for (int i = 0; i < cipherData.length; i++)
			{
				cipherData[i] ^= randomDataPool[randomDataPoolIndex];
				if (++randomDataPoolIndex >= RANDOM_DATA_POOL_LENGTH)
					randomDataPoolIndex = 0;
			}

			// Calculate CRC of cipher data
			crc.reset();
			crc.update(cipherData);
			if ((crc.getValue() & FortunaCipher.ID_MASK) == cipherId)
				break;
		}

		// Create random padding
		byte[] padding = prng.getRandomBytes(NUM_PADDINGS * PADDING_SIZE);

		// Write header
		if (header != null)
			write(output, header.toByteArray());

		// Write salt and KDF parameters; generate encryption key
		byte[] encryptionKey = key;
		if (kdfParams != null)
		{
			// Generate and write salt
			byte[] salt = prng.getRandomBytes(SALT_FIELD_SIZE);
			write(output, salt);

			// Encode and write KDF parameters
			byte[] paramData = new byte[PARAMETERS_FIELD_SIZE];
			NumberCodec.uIntToBytesLE(kdfParams.getEncodedValue(false), paramData);
			for (int i = 0; i < paramData.length; i++)
				paramData[i] ^= salt[i];
			write(output, paramData);

			// Generate encryption key
			Scrypt.KeyGenerator keyGenerator = createKeyGenerator(key, salt, kdfParams);
			if (kdfExecutor == null)
				keyGenerator.run();
			else
				kdfExecutor.invoke(keyGenerator);
			if (keyGenerator.isOutOfMemory())
				throw new AppException(ErrorId.NOT_ENOUGH_MEMORY);
			encryptionKey = keyGenerator.getDerivedKey();
		}

		// Write cipher ID
		write(output, cipherData);

		// Create combiner from encryption key
		Fortuna.XorCombiner combiner = cipher.createCombiner(encryptionKey, COMBINER_BLOCK_SIZE);

		// Encrypt and write padding lengths
		for (int i = 0; i < NUM_PADDINGS; i++)
		{
			byte[] paddingLengthData = { (byte)paddingLengths[i]  };
			combiner.combine(paddingLengthData);
			write(output, paddingLengthData);
		}

		// Write first padding
		paddingIndex = 0;
		write(output, padding, paddingIndex * PADDING_SIZE, paddingLengths[paddingIndex]);
		++paddingIndex;

		// Rearrange bits of timestamp
		byte[] timestampData = new byte[TIMESTAMP_FIELD_SIZE];
		int[] indices = getBitIndices(Long.SIZE, combiner.getPrng());
		for (int i = 0; i < indices.length; i++)
		{
			if ((timestamp & 1L << indices[i]) != 0)
				timestampData[i >>> 3] |= 1 << (i & 0x07);
		}

		// Encrypt and write timestamp
		combiner.combine(timestampData);
		write(output, timestampData);

		// Create hash-function object
		HmacSha256 hash = new HmacSha256(encryptionKey);

		// Update hash with timestamp
		NumberCodec.uLongToBytesLE(timestamp, timestampData, 0, timestampData.length);
		hash.update(timestampData);

		// Compress and encrypt data from input stream
		Deflater compressor = new Deflater(compressionLevel, true);
		byte[] inBuffer = new byte[BUFFER_LENGTH];
		byte[] outBuffer = new byte[BUFFER_LENGTH];
		long offset = 0;
		while (offset < length)
		{
			// Test whether task has been cancelled by a monitor
			for (IProgressListener listener : progressListeners)
			{
				if (listener.isTaskCancelled())
					throw new TaskCancelledException();
			}

			// Read block of data from input stream
			int blockLength = (int)Math.min(length - offset, BUFFER_LENGTH);
			read(input, inBuffer, 0, blockLength);
			hash.update(inBuffer, 0, blockLength);

			// Compress and encrypt input data and write it to output stream
			compressor.setInput(inBuffer, 0, blockLength);
			while (true)
			{
				int outLength = compressor.deflate(outBuffer);
				if (outLength == 0)
					break;
				combiner.combine(outBuffer, 0, outLength);
				write(output, outBuffer, 0, outLength);
			}

			// Increment offset
			offset += blockLength;

			// Update progress of task
			double progress = (double)offset / (double)length;
			for (IProgressListener listener : progressListeners)
				listener.setProgress(progress);
		}

		// Write remaining compressed data
		compressor.finish();
		while (true)
		{
			int outLength = compressor.deflate(outBuffer);
			if (outLength == 0)
				break;
			combiner.combine(outBuffer, 0, outLength);
			write(output, outBuffer, 0, outLength);
		}

		// Write second padding
		write(output, padding, paddingIndex * PADDING_SIZE, paddingLengths[paddingIndex]);
		++paddingIndex;

		// Encrypt and write hash value
		hashValue = hash.getValue();
		byte[] hashValueData = hashValue.clone();
		combiner.combine(hashValueData);
		write(output, hashValueData);

		// Write third padding
		write(output, padding, paddingIndex * PADDING_SIZE, paddingLengths[paddingIndex]);
		++paddingIndex;
	}

	//------------------------------------------------------------------

	/**
	 * Decrypts data from the specified input stream and writes the resulting plaintext to the specified output stream.
	 * <p>
	 * The expected composition of the input data is described in the class comment for {@link StreamEncrypter}.
	 * </p>
	 *
	 * @param  inStream
	 *           the input stream from which the data to be decrypted will be read.
	 * @param  outStream
	 *           the output stream to which the plaintext will be written.
	 * @param  length
	 *           the length of the data to be decrypted.
	 * @param  key
	 *           if parameters of the key-derivation function were specified when this encrypter was created, the key
	 *           from which the content-encryption key will be derived; otherwise, the key that will be used as the
	 *           content-encryption key.
	 * @param  kdfExecutor
	 *           the executor of the key-derivation function, which is ignored if it is {@code null}.
	 * @return the timestamp of the input data.
	 * @throws AppException
	 *           if there was not enough memory for the key-derivation function to generate the content-encryption key.
	 * @throws InputException
	 *           if an error occurs when reading from the input stream.
	 * @throws OutputException
	 *           if an error occurs when writing to the output stream.
	 * @throws TaskCancelledException
	 *           if the decryption operation was cancelled by the user.
	 * @see    #decrypt(IInput, IOutput, long, byte[], IProcedure1)
	 * @see    #encrypt(IInput, IOutput, long, long, byte[], byte[], IProcedure1)
	 * @see    #encrypt(InputStream, OutputStream, long, long, byte[], byte[], IProcedure1)
	 */

	public long decrypt(
		InputStream				inStream,
		OutputStream			outStream,
		long					length,
		byte[]					key,
		IProcedure1<Runnable>	kdfExecutor)
		throws AppException, InputException, OutputException, TaskCancelledException
	{
		return decrypt(new InputStreamAdapter(inStream), new OutputStreamAdapter(outStream), length, key, kdfExecutor);
	}

	//------------------------------------------------------------------

	/**
	 * Decrypts data from the specified input and writes the resulting plaintext to the specified output.
	 * <p>
	 * The expected composition of the input data is described in the class comment for {@link StreamEncrypter}.
	 * </p>
	 *
	 * @param  input
	 *           the input from which the data to be decrypted will be read.
	 * @param  output
	 *           the output to which the plaintext will be written.
	 * @param  length
	 *           the length of the data to be decrypted.
	 * @param  key
	 *           if parameters of the key-derivation function were specified when this encrypter was created, the key
	 *           from which the content-encryption key will be derived; otherwise, the key that will be used as the
	 *           content-encryption key.
	 * @param  kdfExecutor
	 *           the executor of the key-derivation function, which is ignored if it is {@code null}.
	 * @return the timestamp of the input data.
	 * @throws AppException
	 *           if there was not enough memory for the key-derivation function to generate the content-encryption key.
	 * @throws InputException
	 *           if an error occurs when reading from the input.
	 * @throws OutputException
	 *           if an error occurs when writing to the output.
	 * @throws TaskCancelledException
	 *           if the decryption operation was cancelled by the user.
	 * @see    #decrypt(InputStream, OutputStream, long, byte[], IProcedure1)
	 * @see    #encrypt(IInput, IOutput, long, long, byte[], byte[], IProcedure1)
	 * @see    #encrypt(InputStream, OutputStream, long, long, byte[], byte[], IProcedure1)
	 */

	public long decrypt(
		IInput					input,
		IOutput					output,
		long					length,
		byte[]					key,
		IProcedure1<Runnable>	kdfExecutor)
		throws AppException, InputException, OutputException, TaskCancelledException
	{
		// Process header
		if (header != null)
		{
			// Read and test format identifier
			byte[] idBuffer = new byte[Header.ID_FIELD_SIZE];
			read(input, idBuffer);

			if (NumberCodec.bytesToUIntLE(idBuffer) != header.id)
				throw new InputException(ErrorId.UNEXPECTED_DATA_FORMAT);

			// Read and test version number
			byte[] versionNum = new byte[Header.VERSION_FIELD_SIZE];
			read(input, versionNum);

			int version = NumberCodec.bytesToUIntLE(versionNum);
			if (!header.isSupportedVersion(version))
				throw new InputException(ErrorId.UNSUPPORTED_DATA_VERSION, Integer.toString(version));

			// Read supplementary data
			if (header.supplementaryData != null)
				read(input, header.supplementaryData);

			// Decrement length
			length -= header.getSize();
		}

		// Read salt and KDF parameters; generate encryption key
		byte[] encryptionKey = key;
		if (kdfParams != null)
		{
			// Read salt
			byte[] salt = new byte[SALT_FIELD_SIZE];
			read(input, salt);

			// Read and decode KDF parameters
			byte[] paramData = new byte[PARAMETERS_FIELD_SIZE];
			read(input, paramData);
			for (int i = 0; i < paramData.length; i++)
				paramData[i] ^= salt[i];
			KdfParams params = new KdfParams(NumberCodec.bytesToUIntLE(paramData));
			params.maxNumThreads = kdfParams.maxNumThreads;

			// Generate encryption key
			Scrypt.KeyGenerator keyGenerator = createKeyGenerator(key, salt, params);
			if (kdfExecutor == null)
				keyGenerator.run();
			else
				kdfExecutor.invoke(keyGenerator);
			if (keyGenerator.isOutOfMemory())
				throw new AppException(ErrorId.NOT_ENOUGH_MEMORY);
			encryptionKey = keyGenerator.getDerivedKey();
			if (encryptionKey == null)
				throw new InputException(ErrorId.UNEXPECTED_DATA_FORMAT);
		}

		// Read and decode cipher ID
		byte[] cipherData = new byte[CIPHER_FIELD_SIZE];
		read(input, cipherData);
		CRC32 crc = new CRC32();
		crc.update(cipherData);
		FortunaCipher cipher = FortunaCipher.forId((int)crc.getValue() & FortunaCipher.ID_MASK);
		if (cipher == null)
			throw new InputException(ErrorId.UNRECOGNISED_CIPHER);

		// Create combiner from encryption key
		Fortuna.XorCombiner combiner = cipher.createCombiner(encryptionKey, COMBINER_BLOCK_SIZE);

		// Read and decrypt padding lengths
		int[] paddingLengths = new int[NUM_PADDINGS];
		for (int i = 0; i < NUM_PADDINGS; i++)
		{
			byte[] paddingLengthData = new byte[PADDING_LENGTH_FIELD_SIZE];
			read(input, paddingLengthData);
			combiner.combine(paddingLengthData);
			paddingLengths[i] = paddingLengthData[0] & 0xFF;
		}
		int paddingIndex = 0;

		// Skip first padding
		byte[] padding = new byte[paddingLengths[paddingIndex++]];
		read(input, padding);

		// Get indices of timestamp bits
		int[] indices = getBitIndices(Long.SIZE, combiner.getPrng());

		// Read and decrypt timestamp
		byte[] timestampData = new byte[TIMESTAMP_FIELD_SIZE];
		read(input, timestampData);
		combiner.combine(timestampData);

		// Rearrange bits of timestamp
		long timestamp = 0;
		for (int i = 0; i < indices.length; i++)
		{
			if ((timestampData[i >>> 3] & 1 << (i & 0x07)) != 0)
				timestamp |= 1L << indices[i];
		}

		// Create hash-function object
		HmacSha256 hash = new HmacSha256(encryptionKey);

		// Update hash with timestamp
		NumberCodec.uLongToBytesLE(timestamp, timestampData, 0, timestampData.length);
		hash.update(timestampData);

		// Read and decrypt payload
		Inflater decompressor = new Inflater(true);
		byte[] inBuffer = new byte[BUFFER_LENGTH];
		byte[] outBuffer = new byte[BUFFER_LENGTH];
		length -= METADATA1_SIZE;
		if (kdfParams != null)
			length -= METADATA2_SIZE;
		for (int i = 0; i < NUM_PADDINGS; i++)
			length -= paddingLengths[i];
		long offset = 0;
		while (offset < length)
		{
			// Test whether task has been cancelled by a monitor
			for (IProgressListener listener : progressListeners)
			{
				if (listener.isTaskCancelled())
					throw new TaskCancelledException();
			}

			// Read and decrypt block of data from input stream
			int blockLength = (int)Math.min(length - offset, BUFFER_LENGTH);
			read(input, inBuffer, 0, blockLength);
			combiner.combine(inBuffer, 0, blockLength);

			// Decompress data and write it to output stream
			decompressor.setInput(inBuffer, 0, blockLength);
			try
			{
				while (true)
				{
					int outLength = decompressor.inflate(outBuffer);
					if ((outLength == 0) && decompressor.needsInput())
						break;
					hash.update(outBuffer, 0, outLength);
					write(output, outBuffer, 0, outLength);
				}
			}
			catch (DataFormatException e)
			{
				throw new InputException(ErrorId.INCORRECT_KEY);
			}

			// Increment offset
			offset += blockLength;

			// Update progress of task
			double progress = (double)offset / (double)length;
			for (IProgressListener listener : progressListeners)
				listener.setProgress(progress);
		}

		// Skip second padding
		padding = new byte[paddingLengths[paddingIndex++]];
		read(input, padding);

		// Read and decrypt hash value
		byte[] hashValueData = new byte[HASH_VALUE_FIELD_SIZE];
		read(input, hashValueData);
		combiner.combine(hashValueData);

		// Compare actual hash value with value from input stream
		if (!Arrays.equals(hashValueData, hash.getValue()))
			throw new InputException(ErrorId.INCORRECT_KEY);

		// Update instance variables
		hashValue = hashValueData;

		// Return timestamp
		return timestamp;
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

		ERROR_READING_DATA
		("An error occurred when reading the %1."),

		ERROR_WRITING_DATA
		("An error occurred when writing the %1."),

		PREMATURE_END_OF_DATA
		("The end of the %1 was reached prematurely when reading the %1."),

		UNRECOGNISED_CIPHER
		("The cipher that was used to encrypt the %1 is not recognised."),

		UNEXPECTED_DATA_FORMAT
		("The encrypted %1 does not have the expected format."),

		UNSUPPORTED_DATA_VERSION
		("The version of the %1 (%2) is not supported by this version of the program."),

		INCORRECT_KEY
		("The current key does not match the one that was used to encrypt the %1."),

		NOT_ENOUGH_MEMORY
		("There was not enough memory to generate the content-encryption key.");

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

////////////////////////////////////////////////////////////////////////
//  Member interfaces
////////////////////////////////////////////////////////////////////////


	// INTERFACE: INPUT


	/**
	 * This functional interface defines the method that must be implemented by a producer of the input to an encryption
	 * or decryption operation.
	 *
	 * @see StreamEncrypter.IOutput
	 * @see StreamEncrypter#encrypt(IInput, IOutput, long, long, byte[], byte[], IProcedure1)
	 * @see StreamEncrypter#decrypt(IInput, IOutput, long, byte[], IProcedure1)
	 */

	@FunctionalInterface
	public interface IInput
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Reads data from the input up to the specified length and stores it in a buffer.
		 *
		 * @param  buffer
		 *           the buffer in which the data will be stored.
		 * @param  offset
		 *           the offset in {@code buffer} at which the first byte of data will be stored.
		 * @param  length
		 *           the maximum number of bytes to read.
		 * @return the number of bytes that were read from the input.
		 * @throws IOException
		 *           if an error occurs when reading from the input.
		 */

		int read(
			byte[]	buffer,
			int		offset,
			int		length)
			throws IOException;

		//--------------------------------------------------------------

	}

	//==================================================================


	// INTERFACE: OUTPUT


	/**
	 * This functional interface defines the method that must be implemented by a consumer of the output from an
	 * encryption or decryption operation.
	 *
	 * @see StreamEncrypter.IInput
	 * @see StreamEncrypter#encrypt(IInput, IOutput, long, long, byte[], byte[], IProcedure1)
	 * @see StreamEncrypter#decrypt(IInput, IOutput, long, byte[], IProcedure1)
	 */

	@FunctionalInterface
	public interface IOutput
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Writes a specifed number of bytes of data to the output.
		 *
		 * @param  data
		 *           an array that contains the data to be written.
		 * @param  offset
		 *           the start offset of the data in {@code data}.
		 * @param  length
		 *           the number of bytes to write.
		 * @throws IOException
		 *           if an error occurs when writing to the output.
		 */

		void write(
			byte[]	data,
			int		offset,
			int		length)
			throws IOException;

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: STREAM HEADER


	/**
	 * This class encapsulates the optional header of a stream that is encrypted with {@link StreamEncrypter}.
	 * <p>
	 * The header consists of an identifier field, a version field and optional supplementary data.  The identifier and
	 * version number apply to the <i>payload</i> of the encrypted stream, not to the stream itself, which has a fixed
	 * format.  The sizes of the identifier field and version field are fixed, but the length of any supplementary data
	 * is variable.
	 * </p>
	 */

	public static class Header
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		/**
		 * The size (in bytes) of the identifier field of the header.
		 */
		public static final	int	ID_FIELD_SIZE		= 4;

		/**
		 * The size (in bytes) of the version field of the header.
		 */
		public static final	int	VERSION_FIELD_SIZE	= 2;

		/**
		 * The size (in bytes) of the basic header (ie, identifier and version fields, with no supplementary
		 * data).
		 */
		public static final	int	SIZE	= ID_FIELD_SIZE + VERSION_FIELD_SIZE;

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int		id;
		private	int		version;
		private	int		minSupportedVersion;
		private	int		maxSupportedVersion;
		private	byte[]	supplementaryData;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a header of an encrypted stream with the specified identifier and version number.
		 *
		 * @param id
		 *          the identifier of the payload of the encrypted stream.
		 * @param version
		 *          the version number of the payload of the encrypted stream.
		 */

		public Header(
			int	id,
			int	version)
		{
			this(id, version, version, version, null);
		}

		//--------------------------------------------------------------

		/**
		 * Creates a header of an encrypted stream with the specified identifier and version number, and with empty
		 * supplementary data of the specified length.
		 *
		 * @param id
		 *          the identifier of the payload of the encrypted stream.
		 * @param version
		 *          the version number of the payload of the encrypted stream.
		 * @param supplementaryDataLength
		 *          the length (in bytes) of the supplementary data of the header, which will be all zeros.
		 */

		public Header(
			int	id,
			int	version,
			int	supplementaryDataLength)
		{
			this(id, version, version, version, supplementaryDataLength);
		}

		//--------------------------------------------------------------

		/**
		 * Creates a header of an encrypted stream with the specified identifier, version number and supplementary data.
		 *
		 * @param id
		 *          the identifier of the payload of the encrypted stream.
		 * @param version
		 *          the version number of the payload of the encrypted stream.
		 * @param supplementaryData
		 *          the supplementary data of the header.
		 */

		public Header(
			int		id,
			int		version,
			byte[]	supplementaryData)
		{
			this(id, version, version, version, supplementaryData);
		}

		//--------------------------------------------------------------

		/**
		 * Creates a header of an encrypted stream with the specified identifier and version number, and with a minimum
		 * and maximum supported version number.
		 * <p>
		 * The version number of the payload is tested against the minimum and maximum supported version number when a
		 * stream is decrypted.
		 * </p>
		 *
		 * @param id
		 *          the identifier of the payload of the encrypted stream.
		 * @param version
		 *          the version number of the payload of the encrypted stream.
		 * @param minSupportedVersion
		 *          the minimum version number of the payload that is supported.
		 * @param maxSupportedVersion
		 *          the maximum version number of the payload that is supported.
		 */

		public Header(
			int	id,
			int	version,
			int	minSupportedVersion,
			int	maxSupportedVersion)
		{
			this(id, version, minSupportedVersion, maxSupportedVersion, null);
		}

		//--------------------------------------------------------------

		/**
		 * Creates a header of an encrypted stream with the specified identifier and version number, empty supplementary
		 * data of the specified length, and a minimum and maximum supported version number.
		 * <p>
		 * The version number of the payload is tested against the minimum and maximum supported version number when a
		 * stream is decrypted.
		 * </p>
		 *
		 * @param id
		 *          the identifier of the payload of the encrypted stream.
		 * @param version
		 *          the version number of the payload of the encrypted stream.
		 * @param minSupportedVersion
		 *          the minimum version number of the payload that is supported.
		 * @param maxSupportedVersion
		 *          the maximum version number of the payload that is supported.
		 * @param supplementaryDataLength
		 *          the length (in bytes) of the supplementary data of the header, which will be all zeros.
		 */

		public Header(
			int	id,
			int	version,
			int	minSupportedVersion,
			int	maxSupportedVersion,
			int	supplementaryDataLength)
		{
			this(id, version, minSupportedVersion, maxSupportedVersion,
				 (supplementaryDataLength == 0) ? null : new byte[supplementaryDataLength]);
		}

		//--------------------------------------------------------------

		/**
		 * Creates a header of an encrypted stream with the specified identifier, version number and supplementary data,
		 * and a minimum and maximum supported version number.
		 * <p>
		 * The version number of the payload is tested against the minimum and maximum supported version number when a
		 * stream is decrypted.
		 * </p>
		 *
		 * @param id
		 *          the identifier of the payload of the encrypted stream.
		 * @param version
		 *          the version number of the payload of the encrypted stream.
		 * @param minSupportedVersion
		 *          the minimum version number of the payload that is supported.
		 * @param maxSupportedVersion
		 *          the maximum version number of the payload that is supported.
		 * @param supplementaryData
		 *          the supplementary data of the header.
		 */

		public Header(
			int		id,
			int		version,
			int		minSupportedVersion,
			int		maxSupportedVersion,
			byte[]	supplementaryData)
		{
			this.id = id;
			this.version = version;
			this.minSupportedVersion = minSupportedVersion;
			this.maxSupportedVersion = maxSupportedVersion;
			this.supplementaryData = supplementaryData;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the identifier of this header.
		 *
		 * @return the identifier of this header.
		 */

		public int getId()
		{
			return id;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the version number of this header.
		 *
		 * @return the version number of this header.
		 */

		public int getVersion()
		{
			return version;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the minimum supported version number of this header.
		 *
		 * @return the minimum supported version number of this header.
		 */

		public int getMinSupportedVersion()
		{
			return minSupportedVersion;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the maximum supported version number of this header.
		 *
		 * @return the maximum supported version number of this header.
		 */

		public int getMaxSupportedVersion()
		{
			return maxSupportedVersion;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the supplementary data of this header.
		 *
		 * @return the supplementary data of this header as a byte array.
		 */

		public byte[] getSupplementaryData()
		{
			return supplementaryData;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the size of this header including any supplementary data.
		 *
		 * @return the size (in bytes) of this header including any supplementary data.
		 */

		public int getSize()
		{
			int size = ID_FIELD_SIZE + VERSION_FIELD_SIZE;
			if (supplementaryData != null)
				size += supplementaryData.length;
			return size;
		}

		//--------------------------------------------------------------

		/**
		 * Returns {@code true} if the specified version number is within the interval defined by the minimum and
		 * maximum supported version numbers of this header.
		 *
		 * @param  version  the version number that will be tested.
		 * @return {@code true} if {@code version} is within the interval defined by minimum and maximum supported
		 *         version numbers of this header; {@code false} otherwise.
		 */

		public boolean isSupportedVersion(
			int	version)
		{
			return (version >= minSupportedVersion) && (version <= maxSupportedVersion);
		}

		//--------------------------------------------------------------

		/**
		 * Returns this header as an array of bytes.
		 *
		 * @return this header converted to an array of bytes.
		 */

		public byte[] toByteArray()
		{
			byte[] buffer = new byte[getSize()];
			int offset = 0;

			// Encode format ID
			int length = ID_FIELD_SIZE;
			NumberCodec.uIntToBytesLE(id, buffer, offset, length);
			offset += length;

			// Encode version number
			length = VERSION_FIELD_SIZE;
			NumberCodec.uIntToBytesLE(version, buffer, offset, length);
			offset += length;

			// Encode supplementary data
			if (supplementaryData != null)
			{
				length = supplementaryData.length;
				System.arraycopy(supplementaryData, 0, buffer, offset, length);
				offset += length;
			}

			return buffer;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: KEY-DERIVATION FUNCTION PARAMETERS


	/**
	 * This class encapsulates the parameters of the {@linkplain Scrypt scrypt} password-based key-derivation function
	 * (KDF), which is used to derive a content-encryption key for the encryption and decryption operations of {@link
	 * StreamEncrypter}.  The {@link Scrypt.Params} class encapsulates the three parameters of the scrypt function:
	 * <ul>
	 *   <li>CPU/memory cost,</li>
	 *   <li>number of blocks, and</li>
	 *   <li>number of parallel superblocks ('parallelisation parameter').</li>
	 * </ul>
	 * This class extends the {@link Scrypt.Params} class by adding two parameters that are not part of the scrypt
	 * specification:
	 * <ul>
	 *   <li>
	 *     The <i>number of rounds</i> parameter allows the number of rounds of the Salsa20 core to be selected from the
	 *     set { 8, 12, 16, 20 } instead of being fixed at 8 in accordance with the scrypt specification.
	 *   </li>
	 *   <li>
	 *     The <i>maximum number of threads</i> parameter denotes the maximum number of threads that will be created to
	 *     process the parallel superblocks at the highest level of the scrypt algorithm.  If the value of the parameter
	 *     is zero, the maximum number of threads will be the number of available processors on the system.  The number
	 *     of threads allocated by the KDF will not exceed the number of parallel blocks nor the number of available
	 *     processors.  The number of threads affects the performance of the KDF but not its result.
	 *   </li>
	 * </ul>
	 * <p>
	 * For compactness, the five parameters can be encoded as bit fields in a 32-bit integer:
	 * </p>
	 * <p>
	 * bits <code>&nbsp;3:0&nbsp;</code> : function ID (scrypt = 0)<br>
	 * bits <code>&nbsp;5:4&nbsp;</code> : number of rounds of the Salsa20 core<br>
	 * bits <code>10:6&nbsp;</code> : CPU/memory cost<br>
	 * bits <code>18:11</code> : number of blocks<br>
	 * bits <code>24:19</code> : number of parallel superblocks<br>
	 * bits <code>31:25</code> : maximum number of threads<br>
	 * </p>
	 */

	public static class KdfParams
		extends Scrypt.Params
		implements Cloneable
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		/** The minimum value of the CPU/memory cost parameter, which is the binary logarithm of the <i>N</i> parameter
			(CPU/memory cost) of the scrypt algorithm. */
		public static final		int	MIN_COST	= Scrypt.MIN_COST;

		/** The maximum value of the CPU/memory cost parameter, which is the binary logarithm of the <i>N</i> parameter
			(CPU/memory cost) of the scrypt algorithm. */
		public static final		int	MAX_COST	= Scrypt.MAX_COST;

		/** The minimum value of the <i>r</i> parameter (block size) of the scrypt algorithm. */
		public static final		int	MIN_NUM_BLOCKS	= Scrypt.MIN_NUM_BLOCKS;

		/** The maximum value of the <i>r</i> parameter (block size) of the scrypt algorithm. */
		public static final		int	MAX_NUM_BLOCKS	= 256;

		/** The minimum value of the <i>p</i> parameter (parallelisation) of the scrypt algorithm. */
		public static final		int	MIN_NUM_SUPERBLOCKS	= Scrypt.MIN_NUM_SUPERBLOCKS;

		/** The maximum value of the <i>p</i> parameter (parallelisation) of the scrypt algorithm. */
		public static final		int	MAX_NUM_SUPERBLOCKS	= Scrypt.MAX_NUM_SUPERBLOCKS;

		/** The minimum value of the <i>maximum number of threads</i> parameter.  The value of zero denotes the number
			of available processors on the system. */
		public static final		int	MIN_MAX_NUM_THREADS	= 0;

		/** The maximum value of the <i>maximum number of threads</i> parameter. */
		public static final		int	MAX_MAX_NUM_THREADS	= Scrypt.MAX_NUM_THREADS;

		private static final	int	NUM_PARAMETERS	= 5;

		private static final	int	FUNCTION_ID	= 0;

		private static final	int	FUNCTION_ID_FIELD_OFFSET		= 0;
		private static final	int	FUNCTION_ID_FIELD_LENGTH		= 4;

		private static final	int	NUM_ROUNDS_FIELD_OFFSET			=
				FUNCTION_ID_FIELD_OFFSET + FUNCTION_ID_FIELD_LENGTH;
		private static final	int	NUM_ROUNDS_FIELD_LENGTH			= 2;

		private static final	int	COST_FIELD_OFFSET				= NUM_ROUNDS_FIELD_OFFSET + NUM_ROUNDS_FIELD_LENGTH;
		private static final	int	COST_FIELD_LENGTH				= 5;

		private static final	int	NUM_BLOCKS_FIELD_OFFSET			= COST_FIELD_OFFSET + COST_FIELD_LENGTH;
		private static final	int	NUM_BLOCKS_FIELD_LENGTH			= 8;

		private static final	int	NUM_SUPERBLOCKS_FIELD_OFFSET	= NUM_BLOCKS_FIELD_OFFSET + NUM_BLOCKS_FIELD_LENGTH;
		private static final	int	NUM_SUPERBLOCKS_FIELD_LENGTH	= 6;

		private static final	int	MAX_NUM_THREADS_FIELD_OFFSET	=
				NUM_SUPERBLOCKS_FIELD_OFFSET + NUM_SUPERBLOCKS_FIELD_LENGTH;
		private static final	int	MAX_NUM_THREADS_FIELD_LENGTH	= 7;

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		/** The number of rounds of the core hash function. */
		public	Scrypt.CoreHashNumRounds	numRounds;

		/** The maximum number of threads that will be allocated by the parallel phase of the scrypt KDF. */
		public	int							maxNumThreads;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a set of KDF parameters from their encoded form as bit fields in a 32-bit integer.
		 * <p>
		 * The bit fields are as follows:
		 * </p>
		 * <p>
		 * bits <code>&nbsp;3:0&nbsp;</code> : function ID (scrypt = 0)<br>
		 * bits <code>&nbsp;5:4&nbsp;</code> : number of rounds of the Salsa20 core<br>
		 * bits <code>10:6&nbsp;</code> : CPU/memory cost<br>
		 * bits <code>18:11</code> : number of blocks<br>
		 * bits <code>24:19</code> : number of parallel superblocks<br>
		 * bits <code>31:25</code> : maximum number of threads<br>
		 * </p>
		 * <p>
		 * The encoded value of the number of rounds of the Salsa20 core is the index of the value in the vector of
		 * supported values, [ 8, 12, 16, 20 ]; the encoded value of the other parameters is an offset from the
		 * parameter's minimum value.
		 * </p>
		 *
		 * @param encodedValue  the parameters encoded as bit fields in a 32-bit integer.
		 */

		public KdfParams(
			int	encodedValue)
		{
			super(getFieldValue(encodedValue, MIN_COST, COST_FIELD_OFFSET, COST_FIELD_LENGTH),
				  getFieldValue(encodedValue, MIN_NUM_BLOCKS, NUM_BLOCKS_FIELD_OFFSET, NUM_BLOCKS_FIELD_LENGTH),
				  getFieldValue(encodedValue, MIN_NUM_SUPERBLOCKS, NUM_SUPERBLOCKS_FIELD_OFFSET,
								NUM_SUPERBLOCKS_FIELD_LENGTH));
			int index = getFieldValue(encodedValue, 0, NUM_ROUNDS_FIELD_OFFSET, NUM_ROUNDS_FIELD_LENGTH);
			numRounds = Scrypt.CoreHashNumRounds.values()[index];
			maxNumThreads = getFieldValue(encodedValue, MIN_MAX_NUM_THREADS, MAX_NUM_THREADS_FIELD_OFFSET,
										  MAX_NUM_THREADS_FIELD_LENGTH);
		}

		//--------------------------------------------------------------

		/**
		 * Creates a set of KDF parameters with the specified value for each of the five parameters.
		 *
		 * @param numRounds
		 *          the number of rounds of the Salsa20 core.
		 * @param cost
		 *          the CPU/memory cost (scrypt parameter <i>N</i>).
		 * @param numBlocks
		 *          the number of blocks (scrypt parameter <i>r</i>).
		 * @param numSuperblocks
		 *          the number of parallel superblocks (scrypt parameter <i>p</i>).
		 * @param maxNumThreads
		 *          the maximum number of threads.
		 */

		public KdfParams(
			Scrypt.CoreHashNumRounds	numRounds,
			int							cost,
			int							numBlocks,
			int							numSuperblocks,
			int							maxNumThreads)
		{
			super(cost, numBlocks, numSuperblocks);
			this.numRounds = numRounds;
			this.maxNumThreads = maxNumThreads;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a {@code KdfParams} object from a string representation of the KDF parameters.
		 * <p>
		 * The string consists of a decimal-number representation of each of the five parameters.  Successive parameters
		 * are separated with a comma and optional spaces.
		 * </p>
		 *
		 * @param  str
		 *           a string representation of the KDF parameters.
		 * @return a {@code KdfParams} object.
		 * @throws IllegalArgumentException
		 *           if the string is malformed or the number of rounds of the Salsa20 core is not
		 *           supported.
		 * @throws ValueOutOfBoundsException
		 *           if the value of the cost, number of blocks, number of parallel superblocks or number of
		 *           threads is out of bounds.
		 * @see    #toString()
		 */

		public static KdfParams parseParams(
			String	str)
		{
			String[] strs = str.split(" *, *", -1);
			if (strs.length != NUM_PARAMETERS)
				throw new IllegalArgumentException();

			// Number of rounds
			int index = 0;
			Scrypt.CoreHashNumRounds numRounds = Scrypt.CoreHashNumRounds.forNumRounds(Integer.parseInt(strs[index++]));
			if (numRounds == null)
				throw new IllegalArgumentException();

			// Cost
			int cost = Integer.parseInt(strs[index++]);
			if ((cost < MIN_COST) || (cost > MAX_COST))
				throw new ValueOutOfBoundsException();

			// Number of blocks
			int numBlocks = Integer.parseInt(strs[index++]);
			if ((numBlocks < MIN_NUM_BLOCKS) || (numBlocks > MAX_NUM_BLOCKS))
				throw new ValueOutOfBoundsException();

			// Number of parallel superblocks
			int numSuperblocks = Integer.parseInt(strs[index++]);
			if ((numSuperblocks < MIN_NUM_SUPERBLOCKS) || (numSuperblocks > MAX_NUM_SUPERBLOCKS))
				throw new ValueOutOfBoundsException();

			// Maximum number of threads
			int maxNumThreads = Integer.parseInt(strs[index++]);
			if ((maxNumThreads < MIN_MAX_NUM_THREADS) || (maxNumThreads > MAX_MAX_NUM_THREADS))
				throw new ValueOutOfBoundsException();

			return new KdfParams(numRounds, cost, numBlocks, numSuperblocks, maxNumThreads);
		}

		//--------------------------------------------------------------

		private static int getFieldValue(
			int	data,
			int	minValue,
			int	offset,
			int	length)
		{
			return (minValue + BitUtils.getBitField(data, offset, length));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a copy of this object.
		 *
		 * @return a {@link KdfParams} object that is a copy of this object.
		 */

		@Override
		public KdfParams clone()
		{
			return (KdfParams)super.clone();
		}

		//--------------------------------------------------------------

		/**
		 * Returns a string representation of this object that can be parsed by {@link #parseParams(String)}.
		 * <p>
		 * The string consists of a decimal-number representation of each of the five parameters.  Successive parameters
		 * are separated with a comma and optional spaces.
		 * </p>
		 *
		 * @return a string representation of this object.
		 * @see    #parseParams(String)
		 */

		@Override
		public String toString()
		{
			StringBuilder buffer = new StringBuilder(32);
			buffer.append(numRounds.getValue());
			buffer.append(", ");
			buffer.append(getCost());
			buffer.append(", ");
			buffer.append(getNumBlocks());
			buffer.append(", ");
			buffer.append(getNumSuperblocks());
			buffer.append(", ");
			buffer.append(maxNumThreads);
			return buffer.toString();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the number of threads that should be allocated for the processing of parallel superblocks
		 * in the KDF.
		 * <p>
		 * The value returned by this method will not exceed the number of available processors on the
		 * system.  If the <i>maximum number of threads</i> parameter of this parameter set is zero, the
		 * number of threads will be the number of available processors.
		 * </p>
		 *
		 * @return the number of threads that should be allocated for the processing of parallel superblocks
		 *         in the KDF.
		 */

		public int getNumThreads()
		{
			int numProcessors = Runtime.getRuntime().availableProcessors();
			return ((maxNumThreads == 0) ? numProcessors : Math.min(maxNumThreads, numProcessors));
		}

		//--------------------------------------------------------------

		/**
		 * Returns this set of KDF parameters encoded as bit fields in a 32-bit integer.
		 * <p>
		 * The bit fields are as follows:
		 * </p>
		 * <p>
		 * bits <code>&nbsp;3:0&nbsp;</code> : function ID (scrypt = 0)<br>
		 * bits <code>&nbsp;5:4&nbsp;</code> : number of rounds of the Salsa20 core<br>
		 * bits <code>10:6&nbsp;</code> : CPU/memory cost<br>
		 * bits <code>18:11</code> : number of blocks<br>
		 * bits <code>24:19</code> : number of parallel superblocks<br>
		 * bits <code>31:25</code> : maximum number of threads<br>
		 * </p>
		 * <p>
		 * The encoded value of the number of rounds of the Salsa20 core is the index of the value in the
		 * vector of supported values, [ 8, 12, 16, 20 ]; the encoded value of the other parameters is an
		 * offset from the parameter's minimum value.
		 * </p>
		 *
		 * @return all the parameters of this object encoded as bit fields in a 32-bit integer.
		 * @see    #getEncodedValue(boolean)
		 */

		public int getEncodedValue()
		{
			return getEncodedValue(true);
		}

		//--------------------------------------------------------------

		/**
		 * Returns this set of KDF parameters encoded as bit fields in a 32-bit integer.  The <i>maximum
		 * number of threads</i> parameter may be optionally included in the encoded value.
		 * <p style="margin-bottom: 0.25em;">
		 * The bit fields are:
		 * </p>
		 * <table style="margin-top: 0.25em; padding-left: 1em;">
		 *   <tbody>
		 *     <tr>
		 *       <td>bits 3:0</td>
		 *       <td style="padding-left: 0.7em;">function ID (scrypt = 0)</td>
		 *     </tr>
		 *     <tr>
		 *       <td>bits 5:4</td>
		 *       <td style="padding-left: 0.7em;">number of rounds of the Salsa20 core</td>
		 *     </tr>
		 *     <tr>
		 *       <td>bits 10:6</td>
		 *       <td style="padding-left: 0.7em;">CPU/memory cost</td>
		 *     </tr>
		 *     <tr>
		 *       <td>bits 18:11</td>
		 *       <td style="padding-left: 0.7em;">number of blocks</td>
		 *     </tr>
		 *     <tr>
		 *       <td>bits 24:19</td>
		 *       <td style="padding-left: 0.7em;">number of parallel superblocks</td>
		 *     </tr>
		 *     <tr>
		 *       <td colspan="2" style="padding-top: 0.25em;">and, optionally,</td>
		 *     </tr>
		 *     <tr>
		 *       <td>bits 31:25</td>
		 *       <td style="padding-left: 0.7em;">maximum number of threads</td>
		 *     </tr>
		 * </table>
		 * <p>
		 * The encoded value of the number of rounds of the Salsa20 core is the index of the value in the
		 * vector of supported values, [ 8, 12, 16, 20 ]; the encoded value of the other parameters is an
		 * offset from the parameter's minimum value.
		 * </p>
		 *
		 * @param  includeNumThreads
		 *           if {@code true}, include the maximum number of threads in the encoded value; if {@code false}, omit
		 *           the maximum number of threads.
		 * @return the parameters of this object encoded as bit fields in a 32-bit integer, with the maximum
		 *         number of threads included or omitted according to {@code includeNumThreads}.
		 * @see    #getEncodedValue()
		 */

		public int getEncodedValue(
			boolean	includeNumThreads)
		{
			int data = 0;
			data = BitUtils.setBitField(data, FUNCTION_ID, FUNCTION_ID_FIELD_OFFSET, FUNCTION_ID_FIELD_LENGTH);
			data = BitUtils.setBitField(data, numRounds.ordinal(), NUM_ROUNDS_FIELD_OFFSET, NUM_ROUNDS_FIELD_LENGTH);
			data = BitUtils.setBitField(data, getCost() - MIN_COST, COST_FIELD_OFFSET, COST_FIELD_LENGTH);
			data = BitUtils.setBitField(data, getNumBlocks() - MIN_NUM_BLOCKS, NUM_BLOCKS_FIELD_OFFSET,
										NUM_BLOCKS_FIELD_LENGTH);
			data = BitUtils.setBitField(data, getNumSuperblocks() - MIN_NUM_SUPERBLOCKS, NUM_SUPERBLOCKS_FIELD_OFFSET,
										NUM_SUPERBLOCKS_FIELD_LENGTH);
			if (includeNumThreads)
				data = BitUtils.setBitField(data, maxNumThreads - MIN_MAX_NUM_THREADS, MAX_NUM_THREADS_FIELD_OFFSET,
											MAX_NUM_THREADS_FIELD_LENGTH);
			return data;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: INPUT EXCEPTION


	/**
	 * This class encapsulates an exception that occurs when reading from an input stream.
	 *
	 * @see OutputException
	 */

	public static class InputException
		extends EncrypterException
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private InputException(
			ErrorId	id)
		{
			super(id);
		}

		//--------------------------------------------------------------

		private InputException(
			ErrorId		id,
			Throwable	cause)
		{
			super(id, cause);
		}

		//--------------------------------------------------------------

		private InputException(
			ErrorId	id,
			String	str)
		{
			super(id, str);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: OUTPUT EXCEPTION


	/**
	 * This class encapsulates an exception that occurs when writing to an output stream.
	 *
	 * @see InputException
	 */

	public static class OutputException
		extends EncrypterException
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private OutputException(
			ErrorId	id)
		{
			super(id);
		}

		//--------------------------------------------------------------

		private OutputException(
			ErrorId		id,
			Throwable	cause)
		{
			super(id, cause);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: ENCRYPTER EXCEPTION


	/**
	 * This is the base class of the {@link InputException} and {@link OutputException} classes.
	 *
	 * @see InputException
	 * @see OutputException
	 */

	private static abstract class EncrypterException
		extends AppException
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private EncrypterException(
			ErrorId	id)
		{
			super(id, DATA_STR);
		}

		//--------------------------------------------------------------

		private EncrypterException(
			ErrorId		id,
			Throwable	cause)
		{
			super(id, cause, DATA_STR);
		}

		//--------------------------------------------------------------

		private EncrypterException(
			ErrorId	id,
			String	str)
		{
			super(id, DATA_STR, str);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Sets the description of data in the message of an exception.
		 * <p>
		 * By default, the data that is processed by an encryption or decryption operation is described as
		 * "data" in the message of an exception.  This method allows the description to be replaced by
		 * something more specific to the kind of data stream; eg, "file".
		 * </p>
		 *
		 * @param description  the description that will be applied to data in the message of an exception.
		 */

		public void setDataDescription(
			String	description)
		{
			setReplacement(0, description);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: INPUT STREAM ADAPTER


	/**
	 * This class translates the interface of the {@link InputStream} class into the {@link IInput} interface so that
	 * {@code InputStream} can be used as a parameter type for an encryption or decryption operation.
	 *
	 * @see StreamEncrypter#encrypt(InputStream, OutputStream, long, long, byte[], byte[], IProcedure1)
	 * @see StreamEncrypter#decrypt(InputStream, OutputStream, long, byte[], IProcedure1)
	 */

	private static class InputStreamAdapter
		implements IInput
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	InputStream	inputStream;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates an adapter to wrap the specified input stream.
		 *
		 * @param inStream
		 *          the input stream that will be adapted by this implementation of {@link IInput}.
		 */

		private InputStreamAdapter(
			InputStream	inStream)
		{
			inputStream = inStream;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IInput interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Reads data from the input stream up to the specified length, and stores it in a buffer.
		 *
		 * @param  buffer
		 *           the buffer in which the data will be stored.
		 * @param  offset
		 *           the offset in {@code buffer} at which the first byte of data will be stored.
		 * @param  length
		 *           the maximum number of bytes to read.
		 * @return the number of bytes that were read from the input stream.
		 * @throws IOException
		 *           if an error occurs when reading from the input stream.
		 */

		@Override
		public int read(
			byte[]	buffer,
			int		offset,
			int		length)
			throws IOException
		{
			return inputStream.read(buffer, offset, length);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: OUTPUT STREAM ADAPTER


	/**
	 * This class translates the interface of the {@link OutputStream} class into the {@link IOutput} interface so that
	 * {@code OutputStream} can be used as a parameter type for an encryption or decryption operation.
	 *
	 * @see StreamEncrypter#encrypt(InputStream, OutputStream, long, long, byte[], byte[], IProcedure1)
	 * @see StreamEncrypter#decrypt(InputStream, OutputStream, long, byte[], IProcedure1)
	 */

	private static class OutputStreamAdapter
		implements IOutput
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	OutputStream	outputStream;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates an adapter to wrap the specified output stream.
		 *
		 * @param outStream
		 *          the output stream that will be adapted by this implementation of {@link IOutput}.
		 */

		private OutputStreamAdapter(
			OutputStream	outStream)
		{
			outputStream = outStream;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IOutput interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Writes data from an array of bytes to the output stream.
		 *
		 * @param  data
		 *           an array that contains the data to be written.
		 * @param  offset
		 *           the start offset of the data in {@code data}.
		 * @param  length
		 *           the number of bytes to write.
		 * @throws IOException
		 *           if an error occurs when writing to the output stream.
		 */

		@Override
		public void write(
			byte[]	data,
			int		offset,
			int		length)
			throws IOException
		{
			outputStream.write(data, offset, length);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
