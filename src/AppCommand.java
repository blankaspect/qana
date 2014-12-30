/*====================================================================*\

AppCommand.java

Application command enumeration.

\*====================================================================*/


// IMPORTS


import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.KeyStroke;

import uk.org.blankaspect.util.Command;

//----------------------------------------------------------------------


// APPLICATION COMMAND ENUMERATION


enum AppCommand
    implements Action
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    // Commands

    TIMER_EXPIRED
    (
        "timerExpired"
    ),

    CREATE_FILE
    (
        "createFile",
        "New archive" + AppConstants.ELLIPSIS_STR,
        KeyStroke.getKeyStroke( KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK )
    ),

    OPEN_FILE
    (
        "openFile",
        "Open archive" + AppConstants.ELLIPSIS_STR,
        KeyStroke.getKeyStroke( KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK )
    ),

    REVERT_FILE
    (
        "revertFile",
        "Revert archive"
    ),

    CLOSE_FILE
    (
        "closeFile",
        "Close document",
        KeyStroke.getKeyStroke( KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK )
    ),

    CLOSE_ALL_FILES
    (
        "closeAllFiles",
        "Close all documents"
    ),

    SAVE_FILE
    (
        "saveFile",
        "Save archive",
        KeyStroke.getKeyStroke( KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK )
    ),

    SAVE_FILE_AS
    (
        "saveFileAs",
        "Save archive as" + AppConstants.ELLIPSIS_STR
    ),

    ENCRYPT_FILE
    (
        "encryptFile",
        "Encrypt file" + AppConstants.ELLIPSIS_STR,
        KeyStroke.getKeyStroke( KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK )
    ),

    DECRYPT_FILE
    (
        "decryptFile",
        "Decrypt file" + AppConstants.ELLIPSIS_STR,
        KeyStroke.getKeyStroke( KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK )
    ),

    VALIDATE_FILE
    (
        "validateFile",
        "Validate encrypted file" + AppConstants.ELLIPSIS_STR
    ),

    CONCEAL_FILE
    (
        "concealFile",
        "Conceal file in image" + AppConstants.ELLIPSIS_STR
    ),

    RECOVER_FILE
    (
        "recoverFile",
        "Recover file from image" + AppConstants.ELLIPSIS_STR
    ),

    SPLIT_FILE
    (
        "splitFile",
        "Split file" + AppConstants.ELLIPSIS_STR
    ),

    JOIN_FILES
    (
        "joinFiles",
        "Join files" + AppConstants.ELLIPSIS_STR
    ),

    ERASE_FILES
    (
        "eraseFiles",
        "Erase files" + AppConstants.ELLIPSIS_STR,
        KeyStroke.getKeyStroke( KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK )
    ),

    EXIT
    (
        "exit",
        "Exit"
    ),

    CREATE_TEXT
    (
        "createText",
        "New text",
        KeyStroke.getKeyStroke( KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK )
    ),

    RECOVER_TEXT
    (
        "recoverText",
        "Recover text from image" + AppConstants.ELLIPSIS_STR
    ),

    SET_GLOBAL_KEY
    (
        "setGlobalKey",
        "Set global key" + AppConstants.ELLIPSIS_STR,
        KeyStroke.getKeyStroke( KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK )
    ),

    CLEAR_GLOBAL_KEY
    (
        "clearGlobalKey",
        "Clear global key" + AppConstants.ELLIPSIS_STR,
        KeyStroke.getKeyStroke( KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK )
    ),

    TOGGLE_AUTO_USE_GLOBAL_KEY
    (
        "toggleAutoUseGlobalKey",
        "Automatically use global key",
        KeyStroke.getKeyStroke( KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK )
    ),

    EDIT_KEYS
    (
        "editKeys",
        "Edit keys" + AppConstants.ELLIPSIS_STR,
        KeyStroke.getKeyStroke( KeyEvent.VK_F12, 0 )
    ),

    SHOW_ENTROPY_METRICS
    (
        "showEntropyMetrics",
        "Show entropy metrics" + AppConstants.ELLIPSIS_STR,
        KeyStroke.getKeyStroke( KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK )
    ),

    GENERATE_GARBAGE
    (
        "generateGarbage",
        "Generate garbage" + AppConstants.ELLIPSIS_STR
    ),

    TOGGLE_SHOW_FULL_PATHNAMES
    (
        "toggleShowFullPathnames",
        "Show full pathnames"
    ),

    MANAGE_FILE_ASSOCIATIONS
    (
        "manageFileAssociations",
        "Manage file associations" + AppConstants.ELLIPSIS_STR
    ),

    EDIT_PREFERENCES
    (
        "editPreferences",
        "Preferences" + AppConstants.ELLIPSIS_STR
    );

    //------------------------------------------------------------------

    // Property keys
    interface Property
    {
        String  COMPONENT   = "component";
        String  FILES       = "files";
    }

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private AppCommand( String key )
    {
        command = new Command( this );
        putValue( Action.ACTION_COMMAND_KEY, key );
    }

    //------------------------------------------------------------------

    private AppCommand( String key,
                        String name )
    {
        this( key );
        putValue( Action.NAME, name );
    }

    //------------------------------------------------------------------

    private AppCommand( String    key,
                        String    name,
                        KeyStroke acceleratorKey )
    {
        this( key, name );
        putValue( Action.ACCELERATOR_KEY, acceleratorKey );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

    public static void setAllEnabled( boolean enabled )
    {
        for ( AppCommand command : values( ) )
            command.setEnabled( enabled );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : Action interface
////////////////////////////////////////////////////////////////////////

    public void addPropertyChangeListener( PropertyChangeListener listener )
    {
        command.addPropertyChangeListener( listener );
    }

    //------------------------------------------------------------------

    public Object getValue( String key )
    {
        return command.getValue( key );
    }

    //------------------------------------------------------------------

    public boolean isEnabled( )
    {
        return command.isEnabled( );
    }

    //------------------------------------------------------------------

    public void putValue( String key,
                          Object value )
    {
        command.putValue( key, value );
    }

    //------------------------------------------------------------------

    public void removePropertyChangeListener( PropertyChangeListener listener )
    {
        command.removePropertyChangeListener( listener );
    }

    //------------------------------------------------------------------

    public void setEnabled( boolean enabled )
    {
        command.setEnabled( enabled );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

    public void actionPerformed( ActionEvent event )
    {
        App.getInstance( ).executeCommand( this );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    public void setSelected( boolean selected )
    {
        putValue( Action.SELECTED_KEY, selected );
    }

    //------------------------------------------------------------------

    public void execute( )
    {
        actionPerformed( null );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private Command command;

}

//----------------------------------------------------------------------
