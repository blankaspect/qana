/*====================================================================*\

AppConfig.java

Application configuration class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JFileChooser;
import javax.swing.UIManager;

import uk.blankaspect.common.crypto.EntropyAccumulator;
import uk.blankaspect.common.crypto.FortunaCipher;
import uk.blankaspect.common.crypto.StreamEncrypter;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.common.gui.ByteUnitIntegerSpinnerPanel;
import uk.blankaspect.common.gui.Colours;
import uk.blankaspect.common.gui.FontEx;
import uk.blankaspect.common.gui.IProgressView;
import uk.blankaspect.common.gui.TextRendering;

import uk.blankaspect.common.misc.FilenameSuffixFilter;
import uk.blankaspect.common.misc.IntegerRange;
import uk.blankaspect.common.misc.NumberUtils;
import uk.blankaspect.common.misc.Property;
import uk.blankaspect.common.misc.PropertySet;
import uk.blankaspect.common.misc.PropertyString;
import uk.blankaspect.common.misc.StringUtils;

import uk.blankaspect.common.regex.RegexUtils;

//----------------------------------------------------------------------


// APPLICATION CONFIGURATION CLASS


class AppConfig
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		AppConfig	INSTANCE;

	public static final		int	ENTROPY_SOURCE_MIN_NUM_BITS	= 0;
	public static final		int	ENTROPY_SOURCE_MAX_NUM_BITS	= 4;

	private static final	int	VERSION					= 0;
	private static final	int	MIN_SUPPORTED_VERSION	= 0;
	private static final	int	MAX_SUPPORTED_VERSION	= 0;

	private static final	String	CONFIG_ERROR_STR	= "Configuration error";
	private static final	String	CONFIG_DIR_KEY		= Property.APP_PREFIX + "configDir";
	private static final	String	PROPERTIES_FILENAME	= App.NAME_KEY + "-properties" +
																			AppConstants.XML_FILE_SUFFIX;
	private static final	String	FILENAME_BASE		= App.NAME_KEY + "-config";
	private static final	String	CONFIG_FILENAME		= FILENAME_BASE + AppConstants.XML_FILE_SUFFIX;
	private static final	String	CONFIG_OLD_FILENAME	= FILENAME_BASE + "-old" +
																			AppConstants.XML_FILE_SUFFIX;

	private static final	String	SAVE_CONFIGURATION_FILE_STR	= "Save configuration file";
	private static final	String	WRITING_STR					= "Writing";

	private interface Key
	{
		String	APPEARANCE								= "appearance";
		String	ARCHIVE									= "archive";
		String	AUTO_USE_GLOBAL_KEY						= "autoUseGlobalKey";
		String	BACKGROUND								= "background";
		String	BIT_MASK								= "bitMask";
		String	CARRIER_IMAGE_KIND						= "carrierImageKind";
		String	CLEAR_CLIPBOARD_ON_EXIT					= "clearClipboardOnExit";
		String	COLOUR									= "colour";
		String	COLUMN_WIDTH							= "columnWidth";
		String	CONFIGURATION							= App.NAME_KEY + "Configuration";
		String	CRYPTO									= "crypto";
		String	DEFAULT_LINE_LENGTH						= "defaultLineLength";
		String	ENCRYPTED_FILE_DRAG_AND_DROP_ACTION		= "encryptedFileDragAndDropAction";
		String	END_OF_SENTENCE_PATTERN					= "endOfSentencePattern";
		String	ENTROPY									= "entropy";
		String	ERASURE_NUM_PASSES						= "erasureNumPasses";
		String	FILE									= "file";
		String	FILE_PART_LENGTH_LOWER_BOUND			= "filePartLengthLowerBound";
		String	FILE_PART_LENGTH_UPPER_BOUND			= "filePartLengthUpperBound";
		String	FILENAME_SUFFIX							= "filenameSuffix";
		String	FONT									= "font";
		String	GENERAL									= "general";
		String	INTERVAL								= "interval";
		String	KDF_PARAMETERS							= "kdfParameters";
		String	KEY_DATABASE							= "keyDatabase";
		String	LOOK_AND_FEEL							= "lookAndFeel";
		String	MAIN_WINDOW_LOCATION					= "mainWindowLocation";
		String	MAIN_WINDOW_SIZE						= "mainWindowSize";
		String	MAX_EDIT_LIST_LENGTH					= "maxEditListLength";
		String	NUM_ROWS								= "numRows";
		String	NUM_SPACES_BETWEEN_SENTENCES			= "numSpacesBetweenSentences";
		String	PATH									= "path";
		String	PRNG_DEFAULT_CIPHER						= "prngDefaultCipher";
		String	SAVE_FILE_SELECTION_PATHNAMES			= "saveFileSelectionPathnames";
		String	SEED_FILE_DIRECTORY						= "seedFileDirectory";
		String	SELECT_ENCRYPT_DECRYPT_OUTPUT_FILE		= "selectEncryptDecryptOutputFile";
		String	SELECT_TEXT_ON_FOCUS_GAINED				= "selectTextOnFocusGained";
		String	SELECTION_BACKGROUND					= "selectionBackground";
		String	SELECTION_TEXT							= "selectionText";
		String	SHOW_FULL_PATHNAMES						= "showFullPathnames";
		String	SHOW_UNIX_PATHNAMES						= "showUnixPathnames";
		String	SIZE									= "size";
		String	SOURCE									= "source";
		String	SPLIT									= "split";
		String	STATUS_TEXT_COLOUR						= "statusTextColour";
		String	TEXT									= "text";
		String	TEXT_ANTIALIASING						= "textAntialiasing";
		String	TIMER_DIVISOR							= "timerDivisor";
		String	VIEW									= "view";
		String	WARN_NOT_SEEDED							= "warnNotSeeded";
		String	WARN_TEMPORARY_KEY						= "warnTemporaryKey";
		String	WRAP									= "wrap";
		String	WRAP_CIPHERTEXT_IN_XML					= "wrapCiphertextInXml";
	}

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

		ERROR_READING_PROPERTIES_FILE
		("An error occurred when reading the properties file."),

		NO_CONFIGURATION_FILE
		("No configuration file was found at the specified location."),

		NO_VERSION_NUMBER
		("The configuration file does not have a version number."),

		INVALID_VERSION_NUMBER
		("The version number of the configuration file is invalid."),

		UNSUPPORTED_CONFIGURATION_FILE
		("The version of the configuration file (%1) is not supported by this version of " +
			App.SHORT_NAME + "."),

		FAILED_TO_CREATE_DIRECTORY
		("Failed to create the directory for the configuration file."),

		MALFORMED_PATTERN
		("The pattern is not a well-formed regular expression.\n(%1)");

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
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CONFIGURATION FILE CLASS


	private static class ConfigFile
		extends PropertySet
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	CONFIG_FILE1_STR	= "configuration file";
		private static final	String	CONFIG_FILE2_STR	= "Configuration file";

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ConfigFile()
		{
		}

		//--------------------------------------------------------------

		private ConfigFile(String versionStr)
			throws AppException
		{
			super(Key.CONFIGURATION, null, versionStr);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public String getSourceName()
		{
			return CONFIG_FILE2_STR;
		}

		//--------------------------------------------------------------

		@Override
		protected String getFileKindString()
		{
			return CONFIG_FILE1_STR;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public void read(File file)
			throws AppException
		{
			// Read file
			read(file, Key.CONFIGURATION);

			// Test version number
			String versionStr = getVersionString();
			if (versionStr == null)
				throw new FileException(ErrorId.NO_VERSION_NUMBER, file);
			try
			{
				int version = Integer.parseInt(versionStr);
				if ((version < MIN_SUPPORTED_VERSION) || (version > MAX_SUPPORTED_VERSION))
					throw new FileException(ErrorId.UNSUPPORTED_CONFIGURATION_FILE, file, versionStr);
			}
			catch (NumberFormatException e)
			{
				throw new FileException(ErrorId.INVALID_VERSION_NUMBER, file);
			}
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// PROPERTY CLASS: SHOW UNIX PATHNAMES


	private class CPShowUnixPathnames
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPShowUnixPathnames()
		{
			super(concatenateKeys(Key.GENERAL, Key.SHOW_UNIX_PATHNAMES));
			value = Boolean.FALSE;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isShowUnixPathnames()
	{
		return cpShowUnixPathnames.getValue();
	}

	//------------------------------------------------------------------

	public void setShowUnixPathnames(boolean value)
	{
		cpShowUnixPathnames.setValue(value);
	}

	//------------------------------------------------------------------

	public void addShowUnixPathnamesObserver(Property.IObserver observer)
	{
		cpShowUnixPathnames.addObserver(observer);
	}

	//------------------------------------------------------------------

	public void removeShowUnixPathnamesObserver(Property.IObserver observer)
	{
		cpShowUnixPathnames.removeObserver(observer);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPShowUnixPathnames	cpShowUnixPathnames	= new CPShowUnixPathnames();

	//==================================================================


	// PROPERTY CLASS: SELECT TEXT ON FOCUS GAINED


	private class CPSelectTextOnFocusGained
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPSelectTextOnFocusGained()
		{
			super(concatenateKeys(Key.GENERAL, Key.SELECT_TEXT_ON_FOCUS_GAINED));
			value = Boolean.TRUE;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isSelectTextOnFocusGained()
	{
		return cpSelectTextOnFocusGained.getValue();
	}

	//------------------------------------------------------------------

	public void setSelectTextOnFocusGained(boolean value)
	{
		cpSelectTextOnFocusGained.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPSelectTextOnFocusGained	cpSelectTextOnFocusGained	= new CPSelectTextOnFocusGained();

	//==================================================================


	// PROPERTY CLASS: SHOW FULL PATHNAMES


	private class CPShowFullPathnames
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPShowFullPathnames()
		{
			super(concatenateKeys(Key.GENERAL, Key.SHOW_FULL_PATHNAMES));
			value = Boolean.FALSE;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isShowFullPathnames()
	{
		return cpShowFullPathnames.getValue();
	}

	//------------------------------------------------------------------

	public void setShowFullPathnames(boolean value)
	{
		cpShowFullPathnames.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPShowFullPathnames	cpShowFullPathnames	= new CPShowFullPathnames();

	//==================================================================


	// PROPERTY CLASS: MAIN WINDOW LOCATION


	private class CPMainWindowLocation
		extends Property.SimpleProperty<Point>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPMainWindowLocation()
		{
			super(concatenateKeys(Key.GENERAL, Key.MAIN_WINDOW_LOCATION));
			value = new Point();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input input)
			throws AppException
		{
			if (input.getValue().isEmpty())
				value = null;
			else
			{
				int[] outValues = input.parseIntegers(2, null);
				value = new Point(outValues[0], outValues[1]);
			}
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return ((value == null) ? "" : value.x + ", " + value.y);
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isMainWindowLocation()
	{
		return (getMainWindowLocation() != null);
	}

	//------------------------------------------------------------------

	public Point getMainWindowLocation()
	{
		return cpMainWindowLocation.getValue();
	}

	//------------------------------------------------------------------

	public void setMainWindowLocation(Point value)
	{
		cpMainWindowLocation.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPMainWindowLocation	cpMainWindowLocation	= new CPMainWindowLocation();

	//==================================================================


	// PROPERTY CLASS: MAIN WINDOW SIZE


	private class CPMainWindowSize
		extends Property.SimpleProperty<Dimension>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPMainWindowSize()
		{
			super(concatenateKeys(Key.GENERAL, Key.MAIN_WINDOW_SIZE));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input input)
			throws AppException
		{
			if (input.getValue().isEmpty())
				value = null;
			else
			{
				int[] outValues = input.parseIntegers(2, null);
				value = new Dimension(outValues[0], outValues[1]);
			}
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return ((value == null) ? "" : value.width + ", " + value.height);
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isMainWindowSize()
	{
		return (getMainWindowSize() != null);
	}

	//------------------------------------------------------------------

	public Dimension getMainWindowSize()
	{
		return cpMainWindowSize.getValue();
	}

	//------------------------------------------------------------------

	public void setMainWindowSize(Dimension value)
	{
		cpMainWindowSize.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPMainWindowSize	cpMainWindowSize	= new CPMainWindowSize();

	//==================================================================


	// PROPERTY CLASS: MAXIMUM EDIT LIST LENGTH


	private class CPMaxEditListLength
		extends Property.IntegerProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPMaxEditListLength()
		{
			super(concatenateKeys(Key.GENERAL, Key.MAX_EDIT_LIST_LENGTH),
				  TextDocument.MIN_MAX_EDIT_LIST_LENGTH, TextDocument.MAX_MAX_EDIT_LIST_LENGTH);
			value = TextDocument.DEFAULT_MAX_EDIT_LIST_LENGTH;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public int getMaxEditListLength()
	{
		return cpMaxEditListLength.getValue();
	}

	//------------------------------------------------------------------

	public void setMaxEditListLength(int value)
	{
		cpMaxEditListLength.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPMaxEditListLength	cpMaxEditListLength	= new CPMaxEditListLength();

	//==================================================================


	// PROPERTY CLASS: CLEAR CLIPBOARD ON EXIT


	private class CPClearClipboardOnExit
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPClearClipboardOnExit()
		{
			super(concatenateKeys(Key.GENERAL, Key.CLEAR_CLIPBOARD_ON_EXIT));
			value = Boolean.TRUE;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isClearClipboardOnExit()
	{
		return cpClearClipboardOnExit.getValue();
	}

	//------------------------------------------------------------------

	public void setClearClipboardOnExit(boolean value)
	{
		cpClearClipboardOnExit.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPClearClipboardOnExit	cpClearClipboardOnExit	= new CPClearClipboardOnExit();

	//==================================================================


	// PROPERTY CLASS: ENCRYPTED FILE DRAG-AND-DROP ACTION


	private class CPEncryptedFileDragAndDropAction
		extends Property.EnumProperty<EncryptedFileImportKind>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPEncryptedFileDragAndDropAction()
		{
			super(concatenateKeys(Key.GENERAL, Key.ENCRYPTED_FILE_DRAG_AND_DROP_ACTION),
				  EncryptedFileImportKind.class);
			value = EncryptedFileImportKind.DECRYPT;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public EncryptedFileImportKind getEncryptedFileDragAndDropAction()
	{
		return cpEncryptedFileDragAndDropAction.getValue();
	}

	//------------------------------------------------------------------

	public void setEncryptedFileDragAndDropAction(EncryptedFileImportKind value)
	{
		cpEncryptedFileDragAndDropAction.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPEncryptedFileDragAndDropAction	cpEncryptedFileDragAndDropAction	=
																	new CPEncryptedFileDragAndDropAction();

	//==================================================================


	// PROPERTY CLASS: AUTOGENERATED CARRIER IMAGE KIND


	private class CPCarrierImageKind
		extends Property.EnumProperty<CarrierImage.Kind>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPCarrierImageKind()
		{
			super(concatenateKeys(Key.GENERAL, Key.CARRIER_IMAGE_KIND), CarrierImage.Kind.class);
			value = CarrierImage.Kind.LINEAR;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public CarrierImage.Kind getCarrierImageKind()
	{
		return cpCarrierImageKind.getValue();
	}

	//------------------------------------------------------------------

	public void setCarrierImageKind(CarrierImage.Kind value)
	{
		cpCarrierImageKind.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPCarrierImageKind	cpCarrierImageKind	= new CPCarrierImageKind();

	//==================================================================


	// PROPERTY CLASS: LOOK-AND-FEEL


	private class CPLookAndFeel
		extends Property.StringProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPLookAndFeel()
		{
			super(concatenateKeys(Key.APPEARANCE, Key.LOOK_AND_FEEL));
			value = "";
			for (UIManager.LookAndFeelInfo lookAndFeelInfo : UIManager.getInstalledLookAndFeels())
			{
				if (lookAndFeelInfo.getClassName().
											equals(UIManager.getCrossPlatformLookAndFeelClassName()))
				{
					value = lookAndFeelInfo.getName();
					break;
				}
			}
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public String getLookAndFeel()
	{
		return cpLookAndFeel.getValue();
	}

	//------------------------------------------------------------------

	public void setLookAndFeel(String value)
	{
		cpLookAndFeel.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPLookAndFeel	cpLookAndFeel	= new CPLookAndFeel();

	//==================================================================


	// PROPERTY CLASS: TEXT ANTIALIASING


	private class CPTextAntialiasing
		extends Property.EnumProperty<TextRendering.Antialiasing>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPTextAntialiasing()
		{
			super(concatenateKeys(Key.APPEARANCE, Key.TEXT_ANTIALIASING),
				  TextRendering.Antialiasing.class);
			value = TextRendering.Antialiasing.DEFAULT;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public TextRendering.Antialiasing getTextAntialiasing()
	{
		return cpTextAntialiasing.getValue();
	}

	//------------------------------------------------------------------

	public void setTextAntialiasing(TextRendering.Antialiasing value)
	{
		cpTextAntialiasing.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPTextAntialiasing	cpTextAntialiasing	= new CPTextAntialiasing();

	//==================================================================


	// PROPERTY CLASS: STATUS TEXT COLOUR


	private class CPStatusTextColour
		extends Property.ColourProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPStatusTextColour()
		{
			super(concatenateKeys(Key.APPEARANCE, Key.STATUS_TEXT_COLOUR));
			value = StatusPanel.DEFAULT_TEXT_COLOUR;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public Color getStatusTextColour()
	{
		return cpStatusTextColour.getValue();
	}

	//------------------------------------------------------------------

	public void setStatusTextColour(Color value)
	{
		cpStatusTextColour.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPStatusTextColour	cpStatusTextColour	= new CPStatusTextColour();

	//==================================================================


	// PROPERTY CLASS: SELECT ENCRYPT/DECRYPT OUTPUT FILE


	private class CPSelectEncryptDecryptOutputFile
		extends Property.PropertyMap<ActionSource, Boolean>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPSelectEncryptDecryptOutputFile()
		{
			super(concatenateKeys(Key.FILE, Key.SELECT_ENCRYPT_DECRYPT_OUTPUT_FILE),
				  ActionSource.class);
			for (ActionSource actionSource : ActionSource.values())
				values.put(actionSource, actionSource.getDefaultSelection());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input        input,
						  ActionSource actionSource)
			throws AppException
		{
			values.put(actionSource, input.parseBoolean());
		}

		//--------------------------------------------------------------

		@Override
		public String toString(ActionSource actionSource)
		{
			return getValue(actionSource).toString();
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isSelectEncryptDecryptOutputFile(ActionSource key)
	{
		return cpSelectEncryptDecryptOutputFile.getValue(key);
	}

	//------------------------------------------------------------------

	public void setSelectEncryptDecryptOutputFile(ActionSource key,
												  boolean      value)
	{
		cpSelectEncryptDecryptOutputFile.setValue(key, value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPSelectEncryptDecryptOutputFile	cpSelectEncryptDecryptOutputFile	=
																	new CPSelectEncryptDecryptOutputFile();

	//==================================================================


	// PROPERTY CLASS: NUMBER OF PASSES WHEN ERASING FILE


	private class CPFileErasureNumPasses
		extends Property.IntegerProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPFileErasureNumPasses()
		{
			super(concatenateKeys(Key.FILE, Key.ERASURE_NUM_PASSES),
				  FileEraser.MIN_NUM_PASSES, FileEraser.MAX_NUM_PASSES);
			value = FileEraser.DEFAULT_NUM_PASSES;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public int getFileErasureNumPasses()
	{
		return cpFileErasureNumPasses.getValue();
	}

	//------------------------------------------------------------------

	public void setFileErasureNumPasses(int value)
	{
		cpFileErasureNumPasses.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPFileErasureNumPasses	cpFileErasureNumPasses	= new CPFileErasureNumPasses();

	//==================================================================


	// PROPERTY CLASS: SAVE FILE-SELECTION PATHNAMES


	private class CPSaveFileSelectionPathnames
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPSaveFileSelectionPathnames()
		{
			super(concatenateKeys(Key.FILE, Key.SAVE_FILE_SELECTION_PATHNAMES));
			value = Boolean.FALSE;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isSaveFileSelectionPathnames()
	{
		return cpSaveFileSelectionPathnames.getValue();
	}

	//------------------------------------------------------------------

	public void setSaveFileSelectionPathnames(boolean value)
	{
		cpSaveFileSelectionPathnames.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPSaveFileSelectionPathnames	cpSaveFileSelectionPathnames	=
																		new CPSaveFileSelectionPathnames();

	//==================================================================


	// PROPERTY CLASS: FILE-SELECTION PATHNAMES


	private class CPFileSelectionPathname
		extends Property.PropertyMap<FileSelectionKind, String>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPFileSelectionPathname()
		{
			super(Key.PATH, FileSelectionKind.class);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input             input,
						  FileSelectionKind fileSelectionKind)
			throws AppException
		{
			values.put(fileSelectionKind, input.getValue());
		}

		//--------------------------------------------------------------

		@Override
		public String toString(FileSelectionKind fileSelectionKind)
		{
			return getValue(fileSelectionKind);
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public String getFileSelectionPathname(FileSelectionKind key)
	{
		return cpFileSelectionPathname.getValue(key);
	}

	//------------------------------------------------------------------

	public File getFileSelectionDirectory(FileSelectionKind key)
	{
		return new File(PropertyString.parsePathname(getFileSelectionPathname(key)));
	}

	//------------------------------------------------------------------

	public void setFileSelectionPathname(FileSelectionKind key,
										 String            value)
	{
		cpFileSelectionPathname.setValue(key, value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPFileSelectionPathname	cpFileSelectionPathname	= new CPFileSelectionPathname();

	//==================================================================


	// PROPERTY CLASS: FILENAME SUFFIXES


	private class CPFilenameSuffix
		extends Property.PropertyMap<FileKind, String>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPFilenameSuffix()
		{
			super(concatenateKeys(Key.FILE, Key.FILENAME_SUFFIX), FileKind.class);
			for (FileKind fileKind : FileKind.values())
				values.put(fileKind, fileKind.getDefaultFilenameSuffix());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input    input,
						  FileKind fileKind)
			throws AppException
		{
			values.put(fileKind, input.getValue());
		}

		//--------------------------------------------------------------

		@Override
		public String toString(FileKind fileKind)
		{
			return getValue(fileKind);
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public String getFilenameSuffix(FileKind key)
	{
		return cpFilenameSuffix.getValue(key);
	}

	//------------------------------------------------------------------

	public void setFilenameSuffix(FileKind key,
								  String   value)
	{
		cpFilenameSuffix.setValue(key, value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPFilenameSuffix	cpFilenameSuffix	= new CPFilenameSuffix();

	//==================================================================


	// PROPERTY CLASS: ARCHIVE VIEW NUMBER OF ROWS


	private class CPArchiveViewNumRows
		extends Property.IntegerProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPArchiveViewNumRows()
		{
			super(concatenateKeys(Key.ARCHIVE, Key.VIEW, Key.NUM_ROWS),
				  ArchiveView.MIN_NUM_ROWS, ArchiveView.MAX_NUM_ROWS);
			value = ArchiveView.DEFAULT_NUM_ROWS;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public int getArchiveViewNumRows()
	{
		return cpArchiveViewNumRows.getValue();
	}

	//------------------------------------------------------------------

	public void setArchiveViewNumRows(int value)
	{
		cpArchiveViewNumRows.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPArchiveViewNumRows	cpArchiveViewNumRows	= new CPArchiveViewNumRows();

	//==================================================================


	// PROPERTY CLASS: ARCHIVE VIEW COLUMN WIDTHS


	private class CPArchiveViewColumnWidth
		extends Property.PropertyMap<ArchiveView.Column, Integer>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPArchiveViewColumnWidth()
		{
			super(concatenateKeys(Key.ARCHIVE, Key.VIEW, Key.COLUMN_WIDTH), ArchiveView.Column.class);
			for (ArchiveView.Column column : ArchiveView.Column.values())
				values.put(column, column.getDefaultWidth());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input              input,
						  ArchiveView.Column key)
			throws AppException
		{
			if (input.getValue().isEmpty())
				values.put(key, 0);
			else
			{
				IntegerRange range = new IntegerRange(ArchiveView.MIN_COLUMN_WIDTH,
													  ArchiveView.MAX_COLUMN_WIDTH);
				values.put(key, input.parseInteger(range));
			}
		}

		//--------------------------------------------------------------

		@Override
		public String toString(ArchiveView.Column key)
		{
			int value = getValue(key);
			return ((value == 0) ? "" : Integer.toString(value));
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public int getArchiveViewColumnWidth(ArchiveView.Column key)
	{
		return cpArchiveViewColumnWidth.getValue(key);
	}

	//------------------------------------------------------------------

	public void setArchiveViewColumnWidth(ArchiveView.Column key,
										  int                value)
	{
		cpArchiveViewColumnWidth.setValue(key, value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPArchiveViewColumnWidth	cpArchiveViewColumnWidth	= new CPArchiveViewColumnWidth();

	//==================================================================


	// PROPERTY CLASS: TEXT VIEW SIZE


	private class CPTextViewSize
		extends Property.SimpleProperty<Dimension>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPTextViewSize()
		{
			super(concatenateKeys(Key.TEXT, Key.VIEW, Key.SIZE));
			value = new Dimension(TextView.DEFAULT_NUM_COLUMNS, TextView.DEFAULT_NUM_ROWS);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input input)
			throws AppException
		{
			IntegerRange[] ranges =
			{
				new IntegerRange(TextView.MIN_NUM_COLUMNS, TextView.MAX_NUM_COLUMNS),
				new IntegerRange(TextView.MIN_NUM_ROWS, TextView.MAX_NUM_ROWS)
			};
			int[] outValues = input.parseIntegers(2, ranges);
			value = new Dimension(outValues[0], outValues[1]);
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return (value.width + ", " + value.height);
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public Dimension getTextViewSize()
	{
		return cpTextViewSize.getValue();
	}

	//------------------------------------------------------------------

	public void setTextViewSize(Dimension value)
	{
		cpTextViewSize.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPTextViewSize	cpTextViewSize	= new CPTextViewSize();

	//==================================================================


	// PROPERTY CLASS: TEXT VIEW TEXT COLOUR


	private class CPTextViewTextColour
		extends Property.ColourProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPTextViewTextColour()
		{
			super(concatenateKeys(Key.TEXT, Key.VIEW, Key.COLOUR, Key.TEXT));
			value = Colours.FOREGROUND;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public Color getTextViewTextColour()
	{
		return cpTextViewTextColour.getValue();
	}

	//------------------------------------------------------------------

	public void setTextViewTextColour(Color value)
	{
		cpTextViewTextColour.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPTextViewTextColour	cpTextViewTextColour	= new CPTextViewTextColour();

	//==================================================================


	// PROPERTY CLASS: TEXT VIEW BACKGROUND COLOUR


	private class CPTextViewBackgroundColour
		extends Property.ColourProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPTextViewBackgroundColour()
		{
			super(concatenateKeys(Key.TEXT, Key.VIEW, Key.COLOUR, Key.BACKGROUND));
			value = Colours.BACKGROUND;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public Color getTextViewBackgroundColour()
	{
		return cpTextViewBackgroundColour.getValue();
	}

	//------------------------------------------------------------------

	public void setTextViewBackgroundColour(Color value)
	{
		cpTextViewBackgroundColour.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPTextViewBackgroundColour	cpTextViewBackgroundColour	= new CPTextViewBackgroundColour();

	//==================================================================


	// PROPERTY CLASS: TEXT VIEW SELECTION TEXT COLOUR


	private class CPTextViewSelectionTextColour
		extends Property.ColourProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPTextViewSelectionTextColour()
		{
			super(concatenateKeys(Key.TEXT, Key.VIEW, Key.COLOUR, Key.SELECTION_TEXT));
			value = Colours.FOCUSED_SELECTION_FOREGROUND;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public Color getTextViewSelectionTextColour()
	{
		return cpTextViewSelectionTextColour.getValue();
	}

	//------------------------------------------------------------------

	public void setTextViewSelectionTextColour(Color value)
	{
		cpTextViewSelectionTextColour.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPTextViewSelectionTextColour	cpTextViewSelectionTextColour	=
																	new CPTextViewSelectionTextColour();

	//==================================================================


	// PROPERTY CLASS: TEXT VIEW SELECTION BACKGROUND COLOUR


	private class CPTextViewSelectionBackgroundColour
		extends Property.ColourProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPTextViewSelectionBackgroundColour()
		{
			super(concatenateKeys(Key.TEXT, Key.VIEW, Key.COLOUR, Key.SELECTION_BACKGROUND));
			value = Colours.FOCUSED_SELECTION_BACKGROUND;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public Color getTextViewSelectionBackgroundColour()
	{
		return cpTextViewSelectionBackgroundColour.getValue();
	}

	//------------------------------------------------------------------

	public void setTextViewSelectionBackgroundColour(Color value)
	{
		cpTextViewSelectionBackgroundColour.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPTextViewSelectionBackgroundColour	cpTextViewSelectionBackgroundColour	=
																new CPTextViewSelectionBackgroundColour();

	//==================================================================


	// PROPERTY CLASS: TEXT WRAP, DEFAULT LINE LENGTH


	private class CPTextWrapDefaultLineLength
		extends Property.IntegerProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPTextWrapDefaultLineLength()
		{
			super(concatenateKeys(Key.TEXT, Key.WRAP, Key.DEFAULT_LINE_LENGTH),
				  TextDocument.MIN_WRAP_LINE_LENGTH, TextDocument.MAX_WRAP_LINE_LENGTH);
			value = TextDocument.DEFAULT_WRAP_LINE_LENGTH;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input input)
			throws AppException
		{
			if (input.getValue().isEmpty())
				value = null;
			else
				super.parse(input);
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return ((value == null) ? "" : value.toString());
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public Integer getTextWrapDefaultLineLength()
	{
		return cpTextWrapDefaultLineLength.getValue();
	}

	//------------------------------------------------------------------

	public void setTextWrapDefaultLineLength(Integer value)
	{
		cpTextWrapDefaultLineLength.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPTextWrapDefaultLineLength	cpTextWrapDefaultLineLength	= new CPTextWrapDefaultLineLength();

	//==================================================================


	// PROPERTY CLASS: TEXT WRAP, END-OF-SENTENCE PATTERN


	private class CPTextWrapEndOfSentencePattern
		extends Property.StringProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPTextWrapEndOfSentencePattern()
		{
			super(concatenateKeys(Key.TEXT, Key.WRAP, Key.END_OF_SENTENCE_PATTERN));
			value = TextDocument.DEFAULT_END_OF_SENTENCE_PATTERN;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input input)
		{
			try
			{
				String patternStr = input.getValue();
				if (!patternStr.isEmpty())
				{
					Pattern.compile(patternStr);
					Pattern.compile("(" + patternStr + ") +");
				}
				value = patternStr;
			}
			catch (PatternSyntaxException e)
			{
				InputException exception = new InputException(AppConfig.ErrorId.MALFORMED_PATTERN, input);
				exception.setSubstitutionStrings(RegexUtils.getExceptionMessage(e));
				showWarningMessage(exception);
			}
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public String getTextWrapEndOfSentencePattern()
	{
		return cpTextWrapEndOfSentencePattern.getValue();
	}

	//------------------------------------------------------------------

	public void setTextWrapEndOfSentencePattern(String value)
	{
		cpTextWrapEndOfSentencePattern.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPTextWrapEndOfSentencePattern	cpTextWrapEndOfSentencePattern	=
																	new CPTextWrapEndOfSentencePattern();

	//==================================================================


	// PROPERTY CLASS: TEXT WRAP, NUMBER OF SPACES BETWEEN SENTENCES


	private class CPTextWrapNumSpacesBetweenSentences
		extends Property.IntegerProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPTextWrapNumSpacesBetweenSentences()
		{
			super(concatenateKeys(Key.TEXT, Key.WRAP, Key.NUM_SPACES_BETWEEN_SENTENCES),
				  TextDocument.MIN_NUM_SPACES_BETWEEN_SENTENCES,
				  TextDocument.MAX_NUM_SPACES_BETWEEN_SENTENCES);
			value = TextDocument.DEFAULT_NUM_SPACES_BETWEEN_SENTENCES;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public int getTextWrapNumSpacesBetweenSentences()
	{
		return cpTextWrapNumSpacesBetweenSentences.getValue();
	}

	//------------------------------------------------------------------

	public void setTextWrapNumSpacesBetweenSentences(int value)
	{
		cpTextWrapNumSpacesBetweenSentences.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPTextWrapNumSpacesBetweenSentences	cpTextWrapNumSpacesBetweenSentences	=
																new CPTextWrapNumSpacesBetweenSentences();

	//==================================================================


	// PROPERTY CLASS: DEFAULT CIPHER OF PSEUDO-RANDOM NUMBER GENERATOR


	private class CPPrngDefaultCipher
		extends Property.EnumProperty<FortunaCipher>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPPrngDefaultCipher()
		{
			super(concatenateKeys(Key.CRYPTO, Key.PRNG_DEFAULT_CIPHER), FortunaCipher.class);
			value = FortunaCipher.AES256;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public FortunaCipher getPrngDefaultCipher()
	{
		return cpPrngDefaultCipher.getValue();
	}

	//------------------------------------------------------------------

	public void setPrngDefaultCipher(FortunaCipher value)
	{
		cpPrngDefaultCipher.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPPrngDefaultCipher	cpPrngDefaultCipher	= new CPPrngDefaultCipher();

	//==================================================================


	// PROPERTY CLASS: PATHNAME OF KEY DATABASE


	private class CPKeyDatabasePathname
		extends Property.StringProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	DEFAULT_FILENAME	= App.NAME_KEY + AppConstants.KEY_FILE_SUFFIX;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPKeyDatabasePathname()
		{
			super(concatenateKeys(Key.CRYPTO, Key.KEY_DATABASE));
			value = Utils.getPathname(new File(Utils.getPropertiesPathname(), DEFAULT_FILENAME), true);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input input)
		{
			value = input.getValue();
			if (value.isEmpty())
				value = null;
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return ((value == null) ? "" : value.toString());
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public String getKeyDatabasePathname()
	{
		return cpKeyDatabasePathname.getValue();
	}

	//------------------------------------------------------------------

	public File getKeyDatabaseFile()
	{
		String pathname = getKeyDatabasePathname();
		return (StringUtils.isNullOrEmpty(pathname)
												? null
												: new File(PropertyString.parsePathname(pathname)));
	}

	//------------------------------------------------------------------

	public void setKeyDatabasePathname(String value)
	{
		cpKeyDatabasePathname.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPKeyDatabasePathname	cpKeyDatabasePathname	= new CPKeyDatabasePathname();

	//==================================================================


	// PROPERTY CLASS: KEY DERIVATION FUNCTION PARAMETERS


	private class CPKdfParameters
		extends Property.PropertyMap<KdfUse, StreamEncrypter.KdfParams>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPKdfParameters()
		{
			super(concatenateKeys(Key.CRYPTO, Key.KDF_PARAMETERS), KdfUse.class);
			for (KdfUse kdfUse : KdfUse.values())
				values.put(kdfUse, KeyList.DEFAULT_KDF_PARAMS);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input  input,
						  KdfUse kdfUse)
			throws AppException
		{
			try
			{
				values.put(kdfUse, StreamEncrypter.KdfParams.parseParams(input.getValue()));
			}
			catch (IllegalArgumentException e)
			{
				showWarningMessage(new IllegalValueException(input));
			}
			catch (uk.blankaspect.common.exception.ValueOutOfBoundsException e)
			{
				showWarningMessage(new ValueOutOfBoundsException(input));
			}
		}

		//--------------------------------------------------------------

		@Override
		public String toString(KdfUse kdfUse)
		{
			return getValue(kdfUse).toString();
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public StreamEncrypter.KdfParams getKdfParameters(KdfUse key)
	{
		return cpKdfParameters.getValue(key);
	}

	//------------------------------------------------------------------

	public void setKdfParameters(KdfUse                    key,
								 StreamEncrypter.KdfParams value)
	{
		cpKdfParameters.setValue(key, value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPKdfParameters	cpKdfParameters	= new CPKdfParameters();

	//==================================================================


	// PROPERTY CLASS: AUTOMATICALLY USE GLOBAL KEY


	private class CPAutoUseGlobalKey
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPAutoUseGlobalKey()
		{
			super(concatenateKeys(Key.CRYPTO, Key.AUTO_USE_GLOBAL_KEY));
			value = Boolean.FALSE;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isAutoUseGlobalKey()
	{
		return cpAutoUseGlobalKey.getValue();
	}

	//------------------------------------------------------------------

	public void setAutoUseGlobalKey(boolean value)
	{
		cpAutoUseGlobalKey.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPAutoUseGlobalKey	cpAutoUseGlobalKey	= new CPAutoUseGlobalKey();

	//==================================================================


	// PROPERTY CLASS: WARN OF USE OF TEMPORARY KEY


	private class CPWarnTemporaryKey
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPWarnTemporaryKey()
		{
			super(concatenateKeys(Key.CRYPTO, Key.WARN_TEMPORARY_KEY));
			value = Boolean.TRUE;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isWarnTemporaryKey()
	{
		return cpWarnTemporaryKey.getValue();
	}

	//------------------------------------------------------------------

	public void setWarnTemporaryKey(boolean value)
	{
		cpWarnTemporaryKey.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPWarnTemporaryKey	cpWarnTemporaryKey	= new CPWarnTemporaryKey();

	//==================================================================


	// PROPERTY CLASS: WRAP CIPHERTEXT IN XML


	private class CPWrapCiphertextInXml
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPWrapCiphertextInXml()
		{
			super(concatenateKeys(Key.CRYPTO, Key.WRAP_CIPHERTEXT_IN_XML));
			value = Boolean.TRUE;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isWrapCiphertextInXml()
	{
		return cpWrapCiphertextInXml.getValue();
	}

	//------------------------------------------------------------------

	public void setWrapCiphertextInXml(boolean value)
	{
		cpWrapCiphertextInXml.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPWrapCiphertextInXml	cpWrapCiphertextInXml	= new CPWrapCiphertextInXml();

	//==================================================================


	// PROPERTY CLASS: PATHNAME OF DIRECTORY OF SEED FILE


	private class CPSeedFileDirectoryPathname
		extends Property.StringProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPSeedFileDirectoryPathname()
		{
			super(concatenateKeys(Key.ENTROPY, Key.SEED_FILE_DIRECTORY));
			value = Utils.getPathname(new File(Utils.getPropertiesPathname()), true);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input input)
		{
			value = input.getValue();
			if (value.isEmpty())
				value = null;
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return ((value == null) ? "" : value.toString());
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public String getSeedFileDirectoryPathname()
	{
		return cpSeedFileDirectoryPathname.getValue();
	}

	//------------------------------------------------------------------

	public File getSeedFileDirectory()
	{
		String pathname = getSeedFileDirectoryPathname();
		return (StringUtils.isNullOrEmpty(pathname)
												? null
												: new File(PropertyString.parsePathname(pathname)));
	}

	//------------------------------------------------------------------

	public void setSeedFileDirectoryPathname(String value)
	{
		cpSeedFileDirectoryPathname.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPSeedFileDirectoryPathname	cpSeedFileDirectoryPathname	= new CPSeedFileDirectoryPathname();

	//==================================================================


	// PROPERTY CLASS: WARN IF PRNG IS NOT SEEDED


	private class CPWarnNotSeeded
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPWarnNotSeeded()
		{
			super(concatenateKeys(Key.ENTROPY, Key.WARN_NOT_SEEDED));
			value = Boolean.TRUE;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public boolean isWarnNotSeeded()
	{
		return cpWarnNotSeeded.getValue();
	}

	//------------------------------------------------------------------

	public void setWarnNotSeeded(boolean value)
	{
		cpWarnNotSeeded.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPWarnNotSeeded	cpWarnNotSeeded	= new CPWarnNotSeeded();

	//==================================================================


	// PROPERTY CLASS: ENTROPY HIGH-RESOLUTION TIMER DIVISOR


	private class CPEntropyTimerDivisor
		extends Property.IntegerProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	DEFAULT_TIMER_DIVISOR	= EntropyAccumulator.MIN_TIMER_DIVISOR;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPEntropyTimerDivisor()
		{
			super(concatenateKeys(Key.ENTROPY, Key.TIMER_DIVISOR),
				  EntropyAccumulator.MIN_TIMER_DIVISOR, EntropyAccumulator.MAX_TIMER_DIVISOR);
			value = DEFAULT_TIMER_DIVISOR;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public int getEntropyTimerDivisor()
	{
		return cpEntropyTimerDivisor.getValue();
	}

	//------------------------------------------------------------------

	public void setEntropyTimerDivisor(int value)
	{
		cpEntropyTimerDivisor.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPEntropyTimerDivisor	cpEntropyTimerDivisor	= new CPEntropyTimerDivisor();

	//==================================================================


	// PROPERTY CLASS: ENTROPY SOURCE, BIT MASK


	private class CPEntropySourceBitMask
		extends Property.PropertyMap<EntropyAccumulator.SourceKind, Integer>
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	MIN_VALUE	= 0;
		private static final	int	MAX_VALUE	=
												(1 << EntropyAccumulator.SourceParams.BIT_MASK_LENGTH) - 1;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPEntropySourceBitMask()
		{
			super(concatenateKeys(Key.ENTROPY, Key.SOURCE, Key.BIT_MASK),
				  EntropyAccumulator.SourceKind.class);
			for (EntropyAccumulator.SourceKind sourceKind : EntropyAccumulator.SourceKind.values())
				values.put(sourceKind, EntropyAccumulator.SourceParams.DEFAULT_BIT_MASK);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input                         input,
						  EntropyAccumulator.SourceKind sourceKind)
			throws AppException
		{
			IntegerRange range = new IntegerRange(MIN_VALUE, MAX_VALUE);
			int bitMask = input.parseInteger(range, 2);
			if (Integer.bitCount(bitMask) > ENTROPY_SOURCE_MAX_NUM_BITS)
				throw new IllegalValueException(input);
			values.put(sourceKind, bitMask);
		}

		//--------------------------------------------------------------

		@Override
		public String toString(EntropyAccumulator.SourceKind sourceKind)
		{
			return NumberUtils.uIntToBinString(getValue(sourceKind),
											   EntropyAccumulator.SourceParams.BIT_MASK_LENGTH, '0');
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public int getEntropySourceBitMask(EntropyAccumulator.SourceKind key)
	{
		return cpEntropySourceBitMask.getValue(key);
	}

	//------------------------------------------------------------------

	public void setEntropySourceBitMask(EntropyAccumulator.SourceKind key,
										int                           value)
	{
		cpEntropySourceBitMask.setValue(key, value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPEntropySourceBitMask	cpEntropySourceBitMask	= new CPEntropySourceBitMask();

	//==================================================================


	// PROPERTY CLASS: ENTROPY SOURCE, INTERVAL


	private class CPEntropySourceInterval
		extends Property.PropertyMap<EntropyAccumulator.SourceKind, Integer>
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	DEFAULT_INTERVAL	= 4;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPEntropySourceInterval()
		{
			super(concatenateKeys(Key.ENTROPY, Key.SOURCE, Key.INTERVAL),
				  EntropyAccumulator.SourceKind.class);
			for (EntropyAccumulator.SourceKind sourceKind : EntropyAccumulator.SourceKind.values())
			{
				if (sourceKind.hasInterval())
					values.put(sourceKind, DEFAULT_INTERVAL);
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input                         input,
						  EntropyAccumulator.SourceKind sourceKind)
			throws AppException
		{
			IntegerRange range = new IntegerRange(EntropyAccumulator.SourceParams.MIN_INTERVAL,
												  EntropyAccumulator.SourceParams.MAX_INTERVAL);
			values.put(sourceKind, input.parseInteger(range));
		}

		//--------------------------------------------------------------

		@Override
		public String toString(EntropyAccumulator.SourceKind sourceKind)
		{
			return (sourceKind.hasInterval() ? Integer.toString(getValue(sourceKind)) : null);
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public int getEntropySourceInterval(EntropyAccumulator.SourceKind key)
	{
		return cpEntropySourceInterval.getValue(key);
	}

	//------------------------------------------------------------------

	public void setEntropySourceInterval(EntropyAccumulator.SourceKind key,
										 int                           value)
	{
		cpEntropySourceInterval.setValue(key, value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPEntropySourceInterval	cpEntropySourceInterval	= new CPEntropySourceInterval();

	//==================================================================


	// PROPERTY CLASS: SPLIT FILE-PART LENGTH LOWER BOUND


	private class CPSplitFilePartLengthLowerBound
		extends Property.SimpleProperty<ByteUnitIntegerSpinnerPanel.Value>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPSplitFilePartLengthLowerBound()
		{
			super(concatenateKeys(Key.SPLIT, Key.FILE_PART_LENGTH_LOWER_BOUND));
			value = new ByteUnitIntegerSpinnerPanel.Value(ByteUnitIntegerSpinnerPanel.Unit.MEBIBYTE,
														  16 << 20);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input input)
			throws IllegalValueException
		{
			try
			{
				value = new ByteUnitIntegerSpinnerPanel.Value(input.getValue());
			}
			catch (IllegalArgumentException e)
			{
				throw new IllegalValueException(input);
			}
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return value.toString();
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public ByteUnitIntegerSpinnerPanel.Value getSplitFilePartLengthLowerBound()
	{
		return cpSplitFilePartLengthLowerBound.getValue();
	}

	//------------------------------------------------------------------

	public void setSplitFilePartLengthLowerBound(ByteUnitIntegerSpinnerPanel.Value value)
	{
		cpSplitFilePartLengthLowerBound.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPSplitFilePartLengthLowerBound	cpSplitFilePartLengthLowerBound	=
																	new CPSplitFilePartLengthLowerBound();

	//==================================================================


	// PROPERTY CLASS: SPLIT FILE-PART LENGTH UPPER BOUND


	private class CPSplitFilePartLengthUpperBound
		extends Property.SimpleProperty<ByteUnitIntegerSpinnerPanel.Value>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPSplitFilePartLengthUpperBound()
		{
			super(concatenateKeys(Key.SPLIT, Key.FILE_PART_LENGTH_UPPER_BOUND));
			value = new ByteUnitIntegerSpinnerPanel.Value(ByteUnitIntegerSpinnerPanel.Unit.MEBIBYTE,
														  64 << 20);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input input)
			throws IllegalValueException
		{
			try
			{
				value = new ByteUnitIntegerSpinnerPanel.Value(input.getValue());
			}
			catch (IllegalArgumentException e)
			{
				throw new IllegalValueException(input);
			}
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return value.toString();
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public ByteUnitIntegerSpinnerPanel.Value getSplitFilePartLengthUpperBound()
	{
		return cpSplitFilePartLengthUpperBound.getValue();
	}

	//------------------------------------------------------------------

	public void setSplitFilePartLengthUpperBound(ByteUnitIntegerSpinnerPanel.Value value)
	{
		cpSplitFilePartLengthUpperBound.setValue(value);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPSplitFilePartLengthUpperBound	cpSplitFilePartLengthUpperBound	=
																	new CPSplitFilePartLengthUpperBound();

	//==================================================================


	// PROPERTY CLASS: FONTS


	private class CPFonts
		extends Property.PropertyMap<AppFont, FontEx>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPFonts()
		{
			super(Key.FONT, AppFont.class);
			for (AppFont font : AppFont.values())
				values.put(font, new FontEx());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input   input,
						  AppFont appFont)
		{
			try
			{
				FontEx font = new FontEx(input.getValue());
				appFont.setFontEx(font);
				values.put(appFont, font);
			}
			catch (IllegalArgumentException e)
			{
				showWarningMessage(new IllegalValueException(input));
			}
			catch (uk.blankaspect.common.exception.ValueOutOfBoundsException e)
			{
				showWarningMessage(new ValueOutOfBoundsException(input));
			}
		}

		//--------------------------------------------------------------

		@Override
		public String toString(AppFont appFont)
		{
			return getValue(appFont).toString();
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance methods : associated methods in enclosing class
//--////////////////////////////////////////////////////////////////////

	public FontEx getFont(int index)
	{
		return cpFonts.getValue(AppFont.values()[index]);
	}

	//------------------------------------------------------------------

	public void setFont(int    index,
						FontEx font)
	{
		cpFonts.setValue(AppFont.values()[index], font);
	}

	//------------------------------------------------------------------

//--////////////////////////////////////////////////////////////////////
//--//  Instance fields : associated variables in enclosing class
//--////////////////////////////////////////////////////////////////////

	private	CPFonts	cpFonts	= new CPFonts();

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private AppConfig()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static void showWarningMessage(AppException exception)
	{
		App.INSTANCE.showWarningMessage(App.SHORT_NAME + " | " + CONFIG_ERROR_STR, exception);
	}

	//------------------------------------------------------------------

	public static void showErrorMessage(AppException exception)
	{
		App.INSTANCE.showErrorMessage(App.SHORT_NAME + " | " + CONFIG_ERROR_STR, exception);
	}

	//------------------------------------------------------------------

	private static File getFile()
		throws AppException
	{
		File file = null;

		// Get directory of JAR file
		File jarDirectory = null;
		try
		{
			jarDirectory = new File(AppConfig.class.getProtectionDomain().getCodeSource().getLocation().
																				toURI()).getParentFile();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// Get pathname of configuration directory from properties file
		String pathname = null;
		File propertiesFile = new File(jarDirectory, PROPERTIES_FILENAME);
		if (propertiesFile.isFile())
		{
			try
			{
				Properties properties = new Properties();
				properties.loadFromXML(new FileInputStream(propertiesFile));
				pathname = properties.getProperty(CONFIG_DIR_KEY);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.ERROR_READING_PROPERTIES_FILE, propertiesFile);
			}
		}

		// Get pathname of configuration directory from system property or set system property to pathname
		try
		{
			if (pathname == null)
				pathname = System.getProperty(CONFIG_DIR_KEY);
			else
				System.setProperty(CONFIG_DIR_KEY, pathname);
		}
		catch (SecurityException e)
		{
			// ignore
		}

		// Look for configuration file in default locations
		if (pathname == null)
		{
			// Look for configuration file in local directory
			file = new File(CONFIG_FILENAME);

			// Look for configuration file in default configuration directory
			if (!file.isFile())
			{
				file = null;
				pathname = Utils.getPropertiesPathname();
				if (pathname != null)
				{
					file = new File(pathname, CONFIG_FILENAME);
					if (!file.isFile())
						file = null;
				}
			}
		}

		// Set configuration file from pathname of configuration directory
		else if (!pathname.isEmpty())
		{
			file = new File(PropertyString.parsePathname(pathname), CONFIG_FILENAME);
			if (!file.isFile())
				throw new FileException(ErrorId.NO_CONFIGURATION_FILE, file);
		}

		return file;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public File chooseFile(Component parent)
	{
		if (fileChooser == null)
		{
			fileChooser = new JFileChooser();
			fileChooser.setDialogTitle(SAVE_CONFIGURATION_FILE_STR);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setFileFilter(new FilenameSuffixFilter(AppConstants.XML_FILES_STR,
															   AppConstants.XML_FILE_SUFFIX));
			selectedFile = file;
		}

		fileChooser.setSelectedFile((selectedFile == null) ? new File(CONFIG_FILENAME).getAbsoluteFile()
														   : selectedFile.getAbsoluteFile());
		fileChooser.rescanCurrentDirectory();
		if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			selectedFile = Utils.appendSuffix(fileChooser.getSelectedFile(),
											  AppConstants.XML_FILE_SUFFIX);
			return selectedFile;
		}
		return null;
	}

	//------------------------------------------------------------------

	public void read()
	{
		// Read configuration file
		fileRead = false;
		ConfigFile configFile = null;
		try
		{
			file = getFile();
			if (file != null)
			{
				configFile = new ConfigFile();
				configFile.read(file);
				fileRead = true;
			}
		}
		catch (AppException e)
		{
			showErrorMessage(e);
		}

		// Get properties
		if (fileRead)
			getProperties(configFile, Property.getSystemSource());
		else
			getProperties(Property.getSystemSource());

		// Reset changed status of properties
		resetChanged();
	}

	//------------------------------------------------------------------

	public void write()
	{
		if (isChanged())
		{
			try
			{
				if (file == null)
				{
					if (System.getProperty(CONFIG_DIR_KEY) == null)
					{
						String pathname = Utils.getPropertiesPathname();
						if (pathname != null)
						{
							File directory = new File(pathname);
							if (!directory.exists() && !directory.mkdirs())
								throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory);
							file = new File(directory, CONFIG_FILENAME);
						}
					}
				}
				else
				{
					if (!fileRead)
						file.renameTo(new File(file.getParentFile(), CONFIG_OLD_FILENAME));
				}
				if (file != null)
				{
					write(file);
					resetChanged();
				}
			}
			catch (AppException e)
			{
				showErrorMessage(e);
			}
		}
	}

	//------------------------------------------------------------------

	public void write(File file)
		throws AppException
	{
		// Initialise progress view
		IProgressView progressView = Task.getProgressView();
		if (progressView != null)
		{
			progressView.setInfo(WRITING_STR, file);
			progressView.setProgress(0, -1.0);
		}

		// Create new DOM document
		ConfigFile configFile = new ConfigFile(Integer.toString(VERSION));

		// Set configuration properties in document
		putProperties(configFile);

		// Write file
		configFile.write(file);
	}

	//------------------------------------------------------------------

	public Map<EntropyAccumulator.SourceKind, EntropyAccumulator.SourceParams> getEntropySourceParams()
	{
		Map<EntropyAccumulator.SourceKind, EntropyAccumulator.SourceParams> sourceParams =
													new EnumMap<>(EntropyAccumulator.SourceKind.class);
		for (EntropyAccumulator.SourceKind sourceKind : EntropyAccumulator.SourceKind.values())
		{
			int bitMask = getEntropySourceBitMask(sourceKind);
			if (bitMask != 0)
			{
				int interval = 0;
				if (sourceKind.hasInterval())
					interval = getEntropySourceInterval(sourceKind);
				sourceParams.put(sourceKind, new EntropyAccumulator.SourceParams(bitMask, interval));
			}
		}
		return sourceParams;
	}

	//------------------------------------------------------------------

	private void getProperties(Property.ISource... propertySources)
	{
		for (Property property : getProperties())
		{
			try
			{
				property.get(propertySources);
			}
			catch (AppException e)
			{
				showWarningMessage(e);
			}
		}
	}

	//------------------------------------------------------------------

	private void putProperties(Property.ITarget propertyTarget)
	{
		for (Property property : getProperties())
			property.put(propertyTarget);
	}

	//------------------------------------------------------------------

	private boolean isChanged()
	{
		for (Property property : getProperties())
		{
			if (property.isChanged())
				return true;
		}
		return false;
	}

	//------------------------------------------------------------------

	private void resetChanged()
	{
		for (Property property : getProperties())
			property.setChanged(false);
	}

	//------------------------------------------------------------------

	private List<Property> getProperties()
	{
		if (properties == null)
		{
			properties = new ArrayList<>();
			for (Field field : getClass().getDeclaredFields())
			{
				try
				{
					if (field.getName().startsWith(Property.FIELD_PREFIX))
						properties.add((Property)field.get(this));
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
		}
		return properties;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Static initialiser
////////////////////////////////////////////////////////////////////////

	static
	{
		INSTANCE = new AppConfig();
	}

////////////////////////////////////////////////////////////////////////
//  Instance fields
////////////////////////////////////////////////////////////////////////

	private	File			file;
	private	boolean			fileRead;
	private	File			selectedFile;
	private	JFileChooser	fileChooser;
	private	List<Property>	properties;

}

//----------------------------------------------------------------------
