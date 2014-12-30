/*====================================================================*\

KeyPropertiesDialog.java

Key properties dialog box class.

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

import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import uk.org.blankaspect.crypto.FortunaCipher;
import uk.org.blankaspect.crypto.StreamEncrypter;

import uk.org.blankaspect.gui.FButton;
import uk.org.blankaspect.gui.GuiUtilities;
import uk.org.blankaspect.gui.TitledBorder;

import uk.org.blankaspect.util.KeyAction;

//----------------------------------------------------------------------


// KEY PROPERTIES DIALOG BOX CLASS


class KeyPropertiesDialog
    extends JDialog
    implements ActionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    private static final    String  TITLE_STR                   = "Key properties";
    private static final    String  KEY_DERIVATION_FUNCTION_STR = "Key derivation function";
    private static final    String  CIPHERS_STR                 = "Ciphers";

    // Commands
    private interface Command
    {
        String  ACCEPT  = "accept";
        String  CLOSE   = "close";
    }

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


    // RESULT CLASS


    public static class Result
    {

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        public Result( Map<KdfUse, StreamEncrypter.KdfParams> kdfParameterMap,
                       Set<FortunaCipher>                     allowedCiphers,
                       FortunaCipher                          preferredCipher )
        {
            this.kdfParameterMap = kdfParameterMap;
            this.allowedCiphers = allowedCiphers;
            this.preferredCipher = preferredCipher;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        Map<KdfUse, StreamEncrypter.KdfParams>  kdfParameterMap;
        Set<FortunaCipher>                      allowedCiphers;
        FortunaCipher                           preferredCipher;

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private KeyPropertiesDialog( Window                                 owner,
                                 String                                 titleStr,
                                 Map<KdfUse, StreamEncrypter.KdfParams> kdfParameterMap,
                                 Set<FortunaCipher>                     allowedCiphers,
                                 FortunaCipher                          preferredCipher )
    {

        // Call superclass constructor
        super( owner, (titleStr == null) ? TITLE_STR : TITLE_STR + " | " + titleStr,
               Dialog.ModalityType.APPLICATION_MODAL );

        // Set icons
        setIconImages( owner.getIconImages( ) );


        //----  KDF parameter panel

        kdfParameterPanel = new KdfParameterPanel( kdfUse, kdfParameterMap );
        TitledBorder.setPaddedBorder( kdfParameterPanel, KEY_DERIVATION_FUNCTION_STR );


        //----  Cipher panel

        GridBagLayout gridBag = new GridBagLayout( );
        GridBagConstraints gbc = new GridBagConstraints( );

        JPanel cipherPanel = new JPanel( gridBag );
        TitledBorder.setPaddedBorder( cipherPanel, CIPHERS_STR );

        cipherTable = new CipherTable( allowedCiphers, preferredCipher );

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets( 0, 0, 0, 0 );
        gridBag.setConstraints( cipherTable, gbc );
        cipherPanel.add( cipherTable );


        //----  Button panel

        JPanel buttonPanel = new JPanel( new GridLayout( 1, 0, 8, 0 ) );
        buttonPanel.setBorder( BorderFactory.createEmptyBorder( 3, 8, 3, 8 ) );

        // Button: OK
        JButton okButton = new FButton( AppConstants.OK_STR );
        okButton.setActionCommand( Command.ACCEPT );
        okButton.addActionListener( this );
        buttonPanel.add( okButton );

        // Button: cancel
        JButton cancelButton = new FButton( AppConstants.CANCEL_STR );
        cancelButton.setActionCommand( Command.CLOSE );
        cancelButton.addActionListener( this );
        buttonPanel.add( cancelButton );


        //----  Main panel

        JPanel mainPanel = new JPanel( gridBag );
        mainPanel.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) );

        int gridY = 0;

        gbc.gridx = 0;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets( 0, 0, 0, 0 );
        gridBag.setConstraints( kdfParameterPanel, gbc );
        mainPanel.add( kdfParameterPanel );

        gbc.gridx = 0;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets( 3, 0, 0, 0 );
        gridBag.setConstraints( cipherPanel, gbc );
        mainPanel.add( cipherPanel );

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


        //----  Window

        // Set content pane
        setContentPane( mainPanel );

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
        getRootPane( ).setDefaultButton( okButton );

        // Show dialog
        setVisible( true );

    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

    public static Result showDialog( Component                              parent,
                                     String                                 titleStr,
                                     Map<KdfUse, StreamEncrypter.KdfParams> kdfParameterMap,
                                     Set<FortunaCipher>                     allowedCiphers,
                                     FortunaCipher                          preferredCipher )
    {
        return new KeyPropertiesDialog( GuiUtilities.getWindow( parent ), titleStr, kdfParameterMap,
                                        allowedCiphers, preferredCipher ).getResult( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

    public void actionPerformed( ActionEvent event )
    {
        String command = event.getActionCommand( );

        if ( command.equals( Command.ACCEPT ) )
            onAccept( );

        else if ( command.equals( Command.CLOSE ) )
            onClose( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    private Result getResult( )
    {
        return ( accepted ? new Result( kdfParameterPanel.getParameterMap( ),
                                        cipherTable.getAllowedCiphers( ),
                                        cipherTable.getPreferredCipher( ) )
                          : null );
    }

    //------------------------------------------------------------------

    private void onAccept( )
    {
        accepted = true;
        onClose( );
    }

    //------------------------------------------------------------------

    private void onClose( )
    {
        location = getLocation( );
        kdfUse = kdfParameterPanel.getKdfUse( );
        setVisible( false );
        dispose( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

    private static  Point   location;
    private static  KdfUse  kdfUse      = KdfUse.VERIFICATION;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private boolean             accepted;
    private KdfParameterPanel   kdfParameterPanel;
    private CipherTable         cipherTable;

}

//----------------------------------------------------------------------
