/*====================================================================*\

TextView.java

Text view class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.BorderLayout;
import java.awt.Point;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;

import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.TransferHandler;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.blankaspect.ui.swing.menu.FCheckBoxMenuItem;
import uk.blankaspect.ui.swing.menu.FMenuItem;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.transfer.DataImporter;
import uk.blankaspect.ui.swing.transfer.TextExporter;

//----------------------------------------------------------------------


// TEXT VIEW CLASS


class TextView
	extends View
	implements MouseListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final	int	MIN_NUM_COLUMNS		= 8;
	public static final	int	MAX_NUM_COLUMNS		= 256;
	public static final	int	DEFAULT_NUM_COLUMNS	= 80;

	public static final	int	MIN_NUM_ROWS		= 8;
	public static final	int	MAX_NUM_ROWS		= 256;
	public static final	int	DEFAULT_NUM_ROWS	= 24;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	TextArea	textArea;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public TextView(TextDocument document)
	{
		// Set layout manager
		setLayout(new BorderLayout());

		// Text area
		AppConfig config = AppConfig.INSTANCE;
		textArea = new TextArea(config.getTextViewSize().height, config.getTextViewSize().width);
		textArea.addMouseListener(this);
		textArea.addCaretListener(document);
		textArea.getDocument().addDocumentListener(document);
		textArea.getDocument().addUndoableEditListener(document.getUndoManager());
		document.setTextArea(textArea);

		// Scroll pane
		add(new TextAreaScrollPane());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MouseListener interface
////////////////////////////////////////////////////////////////////////

	public void mouseClicked(MouseEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	public void mouseEntered(MouseEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	public void mouseExited(MouseEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	public void mousePressed(MouseEvent event)
	{
		showContextMenu(event);
	}

	//------------------------------------------------------------------

	public void mouseReleased(MouseEvent event)
	{
		showContextMenu(event);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public boolean requestFocusInWindow()
	{
		return textArea.requestFocusInWindow();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public TextArea getTextArea()
	{
		return textArea;
	}

	//------------------------------------------------------------------

	private void showContextMenu(MouseEvent event)
	{
		if (event.isPopupTrigger())
		{
			// Create context menu
			JPopupMenu menu = new JPopupMenu();

			menu.add(new FMenuItem(TextDocument.Command.CUT));
			menu.add(new FMenuItem(TextDocument.Command.COPY));
			menu.add(new FMenuItem(TextDocument.Command.COPY_ALL));
			menu.add(new FMenuItem(TextDocument.Command.PASTE));
			menu.add(new FMenuItem(TextDocument.Command.PASTE_ALL));

			menu.addSeparator();

			menu.add(new FMenuItem(TextDocument.Command.CLEAR));

			menu.addSeparator();

			menu.add(new FMenuItem(TextDocument.Command.SELECT_ALL));

			menu.addSeparator();

			menu.add(new FMenuItem(TextDocument.Command.WRAP));

			menu.addSeparator();

			menu.add(new FMenuItem(TextDocument.Command.ENCRYPT));
			menu.add(new FMenuItem(TextDocument.Command.DECRYPT));
			menu.add(new FCheckBoxMenuItem(TextDocument.Command.TOGGLE_WRAP_CIPHERTEXT_IN_XML));

			menu.addSeparator();

			menu.add(new FMenuItem(TextDocument.Command.CONCEAL));
			menu.add(new FMenuItem(AppCommand.RECOVER_TEXT));

			menu.addSeparator();

			menu.add(new FMenuItem(TextDocument.Command.SET_KEY));
			menu.add(new FMenuItem(TextDocument.Command.CLEAR_KEY));

			// Update commands for menu items
			QanaApp.INSTANCE.updateCommands();
			QanaApp.INSTANCE.getTextDocument().updateCommands();

			// Display menu
			menu.show(event.getComponent(), event.getX(), event.getY());
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// TEXT AREA CLASS


	private static class TextArea
		extends JTextArea
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	VERTICAL_MARGIN		= 2;
		private static final	int	HORIZONTAL_MARGIN	= 4;

		private static final	int	TAB_WIDTH	= 4;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private TextArea(int numRows,
						 int numColumns)
		{
			// Call superclass constructor
			super(numRows, numColumns);

			// Set properties
			AppFont.TEXT_VIEW.apply(this);
			setBorder(null);
			setTabSize(TAB_WIDTH);

			// Set colours
			AppConfig config = AppConfig.INSTANCE;
			setForeground(config.getTextViewTextColour());
			setBackground(config.getTextViewBackgroundColour());
			setSelectedTextColor(config.getTextViewSelectionTextColour());
			setSelectionColor(config.getTextViewSelectionBackgroundColour());

			// Set transfer handler
			setTransferHandler(new FileTransferHandler(getTransferHandler()));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public int getRowHeight()
		{
			return super.getRowHeight();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Member classes : inner classes
	////////////////////////////////////////////////////////////////////


		// FILE TRANSFER HANDLER CLASS


		private static class FileTransferHandler
			extends TextExporter
		{

		////////////////////////////////////////////////////////////////
		//  Instance variables
		////////////////////////////////////////////////////////////////

			private	TransferHandler	oldTransferHandler;

		////////////////////////////////////////////////////////////////
		//  Constructors
		////////////////////////////////////////////////////////////////

			private FileTransferHandler(TransferHandler oldTransferHandler)
			{
				this.oldTransferHandler = oldTransferHandler;
			}

			//----------------------------------------------------------

		////////////////////////////////////////////////////////////////
		//  Instance methods : overriding methods
		////////////////////////////////////////////////////////////////

			@Override
			public boolean canImport(TransferHandler.TransferSupport support)
			{
				boolean supported = support.isDrop() && ((support.getSourceDropActions() & COPY) == COPY);
				if (supported)
				{
					supported = DataImporter.isFileList(support.getDataFlavors()) ||
								((oldTransferHandler != null) && oldTransferHandler.canImport(support));
					if (supported)
						support.setDropAction(COPY);
				}
				return supported;
			}

			//----------------------------------------------------------

			@Override
			public boolean importData(TransferHandler.TransferSupport support)
			{
				// Import transferred data as a list of files
				if (support.isDrop() && DataImporter.isFileList(support.getDataFlavors()))
				{
					try
					{
						List<File> files = DataImporter.getFiles(support.getTransferable());
						if (!files.isEmpty())
						{
							GuiUtils.getWindow(support.getComponent()).toFront();
							QanaApp.INSTANCE.addImport(null, files);
							return true;
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						return false;
					}
				}

				// Import transferred data with the old transfer handler
				return (oldTransferHandler == null) ? false : oldTransferHandler.importData(support);
			}

			//----------------------------------------------------------

		}

		//==============================================================

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// TEXT AREA SCROLL PANE CLASS


	private class TextAreaScrollPane
		extends JScrollPane
		implements ChangeListener
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private TextAreaScrollPane()
		{
			// Call superclass constructor
			super(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				  JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

			// Set properties
			setCorner(JScrollPane.LOWER_RIGHT_CORNER, new JPanel());
			GuiUtils.setViewportBorder(this, TextArea.VERTICAL_MARGIN, TextArea.HORIZONTAL_MARGIN);
			getViewport().setFocusable(true);
			getVerticalScrollBar().setFocusable(false);
			getHorizontalScrollBar().setFocusable(false);

			// Add listeners
			getVerticalScrollBar().getModel().addChangeListener(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ChangeListener interface
	////////////////////////////////////////////////////////////////////

		public void stateChanged(ChangeEvent event)
		{
			if (!getVerticalScrollBar().getValueIsAdjusting())
			{
				Point viewPosition = viewport.getViewPosition();
				int rowHeight = textArea.getRowHeight();
				if (viewPosition.y + viewport.getExtentSize().height <
																	textArea.getLineCount() * rowHeight)
				{
					int y = Math.max(0, viewPosition.y) / rowHeight * rowHeight;
					if (viewPosition.y != y)
					{
						viewPosition.y = y;
						viewport.setViewPosition(viewPosition);
					}
				}
			}
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
