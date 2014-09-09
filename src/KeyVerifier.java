/*====================================================================*\

KeyVerifier.java

Key verifier class.

\*====================================================================*/


// IMPORTS


import java.awt.Component;

import uk.org.blankaspect.exception.AppException;

import uk.org.blankaspect.gui.RunnableMessageDialog;

import uk.org.blankaspect.util.StringUtilities;

//----------------------------------------------------------------------


// KEY VERIFIER CLASS


class KeyVerifier
    implements RunnableMessageDialog.Runnable
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    private static final    String  MESSAGE_STR = "Verifying key \"%1\" " + AppConstants.ELLIPSIS_STR;

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

        INCORRECT_PASSPHRASE
        ( "The passphrase is incorrect." ),

        NOT_ENOUGH_MEMORY
        ( "There was not enough memory to verify the key." );

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
//  Constructors
////////////////////////////////////////////////////////////////////////

    public KeyVerifier( KeyList.Key key,
                        String      passphrase )
    {
        this.key = key;
        this.passphrase = passphrase;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : RunnableMessageDialog.Runnable interface
////////////////////////////////////////////////////////////////////////

    public String getMessage( )
    {
        return StringUtilities.substitute( MESSAGE_STR, key.getName( ) );
    }

    //------------------------------------------------------------------

    public void run( )
    {
        try
        {
            verified = key.verify( passphrase );
        }
        catch ( OutOfMemoryError e )
        {
            outOfMemory = true;
        }
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    public void verify( Component component )
        throws AppException
    {
        outOfMemory = false;
        RunnableMessageDialog.showDialog( component, this );
        if ( outOfMemory )
            throw new AppException( ErrorId.NOT_ENOUGH_MEMORY );
        if ( !verified )
            throw new AppException( ErrorId.INCORRECT_PASSPHRASE );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private KeyList.Key key;
    private String      passphrase;
    private boolean     verified;
    private boolean     outOfMemory;

}

//----------------------------------------------------------------------
