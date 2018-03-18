package u1606484.banksim;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class provides the functionality to encrypt or decrypt files.
 *
 * <p>In order to encrypt a file, the {@link AesEncryption#swap} method is
 * called with a parameter of {@code Cipher.ENCRYPT_MODE}. Similarly, to decrypt
 * a file, the same method is called, but with a parameter of {@code
 * Cipher.DECRYPT_MODE}.
 *
 * <p>No other modes of operation are available.
 *
 * <p>Encryption is 128-bit, since 256 and 196 bit AES encryption require
 * additional dependencies. Keys are automatically sized to 16 bytes by hashing,
 * and truncating to just the first half.
 */
public class AesEncryption {

    /**
     * Algorithm to use for encryption/decryption
     */
    private static final String ALGORITHM = "AES";

    /**
     * Length of key in bytes
     */
    private static final int KEY_LENGTH = 16; // 128 bits

    /**
     * Encrypts or decrypts an array of bytes using a specified password
     *
     * <p>A vast majority of the work encrypting or decrypting data is the same,
     * so this method combines both with a "mode" argument to toggle.
     *
     * @param data The data to process
     * @param password The password to encrypt/decrypt with
     * @param operation Whether to encrypt or decrypt
     */
    private static byte[] swap(byte[] data, String password, int operation) {
        if (operation != Cipher.DECRYPT_MODE
                && operation != Cipher.ENCRYPT_MODE) {
            throw new IllegalArgumentException("Must encrypt or decrypt");
        }

        byte[] widthPassword = enforcePasswordLength(password);
        Key key = new SecretKeySpec(widthPassword, 0, KEY_LENGTH, ALGORITHM);

        try {
            Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(operation, key);

            // Do the business
            return c.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException |
                NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encrypt(byte[] data, String password) {
        return swap(data, password, Cipher.ENCRYPT_MODE);
    }

    public static byte[] decrypt(byte[] data, String password) {
        return swap(data, password, Cipher.DECRYPT_MODE);
    }

    /**
     * Ensures that passwords are the correct bit-length.
     *
     * <p> Passwords are hashed, and the hash is truncated to the correct number
     * of bits.
     *
     * @param password Password to enforce length for
     */
    private static byte[] enforcePasswordLength(String password) {
        return enforcePasswordLength(password.getBytes());
    }

    /**
     * Ensures that passwords are the correct bit-length.
     *
     * <p> Passwords are hashed, and the hash is truncated to the correct number
     * of bits.
     *
     * @param password Password to enforce length for
     */
    private static byte[] enforcePasswordLength(byte[] password) {
        byte[] passwordHash;
        byte[] truncatedHash = new byte[KEY_LENGTH];

        passwordHash = SecurityService.getHash(password);

        System.arraycopy(passwordHash, 0, truncatedHash, 0, KEY_LENGTH);
        return truncatedHash;
    }
}
