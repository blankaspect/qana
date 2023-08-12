/*====================================================================*\

FileSelectionPanel.java

File selection panel class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.container;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListModel;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.ExceptionUtils;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.common.misc.FilenameSuffixFilter;
import uk.blankaspect.common.misc.IFileImporter;
import uk.blankaspect.common.misc.SystemUtils;

import uk.blankaspect.common.string.StringUtils;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.inputmap.InputMapUtils;

import uk.blankaspect.ui.swing.list.SelectionList;

import uk.blankaspect.ui.swing.menu.FMenuItem;

import uk.blankaspect.ui.swing.misc.GuiConstants;
import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.transfer.DataImporter;

//----------------------------------------------------------------------


// FILE SELECTION PANEL CLASS


public abstract class FileSelectionPanel
	extends JPanel
	implements ActionListener, IFileImporter, FocusListener, ListSelectionListener, MouseListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	LIST_CELL_VERTICAL_MARGIN	= 1;
	private static final	int	LIST_CELL_HORIZONTAL_MARGIN	= 4;

	private static final	char	FILE_SEPARATOR_CHAR	= '/';
	private static final	String	FILE_SEPARATOR		= Character.toString(FILE_SEPARATOR_CHAR);

	private static final	String	ADD_STR							= "Add";
	private static final	String	REMOVE_STR						= "Remove";
	private static final	String	PASTE_STR						= "Paste";
	private static final	String	SELECT_STR						= "Select";
	private static final	String	SELECT_FILE_STR					= "Select file";
	private static final	String	SELECT_DIRECTORY_STR			= "Select directory";
	private static final	String	SELECT_FILE_OR_DIRECTORY_STR	= "Select file or directory";
	private static final	String	PATHNAME_KIND_TOOLTIP_STR		= "Show %s pathnames";

	private static final	KeyStroke	DELETE_KEY	= KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,
																			 KeyEvent.SHIFT_DOWN_MASK);
	private static final	KeyStroke	PASTE_KEY	= KeyStroke.getKeyStroke(KeyEvent.VK_V,
																			 KeyEvent.CTRL_DOWN_MASK);

	// Commands
	private interface Command
	{
		String	ADD						= "add";
		String	REMOVE					= "remove";
		String	TOGGLE_PATHNAME_KIND	= "togglePathnameKind";
		String	PASTE_FILES				= "pasteFiles";
	}

	private static final	KeyAction.KeyCommandPair[]	KEY_COMMANDS	=
	{
		new KeyAction.KeyCommandPair(DELETE_KEY, Command.REMOVE),
		new KeyAction.KeyCommandPair(PASTE_KEY,  Command.PASTE_FILES)
	};

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// SELECTION MODE


	public enum Mode
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FILES
		(
			true,
			false,
			false,
			JFileChooser.FILES_ONLY,
			SELECT_FILE_STR
		),

		FILES_RECURSIVE
		(
			true,
			false,
			true,
			JFileChooser.FILES_AND_DIRECTORIES,
			SELECT_FILE_OR_DIRECTORY_STR
		),

		DIRECTORIES
		(
			false,
			true,
			false,
			JFileChooser.DIRECTORIES_ONLY,
			SELECT_DIRECTORY_STR
		),

		DIRECTORIES_RECURSIVE
		(
			false,
			true,
			true,
			JFileChooser.FILES_AND_DIRECTORIES,
			SELECT_FILE_OR_DIRECTORY_STR
		),

		FILES_AND_DIRECTORIES
		(
			true,
			true,
			false,
			JFileChooser.FILES_AND_DIRECTORIES,
			SELECT_FILE_OR_DIRECTORY_STR
		),

		FILES_AND_DIRECTORIES_RECURSIVE
		(
			true,
			true,
			true,
			JFileChooser.FILES_AND_DIRECTORIES,
			SELECT_FILE_OR_DIRECTORY_STR
		);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Mode(boolean files,
					 boolean directories,
					 boolean recursive,
					 int     fileChooserMode,
					 String  selectionText)
		{
			this.files = files;
			this.directories = directories;
			this.recursive = recursive;
			this.fileChooserMode = fileChooserMode;
			this.selectionText = selectionText;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	boolean	files;
		private	boolean	directories;
		private	boolean	recursive;
		private	int		fileChooserMode;
		private	String	selectionText;

	}

	//==================================================================


	// PATHNAME KIND


	public enum PathnameKind
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		NORMAL
		(
			"normal",
			Color.BLACK
		),

		RELATIVE
		(
			"relative",
			new Color(0, 0, 160)
		);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private PathnameKind(String text,
							 Color  textColour)
		{
			this.text = text;
			this.textColour = textColour;
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
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public PathnameKind getOther()
		{
			return ((this == NORMAL) ? RELATIVE : NORMAL);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	text;
		private	Color	textColour;

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

		CLIPBOARD_IS_UNAVAILABLE
		("The clipboard is currently unavailable."),

		NO_TEXT_ON_CLIPBOARD
		("There is no text on the clipboard."),

		FAILED_TO_GET_CLIPBOARD_DATA
		("Failed to get data from the clipboard."),

		FAILED_TO_LIST_DIRECTORY_ENTRIES
		("Failed to get a list of directory entries.");

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

		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// FILE LIST MODEL CLASS


	private static class FileListModel
		implements ListModel<SelectedFile>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FileListModel()
		{
			files = new ArrayList<>();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ListModel interface
	////////////////////////////////////////////////////////////////////

		public SelectedFile getElementAt(int index)
		{
			return files.get(index);
		}

		//--------------------------------------------------------------

		public int getSize()
		{
			return files.size();
		}

		//--------------------------------------------------------------

		public void addListDataListener(ListDataListener listener)
		{
			if (listeners == null)
				listeners = new ArrayList<>();
			listeners.add(listener);
		}

		//--------------------------------------------------------------

		public void removeListDataListener(ListDataListener listener)
		{
			if (listeners != null)
				listeners.remove(listener);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void add(SelectedFile file)
		{
			if (!files.contains(file))
			{
				files.add(file);
				changed = true;
			}
		}

		//--------------------------------------------------------------

		private void remove(int index)
		{
			files.remove(index);
			changed = true;
		}

		//--------------------------------------------------------------

		private void clear()
		{
			files.clear();
			changed = true;
		}

		//--------------------------------------------------------------

		private void update()
		{
			if (changed)
			{
				files.sort(null);
				ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, files.size() - 1);
				for (int i = listeners.size() - 1; i >= 0; i--)
					listeners.get(i).contentsChanged(event);
				changed = false;
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	List<SelectedFile>		files;
		private	List<ListDataListener>	listeners;
		private	boolean					changed;

	}

	//==================================================================


	// CORNER CLASS


	private static class Corner
		extends JComponent
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	Color	SELECTED_BACKGROUND_COLOUR	= new Color(248, 156, 32);
		private static final	Color	SELECTED_BORDER_COLOUR		= new Color(192, 112, 0);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public Corner()
		{
			setOpaque(true);
			setFocusable(false);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected void paintComponent(Graphics gr)
		{
			int width = getWidth();
			int height = getHeight();
			if (selected)
			{
				gr.setColor(SELECTED_BACKGROUND_COLOUR);
				gr.fillRect(2, 2, width - 4, height - 4);
				gr.setColor(SELECTED_BORDER_COLOUR);
				gr.drawRect(1, 1, width - 3, height - 3);
				gr.setColor(getBackground());
				gr.drawRect(0, 0, width - 1, height - 1);
			}
			else
			{
				gr.setColor(getBackground());
				gr.fillRect(0, 0, width, height);
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void setSelected(boolean selected)
		{
			if (this.selected != selected)
			{
				this.selected = selected;
				repaint();
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	boolean	selected;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// SELECTED FILE CLASS


	public class SelectedFile
		implements Comparable<SelectedFile>, SelectionList.ITooltipSource
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	char	SEPARATOR_CHAR	= '/';

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private SelectedFile(File   file,
							 String relativePathname,
							 String pathname)
		{
			this.file = file;
			this.relativePathname = relativePathname;
			this.pathname = pathname;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Comparable interface
	////////////////////////////////////////////////////////////////////

		@Override
		public int compareTo(SelectedFile selectedFile)
		{
			List<String> components1 = StringUtils.split(getPathname().replace(File.separatorChar, SEPARATOR_CHAR),
														 SEPARATOR_CHAR);
			List<String> components2 = StringUtils.split(selectedFile.getPathname()
																		.replace(File.separatorChar, SEPARATOR_CHAR),
														 SEPARATOR_CHAR);
			boolean ignoreCase = (File.separatorChar == '\\');
			int length = (components1.size() == components2.size())
														? components1.size()
														: Math.min(components1.size(), components2.size()) - 1;
			int result = 0;
			for (int i = 0; i < length; i++)
			{
				result = ignoreCase ? components1.get(i).compareToIgnoreCase(components2.get(i))
									: components1.get(i).compareTo(components2.get(i));
				if (result != 0)
					break;
			}
			if (result == 0)
				result = Integer.compare(components1.size(), components2.size());
			return result;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : SelectionList.ITooltipSource interface
	////////////////////////////////////////////////////////////////////

		@Override
		public String getTooltip()
		{
			return ((pathnameKind == null) ? null
										   : (pathnameKind == PathnameKind.RELATIVE) ? pathname
																					 : relativePathname);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public boolean equals(Object obj)
		{
			return ((obj instanceof SelectedFile) &&
					 getPathname().equals(((SelectedFile)obj).getPathname()));
		}

		//--------------------------------------------------------------

		@Override
		public int hashCode()
		{
			return getPathname().hashCode();
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return ((pathnameKind == PathnameKind.RELATIVE) ? relativePathname : pathname);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private String getPathname()
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

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		public	File	file;
		public	String	relativePathname;
		private	String	pathname;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public FileSelectionPanel(int          listNumViewableColumns,
							  int          listNumViewableRows,
							  Mode         mode,
							  PathnameKind pathnameKind,
							  String       titleStr)
	{
		this(listNumViewableColumns, listNumViewableRows, mode, pathnameKind, titleStr, null);
	}

	//------------------------------------------------------------------

	public FileSelectionPanel(int                    listNumViewableColumns,
							  int                    listNumViewableRows,
							  Mode                   mode,
							  PathnameKind           pathnameKind,
							  String                 titleStr,
							  FilenameSuffixFilter[] filenameFilters)
	{
		// Initialise instance variables
		this.mode = mode;
		this.pathnameKind = pathnameKind;
		fileListModel = new FileListModel();

		fileChooser = new JFileChooser(SystemUtils.getUserHomePathname());
		fileChooser.setDialogTitle(titleStr);
		fileChooser.setFileSelectionMode(mode.fileChooserMode);
		if ((filenameFilters != null) && (filenameFilters.length > 0))
		{
			for (FilenameSuffixFilter filenameFilter : filenameFilters)
				fileChooser.addChoosableFileFilter(filenameFilter);
			fileChooser.setFileFilter(filenameFilters[0]);
		}
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		fileChooser.setApproveButtonToolTipText(mode.selectionText);


		//----  File list

		// File selection list
		fileList = new SelectionList<>(listNumViewableColumns, listNumViewableRows,
									   LIST_CELL_VERTICAL_MARGIN, LIST_CELL_HORIZONTAL_MARGIN);
		fileList.addFocusListener(this);
		fileList.addListSelectionListener(this);
		fileList.addMouseListener(this);
		fileList.setModel(fileListModel);

		// Scroll pane: file selection list
		corner = new Corner();
		fileListScrollPane = new JScrollPane(fileList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
											 JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		fileListScrollPane.setCorner(JScrollPane.LOWER_TRAILING_CORNER, corner);
		fileListScrollPane.getVerticalScrollBar().setFocusable(false);
		fileListScrollPane.getHorizontalScrollBar().setFocusable(false);


		//----  List button panel

		JPanel listButtonPanel = new JPanel(new GridLayout(0, 1, 0, 8));

		// Button: add
		JButton addButton = new FButton(ADD_STR + GuiConstants.ELLIPSIS_STR);
		addButton.setActionCommand(Command.ADD);
		addButton.addActionListener(this);
		listButtonPanel.add(addButton);

		// Button: remove
		removeButton = new FButton(REMOVE_STR);
		removeButton.setEnabled(false);
		removeButton.setActionCommand(Command.REMOVE);
		removeButton.addActionListener(this);
		listButtonPanel.add(removeButton);


		//----  Pathname kind button panel

		JPanel pathnameKindButtonPanel = new JPanel(new GridLayout(0, 1, 0, 8));

		if (pathnameKind != null)
		{
			// Button: pathname kind
			pathnameKindButton = new FButton("");
			pathnameKindButton.setActionCommand(Command.TOGGLE_PATHNAME_KIND);
			pathnameKindButton.addActionListener(this);
			pathnameKindButtonPanel.add(pathnameKindButton);

			// Update file list and pathname kind button
			updatePathnameKind();
		}


		//----  Button panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel buttonPanel = new JPanel(gridBag);

		int gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.5;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 12, 0);
		gridBag.setConstraints(listButtonPanel, gbc);
		buttonPanel.add(listButtonPanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.5;
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(12, 0, 0, 0);
		gridBag.setConstraints(pathnameKindButtonPanel, gbc);
		buttonPanel.add(pathnameKindButtonPanel);


		//----  Outer panel

		setLayout(gridBag);

		int gridX = 0;

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 8);
		gridBag.setConstraints(fileListScrollPane, gbc);
		add(fileListScrollPane);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = new Insets(6, 0, 6, 0);
		gridBag.setConstraints(buttonPanel, gbc);
		add(buttonPanel);

		// Remove keys from input map of components
		for (KeyAction.KeyCommandPair keyCommand : KEY_COMMANDS)
			InputMapUtils.removeFromInputMap(this, true, JComponent.WHEN_FOCUSED, keyCommand.keyStroke);

		// Add commands to action map
		KeyAction.create(this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, this, KEY_COMMANDS);

		// Set action text
		getPasteAction().putValue(Action.NAME, PASTE_STR);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Abstract methods
////////////////////////////////////////////////////////////////////////

	protected abstract String getTitle();

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		try
		{
			String command = event.getActionCommand();

			if (command.equals(Command.ADD))
				onAdd();

			else if (command.equals(Command.REMOVE))
				onRemove();

			else if (command.equals(Command.TOGGLE_PATHNAME_KIND))
				onTogglePathnameKind();

			else if (command.equals(Command.PASTE_FILES))
				onPasteFiles();
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, getTitle(), JOptionPane.ERROR_MESSAGE);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IFileImporter interface
////////////////////////////////////////////////////////////////////////

	@Override
	public boolean canImportFiles()
	{
		return true;
	}

	//------------------------------------------------------------------

	@Override
	public boolean canImportMultipleFiles()
	{
		return true;
	}

	//------------------------------------------------------------------

	@Override
	public void importFiles(List<File> files)
	{
		addFiles(files);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : FocusListener interface
////////////////////////////////////////////////////////////////////////

	public void focusGained(FocusEvent event)
	{
		corner.setSelected(true);
	}

	//------------------------------------------------------------------

	public void focusLost(FocusEvent event)
	{
		corner.setSelected(false);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ListSelectionListener interface
////////////////////////////////////////////////////////////////////////

	public void valueChanged(ListSelectionEvent event)
	{
		removeButton.setEnabled(!fileList.isSelectionEmpty());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MouseListener interface
////////////////////////////////////////////////////////////////////////

	public void mouseClicked(MouseEvent event)
	{
		// do nothing
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
		showContextMenu(event);
	}

	//------------------------------------------------------------------

	public void mouseReleased(MouseEvent event)
	{
		showContextMenu(event);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public PathnameKind getPathnameKind()
	{
		return pathnameKind;
	}

	//------------------------------------------------------------------

	public int getNumFiles()
	{
		return fileListModel.getSize();
	}

	//------------------------------------------------------------------

	public List<SelectedFile> getFiles()
	{
		return Collections.unmodifiableList(fileListModel.files);
	}

	//------------------------------------------------------------------

	public List<String> getPathnames()
	{
		List<String> pathnames = new ArrayList<>();
		for (SelectedFile file : fileListModel.files)
			pathnames.add(file.toString());
		return pathnames;
	}

	//------------------------------------------------------------------

	public void setFiles(File[] files)
	{
		setFiles(Arrays.asList(files), false);
	}

	//------------------------------------------------------------------

	public void setFiles(File[]  files,
						 boolean mustExist)
	{
		setFiles(Arrays.asList(files), mustExist);
	}

	//------------------------------------------------------------------

	public void setFiles(List<File> files)
	{
		setFiles(files, false);
	}

	//------------------------------------------------------------------

	public void setFiles(List<File> files,
						 boolean    mustExist)
	{
		fileListModel.clear();
		for (File file : files)
		{
			if (mustExist)
				processFile(file, file.getName());
			else
				addFile(file, file.getName());
		}
		fileListModel.update();
	}

	//------------------------------------------------------------------

	public void addFiles(Collection<File> files)
	{
		for (File file : files)
			processFile(file, file.getName());
		fileListModel.update();
	}

	//------------------------------------------------------------------

	public void addListDataListener(ListDataListener listener)
	{
		fileListModel.addListDataListener(listener);
	}

	//------------------------------------------------------------------

	public void removeListDataListener(ListDataListener listener)
	{
		fileListModel.removeListDataListener(listener);
	}

	//------------------------------------------------------------------

	protected String fileToPathname(File file)
	{
		return file.getAbsolutePath();
	}

	//------------------------------------------------------------------

	protected String pathnameKindToString(PathnameKind pathnameKind,
										  boolean      title)
	{
		return (title ? StringUtils.firstCharToUpperCase(pathnameKind.text) : pathnameKind.text);
	}

	//------------------------------------------------------------------

	protected void addFiles(File   directory,
							String relativePathname)
	{
		try
		{
			File[] files = directory.listFiles();
			if (files == null)
				throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, directory);
			for (File file : files)
				processFile(file, relativePathname + FILE_SEPARATOR + file.getName());
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, getTitle(), JOptionPane.ERROR_MESSAGE);
		}
	}

	//------------------------------------------------------------------

	private void updatePathnameKind()
	{
		fileList.setForeground(pathnameKind.textColour);
		fileList.setSelectionForeground(pathnameKind.textColour);
		pathnameKindButton.setText(pathnameKindToString(pathnameKind.getOther(), true));
		pathnameKindButton.setToolTipText(String.format(PATHNAME_KIND_TOOLTIP_STR,
														pathnameKindToString(pathnameKind.getOther(), false)));
	}

	//------------------------------------------------------------------

	private void processFile(File   file,
							 String relativePathname)
	{
		if (file.isFile())
		{
			if (mode.files)
				addFile(file, relativePathname);
		}
		else if (file.isDirectory())
		{
			if (mode.directories)
				addFile(file, relativePathname);
			if (mode.recursive)
				addFiles(file, relativePathname);
		}
	}

	//------------------------------------------------------------------

	private void addFile(File   file,
						 String relativePathname)
	{
		fileListModel.add(new SelectedFile(file, relativePathname, fileToPathname(file)));
	}

	//------------------------------------------------------------------

	private Action getPasteAction()
	{
		return getActionMap().get(Command.PASTE_FILES);
	}

	//------------------------------------------------------------------

	private void showContextMenu(MouseEvent event)
	{
		if (event.isPopupTrigger())
		{
			// Create context menu
			if (contextMenu == null)
			{
				contextMenu = new JPopupMenu();
				contextMenu.add(new FMenuItem(getPasteAction()));
			}

			// Update actions for menu items
			try
			{
				DataFlavor[] flavours = getToolkit().getSystemClipboard().getAvailableDataFlavors();
				getPasteAction().setEnabled(canImportFiles() && DataImporter.isFileList(flavours));
			}
			catch (Exception e)
			{
				// ignore
			}

			// Display menu
			contextMenu.show(event.getComponent(), event.getX(), event.getY());
		}
	}

	//------------------------------------------------------------------

	private void onAdd()
	{
		fileChooser.rescanCurrentDirectory();
		if (fileChooser.showDialog(GuiUtils.getWindow(this), SELECT_STR) == JFileChooser.APPROVE_OPTION)
		{
			addFiles(Arrays.asList(fileChooser.getSelectedFiles()));
			fileListModel.update();
		}
	}

	//------------------------------------------------------------------

	private void onRemove()
	{
		int[] indices = fileList.getSelectedIndices();
		for (int i = indices.length - 1; i >= 0; i--)
			fileListModel.remove(indices[i]);
		fileListModel.update();
		fileList.getSelectionModel().clearSelection();
	}

	//------------------------------------------------------------------

	private void onTogglePathnameKind()
	{
		pathnameKind = pathnameKind.getOther();
		updatePathnameKind();
	}

	//------------------------------------------------------------------

	private void onPasteFiles()
		throws AppException
	{
		try
		{
			Clipboard clipboard = getToolkit().getSystemClipboard();
			if (DataImporter.isFileList(clipboard.getAvailableDataFlavors()))
			{
				List<File> files = DataImporter.getFiles(clipboard.getContents(null), true);
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

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	Mode						mode;
	private	PathnameKind				pathnameKind;
	private	FileListModel				fileListModel;
	private	SelectionList<SelectedFile>	fileList;
	private	Corner						corner;
	private	JScrollPane					fileListScrollPane;
	private	JButton						removeButton;
	private	JButton						pathnameKindButton;
	private	JFileChooser				fileChooser;
	private	JPopupMenu					contextMenu;

}

//----------------------------------------------------------------------
