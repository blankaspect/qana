/*====================================================================*\

FileSelectionDialog.java

File selection dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
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
import java.util.Objects;

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

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.colour.Colours;

import uk.blankaspect.ui.swing.label.FLabel;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.text.TextRendering;

import uk.blankaspect.ui.swing.workaround.LinuxWorkarounds;

//----------------------------------------------------------------------


// FILE SELECTION DIALOG CLASS


class FileSelectionDialog
	extends JDialog
	implements ActionListener, ListDataListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		FILE_LIST_NUM_COLUMNS	= 80;
	private static final	int		FILE_LIST_NUM_ROWS		= 20;

	private static final	String	INPUT_FILES_STR			= "Input files";
	private static final	String	INPUT_FILES_TITLE_STR	= "Input files";

	// Commands
	private interface Command
	{
		String	ACCEPT	= "accept";
		String	CLOSE	= "close";
	}

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

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private FileSelectionDialog(Window owner,
								String title,
								int    maxNumFiles)
	{
		// Call superclass constructor
		super(owner, title, ModalityType.APPLICATION_MODAL);

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
		getRootPane().setDefaultButton(okButton);

		// Show dialog
		setVisible(true);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static List<FileSelectionPanel.SelectedFile> showDialog(Component parent,
																   String    title,
																   int       maxNumFiles)
	{
		return new FileSelectionDialog(GuiUtils.getWindow(parent), title, maxNumFiles).getFiles();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void actionPerformed(ActionEvent event)
	{
		switch (event.getActionCommand())
		{
			case Command.ACCEPT -> onAccept();
			case Command.CLOSE  -> onClose();
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ListDataListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void intervalAdded(ListDataEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void intervalRemoved(ListDataEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
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
		return accepted ? inputFilePanel.getFiles() : null;
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
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// FILE SELECTION PANEL CLASS


	private static class FileSelectionPanel
		extends uk.blankaspect.ui.swing.container.FileSelectionPanel
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	SYSTEM_STR	= "system";
		private static final	String	ARCHIVE_STR	= "archive";

	////////////////////////////////////////////////////////////////////
	//  Class variables
	////////////////////////////////////////////////////////////////////

		private static	PathnameKind	pathnameKind	= PathnameKind.NORMAL;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public FileSelectionPanel(int    listNumViewableColumns,
								  int    listNumViewableRows,
								  String title)
		{
			super(listNumViewableColumns, listNumViewableRows, Mode.FILES_RECURSIVE, pathnameKind, title, null);
			setTransferHandler(FileTransferHandler.INSTANCE);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected String getTitle()
		{
			return QanaApp.SHORT_NAME;
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
			return title ? StringUtils.firstCharToUpperCase(str) : str;
		}

		//--------------------------------------------------------------

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
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int		maxLength;
		private	String	text;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private LengthField(int maxLength)
		{
			// Initialise instance variables
			this.maxLength = maxLength;
			AppFont.MAIN.apply(this);
			int numDigits = NumberUtils.getNumDecDigitsInt(maxLength);
			String prototypeStr = PREFIX + EXCESS_PREFIX + "0".repeat(numDigits) + SUFFIX;
			FontMetrics fontMetrics = getFontMetrics(getFont());
			setPreferredSize(new Dimension(fontMetrics.stringWidth(prototypeStr),
										   fontMetrics.getAscent() + fontMetrics.getDescent()));

			// Set properties
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
			Graphics2D gr2d = GuiUtils.copyGraphicsContext(gr);

			// Get dimensions
			int width = getWidth();
			int height = getHeight();

			// Draw background
			gr2d.setColor(getBackground());
			gr2d.fillRect(0, 0, width, height);

			// Set rendering hints for text antialiasing and fractional metrics
			TextRendering.setHints(gr2d);

			// Draw text
			gr2d.setColor(TEXT_COLOUR);
			FontMetrics fontMetrics = gr2d.getFontMetrics();
			gr2d.drawString(text, width - fontMetrics.stringWidth(text), fontMetrics.getAscent());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public void setLength(int length)
		{
			String str = PREFIX + ((length > maxLength) ? EXCESS_PREFIX + Integer.toString(maxLength)
														: Integer.toString(length)) + SUFFIX;
			if (!Objects.equals(str, text))
			{
				text = str;
				repaint();
			}
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
