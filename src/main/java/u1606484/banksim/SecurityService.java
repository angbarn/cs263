package u1606484.banksim;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class SecurityService {

    /**
     * Lookup table used for converting binary chunks to human-readable
     * characters
     */
    private static final char[] ALPHABET_LOOKUP = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'J', 'K', 'L', 'M', 'P', 'Q',
            'R', 'S', 'T', 'U', 'W', 'X', 'Y', 'Z'
    };

    private static final int CHUNK_BIT_COUNT = 5;
    private static final int OTAC_LENGTH = 8;
    private static final Random RANDOM_GENERATOR = new SecureRandom();
    private static final int SALT_LENGTH_DEFAULT = 20;

    private static byte byteSubstring(byte[] bytes, int startPos) {
        byte newByte = 0;

        for (int i = 0; i < CHUNK_BIT_COUNT; i++) {
            int targetGlobalBit = startPos + i;
            int targetByte = targetGlobalBit / 8;
            int targetByteBit = targetGlobalBit % 8;
            byte singleBit = (byte)
                    ((bytes[targetByte] >> (7 - targetByteBit)) & 1);

            newByte += singleBit << (CHUNK_BIT_COUNT - 1 - i);
        }

        return newByte;
    }

    public static byte[] getSalt(int byteCount) {
        byte[] bytes = new byte[byteCount];
        RANDOM_GENERATOR.nextBytes(bytes);
        return bytes;
    }

    public static byte[] getSalt() {
        return getSalt(SALT_LENGTH_DEFAULT);
    }

    public static byte[] getHash(String message) {
        return getHash(message.getBytes());
    }

    public static byte[] getHash(byte[] message, int repeats) {
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

    public static byte[] getHash(byte[] message) {
        return getHash(message, 1);
    }

    /**
     * Maps a hash to an 8 character code
     */
    private static String hashToCode(byte[] hash) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < OTAC_LENGTH; i++) {
            int lookupIndex = (int) byteSubstring(hash, i * 5);
            out.append(ALPHABET_LOOKUP[lookupIndex]);
        }

        return out.toString();
    }

	/*
    public static String generateOtac(int precision, String content) {
		long timeValue = System.currentTimeMillis() / precision / 1000;
		byte[] totalHash = getHash(timeValue + content);
		return hashToCode(totalHash);
	}
	*/

    public static String generateOtac(int precision, String key) {
        throw new IllegalStateException("Not implemented");
    }

    public static void main(String[] arguments)
            throws InterruptedException {
        // 00000001 10000000
        long lastSecond = 0;
        int precision = 10;
        for (int i = 0; i < 10000; i++) {
            long currentSecond =
                    System.currentTimeMillis() / 1000 % 60 / precision;
            if (currentSecond != lastSecond) {
                lastSecond = currentSecond;
                System.out.println(generateOtac(precision, "bananarama"));
            }
            Thread.sleep(1000);
        }
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