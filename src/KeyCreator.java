/*====================================================================*\

KeyCreator.java

Key creator class.

\*====================================================================*/


// IMPORTS


import java.awt.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import uk.org.blankaspect.crypto.FortunaCipher;
import uk.org.blankaspect.crypto.StreamEncrypter;

import uk.org.blankaspect.exception.AppException;

import uk.org.blankaspect.gui.RunnableMessageDialog;

import uk.org.blankaspect.util.StringUtilities;

//----------------------------------------------------------------------


// KEY CREATOR CLASS


class KeyCreator
    implements RunnableMessageDialog.Runnable
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    private static final    String  PERSISTENT_MESSAGE_STR  = "Creating key \"%1\"" +
                                                                                AppConstants.ELLIPSIS_STR;
    private static final    String  TEMPORARY_MESSAGE_STR   = "Creating a temporary key " +
                                                                                AppConstants.ELLIPSIS_STR;

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

        NOT_ENOUGH_MEMORY
        ( "There was not enough memory to create the key." );

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

    public KeyCreator( String                                 name,
                       String                                 passphrase,
                       Map<KdfUse, StreamEncrypter.KdfParams> kdfParamMap,
                       Set<FortunaCipher>                     allowedCiphers,
                       FortunaCipher                          preferredCipher )
    {
        this.name = name;
        this.passphrase = passphrase;
        this.kdfParamMap = kdfParamMap;
        this.allowedCiphers = EnumSet.copyOf( allowedCiphers );
        this.preferredCipher = preferredCipher;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : RunnableMessageDialog.Runnable interface
////////////////////////////////////////////////////////////////////////

    public String getMessage( )
    {
        return ( (name == null) ? TEMPORARY_MESSAGE_STR
                                : StringUtilities.substitute( PERSISTENT_MESSAGE_STR, name ) );
    }

    //------------------------------------------------------------------

    public void run( )
    {
        try
        {
            key = KeyList.createKey( name, passphrase, kdfParamMap.get( KdfUse.VERIFICATION ),
                                     kdfParamMap.get( KdfUse.GENERATION ), allowedCiphers,
                                     preferredCipher );
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

    public KeyList.Key create( Component component )
        throws AppException
    {
        outOfMemory = false;
        RunnableMessageDialog.showDialog( component, this );
        if ( outOfMemory )
            throw new AppException( ErrorId.NOT_ENOUGH_MEMORY );
        return key;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private String                                  name;
    private String                                  passphrase;
    private Map<KdfUse, StreamEncrypter.KdfParams>  kdfParamMap;
    private Set<FortunaCipher>                      allowedCiphers;
    private FortunaCipher                           preferredCipher;
    private KeyList.Key                             key;
    private boolean                                 outOfMemory;

}

//----------------------------------------------------------------------
