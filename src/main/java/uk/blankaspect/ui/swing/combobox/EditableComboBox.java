/*====================================================================*\

EditableComboBox.java

Editable combo box class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.combobox;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import uk.blankaspect.ui.swing.font.FontKey;
import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.misc.GuiUtils;

//----------------------------------------------------------------------


// EDITABLE COMBO BOX CLASS


public class EditableComboBox
	extends JComboBox<String>
	implements FocusListener, KeyListener, PopupMenuListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	VERTICAL_MARGIN		= 1;
	private static final	int	HORIZONTAL_MARGIN	= 4;

////////////////////////////////////////////////////////////////////////
//  Member interfaces
////////////////////////////////////////////////////////////////////////


	// COMBO BOX EDITOR INTERFACE


	public interface IEditor
		extends ComboBoxEditor
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		int getFieldWidth();

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * @throws IllegalArgumentException
	 */

	public EditableComboBox(IEditor editor,
							int     maxNumItems)
	{
		_init(editor, maxNumItems);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	public EditableComboBox(IEditor   editor,
							int       maxNumItems,
							String... items)
	{
		super(items);
		_init(editor, maxNumItems);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	public EditableComboBox(IEditor      editor,
							int          maxNumItems,
							List<String> items)
	{
		for (String item : items)
			addItem(item);
		_init(editor, maxNumItems);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : FocusListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void focusGained(FocusEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void focusLost(FocusEvent event)
	{
		updateList();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : KeyListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void keyPressed(KeyEvent event)
	{
		if ((event.getID() == KeyEvent.KEY_PRESSED) && (event.getKeyCode() == KeyEvent.VK_ENTER) &&
			(event.getModifiersEx() == 0))
		{
			event.consume();

			if (isPopupVisible())
			{
				if (getField().getText().isEmpty())
					getEditor().setItem(getSelectedItem());
				setPopupVisible(false);
			}
			else
				updateList();
		}
	}

	//------------------------------------------------------------------

	@Override
	public void keyReleased(KeyEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void keyTyped(KeyEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : PopupMenuListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void popupMenuCanceled(PopupMenuEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent event)
	{
		updateList();
	}

	//------------------------------------------------------------------

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public String getText()
	{
		return getField().getText();
	}

	//------------------------------------------------------------------

	public List<String> getItems()
	{
		List<String> items = new ArrayList<>();
		for (int i = 0; i < getItemCount(); i++)
			items.add(getItemAt(i));
		return items;
	}

	//------------------------------------------------------------------

	public void setText(String text)
	{
		getField().setText(text);
	}

	//------------------------------------------------------------------

	public void setCaretPosition(int index)
	{
		int length = getField().getText().length();
		getField().setCaretPosition((index < 0) ? length : Math.min(index, length));
	}

	//------------------------------------------------------------------

	public void setItems(String[] items)
	{
		removeAllItems();
		for (String item : items)
			addItem(item);
		setSelectedIndex((items.length == 0) ? -1 : 0);
	}

	//------------------------------------------------------------------

	public void setItems(List<String> items)
	{
		removeAllItems();
		for (String item : items)
			addItem(item);
		setSelectedIndex(items.isEmpty() ? -1 : 0);
	}

	//------------------------------------------------------------------

	public void updateList()
	{
		String str = getText();
		if (!str.isEmpty())
		{
			insertItemAt(str, 0);
			for (int i = 1; i < getItemCount(); i++)
			{
				if (getItemAt(i).equals(str))
				{
					removeItemAt(i);
					break;
				}
			}
			while (getItemCount() > maxNumItems)
				removeItemAt(getItemCount() - 1);
			setSelectedIndex(0);
		}
	}

	//------------------------------------------------------------------

	public void setDefaultColours()
	{
		((ComboBoxRenderer<?>)getRenderer()).setDefaultColours();
	}

	//------------------------------------------------------------------

	private JTextField getField()
	{
		return (JTextField)getEditor();
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	private void _init(IEditor editor,
					   int     maxNumItems)
	{
		// Validate arguments
		if (!(editor instanceof JTextField))
			throw new IllegalArgumentException();

		// Initialise instance variables
		this.maxNumItems = maxNumItems;

		// Set font and border
		FontUtils.setAppFont(FontKey.TEXT_FIELD, this);
		GuiUtils.setPaddedLineBorder(this, 1);

		// Set editor
		setEditor(editor);
		setEditable(true);
		getField().setBorder(BorderFactory.createEmptyBorder(VERTICAL_MARGIN, HORIZONTAL_MARGIN, VERTICAL_MARGIN,
															 HORIZONTAL_MARGIN));

		// Set renderer
		setRenderer(new ComboBoxRenderer<>(this, editor.getFieldWidth()));

		// Set prototype item
		setPrototypeDisplayValue("M".repeat(getField().getColumns()));

		// Add listeners
		getField().addFocusListener(this);
		getField().addKeyListener(this);
		addPopupMenuListener(this);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	int	maxNumItems;

}

//----------------------------------------------------------------------
