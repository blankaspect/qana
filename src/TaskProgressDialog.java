/*====================================================================*\

TaskProgressDialog.java

Task progress dialog box class.

\*====================================================================*/


// IMPORTS


import java.awt.Component;
import java.awt.Window;

import java.io.File;

import uk.org.blankaspect.exception.AppException;

import uk.org.blankaspect.gui.GuiUtilities;

//----------------------------------------------------------------------


// TASK PROGRESS DIALOG BOX CLASS


class TaskProgressDialog
    extends uk.org.blankaspect.gui.TaskProgressDialog
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

    private static final    int DEFAULT_DELAY   = 0;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

    private TaskProgressDialog( Window owner,
                                String titleStr,
                                Task   task,
                                int    delay,
                                int    numProgressBars )
        throws AppException
    {
        super( owner, titleStr, task, delay, numProgressBars, (numProgressBars == 1) ? -1 : 1, true );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

    public static void showDialog( Component parent,
                                   String    titleStr,
                                   Task      task )
        throws AppException
    {
        new TaskProgressDialog( GuiUtilities.getWindow( parent ), titleStr, task, DEFAULT_DELAY, 1 );
    }

    //------------------------------------------------------------------

    public static void showDialog2( Component parent,
                                    String    titleStr,
                                    Task      task )
        throws AppException
    {
        new TaskProgressDialog( GuiUtilities.getWindow( parent ), titleStr, task, DEFAULT_DELAY, 2 );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

    @Override
    public void setProgress( double fractionDone )
    {
        setProgress( 0, fractionDone );
        if ( (getNumProgressBars( ) > 1) && (overallProgressLength > 0.0) )
            setProgress( 1, overallProgressOffset + fractionDone * overallProgressLength );
    }

    //------------------------------------------------------------------

    @Override
    protected String getPathname( File file )
    {
        return Util.getPathname( file );
    }

    //------------------------------------------------------------------

    @Override
    protected char getFileSeparatorChar( )
    {
        return Util.getFileSeparatorChar( );
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

    public void initOverallProgress( long offset,
                                     long length,
                                     long totalLength )
    {
        overallProgressOffset = (double)offset / (double)totalLength;
        overallProgressLength = (double)length / (double)totalLength;
    }

    //------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

    private double  overallProgressOffset;
    private double  overallProgressLength;

}

//----------------------------------------------------------------------
