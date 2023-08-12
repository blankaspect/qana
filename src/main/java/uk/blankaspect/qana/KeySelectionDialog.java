/*====================================================================*\

KeySelectionDialog.java

Key selection dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.blankaspect.common.crypto.FortunaCipher;
import uk.blankaspect.common.crypto.StreamEncrypter;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.CancelledException;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.misc.GuiUtils;

//----------------------------------------------------------------------


// KEY SELECTION DIALOG CLASS


class KeySelectionDialog
	extends JDialog
	implements ActionListener, ListSelectionListener, MouseListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	KEY_LIST_NUM_ROWS	= 16;

	private static final	String	KEY_STR		= "Key ";
	private static final	String	NEW_KEY_STR	= "New key";

	// Commands
	private interface Command
	{
		String	ACCEPT	= "accept";
		String	CLOSE	= "close";
	}

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private KeySelectionDialog(Window            owner,
							   String            titleStr,
							   List<KeyList.Key> keys)
	{

		// Call superclass constructor
		super(owner, titleStr, Dialog.ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());


		//----  Key selection list

		// Selection list
		selectionList = new KeySelectionList(KEY_LIST_NUM_ROWS, keys);
		selectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionList.addListSelectionListener(this);
		selectionList.addMouseListener(this);

		// Scroll pane: selection list
		JScrollPane listScrollPane = new JScrollPane(selectionList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
													 JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		listScrollPane.getVerticalScrollBar().setFocusable(false);
		listScrollPane.getHorizontalScrollBar().setFocusable(false);


		//----  Button panel

		JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 8, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

		// Button: OK
		okButton = new FButton(AppConstants.OK_STR);
		okButton.setActionCommand(Command.ACCEPT);
		okButton.addActionListener(this);
		buttonPanel.add(okButton);

		// Button: cancel
		JButton cancelButton = new FButton(AppConstants.CANCEL_STR);
		cancelButton.setActionCommand(Command.CLOSE);
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);


		//----  Main panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel mainPanel = new JPanel(gridBag);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		int gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(listScrollPane, gbc);
		mainPanel.add(listScrollPane);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(3, 0, 0, 0);
		gridBag.setConstraints(buttonPanel, gbc);
		mainPanel.add(buttonPanel);

		// Add commands to action map
		KeyAction.create(mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						 KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), Command.CLOSE, this);


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

		// Update components
		updateComponents();

		// Dispose of window explicitly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Handle window closing
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				onClose();
			}
		});

		// Prevent dialog from being resized
		setResizable(false);

		// Resize dialog to its preferred size
		pack();

		// Set location of dialog box
		if (location == null)
			location = GuiUtils.getComponentLocation(this, owner);
		setLocation(location);

		// Set default button
		getRootPane().setDefaultButton(okButton);

		// Show dialog
		setVisible(true);

	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static KeyList.Key showDialog(Component         parent,
										 String            titleStr,
										 List<KeyList.Key> keys)
	{
		return new KeySelectionDialog(GuiUtils.getWindow(parent), titleStr, keys).getSelectedKey();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();

		if (command.equals(Command.ACCEPT))
			onAccept();

		else if (command.equals(Command.CLOSE))
			onClose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ListSelectionListener interface
////////////////////////////////////////////////////////////////////////

	public void valueChanged(ListSelectionEvent event)
	{
		if (!event.getValueIsAdjusting())
			updateComponents();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MouseListener interface
////////////////////////////////////////////////////////////////////////

	public void mouseClicked(MouseEvent event)
	{
		if (SwingUtilities.isLeftMouseButton(event) && (event.getClickCount() > 1))
		{
			Point point = event.getPoint();
			int index = selectionList.locationToIndex(point);
			if ((index >= 0) && selectionList.getCellBounds(index, index).contains(point))
				actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Command.ACCEPT));
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
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public KeyList.Key getSelectedKey()
	{
		return selectedKey;
	}

	//------------------------------------------------------------------

	public int getSelectedIndex()
	{
		return selectionList.getSelectedIndex();
	}

	//------------------------------------------------------------------

	private void updateComponents()
	{
		okButton.setEnabled(!selectionList.isSelectionEmpty());
	}

	//------------------------------------------------------------------

	private void setKey()
		throws AppException
	{
		KeyList.Key key = selectionList.getSelectedValue();
		switch (key.getKind())
		{
			case NEW:
			{
				// Get passphrase
				String passphrase = PassphraseDialog.showDialog(this, NEW_KEY_STR);
				if (passphrase == null)
					throw new CancelledException();

				// Get key properties
				KeyPropertiesDialog.Result result = KeyPropertiesDialog.showDialog(this, NEW_KEY_STR, kdfParamMap,
																				   allowedCiphers, preferredCipher);
				if (result == null)
					throw new CancelledException();

				// Set key properties
				kdfParamMap = result.kdfParameterMap;
				allowedCiphers = result.allowedCiphers;
				preferredCipher = result.preferredCipher;

				// Create temporary key
				key = new KeyCreator(null, passphrase, kdfParamMap, allowedCiphers, preferredCipher).create(this);
				App.INSTANCE.addTemporaryKey(key);
				break;
			}

			case PERSISTENT:
			{
				if (key.getKey() == null)
				{
					// Get passphrase
					String passphrase = PassphraseDialog.showDialog(this, KEY_STR + "'" + key.getName() + "'");
					if (passphrase == null)
						throw new CancelledException();

					// Verify key
					new KeyVerifier(key, passphrase).verify(this);
				}
				break;
			}

			case TEMPORARY:
				// do nothing
				break;
		}
		selectedKey = key;
	}

	//------------------------------------------------------------------

	private void onAccept()
	{
		try
		{
			setKey();
			onClose();
		}
		catch (CancelledException e)
		{
			// ignore
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, App.SHORT_NAME, JOptionPane.ERROR_MESSAGE);
		}
	}

	//------------------------------------------------------------------

	private void onClose()
	{
		location = getLocation();
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point									location;
	private static	Map<KdfUse, StreamEncrypter.KdfParams>	kdfParamMap	= KdfUse.getKdfParameterMap();
	private static	Set<FortunaCipher>						allowedCiphers	=
																	EnumSet.allOf(FortunaCipher.class);
	private static	FortunaCipher							preferredCipher	= FortunaCipher.AES256;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	KeySelectionList	selectionList;
	private	KeyList.Key			selectedKey;
	private	JButton				okButton;

}

//----------------------------------------------------------------------
