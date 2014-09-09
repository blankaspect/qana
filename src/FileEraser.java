/*====================================================================*\

FileEraser.java

File eraser class.

\*====================================================================*/


// IMPORTS


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.JOptionPane;

import uk.org.blankaspect.exception.AppException;
import uk.org.blankaspect.exception.FileException;
import uk.org.blankaspect.exception.TaskCancelledException;

import uk.org.blankaspect.random.Prng01;

import uk.org.blankaspect.util.PropertyString;

//----------------------------------------------------------------------


// FILE ERASER CLASS


class FileEraser
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    public static final     int MIN_NUM_PASSES      = 1;
    public static final     int MAX_NUM_PASSES      = 20;
    public static final     int DEFAULT_NUM_PASSES  = 4;

    private static final    int BUFFER_SIZE = 1 << 13;  // 8192

    private static final    int MAX_FILENAME_LENGTH = 224;

    private static final    int ASSUMED_DIRECTORY_LENGTH    = 1 << 16;

    private static final    String  ERASE_FILES_STR = "Erase files";
    private static final    String  ERASING_STR     = "Erasing";
    private static final    String  RETRY_STR       = "Retry";
    private static final    String  SKIP_STR        = "Skip";

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

        FAILED_TO_GET_PATHNAME
        ( "Failed to get the canonical pathname for the file." ),

        FILE_OR_DIRECTORY_DOES_NOT_EXIST
        ( "The file or directory does not exist." ),

        FAILED_TO_OPEN_FILE
        ( "Failed to open the file." ),

        FAILED_TO_CLOSE_FILE
        ( "Failed to close the file." ),

        FAILED_TO_LOCK_FILE
        ( "Failed to lock the file." ),

        ERROR_READING_FILE
        ( "An error occurred when reading the file." ),

        ERROR_WRITING_FILE
        ( "An error occurred when writing the file." ),

        FILE_ACCESS_NOT_PERMITTED
        ( "Access to the file was not permitted." ),

        FAILED_TO_RENAME_FILE
        ( "Failed to rename the file." ),

        FAILED_TO_RENAME_DIRECTORY
        ( "Failed to rename the directory." ),

        FAILED_TO_DELETE_FILE
        ( "Failed to delete the file." ),

        FAILED_TO_DELETE_DIRECTORY
        ( "Failed to delete the directory." );

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


    // PATHNAME COMPARATOR CLASS


    private static class PathnameComparator
        implements Comparator<File>
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        private static final    char    SEPARATOR_CHAR  = '/';
        private static final    String  SEPARATOR_STR   = Character.toString( SEPARATOR_CHAR );

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private PathnameComparator( )
        {
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Class methods
    ////////////////////////////////////////////////////////////////////

        private static boolean isAncestor( File directory,
                                           File file )
        {
            String pathname1 = directory.getPath( ).replace( File.separatorChar, SEPARATOR_CHAR );
            while ( pathname1.endsWith( SEPARATOR_STR ) )
                pathname1 = pathname1.substring( 0, pathname1.length( ) - 1 );

            String pathname2 = file.getPath( ).replace( File.separatorChar, SEPARATOR_CHAR );
            while ( pathname2.endsWith( SEPARATOR_STR ) )
                pathname2 = pathname2.substring( 0, pathname2.length( ) - 1 );

            int length1 = pathname1.length( );
            return ( (length1 < pathname2.length( )) && pathname2.startsWith( pathname1 ) &&
                     (pathname2.charAt( length1 ) == SEPARATOR_CHAR) );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : Comparator interface
    ////////////////////////////////////////////////////////////////////

        public int compare( File file1,
                            File file2 )
        {
            if ( file2.isDirectory( ) && (file1.isFile( ) || isAncestor( file2, file1 )) )
                return -1;
            if ( file1.isDirectory( ) && (file2.isFile( ) || isAncestor( file1, file2 )) )
                return 1;
            return file1.compareTo( file2 );
        }

        //--------------------------------------------------------------

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    public FileEraser( )
    {
        buffer = new byte[BUFFER_SIZE];
        prng = new Prng01( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    public void erase( List<String> pathnames )
        throws AppException
    {
        // Convert pathnames to canonical form
        File[] files = new File[pathnames.size( )];
        int numPasses = AppConfig.getInstance( ).getFileErasureNumPasses( );
        long totalLength = 0;
        for ( int i = 0; i < files.length; ++i )
        {
            File file = new File( PropertyString.parsePathname( pathnames.get( i ) ) );
            try
            {
                files[i] = file.getCanonicalFile( );
                if ( file.isDirectory( ) )
                    totalLength += ASSUMED_DIRECTORY_LENGTH;
                else if ( file.isFile( ) )
                    totalLength += numPasses * file.length( );
            }
            catch ( IOException e )
            {
                throw new FileException( ErrorId.FAILED_TO_GET_PATHNAME, file );
            }
        }

        // Sort pathnames
        Arrays.sort( files, new PathnameComparator( ) );

        // Reset progress in progress view
        TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView( );
        progressView.setProgress( 0, 0.0 );
        progressView.setProgress( 1, 0.0 );

        // Delete files and directories
        long offset = 0;
        for ( int i = 0; i < files.length; ++i )
        {
            // Test whether oepration has been cancelled
            Task.throwIfCancelled( );

            // Get file length
            File file = files[i];
            long length = file.isDirectory( ) ? ASSUMED_DIRECTORY_LENGTH
                                              : file.isFile( ) ? numPasses * file.length( )
                                                               : 0;

            // Update information in progress view
            progressView.setInfo( ERASING_STR, file );
            progressView.setProgress( 0, 0.0 );
            progressView.initOverallProgress( offset, length, totalLength );

            // Delete file or directory
            try
            {
                if ( file.isDirectory( ) )
                    eraseDirectory( file );
                else if ( file.isFile( ) )
                    eraseFile( file, numPasses );
                else
                    throw new FileException( ErrorId.FILE_OR_DIRECTORY_DOES_NOT_EXIST, file );
            }
            catch ( TaskCancelledException e )
            {
                throw e;
            }
            catch ( AppException e )
            {
                String[] optionStrs = Util.getOptionStrings( RETRY_STR, SKIP_STR );
                int result = JOptionPane.showOptionDialog( App.getInstance( ).getMainWindow( ), e,
                                                           ERASE_FILES_STR,
                                                           JOptionPane.YES_NO_CANCEL_OPTION,
                                                           JOptionPane.QUESTION_MESSAGE, null, optionStrs,
                                                           optionStrs[0] );
                if ( result == JOptionPane.YES_OPTION )
                {
                    --i;
                    length = 0;
                }
                else if ( result != JOptionPane.NO_OPTION )
                    break;
            }
            finally
            {
                offset += length;
            }
        }
    }

    //------------------------------------------------------------------

    private void eraseDirectory( File directory )
        throws AppException
    {
        if ( directory.list( ).length == 0 )
        {
            // Rename directory
            File renamedDirectory = getNewName( directory );
            try
            {
                if ( !directory.renameTo( renamedDirectory ) )
                    throw new FileException( ErrorId.FAILED_TO_RENAME_DIRECTORY, directory );
            }
            catch ( SecurityException e )
            {
                throw new FileException( ErrorId.FAILED_TO_RENAME_DIRECTORY, directory, e );
            }

            // Delete directory
            try
            {
                if ( !renamedDirectory.delete( ) )
                    throw new FileException( ErrorId.FAILED_TO_DELETE_DIRECTORY, renamedDirectory );
            }
            catch ( SecurityException e )
            {
                throw new FileException( ErrorId.FAILED_TO_DELETE_DIRECTORY, renamedDirectory, e );
            }
        }

        // Update progress of task
        ((TaskProgressDialog)Task.getProgressView( )).setProgress( 1.0 );
    }

    //------------------------------------------------------------------

    private void eraseFile( File file,
                            int  numPasses )
        throws AppException
    {
        // Overwrite file
        RandomAccessFile raFile = null;
        try
        {
            // Open file for writing
            try
            {
                raFile = new RandomAccessFile( file, "rw" );
            }
            catch ( FileNotFoundException e )
            {
                throw new FileException( ErrorId.FAILED_TO_OPEN_FILE, file, e );
            }
            catch ( SecurityException e )
            {
                throw new FileException( ErrorId.FILE_ACCESS_NOT_PERMITTED, file, e );
            }

            // Lock file
            try
            {
                if ( raFile.getChannel( ).tryLock( ) == null )
                    throw new FileException( ErrorId.FAILED_TO_LOCK_FILE, file );
            }
            catch ( Exception e )
            {
                throw new FileException( ErrorId.FAILED_TO_LOCK_FILE, file, e );
            }

            // Overwrite file
            try
            {
                for ( int i = 0; i < numPasses; ++i )
                    overwrite( raFile, i, numPasses );
            }
            catch ( TaskCancelledException e )
            {
                throw e;
            }
            catch ( AppException e )
            {
                throw new FileException( e, file );
            }

            // Close file
            try
            {
                RandomAccessFile tempRaFile = raFile;
                raFile = null;
                tempRaFile.close( );
            }
            catch ( IOException e )
            {
                throw new FileException( ErrorId.FAILED_TO_CLOSE_FILE, file );
            }

            // Rename file
            File renamedFile = getNewName( file );
            try
            {
                if ( !file.renameTo( renamedFile ) )
                    throw new FileException( ErrorId.FAILED_TO_RENAME_FILE, file );
            }
            catch ( SecurityException e )
            {
                throw new FileException( ErrorId.FAILED_TO_RENAME_FILE, file, e );
            }

            // Delete file
            try
            {
                if ( !renamedFile.delete( ) )
                    throw new FileException( ErrorId.FAILED_TO_DELETE_FILE, renamedFile );
            }
            catch ( SecurityException e )
            {
                throw new FileException( ErrorId.FAILED_TO_DELETE_FILE, renamedFile, e );
            }
        }
        catch ( AppException e )
        {
            // Close file
            if ( raFile != null )
            {
                try
                {
                    raFile.close( );
                }
                catch ( IOException e1 )
                {
                    // ignore
                }
            }

            // Rethrow exception
            throw e;
        }
    }

    //------------------------------------------------------------------

    private void overwrite( RandomAccessFile file,
                            int              pass,
                            int              numPasses )
        throws AppException
    {
        // Get file length
        long length = 0;
        try
        {
            length = file.length( );
        }
        catch ( IOException e )
        {
            throw new AppException( ErrorId.ERROR_READING_FILE, e );
        }

        // Overwrite file
        try
        {
            // Seek start of file
            file.seek( 0 );

            // Overwrite file
            long offset = 0;
            while ( offset < length )
            {
                // Test whether oepration has been cancelled
                Task.throwIfCancelled( );

                // Fill buffer with random bytes
                prng.nextBytes( buffer );

                // Write file
                int blockLength = (int)Math.min( length - offset, BUFFER_SIZE );
                file.write( buffer, 0, blockLength );
                offset += blockLength;

                // Update progress of task
                ((TaskProgressDialog)Task.getProgressView( )).
                            setProgress( (double)(pass * length + offset) / (double)(numPasses * length) );
            }
        }
        catch ( IOException e )
        {
            throw new AppException( ErrorId.ERROR_WRITING_FILE, e );
        }
    }

    //------------------------------------------------------------------

    private File getNewName( File file )
        throws AppException
    {
        String filename = file.getName( );
        char[] chars = new char[Math.max( filename.length( ), MAX_FILENAME_LENGTH )];
        File renamedFile = null;
        while ( true )
        {
            for ( int i = 0; i < chars.length; ++i )
                chars[i] = (char)('a' + prng.nextInt( 'z' - 'a' + 1 ));
            renamedFile = new File( file.getParentFile( ), new String( chars ) );
            if ( !renamedFile.exists( ) )
                break;
        }
        return renamedFile;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private byte[]  buffer;
    private Prng01  prng;

}

//----------------------------------------------------------------------
