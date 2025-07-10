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
import java.util.Objects;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.blankaspect.common.crypto.Fortuna;
import uk.blankaspect.common.crypto.Scrypt;
import uk.blankaspect.common.crypto.ScryptSalsa20;
import uk.blankaspect.common.crypto.StreamEncrypter;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.ui.swing.button.FButton;
import uk.blankaspect.ui.swing.button.FRadioButton;

import uk.blankaspect.ui.swing.combobox.FComboBox;

import uk.blankaspect.ui.swing.dialog.RunnableMessageDialog;

import uk.blankaspect.ui.swing.label.FLabel;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.spinner.FIntegerSpinner;

import uk.blankaspect.ui.swing.text.TextRendering;

//----------------------------------------------------------------------


// KEY DERIVATION FUNCTION PARAMETER PANEL CLASS


class KdfParameterPanel
	extends JPanel
	implements ActionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int		COST_FIELD_LENGTH				= 2;
	private static final	int		NUM_BLOCKS_FIELD_LENGTH			= 3;
	private static final	int		NUM_SUPERBLOCKS_FIELD_LENGTH	= 2;
	private static final	int		MAX_NUM_THREADS_FIELD_LENGTH	= 2;

	private static final	Color	PANEL_BORDER_COLOUR	= new Color(160, 184, 160);

	private static final	String	NUM_ROUNDS_STR			= "Number of rounds";
	private static final	String	COST_STR				= "CPU/memory cost";
	private static final	String	NUM_BLOCKS_STR			= "Number of blocks";
	private static final	String	NUM_SUPERBLOCKS_STR		= "Number of parallel superblocks";
	private static final	String	MAX_NUM_THREADS_STR		= "Maximum number of threads";
	private static final	String	GENERATE_KEY_STR		= "Generate a test key";
	private static final	String	GENERATING_KEY_STR		= "Generating a test key " + AppConstants.ELLIPSIS_STR;
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
	//  Instance variables
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
	//  Class variables
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

		private static final	int		VERTICAL_MARGIN		= 3;
		private static final	int		HORIZONTAL_MARGIN	= 6;

		private static final	Color	TEXT_COLOUR			= Color.BLACK;
		private static final	Color	BACKGROUND_COLOUR	= new Color(248, 240, 200);
		private static final	Color	BORDER_COLOUR		= new Color(224, 200, 160);

		private static final	String	PROTOTYPE_STR	= "0".repeat(10);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private TimeField()
		{
			// Set font
			AppFont.TEXT_FIELD.apply(this);

			// Set preferred size
			FontMetrics fontMetrics = getFontMetrics(getFont());
			int width = 2 * HORIZONTAL_MARGIN + fontMetrics.stringWidth(PROTOTYPE_STR);
			int height = 2 * VERTICAL_MARGIN + fontMetrics.getAscent() + fontMetrics.getDescent();
			setPreferredSize(new Dimension(width, height));

			// Set properties
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
			if (!Objects.equals(text, this.text))
			{
				this.text = text;
				repaint();
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	text;

	}

	//==================================================================


	// KEY GENERATOR CLASS


	private static class KeyGenerator
		implements Runnable
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

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
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void run()
		{
			try
			{
				long startTime = System.currentTimeMillis();
				new ScryptSalsa20(kdfParams.numRounds)
								.deriveKey(key, salt, kdfParams, kdfParams.getNumThreads(), KeyList.DERIVED_KEY_SIZE);
				time = System.currentTimeMillis() - startTime;
			}
			catch (OutOfMemoryError e)
			{
				outOfMemory = true;
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class variables
	////////////////////////////////////////////////////////////////////

		private static	byte[]	key		= Fortuna.keyStringToBytes(PASSPHRASE_STR);
		private static	byte[]	salt	= QanaApp.INSTANCE.getRandomBytes(KeyList.SALT_SIZE);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
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
		// Initialise instance variables
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
		numRoundsComboBox = new FComboBox<>(Scrypt.CoreHashNumRounds.values());
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
		costSpinner = new FIntegerSpinner(params.getCost(), StreamEncrypter.KdfParams.MIN_COST,
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
		numBlocksSpinner = new FIntegerSpinner(params.getNumBlocks(), StreamEncrypter.KdfParams.MIN_NUM_BLOCKS,
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

		// Label: number of superblocks
		JLabel numSuperblocksLabel = new FLabel(NUM_SUPERBLOCKS_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(numSuperblocksLabel, gbc);
		paramPanel.add(numSuperblocksLabel);

		// Spinner: number of superblocks
		numSuperblocksSpinner = new FIntegerSpinner(params.getNumSuperblocks(),
													StreamEncrypter.KdfParams.MIN_NUM_SUPERBLOCKS,
													StreamEncrypter.KdfParams.MAX_NUM_SUPERBLOCKS,
													NUM_SUPERBLOCKS_FIELD_LENGTH);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(numSuperblocksSpinner, gbc);
		paramPanel.add(numSuperblocksSpinner);

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

	@Override
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
											 numSuperblocksSpinner.getIntValue(),
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
		costSpinner.setIntValue(params.getCost());
		numBlocksSpinner.setIntValue(params.getNumBlocks());
		numSuperblocksSpinner.setIntValue(params.getNumSuperblocks());
		maxNumThreadsSpinner.setIntValue(params.maxNumThreads);
	}

	//------------------------------------------------------------------

	private void onGenerateKey()
	{
		try
		{
			KeyGenerator keyGenerator = new KeyGenerator(getParams());
			RunnableMessageDialog.showDialog(this, GENERATING_KEY_STR, keyGenerator);
			if (keyGenerator.outOfMemory)
				throw new AppException(ErrorId.NOT_ENOUGH_MEMORY);
			generationTimeField.setText(Long.toString(keyGenerator.time));
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, QanaApp.SHORT_NAME, JOptionPane.ERROR_MESSAGE);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	KdfUse									kdfUse;
	private	Map<KdfUse, StreamEncrypter.KdfParams>	paramMap;
	private	Map<KdfUse, FRadioButton>				kdfUseRadioButtons;
	private	FComboBox<Scrypt.CoreHashNumRounds>		numRoundsComboBox;
	private	FIntegerSpinner							costSpinner;
	private	FIntegerSpinner							numBlocksSpinner;
	private	FIntegerSpinner							numSuperblocksSpinner;
	private	FIntegerSpinner							maxNumThreadsSpinner;
	private	TimeField								generationTimeField;

}

//----------------------------------------------------------------------
