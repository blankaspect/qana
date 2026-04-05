/*====================================================================*\

FileKind.java

Enumeration: kind of file.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.misc.FilenameSuffixFilter;
import uk.blankaspect.common.misc.IStringKeyed;

import uk.blankaspect.common.platform.windows.FileAssociations;

//----------------------------------------------------------------------


// ENUMERATION: KIND OF FILE


enum FileKind
	implements IStringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	ENCRYPTED
	(
		"encrypted",
		".qana",
		"Encrypted files",
		FileAssoc.KEY_PREFIX + ".encryptedFile",
		"Qana-encrypted file",
		"&Decrypt with " + QanaApp.SHORT_NAME
	),

	ARCHIVE
	(
		"archive",
		".qarc",
		"Encrypted archives",
		FileAssoc.KEY_PREFIX + ".archive",
		"Qana archive",
		"&Open with " + QanaApp.SHORT_NAME
	);

	private interface FileAssoc
	{
		String	KEY_PREFIX	= "BlankAspect." + QanaApp.SHORT_NAME;
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	String	key;
	private	String	defaultFilenameSuffix;
	private	String	description;
	private	String	fileAssocFileKindKey;
	private	String	fileAssocFileKindText;
	private	String	fileAssocFileOpenText;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private FileKind(
		String	key,
		String	defaultFilenameSuffix,
		String	description,
		String	fileAssocFileKindKey,
		String	fileAssocFileKindText,
		String	fileAssocFileOpenText)
	{
		this.key = key;
		this.defaultFilenameSuffix = defaultFilenameSuffix;
		this.description = description;
		this.fileAssocFileKindKey = fileAssocFileKindKey;
		this.fileAssocFileKindText = fileAssocFileKindText;
		this.fileAssocFileOpenText = fileAssocFileOpenText;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IStringKeyed interface
////////////////////////////////////////////////////////////////////////

	@Override
	public String getKey()
	{
		return key;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public String getDefaultFilenameSuffix()
	{
		return defaultFilenameSuffix;
	}

	//------------------------------------------------------------------

	public String getDescription()
	{
		return description;
	}

	//------------------------------------------------------------------

	public String getFilenameSuffix()
	{
		return AppConfig.INSTANCE.getFilenameSuffix(this);
	}

	//------------------------------------------------------------------

	public FilenameSuffixFilter getFileFilter()
	{
		return new FilenameSuffixFilter(description, getFilenameSuffix());
	}

	//------------------------------------------------------------------

	public void addFileAssocParams(
		FileAssociations	fileAssociations)
	{
		fileAssociations.addParams(fileAssocFileKindKey, fileAssocFileKindText, fileAssocFileOpenText,
								   getFilenameSuffix());
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
