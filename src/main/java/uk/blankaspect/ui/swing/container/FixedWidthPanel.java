/*====================================================================*\

FixedWidthPanel.java

Fixed-width panel class.

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


// FIXED-WIDTH PANEL CLASS


public abstract class FixedWidthPanel
	extends JPanel
	implements MaxValueMap.IEntry
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	protected FixedWidthPanel(LayoutManager layout)
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

	public int getValue()
	{
		return getPreferredSize().width;
	}

	//------------------------------------------------------------------

	public void setValue(int value)
	{
		setPreferredSize(new Dimension(value, getPreferredSize().height));
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
