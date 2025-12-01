/*====================================================================*\

StreamConcealer.java

Class: stream concealer.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.image.BufferedImage;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.TaskCancelledException;

import uk.blankaspect.common.misc.IProgressListener;

import uk.blankaspect.common.number.NumberUtils;

//----------------------------------------------------------------------


// CLASS: STREAM CONCEALER


/**
 * This class implements a mechanism for concealing a data stream in an image.
 * <p>
 * The data that is to be concealed is called the <i>payload</i>, and the image in which it will be concealed is called
 * the <i>carrier</i>.  In this class, the carrier image is represented by an RGB colour model in which a pixel consists
 * of R(ed), G(reen) and B(lue) components, each of which is an 8-bit value.  The payload is concealed by replacing
 * low-order bits of the RGB colour components of the carrier with bits of the payload, up to a specified number of bits
 * per component (the maximum replacement depth).  The replacements are spread evenly throughout the image with a
 * modified form of the Bresenham line-drawing algorithm.
 * </p>
 * <p>
 * Bits of the RGB components of the carrier that are not replaced by the payload may optionally be replaced by random
 * bits, up to the maximum replacement depth, to disguise the presence of the payload as dither.  Random data is
 * supplied by a class that implements the {@link IRandomSource} interface.
 * </p>
 * <p>
 * In order for a concealed payload to be recovered from an image, its length must be known by the recovery algorithm.
 * To achieve this, the length of the payload is concealed in the least significant bits of the RGB components at the
 * start of the image.  The payload length is encoded and decoded by classes that implement the {@link ILengthEncoder}
 * and {@link ILengthDecoder} interfaces respectively.
 * </p>
 */

