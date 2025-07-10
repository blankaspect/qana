/*====================================================================*\

BinaryFile.java

Binary file class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.misc;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import uk.blankaspect.common.bytedata.ByteDataList;
import uk.blankaspect.common.bytedata.IByteDataInputStream;
import uk.blankaspect.common.bytedata.IByteDataSource;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.TaskCancelledException;

//----------------------------------------------------------------------


// BINARY FILE CLASS


public class BinaryFile
	extends AbstractBinaryFile
{

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

		ERROR_READING_DATA
		("An error occurred when reading from the input stream."),

		ERROR_WRITING_FILE
		("An error occurred when writing the file.");

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

	public BinaryFile(File file)
	{
		super(file);
	}

	//------------------------------------------------------------------

	public BinaryFile(File            file,
					  IByteDataSource dataSource)
	{
		super(file);
		this.dataSource = dataSource;
	}

	//------------------------------------------------------------------

	public BinaryFile(File                 file,
					  IByteDataInputStream inStream)
	{
		super(file);
		this.inStream = inStream;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static byte[] read(File file)
		throws AppException
	{
		return new BinaryFile(file).read();
	}

	//------------------------------------------------------------------

	public static void read(File   file,
							byte[] buffer)
		throws AppException
	{
		new BinaryFile(file).read(buffer);
	}

	//------------------------------------------------------------------

	public static void read(File   file,
							byte[] buffer,
							int    offset,
							int    length)
		throws AppException
	{
		new BinaryFile(file).read(buffer, offset, length);
	}

	//------------------------------------------------------------------

	public static void write(File   file,
							 byte[] data)
		throws AppException
	{
		BinaryFile.write(file, data, FileWritingMode.DIRECT);
	}

	//------------------------------------------------------------------

	public static void write(File            file,
							 byte[]          data,
							 FileWritingMode writeMode)
		throws AppException
	{
		ByteDataList dataBlocks = new ByteDataList();
		dataBlocks.add(data);
		new BinaryFile(file, dataBlocks).write(FileWritingMode.DIRECT);
	}

	//------------------------------------------------------------------

	public static void write(File            file,
							 IByteDataSource dataSource)
		throws AppException
	{
		new BinaryFile(file, dataSource).write(FileWritingMode.DIRECT);
	}

	//------------------------------------------------------------------

	public static void write(File            file,
							 IByteDataSource dataSource,
							 FileWritingMode writeMode)
		throws AppException
	{
		new BinaryFile(file, dataSource).write(writeMode);
	}

	//------------------------------------------------------------------

	public static void write(File                 file,
							 IByteDataInputStream inStream)
		throws AppException
	{
		new BinaryFile(file, inStream).write(FileWritingMode.DIRECT);
	}

	//------------------------------------------------------------------

	public static void write(File                 file,
							 IByteDataInputStream inStream,
							 FileWritingMode      writeMode)
		throws AppException
	{
		new BinaryFile(file, inStream).write(writeMode);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public void writeData(OutputStream outStream)
		throws AppException
	{
		// Read data from data source and write it to output stream
		if (dataSource != null)
			writeData(outStream, dataSource);

		// Read data from input stream and write it to output stream
		else if (inStream != null)
			writeData(outStream, inStream);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private void writeData(OutputStream    outStream,
						   IByteDataSource dataSource)
		throws AppException
	{
		long inLength = dataSource.getLength();
		long offset = 0;
		while (true)
		{
			// Test whether task has been cancelled by a monitor
			for (IProgressListener listener : progressListeners)
			{
				if (listener.isTaskCancelled())
					throw new TaskCancelledException();
			}

			// Get data from source
			IByteDataSource.ByteData byteData = dataSource.getData();
			if (byteData == null)
				break;

			// Write data to output stream
			try
			{
				outStream.write(byteData.data, byteData.offset, byteData.length);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.ERROR_WRITING_FILE, file, e);
			}
			offset += byteData.length;

			// Notify monitor of progress
			for (IProgressListener listener : progressListeners)
				listener.setProgress((double)offset / (double)inLength);
		}
	}

	//------------------------------------------------------------------

	private void writeData(OutputStream         outStream,
						   IByteDataInputStream inStream)
		throws AppException
	{
		final	int	BUFFER_LENGTH	= 1 << 13;  // 8192

		byte[] buffer = new byte[BUFFER_LENGTH];
		long inLength = inStream.getLength();
		int blockLength = 0;
		long offset = 0;
		while (offset < inLength)
		{
			// Test whether task has been cancelled by a monitor
			for (IProgressListener listener : progressListeners)
			{
				if (listener.isTaskCancelled())
					throw new TaskCancelledException();
			}

			// Read data from input stream
			blockLength = (int)Math.min(inLength - offset, BUFFER_LENGTH);
			blockLength = inStream.read(buffer, 0, blockLength);
			if (blockLength < 0)
				throw new AppException(ErrorId.ERROR_READING_DATA);

			// Write data to output stream
			try
			{
				outStream.write(buffer, 0, blockLength);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.ERROR_WRITING_FILE, file, e);
			}
			offset += blockLength;

			// Notify monitor of progress
			for (IProgressListener listener : progressListeners)
				listener.setProgress((double)offset / (double)inLength);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	IByteDataSource			dataSource;
	private	IByteDataInputStream	inStream;

}

//----------------------------------------------------------------------
