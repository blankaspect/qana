/*====================================================================*\

FilePartSetSelectionDialog.java

File-part set selection dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
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

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.blankaspect.common.time.CalendarTime;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.list.SingleSelectionList;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.text.TextRendering;

import uk.blankaspect.ui.swing.workaround.LinuxWorkarounds;

//----------------------------------------------------------------------


// FILE-PART SET SELECTION DIALOG CLASS


class FilePartSetSelectionDialog
	extends JDialog
	implements ActionListener, ChangeListener, ListSelectionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	TITLE_STR	= "Select set of file parts";

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

	private	boolean			accepted;
	private	FilePartList	filePartList;
	private	JScrollPane		filePartListScrollPane;
	private	JButton			okButton;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private FilePartSetSelectionDialog(Window                           owner,
									   List<FileSplitter.FirstFilePart> fileParts)
	{
		// Call superclass constructor
		super(owner, TITLE_STR, ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());


		//----  File-part list

		// List
		filePartList = new FilePartList(fileParts);
		filePartList.addActionListener(this);
		filePartList.addListSelectionListener(this);

		// Scroll pane
		filePartListScrollPane = new JScrollPane(filePartList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
												 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		filePartListScrollPane.getVerticalScrollBar().setFocusable(false);
		filePartListScrollPane.getVerticalScrollBar().getModel().addChangeListener(this);

		filePartList.setViewport(filePartListScrollPane.getViewport());


		//----  Button panel

		JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 8, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

		// Button: OK
		okButton = new FButton(AppConstants.OK_STR);
		okButton.setActionCommand(Command.ACCEPT);
		okButton.addActionListener(this);
		buttonPanel.add(okButton);

		// Button: cancel
		JButton cancelButton = new FButton(AppConstants.CANCEL_STR);
		cancelButton.setActionCommand(Command.CLOSE);
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);


		//----  Main panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel listPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(listPanel);

		JPanel mainPanel = new JPanel(gridBag);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		int gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(filePartListScrollPane, gbc);
		mainPanel.add(filePartListScrollPane);

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

	public static FileSplitter.FirstFilePart showDialog(Component                        parent,
														List<FileSplitter.FirstFilePart> fileParts)
	{
		return new FilePartSetSelectionDialog(GuiUtils.getWindow(parent), fileParts).getFilePart();
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
			case Command.ACCEPT, FilePartList.Command.EDIT_ELEMENT -> onAccept();
			case Command.CLOSE                                     -> onClose();
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ChangeListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void stateChanged(ChangeEvent event)
	{
		if (!filePartListScrollPane.getVerticalScrollBar().getValueIsAdjusting() &&
			 !filePartList.isDragging())
			filePartList.snapViewPosition();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ListSelectionListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void valueChanged(ListSelectionEvent event)
	{
		okButton.setEnabled(filePartList.isSelection());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private FileSplitter.FirstFilePart getFilePart()
	{
		return accepted ? filePartList.getSelectedElement() : null;
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
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// FILE-PART LIST CLASS


	private static class FilePartList
		extends SingleSelectionList<FileSplitter.FirstFilePart>
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int		NUM_COLUMNS	= 1;
		private static final	int		NUM_ROWS	= 12;

		private static final	int		SEPARATOR_WIDTH	= 1;

		private static final	Color	SEPARATOR_COLOUR	= new Color(192, 200, 192);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int	filenameFieldWidth;
		private	int	numPartsFieldWidth;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FilePartList(List<FileSplitter.FirstFilePart> fileParts)
		{
			// Call superclass constructor
			super(NUM_COLUMNS, NUM_ROWS, AppFont.MAIN.getFont(), fileParts);

			// Get widths of fields
			FontMetrics fontMetrics = getFontMetrics(getFont());
			int timestampFieldWidth = 0;
			for (FileSplitter.FirstFilePart filePart : fileParts)
			{
				int width = fontMetrics.stringWidth(filePart.name);
				if (filenameFieldWidth < width)
					filenameFieldWidth = width;

				width = fontMetrics.stringWidth(Integer.toString(filePart.numFileParts));
				if (numPartsFieldWidth < width)
					numPartsFieldWidth = width;

				width = fontMetrics.stringWidth(getTimestampString(filePart));
				if (timestampFieldWidth < width)
					timestampFieldWidth = width;
			}

			// Set column width, extra width and row height
			setColumnWidth(filenameFieldWidth + numPartsFieldWidth + timestampFieldWidth);
			setExtraWidth(2 * (2 * getHorizontalMargin() + SEPARATOR_WIDTH));
			setRowHeight(getRowHeight() + 1);

			// Set properties
			setDragEnabled(false);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static String getTimestampString(FileSplitter.FirstFilePart filePart)
		{
			return CalendarTime.timeToString(filePart.timestamp, "  ");
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected void drawElement(Graphics gr,
								   int      index)
		{
			// Create copy of graphics context
			Graphics2D gr2d = GuiUtils.copyGraphicsContext(gr);

			// Set rendering hints for text antialiasing and fractional metrics
			TextRendering.setHints(gr2d);

			// Get file part
			FileSplitter.FirstFilePart filePart = getElement(index);

			// Draw filename
			int rowHeight = getRowHeight();
			int x = getHorizontalMargin();
			int y = index * rowHeight;
			FontMetrics fontMetrics = gr2d.getFontMetrics();
			int textY = y + DEFAULT_VERTICAL_MARGIN + fontMetrics.getAscent();
			gr2d.setColor(getForegroundColour(index));
			gr2d.drawString(filePart.name, x, textY);

			// Draw number of file parts
			String str = Integer.toString(filePart.numFileParts);
			x += filenameFieldWidth + getExtraWidth() / 2;
			gr2d.drawString(str, x + numPartsFieldWidth - fontMetrics.stringWidth(str), textY);

			// Draw timestamp
			x += numPartsFieldWidth + getExtraWidth() / 2;
			gr2d.drawString(getTimestampString(filePart), x, textY);

			// Draw separators
			gr2d.setColor(SEPARATOR_COLOUR);
			x = 2 * getHorizontalMargin() + filenameFieldWidth;
			int y2 = y + rowHeight - 1;
			gr2d.drawLine(x, y, x, y2);
			x += getExtraWidth() / 2 + numPartsFieldWidth;
			gr2d.drawLine(x, y, x, y2);

			// Draw bottom border
			y += rowHeight - 1;
			gr2d.drawLine(0, y, getWidth() - 1, y);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
