/*====================================================================*\

ByteDataList.java

Class: byte-data list.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.bytedata;

//----------------------------------------------------------------------


// IMPORTS


import java.util.ArrayList;
import java.util.List;

import uk.blankaspect.common.exception.AppException;

//----------------------------------------------------------------------


// CLASS: BYTE-DATA LIST


public class ByteDataList
	implements IByteDataSource
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public ByteDataList()
	{
		dataBlocks = new ArrayList<>();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IByteDataSource interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void reset()
	{
		outIndex = 0;
	}

	//------------------------------------------------------------------

	@Override
	public long getLength()
	{
		long length = 0;
		for (ByteData dataBlock : dataBlocks)
			length += dataBlock.length;
		return length;
	}

	//------------------------------------------------------------------

	@Override
	public ByteData getData()
		throws AppException
	{
		return (outIndex < dataBlocks.size()) ? dataBlocks.get(outIndex++) : null;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public int getNumBlocks()
	{
		return dataBlocks.size();
	}

	//------------------------------------------------------------------

	public ByteData getBlock(int index)
	{
		return dataBlocks.get(index);
	}

	//------------------------------------------------------------------

	public void add(byte[] data)
	{
		dataBlocks.add(new ByteData(data));
	}

	//------------------------------------------------------------------

	public void add(byte[] data,
					int    offset,
					int    length)
	{
		dataBlocks.add(new ByteData(data, offset, length));
	}

	//------------------------------------------------------------------

	public int getData(byte[] buffer)
	{
		return getData(buffer, 0, buffer.length);
	}

	//------------------------------------------------------------------

	public int getData(byte[] buffer,
					   int    offset,
					   int    length)
	{
		int startOffset = offset;
		int endOffset = offset + length;
		for (ByteData dataBlock : dataBlocks)
		{
			if (offset >= endOffset)
				break;
			int blockLength = Math.min(dataBlock.length, endOffset - offset);
			System.arraycopy(dataBlock.data, dataBlock.offset, buffer, offset, blockLength);
			offset += blockLength;
		}
		return offset - startOffset;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	List<ByteData>	dataBlocks;
	private	int				outIndex;

}

//----------------------------------------------------------------------
