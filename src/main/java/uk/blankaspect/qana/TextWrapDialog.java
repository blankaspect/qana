/*====================================================================*\

TextWrapDialog.java

Text wrap dialog box class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.string.StringUtils;

import uk.blankaspect.common.swing.action.KeyAction;

import uk.blankaspect.common.swing.button.FButton;

import uk.blankaspect.common.swing.checkbox.FCheckBox;

import uk.blankaspect.common.swing.colour.Colours;

import uk.blankaspect.common.swing.combobox.EditableComboBox;
import uk.blankaspect.common.swing.combobox.UnsignedIntegerComboBox;

import uk.blankaspect.common.swing.font.FontStyle;
import uk.blankaspect.common.swing.font.FontUtils;

import uk.blankaspect.common.swing.label.FLabel;

import uk.blankaspect.common.swing.misc.GuiUtils;

import uk.blankaspect.common.swing.text.TaggedText;

import uk.blankaspect.common.swing.textfield.ConstrainedTextField;

import uk.blankaspect.common.tuple.IntegerPair;

//----------------------------------------------------------------------


// TEXT WRAP DIALOG BOX CLASS


class TextWrapDialog
	extends JDialog
	implements ActionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	MIN_INDENT	= 0;
	private static final	int	MAX_INDENT	= 999;

	private static final	Color	HELP_PANEL_BACKGROUND_COLOUR	= Color.WHITE;
	private static final	Color	HELP_PANEL_BORDER_COLOUR		= Colours.LINE_BORDER;

	private static final	String	WRAP_TEXT_STR	= "Wrap text";
	private static final	String	LINE_LENGTH_STR	= "Line length";
	private static final	String	INDENT_STR		= "Indent";
	private static final	String	HELP_STR		= "Help";
	private static final	String	WRAP_STR		= "Wrap";

	private static final	String[]	HELP_TEXT_STRS	=
	{
		"{s0}Line length",
		"{v0}",
		"{s1}If the line-length field is empty or the line length is zero, each paragraph of the",
		"{s1}text is {s5}unwrapped{s1} onto a single line.",
		"{v1}",
		"{s1}If the line length is greater than zero, each paragraph of the text is wrapped so",
		"{s1}that no line, including any indent, is longer than the specified length, unless the",
		"{s1}line contains no spaces.",
		"{v1}",
		"{s0}Indent",
		"{v0}",
		"{s1}The indent specifier consists of either a single argument or two arguments",
		"{s1}separated by a comma.  The arguments may be either absolute or relative values.",
		"{v1}",
		"{s1}If the indent specifier has only one argument, it applies to all the lines in a",
		"{s1}paragraph.",
		"{v1}",
		"{s1}If the indent specifier has two arguments, the first argument applies to the first",
		"{s1}line of the paragraph, and the second argument applies to the remaining lines.",
		"{v1}",
		"{s1}In the following descriptions of the arguments, {s3}n{s1} denotes a decimal number",
		"{s1}between 0 and 999 inclusive, and [{s2}+{s1}|{s2}\u2212{s1}] denotes either a {s5}plus{s1} " +
			"or {s5}minus{s1} character.",
		"{v1}",
		"{s4}First argument:",
		"{h0}{f0}{s3}n{s}{f}{h1}Absolute indent",
		"{h0}{f0}{s2}${f}{h1}The current indent of the first line",
		"{h0}{f0}{s2}${s}[{s2}+{s}|{s2}\u2212{s}]{s3}n{s}{f}{h1}Relative to the current indent of the " +
			"first line",
		"{v1}",
		"{s4}Second argument:",
		"{h0}{f0}{s3}n{s}{f}{h1}Absolute indent",
		"{h0}{f0}{s2}${f}{h1}The current indent of the first line",
		"{h0}{f0}{s2}${s}[{s2}+{s}|{s2}\u2212{s}]{s3}n{s}{f}{h1}Relative to the current indent of the " +
			"first line",
		"{h0}{f0}[{s2}+{s}|{s2}\u2212{s}]{s3}n{s}{f}{h1}Relative to the indent of the first line after " +
		"the first argument is applied"
	};

	private static final	char[]	MINUS_CHARS	=
	{
		'\u2212',   // minus sign
		'\u2012',   // figure dash
		'\u2013'    // en dash
	};

	private static final	List<TaggedText.FieldDef>		FIELD_DEFS		= Arrays.asList
	(
		new TaggedText.FieldDef(0)
	);
	private static final	List<TaggedText.VPaddingDef>	V_PADDING_DEFS	= Arrays.asList
	(
		new TaggedText.VPaddingDef(0, 0.2f, new Color(160, 192, 160)),
		new TaggedText.VPaddingDef(1, 0.5f)
	);
	private static final	List<TaggedText.StyleDef>		STYLE_DEFS		= Arrays.asList
	(
		new TaggedText.StyleDef(0, FontStyle.BOLD, new Color(0, 64, 64)),
		new TaggedText.StyleDef(1, new Color(32, 32, 32)),
		new TaggedText.StyleDef(2, FontStyle.BOLD, new Color(192, 64, 0)),
		new TaggedText.StyleDef(3, FontStyle.BOLD_ITALIC, new Color(0, 96, 128)),
		new TaggedText.StyleDef(4, FontStyle.BOLD_ITALIC, new Color(0, 96, 0)),
		new TaggedText.StyleDef(5, FontStyle.ITALIC, new Color(64, 64, 80))
	);
	private static final	List<TaggedText.HPaddingDef>	H_PADDING_DEFS	= Arrays.asList
	(
		new TaggedText.HPaddingDef(0, 1.0f),
		new TaggedText.HPaddingDef(1, 1.2f)
	);

	// Commands
	private interface Command
	{
		String	TOGGLE_INDENT	= "toggleIndent";
		String	HELP			= "help";
		String	ACCEPT			= "accept";
		String	CLOSE			= "close";
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

		LINE_LENGTH_OUT_OF_BOUNDS
		("The line length must be between " + TextDocument.MIN_WRAP_LINE_LENGTH + " and " +
			TextDocument.MAX_WRAP_LINE_LENGTH + "."),

		INVALID_INDENT
		("The indent is invalid."),

		FIRST_INDENT_OUT_OF_BOUNDS
		("The first indent is out of bounds.\n" +
			"The absolute value of the indent must be between " + MIN_INDENT + " and " + MAX_INDENT + "."),

		SECOND_INDENT_OUT_OF_BOUNDS
		("The second indent is out of bounds.\n" +
			"The absolute value of the indent must be between " + MIN_INDENT + " and " + MAX_INDENT + "."),

		LINE_LENGTH_AND_FIRST_INDENT_OUT_OF_ORDER
		("The first indent is greater than or equal to the line length."),

		LINE_LENGTH_AND_SECOND_INDENT_OUT_OF_ORDER
		("The second indent is greater than or equal to the line length.");

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


	// RESULT CLASS


	public static class Result
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Result(int lineLength,
					   int indent1,
					   int indent2)
		{
			this.lineLength = lineLength;
			this.indent1 = indent1;
			this.indent2 = indent2;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		int	lineLength;
		int	indent1;
		int	indent2;

	}

	//==================================================================


	// LINE LENGTH COMBO BOX CLASS


	private static class LineLengthComboBox
		extends UnsignedIntegerComboBox
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	FIELD_LENGTH	= 3;
		private static final	int	MAX_NUM_ITEMS	= 32;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private LineLengthComboBox(List<Integer> items)
		{
			super(FIELD_LENGTH, MAX_NUM_ITEMS, items);
			setDefaultComparator();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public int getValue()
		{
			return (isEmpty() ? 0 : super.getValue());
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// INDENT FIELD CLASS


	private static class IndentField
		extends ConstrainedTextField
		implements EditableComboBox.IEditor
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	FIELD_LENGTH	= 11;

		private static final	char	BASE_VALUE_PREFIX_CHAR	= '$';

		private static final	String	VALID_CHARS	= "$+,-0123456789";

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private IndentField()
		{
			super(FIELD_LENGTH);
			AppFont.TEXT_FIELD.apply(this);
			GuiUtils.setTextComponentMargins(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		/**
		 * @throws NumberFormatException
		 */

		private static int parseRelativeValue(String str,
											  int    baseValue)
		{
			int value = baseValue;
			if (!str.isEmpty())
			{
				int index = 0;
				switch (str.charAt(0))
				{
					case '+':
						if ((str.length() < 2) || (str.charAt(1) < '0') || (str.charAt(1) > '9'))
							throw new NumberFormatException();
						++index;
						break;

					case '-':
						break;

					default:
						throw new NumberFormatException();
				}
				value += Integer.parseInt(str.substring(index));
			}
			return value;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : EditableComboBox.IEditor interface
	////////////////////////////////////////////////////////////////////

		public Component getEditorComponent()
		{
			return this;
		}

		//--------------------------------------------------------------

		public Object getItem()
		{
			return getText();
		}

		//--------------------------------------------------------------

		public void setItem(Object obj)
		{
			setText((obj == null) ? null : obj.toString());
		}

		//--------------------------------------------------------------

		public int getFieldWidth()
		{
			return (getColumns() * getColumnWidth());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected boolean acceptCharacter(char ch,
										  int  index)
		{
			return (VALID_CHARS.indexOf(ch) >= 0);
		}

		//--------------------------------------------------------------

		@Override
		protected int getColumnWidth()
		{
			return (FontUtils.getCharWidth('0', getFontMetrics(getFont())) + 1);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * @throws NumberFormatException
		 */

		private IntegerPair getValue(int baseValue)
		{
			int value1 = 0;
			int value2 = 0;

			if (!isEmpty())
			{
				// Split string
				List<String> strs = StringUtils.split(getText(), ',');
				if (strs.size() > 2)
					throw new NumberFormatException();

				// Parse first component
				String str = strs.get(0);
				char ch = str.charAt(0);
				if ((ch == '-') || (ch == '+'))
					throw new NumberFormatException();
				value1 = (ch == BASE_VALUE_PREFIX_CHAR) ? parseRelativeValue(str.substring(1), baseValue)
														: Integer.parseInt(str);

				// Parse second component
				if (strs.size() == 1)
					value2 = value1;
				else
				{
					str = strs.get(1);
					ch = str.charAt(0);
					value2 = (ch == BASE_VALUE_PREFIX_CHAR)
												? parseRelativeValue(str.substring(1), baseValue)
												: ((ch == '+') || (ch == '-'))
														? parseRelativeValue(str, value1)
														: Integer.parseInt(str);
				}
			}

			return new IntegerPair(value1, value2);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// INDENT COMBO BOX CLASS


	private static class IndentComboBox
		extends EditableComboBox
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	MAX_NUM_ITEMS	= 32;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public IndentComboBox(List<String> items)
		{
			super(new IndentField(), MAX_NUM_ITEMS, items);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private IntegerPair getValue(int baseValue)
		{
			return ((IndentField)getEditor()).getValue(baseValue);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// HELP DIALOG BOX CLASS


	private class HelpDialog
		extends JDialog
		implements ActionListener
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	TITLE_STR	= WRAP_TEXT_STR + " : Help";

	////////////////////////////////////////////////////////////////////
	//  Member classes : inner classes
	////////////////////////////////////////////////////////////////////


		// TEXT PANEL CLASS


		private class TextPanel
			extends JComponent
		{

		////////////////////////////////////////////////////////////////
		//  Constants
		////////////////////////////////////////////////////////////////

			private static final	int	VERTICAL_MARGIN		= 3;
			private static final	int	HORIZONTAL_MARGIN	= 6;

		////////////////////////////////////////////////////////////////
		//  Constructors
		////////////////////////////////////////////////////////////////

			private TextPanel()
			{
				AppFont.MAIN.apply(this);

				String[] strs = HELP_TEXT_STRS;
				char minusChar = getMinusChar(getFont());
				if (minusChar != MINUS_CHARS[0])
				{
					for (String str : strs)
						str.replace(MINUS_CHARS[0], minusChar);
				}
				text = new TaggedText('{', '}', FIELD_DEFS, STYLE_DEFS, V_PADDING_DEFS, H_PADDING_DEFS,
									  strs);
				setOpaque(true);
				setFocusable(false);
			}

			//----------------------------------------------------------

		////////////////////////////////////////////////////////////////
		//  Instance methods : overriding methods
		////////////////////////////////////////////////////////////////

			@Override
			public Dimension getPreferredSize()
			{
				return new Dimension(2 * HORIZONTAL_MARGIN + text.getWidth(),
									 2 * VERTICAL_MARGIN + text.getHeight());
			}

			//----------------------------------------------------------

			@Override
			protected void paintComponent(Graphics gr)
			{
				// Fill background
				Rectangle rect = gr.getClipBounds();
				gr.setColor(HELP_PANEL_BACKGROUND_COLOUR);
				gr.fillRect(rect.x, rect.y, rect.width, rect.height);

				// Draw border
				gr.setColor(HELP_PANEL_BORDER_COLOUR);
				gr.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

				// Draw text
				text.draw(gr, VERTICAL_MARGIN, HORIZONTAL_MARGIN);
			}

			//----------------------------------------------------------

		////////////////////////////////////////////////////////////////
		//  Instance methods
		////////////////////////////////////////////////////////////////

			public void updateText()
			{
				text.update(getGraphics());
			}

			//----------------------------------------------------------

		////////////////////////////////////////////////////////////////
		//  Instance variables
		////////////////////////////////////////////////////////////////

			private	TaggedText	text;

		}

		//==============================================================

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private HelpDialog()
		{
			// Call superclass constructor
			super(TextWrapDialog.this, TITLE_STR);

			// Set icons
			setIconImages(TextWrapDialog.this.getIconImages());


			//----  Text panel

			TextPanel textPanel = new TextPanel();


			//----  Button panel

			JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 0, 0));
			buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

			// Button: close
			JButton closeButton = new FButton(AppConstants.CLOSE_STR);
			closeButton.setActionCommand(Command.CLOSE);
			closeButton.addActionListener(this);
			buttonPanel.add(closeButton);


			//----  Main panel

			GridBagLayout gridBag = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();

			JPanel mainPanel = new JPanel(gridBag);

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(0, 0, 0, 0);
			gridBag.setConstraints(textPanel, gbc);
			mainPanel.add(textPanel);

			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(3, 0, 3, 0);
			gridBag.setConstraints(buttonPanel, gbc);
			mainPanel.add(buttonPanel);

			// Add commands to action map
			KeyAction.create(mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
							 KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), Command.CLOSE, this);


			//----  Window

			// Set content pane
			setContentPane(mainPanel);

			// Dispose of window explicitly
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

			// Handle window closing
			addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent event)
				{
					onClose();
				}
			});

			// Prevent dialog from being resized
			setResizable(false);

			// Resize dialog to its preferred size
			pack();

			// Resize dialog again after updating dimensions of tagged text
			textPanel.updateText();
			pack();

			// Set location of dialog box
			if (helpDialogLocation == null)
				helpDialogLocation = GuiUtils.getComponentLocation(this, TextWrapDialog.this);
			setLocation(helpDialogLocation);

			// Set default button
			getRootPane().setDefaultButton(closeButton);

			// Set focus
			closeButton.requestFocusInWindow();

			// Show dialog
			setVisible(true);

		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ActionListener interface
	////////////////////////////////////////////////////////////////////

		public void actionPerformed(ActionEvent event)
		{
			if (event.getActionCommand().equals(Command.CLOSE))
				onClose();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void close()
		{
			helpDialogLocation = getLocation();
			setVisible(false);
			dispose();
		}

		//--------------------------------------------------------------

		private void onClose()
		{
			TextWrapDialog.this.closeHelpDialog();
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private TextWrapDialog(Window owner,
						   int    currentIndent)
	{

		// Call superclass constructor
		super(owner, WRAP_TEXT_STR, Dialog.ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());

		// Initialise instance variables
		this.currentIndent = currentIndent;


		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		int gridY = 0;

		// Label: line length
		JLabel lineLengthLabel = new FLabel(LINE_LENGTH_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(lineLengthLabel, gbc);
		controlPanel.add(lineLengthLabel);

		// Combo box: line length
		lineLengthComboBox = new LineLengthComboBox(lineLengths);
		if (lineLength > 0)
			lineLengthComboBox.setSelectedItem(lineLength);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(lineLengthComboBox, gbc);
		controlPanel.add(lineLengthComboBox);

		// Check box: indent
		indentCheckBox = new FCheckBox(INDENT_STR);
		indentCheckBox.setSelected(indent != null);
		indentCheckBox.setActionCommand(Command.TOGGLE_INDENT);
		indentCheckBox.addActionListener(this);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(indentCheckBox, gbc);
		controlPanel.add(indentCheckBox);

		// Combo box: indent
		indentComboBox = new IndentComboBox(indents);
		if (indent != null)
			indentComboBox.setSelectedItem(indent);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(indentComboBox, gbc);
		controlPanel.add(indentComboBox);


		//----  Button panel

		// Left button panel
		JPanel leftButtonPanel = new JPanel(new GridLayout(1, 0, 8, 0));

		// Button: help
		helpButton = new FButton(HELP_STR + AppConstants.ELLIPSIS_STR);
		helpButton.setActionCommand(Command.HELP);
		helpButton.addActionListener(this);
		leftButtonPanel.add(helpButton);

		// Right button panel
		JPanel rightButtonPanel = new JPanel(new GridLayout(1, 0, 8, 0));

		// Button: wrap
		JButton wrapButton = new FButton(WRAP_STR);
		wrapButton.setActionCommand(Command.ACCEPT);
		wrapButton.addActionListener(this);
		rightButtonPanel.add(wrapButton);

		// Button: cancel
		JButton cancelButton = new FButton(AppConstants.CANCEL_STR);
		cancelButton.setActionCommand(Command.CLOSE);
		cancelButton.addActionListener(this);
		rightButtonPanel.add(cancelButton);

		// Outer button panel
		JPanel buttonPanel = new JPanel(gridBag);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

		int gridX = 0;

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 12);
		gridBag.setConstraints(leftButtonPanel, gbc);
		buttonPanel.add(leftButtonPanel);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 12, 0, 0);
		gridBag.setConstraints(rightButtonPanel, gbc);
		buttonPanel.add(rightButtonPanel);


		//----  Main panel

		JPanel mainPanel = new JPanel(gridBag);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

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
		mainPanel.add(controlPanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(3, 0, 0, 0);
		gridBag.setConstraints(buttonPanel, gbc);
		mainPanel.add(buttonPanel);

		// Add commands to action map
		KeyAction.create(mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						 KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), Command.CLOSE, this);

		// Update components
		updateComponents();


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

		// Set transfer handler
		setTransferHandler(FileTransferHandler.INSTANCE);

		// Dispose of window explicitly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Handle window closing
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				onClose();
			}
		});

		// Prevent dialog from being resized
		setResizable(false);

		// Resize dialog to its preferred size
		pack();

		// Set location of dialog box
		if (location == null)
			location = GuiUtils.getComponentLocation(this, owner);
		setLocation(location);

		// Set default button
		getRootPane().setDefaultButton(wrapButton);

		// Show dialog
		setVisible(true);

	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static Result showDialog(Component parent,
									int       currentIndent)
	{
		return new TextWrapDialog(GuiUtils.getWindow(parent), currentIndent).getResult();
	}

	//------------------------------------------------------------------

	private static char getMinusChar(Font font)
	{
		for (char ch : MINUS_CHARS)
		{
			if (font.canDisplay(ch))
				return ch;
		}
		return '-';
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();

		if (command.equals(Command.TOGGLE_INDENT))
			onToggleIndent();

		else if (command.equals(Command.HELP))
			onHelp();

		else if (command.equals(Command.ACCEPT))
			onAccept();

		else if (command.equals(Command.CLOSE))
			onClose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private Result getResult()
	{
		Result result = null;
		if (accepted)
		{
			IntegerPair indent = indentComboBox.isEnabled() ? indentComboBox.getValue(currentIndent)
															: new IntegerPair();
			result = new Result(lineLengthComboBox.getValue(), indent.getFirst(), indent.getSecond());
		}
		return result;
	}

	//------------------------------------------------------------------

	private void updateComponents()
	{
		indentComboBox.setEnabled(indentCheckBox.isSelected());
		if (indentComboBox.isEnabled())
			GuiUtils.setFocus(indentComboBox);
	}

	//------------------------------------------------------------------

	private void validateUserInput()
		throws AppException
	{
		// Line length
		int lineLength = lineLengthComboBox.getValue();
		if (lineLength > 0)
		{
			try
			{
				if ((lineLength < TextDocument.MIN_WRAP_LINE_LENGTH) ||
					 (lineLength > TextDocument.MAX_WRAP_LINE_LENGTH))
					throw new AppException(ErrorId.LINE_LENGTH_OUT_OF_BOUNDS);
			}
			catch (AppException e)
			{
				GuiUtils.setFocus(lineLengthComboBox);
				throw e;
			}
		}

		// Indent
		if (indentComboBox.isEnabled())
		{
			try
			{
				try
				{
					IntegerPair indent = indentComboBox.getValue(currentIndent);
					if ((indent.getFirst() < MIN_INDENT) || (indent.getFirst() > MAX_INDENT))
						throw new AppException(ErrorId.FIRST_INDENT_OUT_OF_BOUNDS);
					if ((indent.getSecond() < MIN_INDENT) || (indent.getSecond() > MAX_INDENT))
						throw new AppException(ErrorId.SECOND_INDENT_OUT_OF_BOUNDS);

					if (lineLength > 0)
					{
						if (indent.getFirst() >= lineLength)
							throw new AppException(ErrorId.LINE_LENGTH_AND_FIRST_INDENT_OUT_OF_ORDER);
						if (indent.getSecond() >= lineLength)
							throw new AppException(ErrorId.LINE_LENGTH_AND_SECOND_INDENT_OUT_OF_ORDER);
					}
				}
				catch (NumberFormatException e)
				{
					throw new AppException(ErrorId.INVALID_INDENT);
				}
			}
			catch (AppException e)
			{
				GuiUtils.setFocus(indentComboBox);
				throw e;
			}
		}
	}

	//------------------------------------------------------------------

	private void closeHelpDialog()
	{
		helpDialog.close();
		helpDialog = null;
		helpButton.setEnabled(true);
	}

	//------------------------------------------------------------------

	private void onToggleIndent()
	{
		updateComponents();
	}

	//------------------------------------------------------------------

	private void onHelp()
	{
		if (helpDialog == null)
		{
			helpButton.setEnabled(false);
			helpDialog = new HelpDialog();
		}
	}

	//------------------------------------------------------------------

	private void onAccept()
	{
		try
		{
			// Validate user input
			validateUserInput();

			// Update class fields
			lineLengthComboBox.updateList();
			lineLengths = lineLengthComboBox.getItems();
			lineLength = lineLengthComboBox.getValue();
			indents = indentComboBox.getItems();
			indent = indentComboBox.isEnabled() ? indentComboBox.getText() : null;

			// Close dialog
			accepted = true;
			onClose();
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, App.SHORT_NAME, JOptionPane.ERROR_MESSAGE);
		}
	}

	//------------------------------------------------------------------

	private void onClose()
	{
		location = getLocation();
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point			location;
	private static	Point			helpDialogLocation;
	private static	List<Integer>	lineLengths			= new ArrayList<>();
	private static	int				lineLength;
	private static	List<String>	indents				= new ArrayList<>();
	private static	String			indent;

////////////////////////////////////////////////////////////////////////
//  Static initialiser
////////////////////////////////////////////////////////////////////////

	static
	{
		Integer length = AppConfig.INSTANCE.getTextWrapDefaultLineLength();
		if (length != null)
		{
			lineLengths.add(length);
			lineLength = length;
		}
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	boolean				accepted;
	private	int					currentIndent;
	private	LineLengthComboBox	lineLengthComboBox;
	private	JCheckBox			indentCheckBox;
	private	IndentComboBox		indentComboBox;
	private	JButton				helpButton;
	private	HelpDialog			helpDialog;

}

//----------------------------------------------------------------------
