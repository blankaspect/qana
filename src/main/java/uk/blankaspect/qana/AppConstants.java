/*====================================================================*\

AppConstants.java

Interface: application constants.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Insets;

import java.text.DecimalFormat;

import uk.blankaspect.common.misc.FilenameSuffixFilter;

//----------------------------------------------------------------------


// INTERFACE: APPLICATION CONSTANTS


interface AppConstants
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	// Component constants
	Insets	COMPONENT_INSETS			= new Insets(2, 3, 2, 3);
	Insets	COMPONENT_INSETS_EXTRA_TOP	= new Insets(4, 3, 2, 3);

	// Decimal formats
	DecimalFormat	FORMAT_1_3	= new DecimalFormat("0.0##");
	DecimalFormat	FORMAT_1_4	= new DecimalFormat("0.0###");

	// Strings
	String	ELLIPSIS_STR		= "...";
	String	FILE_CHANGED_SUFFIX	= " *";
	String	OK_STR				= "OK";
	String	CANCEL_STR			= "Cancel";
	String	CLOSE_STR			= "Close";
	String	CONTINUE_STR		= "Continue";
	String	REPLACE_STR			= "Replace";
	String	CLEAR_STR			= "Clear";
	String	DELETE_STR			= "Delete";
	String	ALREADY_EXISTS_STR	= "\nThe file already exists.\nDo you want to replace it?";

	// Filename extensions
	String	BMP_FILENAME_EXTENSION		= ".bmp";
	String	GIF_FILENAME_EXTENSION		= ".gif";
	String	JPEG_FILENAME_EXTENSION1	= ".jpg";
	String	JPEG_FILENAME_EXTENSION2	= ".jpeg";
	String	KEY_FILENAME_EXTENSION		= ".keys";
	String	PNG_FILENAME_EXTENSION		= ".png";
	String	XML_FILENAME_EXTENSION		= ".xml";

	// Filters for file choosers
	FilenameSuffixFilter BMP_FILE_FILTER	=
			new FilenameSuffixFilter("Bitmap files", BMP_FILENAME_EXTENSION);
	FilenameSuffixFilter GIF_FILE_FILTER	=
			new FilenameSuffixFilter("GIF files", GIF_FILENAME_EXTENSION);
	FilenameSuffixFilter JPEG_FILE_FILTER	=
			new FilenameSuffixFilter("JPEG files", JPEG_FILENAME_EXTENSION1, JPEG_FILENAME_EXTENSION2);
	FilenameSuffixFilter KEY_FILE_FILTER	=
			new FilenameSuffixFilter(QanaApp.SHORT_NAME + " key database files", KEY_FILENAME_EXTENSION);
	FilenameSuffixFilter PNG_FILE_FILTER	=
			new FilenameSuffixFilter("PNG files", PNG_FILENAME_EXTENSION);
	FilenameSuffixFilter XML_FILE_FILTER	=
			new FilenameSuffixFilter("XML files", XML_FILENAME_EXTENSION);

}

//----------------------------------------------------------------------
