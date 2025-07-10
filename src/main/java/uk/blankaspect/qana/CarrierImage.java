/*====================================================================*\

CarrierImage.java

Class: carrier image.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.List;

import uk.blankaspect.common.misc.IStringKeyed;

import uk.blankaspect.common.random.Prng01;

import uk.blankaspect.common.range.DoubleRange;
import uk.blankaspect.common.range.IntegerRange;

//----------------------------------------------------------------------


// CLASS: CARRIER IMAGE


class CarrierImage
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		int		NUM_CARRIER_BITS	= 2;

	private static final	IntegerRange	VERTEX_RANGE			= new IntegerRange(3, 7);
	private static final	DoubleRange		NORMAL_OFFSET_RANGE		= new DoubleRange(0.25, 0.75);
	private static final	DoubleRange		NORMAL_RATIO_RANGE		= new DoubleRange(0.2, 1.0);
	private static final	DoubleRange		INITIAL_LENGTH_RANGE	= new DoubleRange(1.5, 3.0);
	private static final	DoubleRange		CONTROL_COORD_RANGE		= new DoubleRange(0.2, 0.8);

	private static final	float	BACKGROUND_SATURATION	= 0.05f;
	private static final	float	BACKGROUND_BRIGHTNESS	= 0.98f;
	private static final	float	SHAPE_SATURATION		= 0.6f;
	private static final	float	SHAPE_BRIGHTNESS		= 0.75f;
	private static final	float	GREEN_FACTOR			= 0.85f;

	private static final	int		ALPHA	= 64;

	private static final	int		MARGIN	= 2;

	private static final	int		RANDOM_MASK	= (1 << NUM_CARRIER_BITS) - 1;
	private static final	int		RGB_MASK	= 0xFF ^ RANDOM_MASK;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	BufferedImage	image;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public CarrierImage(
		int		width,
		int		height,
		int		cellSize,
		Kind	imageKind)
	{
		// Initialise local variables
		int numColumns = 2 * MARGIN + (width + cellSize - 1) / cellSize;
		int numRows = 2 * MARGIN + (height + cellSize - 1) / cellSize;

		DoubleRange initialLengthRange = new DoubleRange(INITIAL_LENGTH_RANGE.lowerBound * (double)cellSize,
														 INITIAL_LENGTH_RANGE.upperBound * (double)cellSize);
		DoubleRange controlCoordRange = new DoubleRange(CONTROL_COORD_RANGE.lowerBound * (double)cellSize,
														CONTROL_COORD_RANGE.upperBound * (double)cellSize);

		Prng01 prng = new Prng01();

		// Create image and graphics context
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = image.createGraphics();

		// Set rendering hints
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
		gr.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);
		gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION,       RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		gr.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		gr.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,     RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		gr.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,      RenderingHints.VALUE_STROKE_PURE);

		// Fill image with background colour
		Color colour = new Color(Color.HSBtoRGB((float)prng.nextDouble(), BACKGROUND_SATURATION,
												BACKGROUND_BRIGHTNESS));
		gr.setColor(colour);
		gr.fillRect(0, 0, width, height);

		// Create and draw shapes
		for (int i = 0; i < numColumns * numRows; i++)
		{
			int numVertices = prng.nextInt(VERTEX_RANGE);
			Shape shape = new Shape(numVertices, imageKind, initialLengthRange, NORMAL_OFFSET_RANGE, NORMAL_RATIO_RANGE,
									controlCoordRange, (float)prng.nextDouble(), prng);

			Rectangle2D bounds = shape.path.getBounds2D();
			int ix = i % numColumns;
			int iy = (i / numColumns) % numRows;
			double dx = (double)((ix - MARGIN) * cellSize) + 0.5 * ((double)cellSize - bounds.getWidth());
			double dy = (double)((iy - MARGIN) * cellSize) + 0.5 * ((double)cellSize - bounds.getHeight());
			shape.translate(dx, dy);

			colour = new Color(Color.HSBtoRGB(shape.hue, SHAPE_SATURATION, SHAPE_BRIGHTNESS));
			colour = new Color(colour.getRed(), (int)StrictMath.round((float)colour.getGreen() * GREEN_FACTOR),
							   colour.getBlue(), ALPHA);
			gr.setColor(colour);
			gr.fill(shape.path);
		}

		// Randomise least significant bits of RGB values
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				int value = prng.nextInt32();
				int rgb = image.getRGB(x, y);
				int r = (rgb >>> 16 & RGB_MASK) | (value & RANDOM_MASK);
				value >>>= NUM_CARRIER_BITS;
				int g = (rgb >>> 8 & RGB_MASK) | (value & RANDOM_MASK);
				value >>>= NUM_CARRIER_BITS;
				int b = (rgb & RGB_MASK) | (value & RANDOM_MASK);
				image.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
			}
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public BufferedImage getImage()
	{
		return image;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: KIND OF IMAGE


	enum Kind
		implements IStringKeyed
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		LINEAR
		(
			"linear",
			"Linear",
			8
		),

		CUBIC1
		(
			"cubic1",
			"Cubic 1",
			6
		),

		CUBIC2
		(
			"cubic2",
			"Cubic 2",
			8
		);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	key;
		private	String	text;
		private	int		cellSizeDivisor;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Kind(
			String	key,
			String	text,
			int		cellSizeDivisor)
		{
			this.key = key;
			this.text = text;
			this.cellSizeDivisor = cellSizeDivisor;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IStringKeyed interface
	////////////////////////////////////////////////////////////////////

		@Override
		public String getKey()
		{
			return key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public String toString()
		{
			return text;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public int getCellSizeDivisor()
		{
			return cellSizeDivisor;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: SHAPE


	private static class Shape
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	Path2D.Double	path;
		private	float			hue;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * @throws IllegalArgumentException
		 */

		private Shape(
			int			numVertices,
			Kind		imageKind,
			DoubleRange	initialLengthRange,
			DoubleRange	normalOffsetRange,
			DoubleRange	normalRatioRange,
			DoubleRange	controlCoordRange,
			float		hue,
			Prng01		prng)
		{
			final	double	TWO_PI				= 2.0 * StrictMath.PI;
			final	double	MIN_DT_FACTOR		= 0.15;
			final	int		MAX_NUM_ATTEMPTS	= 200;

			// Validate arguments
			if ((numVertices < 3) ||
				(initialLengthRange.lowerBound < 0.0) ||
				(initialLengthRange.lowerBound > initialLengthRange.upperBound) ||
				(normalOffsetRange.lowerBound < 0.0) || (normalOffsetRange.lowerBound > 1.0) ||
				(normalOffsetRange.upperBound < 0.0) || (normalOffsetRange.upperBound > 1.0) ||
				(normalOffsetRange.lowerBound > normalOffsetRange.upperBound) ||
				(normalRatioRange.lowerBound < 0.0) ||
				(normalRatioRange.lowerBound > normalRatioRange.upperBound) ||
				((controlCoordRange != null) &&
				 ((controlCoordRange.lowerBound < 0.0) ||
				  (controlCoordRange.lowerBound > controlCoordRange.upperBound))))
				throw new IllegalArgumentException();

			// Initialise instance variables
			this.hue = hue;

			// Add first vertex, P, at (0, 0)
			List<Point2D.Double> vertices = new ArrayList<>();
			Point2D.Double v1 = new Point2D.Double();
			vertices.add(v1);

			// Generate second vertex, Q
			double length = prng.nextDouble(initialLengthRange);
			double angle = prng.nextDouble() * TWO_PI;
			Point2D.Double v2 = new Point2D.Double(length * StrictMath.cos(angle), length * StrictMath.sin(angle));
			vertices.add(v2);

			// Generate third vertex as normal displacement from line segment PQ
			double p = prng.nextDouble(normalOffsetRange);
			double d = prng.nextDouble(normalRatioRange) * length
											* StrictMath.sqrt((p < 0.5) ? p * (2.0 - p) : 1.0 - p * p);
			vertices.add(getNormalVertex(v1, v2, p, -d));

			// Generate remaining vertices
			int numAttempts = 0;
			while (vertices.size() < numVertices)
			{
				// Find longest edge
				int lastIndex = vertices.size() - 1;
				double maxLengthSq = 0.0;
				int index = 0;
				for (int i = 0; i < vertices.size(); i++)
				{
					v1 = vertices.get(i);
					v2 = vertices.get((i == lastIndex) ? 0 : i + 1);
					double lengthSq = v1.distanceSq(v2);
					if (maxLengthSq < lengthSq)
					{
						maxLengthSq = lengthSq;
						index = i;
					}
				}

				// Generate normal from longest edge
				p = prng.nextDouble(normalOffsetRange);
				d = prng.nextDouble(normalRatioRange) * StrictMath.sqrt(maxLengthSq * ((p < 0.5) ? p * (2.0 - p)
																								 : 1.0 - p * p));
				if (prng.nextBoolean())
					d = -0.5 * d;
				v1 = vertices.get(index);
				v2 = vertices.get((index == lastIndex) ? 0 : index + 1);
				Point2D.Double v3 = getNormalVertex(v1, v2, p, d);

				// Test whether either of the new edges intersects an existing edge
				boolean intersects = false;
				for (int i = 0; i < vertices.size(); i++)
				{
					if (i != index)
					{
						Point2D.Double vv1 = vertices.get(i);
						Point2D.Double vv2 = vertices.get((i == lastIndex) ? 0 : i + 1);
						Line2D.Double edge = new Line2D.Double(vv1, vv2);

						if ((!v1.equals(vv2) && edge.intersectsLine(v1.x, v1.y, v3.x, v3.y)) ||
							 (!v2.equals(vv1) && edge.intersectsLine(v3.x, v3.y, v2.x, v2.y)))
						{
							intersects = true;
							break;
						}
					}
				}

				// Insert new vertex into list
				if (intersects)
				{
					if (++numAttempts > MAX_NUM_ATTEMPTS)
						break;
				}
				else
				{
					numAttempts = 0;
					vertices.add(index + 1, v3);
				}
			}

			// Get minimum x and y coordinates
			double minX = Double.MAX_VALUE;
			double minY = Double.MAX_VALUE;
			for (Point2D.Double vertex : vertices)
			{
				if (minX > vertex.x)
					minX = vertex.x;
				if (minY > vertex.y)
					minY = vertex.y;
			}

			// Subtract offset from each coordinate
			for (Point2D.Double vertex : vertices)
			{
				vertex.x -= minX;
				vertex.y -= minY;
			}

			// Create path object from vertices
			path = new Path2D.Double();
			if (imageKind == Kind.LINEAR)
			{
				for (int i = 0; i < vertices.size(); i++)
				{
					Point2D.Double vertex = vertices.get(i);
					if (i == 0)
						path.moveTo(vertex.x, vertex.y);
					else
						path.lineTo(vertex.x, vertex.y);
				}
				Point2D.Double vertex = vertices.get(0);
				path.lineTo(vertex.x, vertex.y);
			}
			else
			{
				double dx = 0.0;
				double dy = 0.0;
				double dx0 = 0.0;
				double dy0 = 0.0;
				double c0x = 0.0;
				double c0y = 0.0;
				double c1x = 0.0;
				double c1y = 0.0;
				for (int i = 0; i < vertices.size(); i++)
				{
					Point2D.Double vertex = vertices.get(i);
					double t = 0.0;
					switch (imageKind)
					{
						case LINEAR:
							// do nothing
							break;

						case CUBIC1:
							t = prng.nextDouble() * TWO_PI;
							break;

						case CUBIC2:
						{
							int iLast = vertices.size() - 1;
							v1 = vertices.get((i == 0) ? iLast : i - 1);
							v2 = vertices.get((i == iLast) ? 0 : i + 1);

							double t1 = StrictMath.atan2(v1.y - vertex.y, v1.x - vertex.x);
							double t2 = StrictMath.atan2(v2.y - vertex.y, v2.x - vertex.x);
							double dt = t2 - t1 - StrictMath.PI;
							while (dt < -StrictMath.PI)
								dt += TWO_PI;
							while (dt >= StrictMath.PI)
								dt -= TWO_PI;
							dt *= MIN_DT_FACTOR + (1.0 - 2.0 * MIN_DT_FACTOR) * prng.nextDouble();
							t = t1 + dt;
							break;
						}
					}

					double r = prng.nextDouble(controlCoordRange);
					dx = r * StrictMath.cos(t);
					dy = r * StrictMath.sin(t);

					c1x = vertex.x + dx;
					c1y = vertex.y + dy;
					if (i == 0)
					{
						dx0 = dx;
						dy0 = dy;
						path.moveTo(vertex.x, vertex.y);
					}
					else
						path.curveTo(c0x, c0y, c1x, c1y, vertex.x, vertex.y);

					c0x = vertex.x - dx;
					c0y = vertex.y - dy;
				}
				Point2D.Double vertex = vertices.get(0);
				c1x = vertex.x + dx0;
				c1y = vertex.y + dy0;
				path.curveTo(c0x, c0y, c1x, c1y, vertex.x, vertex.y);
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static Point2D.Double getNormalVertex(
			Point2D.Double	vertex1,
			Point2D.Double	vertex2,
			double			fraction,
			double			normalLength)
		{
			if (vertex1.equals(vertex2))
				throw new IllegalArgumentException();
			if ((fraction < 0.0) || (fraction > 1.0))
				throw new IllegalArgumentException();

			Point2D.Double normalVertex = null;

			double x1 = vertex1.x;
			double y1 = vertex1.y;
			double x2 = vertex2.x;
			double y2 = vertex2.y;

			double dx = x2 - x1;
			double dy = y2 - y1;
			if (StrictMath.abs(dx) < StrictMath.abs(dy))
			{
				double m = dx / dy;
				double k = normalLength / StrictMath.sqrt(m * m + 1.0);
				if (y1 < y2)
					k = -k;
				normalVertex = new Point2D.Double(x1 + fraction * dx - k, y1 + fraction * dy + m * k);
			}
			else
			{
				double m = dy / dx;
				double k = normalLength / StrictMath.sqrt(m * m + 1.0);
				if (x1 > x2)
					k = -k;
				normalVertex = new Point2D.Double(x1 + fraction * dx + m * k, y1 + fraction * dy - k);
			}

			return normalVertex;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void translate(
			double	deltaX,
			double	deltaY)
		{
			path.transform(AffineTransform.getTranslateInstance(deltaX, deltaY));
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
