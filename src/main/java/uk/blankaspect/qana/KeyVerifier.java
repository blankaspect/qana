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

import uk.blankaspect.common.gui.RunnableMessageDialog;

import uk.blankaspect.common.indexedsub.IndexedSub;

//----------------------------------------------------------------------


// KEY VERIFIER CLASS


class KeyVerifier
	implements RunnableMessageDialog.IRunnable
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	MESSAGE_STR	= "Verifying key \"%1\" " + AppConstants.ELLIPSIS_STR;

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
	//  Instance fields
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
//  Instance methods : RunnableMessageDialog.IRunnable interface
////////////////////////////////////////////////////////////////////////

	@Override
	public String getMessage()
	{
		return IndexedSub.sub(MESSAGE_STR, key.getName());
	}

	//------------------------------------------------------------------

	@Override
	public void run()
	{
		try
		{
			verified = key.verify(passphrase);
		}
		catch (OutOfMemoryError e)
		{
			outOfMemory = true;
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void verify(Component component)
		throws AppException
	{
		outOfMemory = false;
		RunnableMessageDialog.showDialog(component, this);
		if (outOfMemory)
			throw new AppException(ErrorId.NOT_ENOUGH_MEMORY);
		if (!verified)
			throw new AppException(ErrorId.INCORRECT_PASSPHRASE);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance fields
////////////////////////////////////////////////////////////////////////

	private	KeyList.Key	key;
	private	String		passphrase;
	private	boolean		verified;
	private	boolean		outOfMemory;

}

//----------------------------------------------------------------------
