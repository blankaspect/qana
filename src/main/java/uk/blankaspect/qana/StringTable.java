/*====================================================================*\

StringTable.java

String table class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.io.ByteArrayOutputStream;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

import uk.blankaspect.common.exception.UnexpectedRuntimeException;

//----------------------------------------------------------------------


// STRING TABLE CLASS


class StringTable
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public StringTable()
	{
		// Initialise list of strings
		strings = new ArrayList<>();

		// Add empty string
		strings.add("");
	}

	//------------------------------------------------------------------

	public StringTable(byte[] data,
					   int    offset,
					   int    length)
	{
		strings = new ArrayList<>();
		int endOffset = offset + length;
		while (offset < endOffset)
		{
			int startOffset = offset;
			while (data[offset] != 0)
				++offset;
			strings.add(new String(data, startOffset, offset - startOffset, StandardCharsets.UTF_8));
			++offset;
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void clear()
	{
		strings.clear();
		strings.add("");
	}

	//------------------------------------------------------------------

	public String get(int offset)
	{
		int startOffset = 0;
		for (String str : strings)
		{
			if (offset <= startOffset + str.length())
				return ((offset == startOffset) ? str : str.substring(offset - startOffset));
			startOffset += str.length() + 1;
		}
		return null;
	}

	//------------------------------------------------------------------

	public int find(String str)
	{
		int offset = 0;
		for (String s : strings)
		{
			offset += s.length();
			if (s.endsWith(str))
				return (offset - str.length());
			++offset;
		}
		return -offset;
	}

	//------------------------------------------------------------------

	public int add(String str)
	{
		int offset = find(str);
		if (offset < 0)
		{
			strings.add(str);
			offset = -offset;
		}
		return offset;
	}

	//------------------------------------------------------------------

	public void sort()
	{
		strings.sort(null);
	}

	//------------------------------------------------------------------

	public byte[] toByteArray()
	{
		try
		{
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			for (String str : strings)
			{
				outStream.write(str.getBytes(StandardCharsets.UTF_8));
				outStream.write(0);
			}
			return outStream.toByteArray();
		}
		catch (Exception e)
		{
			throw new UnexpectedRuntimeException(e);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	List<String>	strings;

}

//----------------------------------------------------------------------
