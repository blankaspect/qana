/*====================================================================*\

AppConstants.java

Application constants interface.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Insets;

import java.text.DecimalFormat;

//----------------------------------------------------------------------


// APPLICATION CONSTANTS INTERFACE


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
	String	EXE_FILENAME_EXTENSION		= ".exe";
	String	GIF_FILENAME_EXTENSION		= ".gif";
	String	ICON_FILENAME_EXTENSION		= ".ico";
	String	JAR_FILENAME_EXTENSION		= ".jar";
	String	JPEG_FILENAME_EXTENSION1	= ".jpg";
	String	JPEG_FILENAME_EXTENSION2	= ".jpeg";
	String	KEY_FILENAME_EXTENSION		= ".keys";
	String	PNG_FILENAME_EXTENSION		= ".png";
	String	XML_FILENAME_EXTENSION		= ".xml";

	// File-filter descriptions
	String	BMP_FILES_STR	= "Bitmap files";
	String	EXE_FILES_STR	= "Windows executable files";
	String	ICON_FILES_STR	= "Windows icon files";
	String	GIF_FILES_STR	= "GIF files";
	String	JAR_FILES_STR	= "JAR files";
	String	JPEG_FILES_STR	= "JPEG files";
	String	KEY_FILES_STR	= QanaApp.SHORT_NAME + " key database files";
	String	PNG_FILES_STR	= "PNG files";
	String	XML_FILES_STR	= "XML files";

}

//----------------------------------------------------------------------
