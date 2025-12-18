/*====================================================================*\

FortunaCipher.java

Enumeration: kind of Fortuna cipher.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.crypto;

//----------------------------------------------------------------------


// IMPORTS


import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import uk.blankaspect.common.misc.IStringKeyed;

//----------------------------------------------------------------------


// ENUMERATION: KIND OF FORTUNA CIPHER


/**
 * This is an enumeration of the kinds of cipher that are supported by the {@link Fortuna} cryptographically secure
 * pseudo-random number generator (PRNG).
 * <p>
 * The enum constants have factory methods for creating the appropriate subclasses of {@link Fortuna} and {@link
 * Fortuna.XorCombiner}.
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
		public Fortuna prng(
			byte[]	seed)
		{
			return new FortunaAes256(seed);
		}

		//--------------------------------------------------------------

		@Override
		public Fortuna prng(
			String	seed)
		{
			return new FortunaAes256(seed);
		}

		//--------------------------------------------------------------

		@Override
		public Fortuna.XorCombiner combiner(
			byte[]	seed,
			int		blockSize)
		{
			return FortunaAes256.combiner(seed, blockSize);
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
		public Fortuna prng(
			byte[]	seed)
		{
			return new FortunaSalsa20(seed);
		}

		//--------------------------------------------------------------

		@Override
		public Fortuna prng(
			String	seed)
		{
			return new FortunaSalsa20(seed);
		}

		//--------------------------------------------------------------

		@Override
		public Fortuna.XorCombiner combiner(
			byte[]	seed,
			int		blockSize)
		{
			return FortunaSalsa20.combiner(seed, blockSize);
		}

		//--------------------------------------------------------------
	},

	/**
	 * The ChaCha20 stream cipher with 20 rounds and a 256-bit key.
	 */

	CHA_CHA20
	(
		"chaCha20",
		"ChaCha20",
		ChaCha20.KEY_SIZE
	)
	{
		@Override
		public Fortuna prng(
			byte[]	seed)
		{
			return new FortunaChaCha20(seed);
		}

		//--------------------------------------------------------------

		@Override
		public Fortuna prng(
			String	seed)
		{
			return new FortunaChaCha20(seed);
		}

		//--------------------------------------------------------------

		@Override
		public Fortuna.XorCombiner combiner(
			byte[]	seed,
			int		blockSize)
		{
			return FortunaChaCha20.combiner(seed, blockSize);
		}

		//--------------------------------------------------------------
	};

	//------------------------------------------------------------------

	private static final	int	ID_NUM_BITS	= 4;

	public static final		int	MAX_NUM_IDS	= 1 << ID_NUM_BITS;
	public static final		int	ID_MASK		= MAX_NUM_IDS - 1;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	String	key;
	private	String	text;
	private	int		keySize;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private FortunaCipher(
		String	key,
		String	text,
		int		keySize)
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
	 * Returns the kind of cipher that is associated with the specified key.
	 *
	 * @param  key
	 *           the key whose associated kind of cipher is sought.
	 * @return the kind of cipher that is associated with {@code key}, or {@code null} if there is such kind of cipher.
	 */

	public static FortunaCipher forKey(
		String	key)
	{
		return Arrays.stream(values()).filter(value -> value.key.equals(key)).findFirst().orElse(null);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the kind of cipher that is associated with the specified identifier.
	 *
	 * @param  id
	 *           the identifier whose associated kind of cipher is sought.
	 * @return the kind of cipher that is associated with {@code id}, or {@code null} if there is such kind of cipher.
	 */

	public static FortunaCipher forId(
		int	id)
	{
		return Arrays.stream(values()).filter(value -> value.id() == id).findFirst().orElse(null);
	}

	//------------------------------------------------------------------

	/**
	 * Converts a set of {@link FortunaCipher} enumeration constants to a bit mask and returns it.
	 *
	 * @param  ciphers
	 *           the set of {@link FortunaCipher} enumeration constants to be converted.
	 * @return the bit mask that corresponds to {@code ciphers}.
	 */

	public static int setToBitMask(
		Set<FortunaCipher>	ciphers)
	{
		int mask = 0;
		for (FortunaCipher cipher : ciphers)
			mask |= 1 << cipher.ordinal();
		return mask;
	}

	//------------------------------------------------------------------

	/**
	 * Converts a bit mask of ciphers to a set of {@link FortunaCipher} enumeration constants and returns it.
	 * <p>
	 * A bit of {@code mask} that does not correspond to a {@link FortunaCipher} enumeration constant is ignored.
	 * </p>
	 *
	 * @param  mask
	 *           the bit mask to be converted.
	 * @return a set of {@link FortunaCipher} enumeration constants that corresponds to {@code mask}.
	 */

	public static Set<FortunaCipher> bitMaskToSet(
		int	mask)
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
	 * Creates and returns a new instance of a pseudo-random number generator that is initialised with the specified
	 * seed.
	 * <p>
	 * The PRNG will not be able to generate random data until sufficient entropy has accumulated for the generator to
	 * be reseeded.  The ability to reseed can be tested with {@link Fortuna#canReseed()}.
	 * </p>
	 *
	 * @param  seed
	 *           a sequence of bytes that will be used to seed the pseudo-random number generator.  If {@code seed} is
	 *           {@code null}, a random seed derived from the sources of entropy will be used.
	 * @return the pseudo-random number generator that was created.
	 */

	public abstract Fortuna prng(
		byte[]	seed);

	//------------------------------------------------------------------

	/**
	 * Creates and returns a new instance of a pseudo-random number generator that is initialised with the specified
	 * seed in the form of a string.
	 * <p>
	 * If the seed is {@code null}, a random seed derived from the sources of entropy will be used.  In this case, the
	 * PRNG will not be able to generate random data until sufficient entropy has accumulated for the generator to be
	 * reseeded.  The ability to reseed can be tested with {@link Fortuna#canReseed()}.
	 * </p>
	 *
	 * @param  seed
	 *           a string whose UTF-8 encoding will be used to seed the pseudo-random number generator.  If {@code seed}
	 *           is {@code null}, a random seed derived from the sources of entropy will be used.
	 * @return the pseudo-random number generator that was created.
	 */

	public abstract Fortuna prng(
		String	seed);

	//------------------------------------------------------------------

	/**
	 * Creates and returns a new instance of an object that can be used to combine data and random data generated by a
	 * PRNG with an exclusive-OR operation.
	 * <p>
	 * The PRNG is created by this method and initialised with the specified seed.  If the seed is {@code null}, a
	 * random seed derived from the sources of entropy will be used.  In this case, the PRNG will not be able to
	 * generate random data until sufficient entropy has accumulated for the generator to be reseeded.  The ability to
	 * reseed can be tested with the {@link Fortuna#canReseed() canReseed()} method of the PRNG that is returned by
	 * {@link Fortuna.XorCombiner#getPrng()}.
	 * </p>
	 *
	 * @param  seed
	 *           the seed that will be used to initialise the PRNG that will generate the random data for the
	 *           exclusive-OR operation.  If {@code seed} is {@code null}, a random seed derived from the sources of
	 *           entropy will be used.
	 * @param  blockSize
	 *           the number of bytes of random data that will be extracted from this object's PRNG with each request.
	 * @return an exclusive-OR combiner object.
	 * @throws IllegalArgumentException
	 *           if {@code blockSize} is out of bounds for the particular cipher.
	 */

	public abstract Fortuna.XorCombiner combiner(
		byte[]	seed,
		int		blockSize);

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IStringKeyed interface
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the key of this enum constant.
	 *
	 * @return the key of this enum constant.
	 */

	@Override
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
	 * Returns the identifier of the cipher that is associated with this enum constant.
	 *
	 * @return the identifier of the cipher that is associated with this enum constant.
	 */

	public int id()
	{
		return ordinal();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the size of the key of the cipher that is associated with this enum constant.
	 *
	 * @return the size of the key of the cipher that is associated with this enum constant.
	 */

	public int keySize()
	{
		return keySize;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
