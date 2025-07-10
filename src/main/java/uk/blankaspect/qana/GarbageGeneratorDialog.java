/*====================================================================*\

GarbageGeneratorDialog.java

Garbage generator dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.common.misc.FilenameSuffixFilter;
import uk.blankaspect.common.misc.MaxValueMap;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;
import uk.blankaspect.ui.swing.button.FixedWidthRadioButton;

import uk.blankaspect.ui.swing.combobox.FComboBox;

import uk.blankaspect.ui.swing.container.PathnamePanel;

import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.label.FLabel;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.textfield.IntegerField;

//----------------------------------------------------------------------


// GARBAGE GENERATOR DIALOG CLASS


class GarbageGeneratorDialog
	extends JDialog
	implements ActionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	MIN_OUTPUT_LENGTH		= 1;
	private static final	int	MAX_OUTPUT_LENGTH		= 99999999;
	private static final	int	DEFAULT_OUTPUT_LENGTH	= 1000;

	private static final	Insets	GENERATE_LENGTH_BUTTON_MARGINS	= new Insets(1, 4, 1, 4);

	private static final	String	KEY	= GarbageGeneratorDialog.class.getCanonicalName();

	private static final	String	TITLE_STR		= "Generate garbage";
	private static final	String	LENGTH_STR		= "Length";
	private static final	String	RANDOMISE_STR	= "Randomise";
	private static final	String	OUTPUT_FILE_STR	= "Output file";
	private static final	String	IMAGE_FILE_STR	= "Image file";
	private static final	String	SELECT_STR		= "Select";
	private static final	String	SELECT_FILE_STR	= "Select file";

	private static final	List<LengthRange>	LENGTH_RANGES;

	// Commands
	private interface Command
	{
		String	SELECT_OUTPUT_KIND	= "selectOutputKind";
		String	CHOOSE_OUTPUT_FILE	= "chooseOutputFile";
		String	CHOOSE_IMAGE_FILE	= "chooseImageFile";
		String	GENERATE_LENGTH		= "generateLength";
		String	ACCEPT				= "accept";
		String	CLOSE				= "close";
	}

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// OUTPUT KIND


	private enum OutputKind
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FILE
		(
			"File",
			KeyEvent.VK_F,
			11
		),

		TEXT
		(
			"Text",
			KeyEvent.VK_T,
			7
		),

		IMAGE
		(
			"Image",
			KeyEvent.VK_I,
			9
		);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private OutputKind(String text,
						   int    mnemonicKey,
						   int    numLengthRanges)
		{
			this.text = text;
			this.mnemonicKey = mnemonicKey;
			this.numLengthRanges = numLengthRanges;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		public static OutputKind get()
		{
			for (OutputKind value : values())
			{
				if (value.radioButton.isSelected())
					return value;
			}
			return null;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public RadioButton getRadioButton()
		{
			if (radioButton == null)
			{
				radioButton = new RadioButton(" " + text);
				radioButton.setMnemonic(mnemonicKey);
				radioButton.setActionCommand(Command.SELECT_OUTPUT_KIND);
				if (buttonGroup == null)
					buttonGroup = new ButtonGroup();
				buttonGroup.add(radioButton);
			}
			return radioButton;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class variables
	////////////////////////////////////////////////////////////////////

		private static	ButtonGroup	buttonGroup;

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String		text;
		private	int			mnemonicKey;
		private	RadioButton	radioButton;
		private	int			numLengthRanges;

	}

	//==================================================================


	// ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		NO_FILE
		("No file was specified."),

		NO_IMAGE_FILE
		("No image file was specified."),

		NOT_A_FILE
		("The pathname does not denote a file."),

		INVALID_LENGTH
		("The length is invalid."),

		LENGTH_OUT_OF_BOUNDS
		("The length must be between " + MIN_OUTPUT_LENGTH + " and " + MAX_OUTPUT_LENGTH + ".");

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
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// RESULT CLASS


	public static class Result
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Result(File file,
					   File imageFile,
					   int  length)
		{
			this.file = file;
			this.imageFile = imageFile;
			this.length = length;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		File	file;
		File	imageFile;
		int		length;

	}

	//==================================================================


	// LENGTH RANGE CLASS


	private static class LengthRange
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	MIN_EXPONENT		= 4;
		private static final	int	MAX_EXPONENT		= 24;
		private static final	int	EXPONENT_INCREMENT	= 2;

		private static final	int	K_EXPONENT	= 10;
		private static final	int	K_BOUND		= 1 << K_EXPONENT;

		private static final	int	M_EXPONENT	= 20;
		private static final	int	M_BOUND		= 1 << M_EXPONENT;

		private static final	String	K_SUFFIX	= " KiB";
		private static final	String	M_SUFFIX	= " MiB";

		private static final	String	DEFAULT_BOUND_SEPARATOR	= "to";
		private static final	char[]	BOUND_SEPARATOR_CHARS	=
		{
			'\u2013',   // EN DASH
			'\u2212'    // MINUS SIGN
		};

		private static final	String	BOUND_SEPARATOR;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private LengthRange(int exponent)
		{
			lowerBound = 1 << exponent;
			upperBound = 1 << (exponent + EXPONENT_INCREMENT);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static String boundToString(int bound)
		{
			String suffix = "";
			int value = bound;
			if (bound >= M_BOUND)
			{
				suffix = M_SUFFIX;
				value >>>= M_EXPONENT;
			}
			else if (bound >= K_BOUND)
			{
				suffix = K_SUFFIX;
				value >>>= K_EXPONENT;
			}
			return (Integer.toString(value) + suffix);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public String toString()
		{
			return (boundToString(lowerBound) + " " + BOUND_SEPARATOR + " " +
																			boundToString(upperBound));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Static initialiser
	////////////////////////////////////////////////////////////////////

		static
		{
			String separator = DEFAULT_BOUND_SEPARATOR;
			for (char separatorChar : BOUND_SEPARATOR_CHARS)
			{
				if (AppFont.COMBO_BOX.getFont().canDisplay(separatorChar))
				{
					separator = Character.toString(separatorChar);
					break;
				}
			}
			BOUND_SEPARATOR = separator;
		}

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int	lowerBound;
		private	int	upperBound;

	}

	//==================================================================


	// RADIO BUTTON CLASS


	private static class RadioButton
		extends FixedWidthRadioButton
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	KEY	= RadioButton.class.getCanonicalName();

		private static final	Color	BACKGROUND_COLOUR	= new Color(252, 224, 128);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private RadioButton(String text)
		{
			super(text);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static void reset()
		{
			MaxValueMap.removeAll(KEY);
		}

		//--------------------------------------------------------------

		private static void update()
		{
			MaxValueMap.update(KEY);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public Color getBackground()
		{
			return (isSelected() ? BACKGROUND_COLOUR : super.getBackground());
		}

		//--------------------------------------------------------------

		@Override
		protected String getKey()
		{
			return KEY;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// LENGTH FIELD CLASS


	private static class LengthField
		extends IntegerField.Unsigned
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private static final	int	LENGTH	= 8;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private LengthField(int value)
		{
			super(LENGTH, value);
			AppFont.TEXT_FIELD.apply(this);
			GuiUtils.setTextComponentMargins(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected int getColumnWidth()
		{
			return (FontUtils.getCharWidth('0', getFontMetrics(getFont())) + 1);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private GarbageGeneratorDialog(Window  owner,
								   boolean canGenerateText)
	{
		// Call superclass constructor
		super(owner, TITLE_STR, ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());

		// Initialise instance variables
		outFileChooser = new JFileChooser();
		outFileChooser.setDialogTitle(OUTPUT_FILE_STR);
		outFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		outFileChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		outFileChooser.setApproveButtonToolTipText(SELECT_FILE_STR);
		outFileChooser.setFileFilter(FileKind.ENCRYPTED.getFileFilter());

		imageFileChooser = new JFileChooser();
		imageFileChooser.setDialogTitle(IMAGE_FILE_STR);
		imageFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		imageFileChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		imageFileChooser.setApproveButtonToolTipText(SELECT_FILE_STR);
		imageFileChooser.setFileFilter(new FilenameSuffixFilter(AppConstants.PNG_FILES_STR,
																AppConstants.PNG_FILENAME_EXTENSION));


		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		int gridY = 0;

		// Reset fixed-width radio buttons
		RadioButton.reset();

		// Radio button: file
		JRadioButton fileRadioButton = OutputKind.FILE.getRadioButton();
		fileRadioButton.addActionListener(this);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(fileRadioButton, gbc);
		controlPanel.add(fileRadioButton);

		// Panel: output pathname
		outPathnameField = new FPathnameField(outputFile);
		FPathnameField.addObserver(KEY, outPathnameField);
		outPathnamePanel = new PathnamePanel(outPathnameField, Command.CHOOSE_OUTPUT_FILE, this);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(outPathnamePanel, gbc);
		controlPanel.add(outPathnamePanel);

		// Radio button: text
		JRadioButton textRadioButton = OutputKind.TEXT.getRadioButton();
		if (canGenerateText)
			textRadioButton.addActionListener(this);
		else
		{
			textRadioButton.setEnabled(false);
			if (outputKind == OutputKind.TEXT)
				 outputKind = OutputKind.FILE;
		}

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(textRadioButton, gbc);
		controlPanel.add(textRadioButton);

		// Radio button: image
		JRadioButton imageRadioButton = OutputKind.IMAGE.getRadioButton();
		imageRadioButton.addActionListener(this);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(imageRadioButton, gbc);
		controlPanel.add(imageRadioButton);

		// Panel: image pathname
		imagePathnameField = new FPathnameField(imageFile);
		FPathnameField.addObserver(KEY, imagePathnameField);
		imagePathnamePanel = new PathnamePanel(imagePathnameField, Command.CHOOSE_IMAGE_FILE, this);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(imagePathnamePanel, gbc);
		controlPanel.add(imagePathnamePanel);

		// Label: length
		JLabel lengthLabel = new FLabel(LENGTH_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(lengthLabel, gbc);
		controlPanel.add(lengthLabel);

		// Panel: length
		JPanel lengthPanel = new JPanel(gridBag);

		int gridX = 0;

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(lengthPanel, gbc);
		controlPanel.add(lengthPanel);

		// Field: length
		lengthField = new LengthField(outputLength);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(lengthField, gbc);
		lengthPanel.add(lengthField);

		// Label: arrow
		JLabel arrowLabel = new JLabel(Icons.ANGLE_DOUBLE_LEFT);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 6, 0, 0);
		gridBag.setConstraints(arrowLabel, gbc);
		lengthPanel.add(arrowLabel);

		// Combo box: length ranges
		lengthRangeComboBox = new FComboBox<>();

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 6, 0, 0);
		gridBag.setConstraints(lengthRangeComboBox, gbc);
		lengthPanel.add(lengthRangeComboBox);

		// Button: generate random length
		JButton generateLengthButton = new FButton(RANDOMISE_STR);
		generateLengthButton.setMargin(GENERATE_LENGTH_BUTTON_MARGINS);
		generateLengthButton.setActionCommand(Command.GENERATE_LENGTH);
		generateLengthButton.addActionListener(this);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 8, 0, 0);
		gridBag.setConstraints(generateLengthButton, gbc);
		lengthPanel.add(generateLengthButton);

		// Update widths of radio buttons
		RadioButton.update();

		// Select radio button
		outputKind.getRadioButton().setSelected(true);


		//----  Button panel

		JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 8, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

		// Button: OK
		JButton okButton = new FButton(AppConstants.OK_STR);
		okButton.setActionCommand(Command.ACCEPT);
		okButton.addActionListener(this);
		buttonPanel.add(okButton);

		// Button: cancel
		JButton cancelButton = new FButton(AppConstants.CANCEL_STR);
		cancelButton.setActionCommand(Command.CLOSE);
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);


		//----  Main panel

		JPanel mainPanel = new JPanel(gridBag);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(controlPanel, gbc);
		mainPanel.add(controlPanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(3, 0, 0, 0);
		gridBag.setConstraints(buttonPanel, gbc);
		mainPanel.add(buttonPanel);

		// Add commands to action map
		KeyAction.create(mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						 KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), Command.CLOSE, this);

		// Update components
		updateComponents();


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

		// Dispose of window explicitly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Handle window closing
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				onClose();
			}
		});

		// Prevent dialog from being resized
		setResizable(false);

		// Resize dialog to its preferred size
		pack();

		// Set location of dialog
		if (location == null)
			location = GuiUtils.getComponentLocation(this, owner);
		setLocation(location);

		// Show dialog
		setVisible(true);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static Result showDialog(Component parent,
									boolean   canGenerateText)
	{
		return new GarbageGeneratorDialog(GuiUtils.getWindow(parent), canGenerateText).getResult();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();

		if (command.equals(Command.SELECT_OUTPUT_KIND))
			onSelectOutputKind();

		else if (command.equals(Command.CHOOSE_OUTPUT_FILE))
			onChooseOutputFile();

		else if (command.equals(Command.CHOOSE_IMAGE_FILE))
			onChooseImageFile();

		else if (command.equals(Command.GENERATE_LENGTH))
			onGenerateLength();

		else if (command.equals(Command.ACCEPT))
			onAccept();

		else if (command.equals(Command.CLOSE))
			onClose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private Result getResult()
	{
		Result result = null;
		if (accepted)
		{
			File file = (outputKind == OutputKind.FILE) ? outPathnameField.getFile() : null;
			File imageFile = (outputKind == OutputKind.IMAGE) ? imagePathnameField.getFile() : null;
			int length = lengthField.getValue();
			result = new Result(file, imageFile, length);
		}
		return result;
	}

	//------------------------------------------------------------------

	private void updateComponents()
	{
		GuiUtils.setAllEnabled(outPathnamePanel, (outputKind == OutputKind.FILE));
		GuiUtils.setAllEnabled(imagePathnamePanel, (outputKind == OutputKind.IMAGE));

		int index = lengthRangeComboBox.getSelectedIndex();
		if (index < 0)
			index = lengthRangeIndex;
		lengthRangeComboBox.removeAllItems();
		for (int i = 0; i < outputKind.numLengthRanges; i++)
			lengthRangeComboBox.addItem(LENGTH_RANGES.get(i));
		lengthRangeComboBox.setSelectedIndex(Math.min(Math.max(0, index),
													  outputKind.numLengthRanges - 1));
	}

	//------------------------------------------------------------------

	private void validateUserInput()
		throws AppException
	{
		// File
		if (outputKind == OutputKind.FILE)
		{
			try
			{
				if (outPathnameField.isEmpty())
					throw new AppException(ErrorId.NO_FILE);
				File file = outPathnameField.getFile();
				if (file.exists() && !file.isFile())
					throw new FileException(ErrorId.NOT_A_FILE, file);
			}
			catch (AppException e)
			{
				GuiUtils.setFocus(outPathnameField);
				throw e;
			}
		}

		// Image
		if (outputKind == OutputKind.IMAGE)
		{
			try
			{
				if (imagePathnameField.isEmpty())
					throw new AppException(ErrorId.NO_IMAGE_FILE);
				File file = imagePathnameField.getFile();
				if (file.exists() && !file.isFile())
					throw new FileException(ErrorId.NOT_A_FILE, file);
			}
			catch (AppException e)
			{
				GuiUtils.setFocus(imagePathnameField);
				throw e;
			}
		}

		// Length
		try
		{
			try
			{
				int length = lengthField.getValue();
				if ((length < MIN_OUTPUT_LENGTH) || (length > MAX_OUTPUT_LENGTH))
					throw new AppException(ErrorId.LENGTH_OUT_OF_BOUNDS);
			}
			catch (NumberFormatException e)
			{
				throw new AppException(ErrorId.INVALID_LENGTH);
			}
		}
		catch (AppException e)
		{
			GuiUtils.setFocus(lengthField);
			throw e;
		}
	}

	//------------------------------------------------------------------

	private void onSelectOutputKind()
	{
		outputKind = OutputKind.get();
		updateComponents();
	}

	//------------------------------------------------------------------

	private void onChooseOutputFile()
	{
		outFileChooser.setCurrentDirectory(outPathnameField.getCanonicalFile());
		outFileChooser.rescanCurrentDirectory();
		if (outFileChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			outPathnameField.setFile(Utils.appendSuffix(outFileChooser.getSelectedFile(),
														FileKind.ENCRYPTED.getFilenameSuffix()));
	}

	//------------------------------------------------------------------

	private void onChooseImageFile()
	{
		imageFileChooser.setCurrentDirectory(imagePathnameField.getCanonicalFile());
		imageFileChooser.rescanCurrentDirectory();
		if (imageFileChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			imagePathnameField.setFile(Utils.appendSuffix(imageFileChooser.getSelectedFile(),
														  AppConstants.PNG_FILENAME_EXTENSION));
	}

	//------------------------------------------------------------------

	private void onGenerateLength()
	{
		LengthRange lengthRange = lengthRangeComboBox.getSelectedValue();
		int length = lengthRange.lowerBound +
										randSeq.nextInt(lengthRange.upperBound - lengthRange.lowerBound);
		lengthField.setValue(length);
	}

	//------------------------------------------------------------------

	private void onAccept()
	{
		try
		{
			// Validate user input
			validateUserInput();

			// Update class fields
			outputFile = outPathnameField.isEmpty() ? null : outPathnameField.getFile();
			imageFile = imagePathnameField.isEmpty() ? null : imagePathnameField.getFile();
			try
			{
				outputLength = lengthField.getValue();
			}
			catch (NumberFormatException e)
			{
				outputLength = DEFAULT_OUTPUT_LENGTH;
			}

			// Close dialog
			accepted = true;
			onClose();
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, QanaApp.SHORT_NAME, JOptionPane.ERROR_MESSAGE);
		}
	}

	//------------------------------------------------------------------

	private void onClose()
	{
		FPathnameField.removeObservers(KEY);

		location = getLocation();
		lengthRangeIndex = lengthRangeComboBox.getSelectedIndex();
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point		location;
	private static	int			lengthRangeIndex;
	private static	Random		randSeq				= new Random();
	private static	OutputKind	outputKind			= OutputKind.FILE;
	private static	File		outputFile;
	private static	File		imageFile;
	private static	int			outputLength		= DEFAULT_OUTPUT_LENGTH;

////////////////////////////////////////////////////////////////////////
//  Static initialiser
////////////////////////////////////////////////////////////////////////

	static
	{
		LENGTH_RANGES = new ArrayList<>();
		for (int exponent = LengthRange.MIN_EXPONENT; exponent <= LengthRange.MAX_EXPONENT;
																exponent += LengthRange.EXPONENT_INCREMENT)
			LENGTH_RANGES.add(new LengthRange(exponent));
		LENGTH_RANGES.get(0).lowerBound = 1;
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	boolean					accepted;
	private	FPathnameField			outPathnameField;
	private	PathnamePanel			outPathnamePanel;
	private	FPathnameField			imagePathnameField;
	private	PathnamePanel			imagePathnamePanel;
	private	LengthField				lengthField;
	private	FComboBox<LengthRange>	lengthRangeComboBox;
	private	JFileChooser			outFileChooser;
	private	JFileChooser			imageFileChooser;

}

//----------------------------------------------------------------------
