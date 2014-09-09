/*====================================================================*\

KeyKind.java

Key kind enumeration.

\*====================================================================*/


// IMPORTS


import java.awt.Color;

import uk.org.blankaspect.gui.Colours;

import uk.org.blankaspect.util.StringKeyed;
import uk.org.blankaspect.util.StringUtilities;

//----------------------------------------------------------------------


// KEY KIND ENUMERATION


enum KeyKind
    implements StringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    NEW
    (
        "new",
        new Color( 232, 240, 232 ),
        new Color( 0, 144, 0 )
    ),

    TEMPORARY
    (
        "temporary",
        new Color( 252, 248, 208 ),
        new Color( 192, 64, 0 )
    ),

    PERSISTENT
    (
        "persistent",
        Colours.List.BACKGROUND.getColour( ),
        Colours.List.FOREGROUND.getColour( )
    );

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private KeyKind( String key,
                     Color  backgroundColour,
                     Color  textColour )
    {
        this.key = key;
        this.backgroundColour = backgroundColour;
        this.textColour = textColour;
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
//  Instance methods
////////////////////////////////////////////////////////////////////////

    public Color getBackgroundColour( )
    {
        return backgroundColour;
    }

    //------------------------------------------------------------------

    public Color getTextColour( )
    {
        return textColour;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private String  key;
    private Color   backgroundColour;
    private Color   textColour;

}

//----------------------------------------------------------------------
