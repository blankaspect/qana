/*====================================================================*\

FileSelectionKind.java

Enumeration: kind of file selection.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.event.KeyEvent;

import java.io.File;

import java.util.Arrays;

import javax.swing.JFileChooser;

import uk.blankaspect.common.misc.IStringKeyed;

import uk.blankaspect.ui.swing.filechooser.FileChooserUtils;

//----------------------------------------------------------------------


// ENUMERATION: KIND OF FILE SELECTION


enum FileSelectionKind
	implements IStringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	OPEN_ARCHIVE
	(
		"openArchive",
		"Open archive",
		FileKind.ARCHIVE
	)
	{
		@Override
		protected void initFileChooser(
			JFileChooser	fileChooser)
		{
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		}
	},

	SAVE_ARCHIVE
	(
		"saveArchive",
		"Save archive",
		FileKind.ARCHIVE
	)
	{
		@Override
		protected void initFileChooser(
			JFileChooser	fileChooser)
		{
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		}
	},

	ARCHIVE_DIRECTORY
	(
		"archiveDirectory",
		"Archive directory",
		null
	)
	{
		@Override
		protected void initFileChooser(
			JFileChooser	fileChooser)
		{
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
			fileChooser.setApproveButtonToolTipText(SELECT_DIRECTORY_STR);
		}
	},

	ENCRYPT
	(
		"encrypt",
		"Encrypt file",
		null
	)
	{
		@Override
		protected void initFileChooser(
			JFileChooser	fileChooser)
		{
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		}
	},

	DECRYPT
	(
		"decrypt",
		"Decrypt file",
		FileKind.ENCRYPTED
	)
	{
		@Override
		protected void initFileChooser(
			JFileChooser	fileChooser)
		{
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		}
	},

	VALIDATE
	(
		"validate",
		"Validate file",
		FileKind.ENCRYPTED
	)
	{
		@Override
		protected void initFileChooser(
			JFileChooser	fileChooser)
		{
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		}
	};

	//------------------------------------------------------------------

	private static final	String	SELECT_DIRECTORY_STR	= "Select directory";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	String			key;
	private	String			title;
	private	FileKind		fileKind;
	private	JFileChooser	fileChooser;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private FileSelectionKind(
		String		key,
		String		title,
		FileKind	fileKind)
	{
		this.key = key;
		this.title = title;
		this.fileKind = fileKind;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static FileSelectionKind forKey(
		String	key)
	{
		return Arrays.stream(values()).filter(value -> value.key.equals(key)).findFirst().orElse(null);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Abstract methods
////////////////////////////////////////////////////////////////////////

	protected abstract void initFileChooser(
		JFileChooser	fileChooser);

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

	public JFileChooser getFileChooser()
	{
		if (fileChooser == null)
		{
			AppConfig config = AppConfig.INSTANCE;
			File directory = config.isSaveFileSelectionPathnames() ? config.getFileSelectionDirectory(this) : null;
			fileChooser = new JFileChooser(directory);
			fileChooser.setDialogTitle(title);
			initFileChooser(fileChooser);
		}
		if (fileKind != null)
			FileChooserUtils.setFilter(fileChooser, fileKind.getFileFilter());
		return fileChooser;
	}

	//------------------------------------------------------------------

	public void updateDirectory()
	{
		AppConfig config = AppConfig.INSTANCE;
		String pathname = config.isSaveFileSelectionPathnames()
								? Utils.getPathname(fileChooser.getCurrentDirectory())
								: null;
		config.setFileSelectionPathname(this, pathname);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
