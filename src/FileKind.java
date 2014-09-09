/*====================================================================*\

FileKind.java

File kind enumeration.

\*====================================================================*/


// IMPORTS


import uk.org.blankaspect.util.FilenameSuffixFilter;
import uk.org.blankaspect.util.StringKeyed;

//----------------------------------------------------------------------


// FILE KIND ENUMERATION


enum FileKind
    implements StringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    ENCRYPTED
    (
        "encrypted",
        ".qana",
        "Encrypted files"
    ),

    ARCHIVE
    (
        "archive",
        ".qarc",
        "Archive database files"
    );

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private FileKind( String key,
                      String defaultFilenameSuffix,
                      String description )
    {
        this.key = key;
        this.defaultFilenameSuffix = defaultFilenameSuffix;
        this.description = description;
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
//  Instance methods
////////////////////////////////////////////////////////////////////////

    public String getDefaultFilenameSuffix( )
    {
        return defaultFilenameSuffix;
    }

    //------------------------------------------------------------------

    public String getDescription( )
    {
        return description;
    }

    //------------------------------------------------------------------

    public String getFilenameSuffix( )
    {
        return AppConfig.getInstance( ).getFilenameSuffix( this );
    }

    //------------------------------------------------------------------

    public FilenameSuffixFilter getFileFilter( )
    {
        return new FilenameSuffixFilter( description, getFilenameSuffix( ) );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private String  key;
    private String  defaultFilenameSuffix;
    private String  description;

}

//----------------------------------------------------------------------
