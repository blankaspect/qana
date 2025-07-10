/*====================================================================*\

MainWindow.java

Main window class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.Dimension;

import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import uk.blankaspect.common.crypto.FortunaCipher;

import uk.blankaspect.common.misc.IFileImporter;

import uk.blankaspect.common.time.CalendarTime;

import uk.blankaspect.ui.swing.menu.FCheckBoxMenuItem;
import uk.blankaspect.ui.swing.menu.FMenu;
import uk.blankaspect.ui.swing.menu.FMenuItem;
import uk.blankaspect.ui.swing.menu.FRadioButtonMenuItem;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.tabbedpane.TabbedPane;

//----------------------------------------------------------------------


// MAIN WINDOW CLASS


class MainWindow
	extends JFrame
	implements ChangeListener, IFileImporter, FlavorListener, ListSelectionListener, MenuListener,
			   MouseListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	DEFAULT_CIPHER_STR	= "Default cipher";
	private static final	String	GLOBAL_STR			= "G : ";
	private static final	String	DOCUMENT_STR		= "D : ";
	private static final	String	NO_STR				= "No";
	private static final	String	SELECTED_STR		= "selected";

	private static final	String	MAIN_MENU_KEY		= "mainMenu";
	private static final	String	CONTEXT_MENU_KEY	= "contextMenu";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	TabbedPane	tabbedPanel;
	private	StatusPanel	statusPanel;
	private	JPopupMenu	contextMenu;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public MainWindow()
	{
		// Set icons
		setIconImages(Images.APP_ICON_IMAGES);


		//----  Menu bar

		JMenuBar menuBar = new JMenuBar();
		menuBar.setBorder(null);

		// File menu
		JMenu menu = Menu.FILE.menu;
		menu.addMenuListener(this);

		menu.add(new FMenuItem(AppCommand.CREATE_FILE, KeyEvent.VK_N));
		menu.add(new FMenuItem(AppCommand.OPEN_FILE, KeyEvent.VK_O));
		menu.add(new FMenuItem(AppCommand.REVERT_FILE, KeyEvent.VK_R));

		menu.addSeparator();

		menu.add(new FMenuItem(AppCommand.CLOSE_FILE, KeyEvent.VK_C));
		menu.add(new FMenuItem(AppCommand.CLOSE_ALL_FILES, KeyEvent.VK_L));

		menu.addSeparator();

		menu.add(new FMenuItem(AppCommand.SAVE_FILE, KeyEvent.VK_S));
		menu.add(new FMenuItem(AppCommand.SAVE_FILE_AS, KeyEvent.VK_A));

		menu.addSeparator();

		menu.add(new FMenuItem(AppCommand.ENCRYPT_FILE, KeyEvent.VK_E));
		menu.add(new FMenuItem(AppCommand.DECRYPT_FILE, KeyEvent.VK_D));
		menu.add(new FMenuItem(AppCommand.VALIDATE_FILE, KeyEvent.VK_T));

		menu.addSeparator();

		menu.add(new FMenuItem(AppCommand.CONCEAL_FILE, KeyEvent.VK_I));
		menu.add(new FMenuItem(AppCommand.RECOVER_FILE, KeyEvent.VK_V));

		menu.addSeparator();

		menu.add(new FMenuItem(AppCommand.SPLIT_FILE, KeyEvent.VK_P));
		menu.add(new FMenuItem(AppCommand.JOIN_FILES, KeyEvent.VK_J));

		menu.addSeparator();

		menu.add(new FMenuItem(AppCommand.ERASE_FILES, KeyEvent.VK_F));

		menu.addSeparator();

		menu.add(new FMenuItem(AppCommand.EXIT, KeyEvent.VK_X));

		menuBar.add(menu);

		// Edit menu
		menu = Menu.EDIT.menu;
		menu.addMenuListener(this);

		menuBar.add(menu);

		// Archive menu
		menu = Menu.ARCHIVE.menu;
		menu.addMenuListener(this);

		menu.add(new FMenuItem(ArchiveDocument.Command.CHOOSE_ARCHIVE_DIRECTORY, KeyEvent.VK_C));

		menu.addSeparator();

		menu.add(new FMenuItem(ArchiveDocument.Command.ADD_FILES, KeyEvent.VK_A));
		menu.add(new FMenuItem(ArchiveDocument.Command.EXTRACT_FILES, KeyEvent.VK_E));
		menu.add(new FMenuItem(ArchiveDocument.Command.VALIDATE_FILES, KeyEvent.VK_V));
		menu.add(new FMenuItem(ArchiveDocument.Command.DELETE_FILES, KeyEvent.VK_D));

		menu.addSeparator();

		menu.add(new FMenuItem(ArchiveDocument.Command.DISPLAY_FILE_LIST, KeyEvent.VK_F));
		menu.add(new FMenuItem(ArchiveDocument.Command.DISPLAY_FILE_MAP, KeyEvent.VK_M));

		menu.addSeparator();

		menu.add(new FMenuItem(ArchiveDocument.Command.SET_KEY, KeyEvent.VK_K));
		menu.add(new FMenuItem(ArchiveDocument.Command.CLEAR_KEY, KeyEvent.VK_L));

		menuBar.add(menu);

		// Text menu
		menu = Menu.TEXT.menu;
		menu.addMenuListener(this);

		menu.add(new FMenuItem(AppCommand.CREATE_TEXT, KeyEvent.VK_N));

		menu.addSeparator();

		menu.add(new FMenuItem(TextDocument.Command.ENCRYPT, KeyEvent.VK_E));
		menu.add(new FMenuItem(TextDocument.Command.DECRYPT, KeyEvent.VK_D));
		menu.add(new FCheckBoxMenuItem(TextDocument.Command.TOGGLE_WRAP_CIPHERTEXT_IN_XML,
									   KeyEvent.VK_X));

		menu.addSeparator();

		menu.add(new FMenuItem(TextDocument.Command.CONCEAL, KeyEvent.VK_C));
		menu.add(new FMenuItem(AppCommand.RECOVER_TEXT, KeyEvent.VK_R));

		menu.addSeparator();

		menu.add(new FMenuItem(TextDocument.Command.SET_KEY, KeyEvent.VK_K));
		menu.add(new FMenuItem(TextDocument.Command.CLEAR_KEY, KeyEvent.VK_L));

		menuBar.add(menu);

		// Encryption menu
		menu = Menu.ENCRYPTION.menu;
		menu.addMenuListener(this);

		menu.add(new FMenuItem(AppCommand.SET_GLOBAL_KEY, KeyEvent.VK_K));
		menu.add(new FMenuItem(AppCommand.CLEAR_GLOBAL_KEY, KeyEvent.VK_C));
		menu.add(new FCheckBoxMenuItem(AppCommand.TOGGLE_AUTO_USE_GLOBAL_KEY, KeyEvent.VK_A));

		menu.addSeparator();

		JMenu submenu = new FMenu(DEFAULT_CIPHER_STR);
		for (FortunaCipher cipher : FortunaCipher.values())
			submenu.add(CipherAction.getMenuItem(MAIN_MENU_KEY, cipher));
		menu.add(submenu);

		menu.addSeparator();

		menu.add(new FMenuItem(AppCommand.EDIT_KEYS, KeyEvent.VK_E));

		menu.addSeparator();

		menu.add(new FMenuItem(AppCommand.SHOW_ENTROPY_METRICS, KeyEvent.VK_M));

		menu.addSeparator();

		menu.add(new FMenuItem(AppCommand.GENERATE_GARBAGE, KeyEvent.VK_G));

		menuBar.add(menu);

		// Options menu
		menu = Menu.OPTIONS.menu;
		menu.addMenuListener(this);

		menu.add(new FCheckBoxMenuItem(AppCommand.TOGGLE_SHOW_FULL_PATHNAMES, KeyEvent.VK_F));
		menu.add(new FMenuItem(AppCommand.MANAGE_FILE_ASSOCIATIONS, KeyEvent.VK_A));
		menu.add(new FMenuItem(AppCommand.EDIT_PREFERENCES, KeyEvent.VK_P));

		menuBar.add(menu);

		// Set menu bar
		setJMenuBar(menuBar);


		//----  Tabbed panel

		tabbedPanel = new TabbedPane();
		tabbedPanel.setIgnoreCase(true);
		tabbedPanel.addChangeListener(this);
		tabbedPanel.addMouseListener(this);


		//----  Status panel

		statusPanel = new StatusPanel();


		//----  Main panel

		MainPanel mainPanel = new MainPanel();


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

		// Set transfer handler
		setTransferHandler(FileTransferHandler.INSTANCE);

		// Dispose of window explicitly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Handle window closing
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				AppCommand.EXIT.execute();
			}
		});

		// Respond to changes to data flavours on system clipboard
		getToolkit().getSystemClipboard().addFlavorListener(this);

		// Resize window to its preferred size
		pack();

		// Set minimum size of window
		setMinimumSize(getPreferredSize());

		// Set window to its default size with temporary views
		for (Document.Kind documentKind : Document.Kind.values())
			addView("", null, documentKind.createDocument().createView());
		pack();
		while (tabbedPanel.getNumTabs() > 0)
			removeView(tabbedPanel.getNumTabs() - 1);

		// Set size of window
		AppConfig config = AppConfig.INSTANCE;
		Dimension size = config.getMainWindowSize();
		if ((size != null) && (size.width > 0) && (size.height > 0))
			setSize(size);

		// Set location of window
		setLocation(config.isMainWindowLocation()
								? GuiUtils.getLocationWithinScreen(this, config.getMainWindowLocation())
								: GuiUtils.getComponentLocation(this));

		// Update title and menus
		updateTitleAndMenus();

		// Update status
		updateStatus();

		// Make window visible
		setVisible(true);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ChangeListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void stateChanged(ChangeEvent event)
	{
		if (event.getSource() == tabbedPanel)
		{
			if (isVisible())
			{
				updateTitleAndMenus();
				updateStatus();
			}
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IFileImporter interface
////////////////////////////////////////////////////////////////////////

	@Override
	public boolean canImportFiles()
	{
		return !QanaApp.INSTANCE.isDocumentsFull();
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
		QanaApp.INSTANCE.importFiles(files);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : FlavorListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void flavorsChanged(FlavorEvent event)
	{
		Menu.updateTextDocumentCommands();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ListSelectionListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void valueChanged(ListSelectionEvent event)
	{
		Menu.ARCHIVE.update();
		updateDocumentInfo();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MenuListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void menuCanceled(MenuEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void menuDeselected(MenuEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void menuSelected(MenuEvent event)
	{
		Object eventSource = event.getSource();
		for (Menu menu : Menu.values())
		{
			if (eventSource == menu.menu)
				menu.update();
		}
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

	public int getTabIndex()
	{
		return tabbedPanel.getSelectedIndex();
	}

	//------------------------------------------------------------------

	public void addView(String title,
						String tooltipText,
						View   view)
	{
		tabbedPanel.addComponent(title, new CloseAction(), view);
		int index = tabbedPanel.getNumTabs() - 1;
		tabbedPanel.setTooltipText(index, tooltipText);
		tabbedPanel.setSelectedIndex(index);

		view.requestFocusInWindow();
	}

	//------------------------------------------------------------------

	public void removeView(int index)
	{
		tabbedPanel.removeComponent(index);
	}

	//------------------------------------------------------------------

	public void setView(int  index,
						View view)
	{
		tabbedPanel.setComponent(index, view);
	}

	//------------------------------------------------------------------

	public void selectView(int index)
	{
		tabbedPanel.setSelectedIndex(index);
	}

	//------------------------------------------------------------------

	public void setTabText(int    index,
						   String title,
						   String tooltipText)
	{
		tabbedPanel.setTitle(index, title);
		tabbedPanel.setTooltipText(index, tooltipText);
	}

	//------------------------------------------------------------------

	public void updatePrngCanReseed()
	{
		statusPanel.setPrngCanReseed(QanaApp.INSTANCE.canPrngReseed());
	}

	//------------------------------------------------------------------

	public void updateCipher()
	{
		CipherAction.updateMenuItems();
		statusPanel.setCipher(Utils.getCipher());
	}

	//------------------------------------------------------------------

	public void updateStatus()
	{
		updatePrngCanReseed();
		updateCipher();
		updateKey();
		updateDocumentInfo();
	}

	//------------------------------------------------------------------

	public void updateTitle()
	{
		Document document = QanaApp.INSTANCE.getDocument();
		boolean fullPathname = AppConfig.INSTANCE.isShowFullPathnames();
		setTitle((document == null) ? QanaApp.LONG_NAME + " " + QanaApp.INSTANCE.getVersionString()
									: QanaApp.SHORT_NAME + " - " + document.getTitleString(fullPathname));
	}

	//------------------------------------------------------------------

	public void updateMenus()
	{
		for (Menu menu : Menu.values())
			menu.update();
	}

	//------------------------------------------------------------------

	public void updateTitleAndMenus()
	{
		updateTitle();
		updateMenus();
	}

	//------------------------------------------------------------------

	private void updateKey()
	{
		String documentKeyText = null;
		Document document = QanaApp.INSTANCE.getDocument();
		if (document != null)
		{
			KeyList.Key key = document.getKey();
			if (key != null)
				documentKeyText = DOCUMENT_STR + key.getQuotedName();
		}
		KeyList.Key globalKey = QanaApp.INSTANCE.getGlobalKey();
		statusPanel.setGlobalKeyText((globalKey == null) ? null : GLOBAL_STR + globalKey.getQuotedName());
		statusPanel.setDocumentKeyText(documentKeyText);
	}

	//------------------------------------------------------------------

	private void updateDocumentInfo()
	{
		String str = null;
		Document document = QanaApp.INSTANCE.getDocument();
		if (document != null)
		{
			switch (document.getKind())
			{
				case ARCHIVE:
				{
					ArchiveView view = QanaApp.INSTANCE.getArchiveView();
					if (view != null)
					{
						int numSelected = view.getTable().getSelectedRowCount();
						str = ((numSelected == 0) ? NO_STR : Integer.toString(numSelected)) + " " +
													Utils.getFileString(numSelected) + " " + SELECTED_STR;
					}
					break;
				}

				case TEXT:
				{
					long timestamp = document.getTimestamp();
					if (timestamp != 0)
						str = CalendarTime.timeToString(timestamp, "  ");
					break;
				}
			}
		}
		statusPanel.setDocumentInfoText(str);
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

				contextMenu.add(new FMenuItem(AppCommand.ENCRYPT_FILE));
				contextMenu.add(new FMenuItem(AppCommand.DECRYPT_FILE));
				contextMenu.add(new FMenuItem(AppCommand.VALIDATE_FILE));

				contextMenu.addSeparator();

				contextMenu.add(new FMenuItem(AppCommand.CONCEAL_FILE));
				contextMenu.add(new FMenuItem(AppCommand.RECOVER_FILE));

				contextMenu.addSeparator();

				contextMenu.add(new FMenuItem(AppCommand.SPLIT_FILE));
				contextMenu.add(new FMenuItem(AppCommand.JOIN_FILES));

				contextMenu.addSeparator();

				contextMenu.add(new FMenuItem(AppCommand.ERASE_FILES));

				contextMenu.addSeparator();

				JMenu submenu = new FMenu(DEFAULT_CIPHER_STR);
				for (FortunaCipher cipher : FortunaCipher.values())
					submenu.add(CipherAction.getMenuItem(CONTEXT_MENU_KEY, cipher));
				contextMenu.add(submenu);

				contextMenu.addSeparator();

				contextMenu.add(new FMenuItem(AppCommand.SET_GLOBAL_KEY));
				contextMenu.add(new FMenuItem(AppCommand.CLEAR_GLOBAL_KEY));
				contextMenu.add(new FCheckBoxMenuItem(AppCommand.TOGGLE_AUTO_USE_GLOBAL_KEY));
			}

			// Update commands for menu items
			QanaApp.INSTANCE.updateCommands();

			// Display menu
			contextMenu.show(event.getComponent(), event.getX(), event.getY());
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// MENUS


	private enum Menu
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FILE
		(
			"File",
			KeyEvent.VK_F
		)
		{
			@Override
			protected void update()
			{
				updateAppCommands();
			}
		},

		EDIT
		(
			"Edit",
			KeyEvent.VK_E
		)
		{
			@Override
			protected void update()
			{
				JMenu menu = getMenu();
				menu.removeAll();
				Document document = QanaApp.INSTANCE.getDocument();
				if (document == null)
					menu.setEnabled(false);
				else
				{
					menu.setEnabled(true);
					switch (document.getKind())
					{
						case ARCHIVE:
							updateArchiveDocumentCommands();

							menu.add(new FMenuItem(ArchiveDocument.Command.SELECT_ALL, KeyEvent.VK_A));
							menu.add(new FMenuItem(ArchiveDocument.Command.INVERT_SELECTION,
												   KeyEvent.VK_I));
							break;

						case TEXT:
							updateTextDocumentCommands();

							menu.add(new FMenuItem(TextDocument.Command.UNDO, KeyEvent.VK_U));
							menu.add(new FMenuItem(TextDocument.Command.REDO, KeyEvent.VK_R));
							menu.add(new FMenuItem(TextDocument.Command.CLEAR_EDIT_LIST,
												   KeyEvent.VK_L));

							menu.addSeparator();

							menu.add(new FMenuItem(TextDocument.Command.CUT, KeyEvent.VK_T));
							menu.add(new FMenuItem(TextDocument.Command.COPY, KeyEvent.VK_C));
							menu.add(new FMenuItem(TextDocument.Command.COPY_ALL, KeyEvent.VK_O));
							menu.add(new FMenuItem(TextDocument.Command.PASTE, KeyEvent.VK_P));
							menu.add(new FMenuItem(TextDocument.Command.PASTE_ALL, KeyEvent.VK_S));

							menu.addSeparator();

							menu.add(new FMenuItem(TextDocument.Command.CLEAR, KeyEvent.VK_E));

							menu.addSeparator();

							menu.add(new FMenuItem(TextDocument.Command.SELECT_ALL, KeyEvent.VK_A));

							menu.addSeparator();

							menu.add(new FMenuItem(TextDocument.Command.WRAP, KeyEvent.VK_W));

							break;
					}
				}
			}
		},

		ARCHIVE
		(
			"Archive",
			KeyEvent.VK_A
		)
		{
			@Override
			protected void update()
			{
				getMenu().setEnabled(QanaApp.INSTANCE.getArchiveDocument() != null);
				updateArchiveDocumentCommands();
			}
		},

		TEXT
		(
			"Text",
			KeyEvent.VK_T
		)
		{
			@Override
			protected void update()
			{
				updateAppCommands();
				updateTextDocumentCommands();
			}
		},

		ENCRYPTION
		(
			"Encryption",
			KeyEvent.VK_C
		)
		{
			@Override
			protected void update()
			{
				updateAppCommands();
			}
		},

		OPTIONS
		(
			"Options",
			KeyEvent.VK_O
		)
		{
			@Override
			protected void update()
			{
				updateAppCommands();
			}
		};

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	JMenu	menu;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Menu(String text,
					 int    keyCode)
		{
			menu = new FMenu(text, keyCode);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static void updateAppCommands()
		{
			QanaApp.INSTANCE.updateCommands();
		}

		//--------------------------------------------------------------

		private static void updateArchiveDocumentCommands()
		{
			ArchiveDocument archiveDocument = QanaApp.INSTANCE.getArchiveDocument();
			if (archiveDocument == null)
				ArchiveDocument.Command.setAllEnabled(false);
			else
				archiveDocument.updateCommands();
		}

		//--------------------------------------------------------------

		private static void updateTextDocumentCommands()
		{
			TextDocument textDocument = QanaApp.INSTANCE.getTextDocument();
			if (textDocument == null)
				TextDocument.Command.setAllEnabled(false);
			else
				textDocument.updateCommands();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Abstract methods
	////////////////////////////////////////////////////////////////////

		protected abstract void update();

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		protected JMenu getMenu()
		{
			return menu;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLOSE ACTION CLASS


	private static class CloseAction
		extends AbstractAction
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CloseAction()
		{
			putValue(Action.ACTION_COMMAND_KEY, "");
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ActionListener interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void actionPerformed(ActionEvent event)
		{
			QanaApp.INSTANCE.closeDocument(Integer.parseInt(event.getActionCommand()));
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CIPHER ACTION CLASS


	private static class CipherAction
		extends AbstractAction
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	COMMAND_STR	= "selectCipherKind.";

	////////////////////////////////////////////////////////////////////
	//  Class variables
	////////////////////////////////////////////////////////////////////

		private static	Map<String, Map<FortunaCipher, FRadioButtonMenuItem>>	menuItemMap	= new HashMap<>();

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	FortunaCipher	cipher;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CipherAction(FortunaCipher cipher)
		{
			super(cipher.toString());
			putValue(Action.ACTION_COMMAND_KEY, COMMAND_STR + cipher.getKey());
			this.cipher = cipher;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ActionListener interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void actionPerformed(ActionEvent event)
		{
			AppConfig.INSTANCE.setPrngDefaultCipher(cipher);
			QanaApp.INSTANCE.getMainWindow().updateCipher();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static FRadioButtonMenuItem getMenuItem(String        key,
														FortunaCipher cipher)
		{
			Map<FortunaCipher, FRadioButtonMenuItem> menuItems = menuItemMap.get(key);
			if (menuItems == null)
			{
				menuItems = new EnumMap<>(FortunaCipher.class);
				menuItemMap.put(key, menuItems);
			}

			FRadioButtonMenuItem menuItem = menuItems.get(cipher);
			if (menuItem == null)
			{
				menuItem = new FRadioButtonMenuItem(new CipherAction(cipher),
													Utils.getCipher() == cipher);
				menuItems.put(cipher, menuItem);
			}

			return menuItem;
		}

		//--------------------------------------------------------------

		private static void updateMenuItems()
		{
			FortunaCipher currentCipher = Utils.getCipher();
			for (String key : menuItemMap.keySet())
			{
				Map<FortunaCipher, FRadioButtonMenuItem> menuItems = menuItemMap.get(key);
				for (FortunaCipher cipher : menuItems.keySet())
					menuItems.get(cipher).setSelected(cipher == currentCipher);
			}
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// MAIN PANEL CLASS


	private class MainPanel
		extends JPanel
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	VERTICAL_GAP	= 0;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private MainPanel()
		{
			// Lay out components explicitly
			setLayout(null);

			// Add components
			add(tabbedPanel);
			add(statusPanel);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public Dimension getMinimumSize()
		{
			int width = tabbedPanel.getMinimumSize().width;
			int height = -VERTICAL_GAP;
			for (Component component : getComponents())
				height += component.getMinimumSize().height + VERTICAL_GAP;
			return new Dimension(width, height);
		}

		//--------------------------------------------------------------

		@Override
		public Dimension getPreferredSize()
		{
			int width = tabbedPanel.getPreferredSize().width;
			int height = -VERTICAL_GAP;
			for (Component component : getComponents())
				height += component.getPreferredSize().height + VERTICAL_GAP;
			return new Dimension(width, height);
		}

		//--------------------------------------------------------------

		@Override
		public void doLayout()
		{
			int width = getWidth();
			Dimension statusPanelSize = statusPanel.getPreferredSize();
			Dimension tabbedPanelSize = tabbedPanel.getFrameSize();

			int y = 0;
			tabbedPanel.setBounds(0, y, Math.max(tabbedPanelSize.width, width),
								  Math.max(tabbedPanelSize.height,
										   getHeight() - statusPanelSize.height - VERTICAL_GAP));

			y += tabbedPanel.getHeight() + VERTICAL_GAP;
			statusPanel.setBounds(0, y, Math.min(width, statusPanelSize.width), statusPanelSize.height);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
