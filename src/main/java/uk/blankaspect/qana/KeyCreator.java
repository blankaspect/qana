/*====================================================================*\

KeyCreator.java

Key creator class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import uk.blankaspect.common.crypto.FortunaCipher;
import uk.blankaspect.common.crypto.StreamEncrypter;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.gui.RunnableMessageDialog;

import uk.blankaspect.common.misc.StringUtils;

//----------------------------------------------------------------------


// KEY CREATOR CLASS


class KeyCreator
	implements RunnableMessageDialog.IRunnable
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	PERSISTENT_MESSAGE_STR	= "Creating key \"%1\"" + AppConstants.ELLIPSIS_STR;
	private static final	String	TEMPORARY_MESSAGE_STR	= "Creating a temporary key " + AppConstants.ELLIPSIS_STR;

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

		NOT_ENOUGH_MEMORY
		("There was not enough memory to create the key.");

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

	public KeyCreator(String                                 name,
					  String                                 passphrase,
					  Map<KdfUse, StreamEncrypter.KdfParams> kdfParamMap,
					  Set<FortunaCipher>                     allowedCiphers,
					  FortunaCipher                          preferredCipher)
	{
		this.name = name;
		this.passphrase = passphrase;
		this.kdfParamMap = kdfParamMap;
		this.allowedCiphers = EnumSet.copyOf(allowedCiphers);
		this.preferredCipher = preferredCipher;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : RunnableMessageDialog.IRunnable interface
////////////////////////////////////////////////////////////////////////

	@Override
	public String getMessage()
	{
		return ((name == null) ? TEMPORARY_MESSAGE_STR : StringUtils.substitute(PERSISTENT_MESSAGE_STR, name));
	}

	//------------------------------------------------------------------

	@Override
	public void run()
	{
		try
		{
			key = KeyList.createKey(name, passphrase, kdfParamMap.get(KdfUse.VERIFICATION),
									kdfParamMap.get(KdfUse.GENERATION), allowedCiphers, preferredCipher);
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

	public KeyList.Key create(Component component)
		throws AppException
	{
		outOfMemory = false;
		RunnableMessageDialog.showDialog(component, this);
		if (outOfMemory)
			throw new AppException(ErrorId.NOT_ENOUGH_MEMORY);
		return key;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance fields
////////////////////////////////////////////////////////////////////////

	private	String									name;
	private	String									passphrase;
	private	Map<KdfUse, StreamEncrypter.KdfParams>	kdfParamMap;
	private	Set<FortunaCipher>						allowedCiphers;
	private	FortunaCipher							preferredCipher;
	private	KeyList.Key								key;
	private	boolean									outOfMemory;

}

//----------------------------------------------------------------------
