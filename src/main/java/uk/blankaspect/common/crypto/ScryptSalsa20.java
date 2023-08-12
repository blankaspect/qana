/*====================================================================*\

ScryptSalsa20.java

Class: scrypt password-based key-derivation function with Salsa20 as its core hash function.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// CLASS: SCRYPT PASSWORD-BASED KEY-DERIVATION FUNCTION WITH SALSA20 AS ITS CORE HASH FUNCTION


/**
 * This class is an implementation of the <i>scrypt</i> password-based key derivation function (KDF) with Salsa20 as its
 * core hash function.
 * <p>
 * The <i>scrypt</i> function is specified in <a href="https://tools.ietf.org/html/rfc7914">IETF RFC 7914</a>.
 * </p>
 */

public class ScryptSalsa20
	extends Scrypt
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of a scrypt password-based key-derivation function with the default number of rounds of
	 * the core hash function.
	 */

	public ScryptSalsa20()
	{
		// Call alternative constructor
		this(CoreHashNumRounds.DEFAULT);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of a scrypt password-based key-derivation function with the specified number of rounds of
	 * the core hash function.
	 *
	 * @param coreHashNumRounds
	 *          the number of rounds of the core hash function.
	 */

	public ScryptSalsa20(CoreHashNumRounds coreHashNumRounds)
	{
		// Call superclass constructor
		super(coreHashNumRounds, (inData, outData, numRounds) -> Salsa20.hash(inData, outData, numRounds));
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
