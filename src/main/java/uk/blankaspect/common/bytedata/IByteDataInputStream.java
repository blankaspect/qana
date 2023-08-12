/*====================================================================*\

IByteDataInputStream.java

Byte data input stream interface.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.bytedata;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.misc.IDataInput;

//----------------------------------------------------------------------


// BYTE DATA INPUT STREAM INTERFACE


public interface IByteDataInputStream
	extends IDataInput
{

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	int read(byte[] buffer,
			 int    offset,
			 int    length)
		throws AppException;

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
