/*====================================================================*\

ByteUnitIntegerSpinnerPanel.java

Class: byte-unit integer spinner panel.

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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.blankaspect.common.misc.IStringKeyed;

import uk.blankaspect.ui.swing.colour.Colours;

import uk.blankaspect.ui.swing.font.FontKey;
import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.misc.GuiConstants;
import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.modifiers.InputModifiers;

import uk.blankaspect.ui.swing.spinner.AbstractIntegerSpinner;
import uk.blankaspect.ui.swing.spinner.IntegerSpinner;

import uk.blankaspect.ui.swing.text.TextRendering;

//----------------------------------------------------------------------


// CLASS: BYTE-UNIT INTEGER SPINNER PANEL


public class ByteUnitIntegerSpinnerPanel
	extends JPanel
	implements ChangeListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public enum Property
	{
		UNIT,
		VALUE,
		MINIMUM_VALUE,
		MAXIMUM_VALUE
	}

	private static final	Color	UNIT_BUTTON_TEXT_COLOUR				= Colours.FOREGROUND;
	private static final	Color	UNIT_BUTTON_DISABLED_TEXT_COLOUR	= new Color(160, 160, 160);
	private static final	Color	UNIT_BUTTON_BORDER_COLOUR			= Color.GRAY;
	private static final	Color	UNIT_BUTTON_DISABLED_BORDER_COLOUR	= UNIT_BUTTON_DISABLED_TEXT_COLOUR;
	private static final	Color	UNIT_BUTTON_FOCUSED_BORDER_COLOUR1	= Color.WHITE;
	private static final	Color	UNIT_BUTTON_FOCUSED_BORDER_COLOUR2	= Color.BLACK;

	private static final	Unit	DEFAULT_UNIT	= Unit.BYTE;

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: UNIT


	public enum Unit
		implements IStringKeyed
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		BYTE
		(
			"byte",
			"B",
			0,
			new Color(208, 224, 240)
		),

		KIBIBYTE
		(
			"kibibyte",
			"KiB",
			10,
			new Color(192, 224, 192)
		),

		MEBIBYTE
		(
			"mebibyte",
			"MiB",
			20,
			new Color(240, 216, 160)
		);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	key;
		private	String	text;
		private	int		exponent;
		private	Color	colour;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Unit(String key,
					 String text,
					 int    exponent,
					 Color  colour)
		{
			this.key = key;
			this.text = text;
			this.exponent = exponent;
			this.colour = colour;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IStringKeyed interface
	////////////////////////////////////////////////////////////////////

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

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member interfaces
////////////////////////////////////////////////////////////////////////


	// INTERFACE: OBSERVER


	public interface IObserver
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		void notifyChanged(ByteUnitIntegerSpinnerPanel source,
						   Property                    changedProperty);

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: VALUE


	public static class Value
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		public	Unit	unit;
		public	int		value;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public Value(Unit unit,
					 int  value)
		{
			this.unit = unit;
			this.value = value;
		}

		//--------------------------------------------------------------

		/**
		 * @throws IllegalArgumentException
		 */

		public Value(String str)
		{
			String[] strs = str.split(" +", -1);
			if (strs.length == 2)
			{
				for (Unit u : Unit.values())
				{
					if (strs[1].equals(u.text))
					{
						unit = u;
						break;
					}
				}
			}
			if (unit == null)
				throw new IllegalArgumentException();

			value = Integer.parseInt(strs[0]) << unit.exponent;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof Value)
			{
				Value other = (Value)obj;
				return ((unit == other.unit) && (value == other.value));
			}
			return false;
		}

		//--------------------------------------------------------------

		@Override
		public int hashCode()
		{
			return (unit.ordinal() * 31 + value);
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return (Integer.toString(value >>> unit.exponent) + " " + unit.text);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: SIGNIFICAND SPINNER


	private class SignificandSpinner
		extends IntegerSpinner
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private SignificandSpinner(int value,
								   int minValue,
								   int maxValue,
								   int maxLength)
		{
			// Call superclass constructor
			super(value, minValue, maxValue, maxLength, false);

			// Set step size
			updateStepSize();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		/**
		 * @throws IllegalArgumentException
		 */

		@Override
		protected int getEditorValue()
		{
			return (editor.getValue() << unit.exponent);
		}

		//--------------------------------------------------------------

		@Override
		protected void setEditorValue(int value)
		{
			editor.setValue(value >>> unit.exponent);
		}

		//--------------------------------------------------------------

		@Override
		protected int getModifierFactor(InputModifiers modifiers)
		{
			return (super.getModifierFactor(modifiers) << unit.exponent);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void updateStepSize()
		{
			((SpinnerNumberModel)getModel()).setStepSize(1 << unit.exponent);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: UNIT BUTTON


	private class UnitButton
		extends JButton
		implements ActionListener
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	VERTICAL_MARGIN		= 2;
		private static final	int	HORIZONTAL_MARGIN	= 3;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private UnitButton()
		{
			// Set font
			FontUtils.setAppFont(FontKey.MAIN, this);

			// Get maximum width of unit strings
			FontMetrics fontMetrics = getFontMetrics(getFont());
			int maxStrWidth = 0;
			for (Unit u : Unit.values())
			{
				int width = fontMetrics.stringWidth(u.toString());
				if (maxStrWidth < width)
					maxStrWidth = width;
			}

			// Set attributes
			setPreferredSize(new Dimension(2 * HORIZONTAL_MARGIN + maxStrWidth,
										   2 * VERTICAL_MARGIN + fontMetrics.getAscent() + fontMetrics.getDescent()));
			setBorder(null);

			// Add listeners
			addActionListener(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ActionListener interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void actionPerformed(ActionEvent event)
		{
			int index = unit.ordinal() + 1;
			if (index >= Unit.values().length)
				index = 0;
			setUnit(Unit.values()[index]);
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

			// Fill interior
			gr2d.setColor(isEnabled() ? unit.colour : getBackground());
			gr2d.fillRect(0, 0, width, height);

			// Set rendering hints for text antialiasing and fractional metrics
			TextRendering.setHints(gr2d);

			// Draw text
			FontMetrics fontMetrics = gr2d.getFontMetrics();
			String str = unit.toString();
			gr2d.setColor(isEnabled() ? UNIT_BUTTON_TEXT_COLOUR : UNIT_BUTTON_DISABLED_TEXT_COLOUR);
			gr2d.drawString(str, (width - fontMetrics.stringWidth(str)) / 2,
							FontUtils.getBaselineOffset(height, fontMetrics));

			// Draw border
			gr2d.setColor(isEnabled() ? UNIT_BUTTON_BORDER_COLOUR : UNIT_BUTTON_DISABLED_BORDER_COLOUR);
			gr2d.drawRect(0, 0, width - 1, height - 1);
			if (isFocusOwner())
			{
				gr2d.setColor(UNIT_BUTTON_FOCUSED_BORDER_COLOUR1);
				gr2d.drawRect(1, 1, width - 3, height - 3);

				gr2d.setStroke(GuiConstants.BASIC_DASH);
				gr2d.setColor(UNIT_BUTTON_FOCUSED_BORDER_COLOUR2);
				gr2d.drawRect(1, 1, width - 3, height - 3);
			}
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	Unit				unit;
	private	List<IObserver>		observers;
	private	SignificandSpinner	spinner;
	private	JButton				unitButton;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public ByteUnitIntegerSpinnerPanel(int value,
									   int minValue,
									   int maxValue,
									   int maxLength)
	{
		this(value, minValue, maxValue, maxLength, DEFAULT_UNIT);
	}

	//------------------------------------------------------------------

	public ByteUnitIntegerSpinnerPanel(Value value,
									   int   minValue,
									   int   maxValue,
									   int   maxLength)
	{
		this(value.value, minValue, maxValue, maxLength, value.unit);
	}

	//------------------------------------------------------------------

	public ByteUnitIntegerSpinnerPanel(int  value,
									   int  minValue,
									   int  maxValue,
									   int  maxLength,
									   Unit unit)
	{
		// Initialise instance variables
		this.unit = unit;
		observers = new ArrayList<>();

		// Set layout manager
		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		setLayout(gridBag);

		// Spinner
		spinner = new SignificandSpinner(value, minValue, maxValue, maxLength);
		FontUtils.setAppFont(FontKey.TEXT_FIELD, spinner);
		spinner.addChangeListener(this);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(spinner, gbc);
		add(spinner);

		// Unit button
		unitButton = new UnitButton();

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 2, 0, 0);
		gridBag.setConstraints(unitButton, gbc);
		add(unitButton);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ChangeListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void stateChanged(ChangeEvent event)
	{
		notifyObservers(Property.VALUE);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public AbstractIntegerSpinner getSpinner()
	{
		return spinner;
	}

	//------------------------------------------------------------------

	public Value getValue()
	{
		return new Value(unit, spinner.getIntValue());
	}

	//------------------------------------------------------------------

	public int getIntValue()
	{
		return spinner.getIntValue();
	}

	//------------------------------------------------------------------

	public Unit getUnit()
	{
		return unit;
	}

	//------------------------------------------------------------------

	public void setValue(int value)
	{
		if (spinner.getIntValue() != value)
			spinner.setIntValue(value);
	}

	//------------------------------------------------------------------

	public void setValue(Value value)
	{
		setUnit(value.unit);
		setValue(value.value);
	}

	//------------------------------------------------------------------

	public void setMinimum(int minValue)
	{
		if (minValue != (Integer)((SpinnerNumberModel)spinner.getModel()).getMinimum())
		{
			spinner.setMinimum(minValue);
			notifyObservers(Property.MINIMUM_VALUE);
		}
	}

	//------------------------------------------------------------------

	public void setMinimum(Value minValue)
	{
		setUnit(minValue.unit);
		setMinimum(minValue.value);
	}

	//------------------------------------------------------------------

	public void setMaximum(int maxValue)
	{
		if (maxValue != (Integer)((SpinnerNumberModel)spinner.getModel()).getMaximum())
		{
			spinner.setMaximum(maxValue);
			notifyObservers(Property.MAXIMUM_VALUE);
		}
	}

	//------------------------------------------------------------------

	public void setMaximum(Value maxValue)
	{
		setUnit(maxValue.unit);
		setMaximum(maxValue.value);
	}

	//------------------------------------------------------------------

	public void setUnit(Unit unit)
	{
		if (this.unit != unit)
		{
			this.unit = unit;
			spinner.updateStepSize();
			spinner.updateEditorValue();
			unitButton.repaint();
			notifyObservers(Property.UNIT);
		}
	}

	//------------------------------------------------------------------

	public void addObserver(IObserver observer)
	{
		observers.add(observer);
	}

	//------------------------------------------------------------------

	private void notifyObservers(Property changedProperty)
	{
		for (int i = observers.size() - 1; i >= 0; i--)
			observers.get(i).notifyChanged(this, changedProperty);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
