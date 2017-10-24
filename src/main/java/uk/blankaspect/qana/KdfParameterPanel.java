/*====================================================================*\

KdfParameterPanel.java

Key derivation function parameter panel class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.blankaspect.common.crypto.Fortuna;
import uk.blankaspect.common.crypto.Scrypt;
import uk.blankaspect.common.crypto.StreamEncrypter;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.gui.FButton;
import uk.blankaspect.common.gui.FComboBox;
import uk.blankaspect.common.gui.FIntegerSpinner;
import uk.blankaspect.common.gui.FLabel;
import uk.blankaspect.common.gui.FRadioButton;
import uk.blankaspect.common.gui.GuiUtils;
import uk.blankaspect.common.gui.RunnableMessageDialog;
import uk.blankaspect.common.gui.TextRendering;

import uk.blankaspect.common.misc.StringUtils;

//----------------------------------------------------------------------


// KEY DERIVATION FUNCTION PARAMETER PANEL CLASS


class KdfParameterPanel
	extends JPanel
	implements ActionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		COST_FIELD_LENGTH					= 2;
	private static final	int		NUM_BLOCKS_FIELD_LENGTH				= 3;
	private static final	int		NUM_PARALLEL_BLOCKS_FIELD_LENGTH	= 2;
	private static final	int		MAX_NUM_THREADS_FIELD_LENGTH		= 2;

	private static final	Color	PANEL_BORDER_COLOUR	= new Color(160, 184, 160);

	private static final	String	NUM_ROUNDS_STR			= "Number of rounds";
	private static final	String	COST_STR				= "CPU/memory cost";
	private static final	String	NUM_BLOCKS_STR			= "Number of blocks";
	private static final	String	NUM_PARALLEL_BLOCKS_STR	= "Number of parallel superblocks";
	private static final	String	MAX_NUM_THREADS_STR		= "Maximum number of threads";
	private static final	String	GENERATE_KEY_STR		= "Generate a test key";
	private static final	String	KEY_GENERATION_TIME_STR	= "Key generation time";
	private static final	String	MS_STR					= "ms";

	// Commands
	private interface Command
	{
		String	SELECT_PARAMETER_KIND	= "selectParameterKind";
		String	GENERATE_KEY			= "generateKey";
	}

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		NOT_ENOUGH_MEMORY
		("There was not enough memory to generate the key.");

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
	//  Instance fields
	////////////////////////////////////////////////////////////////////

		private	String	message;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// RADIO BUTTON CLASS


	private static class RadioButton
		extends FRadioButton
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	Color	BACKGROUND_COLOUR	= new Color(252, 224, 128);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private RadioButton(String text,
							int    mnemonicKey)
		{
			super(text);
			setMnemonic(mnemonicKey);
			setActionCommand(Command.SELECT_PARAMETER_KIND);
			if (buttonGroup == null)
				buttonGroup = new ButtonGroup();
			buttonGroup.add(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public Color getBackground()
		{
			return (isSelected() ? BACKGROUND_COLOUR : super.getBackground());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class fields
	////////////////////////////////////////////////////////////////////

		private static	ButtonGroup	buttonGroup;

	}

	//==================================================================


	// TIME FIELD CLASS


	private static class TimeField
		extends JComponent
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	VERTICAL_MARGIN		= 3;
		private static final	int	HORIZONTAL_MARGIN	= 6;

		private static final	int	NUM_DIGITS	= 10;

		private static final	Color	TEXT_COLOUR			= Color.BLACK;
		private static final	Color	BACKGROUND_COLOUR	= new Color(248, 240, 200);
		private static final	Color	BORDER_COLOUR		= new Color(224, 200, 160);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private TimeField()
		{
			// Set font
			AppFont.TEXT_FIELD.apply(this);

			// Set preferred size
			FontMetrics fontMetrics = getFontMetrics(getFont());
			String prototypeStr = StringUtils.createCharString('0', NUM_DIGITS);
			int width = 2 * HORIZONTAL_MARGIN + fontMetrics.stringWidth(prototypeStr);
			int height = 2 * VERTICAL_MARGIN + fontMetrics.getAscent() + fontMetrics.getDescent();
			setPreferredSize(new Dimension(width, height));

			// Set attributes
			setOpaque(true);
			setFocusable(false);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected void paintComponent(Graphics gr)
		{
			// Create copy of graphics context
			gr = gr.create();

			// Get dimensions
			int width = getWidth();
			int height = getHeight();

			// Draw background
			gr.setColor(BACKGROUND_COLOUR);
			gr.fillRect(0, 0, width, height);

			// Draw text
			if (text != null)
			{
				// Set rendering hints for text antialiasing and fractional metrics
				TextRendering.setHints((Graphics2D)gr);

				// Draw text
				FontMetrics fontMetrics = gr.getFontMetrics();
				gr.setColor(TEXT_COLOUR);
				gr.drawString(text, width - HORIZONTAL_MARGIN - fontMetrics.stringWidth(text),
							  VERTICAL_MARGIN + fontMetrics.getAscent());
			}

			// Draw border
			gr.setColor(BORDER_COLOUR);
			gr.drawRect(0, 0, width - 1, height - 1);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void setText(String text)
		{
			if (!StringUtils.equal(text, this.text))
			{
				this.text = text;
				repaint();
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance fields
	////////////////////////////////////////////////////////////////////

		private	String	text;

	}

	//==================================================================


	// KEY GENERATOR CLASS


	private static class KeyGenerator
		implements RunnableMessageDialog.IRunnable
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	MESSAGE_STR		= "Generating a test key " + AppConstants.ELLIPSIS_STR;
		private static final	String	PASSPHRASE_STR	= "passphrase";

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private KeyGenerator(StreamEncrypter.KdfParams kdfParams)
		{
			this.kdfParams = kdfParams;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : RunnableMessageDialog.IRunnable interface
	////////////////////////////////////////////////////////////////////

		@Override
		public String getMessage()
		{
			return MESSAGE_STR;
		}

		//--------------------------------------------------------------

		@Override
		public void run()
		{
			try
			{
				long startTime = System.currentTimeMillis();
				Scrypt.setSalsa20CoreNumRounds(kdfParams.numRounds);
				Scrypt.deriveKey(key, salt, kdfParams, kdfParams.getNumThreads(),
								 KeyList.DERIVED_KEY_SIZE);
				time = System.currentTimeMillis() - startTime;
			}
			catch (OutOfMemoryError e)
			{
				outOfMemory = true;
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class fields
	////////////////////////////////////////////////////////////////////

		private static	byte[]	key		= Fortuna.keyStringToBytes(PASSPHRASE_STR);
		private static	byte[]	salt	= App.INSTANCE.getRandomBytes(KeyList.SALT_SIZE);

	////////////////////////////////////////////////////////////////////
	//  Instance fields
	////////////////////////////////////////////////////////////////////

		private	StreamEncrypter.KdfParams	kdfParams;
		private	long						time;
		private	boolean						outOfMemory;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public KdfParameterPanel(KdfUse                                 kdfUse,
							  Map<KdfUse, StreamEncrypter.KdfParams> paramMap)
	{
		// Initialise instance fields
		this.kdfUse = kdfUse;
		this.paramMap = new EnumMap<>(paramMap);
		StreamEncrypter.KdfParams params = paramMap.get(kdfUse);


		//----  KDF use panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel kdfUsePanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(kdfUsePanel, 2, 6, PANEL_BORDER_COLOUR);

		int gridY = 0;

		// Radio buttons: KDF use
		kdfUseRadioButtons = new EnumMap<>(KdfUse.class);
		for (KdfUse use : KdfUse.values())
		{
			RadioButton radioButton = new RadioButton(use.toString(), use.getMnemonicKey());
			radioButton.setSelected(use == kdfUse);
			radioButton.addActionListener(this);
			kdfUseRadioButtons.put(use, radioButton);

			gbc.gridx = 0;
			gbc.gridy = gridY++;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(radioButton, gbc);
			kdfUsePanel.add(radioButton);
		}


		//----  Parameter panel

		JPanel paramPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(paramPanel, PANEL_BORDER_COLOUR);

		gridY = 0;

		// Label: number of rounds
		JLabel numRoundsLabel = new FLabel(NUM_ROUNDS_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(numRoundsLabel, gbc);
		paramPanel.add(numRoundsLabel);

		// Combo box: number of rounds
		numRoundsComboBox = new FComboBox<>(Scrypt.Salsa20NumRounds.values());
		numRoundsComboBox.setSelectedValue(params.numRounds);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(numRoundsComboBox, gbc);
		paramPanel.add(numRoundsComboBox);

		// Label: cost
		JLabel costLabel = new FLabel(COST_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(costLabel, gbc);
		paramPanel.add(costLabel);

		// Spinner: cost
		costSpinner = new FIntegerSpinner(params.cost, StreamEncrypter.KdfParams.MIN_COST,
										  StreamEncrypter.KdfParams.MAX_COST, COST_FIELD_LENGTH);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(costSpinner, gbc);
		paramPanel.add(costSpinner);

		// Label: number of blocks
		JLabel numBlocksLabel = new FLabel(NUM_BLOCKS_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(numBlocksLabel, gbc);
		paramPanel.add(numBlocksLabel);

		// Spinner: number of blocks
		numBlocksSpinner = new FIntegerSpinner(params.numBlocks, StreamEncrypter.KdfParams.MIN_NUM_BLOCKS,
											   StreamEncrypter.KdfParams.MAX_NUM_BLOCKS,
											   NUM_BLOCKS_FIELD_LENGTH);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(numBlocksSpinner, gbc);
		paramPanel.add(numBlocksSpinner);

		// Label: number of parallel superblocks
		JLabel numParallelElementsLabel = new FLabel(NUM_PARALLEL_BLOCKS_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(numParallelElementsLabel, gbc);
		paramPanel.add(numParallelElementsLabel);

		// Spinner: number of parallel superblocks
		numParallelBlocksSpinner = new FIntegerSpinner(params.numParallelBlocks,
													   StreamEncrypter.KdfParams.MIN_NUM_PARALLEL_BLOCKS,
													   StreamEncrypter.KdfParams.MAX_NUM_PARALLEL_BLOCKS,
													   NUM_PARALLEL_BLOCKS_FIELD_LENGTH);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(numParallelBlocksSpinner, gbc);
		paramPanel.add(numParallelBlocksSpinner);

		// Label: maximum number of threads
		JLabel numThreadsLabel = new FLabel(MAX_NUM_THREADS_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(numThreadsLabel, gbc);
		paramPanel.add(numThreadsLabel);

		// Spinner: maximum number of threads
		maxNumThreadsSpinner = new FIntegerSpinner(params.maxNumThreads,
												   StreamEncrypter.KdfParams.MIN_MAX_NUM_THREADS,
												   StreamEncrypter.KdfParams.MAX_MAX_NUM_THREADS,
												   MAX_NUM_THREADS_FIELD_LENGTH);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(maxNumThreadsSpinner, gbc);
		paramPanel.add(maxNumThreadsSpinner);


		//----  Control panel

		JPanel controlPanel = new JPanel(gridBag);

		int gridX = 0;

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(kdfUsePanel, gbc);
		controlPanel.add(kdfUsePanel);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 4, 0, 0);
		gridBag.setConstraints(paramPanel, gbc);
		controlPanel.add(paramPanel);


		//----  Test panel

		JPanel testPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(testPanel, PANEL_BORDER_COLOUR);

		gridX = 0;

		// Button: generate key
		JButton generateButton = new FButton(GENERATE_KEY_STR);
		generateButton.setActionCommand(Command.GENERATE_KEY);
		generateButton.addActionListener(this);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(generateButton, gbc);
		testPanel.add(generateButton);

		// Label: generation time
		JLabel generationTimeLabel = new FLabel(KEY_GENERATION_TIME_STR);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 12, 0, 0);
		gridBag.setConstraints(generationTimeLabel, gbc);
		testPanel.add(generationTimeLabel);

		// Field: key generation time
		generationTimeField = new TimeField();

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 6, 0, 0);
		gridBag.setConstraints(generationTimeField, gbc);
		testPanel.add(generationTimeField);

		// Label: ms
		JLabel msLabel = new FLabel(MS_STR);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 4, 0, 0);
		gridBag.setConstraints(msLabel, gbc);
		testPanel.add(msLabel);


		//----  Outer panel

		// Set layout manager
		setLayout(gridBag);

		gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(controlPanel, gbc);
		add(controlPanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(4, 0, 0, 0);
		gridBag.setConstraints(testPanel, gbc);
		add(testPanel);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();

		if (command.equals(Command.SELECT_PARAMETER_KIND))
			onSelectParameterKind();

		else if (command.equals(Command.GENERATE_KEY))
			onGenerateKey();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public KdfUse getKdfUse()
	{
		return kdfUse;
	}

	//------------------------------------------------------------------

	public Map<KdfUse, StreamEncrypter.KdfParams> getParameterMap()
	{
		paramMap.put(kdfUse, getParams());
		return Collections.unmodifiableMap(paramMap);
	}

	//------------------------------------------------------------------

	private StreamEncrypter.KdfParams getParams()
	{
		return new StreamEncrypter.KdfParams(numRoundsComboBox.getSelectedValue(),
											 costSpinner.getIntValue(), numBlocksSpinner.getIntValue(),
											 numParallelBlocksSpinner.getIntValue(),
											 maxNumThreadsSpinner.getIntValue());
	}

	//------------------------------------------------------------------

	private void onSelectParameterKind()
	{
		paramMap.put(kdfUse, getParams());

		for (KdfUse kdfUse : kdfUseRadioButtons.keySet())
		{
			if (kdfUseRadioButtons.get(kdfUse).isSelected())
			{
				this.kdfUse = kdfUse;
				break;
			}
		}

		StreamEncrypter.KdfParams params = paramMap.get(kdfUse);
		numRoundsComboBox.setSelectedValue(params.numRounds);
		costSpinner.setIntValue(params.cost);
		numBlocksSpinner.setIntValue(params.numBlocks);
		numParallelBlocksSpinner.setIntValue(params.numParallelBlocks);
		maxNumThreadsSpinner.setIntValue(params.maxNumThreads);
	}

	//------------------------------------------------------------------

	private void onGenerateKey()
	{
		try
		{
			KeyGenerator keyGenerator = new KeyGenerator(getParams());
			RunnableMessageDialog.showDialog(this, keyGenerator);
			if (keyGenerator.outOfMemory)
				throw new AppException(ErrorId.NOT_ENOUGH_MEMORY);
			generationTimeField.setText(Long.toString(keyGenerator.time));
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, App.SHORT_NAME, JOptionPane.ERROR_MESSAGE);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance fields
////////////////////////////////////////////////////////////////////////

	private	KdfUse									kdfUse;
	private	Map<KdfUse, StreamEncrypter.KdfParams>	paramMap;
	private	Map<KdfUse, FRadioButton>				kdfUseRadioButtons;
	private	FComboBox<Scrypt.Salsa20NumRounds>		numRoundsComboBox;
	private	FIntegerSpinner							costSpinner;
	private	FIntegerSpinner							numBlocksSpinner;
	private	FIntegerSpinner							numParallelBlocksSpinner;
	private	FIntegerSpinner							maxNumThreadsSpinner;
	private	TimeField								generationTimeField;

}

//----------------------------------------------------------------------
