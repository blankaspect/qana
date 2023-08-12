/*====================================================================*\

KeyVerifier.java

Key verifier class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.ui.swing.dialog.RunnableMessageDialog;

//----------------------------------------------------------------------


// KEY VERIFIER CLASS


class KeyVerifier
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	MESSAGE_STR	= "Verifying key '%s' " + AppConstants.ELLIPSIS_STR;

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

		INCORRECT_PASSPHRASE
		("The passphrase is incorrect."),

		NOT_ENOUGH_MEMORY
		("There was not enough memory to verify the key.");

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

	public KeyVerifier(KeyList.Key key,
					   String      passphrase)
	{
		this.key = key;
		this.passphrase = passphrase;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void verify(Component component)
		throws AppException
	{
		outOfMemory = false;
		RunnableMessageDialog.showDialog(component, String.format(MESSAGE_STR, key.getName()), () ->
		{
			try
			{
				verified = key.verify(passphrase);
			}
			catch (OutOfMemoryError e)
			{
				outOfMemory = true;
			}
		});
		if (outOfMemory)
			throw new AppException(ErrorId.NOT_ENOUGH_MEMORY);
		if (!verified)
			throw new AppException(ErrorId.INCORRECT_PASSPHRASE);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	KeyList.Key	key;
	private	String		passphrase;
	private	boolean		verified;
	private	boolean		outOfMemory;

}

//----------------------------------------------------------------------
