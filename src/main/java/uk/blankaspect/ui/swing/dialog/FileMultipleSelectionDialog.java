/*====================================================================*\

FileMultipleSelectionDialog.java

Multiple file selection dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.dialog;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.ExceptionUtils;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.inputmap.InputMapUtils;

import uk.blankaspect.ui.swing.list.SelectionList;

import uk.blankaspect.ui.swing.menu.FMenuItem;

import uk.blankaspect.ui.swing.misc.GuiConstants;
import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.transfer.DataImporter;

import uk.blankaspect.ui.swing.workaround.LinuxWorkarounds;

//----------------------------------------------------------------------


// MULTIPLE FILE SELECTION DIALOG CLASS


public class FileMultipleSelectionDialog
	extends JDialog
	implements ActionListener, ListSelectionListener, MouseListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public enum Capability
	{
		ADD,
		REMOVE
	}

	private static final	int		LIST_CELL_VERTICAL_MARGIN	= 1;
	private static final	int		LIST_CELL_HORIZONTAL_MARGIN	= 4;

	private static final	String	DEFAULT_TITLE	= "Select files";

	private static final	String	ADD_STR							= "Add";
	private static final	String	REMOVE_STR						= "Remove";
	private static final	String	PASTE_STR						= "Paste";
	private static final	String	SELECT_STR						= "Select";
	private static final	String	SELECT_FILE_OR_DIRECTORY_STR	= "Select file or directory";

	private static final	KeyStroke	DELETE_KEY	=
			KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.SHIFT_DOWN_MASK);
	private static final	KeyStroke	PASTE_KEY	=
			KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK);

	// Commands
	private interface Command
	{
		String	ADD			= "add";
		String	REMOVE		= "remove";
		String	PASTE_FILES	= "pasteFiles";
		String	ACCEPT		= "accept";
		String	CLOSE		= "close";
	}

	private static final	KeyAction.KeyCommandPair[]	KEY_COMMANDS	=
	{
		KeyAction.command(DELETE_KEY,                                    Command.REMOVE),
		KeyAction.command(PASTE_KEY,                                     Command.PASTE_FILES),
		KeyAction.command(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), Command.CLOSE)
	};

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Map<String, Point>	locations	= new HashMap<>();
	private static	JFileChooser		fileChooser;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	String					key;
	private	SelectionMode			selectionMode;
	private	Set<Capability>			capabilities;
	private	Point					location;
	private	boolean					accepted;
	private	FileListModel			fileListModel;
	private	SelectionList<String>	fileList;
	private	JScrollPane				fileListScrollPane;
	private	JPanel					mainPanel;
	private	JButton					removeButton;

////////////////////////////////////////////////////////////////////////
//  Static initialiser
////////////////////////////////////////////////////////////////////////

	static
	{
		fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(SELECT_FILE_OR_DIRECTORY_STR);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		fileChooser.setApproveButtonToolTipText(SELECT_FILE_OR_DIRECTORY_STR);
	}

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	protected FileMultipleSelectionDialog(Window          owner,
										  int             listNumViewableColumns,
										  int             listNumViewableRows,
										  String          title,
										  String          acceptStr,
										  String          key,
										  SelectionMode   selectionMode,
										  Set<Capability> capabilities,
										  List<String>    pathnames,
										  boolean         show)
	{
		// Call superclass constructor
		super(owner, (title == null) ? DEFAULT_TITLE : title, ModalityType.APPLICATION_MODAL);

		// Set icons
		if (owner != null)
			setIconImages(owner.getIconImages());

		// Initialise instance variables
		this.key = key;
		this.selectionMode = selectionMode;
		this.capabilities = (capabilities == null) ? EnumSet.noneOf(Capability.class) : capabilities;
		fileListModel = new FileListModel();

		// Initialise instance of subclass
		init();

		// Add pathnames to file-list model
		if (pathnames != null)
			addPathnames(pathnames);


		//----  File list

		// File selection list
		fileList = new SelectionList<>(listNumViewableColumns, listNumViewableRows, LIST_CELL_VERTICAL_MARGIN,
									   LIST_CELL_HORIZONTAL_MARGIN);
		fileList.addListSelectionListener(this);
		fileList.addMouseListener(this);
		fileList.setModel(fileListModel);

		// Scroll pane: file selection list
		fileListScrollPane = new JScrollPane(fileList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
											 JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		fileListScrollPane.getVerticalScrollBar().setFocusable(false);
		fileListScrollPane.getHorizontalScrollBar().setFocusable(false);


		//----  List button panel

		JPanel listButtonPanel = new JPanel(new GridLayout(0, 1, 0, 8));

		// Button: add
		JButton addButton = new FButton(ADD_STR + GuiConstants.ELLIPSIS_STR);
		addButton.setEnabled(capabilities.contains(Capability.ADD));
		addButton.setActionCommand(Command.ADD);
		addButton.addActionListener(this);
		listButtonPanel.add(addButton);

		// Button: remove
		removeButton = new FButton(REMOVE_STR);
		removeButton.setEnabled(false);
		removeButton.setActionCommand(Command.REMOVE);
		removeButton.addActionListener(this);
		listButtonPanel.add(removeButton);


		//----  Close button panel

		JPanel closeButtonPanel = new JPanel(new GridLayout(0, 1, 0, 8));

		// Button: accept
		JButton acceptButton = new FButton((acceptStr == null) ? GuiConstants.OK_STR : acceptStr);
		acceptButton.setActionCommand(Command.ACCEPT);
		acceptButton.addActionListener(this);
		closeButtonPanel.add(acceptButton);

		// Button: cancel
		JButton cancelButton = new FButton(GuiConstants.CANCEL_STR);
		cancelButton.setActionCommand(Command.CLOSE);
		cancelButton.addActionListener(this);
		closeButtonPanel.add(cancelButton);


		//----  Main panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		mainPanel = new JPanel(gridBag);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		mainPanel.setTransferHandler(new FileTransferHandler());

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 2;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 8);
		gridBag.setConstraints(fileListScrollPane, gbc);
		mainPanel.add(fileListScrollPane);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(6, 0, 0, 4);
		gridBag.setConstraints(listButtonPanel, gbc);
		mainPanel.add(listButtonPanel);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(12, 0, 6, 4);
		gridBag.setConstraints(closeButtonPanel, gbc);
		mainPanel.add(closeButtonPanel);

		// Remove keys from input map of components
		InputMapUtils.removeFromInputMap(mainPanel, true, JComponent.WHEN_FOCUSED, DELETE_KEY);
		InputMapUtils.removeFromInputMap(mainPanel, true, JComponent.WHEN_FOCUSED, PASTE_KEY);

		// Add commands to action map
		KeyAction.create(mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, this, KEY_COMMANDS);

		// Set action text
		getPasteAction().putValue(Action.NAME, PASTE_STR);


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

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
		location = locations.get(key);
		if (location == null)
			location = GuiUtils.getComponentLocation(this, owner);
		setLocation(location);

		// Set default button
		getRootPane().setDefaultButton(addButton);

		// Show dialog
		if (show)
			setVisible(true);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static List<String> showDialog(Component       parent,
										  int             listNumViewableColumns,
										  int             listNumViewableRows,
										  String          title,
										  String          acceptStr,
										  String          key,
										  SelectionMode   selectionMode,
										  Set<Capability> capabilities,
										  List<String>    pathnames)
	{
		return new FileMultipleSelectionDialog(GuiUtils.getWindow(parent), listNumViewableColumns, listNumViewableRows,
											   title, acceptStr, key, selectionMode, capabilities, pathnames, true)
				.getPathnames();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void actionPerformed(ActionEvent event)
	{
		try
		{
			switch (event.getActionCommand())
			{
				case Command.ADD         -> onAdd();
				case Command.REMOVE      -> onRemove();
				case Command.PASTE_FILES -> onPasteFiles();
				case Command.ACCEPT      -> onAccept();
				case Command.CLOSE       -> onClose();
			}
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, getTitle(), JOptionPane.ERROR_MESSAGE);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ListSelectionListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void valueChanged(ListSelectionEvent event)
	{
		removeButton.setEnabled(capabilities.contains(Capability.REMOVE) && !fileList.isSelectionEmpty());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MouseListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void mouseClicked(MouseEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void mouseEntered(MouseEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void mouseExited(MouseEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void mousePressed(MouseEvent event)
	{
		showContextMenu(event);
	}

	//------------------------------------------------------------------

	@Override
	public void mouseReleased(MouseEvent event)
	{
		showContextMenu(event);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public boolean isAccepted()
	{
		return accepted;
	}

	//------------------------------------------------------------------

	public List<String> getPathnames()
	{
		return accepted ? fileListModel.pathnames : null;
	}

	//------------------------------------------------------------------

	protected void init()
	{
		// do nothing
	}

	//------------------------------------------------------------------

	protected String getPathname(File file)
	{
		String pathname = null;
		try
		{
			pathname = file.getCanonicalPath();
		}
		catch (Exception e)
		{
			ExceptionUtils.printTopOfStack(e);
			pathname = file.getAbsolutePath();
		}
		return pathname;
	}

	//------------------------------------------------------------------

	protected void addPathnames(Collection<String> pathnames)
	{
		Iterator<String> it = pathnames.iterator();
		while (it.hasNext())
			fileListModel.add(it.next());
	}

	//------------------------------------------------------------------

	protected void addFiles(List<File> files)
	{
		for (File file : files)
		{
			if (file.isFile())
				fileListModel.add(getPathname(file));
		}

		for (File file : files)
		{
			if (file.isDirectory())
			{
				if (selectionMode == SelectionMode.FILES_AND_DIRECTORIES)
					fileListModel.add(getPathname(file));

				File[] entries = file.listFiles();
				if (entries == null)
				{
					try
					{
						throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, file);
					}
					catch (AppException e)
					{
						JOptionPane.showMessageDialog(this, e, getTitle(), JOptionPane.ERROR_MESSAGE);
					}
				}
				else
					addFiles(List.of(entries));
			}
		}
	}

	//------------------------------------------------------------------

	protected boolean accept(List<String> pathnames)
	{
		return true;
	}

	//------------------------------------------------------------------

	private Action getPasteAction()
	{
		return mainPanel.getActionMap().get(Command.PASTE_FILES);
	}

	//------------------------------------------------------------------

	private void showContextMenu(MouseEvent event)
	{
		if (event.isPopupTrigger())
		{
			// Create context menu
			JPopupMenu menu = new JPopupMenu();
			menu.add(new FMenuItem(getPasteAction()));

			// Update actions for menu items
			try
			{
				DataFlavor[] flavours = getToolkit().getSystemClipboard().getAvailableDataFlavors();
				getPasteAction().setEnabled(capabilities.contains(Capability.ADD) && DataImporter.isFileList(flavours));
			}
			catch (Exception e)
			{
				// ignore
			}

			// Display menu
			menu.show(event.getComponent(), event.getX(), event.getY());
		}
	}

	//------------------------------------------------------------------

	private void onAdd()
	{
		if (capabilities.contains(Capability.ADD))
		{
			fileChooser.setDialogTitle(getTitle());
			fileChooser.setFileSelectionMode(selectionMode.chooserSelectionMode);
			fileChooser.resetChoosableFileFilters();
			fileChooser.rescanCurrentDirectory();
			if (fileChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			{
				addFiles(List.of(fileChooser.getSelectedFile()));
				fileListModel.update();
			}
		}
	}

	//------------------------------------------------------------------

	private void onRemove()
	{
		if (capabilities.contains(Capability.REMOVE))
		{
			int[] indices = fileList.getSelectedIndices();
			for (int i = indices.length - 1; i >= 0; i--)
				fileListModel.remove(indices[i]);
			fileListModel.update();
			fileList.getSelectionModel().clearSelection();
		}
	}

	//------------------------------------------------------------------

	private void onPasteFiles()
		throws AppException
	{
		if (capabilities.contains(Capability.ADD))
		{
			try
			{
				Clipboard clipboard = getToolkit().getSystemClipboard();
				if (DataImporter.isFileList(clipboard.getAvailableDataFlavors()))
				{
					List<File> files = DataImporter.getFiles(clipboard.getContents(null));
					if (!files.isEmpty())
					{
						addFiles(files);
						fileListModel.update();
					}
				}
			}
			catch (IllegalStateException e)
			{
				throw new AppException(ErrorId.CLIPBOARD_IS_UNAVAILABLE, e);
			}
			catch (UnsupportedFlavorException e)
			{
				throw new AppException(ErrorId.NO_TEXT_ON_CLIPBOARD);
			}
			catch (IOException e)
			{
				throw new AppException(ErrorId.FAILED_TO_GET_CLIPBOARD_DATA, e);
			}
		}
	}

	//------------------------------------------------------------------

	private void onAccept()
	{
		accepted = accept(Collections.unmodifiableList(fileListModel.pathnames));
		if (accepted)
			onClose();
	}

	//------------------------------------------------------------------

	private void onClose()
	{
		locations.put(key, getLocation());
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// SELECTION MODE


	public enum SelectionMode
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FILES_ONLY              (JFileChooser.FILES_ONLY),
		FILES_AND_DIRECTORIES   (JFileChooser.FILES_AND_DIRECTORIES);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int	chooserSelectionMode;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private SelectionMode(int chooserSelectionMode)
		{
			this.chooserSelectionMode = chooserSelectionMode;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FILE_TRANSFER_NOT_SUPPORTED
		("File transfer is not supported."),

		ERROR_TRANSFERRING_DATA
		("An error occurred while transferring data."),

		FAILED_TO_LIST_DIRECTORY_ENTRIES
		("Failed to get a list of directory entries."),

		CLIPBOARD_IS_UNAVAILABLE
		("The clipboard is currently unavailable."),

		NO_TEXT_ON_CLIPBOARD
		("There is no text on the clipboard."),

		FAILED_TO_GET_CLIPBOARD_DATA
		("Failed to get data from the clipboard.");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ErrorId(String message)
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


	// FILE LIST MODEL


	private static class FileListModel
		implements ListModel<String>
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	List<String>			pathnames;
		private	List<ListDataListener>	listeners;
		private	boolean					changed;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FileListModel()
		{
			pathnames = new ArrayList<>();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ListModel interface
	////////////////////////////////////////////////////////////////////

		@Override
		public String getElementAt(int index)
		{
			return pathnames.get(index);
		}

		//--------------------------------------------------------------

		@Override
		public int getSize()
		{
			return pathnames.size();
		}

		//--------------------------------------------------------------

		@Override
		public void addListDataListener(ListDataListener listener)
		{
			if (listeners == null)
				listeners = new ArrayList<>();
			listeners.add(listener);
		}

		//--------------------------------------------------------------

		@Override
		public void removeListDataListener(ListDataListener listener)
		{
			if (listeners != null)
				listeners.remove(listener);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void add(String pathname)
		{
			if (!pathnames.contains(pathname))
			{
				pathnames.add(pathname);
				changed = true;
			}
		}

		//--------------------------------------------------------------

		private void remove(int index)
		{
			pathnames.remove(index);
			changed = true;
		}

		//--------------------------------------------------------------

		private void update()
		{
			if (changed)
			{
				pathnames.sort(null);
				ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, pathnames.size() - 1);
				for (int i = listeners.size() - 1; i >= 0; i--)
					listeners.get(i).contentsChanged(event);
				changed = false;
			}
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// FILE TRANSFER HANDLER CLASS


	private class FileTransferHandler
		extends TransferHandler
		implements Runnable
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	List<File>	files;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FileTransferHandler()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void run()
		{
			addFiles(files);
			fileListModel.update();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public boolean canImport(TransferHandler.TransferSupport support)
		{
			boolean supported = !support.isDrop() || ((support.getSourceDropActions() & COPY) == COPY);
			if (supported)
				supported = capabilities.contains(Capability.ADD) && DataImporter.isFileList(support.getDataFlavors());
			if (support.isDrop() && supported)
				support.setDropAction(COPY);
			return supported;
		}

		//--------------------------------------------------------------

		@Override
		public boolean importData(TransferHandler.TransferSupport support)
		{
			if (canImport(support))
			{
				try
				{
					try
					{
						List<File> files = DataImporter.getFiles(support.getTransferable());
						if (files != null)
						{
							this.files = files;
							SwingUtilities.getWindowAncestor(support.getComponent()).toFront();
							SwingUtilities.invokeLater(this);
							return true;
						}
					}
					catch (UnsupportedFlavorException e)
					{
						throw new AppException(ErrorId.FILE_TRANSFER_NOT_SUPPORTED);
					}
					catch (IOException e)
					{
						throw new AppException(ErrorId.ERROR_TRANSFERRING_DATA);
					}
				}
				catch (AppException e)
				{
					JOptionPane.showMessageDialog(support.getComponent(), e, getTitle(), JOptionPane.ERROR_MESSAGE);
				}
			}
			return false;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
