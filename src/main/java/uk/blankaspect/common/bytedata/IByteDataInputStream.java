/*====================================================================*\

IByteDataInputStream.java

Interface: byte-data input stream.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.bytedata;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.misc.IDataInput;

//----------------------------------------------------------------------


// INTERFACE: BYTE-DATA INPUT STREAM


public interface IByteDataInputStream
	extends IDataInput
{

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	int read(
		byte[]	buffer,
		int		offset,
		int		length)
		throws AppException;

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
