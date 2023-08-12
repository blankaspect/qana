/*====================================================================*\

ImportQueue.java

Import queue class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.misc;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.concurrent.ConcurrentLinkedQueue;

//----------------------------------------------------------------------


// IMPORT QUEUE CLASS


public class ImportQueue
{

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// QUEUE ELEMENT CLASS


	public static class Element
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public Element(IFileImporter    fileImporter,
					   Collection<File> files)
		{
			this.fileImporter = fileImporter;
			this.files = new ArrayList<>(files);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		public	IFileImporter	fileImporter;
		public	List<File>		files;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public ImportQueue()
	{
		elements = new ConcurrentLinkedQueue<>();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public boolean isEmpty()
	{
		return elements.isEmpty();
	}

	//------------------------------------------------------------------

	public void add(IFileImporter    fileImporter,
					Collection<File> files)
	{
		elements.add(new Element(fileImporter, files));
	}

	//------------------------------------------------------------------

	public Element remove()
	{
		return elements.poll();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	ConcurrentLinkedQueue<Element>	elements;

}

//----------------------------------------------------------------------
