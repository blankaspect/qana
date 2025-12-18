/*====================================================================*\

TextDocument.java

Text document class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javax.swing.undo.UndoManager;

import org.w3c.dom.Element;

import uk.blankaspect.common.base64.Base64Encoder;

import uk.blankaspect.common.crypto.FileConcealer;
import uk.blankaspect.common.crypto.FortunaCipher;
import uk.blankaspect.common.crypto.StreamEncrypter;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.common.misc.IProgressListener;
import uk.blankaspect.common.misc.ModernCalendar;

import uk.blankaspect.common.regex.RegexUtils;

import uk.blankaspect.common.xml.AttributeList;
import uk.blankaspect.common.xml.XmlConstants;
import uk.blankaspect.common.xml.XmlParseException;
import uk.blankaspect.common.xml.XmlUtils;
import uk.blankaspect.common.xml.XmlWriter;

//----------------------------------------------------------------------


// TEXT DOCUMENT CLASS


class TextDocument
	extends Document
	implements CaretListener, DocumentListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		int		MIN_MAX_EDIT_LIST_LENGTH		= 1;
	public static final		int		MAX_MAX_EDIT_LIST_LENGTH		= 9999;
	public static final		int		DEFAULT_MAX_EDIT_LIST_LENGTH	= 100;

	public static final		int		MIN_WRAP_LINE_LENGTH		= 8;
	public static final		int		MAX_WRAP_LINE_LENGTH		= 999;
	public static final		int		DEFAULT_WRAP_LINE_LENGTH	= 80;

	public static final		int		MIN_NUM_SPACES_BETWEEN_SENTENCES		= 1;
	public static final		int		MAX_NUM_SPACES_BETWEEN_SENTENCES		= 9;
	public static final		int		DEFAULT_NUM_SPACES_BETWEEN_SENTENCES	= 2;

	public static final		String	DEFAULT_END_OF_SENTENCE_PATTERN	=
			"(?:(?:\\p{L}|\\p{N}|['\"\u00BB\u201C)\\]>%])[.!?])|(?:(?:\\p{L}|\\p{N})[.!?]['\"\u00BB\u201C)\\]>%])";

	private static final	int		ENCRYPTED_LINE_LENGTH	= 76;

	private static final	int		VERSION					= 0;
	private static final	int		MIN_SUPPORTED_VERSION	= 0;
	private static final	int		MAX_SUPPORTED_VERSION	= 0;

	private static final	String	TEXT_STR			= "Text";
	private static final	String	ENCRYPT_TEXT_STR	= "Encrypt text";
	private static final	String	ENCRYPTING_TEXT_STR	= "Encrypting text " + AppConstants.ELLIPSIS_STR;
	private static final	String	DECRYPT_TEXT_STR	= "Decrypt text";
	private static final	String	DECRYPTING_TEXT_STR	= "Decrypting text " + AppConstants.ELLIPSIS_STR;
	private static final	String	CONCEAL_TEXT_STR	= "Conceal text";
	private static final	String	CONCEALING_TEXT_STR	= "Concealing text " + AppConstants.ELLIPSIS_STR;
	private static final	String	RECOVERING_TEXT_STR	= "Recovering text " + AppConstants.ELLIPSIS_STR;
	private static final	String	CLEAR_EDIT_LIST_STR	= "Do you want to clear all the undo/redo actions?";
	private static final	String	XML_VERSION_STR		= "1.0";
	private static final	String	XML_DECLARATION_STR	= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private static final	String	START_TAG_STR		= "<" + ElementName.QANA_TEXT;
	private static final	String	END_TAG_STR			= "</" + ElementName.QANA_TEXT + ">";

	private interface ElementName
	{
		String	QANA_TEXT	= "qanaText";
	}

	private interface AttrName
	{
		String	VERSION	= "version";
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	int			instanceIndex;
	private	UndoManager	undoManager;
	private	JTextArea	textArea;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public TextDocument(int instanceIndex)
	{
		this.instanceIndex = instanceIndex;
		undoManager = new UndoManager();
		undoManager.setLimit(AppConfig.INSTANCE.getMaxEditListLength());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static String bytesToBase64(byte[] data)
	{
		return new Base64Encoder(ENCRYPTED_LINE_LENGTH, "\n").encode(data);
	}

	//------------------------------------------------------------------

	public static String wrapTextInXml(String        text,
									   FortunaCipher cipher)
	{
		CharArrayWriter charArrayWriter = new CharArrayWriter();
		try
		{
			XmlWriter writer = new XmlWriter(charArrayWriter);
			writer.writeXmlDeclaration(XML_VERSION_STR, XmlConstants.ENCODING_NAME_UTF8, XmlWriter.Standalone.NONE);

			AttributeList attributes = new AttributeList();
			attributes.add(AttrName.VERSION, VERSION);
			writer.writeElementStart(ElementName.QANA_TEXT, attributes, 0, true, false);

			writer.write(text);

			writer.writeElementEnd(ElementName.QANA_TEXT, 0);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return charArrayWriter.toString();
	}

	//------------------------------------------------------------------

	private static int getTextIndent(String text)
		throws AppException
	{
		int indent = 0;
		for (int i = 0; i < text.length(); i++)
		{
			char ch = text.charAt(i);
			if (ch == '\n')
				indent = 0;
			else
			{
				if (ch != ' ')
					break;
				++indent;
			}
		}
		return indent;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : CaretListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void caretUpdate(CaretEvent event)
	{
		updateCommands();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : DocumentListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void changedUpdate(DocumentEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void insertUpdate(DocumentEvent event)
	{
		updateCommands();
	}

	//------------------------------------------------------------------

	@Override
	public void removeUpdate(DocumentEvent event)
	{
		updateCommands();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public Kind getKind()
	{
		return Kind.TEXT;
	}

	//------------------------------------------------------------------

	@Override
	public boolean isChanged()
	{
		return isText();
	}

	//------------------------------------------------------------------

	@Override
	public String getName()
	{
		return TEXT_STR + " " + instanceIndex;
	}

	//------------------------------------------------------------------

	@Override
	public String getTitleString(boolean full)
	{
		return getName();
	}

	//------------------------------------------------------------------

	@Override
	public View createView()
	{
		return new TextView(this);
	}

	//------------------------------------------------------------------

	@Override
	public void updateCommands()
	{
		boolean isText = isText();
		boolean isTextOnClipboard = Utils.clipboardHasText();
		boolean isSelection = isSelection();

		AppConfig config = AppConfig.INSTANCE;

		Command.UNDO.setEnabled(undoManager.canUndo());
		Command.REDO.setEnabled(undoManager.canRedo());
		Command.CLEAR_EDIT_LIST.setEnabled(undoManager.canUndoOrRedo());
		Command.CUT.setEnabled(isSelection);
		Command.COPY.setEnabled(isSelection);
		Command.COPY_ALL.setEnabled(isText);
		Command.PASTE.setEnabled(isTextOnClipboard);
		Command.PASTE_ALL.setEnabled(isTextOnClipboard);
		Command.CLEAR.setEnabled(isText);
		Command.SELECT_ALL.setEnabled(isText);
		Command.WRAP.setEnabled(isSelection);
		Command.ENCRYPT.setEnabled(isText);
		Command.DECRYPT.setEnabled(isText);
		Command.CONCEAL.setEnabled(isText);
		Command.TOGGLE_WRAP_CIPHERTEXT_IN_XML.setEnabled(true);
		Command.TOGGLE_WRAP_CIPHERTEXT_IN_XML.setSelected(config.isWrapCiphertextInXml());
		Command.SET_KEY.setEnabled(true);
		Command.CLEAR_KEY.setEnabled(isKey());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public UndoManager getUndoManager()
	{
		return undoManager;
	}

	//------------------------------------------------------------------

	public void setTextArea(JTextArea textArea)
	{
		this.textArea = textArea;
	}

	//------------------------------------------------------------------

	public void setText(String  text,
						boolean caretToStart)
	{
		SwingUtilities.invokeLater(() ->
		{
			textArea.setText(text);
			if (caretToStart)
				textArea.setCaretPosition(0);
		});
	}

	//------------------------------------------------------------------

	public void encrypt(KeyList.Key key)
		throws AppException
	{
		// Reset progress in progress view
		TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
		progressView.setInfo(ENCRYPTING_TEXT_STR);
		progressView.setProgress(0, 0.0);
		progressView.waitForIdle();

		// Encrypt text and encode encrypted data as Base64
		String ciphertext = bytesToBase64(encrypt(key, progressView));

		// Display text
		setText(AppConfig.INSTANCE.isWrapCiphertextInXml() ? wrapTextInXml(ciphertext, Utils.getCipher(key))
														   : ciphertext,
				true);
	}

	//------------------------------------------------------------------

	public void decrypt(KeyList.Key key)
		throws AppException
	{
		// Reset progress in progress view
		TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
		progressView.setInfo(DECRYPTING_TEXT_STR);
		progressView.setProgress(0, 0.0);
		progressView.waitForIdle();

		// Extract ciphertext
		String text = getText();
		if (AppConfig.INSTANCE.isWrapCiphertextInXml())
		{
			// Get start and end indices of first ciphertext element
			int index = text.indexOf(START_TAG_STR);
			if (index < 0)
				throw new AppException(ErrorId.NO_XML_WRAPPER);
			int startIndex = index;
			index += START_TAG_STR.length();

			index = text.indexOf('>', index);
			if (index < 0)
				throw new AppException(ErrorId.NO_XML_WRAPPER);
			++index;

			index = text.indexOf(END_TAG_STR, index);
			if (index < 0)
				throw new AppException(ErrorId.MALFORMED_XML_WRAPPER);
			int endIndex = index + END_TAG_STR.length();

			// Create XML document from ciphertext element
			text = XML_DECLARATION_STR + text.substring(startIndex, endIndex);
			org.w3c.dom.Document document = XmlUtils.createDocument(text);

			// Test root element
			Element element = document.getDocumentElement();
			if (!element.getNodeName().equals(ElementName.QANA_TEXT))
				throw new AppException(ErrorId.NO_ENCRYPTED_TEXT);

			// Test version number
			String elementPath = ElementName.QANA_TEXT;
			String attrName = AttrName.VERSION;
			String attrKey = XmlUtils.appendAttributeName(elementPath, attrName);
			String attrValue = XmlUtils.getAttribute(element, attrName);
			if (attrValue == null)
				throw new XmlParseException(ErrorId.NO_ATTRIBUTE, attrKey);
			try
			{
				int version = Integer.parseInt(attrValue);
				if ((version < MIN_SUPPORTED_VERSION) || (version > MAX_SUPPORTED_VERSION))
					throw new AppException(ErrorId.UNSUPPORTED_VERSION, attrValue);
			}
			catch (NumberFormatException e)
			{
				throw new XmlParseException(ErrorId.INVALID_ATTRIBUTE, attrKey, attrValue);
			}

			// Set text
			text = element.getTextContent();
		}

		// Decode Base64 text
		byte[] data = null;
		try
		{
			data = new Base64Encoder().decode(text);
		}
		catch (Base64Encoder.IllegalCharacterException e)
		{
			throw new AppException(ErrorId.ILLEGAL_CHARACTER);
		}
		catch (Base64Encoder.MalformedDataException e)
		{
			throw new AppException(ErrorId.MALFORMED_ENCRYPTED_TEXT);
		}

		// Decrypt data
		decrypt(data, key, progressView);
	}

	//------------------------------------------------------------------

	public void conceal(File        carrierFile,
						File        outFile,
						int         maxNumBits,
						boolean     setTimestamp,
						boolean     addRandomBits,
						KeyList.Key key)
		throws AppException
	{
		// Reset progress in progress view
		TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
		progressView.setInfo(CONCEALING_TEXT_STR);
		progressView.setProgress(0, 0.0);
		progressView.setProgress(1, 0.0);
		progressView.waitForIdle();

		// Encrypt text
		progressView.initOverallProgress(0, 1, 2);
		ByteArrayInputStream inStream = new ByteArrayInputStream(encrypt(key, progressView));

		// Conceal encrypted text
		progressView.initOverallProgress(1, 1, 2);
		QanaApp app = QanaApp.INSTANCE;
		new FileConcealer().conceal(inStream, (carrierFile == null) ? app.getCarrierImageSource() : null, carrierFile,
									outFile, inStream.available(), app.getLengthEncoder(), maxNumBits,
									addRandomBits ? app.getRandomSource() : null);

		// Set timestamp of output file
		if (setTimestamp)
		{
			long timestamp = carrierFile.lastModified();
			if (timestamp == 0)
				throw new FileException(ErrorId.FAILED_TO_GET_FILE_TIMESTAMP, carrierFile);
			if (!outFile.setLastModified(timestamp))
				throw new FileException(ErrorId.FAILED_TO_SET_FILE_TIMESTAMP, outFile);
		}
	}

	//------------------------------------------------------------------

	public void recover(File        inFile,
						KeyList.Key key)
		throws AppException
	{
		// Reset progress in progress view
		TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
		progressView.setInfo(RECOVERING_TEXT_STR);
		progressView.setProgress(0, 0.0);
		progressView.setProgress(1, 0.0);
		progressView.waitForIdle();

		// Recover file
		progressView.initOverallProgress(0, 1, 2);
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		new FileConcealer().recover(inFile, outStream, QanaApp.INSTANCE.getLengthDecoder());

		// Decrypt text
		progressView.initOverallProgress(1, 1, 2);
		decrypt(outStream.toByteArray(), key, progressView);
	}

	//------------------------------------------------------------------

	private boolean isText()
	{
		return (textArea.getDocument().getLength() > 0);
	}

	//------------------------------------------------------------------

	private String getText()
	{
		return textArea.getText();
	}

	//------------------------------------------------------------------

	private boolean isSelection()
	{
		return (textArea.getSelectionStart() < textArea.getSelectionEnd());
	}

	//------------------------------------------------------------------

	private String wrapText(String  text,
							Pattern endOfSentencePattern,
							int     maxLineLength,
							int     indent1,
							int     indent2)
	{
		// Add LF to end of text if there is none
		StringBuilder textBuffer = new StringBuilder(text);
		int textLength = textBuffer.length();
		if ((textLength == 0) || (textBuffer.charAt(textLength - 1) != '\n'))
		{
			textBuffer.append('\n');
			++textLength;
		}

		// Strip leading and trailing spaces from each line
		int lineIndex = 0;
		int inIndex = 0;
		int outIndex = 0;
		int startIndex = 0;
		int index = 0;
		while (index < textLength)
		{
			if (textBuffer.charAt(index) == '\n')
			{
				inIndex = startIndex;
				if (lineIndex > 0)
				{
					while (inIndex < index)
					{
						if (textBuffer.charAt(inIndex) != ' ')
							break;
						++inIndex;
					}
				}
				int endIndex = index;
				while (--endIndex >= inIndex)
				{
					if (textBuffer.charAt(endIndex) != ' ')
						break;
				}
				++endIndex;
				if (inIndex < endIndex)
					++lineIndex;
				else
					lineIndex = 0;
				while (inIndex < endIndex)
					textBuffer.setCharAt(outIndex++, textBuffer.charAt(inIndex++));
				textBuffer.setCharAt(outIndex++, '\n');
				startIndex = index + 1;
			}
			++index;
		}
		textLength = outIndex;
		textBuffer.setLength(textLength);

		// Combine lines of text into paragraphs
		List<String> paragraphs = new ArrayList<>();
		StringBuilder buffer = new StringBuilder();
		int lineLength = 0;
		index = 0;
		while (index < textLength)
		{
			char ch = textBuffer.charAt(index++);
			if (ch == '\n')
			{
				if ((lineLength == 0) || (index == textLength) || (textBuffer.charAt(index) == '\n'))
				{
					paragraphs.add(buffer.toString());
					buffer.setLength(0);
				}
				else
					buffer.append(' ');
				lineLength = 0;
			}
			else
			{
				buffer.append(ch);
				++lineLength;
			}
		}
		if (!buffer.isEmpty())
			paragraphs.add(buffer.toString());

		// Format ends of sentences
		if (endOfSentencePattern != null)
		{
			int numSpaces = AppConfig.INSTANCE.getTextWrapNumSpacesBetweenSentences();
			String replacementStr = "$1" + " ".repeat(numSpaces);
			for (int i = 0; i < paragraphs.size(); i++)
			{
				String str = paragraphs.get(i);
				if (!str.isEmpty())
					paragraphs.set(i, endOfSentencePattern.matcher(str).replaceAll(replacementStr));
			}
		}

		// Format paragraphs
		String indentChars = " ".repeat(Math.max(indent1, indent2));
		textBuffer.setLength(0);
		for (String str : paragraphs)
		{
			if (str.isEmpty())
				textBuffer.append('\n');
			else
			{
				int indent = indent1;
				textBuffer.append(indentChars, 0, indent);
				if (maxLineLength == 0)
				{
					textBuffer.append(str);
					textBuffer.append('\n');
				}
				else
				{
					index = 0;
					while (index < str.length())
					{
						boolean space = false;
						int breakIndex = index;
						int endIndex = index + maxLineLength - indent;
						for (int i = index; (i <= endIndex) || (breakIndex == index); i++)
						{
							if (i == str.length())
							{
								if (!space)
									breakIndex = i;
								break;
							}
							if (str.charAt(i) == ' ')
							{
								if (!space)
								{
									space = true;
									breakIndex = i;
								}
							}
							else
								space = false;
						}
						if (breakIndex - index > 0)
							textBuffer.append(str.substring(index, breakIndex));
						textBuffer.append('\n');
						indent = indent2;
						textBuffer.append(indentChars, 0, indent);
						for (index = breakIndex; index < str.length(); index++)
						{
							if (str.charAt(index) != ' ')
								break;
						}
					}
				}
			}
			for (index = textBuffer.length() - 1; index >= 0; index--)
			{
				if (textBuffer.charAt(index) != ' ')
					break;
			}
			if (index < 0)
				index = 0;
			else
				++index;
			textBuffer.setLength(index);
		}

		// Return wrapped text
		return textBuffer.toString();
	}

	//------------------------------------------------------------------

	private byte[] encrypt(KeyList.Key       key,
						   IProgressListener progressListener)
		throws AppException
	{
		// Convert text to UTF-8
		byte[] data = getText().getBytes(StandardCharsets.UTF_8);

		// Encrypt data
		InputStream inStream = new ByteArrayInputStream(data);
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		StreamEncrypter encrypter = key.getStreamEncrypter(Utils.getCipher(key));
		if (progressListener != null)
			encrypter.addProgressListener(progressListener);
		encrypter.encrypt(inStream, outStream, data.length, new ModernCalendar().getTimeInMillis(), key.getKey(),
						  QanaApp.INSTANCE.getRandomKey(), QanaApp.INSTANCE::generateKey);

		// Return encrypted data
		return outStream.toByteArray();
	}

	//------------------------------------------------------------------

	private void decrypt(byte[]            data,
						 KeyList.Key       key,
						 IProgressListener progressListener)
		throws AppException
	{
		// Create decrypter
		StreamEncrypter decrypter = key.getStreamEncrypter(null);

		// Test length of data
		if (data.length < decrypter.getMinOverheadSize())
			throw new AppException(ErrorId.INSUFFICIENT_ENCRYPTED_TEXT);

		// Decrypt data
		InputStream inStream = new ByteArrayInputStream(data);
		ByteArrayOutputStream outStream = new ByteArrayOutputStream()
		{
			@Override
			public String toString()
			{
				return new String(buf, 0, count, StandardCharsets.UTF_8);
			}
		};
		if (progressListener != null)
			decrypter.addProgressListener(progressListener);
		setTimestamp(decrypter.decrypt(inStream, outStream, data.length, key.getKey(), QanaApp.INSTANCE::generateKey));

		// Set decrypted text
		setText(outStream.toString(), true);

		// Update status
		getWindow().updateStatus();
	}

	//------------------------------------------------------------------

	private Pattern getEndOfSentencePattern()
		throws AppException
	{
		// Validate end-of-sentence pattern
		try
		{
			Pattern endOfSentencePattern = null;
			String patternStr = AppConfig.INSTANCE.getTextWrapEndOfSentencePattern();
			if (!patternStr.isEmpty())
			{
				endOfSentencePattern = Pattern.compile(patternStr);
				endOfSentencePattern = Pattern.compile("(" + patternStr + ") +");
			}
			return endOfSentencePattern;
		}
		catch (PatternSyntaxException e)
		{
			throw new AppException(ErrorId.MALFORMED_END_OF_SENTENCE_PATTERN, RegexUtils.getExceptionMessage(e));
		}
	}

	//------------------------------------------------------------------

	private void performAction(Action action)
	{
		action.actionPerformed(new ActionEvent(textArea, ActionEvent.ACTION_PERFORMED,
											   action.getValue(Action.NAME).toString()));
	}

	//------------------------------------------------------------------

	private void executeCommand(Command command)
	{
		// Perform command
		try
		{
			try
			{
				switch (command)
				{
					case UNDO                          -> onUndo();
					case REDO                          -> onRedo();
					case CLEAR_EDIT_LIST               -> onClearEditList();
					case CUT                           -> onCut();
					case COPY                          -> onCopy();
					case COPY_ALL                      -> onCopyAll();
					case PASTE                         -> onPaste();
					case PASTE_ALL                     -> onPasteAll();
					case CLEAR                         -> onClear();
					case SELECT_ALL                    -> onSelectAll();
					case WRAP                          -> onWrap();
					case ENCRYPT                       -> onEncrypt();
					case DECRYPT                       -> onDecrypt();
					case CONCEAL                       -> onConceal();
					case TOGGLE_WRAP_CIPHERTEXT_IN_XML -> onToggleWrapCiphertextInXml();
					case SET_KEY                       -> onSetKey();
					case CLEAR_KEY                     -> onClearKey();
				}
			}
			catch (OutOfMemoryError e)
			{
				throw new AppException(ErrorId.NOT_ENOUGH_MEMORY_TO_PERFORM_COMMAND);
			}
		}
		catch (AppException e)
		{
			QanaApp.INSTANCE.showErrorMessage(QanaApp.SHORT_NAME, e);
		}

		// Update tab text and title and menus in main window
		QanaApp.INSTANCE.updateTabText(this);
		getWindow().updateTitleAndMenus();
	}

	//------------------------------------------------------------------

	private void onUndo()
	{
		undoManager.undo();
	}

	//------------------------------------------------------------------

	private void onRedo()
	{
		undoManager.redo();
	}

	//------------------------------------------------------------------

	private void onClearEditList()
	{
		String[] optionStrs = Utils.getOptionStrings(AppConstants.CLEAR_STR);
		if (JOptionPane.showOptionDialog(getWindow(), CLEAR_EDIT_LIST_STR, QanaApp.SHORT_NAME,
										 JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
										 optionStrs, optionStrs[1]) == JOptionPane.OK_OPTION)
		{
			undoManager.discardAllEdits();
			System.gc();
		}
	}

	//------------------------------------------------------------------

	private void onCut()
	{
		performAction(TransferHandler.getCutAction());
	}

	//------------------------------------------------------------------

	private void onCopy()
	{
		performAction(TransferHandler.getCopyAction());
	}

	//------------------------------------------------------------------

	private void onCopyAll()
		throws AppException
	{
		Utils.putClipboardText(getText());
	}

	//------------------------------------------------------------------

	private void onPaste()
	{
		performAction(TransferHandler.getPasteAction());
	}

	//------------------------------------------------------------------

	private void onPasteAll()
		throws AppException
	{
		setText(Utils.getClipboardText(), false);
	}

	//------------------------------------------------------------------

	private void onClear()
	{
		setText(null, false);
	}

	//------------------------------------------------------------------

	private void onSelectAll()
	{
		textArea.selectAll();
	}

	//------------------------------------------------------------------

	private void onWrap()
		throws AppException
	{
		// Validate end-of-sentence pattern
		Pattern endOfSentencePattern = getEndOfSentencePattern();

		// Display dialog for specifying wrapping parameters
		String text = textArea.getSelectedText();
		TextWrapDialog.Result result = TextWrapDialog.showDialog(getWindow(), getTextIndent(text));

		// Wrap text
		if (result != null)
		{
			text = wrapText(text, endOfSentencePattern, result.lineLength(), result.indent1(), result.indent2());
			textArea.replaceSelection(text);
		}
	}

	//------------------------------------------------------------------

	private void onEncrypt()
		throws AppException
	{
		// Get key
		KeyList.Key key = getKey(ENCRYPT_TEXT_STR);

		// Encrypt text
		if ((key != null) && QanaApp.INSTANCE.confirmUseTemporaryKey(key))
		{
			// Check cipher
			key.checkAllowedCipher(Utils.getCipher(key));

			// Encrypt text
			TaskProgressDialog.showDialog(getWindow(), ENCRYPT_TEXT_STR, new Task.EncryptText(this, key));
		}
	}

	//------------------------------------------------------------------

	private void onDecrypt()
		throws AppException
	{
		// Get key
		KeyList.Key key = getKey(DECRYPT_TEXT_STR);

		// Decrypt text
		if (key != null)
			TaskProgressDialog.showDialog(getWindow(), DECRYPT_TEXT_STR, new Task.DecryptText(this, key));
	}

	//------------------------------------------------------------------

	private void onConceal()
		throws AppException
	{
		// Get parameters of concealment operation
		ConcealDialog.Result result = ConcealDialog.showDialog(getWindow(), false);
		if (result != null)
		{
			// Get key
			KeyList.Key key = getKey(CONCEAL_TEXT_STR);

			// Conceal text
			if ((key != null) && QanaApp.INSTANCE.confirmUseTemporaryKey(key))
			{
				// Check cipher
				key.checkAllowedCipher(Utils.getCipher(key));

				// Conceal text
				TaskProgressDialog.showDialog2(getWindow(), CONCEAL_TEXT_STR,
											   new Task.ConcealText(this, result.carrierFile(), result.outFile(),
																	result.maxNumBits(), result.setTimestamp(),
																	result.addRandomBits(), key));
			}
		}
	}

	//------------------------------------------------------------------

	private void onToggleWrapCiphertextInXml()
	{
		AppConfig config = AppConfig.INSTANCE;
		config.setWrapCiphertextInXml(!config.isWrapCiphertextInXml());
	}

	//------------------------------------------------------------------

	private void onSetKey()
	{
		setKey();
	}

	//------------------------------------------------------------------

	private void onClearKey()
	{
		clearKey();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// COMMANDS


	enum Command
		implements Action
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		UNDO
		(
			"undo",
			"Undo",
			KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK)
		),

		REDO
		(
			"redo",
			"Redo",
			KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK)
		),

		CLEAR_EDIT_LIST
		(
			"clearEditList",
			"Clear edit history" + AppConstants.ELLIPSIS_STR
		),

		CUT
		(
			"cut",
			"Cut",
			KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK)
		),

		COPY
		(
			"copy",
			"Copy",
			KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK)
		),

		COPY_ALL
		(
			"copyAll",
			"Copy all",
			KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)
		),

		PASTE
		(
			"paste",
			"Paste",
			KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK)
		),

		PASTE_ALL
		(
			"pasteAll",
			"Paste all",
			KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)
		),

		CLEAR
		(
			"clear",
			"Clear",
			KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)
		),

		SELECT_ALL
		(
			"selectAll",
			"Select all",
			KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK)
		),

		WRAP
		(
			"wrap",
			"Wrap text" + AppConstants.ELLIPSIS_STR,
			KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK)
		),

		ENCRYPT
		(
			"encrypt",
			"Encrypt text",
			KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)
		),

		DECRYPT
		(
			"decrypt",
			"Decrypt text",
			KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)
		),

		CONCEAL
		(
			"conceal",
			"Conceal text in image" + AppConstants.ELLIPSIS_STR
		),

		TOGGLE_WRAP_CIPHERTEXT_IN_XML
		(
			"toggleWrapCiphertextInXml",
			"Wrap encrypted text in XML",
			KeyStroke.getKeyStroke(KeyEvent.VK_F9, KeyEvent.CTRL_DOWN_MASK)
		),

		SET_KEY
		(
			"setKey",
			"Set key" + AppConstants.ELLIPSIS_STR,
			KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK)
		),

		CLEAR_KEY
		(
			"clearKey",
			"Clear key" + AppConstants.ELLIPSIS_STR,
			KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)
		);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	uk.blankaspect.ui.swing.action.Command	command;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Command(String key)
		{
			command = new uk.blankaspect.ui.swing.action.Command(this);
			putValue(Action.ACTION_COMMAND_KEY, key);
		}

		//--------------------------------------------------------------

		private Command(String key,
						String name)
		{
			this(key);
			putValue(Action.NAME, name);
		}

		//--------------------------------------------------------------

		private Command(String    key,
						String    name,
						KeyStroke acceleratorKey)
		{
			this(key, name);
			putValue(Action.ACCELERATOR_KEY, acceleratorKey);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		public static void setAllEnabled(boolean enabled)
		{
			for (Command command : values())
				command.setEnabled(enabled);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Action interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void addPropertyChangeListener(PropertyChangeListener listener)
		{
			command.addPropertyChangeListener(listener);
		}

		//--------------------------------------------------------------

		@Override
		public Object getValue(String key)
		{
			return command.getValue(key);
		}

		//--------------------------------------------------------------

		@Override
		public boolean isEnabled()
		{
			return command.isEnabled();
		}

		//--------------------------------------------------------------

		@Override
		public void putValue(String key,
							 Object value)
		{
			command.putValue(key, value);
		}

		//--------------------------------------------------------------

		@Override
		public void removePropertyChangeListener(PropertyChangeListener listener)
		{
			command.removePropertyChangeListener(listener);
		}

		//--------------------------------------------------------------

		@Override
		public void setEnabled(boolean enabled)
		{
			command.setEnabled(enabled);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ActionListener interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void actionPerformed(ActionEvent event)
		{
			TextDocument document = QanaApp.INSTANCE.getTextDocument();
			if (document != null)
				document.executeCommand(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public void setSelected(boolean selected)
		{
			putValue(Action.SELECTED_KEY, selected);
		}

		//--------------------------------------------------------------

		public void execute()
		{
			actionPerformed(null);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		ILLEGAL_CHARACTER
		("The encrypted text contains an illegal character."),

		MALFORMED_ENCRYPTED_TEXT
		("The encrypted text is malformed."),

		INSUFFICIENT_ENCRYPTED_TEXT
		("The text is too short to have been encrypted by this program."),

		NO_XML_WRAPPER
		("The expected XML wrapper was not found."),

		MALFORMED_XML_WRAPPER
		("The XML wrapper was malformed."),

		MALFORMED_END_OF_SENTENCE_PATTERN
		("The end-of-sentence pattern is not a well-formed regular expression.\n(%1)"),

		NO_ENCRYPTED_TEXT
		("The text was not encrypted by this program."),

		UNRECOGNISED_ENCRYPTION_ID
		("The text was encrypted with an unrecognised algorithm."),

		UNSUPPORTED_VERSION
		("The version of the encrypted text (%1) is not supported by this version of " + QanaApp.SHORT_NAME + "."),

		NO_ATTRIBUTE
		("The required attribute is missing."),

		INVALID_ATTRIBUTE
		("The attribute is invalid."),

		FAILED_TO_GET_FILE_TIMESTAMP
		("Failed to get the timestamp of the file."),

		FAILED_TO_SET_FILE_TIMESTAMP
		("Failed to set the timestamp of the file."),

		NOT_ENOUGH_MEMORY_TO_PERFORM_COMMAND
		("There was not enough memory to perform the command.");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

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

		@Override
		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
