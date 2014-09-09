/*====================================================================*\

KeySelectionList.java

Key selection list class.

\*====================================================================*/


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import uk.org.blankaspect.gui.Colours;
import uk.org.blankaspect.gui.GuiUtilities;

//----------------------------------------------------------------------


// KEY SELECTION LIST CLASS


class KeySelectionList
    extends JList<KeyList.Key>
    implements FocusListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    private static final    int NUM_COLUMNS = 40;

    private static final    ImageIcon   BLANK_ICON      = new ImageIcon( ImageData.BLANK );
    private static final    ImageIcon   ASTERISK_ICON   = new ImageIcon( ImageData.ASTERISK );
    private static final    ImageIcon   RHOMBUS_ICON    = new ImageIcon( ImageData.RHOMBUS );
    private static final    ImageIcon   TICK_ICON       = new ImageIcon( ImageData.TICK );

    private interface ImageData
    {
        byte[]  BLANK       =
        {
            (byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0D, (byte)0x49, (byte)0x48, (byte)0x44, (byte)0x52,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08,
            (byte)0x08, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xC4, (byte)0x0F, (byte)0xBE,
            (byte)0x8B, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0D, (byte)0x49, (byte)0x44, (byte)0x41,
            (byte)0x54, (byte)0x78, (byte)0xDA, (byte)0x63, (byte)0x60, (byte)0x18, (byte)0x05, (byte)0x20,
            (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x5D, (byte)0xB7,
            (byte)0x19, (byte)0x0D, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x49, (byte)0x45,
            (byte)0x4E, (byte)0x44, (byte)0xAE, (byte)0x42, (byte)0x60, (byte)0x82
        };

        byte[]  ASTERISK    =
        {
            (byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0D, (byte)0x49, (byte)0x48, (byte)0x44, (byte)0x52,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08,
            (byte)0x08, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xC4, (byte)0x0F, (byte)0xBE,
            (byte)0x8B, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x71, (byte)0x49, (byte)0x44, (byte)0x41,
            (byte)0x54, (byte)0x78, (byte)0xDA, (byte)0x63, (byte)0xF8, (byte)0xFF, (byte)0xFF, (byte)0x3F,
            (byte)0x03, (byte)0x0C, (byte)0x33, (byte)0x4C, (byte)0x60, (byte)0x98, (byte)0x0D, (byte)0xC4,
            (byte)0xDB, (byte)0x51, (byte)0xC4, (byte)0x18, (byte)0x26, (byte)0x31, (byte)0x88, (byte)0x32,
            (byte)0x4C, (byte)0x64, (byte)0x48, (byte)0x63, (byte)0xE8, (byte)0x66, (byte)0x10, (byte)0x03,
            (byte)0x4A, (byte)0xFE, (byte)0x07, (byte)0x63, (byte)0x10, (byte)0x7B, (byte)0x22, (byte)0x83,
            (byte)0x3F, (byte)0x98, (byte)0x06, (byte)0x32, (byte)0x72, (byte)0xA1, (byte)0x12, (byte)0x1F,
            (byte)0xE1, (byte)0x0A, (byte)0x10, (byte)0xEC, (byte)0x3A, (byte)0x06, (byte)0x86, (byte)0x7E,
            (byte)0x06, (byte)0x01, (byte)0x90, (byte)0xB1, (byte)0x50, (byte)0x81, (byte)0x3E, (byte)0x20,
            (byte)0x9E, (byte)0x08, (byte)0x65, (byte)0x6F, (byte)0x87, (byte)0x98, (byte)0x30, (byte)0x81,
            (byte)0x41, (byte)0x1C, (byte)0x49, (byte)0x41, (byte)0x2F, (byte)0x50, (byte)0x43, (byte)0x1F,
            (byte)0x5C, (byte)0x01, (byte)0x48, (byte)0x0E, (byte)0xA8, (byte)0xBE, (byte)0x05, (byte)0x8F,
            (byte)0x15, (byte)0x8D, (byte)0x0C, (byte)0x0C, (byte)0x53, (byte)0x19, (byte)0x24, (byte)0x80,
            (byte)0x8C, (byte)0x2C, (byte)0x0C, (byte)0x47, (byte)0xC2, (byte)0xC4, (byte)0xD0, (byte)0xBC,
            (byte)0xB9, (byte)0x05, (byte)0x84, (byte)0x91, (byte)0xC5, (byte)0x00, (byte)0xE0, (byte)0x85,
            (byte)0x61, (byte)0x05, (byte)0xD0, (byte)0x6F, (byte)0x32, (byte)0x34, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x49, (byte)0x45, (byte)0x4E, (byte)0x44, (byte)0xAE, (byte)0x42,
            (byte)0x60, (byte)0x82
        };

        byte[]  RHOMBUS     =
        {
            (byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0D, (byte)0x49, (byte)0x48, (byte)0x44, (byte)0x52,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08,
            (byte)0x08, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xC4, (byte)0x0F, (byte)0xBE,
            (byte)0x8B, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x53, (byte)0x49, (byte)0x44, (byte)0x41,
            (byte)0x54, (byte)0x78, (byte)0xDA, (byte)0x63, (byte)0xF8, (byte)0xFF, (byte)0xFF, (byte)0x3F,
            (byte)0x03, (byte)0x08, (byte)0xEF, (byte)0x77, (byte)0x60, (byte)0x60, (byte)0x01, (byte)0xE2,
            (byte)0x9E, (byte)0x83, (byte)0x0E, (byte)0x0C, (byte)0xCD, (byte)0x20, (byte)0x36, (byte)0x4C,
            (byte)0x1C, (byte)0x2E, (byte)0x79, (byte)0xC0, (byte)0x81, (byte)0x61, (byte)0x35, (byte)0x10,
            (byte)0xFF, (byte)0x87, (byte)0xE2, (byte)0xD5, (byte)0x30, (byte)0x45, (byte)0xE8, (byte)0x92,
            (byte)0x9F, (byte)0xA1, (byte)0x18, (byte)0xAE, (byte)0x88, (byte)0x01, (byte)0x64, (byte)0x24,
            (byte)0x4C, (byte)0x12, (byte)0x28, (byte)0x60, (byte)0x03, (byte)0xC2, (byte)0x30, (byte)0x45,
            (byte)0x20, (byte)0x2B, (byte)0x41, (byte)0x26, (byte)0xF4, (byte)0xE0, (byte)0x52, (byte)0x00,
            (byte)0xD2, (byte)0x4C, (byte)0xD8, (byte)0x0A, (byte)0x82, (byte)0x8E, (byte)0x44, (byte)0xF6,
            (byte)0x26, (byte)0xD4, (byte)0x8B, (byte)0x3D, (byte)0xC8, (byte)0xDE, (byte)0x04, (byte)0x00,
            (byte)0x08, (byte)0x41, (byte)0x76, (byte)0x0D, (byte)0xCA, (byte)0xA5, (byte)0x62, (byte)0x04,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x49, (byte)0x45, (byte)0x4E, (byte)0x44,
            (byte)0xAE, (byte)0x42, (byte)0x60, (byte)0x82
        };

        byte[]  TICK        =
        {
            (byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0D, (byte)0x49, (byte)0x48, (byte)0x44, (byte)0x52,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08,
            (byte)0x08, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xC4, (byte)0x0F, (byte)0xBE,
            (byte)0x8B, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x99, (byte)0x49, (byte)0x44, (byte)0x41,
            (byte)0x54, (byte)0x78, (byte)0xDA, (byte)0x63, (byte)0xF8, (byte)0xFF, (byte)0xFF, (byte)0x3F,
            (byte)0x03, (byte)0x3A, (byte)0x0E, (byte)0x6D, (byte)0x68, (byte)0x60, (byte)0x4B, (byte)0x28,
            (byte)0x9C, (byte)0xB0, (byte)0x27, (byte)0xA1, (byte)0xA0, (byte)0x7F, (byte)0x03, (byte)0x03,
            (byte)0x36, (byte)0x05, (byte)0xF1, (byte)0x05, (byte)0xFD, (byte)0xD5, (byte)0x09, (byte)0x05,
            (byte)0x13, (byte)0xFE, (byte)0x03, (byte)0xF1, (byte)0x29, (byte)0x0C, (byte)0xC9, (byte)0xA4,
            (byte)0xC2, (byte)0x7E, (byte)0x25, (byte)0xA0, (byte)0xC4, (byte)0x37, (byte)0x90, (byte)0x82,
            (byte)0xB8, (byte)0xA2, (byte)0x89, (byte)0x8E, (byte)0x0C, (byte)0x89, (byte)0x45, (byte)0xBD,
            (byte)0xB2, (byte)0x71, (byte)0x85, (byte)0x7D, (byte)0xD2, (byte)0x30, (byte)0x05, (byte)0x09,
            (byte)0xF9, (byte)0x13, (byte)0xB6, (byte)0x43, (byte)0x74, (byte)0x4F, (byte)0x5C, (byte)0x02,
            (byte)0xE2, (byte)0x33, (byte)0x00, (byte)0x39, (byte)0x77, (byte)0x80, (byte)0xF8, (byte)0x47,
            (byte)0x42, (byte)0xE1, (byte)0x44, (byte)0x0F, (byte)0xA0, (byte)0x64, (byte)0x28, (byte)0x54,
            (byte)0xF2, (byte)0x43, (byte)0x42, (byte)0xE9, (byte)0x54, (byte)0x09, (byte)0xB0, (byte)0x82,
            (byte)0xC4, (byte)0xC2, (byte)0x09, (byte)0x13, (byte)0xA1, (byte)0xF6, (byte)0xFD, (byte)0x00,
            (byte)0xE2, (byte)0x57, (byte)0x20, (byte)0x76, (byte)0x62, (byte)0xFE, (byte)0x84, (byte)0x1C,
            (byte)0x98, (byte)0x89, (byte)0x40, (byte)0xF0, (byte)0x9F, (byte)0x31, (byte)0xBE, (byte)0x60,
            (byte)0xE2, (byte)0x34, (byte)0xA8, (byte)0x22, (byte)0x10, (byte)0x3E, (byte)0x1B, (byte)0x1A,
            (byte)0xBA, (byte)0x8A, (byte)0x19, (byte)0xAE, (byte)0x00, (byte)0xA2, (byte)0xEA, (byte)0x3F,
            (byte)0x23, (byte)0x50, (byte)0x57, (byte)0x0F, (byte)0x50, (byte)0xF2, (byte)0x7E, (byte)0x62,
            (byte)0x41, (byte)0xBF, (byte)0x11, (byte)0xB2, (byte)0xA3, (byte)0x01, (byte)0xD5, (byte)0x99,
            (byte)0x98, (byte)0x1E, (byte)0xFD, (byte)0xFD, (byte)0x16, (byte)0xBE, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x49, (byte)0x45, (byte)0x4E, (byte)0x44, (byte)0xAE, (byte)0x42,
            (byte)0x60, (byte)0x82
        };
    }

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


    // LIST MODEL CLASS


    private static class ListModel
        extends DefaultListModel<KeyList.Key>
    {

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private ListModel( List<KeyList.Key> keys )
        {
            for ( KeyList.Key key : keys)
                addElement( key );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods
    ////////////////////////////////////////////////////////////////////

        private void updateKey( KeyList.Key key )
        {
            int index = indexOf( key );
            if ( index >= 0 )
                fireContentsChanged( this, index, index );
        }

        //--------------------------------------------------------------

    }

    //==================================================================


    // KEY SELECTION LIST CELL RENDERER CLASS


    private static class CellRenderer
        extends JLabel
        implements ListCellRenderer<KeyList.Key>
    {

    ////////////////////////////////////////////////////////////////////
    //  Constants
    ////////////////////////////////////////////////////////////////////

        private static final    int TOP_MARGIN      = 1;
        private static final    int BOTTOM_MARGIN   = 1;
        private static final    int LEFT_MARGIN     = 6;
        private static final    int RIGHT_MARGIN    = 4;
        private static final    int GAP             = LEFT_MARGIN;

        private static final    Color   BORDER_COLOUR   = new Color( 212, 220, 220 );

        private static final    String  NEW_STR = "New";

    ////////////////////////////////////////////////////////////////////
    //  Constructors
    ////////////////////////////////////////////////////////////////////

        private CellRenderer( )
        {
            AppFont.COMBO_BOX.apply( this );
            setBorder( BorderFactory.createCompoundBorder(
                                        BorderFactory.createMatteBorder( 0, 0, 1, 0, BORDER_COLOUR ),
                                        BorderFactory.createEmptyBorder( TOP_MARGIN, LEFT_MARGIN,
                                                                         BOTTOM_MARGIN, RIGHT_MARGIN ) ) );
            setIconTextGap( GAP );
            setOpaque( true );
            setFocusable( false );
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods : ListCellRenderer interface
    ////////////////////////////////////////////////////////////////////

        public Component getListCellRendererComponent( JList<? extends KeyList.Key> list,
                                                       KeyList.Key                  key,
                                                       int                          index,
                                                       boolean                      isSelected,
                                                       boolean                      cellHasFocus )
        {
            KeyKind keyKind = key.getKind( );
            setBackground( isSelected ? list.isFocusOwner( )
                                                    ? Colours.List.FOCUSED_SELECTION_BACKGROUND.getColour( )
                                                    : Colours.List.SELECTION_BACKGROUND.getColour( )
                                      : keyKind.getBackgroundColour( ) );
            setForeground( keyKind.getTextColour( ) );

            ImageIcon icon = null;
            String text = null;
            switch ( keyKind )
            {
                case NEW:
                    icon = ASTERISK_ICON;
                    text = NEW_STR;
                    break;

                case TEMPORARY:
                    icon = RHOMBUS_ICON;
                    text = key.getName( );
                    break;

                case PERSISTENT:
                    icon = (key.getKey( ) == null) ? BLANK_ICON : TICK_ICON;
                    text = key.getName( );
                    break;
            }
            setIcon( icon );
            setText( text );

            return this;
        }

        //--------------------------------------------------------------

    ////////////////////////////////////////////////////////////////////
    //  Instance methods
    ////////////////////////////////////////////////////////////////////

        private int getColumnWidth( )
        {
            return GuiUtilities.getCharWidth( '0', getFontMetrics( getFont( ) ) );
        }

        //--------------------------------------------------------------

        private int getRowHeight( )
        {
            return ( TOP_MARGIN + getFontMetrics( getFont( ) ).getHeight( ) + BOTTOM_MARGIN );
        }

        //--------------------------------------------------------------

    }

    //==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    public KeySelectionList( int               numRows,
                             List<KeyList.Key> keys )
    {
        // Call superclass constructor
        super( new ListModel( keys ) );

        // Set attributes
        AppFont.COMBO_BOX.apply( this );
        CellRenderer cellRenderer = new CellRenderer( );
        setCellRenderer( cellRenderer );
        setFixedCellHeight( cellRenderer.getRowHeight( ) );
        setVisibleRowCount( numRows );
        setBackground( Colours.List.BACKGROUND.getColour( ) );

        // Add listeners
        addFocusListener( this );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : FocusListener interface
////////////////////////////////////////////////////////////////////////

    public void focusGained( FocusEvent event )
    {
        repaint( );
    }

    //------------------------------------------------------------------

    public void focusLost( FocusEvent event )
    {
        repaint( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

    @Override
    public Dimension getPreferredScrollableViewportSize( )
    {
        return new Dimension( NUM_COLUMNS * ((CellRenderer)getCellRenderer( )).getColumnWidth( ),
                              getVisibleRowCount( ) * getFixedCellHeight( ) );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    public boolean isChanged( )
    {
        return changed;
    }

    //------------------------------------------------------------------

    public List<KeyList.Key> getKeys( )
    {
        return getKeys( null );
    }

    //------------------------------------------------------------------

    public List<KeyList.Key> getKeys( KeyKind keyKind )
    {
        List<KeyList.Key> keys = new ArrayList<>( );
        ListModel listModel = (ListModel)getModel( );
        for ( int i = 0; i < listModel.getSize( ); ++i )
        {
            KeyList.Key key = listModel.getElementAt( i );
            if ( (keyKind == null) || (keyKind == key.getKind( )) )
                keys.add( key );
        }
        return keys;
    }

    //------------------------------------------------------------------

    public void setChanged( )
    {
        changed = true;
    }

    //------------------------------------------------------------------

    public void setKeys( List<KeyList.Key> keys )
    {
        setModel( new ListModel( keys ) );
        changed = true;
    }

    //------------------------------------------------------------------

    public boolean contains( String name )
    {
        ListModel listModel = (ListModel)getModel( );
        for ( int i = 0; i < listModel.getSize( ); ++i )
        {
            if ( name.equals( listModel.getElementAt( i ).getName( ) ) )
                return true;
        }
        return false;
    }

    //------------------------------------------------------------------

    public void updateKey( KeyList.Key key )
    {
        ((ListModel)getModel( )).updateKey( key );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private boolean changed;

}

//----------------------------------------------------------------------
