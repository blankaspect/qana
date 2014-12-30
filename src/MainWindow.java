/*====================================================================*\

MainWindow.java

Main window class.

\*====================================================================*/


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

import uk.org.blankaspect.crypto.FortunaCipher;

import uk.org.blankaspect.gui.FCheckBoxMenuItem;
import uk.org.blankaspect.gui.FMenu;
import uk.org.blankaspect.gui.FMenuItem;
import uk.org.blankaspect.gui.FRadioButtonMenuItem;
import uk.org.blankaspect.gui.GuiUtilities;
import uk.org.blankaspect.gui.TabbedPanel;

import uk.org.blankaspect.util.CalendarTime;
import uk.org.blankaspect.util.FileImporter;

//----------------------------------------------------------------------


// MAIN WINDOW CLASS


class MainWindow
    extends JFrame
    implements ChangeListener, FileImporter, FlavorListener, ListSelectionListener, MenuListener,
               MouseListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    private static final    String  DEFAULT_CIPHER_STR  = "Default cipher";
    private static final    String  GLOBAL_STR          = "G : ";
    private static final    String  DOCUMENT_STR        = "D : ";
    private static final    String  NO_STR              = "No";
    private static final    String  SELECTED_STR        = "selected";

    private static final    String  MAIN_MENU_KEY       = "mainMenu";
    private static final    String  CONTEXT_MENU_KEY    = "contextMenu";

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
            protected void update( )
            {
                updateAppCommands( );
            }
        },

        EDIT
        (
            "Edit",
            KeyEvent.VK_E
        )
        {
            protected void update( )
            {
                JMenu menu = getMenu( );
                menu.removeAll( );
                Document document = App.getInstance( ).getDocument( );
                if ( document == null )
                    menu.setEnabled( false );
                else
                {
                    menu.setEnabled( true );
                    switch ( document.getKind( ) )
                    {
                        case ARCHIVE:
                            updateArchiveDocumentCommands( );

                            menu.add( new FMenuItem( ArchiveDocument.Command.SELECT_ALL, KeyEvent.VK_A ) );
                            menu.add( new FMenuItem( ArchiveDocument.Command.INVERT_SELECTION,
                                                     KeyEvent.VK_I ) );
                            break;

                        case TEXT:
                            updateTextDocumentCommands( );

                            menu.add( new FMenuItem( TextDocument.Command.UNDO, KeyEvent.VK_U ) );
                            menu.add( new FMenuItem( TextDocument.Command.REDO, KeyEvent.VK_R ) );
                            menu.add( new FMenuItem( TextDocument.Command.CLEAR_EDIT_LIST,
                                                     KeyEvent.VK_L ) );

                            menu.addSeparator( );

                            menu.add( new FMenuItem( TextDocument.Command.CUT, KeyEvent.VK_T ) );
                            menu.add( new FMenuItem( TextDocument.Command.COPY, KeyEvent.VK_C ) );
                            menu.add( new FMenuItem( TextDocument.Command.COPY_ALL, KeyEvent.VK_O ) );
                            menu.add( new FMenuItem( TextDocument.Command.PASTE, KeyEvent.VK_P ) );
                            menu.add( new FMenuItem( TextDocument.Command.PASTE_ALL, KeyEvent.VK_S ) );

                            menu.addSeparator( );

                            menu.add( new FMenuItem( TextDocument.Command.CLEAR, KeyEvent.VK_E ) );

                            menu.addSeparator( );

                            menu.add( new FMenuItem( TextDocument.Command.SELECT_ALL, KeyEvent.VK_A ) );

                            menu.addSeparator( );

                            menu.add( new FMenuItem( TextDocument.Command.WRAP, KeyEvent.VK_W ) );

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
            protected void update( )
            {
                getMenu( ).setEnabled( App.getInstance( ).getArchiveDocument( ) != null );
                updateArchiveDocumentCommands( );
            }
        },

        TEXT
        (
            "Text",
            KeyEvent.VK_T
        )
        {
            protected void update( )
            {
                updateAppCommands( );
                updateTextDocumentCommands( );
            }
        },

        ENCRYPTION
        (
            "Encryption",
            KeyEvent.VK_C
        )
        {
            protected void update( )
            {
                updateAppCommands( );
            }
        },

        OPTIONS
        (
            "Options",
            KeyEvent.VK_O
        )
        {
            protected void update( )
            {
                updateAppCommands( );
            }
        };

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private Menu( String text,
                      int    keyCode )
        {
            menu = new FMenu( text, keyCode );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Class methods
    ////////////////////////////////////////////////////////////////////

        private static void updateAppCommands( )
        {
            App.getInstance( ).updateCommands( );
        }

        //--------------------------------------------------------------

        private static void updateArchiveDocumentCommands( )
        {
            ArchiveDocument archiveDocument = App.getInstance( ).getArchiveDocument( );
            if ( archiveDocument == null )
                ArchiveDocument.Command.setAllEnabled( false );
            else
                archiveDocument.updateCommands( );
        }

        //--------------------------------------------------------------

        private static void updateTextDocumentCommands( )
        {
            TextDocument textDocument = App.getInstance( ).getTextDocument( );
            if ( textDocument == null )
                TextDocument.Command.setAllEnabled( false );
            else
                textDocument.updateCommands( );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Abstract methods
    ////////////////////////////////////////////////////////////////////

        protected abstract void update( );

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods
    ////////////////////////////////////////////////////////////////////

        protected JMenu getMenu( )
        {
            return menu;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        private JMenu   menu;

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

        private CloseAction( )
        {
            putValue( Action.ACTION_COMMAND_KEY, new String( ) );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : ActionListener interface
    ////////////////////////////////////////////////////////////////////

        public void actionPerformed( ActionEvent event )
        {
            App.getInstance( ).closeDocument( Integer.parseInt( event.getActionCommand( ) ) );
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

        private static final    String  COMMAND_STR = "selectCipherKind.";

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private CipherAction( FortunaCipher cipher )
        {
            super( cipher.toString( ) );
            putValue( Action.ACTION_COMMAND_KEY, COMMAND_STR + cipher.getKey( ) );
            this.cipher = cipher;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : ActionListener interface
    ////////////////////////////////////////////////////////////////////

        public void actionPerformed( ActionEvent event )
        {
            AppConfig.getInstance( ).setPrngDefaultCipher( cipher );
            App.getInstance( ).getMainWindow( ).updateCipher( );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Class methods
    ////////////////////////////////////////////////////////////////////

        private static FRadioButtonMenuItem getMenuItem( String        key,
                                                         FortunaCipher cipher )
        {
            Map<FortunaCipher, FRadioButtonMenuItem> menuItems = menuItemMap.get( key );
            if ( menuItems == null )
            {
                menuItems = new EnumMap<>( FortunaCipher.class );
                menuItemMap.put( key, menuItems );
            }

            FRadioButtonMenuItem menuItem = menuItems.get( cipher );
            if ( menuItem == null )
            {
                menuItem = new FRadioButtonMenuItem( new CipherAction( cipher ),
                                                     Util.getCipher( ) == cipher );
                menuItems.put( cipher, menuItem );
            }

            return menuItem;
        }

        //--------------------------------------------------------------

        private static void updateMenuItems( )
        {
            FortunaCipher currentCipher = Util.getCipher( );
            for ( String key : menuItemMap.keySet( ) )
            {
                Map<FortunaCipher, FRadioButtonMenuItem> menuItems = menuItemMap.get( key );
                for ( FortunaCipher cipher : menuItems.keySet( ) )
                    menuItems.get( cipher ).setSelected( cipher == currentCipher );
            }
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Class variables
    ////////////////////////////////////////////////////////////////////

        private static  Map<String, Map<FortunaCipher, FRadioButtonMenuItem>>   menuItemMap =
                                                                                        new HashMap<>( );

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        private FortunaCipher   cipher;

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

        private static final    int VERTICAL_GAP    = 0;

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private MainPanel( )
        {
            // Lay out components explicitly
            setLayout( null );

            // Add components
            add( tabbedPanel );
            add( statusPanel );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : overriding methods
    ////////////////////////////////////////////////////////////////////

        @Override
        public Dimension getMinimumSize( )
        {
            int width = tabbedPanel.getMinimumSize( ).width;
            int height = -VERTICAL_GAP;
            for ( Component component : getComponents( ) )
                height += component.getMinimumSize( ).height + VERTICAL_GAP;
            return new Dimension( width, height );
        }

        //--------------------------------------------------------------

        @Override
        public Dimension getPreferredSize( )
        {
            int width = tabbedPanel.getPreferredSize( ).width;
            int height = -VERTICAL_GAP;
            for ( Component component : getComponents( ) )
                height += component.getPreferredSize( ).height + VERTICAL_GAP;
            return new Dimension( width, height );
        }

        //--------------------------------------------------------------

        @Override
        public void doLayout( )
        {
            int width = getWidth( );
            Dimension statusPanelSize = statusPanel.getPreferredSize( );
            Dimension tabbedPanelSize = tabbedPanel.getFrameSize( );

            int y = 0;
            tabbedPanel.setBounds( 0, y, Math.max( tabbedPanelSize.width, width ),
                                   Math.max( tabbedPanelSize.height,
                                             getHeight( ) - statusPanelSize.height - VERTICAL_GAP ) );

            y += tabbedPanel.getHeight( ) + VERTICAL_GAP;
            statusPanel.setBounds( 0, y, Math.min( width, statusPanelSize.width ), statusPanelSize.height );
        }

        //--------------------------------------------------------------

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    public MainWindow( )
    {

        // Set icons
        setIconImages( AppIcon.getAppIconImages( ) );


        //----  Menu bar

        JMenuBar menuBar = new JMenuBar( );
        menuBar.setBorder( null );

        // File menu
        JMenu menu = Menu.FILE.menu;
        menu.addMenuListener( this );

        menu.add( new FMenuItem( AppCommand.CREATE_FILE, KeyEvent.VK_N ) );
        menu.add( new FMenuItem( AppCommand.OPEN_FILE, KeyEvent.VK_O ) );
        menu.add( new FMenuItem( AppCommand.REVERT_FILE, KeyEvent.VK_R ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( AppCommand.CLOSE_FILE, KeyEvent.VK_C ) );
        menu.add( new FMenuItem( AppCommand.CLOSE_ALL_FILES, KeyEvent.VK_L ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( AppCommand.SAVE_FILE, KeyEvent.VK_S ) );
        menu.add( new FMenuItem( AppCommand.SAVE_FILE_AS, KeyEvent.VK_A ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( AppCommand.ENCRYPT_FILE, KeyEvent.VK_E ) );
        menu.add( new FMenuItem( AppCommand.DECRYPT_FILE, KeyEvent.VK_D ) );
        menu.add( new FMenuItem( AppCommand.VALIDATE_FILE, KeyEvent.VK_T ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( AppCommand.CONCEAL_FILE, KeyEvent.VK_I ) );
        menu.add( new FMenuItem( AppCommand.RECOVER_FILE, KeyEvent.VK_V ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( AppCommand.SPLIT_FILE, KeyEvent.VK_P ) );
        menu.add( new FMenuItem( AppCommand.JOIN_FILES, KeyEvent.VK_J ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( AppCommand.ERASE_FILES, KeyEvent.VK_F ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( AppCommand.EXIT, KeyEvent.VK_X ) );

        menuBar.add( menu );

        // Edit menu
        menu = Menu.EDIT.menu;
        menu.addMenuListener( this );

        menuBar.add( menu );

        // Archive menu
        menu = Menu.ARCHIVE.menu;
        menu.addMenuListener( this );

        menu.add( new FMenuItem( ArchiveDocument.Command.CHOOSE_ARCHIVE_DIRECTORY, KeyEvent.VK_C ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( ArchiveDocument.Command.ADD_FILES, KeyEvent.VK_A ) );
        menu.add( new FMenuItem( ArchiveDocument.Command.EXTRACT_FILES, KeyEvent.VK_E ) );
        menu.add( new FMenuItem( ArchiveDocument.Command.VALIDATE_FILES, KeyEvent.VK_V ) );
        menu.add( new FMenuItem( ArchiveDocument.Command.DELETE_FILES, KeyEvent.VK_D ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( ArchiveDocument.Command.DISPLAY_FILE_LIST, KeyEvent.VK_F ) );
        menu.add( new FMenuItem( ArchiveDocument.Command.DISPLAY_FILE_MAP, KeyEvent.VK_M ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( ArchiveDocument.Command.SET_KEY, KeyEvent.VK_K ) );
        menu.add( new FMenuItem( ArchiveDocument.Command.CLEAR_KEY, KeyEvent.VK_L ) );

        menuBar.add( menu );

        // Text menu
        menu = Menu.TEXT.menu;
        menu.addMenuListener( this );

        menu.add( new FMenuItem( AppCommand.CREATE_TEXT, KeyEvent.VK_N ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( TextDocument.Command.ENCRYPT, KeyEvent.VK_E ) );
        menu.add( new FMenuItem( TextDocument.Command.DECRYPT, KeyEvent.VK_D ) );
        menu.add( new FCheckBoxMenuItem( TextDocument.Command.TOGGLE_WRAP_CIPHERTEXT_IN_XML,
                                         KeyEvent.VK_X ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( TextDocument.Command.CONCEAL, KeyEvent.VK_C ) );
        menu.add( new FMenuItem( AppCommand.RECOVER_TEXT, KeyEvent.VK_R ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( TextDocument.Command.SET_KEY, KeyEvent.VK_K ) );
        menu.add( new FMenuItem( TextDocument.Command.CLEAR_KEY, KeyEvent.VK_L ) );

        menuBar.add( menu );

        // Encryption menu
        menu = Menu.ENCRYPTION.menu;
        menu.addMenuListener( this );

        menu.add( new FMenuItem( AppCommand.SET_GLOBAL_KEY, KeyEvent.VK_K ) );
        menu.add( new FMenuItem( AppCommand.CLEAR_GLOBAL_KEY, KeyEvent.VK_C ) );
        menu.add( new FCheckBoxMenuItem( AppCommand.TOGGLE_AUTO_USE_GLOBAL_KEY, KeyEvent.VK_A ) );

        menu.addSeparator( );

        JMenu submenu = new FMenu( DEFAULT_CIPHER_STR );
        for ( FortunaCipher cipher : FortunaCipher.values( ) )
            submenu.add( CipherAction.getMenuItem( MAIN_MENU_KEY, cipher ) );
        menu.add( submenu );

        menu.addSeparator( );

        menu.add( new FMenuItem( AppCommand.EDIT_KEYS, KeyEvent.VK_E ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( AppCommand.SHOW_ENTROPY_METRICS, KeyEvent.VK_M ) );

        menu.addSeparator( );

        menu.add( new FMenuItem( AppCommand.GENERATE_GARBAGE, KeyEvent.VK_G ) );

        menuBar.add( menu );

        // Options menu
        menu = Menu.OPTIONS.menu;
        menu.addMenuListener( this );

        menu.add( new FCheckBoxMenuItem( AppCommand.TOGGLE_SHOW_FULL_PATHNAMES, KeyEvent.VK_F ) );
        menu.add( new FMenuItem( AppCommand.MANAGE_FILE_ASSOCIATIONS, KeyEvent.VK_A ) );
        menu.add( new FMenuItem( AppCommand.EDIT_PREFERENCES, KeyEvent.VK_P ) );

        menuBar.add( menu );

        // Set menu bar
        setJMenuBar( menuBar );


        //----  Tabbed panel

        tabbedPanel = new TabbedPanel( );
        tabbedPanel.setIgnoreCase( true );
        tabbedPanel.addChangeListener( this );
        tabbedPanel.addMouseListener( this );


        //----  Status panel

        statusPanel = new StatusPanel( );


        //----  Main panel

        MainPanel mainPanel = new MainPanel( );


        //----  Window

        // Set content pane
        setContentPane( mainPanel );

        // Set transfer handler
        setTransferHandler( FileTransferHandler.getInstance( ) );

        // Dispose of window explicitly
        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );

        // Handle window closing
        addWindowListener( new WindowAdapter( )
        {
            @Override
            public void windowClosing( WindowEvent event )
            {
                AppCommand.EXIT.execute( );
            }
        } );

        // Respond to changes to data flavours on system clipboard
        getToolkit( ).getSystemClipboard( ).addFlavorListener( this );

        // Resize window to its preferred size
        pack( );

        // Set minimum size of window
        setMinimumSize( getPreferredSize( ) );

        // Set window to its default size with temporary views
        for ( Document.Kind documentKind : Document.Kind.values( ) )
            addView( new String( ), null, documentKind.createDocument( ).createView( ) );
        pack( );
        while ( tabbedPanel.getNumTabs( ) > 0 )
            removeView( tabbedPanel.getNumTabs( ) - 1 );

        // Set location of window
        AppConfig config = AppConfig.getInstance( );
        if ( config.isMainWindowLocation( ) )
            setLocation( GuiUtilities.getLocationWithinScreen( this, config.getMainWindowLocation( ) ) );

        // Set size of window
        Dimension size = config.getMainWindowSize( );
        if ( (size != null) && (size.width > 0) && (size.height > 0) )
            setSize( size );

        // Update title and menus
        updateTitleAndMenus( );

        // Update status
        updateStatus( );

        // Make window visible
        setVisible( true );

    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ChangeListener interface
////////////////////////////////////////////////////////////////////////

    public void stateChanged( ChangeEvent event )
    {
        if ( event.getSource( ) == tabbedPanel )
        {
            if ( isVisible( ) )
            {
                updateTitleAndMenus( );
                updateStatus( );
            }
        }
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : FileImporter interface
////////////////////////////////////////////////////////////////////////

    public boolean canImportFiles( )
    {
        return !App.getInstance( ).isDocumentsFull( );
    }

    //------------------------------------------------------------------

    public boolean canImportMultipleFiles( )
    {
        return true;
    }

    //------------------------------------------------------------------

    public void importFiles( File[] files )
    {
        App.getInstance( ).importFiles( files );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : FlavorListener interface
////////////////////////////////////////////////////////////////////////

    public void flavorsChanged( FlavorEvent event )
    {
        Menu.updateTextDocumentCommands( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ListSelectionListener interface
////////////////////////////////////////////////////////////////////////

    public void valueChanged( ListSelectionEvent event )
    {
        Menu.ARCHIVE.update( );
        updateDocumentInfo( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MenuListener interface
////////////////////////////////////////////////////////////////////////

    public void menuCanceled( MenuEvent event )
    {
        // do nothing
    }

    //------------------------------------------------------------------

    public void menuDeselected( MenuEvent event )
    {
        // do nothing
    }

    //------------------------------------------------------------------

    public void menuSelected( MenuEvent event )
    {
        Object eventSource = event.getSource( );
        for ( Menu menu : Menu.values( ) )
        {
            if ( eventSource == menu.menu )
                menu.update( );
        }
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MouseListener interface
////////////////////////////////////////////////////////////////////////

    public void mouseClicked( MouseEvent event )
    {
        // do nothing
    }

    //------------------------------------------------------------------

    public void mouseEntered( MouseEvent event )
    {
        // do nothing
    }

    //------------------------------------------------------------------

    public void mouseExited( MouseEvent event )
    {
        // do nothing
    }

    //------------------------------------------------------------------

    public void mousePressed( MouseEvent event )
    {
        showContextMenu( event );
    }

    //------------------------------------------------------------------

    public void mouseReleased( MouseEvent event )
    {
        showContextMenu( event );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    public int getTabIndex( )
    {
        return tabbedPanel.getSelectedIndex( );
    }

    //------------------------------------------------------------------

    public void addView( String title,
                         String tooltipText,
                         View   view )
    {
        tabbedPanel.addComponent( title, new CloseAction( ), view );
        int index = tabbedPanel.getNumTabs( ) - 1;
        tabbedPanel.setTooltipText( index, tooltipText );
        tabbedPanel.setSelectedIndex( index );

        view.requestFocusInWindow( );
    }

    //------------------------------------------------------------------

    public void removeView( int index )
    {
        tabbedPanel.removeComponent( index );
    }

    //------------------------------------------------------------------

    public void setView( int  index,
                         View view )
    {
        tabbedPanel.setComponent( index, view );
    }

    //------------------------------------------------------------------

    public void selectView( int index )
    {
        tabbedPanel.setSelectedIndex( index );
    }

    //------------------------------------------------------------------

    public void setTabText( int    index,
                            String title,
                            String tooltipText )
    {
        tabbedPanel.setTitle( index, title );
        tabbedPanel.setTooltipText( index, tooltipText );
    }

    //------------------------------------------------------------------

    public void updatePrngCanReseed( )
    {
        statusPanel.setPrngCanReseed( App.getInstance( ).canPrngReseed( ) );
    }

    //------------------------------------------------------------------

    public void updateCipher( )
    {
        CipherAction.updateMenuItems( );
        statusPanel.setCipher( Util.getCipher( ) );
    }

    //------------------------------------------------------------------

    public void updateStatus( )
    {
        updatePrngCanReseed( );
        updateCipher( );
        updateKey( );
        updateDocumentInfo( );
    }

    //------------------------------------------------------------------

    public void updateTitle( )
    {
        Document document = App.getInstance( ).getDocument( );
        boolean fullPathname = AppConfig.getInstance( ).isShowFullPathnames( );
        setTitle( (document == null) ? App.LONG_NAME + " " + App.getInstance( ).getVersionString( )
                                     : App.SHORT_NAME + " - " + document.getTitleString( fullPathname ) );
    }

    //------------------------------------------------------------------

    public void updateMenus( )
    {
        for ( Menu menu : Menu.values( ) )
            menu.update( );
    }

    //------------------------------------------------------------------

    public void updateTitleAndMenus( )
    {
        updateTitle( );
        updateMenus( );
    }

    //------------------------------------------------------------------

    private void updateKey( )
    {
        String documentKeyText = null;
        Document document = App.getInstance( ).getDocument( );
        if ( document != null )
        {
            KeyList.Key key = document.getKey( );
            if ( key != null )
                documentKeyText = DOCUMENT_STR + key.getQuotedName( );
        }
        KeyList.Key globalKey = App.getInstance( ).getGlobalKey( );
        statusPanel.setGlobalKeyText( (globalKey == null) ? null
                                                          : GLOBAL_STR + globalKey.getQuotedName( ) );
        statusPanel.setDocumentKeyText( documentKeyText );
    }

    //------------------------------------------------------------------

    private void updateDocumentInfo( )
    {
        String str = null;
        Document document = App.getInstance( ).getDocument( );
        if ( document != null )
        {
            switch ( document.getKind( ) )
            {
                case ARCHIVE:
                {
                    ArchiveView view = App.getInstance( ).getArchiveView( );
                    if ( view != null )
                    {
                        int numSelected = view.getTable( ).getSelectedRowCount( );
                        str = ((numSelected == 0) ? NO_STR : Integer.toString( numSelected )) + " " +
                                                    Util.getFileString( numSelected ) + " " + SELECTED_STR;
                    }
                    break;
                }

                case TEXT:
                {
                    long timestamp = document.getTimestamp( );
                    if ( timestamp != 0 )
                        str = CalendarTime.timeToString( timestamp, "  " );
                    break;
                }
            }
        }
        statusPanel.setDocumentInfoText( str );
    }

    //------------------------------------------------------------------

    private void showContextMenu( MouseEvent event )
    {
        if ( event.isPopupTrigger( ) )
        {
            // Create context menu
            if ( contextMenu == null )
            {
                contextMenu = new JPopupMenu( );

                contextMenu.add( new FMenuItem( AppCommand.ENCRYPT_FILE ) );
                contextMenu.add( new FMenuItem( AppCommand.DECRYPT_FILE ) );
                contextMenu.add( new FMenuItem( AppCommand.VALIDATE_FILE ) );

                contextMenu.addSeparator( );

                contextMenu.add( new FMenuItem( AppCommand.CONCEAL_FILE ) );
                contextMenu.add( new FMenuItem( AppCommand.RECOVER_FILE ) );

                contextMenu.addSeparator( );

                contextMenu.add( new FMenuItem( AppCommand.SPLIT_FILE ) );
                contextMenu.add( new FMenuItem( AppCommand.JOIN_FILES ) );

                contextMenu.addSeparator( );

                contextMenu.add( new FMenuItem( AppCommand.ERASE_FILES ) );

                contextMenu.addSeparator( );

                JMenu submenu = new FMenu( DEFAULT_CIPHER_STR );
                for ( FortunaCipher cipher : FortunaCipher.values( ) )
                    submenu.add( CipherAction.getMenuItem( CONTEXT_MENU_KEY, cipher ) );
                contextMenu.add( submenu );

                contextMenu.addSeparator( );

                contextMenu.add( new FMenuItem( AppCommand.SET_GLOBAL_KEY ) );
                contextMenu.add( new FMenuItem( AppCommand.CLEAR_GLOBAL_KEY ) );
                contextMenu.add( new FCheckBoxMenuItem( AppCommand.TOGGLE_AUTO_USE_GLOBAL_KEY ) );
            }

            // Update commands for menu items
            App.getInstance( ).updateCommands( );

            // Display menu
            contextMenu.show( event.getComponent( ), event.getX( ), event.getY( ) );
        }
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private TabbedPanel tabbedPanel;
    private StatusPanel statusPanel;
    private JPopupMenu  contextMenu;

}

//----------------------------------------------------------------------
