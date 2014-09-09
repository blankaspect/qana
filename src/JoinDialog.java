/*====================================================================*\

JoinDialog.java

Join dialog box class.

\*====================================================================*/


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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import uk.org.blankaspect.exception.AppException;
import uk.org.blankaspect.exception.FileException;

import uk.org.blankaspect.gui.FButton;
import uk.org.blankaspect.gui.FLabel;
import uk.org.blankaspect.gui.GuiUtilities;
import uk.org.blankaspect.gui.PathnamePanel;

import uk.org.blankaspect.textfield.PathnameField;

import uk.org.blankaspect.util.FileImporter;
import uk.org.blankaspect.util.KeyAction;

//----------------------------------------------------------------------


// JOIN DIALOG BOX CLASS


class JoinDialog
    extends JDialog
    implements ActionListener, DocumentListener, FileImporter
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    private static final    String  TITLE_STR                   = "Join files";
    private static final    String  INPUT_DIRECTORY_STR         = "Input directory:";
    private static final    String  OUTPUT_FILE_STR             = "Output file:";
    private static final    String  JOIN_STR                    = "Join";
    private static final    String  INPUT_DIRECTORY_TITLE_STR   = JOIN_STR + " | Input directory";
    private static final    String  OUTPUT_FILE_TITLE_STR       = JOIN_STR + " | Output file";
    private static final    String  SELECT_STR                  = "Select";
    private static final    String  SELECT_FILE_STR             = "Select file";
    private static final    String  SELECT_DIRECTORY_STR        = "Select directory";

    // Commands
    private interface Command
    {
        String  CHOOSE_INPUT_DIRECTORY  = "chooseInputDirectory";
        String  CHOOSE_OUTPUT_FILE      = "chooseOutputFile";
        String  ACCEPT                  = "accept";
        String  CLOSE                   = "close";
    }

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


    // ERROR IDENTIFIERS


    private enum ErrorId
        implements AppException.Id
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        DIRECTORY_DOES_NOT_EXIST
        ( "The directory does not exist." ),

        NOT_A_FILE
        ( "The output pathname does not denote a normal file." ),

        NOT_A_DIRECTORY
        ( "The input pathname does not denote a directory." ),

        NO_INPUT_DIRECTORY
        ( "No input directory was specified." ),

        NO_OUTPUT_FILE
        ( "No output file was specified." );

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private ErrorId( String message )
        {
            this.message = message;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : AppException.Id interface
    ////////////////////////////////////////////////////////////////////

        public String getMessage( )
        {
            return message;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        private String  message;

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


    // RESULT CLASS


    public static class Result
    {

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        public Result( File inDirectory,
                       File outFile )
        {
            this.inDirectory = inDirectory;
            this.outFile = outFile;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        File    inDirectory;
        File    outFile;

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


    // WINDOW EVENT HANDLER CLASS


    private class WindowEventHandler
        extends WindowAdapter
    {

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private WindowEventHandler( )
        {
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : overriding methods
    ////////////////////////////////////////////////////////////////////

        @Override
        public void windowClosing( WindowEvent event )
        {
            onClose( );
        }

        //--------------------------------------------------------------

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private JoinDialog( Window owner )
    {

        // Call superclass constructor
        super( owner, TITLE_STR, Dialog.ModalityType.APPLICATION_MODAL );

        // Set icons
        setIconImages( owner.getIconImages( ) );


        //----  Control panel

        GridBagLayout gridBag = new GridBagLayout( );
        GridBagConstraints gbc = new GridBagConstraints( );

        JPanel controlPanel = new JPanel( gridBag );
        GuiUtilities.setPaddedLineBorder( controlPanel );

        int gridY = 0;

        // Label: input directory
        JLabel inDirectoryLabel = new FLabel( INPUT_DIRECTORY_STR );

        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = AppConstants.COMPONENT_INSETS;
        gridBag.setConstraints( inDirectoryLabel, gbc );
        controlPanel.add( inDirectoryLabel );

        // Panel: input directory
        inputDirectoryField = new FPathnameField( inputDirectory );
        inputDirectoryField.getDocument( ).addDocumentListener( this );
        PathnamePanel inDirectoryPanel = new PathnamePanel( inputDirectoryField,
                                                            Command.CHOOSE_INPUT_DIRECTORY, this );

        gbc.gridx = 1;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = AppConstants.COMPONENT_INSETS;
        gridBag.setConstraints( inDirectoryPanel, gbc );
        controlPanel.add( inDirectoryPanel );

        // Label: output file
        JLabel outFileLabel = new FLabel( OUTPUT_FILE_STR );

        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = AppConstants.COMPONENT_INSETS;
        gridBag.setConstraints( outFileLabel, gbc );
        controlPanel.add( outFileLabel );

        // Panel: output file
        outputFileField = new FPathnameField( outputFile );
        outputFileField.getDocument( ).addDocumentListener( this );
        PathnamePanel outputFilePanel = new PathnamePanel( outputFileField, Command.CHOOSE_OUTPUT_FILE,
                                                           this );

        gbc.gridx = 1;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = AppConstants.COMPONENT_INSETS;
        gridBag.setConstraints( outputFilePanel, gbc );
        controlPanel.add( outputFilePanel );


        //----  Button panel

        JPanel buttonPanel = new JPanel( new GridLayout( 1, 0, 8, 0 ) );
        buttonPanel.setBorder( BorderFactory.createEmptyBorder( 3, 8, 3, 8 ) );

        // Button: join
        joinButton = new FButton( JOIN_STR );
        joinButton.setActionCommand( Command.ACCEPT );
        joinButton.addActionListener( this );
        buttonPanel.add( joinButton );

        // Button: cancel
        JButton cancelButton = new FButton( AppConstants.CANCEL_STR );
        cancelButton.setActionCommand( Command.CLOSE );
        cancelButton.addActionListener( this );
        buttonPanel.add( cancelButton );


        //----  Main panel

        JPanel mainPanel = new JPanel( gridBag );
        mainPanel.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) );

        gridY = 0;

        gbc.gridx = 0;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets( 0, 0, 0, 0 );
        gridBag.setConstraints( controlPanel, gbc );
        mainPanel.add( controlPanel );

        gbc.gridx = 0;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets( 3, 0, 0, 0 );
        gridBag.setConstraints( buttonPanel, gbc );
        mainPanel.add( buttonPanel );

        // Add commands to action map
        KeyAction.create( mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                          KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), Command.CLOSE, this );

        // Update components
        updateAcceptButton( );


        //----  Window

        // Set content pane
        setContentPane( mainPanel );

        // Set transfer handler
        setTransferHandler( FileTransferHandler.getInstance( ) );

        // Dispose of window explicitly
        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );

        // Handle window events
        addWindowListener( new WindowEventHandler( ) );

        // Prevent dialog from being resized
        setResizable( false );

        // Resize dialog to its preferred size
        pack( );

        // Set location of dialog box
        if ( location == null )
            location = GuiUtilities.getComponentLocation( this, owner );
        setLocation( location );

        // Set default button
        getRootPane( ).setDefaultButton( joinButton );

        // Show dialog
        setVisible( true );

    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

    public static Result showDialog( Component parent )
    {
        return new JoinDialog( GuiUtilities.getWindow( parent ) ).getResult( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

    public void actionPerformed( ActionEvent event )
    {
        String command = event.getActionCommand( );

        if ( command.equals( Command.CHOOSE_INPUT_DIRECTORY ) )
            onChooseInputDirectory( );

        else if ( command.equals( Command.CHOOSE_OUTPUT_FILE ) )
            onChooseOutputFile( );

        else if ( command.equals( Command.ACCEPT ) )
            onAccept( );

        else if ( command.equals( Command.CLOSE ) )
            onClose( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : DocumentListener interface
////////////////////////////////////////////////////////////////////////

    public void changedUpdate( DocumentEvent event )
    {
        // do nothing
    }

    //------------------------------------------------------------------

    public void insertUpdate( DocumentEvent event )
    {
        updateAcceptButton( );
    }

    //------------------------------------------------------------------

    public void removeUpdate( DocumentEvent event )
    {
        updateAcceptButton( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : FileImporter interface
////////////////////////////////////////////////////////////////////////

    public boolean canImportFiles( )
    {
        return true;
    }

    //------------------------------------------------------------------

    public boolean canImportMultipleFiles( )
    {
        return false;
    }

    //------------------------------------------------------------------

    public void importFiles( File[] files )
    {
        if ( files[0].isDirectory( ) )
            inputDirectoryField.setFile( files[0] );
        else
            outputFileField.setFile( files[0] );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    private Result getResult( )
    {
        return ( accepted ? new Result( inputDirectory, outputFile ) : null );
    }

    //------------------------------------------------------------------

    private void updateAcceptButton( )
    {
        joinButton.setEnabled( !inputDirectoryField.isEmpty( ) && !outputFileField.isEmpty( ) );
    }

    //------------------------------------------------------------------

    private void validateUserInput( )
        throws AppException
    {
        // Input directory
        try
        {
            if ( inputDirectoryField.isEmpty( ) )
                throw new AppException( ErrorId.NO_INPUT_DIRECTORY );
            File directory = inputDirectoryField.getFile( );
            if ( !directory.exists( ) )
                throw new FileException( ErrorId.DIRECTORY_DOES_NOT_EXIST, directory );
            if ( !directory.isDirectory( ) )
                throw new FileException( ErrorId.NOT_A_DIRECTORY, directory );
        }
        catch ( AppException e )
        {
            GuiUtilities.setFocus( inputDirectoryField );
            throw e;
        }

        // Output file
        try
        {
            if ( outputFileField.isEmpty( ) )
                throw new AppException( ErrorId.NO_OUTPUT_FILE );
            File file = outputFileField.getFile( );
            if ( file.exists( ) && !file.isFile( ) )
                throw new FileException( ErrorId.NOT_A_FILE, file );
        }
        catch ( AppException e )
        {
            GuiUtilities.setFocus( outputFileField );
            throw e;
        }
    }

    //------------------------------------------------------------------

    private void onChooseInputDirectory( )
    {
        if ( !inputDirectoryField.isEmpty( ) )
            inputDirectoryChooser.setCurrentDirectory( inputDirectoryField.getCanonicalFile( ) );
        inputDirectoryChooser.rescanCurrentDirectory( );
        if ( inputDirectoryChooser.showDialog( this, SELECT_STR ) == JFileChooser.APPROVE_OPTION )
            inputDirectoryField.setFile( inputDirectoryChooser.getSelectedFile( ) );
    }

    //------------------------------------------------------------------

    private void onChooseOutputFile( )
    {
        if ( !outputFileField.isEmpty( ) )
            outputFileChooser.setSelectedFile( outputFileField.getCanonicalFile( ) );
        outputFileChooser.rescanCurrentDirectory( );
        if ( outputFileChooser.showDialog( this, SELECT_STR ) == JFileChooser.APPROVE_OPTION )
            outputFileField.setFile( outputFileChooser.getSelectedFile( ) );
    }

    //------------------------------------------------------------------

    private void onAccept( )
    {
        try
        {
            // Validate user input
            validateUserInput( );

            // Update class variables
            inputDirectory = inputDirectoryField.isEmpty( ) ? null : inputDirectoryField.getFile( );
            outputFile = outputFileField.isEmpty( ) ? null : outputFileField.getFile( );

            // Close dialog
            accepted = true;
            onClose( );
        }
        catch ( AppException e )
        {
            JOptionPane.showMessageDialog( this, e, App.SHORT_NAME, JOptionPane.ERROR_MESSAGE );
        }
    }

    //------------------------------------------------------------------

    private void onClose( )
    {
        location = getLocation( );
        setVisible( false );
        dispose( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

    private static  Point           location;
    private static  File            inputDirectory;
    private static  File            outputFile;
    private static  JFileChooser    inputDirectoryChooser;
    private static  JFileChooser    outputFileChooser;

////////////////////////////////////////////////////////////////////////
//  Static initialiser
////////////////////////////////////////////////////////////////////////

    static
    {
        inputDirectoryChooser = new JFileChooser( );
        inputDirectoryChooser.setDialogTitle( INPUT_DIRECTORY_TITLE_STR );
        inputDirectoryChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
        inputDirectoryChooser.setApproveButtonMnemonic( KeyEvent.VK_S );
        inputDirectoryChooser.setApproveButtonToolTipText( SELECT_DIRECTORY_STR );

        outputFileChooser = new JFileChooser( );
        outputFileChooser.setDialogTitle( OUTPUT_FILE_TITLE_STR );
        outputFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        outputFileChooser.setApproveButtonMnemonic( KeyEvent.VK_S );
        outputFileChooser.setApproveButtonToolTipText( SELECT_FILE_STR );
    }

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private boolean         accepted;
    private PathnameField   inputDirectoryField;
    private PathnameField   outputFileField;
    private JButton         joinButton;

}

//----------------------------------------------------------------------
