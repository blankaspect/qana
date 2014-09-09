/*====================================================================*\

EntropyMetricsDialog.java

Entropy metrics dialog box class.

\*====================================================================*/


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import uk.org.blankaspect.crypto.EntropyAccumulator;
import uk.org.blankaspect.crypto.Fortuna;

import uk.org.blankaspect.gui.Colours;
import uk.org.blankaspect.gui.FButton;
import uk.org.blankaspect.gui.FTabbedPane;
import uk.org.blankaspect.gui.GuiUtilities;
import uk.org.blankaspect.gui.TextRendering;
import uk.org.blankaspect.gui.TitledBorder;

import uk.org.blankaspect.util.KeyAction;
import uk.org.blankaspect.util.NumberUtilities;

//----------------------------------------------------------------------


// ENTROPY METRICS DIALOG BOX CLASS


class EntropyMetricsDialog
    extends JDialog
    implements ActionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    private static final    Color   BORDER_COLOUR   = Colours.LINE_BORDER;

    private static final    String  TITLE_STR           = "Entropy metrics";
    private static final    String  ENTROPY_SOURCES_STR = "Entropy sources";
    private static final    String  ENTROPY_POOLS_STR   = "Entropy pools";
    private static final    String  UPDATE_STR          = "Update";
    private static final    String  CLEAR_STR           = "Clear";

    private static final    String  CLEAR_METRICS_STR           = "Clear entropy metrics";
    private static final    String  CLEAR_METRICS_MESSAGE_STR   = "Do you want to clear the entropy " +
                                                                    "metrics?";

    // Commands
    private interface Command
    {
        String  UPDATE  = "update";
        String  CLEAR   = "clear";
        String  CLOSE   = "close";
    }

    private static final    KeyAction.KeyCommandPair[]  KEY_COMMANDS    =
    {
        new KeyAction.KeyCommandPair( KeyStroke.getKeyStroke( KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK ),
                                      Command.UPDATE ),
        new KeyAction.KeyCommandPair( KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK ),
                                      Command.CLEAR ),
        new KeyAction.KeyCommandPair( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ),
                                      Command.CLOSE )
    };

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


    // ENTROPY-SOURCE 1-BIT PANEL CLASS


    private static class OneBitPanel
        extends JComponent
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        private static final    int NUM_FREQUENCIES = AppConfig.ENTROPY_SOURCE_MAX_NUM_BITS;

        private static final    int TOP_MARGIN      = 4;
        private static final    int BOTTOM_MARGIN   = 2;
        private static final    int LEFT_MARGIN     = 4;
        private static final    int RIGHT_MARGIN    = LEFT_MARGIN;

        private static final    int DIVISION_HEIGHT     = 50;
        private static final    int INDEX_MARKER_HEIGHT = 3;

        private static final    int NUM_Y_DIVISIONS     = 4;
        private static final    int PLOT_HEIGHT         = NUM_Y_DIVISIONS * DIVISION_HEIGHT + 1;
        private static final    int HALF_PLOT_HEIGHT    = NUM_Y_DIVISIONS / 2 * DIVISION_HEIGHT;

        private static final    int BAR_GAP = 4;

        private static final    Color   BACKGROUND_COLOUR       = Colours.BACKGROUND;
        private static final    Color   INDEX_BACKGROUND_COLOUR = new Color( 224, 236, 224 );
        private static final    Color   GRID_COLOUR             = new Color( 216, 216, 210 );
        private static final    Color   INDEX_COLOUR            = Colours.FOREGROUND;
        private static final    Color   MARKER_COLOUR1          = Color.BLACK;
        private static final    Color   BAR_COLOUR0             = Color.DARK_GRAY;
        private static final    Color   BAR_COLOUR1             = new Color( 0, 64, 192 );
        private static final    Color   BAR_COLOUR2             = new Color( 192, 64, 0 );

        private static final    String  BIT_STR         = "Bit ";
        private static final    String  PROTOTYPE_STR   = "00";

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private OneBitPanel( int bitMask )
        {
            // Set font
            AppFont.MAIN.apply( this );

            // Initialise instance variables
            bitIndices = getBitIndices( bitMask );
            FontMetrics fontMetrics = getFontMetrics( getFont( ) );
            barWidth = fontMetrics.stringWidth( PROTOTYPE_STR );
            height = TOP_MARGIN + PLOT_HEIGHT + INDEX_MARKER_HEIGHT + fontMetrics.getAscent( ) +
                                                                fontMetrics.getDescent( ) + BOTTOM_MARGIN;

            // Set component attributes
            setOpaque( true );
            setFocusable( false );
            setBorder( BorderFactory.createLineBorder( BORDER_COLOUR ) );
            setToolTipText( new String( ) );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : overriding methods
    ////////////////////////////////////////////////////////////////////

        @Override
        public String getToolTipText( MouseEvent event )
        {
            String text = null;
            int barInterval = barWidth + BAR_GAP;
            int plotWidth = LEFT_MARGIN + getPlotWidth( ) + RIGHT_MARGIN;
            if ( frequencyStrs != null )
            {
                int y = event.getY( ) - TOP_MARGIN;
                if ( (y >= 0) && (y < PLOT_HEIGHT) )
                {
                    int x = event.getX( ) - LEFT_MARGIN;
                    if ( (x >= 0) && (x < plotWidth) && (x % barInterval < barWidth) )
                    {
                        int index = x / barInterval;
                        if ( (index < frequencyStrs.length) && (frequencyStrs[index] != null) )
                        {
                            StringBuilder buffer = new StringBuilder( 32 );
                            buffer.append( BIT_STR );
                            buffer.append( bitIndices.get( index ) );
                            buffer.append( " : " );
                            buffer.append( frequencyStrs[index] );
                            text = buffer.toString( );
                        }
                    }
                }
            }
            return text;
        }

        //--------------------------------------------------------------

        @Override
        public Dimension getPreferredSize( )
        {
            return new Dimension( LEFT_MARGIN + getPlotWidth( ) + RIGHT_MARGIN, height );
        }

        //--------------------------------------------------------------

        @Override
        protected void paintComponent( Graphics gr )
        {
            // Create copy of graphics context
            gr = gr.create( );

            // Draw background
            Rectangle rect = gr.getClipBounds( );
            gr.setColor( BACKGROUND_COLOUR );
            gr.fillRect( rect.x, rect.y, rect.width, rect.height );

            int y = TOP_MARGIN + PLOT_HEIGHT;
            gr.setColor( INDEX_BACKGROUND_COLOUR );
            gr.fillRect( rect.x, y, rect.width, getHeight( ) - y );

            // Draw horizontal grid lines
            y = TOP_MARGIN;
            gr.setColor( GRID_COLOUR );
            for ( int i = 0; i <= NUM_Y_DIVISIONS; ++i )
            {
                gr.drawLine( 0, y, getWidth( ) - 1, y );
                y += DIVISION_HEIGHT;
            }

            // Draw frequency bars
            int barInterval = barWidth + BAR_GAP;
            if ( frequencyHeights != null )
            {
                int x = LEFT_MARGIN;
                int midY = TOP_MARGIN + HALF_PLOT_HEIGHT;
                for ( int i = 0; i < frequencyHeights.length; ++i )
                {
                    int barHeight = frequencyHeights[i];
                    int y1 = (barHeight < 0) ? midY : midY - barHeight;
                    int y2 = (barHeight < 0) ? midY - barHeight : midY;
                    gr.setColor( (barHeight == 0) ? BAR_COLOUR0
                                                  : (barHeight < 0) ? BAR_COLOUR1 : BAR_COLOUR2 );
                    gr.fillRect( x, y1, barWidth, y2 - y1 + 1 );
                    x += barInterval;
                }
            }

            // Set rendering hints for text antialiasing and fractional metrics
            TextRendering.setHints( (Graphics2D)gr );

            // Draw index markers and text
            FontMetrics fontMetrics = gr.getFontMetrics( );
            int x = LEFT_MARGIN + barWidth / 2;
            y = TOP_MARGIN + PLOT_HEIGHT;
            for ( int i = 0; i < NUM_FREQUENCIES; ++i )
            {
                // Draw index marker
                gr.setColor( MARKER_COLOUR1 );
                gr.drawLine( x, y, x, y + INDEX_MARKER_HEIGHT - 1 );

                // Draw text
                if ( i < bitIndices.size( ) )
                {
                    String str = Integer.toString( bitIndices.get( i ) );
                    int strWidth = fontMetrics.stringWidth( str );
                    gr.setColor( INDEX_COLOUR );
                    gr.drawString( str, x - (strWidth - 1) / 2,
                                   y + INDEX_MARKER_HEIGHT + fontMetrics.getAscent( ) );
                }

                // Increment x coordinate
                x += barInterval;
            }
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods
    ////////////////////////////////////////////////////////////////////

        private int getPlotWidth( )
        {
            return ( NUM_FREQUENCIES * (barWidth + BAR_GAP) - BAR_GAP );
        }

        //--------------------------------------------------------------

        private void setFrequencies( double[] frequencies )
        {
            frequencyHeights = new int[frequencies.length];
            frequencyStrs = new String[frequencies.length];
            for ( int i = 0; i < frequencies.length; ++i )
            {
                double freq = frequencies[i];
                if ( freq >= 0.0 )
                {
                    frequencyHeights[i] = (int)Math.round( (freq - 0.5) * (double)HALF_PLOT_HEIGHT );
                    frequencyStrs[i] = AppConstants.FORMAT_1_3.format( freq );
                }
            }

            repaint( );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        private List<Integer>   bitIndices;
        private int             barWidth;
        private int             height;
        private int[]           frequencyHeights;
        private String[]        frequencyStrs;

    }

    //==================================================================


    // ENTROPY-SOURCE BIT SEQUENCE PANEL CLASS


    private static class BitSequencePanel
        extends JComponent
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        private static final    int NUM_FREQUENCIES = EntropyAccumulator.Metrics.NUM_BIT_SEQUENCES;

        private static final    int TOP_MARGIN      = 4;
        private static final    int BOTTOM_MARGIN   = 4;
        private static final    int LEFT_MARGIN     = 5;
        private static final    int RIGHT_MARGIN    = LEFT_MARGIN;

        private static final    int LABEL_GAP   = 4;

        private static final    int X_DIVISION_WIDTH    = 16;
        private static final    int NUM_X_DIVISIONS     = NUM_FREQUENCIES / X_DIVISION_WIDTH;

        private static final    int Y_DIVISION_HEIGHT   = 40;
        private static final    int NUM_Y_DIVISIONS     = 2;
        private static final    int PLOT_HEIGHT         = NUM_Y_DIVISIONS * Y_DIVISION_HEIGHT + 1;
        private static final    int HALF_PLOT_HEIGHT    = NUM_Y_DIVISIONS / 2 * Y_DIVISION_HEIGHT;
        private static final    int PLOT_GAP            = 4;
        private static final    int PLOT_INTERVAL       = PLOT_HEIGHT + PLOT_GAP;

        private static final    int BAR_WIDTH   = 1;
        private static final    int PLOT_WIDTH  = NUM_FREQUENCIES * BAR_WIDTH;

        private static final    Color   BACKGROUND_COLOUR   = Colours.BACKGROUND;
        private static final    Color   TEXT_COLOUR         = Colours.FOREGROUND;
        private static final    Color   GRID_COLOUR         = new Color( 216, 216, 210 );
        private static final    Color   BAR_COLOUR0         = Color.DARK_GRAY;
        private static final    Color   BAR_COLOUR1         = new Color( 0, 64, 192 );
        private static final    Color   BAR_COLOUR2         = new Color( 192, 64, 0 );

        private static final    String  BIT_STR         = "Bit ";
        private static final    String  SEQUENCE_STR    = "sequence ";

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private BitSequencePanel( int bitMask )
        {
            // Set font
            AppFont.MAIN.apply( this );

            // Initialise instance variables
            bitIndices = getBitIndices( bitMask );
            String str = BIT_STR + Integer.toString( bitIndices.get( bitIndices.size( ) - 1 ) );
            labelWidth = getFontMetrics( getFont( ) ).stringWidth( str );

            // Set component attributes
            setOpaque( true );
            setFocusable( false );
            setBorder( BorderFactory.createLineBorder( BORDER_COLOUR ) );
            setToolTipText( new String( ) );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : overriding methods
    ////////////////////////////////////////////////////////////////////

        @Override
        public String getToolTipText( MouseEvent event )
        {
            String text = null;
            if ( frequencyStrs != null )
            {
                int y = event.getY( ) - TOP_MARGIN;
                if ( (y >= 0) && (y < getTotalPlotHeight( )) && (y % PLOT_INTERVAL < PLOT_HEIGHT) )
                {
                    int bitIndex = y / PLOT_INTERVAL;
                    int x = event.getX( ) - getPlotX( );
                    if ( (x >= 0) && (x < NUM_FREQUENCIES) )
                    {
                        int seqIndex = x;
                        if ( frequencyStrs[bitIndex][seqIndex] != null )
                        {
                            StringBuilder buffer = new StringBuilder( 32 );
                            buffer.append( BIT_STR );
                            buffer.append( bitIndices.get( bitIndex ) );
                            buffer.append( ", " );
                            buffer.append( SEQUENCE_STR );
                            buffer.append( NumberUtilities.uIntToBinString( seqIndex, 8, '0' ) );
                            buffer.append( " : " );
                            buffer.append( frequencyStrs[bitIndex][seqIndex] );
                            text = buffer.toString( );
                        }
                    }
                }
            }
            return text;
        }

        //--------------------------------------------------------------

        @Override
        public Dimension getPreferredSize( )
        {
            return new Dimension( LEFT_MARGIN + labelWidth + LABEL_GAP + PLOT_WIDTH + RIGHT_MARGIN,
                                  TOP_MARGIN + getTotalPlotHeight( ) + BOTTOM_MARGIN );
        }

        //--------------------------------------------------------------

        @Override
        protected void paintComponent( Graphics gr )
        {
            // Create copy of graphics context
            gr = gr.create( );

            // Draw background
            Rectangle rect = gr.getClipBounds( );
            gr.setColor( BACKGROUND_COLOUR );
            gr.fillRect( rect.x, rect.y, rect.width, rect.height );

            // Set rendering hints for text antialiasing and fractional metrics
            TextRendering.setHints( (Graphics2D)gr );

            // Draw plots
            int plotX = getPlotX( );
            int plotY = TOP_MARGIN;
            for ( int i = 0; i < bitIndices.size( ); ++i )
            {
                // Draw label
                int y = plotY + GuiUtilities.getBaselineOffset( PLOT_HEIGHT - 1, gr.getFontMetrics( ) );
                gr.setColor( TEXT_COLOUR );
                gr.drawString( BIT_STR + bitIndices.get( i ), LEFT_MARGIN, y );

                // Draw vertical grid lines
                int x = plotX;
                int y1 = plotY;
                int y2 = y1 + PLOT_HEIGHT - 1;
                gr.setColor( GRID_COLOUR );
                for ( int j = 0; j <= NUM_X_DIVISIONS; ++j )
                {
                    gr.drawLine( x, y1, x, y2 );
                    x += X_DIVISION_WIDTH;
                }

                // Draw horizontal grid lines
                int x1 = plotX;
                int x2 = x1 + PLOT_WIDTH;
                y = plotY;
                for ( int j = 0; j <= NUM_Y_DIVISIONS; ++j )
                {
                    gr.drawLine( x1, y, x2, y );
                    y += Y_DIVISION_HEIGHT;
                }

                // Draw frequency bars
                if ( frequencyHeights != null )
                {
                    x = plotX;
                    int midY = plotY + HALF_PLOT_HEIGHT;
                    for ( int j = 0; j < NUM_FREQUENCIES; ++j )
                    {
                        int barHeight = frequencyHeights[i][j];
                        y1 = (barHeight < 0) ? midY : midY - barHeight;
                        y2 = (barHeight < 0) ? midY - barHeight : midY;
                        gr.setColor( (barHeight == 0) ? BAR_COLOUR0
                                                      : (barHeight < 0) ? BAR_COLOUR1 : BAR_COLOUR2 );
                        gr.drawLine( x, y1, x, y2 );
                        ++x;
                    }
                }

                // Increment y coordinate
                plotY += PLOT_INTERVAL;
            }
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods
    ////////////////////////////////////////////////////////////////////

        private int getPlotX( )
        {
            return ( LEFT_MARGIN + labelWidth + LABEL_GAP );
        }

        //--------------------------------------------------------------

        private int getTotalPlotHeight( )
        {
            return ( bitIndices.size( ) * PLOT_INTERVAL - PLOT_GAP );
        }

        //--------------------------------------------------------------

        private void setFrequencies( double[][] frequencies )
        {
            final   double  EXPECTED_FREQ   = 1.0 / NUM_FREQUENCIES;

            double maxDeltaFreq = 0.0;
            for ( int i = 0; i < frequencies.length; ++i )
            {
                for ( int j = 0; j < frequencies[i].length; ++j )
                {
                    double deltaFreq = Math.abs( frequencies[i][j] - EXPECTED_FREQ );
                    if ( maxDeltaFreq < deltaFreq )
                        maxDeltaFreq = deltaFreq;
                }
            }
            double factor = (maxDeltaFreq == 0.0) ? 0.0 : (double)HALF_PLOT_HEIGHT / maxDeltaFreq;

            frequencyHeights = new int[frequencies.length][NUM_FREQUENCIES];
            frequencyStrs = new String[frequencies.length][NUM_FREQUENCIES];
            for ( int i = 0; i < frequencies.length; ++i )
            {
                for ( int j = 0; j < frequencies[i].length; ++j )
                {
                    double freq = frequencies[i][j];
                    if ( freq >= 0.0 )
                    {
                        frequencyHeights[i][j] = (int)Math.round( (freq - EXPECTED_FREQ) * factor );
                        frequencyStrs[i][j] = AppConstants.FORMAT_1_4.format( freq );
                    }
                }
            }

            repaint( );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        private List<Integer>   bitIndices;
        private int             labelWidth;
        private int[][]         frequencyHeights;
        private String[][]      frequencyStrs;

    }

    //==================================================================


    // ENTROPY-POOL LENGTH PANEL CLASS


    private static class PoolLengthPanel
        extends JComponent
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        private static final    int NUM_POOLS   = Fortuna.NUM_ENTROPY_POOLS;

        private static final    int TOP_MARGIN      = 4;
        private static final    int BOTTOM_MARGIN   = 2;
        private static final    int LEFT_MARGIN     = 4;
        private static final    int RIGHT_MARGIN    = LEFT_MARGIN;

        private static final    int DIVISION_HEIGHT             = 25;
        private static final    int INDEX_MARKER_HEIGHT         = 3;
        private static final    int INDEX_MARKER_EXTRA_HEIGHT   = 4;

        private static final    int NUM_Y_DIVISIONS     = 4;
        private static final    int PLOT_HEIGHT         = NUM_Y_DIVISIONS * DIVISION_HEIGHT + 1;

        private static final    int BAR_WIDTH       = 5;
        private static final    int BAR_GAP         = 1;
        private static final    int BAR_INTERVAL    = BAR_WIDTH + BAR_GAP;
        private static final    int PLOT_WIDTH      = NUM_POOLS * BAR_INTERVAL - BAR_GAP;
        private static final    int INDEX_INTERVAL  = 4;

        private static final    Color   BACKGROUND_COLOUR       = Colours.BACKGROUND;
        private static final    Color   INDEX_BACKGROUND_COLOUR = new Color( 224, 236, 224 );
        private static final    Color   GRID_COLOUR             = new Color( 216, 216, 210 );
        private static final    Color   INDEX_COLOUR            = Colours.FOREGROUND;
        private static final    Color   MARKER_COLOUR1          = Color.BLACK;
        private static final    Color   MARKER_COLOUR2          = Color.GRAY;
        private static final    Color   BAR_COLOUR1             = new Color( 0, 160, 0 );
        private static final    Color   BAR_COLOUR2             = new Color( 192, 96, 0 );

        private static final    String  POOL_STR        = "Pool ";
        private static final    String  BYTES_STR       = " bytes";
        private static final    String  PROTOTYPE_STR   = "00";

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private PoolLengthPanel( )
        {
            // Set font
            Font font = AppFont.MAIN.getFont( );
            FontMetrics fontMetrics = getFontMetrics( font );
            int strWidth = fontMetrics.stringWidth( PROTOTYPE_STR );
            int maxStrWidth = INDEX_INTERVAL * BAR_INTERVAL - 4;
            if ( strWidth > maxStrWidth )
                font = font.deriveFont( (float)maxStrWidth / (float)strWidth );
            setFont( font );

            // Initialise instance variables
            height = TOP_MARGIN + PLOT_HEIGHT + INDEX_MARKER_HEIGHT + fontMetrics.getAscent( ) +
                                                                fontMetrics.getDescent( ) + BOTTOM_MARGIN;

            // Set component attributes
            setOpaque( true );
            setFocusable( false );
            setBorder( BorderFactory.createLineBorder( BORDER_COLOUR ) );
            setToolTipText( new String( ) );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : overriding methods
    ////////////////////////////////////////////////////////////////////

        @Override
        public String getToolTipText( MouseEvent event )
        {
            String text = null;
            if ( lengthStrs != null )
            {
                int y = event.getY( ) - TOP_MARGIN;
                if ( (y >= 0) && (y < PLOT_HEIGHT) )
                {
                    int x = event.getX( ) - LEFT_MARGIN;
                    if ( (x >= 0) && (x < PLOT_WIDTH) && (x % BAR_INTERVAL < BAR_WIDTH) )
                    {
                        int index = x / BAR_INTERVAL;
                        if ( (index < lengthStrs.length) && (lengthStrs[index] != null) )
                        {
                            StringBuilder buffer = new StringBuilder( 32 );
                            buffer.append( POOL_STR );
                            buffer.append( index );
                            buffer.append( " : " );
                            buffer.append( lengthStrs[index] );
                            buffer.append( BYTES_STR );
                            text = buffer.toString( );
                        }
                    }
                }
            }
            return text;
        }

        //--------------------------------------------------------------

        @Override
        public Dimension getPreferredSize( )
        {
            return new Dimension( LEFT_MARGIN + PLOT_WIDTH + RIGHT_MARGIN, height );
        }

        //--------------------------------------------------------------

        @Override
        protected void paintComponent( Graphics gr )
        {
            // Create copy of graphics context
            gr = gr.create( );

            // Draw background
            Rectangle rect = gr.getClipBounds( );
            gr.setColor( BACKGROUND_COLOUR );
            gr.fillRect( rect.x, rect.y, rect.width, rect.height );

            int y = TOP_MARGIN + PLOT_HEIGHT;
            gr.setColor( INDEX_BACKGROUND_COLOUR );
            gr.fillRect( rect.x, y, rect.width, getHeight( ) - y );

            // Draw horizontal grid lines
            y = TOP_MARGIN;
            gr.setColor( GRID_COLOUR );
            for ( int i = 0; i <= NUM_Y_DIVISIONS; ++i )
            {
                gr.drawLine( 0, y, getWidth( ) - 1, y );
                y += DIVISION_HEIGHT;
            }

            // Draw length bars
            if ( lengthHeights != null )
            {
                int x = LEFT_MARGIN;
                for ( int i = 0; i < lengthHeights.length; ++i )
                {
                    int y2 = TOP_MARGIN + PLOT_HEIGHT - 1;
                    int y1 = y2 - lengthHeights[i];
                    gr.setColor( canReseed ? BAR_COLOUR1 : BAR_COLOUR2 );
                    gr.fillRect( x, y1, BAR_WIDTH, y2 - y1 + 1 );
                    x += BAR_INTERVAL;
                }
            }

            // Set rendering hints for text antialiasing and fractional metrics
            TextRendering.setHints( (Graphics2D)gr );

            // Draw index markers and text
            FontMetrics fontMetrics = gr.getFontMetrics( );
            int x = LEFT_MARGIN + BAR_WIDTH / 2;
            y = TOP_MARGIN + PLOT_HEIGHT;
            for ( int i = 0; i < NUM_POOLS; ++i )
            {
                if ( i % INDEX_INTERVAL == 0 )
                {
                    // Draw index marker
                    gr.setColor( MARKER_COLOUR1 );
                    gr.drawLine( x, y, x, y + INDEX_MARKER_HEIGHT + INDEX_MARKER_EXTRA_HEIGHT - 1 );

                    // Draw text
                    String str = Integer.toString( i );
                    gr.setColor( INDEX_COLOUR );
                    gr.drawString( str, x + 2, y + INDEX_MARKER_HEIGHT + fontMetrics.getAscent( ) );
                }
                else
                {
                    // Draw index marker
                    gr.setColor( MARKER_COLOUR2 );
                    gr.drawLine( x, y, x, y + INDEX_MARKER_HEIGHT - 1 );
                }

                // Increment x coordinate
                x += BAR_INTERVAL;
            }
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods
    ////////////////////////////////////////////////////////////////////

        private void setLengths( int[] lengths )
        {
            canReseed = (lengths[0] >= Fortuna.RESEED_ENTROPY_THRESHOLD);

            double maxLogLength = 0.0;
            for ( int i = 0; i < lengths.length; ++i )
            {
                double logLength = (lengths[i] == 0) ? 0.0 : Math.log( (double)lengths[i] );
                if ( maxLogLength < logLength )
                    maxLogLength = logLength;
            }
            double factor = (maxLogLength == 0.0) ? 0.0 : (double)(PLOT_HEIGHT - 1) / maxLogLength;

            lengthHeights = new int[lengths.length];
            lengthStrs = new String[lengths.length];
            for ( int i = 0; i < lengths.length; ++i )
            {
                int length = lengths[i];
                lengthHeights[i] = (int)Math.round( Math.log( (double)length ) * factor );
                lengthStrs[i] = Integer.toString( length );
            }

            repaint( );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance variables
    ////////////////////////////////////////////////////////////////////

        private int         height;
        private int[]       lengthHeights;
        private boolean     canReseed;
        private String[]    lengthStrs;

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


    // WINDOW EVENT HANDLER CLASS


    private class WindowEventHandler
        extends WindowAdapter
    {

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private WindowEventHandler( )
        {
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : overriding methods
    ////////////////////////////////////////////////////////////////////

        @Override
        public void windowOpened( WindowEvent event )
        {
            onUpdate( );
        }

        //--------------------------------------------------------------

        @Override
        public void windowClosing( WindowEvent event )
        {
            onClose( );
        }

        //--------------------------------------------------------------

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private EntropyMetricsDialog( Window             owner,
                                  Fortuna            prng,
                                  EntropyAccumulator entropyAccumulator )
    {

        // Call superclass constructor
        super( owner, TITLE_STR, Dialog.ModalityType.APPLICATION_MODAL );

        // Set icons
        setIconImages( owner.getIconImages( ) );

        // Initialise instance variables
        this.prng = prng;
        this.entropyAccumulator = entropyAccumulator;


        //----  Entropy sources panel

        GridBagLayout gridBag = new GridBagLayout( );
        GridBagConstraints gbc = new GridBagConstraints( );

        int numSources = entropyAccumulator.getSourceKinds( ).size( );
        if ( numSources > 0 )
        {
            sourcesPanel = new FTabbedPane( );
            TitledBorder.setPaddedBorder( sourcesPanel, ENTROPY_SOURCES_STR );

            oneBitPanels = new EnumMap<>( EntropyAccumulator.SourceKind.class );
            bitSequencePanels = new EnumMap<>( EntropyAccumulator.SourceKind.class );
            for ( EntropyAccumulator.SourceKind sourceKind : entropyAccumulator.getSourceKinds( ) )
            {
                // Panel: source kind
                JPanel sourceKindPanel = new JPanel( gridBag );
                sourceKindPanel.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );
                sourcesPanel.addTab( sourceKind.toString( ), sourceKindPanel );

                // Panel: one-bit frequencies
                int bitMask = entropyAccumulator.getSourceBitMask( sourceKind );
                OneBitPanel oneBitPanel = new OneBitPanel( bitMask );
                oneBitPanels.put( sourceKind, oneBitPanel );

                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.gridwidth = 1;
                gbc.gridheight = 1;
                gbc.weightx = 0.0;
                gbc.weighty = 0.0;
                gbc.anchor = GridBagConstraints.CENTER;
                gbc.fill = GridBagConstraints.NONE;
                gbc.insets = new Insets( 0, 0, 0, 0 );
                gridBag.setConstraints( oneBitPanel, gbc );
                sourceKindPanel.add( oneBitPanel );

                // Panel: bit-sequence frequencies
                BitSequencePanel bitSequencePanel = new BitSequencePanel( bitMask );
                bitSequencePanels.put( sourceKind, bitSequencePanel );

                gbc.gridx = 1;
                gbc.gridy = 0;
                gbc.gridwidth = 1;
                gbc.gridheight = 1;
                gbc.weightx = 0.0;
                gbc.weighty = 0.0;
                gbc.anchor = GridBagConstraints.CENTER;
                gbc.fill = GridBagConstraints.NONE;
                gbc.insets = new Insets( 0, 6, 0, 0 );
                gridBag.setConstraints( bitSequencePanel, gbc );
                sourceKindPanel.add( bitSequencePanel );
            }

            if ( sourceIndex < numSources )
                sourcesPanel.setSelectedIndex( sourceIndex );
        }


        //----  Entropy pools panel

        JPanel poolsPanel = new JPanel( gridBag );
        TitledBorder.setPaddedBorder( poolsPanel, ENTROPY_POOLS_STR );

        // Panel: pool lengths
        poolLengthPanel = new PoolLengthPanel( );

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets( 0, 0, 0, 0 );
        gridBag.setConstraints( poolLengthPanel, gbc );
        poolsPanel.add( poolLengthPanel );


        //----  Upper panel

        JPanel upperPanel = new JPanel( gridBag );

        int gridX = 0;

        if ( numSources > 0 )
        {
            gbc.gridx = gridX++;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 0.0;
            gbc.weighty = 0.0;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.VERTICAL;
            gbc.insets = new Insets( 0, 0, 0, 3 );
            gridBag.setConstraints( sourcesPanel, gbc );
            upperPanel.add( sourcesPanel );
        }

        gbc.gridx = gridX++;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets( 0, 0, 0, 0 );
        gridBag.setConstraints( poolsPanel, gbc );
        upperPanel.add( poolsPanel );


        //----  Button panel

        JPanel buttonPanel = new JPanel( new GridLayout( 1, 0, 8, 0 ) );
        buttonPanel.setBorder( BorderFactory.createEmptyBorder( 3, 8, 3, 8 ) );

        // Button: update
        JButton updateButton = new FButton( UPDATE_STR );
        updateButton.setActionCommand( Command.UPDATE );
        updateButton.addActionListener( this );
        buttonPanel.add( updateButton );

        // Button: clear
        if ( numSources > 0 )
        {
            JButton clearButton = new FButton( CLEAR_STR + AppConstants.ELLIPSIS_STR );
            clearButton.setActionCommand( Command.CLEAR );
            clearButton.addActionListener( this );
            buttonPanel.add( clearButton );
        }

        // Button: close
        JButton closeButton = new FButton( AppConstants.CLOSE_STR );
        closeButton.setActionCommand( Command.CLOSE );
        closeButton.addActionListener( this );
        buttonPanel.add( closeButton );


        //----  Main panel

        JPanel mainPanel = new JPanel( gridBag );
        mainPanel.setBorder( BorderFactory.createEmptyBorder( 3, 3, 2, 3 ) );

        int gridY = 0;

        gbc.gridx = 0;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets( 0, 0, 0, 0 );
        gridBag.setConstraints( upperPanel, gbc );
        mainPanel.add( upperPanel );

        gbc.gridx = 0;
        gbc.gridy = gridY++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets( 3, 0, 0, 0 );
        gridBag.setConstraints( buttonPanel, gbc );
        mainPanel.add( buttonPanel );

        // Add commands to action map
        KeyAction.create( mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, this, KEY_COMMANDS );


        //----  Window

        // Set content pane
        setContentPane( mainPanel );

        // Dispose of window explicitly
        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );

        // Handle window events
        addWindowListener( new WindowEventHandler( ) );

        // Prevent dialog from being resized
        setResizable( false );

        // Resize dialog to its preferred size
        pack( );

        // Set location of dialog box
        if ( location == null )
            location = GuiUtilities.getComponentLocation( this, owner );
        setLocation( location );

        // Set default button
        getRootPane( ).setDefaultButton( updateButton );

        // Show dialog
        setVisible( true );

    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

    public static void showDialog( Component          parent,
                                   Fortuna            prng,
                                   EntropyAccumulator entropyAccumulator )
    {
        new EntropyMetricsDialog( GuiUtilities.getWindow( parent ), prng, entropyAccumulator );
    }

    //------------------------------------------------------------------

    private static List<Integer> getBitIndices( int mask )
    {
        List<Integer> indices = new ArrayList<>( );
        for ( int i = 0; i < EntropyAccumulator.SourceParams.BIT_MASK_LENGTH; ++i )
        {
            if ( (mask & 1 << i) != 0 )
                indices.add( i );
        }
        return indices;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

    public void actionPerformed( ActionEvent event )
    {
        String command = event.getActionCommand( );

        if ( command.equals( Command.UPDATE ) )
            onUpdate( );

        else if ( command.equals( Command.CLEAR ) )
            onClear( );

        else if ( command.equals( Command.CLOSE ) )
            onClose( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    private void updateSources( )
    {
        Map<EntropyAccumulator.SourceKind, EntropyAccumulator.Metrics> allMetrics =
                                                                        entropyAccumulator.getMetrics( );
        if ( allMetrics != null )
        {
            for ( EntropyAccumulator.SourceKind sourceKind : allMetrics.keySet( ) )
            {
                EntropyAccumulator.Metrics metrics = allMetrics.get( sourceKind );
                oneBitPanels.get( sourceKind ).setFrequencies( metrics.oneBitFrequencies );
                bitSequencePanels.get( sourceKind ).setFrequencies( metrics.bitSequenceFrequencies );
            }
        }
    }

    //------------------------------------------------------------------

    private void onUpdate( )
    {
        // Update entropy sources
        updateSources( );

        // Update entropy pools
        poolLengthPanel.setLengths( prng.getEntropyPoolLengths( ) );
    }

    //------------------------------------------------------------------

    private void onClear( )
    {
        String[] optionStrs = Util.getOptionStrings( AppConstants.CLEAR_STR );
        if ( JOptionPane.showOptionDialog( this, CLEAR_METRICS_MESSAGE_STR, CLEAR_METRICS_STR,
                                           JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                           optionStrs, optionStrs[1] ) == JOptionPane.OK_OPTION )
        {
            entropyAccumulator.clearMetrics( );
            updateSources( );
        }
    }

    //------------------------------------------------------------------

    private void onClose( )
    {
        location = getLocation( );
        sourceIndex = (sourcesPanel == null) ? 0 : sourcesPanel.getSelectedIndex( );
        setVisible( false );
        dispose( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

    private static  Point   location;
    private static  int     sourceIndex;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private Fortuna                                                 prng;
    private EntropyAccumulator                                      entropyAccumulator;
    private JTabbedPane                                             sourcesPanel;
    private Map<EntropyAccumulator.SourceKind, OneBitPanel>         oneBitPanels;
    private Map<EntropyAccumulator.SourceKind, BitSequencePanel>    bitSequencePanels;
    private PoolLengthPanel                                         poolLengthPanel;

}

//----------------------------------------------------------------------
