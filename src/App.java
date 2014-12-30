/*====================================================================*\

App.java

Application class.

\*====================================================================*/


// IMPORTS


import java.awt.Dimension;
import java.awt.Point;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import uk.org.blankaspect.crypto.FileConcealer;
import uk.org.blankaspect.crypto.Fortuna;
import uk.org.blankaspect.crypto.FortunaAes256;
import uk.org.blankaspect.crypto.FortunaCipher;
import uk.org.blankaspect.crypto.StandardCsprng;
import uk.org.blankaspect.crypto.StreamConcealer;
import uk.org.blankaspect.crypto.StreamEncrypter;

import uk.org.blankaspect.exception.AppException;
import uk.org.blankaspect.exception.CancelledException;
import uk.org.blankaspect.exception.ExceptionUtilities;
import uk.org.blankaspect.exception.FileException;
import uk.org.blankaspect.exception.TaskCancelledException;
import uk.org.blankaspect.exception.UnexpectedRuntimeException;

import uk.org.blankaspect.gui.GuiUtilities;
import uk.org.blankaspect.gui.TextRendering;

import uk.org.blankaspect.textfield.TextFieldUtilities;

import uk.org.blankaspect.util.BinaryFile;
import uk.org.blankaspect.util.ByteDataInputStream;
import uk.org.blankaspect.util.CalendarTime;
import uk.org.blankaspect.util.FileImporter;
import uk.org.blankaspect.util.ImportQueue;
import uk.org.blankaspect.util.NoYes;
import uk.org.blankaspect.util.NumberUtilities;
import uk.org.blankaspect.util.PropertyString;
import uk.org.blankaspect.util.ResourceProperties;
import uk.org.blankaspect.util.StringUtilities;

import uk.org.blankaspect.windows.FileAssociations;

//----------------------------------------------------------------------


// APPLICATION CLASS


