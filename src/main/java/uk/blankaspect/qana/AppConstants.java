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

	// Temporary-file prefix
	String	TEMP_FILE_PREFIX	= "_$_";

	// Filename suffixes
	String	BMP_FILE_SUFFIX		= ".bmp";
	String	EXE_FILE_SUFFIX		= ".exe";
	String	GIF_FILE_SUFFIX		= ".gif";
	String	ICON_FILE_SUFFIX	= ".ico";
	String	JAR_FILE_SUFFIX		= ".jar";
	String	JPEG_FILE_SUFFIX1	= ".jpg";
	String	JPEG_FILE_SUFFIX2	= ".jpeg";
	String	KEY_FILE_SUFFIX		= ".keys";
	String	PNG_FILE_SUFFIX		= ".png";
	String	XML_FILE_SUFFIX		= ".xml";

	// File-filter descriptions
	String	BMP_FILES_STR	= "Bitmap files";
	String	EXE_FILES_STR	= "Windows executable files";
	String	ICON_FILES_STR	= "Windows icon files";
	String	GIF_FILES_STR	= "GIF files";
	String	JAR_FILES_STR	= "JAR files";
	String	JPEG_FILES_STR	= "JPEG files";
	String	KEY_FILES_STR	= App.SHORT_NAME + " key database files";
	String	PNG_FILES_STR	= "PNG files";
	String	XML_FILES_STR	= "XML files";

}

//----------------------------------------------------------------------
