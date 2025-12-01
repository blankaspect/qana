/*====================================================================*\

BitSelectionPanel.java

Class: bit-selection panel.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.container;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import uk.blankaspect.common.number.NumberUtils;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.colour.Colours;

import uk.blankaspect.ui.swing.font.FontKey;
import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.text.TextRendering;

//----------------------------------------------------------------------


// CLASS: BIT-SELECTION PANEL


public class BitSelectionPanel
	extends JComponent
	implements ActionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		NUM_ROWS	= 1;

	private static final	int		HORIZONTAL_MARGIN		= 2;
	private static final	int		INNER_VERTICAL_MARGIN	= 2;
	private static final	int		OUTER_VERTICAL_MARGIN	= 2;
	private static final	int		GRID_LINE_WIDTH			= 1;

	private static final	Color	BACKGROUND_COLOUR					= new Color(254, 254, 250);
	private static final	Color	TEXT_COLOUR							= Colours.FOREGROUND;
	private static final	Color	BORDER_COLOUR						= new Color(176, 184, 176);
	private static final	Color	FOCUSED_BORDER_COLOUR				= new Color(80, 160, 208);
	private static final	Color	DISABLED_BACKGROUND_COLOUR			= new Color(232, 232, 232);
	private static final	Color	SELECTED_BACKGROUND_COLOUR			= new Color(200, 224, 200);
	private static final	Color	FOCUSED_SELECTED_BACKGROUND_COLOUR	= new Color(248, 216, 144);

	// Commands
	private interface Command
	{
		String	TOGGLE_SELECTED		= "toggleSelected";
		String	SELECT_LEFT_UNIT	= "selectLeftUnit";
		String	SELECT_RIGHT_UNIT	= "selectRightUnit";
		String	SELECT_LEFT_MAX		= "selectLeftMax";
		String	SELECT_RIGHT_MAX	= "selectRightMax";
	}

	private static final	KeyAction.KeyCommandPair[]	KEY_COMMANDS	=
	{
		KeyAction.command(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
						  Command.TOGGLE_SELECTED),
		KeyAction.command(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
						  Command.SELECT_LEFT_UNIT),
		KeyAction.command(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
						  Command.SELECT_RIGHT_UNIT),
		KeyAction.command(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),
						  Command.SELECT_LEFT_MAX),
		KeyAction.command(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0),
						  Command.SELECT_RIGHT_MAX)
	};

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	int	numBits;
	private	int	enabledMask;
	private	int	selectedMask;
	private	int	activeIndex;
	private	int	cellWidth;
	private	int	cellHeight;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * @throws IllegalArgumentException
	 */

	public BitSelectionPanel(
		int	numBits,
		int	selectedMask)
	{
		// Call alternative constructor
		this(numBits, (1 << Integer.SIZE) - 1, selectedMask);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	public BitSelectionPanel(
		int	numBits,
		int	enabledMask,
		int	selectedMask)
	{
		// Validate arguments
		if ((numBits <= 0) || (numBits > Integer.SIZE))
			throw new IllegalArgumentException("Number of bits out of bounds: " + numBits);

		// Set font
		FontUtils.setAppFont(FontKey.MAIN, this);

		// Initialise instance variables
		this.numBits = numBits;
		this.enabledMask = enabledMask;
		this.selectedMask = selectedMask;
		FontMetrics fontMetrics = getFontMetrics(getFont());
		String text = "0".repeat(NumberUtils.getNumDecDigitsInt(numBits - 1));
		cellWidth = GRID_LINE_WIDTH + 2 * HORIZONTAL_MARGIN + fontMetrics.stringWidth(text);
		cellHeight = GRID_LINE_WIDTH + 2 * INNER_VERTICAL_MARGIN + fontMetrics.getAscent() + fontMetrics.getDescent();

		// Set properties
		setOpaque(true);
		setFocusable(true);

		// Add commands to action map
		KeyAction.create(this, JComponent.WHEN_FOCUSED, this, KEY_COMMANDS);

		// Add focus listener
		addFocusListener(new FocusListener()
		{
			@Override
			public void focusGained(
				FocusEvent	event)
			{
				repaint();
			}

			@Override
			public void focusLost(
				FocusEvent	event)
			{
				repaint();
			}
		});

		// Add mouse listener
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(
				MouseEvent	event)
			{
				if (SwingUtilities.isLeftMouseButton(event))
				{
					int x = event.getX();
					int y = event.getY();
					if ((x >= 0) && (x < numBits * cellWidth) && (y >= 0) && (y < NUM_ROWS * cellHeight))
					{
						requestFocusInWindow();
						int index = numBits - x / cellWidth - 1;
						if (isBitEnabled(index))
						{
							setActiveIndex(index);
							onToggleSelectedBit();
						}
					}
				}
			}
		});

		// Select initial bit
		onSelectRightMax();
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
			case Command.TOGGLE_SELECTED   -> onToggleSelectedBit();
			case Command.SELECT_LEFT_UNIT  -> onSelectLeftUnit();
			case Command.SELECT_RIGHT_UNIT -> onSelectRightUnit();
			case Command.SELECT_LEFT_MAX   -> onSelectLeftMax();
			case Command.SELECT_RIGHT_MAX  -> onSelectRightMax();
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(GRID_LINE_WIDTH + numBits * cellWidth,
							 2 * OUTER_VERTICAL_MARGIN + GRID_LINE_WIDTH + NUM_ROWS * cellHeight);
	}

	//------------------------------------------------------------------

	@Override
	protected void paintComponent(
		Graphics	gr)
	{
		// Create copy of graphics context
		Graphics2D gr2d = GuiUtils.copyGraphicsContext(gr);

		// Draw background
		Rectangle rect = gr2d.getClipBounds();
		gr2d.setColor(getBackground());
		gr2d.fillRect(rect.x, rect.y, rect.width, rect.height);

		// Draw cell backgrounds
		int x = GRID_LINE_WIDTH;
		int y = OUTER_VERTICAL_MARGIN + GRID_LINE_WIDTH;
		for (int i = numBits - 1; i >= 0; i--)
		{
			gr2d.setColor(isBitEnabled(i)
								? isBitSelected(i)
										? isFocusOwner()
												? FOCUSED_SELECTED_BACKGROUND_COLOUR
												: SELECTED_BACKGROUND_COLOUR
										: BACKGROUND_COLOUR
								: DISABLED_BACKGROUND_COLOUR);
			gr2d.fillRect(x, y, cellWidth - 1, cellHeight - 1);
			x += cellWidth;
		}

		// Draw horizontal grid lines
		gr2d.setColor(BORDER_COLOUR);
		int x1 = 0;
		int x2 = getWidth() - 1;
		y = OUTER_VERTICAL_MARGIN;
		for (int i = 0; i <= NUM_ROWS; i++)
		{
			gr2d.drawLine(x1, y, x2, y);
			y += cellHeight;
		}

		// Draw vertical grid lines
		x = 0;
		int y1 = OUTER_VERTICAL_MARGIN;
		int y2 = y1 + cellHeight;
		for (int i = 0; i <= numBits; i++)
		{
			gr2d.drawLine(x, y1, x, y2);
			x += cellWidth;
		}

		// Set rendering hints for text antialiasing and fractional metrics
		TextRendering.setHints(gr2d);

		// Draw text
		gr2d.setColor(TEXT_COLOUR);
		FontMetrics fontMetrics = gr2d.getFontMetrics();
		int digitWidth = fontMetrics.charWidth('0');
		x = GRID_LINE_WIDTH;
		y = OUTER_VERTICAL_MARGIN + GRID_LINE_WIDTH + INNER_VERTICAL_MARGIN + fontMetrics.getAscent();
		for (int i = numBits - 1; i >= 0; i--)
		{
			String text = Integer.toString(i);
			gr2d.drawString(text, x + (cellWidth - ((i < 10) ? 1 : 2) * digitWidth) / 2, y);
			x += cellWidth;
		}

		// Draw focus indicator
		if (isFocusOwner())
		{
			gr2d.setColor(FOCUSED_BORDER_COLOUR);
			x = (numBits - activeIndex - 1) * cellWidth;
			y = OUTER_VERTICAL_MARGIN;
			gr2d.drawRect(x, y, cellWidth, cellHeight);
			gr2d.drawRect(++x, ++y, cellWidth - 2, cellHeight - 2);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public int getSelectedMask()
	{
		return selectedMask;
	}

	//------------------------------------------------------------------

	private boolean isBitEnabled(
		int	index)
	{
		return ((enabledMask & 1 << index) != 0);
	}

	//------------------------------------------------------------------

	private boolean isBitSelected(
		int	index)
	{
		return ((selectedMask & 1 << index) != 0);
	}

	//------------------------------------------------------------------

	private void setActiveIndex(
		int	index)
	{
		if (activeIndex != index)
		{
			activeIndex = index;
			repaint();
		}
	}

	//------------------------------------------------------------------

	private void onToggleSelectedBit()
	{
		if (isBitEnabled(activeIndex))
		{
			if (isBitSelected(activeIndex))
				selectedMask &= ~(1 << activeIndex);
			else
				selectedMask |= 1 << activeIndex;

			repaint();
		}
	}

	//------------------------------------------------------------------

	private void onSelectLeftUnit()
	{
		int index = activeIndex;
		while (++index < numBits)
		{
			if (isBitEnabled(index))
				break;
		}
		if (index < numBits)
			setActiveIndex(index);
	}

	//------------------------------------------------------------------

	private void onSelectRightUnit()
	{
		int index = activeIndex;
		while (--index >= 0)
		{
			if (isBitEnabled(index))
				break;
		}
		if (index >= 0)
			setActiveIndex(index);
	}

	//------------------------------------------------------------------

	private void onSelectLeftMax()
	{
		activeIndex = numBits;
		onSelectRightUnit();
	}

	//------------------------------------------------------------------

	private void onSelectRightMax()
	{
		activeIndex = -1;
		onSelectLeftUnit();
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
