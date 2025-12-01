/*====================================================================*\

FixedWidthRadioButton.java

Class: fixed-width radio button.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.button;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.misc.MaxValueMap;

//----------------------------------------------------------------------


// CLASS: FIXED-WIDTH RADIO BUTTON


public abstract class FixedWidthRadioButton
	extends FRadioButton
	implements MaxValueMap.IEntry
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	MIN_ICON_TEXT_GAP	= 6;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	protected FixedWidthRadioButton(
		String	text)
	{
		super(text);
		MaxValueMap.add(getKey(), this);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Abstract methods
////////////////////////////////////////////////////////////////////////

	protected abstract String getKey();

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MaxValueMap.IEntry interface
////////////////////////////////////////////////////////////////////////

	@Override
	public int getValue()
	{
		return getPreferredSize().width;
	}

	//------------------------------------------------------------------

	@Override
	public void setValue(
		int	value)
	{
		setIconTextGap(MIN_ICON_TEXT_GAP + value - getPreferredSize().width);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
