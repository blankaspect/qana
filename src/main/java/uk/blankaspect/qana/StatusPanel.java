/*====================================================================*\

StatusPanel.java

Status panel class.

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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import uk.blankaspect.common.crypto.FortunaCipher;

import uk.blankaspect.ui.swing.icon.DialogIcon;

import uk.blankaspect.ui.swing.text.TextRendering;
import uk.blankaspect.ui.swing.text.TextUtils;

//----------------------------------------------------------------------


// STATUS PANEL CLASS


class StatusPanel
	extends JPanel
	implements MouseListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		Color	DEFAULT_TEXT_COLOUR	= Color.BLACK;

	private static final	int	VERTICAL_MARGIN	= 1;

	private static final	ImageIcon[]	ICONS	=
	{
		DialogIcon.TICK,
		DialogIcon.CROSS
	};

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// STATUS FIELD CLASS


	private static class StatusField
		extends JComponent
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	VERTICAL_MARGIN		= 1;
		private static final	int	HORIZONTAL_MARGIN	= 6;
		private static final	int	SEPARATOR_WIDTH		= 1;

		private static final	Color	LINE_COLOUR	= Color.GRAY;

		private static final	String	PROTOTYPE_STR	= " ".repeat(4);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private StatusField(boolean hasIcon)
		{
			// Set font
			AppFont.MAIN.apply(this);

			// Initialise instance variables
			FontMetrics fontMetrics = getFontMetrics(getFont());
			preferredWidth = 2 * HORIZONTAL_MARGIN + SEPARATOR_WIDTH + fontMetrics.stringWidth(PROTOTYPE_STR);
			if (hasIcon)
			{
				for (ImageIcon icon : ICONS)
				{
					int iconHeight = icon.getIconHeight();
					if (preferredHeight < iconHeight)
						preferredHeight = iconHeight;
				}
			}
			else
				preferredHeight = fontMetrics.getAscent() + fontMetrics.getDescent();
			preferredHeight += 2 * VERTICAL_MARGIN;

			// Set properties
			setOpaque(true);
			setFocusable(false);
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
			gr.setColor(getBackground());
			gr.fillRect(0, 0, width, height);

			// Draw icon
			if (icon != null)
				gr.drawImage(icon.getImage(), HORIZONTAL_MARGIN,
							 (getHeight() - icon.getIconHeight()) / 2, null);

			// Draw text
			else if (text != null)
			{
				// Set rendering hints for text antialiasing and fractional metrics
				TextRendering.setHints((Graphics2D)gr);

				// Draw text
				FontMetrics fontMetrics = gr.getFontMetrics();
				int maxWidth = width - 2 * HORIZONTAL_MARGIN - SEPARATOR_WIDTH;
				String str = TextUtils.getLimitedWidthString(text, fontMetrics, maxWidth,
															 TextUtils.RemovalMode.END);
				gr.setColor(AppConfig.INSTANCE.getStatusTextColour());
				gr.drawString(str, HORIZONTAL_MARGIN, VERTICAL_MARGIN + fontMetrics.getAscent());
			}

			// Draw separator
			int x = width - SEPARATOR_WIDTH;
			gr.setColor(LINE_COLOUR);
			gr.drawLine(x, 0, x, height - 1);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void setIcon(ImageIcon icon)
		{
			if (this.icon != icon)
			{
				this.icon = icon;
				preferredWidth = 2 * HORIZONTAL_MARGIN + SEPARATOR_WIDTH + icon.getIconWidth();
				revalidate();
				repaint();
			}
		}

		//--------------------------------------------------------------

		private void setText(String text)
		{
			if (!Objects.equals(text, this.text))
			{
				this.text = text;
				int textWidth = getFontMetrics(getFont()).
													stringWidth((text == null) ? PROTOTYPE_STR : text);
				preferredWidth = 2 * HORIZONTAL_MARGIN + SEPARATOR_WIDTH + textWidth;
				revalidate();
				repaint();
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int			preferredWidth;
		private	int			preferredHeight;
		private	ImageIcon	icon;
		private	String		text;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public StatusPanel()
	{
		// Lay out components explicitly
		setLayout(null);

		// Field: PRNG can reseed
		prngCanReseedField = new StatusField(true);
		add(prngCanReseedField);

		// Field: cipher
		cipherField = new StatusField(false);
		cipherField.addMouseListener(this);
		add(cipherField);

		// Field: global key
		globalKeyField = new StatusField(false);
		add(globalKeyField);

		// Field: document key
		documentKeyField = new StatusField(false);
		add(documentKeyField);

		// Field: document information
		documentInfoField = new StatusField(false);
		add(documentInfoField);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MouseListener interface
////////////////////////////////////////////////////////////////////////

	public void mouseClicked(MouseEvent event)
	{
		if (SwingUtilities.isLeftMouseButton(event))
		{
			AppConfig config = AppConfig.INSTANCE;
			FortunaCipher cipher = config.getPrngDefaultCipher();
			int index = cipher.ordinal() + 1;
			if (index >= FortunaCipher.values().length)
				index = 0;
			config.setPrngDefaultCipher(FortunaCipher.values()[index]);
			QanaApp.INSTANCE.getMainWindow().updateCipher();
		}
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
		// do nothing
	}

	//------------------------------------------------------------------

	public void mouseReleased(MouseEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	//------------------------------------------------------------------

	@Override
	public Dimension getPreferredSize()
	{
		int width = 0;
		int height = 0;
		for (Component component : getComponents())
		{
			Dimension size = component.getPreferredSize();
			width += size.width;
			if (height < size.height)
				height = size.height;
		}
		if (width == 0)
			++width;
		height += 2 * VERTICAL_MARGIN;
		return new Dimension(width, height);
	}

	//------------------------------------------------------------------

	@Override
	public void doLayout()
	{
		// Get maximum height of components
		int maxHeight = 0;
		for (Component component : getComponents())
		{
			int height = component.getPreferredSize().height;
			if (maxHeight < height)
				maxHeight = height;
		}

		// Set location and size of components
		int x = 0;
		int y = VERTICAL_MARGIN;
		for (Component component : getComponents())
		{
			Dimension size = component.getPreferredSize();
			component.setBounds(x, y, size.width, maxHeight);
			x += size.width;
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void setPrngCanReseed(boolean canReseed)
	{
		prngCanReseedField.setIcon(canReseed ? DialogIcon.TICK : DialogIcon.CROSS);
	}

	//------------------------------------------------------------------

	public void setCipher(FortunaCipher cipher)
	{
		cipherField.setText(cipher.toString());
	}

	//------------------------------------------------------------------

	public void setGlobalKeyText(String str)
	{
		globalKeyField.setText(str);
	}

	//------------------------------------------------------------------

	public void setDocumentKeyText(String str)
	{
		documentKeyField.setText(str);
	}

	//------------------------------------------------------------------

	public void setDocumentInfoText(String str)
	{
		documentInfoField.setText(str);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	StatusField	prngCanReseedField;
	private	StatusField	cipherField;
	private	StatusField	globalKeyField;
	private	StatusField	documentKeyField;
	private	StatusField	documentInfoField;

}

//----------------------------------------------------------------------
