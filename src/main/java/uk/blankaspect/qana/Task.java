/*====================================================================*\

Task.java

Task class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

import java.util.List;

import uk.blankaspect.common.crypto.FortunaCipher;
import uk.blankaspect.common.crypto.StandardCsprng;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.TaskCancelledException;

import uk.blankaspect.common.platform.windows.FileAssociations;

//----------------------------------------------------------------------


// TASK CLASS


abstract class Task
	extends uk.blankaspect.common.misc.Task
{

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// READ KEY LIST TASK CLASS


	public static class ReadKeyList
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public ReadKeyList(KeyList keyList,
						   File    file)
		{
			this.keyList = keyList;
			this.file = file;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				keyList.read(file);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	KeyList	keyList;
		private	File	file;

	}

	//==================================================================


	// WRITE KEY LIST TASK CLASS


	public static class WriteKeyList
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public WriteKeyList(KeyList keyList,
							File    file)
		{
			this.keyList = keyList;
			this.file = file;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				keyList.write(file);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	KeyList	keyList;
		private	File	file;

	}

	//==================================================================


	// READ SEED FILE TASK CLASS


	public static class ReadSeedFile
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public ReadSeedFile(StandardCsprng prng,
							File           directory,
							int[]          randomDataLengthBuffer)
		{
			this.prng = prng;
			this.directory = directory;
			this.randomDataLengthBuffer = randomDataLengthBuffer;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				randomDataLengthBuffer[0] = prng.readSeedFile(directory);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	StandardCsprng	prng;
		private	File			directory;
		private	int[]			randomDataLengthBuffer;

	}

	//==================================================================


	// WRITE SEED FILE TASK CLASS


	public static class WriteSeedFile
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public WriteSeedFile(StandardCsprng prng,
							 File           directory)
		{
			this.prng = prng;
			this.directory = directory;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				prng.writeSeedFile(directory);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	StandardCsprng	prng;
		private	File			directory;

	}

	//==================================================================


	// READ DOCUMENT TASK CLASS


	public static class ReadDocument
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public ReadDocument(ArchiveDocument document,
							File            file)
		{
			this.document = document;
			this.file = file;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				document.read(file);
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	ArchiveDocument	document;
		private	File			file;

	}

	//==================================================================


	// WRITE DOCUMENT TASK CLASS


	public static class WriteDocument
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public WriteDocument(ArchiveDocument document,
							 File            file)
		{
			this.document = document;
			this.file = file;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				document.write(file);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	ArchiveDocument	document;
		private	File			file;

	}

	//==================================================================


	// ENCRYPT FILES TASK CLASS


	public static class EncryptFiles
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public EncryptFiles(ArchiveDocument                 document,
							List<ArchiveDocument.InputFile> files)
		{
			this.document = document;
			this.files = files;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				document.encryptFiles(files);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	ArchiveDocument					document;
		private	List<ArchiveDocument.InputFile>	files;

	}

	//==================================================================


	// DECRYPT FILES TASK CLASS


	public static class DecryptFiles
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public DecryptFiles(ArchiveDocument document,
							int[]           indices,
							File            outDirectory)
		{
			this.document = document;
			this.indices = indices;
			this.outDirectory = outDirectory;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				document.decryptFiles(indices, outDirectory);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	ArchiveDocument	document;
		private	int[]			indices;
		private	File			outDirectory;

	}

	//==================================================================


	// VALIDATE FILES TASK CLASS


	public static class ValidateFiles
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public ValidateFiles(ArchiveDocument document,
							 int[]           indices)
		{
			this.document = document;
			this.indices = indices;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				document.validateFiles(indices);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	ArchiveDocument	document;
		private	int[]			indices;

	}

	//==================================================================


	// DELETE FILES TASK CLASS


	public static class DeleteFiles
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public DeleteFiles(ArchiveDocument document,
						   int[]           indices)
		{
			this.document = document;
			this.indices = indices;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				document.deleteFiles(indices);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	ArchiveDocument	document;
		private	int[]			indices;

	}

	//==================================================================


	// ENCRYPT TASK CLASS


	public static class Encrypt
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public Encrypt(File          inFile,
					   File          outFile,
					   FortunaCipher cipher,
					   KeyList.Key   key)
		{
			this.inFile = inFile;
			this.outFile = outFile;
			this.cipher = cipher;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				key.getFileEncrypter(cipher, App.getEncryptionHeader()).
							encrypt(inFile, outFile, key.getKey(), App.INSTANCE.getRandomKey());
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	File			inFile;
		private	File			outFile;
		private	FortunaCipher	cipher;
		private	KeyList.Key		key;

	}

	//==================================================================


	// DECRYPT TASK CLASS


	public static class Decrypt
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public Decrypt(File        inFile,
					   File        outFile,
					   KeyList.Key key)
		{
			this.inFile = inFile;
			this.outFile = outFile;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				key.getFileEncrypter(null, App.getEncryptionHeader()).
																decrypt(inFile, outFile, key.getKey());
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	File		inFile;
		private	File		outFile;
		private	KeyList.Key	key;

	}

	//==================================================================


	// VALIDATE TASK CLASS


	public static class Validate
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public Validate(File        file,
						KeyList.Key key)
		{
			this.file = file;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				key.getFileEncrypter(null, App.getEncryptionHeader()).validate(file, key.getKey());
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	File		file;
		private	KeyList.Key	key;

	}

	//==================================================================


	// ENCRYPT TEXT TASK CLASS


	public static class EncryptText
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public EncryptText(TextDocument document,
						   KeyList.Key  key)
		{
			this.document = document;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				document.encrypt(key);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	TextDocument	document;
		private	KeyList.Key		key;

	}

	//==================================================================


	// DECRYPT TEXT TASK CLASS


	public static class DecryptText
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public DecryptText(TextDocument document,
						   KeyList.Key  key)
		{
			this.document = document;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				document.decrypt(key);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	TextDocument	document;
		private	KeyList.Key		key;

	}

	//==================================================================


	// CONCEAL FILE TASK CLASS


	public static class ConcealFile
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public ConcealFile(File        inFile,
						   File        carrierFile,
						   File        outFile,
						   int         maxNumBits,
						   boolean     setTimestamp,
						   boolean     addRandomBits,
						   KeyList.Key key)
		{
			this.inFile = inFile;
			this.carrierFile = carrierFile;
			this.outFile = outFile;
			this.maxNumBits = maxNumBits;
			this.setTimestamp = setTimestamp;
			this.addRandomBits = addRandomBits;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				App.INSTANCE.concealFile(inFile, carrierFile, outFile, maxNumBits, setTimestamp,
											  addRandomBits, key);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	File		inFile;
		private	File		carrierFile;
		private	File		outFile;
		private	int			maxNumBits;
		private	boolean		setTimestamp;
		private	boolean		addRandomBits;
		private	KeyList.Key	key;

	}

	//==================================================================


	// RECOVER FILE TASK CLASS


	public static class RecoverFile
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public RecoverFile(File        inFile,
						   File        outFile,
						   KeyList.Key key)
		{
			this.inFile = inFile;
			this.outFile = outFile;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				App.INSTANCE.recoverFile(inFile, outFile, key);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	File		inFile;
		private	File		outFile;
		private	KeyList.Key	key;

	}

	//==================================================================


	// CONCEAL TEXT TASK CLASS


	public static class ConcealText
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public ConcealText(TextDocument document,
						   File         carrierFile,
						   File         outFile,
						   int          maxNumBits,
						   boolean      setTimestamp,
						   boolean      addRandomBits,
						   KeyList.Key  key)
		{
			this.document = document;
			this.carrierFile = carrierFile;
			this.outFile = outFile;
			this.maxNumBits = maxNumBits;
			this.setTimestamp = setTimestamp;
			this.addRandomBits = addRandomBits;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				document.conceal(carrierFile, outFile, maxNumBits, setTimestamp, addRandomBits, key);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	TextDocument	document;
		private	File			carrierFile;
		private	File			outFile;
		private	int				maxNumBits;
		private	boolean			setTimestamp;
		private	boolean			addRandomBits;
		private	KeyList.Key		key;

	}

	//==================================================================


	// RECOVER TEXT TASK CLASS


	public static class RecoverText
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public RecoverText(TextDocument document,
						   File         inFile,
						   KeyList.Key  key)
		{
			this.document = document;
			this.inFile = inFile;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				document.recover(inFile, key);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	TextDocument	document;
		private	File			inFile;
		private	KeyList.Key		key;

	}

	//==================================================================


	// SPLIT FILE TASK CLASS


	public static class SplitFile
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public SplitFile(File        inFile,
						 File        outDirectory,
						 int         filePartLengthLowerBound,
						 int         filePartLengthUpperBound,
						 KeyList.Key key)
		{
			this.inFile = inFile;
			this.outDirectory = outDirectory;
			this.filePartLengthLowerBound = filePartLengthLowerBound;
			this.filePartLengthUpperBound = filePartLengthUpperBound;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				new FileSplitter().split(inFile, outDirectory, filePartLengthLowerBound,
										 filePartLengthUpperBound, key);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	File		inFile;
		private	File		outDirectory;
		private	int			filePartLengthLowerBound;
		private	int			filePartLengthUpperBound;
		private	KeyList.Key	key;

	}

	//==================================================================


	// JOIN FILES TASK CLASS


	public static class JoinFiles
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public JoinFiles(File        inDirectory,
						 File        outFile,
						 KeyList.Key key)
		{
			this.inDirectory = inDirectory;
			this.outFile = outFile;
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				new FileSplitter().join(inDirectory, outFile, key);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	File		inDirectory;
		private	File		outFile;
		private	KeyList.Key	key;

	}

	//==================================================================


	// ERASE FILES TASK CLASS


	public static class EraseFiles
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public EraseFiles(List<String> pathnames)
		{
			this.pathnames = pathnames;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				new FileEraser().erase(pathnames);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	List<String>	pathnames;

	}

	//==================================================================


	// SET FILE ASSOCIATIONS TASK CLASS


	public static class SetFileAssociations
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public SetFileAssociations(FileAssociations                 fileAssociations,
								   String                           javaLauncherPathname,
								   String                           jarPathname,
								   String                           iconPathname,
								   String                           tempDirectoryPrefix,
								   String                           scriptFilename,
								   boolean                          removeEntries,
								   FileAssociations.ScriptLifeCycle scriptLifeCycle)
		{
			this.fileAssociations = fileAssociations;
			this.javaLauncherPathname = javaLauncherPathname;
			this.jarPathname = jarPathname;
			this.iconPathname = iconPathname;
			this.tempDirectoryPrefix = tempDirectoryPrefix;
			this.scriptFilename = scriptFilename;
			this.removeEntries = removeEntries;
			this.scriptLifeCycle = scriptLifeCycle;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				fileAssociations.executeScript(App.SHORT_NAME, javaLauncherPathname, jarPathname,
											   iconPathname, tempDirectoryPrefix, scriptFilename,
											   removeEntries, scriptLifeCycle,
											   ((TextOutputTaskDialog)getProgressView()).getWriter());
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	FileAssociations					fileAssociations;
		private	String								javaLauncherPathname;
		private	String								jarPathname;
		private	String								iconPathname;
		private	String								tempDirectoryPrefix;
		private	String								scriptFilename;
		private	boolean								removeEntries;
		private	FileAssociations.ScriptLifeCycle	scriptLifeCycle;

	}

	//==================================================================


	// WRITE CONFIGURATION TASK CLASS


	public static class WriteConfig
		extends Task
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public WriteConfig(File file)
		{
			this.file = file;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			// Perform task
			try
			{
				AppConfig.INSTANCE.write(file);
			}
			catch (TaskCancelledException e)
			{
				// ignore
			}
			catch (AppException e)
			{
				setException(e, false);
			}

			// Remove thread
			removeThread();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	File	file;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private Task()
	{
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