public class StreamConcealer
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The minimum value of the maximum number of bits per RGB colour component that will be replaced by the
		payload. */
	public static final		int		MIN_MAX_REPLACEMENT_DEPTH	= 1;

	/** The maximum value of the maximum number of bits per RGB colour component that will be replaced by the
		payload. */
	public static final		int		MAX_MAX_REPLACEMENT_DEPTH	= 6;

	/** The maximum number of bits in the binary representation of the length of a payload. */
	public static final		int		MAX_NUM_LENGTH_BITS	= 24;

	/** The maximum length (in bytes) of a payload. */
	public static final		int		MAX_PAYLOAD_LENGTH	= (1 << MAX_NUM_LENGTH_BITS) - 1;

	private static final	int		BYTES_PER_PIXEL		= 3;
	private static final	int		RGB_INITIAL_SHIFT	= (BYTES_PER_PIXEL - 1) * 8;

	/** The maximum size (in bytes) of a carrier image that is represented by 8-bit RGB colour components. */
	public static final		int		MAX_CARRIER_SIZE	= MAX_PAYLOAD_LENGTH / BYTES_PER_PIXEL;

	private static final	int		BUFFER_LENGTH			= 1 << 16;  // 65536
	private static final	int		RANDOM_BUFFER_LENGTH	= 1 << 12;  // 4096

	private static final	String	DATA_STR	= "data";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	List<IProgressListener>	progressListeners;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an instance of {@link StreamConcealer}.
	 */

	public StreamConcealer()
	{
		progressListeners = new ArrayList<>();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Encodes a specified length of a payload with a specified encoder and conceals the encoded length in a
	 * specified image.
	 *
	 * @param image
	 *          the image in which the length will be concealed.
	 * @param length
	 *          the length that will be concealed.
	 * @param lengthEncoder
	 *          the object that will encode the length as an array of bytes.
	 * @see   #recoverLength(BufferedImage, ILengthDecoder)
	 */

	public static void concealLength(
		BufferedImage	image,
		int				length,
		ILengthEncoder	lengthEncoder)
	{
		int width = image.getWidth();
		int numPixels = width * image.getHeight();
		int lengthFieldNumBits = lengthEncoder.getLengthFieldNumBits(numPixels);
		byte[] lengthData = lengthEncoder.encodeLength(length, numPixels);
		int x = 0;
		int y = 0;
		int rgb = 0;
		int shift = RGB_INITIAL_SHIFT;
		for (int i = 0; i < lengthFieldNumBits; i++)
		{
			int pixelIndex = i / BYTES_PER_PIXEL;
			x = pixelIndex % width;
			y = pixelIndex / width;
			if (shift == RGB_INITIAL_SHIFT)
				rgb = image.getRGB(x, y);

			int mask = 1 << shift;
			if ((lengthData[i >>> 3] & 1 << (i & 0x07)) == 0)
				rgb &= ~mask;
			else
				rgb |= mask;

			shift -= 8;
			if (shift < 0)
			{
				shift = RGB_INITIAL_SHIFT;
				image.setRGB(x, y, rgb);
			}
		}
		if (shift < RGB_INITIAL_SHIFT)
			image.setRGB(x, y, rgb);
	}

	//------------------------------------------------------------------

	/**
	 * Extracts the encoded length of a payload from a specified image, decodes the length with a specified
	 * decoder and returns the length.
	 *
	 * @param  image
	 *           the image from which the length will be recovered.
	 * @param  lengthDecoder
	 *           the object that will decode the length from an array of bytes.
	 * @return the length that was recovered from the image and decoded.
	 * @see    #concealLength(BufferedImage, int, ILengthEncoder)
	 */

	public static int recoverLength(
		BufferedImage	image,
		ILengthDecoder	lengthDecoder)
	{
		int width = image.getWidth();
		int numPixels = width * image.getHeight();
		int lengthFieldNumBits = lengthDecoder.getLengthFieldNumBits(numPixels);
		byte[] lengthData = new byte[NumberUtils.roundUpQuotientInt(lengthFieldNumBits, Byte.SIZE)];
		int shift = RGB_INITIAL_SHIFT;
		int rgb = 0;
		for (int i = 0; i < lengthFieldNumBits; i++)
		{
			int pixelIndex = i / BYTES_PER_PIXEL;
			int x = pixelIndex % width;
			int y = pixelIndex / width;
			if (shift == RGB_INITIAL_SHIFT)
				rgb = image.getRGB(x, y);

			if ((rgb & 1 << shift) != 0)
				lengthData[i >>> 3] |= 1 << (i & 0x07);

			shift -= 8;
			if (shift < 0)
				shift = RGB_INITIAL_SHIFT;
		}
		return lengthDecoder.decodeLength(lengthData, numPixels);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Adds a specified progress listener to this concealer's list of progress listeners.
	 *
	 * @param listener
	 *          the progress listener that will be added to the list.
	 * @see   #removeProgressListener(IProgressListener)
	 * @see   #progressListeners()
	 */

	public void addProgressListener(
		IProgressListener	listener)
	{
		progressListeners.add(listener);
	}

	//------------------------------------------------------------------

	/**
	 * Removes a specified progress listener from this concealer's list of progress listeners.
	 * <p>
	 * The removal operation has no effect if the specified listener is not in the list.
	 * </p>
	 *
	 * @param listener
	 *          the progress listener that will be removed from the list.
	 * @see   #addProgressListener(IProgressListener)
	 * @see   #progressListeners()
	 */

	public void removeProgressListener(
		IProgressListener	listener)
	{
		progressListeners.remove(listener);
	}

	//------------------------------------------------------------------

	/**
	 * Returns this concealer's list of progress listeners.
	 *
	 * @return the list of progress listeners as an array.
	 * @see    #addProgressListener(IProgressListener)
	 * @see    #removeProgressListener(IProgressListener)
	 */

	public IProgressListener[] progressListeners()
	{
		return progressListeners.toArray(IProgressListener[]::new);
	}

	//------------------------------------------------------------------

	/**
	 * Conceals data read from a specified input stream (the <i>payload</i>) in a specified image (the
	 * <i>carrier</i>), and returns the resulting image as a newly allocated object.
	 *
	 * @param  inStream
	 *           the input stream from which the payload will be read.
	 * @param  image
	 *           the image in which the payload will be concealed.
	 * @param  length
	 *           the length (in bytes) of the payload.
	 * @param  lengthEncoder
	 *           the object that will encode the length of the payload as an array of bytes.
	 * @param  maxReplacementDepth
	 *           the maximum number of bits per RGB colour component of {@code image} that will be replaced by the
	 *           payload.
	 * @param  randomSource
	 *           a source of random data for replacing bits of the RGB components of {@code image} that are not replaced
	 *           by the payload, up to {@code maxReplacementDepth}.  If {@code randomSource} is {@code null}, no carrier
	 *           bits will be replaced by random data.
	 * @return a newly allocated image in which the payload is concealed.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code length} is negative or greater than 2<sup>24</sup> - 1 (16777215), or</li>
	 *             <li>{@code maxReplacementDepth} is less than 1 or greater than 6.</li>
	 *           </ul>
	 * @throws InputException
	 *           if an error occurs when reading from the input stream.
	 * @throws TaskCancelledException
	 *           if the concealment operation was cancelled by the user.
	 * @see    #conceal(IInput, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 * @see    #recover(BufferedImage, IOutput, ILengthDecoder)
	 * @see    #recover(BufferedImage, OutputStream, ILengthDecoder)
	 */

	public BufferedImage conceal(
		InputStream		inStream,
		BufferedImage	image,
		int				length,
		ILengthEncoder	lengthEncoder,
		int				maxReplacementDepth,
		IRandomSource	randomSource)
		throws InputException, TaskCancelledException
	{
		return conceal(new InputStreamAdapter(inStream), image, length, lengthEncoder, maxReplacementDepth,
					   randomSource);
	}

	//------------------------------------------------------------------

	/**
	 * Conceals data read from a specified input (the <i>payload</i>) in a specified image (the
	 * <i>carrier</i>), and returns the resulting image as a newly allocated object.
	 *
	 * @param  input
	 *           the input from which the payload will be read.
	 * @param  image
	 *           the image in which the payload will be concealed.
	 * @param  length
	 *           the length (in bytes) of the payload.
	 * @param  lengthEncoder
	 *           the object that will encode the length of the payload as an array of bytes.
	 * @param  maxReplacementDepth
	 *           the maximum number of bits per RGB colour component of {@code image} that will be replaced by the
	 *           payload.
	 * @param  randomSource
	 *           a source of random data for replacing bits of the RGB components of {@code image} that are not replaced
	 *           by the payload, up to {@code maxReplacementDepth}.  If {@code randomSource} is {@code null}, no carrier
	 *           bits will be replaced by random data.
	 * @return a newly allocated image in which the payload is concealed.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code length} is negative or greater than 2<sup>24</sup> - 1 (16777215), or</li>
	 *             <li>{@code maxReplacementDepth} is less than 1 or greater than 6.</li>
	 *           </ul>
	 * @throws InputException
	 *           if an error occurs when reading from the input.
	 * @throws TaskCancelledException
	 *           if the concealment operation was cancelled by the user.
	 * @see    #conceal(InputStream, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 * @see    #recover(BufferedImage, IOutput, ILengthDecoder)
	 * @see    #recover(BufferedImage, OutputStream, ILengthDecoder)
	 */

	public BufferedImage conceal(
		IInput			input,
		BufferedImage	image,
		int				length,
		ILengthEncoder	lengthEncoder,
		int				maxReplacementDepth,
		IRandomSource	randomSource)
		throws InputException, TaskCancelledException
	{
		// Validate arguments
		if ((length < 0) || (length > MAX_PAYLOAD_LENGTH)
			|| (maxReplacementDepth < MIN_MAX_REPLACEMENT_DEPTH) || (maxReplacementDepth > MAX_MAX_REPLACEMENT_DEPTH))
			throw new IllegalArgumentException();

		// Test size of image
		int width = image.getWidth();
		int height = image.getHeight();
		long imageSize = width * height;
		if (imageSize > MAX_CARRIER_SIZE)
			throw new InputException(ErrorId.IMAGE_IS_TOO_LARGE);

		// Test whether image is large enough to contain input data
		int numPayloadBits = length * 8;
		int lengthFieldNumBits = lengthEncoder.getLengthFieldNumBits((int)imageSize);
		imageSize *= BYTES_PER_PIXEL;
		imageSize -= lengthFieldNumBits;
		if (imageSize * maxReplacementDepth < numPayloadBits)
			throw new InputException(ErrorId.IMAGE_IS_TOO_SMALL);

		// Create output image
		BufferedImage outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		// Initialise variables of Bresenham algorithm: deltas, decision threshold and increments
		int numImageBytes = (int)imageSize;
		int payloadBitsPerImageByte = numPayloadBits / numImageBytes;
		int remainder = numPayloadBits % numImageBytes;
		int dx = numImageBytes - 1;
		int dy = remainder;
		int d = 2 * dy - dx;
		int inc0 = 2 * dy;
		int inc1 = 2 * (dy - dx);

		// Initialise random source
		int randomMask = 0;
		byte[] randomBuffer = null;
		int randomIndex = 0;
		int randomLength = 0;
		if (randomSource != null)
		{
			int numRandomBits = payloadBitsPerImageByte;
			if (remainder > 0)
				++numRandomBits;
			randomMask = (1 << numRandomBits) - 1;
			randomBuffer = new byte[RANDOM_BUFFER_LENGTH];
		}

		// Set input data in least significant bits of RGB values of image
		byte[] buffer = new byte[BUFFER_LENGTH];
		int numBits = 0;
		int bitBuffer = 0;
		int bitDataLength = 0;
		int imageOffset = -lengthFieldNumBits;
		int blockIndex = 0;
		int blockLength = 0;
		int offset = 0;
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				// Test whether task has been cancelled by a monitor
				for (IProgressListener listener : progressListeners)
				{
					if (listener.isTaskCancelled())
						throw new TaskCancelledException();
				}

				// Get RGB value of pixel
				int rgb = image.getRGB(x, y);

				// Set input data in RGB components of pixel
				int shift = RGB_INITIAL_SHIFT;
				for (int i = 0; i < BYTES_PER_PIXEL; i++)
				{
					// Get bits from bit buffer, reading data from input if necessary
					if (bitDataLength < numBits)
					{
						while ((blockIndex >= blockLength) && (offset < length))
						{
							// Read data from input
							blockIndex = 0;
							blockLength = Math.min(length - offset, BUFFER_LENGTH);
							try
							{
								blockLength = input.read(buffer, 0, blockLength);
								if (blockLength < 0)
									throw new InputException(ErrorId.PREMATURE_END_OF_DATA);
							}
							catch (IOException e)
							{
								throw new InputException(ErrorId.ERROR_READING_DATA);
							}
							offset += blockLength;
						}
						bitBuffer <<= 8;
						bitBuffer |= buffer[blockIndex++] & 0xFF;
						bitDataLength += 8;
					}

					// Set random bits in RGB value
					if (randomSource != null)
					{
						while (randomIndex >= randomLength)
						{
							randomIndex = 0;
							randomLength = randomSource.getRandomBytes(randomBuffer, 0, randomBuffer.length);
						}
						rgb &= ~(randomMask << shift);
						rgb |= (randomBuffer[randomIndex++] & randomMask) << shift;
					}

					// Set payload bits in RGB value
					if (numBits > 0)
					{
						bitDataLength -= numBits;
						int mask = (1 << numBits) - 1;
						rgb &= ~(mask << shift);
						rgb |= (bitBuffer >>> bitDataLength & mask) << shift;
					}

					// Set number of bits of next payload sample and increment decision threshold
					if (++imageOffset >= 0)
					{
						numBits = payloadBitsPerImageByte;
						if (imageOffset > 0)
						{
							if (d > 0)
							{
								d += inc1;
								++numBits;
							}
							else
								d += inc0;
						}
					}

					// Decrement RGB shift
					shift -= 8;
				}

				// Set RGB value of pixel in output image
				outImage.setRGB(x, y, rgb);

				// Update progress of task
				double progress = (double)Math.max(0, imageOffset) / (double)imageSize;
				for (IProgressListener listener : progressListeners)
					listener.setProgress(progress);
			}
		}

		// Set length in RGB values at start of image
		concealLength(outImage, length, lengthEncoder);

		return outImage;
	}

	//------------------------------------------------------------------

	/**
	 * Recovers concealed data (the <i>payload</i>) from a specified image and writes the recovered data to
	 * a specified output stream.
	 *
	 * @param  image
	 *           the image from which the concealed data will be recovered.
	 * @param  outStream
	 *           the output stream to which the recovered data will be written.
	 * @param  lengthDecoder
	 *           the object that will decode the length of the payload from an array of bytes.
	 * @throws InputException
	 *           if an error occurs when recovering the concealed data.
	 * @throws OutputException
	 *           if an error occurs when writing to the output stream.
	 * @throws TaskCancelledException
	 *           if the recovery operation was cancelled by the user.
	 * @see    #recover(BufferedImage, IOutput, ILengthDecoder)
	 * @see    #conceal(IInput, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 * @see    #conceal(InputStream, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 */

	public void recover(
		BufferedImage	image,
		OutputStream	outStream,
		ILengthDecoder	lengthDecoder)
		throws InputException, OutputException, TaskCancelledException
	{
		recover(image, new OutputStreamAdapter(outStream), lengthDecoder);
	}

	//------------------------------------------------------------------

	/**
	 * Recovers concealed data (the <i>payload</i>) from a specified image and writes the recovered data to
	 * a specified output.
	 *
	 * @param  image
	 *           the image from which the concealed data will be recovered.
	 * @param  output
	 *           the output to which the recovered data will be written.
	 * @param  lengthDecoder
	 *           the object that will decode the length of the payload from an array of bytes.
	 * @throws InputException
	 *           if an error occurs when recovering the concealed data.
	 * @throws OutputException
	 *           if an error occurs when writing to the output.
	 * @throws TaskCancelledException
	 *           if the recovery operation was cancelled by the user.
	 * @see    #recover(BufferedImage, OutputStream, ILengthDecoder)
	 * @see    #conceal(IInput, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 * @see    #conceal(InputStream, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 */

	public void recover(
		BufferedImage	image,
		IOutput			output,
		ILengthDecoder	lengthDecoder)
		throws InputException, OutputException, TaskCancelledException
	{
		// Test size of image
		int width = image.getWidth();
		int height = image.getHeight();
		long imageSize = width * height;
		if (imageSize > MAX_CARRIER_SIZE)
			throw new InputException(ErrorId.IMAGE_IS_TOO_LARGE);
		int lengthFieldNumBits = lengthDecoder.getLengthFieldNumBits((int)imageSize);
		imageSize *= BYTES_PER_PIXEL;
		imageSize -= lengthFieldNumBits;
		if (imageSize < 0)
			throw new InputException(ErrorId.IMAGE_IS_TOO_SMALL);

		// Recover length from RGB values at start of image
		int length = recoverLength(image, lengthDecoder);

		// Test length of payload
		if (length > imageSize)
			throw new InputException(ErrorId.UNEXPECTED_DATA_FORMAT);

		// Initialise variables of Bresenham algorithm: deltas, decision threshold and increments
		int numPayloadBits = length * 8;
		int numImageBytes = (int)imageSize;
		int payloadBitsPerImageByte = numPayloadBits / numImageBytes;
		int remainder = numPayloadBits % numImageBytes;
		int dx = numImageBytes - 1;
		int dy = remainder;
		int d = 2 * dy - dx;
		int inc0 = 2 * dy;
		int inc1 = 2 * (dy - dx);

		// Extract output data from least significant bits of RGB values of image
		byte[] buffer = new byte[BUFFER_LENGTH];
		int numBits = 0;
		int bitBuffer = 0;
		int bitDataLength = 0;
		int imageOffset = -lengthFieldNumBits;
		int bufferIndex = 0;
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				// Test whether task has been cancelled by a monitor
				for (IProgressListener listener : progressListeners)
				{
					if (listener.isTaskCancelled())
						throw new TaskCancelledException();
				}

				// Get RGB value of pixel
				int rgb = image.getRGB(x, y);

				// Extract payload data from RGB components of pixel
				int shift = RGB_INITIAL_SHIFT;
				for (int i = 0; i < BYTES_PER_PIXEL; i++)
				{
					// Extract payload bits from RGB component and add them to bit buffer
					if (numBits > 0)
					{
						bitBuffer <<= numBits;
						bitBuffer |= rgb >> shift & ((1 << numBits) - 1);
						bitDataLength += numBits;
					}

					// Write buffered data to output
					while (bitDataLength >= 8)
					{
						bitDataLength -= 8;
						buffer[bufferIndex++] = (byte)(bitBuffer >>> bitDataLength);
						if (bufferIndex >= buffer.length)
						{
							try
							{
								output.write(buffer, 0, buffer.length);
							}
							catch (IOException e)
							{
								throw new OutputException(ErrorId.ERROR_WRITING_DATA);
							}
							bufferIndex = 0;
						}
					}

					// Set number of bits of next payload sample and increment decision threshold
					if (++imageOffset >= 0)
					{
						numBits = payloadBitsPerImageByte;
						if (imageOffset > 0)
						{
							if (d > 0)
							{
								d += inc1;
								++numBits;
							}
							else
								d += inc0;
						}
					}

					// Decrement RGB shift
					shift -= 8;
				}

				// Update progress of task
				double progress = (double)Math.max(0, imageOffset) / (double)imageSize;
				for (IProgressListener listener : progressListeners)
					listener.setProgress(progress);
			}
		}

		// Write residual data to output
		if (bufferIndex > 0)
		{
			try
			{
				output.write(buffer, 0, bufferIndex);
			}
			catch (IOException e)
			{
				throw new OutputException(ErrorId.ERROR_WRITING_DATA);
			}
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		ERROR_READING_DATA
		("An error occurred when reading the %1."),

		ERROR_WRITING_DATA
		("An error occurred when writing the %1."),

		PREMATURE_END_OF_DATA
		("The end of the %1 was reached prematurely when reading the %1."),

		IMAGE_IS_TOO_LARGE
		("The image is too large to be used as a carrier."),

		IMAGE_IS_TOO_SMALL
		("The image is too small to conceal the input %1."),

		UNEXPECTED_ID
		("The image does not contain concealed data with the expected ID."),

		UNEXPECTED_DATA_FORMAT
		("The image does not contain concealed data in the expected format.");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ErrorId(
			String	message)
		{
			this.message = message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : AppException.IId interface
	////////////////////////////////////////////////////////////////////

		@Override
		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member interfaces
////////////////////////////////////////////////////////////////////////


	// INTERFACE: INPUT


	/**
	 * This interface specifies an input to an concealment or recovery operation.
	 *
	 * @see StreamConcealer.IOutput
	 * @see StreamConcealer#conceal(IInput, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 * @see StreamConcealer#recover(BufferedImage, IOutput, ILengthDecoder)
	 */

	public interface IInput
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Reads data from the input up to a specified length and stores it in a buffer.
		 *
		 * @param  buffer
		 *           the buffer in which the data will be stored.
		 * @param  offset
		 *           the offset in {@code buffer} at which the first byte of data will be stored.
		 * @param  length
		 *           the maximum number of bytes to read.
		 * @return the number of bytes that were read from the input.
		 * @throws IOException
		 *           if an error occurs when reading from the input.
		 */

		int read(
			byte[]	buffer,
			int		offset,
			int		length)
			throws IOException;

		//--------------------------------------------------------------

	}

	//==================================================================


	// INTERFACE: OUTPUT


	/**
	 * This interface specifies an output from an concealment or recovery operation.
	 *
	 * @see StreamConcealer.IInput
	 * @see StreamConcealer#conceal(IInput, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 * @see StreamConcealer#recover(BufferedImage, IOutput, ILengthDecoder)
	 */

	public interface IOutput
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Writes a specifed number of bytes of data to the output.
		 *
		 * @param  data
		 *           the data to be written.
		 * @param  offset
		 *           the start offset of the data in {@code data}.
		 * @param  length
		 *           the number of bytes to write.
		 * @throws IOException
		 *           if an error occurs when writing to the output.
		 */

		void write(
			byte[]	data,
			int		offset,
			int		length)
			throws IOException;

		//--------------------------------------------------------------

	}

	//==================================================================


	// INTERFACE: LENGTH ENCODER


	/**
	 * This interface specifies the methods that must be implemented an encoder of the length of a concealed payload.
	 *
	 * @see StreamConcealer.ILengthDecoder
	 * @see StreamConcealer#conceal(IInput, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 * @see StreamConcealer#conceal(InputStream, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 */

	public interface ILengthEncoder
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		int getLengthFieldNumBits(
			int	numPixels);

		//--------------------------------------------------------------

		byte[] encodeLength(
			int	length,
			int	numPixels);

		//--------------------------------------------------------------

	}

	//==================================================================


	// INTERFACE: LENGTH DECODER


	/**
	 * This interface specifies the methods that must be implemented by a decoder of the length of a concealed payload.
	 *
	 * @see StreamConcealer.ILengthEncoder
	 * @see StreamConcealer#recover(BufferedImage, IOutput, ILengthDecoder)
	 * @see StreamConcealer#recover(BufferedImage, OutputStream, ILengthDecoder)
	 */

	public interface ILengthDecoder
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		int getLengthFieldNumBits(
			int	numPixels);

		//--------------------------------------------------------------

		int decodeLength(
			byte[]	data,
			int		numPixels);

		//--------------------------------------------------------------

	}

	//==================================================================


	// INTERFACE: RANDOM SOURCE


	/**
	 * This functional interface specifies the method that must be implemented by a provider of random data for
	 * replacing bits of the RGB colour components of a carrier that are not replaced by the payload, up to the maximum
	 * replacement depth.
	 */

	@FunctionalInterface
	public interface IRandomSource
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Provides random bytes up to a specified length.
		 *
		 * @param  buffer
		 *           the buffer in which the random data will be stored.
		 * @param  offset
		 *           the offset in {@code buffer} at which the first byte of random data will be stored.
		 * @param  length
		 *           the maximum number of bytes to store.
		 * @return the number of bytes that were stored in the buffer.
		 */

		int getRandomBytes(
			byte[]	buffer,
			int		offset,
			int		length);

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: INPUT EXCEPTION


	/**
	 * This class encapsulates an exception that occurs when reading from an input stream.
	 *
	 * @see OutputException
	 */

	public static class InputException
		extends ConcealerException
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private InputException(
			ErrorId	id)
		{
			super(id);
		}

		//--------------------------------------------------------------

		private InputException(
			ErrorId		id,
			Throwable	cause)
		{
			super(id, cause);
		}

		//--------------------------------------------------------------

		private InputException(
			ErrorId	id,
			String	str)
		{
			super(id, str);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: OUTPUT EXCEPTION


	/**
	 * This class encapsulates an exception that occurs when writing to an output stream.
	 *
	 * @see InputException
	 */

	public static class OutputException
		extends ConcealerException
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private OutputException(
			ErrorId	id)
		{
			super(id);
		}

		//--------------------------------------------------------------

		private OutputException(
			ErrorId		id,
			Throwable	cause)
		{
			super(id, cause);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: CONCEALER EXCEPTION


	/**
	 * This is the base class of the {@link InputException} and {@link OutputException} classes.
	 *
	 * @see InputException
	 * @see OutputException
	 */

	private static abstract class ConcealerException
		extends AppException
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ConcealerException(
			ErrorId	id)
		{
			super(id, DATA_STR);
		}

		//--------------------------------------------------------------

		private ConcealerException(
			ErrorId		id,
			Throwable	cause)
		{
			super(id, cause, DATA_STR);
		}

		//--------------------------------------------------------------

		private ConcealerException(
			ErrorId	id,
			String	str)
		{
			super(id, DATA_STR, str);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Sets the description of data in the message of an exception.
		 * <p>
		 * By default, the data that is processed by an concealment or recovery operation is described as 'data' in the
		 * message of an exception.  This method allows the description to be replaced by something more specific to the
		 * kind of data stream; eg, 'file'.
		 * </p>
		 *
		 * @param description
		 *          the description that will be applied to data in the message of an exception.
		 */

		public void setDataDescription(
			String	description)
		{
			setReplacement(0, description);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: INPUT STREAM ADAPTER


	/**
	 * This class translates the interface of the {@link java.io.InputStream InputStream} class into the
	 * {@link IInput} interface so that {@code InputStream} can be used as a parameter type for a
	 * concealment operation.
	 *
	 * @see StreamConcealer#conceal(InputStream, BufferedImage, int, ILengthEncoder, int, IRandomSource)
	 */


	private static class InputStreamAdapter
		implements IInput
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	InputStream	inputStream;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates an adapter to wrap a specified input stream.
		 *
		 * @param inStream
		 *          the input stream that will be translated by this implementation of {@link IInput}.
		 */

		private InputStreamAdapter(
			InputStream	inStream)
		{
			inputStream = inStream;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IInput interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Reads data from the input stream up to a specified length, and stores it in a buffer.
		 *
		 * @param  buffer
		 *           the buffer in which the data will be stored.
		 * @param  offset
		 *           the offset in {@code buffer} at which the first byte of data will be stored.
		 * @param  length
		 *           the maximum number of bytes to read.
		 * @return the number of bytes that were read from the input stream.
		 * @throws IOException
		 *           if an error occurs when reading from the input stream.
		 */

		@Override
		public int read(
			byte[]	buffer,
			int		offset,
			int		length)
			throws IOException
		{
			return inputStream.read(buffer, offset, length);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: OUTPUT STREAM ADAPTER


	/**
	 * This class translates the interface of the {@link java.io.OutputStream OutputStream} class into the
	 * {@link IOutput} interface so that {@code OutputStream} can be used as a parameter type for a recovery
	 * operation.
	 *
	 * @see StreamConcealer#recover(BufferedImage, OutputStream, ILengthDecoder)
	 */

	private static class OutputStreamAdapter
		implements IOutput
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	OutputStream	outputStream;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates an adapter to wrap a specified output stream.
		 *
		 * @param outStream
		 *          the output stream that will be translated by this implementation of {@link IOutput}.
		 */

		private OutputStreamAdapter(
			OutputStream	outStream)
		{
			outputStream = outStream;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IOutput interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Writes data from an array of bytes to the output stream.
		 *
		 * @param  data
		 *           the array of data to be written.
		 * @param  offset
		 *           the start offset of the data in {@code data}.
		 * @param  length
		 *           the number of bytes to write.
		 * @throws IOException
		 *           if an error occurs when writing to the output stream.
		 */

		@Override
		public void write(
			byte[]	data,
			int		offset,
			int		length)
			throws IOException
		{
			outputStream.write(data, offset, length);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
