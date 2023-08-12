/*====================================================================*\

TaggedText.java

Tagged text class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.text;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.string.StringUtils;

import uk.blankaspect.ui.swing.font.FontStyle;

//----------------------------------------------------------------------


// TAGGED TEXT CLASS


public class TaggedText
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public enum Alignment
	{
		LEFT,
		RIGHT,
		CENTRE
	}

	private static final	int	MIN_TAG_INDEX	= 0;
	private static final	int	MAX_TAG_INDEX	= 255;

	private static final	char	END_OF_LINE	= '\n';

	private static final	String	TAG_STR	= "Tag: ";

	private enum ParseState
	{
		TEXT,
		TAG_START,
		TAG_INDEX,
		TAG_END,
		STOP
	}

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// TAG


	private enum Tag
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FIELD               ('f'),
		VERTICAL_PADDING    ('v'),
		STYLE               ('s'),
		HORIZONTAL_PADDING  ('h');

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Tag(char key)
		{
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static Tag get(char key)
		{
			for (Tag value : values())
			{
				if (value.key == key)
					return value;
			}
			return null;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	char	key;

	}

	//==================================================================


	// ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		TAG_IDENTIFIER_EXPECTED
		("A tag identifier was expected."),

		UNCLOSED_TAG
		("The tag is not closed."),

		TAG_INDEX_OUT_OF_BOUNDS
		("The tag index must be between " + MIN_TAG_INDEX + " and " + MAX_TAG_INDEX + "."),

		VERTICAL_PADDING_TAG_NOT_ALONE
		("A vertical padding tag must be the only element in a line.");

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ErrorId(String message)
		{
			this.message = message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : AppException.IId interface
	////////////////////////////////////////////////////////////////////

		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// FIELD DEFINITION CLASS


	public static class FieldDef
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	Alignment	DEFAULT_ALIGNMENT	= Alignment.LEFT;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public FieldDef()
		{
			this(-1, DEFAULT_ALIGNMENT);
		}

		//--------------------------------------------------------------

		public FieldDef(int index)
		{
			this(index, DEFAULT_ALIGNMENT);
		}

		//--------------------------------------------------------------

		public FieldDef(int       index,
						Alignment alignment)
		{
			this.index = index;
			this.alignment = alignment;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int			index;
		private	Alignment	alignment;

	}

	//==================================================================


	// VERTICAL PADDING DEFINITION CLASS


	public static class VPaddingDef
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public VPaddingDef(int   index,
						   float height)
		{
			this(index, height, null);
		}

		//--------------------------------------------------------------

		public VPaddingDef(int   index,
						   float height,
						   Color lineColour)
		{
			this.index = index;
			this.height = height;
			this.lineColour = lineColour;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int		index;
		private	float	height;
		private	Color	lineColour;

	}

	//==================================================================


	// STYLE DEFINITION CLASS


	public static class StyleDef
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	FontStyle	DEFAULT_FONT_STYLE	= FontStyle.PLAIN;
		private static final	Color		DEFAULT_COLOUR		= Color.BLACK;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public StyleDef()
		{
			this(-1, DEFAULT_FONT_STYLE, DEFAULT_COLOUR);
		}

		//--------------------------------------------------------------

		public StyleDef(int       index,
						FontStyle fontStyle)
		{
			this(index, fontStyle, DEFAULT_COLOUR);
		}

		//--------------------------------------------------------------

		public StyleDef(int   index,
						Color colour)
		{
			this(index, DEFAULT_FONT_STYLE, colour);
		}

		//--------------------------------------------------------------

		public StyleDef(int       index,
						FontStyle fontStyle,
						Color     colour)
		{
			this.index = index;
			this.fontStyle = fontStyle;
			this.colour = colour;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int			index;
		private	FontStyle	fontStyle;
		private	Color		colour;

	}

	//==================================================================


	// HORIZONTAL PADDING DEFINITION CLASS


	public static class HPaddingDef
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public HPaddingDef(int   index,
						   float width)
		{
			this.index = index;
			this.width = width;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int		index;
		private	float	width;

	}

	//==================================================================


	// PARSE EXCEPTION CLASS


	public static class ParseException
		extends RuntimeException
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	INDEX_STR	= "Index ";

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ParseException(AppException.IId id,
							   int              index)
		{
			this.id = id;
			this.index = index;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public String toString()
		{
			return getException().toString();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public AppException.IId getId()
		{
			return id;
		}

		//--------------------------------------------------------------

		public int getLineNum()
		{
			return lineNum;
		}

		//--------------------------------------------------------------

		public int getIndex()
		{
			return index;
		}

		//--------------------------------------------------------------

		public AppException getException()
		{
			StringBuilder buffer = new StringBuilder(32);
			buffer.append(INDEX_STR);
			if (lineNum > 0)
			{
				buffer.append(lineNum);
				buffer.append(':');
			}
			buffer.append(index);
			buffer.append(": ");
			buffer.append(id.getMessage());

			return new AppException(buffer.toString());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	AppException.IId	id;
		private	int					lineNum;
		private	int					index;

	}

	//==================================================================


	// UNDEFINED TAG EXCEPTION CLASS


	public static class UndefinedTagException
		extends RuntimeException
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private UndefinedTagException(Tag tag,
									  int index)
		{
			super(TAG_STR + tag.key + index);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// DUPLICATE DEFINITION EXCEPTION CLASS


	public static class DuplicateDefinitionException
		extends RuntimeException
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private DuplicateDefinitionException(Tag tag,
											 int index)
		{
			super(TAG_STR + tag.key + index);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// SPAN CLASS


	private static class Span
		extends Field.Element
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Span(int    index,
					 String str)
		{
			super(index);
			this.str = str;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		protected Field.Element.Kind getKind()
		{
			return Field.Element.Kind.SPAN;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	str;

	}

	//==================================================================


	// HORIZONTAL PADDING CLASS


	private static class HPadding
		extends Field.Element
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private HPadding(int index)
		{
			super(index);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		protected Field.Element.Kind getKind()
		{
			return Field.Element.Kind.PADDING;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// FIELD CLASS


	private static class Field
		extends Line.Element
	{

	////////////////////////////////////////////////////////////////////
	//  Member classes : non-inner classes
	////////////////////////////////////////////////////////////////////


		// FIELD ELEMENT CLASS


		private static abstract class Element
		{

		////////////////////////////////////////////////////////////////
		//  Enumerated types
		////////////////////////////////////////////////////////////////

			private enum Kind
			{
				SPAN,
				PADDING
			}

		////////////////////////////////////////////////////////////////
		//  Constructors
		////////////////////////////////////////////////////////////////

			protected Element(int index)
			{
				this.index = index;
			}

			//----------------------------------------------------------

		////////////////////////////////////////////////////////////////
		//  Abstract methods
		////////////////////////////////////////////////////////////////

			protected abstract Kind getKind();

			//----------------------------------------------------------

		////////////////////////////////////////////////////////////////
		//  Instance variables
		////////////////////////////////////////////////////////////////

			protected	int	index;
			protected	int	width;

		}

		//==============================================================

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Field()
		{
			this(-1);
		}

		//--------------------------------------------------------------

		private Field(int index)
		{
			super(index);
			elements = new ArrayList<>();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		protected Line.Element.Kind getKind()
		{
			return Line.Element.Kind.FIELD;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private boolean isEmpty()
		{
			return elements.isEmpty();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int				elementWidth;
		private	int				width;
		private	List<Element>	elements;

	}

	//==================================================================


	// VERTICAL PADDING CLASS


	private static class VPadding
		extends Line.Element
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private VPadding(int index)
		{
			super(index);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		protected Line.Element.Kind getKind()
		{
			return Line.Element.Kind.PADDING;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// LINE CLASS


	private static class Line
	{

	////////////////////////////////////////////////////////////////////
	//  Member classes : non-inner classes
	////////////////////////////////////////////////////////////////////


		// LINE ELEMENT CLASS


		private static abstract class Element
		{

		////////////////////////////////////////////////////////////////
		//  Enumerated types
		////////////////////////////////////////////////////////////////

			private enum Kind
			{
				FIELD,
				PADDING
			}

		////////////////////////////////////////////////////////////////
		//  Constructors
		////////////////////////////////////////////////////////////////

			protected Element(int index)
			{
				this.index = index;
			}

			//----------------------------------------------------------

		////////////////////////////////////////////////////////////////
		//  Abstract methods
		////////////////////////////////////////////////////////////////

			protected abstract Kind getKind();

			//----------------------------------------------------------

		////////////////////////////////////////////////////////////////
		//  Instance variables
		////////////////////////////////////////////////////////////////

			protected	int	index;
			protected	int	height;

		}

		//==============================================================

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Line()
		{
			elements = new ArrayList<>();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private boolean isEmpty()
		{
			return elements.isEmpty();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int				width;
		private	int				height;
		private	List<Element>	elements;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public TaggedText(char tagDelimiter)
	{
		this(tagDelimiter, tagDelimiter);
	}

	//------------------------------------------------------------------

	public TaggedText(char tagPrefix,
					  char tagSuffix)
	{
		this.tagPrefix = tagPrefix;
		this.tagSuffix = tagSuffix;
		fieldDefs = new HashMap<>();
		styleDefs = new HashMap<>();
		vPaddingDefs = new HashMap<>();
		hPaddingDefs = new HashMap<>();
		lines = new ArrayList<>();
	}

	//------------------------------------------------------------------

	/**
	 * @throws DuplicateDefinitionException
	 * @throws ParseException
	 */

	public TaggedText(char              tagDelimiter,
					  List<FieldDef>    fieldDefs,
					  List<StyleDef>    styleDefs,
					  List<VPaddingDef> vPaddingDefs,
					  List<HPaddingDef> hPaddingDefs,
					  String...         strs)
	{
		this(tagDelimiter, tagDelimiter, fieldDefs, styleDefs, vPaddingDefs, hPaddingDefs, strs);
	}

	//------------------------------------------------------------------

	/**
	 * @throws DuplicateDefinitionException
	 * @throws ParseException
	 */

	public TaggedText(char              tagPrefix,
					  char              tagSuffix,
					  List<FieldDef>    fieldDefs,
					  List<StyleDef>    styleDefs,
					  List<VPaddingDef> vPaddingDefs,
					  List<HPaddingDef> hPaddingDefs,
					  String...         strs)
	{
		this(tagPrefix, tagSuffix);
		setFieldDefs(fieldDefs);
		setStyleDefs(styleDefs);
		setVPaddingDefs(vPaddingDefs);
		setHPaddingDefs(hPaddingDefs);
		parse(strs);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public int getNumLines()
	{
		return lines.size();
	}

	//------------------------------------------------------------------

	public int getWidth()
	{
		return width;
	}

	//------------------------------------------------------------------

	public int getHeight()
	{
		return height;
	}

	//------------------------------------------------------------------

	/**
	 * @throws DuplicateDefinitionException
	 */

	public void setFieldDefs(List<FieldDef> defs)
	{
		fieldDefs.clear();
		if (defs != null)
		{
			for (FieldDef def : defs)
			{
				int index = def.index;
				if (fieldDefs.containsKey(index))
					throw new DuplicateDefinitionException(Tag.FIELD, index);
				fieldDefs.put(index, def);
			}
		}
	}

	//------------------------------------------------------------------

	/**
	 * @throws DuplicateDefinitionException
	 */

	public void setStyleDefs(List<StyleDef> defs)
	{
		styleDefs.clear();
		if (defs != null)
		{
			for (StyleDef def : defs)
			{
				int index = def.index;
				if (styleDefs.containsKey(index))
					throw new DuplicateDefinitionException(Tag.STYLE, index);
				styleDefs.put(index, def);
			}
		}
	}

	//------------------------------------------------------------------

	/**
	 * @throws DuplicateDefinitionException
	 */

	public void setVPaddingDefs(List<VPaddingDef> defs)
	{
		vPaddingDefs.clear();
		if (defs != null)
		{
			for (VPaddingDef def : defs)
			{
				int index = def.index;
				if (vPaddingDefs.containsKey(index))
					throw new DuplicateDefinitionException(Tag.VERTICAL_PADDING, index);
				vPaddingDefs.put(index, def);
			}
		}
	}

	//------------------------------------------------------------------

	/**
	 * @throws DuplicateDefinitionException
	 */

	public void setHPaddingDefs(List<HPaddingDef> defs)
	{
		hPaddingDefs.clear();
		if (defs != null)
		{
			for (HPaddingDef def : defs)
			{
				int index = def.index;
				if (hPaddingDefs.containsKey(index))
					throw new DuplicateDefinitionException(Tag.HORIZONTAL_PADDING, index);
				hPaddingDefs.put(index, def);
			}
		}
	}

	//------------------------------------------------------------------

	/**
	 * @throws ParseException
	 */

	public void parse(String... strs)
	{
		// Create list of lines
		List<String> inLines = new ArrayList<>();
		for (String str : strs)
			inLines.addAll(StringUtils.split(str, '\n'));

		// Parse lines
		lines.clear();
		for (int i = 0; i < inLines.size(); i++)
		{
			try
			{
				lines.add(parseLine(inLines.get(i)));
			}
			catch (ParseException e)
			{
				e.lineNum = i + 1;
				throw e;
			}
		}
	}

	//------------------------------------------------------------------

	/**
	 * @throws UndefinedTagException
	 */

	public void update(Component component)
	{
		update(component, null);
	}

	//------------------------------------------------------------------

	/**
	 * @throws UndefinedTagException
	 */

	public void update(Component component,
					   Font      font)
	{
		Graphics gr = component.getGraphics();
		if (gr == null)
		{
			// Create map of font metrics for font styles
			if (font == null)
				font = component.getFont();
			Map<FontStyle, FontMetrics> fontMetricsMap = new EnumMap<>(FontStyle.class);
			FontStyle style0 = FontStyle.forAwtStyle(font.getStyle());
			for (FontStyle style : FontStyle.values())
			{
				Font fnt = (style == style0) ? font : font.deriveFont(style.getAwtStyle());
				fontMetricsMap.put(style, component.getFontMetrics(fnt));
			}

			// Set text dimensions
			setDimensions(fontMetricsMap);
		}
		else
			update(gr, font);
	}

	//------------------------------------------------------------------

	/**
	 * @throws UndefinedTagException
	 */

	public void update(Graphics gr)
	{
		update(gr, null);
	}

	//------------------------------------------------------------------

	/**
	 * @throws UndefinedTagException
	 */

	public void update(Graphics gr,
					   Font     font)
	{
		// Create copy of graphics context
		gr = gr.create();

		// Set rendering hints for text antialiasing and fractional metrics
		TextRendering.setHints((Graphics2D)gr);

		// Create map of font metrics for font styles
		if (font == null)
			font = gr.getFont();
		Map<FontStyle, FontMetrics> fontMetricsMap = new EnumMap<>(FontStyle.class);
		FontStyle style0 = FontStyle.forAwtStyle(font.getStyle());
		for (FontStyle style : FontStyle.values())
		{
			Font fnt = (style == style0) ? font : font.deriveFont(style.getAwtStyle());
			fontMetricsMap.put(style, gr.getFontMetrics(fnt));
		}

		// Set text dimensions
		setDimensions(fontMetricsMap);
	}

	//------------------------------------------------------------------

	public void draw(Graphics gr,
					 int      verticalMargin,
					 int      horizontalMargin)
	{
		// Create map of fonts for styles
		Font font = gr.getFont();
		FontStyle style0 = FontStyle.forAwtStyle(font.getStyle());
		Map<FontStyle, Font> fontMap = new EnumMap<>(FontStyle.class);
		for (FontStyle style : FontStyle.values())
			fontMap.put(style, (style == style0) ? font : font.deriveFont(style.getAwtStyle()));

		// Create copy of graphics context
		gr = gr.create();

		// Set rendering hints for text antialiasing and fractional metrics
		TextRendering.setHints((Graphics2D)gr);

		// Draw text
		FontMetrics fontMetrics = gr.getFontMetrics(fontMap.get(FontStyle.PLAIN));
		int y0 = verticalMargin;
		int ascent = fontMetrics.getAscent();
		for (Line line : lines)
		{
			int x0 = horizontalMargin;
			for (Line.Element lineElement : line.elements)
			{
				switch (lineElement.getKind())
				{
					case FIELD:
					{
						Field field = (Field)lineElement;
						int x = x0;
						FieldDef fieldDef = (lineElement.index < 0) ? new FieldDef()
																	: fieldDefs.get(lineElement.index);
						if (fieldDef != null)
						{
							switch (fieldDef.alignment)
							{
								case LEFT:
									// do nothing
									break;

								case RIGHT:
									x += field.width - field.elementWidth;
									break;

								case CENTRE:
									x += (field.width - field.elementWidth) / 2;
									break;
							}
							for (Field.Element element : field.elements)
							{
								if (element.getKind() == Field.Element.Kind.SPAN)
								{
									StyleDef styleDef = (element.index < 0)
																		? new StyleDef()
																		: styleDefs.get(element.index);
									if (styleDef != null)
									{
										gr.setFont(fontMap.get(styleDef.fontStyle));
										gr.setColor(styleDef.colour);
										gr.drawString(((Span)element).str, x, y0 + ascent);
									}
								}
								x += element.width;
							}
						}
						x0 += field.width;
						break;
					}

					case PADDING:
					{
						VPaddingDef paddingDef = vPaddingDefs.get(lineElement.index);
						if (paddingDef.lineColour != null)
						{
							int y = y0 + line.height / 2;
							gr.setColor(paddingDef.lineColour);
							gr.drawLine(x0, y, x0 + width - 1, y);
						}
						break;
					}
				}
			}
			y0 += line.height;
		}
	}

	//------------------------------------------------------------------

	/**
	 * @throws ParseException
	 */

	private Line parseLine(String str)
	{
		Line line = new Line();
		Field field = new Field();
		int styleIndex = -1;
		Tag tag = null;
		StringBuilder buffer = new StringBuilder(256);
		int index = 0;
		ParseState state = ParseState.TEXT;
		while (state != ParseState.STOP)
		{
			char ch = (index < str.length()) ? str.charAt(index) : END_OF_LINE;
			switch (state)
			{
				case TEXT:
					if (ch == END_OF_LINE)
					{
						if (!buffer.isEmpty())
						{
							field.elements.add(new Span(styleIndex, buffer.toString()));
							buffer.setLength(0);
						}
						if (!field.isEmpty())
							line.elements.add(field);
						state = ParseState.STOP;
					}
					else
					{
						if (ch == tagPrefix)
							state = ParseState.TAG_START;
						else
							buffer.append(ch);
						++index;
					}
					break;

				case TAG_START:
					if (ch == tagPrefix)
					{
						buffer.append(ch);
						++index;
						state = ParseState.TEXT;
					}
					else
					{
						tag = Tag.get(ch);
						if (tag == null)
							throw new ParseException(ErrorId.TAG_IDENTIFIER_EXPECTED, index);
						if (!buffer.isEmpty())
						{
							field.elements.add(new Span(styleIndex, buffer.toString()));
							buffer.setLength(0);
						}
						++index;
						state = ParseState.TAG_INDEX;
					}
					break;

				case TAG_INDEX:
					if ((ch >= '0') && (ch <= '9'))
					{
						buffer.append(ch);
						++index;
					}
					else
						state = ParseState.TAG_END;
					break;

				case TAG_END:
					if (ch != tagSuffix)
						throw new ParseException(ErrorId.UNCLOSED_TAG, index);
					int tagIndex = -1;
					if (!buffer.isEmpty())
					{
						try
						{
							tagIndex = Integer.parseInt(buffer.toString());
							if ((tagIndex < MIN_TAG_INDEX) || (tagIndex > MAX_TAG_INDEX))
								throw new NumberFormatException();
						}
						catch (NumberFormatException e)
						{
							throw new ParseException(ErrorId.TAG_INDEX_OUT_OF_BOUNDS,
													 index - buffer.length());
						}
						buffer.setLength(0);
					}
					switch (tag)
					{
						case FIELD:
							if (!field.isEmpty())
								line.elements.add(field);
							field = new Field(tagIndex);
							styleIndex = -1;
							break;

						case VERTICAL_PADDING:
							if (!line.isEmpty())
								throw new ParseException(ErrorId.VERTICAL_PADDING_TAG_NOT_ALONE,
														 index - buffer.length());
							line.elements.add(new VPadding(tagIndex));
							break;

						case STYLE:
							styleIndex = tagIndex;
							break;

						case HORIZONTAL_PADDING:
							field.elements.add(new HPadding(tagIndex));
							break;
					}
					++index;
					state = ParseState.TEXT;
					break;

				case STOP:
					// do nothing
					break;
			}
		}
		return line;
	}

	//------------------------------------------------------------------

	/**
	 * @throws UndefinedTagException
	 */

	private void setDimensions(Map<FontStyle, FontMetrics> fontMetricsMap)
	{
		// Set widths and heights of field elements and line elements
		int lineHeight = fontMetricsMap.get(FontStyle.PLAIN).getHeight();
		int em = fontMetricsMap.get(FontStyle.PLAIN).charWidth('m');
		Map<Integer, Integer> fieldWidths = new HashMap<>();
		for (Line line : lines)
		{
			for (Line.Element lineElement : line.elements)
			{
				switch (lineElement.getKind())
				{
					case FIELD:
					{
						Field field = (Field)lineElement;
						if (field.index >= 0)
						{
							if (!fieldDefs.containsKey(field.index))
								throw new UndefinedTagException(Tag.FIELD, field.index);
							if (!field.isEmpty() && !fieldWidths.containsKey(field.index))
								fieldWidths.put(field.index, 0);
						}
						field.elementWidth = 0;
						for (Field.Element fieldElement : field.elements)
						{
							switch (fieldElement.getKind())
							{
								case SPAN:
								{
									StyleDef styleDef = (fieldElement.index < 0)
																	? new StyleDef()
																	: styleDefs.get(fieldElement.index);
									if (styleDef == null)
										throw new UndefinedTagException(Tag.STYLE, fieldElement.index);
									fieldElement.width = fontMetricsMap.get(styleDef.fontStyle).
																	stringWidth(((Span)fieldElement).str);
									break;
								}

								case PADDING:
								{
									HPaddingDef paddingDef = hPaddingDefs.get(fieldElement.index);
									if (paddingDef == null)
										throw new UndefinedTagException(Tag.HORIZONTAL_PADDING,
																		fieldElement.index);
									fieldElement.width = (int)Math.round(paddingDef.width * (float)em);
									break;
								}
							}
							field.elementWidth += fieldElement.width;
						}
						if ((field.index >= 0) && (fieldWidths.get(field.index) < field.elementWidth))
							fieldWidths.put(field.index, field.elementWidth);
						lineElement.height = lineHeight;
						break;
					}

					case PADDING:
					{
						VPaddingDef paddingDef = vPaddingDefs.get(lineElement.index);
						if (paddingDef == null)
							throw new UndefinedTagException(Tag.VERTICAL_PADDING, lineElement.index);
						lineElement.height = (int)Math.round(paddingDef.height * (float)lineHeight);
						break;
					}
				}
			}
		}

		// Set field widths and line widths
		width = 0;
		height = 0;
		for (Line line : lines)
		{
			line.width = 0;
			if (line.isEmpty())
				line.height = lineHeight;
			else
			{
				for (Line.Element lineElement : line.elements)
				{
					if (lineElement.getKind() == Line.Element.Kind.FIELD)
					{
						Field field = (Field)lineElement;
						field.width = fieldWidths.getOrDefault(field.index, field.elementWidth);
						line.width += field.width;
					}
					line.height = lineElement.height;
				}
			}
			if (width < line.width)
				width = line.width;
			height += line.height;
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	char						tagPrefix;
	private	char						tagSuffix;
	private	Map<Integer, FieldDef>		fieldDefs;
	private	Map<Integer, StyleDef>		styleDefs;
	private	Map<Integer, VPaddingDef>	vPaddingDefs;
	private	Map<Integer, HPaddingDef>	hPaddingDefs;
	private	int							width;
	private	int							height;
	private	List<Line>					lines;

}

//----------------------------------------------------------------------
