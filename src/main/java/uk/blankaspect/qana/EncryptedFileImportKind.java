/*====================================================================*\

EncryptedFileImportKind.java

Encrypted file import kind enumeration.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.misc.IStringKeyed;

import uk.blankaspect.common.string.StringUtils;

//----------------------------------------------------------------------


// ENCRYPTED FILE IMPORT KIND ENUMERATION


enum EncryptedFileImportKind
	implements IStringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	DECRYPT     ("decrypt"),
	VALIDATE    ("validate");

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private EncryptedFileImportKind(String key)
	{
		this.key = key;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static EncryptedFileImportKind forKey(String key)
	{
		for (EncryptedFileImportKind value : values())
		{
			if (value.key.equals(key))
				return value;
		}
		return null;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IStringKeyed interface
////////////////////////////////////////////////////////////////////////

	public String getKey()
	{
		return key;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public String toString()
	{
		return StringUtils.firstCharToUpperCase(key);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	String	key;

}

//----------------------------------------------------------------------
