/*====================================================================*\

FixedWidthPanel.java

Class: fixed-width panel.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.container;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.JPanel;

import uk.blankaspect.common.misc.MaxValueMap;

//----------------------------------------------------------------------


// CLASS: FIXED-WIDTH PANEL


public abstract class FixedWidthPanel
	extends JPanel
	implements MaxValueMap.IEntry
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	protected FixedWidthPanel(
		LayoutManager	layout)
	{
		super(layout);
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
		setPreferredSize(new Dimension(value, getPreferredSize().height));
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
