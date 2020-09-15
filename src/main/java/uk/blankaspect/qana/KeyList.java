/*====================================================================*\

KeyList.java

Key list class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.qana;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import uk.blankaspect.common.crypto.FileEncrypter;
import uk.blankaspect.common.crypto.Fortuna;
import uk.blankaspect.common.crypto.FortunaCipher;
import uk.blankaspect.common.crypto.Scrypt;
import uk.blankaspect.common.crypto.ScryptSalsa20;
import uk.blankaspect.common.crypto.StreamEncrypter;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.UnexpectedRuntimeException;

import uk.blankaspect.common.misc.BinaryFile;
import uk.blankaspect.common.misc.ByteDataList;
import uk.blankaspect.common.misc.FileWritingMode;

import uk.blankaspect.common.number.NumberUtils;

//----------------------------------------------------------------------


// KEY LIST CLASS


class KeyList
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		int		SALT_SIZE			= StreamEncrypter.SALT_SIZE;
	public static final		int		DERIVED_KEY_SIZE	= StreamEncrypter.DERIVED_KEY_SIZE;

	public static final		StreamEncrypter.KdfParams	DEFAULT_KDF_PARAMS	=
												new StreamEncrypter.KdfParams(Scrypt.CoreHashNumRounds._8, 16, 8, 1, 0);

	private static final	int		FILE_ID	= 0x41AC38D6;

	private static final	int		VERSION					= 0;
	private static final	int		MIN_SUPPORTED_VERSION	= 0;
	private static final	int		MAX_SUPPORTED_VERSION	= 0;

	private static final	int		ID_FIELD_SIZE					= 4;
	private static final	int		VERSION_FIELD_SIZE				= 2;
	private static final	int		STRING_TABLE_OFFSET_FIELD_SIZE	= 4;
	private static final	int		NUM_KEYS_FIELD_SIZE				= 2;

	private static final	int		HEADER_SIZE	= ID_FIELD_SIZE + VERSION_FIELD_SIZE + STRING_TABLE_OFFSET_FIELD_SIZE
													+ NUM_KEYS_FIELD_SIZE;

	private static final	String	READING_STR	= "Reading";
	private static final	String	WRITING_STR	= "Writing";

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

		FILE_DOES_NOT_EXIST
		("The key file does not exist."),

		NOT_A_KEY_FILE
		("The file is not a key file."),

		UNSUPPORTED_FILE_VERSION
		("The version of the key file (%1) is not supported by this version of the program."),

		MALFORMED_KEY_FILE
		("The key file is malformed."),

		DUPLICATE_KEY_NAME
		("The key file contains more than one key with the name '%1'."),

		CIPHER_NOT_ALLOWED
		("Key: %1\nThe key does not allow the %2 cipher to be used for encryption.");

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


	// KEY CLASS


	public static class Key
		implements Comparable<Key>
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		public static final		char	TEMPORARY_PREFIX_CHAR	= '$';
		public static final		String	TEMPORARY_PREFIX		= Character.toString(TEMPORARY_PREFIX_CHAR);

		private static final	int		NAME_OFFSET_FIELD_SIZE		= 4;
		private static final	int		KDF_PARAMS_VER_FIELD_SIZE	= 4;
		private static final	int		KDF_PARAMS_GEN_FIELD_SIZE	= 4;
		private static final	int		ALLOWED_CIPHERS_FIELD_SIZE	= 2;
		private static final	int		PREFERRED_CIPHER_FIELD_SIZE	= 2;
		private static final	int		RESERVED_FIELD_SIZE			= 16;
		private static final	int		SALT_FIELD_SIZE				= SALT_SIZE;
		private static final	int		HASH_VALUE_FIELD_SIZE		= DERIVED_KEY_SIZE;

		private static final	int		SIZE	= NAME_OFFSET_FIELD_SIZE + KDF_PARAMS_VER_FIELD_SIZE
													+ KDF_PARAMS_GEN_FIELD_SIZE + ALLOWED_CIPHERS_FIELD_SIZE
													+ PREFERRED_CIPHER_FIELD_SIZE + RESERVED_FIELD_SIZE
													+ SALT_FIELD_SIZE + HASH_VALUE_FIELD_SIZE;

	////////////////////////////////////////////////////////////////////
	//  Class variables
	////////////////////////////////////////////////////////////////////

		private static	int	temporaryIndex;

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	name;
		private	int		index;
		private	int		kdfParamsVer;
		private	int		kdfParamsGen;
		private	int		allowedCiphers;
		private	int		preferredCipher;
		private	byte[]	key;
		private	byte[]	salt;
		private	byte[]	hashValue;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Key()
		{
		}

		//--------------------------------------------------------------

		private Key(String             name,
					int                kdfParamsVer,
					int                kdfParamsGen,
					Set<FortunaCipher> allowedCiphers,
					FortunaCipher      preferredCipher,
					byte[]             key,
					byte[]             salt,
					byte[]             hashValue)
		{
			this.name = name;
			if (name == null)
			{
				index = ++temporaryIndex;
				if (index < 0)
					throw new UnexpectedRuntimeException();
			}
			this.kdfParamsVer = kdfParamsVer;
			this.kdfParamsGen = kdfParamsGen;
			this.allowedCiphers = FortunaCipher.setToBitMask(allowedCiphers);
			this.preferredCipher = -1;
			for (FortunaCipher cipher : FortunaCipher.values())
			{
				if (cipher == preferredCipher)
				{
					this.preferredCipher = cipher.ordinal();
					break;
				}
			}
			this.key = key;
			this.salt = salt;
			this.hashValue = hashValue;
		}

		//--------------------------------------------------------------

		private Key(byte[]      data,
					int         offset,
					StringTable stringTable)
			throws AppException
		{
			// Test whether element extends beyond end of data
			if (offset + SIZE > data.length)
				throw new AppException(ErrorId.MALFORMED_KEY_FILE);

			// Parse field: name offset
			int length = NAME_OFFSET_FIELD_SIZE;
			int nameOffset = NumberUtils.bytesToUIntLE(data, offset, length);
			offset += length;

			// Get name from string table
			name = stringTable.get(nameOffset);
			if (name == null)
				throw new AppException(ErrorId.MALFORMED_KEY_FILE);

			// Parse field: KDF parameters, verification
			length = KDF_PARAMS_VER_FIELD_SIZE;
			kdfParamsVer = NumberUtils.bytesToUIntLE(data, offset, length);
			offset += length;

			// Parse field: KDF parameters, CEK generation
			length = KDF_PARAMS_GEN_FIELD_SIZE;
			kdfParamsGen = NumberUtils.bytesToUIntLE(data, offset, length);
			offset += length;

			// Parse field: allowed ciphers
			length = ALLOWED_CIPHERS_FIELD_SIZE;
			allowedCiphers = NumberUtils.bytesToUIntLE(data, offset, length);
			offset += length;

			// Parse field: preferred cipher
			length = PREFERRED_CIPHER_FIELD_SIZE;
			preferredCipher = NumberUtils.bytesToIntLE(data, offset, length);
			offset += length;

			// Skip field: reserved
			length = RESERVED_FIELD_SIZE;
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
		public int compareTo(Key other)
		{
			return (name == null)
						? (other.name == null)
								? Integer.compare(index, other.index)
								: -1
						: (other.name == null)
								? 1
								: name.compareTo(other.name);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public boolean equals(Object obj)
		{
			return (obj instanceof Key) && (compareTo((Key)obj) == 0);
		}

		//--------------------------------------------------------------

		@Override
		public int hashCode()
		{
			return (name == null) ? -index : name.hashCode();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public KeyKind getKind()
		{
			return (name == null)
						? (index == 0)
								? KeyKind.NEW
								: KeyKind.TEMPORARY
						: KeyKind.PERSISTENT;
		}

		//--------------------------------------------------------------

		public String getName()
		{
			return (name == null) ? TEMPORARY_PREFIX + index : name;
		}

		//--------------------------------------------------------------

		public String getQuotedName()
		{
			return (name == null) ? TEMPORARY_PREFIX + index : "'" + name + "'";
		}

		//--------------------------------------------------------------

		public byte[] getKey()
		{
			return key;
		}

		//--------------------------------------------------------------

		public StreamEncrypter.KdfParams getKdfParamsVer()
		{
			return new StreamEncrypter.KdfParams(kdfParamsVer);
		}

		//--------------------------------------------------------------

		public StreamEncrypter.KdfParams getKdfParamsGen()
		{
			return new StreamEncrypter.KdfParams(kdfParamsGen);
		}

		//--------------------------------------------------------------

		public Set<FortunaCipher> getAllowedCiphers()
		{
			return FortunaCipher.bitMaskToSet(allowedCiphers);
		}

		//--------------------------------------------------------------

		public FortunaCipher getPreferredCipher()
		{
			return ((preferredCipher >= 0)
					&& (preferredCipher < FortunaCipher.values().length)
					&& ((allowedCiphers & 1 << preferredCipher) != 0))
															? FortunaCipher.values()[preferredCipher]
															: null;
		}

		//--------------------------------------------------------------

		public void checkAllowedCipher(FortunaCipher cipher)
			throws AppException
		{
			if (!getAllowedCiphers().contains(cipher))
				throw new AppException(ErrorId.CIPHER_NOT_ALLOWED, getQuotedName(), cipher.toString());
		}

		//--------------------------------------------------------------

		public boolean verify(String passphrase)
		{
			byte[] key = Fortuna.keyStringToBytes(passphrase);
			boolean verified = Arrays.equals(hashValue, deriveKey(key, salt, getKdfParamsVer()));
			if (verified)
				this.key = key;
			return verified;
		}

		//--------------------------------------------------------------

		/**
		 * @throws IllegalStateException
		 */

		public void setKdfParamsVer(StreamEncrypter.KdfParams kdfParams)
		{
			// Set KDF parameters, verification
			kdfParamsVer = kdfParams.getEncodedValue();

			// Update hash value
			if (key == null)
				throw new IllegalStateException("No key");
			hashValue = deriveKey(key, salt, getKdfParamsVer());
		}

		//--------------------------------------------------------------

		public void setKdfParamsGen(StreamEncrypter.KdfParams kdfParams)
		{
			kdfParamsGen = kdfParams.getEncodedValue();
		}

		//--------------------------------------------------------------

		public void setAllowedCiphers(Set<FortunaCipher> ciphers)
		{
			allowedCiphers = FortunaCipher.setToBitMask(ciphers);
		}

		//--------------------------------------------------------------

		public void setPreferredCipher(FortunaCipher cipher)
		{
			preferredCipher = (cipher == null) ? -1 : cipher.ordinal();
		}

		//--------------------------------------------------------------

		public Key createCopy(String name)
		{
			Key copy = new Key();
			copy.name = name;
			copy.kdfParamsVer = kdfParamsVer;
			copy.kdfParamsGen = kdfParamsGen;
			copy.allowedCiphers = allowedCiphers;
			copy.preferredCipher = preferredCipher;
			copy.key = key.clone();
			copy.salt = salt.clone();
			copy.hashValue = hashValue.clone();
			return copy;
		}

		//--------------------------------------------------------------

		public StreamEncrypter getStreamEncrypter(FortunaCipher cipher)
		{
			return getStreamEncrypter(cipher, null);
		}

		//--------------------------------------------------------------

		public StreamEncrypter getStreamEncrypter(FortunaCipher          cipher,
												  StreamEncrypter.Header header)
		{
			return new StreamEncrypter(cipher, getKdfParamsGen(), header);
		}

		//--------------------------------------------------------------

		public FileEncrypter getFileEncrypter(FortunaCipher cipher)
		{
			return getFileEncrypter(cipher, null);
		}

		//--------------------------------------------------------------

		public FileEncrypter getFileEncrypter(FortunaCipher          cipher,
											  StreamEncrypter.Header header)
		{
			return new FileEncrypter(cipher, getKdfParamsGen(), header);
		}

		//--------------------------------------------------------------

		private byte[] toByteArray(StringTable stringTable)
		{
			byte[] buffer = new byte[SIZE];
			int offset = 0;

			// Set field: name offset
			int length = NAME_OFFSET_FIELD_SIZE;
			NumberUtils.intToBytesLE(stringTable.add(name), buffer, offset, length);
			offset += length;

			// Set field: KDF parameters, verification
			length = KDF_PARAMS_VER_FIELD_SIZE;
			NumberUtils.intToBytesLE(kdfParamsVer, buffer, offset, length);
			offset += length;

			// Set field: KDF parameters, CEK generation
			length = KDF_PARAMS_GEN_FIELD_SIZE;
			NumberUtils.intToBytesLE(kdfParamsGen, buffer, offset, length);
			offset += length;

			// Set field: allowed ciphers
			length = ALLOWED_CIPHERS_FIELD_SIZE;
			NumberUtils.intToBytesLE(allowedCiphers, buffer, offset, length);
			offset += length;

			// Set field: preferred cipher
			length = PREFERRED_CIPHER_FIELD_SIZE;
			NumberUtils.intToBytesLE(preferredCipher, buffer, offset, length);
			offset += length;

			// Skip field: reserved
			length = RESERVED_FIELD_SIZE;
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

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	List<Key>	keys;
	private	boolean		changed;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public KeyList()
	{
		keys = new ArrayList<>();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static Key createKey()
	{
		return new Key();
	}

	//------------------------------------------------------------------

	public static Key createKey(String                    name,
								String                    passphrase,
								StreamEncrypter.KdfParams kdfParamsVer,
								StreamEncrypter.KdfParams kdfParamsGen,
								Set<FortunaCipher>        allowedCiphers,
								FortunaCipher             preferredCipher)
	{
		byte[] key = Fortuna.keyStringToBytes(passphrase);
		byte[] salt = App.INSTANCE.getRandomBytes(SALT_SIZE);
		return new Key(name, kdfParamsVer.getEncodedValue(), kdfParamsGen.getEncodedValue(),
					   allowedCiphers, preferredCipher, key, salt, deriveKey(key, salt, kdfParamsVer));
	}

	//------------------------------------------------------------------

	private static byte[] deriveKey(byte[]                    key,
									byte[]                    salt,
									StreamEncrypter.KdfParams kdfParams)
	{
		return new ScryptSalsa20(kdfParams.numRounds)
										.deriveKey(key, salt, kdfParams, kdfParams.getNumThreads(), DERIVED_KEY_SIZE);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public List<Key> getKeys()
	{
		return Collections.unmodifiableList(keys);
	}

	//------------------------------------------------------------------

	public void setKeys(List<Key> keys)
	{
		this.keys.clear();
		this.keys.addAll(keys);
		changed = true;
	}

	//------------------------------------------------------------------

	public void addKey(Key key)
	{
		keys.add(key);
		changed = true;
	}

	//------------------------------------------------------------------

	public boolean isChanged()
	{
		return changed;
	}

	//------------------------------------------------------------------

	public void read(File file)
		throws AppException
	{
		// Initialise progress view
		TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
		progressView.setInfo(READING_STR, file);
		progressView.setProgress(0, 0.0);

		// Test for file
		if (!file.isFile())
			throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, file);

		// Test file length
		if (file.length() < HEADER_SIZE)
			throw new FileException(ErrorId.NOT_A_KEY_FILE, file);

		// Read and parse file
		try
		{
			BinaryFile binaryFile = new BinaryFile(file);
			binaryFile.addProgressListener(progressView);
			parse(binaryFile.read());
		}
		catch (AppException e)
		{
			throw new FileException(e, file);
		}
	}

	//------------------------------------------------------------------

	public void write(File file)
		throws AppException
	{
		// Initialise list of data blocks
		ByteDataList dataBlocks = new ByteDataList();

		// Create file header
		byte[] header = createHeader();
		dataBlocks.add(header);

		// Add elements
		StringTable stringTable = new StringTable();
		for (Key key : keys)
			dataBlocks.add(key.toByteArray(stringTable));

		// Add string table
		dataBlocks.add(stringTable.toByteArray());

		// Initialise progress view
		TaskProgressDialog progressView = (TaskProgressDialog)Task.getProgressView();
		progressView.setInfo(WRITING_STR, file);
		progressView.setProgress(0, 0.0);

		// Write file
		BinaryFile binaryFile = new BinaryFile(file, dataBlocks);
		binaryFile.addProgressListener(progressView);
		binaryFile.write(FileWritingMode.USE_TEMP_FILE);
	}

	//------------------------------------------------------------------

	private void parse(byte[] data)
		throws AppException
	{
		int offset = 0;

		// Parse field: file identifier
		int length = ID_FIELD_SIZE;
		if (NumberUtils.bytesToUIntLE(data, offset, length) != FILE_ID)
			throw new AppException(ErrorId.NOT_A_KEY_FILE);
		offset += length;

		// Parse field: version number
		length = VERSION_FIELD_SIZE;
		int version = NumberUtils.bytesToUIntLE(data, offset, length);
		offset += length;
		if ((version < MIN_SUPPORTED_VERSION) || (version > MAX_SUPPORTED_VERSION))
			throw new AppException(ErrorId.UNSUPPORTED_FILE_VERSION, Integer.toString(version));

		// Parse field: string table offset
		length = STRING_TABLE_OFFSET_FIELD_SIZE;
		int stringTableOffset = NumberUtils.bytesToUIntLE(data, offset, length);
		offset += length;
		if (stringTableOffset >= data.length)
			throw new AppException(ErrorId.MALFORMED_KEY_FILE);

		// Parse field: number of keys
		length = NUM_KEYS_FIELD_SIZE;
		int numKeys = NumberUtils.bytesToUIntLE(data, offset, length);
		offset += length;

		// Parse string table
		StringTable stringTable = new StringTable(data, stringTableOffset,
												  data.length - stringTableOffset);

		// Parse keys
		for (int i = 0; i < numKeys; i++)
		{
			keys.add(new Key(data, offset, stringTable));
			offset += Key.SIZE;
		}

		// Test for unique key names
		for (int i = 0; i < numKeys - 1; i++)
		{
			String name = keys.get(i).name;
			for (int j = i + 1; j < numKeys; j++)
			{
				if (keys.get(j).name.equals(name))
					throw new AppException(ErrorId.DUPLICATE_KEY_NAME, name);
			}
		}

		// Sort keys
		keys.sort(null);
	}

	//------------------------------------------------------------------

	private byte[] createHeader()
	{
		byte[] buffer = new byte[HEADER_SIZE];
		int offset = 0;

		// Set field: file identifier
		int length = ID_FIELD_SIZE;
		NumberUtils.intToBytesLE(FILE_ID, buffer, offset, length);
		offset += length;

		// Set field: version number
		length = VERSION_FIELD_SIZE;
		NumberUtils.intToBytesLE(VERSION, buffer, offset, length);
		offset += length;

		// Set field: string table offset
		length = STRING_TABLE_OFFSET_FIELD_SIZE;
		NumberUtils.intToBytesLE(HEADER_SIZE + keys.size() * Key.SIZE, buffer, offset, length);
		offset += length;

		// Set field: number of keys
		length = NUM_KEYS_FIELD_SIZE;
		NumberUtils.intToBytesLE(keys.size(), buffer, offset, length);
		offset += length;

		return buffer;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
