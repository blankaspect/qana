/*====================================================================*\

SplitDialog.java

Split dialog box class.

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

import uk.org.blankaspect.gui.ByteUnitIntegerSpinnerPanel;
import uk.org.blankaspect.gui.FButton;
import uk.org.blankaspect.gui.FLabel;
import uk.org.blankaspect.gui.GuiUtilities;
import uk.org.blankaspect.gui.LinkedPairButton;
import uk.org.blankaspect.gui.PathnamePanel;

import uk.org.blankaspect.textfield.PathnameField;

import uk.org.blankaspect.util.FileImporter;
import uk.org.blankaspect.util.KeyAction;

//----------------------------------------------------------------------


// SPLIT DIALOG BOX CLASS


class SplitDialog
    extends JDialog
    implements ActionListener, DocumentListener, FileImporter, ByteUnitIntegerSpinnerPanel.Observer
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    private static final    int FILE_PART_LENGTH_FIELD_LENGTH   = 10;

    private static final    String  TITLE_STR                   = "Split file";
    private static final    String  INPUT_FILE_STR              = "Input file:";
    private static final    String  OUTPUT_DIRECTORY_STR        = "Output directory:";
    private static final    String  FILE_PART_LENGTH_STR        = "File-part length:";
    private static final    String  TO_STR                      = "to";
    private static final    String  SPLIT_STR                   = "Split";
    private static final    String  INPUT_FILE_TITLE_STR        = SPLIT_STR + " | Input file";
    private static final    String  OUTPUT_DIRECTORY_TITLE_STR  = SPLIT_STR + " | Output directory";
    private static final    String  SELECT_STR                  = "Select";
    private static final    String  SELECT_FILE_STR             = "Select file";
    private static final    String  SELECT_DIRECTORY_STR        = "Select directory";
    private static final    String  LINK_TOOLTIP_STR            = "lower limit and upper limit";

    // Commands
    private interface Command
    {
        String  CHOOSE_INPUT_FILE           = "chooseInputFile";
        String  CHOOSE_OUTPUT_DIRECTORY     = "chooseOutputDirectory";
        String  TOGGLE_LENGTH_LIMITS_LINKED = "toggleLengthLimitsLinked";
        String  ACCEPT                      = "accept";
        String  CLOSE                       = "close";
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

        FILE_DOES_NOT_EXIST
        ( "The file does not exist." ),

        NOT_A_FILE
        ( "The input pathname does not denote a normal file." ),

        NOT_A_DIRECTORY
        ( "The output pathname does not denote a directory." ),

        NO_INPUT_FILE
        ( "No input file was specified." ),

        FILE_PART_LENGTH_LIMITS_OUT_OF_ORDER
        ( "The upper limit of the file-part length is less than the lower limit." );

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

        public Result( File inFile,
                       File outDirectory,
                       int  filePartLengthLowerLimit,
                       int  filePartLengthUpperLimit )
        {
            this.inFile = inFile;
            this.outDirectory = outDirectory;
            this.filePartLengthLowerLimit = filePartLengthLowerLimit;
            this.filePartLengthUpperLimit = filePartLengthUpperLimit;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        File    inFile;
        File    outDirectory;
        int     filePartLengthLowerLimit;
        int     filePartLengthUpperLimit;

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private SplitDialog( Window owner )
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

        // Label: input file
        JLabel inFileLabel = new FLabel( INPUT_FILE_STR );

        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = AppConstants.COMPONENT_INSETS;
        gridBag.setConstraints( inFileLabel, gbc );
        controlPanel.add( inFileLabel );

        // Panel: input file
        inputFileField = new FPathnameField( inputFile );
        inputFileField.getDocument( ).addDocumentListener( this );
        PathnamePanel inFilePanel = new PathnamePanel( inputFileField, Command.CHOOSE_INPUT_FILE, this );

        gbc.gridx = 1;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = AppConstants.COMPONENT_INSETS;
        gridBag.setConstraints( inFilePanel, gbc );
        controlPanel.add( inFilePanel );

        // Label: output directory
        JLabel outDirectoryLabel = new FLabel( OUTPUT_DIRECTORY_STR );

        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = AppConstants.COMPONENT_INSETS;
        gridBag.setConstraints( outDirectoryLabel, gbc );
        controlPanel.add( outDirectoryLabel );

        // Panel: output directory
        outputDirectoryField = new FPathnameField( outputDirectory );
        PathnamePanel outputDirectoryPanel = new PathnamePanel( outputDirectoryField,
                                                                Command.CHOOSE_OUTPUT_DIRECTORY, this );

        gbc.gridx = 1;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = AppConstants.COMPONENT_INSETS;
        gridBag.setConstraints( outputDirectoryPanel, gbc );
        controlPanel.add( outputDirectoryPanel );

        // Label: file-part length
        JLabel filePartLengthLabel = new FLabel( FILE_PART_LENGTH_STR );

        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = AppConstants.COMPONENT_INSETS;
        gridBag.setConstraints( filePartLengthLabel, gbc );
        controlPanel.add( filePartLengthLabel );

        // Panel: file-part length
        JPanel filePartLengthPanel = new JPanel( gridBag );

        gbc.gridx = 1;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = AppConstants.COMPONENT_INSETS;
        gridBag.setConstraints( filePartLengthPanel, gbc );
        controlPanel.add( filePartLengthPanel );

        int gridX = 0;

        // Spinner: file-part length lower limit
        int maxValue = filePartLengthLimitsLinked ? FileSplitter.MAX_FILE_PART_LENGTH
                                                  : filePartLengthUpperLimit.value;
        filePartLengthLowerLimitSpinner =
                                        new ByteUnitIntegerSpinnerPanel( filePartLengthLowerLimit,
                                                                         FileSplitter.MIN_FILE_PART_LENGTH,
                                                                         maxValue,
                                                                         FILE_PART_LENGTH_FIELD_LENGTH );
        filePartLengthLowerLimitSpinner.addObserver( this );

        gbc.gridx = gridX++;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets( 0, 0, 0, 0 );
        gridBag.setConstraints( filePartLengthLowerLimitSpinner, gbc );
        filePartLengthPanel.add( filePartLengthLowerLimitSpinner );

        // Label: to
        JLabel toLabel = new FLabel( TO_STR );

        gbc.gridx = gridX++;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets( 0, 6, 0, 0 );
        gridBag.setConstraints( toLabel, gbc );
        filePartLengthPanel.add( toLabel );

        // Spinner: file-part length upper limit
        int minValue = filePartLengthLimitsLinked ? FileSplitter.MIN_FILE_PART_LENGTH
                                                  : filePartLengthLowerLimit.value;
        filePartLengthUpperLimitSpinner =
                                        new ByteUnitIntegerSpinnerPanel( filePartLengthUpperLimit, minValue,
                                                                         FileSplitter.MAX_FILE_PART_LENGTH,
                                                                         FILE_PART_LENGTH_FIELD_LENGTH );
        filePartLengthUpperLimitSpinner.addObserver( this );

        gbc.gridx = gridX++;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets( 0, 6, 0, 0 );
        gridBag.setConstraints( filePartLengthUpperLimitSpinner, gbc );
        filePartLengthPanel.add( filePartLengthUpperLimitSpinner );

        // Button: link lower limit and upper limit
        linkButton = new LinkedPairButton( LINK_TOOLTIP_STR );
        linkButton.setSelected( filePartLengthLimitsLinked );
        linkButton.setActionCommand( Command.TOGGLE_LENGTH_LIMITS_LINKED );
        linkButton.addActionListener( this );

        gbc.gridx = gridX++;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets( 0, 6, 0, 0 );
        gridBag.setConstraints( linkButton, gbc );
        filePartLengthPanel.add( linkButton );


        //----  Button panel

        JPanel buttonPanel = new JPanel( new GridLayout( 1, 0, 8, 0 ) );
        buttonPanel.setBorder( BorderFactory.createEmptyBorder( 3, 8, 3, 8 ) );

        // Button: split
        splitButton = new FButton( SPLIT_STR );
        splitButton.setActionCommand( Command.ACCEPT );
        splitButton.addActionListener( this );
        buttonPanel.add( splitButton );

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

        // Handle window closing
        addWindowListener( new WindowAdapter( )
        {
            @Override
            public void windowClosing( WindowEvent event )
            {
                onClose( );
            }
        } );

        // Prevent dialog from being resized
        setResizable( false );

        // Resize dialog to its preferred size
        pack( );

        // Set location of dialog box
        if ( location == null )
            location = GuiUtilities.getComponentLocation( this, owner );
        setLocation( location );

        // Set default button
        getRootPane( ).setDefaultButton( splitButton );

        // Show dialog
        setVisible( true );

    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

    public static ByteUnitIntegerSpinnerPanel.Value getFilePartLengthLowerLimit( )
    {
        return filePartLengthLowerLimit;
    }

    //------------------------------------------------------------------

    public static ByteUnitIntegerSpinnerPanel.Value getFilePartLengthUpperLimit( )
    {
        return filePartLengthUpperLimit;
    }

    //------------------------------------------------------------------

    public static Result showDialog( Component parent )
    {
        return new SplitDialog( GuiUtilities.getWindow( parent ) ).getResult( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

    public void actionPerformed( ActionEvent event )
    {
        String command = event.getActionCommand( );

        if ( command.equals( Command.CHOOSE_INPUT_FILE ) )
            onChooseInputFile( );

        else if ( command.equals( Command.CHOOSE_OUTPUT_DIRECTORY ) )
            onChooseOutputDirectory( );

        else if ( command.equals( Command.TOGGLE_LENGTH_LIMITS_LINKED ) )
            onToggleLengthLimitsLinked( );

        else if ( command.equals( Command.ACCEPT ) )
            onAccept( );

        else if ( command.equals( Command.CLOSE ) )
            onClose( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ByteUnitIntegerSpinnerPanel.Observer interface
////////////////////////////////////////////////////////////////////////

    public void notifyChanged( ByteUnitIntegerSpinnerPanel          source,
                               ByteUnitIntegerSpinnerPanel.Property changedProperty )
    {
        // Spinner, file-part length lower limit
        if ( source == filePartLengthLowerLimitSpinner )
        {
            switch ( changedProperty )
            {
                case UNIT:
                    filePartLengthUpperLimitSpinner.setUnit( filePartLengthLowerLimitSpinner.getUnit( ) );
                    break;

                case VALUE:
                    if ( linkButton.isSelected( ) )
                        filePartLengthUpperLimitSpinner.
                                            setValue( filePartLengthLowerLimitSpinner.getValue( ) );
                    else
                        filePartLengthUpperLimitSpinner.
                                            setMinimum( filePartLengthLowerLimitSpinner.getIntValue( ) );
                    break;

                default:
                    // do nothing
                    break;
            }
        }

        // Spinner, file-part length upper limit
        else if ( source == filePartLengthUpperLimitSpinner )
        {
            switch ( changedProperty )
            {
                case UNIT:
                    filePartLengthLowerLimitSpinner.setUnit( filePartLengthUpperLimitSpinner.getUnit( ) );
                    break;

                case VALUE:
                    if ( linkButton.isSelected( ) )
                        filePartLengthLowerLimitSpinner.
                                            setValue( filePartLengthUpperLimitSpinner.getValue( ) );
                    else
                        filePartLengthLowerLimitSpinner.
                                            setMaximum( filePartLengthUpperLimitSpinner.getIntValue( ) );
                    break;

                default:
                    // do nothing
                    break;
            }
        }
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
            outputDirectoryField.setFile( files[0] );
        else
            inputFileField.setFile( files[0] );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    private Result getResult( )
    {
        return ( accepted ? new Result( inputFile, outputDirectory, filePartLengthLowerLimit.value,
                                        filePartLengthUpperLimit.value )
                          : null );
    }

    //------------------------------------------------------------------

    private void updateAcceptButton( )
    {
        splitButton.setEnabled( !inputFileField.isEmpty( ) );
    }

    //------------------------------------------------------------------

    private void validateUserInput( )
        throws AppException
    {
        // Input file
        try
        {
            if ( inputFileField.isEmpty( ) )
                throw new AppException( ErrorId.NO_INPUT_FILE );
            File file = inputFileField.getFile( );
            if ( !file.exists( ) )
                throw new FileException( ErrorId.FILE_DOES_NOT_EXIST, file );
            if ( !file.isFile( ) )
                throw new FileException( ErrorId.NOT_A_FILE, file );
        }
        catch ( AppException e )
        {
            GuiUtilities.setFocus( inputFileField );
            throw e;
        }

        // Output directory
        try
        {
            if ( !outputDirectoryField.isEmpty( ) )
            {
                File directory = outputDirectoryField.getFile( );
                if ( directory.exists( ) && !directory.isDirectory( ) )
                    throw new FileException( ErrorId.NOT_A_DIRECTORY, directory );
            }
        }
        catch ( AppException e )
        {
            GuiUtilities.setFocus( outputDirectoryField );
            throw e;
        }

        // File-part length limits
        try
        {
            if ( filePartLengthUpperLimitSpinner.getIntValue( ) <
                                                            filePartLengthLowerLimitSpinner.getIntValue( ) )
                throw new AppException( ErrorId.FILE_PART_LENGTH_LIMITS_OUT_OF_ORDER );
        }
        catch ( AppException e )
        {
            GuiUtilities.setFocus( filePartLengthUpperLimitSpinner );
            throw e;
        }
    }

    //------------------------------------------------------------------

    private void onChooseInputFile( )
    {
        if ( !inputFileField.isEmpty( ) )
            inputFileChooser.setSelectedFile( inputFileField.getCanonicalFile( ) );
        inputFileChooser.rescanCurrentDirectory( );
        if ( inputFileChooser.showDialog( this, SELECT_STR ) == JFileChooser.APPROVE_OPTION )
            inputFileField.setFile( inputFileChooser.getSelectedFile( ) );
    }

    //------------------------------------------------------------------

    private void onChooseOutputDirectory( )
    {
        if ( !outputDirectoryField.isEmpty( ) )
            outputDirectoryChooser.setCurrentDirectory( outputDirectoryField.getCanonicalFile( ) );
        outputDirectoryChooser.rescanCurrentDirectory( );
        if ( outputDirectoryChooser.showDialog( this, SELECT_STR ) == JFileChooser.APPROVE_OPTION )
            outputDirectoryField.setFile( outputDirectoryChooser.getSelectedFile( ) );
    }

    //------------------------------------------------------------------

    private void onToggleLengthLimitsLinked( )
    {
        if ( linkButton.isSelected( ) )
        {
            filePartLengthLowerLimitSpinner.setMaximum( FileSplitter.MAX_FILE_PART_LENGTH );
            filePartLengthUpperLimitSpinner.setMinimum( FileSplitter.MIN_FILE_PART_LENGTH );
            filePartLengthUpperLimitSpinner.setValue( filePartLengthLowerLimitSpinner.getValue( ) );
        }
        else
        {
            filePartLengthLowerLimitSpinner.setMaximum( filePartLengthUpperLimitSpinner.getIntValue( ) );
            filePartLengthUpperLimitSpinner.setMinimum( filePartLengthLowerLimitSpinner.getIntValue( ) );
        }
    }

    //------------------------------------------------------------------

    private void onAccept( )
    {
        try
        {
            // Validate user input
            validateUserInput( );

            // Update class variables
            inputFile = inputFileField.isEmpty( ) ? null : inputFileField.getFile( );
            outputDirectory = outputDirectoryField.isEmpty( ) ? null : outputDirectoryField.getFile( );
            filePartLengthLowerLimit = filePartLengthLowerLimitSpinner.getValue( );
            filePartLengthUpperLimit = filePartLengthUpperLimitSpinner.getValue( );
            filePartLengthLimitsLinked = linkButton.isSelected( );

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

    private static  Point                               location;
    private static  File                                inputFile;
    private static  File                                outputDirectory;
    private static  ByteUnitIntegerSpinnerPanel.Value   filePartLengthLowerLimit;
    private static  ByteUnitIntegerSpinnerPanel.Value   filePartLengthUpperLimit;
    private static  boolean                             filePartLengthLimitsLinked;
    private static  JFileChooser                        inputFileChooser;
    private static  JFileChooser                        outputDirectoryChooser;

////////////////////////////////////////////////////////////////////////
//  Static initialiser
////////////////////////////////////////////////////////////////////////

    static
    {
        inputFileChooser = new JFileChooser( );
        inputFileChooser.setDialogTitle( INPUT_FILE_TITLE_STR );
        inputFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        inputFileChooser.setApproveButtonMnemonic( KeyEvent.VK_S );
        inputFileChooser.setApproveButtonToolTipText( SELECT_FILE_STR );

        outputDirectoryChooser = new JFileChooser( );
        outputDirectoryChooser.setDialogTitle( OUTPUT_DIRECTORY_TITLE_STR );
        outputDirectoryChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
        outputDirectoryChooser.setApproveButtonMnemonic( KeyEvent.VK_S );
        outputDirectoryChooser.setApproveButtonToolTipText( SELECT_DIRECTORY_STR );

        AppConfig config = AppConfig.getInstance( );
        filePartLengthLowerLimit = config.getSplitFilePartLengthLowerLimit( );
        filePartLengthUpperLimit = config.getSplitFilePartLengthUpperLimit( );
        filePartLengthLimitsLinked = (filePartLengthLowerLimit.value == filePartLengthUpperLimit.value);
    }

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private boolean                     accepted;
    private PathnameField               inputFileField;
    private PathnameField               outputDirectoryField;
    private ByteUnitIntegerSpinnerPanel filePartLengthLowerLimitSpinner;
    private ByteUnitIntegerSpinnerPanel filePartLengthUpperLimitSpinner;
    private LinkedPairButton            linkButton;
    private JButton                     splitButton;

}

//----------------------------------------------------------------------
