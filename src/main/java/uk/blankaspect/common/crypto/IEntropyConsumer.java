/*====================================================================*\

IEntropyConsumer.java

Entropy consumer interface.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// ENTROPY CONSUMER INTERFACE


/**
 * This interface defines methods with which an entropy producer can add random data to an entropy consumer.
 */

public interface IEntropyConsumer
{

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Adds a specified byte of random data to this entropy consumer.
	 *
	 * @param b  the byte of random data that will be added to this entropy consumer.
	 */

	void addRandomByte(byte b);

	//------------------------------------------------------------------

	/**
	 * Adds a specified number of bytes of random data to this entropy consumer.
	 *
	 * @param data    the random data that will be added to this entropy consumer.
	 * @param offset  the offset of the start of the random data in {@code data}.
	 * @param length  the number of bytes of random data to add.
	 */

	void addRandomBytes(byte[] data,
						int    offset,
						int    length);

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
