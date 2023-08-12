/*====================================================================*\

CipherTable.java

Cipher table class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import uk.blankaspect.common.crypto.FortunaCipher;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.colour.Colours;

import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.misc.GuiConstants;
import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.text.TextRendering;

//----------------------------------------------------------------------


// CIPHER TABLE CLASS


class CipherTable
	extends JComponent
	implements ActionListener, FocusListener, MouseListener, MouseMotionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		NUM_ROWS	= FortunaCipher.values().length + 1;
	private static final	int		NUM_COLUMNS	= Column.values().length;

	private static final	int		CELL_VERTICAL_MARGIN	= 1;
	private static final	int		CELL_HORIZONTAL_MARGIN	= 5;
	private static final	int		GRID_LINE_WIDTH			= 1;

	private static final	Color	BACKGROUND_COLOUR				= Colours.Table.BACKGROUND.getColour();
	private static final	Color	HIGHLIGHTED_BACKGROUND_COLOUR1	= new Color(255, 248, 192);
	private static final	Color	HIGHLIGHTED_BACKGROUND_COLOUR2	= Colours.FOCUSED_SELECTION_BACKGROUND;

	private static final	Color	TEXT_COLOUR	= Colours.Table.FOREGROUND.getColour();

	private static final	Color	HEADER_BACKGROUND_COLOUR			= Colours.Table.HEADER_BACKGROUND1.getColour();
	private static final	Color	FOCUSED_HEADER_BACKGROUND_COLOUR	= Colours.Table.FOCUSED_HEADER_BACKGROUND1.getColour();

	private static final	Color	FOCUSED_BORDER_COLOUR1	= Color.WHITE;
	private static final	Color	FOCUSED_BORDER_COLOUR2	= Color.BLACK;

	private static final	Color	GRID_COLOUR	= new Color(184, 192, 184);

	private enum CellState
	{
		NOT_OVER,
		OVER,
		PRESSED
	}

	private static final	ImageIcon	TICK_ICON	= new ImageIcon(ImageData.TICK);

	// Commands
	private interface Command
	{
		String	SELECT_UP_UNIT		= "selectUpUnit";
		String	SELECT_DOWN_UNIT	= "selectDownUnit";
		String	SELECT_UP_MAX		= "selectUpMax";
		String	SELECT_DOWN_MAX		= "selectDownMax";
		String	SELECT_LEFT_UNIT	= "selectLeftUnit";
		String	SELECT_RIGHT_UNIT	= "selectRightUnit";
		String	SELECT_LEFT_MAX		= "selectLeftMax";
		String	SELECT_RIGHT_MAX	= "selectRightMax";
		String	TOGGLE_CELL			= "toggleCell";
	}

	private static final	KeyAction.KeyCommandPair[]	KEY_COMMANDS	=
	{
		new KeyAction.KeyCommandPair
		(
			KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
			Command.SELECT_UP_UNIT
		),
		new KeyAction.KeyCommandPair
		(
			KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
			Command.SELECT_DOWN_UNIT
		),
		new KeyAction.KeyCommandPair
		(
			KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK),
			Command.SELECT_UP_MAX
		),
		new KeyAction.KeyCommandPair
		(
			KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_DOWN_MASK),
			Command.SELECT_DOWN_MAX
		),
		new KeyAction.KeyCommandPair
		(
			KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
			Command.SELECT_LEFT_UNIT
		),
		new KeyAction.KeyCommandPair
		(
			KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
			Command.SELECT_RIGHT_UNIT
		),
		new KeyAction.KeyCommandPair
		(
			KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),
			Command.SELECT_LEFT_MAX
		),
		new KeyAction.KeyCommandPair
		(
			KeyStroke.getKeyStroke(KeyEvent.VK_END, 0),
			Command.SELECT_RIGHT_MAX
		),
		new KeyAction.KeyCommandPair
		(
			KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
			Command.TOGGLE_CELL
		)
	};

	// Image data
	private interface ImageData
	{
		byte[]	TICK	=
		{
			(byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A,
			(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0D, (byte)0x49, (byte)0x48, (byte)0x44, (byte)0x52,
			(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0A, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0A,
			(byte)0x08, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x8D, (byte)0x32, (byte)0xCF,
			(byte)0xBD, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xB3, (byte)0x49, (byte)0x44, (byte)0x41,
			(byte)0x54, (byte)0x78, (byte)0xDA, (byte)0x63, (byte)0xF8, (byte)0xFF, (byte)0xFF, (byte)0x3F,
			(byte)0x03, (byte)0x2E, (byte)0x1C, (byte)0x5F, (byte)0xD0, (byte)0x6F, (byte)0x9D, (byte)0x50,
			(byte)0x30, (byte)0xF1, (byte)0x26, (byte)0x10, (byte)0xF7, (byte)0xE2, (byte)0x54, (byte)0x14,
			(byte)0xDA, (byte)0xD0, (byte)0xC0, (byte)0x96, (byte)0x90, (byte)0x3F, (byte)0xE1, (byte)0x7A,
			(byte)0x42, (byte)0xC1, (byte)0x84, (byte)0xFF, (byte)0x40, (byte)0x7C, (byte)0x03, (byte)0xA7,
			(byte)0xC2, (byte)0xC4, (byte)0x82, (byte)0xFE, (byte)0x4A, (byte)0xA8, (byte)0x22, (byte)0x20,
			(byte)0xEE, (byte)0x2F, (byte)0xC2, (byte)0xAA, (byte)0x28, (byte)0xB9, (byte)0x78, (byte)0x8A,
			(byte)0x3C, (byte)0x50, (byte)0xC1, (byte)0x57, (byte)0xA8, (byte)0xC2, (byte)0x8B, (byte)0x0D,
			(byte)0x0D, (byte)0x0D, (byte)0x2C, (byte)0x0C, (byte)0x29, (byte)0xF9, (byte)0x13, (byte)0xC4,
			(byte)0x93, (byte)0x0A, (byte)0xFB, (byte)0x5D, (byte)0x42, (byte)0x43, (byte)0x57, (byte)0x31,
			(byte)0x23, (byte)0x4C, (byte)0x9B, (byte)0xB0, (byte)0x11, (byte)0xAA, (byte)0xE8, (byte)0x5F,
			(byte)0x62, (byte)0xDE, (byte)0x44, (byte)0x2B, (byte)0x90, (byte)0x18, (byte)0x03, (byte)0xD0,
			(byte)0xD8, (byte)0x0B, (byte)0x20, (byte)0xC1, (byte)0xF8, (byte)0x82, (byte)0x89, (byte)0xAB,
			(byte)0x41, (byte)0x3A, (byte)0x13, (byte)0x0A, (byte)0xFB, (byte)0x7C, (byte)0xE1, (byte)0x56,
			(byte)0x16, (byte)0x4E, (byte)0x98, (byte)0x03, (byte)0xD3, (byte)0x0C, (byte)0x52, (byte)0xB8,
			(byte)0x13, (byte)0x49, (byte)0x62, (byte)0x0D, (byte)0x90, (byte)0xBE, (byte)0x0F, (byte)0xE5,
			(byte)0xBF, (byte)0x89, (byte)0xCB, (byte)0x9E, (byte)0x22, (byte)0x0C, (byte)0x57, (byte)0x98,
			(byte)0x56, (byte)0xDE, (byte)0xC1, (byte)0x0F, (byte)0x14, (byte)0x3C, (byte)0x81, (byte)0x70,
			(byte)0x38, (byte)0xDC, (byte)0x03, (byte)0x49, (byte)0xC8, (byte)0xEE, (byte)0x06, (byte)0x13,
			(byte)0x58, (byte)0x14, (byte)0x1F, (byte)0x01, (byte)0xCA, (byte)0x30, (byte)0x62, (byte)0x28,
			(byte)0x44, (byte)0x52, (byte)0xBC, (byte)0x36, (byte)0xBE, (byte)0x60, (byte)0xC2, (byte)0xE5,
			(byte)0xA4, (byte)0xFC, (byte)0x09, (byte)0xEA, (byte)0xE8, (byte)0x21, (byte)0x01, (byte)0x00,
			(byte)0x8C, (byte)0xFC, (byte)0xF3, (byte)0xF3, (byte)0x1D, (byte)0xDF, (byte)0xB3, (byte)0xE7,
			(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x49, (byte)0x45, (byte)0x4E, (byte)0x44,
			(byte)0xAE, (byte)0x42, (byte)0x60, (byte)0x82
		};
	}

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// TABLE COLUMN


	private enum Column
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		CIPHER
		(
			"Cipher"
		),

		ALLOWED
		(
			"Allowed"
		),

		PREFERRED
		(
			"Preferred"
		);

		//--------------------------------------------------------------

		private static final	Set<Column>	BOOLEAN_COLUMNS	= EnumSet.of(ALLOWED, PREFERRED);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Column(String text)
		{
			this.text = text;
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
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	text;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CELL LOCATOR CLASS


	private static class CellLocator
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CellLocator(Column        column,
							FortunaCipher cipher)
		{
			this.column = column;
			this.cipher = cipher;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof CellLocator)
			{
				CellLocator locator = (CellLocator)obj;
				return ((column == locator.column) && (cipher == locator.cipher));
			}
			return false;
		}

		//--------------------------------------------------------------

		@Override
		public int hashCode()
		{
			return ((column.ordinal() << 16) | cipher.ordinal());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	Column			column;
		private	FortunaCipher	cipher;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public CipherTable(Set<FortunaCipher> allowedCiphers,
					   FortunaCipher      preferredCipher)
	{
		// Set font
		AppFont.MAIN.apply(this);

		// Initialise instance variables
		this.allowedCiphers = EnumSet.copyOf(allowedCiphers);
		this.preferredCipher = preferredCipher;
		cellState = CellState.NOT_OVER;
		selectedCell = new CellLocator(Column.ALLOWED, FortunaCipher.AES256);

		FontMetrics fontMetrics = getFontMetrics(getFont());
		rowHeight = 2 * CELL_VERTICAL_MARGIN + GRID_LINE_WIDTH +
											Math.max(fontMetrics.getAscent() + fontMetrics.getDescent(),
													 TICK_ICON.getIconHeight());
		columnWidths = new EnumMap<>(Column.class);

		int columnWidth = fontMetrics.stringWidth(Column.CIPHER.text);
		for (FortunaCipher cipher : FortunaCipher.values())
		{
			int width = fontMetrics.stringWidth(cipher.toString());
			if (columnWidth < width)
				columnWidth = width;
		}
		columnWidths.put(Column.CIPHER, 2 * CELL_HORIZONTAL_MARGIN + GRID_LINE_WIDTH + columnWidth);

		columnWidth = 0;
		for (Column column : Column.BOOLEAN_COLUMNS)
		{
			int width = Math.max(fontMetrics.stringWidth(column.text.toString()),
								 TICK_ICON.getIconWidth());
			if (columnWidth < width)
				columnWidth = width;
		}
		for (Column column : Column.BOOLEAN_COLUMNS)
			columnWidths.put(column, 2 * CELL_HORIZONTAL_MARGIN + GRID_LINE_WIDTH + columnWidth);

		// Set attributes
		setOpaque(true);
		setFocusable(true);

		// Add commands to action map
		KeyAction.create(this, JComponent.WHEN_FOCUSED, this, KEY_COMMANDS);

		// Add listeners
		addFocusListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();

		if (command.equals(Command.SELECT_UP_UNIT))
			onSelectUpUnit();

		else if (command.equals(Command.SELECT_DOWN_UNIT))
			onSelectDownUnit();

		else if (command.equals(Command.SELECT_UP_MAX))
			onSelectUpMax();

		else if (command.equals(Command.SELECT_DOWN_MAX))
			onSelectDownMax();

		else if (command.equals(Command.SELECT_LEFT_UNIT))
			onSelectLeftUnit();

		else if (command.equals(Command.SELECT_RIGHT_UNIT))
			onSelectRightUnit();

		else if (command.equals(Command.SELECT_LEFT_MAX))
			onSelectLeftMax();

		else if (command.equals(Command.SELECT_RIGHT_MAX))
			onSelectRightMax();

		else if (command.equals(Command.TOGGLE_CELL))
			onToggleCell();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : FocusListener interface
////////////////////////////////////////////////////////////////////////

	public void focusGained(FocusEvent event)
	{
		repaint();
	}

	//------------------------------------------------------------------

	public void focusLost(FocusEvent event)
	{
		repaint();
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
		updateCellState(event, false);
	}

	//------------------------------------------------------------------

	public void mouseExited(MouseEvent event)
	{
		updateCellState(event, false);
	}

	//------------------------------------------------------------------

	public void mousePressed(MouseEvent event)
	{
		if (SwingUtilities.isLeftMouseButton(event))
			requestFocusInWindow();

		updateCellState(event, true);
	}

	//------------------------------------------------------------------

	public void mouseReleased(MouseEvent event)
	{
		boolean pressed = (cellState == CellState.PRESSED);
		CellLocator cell = activeCell;
		updateCellState(event, false);
		if (SwingUtilities.isLeftMouseButton(event) && (cellState == CellState.OVER) && pressed &&
			 cell.equals(activeCell))
		{
			onToggleCell();
			repaint();
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MouseMotionListener interface
////////////////////////////////////////////////////////////////////////

	public void mouseDragged(MouseEvent event)
	{
		updateCellState(event, true);
	}

	//------------------------------------------------------------------

	public void mouseMoved(MouseEvent event)
	{
		updateCellState(event, false);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public Dimension getPreferredSize()
	{
		int width = 0;
		for (Column column : columnWidths.keySet())
			width += columnWidths.get(column);
		return new Dimension(width + GRID_LINE_WIDTH, NUM_ROWS * rowHeight + GRID_LINE_WIDTH);
	}

	//------------------------------------------------------------------

	@Override
	protected void paintComponent(Graphics gr)
	{
		// Create copy of graphics context
		Graphics2D gr2d = GuiUtils.copyGraphicsContext(gr);

		// Get dimensions
		int width = getWidth();
		int height = getHeight();

		// Draw background of header
		gr2d.setColor(isFocusOwner() ? FOCUSED_HEADER_BACKGROUND_COLOUR : HEADER_BACKGROUND_COLOUR);
		gr2d.fillRect(0, 0, width, rowHeight);

		// Draw background of remaining rows
		gr2d.setColor(BACKGROUND_COLOUR);
		gr2d.fillRect(0, rowHeight, width, height - rowHeight);

		// Draw vertical grid lines
		int x = 0;
		int y1 = 0;
		int y2 = height - 1;
		gr2d.setColor(GRID_COLOUR);
		for (Column column : columnWidths.keySet())
		{
			gr2d.drawLine(x, y1, x, y2);
			x += columnWidths.get(column);
		}
		gr2d.drawLine(x, y1, x, y2);

		// Draw horizontal grid lines
		int x1 = 0;
		int x2 = width - 1;
		int y = 0;
		for (int i = 0; i <= NUM_ROWS; i++)
		{
			gr2d.drawLine(x1, y, x2, y);
			y += rowHeight;
		}

		// Set rendering hints for text antialiasing and fractional metrics
		TextRendering.setHints(gr2d);

		// Draw header text
		FontMetrics fontMetrics = gr2d.getFontMetrics();
		x = GRID_LINE_WIDTH + CELL_HORIZONTAL_MARGIN;
		y = GRID_LINE_WIDTH + FontUtils.getBaselineOffset(rowHeight - GRID_LINE_WIDTH, fontMetrics);
		gr2d.setColor(TEXT_COLOUR);
		for (Column column : columnWidths.keySet())
		{
			gr2d.drawString(column.text, x, y);
			x += columnWidths.get(column);
		}

		// Draw cipher colummn text
		int columnX = 0;
		int columnWidth = columnWidths.get(Column.CIPHER);
		x = columnX + GRID_LINE_WIDTH + CELL_HORIZONTAL_MARGIN;
		y = GRID_LINE_WIDTH + FontUtils.getBaselineOffset(rowHeight - GRID_LINE_WIDTH, fontMetrics);
		for (FortunaCipher cipher : FortunaCipher.values())
		{
			y += rowHeight;
			gr2d.drawString(cipher.toString(), x, y);
		}
		columnX += columnWidth;

		// Draw cells
		for (Column column : Column.BOOLEAN_COLUMNS)
		{
			for (FortunaCipher cipher : FortunaCipher.values())
			{
				// Draw background
				CellLocator cell = new CellLocator(column, cipher);
				Rectangle bounds = getCellBounds(column, cipher);
				Color colour = null;
				if (cell.equals(activeCell))
				{
					switch (cellState)
					{
						case NOT_OVER:
							// do nothing
							break;

						case OVER:
							colour = HIGHLIGHTED_BACKGROUND_COLOUR1;
							break;

						case PRESSED:
							colour = HIGHLIGHTED_BACKGROUND_COLOUR2;
							break;
					}
					if (colour != null)
					{
						gr2d.setColor(colour);
						gr2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
					}
				}

				// Draw icon
				boolean selected = false;
				switch (column)
				{
					case CIPHER:
						// do nothing
						break;

					case ALLOWED:
						selected = allowedCiphers.contains(cipher);
						break;

					case PREFERRED:
						selected = (cipher == preferredCipher);
						break;
				}
				if (selected)
				{
					x = bounds.x + (bounds.width - TICK_ICON.getIconWidth()) / 2;
					y = bounds.y + (bounds.height - TICK_ICON.getIconHeight()) / 2;
					gr2d.drawImage(TICK_ICON.getImage(), x, y, null);
				}
			}
		}

		// Draw border of selected cell
		if (isFocusOwner())
		{
			Rectangle bounds = getCellBounds(selectedCell.column, selectedCell.cipher);
			gr2d.setColor(FOCUSED_BORDER_COLOUR1);
			gr2d.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);

			gr2d.setStroke(GuiConstants.BASIC_DASH);
			gr2d.setColor(FOCUSED_BORDER_COLOUR2);
			gr2d.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public Set<FortunaCipher> getAllowedCiphers()
	{
		return allowedCiphers;
	}

	//------------------------------------------------------------------

	public FortunaCipher getPreferredCipher()
	{
		return preferredCipher;
	}

	//------------------------------------------------------------------

	private Rectangle getCellBounds(Column        column,
									FortunaCipher cipher)
	{
		int x = 0;
		for (Column c : Column.values())
		{
			if (c == column)
				break;
			x += columnWidths.get(c);
		}
		return new Rectangle(x + GRID_LINE_WIDTH, (cipher.ordinal() + 1) * rowHeight + GRID_LINE_WIDTH,
							 columnWidths.get(column) - GRID_LINE_WIDTH, rowHeight - GRID_LINE_WIDTH);
	}

	//------------------------------------------------------------------

	private CellLocator findCell(MouseEvent event)
	{
		for (Column column : Column.BOOLEAN_COLUMNS)
		{
			for (FortunaCipher cipher : FortunaCipher.values())
			{
				if (getCellBounds(column, cipher).contains(event.getPoint()))
					return new CellLocator(column, cipher);
			}
		}
		return null;
	}

	//------------------------------------------------------------------

	private void updateCellState(MouseEvent event,
								 boolean    pressed)
	{
		// Update active cell
		CellLocator oldActiveCell = activeCell;
		activeCell = findCell(event);
		boolean changed = (activeCell == null) ? (oldActiveCell != null)
											   : !activeCell.equals(oldActiveCell);

		// Update cell state
		CellState state = (activeCell == null) ? CellState.NOT_OVER
											   : (pressed && SwingUtilities.isLeftMouseButton(event))
																						? CellState.PRESSED
																						: CellState.OVER;
		if (cellState != state)
		{
			cellState = state;
			changed = true;
		}

		// Update seleccted cell
		if ((state == CellState.PRESSED) && !selectedCell.equals(activeCell))
		{
			selectedCell = activeCell;
			changed = true;
		}

		// Redraw component if changed
		if (changed)
			repaint();
	}

	//------------------------------------------------------------------

	private void incrementSelectionColumn(int increment)
	{
		int index = selectedCell.column.ordinal() + increment;
		index = Math.min(Math.max(1, index), Column.values().length - 1);
		Column column = Column.values()[index];
		if (selectedCell.column != column)
		{
			selectedCell.column = column;
			repaint();
		}
	}

	//------------------------------------------------------------------

	private void incrementSelectionRow(int increment)
	{
		int index = selectedCell.cipher.ordinal() + increment;
		index = Math.min(Math.max(0, index), FortunaCipher.values().length - 1);
		FortunaCipher cipher = FortunaCipher.values()[index];
		if (selectedCell.cipher != cipher)
		{
			selectedCell.cipher = cipher;
			repaint();
		}
	}

	//------------------------------------------------------------------

	private void onSelectUpUnit()
	{
		incrementSelectionRow(-1);
	}

	//------------------------------------------------------------------

	private void onSelectDownUnit()
	{
		incrementSelectionRow(1);
	}

	//------------------------------------------------------------------

	private void onSelectUpMax()
	{
		incrementSelectionRow(-NUM_ROWS);
	}

	//------------------------------------------------------------------

	private void onSelectDownMax()
	{
		incrementSelectionRow(NUM_ROWS);
	}

	//------------------------------------------------------------------

	private void onSelectLeftUnit()
	{
		incrementSelectionColumn(-1);
	}

	//------------------------------------------------------------------

	private void onSelectRightUnit()
	{
		incrementSelectionColumn(1);
	}

	//------------------------------------------------------------------

	private void onSelectLeftMax()
	{
		incrementSelectionColumn(-NUM_COLUMNS);
	}

	//------------------------------------------------------------------

	private void onSelectRightMax()
	{
		incrementSelectionColumn(NUM_COLUMNS);
	}

	//------------------------------------------------------------------

	private void onToggleCell()
	{
		switch (selectedCell.column)
		{
			case CIPHER:
				// do nothing
				break;

			case ALLOWED:
				if (allowedCiphers.contains(selectedCell.cipher))
				{
					allowedCiphers.remove(selectedCell.cipher);
					if (preferredCipher == selectedCell.cipher)
						preferredCipher = null;
				}
				else
					allowedCiphers.add(selectedCell.cipher);
				break;

			case PREFERRED:
				preferredCipher = (preferredCipher == selectedCell.cipher) ? null : selectedCell.cipher;
				break;
		}
		repaint();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	Set<FortunaCipher>		allowedCiphers;
	private	FortunaCipher			preferredCipher;
	private	Map<Column, Integer>	columnWidths;
	private	int						rowHeight;
	private	CellLocator				activeCell;
	private	CellLocator				selectedCell;
	private	CellState				cellState;

}

//----------------------------------------------------------------------
