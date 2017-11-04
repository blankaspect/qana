/*====================================================================*\

DecryptDialog.java

Decrypt dialog box class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.Dialog;
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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.common.gui.FButton;
import uk.blankaspect.common.gui.FLabel;
import uk.blankaspect.common.gui.GuiUtils;
import uk.blankaspect.common.gui.PathnamePanel;

import uk.blankaspect.common.misc.IFileImporter;
import uk.blankaspect.common.misc.KeyAction;

//----------------------------------------------------------------------


// DECRYPT DIALOG BOX CLASS


class DecryptDialog
	extends JDialog
	implements ActionListener, DocumentListener, IFileImporter
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	Insets	ARROW_BUTTON_MARGINS	= new Insets(2, 4, 2, 4);

	private static final	String	KEY	= DecryptDialog.class.getCanonicalName();

	private static final	String	TITLE_STR					= "Decrypt file";
	private static final	String	INPUT_FILE_STR				= "Input file";
	private static final	String	OUTPUT_FILE_STR				= "Output file";
	private static final	String	DECRYPT_STR					= "Decrypt";
	private static final	String	INPUT_FILE_TITLE_STR		= DECRYPT_STR + " : Input file";
	private static final	String	OUTPUT_FILE_TITLE_STR		= DECRYPT_STR + " : Output file";
	private static final	String	SET_OUT_FROM_IN_TOOLTIP_STR	= "Set output file from input file";
	private static final	String	SELECT_STR					= "Select";
	private static final	String	SELECT_FILE_STR				= "Select file";

	// Commands
	private interface Command
	{
		String	CHOOSE_INPUT_FILE				= "chooseInputFile";
		String	CHOOSE_OUTPUT_FILE				= "chooseOutputFile";
		String	SET_OUTPUT_FILE_FROM_INPUT_FILE	= "setOutputFileFromInputFile";
		String	ACCEPT							= "accept";
		String	CLOSE							= "close";
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

		FILE_DOES_NOT_EXIST
		("The file does not exist."),

		NOT_A_FILE
		("The output pathname does not denote a normal file."),

		NO_INPUT_FILE
		("No input file was specified.");

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


	// RESULT CLASS


	public static class Result
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public Result(File inFile,
					  File outFile)
		{
			this.inFile = inFile;
			this.outFile = outFile;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance fields
	////////////////////////////////////////////////////////////////////

		File	inFile;
		File	outFile;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private DecryptDialog(Window owner,
						  File   inFile)
	{

		// Call superclass constructor
		super(owner, TITLE_STR, Dialog.ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());


		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		int gridY = 0;

		// Label: input file
		JLabel inFileLabel = new FLabel(INPUT_FILE_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(inFileLabel, gbc);
		controlPanel.add(inFileLabel);

		// Panel: input file
		inputFileField = new FPathnameField((inFile == null) ? inputFile : inFile);
		if (inFile == null)
			inputFileField.getDocument().addDocumentListener(this);
		else
			inputFileField.setEnabled(false);
		FPathnameField.addObserver(KEY, inputFileField);
		PathnamePanel inFilePanel = new PathnamePanel(inputFileField, Command.CHOOSE_INPUT_FILE,
													  (inFile == null) ? this : null);

		gbc.gridx = 1;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(inFilePanel, gbc);
		controlPanel.add(inFilePanel);

		// Button: set output file from input file
		JButton setOutFileFromInFileButton = new JButton(AppIcon.LONG_ARROW_DOWN);
		setOutFileFromInFileButton.setMargin(ARROW_BUTTON_MARGINS);
		setOutFileFromInFileButton.setToolTipText(SET_OUT_FROM_IN_TOOLTIP_STR);
		setOutFileFromInFileButton.setActionCommand(Command.SET_OUTPUT_FILE_FROM_INPUT_FILE);
		setOutFileFromInFileButton.addActionListener(this);

		gbc.gridx = 2;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(setOutFileFromInFileButton, gbc);
		controlPanel.add(setOutFileFromInFileButton);

		// Label: output file
		JLabel outFileLabel = new FLabel(OUTPUT_FILE_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(outFileLabel, gbc);
		controlPanel.add(outFileLabel);

		// Panel: output file
		outputFileField = new FPathnameField((inFile == null) ? outputFile : null);
		FPathnameField.addObserver(KEY, outputFileField);
		PathnamePanel outputFilePanel = new PathnamePanel(outputFileField, Command.CHOOSE_OUTPUT_FILE,
														  this);

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


		//----  Button panel

		JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 8, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

		// Button: decrypt
		decryptButton = new FButton(DECRYPT_STR);
		decryptButton.setActionCommand(Command.ACCEPT);
		decryptButton.addActionListener(this);
		buttonPanel.add(decryptButton);

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
		updateAcceptButton();


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

		// Set transfer handler
		setTransferHandler(FileTransferHandler.INSTANCE);

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

		// Set location of dialog box
		if (location == null)
			location = GuiUtils.getComponentLocation(this, owner);
		setLocation(location);

		// Set default button
		getRootPane().setDefaultButton(decryptButton);

		// Set focus
		if (inFile != null)
			outputFileField.requestFocusInWindow();

		// Show dialog
		setVisible(true);

	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static Result showDialog(Component parent,
									File      inFile)
	{
		return new DecryptDialog(GuiUtils.getWindow(parent), inFile).getResult();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();

		if (command.equals(Command.CHOOSE_INPUT_FILE))
			onChooseInputFile();

		else if (command.equals(Command.CHOOSE_OUTPUT_FILE))
			onChooseOutputFile();

		else if (command.equals(Command.SET_OUTPUT_FILE_FROM_INPUT_FILE))
			onSetOutputFileFromInputFile();

		else if (command.equals(Command.ACCEPT))
			onAccept();

		else if (command.equals(Command.CLOSE))
			onClose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : DocumentListener interface
////////////////////////////////////////////////////////////////////////

	public void changedUpdate(DocumentEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	public void insertUpdate(DocumentEvent event)
	{
		updateAcceptButton();
	}

	//------------------------------------------------------------------

	public void removeUpdate(DocumentEvent event)
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
	public void importFiles(List<File> files)
	{
		inputFileField.setFile(files.get(0));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private Result getResult()
	{
		return (accepted ? new Result(inputFile, outputFile) : null);
	}

	//------------------------------------------------------------------

	private void updateAcceptButton()
	{
		decryptButton.setEnabled(!inputFileField.isEmpty());
	}

	//------------------------------------------------------------------

	private void validateUserInput()
		throws AppException
	{
		// Input file
		try
		{
			if (inputFileField.isEmpty())
				throw new AppException(ErrorId.NO_INPUT_FILE);
			File file = inputFileField.getFile();
			if (!file.exists())
				throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, file);
			if (!file.isFile())
				throw new FileException(ErrorId.NOT_A_FILE, file);
		}
		catch (AppException e)
		{
			GuiUtils.setFocus(inputFileField);
			throw e;
		}

		// Output file
		try
		{
			if (!outputFileField.isEmpty())
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

	private void onChooseInputFile()
	{
		if (!inputFileField.isEmpty())
			inputFileChooser.setCurrentDirectory(inputFileField.getCanonicalFile());
		inputFileChooser.rescanCurrentDirectory();
		if (inputFileChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			inputFileField.setFile(Utils.appendSuffix(inputFileChooser.getSelectedFile(),
													  FileKind.ENCRYPTED.getFilenameSuffix()));
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

	private void onSetOutputFileFromInputFile()
	{
		if (inputFileField.isEmpty())
			outputFileField.setText(null);
		else
		{
			File file = inputFileField.getFile();
			outputFileField.setFile(Utils.getPlaintextFile(file.getParentFile(), file.getName()));
		}
	}

	//------------------------------------------------------------------

	private void onAccept()
	{
		try
		{
			// Validate user input
			validateUserInput();

			// Update class fields
			inputFile = inputFileField.isEmpty() ? null : inputFileField.getFile();
			outputFile = outputFileField.isEmpty() ? null : outputFileField.getFile();

			// Close dialog
			accepted = true;
			onClose();
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, App.SHORT_NAME, JOptionPane.ERROR_MESSAGE);
		}
	}

	//------------------------------------------------------------------

	private void onClose()
	{
		FPathnameField.removeObservers(KEY);

		location = getLocation();
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class fields
////////////////////////////////////////////////////////////////////////

	private static	Point			location;
	private static	File			inputFile;
	private static	File			outputFile;
	private static	JFileChooser	inputFileChooser;
	private static	JFileChooser	outputFileChooser;

////////////////////////////////////////////////////////////////////////
//  Static initialiser
////////////////////////////////////////////////////////////////////////

	static
	{
		inputFileChooser = new JFileChooser();
		inputFileChooser.setDialogTitle(INPUT_FILE_TITLE_STR);
		inputFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		inputFileChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		inputFileChooser.setApproveButtonToolTipText(SELECT_FILE_STR);
		inputFileChooser.setFileFilter(FileKind.ENCRYPTED.getFileFilter());

		outputFileChooser = new JFileChooser();
		outputFileChooser.setDialogTitle(OUTPUT_FILE_TITLE_STR);
		outputFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		outputFileChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		outputFileChooser.setApproveButtonToolTipText(SELECT_FILE_STR);
	}

////////////////////////////////////////////////////////////////////////
//  Instance fields
////////////////////////////////////////////////////////////////////////

	private	boolean			accepted;
	private	FPathnameField	inputFileField;
	private	FPathnameField	outputFileField;
	private	JButton			decryptButton;

}

//----------------------------------------------------------------------
