/*====================================================================*\

ConcealDialog.java

Class: 'conceal' dialog.

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

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.common.misc.IFileImporter;
import uk.blankaspect.common.misc.MaxValueMap;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;
import uk.blankaspect.ui.swing.button.FixedWidthRadioButton;

import uk.blankaspect.ui.swing.checkbox.FCheckBox;

import uk.blankaspect.ui.swing.combobox.FComboBox;

import uk.blankaspect.ui.swing.container.PathnamePanel;

import uk.blankaspect.ui.swing.filechooser.FileChooserUtils;

import uk.blankaspect.ui.swing.label.FLabel;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.workaround.LinuxWorkarounds;

//----------------------------------------------------------------------


// CLASS: 'CONCEAL' DIALOG


class ConcealDialog
	extends JDialog
	implements ActionListener, DocumentListener, IFileImporter
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		MIN_MAX_NUM_BITS		= 1;
	private static final	int		MAX_MAX_NUM_BITS		= 7;
	private static final	int		DEFAULT_MAX_NUM_BITS	= 2;

	private static final	String	FILE_STR					= "file";
	private static final	String	TEXT_STR					= "text";
	private static final	String	PAYLOAD_FILE_STR			= "Payload file";
	private static final	String	OUTPUT_FILE_STR				= "Output file";
	private static final	String	MAX_NUM_BITS_STR			= "Maximum number of bits";
	private static final	String	SET_TIMESTAMP_STR			= "Set timestamp";
	private static final	String	ADD_RANDOM_BITS_STR			= "Add random bits";
	private static final	String	CONCEAL_STR					= "Conceal";
	private static final	String	PAYLOAD_FILE_TITLE_STR		= CONCEAL_STR + " : Payload file";
	private static final	String	CARRIER_FILE_TITLE_STR		= CONCEAL_STR + " : Carrier file";
	private static final	String	OUTPUT_FILE_TITLE_STR		= CONCEAL_STR + " : Output file";
	private static final	String	SET_TIMESTAMP_TOOLTIP_STR	=
			"Set the timestamp of the output file to that of the carrier file";
	private static final	String	ADD_RANDOM_BITS_TOOLTIP_STR	=
			"Add random bits to the output image to disguise the payload";
	private static final	String	SELECT_STR					= "Select";
	private static final	String	SELECT_FILE_STR				= "Select file";

	// Commands
	private interface Command
	{
		String	CHOOSE_PAYLOAD_FILE	= "choosePayloadFile";
		String	CHOOSE_CARRIER_FILE	= "chooseCarrierFile";
		String	CHOOSE_OUTPUT_FILE	= "chooseOutputFile";
		String	SELECT_CARRIER_KIND	= "selectCarrierKind";
		String	ACCEPT				= "accept";
		String	CLOSE				= "close";
	}

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point			location;
	private static	CarrierKind		carrierKind			= CarrierKind.FILE;
	private static	File			payloadFile;
	private static	File			carrierFile;
	private static	File			outputFile;
	private static	int				maxNumBits			= DEFAULT_MAX_NUM_BITS;
	private static	boolean			setTimestamp		= true;
	private static	boolean			addRandomBits;
	private static	JFileChooser	payloadFileChooser;
	private static	JFileChooser	carrierFileChooser;
	private static	JFileChooser	outputFileChooser;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	boolean				accepted;
	private	FPathnameField		payloadFileField;
	private	FPathnameField		carrierFileField;
	private	PathnamePanel		carrierFilePanel;
	private	FPathnameField		outputFileField;
	private	JLabel				maxNumBitsLabel;
	private	FComboBox<Integer>	maxNumBitsComboBox;
	private	JCheckBox			setTimestampCheckBox;
	private	JCheckBox			addRandomBitsCheckBox;
	private	JButton				concealButton;

////////////////////////////////////////////////////////////////////////
//  Static initialiser
////////////////////////////////////////////////////////////////////////

	static
	{
		payloadFileChooser = new JFileChooser();
		payloadFileChooser.setDialogTitle(PAYLOAD_FILE_TITLE_STR);
		payloadFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		payloadFileChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		payloadFileChooser.setApproveButtonToolTipText(SELECT_FILE_STR);

		carrierFileChooser = new JFileChooser();
		carrierFileChooser.setDialogTitle(CARRIER_FILE_TITLE_STR);
		carrierFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		carrierFileChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		carrierFileChooser.setApproveButtonToolTipText(SELECT_FILE_STR);
		FileChooserUtils.setFilters(carrierFileChooser, AppConstants.PNG_FILE_FILTER, AppConstants.BMP_FILE_FILTER,
									AppConstants.GIF_FILE_FILTER, AppConstants.JPEG_FILE_FILTER,
									AppConstants.PNG_FILE_FILTER);

		outputFileChooser = new JFileChooser();
		outputFileChooser.setDialogTitle(OUTPUT_FILE_TITLE_STR);
		outputFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		outputFileChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		outputFileChooser.setApproveButtonToolTipText(SELECT_FILE_STR);
		FileChooserUtils.setFilter(outputFileChooser, AppConstants.PNG_FILE_FILTER);
	}

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private ConcealDialog(
		Window	owner,
		boolean	inputFromFile)
	{
		// Call superclass constructor
		super(owner, CONCEAL_STR + " " + (inputFromFile ? FILE_STR : TEXT_STR),
			  ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());


		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		// Reset fixed-width radio buttons
		RadioButton.reset();

		int gridY = 0;

		if (inputFromFile)
		{
			// Label: payload file
			JLabel payloadFileLabel = new FLabel(PAYLOAD_FILE_STR);

			gbc.gridx = 0;
			gbc.gridy = gridY;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_END;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(payloadFileLabel, gbc);
			controlPanel.add(payloadFileLabel);

			// Panel: payload file
			payloadFileField = new FPathnameField(payloadFile);
			payloadFileField.getDocument().addDocumentListener(this);
			PathnamePanel payloadFilePanel = new PathnamePanel(payloadFileField, Command.CHOOSE_PAYLOAD_FILE, this);

			gbc.gridx = 1;
			gbc.gridy = gridY++;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(payloadFilePanel, gbc);
			controlPanel.add(payloadFilePanel);
		}

		// Radio button: carrier file
		JRadioButton carrierFileButton = CarrierKind.FILE.getRadioButton();
		carrierFileButton.addActionListener(this);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(carrierFileButton, gbc);
		controlPanel.add(carrierFileButton);

		// Panel: carrier file
		carrierFileField = new FPathnameField(carrierFile);
		carrierFileField.getDocument().addDocumentListener(this);
		carrierFilePanel = new PathnamePanel(carrierFileField, Command.CHOOSE_CARRIER_FILE, this);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(carrierFilePanel, gbc);
		controlPanel.add(carrierFilePanel);

		// Radio button: carrier image
		JRadioButton carrierImageRadioButton = CarrierKind.AUTOGENERATED.getRadioButton();
		carrierImageRadioButton.addActionListener(this);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(carrierImageRadioButton, gbc);
		controlPanel.add(carrierImageRadioButton);

		// Label: output file
		JLabel outputFileLabel = new FLabel(OUTPUT_FILE_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(outputFileLabel, gbc);
		controlPanel.add(outputFileLabel);

		// Panel: output file
		outputFileField = new FPathnameField(outputFile);
		outputFileField.getDocument().addDocumentListener(this);
		PathnamePanel outputFilePanel = new PathnamePanel(outputFileField, Command.CHOOSE_OUTPUT_FILE, this);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(outputFilePanel, gbc);
		controlPanel.add(outputFilePanel);

		// Label: maximum number of bits
		maxNumBitsLabel = new FLabel(MAX_NUM_BITS_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(maxNumBitsLabel, gbc);
		controlPanel.add(maxNumBitsLabel);

		// Combo box: maximum number of bits
		maxNumBitsComboBox = new FComboBox<>();
		for (int i = MIN_MAX_NUM_BITS; i <= MAX_MAX_NUM_BITS; i++)
			maxNumBitsComboBox.addItem(i);
		maxNumBitsComboBox.setSelectedValue(maxNumBits);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(maxNumBitsComboBox, gbc);
		controlPanel.add(maxNumBitsComboBox);

		// Check box: set timestamp
		setTimestampCheckBox = new FCheckBox(SET_TIMESTAMP_STR);
		setTimestampCheckBox.setToolTipText(SET_TIMESTAMP_TOOLTIP_STR);
		setTimestampCheckBox.setSelected(setTimestamp);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS_EXTRA_TOP;
		gridBag.setConstraints(setTimestampCheckBox, gbc);
		controlPanel.add(setTimestampCheckBox);

		// Check box: add random bits
		addRandomBitsCheckBox = new FCheckBox(ADD_RANDOM_BITS_STR);
		addRandomBitsCheckBox.setToolTipText(ADD_RANDOM_BITS_TOOLTIP_STR);
		addRandomBitsCheckBox.setSelected(addRandomBits);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(addRandomBitsCheckBox, gbc);
		controlPanel.add(addRandomBitsCheckBox);

		// Select radio button
		carrierKind.getRadioButton().setSelected(true);


		//----  Button panel

		JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 8, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

		// Button: conceal
		concealButton = new FButton(CONCEAL_STR);
		concealButton.setActionCommand(Command.ACCEPT);
		concealButton.addActionListener(this);
		buttonPanel.add(concealButton);

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
		gbc.fill = GridBagConstraints.NONE;
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

		// Update widths of radio buttons
		RadioButton.update();

		// Set transfer handler
		setTransferHandler(FileTransferHandler.INSTANCE);

		// Dispose of window explicitly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Handle window events
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(
				WindowEvent	event)
			{
				// WORKAROUND for a bug that has been observed on Linux/GNOME whereby a window is displaced downwards
				// when its location is set.  The error in the y coordinate is the height of the title bar of the
				// window.  The workaround is to set the location of the window again with an adjustment for the error.
				LinuxWorkarounds.fixWindowYCoord(event.getWindow(), location);
			}

			@Override
			public void windowClosing(
				WindowEvent	event)
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

		// Set default button
		getRootPane().setDefaultButton(concealButton);

		// Show dialog
		setVisible(true);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static Result showDialog(
		Component	parent,
		boolean		inputFromFile)
	{
		return new ConcealDialog(GuiUtils.getWindow(parent), inputFromFile).getResult();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void actionPerformed(
		ActionEvent	event)
	{
		switch (event.getActionCommand())
		{
			case Command.CHOOSE_PAYLOAD_FILE -> onChoosePayloadFile();
			case Command.CHOOSE_CARRIER_FILE -> onChooseCarrierFile();
			case Command.CHOOSE_OUTPUT_FILE  -> onChooseOutputFile();
			case Command.SELECT_CARRIER_KIND -> onSelectCarrierKind();
			case Command.ACCEPT              -> onAccept();
			case Command.CLOSE               -> onClose();
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : DocumentListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void changedUpdate(
		DocumentEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void insertUpdate(
		DocumentEvent	event)
	{
		updateAcceptButton();
	}

	//------------------------------------------------------------------

	@Override
	public void removeUpdate(
		DocumentEvent	event)
	{
		updateAcceptButton();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IFileImporter interface
////////////////////////////////////////////////////////////////////////

	@Override
	public boolean canImportFiles()
	{
		return true;
	}

	//------------------------------------------------------------------

	@Override
	public boolean canImportMultipleFiles()
	{
		return false;
	}

	//------------------------------------------------------------------

	@Override
	public void importFiles(
		List<File>	files)
	{
		File file = files.get(0);
		if (payloadFileField == null)
		{
			if (carrierKind == CarrierKind.FILE)
				carrierFileField.setFile(file);
			else
				outputFileField.setFile(file);
		}
		else
			payloadFileField.setFile(file);
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
			switch (carrierKind)
			{
				case FILE:
					result = new Result((payloadFileField == null) ? null : payloadFile, carrierFile,
										outputFile, maxNumBits, setTimestamp, addRandomBits);
					break;

				case AUTOGENERATED:
					result = new Result((payloadFileField == null) ? null : payloadFile, null, outputFile,
										CarrierImage.NUM_CARRIER_BITS, false, false);
					break;
			}
		}
		return result;
	}

	//------------------------------------------------------------------

	private void updateComponents()
	{
		boolean file = (carrierKind == CarrierKind.FILE);
		GuiUtils.setAllEnabled(carrierFilePanel, file);
		maxNumBitsLabel.setEnabled(file);
		maxNumBitsComboBox.setEnabled(file);
		setTimestampCheckBox.setEnabled(file);
		addRandomBitsCheckBox.setEnabled(file);

		updateAcceptButton();
	}

	//------------------------------------------------------------------

	private void updateAcceptButton()
	{
		concealButton.setEnabled(((payloadFileField == null) ? !outputFileField.isEmpty()
															 : !payloadFileField.isEmpty())
										&& ((carrierKind == CarrierKind.AUTOGENERATED) || !carrierFileField.isEmpty()));
	}

	//------------------------------------------------------------------

	private void validateUserInput()
		throws AppException
	{
		// Payload file
		try
		{
			if (payloadFileField != null)
			{
				if (payloadFileField.isEmpty())
					throw new AppException(ErrorId.NO_INPUT_FILE);
				File file = payloadFileField.getFile();
				if (!file.exists())
					throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, file);
				if (!file.isFile())
					throw new FileException(ErrorId.NOT_A_FILE, file);
			}
		}
		catch (AppException e)
		{
			GuiUtils.setFocus(payloadFileField);
			throw e;
		}

		// Carrier
		if (carrierKind == CarrierKind.FILE)
		{
			try
			{
				if (carrierFileField.isEmpty())
					throw new AppException(ErrorId.NO_CARRIER_FILE);
				File file = carrierFileField.getFile();
				if (!file.exists())
					throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, file);
				if (!file.isFile())
					throw new FileException(ErrorId.NOT_A_FILE, file);
			}
			catch (AppException e)
			{
				GuiUtils.setFocus(carrierFileField);
				throw e;
			}
		}

		// Output file
		try
		{
			if (outputFileField.isEmpty())
			{
				if (payloadFileField == null)
					throw new AppException(ErrorId.NO_OUTPUT_FILE);
			}
			else
			{
				File file = outputFileField.getFile();
				if (file.exists() && !file.isFile())
					throw new FileException(ErrorId.NOT_A_FILE, file);
			}
		}
		catch (AppException e)
		{
			GuiUtils.setFocus(outputFileField);
			throw e;
		}
	}

	//------------------------------------------------------------------

	private void onChoosePayloadFile()
	{
		if (!payloadFileField.isEmpty())
			payloadFileChooser.setSelectedFile(payloadFileField.getCanonicalFile());
		payloadFileChooser.rescanCurrentDirectory();
		if (payloadFileChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			payloadFileField.setFile(payloadFileChooser.getSelectedFile());
	}

	//------------------------------------------------------------------

	private void onChooseCarrierFile()
	{
		if (!carrierFileField.isEmpty())
			carrierFileChooser.setSelectedFile(carrierFileField.getCanonicalFile());
		carrierFileChooser.rescanCurrentDirectory();
		if (carrierFileChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			carrierFileField.setFile(carrierFileChooser.getSelectedFile());
	}

	//------------------------------------------------------------------

	private void onChooseOutputFile()
	{
		if (!outputFileField.isEmpty())
			outputFileChooser.setSelectedFile(outputFileField.getCanonicalFile());
		outputFileChooser.rescanCurrentDirectory();
		if (outputFileChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			outputFileField.setFile(outputFileChooser.getSelectedFile());
	}

	//------------------------------------------------------------------

	private void onSelectCarrierKind()
	{
		carrierKind = CarrierKind.get();
		updateComponents();
	}

	//------------------------------------------------------------------

	private void onAccept()
	{
		try
		{
			// Validate fields
			validateUserInput();

			// Update class fields
			if (payloadFileField != null)
				payloadFile = payloadFileField.getFile();
			carrierFile = (carrierKind == CarrierKind.FILE) ? carrierFileField.getFile() : null;
			outputFile = outputFileField.isEmpty() ? null : outputFileField.getFile();
			maxNumBits = maxNumBitsComboBox.getSelectedValue();
			setTimestamp = setTimestampCheckBox.isSelected();
			addRandomBits = addRandomBitsCheckBox.isSelected();

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
		location = getLocation();
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: CARRIER KIND


	private enum CarrierKind
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FILE
		(
			"Carrier file",
			KeyEvent.VK_F
		),

		AUTOGENERATED
		(
			"Autogenerated carrier",
			KeyEvent.VK_A
		);

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

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CarrierKind(
			String	text,
			int		mnemonicKey)
		{
			this.text = text;
			this.mnemonicKey = mnemonicKey;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		public static CarrierKind get()
		{
			for (CarrierKind value : values())
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
				radioButton.setActionCommand(Command.SELECT_CARRIER_KIND);
				if (buttonGroup == null)
					buttonGroup = new ButtonGroup();
				buttonGroup.add(radioButton);
			}
			return radioButton;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// ENUMERATION: ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FILE_DOES_NOT_EXIST
		("The file does not exist."),

		NOT_A_FILE
		("The input pathname does not denote a normal file."),

		NO_INPUT_FILE
		("No input file was specified."),

		NO_CARRIER_FILE
		("No carrier file was specified."),

		NO_OUTPUT_FILE
		("No output file was specified.");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ErrorId(
			String	message)
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
//  Member records
////////////////////////////////////////////////////////////////////////


	// RECORD: RESULT


	public record Result(
		File	inFile,
		File	carrierFile,
		File	outFile,
		int		maxNumBits,
		boolean	setTimestamp,
		boolean	addRandomBits)
	{ }

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: RADIO BUTTON


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

		private RadioButton(
			String	text)
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
			return isSelected() ? BACKGROUND_COLOUR : super.getBackground();
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

}

//----------------------------------------------------------------------
