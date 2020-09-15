/*====================================================================*\

ArchiveView.java

Archive view class.

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
import java.awt.Point;

import java.awt.datatransfer.DataFlavor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import uk.blankaspect.common.misc.IFileImporter;
import uk.blankaspect.common.misc.IStringKeyed;

import uk.blankaspect.common.range.IntegerRange;

import uk.blankaspect.common.string.StringUtils;

import uk.blankaspect.common.swing.action.KeyAction;

import uk.blankaspect.common.swing.colour.Colours;

import uk.blankaspect.common.swing.font.FontUtils;

import uk.blankaspect.common.swing.menu.FMenuItem;

import uk.blankaspect.common.swing.text.TextRendering;
import uk.blankaspect.common.swing.text.TextUtils;

import uk.blankaspect.common.swing.transfer.DataImporter;

//----------------------------------------------------------------------


// ARCHIVE VIEW CLASS


class ArchiveView
	extends View
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		int	MIN_NUM_ROWS		= 2;
	public static final		int	MAX_NUM_ROWS		= 256;
	public static final		int	DEFAULT_NUM_ROWS	= 20;

	public static final		int	MIN_COLUMN_WIDTH	= 16;
	public static final		int	MAX_COLUMN_WIDTH	= 1024;

	private static final	int	CELL_VERTICAL_MARGIN	= 1;
	private static final	int	CELL_HORIZONTAL_MARGIN	= 4;
	private static final	int	GRID_LINE_WIDTH			= 1;

	private static final	int	VERTICAL_GAP	= 0;

	// Commands
	private interface Command
	{
		String	SHOW_CONTEXT_MENU	= "showContextMenu";
	}

	private static final	KeyAction.KeyCommandPair[]	KEY_COMMANDS	=
	{
		new KeyAction.KeyCommandPair(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0),
									 Command.SHOW_CONTEXT_MENU)
	};

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// SORTING DIRECTION


	enum SortingDirection
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		ASCENDING   (AppIcon.ARROW_UP),
		DESCENDING  (AppIcon.ARROW_DOWN);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private SortingDirection(ImageIcon icon)
		{
			this.icon = icon;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public SortingDirection getOpposite()
		{
			return ((this == ASCENDING) ? DESCENDING : ASCENDING);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	ImageIcon	icon;

	}

	//==================================================================


	// TABLE COLUMN


	enum Column
		implements IStringKeyed
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		PATH
		(
			"path",
			"Path",
			SwingConstants.LEADING,
			240
		),

		SIZE
		(
			"size",
			"Size",
			SwingConstants.TRAILING,
			96
		),

		TIMESTAMP
		(
			"timestamp",
			"Timestamp",
			SwingConstants.LEADING,
			112
		),

		HASH_VALUE
		(
			"hashValue",
			"Hash value",
			SwingConstants.LEADING,
			400
		);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Column(String key,
					   String text,
					   int    alignment,
					   int    defaultWidth)
		{
			this.key = key;
			this.text = text;
			this.alignment = alignment;
			this.defaultWidth = defaultWidth;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IStringKeyed interface
	////////////////////////////////////////////////////////////////////

		@Override
		public String getKey()
		{
			return key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public String toString()
		{
			return text;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public int getDefaultWidth()
		{
			return defaultWidth;
		}

		//--------------------------------------------------------------

		public int getWidth()
		{
			return AppConfig.INSTANCE.getArchiveViewColumnWidth(this);
		}

		//--------------------------------------------------------------

		public boolean isVisible()
		{
			return (getWidth() > 0);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	key;
		private	String	text;
		private	int		alignment;
		private	int		defaultWidth;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// TABLE CLASS


	public static class Table
		extends JTable
		implements ActionListener, FocusListener, MouseListener
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Table(ArchiveDocument document)
		{
			// Call superclass constructor
			super(document.getTableModel());

			// Set attributes
			setGridColor(Colours.Table.GRID.getColour());
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			setIntercellSpacing(new Dimension());
			AppFont.MAIN.apply(getTableHeader());
			AppFont.MAIN.apply(this);
			int rowHeight = 2 * CELL_VERTICAL_MARGIN + GRID_LINE_WIDTH +
																getFontMetrics(getFont()).getHeight();
			setRowHeight(rowHeight);

			// Initialise columns
			AppConfig config = AppConfig.INSTANCE;
			TableColumnModel columnModel = getColumnModel();
			int width = 0;
			for (int i = 0; i < columnModel.getColumnCount(); i++)
			{
				Column id = document.getColumn(i);
				TableColumn column = columnModel.getColumn(i);
				column.setIdentifier(id);
				width += id.getWidth();
				column.setMinWidth(MIN_COLUMN_WIDTH);
				column.setMaxWidth(MAX_COLUMN_WIDTH);
				column.setPreferredWidth(id.getWidth());
				column.setHeaderValue(id);
				column.setHeaderRenderer(HeaderRenderer.getRenderer(id.alignment));
				column.setCellRenderer(CellRenderer.getRenderer(id.alignment));
			}

			// Set preferred size of viewport
			setPreferredScrollableViewportSize(new Dimension(width, config.getArchiveViewNumRows() *
																							rowHeight));

			// Perform layout
			doLayout();

			// Remove keys from input map
			InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			while (inputMap != null)
			{
				inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));
				inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
				inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK));
				inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));
				inputMap = inputMap.getParent();
			}

			// Add commands to action map
			KeyAction.create(this, JComponent.WHEN_IN_FOCUSED_WINDOW, this, KEY_COMMANDS);

			// Add listeners
			addFocusListener(this);
			addMouseListener(this);
			getTableHeader().addMouseListener(this);
			getSelectionModel().addListSelectionListener(App.INSTANCE.getMainWindow());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ActionListener interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void actionPerformed(ActionEvent event)
		{
			String command = event.getActionCommand();

			if (command.equals(Command.SHOW_CONTEXT_MENU))
				onShowContextMenu();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : FocusListener interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void focusGained(FocusEvent event)
		{
			getTableHeader().repaint();
			repaint();
		}

		//--------------------------------------------------------------

		@Override
		public void focusLost(FocusEvent event)
		{
			getTableHeader().repaint();
			repaint();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : MouseListener interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void mouseClicked(MouseEvent event)
		{
			if (SwingUtilities.isLeftMouseButton(event) && (event.getComponent() == getTableHeader()))
			{
				requestFocusInWindow();

				int index = getTableHeader().columnAtPoint(event.getPoint());
				if (index >= 0)
				{
					Column id = (Column)getColumnModel().getColumn(index).getHeaderValue();
					ArchiveDocument.SortingOrder sortingOrder = ArchiveDocument.getSortingOrder();
					SortingDirection direction = (sortingOrder.key == id)
																	? sortingOrder.direction.getOpposite()
																	: SortingDirection.ASCENDING;
					App.INSTANCE.getArchiveDocument().setSortingOrder(id, direction);
				}
			}
		}

		//--------------------------------------------------------------

		@Override
		public void mouseEntered(MouseEvent event)
		{
			// do nothing
		}

		//--------------------------------------------------------------

		@Override
		public void mouseExited(MouseEvent event)
		{
			// do nothing
		}

		//--------------------------------------------------------------

		@Override
		public void mousePressed(MouseEvent event)
		{
			showContextMenu(event);
		}

		//--------------------------------------------------------------

		@Override
		public void mouseReleased(MouseEvent event)
		{
			showContextMenu(event);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public Dimension getMinimumSize()
		{
			return new Dimension(getColumnCount() * MIN_COLUMN_WIDTH, MIN_NUM_ROWS * getRowHeight());
		}

		//--------------------------------------------------------------

		@Override
		public TransferHandler getTransferHandler()
		{
			MainWindow window = App.INSTANCE.getMainWindow();
			return ((window == null) ? null
									 : ((JComponent)window.getContentPane()).getTransferHandler());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public void invertSelection()
		{
			int index = 0;
			int startIndex = -1;
			List<IntegerRange> ranges = new ArrayList<>();
			while (index < getRowCount())
			{
				if (isRowSelected(index))
				{
					if (startIndex >= 0)
					{
						ranges.add(new IntegerRange(startIndex, index));
						startIndex = -1;
					}
				}
				else
				{
					if (startIndex < 0)
						startIndex = index;
				}
				++index;
			}
			if (startIndex >= 0)
				ranges.add(new IntegerRange(startIndex, index));

			ListSelectionModel selectionModel = getSelectionModel();
			selectionModel.clearSelection();
			if (!ranges.isEmpty())
			{
				selectionModel.setValueIsAdjusting(true);
				for (IntegerRange range : ranges)
					selectionModel.addSelectionInterval(range.lowerBound, range.upperBound - 1);
				selectionModel.setValueIsAdjusting(false);
			}
		}

		//--------------------------------------------------------------

		private void showContextMenu(MouseEvent event)
		{
			if ((event == null) || event.isPopupTrigger())
			{
				// Create context menu
				if (contextMenu == null)
				{
					contextMenu = new JPopupMenu();

					contextMenu.add(new FMenuItem(ArchiveDocument.Command.CHOOSE_ARCHIVE_DIRECTORY));

					contextMenu.addSeparator();

					contextMenu.add(new FMenuItem(ArchiveDocument.Command.ADD_FILES));
					contextMenu.add(new FMenuItem(ArchiveDocument.Command.EXTRACT_FILES));
					contextMenu.add(new FMenuItem(ArchiveDocument.Command.VALIDATE_FILES));
					contextMenu.add(new FMenuItem(ArchiveDocument.Command.DELETE_FILES));

					contextMenu.addSeparator();

					contextMenu.add(new FMenuItem(ArchiveDocument.Command.DISPLAY_FILE_LIST));
					contextMenu.add(new FMenuItem(ArchiveDocument.Command.DISPLAY_FILE_MAP));

					contextMenu.addSeparator();

					contextMenu.add(new FMenuItem(ArchiveDocument.Command.SET_KEY));
					contextMenu.add(new FMenuItem(ArchiveDocument.Command.CLEAR_KEY));
				}

				// Update commands for menu items
				App.INSTANCE.getArchiveDocument().updateCommands();

				// Display menu
				if (event == null)
					contextMenu.show(getTableHeader(), 0, 0);
				else
					contextMenu.show(event.getComponent(), event.getX(), event.getY());
			}
		}

		//--------------------------------------------------------------

		private void onShowContextMenu()
		{
			showContextMenu(null);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	JPopupMenu	contextMenu;

	}

	//==================================================================


	// HEADER RENDERER CLASS


	private static class HeaderRenderer
		extends JComponent
		implements TableCellRenderer
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	VERTICAL_MARGIN		= 2;
		private static final	int	SORTING_ICON_WIDTH	= SortingDirection.ASCENDING.icon.getIconWidth();

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private HeaderRenderer()
		{
			AppFont.MAIN.apply(this);
			setForeground(Colours.Table.FOREGROUND.getColour());
			setOpaque(true);
			setFocusable(false);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		public static HeaderRenderer getRenderer(int alignment)
		{
			HeaderRenderer renderer = null;
			switch (alignment)
			{
				case SwingConstants.LEADING:
					if (leadingRenderer == null)
						leadingRenderer = new HeaderRenderer();
					renderer = leadingRenderer;
					break;

				case SwingConstants.TRAILING:
					if (trailingRenderer == null)
						trailingRenderer = new HeaderRenderer();
					renderer = trailingRenderer;
					break;
			}
			return renderer;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : TableCellRenderer interface
	////////////////////////////////////////////////////////////////////

		@Override
		public Component getTableCellRendererComponent(JTable  table,
													   Object  value,
													   boolean isSelected,
													   boolean hasFocus,
													   int     row,
													   int     column)
		{
			id = (Column)value;
			setBackground(table.isFocusOwner() ? Colours.Table.FOCUSED_HEADER_BACKGROUND1.getColour()
											   : Colours.Table.HEADER_BACKGROUND1.getColour());
			borderColour = table.isFocusOwner() ? Colours.Table.FOCUSED_HEADER_BORDER1.getColour()
												: Colours.Table.HEADER_BORDER1.getColour();
			return this;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public Dimension getPreferredSize()
		{
			FontMetrics fontMetrics = getFontMetrics(getFont());
			int width = GRID_LINE_WIDTH + 2 * CELL_HORIZONTAL_MARGIN + SORTING_ICON_WIDTH +
																		fontMetrics.stringWidth(id.text);
			int height = 2 * GRID_LINE_WIDTH + 2 * VERTICAL_MARGIN + fontMetrics.getAscent() +
																				fontMetrics.getDescent();
			return new Dimension(width, height);
		}

		//--------------------------------------------------------------

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

			// Get limited-width text and x coordinate
			--width;
			FontMetrics fontMetrics = gr.getFontMetrics();
			TextUtils.RemovalMode removalMode = (id.alignment == SwingConstants.TRAILING)
																		? TextUtils.RemovalMode.START
																		: TextUtils.RemovalMode.END;
			String str = TextUtils.getLimitedWidthString(id.text, fontMetrics,
														 width - 2 * CELL_HORIZONTAL_MARGIN -
																						SORTING_ICON_WIDTH,
														 removalMode);
			int x = (id.alignment == SwingConstants.LEADING)
										? CELL_HORIZONTAL_MARGIN
										: width - CELL_HORIZONTAL_MARGIN - fontMetrics.stringWidth(str);

			// Draw text
			gr.setColor(Colours.Table.FOREGROUND.getColour());
			gr.drawString(str, x, FontUtils.getBaselineOffset(height, fontMetrics));

			// Fill horizontal margin
			--height;
			gr.setColor(getBackground());
			gr.fillRect((id.alignment == SwingConstants.LEADING) ? width - CELL_HORIZONTAL_MARGIN : 0, 0,
						CELL_HORIZONTAL_MARGIN, height);

			// Draw sorting indicator
			ArchiveDocument.SortingOrder sortingOrder = ArchiveDocument.getSortingOrder();
			if (sortingOrder.key == id)
			{
				x = (id.alignment == SwingConstants.LEADING)
													? width - CELL_HORIZONTAL_MARGIN - SORTING_ICON_WIDTH
													: CELL_HORIZONTAL_MARGIN;
				gr.drawImage(sortingOrder.direction.icon.getImage(), x,
							 (height - sortingOrder.direction.icon.getIconHeight()) / 2, null);
			}

			// Draw border
			gr.setColor(borderColour);
			gr.drawLine(0, 0, width, 0);
			gr.drawLine(width, 0, width, height);
			gr.drawLine(0, height, width, height);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class variables
	////////////////////////////////////////////////////////////////////

		private static	HeaderRenderer	leadingRenderer;
		private static	HeaderRenderer	trailingRenderer;

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	Column	id;
		private	Color	borderColour;

	}

	//==================================================================


	// CELL RENDERER CLASS


	private static class CellRenderer
		extends JComponent
		implements TableCellRenderer
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CellRenderer(int alignment)
		{
			this.alignment = alignment;
			AppFont.MAIN.apply(this);
			setForeground(Colours.Table.FOREGROUND.getColour());
			setOpaque(true);
			setFocusable(false);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		public static CellRenderer getRenderer(int alignment)
		{
			CellRenderer renderer = null;
			switch (alignment)
			{
				case SwingConstants.LEADING:
					if (leadingRenderer == null)
						leadingRenderer = new CellRenderer(alignment);
					renderer = leadingRenderer;
					break;

				case SwingConstants.TRAILING:
					if (trailingRenderer == null)
						trailingRenderer = new CellRenderer(alignment);
					renderer = trailingRenderer;
					break;
			}
			return renderer;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : TableCellRenderer interface
	////////////////////////////////////////////////////////////////////

		@Override
		public Component getTableCellRendererComponent(JTable  table,
													   Object  value,
													   boolean isSelected,
													   boolean hasFocus,
													   int     row,
													   int     column)
		{
			text = (value == null) ? null : value.toString();
			this.hasFocus = hasFocus;
			setBackground(isSelected ? table.isFocusOwner()
												? Colours.Table.FOCUSED_SELECTION_BACKGROUND.getColour()
												: Colours.Table.SELECTION_BACKGROUND.getColour()
									 : Colours.Table.BACKGROUND.getColour());
			return this;
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

			// Draw text
			--width;
			if ((text != null) && !text.isEmpty())
			{
				// Set rendering hints for text antialiasing and fractional metrics
				TextRendering.setHints((Graphics2D)gr);

				// Get limited-width text and x coordinate
				FontMetrics fontMetrics = gr.getFontMetrics();
				TextUtils.RemovalMode removalMode = (alignment == SwingConstants.TRAILING)
																		? TextUtils.RemovalMode.START
																		: TextUtils.RemovalMode.END;
				String str = TextUtils.getLimitedWidthString(text, fontMetrics,
															 width - 2 * CELL_HORIZONTAL_MARGIN,
															 removalMode);
				int x = (alignment == SwingConstants.LEADING)
										? CELL_HORIZONTAL_MARGIN
										: width - CELL_HORIZONTAL_MARGIN - fontMetrics.stringWidth(str);

				// Draw text
				gr.setColor(getForeground());
				gr.drawString(str, x, FontUtils.getBaselineOffset(height, fontMetrics));
			}

			// Fill horizontal margin
			--height;
			gr.setColor(getBackground());
			gr.fillRect((alignment == SwingConstants.LEADING) ? width - CELL_HORIZONTAL_MARGIN : 0,
						GRID_LINE_WIDTH, CELL_HORIZONTAL_MARGIN, height - 1);

			// Draw grid
			gr.setColor(Colours.Table.GRID.getColour());
			gr.drawLine(width, 0, width, height);
			gr.drawLine(0, height, width, height);

			// Draw focused border
			if (hasFocus)
			{
				gr.setColor(Colours.Table.FOCUSED_CELL_BORDER.getColour());
				gr.drawRect(0, 0, width - 1, height - 1);
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class variables
	////////////////////////////////////////////////////////////////////

		private static	CellRenderer	leadingRenderer;
		private static	CellRenderer	trailingRenderer;

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int		alignment;
		private	String	text;
		private	boolean	hasFocus;

	}

	//==================================================================


	// TABLE SCROLL PANE CLASS


	private static class TableScrollPane
		extends JScrollPane
		implements ChangeListener, MouseListener
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private TableScrollPane(ArchiveDocument document)
		{
			// Call superclass constructor
			super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

			// Set component attributes
			setBorder(null);

			// Create table and set it as view
			table = new Table(document);
			setViewportView(table);

			// Add listeners
			getVerticalScrollBar().getModel().addChangeListener(this);
			addMouseListener(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ChangeListener interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void stateChanged(ChangeEvent event)
		{
			if (!getVerticalScrollBar().getValueIsAdjusting())
			{
				Point viewPosition = viewport.getViewPosition();
				int rowHeight = table.getRowHeight();
				if (viewPosition.y + viewport.getExtentSize().height < table.getRowCount() * rowHeight)
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

	////////////////////////////////////////////////////////////////////
	//  Instance methods : MouseListener interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void mouseClicked(MouseEvent event)
		{
			// do nothing
		}

		//--------------------------------------------------------------

		@Override
		public void mouseEntered(MouseEvent event)
		{
			// do nothing
		}

		//--------------------------------------------------------------

		@Override
		public void mouseExited(MouseEvent event)
		{
			// do nothing
		}

		//--------------------------------------------------------------

		@Override
		public void mousePressed(MouseEvent event)
		{
			table.showContextMenu(event);
		}

		//--------------------------------------------------------------

		@Override
		public void mouseReleased(MouseEvent event)
		{
			table.showContextMenu(event);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	Table	table;

	}

	//==================================================================


	// ARCHIVE DIRECTORY PANEL CLASS


	private static class ArchiveDirectoryPanel
		extends JComponent
		implements ActionListener, IFileImporter, MouseListener
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	VERTICAL_MARGIN		= 2;
		private static final	int	HORIZONTAL_MARGIN	= 5;
		private static final	int	ICON_TEXT_GAP		= 5;

		private static final	Color	TEXT_COLOUR			= Color.BLACK;
		private static final	Color	BACKGROUND_COLOUR	= new Color(248, 244, 224);

		private static final	String	PASTE_STR	= "Paste";

		// Commands
		private interface Command
		{
			String	PASTE	= "paste";
		}

	////////////////////////////////////////////////////////////////////
	//  Member classes : inner classes
	////////////////////////////////////////////////////////////////////


		// COMMAND ACTION CLASS


		protected class CommandAction
			extends AbstractAction
		{

		////////////////////////////////////////////////////////////////
		//  Constructors
		////////////////////////////////////////////////////////////////

			protected CommandAction(String command,
									String text)
			{
				super(text);
				putValue(Action.ACTION_COMMAND_KEY, command);
			}

			//----------------------------------------------------------

		////////////////////////////////////////////////////////////////
		//  Instance methods : ActionListener interface
		////////////////////////////////////////////////////////////////

			@Override
			public void actionPerformed(ActionEvent event)
			{
				ArchiveDirectoryPanel.this.actionPerformed(event);
			}

			//----------------------------------------------------------

		}

		//==============================================================

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ArchiveDirectoryPanel(File directory)
		{
			// Set font
			AppFont.MAIN.apply(this);
			FontMetrics fontMetrics = getFontMetrics(getFont());

			// Initialise instance variables
			if (directory != null)
				pathname = Utils.getPathname(directory);
			preferredWidth = 2 * HORIZONTAL_MARGIN + AppIcon.DIRECTORY.getIconWidth();
			if (pathname != null)
				preferredWidth += ICON_TEXT_GAP + fontMetrics.stringWidth(pathname);
			preferredHeight = 2 * VERTICAL_MARGIN + Math.max(AppIcon.DIRECTORY.getIconHeight(),
															 fontMetrics.getAscent() + fontMetrics.getDescent());
			pasteAction = new CommandAction(Command.PASTE, PASTE_STR);

			// Set attributes
			setOpaque(true);
			setFocusable(false);
			setTransferHandler(FileTransferHandler.INSTANCE);

			// Add listeners
			addMouseListener(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ActionListener interface
	////////////////////////////////////////////////////////////////////

		public void actionPerformed(ActionEvent event)
		{
			String command = event.getActionCommand();

			if (command.equals(Command.PASTE))
				onPaste();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IFileImporter interface
	////////////////////////////////////////////////////////////////////

		@Override
		public boolean canImportFiles()
		{
			return App.INSTANCE.getArchiveDocument().isEmpty();
		}

		//--------------------------------------------------------------

		@Override
		public boolean canImportMultipleFiles()
		{
			return false;
		}

		//--------------------------------------------------------------

		@Override
		public void importFiles(List<File> files)
		{
			App.INSTANCE.setArchiveDirectory(files.get(0));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : MouseListener interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void mouseClicked(MouseEvent event)
		{
			if (SwingUtilities.isLeftMouseButton(event))
			{
				ArchiveDocument document = App.INSTANCE.getArchiveDocument();
				if ((document != null) && document.isEmpty())
					document.executeCommand(ArchiveDocument.Command.CHOOSE_ARCHIVE_DIRECTORY);
			}
		}

		//--------------------------------------------------------------

		@Override
		public void mouseEntered(MouseEvent event)
		{
			// do nothing
		}

		//--------------------------------------------------------------

		@Override
		public void mouseExited(MouseEvent event)
		{
			// do nothing
		}

		//--------------------------------------------------------------

		@Override
		public void mousePressed(MouseEvent event)
		{
			showContextMenu(event);
		}

		//--------------------------------------------------------------

		@Override
		public void mouseReleased(MouseEvent event)
		{
			showContextMenu(event);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(preferredWidth, preferredHeight);
		}

		//--------------------------------------------------------------

		@Override
		protected void paintComponent(Graphics gr)
		{
			// Create copy of graphics context
			gr = gr.create();

			// Get dimensions
			int width = getWidth();
			int height = getHeight();

			// Draw background
			gr.setColor(BACKGROUND_COLOUR);
			gr.fillRect(0, 0, width, height);

			// Draw icon
			gr.drawImage(AppIcon.DIRECTORY.getImage(), HORIZONTAL_MARGIN,
						 (height - AppIcon.DIRECTORY.getIconHeight()) / 2, null);

			// Draw text
			if (pathname != null)
			{
				// Set rendering hints for text antialiasing and fractional metrics
				TextRendering.setHints((Graphics2D)gr);

				// Draw text
				FontMetrics fontMetrics = gr.getFontMetrics();
				int x = HORIZONTAL_MARGIN + AppIcon.DIRECTORY.getIconWidth() + ICON_TEXT_GAP;
				String str = TextUtils.getLimitedWidthPathname(pathname, fontMetrics, width - x - HORIZONTAL_MARGIN);
				gr.setColor(TEXT_COLOUR);
				gr.drawString(str, x, FontUtils.getBaselineOffset(height, fontMetrics));
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void setPathname(String pathname)
		{
			if (!StringUtils.equal(this.pathname, pathname))
			{
				this.pathname = pathname;
				preferredWidth = 2 * HORIZONTAL_MARGIN;
				if (pathname != null)
					preferredWidth += getFontMetrics(getFont()).stringWidth(pathname);
				revalidate();
				repaint();
			}
		}

		//--------------------------------------------------------------

		private void showContextMenu(MouseEvent event)
		{
			if (event.isPopupTrigger())
			{
				// Create context menu
				if (contextMenu == null)
				{
					contextMenu = new JPopupMenu();
					contextMenu.add(new FMenuItem(ArchiveDocument.Command.CHOOSE_ARCHIVE_DIRECTORY));
					contextMenu.add(new FMenuItem(pasteAction));
				}

				// Update actions for menu items
				try
				{
					DataFlavor[] flavours = getToolkit().getSystemClipboard().getAvailableDataFlavors();
					pasteAction.setEnabled(canImportFiles() && DataImporter.isFileList(flavours));
				}
				catch (Exception e)
				{
					// ignore
				}

				// Display menu
				contextMenu.show(event.getComponent(), event.getX(), event.getY());
			}
		}

		//--------------------------------------------------------------

		private void onPaste()
		{
			try
			{
				List<File> files = DataImporter.getFiles(getToolkit().getSystemClipboard().getContents(null));
				if (files != null)
					importFiles(files);
			}
			catch (Exception e)
			{
				// ignore
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int				preferredWidth;
		private	int				preferredHeight;
		private	String			pathname;
		private	CommandAction	pasteAction;
		private	JPopupMenu		contextMenu;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public ArchiveView(ArchiveDocument document)
	{
		// Lay out components explicitly
		setLayout(null);

		// Panel: archive directory
		archiveDirectoryPanel = new ArchiveDirectoryPanel(document.getArchiveDirectory());
		add(archiveDirectoryPanel);

		// Scroll pane: table
		tableScrollPane = new TableScrollPane(document);
		add(tableScrollPane);

		// Set view in document
		document.setView(this);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public Dimension getPreferredSize()
	{
		int width = tableScrollPane.getPreferredSize().width;
		int height = -VERTICAL_GAP;
		for (Component component : getComponents())
			height += component.getPreferredSize().height + VERTICAL_GAP;
		return new Dimension(width, height);
	}

	//------------------------------------------------------------------

	@Override
	public void doLayout()
	{
		int width = getWidth();
		int archiveDirectoryPanelHeight = archiveDirectoryPanel.getPreferredSize().height;

		int y = 0;
		archiveDirectoryPanel.setBounds(0, y, width, archiveDirectoryPanelHeight);

		y += archiveDirectoryPanelHeight + VERTICAL_GAP;
		tableScrollPane.setBounds(0, y, width, getHeight() - y);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public Table getTable()
	{
		return tableScrollPane.table;
	}

	//------------------------------------------------------------------

	public void setArchiveDirectory(File directory)
	{
		archiveDirectoryPanel.setPathname(Utils.getPathname(directory));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	ArchiveDirectoryPanel	archiveDirectoryPanel;
	private	TableScrollPane			tableScrollPane;

}

//----------------------------------------------------------------------
