/*====================================================================*\

FileSplitter.java

File splitter class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.channels.OverlappingFileLockException;

import java.util.ArrayList;
import java.util.List;

import uk.blankaspect.common.crypto.Fortuna;
import uk.blankaspect.common.crypto.FortunaAes256;
import uk.blankaspect.common.crypto.StreamEncrypter;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.TaskCancelledException;
import uk.blankaspect.common.exception.TempFileException;

import uk.blankaspect.common.filesystem.FilenameUtils;

import uk.blankaspect.common.misc.BinaryFile;

import uk.blankaspect.common.number.NumberCodec;
import uk.blankaspect.common.number.NumberUtils;

//----------------------------------------------------------------------


// FILE SPLITTER CLASS


class FileSplitter
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		int	MIN_FILE_PART_LENGTH	= 1 << 10;
	public static final		int	MAX_FILE_PART_LENGTH	= 1 << 30;

	private static final	int	NUM_FILE_PARTS_SHIFT	= 16;

	public static final		int	MAX_NUM_FILE_PARTS	= 1 << NUM_FILE_PARTS_SHIFT;    // 65536

	private static final	int	VERSION					= 0;
	private static final	int	MIN_SUPPORTED_VERSION	= VERSION;
	private static final	int	MAX_SUPPORTED_VERSION	= VERSION;

	private static final	int	FILENAME_LENGTH	= 40;

	private static final	String	FILE_STR	= "file";
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

		FILE_IS_EMPTY
		("The file is empty."),

		FILE_IS_TOO_LONG
		("The file is too long to split."),

		FILE_IS_TOO_SHORT
		("The file is too short to be part of a split file."),

		FAILED_TO_GET_FILE_TIMESTAMP
		("Failed to get the timestamp of the file."),

		FAILED_TO_SET_FILE_TIMESTAMP
		("Failed to set the timestamp of the file."),

		FAILED_TO_LIST_DIRECTORY_ENTRIES
		("Failed to get a list of directory entries."),

		NO_FILE_PARTS
		("The directory contains no valid file parts."),

		NO_SETS_OF_FILE_PARTS
		("The directory contains no valid sets of file parts."),

		INCONSISTENT_FILE_PARTS
		("The set of file parts is inconsistent.");

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
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// FIRST FILE PART CLASS


	public static class FirstFilePart
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FirstFilePart(String name,
							  int    numFileParts,
							  long   timestamp)
		{
			this.name = name;
			this.numFileParts = numFileParts;
			this.timestamp = timestamp;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		String	name;
		int		numFileParts;
		long	timestamp;

	}

	//==================================================================


	// FILE PART CLASS


	private static class FilePart
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FilePart(String name,
						 long   length)
		{
			this.name = name;
			this.length = length;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	name;
		private	long	length;

	}

	//==================================================================


	// FILE-PART FILTER CLASS


	private static class FilePartFilter
		implements FileFilter
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FilePartFilter()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : FileFilter interface
	////////////////////////////////////////////////////////////////////

		public boolean accept(File file)
		{
			if (file.isFile())
			{
				String name = file.getName().toLowerCase();
				if (name.length() == FILENAME_LENGTH)
				{
					for (int i = 0; i < FILENAME_LENGTH; i++)
					{
						if (!NumberUtils.isDigitCharLower(name.charAt(i), 16))
							return false;
					}
					return true;
				}
			}
			return false;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// FILE-PART NAME GENERATOR CLASS


	private static class FilenameGenerator
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FilenameGenerator()
		{
			this(createFilename(new FortunaAes256(QanaApp.INSTANCE.getRandomKey())));
		}

		//--------------------------------------------------------------

		private FilenameGenerator(String firstFilename)
		{
			this.firstFilename = firstFilename;
			prng = new FortunaAes256(firstFilename);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static String createFilename(Fortuna prng)
		{
			boolean caseChanged = NumberUtils.setLower();
			String str = NumberUtils.bytesToHexString(prng.getRandomBytes(FILENAME_LENGTH / 2));
			if (caseChanged)
				NumberUtils.setUpper();
			return str;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private String getNextName(List<FilePart> fileParts)
		{
			String filename = fileParts.isEmpty() ? firstFilename : null;
			while (filename == null)
			{
				filename = createFilename(prng);
				for (int i = 0; i < fileParts.size(); i++)
				{
					if (fileParts.get(i).name.equals(filename))
					{
						filename = null;
						break;
					}
				}
			}
			return filename;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	firstFilename;
		private	Fortuna	prng;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public FileSplitter()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static List<File> getFileParts(File directory)
		throws AppException
	{
		File[] files = directory.listFiles(new FilePartFilter());
		if (files == null)
			throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, directory);
		return List.of(files);
	}

	//------------------------------------------------------------------

	private static StreamEncrypter.Header createHeader(String filename,
													   int    index,
													   int    numFileParts)
	{
		int id = (numFileParts << NUM_FILE_PARTS_SHIFT) | index;
		id ^= new FortunaAes256(filename).getRandomInt();
		return new StreamEncrypter.Header(id, VERSION, MIN_SUPPORTED_VERSION, MAX_SUPPORTED_VERSION);
	}

	//------------------------------------------------------------------

	private static int[] parseHeader(byte[] data,
									 String filename)
	{
		int id = NumberCodec.bytesToUIntLE(data, 0, StreamEncrypter.Header.ID_FIELD_SIZE);
		id ^= new FortunaAes256(filename).getRandomInt();
		return new int[] { id & (1 << NUM_FILE_PARTS_SHIFT) - 1, id >>> NUM_FILE_PARTS_SHIFT };
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void split(File        inFile,
					  File        outDirectory,
					  int         filePartLengthLowerBound,
					  int         filePartLengthUpperBound,
					  KeyList.Key key)
		throws AppException
	{
		// Validate arguments
		if ((filePartLengthLowerBound < MIN_FILE_PART_LENGTH) ||(filePartLengthUpperBound > MAX_FILE_PART_LENGTH)
				|| (filePartLengthLowerBound > filePartLengthUpperBound))
			throw new IllegalArgumentException();

		// Reset progress in progress view
		TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
		progressView.setProgress(0, 0.0);
		progressView.setProgress(1, 0.0);

		// Test file length
		long inFileLength = inFile.length();
		if (inFileLength == 0)
			throw new FileException(ErrorId.FILE_IS_EMPTY, inFile);
		long meanFilePartLength = (filePartLengthLowerBound + filePartLengthUpperBound) / 2;
		if ((inFileLength + meanFilePartLength - 1) / meanFilePartLength > MAX_NUM_FILE_PARTS)
			throw new FileException(ErrorId.FILE_IS_TOO_LONG, inFile);

		// Create output directory
		if (!outDirectory.exists())
		{
			try
			{
				if (!outDirectory.mkdirs())
					throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, outDirectory);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, outDirectory, e);
			}
		}

		// Create list of names of existing file parts in output directory
		File[] files = outDirectory.listFiles(new FilePartFilter());
		if (files == null)
			throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, outDirectory);
		List<String> filenames = new ArrayList<>();
		for (File file : files)
			filenames.add(file.getName());

		// Create list of file parts
		List<FilePart> fileParts = new ArrayList<>();
		while (fileParts.isEmpty())
		{
			FilenameGenerator filenameGenerator = new FilenameGenerator();
			long remainingLength = inFileLength;
			while (remainingLength > 0)
			{
				int lengthRange = filePartLengthUpperBound - filePartLengthLowerBound + 1;
				int length = (int)Math.min(filePartLengthLowerBound + getRandomInt(lengthRange),
										   remainingLength);
				String filename = filenameGenerator.getNextName(fileParts);
				if (filenames.contains(filename))
				{
					fileParts.clear();
					break;
				}
				fileParts.add(new FilePart(filename, length));
				remainingLength -= length;
			}
		}

		// Read input file and write output files
		List<File> writtenFiles = new ArrayList<>();
		File tempFile = null;
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
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

			// Get timestamp
			long timestamp = inFile.lastModified();
			if (timestamp == 0)
				throw new FileException(ErrorId.FAILED_TO_GET_FILE_TIMESTAMP, inFile);

			// Write file parts
			long inOffset = 0;
			for (int i = 0; i < fileParts.size(); i++)
			{
				// Test whether task has been cancelled
				Task.throwIfCancelled();

				// Initialise output file
				FilePart filePart = fileParts.get(i);
				File outFile = new File(outDirectory, filePart.name);

				// Update information in progress view
				if (progressView != null)
				{
					progressView.setInfo(WRITING_STR, outFile);
					progressView.setProgress(0, 0.0);
					progressView.initOverallProgress(inOffset, filePart.length, inFileLength);
					progressView.waitForIdle();
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

				// Encrypt file
				try
				{
					long tstamp = (i == 0) ? timestamp : QanaApp.INSTANCE.getRandomLong();
					StreamEncrypter encrypter =
							key.getStreamEncrypter(Utils.getCipher(key),
												   createHeader(filePart.name, i, fileParts.size()));
					encrypter.addProgressListener(progressView);
					encrypter.encrypt(inStream, outStream, filePart.length, tstamp, key.getKey(),
									  QanaApp.INSTANCE.getRandomKey(), QanaApp.INSTANCE::generateKey);
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
					tempFile = null;
				}
				catch (SecurityException e)
				{
					throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, outFile, e, tempFile);
				}

				// Add file to list
				writtenFiles.add(outFile);

				// Set timestamp of output file
				outFile.setLastModified(timestamp);

				// Increment offset
				inOffset += filePart.length;
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
				if ((tempFile != null) && tempFile.exists())
					tempFile.delete();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Delete written files
			for (File file : writtenFiles)
			{
				try
				{
					file.delete();
				}
				catch (Exception e1)
				{
					// ignore
				}
			}

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

	public void join(File        inDirectory,
					 File        outFile,
					 KeyList.Key key)
		throws AppException
	{
		// Reset progress in progress view
		TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
		progressView.setProgress(0, 0.0);
		progressView.setProgress(1, 0.0);

		// Create an array of file parts in input directory
		File[] files = inDirectory.listFiles(new FilePartFilter());
		if (files == null)
			throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, inDirectory);

		// Create list of all file parts
		List<FilePart> fileParts = new ArrayList<>();
		for (File file : files)
		{
			long length = file.length();
			if (length >= StreamEncrypter.Header.SIZE);
				fileParts.add(new FilePart(file.getName().toLowerCase(), length));
		}
		if (fileParts.isEmpty())
			throw new FileException(ErrorId.NO_FILE_PARTS, inDirectory);

		// Create list of first file parts
		List<FirstFilePart> firstFileParts = new ArrayList<>();
		for (FilePart filePart : fileParts)
		{
			// Test whether task has been cancelled
			Task.throwIfCancelled();

			// Read file header
			File file = new File(inDirectory, filePart.name);
			byte[] buffer = new byte[StreamEncrypter.Header.SIZE];
			BinaryFile.read(file, buffer);

			// Test for first file part
			int[] values = parseHeader(buffer, filePart.name);
			if (values[0] == 0)
				firstFileParts.add(new FirstFilePart(filePart.name, values[1], file.lastModified()));
		}
		if (firstFileParts.isEmpty())
			throw new FileException(ErrorId.NO_SETS_OF_FILE_PARTS, inDirectory);

		// Select first file part
		FirstFilePart firstFilePart = firstFileParts.get(0);
		if (firstFileParts.size() > 1)
		{
			firstFilePart = FilePartSetSelectionDialog.showDialog(progressView.getOwner(),
																  firstFileParts);
			if (firstFilePart == null)
				throw new TaskCancelledException();
		}

		// Create list of file parts of selected set
		FilenameGenerator filenameGenerator = new FilenameGenerator(firstFilePart.name);
		fileParts.clear();
		long totalInFileLengths = 0;
		int numFileParts = firstFilePart.numFileParts;
		for (int i = 0; i < numFileParts; i++)
		{
			// Test whether task has been cancelled
			Task.throwIfCancelled();

			// Test length of file part
			String filename = filenameGenerator.getNextName(fileParts);
			File file = new File(inDirectory, filename);
			long length = file.length();
			if (length < key.getStreamEncrypter(null, createHeader(filename, i, numFileParts)).
																					getMinOverheadSize())
				throw new FileException(ErrorId.FILE_IS_TOO_SHORT, inDirectory);
			totalInFileLengths += length;

			// Read file header
			byte[] buffer = new byte[StreamEncrypter.Header.SIZE];
			BinaryFile.read(file, buffer);

			// Test file header
			int[] values = parseHeader(buffer, filename);
			if ((values[0] != i) || (values[1] != numFileParts))
				throw new AppException(ErrorId.INCONSISTENT_FILE_PARTS);

			// Add file part to list
			fileParts.add(new FilePart(filename, length));
		}
		if (fileParts.size() != numFileParts)
			throw new AppException(ErrorId.INCONSISTENT_FILE_PARTS);

		// Read input files and write output file
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

			// Read input files and write output file
			long timestamp = 0;
			long inOffset = 0;
			for (int i = 0; i < fileParts.size(); i++)
			{
				// Test whether task has been cancelled
				Task.throwIfCancelled();

				// Initialise input file
				FilePart filePart = fileParts.get(i);
				File inFile = new File(inDirectory, filePart.name);

				// Update information in progress view
				if (progressView != null)
				{
					progressView.setInfo(READING_STR, inFile);
					progressView.setProgress(0, 0.0);
					progressView.initOverallProgress(inOffset, filePart.length, totalInFileLengths);
					progressView.waitForIdle();
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

				// Decrypt file
				try
				{
					StreamEncrypter encrypter = key.getStreamEncrypter(null,
																	   createHeader(filePart.name, i,
																					fileParts.size()));
					encrypter.addProgressListener(progressView);
					long tstamp = encrypter.decrypt(inStream, outStream, filePart.length, key.getKey(),
													QanaApp.INSTANCE::generateKey);
					if (i == 0)
						timestamp = tstamp;
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

				// Increment offset
				inOffset += filePart.length;
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

	private int getRandomInt(int range)
	{
		long value = (long)(QanaApp.INSTANCE.getRandomInt() & 0x7FFFFFFF) * (long)range;
		return (int)(value >>> 31);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
