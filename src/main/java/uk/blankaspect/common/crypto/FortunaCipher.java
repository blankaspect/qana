/*====================================================================*\

FortunaCipher.java

Fortuna cipher enumeration.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.util.EnumSet;
import java.util.Set;

import uk.blankaspect.common.misc.IStringKeyed;

//----------------------------------------------------------------------


// FORTUNA CIPHER ENUMERATION


/**
 * This is an enumeration of the kinds of cipher that are supported by the {@link Fortuna} cryptographically
 * secure pseudo-random number generator (PRNG).
 * <p>
 * The enum constants have factory methods for creating the appropriate subclasses of {@link Fortuna} and
 * {@link Fortuna.XorCombiner}.
 * </p>
 */

public enum FortunaCipher
	implements IStringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/**
	 * The Advanced Encryption Standard (AES) block cipher with a 256-bit key.
	 */

	AES256
	(
		"aes256",
		"AES-256",
		Aes256.KEY_SIZE
	)
	{
		@Override
		public Fortuna createPrng(byte[] seed)
		{
			return new FortunaAes256(seed);
		}

		//--------------------------------------------------------------

		@Override
		public Fortuna createPrng(String seed)
		{
			return new FortunaAes256(seed);
		}

		//--------------------------------------------------------------

		@Override
		public Fortuna.XorCombiner createCombiner(byte[] seed,
												  int    blockSize)
		{
			return FortunaAes256.createCombiner(seed, blockSize);
		}

		//--------------------------------------------------------------
	},

	/**
	 * The Salsa20 stream cipher with 20 rounds and a 256-bit key.
	 */

	SALSA20
	(
		"salsa20",
		"Salsa20",
		Salsa20.KEY_SIZE
	)
	{
		@Override
		public Fortuna createPrng(byte[] seed)
		{
			return new FortunaSalsa20(seed);
		}

		//--------------------------------------------------------------

		@Override
		public Fortuna createPrng(String seed)
		{
			return new FortunaSalsa20(seed);
		}

		//--------------------------------------------------------------

		@Override
		public Fortuna.XorCombiner createCombiner(byte[] seed,
												  int    blockSize)
		{
			return FortunaSalsa20.createCombiner(seed, blockSize);
		}

		//--------------------------------------------------------------
	};

	//------------------------------------------------------------------

	private static final	int	ID_NUM_BITS	= 4;

	public static final		int	MAX_NUM_IDS	= 1 << ID_NUM_BITS;
	public static final		int	ID_MASK		= MAX_NUM_IDS - 1;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private FortunaCipher(String key,
						  String text,
						  int    keySize)
	{
		this.key = key;
		this.text = text;
		this.keySize = keySize;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the enum constant corresponding to the specified key.
	 *
	 * @param  key  the key for which the enum constant is sought.
	 * @return the enum constant corresponding to the specified key, or {@code null} if there is no enum
	 *         constant corresponding to {@code key}.
	 */

	public static FortunaCipher forKey(String key)
	{
		for (FortunaCipher value : values())
		{
			if (value.key.equals(key))
				return value;
		}
		return null;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the enum constant corresponding to the specified identifier.
	 *
	 * @param  id  the identifier for which the enum constant is sought.
	 * @return the enum constant corresponding to the specified identifier, or {@code null} if there is no
	 *         enum constant corresponding to {@code id}.
	 */

	public static FortunaCipher forId(int id)
	{
		for (FortunaCipher value : values())
		{
			if (value.getId() == id)
				return value;
		}
		return null;
	}

	//------------------------------------------------------------------

	/**
	 * Converts a set of {@link FortunaCipher} enum constants to a bit mask and returns it.
	 *
	 * @param  ciphers  the set of {@link FortunaCipher} enum constants that will be converted.
	 * @return a bit mask that corresponds to {@code ciphers}.
	 */

	public static int setToBitMask(Set<FortunaCipher> ciphers)
	{
		int mask = 0;
		for (FortunaCipher cipher : ciphers)
			mask |= 1 << cipher.ordinal();
		return mask;
	}

	//------------------------------------------------------------------

	/**
	 * Converts a bit mask of ciphers to a set of {@link FortunaCipher} enum constants and returns it.
	 * <p>
	 * Bits of {@code mask} that do not correspond to a {@link FortunaCipher} enum constant are ignored.
	 * </p>
	 *
	 * @param  mask  the bit mask that will be converted.
	 * @return a set of {@link FortunaCipher} enum constants that corresponds to {@code mask}.
	 */

	public static Set<FortunaCipher> bitMaskToSet(int mask)
	{
		Set<FortunaCipher> ciphers = EnumSet.noneOf(FortunaCipher.class);
		for (FortunaCipher cipher : FortunaCipher.values())
		{
			if ((mask & 1 << cipher.ordinal()) != 0)
				ciphers.add(cipher);
		}
		return ciphers;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Abstract methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a pseudo-random number generator that is initialised with the specified seed.
	 * <p>
	 * The PRNG will not be able to generate random data until sufficient entropy has accumulated for the
	 * generator to be reseeded.  The ability to reseed can be tested with {@link Fortuna#canReseed()}.
	 * </p>
	 *
	 * @param  seed  a sequence of bytes that will be used to seed the pseudo-random number generator.  If
	 *               {@code seed} is {@code null}, a random seed derived from the sources of entropy will be
	 *               used.
	 * @return the pseudo-random number generator that was created.
	 */

	public abstract Fortuna createPrng(byte[] seed);

	//------------------------------------------------------------------

	/**
	 * Creates a pseudo-random number generator that is initialised with the specified seed in the form of a
	 * string.
	 * <p>
	 * If the seed is {@code null}, a random seed derived from the sources of entropy will be used.  In this
	 * case, the PRNG will not be able to generate random data until sufficient entropy has accumulated for
	 * the generator to be reseeded.  The ability to reseed can be tested with {@link Fortuna#canReseed()}.
	 * </p>
	 *
	 * @param  seed  a string whose UTF-8 encoding will be used to seed the pseudo-random number generator.
	 *               If {@code seed} is {@code null}, a random seed derived from the sources of entropy will
	 *               be used.
	 * @return the pseudo-random number generator that was created.
	 */

	public abstract Fortuna createPrng(String seed);

	//------------------------------------------------------------------

	/**
	 * Creates an object that will combine data and random data generated by a PRNG with an exclusive-OR
	 * operation.
	 * <p>
	 * The PRNG is created by this method and initialised with the specified seed.  If the seed is {@code
	 * null}, a random seed derived from the sources of entropy will be used.  In this case, the PRNG will
	 * not be able to generate random data until sufficient entropy has accumulated for the generator to be
	 * reseeded.  The ability to reseed can be tested with the {@link Fortuna#canReseed() canReseed()}
	 * method of the PRNG that is returned by {@link Fortuna.XorCombiner#getPrng()}.
	 * </p>
	 *
	 * @param  seed       the seed that will be used to initialise the PRNG that will generate the random
	 *                    data for the exclusive-OR operation.  If {@code seed} is {@code null}, a random
	 *                    seed derived from the sources of entropy will be used.
	 * @param  blockSize  the number of bytes of random data that will be extracted from this object's
	 *                    PRNG with each request.
	 * @return an exclusive-OR combiner object.
	 * @throws IllegalArgumentException
	 *           if {@code blockSize} is out of bounds for the particular cipher.
	 */

	public abstract Fortuna.XorCombiner createCombiner(byte[] seed,
													   int    blockSize);

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IStringKeyed interface
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the key of this enum constant.
	 *
	 * @return the key of this enum constant.
	 */

	public String getKey()
	{
		return key;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns a string representation of this enum constant.
	 *
	 * @return a string representation of this enum constant.
	 */

	@Override
	public String toString()
	{
		return text;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the identifier of the cipher associated with this enum constant.
	 *
	 * @return the identifier of the cipher associated with this enum constant.
	 */

	public int getId()
	{
		return ordinal();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the key size of the cipher associated with this enum constant.
	 *
	 * @return the key size of the cipher associated with this enum constant.
	 */

	public int getKeySize()
	{
		return keySize;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	String	key;
	private	String	text;
	private	int		keySize;

}

//----------------------------------------------------------------------
