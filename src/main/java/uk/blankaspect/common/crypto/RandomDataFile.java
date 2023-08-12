/*====================================================================*\

RandomDataFile.java

Random data file class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Arrays;

import uk.blankaspect.common.bytedata.ByteDataList;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.UnexpectedRuntimeException;

import uk.blankaspect.common.misc.BinaryFile;
import uk.blankaspect.common.misc.FileWritingMode;
import uk.blankaspect.common.misc.IProgressListener;
import uk.blankaspect.common.misc.Task;

import uk.blankaspect.common.number.NumberCodec;

import uk.blankaspect.common.ui.progress.IProgressView;

//----------------------------------------------------------------------


// RANDOM DATA FILE CLASS


/**
 * This class represents a file of random data.
 * <p>
 * The file consists of a header and a payload.  The header consists of a identifier, version number and
 * SHA-256 hash value of the payload.  The payload is nominally random data but could be anything.
 * </p>
 */

public class RandomDataFile
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		FILE_ID	= 0x526DD384;

	private static final	int		VERSION					= 0;
	private static final	int		MIN_SUPPORTED_VERSION	= 0;
	private static final	int		MAX_SUPPORTED_VERSION	= 0;

	private static final	int		ID_FIELD_SIZE			= 4;
	private static final	int		VERSION_FIELD_SIZE		= 2;
	private static final	int		HASH_VALUE_FIELD_SIZE	= 32;

	private static final	int	HEADER_SIZE		= ID_FIELD_SIZE + VERSION_FIELD_SIZE +
																					HASH_VALUE_FIELD_SIZE;

	private static final	String	HASH_NAME	= "SHA-256";

	private static final	String	READING_STR	= "Reading";
	private static final	String	WRITING_STR	= "Writing";

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FILE_DOES_NOT_EXIST
		("The random data file does not exist."),

		NOT_A_RANDOM_DATA_FILE
		("The file is not a random data file."),

		UNSUPPORTED_FILE_VERSION
		("The version of the random data file (%1) is not supported by this version of the program."),

		INCORRECT_STORED_HASH_VALUE
		("The stored hash value of the random data is incorrect.");

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ErrorId(String message)
		{
			this.message = message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : AppException.IId interface
	////////////////////////////////////////////////////////////////////

		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an instance of {@link RandomDataFile}.
	 *
	 * @throws UnexpectedRuntimeException
	 *           if the {@link java.security.MessageDigest} class does not support the SHA-256 algorithm.
	 *           (Every implementation of the Java platform is required to support the SHA-256 algorithm.)
	 */

	public RandomDataFile()
	{
		try
		{
			hash = MessageDigest.getInstance(HASH_NAME);
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new UnexpectedRuntimeException();
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the random data of this object.
	 *
	 * @return the random data of this object.
	 */

	public byte[] getRandomData()
	{
		return randomData;
	}

	//------------------------------------------------------------------

	/**
	 * Sets the random data of this object to some specified data.
	 *
	 * @param data  the data to which the random data will be set.
	 */

	public void setRandomData(byte[] data)
	{
		randomData = data.clone();
	}

	//------------------------------------------------------------------

	/**
	 * Reads a specified file and sets the random data of this object to the file's payload if the file is a
	 * valid random data file.
	 *
	 * @param  file  the file that will be read.
	 * @throws AppException
	 *           if
	 *           <ul>
	 *             <li>{@code file} does not exist or is not a normal file, or</li>
	 *             <li>an error occurred when reading the file, or</li>
	 *             <li>the file is not a valid random data file.</li>
	 *           </ul>
	 */

	public void read(File file)
		throws AppException
	{
		// Test for file
		if (!file.isFile())
			throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, file);

		// Test file length
		if (file.length() < HEADER_SIZE)
			throw new FileException(ErrorId.NOT_A_RANDOM_DATA_FILE, file);

		// Initialise progress view
		IProgressView progressView = Task.getProgressView();
		if (progressView != null)
		{
			progressView.setInfo(READING_STR, file);
			progressView.setProgress(0, (progressView instanceof IProgressListener) ? 0.0 : -1.0);
		}

		// Read and parse file
		try
		{
			BinaryFile binaryFile = new BinaryFile(file);
			if (progressView instanceof IProgressListener)
				binaryFile.addProgressListener((IProgressListener)progressView);
			parse(binaryFile.read());
		}
		catch (AppException e)
		{
			throw new FileException(e, file);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Writes a specified random data file whose payload is the random data of this object.
	 *
	 * @param  file  the file that will be written.
	 * @throws AppException
	 *           if an error occurred when writing the file.
	 */

	public void write(File file)
		throws AppException
	{
		// Initialise list of data blocks
		ByteDataList dataBlocks = new ByteDataList();

		// Create file header
		byte[] header = createHeader();
		dataBlocks.add(header);

		// Add random data
		dataBlocks.add(randomData);

		// Initialise progress view
		IProgressView progressView = Task.getProgressView();
		if (progressView != null)
		{
			progressView.setInfo(WRITING_STR, file);
			progressView.setProgress(0, (progressView instanceof IProgressListener) ? 0.0 : -1.0);
		}

		// Write file
		BinaryFile binaryFile = new BinaryFile(file, dataBlocks);
		if (progressView instanceof IProgressListener)
			binaryFile.addProgressListener((IProgressListener)progressView);
		binaryFile.write(FileWritingMode.USE_TEMP_FILE);
	}

	//------------------------------------------------------------------

	private void parse(byte[] data)
		throws AppException
	{
		int offset = 0;

		// Parse field: file identifier
		int length = ID_FIELD_SIZE;
		if (NumberCodec.bytesToUIntLE(data, offset, length) != FILE_ID)
			throw new AppException(ErrorId.NOT_A_RANDOM_DATA_FILE);
		offset += length;

		// Parse field: version number
		length = VERSION_FIELD_SIZE;
		int version = NumberCodec.bytesToUIntLE(data, offset, length);
		offset += length;
		if ((version < MIN_SUPPORTED_VERSION) || (version > MAX_SUPPORTED_VERSION))
			throw new AppException(ErrorId.UNSUPPORTED_FILE_VERSION, Integer.toString(version));

		// Parse field: hash value
		length = HASH_VALUE_FIELD_SIZE;
		byte[] hashValue = Arrays.copyOfRange(data, offset, offset + length);
		offset += length;

		// Get random data
		randomData = Arrays.copyOfRange(data, offset, data.length);

		// Test hash value
		hash.reset();
		if (!Arrays.equals(hashValue, hash.digest(randomData)))
			throw new AppException(ErrorId.INCORRECT_STORED_HASH_VALUE);
	}

	//------------------------------------------------------------------

	private byte[] createHeader()
	{
		byte[] buffer = new byte[HEADER_SIZE];
		int offset = 0;

		// Set field: file identifier
		int length = ID_FIELD_SIZE;
		NumberCodec.uIntToBytesLE(FILE_ID, buffer, offset, length);
		offset += length;

		// Set field: version number
		length = VERSION_FIELD_SIZE;
		NumberCodec.uIntToBytesLE(VERSION, buffer, offset, length);
		offset += length;

		// Set field: hash value
		length = HASH_VALUE_FIELD_SIZE;
		hash.reset();
		byte[] hashValue = hash.digest(randomData);
		System.arraycopy(hashValue, 0, buffer, offset, length);
		offset += length;

		return buffer;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	byte[]			randomData;
	private	MessageDigest	hash;

}

//----------------------------------------------------------------------
