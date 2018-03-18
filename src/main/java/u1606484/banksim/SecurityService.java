package u1606484.banksim;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Handles password cryptography, as well as containing a number of important,
 * security-related program constants.
 *
 * The {@code generateSessionKey} method will utilise {@link SecureRandom} to
 * generate a string of lowercase characters. The length of a session key
 * depends upon {@value SESSION_KEY_LENGTH}.
 *
 * <p>The {@code getPasswordHash} method provides the resulting hash of a
 * password after combining plaintext with a salt and iterating over a number of
 * passes. Similarly, {@code verifyPassword} will combine with a salt to test a
 * plaintext password against a stored series of bytes representing a user's
 * saved, hashed password.
 */
public class SecurityService {

    /**
     * The length of a session key in characeters
     */
    public static final int SESSION_KEY_LENGTH = 15;
    /**
     * The time before a session expires and a user has to log in again in
     * milliseconds
     */
    public static final int SESSION_EXPIRY_LENGTH = 30 * 60 * 1000;
    /**
     * The number of hashing passes made on a password by default. If this
     * constant is updated, passwords should be re-hashed upon next login.
     */
    public static final int PASSWORD_HASH_PASSES = 1;
    /**
     * The minimum acceptable length of a password, in characters
     */
    public static final int MINIMUM_PASSWORD_LENGTH = 16;
    /**
     * A random generator used throughout the class to provide cryptographically
     * secure random numbers
     */
    private static final Random RANDOM_GENERATOR = new SecureRandom();
    /**
     * The length of a salt, in bytes
     */
    private static final int SALT_LENGTH_DEFAULT = 20;

    /**
     * Gets a salt a certain number of bytes long
     *
     * @param byteCount The number of bytes long to make the salt
     * @return A salt a certain number of bytes long
     */
    static byte[] getSalt(int byteCount) {
        byte[] bytes = new byte[byteCount];
        RANDOM_GENERATOR.nextBytes(bytes);
        return bytes;
    }

    /**
     * Gets a salt the default number of bytes long
     *
     * @return A salt, with length set by SALT_LENGTH_DEFAULT
     * @see SecurityService#SALT_LENGTH_DEFAULT
     */
    public static byte[] getSalt() {
        return getSalt(SALT_LENGTH_DEFAULT);
    }

    /**
     * Generates a random session key of specified length. Session keys are
     * lowercase alphabetical characters.
     *
     * @param length The length of the provided session key in characters
     * @return A randomly generated alphabetical session key
     */
    public static String generateSessionKey(int length) {
        Supplier<Character> randomHex =
                () -> (char) (RANDOM_GENERATOR.nextInt('z' - 'a') + 'a');

        StringBuilder key = new StringBuilder();
        for (int i = 0; i < length; i++) {
            key.append(randomHex.get());
        }

        return key.toString();
    }

    /**
     * Hashes an array of bytes, iterating the hashing function for multiple
     * passes over the bytes.
     *
     * <p>The hashing algorithm used is SHA-256.
     *
     * @param message The array of bytes to hash
     * @param repeats The number of passes to make over the bytes when hashing
     * @return The hashed bytes
     */
    private static byte[] getHash(byte[] message, int repeats) {
        if (repeats <= 0) {
            throw new IllegalStateException("Repeats must be greater than 0");
        }

        MessageDigest d;
        try {
            d = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalStateException("No such hash algorithm");
        }

        for (int i = 0; i < repeats; i++) {
            message = d.digest(message);
        }

        return message;
    }

    /**
     * Hashes an array of bytes, but iterating only once
     *
     * @param message The array of bytes to hash
     * @return The hashed bytes
     * @see SecurityService#getHash(byte[], int)
     */
    static byte[] getHash(byte[] message) {
        return getHash(message, 1);
    }

    /**
     * Converts plaintext to bytes, combines it with a salt, and hashes the
     * result {@code iterations} number of times.
     *
     * @param plaintext The plaintext password to hash
     * @param salt The salt to add to the password
     * @param iterations The number of iterations to make over the
     * plaintext-salt combination
     * @return The resulting password hash
     */
    public static byte[] getPasswordHash(String plaintext, byte[] salt,
            int iterations) {
        // Concatenate plaintext and password salt
        byte[] plaintextBytes = plaintext.getBytes();
        byte[] attemptBytes = new byte[plaintextBytes.length + salt.length];
        System.arraycopy(plaintextBytes, 0, attemptBytes, 0,
                plaintextBytes.length);
        System.arraycopy(salt, 0, attemptBytes, plaintextBytes.length,
                salt.length);

        return getHash(attemptBytes, iterations);
    }

    /**
     * Combines plaintext with a salt and compares against a known hash to
     * check for equivalence.
     * @param attempt The plaintext password attempt
     * @param salt The salt to combine with the attempt
     * @param hash The known password hash to compare to
     * @param iterations The number of passes to apply to the plaintext
     * before the known hash is expected
     * @return If the passwords match, true. Otherwise false.
     */
    public static boolean verifyPassword(String attempt, byte[] salt,
            byte[] hash, int iterations) {
        byte[] attemptHash = getPasswordHash(attempt, salt, iterations);

        // While this means they're not equal, we should only ever be
        // comparing hashes of the same length, or something has gone wrong
        // elsewhere in the program
        if (attemptHash.length != hash.length) {
            throw new IllegalArgumentException("Hashes of differing length");
        }

        // Test each byte for equivalence
        for (int i = 0; i < attemptHash.length; i++) {
            if (attemptHash[i] != hash[i]) {
                return false;
            }
        }
        return true;
    }
}