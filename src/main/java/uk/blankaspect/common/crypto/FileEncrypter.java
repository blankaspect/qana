/*====================================================================*\

FileEncrypter.java

File encrypter class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.channels.OverlappingFileLockException;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.TaskCancelledException;
import uk.blankaspect.common.exception.TempFileException;

import uk.blankaspect.common.filesystem.FilenameUtils;

import uk.blankaspect.common.function.IProcedure1;

import uk.blankaspect.common.misc.IProgressListener;
import uk.blankaspect.common.misc.NullOutputStream;
import uk.blankaspect.common.misc.Task;

import uk.blankaspect.common.ui.progress.IProgressView;

//----------------------------------------------------------------------


// FILE ENCRYPTER CLASS


/**
 * This class adapts {@link StreamEncrypter} for use with input and output streams that are opened on files.
 * <p>
 * Instances of this class have methods for encrypting, decrypting and validating files.
 * </p>
 */

public class FileEncrypter
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	FILE_STR		= "file";
	private static final	String	ENCRYPTING_STR	= "Encrypting";
	private static final	String	DECRYPTING_STR	= "Decrypting";
	private static final	String	VALIDATING_STR	= "Validating";

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

		FAILED_TO_OPEN_FILE
		("Failed to open the file."),

		FAILED_TO_CLOSE_FILE
		("Failed to close the file."),

		FAILED_TO_LOCK_FILE
		("Failed to lock the file."),

		FILE_ACCESS_NOT_PERMITTED
		("Access to the file was not permitted."),

		FAILED_TO_CREATE_DIRECTORY
		("Failed to create the directory."),

		FAILED_TO_CREATE_TEMPORARY_FILE
		("Failed to create a temporary file."),

		FAILED_TO_DELETE_FILE
		("Failed to delete the existing file."),

		FAILED_TO_RENAME_FILE
		("Failed to rename the temporary file to the specified filename."),

		FAILED_TO_GET_FILE_TIMESTAMP
		("Failed to get the timestamp of the file."),

		FAILED_TO_SET_FILE_TIMESTAMP
		("Failed to set the timestamp of the file."),

		FILE_IS_TOO_SHORT
		("The file is too short to have been created by this program.");

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
	 * Creates an instance of {@link FileEncrypter} with the specified kind of cipher but no KDF parameters
	 * or header.
	 * <p>
	 * The absence of KDF parameters means that the {@link #encrypt(File, File, byte[], byte[])}, {@link
	 * #decrypt(File, File, byte[])} and {@link #validate(File, byte[])} methods will not derive a
	 * content-encryption key (CEK) from their {@code key} argument but will use the {@code key} argument
	 * directly as the CEK.
	 * </p>
	 *
	 * @param cipher  the kind of cipher that will be used by the pseudo-random number generator to generate
	 *                a stream cipher for encryption.  {@code cipher} may be {@code null} if the encrypter
	 *                will not be used for encryption.
	 */

	public FileEncrypter(FortunaCipher cipher)
	{
		this(cipher, null, null);
	}

	//------------------------------------------------------------------

	/**
	 * Creates an instance of {@link FileEncrypter} with the specified kind of cipher and KDF parameters but
	 * no header.
	 *
	 * @param cipher     the kind of cipher that will be used by the pseudo-random number generator to
	 *                   generate a stream cipher for encryption.  {@code cipher} may be {@code null} if the
	 *                   encrypter will not be used for encryption.
	 * @param kdfParams  the parameters that will be used by the key derivation function to derive the
	 *                   content-encryption key when encrypting and decrypting a file.  If {@code kdfParams}
	 *                   is {@code null}, the {@link #encrypt(File, File, byte[], byte[])}, {@link
	 *                   #decrypt(File, File, byte[])} and {@link #validate(File, byte[])} methods will not
	 *                   derive a content-encryption key (CEK) from their {@code key} argument but will use
	 *                   the {@code key} argument directly as the CEK.
	 */

	public FileEncrypter(FortunaCipher             cipher,
						 StreamEncrypter.KdfParams kdfParams)
	{
		this(cipher, kdfParams, null);
	}

	//------------------------------------------------------------------

	/**
	 * Creates an instance of {@link FileEncrypter} with the specified kind of cipher and header but no KDF
	 * parameters.
	 * <p>
	 * The absence of KDF parameters means that the {@link #encrypt(File, File, byte[], byte[])}, {@link
	 * #decrypt(File, File, byte[])} and {@link #validate(File, byte[])} methods will not derive a
	 * content-encryption key (CEK) from their {@code key} argument but will use the {@code key} argument
	 * directly as the CEK.
	 * </p>
	 *
	 * @param cipher  the kind of cipher that will be used by the pseudo-random number generator to generate
	 *                a stream cipher for encryption.  {@code cipher} may be {@code null} if the encrypter
	 *                will not be used for encryption.
	 * @param header  the header that will be included in the output file, in the case of encryption, or
	 *                that will be used to check the identifier and version number against, in the case of
	 *                decryption.
	 */

	public FileEncrypter(FortunaCipher          cipher,
						 StreamEncrypter.Header header)
	{
		this(cipher, null, header);
	}

	//------------------------------------------------------------------

	/**
	 * Creates an instance of {@link FileEncrypter} with the specified kind of cipher, KDF parameters and
	 * header.
	 *
	 * @param cipher     the kind of cipher that will be used by the pseudo-random number generator to
	 *                   generate a stream cipher for encryption.  {@code cipher} may be {@code null} if the
	 *                   encrypter will not be used for encryption.
	 * @param kdfParams  the parameters that will be used by the key derivation function to derive the
	 *                   content-encryption key when encrypting and decrypting a file.  If {@code kdfParams}
	 *                   is {@code null}, the {@link #encrypt(File, File, byte[], byte[])}, {@link
	 *                   #decrypt(File, File, byte[])} and {@link #validate(File, byte[])} methods will not
	 *                   derive a content-encryption key (CEK) from their {@code key} argument but will use
	 *                   the {@code key} argument directly as the CEK.
	 * @param header     the header that will be included in the output file, in the case of encryption, or
	 *                   that will be used to check the identifier and version number against, in the case
	 *                   of decryption.
	 */

	public FileEncrypter(FortunaCipher             cipher,
						 StreamEncrypter.KdfParams kdfParams,
						 StreamEncrypter.Header    header)
	{
		this.cipher = cipher;
		if (kdfParams != null)
			this.kdfParams = kdfParams.clone();
		this.header = header;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Encrypts data from a specified input file and writes the resulting ciphertext to the specified output file.
	 *
	 * @param  inFile
	 *           the file from which the data to be encrypted will be read.
	 * @param  outFile
	 *           the file to which the ciphertext will be written.
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
	 *           if an error occurs during the encryption operation.
	 * @throws TaskCancelledException
	 *           if the encryption operation was cancelled by the user.
	 * @see    #decrypt(File, File, byte[])
	 * @see    #validate(File, byte[])
	 */

	public void encrypt(File                  inFile,
						File                  outFile,
						byte[]                key,
						byte[]                randomKey,
						IProcedure1<Runnable> kdfExecutor)
		throws AppException, TaskCancelledException
	{
		// Reset progress in progress view
		IProgressView progressView = Task.getProgressView();
		if (progressView != null)
		{
			progressView.setInfo(ENCRYPTING_STR, inFile);
			progressView.setProgress(0, 0.0);
			progressView.waitForIdle();
		}

		// Encrypt file
		File tempFile = null;
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
		boolean oldFileDeleted = false;
		try
		{
			// Create parent directory of output file
			File directory = outFile.getAbsoluteFile().getParentFile();
			if ((directory != null) && !directory.exists())
			{
				try
				{
					if (!directory.mkdirs())
						throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory);
				}
				catch (SecurityException e)
				{
					throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory, e);
				}
			}

			// Create temporary file
			try
			{
				tempFile = FilenameUtils.tempLocation(outFile);
				tempFile.createNewFile();
			}
			catch (Exception e)
			{
				throw new AppException(ErrorId.FAILED_TO_CREATE_TEMPORARY_FILE, e);
			}

			// Open output stream on temporary file
			try
			{
				outStream = new FileOutputStream(tempFile);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, tempFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, tempFile, e);
			}

			// Lock output file
			try
			{
				if (outStream.getChannel().tryLock() == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile, e);
			}

			// Open input stream on input file
			try
			{
				inStream = new FileInputStream(inFile);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, inFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, inFile, e);
			}

			// Lock input file
			try
			{
				if (inStream.getChannel().tryLock(0, Long.MAX_VALUE, true) == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, inFile);
			}
			catch (OverlappingFileLockException e)
			{
				// ignore
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, inFile, e);
			}

			// Get length of input file
			long fileLength = inFile.length();

			// Get timestamp
			long timestamp = inFile.lastModified();
			if (timestamp == 0)
				throw new FileException(ErrorId.FAILED_TO_GET_FILE_TIMESTAMP, inFile);

			// Encrypt file
			try
			{
				StreamEncrypter encrypter = new StreamEncrypter(cipher, kdfParams, header);
				if (progressView instanceof IProgressListener progressListener)
					encrypter.addProgressListener(progressListener);
				encrypter.encrypt(inStream, outStream, fileLength, timestamp, key, randomKey, kdfExecutor);
			}
			catch (StreamEncrypter.InputException e)
			{
				e.setDataDescription(FILE_STR);
				throw new FileException(e, inFile);
			}
			catch (StreamEncrypter.OutputException e)
			{
				e.setDataDescription(FILE_STR);
				throw new FileException(e, tempFile);
			}

			// Close input file
			try
			{
				inStream.close();
				inStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, inFile, e);
			}

			// Close output file
			try
			{
				outStream.close();
				outStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, tempFile, e);
			}

			// Delete any existing file
			try
			{
				if (outFile.exists() && !outFile.delete())
					throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, outFile);
				oldFileDeleted = true;
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, outFile, e);
			}

			// Rename temporary file
			try
			{
				if (!tempFile.renameTo(outFile))
					throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, outFile, tempFile);
			}
			catch (SecurityException e)
			{
				throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, outFile, e, tempFile);
			}
		}
		catch (AppException e)
		{
			// Close input file
			try
			{
				if (inStream != null)
					inStream.close();
			}
			catch (IOException e1)
			{
				// ignore
			}

			// Close output file
			try
			{
				if (outStream != null)
					outStream.close();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Delete temporary file
			try
			{
				if (!oldFileDeleted && (tempFile != null) && tempFile.exists())
					tempFile.delete();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Decrypts data from a specified input file and writes the resulting plaintext to a specified output
	 * file.
	 *
	 * @param  inFile
	 *           the file from which the data to be decrypted will be read.
	 * @param  outFile
	 *           the file to which the plaintext will be written.
	 * @param  key
	 *           if parameters of the key-derivation function were specified when this encrypter was created, the key
	 *           from which the content-encryption key will be derived; otherwise, the key that will be used as the
	 *           content-encryption key.
	 * @param  kdfExecutor
	 *           the executor of the key-derivation function, which is ignored if it is {@code null}.
	 * @throws AppException
	 *           if an error occurs during the decryption operation.
	 * @throws TaskCancelledException
	 *           if the decryption operation was cancelled by the user.
	 * @see    #encrypt(File, File, byte[], byte[])
	 * @see    #validate(File, byte[])
	 */

	public void decrypt(File                  inFile,
						File                  outFile,
						byte[]                key,
						IProcedure1<Runnable> kdfExecutor)
		throws AppException, TaskCancelledException
	{
		// Reset progress in progress view
		IProgressView progressView = Task.getProgressView();
		if (progressView != null)
		{
			progressView.setInfo(DECRYPTING_STR, inFile);
			progressView.setProgress(0, 0.0);
			progressView.waitForIdle();
		}

		// Decrypt file
		File tempFile = null;
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
		boolean oldFileDeleted = false;
		try
		{
			// Create parent directory of output file
			File directory = outFile.getAbsoluteFile().getParentFile();
			if ((directory != null) && !directory.exists())
			{
				try
				{
					if (!directory.mkdirs())
						throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory);
				}
				catch (SecurityException e)
				{
					throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory, e);
				}
			}

			// Create temporary file
			try
			{
				tempFile = FilenameUtils.tempLocation(outFile);
				tempFile.createNewFile();
			}
			catch (Exception e)
			{
				throw new AppException(ErrorId.FAILED_TO_CREATE_TEMPORARY_FILE, e);
			}

			// Open output stream on temporary file
			try
			{
				outStream = new FileOutputStream(tempFile);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, tempFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, tempFile, e);
			}

			// Lock output file
			try
			{
				if (outStream.getChannel().tryLock() == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile, e);
			}

			// Open input stream on input file
			try
			{
				inStream = new FileInputStream(inFile);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, inFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, inFile, e);
			}

			// Lock input file
			try
			{
				if (inStream.getChannel().tryLock(0, Long.MAX_VALUE, true) == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, inFile);
			}
			catch (OverlappingFileLockException e)
			{
				// ignore
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, inFile, e);
			}

			// Create decrypter
			StreamEncrypter decrypter = new StreamEncrypter(cipher, kdfParams, header);

			// Test length of input file
			long fileLength = inFile.length();
			if (fileLength < decrypter.getMinOverheadSize())
				throw new FileException(ErrorId.FILE_IS_TOO_SHORT, inFile);

			// Decrypt file
			long timestamp = 0;
			try
			{
				if (progressView instanceof IProgressListener progressListener)
					decrypter.addProgressListener(progressListener);
				timestamp = decrypter.decrypt(inStream, outStream, fileLength, key, kdfExecutor);
			}
			catch (StreamEncrypter.InputException e)
			{
				e.setDataDescription(FILE_STR);
				throw new FileException(e, inFile);
			}
			catch (StreamEncrypter.OutputException e)
			{
				e.setDataDescription(FILE_STR);
				throw new FileException(e, tempFile);
			}

			// Close input file
			try
			{
				inStream.close();
				inStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, inFile, e);
			}

			// Close output file
			try
			{
				outStream.close();
				outStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, tempFile, e);
			}

			// Delete any existing file
			try
			{
				if (outFile.exists() && !outFile.delete())
					throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, outFile);
				oldFileDeleted = true;
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, outFile, e);
			}

			// Rename temporary file
			try
			{
				if (!tempFile.renameTo(outFile))
					throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, outFile, tempFile);
			}
			catch (SecurityException e)
			{
				throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, outFile, e, tempFile);
			}

			// Set timestamp of file
			if (!outFile.setLastModified(timestamp))
				throw new FileException(ErrorId.FAILED_TO_SET_FILE_TIMESTAMP, outFile);
		}
		catch (AppException e)
		{
			// Close input file
			try
			{
				if (inStream != null)
					inStream.close();
			}
			catch (IOException e1)
			{
				// ignore
			}

			// Close output file
			try
			{
				if (outStream != null)
					outStream.close();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Delete temporary file
			try
			{
				if (!oldFileDeleted && (tempFile != null) && tempFile.exists())
					tempFile.delete();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Validates data from a specified input file by decrypting it and discarding the resulting plaintext.
	 *
	 * @param  file
	 *           the file from which the data to be validated will be read.
	 * @param  key
	 *           if parameters of the key-derivation function were specified when this encrypter was created, the key
	 *           from which the content-encryption key will be derived; otherwise, the key that will be used as the
	 *           content-encryption key.
	 * @param  kdfExecutor
	 *           the executor of the key-derivation function, which is ignored if it is {@code null}.
	 * @throws AppException
	 *           if an error occurs during the decryption operation.
	 * @throws TaskCancelledException
	 *           if the decryption operation was cancelled by the user.
	 * @see    #encrypt(File, File, byte[], byte[])
	 * @see    #decrypt(File, File, byte[])
	 */

	public void validate(File                  file,
						 byte[]                key,
						 IProcedure1<Runnable> kdfExecutor)
		throws AppException, TaskCancelledException
	{
		// Reset progress in progress view
		IProgressView progressView = Task.getProgressView();
		if (progressView != null)
		{
			progressView.setInfo(VALIDATING_STR, file);
			progressView.setProgress(0, 0.0);
			progressView.waitForIdle();
		}

		// Decrypt file
		FileInputStream inStream = null;
		try
		{
			// Open input stream on input file
			try
			{
				inStream = new FileInputStream(file);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, file, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, file, e);
			}

			// Lock input file
			try
			{
				if (inStream.getChannel().tryLock(0, Long.MAX_VALUE, true) == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, file);
			}
			catch (OverlappingFileLockException e)
			{
				// ignore
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, file, e);
			}

			// Create decrypter
			StreamEncrypter decrypter = new StreamEncrypter(cipher, kdfParams, header);

			// Test length of input file
			long fileLength = file.length();
			if (fileLength < decrypter.getMinOverheadSize())
				throw new FileException(ErrorId.FILE_IS_TOO_SHORT, file);

			// Decrypt file
			try
			{
				if (progressView instanceof IProgressListener progressListener)
					decrypter.addProgressListener(progressListener);
				decrypter.decrypt(inStream, new NullOutputStream(), fileLength, key, kdfExecutor);
			}
			catch (StreamEncrypter.InputException e)
			{
				e.setDataDescription(FILE_STR);
				throw new FileException(e, file);
			}

			// Close input file
			try
			{
				inStream.close();
				inStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, file, e);
			}
		}
		catch (AppException e)
		{
			// Close input file
			try
			{
				if (inStream != null)
					inStream.close();
			}
			catch (IOException e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	FortunaCipher				cipher;
	private	StreamEncrypter.KdfParams	kdfParams;
	private	StreamEncrypter.Header		header;

}

//----------------------------------------------------------------------
