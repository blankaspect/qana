/*====================================================================*\

ActionSource.java

Action source enumeration.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.misc.IStringKeyed;

//----------------------------------------------------------------------


// ACTION SOURCE ENUMERATION


enum ActionSource
	implements IStringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	MENU_COMMAND
	(
		"menuCommand",
		"Menu command",
		true
	),

	DRAG_AND_DROP
	(
		"dragAndDrop",
		"Drag-and-drop",
		false
	);

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private ActionSource(String  key,
						 String  text,
						 boolean defaultSelection)
	{
		this.key = key;
		this.text = text;
		this.defaultSelection = defaultSelection;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static ActionSource forKey(String key)
	{
		for (ActionSource value : values())
		{
			if (value.key.equals(key))
				return value;
		}
		return null;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IStringKeyed interface
////////////////////////////////////////////////////////////////////////

	public String getKey()
	{
		return key;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public String toString()
	{
		return text;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public boolean getDefaultSelection()
	{
		return defaultSelection;
	}

	//------------------------------------------------------------------

	public boolean isSelectEncryptDecryptOutputFile()
	{
		return AppConfig.INSTANCE.isSelectEncryptDecryptOutputFile(this);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance fields
////////////////////////////////////////////////////////////////////////

	private	String	key;
	private	String	text;
	private	boolean	defaultSelection;

}

//----------------------------------------------------------------------
