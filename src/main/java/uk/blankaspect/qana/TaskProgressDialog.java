/*====================================================================*\

TaskProgressDialog.java

Task progress dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.Window;

import java.io.File;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.ui.swing.misc.GuiUtils;

//----------------------------------------------------------------------


// TASK PROGRESS DIALOG CLASS


class TaskProgressDialog
	extends uk.blankaspect.ui.swing.dialog.TaskProgressDialog
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	DEFAULT_DELAY	= 0;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	double	overallProgressOffset;
	private	double	overallProgressLength;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private TaskProgressDialog(Window owner,
							   String title,
							   Task   task,
							   int    delay,
							   int    numProgressBars)
		throws AppException
	{
		super(owner, title, task, delay, numProgressBars, (numProgressBars == 1) ? -1 : 1, true);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static void showDialog(Component parent,
								  String    title,
								  Task      task)
		throws AppException
	{
		new TaskProgressDialog(GuiUtils.getWindow(parent), title, task, DEFAULT_DELAY, 1);
	}

	//------------------------------------------------------------------

	public static void showDialog2(Component parent,
								   String    title,
								   Task      task)
		throws AppException
	{
		new TaskProgressDialog(GuiUtils.getWindow(parent), title, task, DEFAULT_DELAY, 2);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public void setProgress(double fractionDone)
	{
		setProgress(0, fractionDone);
		if ((getNumProgressBars() > 1) && (overallProgressLength > 0.0))
			setProgress(1, overallProgressOffset + fractionDone * overallProgressLength);
	}

	//------------------------------------------------------------------

	@Override
	protected String getPathname(File file)
	{
		return Utils.getPathname(file);
	}

	//------------------------------------------------------------------

	@Override
	protected char getFileSeparatorChar()
	{
		return Utils.getFileSeparatorChar();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void initOverallProgress(long offset,
									long length,
									long totalLength)
	{
		overallProgressOffset = (double)offset / (double)totalLength;
		overallProgressLength = (double)length / (double)totalLength;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