class App
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    public static final     String  SHORT_NAME  = "Qana";
    public static final     String  LONG_NAME   = "Qana";
    public static final     String  NAME_KEY    = "qana";

    private static final    int ENCRYPTION_ID   = 0x7E391D06;

    private static final    int ENCRYPTION_VERSION                  = 0;
    private static final    int ENCRYPTION_MIN_SUPPORTED_VERSION    = 0;
    private static final    int ENCRYPTION_MAX_SUPPORTED_VERSION    = 0;

    private static final    int MAX_NUM_DOCUMENTS   = 64;

    private static final    int TIMER_INTERVAL  = 500;

    private static final    String  DEBUG_PROPERTY_KEY      = "app.debug";
    private static final    String  VERSION_PROPERTY_KEY    = "version";
    private static final    String  BUILD_PROPERTY_KEY      = "build";
    private static final    String  RELEASE_PROPERTY_KEY    = "release";
    private static final    String  OS_NAME_KEY             = "os.name";

    private static final    String  BUILD_PROPERTIES_PATHNAME   = "resources/build.properties";

    private static final    String  ASSOC_SCRIPT_DIR_PREFIX = NAME_KEY + "_";
    private static final    String  ASSOC_SCRIPT_FILENAME   = NAME_KEY + "Associations";

    private static final    String  DEBUG_STR               = " Debug";
    private static final    String  CONFIG_ERROR_STR        = "Configuration error";
    private static final    String  LAF_ERROR1_STR          = "Look-and-feel: ";
    private static final    String  LAF_ERROR2_STR          = "\nThe look-and-feel is not installed.";
    private static final    String  KEY_DATABASE_STR        = "Key database";
    private static final    String  READ_KEYS_STR           = "Read keys";
    private static final    String  WRITE_KEYS_STR          = "Write keys";
    private static final    String  READ_SEED_FILE_STR      = "Read seed file";
    private static final    String  WRITE_SEED_FILE_STR     = "Write seed file";
    private static final    String  OPEN_FILE_STR           = "Open file";
    private static final    String  REVERT_FILE_STR         = "Revert file";
    private static final    String  CLOSE_TEXT_STR          = "Close text document";
    private static final    String  SAVE_FILE_AS_STR        = "Save file as";
    private static final    String  SAVE_CLOSE_FILE_STR     = "Save file before closing";
    private static final    String  MODIFIED_FILE_STR       = "Modified file";
    private static final    String  READ_FILE_STR           = "Read file";
    private static final    String  WRITE_FILE_STR          = "Write file";
    private static final    String  ENCRYPT_FILE_STR        = "Encrypt file";
    private static final    String  DECRYPT_FILE_STR        = "Decrypt file";
    private static final    String  VALIDATE_FILE_STR       = "Validate file";
    private static final    String  FILE_VALID_STR          = "The file was valid.";
    private static final    String  CONCEAL_FILE_STR        = "Conceal file";
    private static final    String  CONCEALING_STR          = "Concealing";
    private static final    String  RECOVER_FILE_STR        = "Recover file";
    private static final    String  RECOVERING_STR          = "Recovering";
    private static final    String  SPLIT_FILE_STR          = "Split file";
    private static final    String  JOIN_FILES_STR          = "Join files";
    private static final    String  ERASE_FILES_STR         = "Erase files";
    private static final    String  PROCESS_FILES_STR       = "Process files";
    private static final    String  ALL_VALID_STR           = "All files were valid.";
    private static final    String  RECOVER_TEXT_STR        = "Recover text";
    private static final    String  REVERT_STR              = "Revert";
    private static final    String  SAVE_STR                = "Save";
    private static final    String  DISCARD_STR             = "Discard";
    private static final    String  PROCEED_STR             = "Proceed";
    private static final    String  KEEP_STR                = "Keep";
    private static final    String  EXIT_STR                = "Exit";
    private static final    String  REVERT_MESSAGE_STR      = "\nDo you want discard the changes to the " +
                                                                "current document and reopen the " +
                                                                "original file?";
    private static final    String  MODIFIED_MESSAGE_STR    = "\nThe file has been modified externally.\n" +
                                                                "Do you want to open the modified file?";
    private static final    String  UNNAMED_FILE_STR        = "The unnamed file";
    private static final    String  CHANGED_MESSAGE1_STR    = "\nThe file";
    private static final    String  CHANGED_MESSAGE2_STR    = " has changed.\nDo you want to save the " +
                                                                "changed file?";
    private static final    String  CLOSE_MESSAGE_STR       = "Do you want to close the text document?";
    private static final    String  REMAINING_IMPORTS_STR   = "Do you want to continue to process the " +
                                                                "remaining files?";
    private static final    String  PRNG_NOT_SEEDED_STR     = "The pseudo-random number generator was " +
                                                                "not seeded.\nYou should wait until " +
                                                                "enough entropy has accumulated before " +
                                                                "performing encryption.";
    private static final    String  SET_GLOBAL_KEY_STR      = "Set global key";
    private static final    String  KEY_FOR_STR             = "Key for ";
    private static final    String  TEMP_KEY_STR            = "Encrypt with temporary key";
    private static final    String  TEMP_KEY_MESSAGE_STR    = "You are about to encrypt with a temporary " +
                                                                "key.\nDo you want to proceed?";
    private static final    String  CLEAR_KEY_STR           = "Clear global key";
    private static final    String  CLEAR_KEY_MESSAGE_STR   = "Do you want to clear the global key?";
    private static final    String  NO_KEY_DATABASE1_STR    = "File: %1\nThe key database file does not " +
                                                                "exist.\nWhen the program exits, a key " +
                                                                "database will be created at the " +
                                                                "location\nspecified in the preferences.";
    private static final    String  NO_KEY_DATABASE2_STR    = "No key database was specified.\nThe " +
                                                                "changes you have made to persistent " +
                                                                "keys will be lost if you exit now.\n" +
                                                                "To save the keys, specify the location " +
                                                                "of the key database in the preferences.";
    private static final    String  SELECT_STR              = "Select";
    private static final    String  WINDOWS_STR             = "Windows";
    private static final    String  FILE_ASSOCIATIONS_STR   = "File associations";
    private static final    String  FILE_PARTS_STR          = "The output directory already contains " +
                                                                "%1 file part%2.\n" +
                                                                "Do you want to delete %3 file%4?";
    private static final    String[][]  FILE_PARTS_STRS     =
    {
        { "", "s" },
        { "this", "these" },
        { "", "s" }
    };

    private enum RandomDataStreamState
    {
        HEADER,
        SALT,
        KDF_PARAMS,
        RANDOM_DATA
    }

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


    // FILE OPERATION


    private enum FileOperation
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        PROCESS
        (
            null,
            "processed"
        ),

        OPEN
        (
            OPEN_FILE_STR,
            "opened"
        ),

        ENCRYPT
        (
            ENCRYPT_FILE_STR,
            "encrypted"
        ),

        DECRYPT
        (
            DECRYPT_FILE_STR,
            "decrypted"
        ),

        VALIDATE
        (
            VALIDATE_FILE_STR,
            "validated"
        );

        //--------------------------------------------------------------

        private static final    Set<FileOperation>  COUNTABLE_OPERATIONS    = EnumSet.of
        (
            ENCRYPT,
            DECRYPT,
            VALIDATE
        );

        private static final    String  NUM_FILES_STR   = "Number of files ";

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private FileOperation( String titleStr,
                               String completedStr )
        {
            this.titleStr = titleStr;
            this.completedStr = completedStr;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Class methods
    ////////////////////////////////////////////////////////////////////

        private static void initCounts( )
        {
            operationCounts.clear( );
            for ( FileOperation fileOp : FileOperation.values( ) )
                operationCounts.put( fileOp, 0 );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods
    ////////////////////////////////////////////////////////////////////

        private boolean isOperation( )
        {
            return ( operationCounts.get( this ) > 0 );
        }

        //--------------------------------------------------------------

        private String getCountString( )
        {
            return ( NUM_FILES_STR + completedStr + " = " + operationCounts.get( this ) );
        }

        //--------------------------------------------------------------

        private void incrementCount( )
        {
            operationCounts.put( this, operationCounts.get( this ) + 1 );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Class variables
    ////////////////////////////////////////////////////////////////////

        private static  Map<FileOperation, Integer> operationCounts = new EnumMap<>( FileOperation.class );

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        private String  titleStr;
        private String  completedStr;

    }

    //==================================================================


    // ERROR IDENTIFIERS


    private enum ErrorId
        implements AppException.Id
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        FILE_DOES_NOT_EXIST
        ( "The file does not exist." ),

        NOT_A_FILE
        ( "The pathname does not denote a file." ),

        NOT_A_DIRECTORY
        ( "The pathname does not denote a directory." ),

        FAILED_TO_CREATE_ARCHIVE_DIRECTORY
        ( "Failed to create the output directory." ),

        FAILED_TO_CREATE_TEMPORARY_FILE
        ( "Failed to create a temporary file." ),

        FAILED_TO_DELETE_TEMPORARY_FILE
        ( "Failed to delete the temporary file." ),

        FAILED_TO_CREATE_OUTPUT_DIRECTORY
        ( "Failed to create the output directory." ),

        FAILED_TO_GET_TIMESTAMP
        ( "Failed to get the timestamp of the file." ),

        FAILED_TO_SET_TIMESTAMP
        ( "Failed to set the timestamp of the file." ),

        FAILED_TO_DELETE_FILE_PART
        ( "Failed to delete the file part." ),

        FAILED_TO_CLEAR_CLIPBOARD
        ( "Failed to clear the system clipboard." ),

        PAYLOAD_TOO_LARGE
        ( "The payload is too large to be concealed." ),

        NOT_ENOUGH_MEMORY
        ( "There was not enough memory to perform the command." ),

        NOT_ENOUGH_MEMORY_TO_GENERATE_CARRIER_IMAGE
        ( "There was not enough memory to generate a carrier image of the required size." );

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


    // DOCUMENT-VIEW CLASS


    private static class DocumentView
    {

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private DocumentView( Document document )
        {
            this.document = document;
            view = document.createView( );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        private Document    document;
        private View        view;

    }

    //==================================================================


    // CONCEALED PAYLOAD LENGTH ENCODER AND DECODER CLASS


    private static class LengthCoder
        implements StreamConcealer.LengthDecoder, StreamConcealer.LengthEncoder
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        private static final    int MAX_LENGTH_SIZE = StreamConcealer.MAX_NUM_LENGTH_BITS / Byte.SIZE;

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private LengthCoder( )
        {
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : StreamConcealer.LengthDecoder interface
    ////////////////////////////////////////////////////////////////////

        public int getLengthFieldNumBits( int numPixels )
        {
            int maxLength = numPixels + (numPixels >>> 1);
            return ( Integer.SIZE - Integer.numberOfLeadingZeros( maxLength ) );
        }

        //--------------------------------------------------------------

        public int decodeLength( byte[] data,
                                 int    numPixels )
        {
            // Create PRNG
            Fortuna prng = createPrng( numPixels );

            // Get permutation of bit indices
            int numLengthBits = getLengthFieldNumBits( numPixels );
            int[] indices = getBitIndices( numLengthBits, prng );

            // XOR length data
            prng.createCombiner( MAX_LENGTH_SIZE ).combine( data );

            // Extract length from input data
            int length = 0;
            for ( int i = 0; i < numLengthBits; ++i )
            {
                int index = indices[i];
                if ( (data[index >>> 3] & 1 << (index & 0x07)) != 0 )
                    length |= 1 << i;
            }

            // Return length
            return length;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : StreamConcealer.LengthEncoder interface
    ////////////////////////////////////////////////////////////////////

        public byte[] encodeLength( int length,
                                    int numPixels )
        {
            // Create PRNG
            Fortuna prng = createPrng( numPixels );

            // Get permutation of bit indices
            int numLengthBits = getLengthFieldNumBits( numPixels );
            int[] indices = getBitIndices( numLengthBits, prng );

            // Permute bits of length
            byte[] data = new byte[NumberUtilities.roundUpQuotientInt( numLengthBits, Byte.SIZE )];
            for ( int i = 0; i < numLengthBits; ++i )
            {
                int index = indices[i];
                if ( (length & 1 << i) != 0 )
                    data[index >>> 3] |= 1 << (index & 0x07);
            }

            // XOR length data
            prng.createCombiner( MAX_LENGTH_SIZE ).combine( data );

            // Return encoded length data
            return data;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods
    ////////////////////////////////////////////////////////////////////

        private Fortuna createPrng( int value )
        {
            byte[] key = new byte[FortunaAes256.KEY_SIZE];
            NumberUtilities.intToBytesLE( value, key );
            return new FortunaAes256( key );
        }

        //--------------------------------------------------------------

        private int[] getBitIndices( int     numIndices,
                                     Fortuna prng )
        {
            final   int NUM_ITERATIONS  = 1000;

            int[] indices = new int[numIndices];
            for ( int i = 0; i < NUM_ITERATIONS; ++i )
            {
                for ( int j = 0; j < numIndices; ++j )
                {
                    int k = ((prng.getRandomByte( ) & 0xFF) * (j + 1)) >>> 8;
                    indices[j] = indices[k];
                    indices[k] = j;
                }
            }
            return indices;
        }

        //--------------------------------------------------------------

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


    // RANDOM DATA INPUT STREAM CLASS


    private class RandomDataInputStream
        extends InputStream
        implements ByteDataInputStream
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        private static final    int KDF_PARAM_DATA_SIZE = 4;
        private static final    int BLOCK_LENGTH        = 4096;

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private RandomDataInputStream( int     length,
                                       boolean hasHeader )
        {
            StreamEncrypter encrypter = new StreamEncrypter( Util.getCipher( ),
                                                             hasHeader ? getEncryptionHeader( ) : null );
            this.length = length + encrypter.getMaxOverheadSize( );
            state = hasHeader ? RandomDataStreamState.HEADER : RandomDataStreamState.SALT;
            prng = new FortunaAes256( getRandomKey( ) );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : ByteDataInputStream interface
    ////////////////////////////////////////////////////////////////////

        public long getLength( )
        {
            return length;
        }

        //--------------------------------------------------------------

        public void reset( )
        {
            // do nothing
        }

        //--------------------------------------------------------------

        public int read( byte[] buffer,
                         int    offset,
                         int    length )
        {
            byte[] data = null;
            switch ( state )
            {
                case HEADER:
                    data = getEncryptionHeader( ).toByteArray( );
                    state = RandomDataStreamState.SALT;
                    break;

                case SALT:
                    salt = getRandomBytes( StreamEncrypter.SALT_SIZE );
                    data = salt;
                    state = RandomDataStreamState.KDF_PARAMS;
                    break;

                case KDF_PARAMS:
                    data = new byte[KDF_PARAM_DATA_SIZE];
                    NumberUtilities.
                            intToBytesLE( KdfUse.GENERATION.getKdfParameters( ).getEncodedValue( false ),
                                          data );
                    FortunaAes256.createCombiner( salt, KDF_PARAM_DATA_SIZE ).combine( data );
                    state = RandomDataStreamState.RANDOM_DATA;
                    break;

                case RANDOM_DATA:
                    data = prng.getRandomBytes( Math.min( length, BLOCK_LENGTH ) );
                    break;
            }
            System.arraycopy( data, 0, buffer, offset, data.length );
            return data.length;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : overriding methods
    ////////////////////////////////////////////////////////////////////

        /**
         * This method implements an abstract method in java.io.InputStream.  It is implemented only to
         * complete the definition of the RandomDataInputStream class, and it should never be called.
         */

        @Override
        public int read( )
        {
            throw new UnexpectedRuntimeException( );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        private int                     length;
        private RandomDataStreamState   state;
        private Fortuna                 prng;
        private byte[]                  salt;

    }

    //==================================================================


    // CONCEALER RANDOM SOURCE CLASS


    private class RandomSource
        implements StreamConcealer.RandomSource
    {

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private RandomSource( )
        {
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : StreamConcealer.RandomSource interface
    ////////////////////////////////////////////////////////////////////

        public int getRandomBytes( byte[] buffer,
                                   int    offset,
                                   int    length )
        {
            prng.getRandomBytes( buffer, offset, length );
            return length;
        }

        //--------------------------------------------------------------

    }

    //==================================================================


    // CARRIER IMAGE GENERATOR CLASS


    private class CarrierImageGenerator
        implements FileConcealer.ImageSource
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        private static final    int WIDTH_FACTOR    = 4;
        private static final    int HEIGHT_FACTOR   = 3;

        private static final    int SIZE_INTERVAL   = 16;
        private static final    int MIN_WIDTH       = WIDTH_FACTOR * 2 * SIZE_INTERVAL;
        private static final    int MIN_HEIGHT      = HEIGHT_FACTOR * 2 * SIZE_INTERVAL;

        private static final    int MIN_CELL_SIZE   = 16;

        private static final    int BYTES_PER_PIXEL = 3;

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private CarrierImageGenerator( )
        {
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : FileConcealer.ImageSource interface
    ////////////////////////////////////////////////////////////////////

        public BufferedImage getImage( int payloadLength )
            throws AppException
        {
            if ( payloadLength > StreamConcealer.MAX_PAYLOAD_LENGTH )
                throw new AppException( ErrorId.PAYLOAD_TOO_LARGE );

            try
            {
                long numPixels = NumberUtilities.
                                    roundUpQuotientLong( (long)payloadLength * 8,
                                                         BYTES_PER_PIXEL * CarrierImage.NUM_CARRIER_BITS );
                double d = Math.sqrt( (double)numPixels / (double)(WIDTH_FACTOR * HEIGHT_FACTOR) );
                int width = NumberUtilities.roundUpInt( (int)Math.ceil( d * (double)WIDTH_FACTOR ),
                                                        SIZE_INTERVAL );
                int height = NumberUtilities.roundUpInt( (int)Math.ceil( d * (double)HEIGHT_FACTOR ),
                                                         SIZE_INTERVAL );
                CarrierImage.Kind imageKind = AppConfig.getInstance( ).getCarrierImageKind( );
                return new CarrierImage( Math.max( MIN_WIDTH, width ), Math.max( MIN_HEIGHT, height ),
                                         Math.max( MIN_CELL_SIZE, width / imageKind.getCellSizeDivisor( ) ),
                                         imageKind ).getImage( );
            }
            catch ( OutOfMemoryError e )
            {
                throw new AppException( ErrorId.NOT_ENOUGH_MEMORY_TO_GENERATE_CARRIER_IMAGE );
            }
        }

        //--------------------------------------------------------------

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private App( )
    {
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

    public static void main( String[] args )
    {
        getInstance( ).init( args );
    }

    //------------------------------------------------------------------

    public static App getInstance( )
    {
        if ( instance == null )
            instance = new App( );
        return instance;
    }

    //------------------------------------------------------------------

    public static boolean isDebug( )
    {
        return debug;
    }

    //------------------------------------------------------------------

    public static StreamEncrypter.Header getEncryptionHeader( )
    {
        return new StreamEncrypter.Header( ENCRYPTION_ID, ENCRYPTION_VERSION,
                                           ENCRYPTION_MIN_SUPPORTED_VERSION,
                                           ENCRYPTION_MAX_SUPPORTED_VERSION );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    public MainWindow getMainWindow( )
    {
        return mainWindow;
    }

    //------------------------------------------------------------------

    public int getNumDocuments( )
    {
        return documentsViews.size( );
    }

    //------------------------------------------------------------------

    public boolean hasDocuments( )
    {
        return !documentsViews.isEmpty( );
    }

    //------------------------------------------------------------------

    public boolean isDocumentsFull( )
    {
        return ( documentsViews.size( ) >= MAX_NUM_DOCUMENTS );
    }

    //------------------------------------------------------------------

    public Document getDocument( )
    {
        return ( (hasDocuments( ) && (mainWindow != null)) ? getDocument( mainWindow.getTabIndex( ) )
                                                           : null );
    }

    //------------------------------------------------------------------

    public Document getDocument( int index )
    {
        return ( hasDocuments( ) ? documentsViews.get( index ).document : null );
    }

    //------------------------------------------------------------------

    public ArchiveDocument getArchiveDocument( )
    {
        Document document = getDocument( );
        return ( (document instanceof ArchiveDocument) ? (ArchiveDocument)document : null );
    }

    //------------------------------------------------------------------

    public ArchiveDocument getArchiveDocument( int index )
    {
        Document document = getDocument( index );
        return ( (document instanceof ArchiveDocument) ? (ArchiveDocument)document : null );
    }

    //------------------------------------------------------------------

    public TextDocument getTextDocument( )
    {
        Document document = getDocument( );
        return ( (document instanceof TextDocument) ? (TextDocument)document : null );
    }

    //------------------------------------------------------------------

    public TextDocument getTextDocument( int index )
    {
        Document document = getDocument( index );
        return ( (document instanceof TextDocument) ? (TextDocument)document : null );
    }

    //------------------------------------------------------------------

    public View getView( )
    {
        return ( (hasDocuments( ) && (mainWindow != null)) ? getView( mainWindow.getTabIndex( ) ) : null );
    }

    //------------------------------------------------------------------

    public View getView( int index )
    {
        return ( hasDocuments( ) ? documentsViews.get( index ).view : null );
    }

    //------------------------------------------------------------------

    public View getView( Document document )
    {
        for ( DocumentView documentView : documentsViews )
        {
            if ( documentView.document == document )
                return documentView.view;
        }
        return null;
    }

    //------------------------------------------------------------------

    public ArchiveView getArchiveView( )
    {
        View view = getView( );
        return ( (view instanceof ArchiveView) ? (ArchiveView)view : null );
    }

    //------------------------------------------------------------------

    public ArchiveView getArchiveView( int index )
    {
        View view = getView( index );
        return ( (view instanceof ArchiveView) ? (ArchiveView)view : null );
    }

    //------------------------------------------------------------------

    public TextView getTextView( )
    {
        View view = getView( );
        return ( (view instanceof TextView) ? (TextView)view : null );
    }

    //------------------------------------------------------------------

    public TextView getTextView( int index )
    {
        View view = getView( index );
        return ( (view instanceof TextView) ? (TextView)view : null );
    }

    //------------------------------------------------------------------

    public String getVersionString( )
    {
        StringBuilder buffer = new StringBuilder( 32 );
        String str = buildProperties.get( VERSION_PROPERTY_KEY );
        if ( str != null )
            buffer.append( str );

        str = buildProperties.get( RELEASE_PROPERTY_KEY );
        if ( str == null )
        {
            long time = System.currentTimeMillis( );
            if ( buffer.length( ) > 0 )
                buffer.append( ' ' );
            buffer.append( 'b' );
            buffer.append( CalendarTime.dateToString( time ) );
            buffer.append( '-' );
            buffer.append( CalendarTime.hoursMinsToString( time ) );
        }
        else
        {
            NoYes release = NoYes.forKey( str );
            if ( (release == null) || !release.toBoolean( ) )
            {
                str = buildProperties.get( BUILD_PROPERTY_KEY );
                if ( str != null )
                {
                    if ( buffer.length( ) > 0 )
                        buffer.append( ' ' );
                    buffer.append( str );
                }
            }
        }

        if ( debug )
            buffer.append( DEBUG_STR );

        return buffer.toString( );
    }

    //------------------------------------------------------------------

    public byte[] getRandomBytes( int length )
    {
        return prng.getRandomBytes( length );
    }

    //------------------------------------------------------------------

    public int getRandomInt( )
    {
        return prng.getRandomInt( );
    }

    //------------------------------------------------------------------

    public long getRandomLong( )
    {
        return prng.getRandomLong( );
    }

    //------------------------------------------------------------------

    public byte[] getRandomKey( )
    {
        return prng.getRandomBytes( FortunaAes256.KEY_SIZE );
    }

    //------------------------------------------------------------------

    public boolean canPrngReseed( )
    {
        return prng.getPrng( ).canReseed( );
    }

    //------------------------------------------------------------------

    public StreamConcealer.LengthEncoder getLengthEncoder( )
    {
        return new LengthCoder( );
    }

    //------------------------------------------------------------------

    public StreamConcealer.LengthDecoder getLengthDecoder( )
    {
        return new LengthCoder( );
    }

    //------------------------------------------------------------------

    public StreamConcealer.RandomSource getRandomSource( )
    {
        return new RandomSource( );
    }

    //------------------------------------------------------------------

    public FileConcealer.ImageSource getCarrierImageSource( )
    {
        return new CarrierImageGenerator( );
    }

    //------------------------------------------------------------------

    public void addImport( FileImporter fileImporter,
                           File...      files )
    {
        importQueue.add( fileImporter, files );
        SwingUtilities.invokeLater( new Runnable( ){ public void run( ){ doImport( ); } } );
    }

    //------------------------------------------------------------------

    public KeyList.Key getGlobalKey( )
    {
        return globalKey;
    }

    //------------------------------------------------------------------

    public void addTemporaryKey( KeyList.Key key )
    {
        temporaryKeyList.addKey( key );
    }

    //------------------------------------------------------------------

    public void showInfoMessage( String titleStr,
                                 Object message )
    {
        showMessageDialog( titleStr, message, JOptionPane.INFORMATION_MESSAGE );
    }

    //------------------------------------------------------------------

    public void showWarningMessage( String titleStr,
                                    Object message )
    {
        showMessageDialog( titleStr, message, JOptionPane.WARNING_MESSAGE );
    }

    //------------------------------------------------------------------

    public void showErrorMessage( String titleStr,
                                  Object message )
    {
        showMessageDialog( titleStr, message, JOptionPane.ERROR_MESSAGE );
    }

    //------------------------------------------------------------------

    public void showMessageDialog( String titleStr,
                                   Object message,
                                   int    messageKind )
    {
        JOptionPane.showMessageDialog( mainWindow, message, titleStr, messageKind );
    }

    //------------------------------------------------------------------

    public boolean confirmWriteFile( File   file,
                                     String titleStr )
    {
        String[] optionStrs = Util.getOptionStrings( AppConstants.REPLACE_STR );
        return ( !file.exists( ) ||
                 (JOptionPane.showOptionDialog( mainWindow,
                                                Util.getPathname( file ) + AppConstants.ALREADY_EXISTS_STR,
                                                titleStr, JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.WARNING_MESSAGE, null, optionStrs,
                                                optionStrs[1] ) == JOptionPane.OK_OPTION) );
    }

    //------------------------------------------------------------------

    public void importFiles( File[] files )
    {
        processFiles( files );
    }

    //------------------------------------------------------------------

    public KeyList.Key getKey( String operationStr,
                               String targetStr )
    {
        KeyList.Key key = AppConfig.getInstance( ).isAutoUseGlobalKey( ) ? globalKey : null;
        if ( key == null )
            key = selectKey( (targetStr == null) ? operationStr
                                                 : operationStr + " | " + KEY_FOR_STR + targetStr );
        return key;
    }

    //------------------------------------------------------------------

    public KeyList.Key selectKey( String titleStr )
    {
        List<KeyList.Key> keys = new ArrayList<>( );
        keys.add( KeyList.createKey( ) );
        keys.addAll( temporaryKeyList.getKeys( ) );
        keys.addAll( persistentKeyList.getKeys( ) );
        return KeySelectionDialog.showDialog( mainWindow, titleStr, keys );
    }

    //------------------------------------------------------------------

    public void setArchiveDirectory( File directory )
    {
        ArchiveDocument document = getArchiveDocument( );
        if ( document != null )
        {
            try
            {
                // Test for directory; create it if it doesn't exist
                if ( directory.exists( ) )
                {
                    if ( !directory.isDirectory( ) )
                        throw new FileException( ErrorId.NOT_A_DIRECTORY, directory );
                }
                else
                {
                    if ( !directory.mkdirs( ) )
                        throw new FileException( ErrorId.FAILED_TO_CREATE_ARCHIVE_DIRECTORY, directory );
                }

                // Set directory in document and view
                document.setArchiveDirectory( directory );
            }
            catch ( AppException e )
            {
                showErrorMessage( SHORT_NAME, e );
            }
        }
    }

    //------------------------------------------------------------------

    public void concealFile( File        inFile,
                             File        carrierFile,
                             File        outFile,
                             int         maxNumBits,
                             boolean     setTimestamp,
                             boolean     addRandomBits,
                             KeyList.Key key )
        throws AppException
    {
        File tempFile = null;
        try
        {
            // Reset progress in progress view
            TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView( );
            progressView.setInfo( CONCEALING_STR, inFile );
            progressView.setProgress( 0, 0.0 );
            progressView.setProgress( 1, 0.0 );
            progressView.waitForIdle( );

            // Create temporary file
            try
            {
                tempFile = File.createTempFile( AppConstants.TEMP_FILE_PREFIX, null,
                                                outFile.getAbsoluteFile( ).getParentFile( ) );
            }
            catch ( Exception e )
            {
                throw new AppException( ErrorId.FAILED_TO_CREATE_TEMPORARY_FILE, e );
            }

            // Encrypt input file
            progressView.initOverallProgress( 0, 1, 2 );
            key.getFileEncrypter( Util.getCipher( key ) ).
                                                encrypt( inFile, tempFile, key.getKey( ), getRandomKey( ) );

            // Conceal encrypted file
            progressView.initOverallProgress( 1, 1, 2 );
            new FileConcealer( ).conceal( tempFile,
                                          (carrierFile == null) ? getCarrierImageSource( ) : null,
                                          carrierFile, outFile, getLengthEncoder( ), maxNumBits,
                                          addRandomBits ? getRandomSource( ) : null );

            // Delete temporary file
            if ( !tempFile.delete( ) )
                throw new FileException( ErrorId.FAILED_TO_DELETE_TEMPORARY_FILE, tempFile );

            // Set timestamp of output file
            if ( setTimestamp )
            {
                long timestamp = carrierFile.lastModified( );
                if ( timestamp == 0 )
                    throw new FileException( ErrorId.FAILED_TO_GET_TIMESTAMP, carrierFile );
                if ( !outFile.setLastModified( timestamp ) )
                    throw new FileException( ErrorId.FAILED_TO_SET_TIMESTAMP, outFile );
            }
        }
        catch ( AppException e )
        {
            // Delete temporary file
            try
            {
                if ( (tempFile != null) && tempFile.exists( ) )
                    tempFile.delete( );
            }
            catch ( Exception e1 )
            {
                // ignore
            }

            // Rethrow exception
            throw e;
        }
    }

    //------------------------------------------------------------------

    public void recoverFile( File        inFile,
                             File        outFile,
                             KeyList.Key key )
        throws AppException
    {
        final   String  TEMP_FILE_PREFIX    = "recovered";

        File tempFile = null;
        try
        {
            // Reset progress in progress view
            TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView( );
            progressView.setInfo( RECOVERING_STR, inFile );
            progressView.setProgress( 0, 0.0 );
            progressView.setProgress( 1, 0.0 );
            progressView.waitForIdle( );

            // Create temporary file
            try
            {
                tempFile = File.createTempFile( TEMP_FILE_PREFIX, null,
                                                outFile.getAbsoluteFile( ).getParentFile( ) );
            }
            catch ( Exception e )
            {
                throw new AppException( ErrorId.FAILED_TO_CREATE_TEMPORARY_FILE, e );
            }

            // Recover file
            progressView.initOverallProgress( 0, 1, 2 );
            new FileConcealer( ).recover( inFile, tempFile, getLengthDecoder( ) );

            // Decrypt input file
            progressView.initOverallProgress( 1, 1, 2 );
            key.getFileEncrypter( null ).decrypt( tempFile, outFile, key.getKey( ) );

            // Delete temporary file
            if ( !tempFile.delete( ) )
                throw new FileException( ErrorId.FAILED_TO_DELETE_TEMPORARY_FILE, tempFile );
        }
        catch ( AppException e )
        {
            // Delete temporary file
            try
            {
                if ( (tempFile != null) && tempFile.exists( ) )
                    tempFile.delete( );
            }
            catch ( Exception e1 )
            {
                // ignore
            }

            // Rethrow exception
            throw e;
        }
    }

    //------------------------------------------------------------------

    public void chooseArchiveDirectory( File directory )
    {
        JFileChooser fileChooser = FileSelectionKind.ARCHIVE_DIRECTORY.getFileChooser( );
        fileChooser.setSelectedFile( (directory == null) ? new File( new String( ) ) : directory );
        fileChooser.rescanCurrentDirectory( );
        if ( fileChooser.showDialog( mainWindow, SELECT_STR ) == JFileChooser.APPROVE_OPTION )
            setArchiveDirectory( fileChooser.getSelectedFile( ) );
    }

    //------------------------------------------------------------------

    public boolean confirmUseTemporaryKey( KeyList.Key key )
    {
        String[] optionStrs = Util.getOptionStrings( PROCEED_STR );
        return ( (key.getKind( ) != KeyKind.TEMPORARY) ||
                 !AppConfig.getInstance( ).isWarnTemporaryKey( ) ||
                 (JOptionPane.showOptionDialog( mainWindow, TEMP_KEY_MESSAGE_STR, TEMP_KEY_STR,
                                                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                                                null, optionStrs, optionStrs[1] ) ==
                                                                                JOptionPane.OK_OPTION) );
    }

    //------------------------------------------------------------------

    public void updateTabText( Document document )
    {
        for ( int i = 0; i < getNumDocuments( ); ++i )
        {
            if ( getDocument( i ) == document )
            {
                mainWindow.setTabText( i, document.getTitleString( false ),
                                       document.getTitleString( true ) );
                break;
            }
        }
    }

    //------------------------------------------------------------------

    public void updateCommands( )
    {
        boolean isDocument = hasDocuments( );
        ArchiveDocument archiveDocument = getArchiveDocument( );
        boolean isArchiveDocument = (archiveDocument != null);
        boolean notFull = !isDocumentsFull( );
        boolean documentChanged = isArchiveDocument && archiveDocument.isChanged( );
        boolean isWindows = System.getProperty( OS_NAME_KEY, "" ).contains( WINDOWS_STR );

        AppConfig config = AppConfig.getInstance( );

        AppCommand.setAllEnabled( true );

        AppCommand.CREATE_FILE.setEnabled( notFull );
        AppCommand.OPEN_FILE.setEnabled( notFull );
        AppCommand.REVERT_FILE.setEnabled( documentChanged && (archiveDocument.getFile( ) != null) );
        AppCommand.CLOSE_FILE.setEnabled( isDocument );
        AppCommand.CLOSE_ALL_FILES.setEnabled( isDocument );
        AppCommand.SAVE_FILE.setEnabled( documentChanged );
        AppCommand.SAVE_FILE_AS.setEnabled( isArchiveDocument );
        AppCommand.CREATE_TEXT.setEnabled( notFull );
        AppCommand.RECOVER_TEXT.setEnabled( notFull );
        AppCommand.CLEAR_GLOBAL_KEY.setEnabled( globalKey != null );
        AppCommand.TOGGLE_AUTO_USE_GLOBAL_KEY.setSelected( config.isAutoUseGlobalKey( ) );
        AppCommand.TOGGLE_SHOW_FULL_PATHNAMES.setSelected( config.isShowFullPathnames( ) );
        AppCommand.MANAGE_FILE_ASSOCIATIONS.setEnabled( isWindows );
    }

    //------------------------------------------------------------------

    public void executeCommand( AppCommand command )
    {
        try
        {
            switch ( command )
            {
                case TIMER_EXPIRED:
                    onTimerExpired( );
                    break;

                case CREATE_FILE:
                    onCreateFile( );
                    break;

                case OPEN_FILE:
                    onOpenFile( );
                    break;

                case REVERT_FILE:
                    onRevertFile( );
                    break;

                case CLOSE_FILE:
                    onCloseFile( );
                    break;

                case CLOSE_ALL_FILES:
                    onCloseAllFiles( );
                    break;

                case SAVE_FILE:
                    onSaveFile( );
                    break;

                case SAVE_FILE_AS:
                    onSaveFileAs( );
                    break;

                case ENCRYPT_FILE:
                    onEncryptFile( );
                    break;

                case DECRYPT_FILE:
                    onDecryptFile( );
                    break;

                case VALIDATE_FILE:
                    onValidateFile( );
                    break;

                case CONCEAL_FILE:
                    onConcealFile( );
                    break;

                case RECOVER_FILE:
                    onRecoverFile( );
                    break;

                case SPLIT_FILE:
                    onSplitFile( );
                    break;

                case JOIN_FILES:
                    onJoinFiles( );
                    break;

                case ERASE_FILES:
                    onEraseFiles( );
                    break;

                case EXIT:
                    onExit( );
                    break;

                case CREATE_TEXT:
                    onCreateText( );
                    break;

                case RECOVER_TEXT:
                    onRecoverText( );
                    break;

                case SET_GLOBAL_KEY:
                    onSetGlobalKey( );
                    break;

                case CLEAR_GLOBAL_KEY:
                    onClearGlobalKey( );
                    break;

                case TOGGLE_AUTO_USE_GLOBAL_KEY:
                    onToggleAutoUseGlobalKey( );
                    break;

                case EDIT_KEYS:
                    onEditKeys( );
                    break;

                case SHOW_ENTROPY_METRICS:
                    onShowEntropyMetrics( );
                    break;

                case GENERATE_GARBAGE:
                    onGenerateGarbage( );
                    break;

                case TOGGLE_SHOW_FULL_PATHNAMES:
                    onToggleShowFullPathnames( );
                    break;

                case MANAGE_FILE_ASSOCIATIONS:
                    onManageFileAssociations( );
                    break;

                case EDIT_PREFERENCES:
                    onEditPreferences( );
                    break;
            }
        }
        catch ( AppException e )
        {
            showErrorMessage( SHORT_NAME, e );
        }

        // Update main window
        if ( command != AppCommand.TIMER_EXPIRED )
        {
            updateTabText( getDocument( ) );
            mainWindow.updateTitleAndMenus( );
        }
    }

    //------------------------------------------------------------------

    public void closeDocument( int index )
    {
        if ( confirmCloseDocument( index ) )
            removeDocument( index );
    }

    //------------------------------------------------------------------

    private void addDocument( Document document )
    {
        DocumentView documentView = new DocumentView( document );
        documentsViews.add( documentView );
        mainWindow.addView( document.getTitleString( false ), document.getTitleString( true ),
                            documentView.view );
    }

    //------------------------------------------------------------------

    private void removeDocument( int index )
    {
        documentsViews.remove( index );
        mainWindow.removeView( index );
    }

    //------------------------------------------------------------------

    private ArchiveDocument readDocument( File file )
        throws AppException
    {
        ArchiveDocument document = new ArchiveDocument( );
        TaskProgressDialog.showDialog( mainWindow, READ_FILE_STR, new Task.ReadDocument( document, file ) );
        return document;
    }

    //------------------------------------------------------------------

    private void writeDocument( ArchiveDocument document,
                                File            file )
        throws AppException
    {
        TaskProgressDialog.showDialog( mainWindow, WRITE_FILE_STR,
                                       new Task.WriteDocument( document, file ) );
    }

    //------------------------------------------------------------------

    private boolean openDocument( File file )
        throws AppException
    {
        // Test for file
        if ( !file.exists( ) )
            throw new FileException( ErrorId.FILE_DOES_NOT_EXIST, file );
        if ( !file.isFile( ) )
            throw new FileException( ErrorId.NOT_A_FILE, file );

        // Test whether document is already open
        for ( int i = 0; i < documentsViews.size( ); ++i )
        {
            ArchiveDocument document = getArchiveDocument( i );
            if ( (document != null) && Util.isSameFile( file, document.getFile( ) ) )
            {
                mainWindow.selectView( i );
                return true;
            }
        }

        // Read document and add it to list
        try
        {
            addDocument( readDocument( file ) );
            return true;
        }
        catch ( TaskCancelledException e )
        {
            return false;
        }
    }

    //------------------------------------------------------------------

    private void revertDocument( File file )
        throws AppException
    {
        // Read document
        ArchiveDocument document = null;
        try
        {
            document = readDocument( file );
        }
        catch ( TaskCancelledException e )
        {
            // ignore
        }

        // Replace document in list
        int index = mainWindow.getTabIndex( );
        documentsViews.set( index, new DocumentView( document ) );
        mainWindow.setTabText( index, document.getTitleString( false ), document.getTitleString( true ) );
        mainWindow.setView( index, getView( ) );
    }

    //------------------------------------------------------------------

    private boolean confirmCloseDocument( int index )
    {
        // Test whether document has changed
        Document document = getDocument( index );
        if ( (document == null) || !document.isChanged( ) )
            return true;

        // Restore window
        GuiUtilities.restoreFrame( mainWindow );

        // Display document
        mainWindow.selectView( index );

        // Get confirmation of closure
        switch ( document.getKind( ) )
        {
            case ARCHIVE:
            {
                // Display prompt to save changed document
                ArchiveDocument archiveDocument = (ArchiveDocument)document;
                File file = archiveDocument.getFile( );
                String messageStr = ((file == null) ? UNNAMED_FILE_STR
                                                    : Util.getPathname( file ) + CHANGED_MESSAGE1_STR) +
                                                                                    CHANGED_MESSAGE2_STR;
                String[] optionStrs = Util.getOptionStrings( SAVE_STR, DISCARD_STR );
                int result = JOptionPane.showOptionDialog( mainWindow, messageStr, SAVE_CLOSE_FILE_STR,
                                                           JOptionPane.YES_NO_CANCEL_OPTION,
                                                           JOptionPane.QUESTION_MESSAGE, null, optionStrs,
                                                           optionStrs[0] );

                // Discard changed document
                if ( result == JOptionPane.NO_OPTION )
                    return true;

                // Save changed document
                if ( result == JOptionPane.YES_OPTION )
                {
                    // Choose filename
                    if ( file == null )
                    {
                        file = chooseSaveArchive( file, archiveDocument.getArchiveDirectory( ) );
                        if ( file == null )
                            return false;
                        if ( file.exists( ) )
                        {
                            messageStr = Util.getPathname( file ) + AppConstants.ALREADY_EXISTS_STR;
                            result = JOptionPane.showConfirmDialog( mainWindow, messageStr,
                                                                    SAVE_CLOSE_FILE_STR,
                                                                    JOptionPane.YES_NO_CANCEL_OPTION,
                                                                    JOptionPane.WARNING_MESSAGE );
                            if ( result == JOptionPane.NO_OPTION )
                                return true;
                            if ( result != JOptionPane.YES_OPTION )
                                return false;
                        }
                    }

                    // Write file
                    try
                    {
                        writeDocument( archiveDocument, file );
                        return true;
                    }
                    catch ( AppException e )
                    {
                        showErrorMessage( SAVE_CLOSE_FILE_STR, e );
                    }
                }
                break;
            }

            case TEXT:
            {
                String[] optionStrs = Util.getOptionStrings( AppConstants.CLOSE_STR );
                if ( JOptionPane.showOptionDialog( mainWindow, CLOSE_MESSAGE_STR, CLOSE_TEXT_STR,
                                                   JOptionPane.OK_CANCEL_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE, null, optionStrs,
                                                   optionStrs[0] ) == JOptionPane.OK_OPTION )
                    return true;
                break;
            }
        }

        return false;
    }

    //------------------------------------------------------------------

    private void init( final String[] arguments )
    {
        // Set runtime debug flag
        debug = (System.getProperty( DEBUG_PROPERTY_KEY ) != null);

        // Read build properties
        buildProperties = new ResourceProperties( BUILD_PROPERTIES_PATHNAME, getClass( ) );

        // Initialise instance variables
        persistentKeyList = new KeyList( );
        temporaryKeyList = new KeyList( );
        documentsViews = new ArrayList<>( );
        importQueue = new ImportQueue( );

        // Read configuration
        AppConfig config = AppConfig.getInstance( );
        config.read( );

        // Set UNIX style for pathnames in file exceptions
        ExceptionUtilities.setUnixStyle( config.isShowUnixPathnames( ) );

        // Set text antialiasing
        TextRendering.setAntialiasing( config.getTextAntialiasing( ) );

        // Set look-and-feel
        String lookAndFeelName = config.getLookAndFeel( );
        for ( UIManager.LookAndFeelInfo lookAndFeelInfo : UIManager.getInstalledLookAndFeels( ) )
        {
            if ( lookAndFeelInfo.getName( ).equals( lookAndFeelName ) )
            {
                try
                {
                    UIManager.setLookAndFeel( lookAndFeelInfo.getClassName( ) );
                }
                catch ( Exception e )
                {
                    // ignore
                }
                lookAndFeelName = null;
                break;
            }
        }
        if ( lookAndFeelName != null )
            showWarningMessage( SHORT_NAME + " | " + CONFIG_ERROR_STR,
                                LAF_ERROR1_STR + lookAndFeelName + LAF_ERROR2_STR );

        // Select all text when a text field gains focus
        if ( config.isSelectTextOnFocusGained( ) )
            TextFieldUtilities.selectAllOnFocusGained( );

        // Initialise PRNG and entropy accumulator
        try
        {
            prng = new StandardCsprng( config.getPrngDefaultCipher( ), config.getEntropySourceParams( ),
                                       config.getEntropyTimerDivisor( ) );
        }
        catch ( AppException e )
        {
            showWarningMessage( SHORT_NAME, e );
        }

        // Perform remaining initialisation from event-dispatching thread
        SwingUtilities.invokeLater( new Runnable( )
        {
            @Override
            public void run( )
            {
                // Create main window
                mainWindow = new MainWindow( );

                // Read seed file
                AppConfig config = AppConfig.getInstance( );
                try
                {
                    int[] lengthBuffer = new int[1];
                    File directory = config.getSeedFileDirectory( );
                    if ( directory != null )
                        TaskProgressDialog.showDialog( mainWindow, READ_SEED_FILE_STR,
                                                       new Task.ReadSeedFile( prng, directory,
                                                                              lengthBuffer ) );
                    if ( (lengthBuffer[0] < Fortuna.RESEED_ENTROPY_THRESHOLD) && config.isWarnNotSeeded( ) )
                        showWarningMessage( SHORT_NAME + " | " + READ_SEED_FILE_STR, PRNG_NOT_SEEDED_STR );
                }
                catch ( AppException e )
                {
                    showErrorMessage( SHORT_NAME + " | " + READ_SEED_FILE_STR, e );
                }

                // Read persistent keys
                File keyFile = config.getKeyDatabaseFile( );
                if ( keyFile != null )
                {
                    if ( !keyFile.exists( ) )
                        showWarningMessage( SHORT_NAME + " | " + KEY_DATABASE_STR,
                                            StringUtilities.substitute( NO_KEY_DATABASE1_STR,
                                                                        Util.getPathname( keyFile ) ) );
                    else
                    {
                        try
                        {
                            TaskProgressDialog.showDialog( mainWindow, READ_KEYS_STR,
                                                           new Task.ReadKeyList( persistentKeyList,
                                                                                 keyFile ) );
                        }
                        catch ( TaskCancelledException e )
                        {
                            System.exit( 1 );
                        }
                        catch ( AppException e )
                        {
                            showErrorMessage( SHORT_NAME + " | " + KEY_DATABASE_STR, e );
                        }
                    }
                }

                // Start interval timer
                intervalTimer = new Timer( TIMER_INTERVAL, AppCommand.TIMER_EXPIRED );
                intervalTimer.setRepeats( false );
                intervalTimer.start( );

                // Command-line arguments: process files
                if ( arguments.length > 0 )
                {
                    // Create list of files from command-line arguments
                    File[] files = new File[arguments.length];
                    for ( int i = 0; i < arguments.length; ++i )
                        files[i] = new File( PropertyString.parsePathname( arguments[i] ) );

                    // Process files
                    processFiles( files );

                    // Update title and menus
                    mainWindow.updateTitleAndMenus( );
                }
            }
        } );
    }

    //------------------------------------------------------------------

    private File chooseOpenArchive( )
    {
        JFileChooser fileChooser = FileSelectionKind.OPEN_ARCHIVE.getFileChooser( );
        fileChooser.setSelectedFile( new File( new String( ) ) );
        fileChooser.rescanCurrentDirectory( );
        return ( (fileChooser.showOpenDialog( mainWindow ) == JFileChooser.APPROVE_OPTION)
                                                                            ? fileChooser.getSelectedFile( )
                                                                            : null );
    }

    //------------------------------------------------------------------

    private File chooseSaveArchive( File file,
                                    File directory )
    {
        JFileChooser fileChooser = FileSelectionKind.SAVE_ARCHIVE.getFileChooser( );
        if ( file == null )
        {
            fileChooser.setCurrentDirectory( directory );
            fileChooser.setSelectedFile( new File( new String( ) ) );
        }
        else
            fileChooser.setSelectedFile( file.getAbsoluteFile( ) );
        fileChooser.rescanCurrentDirectory( );
        return ( (fileChooser.showSaveDialog( mainWindow ) == JFileChooser.APPROVE_OPTION)
                                                ? Util.appendSuffix( fileChooser.getSelectedFile( ),
                                                                     FileKind.ARCHIVE.getFilenameSuffix( ) )
                                                : null );
    }

    //------------------------------------------------------------------

    private void updateConfiguration( )
    {
        // Set location of main window
        AppConfig config = AppConfig.getInstance( );
        if ( config.isMainWindowLocation( ) )
        {
            Point location = GuiUtilities.getFrameLocation( mainWindow );
            if ( location != null )
                config.setMainWindowLocation( location );
        }

        // Set size of main window
        if ( config.isMainWindowSize( ) )
        {
            Dimension size = GuiUtilities.getFrameSize( mainWindow );
            if ( size != null )
                config.setMainWindowSize( size );
        }

        // Set file locations
        for ( FileSelectionKind fileSelectionKind : FileSelectionKind.values( ) )
            fileSelectionKind.updateDirectory( );

        // Set split file-part length limits
        config.setSplitFilePartLengthLowerLimit( SplitDialog.getFilePartLengthLowerLimit( ) );
        config.setSplitFilePartLengthUpperLimit( SplitDialog.getFilePartLengthUpperLimit( ) );

        // Write configuration file
        config.write( );
    }

    //------------------------------------------------------------------

    private void processFiles( File[] files )
    {
        // Process files according to filename suffix
        FileOperation.initCounts( );
        int numValidated = 0;
        for ( int i = 0; i < files.length; ++i )
        {
            // Test for file
            File file = files[i];
            if ( !file.isFile( ) )
                continue;

            // Process file
            FileOperation fileOp = null;
            try
            {

                //----  Open file

                String filename = file.getName( );
                if ( filename.endsWith( FileKind.ARCHIVE.getFilenameSuffix( ) ) )
                {
                    fileOp = FileOperation.OPEN;
                    if ( !isDocumentsFull( ) )
                    {
                        if ( !openDocument( file ) )
                            throw new CancelledException( );
                    }
                }


                //----  Decrypt or validate file

                else if ( filename.endsWith( FileKind.ENCRYPTED.getFilenameSuffix( ) ) )
                {
                    switch ( AppConfig.getInstance( ).getEncryptedFileDragAndDropAction( ) )
                    {
                        case DECRYPT:
                        {
                            fileOp = FileOperation.DECRYPT;
                            File inFile = file;
                            File outFile = null;

                            // Select output file
                            if ( ActionSource.DRAG_AND_DROP.isSelectEncryptDecryptOutputFile( ) )
                            {
                                DecryptDialog.Result result = DecryptDialog.showDialog( mainWindow, file );
                                if ( result == null )
                                    throw new CancelledException( );
                                inFile = result.inFile;
                                outFile = result.outFile;
                            }

                            // Decrypt file
                            if ( !decryptFile( inFile, outFile ) )
                                throw new CancelledException( );
                            break;
                        }

                        case VALIDATE:
                        {
                            fileOp = FileOperation.VALIDATE;
                            ++numValidated;
                            validateFile( file );
                            break;
                        }
                    }
                }


                //----  Encrypt file

                else
                {
                    fileOp = FileOperation.ENCRYPT;
                    File inFile = file;
                    File outFile = null;

                    // Select output file
                    if ( ActionSource.DRAG_AND_DROP.isSelectEncryptDecryptOutputFile( ) )
                    {
                        EncryptDialog.Result result = EncryptDialog.showDialog( mainWindow, file );
                        if ( result == null )
                            throw new CancelledException( );
                        inFile = result.inFile;
                        outFile = result.outFile;
                    }

                    // Encrypt file
                    if ( !encryptFile( inFile, outFile ) )
                        throw new CancelledException( );
                }

                // Test for cancelled task
                if ( Task.isCancelled( ) )
                    throw new CancelledException( );

                // Update operation count
                if ( FileOperation.COUNTABLE_OPERATIONS.contains( fileOp ) )
                {
                    FileOperation.PROCESS.incrementCount( );
                    fileOp.incrementCount( );
                }
            }
            catch ( CancelledException e )
            {
                String[] optionStrs = Util.getOptionStrings( AppConstants.CONTINUE_STR );
                if ( (i < files.length - 1) &&
                     (JOptionPane.showOptionDialog( mainWindow, REMAINING_IMPORTS_STR, fileOp.titleStr,
                                                    JOptionPane.OK_CANCEL_OPTION,
                                                    JOptionPane.QUESTION_MESSAGE, null, optionStrs,
                                                    optionStrs[1] ) != JOptionPane.OK_OPTION) )
                    break;
            }
            catch ( AppException e )
            {
                if ( i < files.length - 1 )
                {
                    String[] optionStrs = Util.getOptionStrings( AppConstants.CONTINUE_STR );
                    if ( JOptionPane.showOptionDialog( mainWindow, e, fileOp.titleStr,
                                                       JOptionPane.OK_CANCEL_OPTION,
                                                       JOptionPane.ERROR_MESSAGE, null, optionStrs,
                                                       optionStrs[1] ) != JOptionPane.OK_OPTION )
                        break;
                }
                else
                    showErrorMessage( fileOp.titleStr, e );
            }
        }

        // Report results
        if ( FileOperation.PROCESS.isOperation( ) )
        {
            StringBuilder buffer = new StringBuilder( 128 );
            for ( FileOperation fileOp : FileOperation.operationCounts.keySet( ) )
            {
                if ( fileOp.isOperation( ) )
                {
                    if ( buffer.length( ) > 0 )
                        buffer.append( '\n' );
                    buffer.append( fileOp.getCountString( ) );
                }
            }
            if ( (numValidated > 0) &&
                 (numValidated == FileOperation.operationCounts.get( FileOperation.VALIDATE )) )
            {
                buffer.append( '\n' );
                buffer.append( ALL_VALID_STR );
            }
            showInfoMessage( PROCESS_FILES_STR, buffer.toString( ) );
        }
    }

    //------------------------------------------------------------------

    private boolean encryptFile( File inFile,
                                 File outFile )
        throws AppException
    {
        // Test for input file
        if ( !inFile.exists( ) )
            throw new FileException( ErrorId.FILE_DOES_NOT_EXIST, inFile );
        if ( !inFile.isFile( ) )
            throw new FileException( ErrorId.NOT_A_FILE, inFile );

        // Generate name of output file
        if ( outFile == null )
            outFile = new File( inFile.getAbsoluteFile( ).getParentFile( ),
                                inFile.getName( ) + FileKind.ENCRYPTED.getFilenameSuffix( ) );

        if ( confirmWriteFile( outFile, ENCRYPT_FILE_STR ) )
        {
            // Get key
            KeyList.Key key = getKey( ENCRYPT_FILE_STR, inFile.getName( ) );
            if ( (key != null) && confirmUseTemporaryKey( key ) )
            {
                // Check cipher
                FortunaCipher cipher = Util.getCipher( key );
                key.checkAllowedCipher( cipher );

                // Encrypt file
                TaskProgressDialog.showDialog( mainWindow, ENCRYPT_FILE_STR,
                                               new Task.Encrypt( inFile, outFile, Util.getCipher( key ),
                                                                 key ) );
                return true;
            }
        }
        return false;
    }

    //------------------------------------------------------------------

    private boolean decryptFile( File inFile,
                                 File outFile )
        throws AppException
    {
        // Test for input file
        if ( !inFile.exists( ) )
            throw new FileException( ErrorId.FILE_DOES_NOT_EXIST, inFile );
        if ( !inFile.isFile( ) )
            throw new FileException( ErrorId.NOT_A_FILE, inFile );

        // Generate name of output file
        if ( outFile == null )
            outFile = Util.getPlaintextFile( inFile.getAbsoluteFile( ).getParentFile( ),
                                             inFile.getName( ) );

        // Decrypt file
        if ( confirmWriteFile( outFile, DECRYPT_FILE_STR ) )
        {
            // Get key
            KeyList.Key key = getKey( DECRYPT_FILE_STR, inFile.getName( ) );

            // Decrypt file
            if ( (key != null) && confirmUseTemporaryKey( key ) )
            {
                // Decrypt file
                TaskProgressDialog.showDialog( mainWindow, DECRYPT_FILE_STR,
                                               new Task.Decrypt( inFile, outFile, key ) );
                return true;
            }
        }
        return false;
    }

    //------------------------------------------------------------------

    private void validateFile( File file )
        throws AppException
    {
        // Test for input file
        if ( !file.exists( ) )
            throw new FileException( ErrorId.FILE_DOES_NOT_EXIST, file );
        if ( !file.isFile( ) )
            throw new FileException( ErrorId.NOT_A_FILE, file );

        // Get key
        KeyList.Key key = getKey( VALIDATE_FILE_STR, file.getName( ) );

        // Validate file
        if ( (key != null) && confirmUseTemporaryKey( key ) )
            TaskProgressDialog.showDialog( mainWindow, VALIDATE_FILE_STR, new Task.Validate( file, key ) );
    }

    //------------------------------------------------------------------

    private void doImport( )
    {
        while ( !importQueue.isEmpty( ) )
        {
            ImportQueue.Element element = importQueue.remove( );
            if ( element.fileImporter == null )
                importFiles( element.files );
            else
                element.fileImporter.importFiles( element.files );
        }
    }

    //------------------------------------------------------------------

    private void onTimerExpired( )
        throws AppException
    {
        // Update PRNG reseed status
        mainWindow.updatePrngCanReseed( );

        // Check for modified file
        ArchiveDocument document = getArchiveDocument( );
        if ( (document != null) && !document.isExecutingCommand( ) )
        {
            File file = document.getFile( );
            long timestamp = document.getTimestamp( );
            if ( (file != null) && (timestamp != 0) )
            {
                long currentTimestamp = file.lastModified( );
                if ( (currentTimestamp != 0) && (currentTimestamp != timestamp) )
                {
                    String messageStr = Util.getPathname( file ) + MODIFIED_MESSAGE_STR;
                    if ( JOptionPane.showConfirmDialog( mainWindow, messageStr, MODIFIED_FILE_STR,
                                                        JOptionPane.YES_NO_OPTION,
                                                        JOptionPane.QUESTION_MESSAGE ) ==
                                                                                    JOptionPane.YES_OPTION )
                    {
                        revertDocument( file );
                        mainWindow.updateTitleAndMenus( );
                    }
                    else
                        document.setTimestamp( currentTimestamp );
                }
            }
        }

        // Restart timer
        intervalTimer.start( );
    }

    //------------------------------------------------------------------

    private void onCreateFile( )
        throws AppException
    {
        if ( !isDocumentsFull( ) )
            addDocument( new ArchiveDocument( ++newArchiveDocumentIndex ) );
    }

    //------------------------------------------------------------------

    private void onOpenFile( )
        throws AppException
    {
        if ( !isDocumentsFull( ) )
        {
            File file = chooseOpenArchive( );
            if ( file != null )
                openDocument( file );
        }
    }

    //------------------------------------------------------------------

    private void onRevertFile( )
        throws AppException
    {
        ArchiveDocument document = getArchiveDocument( );
        if ( (document != null) && document.isChanged( ) )
        {
            File file = document.getFile( );
            if ( file != null )
            {
                String messageStr = Util.getPathname( file ) + REVERT_MESSAGE_STR;
                String[] optionStrs = Util.getOptionStrings( REVERT_STR );
                if ( JOptionPane.showOptionDialog( mainWindow, messageStr, REVERT_FILE_STR,
                                                   JOptionPane.OK_CANCEL_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE, null, optionStrs,
                                                   optionStrs[1] ) == JOptionPane.OK_OPTION )
                    revertDocument( file );
            }
        }
    }

    //------------------------------------------------------------------

    private void onCloseFile( )
    {
        if ( hasDocuments( ) )
            closeDocument( mainWindow.getTabIndex( ) );
    }

    //------------------------------------------------------------------

    private void onCloseAllFiles( )
    {
        while ( hasDocuments( ) )
        {
            int index = getNumDocuments( ) - 1;
            if ( !confirmCloseDocument( index ) )
                break;
            removeDocument( index );
        }
    }

    //------------------------------------------------------------------

    private void onSaveFile( )
        throws AppException
    {
        ArchiveDocument document = getArchiveDocument( );
        if ( (document != null) && document.isChanged( ) )
        {
            File file = document.getFile( );
            if ( file == null )
                onSaveFileAs( );
            else
                writeDocument( document, file );
        }
    }

    //------------------------------------------------------------------

    private void onSaveFileAs( )
        throws AppException
    {
        ArchiveDocument document = getArchiveDocument( );
        if ( document != null )
        {
            File file = chooseSaveArchive( document.getFile( ), document.getArchiveDirectory( ) );
            if ( (file != null) && confirmWriteFile( file, SAVE_FILE_AS_STR ) )
                writeDocument( document, file );
        }
    }

    //------------------------------------------------------------------

    private void onEncryptFile( )
        throws AppException
    {
        // Get file
        File inFile = null;
        File outFile = null;

        // If configured to select output file, select input and output files ...
        if ( ActionSource.MENU_COMMAND.isSelectEncryptDecryptOutputFile( ) )
        {
            EncryptDialog.Result result = EncryptDialog.showDialog( mainWindow, null );
            if ( result != null )
            {
                inFile = result.inFile;
                outFile = result.outFile;
            }
        }

        // ... otherwise, select input file
        else
        {
            JFileChooser fileChooser = FileSelectionKind.ENCRYPT.getFileChooser( );
            fileChooser.setSelectedFile( new File( new String( ) ) );
            fileChooser.rescanCurrentDirectory( );
            if ( fileChooser.showOpenDialog( mainWindow ) == JFileChooser.APPROVE_OPTION )
                inFile = fileChooser.getSelectedFile( );
        }

        // Encrypt file
        if ( inFile != null )
            encryptFile( inFile, outFile );
    }

    //------------------------------------------------------------------

    private void onDecryptFile( )
        throws AppException
    {
        File inFile = null;
        File outFile = null;

        // If configured to select output file, select input and output files ...
        if ( ActionSource.MENU_COMMAND.isSelectEncryptDecryptOutputFile( ) )
        {
            DecryptDialog.Result result = DecryptDialog.showDialog( mainWindow, null );
            if ( result != null )
            {
                inFile = result.inFile;
                outFile = result.outFile;
            }
        }

        // ... otherwise, select input file
        else
        {
            JFileChooser fileChooser = FileSelectionKind.DECRYPT.getFileChooser( );
            fileChooser.setSelectedFile( new File( new String( ) ) );
            fileChooser.rescanCurrentDirectory( );
            if ( fileChooser.showOpenDialog( mainWindow ) == JFileChooser.APPROVE_OPTION )
                inFile = fileChooser.getSelectedFile( );
        }

        // Decrypt file
        if ( inFile != null )
            decryptFile( inFile, outFile );
    }

    //------------------------------------------------------------------

    private void onValidateFile( )
        throws AppException
    {
        // Select input file
        JFileChooser fileChooser = FileSelectionKind.VALIDATE.getFileChooser( );
        fileChooser.setSelectedFile( new File( new String( ) ) );
        fileChooser.rescanCurrentDirectory( );

        // Validate file
        if ( fileChooser.showOpenDialog( mainWindow ) == JFileChooser.APPROVE_OPTION )
        {
            // Process file
            File file = fileChooser.getSelectedFile( );
            validateFile( file );

            // Report success
            showInfoMessage( VALIDATE_FILE_STR, Util.getPathname( file ) + "\n" + FILE_VALID_STR );
        }
    }

    //------------------------------------------------------------------

    private void onConcealFile( )
        throws AppException
    {
        // Get parameters of concealment operation
        ConcealDialog.Result result = ConcealDialog.showDialog( mainWindow, true );

        // Conceal file
        if ( result != null )
        {
            // Get key
            KeyList.Key key = getKey( CONCEAL_FILE_STR, result.inFile.getName( ) );

            // Conceal file
            if ( (key != null) && confirmUseTemporaryKey( key ) )
            {
                // Check cipher
                key.checkAllowedCipher( Util.getCipher( key ) );

                // Generate name of output file
                File outFile = (result.outFile == null)
                                    ? new File( result.inFile.getAbsoluteFile( ).getParentFile( ),
                                                result.inFile.getName( ) + AppConstants.PNG_FILE_SUFFIX )
                                    : result.outFile;

                // Conceal file
                if ( confirmWriteFile( outFile, CONCEAL_FILE_STR ) )
                    TaskProgressDialog.showDialog2( mainWindow, CONCEAL_FILE_STR,
                                                    new Task.ConcealFile( result.inFile, result.carrierFile,
                                                                          outFile, result.maxNumBits,
                                                                          result.setTimestamp,
                                                                          result.addRandomBits, key ) );
            }
        }
    }

    //------------------------------------------------------------------

    private void onRecoverFile( )
        throws AppException
    {
        // Get parameters of recovery operation
        RecoverDialog.Result result = RecoverDialog.showDialog( mainWindow, true );

        // Recover file
        if ( (result != null) && confirmWriteFile( result.outFile, RECOVER_FILE_STR ) )
        {
            // Get key
            KeyList.Key key = getKey( RECOVER_FILE_STR, result.inFile.getName( ) );

            // Recover file
            if ( key != null )
                TaskProgressDialog.showDialog2( mainWindow, RECOVER_FILE_STR,
                                                new Task.RecoverFile( result.inFile, result.outFile,
                                                                      key ) );
        }
    }

    //------------------------------------------------------------------

    private void onSplitFile( )
        throws AppException
    {
        // Get parameters of split operation
        SplitDialog.Result result = SplitDialog.showDialog( mainWindow );

        // Split file
        if ( result != null )
        {
            // Get key
            KeyList.Key key = getKey( SPLIT_FILE_STR, result.inFile.getName( ) );

            // Split file
            if ( (key != null) && confirmUseTemporaryKey( key ) )
            {
                // Check cipher
                key.checkAllowedCipher( Util.getCipher( key ) );

                // Create output directory
                File outDirectory = (result.outDirectory == null)
                                                        ? result.inFile.getAbsoluteFile( ).getParentFile( )
                                                        : result.outDirectory;
                if ( !outDirectory.exists( ) && !outDirectory.mkdirs( ) )
                    throw new FileException( ErrorId.FAILED_TO_CREATE_OUTPUT_DIRECTORY, outDirectory );

                // Delete existing file parts from output directory
                List<File> fileParts = FileSplitter.getFileParts( outDirectory );
                if ( !fileParts.isEmpty( ) )
                {
                    int numFiles = fileParts.size( );
                    int index = (numFiles == 1) ? 0 : 1;
                    String messageStr = StringUtilities.substitute( FILE_PARTS_STR,
                                                                    Integer.toString( numFiles ),
                                                                    FILE_PARTS_STRS[0][index],
                                                                    FILE_PARTS_STRS[1][index],
                                                                    FILE_PARTS_STRS[2][index] );
                    String[] optionStrs = Util.getOptionStrings( AppConstants.DELETE_STR, KEEP_STR );
                    int result1 = JOptionPane.showOptionDialog( mainWindow, messageStr, SPLIT_FILE_STR,
                                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                                JOptionPane.QUESTION_MESSAGE, null,
                                                                optionStrs, optionStrs[2] );
                    if ( result1 == JOptionPane.YES_OPTION )
                    {
                        for ( File filePart : fileParts )
                        {
                            if ( !filePart.getAbsoluteFile( ).delete( ) )
                            {
                                AppException e = new FileException( ErrorId.FAILED_TO_DELETE_FILE_PART,
                                                                    filePart );
                                optionStrs = Util.getOptionStrings( AppConstants.CONTINUE_STR );
                                if ( JOptionPane.showOptionDialog( mainWindow, e, SPLIT_FILE_STR,
                                                                   JOptionPane.OK_CANCEL_OPTION,
                                                                   JOptionPane.QUESTION_MESSAGE, null,
                                                                   optionStrs, optionStrs[1] ) !=
                                                                                    JOptionPane.OK_OPTION )
                                    return;
                            }
                        }
                    }
                    else if ( result1 != JOptionPane.NO_OPTION )
                        return;
                }

                // Split file
                TaskProgressDialog.showDialog2( mainWindow, SPLIT_FILE_STR,
                                                new Task.SplitFile( result.inFile, outDirectory,
                                                                    result.filePartLengthLowerLimit,
                                                                    result.filePartLengthUpperLimit,
                                                                    key ) );
            }
        }
    }

    //------------------------------------------------------------------

    private void onJoinFiles( )
        throws AppException
    {
        // Get parameters of join operation
        JoinDialog.Result result = JoinDialog.showDialog( mainWindow );
        if ( result != null )
        {
            // Get key
            KeyList.Key key = getKey( JOIN_FILES_STR, result.inDirectory.getName( ) );

            // Join input files
            if ( key != null )
            {
                // Prompt to replace existing output file
                if ( confirmWriteFile( result.outFile, JOIN_FILES_STR ) )
                {
                    // Create output directory
                    File outDirectory = result.outFile.getAbsoluteFile( ).getParentFile( );
                    if ( !outDirectory.exists( ) && !outDirectory.mkdirs( ) )
                        throw new FileException( ErrorId.FAILED_TO_CREATE_OUTPUT_DIRECTORY, outDirectory );

                    // Join files
                    TaskProgressDialog.showDialog2( mainWindow, JOIN_FILES_STR,
                                                    new Task.JoinFiles( result.inDirectory, result.outFile,
                                                                        key ) );
                }
            }
        }
    }

    //------------------------------------------------------------------

    private void onEraseFiles( )
        throws AppException
    {
        List<String> pathnames = EraseDialog.showDialog( mainWindow );
        if ( pathnames != null )
            TaskProgressDialog.showDialog2( mainWindow, ERASE_FILES_STR, new Task.EraseFiles( pathnames ) );
    }

    //------------------------------------------------------------------

    private void onExit( )
    {
        if ( !exiting )
        {
            try
            {
                // Prevent re-entry to this method
                exiting = true;

                // Close all open documents
                while ( hasDocuments( ) )
                {
                    int index = getNumDocuments( ) - 1;
                    if ( !confirmCloseDocument( index ) )
                        return;
                    removeDocument( index );
                }

                // Clear clipboard
                AppConfig config = AppConfig.getInstance( );
                if ( config.isClearClipboardOnExit( ) )
                {
                    try
                    {
                        Util.putClipboardText( new String( ) );
                    }
                    catch ( AppException e )
                    {
                        showWarningMessage( SHORT_NAME, ErrorId.FAILED_TO_CLEAR_CLIPBOARD );
                    }
                }

                // Write seed file
                try
                {
                    File directory = config.getSeedFileDirectory( );
                    if ( directory != null )
                    {
                        directory.mkdirs( );
                        TaskProgressDialog.showDialog( mainWindow, WRITE_SEED_FILE_STR,
                                                       new Task.WriteSeedFile( prng, directory ) );
                    }
                }
                catch ( AppException e )
                {
                    showWarningMessage( SHORT_NAME + " | " + WRITE_SEED_FILE_STR, e );
                }

                // Write key list
                File keyFile = config.getKeyDatabaseFile( );
                if ( persistentKeyList.isChanged( ) || ((keyFile != null) && !keyFile.exists( )) )
                {
                    if ( keyFile == null )
                    {
                        String[] optionStrs = Util.getOptionStrings( EXIT_STR );
                        if ( JOptionPane.showOptionDialog( mainWindow, NO_KEY_DATABASE2_STR,
                                                           SHORT_NAME + " | " + KEY_DATABASE_STR,
                                                           JOptionPane.OK_CANCEL_OPTION,
                                                           JOptionPane.WARNING_MESSAGE, null, optionStrs,
                                                           optionStrs[1] ) != JOptionPane.OK_OPTION )
                            return;
                    }
                    else
                    {
                        try
                        {
                            TaskProgressDialog.showDialog( mainWindow, WRITE_KEYS_STR,
                                                           new Task.WriteKeyList( persistentKeyList,
                                                                                  keyFile ) );
                        }
                        catch ( AppException e )
                        {
                            String[] optionStrs = Util.getOptionStrings( EXIT_STR );
                            if ( JOptionPane.showOptionDialog( mainWindow, e,
                                                               SHORT_NAME + " | " + KEY_DATABASE_STR,
                                                               JOptionPane.OK_CANCEL_OPTION,
                                                               JOptionPane.WARNING_MESSAGE, null,
                                                               optionStrs, optionStrs[1] ) !=
                                                                                    JOptionPane.OK_OPTION )
                                return;
                        }
                    }
                }

                // Update configuration
                updateConfiguration( );

                // Destroy main window
                mainWindow.setVisible( false );
                mainWindow.dispose( );

                // Exit application
                System.exit( 0 );
            }
            finally
            {
                exiting = false;
            }
        }
    }

    //------------------------------------------------------------------

    private void onCreateText( )
    {
        if ( !isDocumentsFull( ) )
            addDocument( new TextDocument( ++newTextDocumentIndex ) );
    }

    //------------------------------------------------------------------

    private void onRecoverText( )
        throws AppException
    {
        if ( !isDocumentsFull( ) )
        {
            // Get parameters of recovery operation
            RecoverDialog.Result result = RecoverDialog.showDialog( mainWindow, false );

            // Recover text
            if ( result != null )
            {
                // Get key
                KeyList.Key key = getKey( RECOVER_TEXT_STR, result.inFile.getName( ) );

                // Recover text
                if ( key != null )
                {
                    TextDocument document = new TextDocument( ++newTextDocumentIndex );
                    addDocument( document );
                    TaskProgressDialog.showDialog2( mainWindow, RECOVER_TEXT_STR,
                                                    new Task.RecoverText( document, result.inFile, key ) );
                }
            }
        }
    }

    //------------------------------------------------------------------

    private void onSetGlobalKey( )
    {
        KeyList.Key key = selectKey( SET_GLOBAL_KEY_STR );
        if ( key != null )
        {
            globalKey = key;
            mainWindow.updateStatus( );
        }
    }

    //------------------------------------------------------------------

    private void onClearGlobalKey( )
    {
        String[] optionStrs = Util.getOptionStrings( AppConstants.CLEAR_STR );
        if ( JOptionPane.showOptionDialog( mainWindow, CLEAR_KEY_MESSAGE_STR, CLEAR_KEY_STR,
                                           JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                           optionStrs, optionStrs[1] ) == JOptionPane.OK_OPTION )
        {
            globalKey = null;
            mainWindow.updateStatus( );
        }
    }

    //------------------------------------------------------------------

    private void onToggleAutoUseGlobalKey( )
    {
        AppConfig.getInstance( ).setAutoUseGlobalKey( !AppConfig.getInstance( ).isAutoUseGlobalKey( ) );
    }

    //------------------------------------------------------------------

    private void onEditKeys( )
    {
        List<KeyList.Key> keys = new ArrayList<>( );
        keys.addAll( temporaryKeyList.getKeys( ) );
        keys.addAll( persistentKeyList.getKeys( ) );
        KeyEditorDialog dialog = KeyEditorDialog.showDialog( mainWindow, keys );
        if ( dialog.hasEdits( ) )
        {
            // Update lists of temporary and persistent keys
            temporaryKeyList.setKeys( dialog.getTemporaryKeys( ) );
            persistentKeyList.setKeys( dialog.getPersistentKeys( ) );

            // Replace global key and document keys
            Map<KeyList.Key, KeyList.Key> keyMap = dialog.getKeyMap( );
            for ( KeyList.Key target : keyMap.keySet( ) )
            {
                // Get replacement for target key
                KeyList.Key replacement = keyMap.get( target );

                // Replace global key
                if ( globalKey == target )
                    globalKey = replacement;

                // Replace document keys
                for ( int i = 0; i < getNumDocuments( ); ++i )
                    getDocument( i ).replaceKey( target, replacement );

                // Update status in main window
                mainWindow.updateStatus( );
            }
        }
    }

    //------------------------------------------------------------------

    private void onShowEntropyMetrics( )
    {
        EntropyMetricsDialog.showDialog( mainWindow, prng.getPrng( ), prng.getEntropyAccumulator( ) );
    }

    //------------------------------------------------------------------

    private void onGenerateGarbage( )
        throws AppException
    {
        GarbageGeneratorDialog.Result result = GarbageGeneratorDialog.showDialog( mainWindow,
                                                                                  !isDocumentsFull( ) );
        if ( result != null )
        {
            try
            {
                // File
                if ( result.file != null )
                    BinaryFile.write( result.file, new RandomDataInputStream( result.length, true ) );

                // Image file
                else if ( result.imageFile != null )
                {
                    RandomDataInputStream inStream = new RandomDataInputStream( result.length, false );
                    new FileConcealer( ).conceal( inStream, getCarrierImageSource( ), null,
                                                  result.imageFile, (int)inStream.getLength( ),
                                                  getLengthEncoder( ), CarrierImage.NUM_CARRIER_BITS,
                                                  null );
                }

                // Text
                else
                {
                    RandomDataInputStream inStream = new RandomDataInputStream( result.length, false );
                    int length = (int)inStream.getLength( );
                    byte[] buffer = new byte[length];
                    int blockLength = 0;
                    for ( int offset = 0; offset < length; offset += blockLength )
                    {
                        blockLength = Math.min( buffer.length - offset,
                                                RandomDataInputStream.BLOCK_LENGTH );
                        blockLength = inStream.read( buffer, offset, blockLength );
                    }
                    String text = TextDocument.bytesToBase64( buffer );
                    if ( AppConfig.getInstance( ).isWrapCiphertextInXml( ) )
                        text = TextDocument.wrapTextInXml( text, Util.getCipher( ) );
                    TextDocument document = new TextDocument( ++newTextDocumentIndex );
                    addDocument( document );
                    document.setText( text, true );
                }
            }
            catch ( OutOfMemoryError e )
            {
                throw new AppException( ErrorId.NOT_ENOUGH_MEMORY );
            }
        }
    }

    //------------------------------------------------------------------

    private void onToggleShowFullPathnames( )
    {
        AppConfig.getInstance( ).setShowFullPathnames( !AppConfig.getInstance( ).isShowFullPathnames( ) );
    }

    //------------------------------------------------------------------

    private void onManageFileAssociations( )
        throws AppException
    {
        FileAssociationDialog.Result result = FileAssociationDialog.showDialog( mainWindow );
        if ( result != null )
        {
            FileAssociations fileAssoc = new FileAssociations( );
            for ( FileKind fileKind : FileKind.values( ) )
                fileKind.addFileAssocParams( fileAssoc );
            TextOutputTaskDialog.showDialog( mainWindow, FILE_ASSOCIATIONS_STR,
                                             new Task.SetFileAssociations( fileAssoc,
                                                                           result.javaLauncherPathname,
                                                                           result.jarPathname,
                                                                           result.iconPathname,
                                                                           ASSOC_SCRIPT_DIR_PREFIX,
                                                                           ASSOC_SCRIPT_FILENAME,
                                                                           result.removeEntries,
                                                                           result.scriptLifeCycle ) );
        }
    }

    //------------------------------------------------------------------

    private void onEditPreferences( )
    {
        if ( PreferencesDialog.showDialog( mainWindow ) )
        {
            AppConfig config = AppConfig.getInstance( );
            ExceptionUtilities.setUnixStyle( config.isShowUnixPathnames( ) );
            prng.getEntropyAccumulator( ).setTimerDivisor( config.getEntropyTimerDivisor( ) );
            prng.getEntropyAccumulator( ).setSources( config.getEntropySourceParams( ) );
        }
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

    private static  App     instance;
    private static  boolean debug;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private ResourceProperties  buildProperties;
    private MainWindow          mainWindow;
    private Timer               intervalTimer;
    private KeyList             persistentKeyList;
    private KeyList             temporaryKeyList;
    private KeyList.Key         globalKey;
    private List<DocumentView>  documentsViews;
    private ImportQueue         importQueue;
    private StandardCsprng      prng;
    private int                 newArchiveDocumentIndex;
    private int                 newTextDocumentIndex;
    private boolean             exiting;

}

//----------------------------------------------------------------------
