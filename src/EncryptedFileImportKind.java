/*====================================================================*\

EncryptedFileImportKind.java

Encrypted file import kind enumeration.

\*====================================================================*/


// IMPORTS


import uk.org.blankaspect.util.StringKeyed;
import uk.org.blankaspect.util.StringUtilities;

//----------------------------------------------------------------------


// ENCRYPTED FILE IMPORT KIND ENUMERATION


enum EncryptedFileImportKind
    implements StringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    DECRYPT     ( "decrypt" ),
    VALIDATE    ( "validate" );

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private EncryptedFileImportKind( String key )
    {
        this.key = key;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

    public static EncryptedFileImportKind forKey( String key )
    {
        for ( EncryptedFileImportKind value : values( ) )
        {
            if ( value.key.equals( key ) )
                return value;
        }
        return null;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : StringKeyed interface
////////////////////////////////////////////////////////////////////////

    public String getKey( )
    {
        return key;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

    @Override
    public String toString( )
    {
        return StringUtilities.firstCharToUpperCase( key );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private String  key;

}

//----------------------------------------------------------------------
