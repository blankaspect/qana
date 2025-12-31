/*====================================================================*\

IByteDataSource.java

Interface: byte-data source.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.bytedata;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.misc.IDataInput;

//----------------------------------------------------------------------


// INTERFACE: BYTE-DATA SOURCE


public interface IByteDataSource
	extends IDataInput
{

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	ByteData getData()
		throws AppException;

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: BYTE DATA


	public static class ByteData
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

		public ByteData(
			byte[]	data)
		{
			this.data = data;
			length = data.length;
		}

		//--------------------------------------------------------------

		public ByteData(
			byte[]	data,
			int		offset,
			int		length)
		{
			this.data = data;
			this.offset = offset;
			this.length = length;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
