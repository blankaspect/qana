/*====================================================================*\

RunnableMessageDialog.java

Class: runnable message dialog box.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.dialog;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

import uk.blankaspect.ui.swing.label.FLabel;

import uk.blankaspect.ui.swing.misc.GuiUtils;

//----------------------------------------------------------------------


// CLASS: RUNNABLE MESSAGE DIALOG BOX


public class RunnableMessageDialog
	extends JDialog
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		VERTICAL_PADDING	= 12;
	private static final	int		HORIZONTAL_PADDING	= 24;

	private static final	Color	BACKGROUND_COLOUR	= new Color(252, 248, 216);
	private static final	Color	BORDER_COLOUR		= new Color(224, 144, 64);

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	boolean	running;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	protected RunnableMessageDialog(Window   owner,
									String   message,
									Runnable runnable)
	{
		// Call superclass constructor
		super(owner, Dialog.ModalityType.APPLICATION_MODAL);


		//----  Message label

		FLabel messageLabel = new FLabel(message);
		messageLabel.setBackground(BACKGROUND_COLOUR);
		messageLabel.setOpaque(true);
		GuiUtils.setPaddedLineBorder(messageLabel, VERTICAL_PADDING, HORIZONTAL_PADDING, BORDER_COLOUR);


		//----  Window

		// Set content pane
		setContentPane(messageLabel);

		// Omit frame from dialog box
		setUndecorated(true);

		// Dispose of window explicitly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Run Runnable when window is activated
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowActivated(WindowEvent event)
			{
				if (!running)
				{
					// Prevent from running again
					running = true;

					// Run Runnable
					runnable.run();

					// Close and destroy dialog
					setVisible(false);
					dispose();
				}
			}
		});

		// Prevent dialog from being resized
		setResizable(false);

		// Resize dialog to its preferred size
		pack();

		// Set location of dialog box
		setLocation(GuiUtils.getComponentLocation(this, owner));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static void showDialog(Component component,
								  String    message,
								  Runnable  runnable)
	{
		new RunnableMessageDialog(GuiUtils.getWindow(component), message, runnable).setVisible(true);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
