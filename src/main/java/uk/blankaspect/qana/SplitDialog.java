/*====================================================================*\

SplitDialog.java

Split dialog box class.

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

import uk.blankaspect.common.gui.ByteUnitIntegerSpinnerPanel;
import uk.blankaspect.common.gui.FButton;
import uk.blankaspect.common.gui.FLabel;
import uk.blankaspect.common.gui.GuiUtils;
import uk.blankaspect.common.gui.LinkedPairButton;
import uk.blankaspect.common.gui.PathnamePanel;

import uk.blankaspect.common.misc.IFileImporter;
import uk.blankaspect.common.misc.KeyAction;

//----------------------------------------------------------------------


// SPLIT DIALOG BOX CLASS


class SplitDialog
	extends JDialog
	implements ActionListener, DocumentListener, IFileImporter, ByteUnitIntegerSpinnerPanel.IObserver
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	FILE_PART_LENGTH_FIELD_LENGTH	= 10;

	private static final	String	KEY	= SplitDialog.class.getCanonicalName();

	private static final	String	TITLE_STR					= "Split file";
	private static final	String	INPUT_FILE_STR				= "Input file";
	private static final	String	OUTPUT_DIRECTORY_STR		= "Output directory";
	private static final	String	FILE_PART_LENGTH_STR		= "File-part length";
	private static final	String	TO_STR						= "to";
	private static final	String	SPLIT_STR					= "Split";
	private static final	String	INPUT_FILE_TITLE_STR		= SPLIT_STR + " | Input file";
	private static final	String	OUTPUT_DIRECTORY_TITLE_STR	= SPLIT_STR + " | Output directory";
	private static final	String	SELECT_STR					= "Select";
	private static final	String	SELECT_FILE_STR				= "Select file";
	private static final	String	SELECT_DIRECTORY_STR		= "Select directory";
	private static final	String	LINK_TOOLTIP_STR			= "lower bound and upper bound";

	// Commands
	private interface Command
	{
		String	CHOOSE_INPUT_FILE			= "chooseInputFile";
		String	CHOOSE_OUTPUT_DIRECTORY		= "chooseOutputDirectory";
		String	TOGGLE_LENGTH_BOUNDS_LINKED	= "toggleLengthBoundsLinked";
		String	ACCEPT						= "accept";
		String	CLOSE						= "close";
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
		("The input pathname does not denote a normal file."),

		NOT_A_DIRECTORY
		("The output pathname does not denote a directory."),

		NO_INPUT_FILE
		("No input file was specified."),

		FILE_PART_LENGTH_BOUNDS_OUT_OF_ORDER
		("The upper bound of the file-part length is less than the lower bound.");

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
					  File outDirectory,
					  int  filePartLengthLowerBound,
					  int  filePartLengthUpperBound)
		{
			this.inFile = inFile;
			this.outDirectory = outDirectory;
			this.filePartLengthLowerBound = filePartLengthLowerBound;
			this.filePartLengthUpperBound = filePartLengthUpperBound;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance fields
	////////////////////////////////////////////////////////////////////

		File	inFile;
		File	outDirectory;
		int		filePartLengthLowerBound;
		int		filePartLengthUpperBound;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private SplitDialog(Window owner)
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
		inputFileField = new FPathnameField(inputFile);
		inputFileField.getDocument().addDocumentListener(this);
		FPathnameField.addObserver(KEY, inputFileField);
		PathnamePanel inFilePanel = new PathnamePanel(inputFileField, Command.CHOOSE_INPUT_FILE, this);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(inFilePanel, gbc);
		controlPanel.add(inFilePanel);

		// Label: output directory
		JLabel outDirectoryLabel = new FLabel(OUTPUT_DIRECTORY_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(outDirectoryLabel, gbc);
		controlPanel.add(outDirectoryLabel);

		// Panel: output directory
		outputDirectoryField = new FPathnameField(outputDirectory);
		FPathnameField.addObserver(KEY, outputDirectoryField);
		PathnamePanel outputDirectoryPanel = new PathnamePanel(outputDirectoryField, Command.CHOOSE_OUTPUT_DIRECTORY,
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
		gridBag.setConstraints(outputDirectoryPanel, gbc);
		controlPanel.add(outputDirectoryPanel);

		// Label: file-part length
		JLabel filePartLengthLabel = new FLabel(FILE_PART_LENGTH_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(filePartLengthLabel, gbc);
		controlPanel.add(filePartLengthLabel);

		// Panel: file-part length
		JPanel filePartLengthPanel = new JPanel(gridBag);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(filePartLengthPanel, gbc);
		controlPanel.add(filePartLengthPanel);

		int gridX = 0;

		// Spinner: file-part length lower bound
		int maxValue = filePartLengthBoundsLinked ? FileSplitter.MAX_FILE_PART_LENGTH : filePartLengthUpperBound.value;
		filePartLengthLowerBoundSpinner = new ByteUnitIntegerSpinnerPanel(filePartLengthLowerBound,
																		  FileSplitter.MIN_FILE_PART_LENGTH, maxValue,
																		  FILE_PART_LENGTH_FIELD_LENGTH);
		filePartLengthLowerBoundSpinner.addObserver(this);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(filePartLengthLowerBoundSpinner, gbc);
		filePartLengthPanel.add(filePartLengthLowerBoundSpinner);

		// Label: to
		JLabel toLabel = new FLabel(TO_STR);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 6, 0, 0);
		gridBag.setConstraints(toLabel, gbc);
		filePartLengthPanel.add(toLabel);

		// Spinner: file-part length upper bound
		int minValue = filePartLengthBoundsLinked ? FileSplitter.MIN_FILE_PART_LENGTH : filePartLengthLowerBound.value;
		filePartLengthUpperBoundSpinner = new ByteUnitIntegerSpinnerPanel(filePartLengthUpperBound, minValue,
																		  FileSplitter.MAX_FILE_PART_LENGTH,
																		  FILE_PART_LENGTH_FIELD_LENGTH);
		filePartLengthUpperBoundSpinner.addObserver(this);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 6, 0, 0);
		gridBag.setConstraints(filePartLengthUpperBoundSpinner, gbc);
		filePartLengthPanel.add(filePartLengthUpperBoundSpinner);

		// Button: link lower bound and upper bound
		linkButton = new LinkedPairButton(LINK_TOOLTIP_STR);
		linkButton.setSelected(filePartLengthBoundsLinked);
		linkButton.setActionCommand(Command.TOGGLE_LENGTH_BOUNDS_LINKED);
		linkButton.addActionListener(this);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 6, 0, 0);
		gridBag.setConstraints(linkButton, gbc);
		filePartLengthPanel.add(linkButton);


		//----  Button panel

		JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 8, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

		// Button: split
		splitButton = new FButton(SPLIT_STR);
		splitButton.setActionCommand(Command.ACCEPT);
		splitButton.addActionListener(this);
		buttonPanel.add(splitButton);

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
		getRootPane().setDefaultButton(splitButton);

		// Show dialog
		setVisible(true);

	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static ByteUnitIntegerSpinnerPanel.Value getFilePartLengthLowerBound()
	{
		return filePartLengthLowerBound;
	}

	//------------------------------------------------------------------

	public static ByteUnitIntegerSpinnerPanel.Value getFilePartLengthUpperBound()
	{
		return filePartLengthUpperBound;
	}

	//------------------------------------------------------------------

	public static Result showDialog(Component parent)
	{
		return new SplitDialog(GuiUtils.getWindow(parent)).getResult();
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

		else if (command.equals(Command.CHOOSE_OUTPUT_DIRECTORY))
			onChooseOutputDirectory();

		else if (command.equals(Command.TOGGLE_LENGTH_BOUNDS_LINKED))
			onToggleLengthBoundsLinked();

		else if (command.equals(Command.ACCEPT))
			onAccept();

		else if (command.equals(Command.CLOSE))
			onClose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ByteUnitIntegerSpinnerPanel.IObserver interface
////////////////////////////////////////////////////////////////////////

	public void notifyChanged(ByteUnitIntegerSpinnerPanel          source,
							  ByteUnitIntegerSpinnerPanel.Property changedProperty)
	{
		// Spinner, file-part length lower bound
		if (source == filePartLengthLowerBoundSpinner)
		{
			switch (changedProperty)
			{
				case UNIT:
					filePartLengthUpperBoundSpinner.setUnit(filePartLengthLowerBoundSpinner.getUnit());
					break;

				case VALUE:
					if (linkButton.isSelected())
						filePartLengthUpperBoundSpinner.
											setValue(filePartLengthLowerBoundSpinner.getValue());
					else
						filePartLengthUpperBoundSpinner.
											setMinimum(filePartLengthLowerBoundSpinner.getIntValue());
					break;

				default:
					// do nothing
					break;
			}
		}

		// Spinner, file-part length upper bound
		else if (source == filePartLengthUpperBoundSpinner)
		{
			switch (changedProperty)
			{
				case UNIT:
					filePartLengthLowerBoundSpinner.setUnit(filePartLengthUpperBoundSpinner.getUnit());
					break;

				case VALUE:
					if (linkButton.isSelected())
						filePartLengthLowerBoundSpinner.
											setValue(filePartLengthUpperBoundSpinner.getValue());
					else
						filePartLengthLowerBoundSpinner.
											setMaximum(filePartLengthUpperBoundSpinner.getIntValue());
					break;

				default:
					// do nothing
					break;
			}
		}
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
		File file = files.get(0);
		if (file.isDirectory())
			outputDirectoryField.setFile(file);
		else
			inputFileField.setFile(file);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private Result getResult()
	{
		return (accepted ? new Result(inputFile, outputDirectory, filePartLengthLowerBound.value,
									  filePartLengthUpperBound.value)
						 : null);
	}

	//------------------------------------------------------------------

	private void updateAcceptButton()
	{
		splitButton.setEnabled(!inputFileField.isEmpty());
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

		// Output directory
		try
		{
			if (!outputDirectoryField.isEmpty())
			{
				File directory = outputDirectoryField.getFile();
				if (directory.exists() && !directory.isDirectory())
					throw new FileException(ErrorId.NOT_A_DIRECTORY, directory);
			}
		}
		catch (AppException e)
		{
			GuiUtils.setFocus(outputDirectoryField);
			throw e;
		}

		// File-part length bounds
		try
		{
			if (filePartLengthUpperBoundSpinner.getIntValue() <
															filePartLengthLowerBoundSpinner.getIntValue())
				throw new AppException(ErrorId.FILE_PART_LENGTH_BOUNDS_OUT_OF_ORDER);
		}
		catch (AppException e)
		{
			GuiUtils.setFocus(filePartLengthUpperBoundSpinner);
			throw e;
		}
	}

	//------------------------------------------------------------------

	private void onChooseInputFile()
	{
		if (!inputFileField.isEmpty())
			inputFileChooser.setSelectedFile(inputFileField.getCanonicalFile());
		inputFileChooser.rescanCurrentDirectory();
		if (inputFileChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			inputFileField.setFile(inputFileChooser.getSelectedFile());
	}

	//------------------------------------------------------------------

	private void onChooseOutputDirectory()
	{
		if (!outputDirectoryField.isEmpty())
			outputDirectoryChooser.setCurrentDirectory(outputDirectoryField.getCanonicalFile());
		outputDirectoryChooser.rescanCurrentDirectory();
		if (outputDirectoryChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			outputDirectoryField.setFile(outputDirectoryChooser.getSelectedFile());
	}

	//------------------------------------------------------------------

	private void onToggleLengthBoundsLinked()
	{
		if (linkButton.isSelected())
		{
			filePartLengthLowerBoundSpinner.setMaximum(FileSplitter.MAX_FILE_PART_LENGTH);
			filePartLengthUpperBoundSpinner.setMinimum(FileSplitter.MIN_FILE_PART_LENGTH);
			filePartLengthUpperBoundSpinner.setValue(filePartLengthLowerBoundSpinner.getValue());
		}
		else
		{
			filePartLengthLowerBoundSpinner.setMaximum(filePartLengthUpperBoundSpinner.getIntValue());
			filePartLengthUpperBoundSpinner.setMinimum(filePartLengthLowerBoundSpinner.getIntValue());
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
			outputDirectory = outputDirectoryField.isEmpty() ? null : outputDirectoryField.getFile();
			filePartLengthLowerBound = filePartLengthLowerBoundSpinner.getValue();
			filePartLengthUpperBound = filePartLengthUpperBoundSpinner.getValue();
			filePartLengthBoundsLinked = linkButton.isSelected();

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

	private static	Point								location;
	private static	File								inputFile;
	private static	File								outputDirectory;
	private static	ByteUnitIntegerSpinnerPanel.Value	filePartLengthLowerBound;
	private static	ByteUnitIntegerSpinnerPanel.Value	filePartLengthUpperBound;
	private static	boolean								filePartLengthBoundsLinked;
	private static	JFileChooser						inputFileChooser;
	private static	JFileChooser						outputDirectoryChooser;

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

		outputDirectoryChooser = new JFileChooser();
		outputDirectoryChooser.setDialogTitle(OUTPUT_DIRECTORY_TITLE_STR);
		outputDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		outputDirectoryChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		outputDirectoryChooser.setApproveButtonToolTipText(SELECT_DIRECTORY_STR);

		AppConfig config = AppConfig.INSTANCE;
		filePartLengthLowerBound = config.getSplitFilePartLengthLowerBound();
		filePartLengthUpperBound = config.getSplitFilePartLengthUpperBound();
		filePartLengthBoundsLinked = (filePartLengthLowerBound.value == filePartLengthUpperBound.value);
	}

////////////////////////////////////////////////////////////////////////
//  Instance fields
////////////////////////////////////////////////////////////////////////

	private	boolean						accepted;
	private	FPathnameField				inputFileField;
	private	FPathnameField				outputDirectoryField;
	private	ByteUnitIntegerSpinnerPanel	filePartLengthLowerBoundSpinner;
	private	ByteUnitIntegerSpinnerPanel	filePartLengthUpperBoundSpinner;
	private	LinkedPairButton			linkButton;
	private	JButton						splitButton;

}

//----------------------------------------------------------------------
