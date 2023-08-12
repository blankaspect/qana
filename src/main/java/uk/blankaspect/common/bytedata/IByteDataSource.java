/*====================================================================*\

IByteDataSource.java

Byte data source interface.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.bytedata;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.misc.IDataInput;

//----------------------------------------------------------------------


// BYTE DATA SOURCE INTERFACE


public interface IByteDataSource
	extends IDataInput
{

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// BYTE DATA CLASS


	public static class ByteData
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public ByteData(byte[] data)
		{
			this.data = data;
			length = data.length;
		}

		//--------------------------------------------------------------

		public ByteData(byte[] data,
						int    offset,
						int    length)
		{
			this.data = data;
			this.offset = offset;
			this.length = length;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		public	byte[]	data;
		public	int		offset;
		public	int		length;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	ByteData getData()
		throws AppException;

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
