package u1606484.banksim;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Supplier;

public class SecurityService {

    private static final Random RANDOM_GENERATOR = new SecureRandom();
    private static final int SALT_LENGTH_DEFAULT = 20;
    public static final int SESSION_KEY_LENGTH = 15;
    public static final int SESSION_EXPIRY_LENGTH = 30 * 60 * 1000;

    static byte[] getSalt(int byteCount) {
        byte[] bytes = new byte[byteCount];
        RANDOM_GENERATOR.nextBytes(bytes);
        return bytes;
    }

    public static byte[] getSalt() {
        return getSalt(SALT_LENGTH_DEFAULT);
    }

    public static String generateSessionKey(int length) {
        Supplier<Character> randomHex =
                () -> (char) (RANDOM_GENERATOR.nextInt('z' - 'a') + 'a');

        StringBuilder key = new StringBuilder();
        for (int i = 0; i < length; i++) {
            key.append(randomHex.get());
        }

        return key.toString();
    }

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

    static byte[] getHash(byte[] message) {
        return getHash(message, 1);
    }

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