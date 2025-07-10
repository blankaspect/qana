/*====================================================================*\

FileTransferHandler.java

File transfer handler class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;

import java.awt.datatransfer.UnsupportedFlavorException;

import java.io.File;
import java.io.IOException;

import java.util.List;

import javax.swing.TransferHandler;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.misc.IFileImporter;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.transfer.DataImporter;

//----------------------------------------------------------------------


// FILE TRANSFER HANDLER CLASS


class FileTransferHandler
	extends TransferHandler
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final	FileTransferHandler	INSTANCE	= new FileTransferHandler();

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

		FILE_TRANSFER_NOT_SUPPORTED
		("File transfer is not supported."),

		MULTIPLE_FILE_TRANSFER_NOT_SUPPORTED
		("The transfer of more than one file is not supported."),

		ERROR_TRANSFERRING_DATA
		("An error occurred while transferring data.");

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

	private FileTransferHandler()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static IFileImporter getFileImporter(Component component)
	{
		while (component != null)
		{
			if (component instanceof IFileImporter)
				return (IFileImporter)component;
			component = component.getParent();
		}
		return null;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public boolean canImport(TransferHandler.TransferSupport support)
	{
		boolean supported = !support.isDrop() || ((support.getSourceDropActions() & COPY) == COPY);
		if (supported)
		{
			IFileImporter fileImporter = getFileImporter(support.getComponent());
			supported = (fileImporter != null) && fileImporter.canImportFiles() &&
						DataImporter.isFileList(support.getDataFlavors());
		}
		if (support.isDrop() && supported)
			support.setDropAction(COPY);
		return supported;
	}

	//------------------------------------------------------------------

	@Override
	public boolean importData(TransferHandler.TransferSupport support)
	{
		if (canImport(support))
		{
			try
			{
				try
				{
					List<File> files = DataImporter.getFiles(support.getTransferable());
					if (!files.isEmpty())
					{
						GuiUtils.getWindow(support.getComponent()).toFront();
						IFileImporter fileImporter = getFileImporter(support.getComponent());
						if ((files.size() > 1) && !fileImporter.canImportMultipleFiles())
							throw new AppException(ErrorId.MULTIPLE_FILE_TRANSFER_NOT_SUPPORTED);
						QanaApp.INSTANCE.addImport(fileImporter, files);
						return true;
					}
				}
				catch (UnsupportedFlavorException e)
				{
					throw new AppException(ErrorId.FILE_TRANSFER_NOT_SUPPORTED);
				}
				catch (IOException e)
				{
					throw new AppException(ErrorId.ERROR_TRANSFERRING_DATA);
				}
			}
			catch (AppException e)
			{
				QanaApp.INSTANCE.showErrorMessage(QanaApp.SHORT_NAME, e);
			}
		}
		return false;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
