/*====================================================================*\

Utils.java

Utility methods class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Toolkit;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.io.File;
import java.io.IOException;

import uk.blankaspect.common.config.PropertiesPathname;

import uk.blankaspect.common.crypto.FortunaCipher;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.exception2.ExceptionUtils;

import uk.blankaspect.common.filesystem.PathnameUtils;

import uk.blankaspect.common.string.StringUtils;

//----------------------------------------------------------------------


// UTILITY METHODS CLASS


class Utils
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	FAILED_TO_GET_PATHNAME_STR	= "Failed to get the canonical pathname for ";

	private static final	String	FILE_STR	= "file";

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

		CLIPBOARD_IS_UNAVAILABLE
		("The clipboard is currently unavailable."),

		FAILED_TO_GET_CLIPBOARD_DATA
		("Failed to get data from the clipboard."),

		NO_TEXT_ON_CLIPBOARD
		("There is no text on the clipboard.");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

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

		@Override
		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private Utils()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static int indexOf(Object   target,
							  Object[] values)
	{
		for (int i = 0; i < values.length; i++)
		{
			if (values[i].equals(target))
				return i;
		}
		return -1;
	}

	//------------------------------------------------------------------

	public static char getFileSeparatorChar()
	{
		return (AppConfig.INSTANCE.isShowUnixPathnames() ? '/' : File.separatorChar);
	}

	//------------------------------------------------------------------

	public static String getPathname(File file)
	{
		return getPathname(file, AppConfig.INSTANCE.isShowUnixPathnames());
	}

	//------------------------------------------------------------------

	public static String getPathname(File    file,
									 boolean unixStyle)
	{
		String pathname = null;
		if (file != null)
		{
			try
			{
				pathname = file.getCanonicalPath();
			}
			catch (Exception e)
			{
				ExceptionUtils.printStderrLocated(FAILED_TO_GET_PATHNAME_STR + file.getPath());
				System.err.println("- " + e);
				pathname = file.getAbsolutePath();
			}

			if (unixStyle)
				pathname = PathnameUtils.toUnixStyle(pathname, true);
		}
		return pathname;
	}

	//------------------------------------------------------------------

	public static String getPropertiesPathname()
	{
		String pathname = PropertiesPathname.getPathname();
		if (pathname != null)
			pathname += App.NAME_KEY;
		return pathname;
	}

	//------------------------------------------------------------------

	public static boolean isSameFile(File file1,
									 File file2)
	{
		try
		{
			if (file1 == null)
				return (file2 == null);
			return ((file2 != null) && file1.getCanonicalPath().equals(file2.getCanonicalPath()));
		}
		catch (IOException e)
		{
			return false;
		}
	}

	//------------------------------------------------------------------

	public static File appendSuffix(File   file,
									String suffix)
	{
		String filename = file.getName();
		if (!filename.isEmpty() && (filename.indexOf('.') < 0))
			file = new File(file.getParentFile(), filename + suffix);
		return file;
	}

	//------------------------------------------------------------------

	public static String[] getOptionStrings(String... optionStrs)
	{
		String[] strs = new String[optionStrs.length + 1];
		System.arraycopy(optionStrs, 0, strs, 0, optionStrs.length);
		strs[optionStrs.length] = AppConstants.CANCEL_STR;
		return strs;
	}

	//------------------------------------------------------------------

	public static boolean clipboardHasText()
	{
		try
		{
			return Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.stringFlavor);
		}
		catch (IllegalStateException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	//------------------------------------------------------------------

	public static String getClipboardText()
		throws AppException
	{
		try
		{
			Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			if (contents == null)
				throw new AppException(ErrorId.NO_TEXT_ON_CLIPBOARD);
			return (String)contents.getTransferData(DataFlavor.stringFlavor);
		}
		catch (IllegalStateException e)
		{
			throw new AppException(ErrorId.CLIPBOARD_IS_UNAVAILABLE, e);
		}
		catch (UnsupportedFlavorException e)
		{
			throw new AppException(ErrorId.NO_TEXT_ON_CLIPBOARD);
		}
		catch (IOException e)
		{
			throw new AppException(ErrorId.FAILED_TO_GET_CLIPBOARD_DATA, e);
		}
	}

	//------------------------------------------------------------------

	public static void putClipboardText(String text)
		throws AppException
	{
		try
		{
			StringSelection selection = new StringSelection(text);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
		}
		catch (IllegalStateException e)
		{
			throw new AppException(ErrorId.CLIPBOARD_IS_UNAVAILABLE, e);
		}
	}

	//------------------------------------------------------------------

	public static String getFileString(int numFiles)
	{
		return ((numFiles == 1) ? FILE_STR : FILE_STR + "s");
	}

	//------------------------------------------------------------------

	public static FortunaCipher getCipher()
	{
		return AppConfig.INSTANCE.getPrngDefaultCipher();
	}

	//------------------------------------------------------------------

	public static FortunaCipher getCipher(KeyList.Key key)
	{
		FortunaCipher cipher = (key == null) ? null : key.getPreferredCipher();
		return ((cipher == null) ? AppConfig.INSTANCE.getPrngDefaultCipher() : cipher);
	}

	//------------------------------------------------------------------

	public static File getPlaintextFile(File   directory,
										String filename)
	{
		File file = null;
		int length = filename.length();
		filename = StringUtils.removeSuffix(filename, FileKind.ENCRYPTED.getFilenameSuffix());
		if (filename.length() < length)
			file = new File(directory, filename);
		else
		{
			String[] filenameParts = StringUtils.splitAtFirst(filename, '.', StringUtils.SplitMode.SUFFIX);
			int index = 1;
			while (true)
			{
				file = new File(directory, filenameParts[0] + "-" + index + filenameParts[1]);
				if (!file.exists())
					break;
				++index;
			}
		}
		return file;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
