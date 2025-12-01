/*====================================================================*\

EntropyAccumulator.java

Class: entropy accumulator.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.AWTEvent;
import java.awt.Point;

import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.blankaspect.common.misc.IStringKeyed;

import uk.blankaspect.common.string.StringUtils;

//----------------------------------------------------------------------


// CLASS: ENTROPY ACCUMULATOR


/**
 * This class implements an entropy accumulator for a pseudo-random number generator.
 * <p>
 * The accumulator supports three entropy sources:
 * </p>
 * <ul>
 *   <li>
 *     keyboard : the interval between successive key presses.
 *   </li>
 *   <li>
 *     mouse : a combination of the <i>x</i> and <i>y</i> coordinates of the mouse cursor when the mouse is moved.
 *   </li>
 *   <li>
 *     timer : the value of the high-resolution time source of the Java virtual machine.
 *   </li>
 * </ul>
 * <p>
 * Individual bits can be extracted from samples from the three sources: each source has a configurable bit mask that
 * will be applied to the 16 low-order bits of the sample.  The mouse and timer sources also have a configurable
 * interval: for the mouse, it's the minimum interval between samples; for the timer, it's the interval between samples.
 * </p>
 * <p>
 * Two of the sources (keyboard and timer) depend on the high-resolution time source of the Java virtual machine, which
 * has nanosecond precision but not necessarily nanosecond resolution.  The time value that is used by the two sources
 * is the value of the high-resolution timer divided by a divisor that is a property of an entropy accumulator.  The
 * divisor can be adjusted so that the resolution and precision of the time value are equal in order to maximise the
 * entropy of its low-order bits.  For example, if the JVM's high-resolution time source is based on a 10 MHz hardware
 * timer (ie, a resolution of 100 nanoseconds), a divisor of 100 will reduce the precision of the time value to the
 * resolution.
 * </p>
 * <p>
 * An entropy accumulator gathers entropy from the keyboard and mouse by acting as a listener for keyboard and mouse
 * events.  An instance of this class can be set to receive keyboard and mouse events in two ways:
 * </p>
 * <ul>
 *   <li>
 *     by calling {@link java.awt.Component#addKeyListener(KeyListener)} or {@link
 *     java.awt.Component#addMouseMotionListener(MouseMotionListener)} to receive events from particular components, or
 *   </li>
 *   <li>
 *     by calling {@link java.awt.Toolkit#addAWTEventListener(AWTEventListener, long)}, to receive system-wide events.
 *   </li>
 * </ul>
 * <p>
 * An entropy accumulator maintains a list of consumers of its entropy.  (A consumer is an object that implements the
 * {@link IEntropyConsumer} interface.)  When a byte of entropy is collected from the entropy sources, it is given in
 * turn to the next consumer in the list, which is treated as circular, so that each consumer receives an equal share of
 * the accumulated entropy.
 * </p>
 * @see IEntropyConsumer
 */

