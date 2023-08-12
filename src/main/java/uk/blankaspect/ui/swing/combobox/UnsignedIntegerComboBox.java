/*====================================================================*\

UnsignedIntegerComboBox.java

Class: unsigned integer combo box.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.combobox;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import uk.blankaspect.ui.swing.font.FontKey;
import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.textfield.IntegerField;

//----------------------------------------------------------------------


// CLASS: UNSIGNED INTEGER COMBO BOX


public class UnsignedIntegerComboBox
	extends JComboBox<Integer>
	implements FocusListener, PopupMenuListener
{

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	int					maxNumItems;
	private	Comparator<Integer>	comparator;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public UnsignedIntegerComboBox(
		int	maxLength)
	{
		// Set font and border
		FontUtils.setAppFont(FontKey.TEXT_FIELD, this);
		GuiUtils.setPaddedLineBorder(this, 1);

		// Set editor
		Editor editor = new Editor(maxLength);
		setEditor(editor);
		setEditable(true);

		// Set renderer
		setRenderer(new ComboBoxRenderer<>(this, editor.getFieldWidth()));

		// Add listeners
		editor.addFocusListener(this);
		addPopupMenuListener(this);
	}

	//------------------------------------------------------------------

	public UnsignedIntegerComboBox(
		int				maxLength,
		int				maxNumItems,
		List<Integer>	items)
	{
		// Initialise combo box
		this(maxLength);

		// Initialise instance variables
		this.maxNumItems = maxNumItems;

		// Set items
		int numItems = items.size();
		if ((maxNumItems > 0) && (numItems > maxNumItems))
			numItems = maxNumItems;
		for (int i = 0; i < numItems; i++)
			addItem(items.get(i));
	}

	//------------------------------------------------------------------

	public UnsignedIntegerComboBox(
		int				maxLength,
		int				maxNumItems,
		List<Integer>	items,
		int				selectedItem)
	{
		this(maxLength, maxNumItems, items);
		if (selectedItem >= 0)
			setSelectedItem(selectedItem);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : FocusListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void focusGained(
		FocusEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void focusLost(
		FocusEvent	event)
	{
		updateList();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : PopupMenuListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void popupMenuCanceled(
		PopupMenuEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void popupMenuWillBecomeInvisible(
		PopupMenuEvent	event)
	{
		updateList();
	}

	//------------------------------------------------------------------

	@Override
	public void popupMenuWillBecomeVisible(
		PopupMenuEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public boolean isEmpty()
	{
		return ((Editor)getEditor()).isEmpty();
	}

	//------------------------------------------------------------------

	public int getValue()
	{
		return ((Editor)getEditor()).getValue();
	}

	//------------------------------------------------------------------

	public List<Integer> getItems()
	{
		List<Integer> items = new ArrayList<>();
		for (int i = 0; i < getItemCount(); i++)
			items.add(getItemAt(i));
		return items;
	}

	//------------------------------------------------------------------

	public List<Integer> getItems(
		boolean	updateList)
	{
		if (updateList)
			updateList();
		return getItems();
	}

	//------------------------------------------------------------------

	public void setItems(
		int[]	items)
	{
		removeAllItems();
		for (int item : items)
			addItem(item);
		setSelectedIndex((items.length == 0) ? -1 : 0);
	}

	//------------------------------------------------------------------

	public void setItems(
		Collection<Integer>	items)
	{
		removeAllItems();
		for (Integer item : items)
			addItem(item);
		setSelectedIndex(items.isEmpty() ? -1 : 0);
	}

	//------------------------------------------------------------------

	public void setComparator(
		Comparator<Integer>	comparator)
	{
		if (!Objects.equals(comparator, this.comparator))
		{
			// Update instance variable
			this.comparator = comparator;

			// Update drop-down list
			updateList();
		}
	}

	//------------------------------------------------------------------

	public void updateList()
	{
		try
		{
			int value = ((Editor)getEditor()).getValue();
			if (comparator == null)
			{
				insertItemAt(value, 0);
				for (int i = 1; i < getItemCount(); i++)
				{
					if (getItemAt(i) == value)
					{
						removeItemAt(i);
						break;
					}
				}
			}
			else
			{
				int index = Collections.binarySearch(getItems(), value, comparator);
				if (index < 0)
					insertItemAt(value, -index - 1);
			}
			if (maxNumItems > 0)
			{
				while (getItemCount() > maxNumItems)
					removeItemAt(getItemCount() - 1);
			}
			setSelectedItem(value);
		}
		catch (NumberFormatException e)
		{
			// ignore
		}
	}

	//------------------------------------------------------------------

	private void accept()
	{
		if (isPopupVisible())
		{
			if (isEmpty())
				getEditor().setItem(getSelectedItem());
			setPopupVisible(false);
		}
		else
			updateList();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: EDITOR


	private class Editor
		extends IntegerField.Unsigned
		implements ComboBoxEditor
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	VERTICAL_MARGIN		= 1;
		private static final	int	HORIZONTAL_MARGIN	= 4;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Editor(
			int	maxLength)
		{
			super(maxLength);
			setBorder(BorderFactory.createEmptyBorder(VERTICAL_MARGIN, HORIZONTAL_MARGIN,
													  VERTICAL_MARGIN, HORIZONTAL_MARGIN));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ComboBoxEditor interface
	////////////////////////////////////////////////////////////////////

		@Override
		public Component getEditorComponent()
		{
			return this;
		}

		//--------------------------------------------------------------

		@Override
		public Object getItem()
		{
			return getText();
		}

		//--------------------------------------------------------------

		@Override
		public void setItem(
			Object	obj)
		{
			setText((obj == null) ? null : obj.toString());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void processKeyEvent(
			KeyEvent	event)
		{
			if ((event.getID() == KeyEvent.KEY_PRESSED) && (event.getKeyCode() == KeyEvent.VK_ENTER) &&
				 (event.getModifiersEx() == 0))
			{
				event.consume();
				accept();
			}
			super.processKeyEvent(event);
		}

		//--------------------------------------------------------------

		@Override
		protected int getColumnWidth()
		{
			return FontUtils.getCharWidth('0', getFontMetrics(getFont()));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public int getFieldWidth()
		{
			return (getColumns() * getColumnWidth());
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
