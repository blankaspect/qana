/*====================================================================*\

ByteBlockInputStream.java

Class: byte-block input stream.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.misc;

//----------------------------------------------------------------------


// IMPORTS


import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

import uk.blankaspect.common.bytedata.IByteDataInputStream;

//----------------------------------------------------------------------


// CLASS: BYTE-BLOCK INPUT STREAM


public class ByteBlockInputStream
	extends InputStream
	implements IByteDataInputStream
{

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	List<Block>	blocks;
	private	int			blockIndex;
	private	int			blockOffset;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public ByteBlockInputStream()
	{
		blocks = new ArrayList<>();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ByteDataInputStream interface
////////////////////////////////////////////////////////////////////////

	@Override
	public long getLength()
	{
		long length = 0;
		for (Block block : blocks)
			length += block.length;
		return length;
	}

	//------------------------------------------------------------------

	@Override
	public void reset()
	{
		blockIndex = 0;
		blockOffset = 0;
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */

	@Override
	public int read(byte[] buffer,
					int    offset,
					int    length)
	{
		// Validate arguments
		if (buffer == null)
			throw new IllegalArgumentException();
		if ((offset < 0) || (offset > buffer.length))
			throw new IndexOutOfBoundsException();
		if ((length < 0) || (length > buffer.length - offset))
			throw new IllegalArgumentException();

		// Test for end of blocks
		if (blockIndex >= blocks.size())
			return -1;

		// Read data from blocks
		int startOffset = offset;
		int endOffset = offset + length;
		while (offset < endOffset)
		{
			if (blockIndex >= blocks.size())
				break;
			Block block = blocks.get(blockIndex);
			int readLength = Math.min(endOffset - offset, block.length - blockOffset);
			System.arraycopy(block.data, block.offset + blockOffset, buffer, offset, readLength);
			blockOffset += readLength;
			if (blockOffset >= block.length)
			{
				blockOffset = 0;
				++blockIndex;
			}
			offset += readLength;
		}
		return (offset - startOffset);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public int available()
	{
		long length = 0;
		for (int i = blockIndex; i < blocks.size(); i++)
		{
			length += blocks.get(i).length;
			if (i == blockIndex)
				length -= blockOffset;
		}
		return (int)Math.min(length, Integer.MAX_VALUE);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */

	@Override
	public int read()
	{
		byte[] buffer = new byte[1];
		return ((read(buffer, 0, 1) < 0) ? -1 : buffer[0] & 0xFF);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */

	@Override
	public int read(byte[] buffer)
	{
		return read(buffer, 0, buffer.length);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	@Override
	public long skip(long length)
	{
		// Validate arguments
		if (length < 0)
			throw new IllegalArgumentException();

		// Skip data in blocks
		long offset = 0;
		while (offset < length)
		{
			if (blockIndex >= blocks.size())
				break;
			Block block = blocks.get(blockIndex);
			int skipLength = (int)Math.min(length - offset, block.length - blockOffset);
			blockOffset += skipLength;
			if (blockOffset >= block.length)
			{
				blockOffset = 0;
				++blockIndex;
			}
			offset += skipLength;
		}
		return offset;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public int getNumBlocks()
	{
		return blocks.size();
	}

	//------------------------------------------------------------------

	public Block getBlock(int index)
	{
		return blocks.get(index);
	}

	//------------------------------------------------------------------

	public void addBlock(byte[] data)
	{
		blocks.add(new Block(data));
	}

	//------------------------------------------------------------------

	public void addBlock(byte[] data,
						 int    offset,
						 int    length)
	{
		blocks.add(new Block(data, offset, length));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: BLOCK


	public static class Block
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		public	byte[]	data;
		public	int		offset;
		public	int		length;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * @throws IllegalArgumentException
		 * @throws IndexOutOfBoundsException
		 */

		public Block(byte[] data)
		{
			this(data, 0, data.length);
		}

		//--------------------------------------------------------------

		/**
		 * @throws IllegalArgumentException
		 * @throws IndexOutOfBoundsException
		 */

		public Block(byte[] data,
					 int    offset,
					 int    length)
		{
			// Validate arguments
			if (data == null)
				throw new IllegalArgumentException("Null data");
			if ((offset < 0) || (offset > data.length))
				throw new IndexOutOfBoundsException("Offset out of bounds");
			if ((length < 0) || (length > data.length - offset))
				throw new IllegalArgumentException("Length out of bounds");

			// Initialise instance variables
			this.data = data;
			this.offset = offset;
			this.length = length;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public int getEndOffset()
		{
			return (offset + length);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
