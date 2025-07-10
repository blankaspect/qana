/*====================================================================*\

ArchiveDocument.java

Class: archive document.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.channels.OverlappingFileLockException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import javax.swing.table.AbstractTableModel;

import uk.blankaspect.common.crypto.FortunaAes256;
import uk.blankaspect.common.crypto.FortunaCipher;
import uk.blankaspect.common.crypto.HmacSha256;
import uk.blankaspect.common.crypto.ScryptSalsa20;
import uk.blankaspect.common.crypto.StreamEncrypter;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.TaskCancelledException;
import uk.blankaspect.common.exception.TempFileException;

import uk.blankaspect.common.filesystem.FilenameUtils;

import uk.blankaspect.common.misc.ByteBlockInputStream;
import uk.blankaspect.common.misc.IStringKeyed;
import uk.blankaspect.common.misc.NullOutputStream;

import uk.blankaspect.common.number.NumberCodec;
import uk.blankaspect.common.number.NumberUtils;

import uk.blankaspect.common.time.CalendarTime;

import uk.blankaspect.common.ui.progress.IProgressView;

import uk.blankaspect.ui.swing.container.FileSelectionPanel;

import uk.blankaspect.ui.swing.dialog.NonEditableTextAreaDialog;
import uk.blankaspect.ui.swing.dialog.QuestionDialog;

import uk.blankaspect.ui.swing.misc.GuiUtils;

//----------------------------------------------------------------------


// CLASS: ARCHIVE DOCUMENT


class ArchiveDocument
	extends Document
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		int		ENCRYPTION_KEY_SIZE	= FortunaAes256.KEY_SIZE;

	public static final		SortingOrder	DEFAULT_SORTING_ORDER	=
			new SortingOrder(ArchiveView.Column.PATH, ArchiveView.SortingDirection.ASCENDING);

	private static final	int		FILE_HASH_VALUE_SIZE	= HmacSha256.HASH_VALUE_SIZE;

	private static final	int		MAX_NUM_ELEMENTS	= (1 << 24) - 1;

	private static final	int		ENCRYPTION_ID	= 0x35DC4EB9;

	private static final	int		VERSION					= 0;
	private static final	int		MIN_SUPPORTED_VERSION	= VERSION;
	private static final	int		MAX_SUPPORTED_VERSION	= VERSION;

	private static final	int		STRING_TABLE_OFFSET_FIELD_SIZE	= 4;
	private static final	int		NUM_ELEMENTS_FIELD_SIZE			= 4;
	private static final	int		HASH_VALUE_FIELD_SIZE			= FILE_HASH_VALUE_SIZE;

	private static final	int		HEADER_SIZE		= STRING_TABLE_OFFSET_FIELD_SIZE + NUM_ELEMENTS_FIELD_SIZE;
	private static final	int		METADATA_SIZE	= HEADER_SIZE + HASH_VALUE_FIELD_SIZE;

	private static final	String	TEMPORARY_FILENAME_EXTENSION	= ".$tmp";

	private static final	String	UNNAMED_STR					= "Unnamed";
	private static final	String	FILE_STR					= "file";
	private static final	String	READING_STR					= "Reading";
	private static final	String	WRITING_STR					= "Writing";
	private static final	String	SORTING_STR					= "Sorting entries " + AppConstants.ELLIPSIS_STR;
	private static final	String	ENCRYPTING_STR				= "Encrypting";
	private static final	String	DECRYPTING_STR				= "Decrypting";
	private static final	String	VALIDATING_STR				= "Validating";
	private static final	String	DELETING_STR				= "Deleting";
	private static final	String	OPEN_ARCHIVE_STR			= "Open archive";
	private static final	String	SAVE_ARCHIVE_STR			= "Save archive";
	private static final	String	ADD_FILES_STR				= "Add files to archive";
	private static final	String	VALIDATE_FILES_STR			= "Validate files";
	private static final	String	EXTRACT_FILES_STR			= "Extract files from archive";
	private static final	String	DELETE_FILES_STR			= "Delete files";
	private static final	String	SKIP_THIS_STR				= "Skip this file";
	private static final	String	SKIP_ALL_STR				= "Skip all conflicting files";
	private static final	String	REPLACE_THIS_STR			= "Replace this file";
	private static final	String	REPLACE_ALL_STR				= "Replace all conflicting files";
	private static final	String	ADD_CONFLICT_STR			= "The archive already contains a file with this path.";
	private static final	String	EXTRACT_CONFLICT_STR		= "The file already exists.";
	private static final	String	DO_WHAT_STR					= "What do you want to do?";
	private static final	String	CONFIRM_DELETE_STR			= "Number of files to be deleted = %d\n" +
																	"Do you want to delete the files?";
	private static final	String	ALL_FILES_VALID_STR			= "All files were valid.";
	private static final	String	NUM_PROCESSED_STR			= "Number of files processed = %d\n";
	private static final	String	NUM_FAILED_VALIDATION_STR	= "Number of files that failed validation = %d\n" +
																	"The invalid files have been selected.";
	private static final	String	NONEXISTENT_FILES1_STR		= "Some of the files listed in the archive do not " +
																	"exist.\nThose files have been removed from the " +
																	"list.";
	private static final	String	NONEXISTENT_FILES2_STR		= "Non-existent files removed from archive";
	private static final	String	NOT_ALL_DELETED_STR			= "Not all the selected files were deleted.";
	private static final	String	NOT_DELETED_STR				= "Files that were not deleted";
	private static final	String	LIST_OF_FILES_STR			= "List of selected files";
	private static final	String	MAP_OF_FILES_STR			= "Map of selected files";

	private static final	QuestionDialog.Option[]	CONFLICT_OPTIONS	=
	{
		new QuestionDialog.Option
		(
			ConflictOption.REPLACE.key,
			REPLACE_THIS_STR,
			KeyEvent.VK_R
		),
		new QuestionDialog.Option
		(
			ConflictOption.REPLACE_ALL.key,
			REPLACE_ALL_STR,
			KeyEvent.VK_E
		),
		new QuestionDialog.Option
		(
			ConflictOption.SKIP.key,
			SKIP_THIS_STR,
			KeyEvent.VK_S
		),
		new QuestionDialog.Option
		(
			ConflictOption.SKIP_ALL.key,
			SKIP_ALL_STR,
			KeyEvent.VK_K
		),
		null,
		new QuestionDialog.Option
		(
			QuestionDialog.CANCEL_KEY,
			AppConstants.CANCEL_STR
		)
	};

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	SortingOrder	sortingOrder	= DEFAULT_SORTING_ORDER;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	File			file;
	private	File			archiveDirectory;
	private	int				unnamedIndex;
	private	boolean			executingCommand;
	private	boolean			changed;
	private	ArchiveView		view;
	private	List<Element>	elements;
	private	TableModel		tableModel;
	private	ConflictOption	conflictOption;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public ArchiveDocument()
	{
		elements = new ArrayList<>();
		tableModel = new TableModel();
	}

	//------------------------------------------------------------------

	public ArchiveDocument(int unnamedIndex)
	{
		this();
		this.unnamedIndex = unnamedIndex;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static SortingOrder getSortingOrder()
	{
		return sortingOrder;
	}

	//------------------------------------------------------------------

	private static String hashValueToString(byte[] hashValue)
	{
		if ((hashValue == null) || (hashValue.length != FILE_HASH_VALUE_SIZE))
			throw new IllegalArgumentException();

		StringBuilder buffer = new StringBuilder();
		for (byte b : hashValue)
		{
			buffer.append(NumberUtils.DIGITS_LOWER[(b >> 4) & 0x0F]);
			buffer.append(NumberUtils.DIGITS_LOWER[b & 0x0F]);
		}
		return buffer.toString();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public Kind getKind()
	{
		return Kind.ARCHIVE;
	}

	//------------------------------------------------------------------

	@Override
	public boolean isChanged()
	{
		return changed;
	}

	//------------------------------------------------------------------

	@Override
	public String getName()
	{
		return ((file == null) ? UNNAMED_STR + unnamedIndex : file.getName());
	}

	//------------------------------------------------------------------

	@Override
	public String getTitleString(boolean full)
	{
		String str = (file == null) ? UNNAMED_STR + unnamedIndex
									: full ? Utils.getPathname(file) : file.getName();
		if (isChanged())
			str += AppConstants.FILE_CHANGED_SUFFIX;
		return str;
	}

	//------------------------------------------------------------------

	@Override
	public View createView()
	{
		return new ArchiveView(this);
	}

	//------------------------------------------------------------------

	@Override
	public void updateCommands()
	{
		int numRows = tableModel.getRowCount();
		int numSelectedRows = getTable().getSelectedRowCount();
		boolean isSelection = numSelectedRows > 0;
		boolean isArchiveDirectory = (archiveDirectory != null);

		Command.CHOOSE_ARCHIVE_DIRECTORY.setEnabled(isEmpty());
		Command.SELECT_ALL.setEnabled(numSelectedRows < numRows);
		Command.INVERT_SELECTION.setEnabled(numRows > 0);
		Command.ADD_FILES.setEnabled(isArchiveDirectory && (elements.size() < MAX_NUM_ELEMENTS));
		Command.EXTRACT_FILES.setEnabled(isSelection && isArchiveDirectory);
		Command.VALIDATE_FILES.setEnabled(isSelection && isArchiveDirectory);
		Command.DELETE_FILES.setEnabled(isSelection && isArchiveDirectory);
		Command.DISPLAY_FILE_LIST.setEnabled(isSelection);
		Command.DISPLAY_FILE_MAP.setEnabled(isSelection);
		Command.SET_KEY.setEnabled(true);
		Command.CLEAR_KEY.setEnabled(isKey());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public File getFile()
	{
		return file;
	}

	//------------------------------------------------------------------

	public File getArchiveDirectory()
	{
		return archiveDirectory;
	}

	//------------------------------------------------------------------

	public boolean isExecutingCommand()
	{
		return executingCommand;
	}

	//------------------------------------------------------------------

	public boolean isEmpty()
	{
		return elements.isEmpty();
	}

	//------------------------------------------------------------------

	public TableModel getTableModel()
	{
		return tableModel;
	}

	//------------------------------------------------------------------

	public ArchiveView.Column getColumn(int index)
	{
		return tableModel.columns.get(index);
	}

	//------------------------------------------------------------------

	public void setView(ArchiveView view)
	{
		this.view = view;
	}

	//------------------------------------------------------------------

	public void setArchiveDirectory(File directory)
	{
		archiveDirectory = directory;
		if (view != null)
			view.setArchiveDirectory(directory);
		updateCommands();
	}

	//------------------------------------------------------------------

	public void setSortingOrder(ArchiveView.Column           key,
								ArchiveView.SortingDirection direction)
	{
		sortingOrder = new SortingOrder(key, direction);
		sort();
		tableModel.fireTableDataChanged();
	}

	//------------------------------------------------------------------

	public void read(File file)
		throws AppException
	{
		// Initialise information in progress view
		TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
		progressView.setInfo(READING_STR, file);
		progressView.setProgress(0, 0.0);
		progressView.waitForIdle();

		// Test for file
		if (!file.exists())
			throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, file);
		if (!file.isFile())
			throw new FileException(ErrorId.NOT_A_FILE, file);

		// Set file and archive directory
		this.file = file;
		archiveDirectory = file.getAbsoluteFile().getParentFile();

		// Get key
		KeyList.Key key = getKey(OPEN_ARCHIVE_STR);
		if (key == null)
			throw new TaskCancelledException();

		// Create decrypter
		StreamEncrypter decrypter = key.getStreamEncrypter(null, getOuterHeader());

		// Test file length
		if (file.length() < decrypter.getMinOverheadSize() + METADATA_SIZE)
			throw new FileException(ErrorId.NOT_A_LIST_ARCHIVE_FILE, file);

		// Read file
		FileInputStream inStream = null;
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		try
		{
			// Open input stream on file
			try
			{
				inStream = new FileInputStream(file);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, file, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, file, e);
			}

			// Lock file
			try
			{
				if (inStream.getChannel().tryLock(0, Long.MAX_VALUE, true) == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, file);
			}
			catch (OverlappingFileLockException e)
			{
				// ignore
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, file, e);
			}

			// Decrypt file
			try
			{
				decrypter.addProgressListener(progressView);
				decrypter.decrypt(inStream, outStream, file.length(), key.getKey(), QanaApp.INSTANCE::generateKey);
			}
			catch (StreamEncrypter.InputException e)
			{
				e.setDataDescription(FILE_STR);
				throw new FileException(e, file);
			}

			// Close file
			try
			{
				inStream.close();
				inStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, file, e);
			}
		}
		catch (AppException e)
		{
			// Close file
			try
			{
				if (inStream != null)
					inStream.close();
			}
			catch (IOException e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}

		// Parse data
		parse(outStream.toByteArray(), key.getKey());

		// Sort elements
		progressView.setInfo(SORTING_STR);
		sort();

		// Update progress
		progressView.setProgress(0, 1.0);

		// Set timestamp and clear changed state
		setTimestamp(file.lastModified());
		changed = false;

		// Check for existence of files in archive
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < elements.size(); i++)
		{
			Element element = elements.get(i);
			if (!new File(archiveDirectory, element.getHashValueString()).isFile())
				indices.add(i);
		}

		// Remove non-existent files from list
		if (!indices.isEmpty())
		{
			StringBuilder buffer = new StringBuilder(256);
			for (int index : indices)
			{
				Element element = elements.get(index);
				if (!buffer.isEmpty())
					buffer.append('\n');
				buffer.append(element.path);
				buffer.append("  (");
				buffer.append(element.getHashValueString());
				buffer.append(')');
			}

			for (int i = indices.size() - 1; i >= 0; i--)
			{
				int index = indices.get(i);
				elements.remove(index);
				tableModel.fireTableRowsDeleted(index, index);
			}
			changed = true;

			QanaApp.INSTANCE.showErrorMessage(QanaApp.SHORT_NAME, NONEXISTENT_FILES1_STR);
			TextAreaDialog.showDialog(getWindow(), NONEXISTENT_FILES2_STR, buffer.toString());
		}
	}

	//------------------------------------------------------------------

	public void write(File file)
		throws AppException
	{
		// Initialise information in progress view
		TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
		progressView.setInfo(WRITING_STR, file);
		progressView.setProgress(0, 0.0);
		progressView.waitForIdle();

		// Get key
		KeyList.Key key = getKey(SAVE_ARCHIVE_STR);
		if ((key == null) || !QanaApp.INSTANCE.confirmUseTemporaryKey(key))
			throw new TaskCancelledException();

		// Check cipher
		FortunaCipher cipher = Utils.getCipher(key);
		key.checkAllowedCipher(cipher);

		// Create hash-function object
		HmacSha256 hash = new HmacSha256(key.getKey());

		// Initialise input stream
		ByteBlockInputStream inStream = new ByteBlockInputStream();

		// Add header
		inStream.addBlock(createHeader());

		// Add placeholder for hash value
		byte[] hashValue = new byte[HASH_VALUE_FIELD_SIZE];
		inStream.addBlock(hashValue);

		// Add elements
		StringTable stringTable = new StringTable();
		for (Element element : elements)
		{
			byte[] elementData = element.toByteArray(stringTable);
			inStream.addBlock(elementData);
			hash.update(elementData);
		}

		// Add string table
		byte[] stringTableData = stringTable.toByteArray();
		inStream.addBlock(stringTableData);
		hash.update(stringTableData);

		// Set hash value
		System.arraycopy(hash.getValue(), 0, hashValue, 0, hashValue.length);

		// Write encrypted file
		File tempFile = null;
		FileOutputStream outStream = null;
		boolean oldFileDeleted = false;
		long timestamp = getTimestamp();
		setTimestamp(0);
		try
		{
			// Create parent directory of output file
			File directory = file.getAbsoluteFile().getParentFile();
			if ((directory != null) && !directory.exists())
			{
				try
				{
					if (!directory.mkdirs())
						throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory);
				}
				catch (SecurityException e)
				{
					throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory, e);
				}
			}

			// Create temporary file
			try
			{
				tempFile = FilenameUtils.tempLocation(file);
				tempFile.createNewFile();
			}
			catch (Exception e)
			{
				throw new AppException(ErrorId.FAILED_TO_CREATE_TEMPORARY_FILE, e);
			}

			// Open output stream on temporary file
			try
			{
				outStream = new FileOutputStream(tempFile);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, tempFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, tempFile, e);
			}

			// Lock file
			try
			{
				if (outStream.getChannel().tryLock() == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile, e);
			}

			// Encrypt file
			try
			{
				StreamEncrypter encrypter = key.getStreamEncrypter(cipher, getOuterHeader());
				encrypter.addProgressListener(progressView);
				encrypter.encrypt(inStream, outStream, inStream.getLength(), 0, key.getKey(),
								  QanaApp.INSTANCE.getRandomKey(), QanaApp.INSTANCE::generateKey);
			}
			catch (StreamEncrypter.OutputException e)
			{
				e.setDataDescription(FILE_STR);
				throw new FileException(e, tempFile);
			}

			// Close file
			try
			{
				outStream.close();
				outStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, tempFile, e);
			}

			// Delete any existing file
			try
			{
				if (file.exists() && !file.delete())
					throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, file);
				oldFileDeleted = true;
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, file, e);
			}

			// Rename temporary file
			try
			{
				if (!tempFile.renameTo(file))
					throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, file, tempFile);
			}
			catch (SecurityException e)
			{
				throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, file, e, tempFile);
			}

			// Set timestamp
			timestamp = file.lastModified();

			// Set file and archive directory
			this.file = file;
			setArchiveDirectory(file.getAbsoluteFile().getParentFile());
		}
		catch (AppException e)
		{
			// Close file
			try
			{
				if (outStream != null)
					outStream.close();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Delete temporary file
			try
			{
				if (!oldFileDeleted && (tempFile != null) && tempFile.exists())
					tempFile.delete();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}
		finally
		{
			setTimestamp(timestamp);
		}

		// Clear changed state
		changed = false;
	}

	//------------------------------------------------------------------

	public void encryptFiles(List<InputFile> files)
		throws AppException
	{
		// Reset progress in progress view
		IProgressView progressView = Task.getProgressView();
		progressView.setProgress(0, 0.0);
		progressView.setProgress(1, 0.0);

		// Calculate total length of files
		long totalFileLength = 0;
		for (InputFile inputFile : files)
			totalFileLength += inputFile.file.length();

		// Encrypt files
		conflictOption = null;
		long fileOffset = 0;
		for (InputFile inputFile : files)
		{
			File inFile = inputFile.file;
			try
			{
				// Update progress view
				progressView.setInfo(ENCRYPTING_STR, inFile);
				progressView.setProgress(0, 0.0);
				progressView.waitForIdle();

				// Test for file
				if (!inFile.exists())
					throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, inFile);
				if (!inFile.isFile())
					throw new FileException(ErrorId.NOT_A_FILE, inFile);

				// Find index of file
				int index = findElement(inputFile.path);
				if (index < 0)
					index = elements.size();

				// Prompt for conflicting file
				else
				{
					String[] strs = { inputFile.path, ADD_CONFLICT_STR, DO_WHAT_STR };
					if (!resolveConflict(ADD_FILES_STR, strs))
						index = -1;
				}

				// Encrypt file and add it to archive
				if (index >= 0)
				{
					byte[] key = QanaApp.INSTANCE.getRandomKey();
					byte[] salt = new byte[Element.SALT_FIELD_SIZE];
					byte[] hashValue = encryptFile(inFile, archiveDirectory, key, fileOffset, totalFileLength, salt);
					Element element =
							new Element(inputFile.path, inFile.length(), inFile.lastModified(), key, salt, hashValue);
					if (index < elements.size())
					{
						elements.set(index, element);
						tableModel.fireTableRowsUpdated(index, index);
					}
					else
					{
						elements.add(element);
						tableModel.fireTableRowsInserted(index, index);
					}
					changed = true;
				}
			}
			catch (TaskCancelledException e)
			{
				break;
			}
			catch (AppException e)
			{
				String[] optionStrs = Utils.getOptionStrings(AppConstants.CONTINUE_STR);
				if (JOptionPane.showOptionDialog(getWindow(), e, QanaApp.SHORT_NAME + " : " + ADD_FILES_STR,
												 JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null,
												 optionStrs, optionStrs[0]) != JOptionPane.OK_OPTION)
					break;
			}

			// Increment file offset
			fileOffset += inFile.length();
		}

		// Sort elements
		progressView.setInfo(SORTING_STR);
		sort();
	}

	//------------------------------------------------------------------

	public void decryptFiles(int[] indices,
							 File  outDirectory)
		throws AppException
	{
		// Reset progress in progress view
		IProgressView progressView = Task.getProgressView();
		progressView.setProgress(0, 0.0);
		progressView.setProgress(1, 0.0);

		// Calculate total length of files
		long totalFileLength = 0;
		for (int index : indices)
			totalFileLength += new File(archiveDirectory, elements.get(index).getHashValueString()).length();

		// Decrypt files
		conflictOption = null;
		long fileOffset = 0;
		for (int index : indices)
		{
			Element element = elements.get(index);
			File inFile = new File(archiveDirectory, element.getHashValueString());
			try
			{
				// Update progress view
				progressView.setInfo(DECRYPTING_STR, inFile);
				progressView.setProgress(0, 0.0);

				// Test for file
				if (!inFile.exists())
					throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, inFile);
				if (!inFile.isFile())
					throw new FileException(ErrorId.NOT_A_FILE, inFile);

				// Prompt to replace file
				File outFile = new File(outDirectory, element.path);
				if (outFile.exists())
				{
					String[] strs = { Utils.getPathname(outFile), EXTRACT_CONFLICT_STR, DO_WHAT_STR };
					if (!resolveConflict(EXTRACT_FILES_STR, strs))
						outFile = null;
				}

				// Decrypt file
				if (outFile != null)
				{
					if (!decryptFile(inFile, outFile, element.key, element.salt, element.timestamp, fileOffset,
									 totalFileLength))
						throw new FileException(ErrorId.INCORRECT_ENCRYPTION_KEY, inFile);
					if (outFile.length() != element.size)
						throw new FileException(ErrorId.INCORRECT_FILE_SIZE, outFile);
				}
			}
			catch (TaskCancelledException e)
			{
				break;
			}
			catch (AppException e)
			{
				String[] optionStrs = Utils.getOptionStrings(AppConstants.CONTINUE_STR);
				if (JOptionPane.showOptionDialog(getWindow(), e, QanaApp.SHORT_NAME + " : " + EXTRACT_FILES_STR,
												 JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null,
												 optionStrs, optionStrs[0]) != JOptionPane.OK_OPTION)
					break;
			}

			// Increment file offset
			fileOffset += inFile.length();
		}
	}

	//------------------------------------------------------------------

	public void validateFiles(int[] indices)
		throws AppException
	{
		// Reset progress in progress view
		IProgressView progressView = Task.getProgressView();
		progressView.setProgress(0, 0.0);
		progressView.setProgress(1, 0.0);

		// Calculate total length of files
		long totalFileLength = 0;
		for (int index : indices)
			totalFileLength += new File(archiveDirectory, elements.get(index).getHashValueString()).length();

		// Validate files
		List<Integer> invalidIndices = new ArrayList<>();
		long fileOffset = 0;
		int numValidated = 0;
		for (int index : indices)
		{
			// Get element and input file
			Element element = elements.get(index);
			File inFile = new File(archiveDirectory, element.getHashValueString());

			// Update progress view
			progressView.setInfo(VALIDATING_STR + " " + element.path);
			progressView.setProgress(0, 0.0);

			// Test for input file
			if (!inFile.exists())
				throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, inFile);
			if (!inFile.isFile())
				throw new FileException(ErrorId.NOT_A_FILE, inFile);

			// Validate file
			try
			{
				if (!validateFile(inFile, element.key, element.salt, fileOffset, totalFileLength))
					invalidIndices.add(index);
				++numValidated;
			}
			catch (TaskCancelledException e)
			{
				break;
			}
			catch (AppException e)
			{
				invalidIndices.add(index);
				String[] optionStrs = Utils.getOptionStrings(AppConstants.CONTINUE_STR);
				if (JOptionPane.showOptionDialog(getWindow(), e, QanaApp.SHORT_NAME + " : " + VALIDATE_FILES_STR,
												 JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null,
												 optionStrs, optionStrs[0]) != JOptionPane.OK_OPTION)
					break;
			}

			// Increment file offset
			fileOffset += inFile.length();
		}

		// Display result of validation
		String numProcessedStr = String.format(NUM_PROCESSED_STR, indices.length);
		if ((numValidated == indices.length) && invalidIndices.isEmpty())
			QanaApp.INSTANCE.showInfoMessage(VALIDATE_FILES_STR, numProcessedStr + ALL_FILES_VALID_STR);
		if (!invalidIndices.isEmpty())
		{
			ListSelectionModel selectionModel = getTable().getSelectionModel();
			selectionModel.clearSelection();
			for (int index : invalidIndices)
				selectionModel.addSelectionInterval(index, index);
			String failedStr = String.format(NUM_FAILED_VALIDATION_STR, invalidIndices.size());
			QanaApp.INSTANCE.showWarningMessage(VALIDATE_FILES_STR, numProcessedStr + failedStr);
		}
	}

	//------------------------------------------------------------------

	public void deleteFiles(int[] indices)
		throws AppException
	{
		// Reset progress in progress view
		IProgressView progressView = Task.getProgressView();
		progressView.setProgress(0, 0.0);

		// Clear selection in table
		getTable().clearSelection();

		// Prepare indices for removal of elements
		Arrays.sort(indices);

		// Remove elements from list and delete files
		StringBuilder buffer = new StringBuilder(256);
		for (int i = indices.length - 1; i >= 0; i--)
		{
			// Remove element from list
			int index = indices[i];
			Element element = elements.remove(index);
			changed = true;

			// Update table
			tableModel.fireTableRowsDeleted(index, index);

			// Update pathname in progress view
			File file = new File(archiveDirectory, element.getHashValueString());
			progressView.setInfo(DELETING_STR, file);

			// Delete file
			if (!file.delete())
			{
				if (!buffer.isEmpty())
					buffer.append('\n');
				buffer.append(Utils.getPathname(file));
			}

			// Update progress in progress view
			progressView.setProgress(0, (double)(i + 1) / (double)indices.length);
		}

		// Display list of files that were not deleted
		if (!buffer.isEmpty())
		{
			QanaApp.INSTANCE.showErrorMessage(DELETE_FILES_STR, NOT_ALL_DELETED_STR);
			TextAreaDialog.showDialog(getWindow(), NOT_DELETED_STR, buffer.toString());
		}
	}

	//------------------------------------------------------------------

	public void executeCommand(Command command)
	{
		// Set command execution flag
		executingCommand = true;

		// Perform command
		try
		{
			try
			{
				switch (command)
				{
					case CHOOSE_ARCHIVE_DIRECTORY:
						onChooseArchiveDirectory();
						break;

					case SELECT_ALL:
						onSelectAll();
						break;

					case INVERT_SELECTION:
						onInvertSelection();
						break;

					case ADD_FILES:
						onAddFiles();
						break;

					case EXTRACT_FILES:
						onExtractFiles();
						break;

					case VALIDATE_FILES:
						onValidateFiles();
						break;

					case DELETE_FILES:
						onDeleteFiles();
						break;

					case DISPLAY_FILE_LIST:
						onDisplayFileList();
						break;

					case DISPLAY_FILE_MAP:
						onDisplayFileMap();
						break;

					case SET_KEY:
						onSetKey();
						break;

					case CLEAR_KEY:
						onClearKey();
						break;
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

		// Clear command execution flag
		executingCommand = false;
	}

	//------------------------------------------------------------------

	private ArchiveView.Table getTable()
	{
		return ((view == null) ? null : view.getTable());
	}

	//------------------------------------------------------------------

	private StreamEncrypter.Header getOuterHeader()
	{
		return new StreamEncrypter.Header(ENCRYPTION_ID, VERSION, MIN_SUPPORTED_VERSION, MAX_SUPPORTED_VERSION);
	}

	//------------------------------------------------------------------

	private void sort()
	{
		elements.sort((element1, element2) ->
		{
			int result = 0;
			switch (sortingOrder.key)
			{
				case PATH:
					break;

				case SIZE:
					result = Long.compare(element1.size, element2.size);
					break;

				case TIMESTAMP:
					result = Long.compare(element1.timestamp, element2.timestamp);
					break;

				case HASH_VALUE:
					for (int i = 0; i < element1.hashValue.length; i++)
					{
						result = Long.compare(element1.hashValue[i] & 0xFFFFFFFFL,
											  element2.hashValue[i] & 0xFFFFFFFFL);
						if (result != 0)
							break;
					}
					break;
			}
			if (result == 0)
				result = element1.compareTo(element2);
			return (sortingOrder.direction == ArchiveView.SortingDirection.ASCENDING) ? result : -result;
		});
	}

	//------------------------------------------------------------------

	private int findElement(String path)
	{
		for (int i = 0; i < elements.size(); i++)
		{
			if (elements.get(i).path.equals(path))
				return i;
		}
		return -1;
	}

	//------------------------------------------------------------------

	private boolean resolveConflict(String   title,
									String[] messageStrs)
		throws TaskCancelledException
	{
		boolean replace = false;
		if (conflictOption == null)
		{
			String optionKey = QuestionDialog.showDialog(getWindow(), title, messageStrs, CONFLICT_OPTIONS, 2,
														 QuestionDialog.CANCEL_KEY, null).selectedKey();
			if (QuestionDialog.CANCEL_KEY.equals(optionKey))
				throw new TaskCancelledException();
			ConflictOption option = ConflictOption.get(optionKey);
			if (option == ConflictOption.REPLACE)
				replace = true;
			if (option.isAll())
				conflictOption = option;
		}
		if (conflictOption == ConflictOption.REPLACE_ALL)
			replace = true;
		return replace;
	}

	//------------------------------------------------------------------

	private void parse(byte[] data,
					   byte[] key)
		throws AppException
	{
		// Parse field: string table offset
		int offset = 0;
		int length = STRING_TABLE_OFFSET_FIELD_SIZE;
		int stringTableOffset = NumberCodec.bytesToUIntLE(data, offset, length);
		offset += length;
		if (stringTableOffset >= data.length)
			throw new AppException(ErrorId.MALFORMED_LIST_ARCHIVE_FILE);

		// Parse field: number of elements
		length = NUM_ELEMENTS_FIELD_SIZE;
		int numElements = NumberCodec.bytesToUIntLE(data, offset, length);
		offset += length;

		// Extract field: hash value
		length = HASH_VALUE_FIELD_SIZE;
		byte[] hashValue = Arrays.copyOfRange(data, offset, offset + length);
		offset += length;

		// Test hash value
		if (!Arrays.equals(hashValue, new HmacSha256(key).getValue(data, offset, data.length - offset)))
			throw new AppException(ErrorId.INCORRECT_ENCRYPTION_KEY);

		// Parse string table
		StringTable stringTable = new StringTable(data, stringTableOffset, data.length - stringTableOffset);

		// Parse elements
		for (int i = 0; i < numElements; i++)
		{
			elements.add(new Element(data, offset, stringTable));
			offset += Element.SIZE;
		}
	}

	//------------------------------------------------------------------

	private byte[] createHeader()
	{
		byte[] buffer = new byte[HEADER_SIZE];
		int offset = 0;

		// Set field: string table offset
		int length = STRING_TABLE_OFFSET_FIELD_SIZE;
		NumberCodec.uIntToBytesLE(METADATA_SIZE + elements.size() * Element.SIZE, buffer, offset, length);
		offset += length;

		// Set field: number of elements
		length = NUM_ELEMENTS_FIELD_SIZE;
		NumberCodec.uIntToBytesLE(elements.size(), buffer, offset, length);
		offset += length;

		return buffer;
	}

	//------------------------------------------------------------------

	private byte[] encryptFile(File   inFile,
							   File   outDirectory,
							   byte[] key,
							   long   fileOffset,
							   long   totalFileLength,
							   byte[] salt)
		throws AppException
	{
		// Encrypt file
		File tempFile = null;
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
		boolean oldFileDeleted = false;
		try
		{
			// Create temporary file
			try
			{
				tempFile = File.createTempFile(null, TEMPORARY_FILENAME_EXTENSION, outDirectory);
			}
			catch (Exception e)
			{
				throw new AppException(ErrorId.FAILED_TO_CREATE_TEMPORARY_FILE, e);
			}

			// Open output stream on temporary file
			try
			{
				outStream = new FileOutputStream(tempFile);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, tempFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, tempFile, e);
			}

			// Lock output file
			try
			{
				if (outStream.getChannel().tryLock() == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile, e);
			}

			// Open input stream on input file
			try
			{
				inStream = new FileInputStream(inFile);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, inFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, inFile, e);
			}

			// Lock input file
			try
			{
				if (inStream.getChannel().tryLock(0, Long.MAX_VALUE, true) == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, inFile);
			}
			catch (OverlappingFileLockException e)
			{
				// ignore
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, inFile, e);
			}

			// Initialise overall progress in progress view
			TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
			progressView.initOverallProgress(fileOffset, inFile.length(), totalFileLength);

			// Encrypt data
			StreamEncrypter encrypter = new StreamEncrypter(Utils.getCipher(getKey()));
			encrypter.addProgressListener(progressView);
			encrypter.encrypt(inStream, outStream, inFile.length(), QanaApp.INSTANCE.getRandomLong(), key,
							  QanaApp.INSTANCE.getRandomKey(), QanaApp.INSTANCE::generateKey);

			// Close input file
			try
			{
				inStream.close();
				inStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, inFile, e);
			}

			// Close output file
			try
			{
				outStream.close();
				outStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, tempFile, e);
			}

			// Generate hash value from hash value of file content and random salt
			byte[] hashValue = null;
			while (hashValue == null)
			{
				System.arraycopy(QanaApp.INSTANCE.getRandomBytes(salt.length), 0, salt, 0, salt.length);
				hashValue = new HashGenerator().generate(encrypter, salt);
				for (Element element : elements)
				{
					if (Arrays.equals(hashValue, element.hashValue))
					{
						hashValue = null;
						break;
					}
				}
			}

			// Create output file object
			File outFile = new File(outDirectory, hashValueToString(hashValue));

			// Delete any existing file
			try
			{
				if (outFile.exists() && !outFile.delete())
					throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, outFile);
				oldFileDeleted = true;
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, outFile, e);
			}

			// Rename temporary file
			try
			{
				if (!tempFile.renameTo(outFile))
					throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, outFile, tempFile);
			}
			catch (SecurityException e)
			{
				throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, outFile, e, tempFile);
			}

			// Return hash value
			return hashValue;
		}
		catch (AppException e)
		{
			// Close input file
			try
			{
				if (inStream != null)
					inStream.close();
			}
			catch (IOException e1)
			{
				// ignore
			}

			// Close output file
			try
			{
				if (outStream != null)
					outStream.close();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Delete temporary file
			try
			{
				if (!oldFileDeleted && (tempFile != null) && tempFile.exists())
					tempFile.delete();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

	private boolean decryptFile(File   inFile,
								File   outFile,
								byte[] key,
								byte[] salt,
								long   timestamp,
								long   fileOffset,
								long   totalFileLength)
		throws AppException
	{
		// Create output directory
		File outDirectory = outFile.getAbsoluteFile().getParentFile();
		if ((outDirectory != null) && !outDirectory.exists() && !outDirectory.mkdirs())
			throw new FileException(ErrorId.FAILED_TO_CREATE_OUTPUT_DIRECTORY, outDirectory);

		// Decrypt file
		File tempFile = null;
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
		boolean oldFileDeleted = false;
		try
		{
			// Create parent directory of output file
			File directory = outFile.getAbsoluteFile().getParentFile();
			if ((directory != null) && !directory.exists())
			{
				try
				{
					if (!directory.mkdirs())
						throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory);
				}
				catch (SecurityException e)
				{
					throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory, e);
				}
			}

			// Create temporary file
			try
			{
				tempFile = FilenameUtils.tempLocation(outFile);
				tempFile.createNewFile();
			}
			catch (Exception e)
			{
				throw new AppException(ErrorId.FAILED_TO_CREATE_TEMPORARY_FILE, e);
			}

			// Open output stream on temporary file
			try
			{
				outStream = new FileOutputStream(tempFile);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, tempFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, tempFile, e);
			}

			// Lock output file
			try
			{
				if (outStream.getChannel().tryLock() == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile, e);
			}

			// Open input stream on input file
			try
			{
				inStream = new FileInputStream(inFile);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, inFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, inFile, e);
			}

			// Lock input file
			try
			{
				if (inStream.getChannel().tryLock(0, Long.MAX_VALUE, true) == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, inFile);
			}
			catch (OverlappingFileLockException e)
			{
				// ignore
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, inFile, e);
			}

			// Initialise overall progress in progress view
			TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
			progressView.initOverallProgress(fileOffset, inFile.length(), totalFileLength);

			// Decrypt file
			StreamEncrypter decrypter = new StreamEncrypter(null);
			decrypter.addProgressListener(progressView);
			decrypter.decrypt(inStream, outStream, inFile.length(), key, QanaApp.INSTANCE::generateKey);

			// Close input file
			try
			{
				inStream.close();
				inStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, inFile, e);
			}

			// Close output file
			try
			{
				outStream.close();
				outStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, tempFile, e);
			}

			// Generate hash value from hash value of file content and salt
			byte[] hashValue = new HashGenerator().generate(decrypter, salt);

			// Test hash value
			if (inFile.getName().equals(hashValueToString(hashValue)))
			{
				// Delete any existing file
				try
				{
					if (outFile.exists() && !outFile.delete())
						throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, outFile);
					oldFileDeleted = true;
				}
				catch (SecurityException e)
				{
					throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, outFile, e);
				}

				// Rename temporary file
				try
				{
					if (!tempFile.renameTo(outFile))
						throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, outFile, tempFile);
				}
				catch (SecurityException e)
				{
					throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, outFile, e, tempFile);
				}

				// Set timestamp of file
				outFile.setLastModified(timestamp);
				return true;
			}
			else
			{
				// Delete temporary file
				try
				{
					if (!tempFile.delete())
						throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, tempFile);
				}
				catch (SecurityException e)
				{
					throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, tempFile, e);
				}
				return false;
			}
		}
		catch (AppException e)
		{
			// Close input file
			try
			{
				if (inStream != null)
					inStream.close();
			}
			catch (IOException e1)
			{
				// ignore
			}

			// Close output file
			try
			{
				if (outStream != null)
					outStream.close();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Delete temporary file
			try
			{
				if (!oldFileDeleted && (tempFile != null) && tempFile.exists())
					tempFile.delete();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

	private boolean validateFile(File   inFile,
								 byte[] key,
								 byte[] salt,
								 long   fileOffset,
								 long   totalFileLength)
		throws AppException
	{
		FileInputStream inStream = null;
		try
		{
			// Open input stream on input file
			try
			{
				inStream = new FileInputStream(inFile);
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, inFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, inFile, e);
			}

			// Lock input file
			try
			{
				if (inStream.getChannel().tryLock(0, Long.MAX_VALUE, true) == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, inFile);
			}
			catch (OverlappingFileLockException e)
			{
				// ignore
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, inFile, e);
			}

			// Initialise overall progress in progress view
			TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
			progressView.initOverallProgress(fileOffset, inFile.length(), totalFileLength);

			// Decrypt file
			StreamEncrypter decrypter = new StreamEncrypter(null);
			decrypter.addProgressListener(progressView);
			decrypter.decrypt(inStream, new NullOutputStream(), inFile.length(), key, QanaApp.INSTANCE::generateKey);

			// Close input file
			try
			{
				inStream.close();
				inStream = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, inFile, e);
			}

			// Generate hash value from hash value of file content and salt
			byte[] hashValue = new HashGenerator().generate(decrypter, salt);

			// Test hash value
			return inFile.getName().equals(hashValueToString(hashValue));
		}
		catch (AppException e)
		{
			// Close input file
			try
			{
				if (inStream != null)
					inStream.close();
			}
			catch (IOException e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

	private void onChooseArchiveDirectory()
	{
		QanaApp.INSTANCE.chooseArchiveDirectory(archiveDirectory);
	}

	//------------------------------------------------------------------

	private void onSelectAll()
	{
		getTable().selectAll();
	}

	//------------------------------------------------------------------

	private void onInvertSelection()
	{
		getTable().invertSelection();
	}

	//------------------------------------------------------------------

	private void onAddFiles()
		throws AppException
	{
		// Select files
		List<FileSelectionPanel.SelectedFile> selectedFiles =
				FileSelectionDialog.showDialog(getWindow(), ADD_FILES_STR, MAX_NUM_ELEMENTS);

		// Add files to archive
		if (selectedFiles != null)
		{
			// Test whether size of archive has been exceeded
			int numFiles = selectedFiles.size();
			if (elements.size() + numFiles > MAX_NUM_ELEMENTS)
			{
				throw new AppException(ErrorId.TOO_MANY_FILES, Integer.toString(numFiles),
									   Integer.toString(MAX_NUM_ELEMENTS - elements.size()));
			}

			// Get key
			KeyList.Key key = getKey(ADD_FILES_STR);

			// Add files
			if (key != null)
			{
				// Check cipher
				key.checkAllowedCipher(Utils.getCipher(key));

				// Encrypt files
				List<InputFile> files = new ArrayList<>();
				for (FileSelectionPanel.SelectedFile selectedFile : selectedFiles)
					files.add(new InputFile(selectedFile.file, selectedFile.relativePathname));
				TaskProgressDialog.showDialog2(getWindow(), ADD_FILES_STR, new Task.EncryptFiles(this, files));
			}
		}
	}

	//------------------------------------------------------------------

	private void onExtractFiles()
		throws AppException
	{
		int[] indices = getTable().getSelectedRows();
		if (indices.length > 0)
		{
			File outDirectory = OutputDirectoryDialog.showDialog(getWindow());
			if (outDirectory != null)
			{
				TaskProgressDialog.showDialog2(getWindow(), EXTRACT_FILES_STR,
											   new Task.DecryptFiles(this, indices, outDirectory));
			}
		}
	}

	//------------------------------------------------------------------

	private void onValidateFiles()
		throws AppException
	{
		int[] indices = getTable().getSelectedRows();
		if (indices.length > 0)
			TaskProgressDialog.showDialog2(getWindow(), VALIDATE_FILES_STR, new Task.ValidateFiles(this, indices));
	}

	//------------------------------------------------------------------

	private void onDeleteFiles()
		throws AppException
	{
		int[] indices = getTable().getSelectedRows();
		if (indices.length > 0)
		{
			String messageStr = String.format(CONFIRM_DELETE_STR, indices.length);
			String[] optionStrs = Utils.getOptionStrings(AppConstants.DELETE_STR);
			if (JOptionPane.showOptionDialog(getWindow(), messageStr, DELETE_FILES_STR, JOptionPane.OK_CANCEL_OPTION,
											 JOptionPane.QUESTION_MESSAGE, null, optionStrs,
											 optionStrs[1]) == JOptionPane.OK_OPTION)
				TaskProgressDialog.showDialog(getWindow(), DELETE_FILES_STR, new Task.DeleteFiles(this, indices));
		}
	}

	//------------------------------------------------------------------

	private void onDisplayFileList()
	{
		int[] indices = getTable().getSelectedRows();
		if (indices.length > 0)
		{
			StringBuilder buffer = new StringBuilder(4096);
			for (int index : indices)
			{
				buffer.append(elements.get(index).path);
				buffer.append('\n');
			}
			TextAreaDialog.showDialog(getWindow(), LIST_OF_FILES_STR, buffer.toString());
		}
	}

	//------------------------------------------------------------------

	private void onDisplayFileMap()
	{
		int[] indices = getTable().getSelectedRows();
		if (indices.length > 0)
		{
			StringBuilder buffer = new StringBuilder(4096);
			for (int index : indices)
			{
				Element element = elements.get(index);
				buffer.append(element.path);
				buffer.append('\t');
				buffer.append(element.getHashValueString());
				buffer.append('\n');
			}
			TextAreaDialog.showDialog(getWindow(), MAP_OF_FILES_STR, buffer.toString());
		}
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


	// ENUMERATION: COMMANDS


	enum Command
		implements Action
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		CHOOSE_ARCHIVE_DIRECTORY
		(
			"chooseArchiveDirectory",
			"Choose archive directory" + AppConstants.ELLIPSIS_STR,
			KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)
		),

		SELECT_ALL
		(
			"selectAll",
			"Select all",
			KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK)
		),

		INVERT_SELECTION
		(
			"invertSelection",
			"Invert selection"
		),

		ADD_FILES
		(
			"addFiles",
			"Add files" + AppConstants.ELLIPSIS_STR,
			KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)
		),

		EXTRACT_FILES
		(
			"extractFiles",
			"Extract files" + AppConstants.ELLIPSIS_STR,
			KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)
		),

		VALIDATE_FILES
		(
			"validateFiles",
			"Validate files"
		),

		DELETE_FILES
		(
			"deleteFiles",
			"Delete files",
			KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)
		),

		DISPLAY_FILE_LIST
		(
			"displayFileList",
			"Display list of files",
			KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)
		),

		DISPLAY_FILE_MAP
		(
			"displayFileMap",
			"Display map of files",
			KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)
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
			ArchiveDocument document = QanaApp.INSTANCE.getArchiveDocument();
			if (document != null)
				document.executeCommand(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public void execute()
		{
			actionPerformed(null);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// ENUMERATION: CONFLICT OPTIONS


	private enum ConflictOption
		implements IStringKeyed
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		REPLACE
		(
			"replace"
		),

		REPLACE_ALL
		(
			"replaceAll"
		),

		SKIP
		(
			"skip"
		),

		SKIP_ALL
		(
			"skipAll"
		);

		//--------------------------------------------------------------

		private static final	String	ALL_SUFFIX	= "All";

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	key;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ConflictOption(String key)
		{
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static ConflictOption get(String key)
		{
			for (ConflictOption value : values())
			{
				if (value.key.equals(key))
					return value;
			}
			return null;
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
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private boolean isAll()
		{
			return key.endsWith(ALL_SUFFIX);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// ENUMERATION: ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FAILED_TO_OPEN_FILE
		("Failed to open the file."),

		FAILED_TO_CLOSE_FILE
		("Failed to close the file."),

		FAILED_TO_LOCK_FILE
		("Failed to lock the file."),

		FILE_ACCESS_NOT_PERMITTED
		("Access to the file was not permitted."),

		FAILED_TO_CREATE_DIRECTORY
		("Failed to create the directory."),

		FAILED_TO_CREATE_TEMPORARY_FILE
		("Failed to create a temporary file."),

		FAILED_TO_DELETE_FILE
		("Failed to delete the existing file."),

		FAILED_TO_RENAME_FILE
		("Failed to rename the temporary file to the specified filename."),

		FILE_DOES_NOT_EXIST
		("The file does not exist."),

		NOT_A_FILE
		("The pathname does not denote a normal file."),

		FAILED_TO_CREATE_OUTPUT_DIRECTORY
		("Failed to create the output directory."),

		NOT_A_LIST_ARCHIVE_FILE
		("The file is not a list archive file."),

		MALFORMED_LIST_ARCHIVE_FILE
		("The list archive file is malformed."),

		INCORRECT_ENCRYPTION_KEY
		("The current key does not match the one that was used to encrypt the file."),

		INCORRECT_FILE_SIZE
		("The size of the extracted file does not match the stored size."),

		TOO_MANY_FILES
		("Adding all the selected files would exceed the maximum length of the archive.\n"
			+ "Number of selected files = %1\n"
			+ "Remaining capacity of archive = %2"),

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

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: SORTING ORDER


	public static class SortingOrder
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		ArchiveView.Column				key;
		ArchiveView.SortingDirection	direction;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private SortingOrder(ArchiveView.Column           key,
							 ArchiveView.SortingDirection direction)
		{
			this.key = key;
			this.direction = direction;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: INPUT FILE


	public static class InputFile
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		File	file;
		String	path;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private InputFile(File   file,
						  String path)
		{
			this.file = file;
			this.path = path;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: LIST ELEMENT


	private static class Element
		implements Comparable<Element>
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	PATH_OFFSET_FIELD_SIZE	= 4;
		private static final	int	SIZE_FIELD_SIZE			= 8;
		private static final	int	TIMESTAMP_FIELD_SIZE	= 8;
		private static final	int	KEY_FIELD_SIZE			= ENCRYPTION_KEY_SIZE;
		private static final	int	SALT_FIELD_SIZE			= FILE_HASH_VALUE_SIZE;
		private static final	int	HASH_VALUE_FIELD_SIZE	= FILE_HASH_VALUE_SIZE;

		private static final	int	SIZE	= PATH_OFFSET_FIELD_SIZE + SIZE_FIELD_SIZE + TIMESTAMP_FIELD_SIZE
												+ KEY_FIELD_SIZE + SALT_FIELD_SIZE + HASH_VALUE_FIELD_SIZE;

		private static final	char	FILE_SEPARATOR_CHAR	= '/';

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	path;
		private	long	size;
		private	long	timestamp;
		private	byte[]	key;
		private	byte[]	salt;
		private	byte[]	hashValue;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Element(String path,
						long   size,
						long   timestamp,
						byte[] key,
						byte[] salt,
						byte[] hashValue)
		{
			this.path = path;
			this.size = size;
			this.timestamp = timestamp;
			this.key = key;
			this.salt = salt;
			this.hashValue = hashValue;
		}

		//--------------------------------------------------------------

		private Element(byte[]      data,
						int         offset,
						StringTable stringTable)
			throws AppException
		{
			// Test whether element extends beyond end of data
			if (offset + SIZE > data.length)
				throw new AppException(ErrorId.MALFORMED_LIST_ARCHIVE_FILE);

			// Parse field: path offset
			int length = PATH_OFFSET_FIELD_SIZE;
			int pathOffset = NumberCodec.bytesToUIntLE(data, offset, length);
			offset += length;

			// Get path from string table
			path = stringTable.get(pathOffset);
			if (path == null)
				throw new AppException(ErrorId.MALFORMED_LIST_ARCHIVE_FILE);

			// Parse field: size
			length = SIZE_FIELD_SIZE;
			size = NumberCodec.bytesToULongLE(data, offset, length);
			offset += length;

			// Parse field: timestamp
			length = TIMESTAMP_FIELD_SIZE;
			timestamp = NumberCodec.bytesToULongLE(data, offset, length);
			offset += length;

			// Parse field: key
			length = KEY_FIELD_SIZE;
			key = Arrays.copyOfRange(data, offset, offset + length);
			offset += length;

			// Extract field: salt
			length = SALT_FIELD_SIZE;
			salt = Arrays.copyOfRange(data, offset, offset + length);
			offset += length;

			// Extract field: hash value
			length = HASH_VALUE_FIELD_SIZE;
			hashValue = Arrays.copyOfRange(data, offset, offset + length);
			offset += length;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Comparable interface
	////////////////////////////////////////////////////////////////////

		@Override
		public int compareTo(Element element)
		{
			List<String> path1 = getPathComponents();
			String name1 = path1.remove(path1.size() - 1);
			List<String> path2 = element.getPathComponents();
			String name2 = path2.remove(path2.size() - 1);

			int index = 0;
			while (true)
			{
				if (path1.size() == index)
					return ((path2.size() == index) ? name1.compareTo(name2) : -1);
				else
				{
					if (path2.size() == index)
						return 1;
				}
				int result = path1.get(index).compareTo(path2.get(index));
				if (result != 0)
					return result;
				++index;
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;

			return (obj instanceof Element other) && Objects.equals(path, other.path);
		}

		//--------------------------------------------------------------

		@Override
		public int hashCode()
		{
			return Objects.hashCode(path);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private String getHashValueString()
		{
			return hashValueToString(hashValue);
		}

		//--------------------------------------------------------------

		private List<String> getPathComponents()
		{
			List<String> pathnameComponents = new ArrayList<>();
			if (path != null)
			{
				int index = 0;
				while (index < path.length())
				{
					int startIndex = index;
					index = path.indexOf(FILE_SEPARATOR_CHAR, index);
					if (index < 0)
						index = path.length();
					pathnameComponents.add(path.substring(startIndex, index));
					++index;
				}
			}
			return pathnameComponents;
		}

		//--------------------------------------------------------------

		private byte[] toByteArray(StringTable stringTable)
		{
			byte[] buffer = new byte[SIZE];
			int offset = 0;

			// Set field: path offset
			int length = PATH_OFFSET_FIELD_SIZE;
			NumberCodec.uIntToBytesLE(stringTable.add(path), buffer, offset, length);
			offset += length;

			// Set field: size
			length = SIZE_FIELD_SIZE;
			NumberCodec.uLongToBytesLE(size, buffer, offset, length);
			offset += length;

			// Set field: timestamp
			length = TIMESTAMP_FIELD_SIZE;
			NumberCodec.uLongToBytesLE(timestamp, buffer, offset, length);
			offset += length;

			// Set field: key
			length = KEY_FIELD_SIZE;
			System.arraycopy(key, 0, buffer, offset, length);
			offset += length;

			// Set field: salt
			length = SALT_FIELD_SIZE;
			System.arraycopy(salt, 0, buffer, offset, length);
			offset += length;

			// Set field: hash value
			length = HASH_VALUE_FIELD_SIZE;
			System.arraycopy(hashValue, 0, buffer, offset, length);
			offset += length;

			return buffer;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: HASH GENERATOR


	private static class HashGenerator
		extends ScryptSalsa20
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	NUM_ITERATIONS	= 4;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private HashGenerator()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private byte[] generate(StreamEncrypter encrypter,
								byte[]          salt)
		{
			return pbkdf2HmacSha256(encrypter.getHashValue(), salt, NUM_ITERATIONS, HASH_VALUE_FIELD_SIZE);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: TEXT AREA DIALOG


	private static class TextAreaDialog
		extends NonEditableTextAreaDialog
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int		NUM_COLUMNS	= 72;
		private static final	int		NUM_ROWS	= 20;

		private static final	String	KEY	= TextAreaDialog.class.getCanonicalName();

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private TextAreaDialog(Window owner,
							   String title,
							   String text)
		{
			super(owner, title, KEY, NUM_COLUMNS, NUM_ROWS, text);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static TextAreaDialog showDialog(Component parent,
												 String    title,
												 String    text)
		{
			return new TextAreaDialog(GuiUtils.getWindow(parent), title, text);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected void setTextAreaAttributes()
		{
			setCaretToStart();
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: TABLE MODEL


	private class TableModel
		extends AbstractTableModel
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	List<ArchiveView.Column>	columns;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private TableModel()
		{
			columns = new ArrayList<>();
			for (ArchiveView.Column column : ArchiveView.Column.values())
			{
				if (column.isVisible())
					columns.add(column);
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public int getRowCount()
		{
			return elements.size();
		}

		//--------------------------------------------------------------

		@Override
		public int getColumnCount()
		{
			return columns.size();
		}

		//--------------------------------------------------------------

		@Override
		public Object getValueAt(int row,
								 int column)
		{
			Object value = null;
			if (row < elements.size())
			{
				Element element = elements.get(row);
				switch (columns.get(column))
				{
					case PATH:
						value = element.path;
						break;

					case SIZE:
						value = Long.toString(element.size);
						break;

					case TIMESTAMP:
						value = CalendarTime.timeToString(element.timestamp, "  ");
						break;

					case HASH_VALUE:
						value = element.getHashValueString();
						break;
				}
			}
			return value;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
