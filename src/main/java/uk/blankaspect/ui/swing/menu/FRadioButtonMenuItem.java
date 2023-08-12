/*====================================================================*\

FRadioButtonMenuItem.java

Radio button menu item class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.menu;

//----------------------------------------------------------------------


// IMPORTS


import javax.swing.Action;
import javax.swing.JRadioButtonMenuItem;

import uk.blankaspect.ui.swing.font.FontKey;
import uk.blankaspect.ui.swing.font.FontUtils;

//----------------------------------------------------------------------


// RADIO BUTTON MENU ITEM CLASS


public class FRadioButtonMenuItem
	extends JRadioButtonMenuItem
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public FRadioButtonMenuItem(Action action)
	{
		super(action);
		FontUtils.setAppFont(FontKey.MAIN, this);
	}

	//------------------------------------------------------------------

	public FRadioButtonMenuItem(Action  action,
								boolean selected)
	{
		this(action);
		setSelected(selected);
	}

	//------------------------------------------------------------------

	public FRadioButtonMenuItem(Action  action,
								boolean selected,
								boolean enabled)
	{
		this(action);
		setSelected(selected);
		setEnabled(enabled);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
