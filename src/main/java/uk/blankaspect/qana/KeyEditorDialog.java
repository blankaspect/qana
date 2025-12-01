/*====================================================================*\

KeyEditorDialog.java

Key editor dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.blankaspect.common.crypto.FortunaCipher;
import uk.blankaspect.common.crypto.StreamEncrypter;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.CancelledException;

import uk.blankaspect.common.misc.MaxValueMap;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.border.TitledBorder;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.container.FixedWidthPanel;

import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.label.FixedWidthLabel;
import uk.blankaspect.ui.swing.label.FLabel;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.text.TextRendering;
import uk.blankaspect.ui.swing.text.TextUtils;

import uk.blankaspect.ui.swing.textfield.ConstrainedTextField;

import uk.blankaspect.ui.swing.workaround.LinuxWorkarounds;

//----------------------------------------------------------------------


// KEY EDITOR DIALOG CLASS


class KeyEditorDialog
	extends JDialog
	implements ActionListener, DocumentListener, ListSelectionListener, MouseListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		KEY_LIST_NUM_ROWS	= 16;

	private static final	Insets	BUTTON_MARGINS	= new Insets(2, 6, 2, 6);

	private static final	String	EDIT_KEYS_STR				= "Edit keys";
	private static final	String	NAME_STR					= "Name";
	private static final	String	ADD_STR						= "Add";
	private static final	String	RENAME_STR					= "Rename";
	private static final	String	EDIT_PROPERTIES_STR			= "Edit properties";
	private static final	String	RENAME_TITLE_STR			= "Rename key";
	private static final	String	DELETE_TITLE_STR			= "Delete key";
	private static final	String	KEY1_STR					= "Key ";
	private static final	String	KEY2_STR					= "Key: ";
	private static final	String	KEY_DERIVATION_FUNCTION_STR	= "Key derivation function";
	private static final	String	CIPHERS_STR					= "Ciphers";
	private static final	String	ALLOWED_STR					= "Allowed";
	private static final	String	PREFERRED_STR				= "Preferred";
	private static final	String	RENAME_MESSAGE_STR			= "Do you want to rename the key to '%s'?";
	private static final	String	DELETE_MESSAGE_STR			= "Do you want to delete the key?";
	private static final	String	ADD_TOOLTIP_STR				= "Create a key with the specified name";
	private static final	String	RENAME_TOOLTIP_STR			= "Rename the selected key";
	private static final	String	EDIT_PROPERTIES_TOOLTIP_STR	= "Edit the properties of the selected key";
	private static final	String	DELETE_TOOLTIP_STR			= "Delete the selected key";
	private static final	String	KDF_PARAM_PROTOTYPE_STR		= "000, 00, 00, 00, 00";

	// Commands
	private interface Command
	{
		String	ADD				= "add";
		String	RENAME			= "rename";
		String	EDIT_PROPERTIES	= "editProperties";
		String	DELETE			= "delete";
		String	ACCEPT			= "accept";
		String	CLOSE			= "close";
	}

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point									location;
	private static	Map<KdfUse, StreamEncrypter.KdfParams>	kdfParamMap		= KdfUse.getKdfParameterMap();
	private static	Set<FortunaCipher>						allowedCiphers	= EnumSet.allOf(FortunaCipher.class);
	private static	FortunaCipher							preferredCipher	= FortunaCipher.AES256;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	boolean							accepted;
	private	Map<KeyList.Key, KeyList.Key>	keyMap;
	private	KeySelectionList				selectionList;
	private	Map<KdfUse, ParameterField>		kdfParamFields;
	private	ParameterField					allowedCiphersField;
	private	ParameterField					preferredCipherField;
	private	NameField						nameField;
	private	JButton							addButton;
	private	JButton							renameButton;
	private	JButton							editPropertiesButton;
	private	JButton							deleteButton;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private KeyEditorDialog(
		Window				owner,
		List<KeyList.Key>	keys)
	{
		// Call superclass constructor
		super(owner, EDIT_KEYS_STR, ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());

		// Initialise instance variables
		keyMap = new HashMap<>();


		//----  Key selection list

		// Selection list
		keys.sort(null);
		selectionList = new KeySelectionList(KEY_LIST_NUM_ROWS, keys);
		selectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionList.addListSelectionListener(this);
		selectionList.addMouseListener(this);

		// Scroll pane: selection list
		JScrollPane listScrollPane = new JScrollPane(selectionList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
													 JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		listScrollPane.getVerticalScrollBar().setFocusable(false);
		listScrollPane.getHorizontalScrollBar().setFocusable(false);


		//----  KDF parameter panel

		// Reset fixed-width labels and panels
		Label.reset();
		Panel.reset();

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel kdfParameterPanel = new JPanel(gridBag);
		TitledBorder.setPaddedBorder(kdfParameterPanel, KEY_DERIVATION_FUNCTION_STR);

		// Panel: KDF parameters, inner
		JPanel kdfParameterInnerPanel = new Panel(gridBag);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(kdfParameterInnerPanel, gbc);
		kdfParameterPanel.add(kdfParameterInnerPanel);

		int gridY = 0;

		kdfParamFields = new EnumMap<>(KdfUse.class);
		for (KdfUse kdfUse : KdfUse.values())
		{
			// Label
			JLabel label = new Label(kdfUse.toString());

			gbc.gridx = 0;
			gbc.gridy = gridY;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_END;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(label, gbc);
			kdfParameterInnerPanel.add(label);

			// Field
			ParameterField field = new ParameterField(KDF_PARAM_PROTOTYPE_STR);
			kdfParamFields.put(kdfUse, field);

			gbc.gridx = 1;
			gbc.gridy = gridY++;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 1.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(field, gbc);
			kdfParameterInnerPanel.add(field);
		}


		//----  Cipher panel

		JPanel cipherPanel = new JPanel(gridBag);
		TitledBorder.setPaddedBorder(cipherPanel, CIPHERS_STR);

		// Panel: ciphers, inner
		JPanel cipherInnerPanel = new Panel(gridBag);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(cipherInnerPanel, gbc);
		cipherPanel.add(cipherInnerPanel);

		gridY = 0;

		// Label: allowed ciphers
		JLabel allowedCiphersLabel = new Label(ALLOWED_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(allowedCiphersLabel, gbc);
		cipherInnerPanel.add(allowedCiphersLabel);

		// Field: allowed ciphers
		allowedCiphersField = new ParameterField(getAllowedCiphersString(EnumSet.allOf(FortunaCipher.class)));

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(allowedCiphersField, gbc);
		cipherInnerPanel.add(allowedCiphersField);

		// Label: preferred cipher
		JLabel preferredCipherLabel = new Label(PREFERRED_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(preferredCipherLabel, gbc);
		cipherInnerPanel.add(preferredCipherLabel);

		// Field: preferred cipher
		List<String> strs = new ArrayList<>();
		for (FortunaCipher cipher : FortunaCipher.values())
			strs.add(cipher.toString());
		preferredCipherField = new ParameterField(TextUtils.getWidestString(getFontMetrics(getFont()), strs).str);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(preferredCipherField, gbc);
		cipherInnerPanel.add(preferredCipherField);


		//----  Name panel

		JPanel namePanel = new JPanel(gridBag);

		gridY = 0;

		// Label: name
		JLabel nameLabel = new FLabel(NAME_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(nameLabel, gbc);
		namePanel.add(nameLabel);

		// Field: name
		nameField = new NameField();
		nameField.setActionCommand(Command.ADD);
		nameField.addActionListener(this);
		nameField.getDocument().addDocumentListener(this);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(nameField, gbc);
		namePanel.add(nameField);


		//----  Edit button panel

		JPanel editButtonPanel = new JPanel(new GridLayout(0, 2, 8, 6));
		editButtonPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		// Button: add
		addButton = new FButton(ADD_STR + AppConstants.ELLIPSIS_STR);
		addButton.setMargin(BUTTON_MARGINS);
		addButton.setToolTipText(ADD_TOOLTIP_STR);
		addButton.setActionCommand(Command.ADD);
		addButton.addActionListener(this);
		editButtonPanel.add(addButton);

		// Button: rename
		renameButton = new FButton(RENAME_STR + AppConstants.ELLIPSIS_STR);
		renameButton.setMargin(BUTTON_MARGINS);
		renameButton.setToolTipText(RENAME_TOOLTIP_STR);
		renameButton.setActionCommand(Command.RENAME);
		renameButton.addActionListener(this);
		editButtonPanel.add(renameButton);

		// Button: edit properties
		editPropertiesButton = new FButton(EDIT_PROPERTIES_STR + AppConstants.ELLIPSIS_STR);
		editPropertiesButton.setMargin(BUTTON_MARGINS);
		editPropertiesButton.setToolTipText(EDIT_PROPERTIES_TOOLTIP_STR);
		editPropertiesButton.setActionCommand(Command.EDIT_PROPERTIES);
		editPropertiesButton.addActionListener(this);
		editButtonPanel.add(editPropertiesButton);

		// Button: delete
		deleteButton = new FButton(AppConstants.DELETE_STR + AppConstants.ELLIPSIS_STR);
		deleteButton.setMargin(BUTTON_MARGINS);
		deleteButton.setToolTipText(DELETE_TOOLTIP_STR);
		deleteButton.setActionCommand(Command.DELETE);
		deleteButton.addActionListener(this);
		editButtonPanel.add(deleteButton);


		//----  Control panel

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(namePanel, gbc);
		controlPanel.add(namePanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4, 0, 0, 0);
		gridBag.setConstraints(editButtonPanel, gbc);
		controlPanel.add(editButtonPanel);


		//----  Right panel

		JPanel rightPanel = new JPanel(gridBag);

		gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(kdfParameterPanel, gbc);
		rightPanel.add(kdfParameterPanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(3, 0, 0, 0);
		gridBag.setConstraints(cipherPanel, gbc);
		rightPanel.add(cipherPanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 0, 0, 0);
		gridBag.setConstraints(controlPanel, gbc);
		rightPanel.add(controlPanel);


		//----  OK/Cancel button panel

		JPanel okCancelButtonPanel = new JPanel(new GridLayout(1, 0, 8, 0));
		okCancelButtonPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		// Button: OK
		JButton okButton = new FButton(AppConstants.OK_STR);
		okButton.setActionCommand(Command.ACCEPT);
		okButton.addActionListener(this);
		okCancelButtonPanel.add(okButton);

		// Button: cancel
		JButton cancelButton = new FButton(AppConstants.CANCEL_STR);
		cancelButton.setActionCommand(Command.CLOSE);
		cancelButton.addActionListener(this);
		okCancelButtonPanel.add(cancelButton);


		//----  Main panel

		JPanel mainPanel = new JPanel(gridBag);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(listScrollPane, gbc);
		mainPanel.add(listScrollPane);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = new Insets(0, 3, 0, 0);
		gridBag.setConstraints(rightPanel, gbc);
		mainPanel.add(rightPanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(3, 0, 0, 0);
		gridBag.setConstraints(okCancelButtonPanel, gbc);
		mainPanel.add(okCancelButtonPanel);

		// Add commands to action map
		KeyAction.create(mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						 KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), Command.CLOSE, this);

		// Update components
		updateComponents();


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

		// Update widths of labels and panels
		Label.update();
		Panel.update();

		// Dispose of window explicitly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Handle window events
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(
				WindowEvent	event)
			{
				// WORKAROUND for a bug that has been observed on Linux/GNOME whereby a window is displaced downwards
				// when its location is set.  The error in the y coordinate is the height of the title bar of the
				// window.  The workaround is to set the location of the window again with an adjustment for the error.
				LinuxWorkarounds.fixWindowYCoord(event.getWindow(), location);
			}

			@Override
			public void windowClosing(
				WindowEvent	event)
			{
				onClose();
			}
		});

		// Prevent dialog from being resized
		setResizable(false);

		// Resize dialog to its preferred size
		pack();

		// Set location of dialog
		if (location == null)
			location = GuiUtils.getComponentLocation(this, owner);
		setLocation(location);

		// Set default button
		getRootPane().setDefaultButton(okButton);

		// Set focus
		nameField.requestFocusInWindow();

		// Show dialog
		setVisible(true);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static KeyEditorDialog showDialog(
		Component			parent,
		List<KeyList.Key>	keys)
	{
		return new KeyEditorDialog(GuiUtils.getWindow(parent), keys);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void actionPerformed(
		ActionEvent	event)
	{
		// Execute command
		try
		{
			switch (event.getActionCommand())
			{
				case Command.ADD             -> onAdd();
				case Command.RENAME          -> onRename();
				case Command.EDIT_PROPERTIES -> onEditProperties();
				case Command.DELETE          -> onDelete();
				case Command.ACCEPT          -> onAccept();
				case Command.CLOSE           -> onClose();
			}
		}
		catch (CancelledException e)
		{
			// ignore
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, QanaApp.SHORT_NAME, JOptionPane.ERROR_MESSAGE);
		}

		// Update components
		selectionList.repaint();
		updateComponents();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : DocumentListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void changedUpdate(
		DocumentEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void insertUpdate(
		DocumentEvent	event)
	{
		updateEditButtons();
	}

	//------------------------------------------------------------------

	@Override
	public void removeUpdate(
		DocumentEvent	event)
	{
		updateEditButtons();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ListSelectionListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void valueChanged(
		ListSelectionEvent	event)
	{
		if (!event.getValueIsAdjusting())
			updateComponents();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MouseListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void mouseClicked(
		MouseEvent	event)
	{
		if (SwingUtilities.isLeftMouseButton(event) && (event.getClickCount() > 1))
		{
			Point point = event.getPoint();
			int index = selectionList.locationToIndex(point);
			if ((index >= 0) && selectionList.getCellBounds(index, index).contains(point))
				actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Command.EDIT_PROPERTIES));
		}
	}

	//------------------------------------------------------------------

	@Override
	public void mouseEntered(
		MouseEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void mouseExited(
		MouseEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void mousePressed(
		MouseEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void mouseReleased(
		MouseEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public boolean hasEdits()
	{
		return accepted && selectionList.isChanged();
	}

	//------------------------------------------------------------------

	public List<KeyList.Key> getTemporaryKeys()
	{
		return selectionList.getKeys(KeyKind.TEMPORARY);
	}

	//------------------------------------------------------------------

	public List<KeyList.Key> getPersistentKeys()
	{
		return selectionList.getKeys(KeyKind.PERSISTENT);
	}

	//------------------------------------------------------------------

	public Map<KeyList.Key, KeyList.Key> getKeyMap()
	{
		return Collections.unmodifiableMap(keyMap);
	}

	//------------------------------------------------------------------

	public String getAllowedCiphersString(
		Collection<FortunaCipher>	ciphers)
	{
		StringBuilder buffer = new StringBuilder(64);
		for (FortunaCipher cipher : ciphers)
		{
			if (!buffer.isEmpty())
				buffer.append(", ");
			buffer.append(cipher);
		}
		return buffer.toString();
	}

	//------------------------------------------------------------------

	private void updateEditButtons()
	{
		boolean isSelection = !selectionList.isSelectionEmpty();
		String name = nameField.getText();
		boolean isValidName = !name.isEmpty() && !selectionList.contains(name);

		addButton.setEnabled(isValidName);
		renameButton.setEnabled(isSelection && isValidName);
		editPropertiesButton.setEnabled(isSelection);
		deleteButton.setEnabled(isSelection);
	}

	//------------------------------------------------------------------

	private void updateComponents()
	{
		KeyList.Key key = selectionList.getSelectedValue();
		boolean isSelection = (key != null);

		for (KdfUse kdfUse : kdfParamFields.keySet())
		{
			String str = null;
			if (isSelection)
			{
				switch (kdfUse)
				{
					case VERIFICATION:
						str = key.getKdfParamsVer().toString();
						break;

					case GENERATION:
						str = key.getKdfParamsGen().toString();
						break;
				}
			}
			kdfParamFields.get(kdfUse).setText(isSelection ? str : null);
		}

		allowedCiphersField.setText(isSelection ? getAllowedCiphersString(key.getAllowedCiphers()) : null);
		FortunaCipher cipher = isSelection ? key.getPreferredCipher() : null;
		preferredCipherField.setText((cipher == null) ? null : cipher.toString());

		updateEditButtons();
	}

	//------------------------------------------------------------------

	private void updateMap(
		KeyList.Key	source,
		KeyList.Key	target)
	{
		for (KeyList.Key key : keyMap.keySet())
		{
			if (keyMap.get(key) == source)
			{
				keyMap.put(key, target);
				source = null;
				break;
			}
		}
		if (source != null)
			keyMap.put(source, target);
	}

	//------------------------------------------------------------------

	private void verifyKey(
		KeyList.Key	key)
		throws AppException
	{
		if (key.getKey() == null)
		{
			// Get passphrase
			String passphrase =
							PassphraseDialog.showDialog(this, KEY1_STR + "'" + key.getName() + "'");
			if (passphrase == null)
				throw new CancelledException();

			// Verify key
			new KeyVerifier(key, passphrase).verify(this);

			// Update key in list
			selectionList.updateKey(key);
		}
	}

	//------------------------------------------------------------------

	private void onAdd()
		throws AppException
	{
		String name = nameField.getText();
		if (!name.isEmpty())
		{
			// Check for conflicting name
			if (selectionList.contains(name))
				throw new AppException(ErrorId.CONFLICTING_NAME, name);

			// Get passphrase
			String keyStr = KEY1_STR + "'" + name + "'";
			String passphrase = PassphraseDialog.showDialog(this, keyStr);
			if (passphrase != null)
			{
				// Get key properties
				KeyPropertiesDialog.Result result = KeyPropertiesDialog.showDialog(this, keyStr, kdfParamMap,
																				   allowedCiphers, preferredCipher);
				if (result != null)
				{
					// Set properties
					kdfParamMap = result.kdfParameterMap();
					allowedCiphers = result.allowedCiphers();
					preferredCipher = result.preferredCipher();

					// Create key
					KeyList.Key key =
							new KeyCreator(name, passphrase, kdfParamMap, allowedCiphers, preferredCipher).create(this);

					// Add key to list
					List<KeyList.Key> keys = selectionList.getKeys();
					keys.add(key);
					keys.sort(null);
					selectionList.setKeys(keys);
				}
			}
		}
	}

	//------------------------------------------------------------------

	private void onRename()
		throws AppException
	{
		String name = nameField.getText();
		int index = selectionList.getSelectedIndex();
		if (!name.isEmpty() && (index >= 0))
		{
			// Check for conflicting name
			if (selectionList.contains(name))
				throw new AppException(ErrorId.CONFLICTING_NAME, name);

			// Get selected key
			List<KeyList.Key> keys = selectionList.getKeys();
			KeyList.Key key = keys.get(index);

			// Verify key
			verifyKey(key);

			// Add key and update list
			String messageStr = String.format(RENAME_MESSAGE_STR, name);
			String[] optionStrs = Utils.getOptionStrings(RENAME_STR);
			if (JOptionPane.showOptionDialog(this, KEY2_STR + key.getQuotedName() + "\n" + messageStr,
											 RENAME_TITLE_STR, JOptionPane.OK_CANCEL_OPTION,
											 JOptionPane.QUESTION_MESSAGE, null, optionStrs,
											 optionStrs[0]) == JOptionPane.OK_OPTION)
			{
				// Create copy of key with new name
				KeyList.Key newKey = key.createCopy(name);

				// Update key map
				updateMap(key, newKey);

				// Update list
				keys.set(index, newKey);
				keys.sort(null);
				selectionList.setKeys(keys);
			}
		}
	}

	//------------------------------------------------------------------

	private void onEditProperties()
		throws AppException
	{
		int index = selectionList.getSelectedIndex();
		if (index >= 0)
		{
			// Get selected key
			KeyList.Key key = selectionList.getKeys().get(index);

			// Verify key
			verifyKey(key);

			// Get key properties
			Map<KdfUse, StreamEncrypter.KdfParams> paramMap = new EnumMap<>(KdfUse.class);
			paramMap.put(KdfUse.VERIFICATION, key.getKdfParamsVer());
			paramMap.put(KdfUse.GENERATION, key.getKdfParamsGen());
			KeyPropertiesDialog.Result result =
					KeyPropertiesDialog.showDialog(this, KEY1_STR + key.getQuotedName(), paramMap,
												   key.getAllowedCiphers(), key.getPreferredCipher());

			// Set properties of key
			if (result != null)
			{
				key.setKdfParamsVer(result.kdfParameterMap().get(KdfUse.VERIFICATION));
				key.setKdfParamsGen(result.kdfParameterMap().get(KdfUse.GENERATION));
				key.setAllowedCiphers(result.allowedCiphers());
				key.setPreferredCipher(result.preferredCipher());
				selectionList.setChanged();
			}
		}
	}

	//------------------------------------------------------------------

	private void onDelete()
		throws AppException
	{
		int index = selectionList.getSelectedIndex();
		if (index >= 0)
		{
			// Get selected key
			List<KeyList.Key> keys = selectionList.getKeys();
			KeyList.Key key = keys.get(index);

			// Verify key
			verifyKey(key);

			// Delete key and update list
			String[] optionStrs = Utils.getOptionStrings(AppConstants.DELETE_STR);
			if (JOptionPane.showOptionDialog(this, KEY2_STR + key.getQuotedName() + "\n" + DELETE_MESSAGE_STR,
											 DELETE_TITLE_STR, JOptionPane.OK_CANCEL_OPTION,
											 JOptionPane.QUESTION_MESSAGE, null, optionStrs, optionStrs[1])
					== JOptionPane.OK_OPTION)
			{
				// Update key map
				updateMap(key, null);

				// Update list
				keys.remove(index);
				selectionList.setKeys(keys);
			}
		}
	}

	//------------------------------------------------------------------

	private void onAccept()
	{
		accepted = true;
		onClose();
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
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		CONFLICTING_NAME
		("A key named '%1' already exists.");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ErrorId(
			String	message)
		{
			this.message = message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : AppException.IId interface
	////////////////////////////////////////////////////////////////////

		@Override
		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: PARAMETER FIELD


	private static class ParameterField
		extends JComponent
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int		VERTICAL_MARGIN		= 2;
		private static final	int		HORIZONTAL_MARGIN	= 6;

		private static final	Color	TEXT_COLOUR			= Color.BLACK;
		private static final	Color	BACKGROUND_COLOUR	= new Color(236, 244, 236);
		private static final	Color	BORDER_COLOUR		= new Color(204, 212, 204);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	text;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ParameterField(
			String	prototype)
		{
			// Set font
			AppFont.TEXT_FIELD.apply(this);

			// Set preferred size
			FontMetrics fontMetrics = getFontMetrics(getFont());
			int width = 2 * HORIZONTAL_MARGIN + fontMetrics.stringWidth(prototype);
			int height = 2 * VERTICAL_MARGIN + fontMetrics.getAscent() + fontMetrics.getDescent();
			setPreferredSize(new Dimension(width, height));

			// Set properties
			setEnabled(false);
			setOpaque(true);
			setFocusable(false);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected void paintComponent(
			Graphics	gr)
		{
			// Create copy of graphics context
			Graphics2D gr2d = GuiUtils.copyGraphicsContext(gr);

			// Get dimensions
			int width = getWidth();
			int height = getHeight();

			// Draw background
			gr2d.setColor(BACKGROUND_COLOUR);
			gr2d.fillRect(0, 0, width, height);

			// Draw text
			if (text != null)
			{
				// Set rendering hints for text antialiasing and fractional metrics
				TextRendering.setHints(gr2d);

				// Draw text
				gr2d.setColor(TEXT_COLOUR);
				gr2d.drawString(text, HORIZONTAL_MARGIN, VERTICAL_MARGIN + gr2d.getFontMetrics().getAscent());
			}

			// Draw border
			gr2d.setColor(BORDER_COLOUR);
			gr2d.drawRect(0, 0, width - 1, height - 1);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void setText(
			String	text)
		{
			if (!Objects.equals(text, this.text))
			{
				this.text = text;
				repaint();
			}
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: NAME FIELD


	private static class NameField
		extends ConstrainedTextField
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	MAX_LENGTH	= 1024;
		private static final	int	NUM_COLUMNS	= 32;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private NameField()
		{
			super(MAX_LENGTH, NUM_COLUMNS);
			AppFont.TEXT_FIELD.apply(this);
			GuiUtils.setTextComponentMargins(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected int getColumnWidth()
		{
			return FontUtils.getCharWidth('0', getFontMetrics(getFont()));
		}

		//--------------------------------------------------------------

		@Override
		protected boolean acceptCharacter(
			char	ch,
			int		index)
		{
			return (ch != KeyList.Key.TEMPORARY_PREFIX_CHAR);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: PANEL


	private static class Panel
		extends FixedWidthPanel
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	KEY	= Panel.class.getCanonicalName();

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Panel(
			LayoutManager	layout)
		{
			super(layout);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static void reset()
		{
			MaxValueMap.removeAll(KEY);
		}

		//--------------------------------------------------------------

		private static void update()
		{
			MaxValueMap.update(KEY);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected String getKey()
		{
			return KEY;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: LABEL


	private static class Label
		extends FixedWidthLabel
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	KEY	= Label.class.getCanonicalName();

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Label(
			String	text)
		{
			super(text);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static void reset()
		{
			MaxValueMap.removeAll(KEY);
		}

		//--------------------------------------------------------------

		private static void update()
		{
			MaxValueMap.update(KEY);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected String getKey()
		{
			return KEY;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
