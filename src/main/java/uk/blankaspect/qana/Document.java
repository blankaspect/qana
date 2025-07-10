/*====================================================================*\

Document.java

Document base class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import javax.swing.JOptionPane;

import uk.blankaspect.common.misc.IStringKeyed;

//----------------------------------------------------------------------


// DOCUMENT BASE CLASS


abstract class Document
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	SET_KEY_STR				= "Set key for ";
	private static final	String	CLEAR_KEY_STR			= "Clear key";
	private static final	String	CLEAR_KEY_MESSAGE_STR	= "Do you want to clear the document key?";

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// DOCUMENT KIND ENUMERATION


	enum Kind
		implements IStringKeyed
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		ARCHIVE
		(
			"archive",
			"Archive"
		)
		{
			@Override
			public Document createDocument()
			{
				return new ArchiveDocument();
			}
		},

		TEXT
		(
			"text",
			"Text"
		)
		{
			@Override
			public Document createDocument()
			{
				return new TextDocument(0);
			}
		};

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Kind(String key,
					 String text)
		{
			this.key = key;
			this.text = text;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		public static Kind forKey(String key)
		{
			for (Kind value : values())
			{
				if (value.key.equals(key))
					return value;
			}
			return null;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Abstract methods
	////////////////////////////////////////////////////////////////////

		public abstract Document createDocument();

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

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	key;
		private	String	text;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	protected Document()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	protected static MainWindow getWindow()
	{
		return QanaApp.INSTANCE.getMainWindow();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Abstract methods
////////////////////////////////////////////////////////////////////////

	public abstract Kind getKind();

	//------------------------------------------------------------------

	public abstract boolean isChanged();

	//------------------------------------------------------------------

	public abstract String getName();

	//------------------------------------------------------------------

	public abstract String getTitleString(boolean full);

	//------------------------------------------------------------------

	public abstract View createView();

	//------------------------------------------------------------------

	public abstract void updateCommands();

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public long getTimestamp()
	{
		return timestamp;
	}

	//------------------------------------------------------------------

	public void setTimestamp(long timestamp)
	{
		this.timestamp = timestamp;
	}

	//------------------------------------------------------------------

	public KeyList.Key getKey()
	{
		return key;
	}

	//------------------------------------------------------------------

	public KeyList.Key getKey(String operationStr)
	{
		if (key == null)
		{
			KeyList.Key key = QanaApp.INSTANCE.getKey(operationStr, getName());
			if (key != null)
				setKey(key);
		}
		return key;
	}

	//------------------------------------------------------------------

	public void replaceKey(KeyList.Key target,
						   KeyList.Key replacement)
	{
		if (key == target)
			key = replacement;
	}

	//------------------------------------------------------------------

	protected boolean isKey()
	{
		return (key != null);
	}

	//------------------------------------------------------------------

	protected void setKey()
	{
		KeyList.Key key = QanaApp.INSTANCE.selectKey(SET_KEY_STR + getName());
		if (key != null)
			setKey(key);
	}

	//------------------------------------------------------------------

	protected void setKey(KeyList.Key key)
	{
		this.key = key;
		getWindow().updateStatus();
	}

	//------------------------------------------------------------------

	protected void clearKey()
	{
		String[] optionStrs = Utils.getOptionStrings(AppConstants.CLEAR_STR);
		if (JOptionPane.showOptionDialog(getWindow(), CLEAR_KEY_MESSAGE_STR, CLEAR_KEY_STR,
										 JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
										 optionStrs, optionStrs[1]) == JOptionPane.OK_OPTION)
		{
			key = null;
			getWindow().updateStatus();
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	long		timestamp;
	private	KeyList.Key	key;

}

//----------------------------------------------------------------------
