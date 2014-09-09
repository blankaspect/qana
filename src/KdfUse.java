/*====================================================================*\

KdfUse.java

Key derivation function use enumeration.

\*====================================================================*/


// IMPORTS


import java.awt.event.KeyEvent;

import java.util.EnumMap;
import java.util.Map;

import uk.org.blankaspect.crypto.StreamEncrypter;

import uk.org.blankaspect.util.StringKeyed;

//----------------------------------------------------------------------


// KEY DERIVATION FUNCTION USE ENUMERATION


enum KdfUse
    implements StringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    VERIFICATION
    (
        "verification",
        "Key verification",
        KeyEvent.VK_V
    ),

    GENERATION
    (
        "generation",
        "CEK generation",
        KeyEvent.VK_G
    );

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private KdfUse( String key,
                    String text,
                    int    mnemonicKey )
    {
        this.key = key;
        this.text = text;
        this.mnemonicKey = mnemonicKey;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

    public static KdfUse forKey( String key )
    {
        for ( KdfUse value : values( ) )
        {
            if ( value.key.equals( key ) )
                return value;
        }
        return null;
    }

    //------------------------------------------------------------------

    public static Map<KdfUse, StreamEncrypter.KdfParams> getKdfParameterMap( )
    {
        Map<KdfUse, StreamEncrypter.KdfParams> paramMap = new EnumMap<>( KdfUse.class );
        for ( KdfUse kdfUse : KdfUse.values( ) )
            paramMap.put( kdfUse, kdfUse.getKdfParameters( ) );
        return paramMap;
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
        return text;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    public int getMnemonicKey( )
    {
        return mnemonicKey;
    }

    //------------------------------------------------------------------

    public StreamEncrypter.KdfParams getKdfParameters( )
    {
        return AppConfig.getInstance( ).getKdfParameters( this );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private String  key;
    private String  text;
    private int     mnemonicKey;

}

//----------------------------------------------------------------------
