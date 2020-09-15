/*====================================================================*\

FileSelectionDialog.java

File selection dialog box class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import uk.blankaspect.common.number.NumberUtils;

import uk.blankaspect.common.string.StringUtils;

import uk.blankaspect.common.swing.action.KeyAction;

import uk.blankaspect.common.swing.button.FButton;

import uk.blankaspect.common.swing.colour.Colours;

import uk.blankaspect.common.swing.label.FLabel;

import uk.blankaspect.common.swing.misc.GuiUtils;

import uk.blankaspect.common.swing.text.TextRendering;

//----------------------------------------------------------------------


// FILE SELECTION DIALOG BOX CLASS


class FileSelectionDialog
	extends JDialog
	implements ActionListener, ListDataListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	FILE_LIST_NUM_COLUMNS	= 80;
	private static final	int	FILE_LIST_NUM_ROWS		= 20;

	private static final	String	INPUT_FILES_STR			= "Input files";
	private static final	String	INPUT_FILES_TITLE_STR	= "Input files";

	// Commands
	private interface Command
	{
		String	ACCEPT	= "accept";
		String	CLOSE	= "close";
	}

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// FILE SELECTION PANEL CLASS


	private static class FileSelectionPanel
		extends uk.blankaspect.common.swing.container.FileSelectionPanel
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	SYSTEM_STR	= "system";
		private static final	String	ARCHIVE_STR	= "archive";

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public FileSelectionPanel(int    listNumViewableColumns,
								  int    listNumViewableRows,
								  String titleStr)
		{
			super(listNumViewableColumns, listNumViewableRows, Mode.FILES_RECURSIVE, pathnameKind,
				  titleStr, null);
			setTransferHandler(FileTransferHandler.INSTANCE);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected String getTitle()
		{
			return App.SHORT_NAME;
		}

		//--------------------------------------------------------------

		@Override
		protected String fileToPathname(File file)
		{
			return Utils.getPathname(file);
		}

		//--------------------------------------------------------------

		@Override
		protected String pathnameKindToString(PathnameKind pathnameKind,
											  boolean      title)
		{
			String str = null;
			switch (pathnameKind)
			{
				case NORMAL:
					str = SYSTEM_STR;
					break;

				case RELATIVE:
					str = ARCHIVE_STR;
					break;
			}
			return (title ? StringUtils.firstCharToUpperCase(str) : str);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class variables
	////////////////////////////////////////////////////////////////////

		private static	PathnameKind	pathnameKind	= PathnameKind.NORMAL;

	}

	//==================================================================


	// LENGTH FIELD CLASS


	private static class LengthField
		extends JComponent
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	Color	TEXT_COLOUR	= Colours.FOREGROUND;

		private static final	String	PREFIX	= "(";
		private static final	String	SUFFIX	= ")";
		private static final	String	EXCESS_PREFIX	= "> ";

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private LengthField(int maxLength)
		{
			// Initialise instance variables
			this.maxLength = maxLength;
			AppFont.MAIN.apply(this);
			int numDigits = NumberUtils.getNumDecDigitsInt(maxLength);
			String prototypeStr = PREFIX + EXCESS_PREFIX +
												StringUtils.createCharString('0', numDigits) + SUFFIX;
			FontMetrics fontMetrics = getFontMetrics(getFont());
			setPreferredSize(new Dimension(fontMetrics.stringWidth(prototypeStr),
										   fontMetrics.getAscent() + fontMetrics.getDescent()));

			// Set attributes
			setOpaque(true);
			setFocusable(false);

			// Initialise length
			setLength(0);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected void paintComponent(Graphics gr)
		{
			// Create copy of graphics context
			gr = gr.create();

			// Get dimensions
			int width = getWidth();
			int height = getHeight();

			// Draw background
			gr.setColor(getBackground());
			gr.fillRect(0, 0, width, height);

			// Set rendering hints for text antialiasing and fractional metrics
			TextRendering.setHints((Graphics2D)gr);

			// Draw text
			gr.setColor(TEXT_COLOUR);
			FontMetrics fontMetrics = gr.getFontMetrics();
			gr.drawString(text, width - fontMetrics.stringWidth(text), fontMetrics.getAscent());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public void setLength(int length)
		{
			String str = PREFIX + ((length > maxLength) ? EXCESS_PREFIX + Integer.toString(maxLength)
														: Integer.toString(length)) + SUFFIX;
			if (!StringUtils.equal(str, text))
			{
				text = str;
				repaint();
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int		maxLength;
		private	String	text;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private FileSelectionDialog(Window owner,
								String titleStr,
								int    maxNumFiles)
	{

		// Call superclass constructor
		super(owner, titleStr, Dialog.ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());


		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		int gridY = 0;

		// Panel: labels
		JPanel labelPanel = new JPanel(gridBag);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.FIRST_LINE_END;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = new Insets(4, 3, 2, 3);
		gridBag.setConstraints(labelPanel, gbc);
		controlPanel.add(labelPanel);

		// Label: input files
		JLabel inputFilesLabel = new FLabel(INPUT_FILES_STR);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.5;
		gbc.anchor = GridBagConstraints.FIRST_LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(inputFilesLabel, gbc);
		labelPanel.add(inputFilesLabel);

		// Field: list length
		listLengthField = new LengthField(maxNumFiles);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.5;
		gbc.anchor = GridBagConstraints.LAST_LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(listLengthField, gbc);
		labelPanel.add(listLengthField);

		// Panel: input files
		inputFilePanel = new FileSelectionPanel(FILE_LIST_NUM_COLUMNS, FILE_LIST_NUM_ROWS,
												INPUT_FILES_TITLE_STR);
		inputFilePanel.addListDataListener(this);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(inputFilePanel, gbc);
		controlPanel.add(inputFilePanel);


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
		mainPanel.setTransferHandler(FileTransferHandler.INSTANCE);

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

		// Set location of dialog box
		if (location == null)
			location = GuiUtils.getComponentLocation(this, owner);
		setLocation(location);

		// Set default button
		getRootPane().setDefaultButton(okButton);

		// Show dialog
		setVisible(true);

	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static List<FileSelectionPanel.SelectedFile> showDialog(Component parent,
																   String    titleStr,
																   int       maxNumFiles)
	{
		return new FileSelectionDialog(GuiUtils.getWindow(parent), titleStr, maxNumFiles).
																								getFiles();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();

		if (command.equals(Command.ACCEPT))
			onAccept();

		else if (command.equals(Command.CLOSE))
			onClose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ListDataListener interface
////////////////////////////////////////////////////////////////////////

	public void intervalAdded(ListDataEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	public void intervalRemoved(ListDataEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	public void contentsChanged(ListDataEvent event)
	{
		listLengthField.setLength(inputFilePanel.getNumFiles());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private List<FileSelectionPanel.SelectedFile> getFiles()
	{
		return (accepted ? inputFilePanel.getFiles() : null);
	}

	//------------------------------------------------------------------

	private void onAccept()
	{
		accepted = true;
		onClose();
	}

	//------------------------------------------------------------------

	private void onClose()
	{
		location = getLocation();
		FileSelectionPanel.pathnameKind = inputFilePanel.getPathnameKind();
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point	location;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	boolean				accepted;
	private	LengthField			listLengthField;
	private	FileSelectionPanel	inputFilePanel;

}

//----------------------------------------------------------------------
