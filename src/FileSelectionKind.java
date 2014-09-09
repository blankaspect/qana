/*====================================================================*\

FileSelectionKind.java

File-selection kind enumeration.

\*====================================================================*/


// IMPORTS


import java.awt.event.KeyEvent;

import java.io.File;

import javax.swing.JFileChooser;

import uk.org.blankaspect.util.StringKeyed;

//----------------------------------------------------------------------


// FILE-SELECTION KIND ENUMERATION


enum FileSelectionKind
    implements StringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    OPEN_ARCHIVE
    (
        "openArchive",
        "Open archive",
        FileKind.ARCHIVE
    )
    {
        @Override
        protected void initFileChooser( JFileChooser fileChooser )
        {
            fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        }
    },

    SAVE_ARCHIVE
    (
        "saveArchive",
        "Save archive",
        FileKind.ARCHIVE
    )
    {
        @Override
        protected void initFileChooser( JFileChooser fileChooser )
        {
            fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        }
    },

    ARCHIVE_DIRECTORY
    (
        "archiveDirectory",
        "Archive directory",
        null
    )
    {
        @Override
        protected void initFileChooser( JFileChooser fileChooser )
        {
            fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
            fileChooser.setApproveButtonMnemonic( KeyEvent.VK_S );
            fileChooser.setApproveButtonToolTipText( SELECT_DIRECTORY_STR );
        }
    },

    ENCRYPT
    (
        "encrypt",
        "Encrypt file",
        null
    )
    {
        @Override
        protected void initFileChooser( JFileChooser fileChooser )
        {
            fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        }
    },

    DECRYPT
    (
        "decrypt",
        "Decrypt file",
        FileKind.ENCRYPTED
    )
    {
        @Override
        protected void initFileChooser( JFileChooser fileChooser )
        {
            fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        }
    },

    VALIDATE
    (
        "validate",
        "Validate file",
        FileKind.ENCRYPTED
    )
    {
        @Override
        protected void initFileChooser( JFileChooser fileChooser )
        {
            fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        }
    };

    //------------------------------------------------------------------

    private static final    String  SELECT_DIRECTORY_STR    = "Select directory";

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private FileSelectionKind( String   key,
                               String   titleStr,
                               FileKind fileKind )
    {
        this.key = key;
        this.titleStr = titleStr;
        this.fileKind = fileKind;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

    public static FileSelectionKind forKey( String key )
    {
        for ( FileSelectionKind value : values( ) )
        {
            if ( value.key.equals( key ) )
                return value;
        }
        return null;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Abstract methods
////////////////////////////////////////////////////////////////////////

    protected abstract void initFileChooser( JFileChooser fileChooser );

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

    public JFileChooser getFileChooser( )
    {
        if ( fileChooser == null )
        {
            AppConfig config = AppConfig.getInstance( );
            File directory = config.isSaveFileSelectionPathnames( )
                                                                ? config.getFileSelectionDirectory( this )
                                                                : null;
            fileChooser = new JFileChooser( directory );
            fileChooser.setDialogTitle( titleStr );
            initFileChooser( fileChooser );
        }
        if ( fileKind != null )
            fileChooser.setFileFilter( fileKind.getFileFilter( ) );
        return fileChooser;
    }

    //------------------------------------------------------------------

    public void updateDirectory( )
    {
        AppConfig config = AppConfig.getInstance( );
        String pathname = config.isSaveFileSelectionPathnames( )
                                                    ? Util.getPathname( fileChooser.getCurrentDirectory( ) )
                                                    : null;
        config.setFileSelectionPathname( this, pathname );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private String          key;
    private String          titleStr;
    private FileKind        fileKind;
    private JFileChooser    fileChooser;

}

//----------------------------------------------------------------------
