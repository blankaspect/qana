/*====================================================================*\

EraseDialog.java

File erasure dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.Window;

import java.io.File;

import java.util.EnumSet;
import java.util.List;

import javax.swing.JOptionPane;

import uk.blankaspect.common.filesystem.PathnameUtils;

import uk.blankaspect.ui.swing.dialog.FileMultipleSelectionDialog;

import uk.blankaspect.ui.swing.misc.GuiUtils;

//----------------------------------------------------------------------


// FILE ERASURE DIALOG CLASS


class EraseDialog
	extends FileMultipleSelectionDialog
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		LIST_NUM_VIEWABLE_COLUMNS	= 48;
	private static final	int		LIST_NUM_VIEWABLE_ROWS		= 24;

	private static final	String	ERASE_FILES_STR		= "Erase files";
	private static final	String	ERASE_STR			= "Erase";
	private static final	String	NUM_DIRECTORIES_STR	= "Number of directories : ";
	private static final	String	NUM_FILES_STR		= "Number of files : ";
	private static final	String	ERASE_ITEMS_STR		= "Do you want to erase these items?";

	private static final	String	KEY	= EraseDialog.class.getCanonicalName();

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private EraseDialog(Window owner)
	{
		super(owner, LIST_NUM_VIEWABLE_COLUMNS, LIST_NUM_VIEWABLE_ROWS, ERASE_FILES_STR,
			  ERASE_STR + AppConstants.ELLIPSIS_STR, KEY, SelectionMode.FILES_AND_DIRECTORIES,
			  EnumSet.of(Capability.ADD, Capability.REMOVE), null, true);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static List<String> showDialog(Component parent)
	{
		return new EraseDialog(GuiUtils.getWindow(parent)).getPathnames();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	protected boolean accept(List<String> pathnames)
	{
		boolean accepted = false;
		if (!pathnames.isEmpty())
		{
			// Count files and directories
			int numFiles = 0;
			int numDirectories = 0;
			for (String pathname : pathnames)
			{
				if (new File(PathnameUtils.parsePathname(pathname)).isDirectory())
					++numDirectories;
				else
					++numFiles;
			}

			// Prompt to delete files and directories
			StringBuilder buffer = new StringBuilder(128);
			if (numDirectories > 0)
			{
				buffer.append(NUM_DIRECTORIES_STR);
				buffer.append(numDirectories);
				buffer.append('\n');
			}
			if (numFiles > 0)
			{
				buffer.append(NUM_FILES_STR);
				buffer.append(numFiles);
				buffer.append('\n');
			}
			buffer.append(ERASE_ITEMS_STR);
			String[] optionStrs = Utils.getOptionStrings(ERASE_STR);
			if (JOptionPane.showOptionDialog(this, buffer, ERASE_FILES_STR, JOptionPane.OK_CANCEL_OPTION,
											 JOptionPane.WARNING_MESSAGE, null, optionStrs,
											 optionStrs[1]) == JOptionPane.OK_OPTION)
				accepted = true;
		}
		return accepted;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