public class EntropyAccumulator
	implements AWTEventListener, KeyListener, MouseMotionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The minimum value of the divisor that is applied to values from the high-resolution time source. */
	public static final		int		MIN_TIMER_DIVISOR	= 1;

	/** The maximum value of the divisor that is applied to values from the high-resolution time source. */
	public static final		int		MAX_TIMER_DIVISOR	= 1000000;

	/** The name of the timer thread. */
	private static final	String	TIMER_THREAD_NAME	= "EntropyAccumulator.Timer";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	Map<SourceKind, SourceParams>	sourceParams;
	private	Map<SourceKind, Long>			lastEventTime;
	private	long							timerDivisor;
	private	int								bitBuffer;
	private	int								bitDataLength;
	private	Object							lock;
	private	boolean							timerRunning;
	private	Thread							timerThread;
	private	List<IEntropyConsumer>			entropyConsumers;
	private	int								entropyConsumerIndex;
	private	Map<SourceKind, SourceMetrics>	metrics;

	private volatile	boolean				timerSuspended;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of an entropy accumulator with the specified intervals for the mouse and timer entropy
	 * sources, and with a timer divisor of 1.
	 *
	 * @param  mouseInterval
	 *           the interval (in milliseconds) between successive samples of the mouse source.
	 * @param  timerInterval
	 *           the interval (in milliseconds) between successive samples of the timer source.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code mouseInterval} is less than 2 or greater than 1000, or</li>
	 *             <li>{@code timerInterval} is less than 2 or greater than 1000.</li>
	 *           </ul>
	 */

	public EntropyAccumulator(
		int	mouseInterval,
		int	timerInterval)
	{
		this(getSourceParams(mouseInterval, timerInterval), MIN_TIMER_DIVISOR);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an entropy accumulator with the specified intervals for the mouse and timer entropy
	 * sources, and with a specified timer divisor.
	 *
	 * @param  mouseInterval
	 *           the interval (in milliseconds) between successive samples of the mouse source.
	 * @param  timerInterval
	 *           the interval (in milliseconds) between successive samples of the timer source.
	 * @param  timerDivisor
	 *           the value that will divide values from the system high-resolution timer that are used by the keyboard
	 *           and timer entropy sources.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code mouseInterval} is less than 2 or greater than 1000, or</li>
	 *             <li>{@code timerInterval} is less than 2 or greater than 1000, or.</li>
	 *             <li>{@code timerDivisor} is less than 1 or greater than 1000000.</li>
	 *           </ul>
	 */

	public EntropyAccumulator(
		int	mouseInterval,
		int	timerInterval,
		int	timerDivisor)
	{
		this(getSourceParams(mouseInterval, timerInterval), timerDivisor);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an entropy accumulator with the specified parameters of the entropy sources, and with a
	 * timer divisor of 1.
	 *
	 * @param  sourceParams
	 *           a map of the parameters of the entropy sources.  A source that does not have a map entry is disabled.
	 * @throws IllegalArgumentException
	 *           if any of the parameter values of {@code sourceParams} are invalid.
	 */

	public EntropyAccumulator(
		Map<SourceKind, SourceParams>	sourceParams)
	{
		this(sourceParams, MIN_TIMER_DIVISOR);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an entropy accumulator with the specified parameters of the entropy sources, and with
	 * the specified timer divisor.
	 *
	 * @param  sourceParams
	 *           a map of the parameters of the entropy sources.  A source that does not have a map entry is disabled.
	 * @param  timerDivisor
	 *           the value that will divide values from the system high-resolution timer that are used by the keyboard
	 *           and timer entropy sources.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>any of the parameter values of {@code sourceParams} are invalid, or</li>
	 *             <li>{@code timerDivisor} is less than 1 or greater than 1000000.</li>
	 *           </ul>
	 */

	public EntropyAccumulator(
		Map<SourceKind, SourceParams>	sourceParams,
		int								timerDivisor)
	{
		// Validate arguments
		if ((timerDivisor < MIN_TIMER_DIVISOR) || (timerDivisor > MAX_TIMER_DIVISOR))
			throw new IllegalArgumentException();

		// Initialise instance variables
		this.timerDivisor = timerDivisor;
		lock = new Object();
		entropyConsumers = new ArrayList<>();

		// Initialise remainder of object
		init(sourceParams);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates and returns a map of parameters for three entropy sources, with a default bit mask for each source and
	 * the specified sample intervals for the mouse and timer sources.
	 *
	 * @param  mouseInterval
	 *           the interval (in milliseconds) between successive samples of the mouse source.
	 * @param  timerInterval
	 *           the interval (in milliseconds) between successive samples of the timer source.
	 * @return a map of parameters for three entropy sources, with a default bit mask for each source.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code mouseInterval} is less than 2 or greater than 1000, or</li>
	 *             <li>{@code timerInterval} is less than 2 or greater than 1000.</li>
	 *           </ul>
	 */

	public static Map<SourceKind, SourceParams> getSourceParams(
		int	mouseInterval,
		int	timerInterval)
	{
		if ((mouseInterval < SourceParams.MIN_INTERVAL) || (mouseInterval > SourceParams.MAX_INTERVAL)
			|| (timerInterval < SourceParams.MIN_INTERVAL) || (timerInterval > SourceParams.MAX_INTERVAL))
			throw new IllegalArgumentException();

		Map<SourceKind, SourceParams> sourceParams = new EnumMap<>(SourceKind.class);
		sourceParams.put(SourceKind.KEYBOARD, new SourceParams(SourceParams.DEFAULT_BIT_MASK, 0));
		sourceParams.put(SourceKind.MOUSE,    new SourceParams(SourceParams.DEFAULT_BIT_MASK, mouseInterval));
		sourceParams.put(SourceKind.TIMER,    new SourceParams(SourceParams.DEFAULT_BIT_MASK, timerInterval));
		return sourceParams;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : AWTEventListener interface
////////////////////////////////////////////////////////////////////////

	/**
	 * Processes keyboard and mouse events for use as sources of entropy.
	 */

	@Override
	public void eventDispatched(
		AWTEvent	event)
	{
		// Key event
		if (event instanceof KeyEvent)
		{
			int id = ((KeyEvent)event).getID();
			if ((id == KeyEvent.KEY_PRESSED) || (id == KeyEvent.KEY_RELEASED))
				processKeyEvent();
		}

		// Mouse event
		else if (event instanceof MouseEvent)
		{
			int id = ((MouseEvent)event).getID();
			if ((id == MouseEvent.MOUSE_MOVED) || (id == MouseEvent.MOUSE_DRAGGED))
				processMouseEvent((MouseEvent)event);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : KeyListener interface
////////////////////////////////////////////////////////////////////////

	/**
	 * Extracts entropy from a <i>key pressed</i> event.
	 */

	@Override
	public void keyPressed(
		KeyEvent	event)
	{
		processKeyEvent();
	}

	//------------------------------------------------------------------

	/**
	 * Extracts entropy from a <i>key released</i> event.
	 */

	@Override
	public void keyReleased(
		KeyEvent	event)
	{
		processKeyEvent();
	}

	//------------------------------------------------------------------

	/**
	 * Handles a <i>key typed</i> event.  This method does nothing.
	 */

	@Override
	public void keyTyped(
		KeyEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MouseMotionListener interface
////////////////////////////////////////////////////////////////////////

	/**
	 * Extracts entropy from a <i>mouse dragged</i> event.
	 */

	@Override
	public void mouseDragged(
		MouseEvent	event)
	{
		processMouseEvent(event);
	}

	//------------------------------------------------------------------

	/**
	 * Extracts entropy from a <i>mouse moved</i> event.
	 */

	@Override
	public void mouseMoved(
		MouseEvent	event)
	{
		processMouseEvent(event);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the kinds of entropy source that were specified when this entropy accumulator was created.
	 *
	 * @return the kinds of entropy source that were specified when this entropy accumulator was created.
	 */

	public Set<SourceKind> getSourceKinds()
	{
		return sourceParams.keySet();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the bit mask for the specified entropy source.
	 *
	 * @param  sourceKind
	 *           the kind of entropy source whose bit mask will be returned.
	 * @return the bit mask of {@code sourceKind}, or zero if the kind of source is not enabled.
	 */

	public int getSourceBitMask(
		SourceKind	sourceKind)
	{
		SourceParams params = sourceParams.get(sourceKind);
		return (params == null) ? 0 : params.bitMask;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the sample interval for the specified entropy source.
	 *
	 * @param  sourceKind
	 *           the kind of entropy source whose sample interval will be returned.
	 * @return the sample interval of the specified entropy source, or zero if the kind of source is not enabled.
	 */

	public int getSourceInterval(
		SourceKind	sourceKind)
	{
		SourceParams params = sourceParams.get(sourceKind);
		return (params == null) ? 0 : params.interval;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the metrics of the entropy sources of this entropy accumulator.
	 * <p>
	 * For each entropy source, there are two metrics for each masked bit of the source: the relative frequency of
	 * '1' bits and the relative frequencies of 8-bit sequences.
	 * </p>
	 *
	 * @return the metrics for the entropy sources, or {@code null} if metrics are not enabled.
	 * @see    #clearMetrics()
	 * @see    #setMetricsEnabled(boolean)
	 */

	public Map<SourceKind, Metrics> getMetrics()
	{
		Map<SourceKind, Metrics> outMetrics = null;
		synchronized (lock)
		{
			if (metrics != null)
			{
				outMetrics = new EnumMap<>(SourceKind.class);
				for (SourceKind sourceKind : metrics.keySet())
				{
					SourceMetrics inMetrics = metrics.get(sourceKind);
					int numBits = inMetrics.bits.size();
					double[] oneBitFreqs = new double[numBits];
					if (inMetrics.sampleCount == 0)
						Arrays.fill(oneBitFreqs, -1.0);
					else
					{
						double factor = 1.0 / (double)inMetrics.sampleCount;
						for (int i = 0; i < numBits; i++)
							oneBitFreqs[i] = (double)inMetrics.bits.get(i).oneBitCount * factor;
					}

					double[][] bitSequenceFreqs = new double[numBits][Metrics.NUM_BIT_SEQUENCES];
					if (inMetrics.sampleCount < Metrics.BIT_SEQUENCE_LENGTH)
					{
						for (int i = 0; i < bitSequenceFreqs.length; i++)
							Arrays.fill(bitSequenceFreqs[i], -1.0);
					}
					else
					{
						double factor = 1.0 / (double)(inMetrics.sampleCount - (Metrics.BIT_SEQUENCE_LENGTH - 1));
						for (int i = 0; i < numBits; i++)
						{
							SourceMetrics.Bit bit = inMetrics.bits.get(i);
							for (int j = 0; j < Metrics.NUM_BIT_SEQUENCES; j++)
								bitSequenceFreqs[i][j] = (double)bit.bitSequenceCounts[j] * factor;
						}
					}

					outMetrics.put(sourceKind, new Metrics(inMetrics.bitMask, oneBitFreqs, bitSequenceFreqs));
				}
			}
		}
		return outMetrics;
	}

	//------------------------------------------------------------------

	/**
	 * Clears the metrics of the entropy sources of this entropy accumulator.
	 *
	 * @see #getMetrics()
	 * @see #setMetricsEnabled(boolean)
	 */

	public void clearMetrics()
	{
		if (metrics != null)
		{
			setMetricsEnabled(false);
			setMetricsEnabled(true);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Enables or disables the metrics of the entropy sources of this entropy accumulator.
	 *
	 * @param enabled
	 *          if {@code true}, the metrics of the entropy sources will be enabled; otherwise, they will be disabled.
	 * @see   #getMetrics()
	 * @see   #clearMetrics()
	 */

	public void setMetricsEnabled(
		boolean	enabled)
	{
		synchronized (lock)
		{
			if (enabled)
			{
				if (metrics == null)
				{
					metrics = new EnumMap<>(SourceKind.class);
					for (SourceKind sourceKind : sourceParams.keySet())
						metrics.put(sourceKind, new SourceMetrics(sourceParams.get(sourceKind).bitMask));
				}
			}
			else
				metrics = null;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Suspends the running of the system high-resolution timer entropy source.
	 * <p>
	 * When the timer entropy source is suspended, its thread keeps running.
	 * </p>
	 *
	 * @see #resumeTimer()
	 */

	public void suspendTimer()
	{
		timerSuspended = true;
	}

	//------------------------------------------------------------------

	/**
	 * Resumes the running of the system high-resolution timer entropy source.
	 *
	 * @see #suspendTimer()
	 */

	public void resumeTimer()
	{
		synchronized (lock)
		{
			timerSuspended = false;
			lock.notify();
		}
	}

	//------------------------------------------------------------------

	/**
	 * Adds a specified entropy consumer to this accumulator's list of entropy consumers.
	 *
	 * @param entropyConsumer
	 *          the entropy consumer that will be added to this accumulator's list of entropy consumers.
	 * @see   #removeEntropyConsumer(IEntropyConsumer)
	 */

	public void addEntropyConsumer(
		IEntropyConsumer	entropyConsumer)
	{
		entropyConsumers.add(entropyConsumer);
	}

	//------------------------------------------------------------------

	/**
	 * Removes a specified entropy consumer from this accumulator's list of entropy consumers, and returns the consumer
	 * that is removed.
	 *
	 * @param  entropyConsumer
	 *           the entropy consumer that will be removed from this accumulator's list of entropy consumers.
	 * @return the element that is removed from the list of entropy consumers, or {@code null} if {@code
	 *         entropyConsumer} is not in the list.
	 * @see    #addEntropyConsumer(IEntropyConsumer)
	 */

	public boolean removeEntropyConsumer(
		IEntropyConsumer	entropyConsumer)
	{
		return entropyConsumers.remove(entropyConsumer);
	}

	//------------------------------------------------------------------

	/**
	 * Sets the system high-resolution timer divisor of this accumulator.
	 * <p>
	 * If metrics are enabled, they are reset by this method.
	 * </p>
	 *
	 * @param  timerDivisor
	 *           the value of the divisor that will be set.
	 * @throws IllegalArgumentException
	 *           if {@code timerDivisor} is less than 1 or greater than 1000000.
	 */

	public void setTimerDivisor(
		int	timerDivisor)
	{
		// Validate arguments
		if ((timerDivisor < MIN_TIMER_DIVISOR) || (timerDivisor > MAX_TIMER_DIVISOR))
			throw new IllegalArgumentException();

		// Set timer divisor
		if (this.timerDivisor != timerDivisor)
		{
			// Stop an existing timer thread and wait for it to finish
			stopTimer();

			// Disable metrics
			boolean metricsEnabled = (metrics != null);
			setMetricsEnabled(false);

			// Set instance variable
			this.timerDivisor = timerDivisor;

			// Enable metrics
			if (metricsEnabled)
				setMetricsEnabled(true);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Sets the entropy sources of this accumulator using the specified source parameters.
	 * <p>
	 * If metrics are enabled, they are reset by this method.
	 * </p>
	 *
	 * @param  sourceParams
	 *           a map of the parameters of the entropy sources.  A source that does not have a map entry is disabled.
	 * @throws IllegalArgumentException
	 *           if any of the parameter values of {@code sourceParams} are invalid.
	 */

	public void setSources(
		Map<SourceKind, SourceParams>	sourceParams)
	{
		boolean changed = false;
		if (sourceParams.keySet().equals(this.sourceParams.keySet()))
		{
			for (SourceKind sourceKind : sourceParams.keySet())
			{
				if (!sourceParams.get(sourceKind).equals(this.sourceParams.get(sourceKind)))
				{
					changed = true;
					break;
				}
			}
		}
		else
			changed = true;

		if (changed)
		{
			// Stop an existing timer thread and wait for it to finish
			stopTimer();

			// Initialise object
			init(sourceParams);
		}
	}

	//------------------------------------------------------------------

	private long getHighResolutionTime()
	{
		return System.nanoTime() / timerDivisor;
	}

	//------------------------------------------------------------------

	/**
	 * Initialises this accumulator using the specified source parameters.
	 * <p>
	 * If metrics are enabled, they are reset by this method.
	 * </p>
	 *
	 * @param  sourceParams
	 *           a map of the parameters of the entropy sources.  A source that does not have a map entry is disabled.
	 * @throws IllegalArgumentException
	 *           if any of the parameter values of {@code sourceParams} are invalid.
	 */

	private void init(
		Map<SourceKind, SourceParams>	sourceParams)
	{
		// Validate arguments
		for (SourceKind sourceKind : sourceParams.keySet())
		{
			SourceParams params = sourceParams.get(sourceKind);
			if ((params.bitMask < 0) || (params.bitMask > (1 << SourceParams.BIT_MASK_LENGTH) - 1))
				throw new IllegalArgumentException();
			if (sourceKind.hasInterval
				&& ((params.interval < SourceParams.MIN_INTERVAL) || (params.interval > SourceParams.MAX_INTERVAL)))
				throw new IllegalArgumentException();
		}

		// Disable metrics
		boolean metricsEnabled = (metrics != null);
		setMetricsEnabled(false);

		// Initialise instance variables
		this.sourceParams = new EnumMap<>(sourceParams);
		lastEventTime = new EnumMap<>(SourceKind.class);
		for (SourceKind sourceKind : sourceParams.keySet())
			lastEventTime.put(sourceKind, 0L);

		// Start timer thread
		if (sourceParams.containsKey(SourceKind.TIMER))
		{
			SourceParams params = sourceParams.get(SourceKind.TIMER);
			timerRunning = true;
			timerSuspended = false;
			timerThread = new Thread(() ->
			{
				while (timerRunning)
				{
					// Put thread to sleep; wait while thread is suspended
					try
					{
						Thread.sleep(params.interval);

						synchronized (lock)
						{
							while (timerRunning && timerSuspended)
								lock.wait();
						}
					}
					catch (InterruptedException e)
					{
						// ignore
					}

					// Add bits from high-precision timer to buffer
					addBits(SourceKind.TIMER, (int)getHighResolutionTime(), params.bitMask);
				}
			}, TIMER_THREAD_NAME);
			timerThread.setDaemon(true);
			timerThread.start();
		}

		// Enable metrics
		if (metricsEnabled)
			setMetricsEnabled(true);
	}

	//------------------------------------------------------------------

	/**
	 * Stops the timer thread.
	 */

	private void stopTimer()
	{
		if (timerThread != null)
		{
			timerRunning = false;
			while (timerThread.isAlive())
			{
				// do nothing
			}
			timerThread = null;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Extracts entropy from a <i>key pressed</i> or <i>key released</i> event.
	 */

	private void processKeyEvent()
	{
		final	SourceKind	SOURCE_KIND	= SourceKind.KEYBOARD;

		SourceParams params = sourceParams.get(SOURCE_KIND);
		if (params != null)
		{
			long time = getHighResolutionTime();
			long prevTime = lastEventTime.get(SOURCE_KIND);
			if (prevTime != 0)
				addBits(SOURCE_KIND, (int)(time - prevTime), params.bitMask);
			lastEventTime.put(SOURCE_KIND, time);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Extracts entropy from the specified mouse motion event.
	 *
	 * @param event
	 *          the mouse event.
	 */

	private void processMouseEvent(
		MouseEvent	event)
	{
		final	SourceKind	SOURCE_KIND	= SourceKind.MOUSE;

		SourceParams params = sourceParams.get(SOURCE_KIND);
		if (params != null)
		{
			long time = System.currentTimeMillis();
			if (time - lastEventTime.get(SOURCE_KIND) >= params.interval)
			{
				Point position = event.getLocationOnScreen();
				addBits(SOURCE_KIND, position.x ^ position.y, params.bitMask);
				lastEventTime.put(SOURCE_KIND, time);
			}
		}
	}

	//------------------------------------------------------------------

	/**
	 * Adds bits from an entropy source.
	 * <p>
	 * The bits of entropy are added to a buffer.  When a byte has accumulated, it is given to the next entropy consumer
	 * in this accumulator's list of consumers.
	 * </p>
	 * <p>
	 * The entropy metrics are updated with the bits of entropy that are passed to this method.
	 * </p>
	 *
	 * @param sourceKind
	 *          the kind of source from which the entropy originated.
	 * @param data
	 *          the bits of entropy.
	 * @param mask
	 *          the bit mask that will be applied to {@code data}.
	 */

	private void addBits(
		SourceKind	sourceKind,
		int			data,
		int			mask)
	{
		synchronized (lock)
		{
			int bits = data;
			while (mask != 0)
			{
				if ((mask & 1) != 0)
				{
					bitBuffer <<= 1;
					if ((bits & 1) != 0)
						++bitBuffer;
					++bitDataLength;
					while (bitDataLength >= 8)
					{
						bitDataLength -= 8;
						if (!entropyConsumers.isEmpty())
						{
							if (entropyConsumerIndex >= entropyConsumers.size())
								entropyConsumerIndex = 0;
							entropyConsumers.get(entropyConsumerIndex++)
																	.addRandomByte((byte)(bitBuffer >>> bitDataLength));
						}
					}
				}
				bits >>>= 1;
				mask >>>= 1;
			}

			if (metrics != null)
				metrics.get(sourceKind).update(data);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: KIND OF ENTROPY SOURCE


	/**
	 * This is an enumeration of the kinds of entropy source: keyboard, mouse and timer.
	 */

	public enum SourceKind
		implements IStringKeyed
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		KEYBOARD
		(
			"keyboard",
			false
		),

		MOUSE
		(
			"mouse",
			true
		),

		TIMER
		(
			"timer",
			true
		);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	key;
		private	boolean	hasInterval;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private SourceKind(
			String	key,
			boolean	hasInterval)
		{
			this.key = key;
			this.hasInterval = hasInterval;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IStringKeyed interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the key for this enum constant.
		 *
		 * @return the key for this enum constant.
		 */

		@Override
		public String getKey()
		{
			return key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns a string representation of this enum constant.
		 *
		 * @return a string representation of this enum constant.
		 */

		@Override
		public String toString()
		{
			return StringUtils.firstCharToUpperCase(key);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns {@code true} if the kind of source has a configurable interval.
		 *
		 * @return {@code true} if the kind of source has a configurable interval, {@code false} otherwise.
		 */

		public boolean hasInterval()
		{
			return hasInterval;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member records
////////////////////////////////////////////////////////////////////////


	// RECORD: ENTROPY METRICS


	/**
	 * This class encapsulates some metrics of an entropy source.
	 * <p>
	 * There are two metrics for each masked bit of the entropy source: the relative frequency of '1' bits and the
	 * relative frequencies of 8-bit sequences.
	 * </p>
	 */

	public record Metrics(
		int			bitMask,
		double[]	oneBitFrequencies,
		double[][]	bitSequenceFrequencies)
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		/** The number of bits in a bit sequence. */
		public static final	int	BIT_SEQUENCE_LENGTH	= 8;

		/** The number of bit sequences. */
		public static final	int	NUM_BIT_SEQUENCES	= 1 << BIT_SEQUENCE_LENGTH;

		/** The mask for extracting bit sequences from a buffer of accumulated bits. */
		public static final	int	BIT_SEQUENCE_MASK	= NUM_BIT_SEQUENCES - 1;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: ENTROPY SOURCE PARAMETERS


	/**
	 * This class encapsulates the configurable parameters of an entropy source: a bit mask and millisecond
	 * interval.
	 */

	public static class SourceParams
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		/** The number of low-order bits in the bit mask. */
		public static final	int	BIT_MASK_LENGTH	= 16;

		/** The default bit mask. */
		public static final	int	DEFAULT_BIT_MASK	= 0b1111;

		/** The minimum interval (in milliseconds) between samples. */
		public static final	int	MIN_INTERVAL	= 2;

		/** The maximum interval (in milliseconds) between samples. */
		public static final	int	MAX_INTERVAL	= 1000;

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int	bitMask;
		private	int	interval;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a set of parameters for an entropy source.
		 *
		 * @param bitMask
		 *          the bit mask that will be applied to the sample value.
		 * @param interval
		 *          the interval (in milliseconds) between successive samples.
		 */

		public SourceParams(
			int	bitMask,
			int	interval)
		{
			this.bitMask = bitMask;
			this.interval = interval;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns {@code true} if the specified object is an instance of {@link SourceParams} and its bit mask and
		 * interval are equal to the bit mask and interval of this set of parameters.
		 *
		 * @return {@code true} if {@code obj} is an instance of {@link SourceParams} and its bit mask and interval are
		 *         equal to the bit mask and interval of this set of parameters; otherwise, {@code false}.
		 */

		@Override
		public boolean equals(
			Object	obj)
		{
			if (this == obj)
				return true;

			return (obj instanceof SourceParams other) && (bitMask == other.bitMask) && (interval == other.interval);
		}

		//--------------------------------------------------------------

		/**
		 * Returns a hash code for this set of parameters.
		 *
		 * @return a hash code for this set of parameters.
		 */

		@Override
		public int hashCode()
		{
			return (bitMask << 16) | interval;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: ENTROPY-SOURCE METRICS


	/**
	 * This class encapsulates some metrics of an entropy source.  The class is used only within {@link
	 * EntropyAccumulator}.
	 */

	private static class SourceMetrics
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int			bitMask;
		private	int			sampleCount;
		private	List<Bit>	bits;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private SourceMetrics(
			int	bitMask)
		{
			this.bitMask = bitMask & (1 << SourceParams.BIT_MASK_LENGTH) - 1;
			bits = new ArrayList<>();
			for (int i = 0; i < SourceParams.BIT_MASK_LENGTH; i++)
			{
				if ((bitMask & 1 << i) != 0)
					bits.add(new Bit(i));
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void update(
			int	data)
		{
			++sampleCount;
			for (Bit bit : bits)
			{
				bit.buffer <<= 1;
				if ((data & 1 << bit.index) != 0)
				{
					++bit.buffer;
					++bit.oneBitCount;
				}
				if (sampleCount >= Metrics.BIT_SEQUENCE_LENGTH)
					++bit.bitSequenceCounts[bit.buffer & Metrics.BIT_SEQUENCE_MASK];
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Member classes : non-inner classes
	////////////////////////////////////////////////////////////////////


		// CLASS: BIT METRICS


		private static class Bit
		{

		////////////////////////////////////////////////////////////////
		//  Instance variables
		////////////////////////////////////////////////////////////////

			private	int		index;
			private	int		buffer;
			private	int		oneBitCount;
			private	int[]	bitSequenceCounts;

		////////////////////////////////////////////////////////////////
		//  Constructors
		////////////////////////////////////////////////////////////////

			private Bit(
				int	index)
			{
				this.index = index;
				bitSequenceCounts = new int[Metrics.NUM_BIT_SEQUENCES];
			}

			//----------------------------------------------------------

		}

		//==============================================================

	}

	//==================================================================

}

//----------------------------------------------------------------------
