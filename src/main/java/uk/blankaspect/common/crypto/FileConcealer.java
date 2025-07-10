/*====================================================================*\

FileConcealer.java

Class: file concealer.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.channels.OverlappingFileLockException;

import javax.imageio.ImageIO;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.TaskCancelledException;
import uk.blankaspect.common.exception.TempFileException;

import uk.blankaspect.common.filesystem.FilenameUtils;

import uk.blankaspect.common.misc.IProgressListener;
import uk.blankaspect.common.misc.Task;

import uk.blankaspect.common.ui.progress.IProgressView;

//----------------------------------------------------------------------


// CLASS: FILE CONCEALER


/**
 * This class adapts {@link StreamConcealer} for use with input and output streams that are opened on files.
 * <p>
 * The concealment operation of this class writes its output to PNG-format image files.
 * </p>
 */

public class FileConcealer
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	PNG_FORMAT_NAME	= "png";

	private static final	String	FILE_STR		= "file";
	private static final	String	READING_STR		= "Reading";
	private static final	String	WRITING_STR		= "Writing";
	private static final	String	CONCEALING_STR	= "Concealing data " + Constants.ELLIPSIS_STR;
	private static final	String	RECOVERING_STR	= "Recovering data " + Constants.ELLIPSIS_STR;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an instance of {@link FileConcealer}.
	 */

	public FileConcealer()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Conceals the contents of a specified file (the <i>payload</i>) in an image (the <i>carrier</i>), and
	 * writes the resulting image in PNG format to a specified file.
	 * <p>
	 * The maximum length of the payload is 2<sup>24</sup> - 1 (16777215).
	 * </p>
	 *
	 * @param  inFile               the input file from which the payload will be read.
	 * @param  carrierImageSource   the source of the image in which the payload will be concealed.  If
	 *                              {@code carrierImageSource} is {@code null}, the carrier image wil be
	 *                              read from the file specified by {@code carrierFile}.
	 * @param  carrierFile          the file containing the image in which the payload will be concealed.
	 *                              {@code carrierFile} is used as the source of the carrier image only if
	 *                              {@code carrierImageSource} is {@code null}.
	 * @param  outFile              the file to which the image containing the concealed payload will be
	 *                              written in PNG format.
	 * @param  lengthEncoder        the object that will encode the length of the payload as an array of
	 *                              bytes.
	 * @param  maxReplacementDepth  the maximum number of bits per RGB colour component of the carrier that
	 *                              will be replaced by the payload.
	 * @param  randomSource         a source of random data for replacing bits of the RGB components of the
	 *                              carrier that are not replaced by the payload, up to {@code
	 *                              maxReplacementDepth}.  If {@code randomSource} is {@code null}, no
	 *                              carrier bits will be replaced by random data.
	 * @throws IllegalArgumentException
	 *           if {@code maxReplacementDepth} is less than 1 or greater than 6.
	 * @throws AppException
	 *           if an error occurred during the concealment operation.
	 * @throws TaskCancelledException
	 *           if the concealment operation was cancelled by the user.
	 * @see    #conceal(InputStream, IImageSource, File, File, int, StreamConcealer.ILengthEncoder, int,
	 *                  StreamConcealer.IRandomSource)
	 * @see    #recover(File, File, StreamConcealer.ILengthDecoder)
	 * @see    #recover(File, OutputStream, StreamConcealer.ILengthDecoder)
	 */

	public void conceal(
		File							inFile,
		IImageSource					carrierImageSource,
		File							carrierFile,
		File							outFile,
		StreamConcealer.ILengthEncoder	lengthEncoder,
		int								maxReplacementDepth,
		StreamConcealer.IRandomSource	randomSource)
		throws AppException, TaskCancelledException
	{
		// Validate arguments
		if ((maxReplacementDepth < StreamConcealer.MIN_MAX_REPLACEMENT_DEPTH) ||
			 (maxReplacementDepth > StreamConcealer.MAX_MAX_REPLACEMENT_DEPTH))
			throw new IllegalArgumentException();

		// Test length of input file
		long fileLength = inFile.length();
		if (fileLength > StreamConcealer.MAX_PAYLOAD_LENGTH)
			throw new FileException(ErrorId.FILE_IS_TOO_LONG, inFile);

		// Enclose input file in image
		FileInputStream inStream = null;
		try
		{
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

			// Conceal data from input file in image
			conceal(inStream, carrierImageSource, carrierFile, outFile, (int)fileLength, lengthEncoder,
					maxReplacementDepth, randomSource);

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

	/**
	 * Conceals data read from an input stream (the <i>payload</i>) in an image (the <i>carrier</i>), and
	 * writes the resulting image in PNG format to a specified file.
	 * <p>
	 * The maximum length of the payload is 2<sup>24</sup> - 1 (16777215).
	 * </p>
	 *
	 * @param  inStream             the input stream from which the payload will be read.
	 * @param  carrierImageSource   the source of the image in which the payload will be concealed.  If
	 *                              {@code carrierImageSource} is {@code null}, the carrier image wil be
	 *                              read from the file specified by {@code carrierFile}.
	 * @param  carrierFile          the file containing the image in which the payload will be concealed.
	 *                              {@code carrierFile} is used as the source of the carrier image only if
	 *                              {@code carrierImageSource} is {@code null}.
	 * @param  outFile              the file to which the image containing the concealed payload will be
	 *                              written in PNG format.
	 * @param  length               the length (in bytes) of the payload.
	 * @param  lengthEncoder        the object that will encode the length of the payload as an array of
	 *                              bytes.
	 * @param  maxReplacementDepth  the maximum number of bits per RGB colour component of the carrier that
	 *                              will be replaced by the payload.
	 * @param  randomSource         a source of random data for replacing bits of the RGB components of the
	 *                              carrier that are not replaced by the payload, up to {@code
	 *                              maxReplacementDepth}.  If {@code randomSource} is {@code null}, no
	 *                              carrier bits will be replaced by random data.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code length} is negative or greater than 2<sup>24</sup> - 1 (16777215), or</li>
	 *             <li>{@code maxReplacementDepth} is less than 1 or greater than 6.</li>
	 *           </ul>
	 * @throws AppException
	 *           if an error occurred during the concealment operation.
	 * @throws TaskCancelledException
	 *           if the concealment operation was cancelled by the user.
	 * @see    #conceal(File, IImageSource, File, File, StreamConcealer.ILengthEncoder, int,
	 *                  StreamConcealer.IRandomSource)
	 * @see    #recover(File, File, StreamConcealer.ILengthDecoder)
	 * @see    #recover(File, OutputStream, StreamConcealer.ILengthDecoder)
	 */

	public void conceal(
		InputStream						inStream,
		IImageSource					carrierImageSource,
		File							carrierFile,
		File							outFile,
		int								length,
		StreamConcealer.ILengthEncoder	lengthEncoder,
		int								maxReplacementDepth,
		StreamConcealer.IRandomSource	randomSource)
		throws AppException, TaskCancelledException
	{
		// Validate arguments
		if ((length < 0) || (length > StreamConcealer.MAX_PAYLOAD_LENGTH) ||
			 (maxReplacementDepth < StreamConcealer.MIN_MAX_REPLACEMENT_DEPTH) ||
			 (maxReplacementDepth > StreamConcealer.MAX_MAX_REPLACEMENT_DEPTH))
			throw new IllegalArgumentException();

		// Run garbage collector to maximise available memory
		System.gc();

		// Get progress view
		IProgressView progressView = Task.getProgressView();

		// Read carrier file
		BufferedImage image = (carrierImageSource == null) ? null : carrierImageSource.getImage(length);
		if (image == null)
		{
			// Update progress view
			if (progressView != null)
			{
				progressView.setInfo(READING_STR, carrierFile);
				progressView.setProgress(0, -1.0);
			}

			// Read carrier image
			try
			{
				image = ImageIO.read(carrierFile);
				if (image == null)
					throw new FileException(ErrorId.INPUT_FORMAT_NOT_SUPPORTED, carrierFile);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, carrierFile);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.ERROR_READING_FILE, carrierFile);
			}
			catch (Exception e)
			{
				// ignore
			}
		}

		// Update progress view
		if (progressView != null)
		{
			progressView.setInfo(CONCEALING_STR);
			progressView.setProgress(0, 0.0);
		}

		// Conceal data from input stream in image
		BufferedImage outImage = null;
		try
		{
			StreamConcealer concealer = new StreamConcealer();
			if (progressView instanceof IProgressListener)
				concealer.addProgressListener((IProgressListener)progressView);
			outImage = concealer.conceal(inStream, image, length, lengthEncoder, maxReplacementDepth,
										 randomSource);
		}
		catch (StreamConcealer.InputException e)
		{
			e.setDataDescription(FILE_STR);
			throw new FileException(e, carrierFile);
		}

		// Update progress view
		if (progressView != null)
		{
			progressView.setInfo(WRITING_STR, outFile);
			progressView.setProgress(0, -1.0);
		}

		// Write output file
		try
		{
			if (!ImageIO.write(outImage, PNG_FORMAT_NAME, outFile))
				throw new FileException(ErrorId.PNG_OUTPUT_NOT_SUPPORTED, outFile);
		}
		catch (SecurityException e)
		{
			throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, outFile);
		}
		catch (IOException e)
		{
			throw new FileException(ErrorId.ERROR_WRITING_FILE, outFile);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Recovers concealed data (the <i>payload</i>) from a specified image file and writes the recovered
	 * data to a specified file.
	 *
	 * @param  inFile         the image file from which the concealed data will be recovered.
	 * @param  outFile        the file to which the recovered data will be written.
	 * @param  lengthDecoder  the object that will decode the length of the payload from an array of bytes.
	 * @throws AppException
	 *           if an error occurred during the recovery operation.
	 * @throws TaskCancelledException
	 *           if the recovery operation was cancelled by the user.
	 * @see    #recover(File, OutputStream, StreamConcealer.ILengthDecoder)
	 * @see    #conceal(File, IImageSource, File, File, StreamConcealer.ILengthEncoder, int,
	 *                  StreamConcealer.IRandomSource)
	 * @see    #conceal(InputStream, IImageSource, File, File, int, StreamConcealer.ILengthEncoder, int,
	 *                  StreamConcealer.IRandomSource)
	 */

	public void recover(
		File							inFile,
		File							outFile,
		StreamConcealer.ILengthDecoder	lengthDecoder)
		throws AppException, TaskCancelledException
	{
		File tempFile = null;
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

			// Recover concealed data from image file
			try
			{
				recover(inFile, outStream, lengthDecoder);
			}
			catch (StreamConcealer.OutputException e)
			{
				e.setDataDescription(FILE_STR);
				throw new FileException(e, tempFile);
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
	 * Recovers concealed data (the <i>payload</i>) from a specified image file and writes the recovered
	 * data to a specified output stream.
	 *
	 * @param  inFile         the image file from which the concealed data will be recovered.
	 * @param  outStream      the output stream to which the recovered data will be written.
	 * @param  lengthDecoder  the object that will decode the length of the payload from an array of bytes.
	 * @throws FileException
	 *           if an error occurs when reading the input file or recovering the concealed data.
	 * @throws StreamConcealer.OutputException
	 *           if an error occurs when writing to the output stream.
	 * @throws TaskCancelledException
	 *           if the recovery operation was cancelled by the user.
	 * @see    #recover(File, File, StreamConcealer.ILengthDecoder)
	 * @see    #conceal(File, IImageSource, File, File, StreamConcealer.ILengthEncoder, int,
	 *                  StreamConcealer.IRandomSource)
	 * @see    #conceal(InputStream, IImageSource, File, File, int, StreamConcealer.ILengthEncoder, int,
	 *                  StreamConcealer.IRandomSource)
	 */

	public void recover(
		File							inFile,
		OutputStream					outStream,
		StreamConcealer.ILengthDecoder	lengthDecoder)
		throws FileException, StreamConcealer.OutputException, TaskCancelledException
	{
		// Reset progress in progress view
		IProgressView progressView = Task.getProgressView();
		if (progressView != null)
		{
			progressView.setInfo(READING_STR, inFile);
			progressView.setProgress(0, -1.0);
		}

		// Read input file
		BufferedImage image = null;
		try
		{
			image = ImageIO.read(inFile);
			if (image == null)
				throw new FileException(ErrorId.INPUT_FORMAT_NOT_SUPPORTED, inFile);
		}
		catch (SecurityException e)
		{
			throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, inFile);
		}
		catch (IOException e)
		{
			throw new FileException(ErrorId.ERROR_READING_FILE, inFile);
		}
		catch (Exception e)
		{
			// ignore
		}

		// Update progress view
		if (progressView != null)
		{
			progressView.setInfo(RECOVERING_STR);
			progressView.setProgress(0, 0.0);
		}

		// Recover concealed data from image file
		try
		{
			StreamConcealer recoverer = new StreamConcealer();
			if (progressView instanceof IProgressListener)
				recoverer.addProgressListener((IProgressListener)progressView);
			recoverer.recover(image, outStream, lengthDecoder);
		}
		catch (StreamConcealer.InputException e)
		{
			e.setDataDescription(FILE_STR);
			throw new FileException(e, inFile);
		}
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

		FAILED_TO_OPEN_FILE
		("Failed to open the file."),

		FAILED_TO_CLOSE_FILE
		("Failed to close the file."),

		FAILED_TO_LOCK_FILE
		("Failed to lock the file."),

		ERROR_READING_FILE
		("An error occurred when reading the file."),

		ERROR_WRITING_FILE
		("An error occurred when writing the file."),

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

		FILE_IS_TOO_LONG
		("The file is too long to be concealed in an image."),

		INPUT_FORMAT_NOT_SUPPORTED
		("The input file may not be an image file or it may be an image file whose format is not\n" +
			"supported by this implementation of Java."),

		PNG_OUTPUT_NOT_SUPPORTED
		("This implementation of Java does not support the writing of PNG files.");

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


	// INTERFACE: IMAGE SOURCE


	/**
	 * This functional interface specifies the method that must be implemented by the provider of an image for use as a
	 * carrier in a concealment operation.
	 */

	@FunctionalInterface
	public interface IImageSource
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns an image for use as a carrier to conceal a payload of a specified length.
		 *
		 * @param  payloadLength  the length (in bytes) of the payload that the image will be used to
		 *                        conceal.
		 * @return an image for use as a carrier to conceal a payload of length {@code payloadLength}.
		 * @throws AppException
		 *           if an error occurred in the production of the image.
		 */

		BufferedImage getImage(
			int	payloadLength)
			throws AppException;

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
