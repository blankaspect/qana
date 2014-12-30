/*====================================================================*\

FileKind.java

File kind enumeration.

\*====================================================================*/


// IMPORTS


import uk.org.blankaspect.util.FilenameSuffixFilter;
import uk.org.blankaspect.util.StringKeyed;

import uk.org.blankaspect.windows.FileAssociations;

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
        "Encrypted files",
        "BlankAspect." + App.SHORT_NAME + ".encryptedFile",
        "Qana-encrypted file",
        "&Decrypt with Qana"
    ),

    ARCHIVE
    (
        "archive",
        ".qarc",
        "Archive database files",
        "BlankAspect." + App.SHORT_NAME + ".archive",
        "Qana archive",
        "&Open with Qana"
    );

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private FileKind( String key,
                      String defaultFilenameSuffix,
                      String description,
                      String fileAssocFileKindKey,
                      String fileAssocFileKindText,
                      String fileAssocFileOpenText )
    {
        this.key = key;
        this.defaultFilenameSuffix = defaultFilenameSuffix;
        this.description = description;
        this.fileAssocFileKindKey = fileAssocFileKindKey;
        this.fileAssocFileKindText = fileAssocFileKindText;
        this.fileAssocFileOpenText = fileAssocFileOpenText;
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

    public void addFileAssocParams( FileAssociations fileAssociations )
    {
        fileAssociations.addParams( fileAssocFileKindKey, fileAssocFileKindText, fileAssocFileOpenText,
                                    getFilenameSuffix( ) );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private String  key;
    private String  defaultFilenameSuffix;
    private String  description;
    private String  fileAssocFileKindKey;
    private String  fileAssocFileKindText;
    private String  fileAssocFileOpenText;

}

//----------------------------------------------------------------------
