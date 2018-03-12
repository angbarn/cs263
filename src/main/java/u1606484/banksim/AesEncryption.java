package u1606484.banksim;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class provides the functionality to encrypt or decrypt files. <p>In
 * order to encrypt a file, the {@link AesEncryption#swap} method is called with
 * a parameter of {@code Cipher.ENCRYPT_MODE}. Similarly, to decrypt a file, the
 * same method is called, but with a parameter of {@code Cipher.DECRYPT_MODE}.
 * <p>No other modes of operation are available. <p>This is a very high level
 * class, so no padding of data / other processing should be necessary.
 */
public class AesEncryption {

	/**
	 * Algorithm to use for encryption/decryption
	 */
	private static final String ALGO = "AES";

	/**
	 * Length of key in bytes
	 */
	private static final int KEY_LENGTH = 16; // 128 bits

	/**
	 * Reads a file's binary data as an array of bytes
	 *
	 * @param fileName The path to the file to read
	 * @return Array of bytes for binary data in file
	 */
	private static byte[] readFile(String fileName) {
		try {
			Path path = Paths.get(fileName);
			return Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("IOE");
		}
	}

	/**
	 * Writes an array of bytes to a file as binary data
	 *
	 * @param data The binary data to write to file
	 * @param fileName The path to save the file
	 */
	private static void writeFile(byte[] data, String fileName) {
		try (FileOutputStream fos = new FileOutputStream(fileName)) {
			fos.write(data);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("IOE");
		}
	}

	/**
	 * Encrypt or decrypt a file, depending on the mode. <p>A vast majority of
	 * the work encrypting and decrypting a file is the same, so this function
	 * combines both with a "mode" variable to toggle between them.
	 *
	 * @param fileName The path to the target file
	 * @param password The password to use for encryption/decryption
	 * @param mode Whether encrypting or decrypting
	 * @param extension The extension to add to the output file so that it
	 * isn't
	 * overwritten.
	 */
	public static void swapFile(String fileName, String password, int mode,
			String extension) throws IllegalBlockSizeException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, BadPaddingException {
		byte[] data = readFile(fileName);
		byte[] newData = swap(data, password, mode);
		writeFile(newData, fileName + extension);
	}

	/**
	 * Encrypts or decrypts an array of bytes using a specified password <p>A
	 * vast majority of the work encrypting or decrypting data is the same, so
	 * this method combines both with a "mode" argument to toggle.
	 *
	 * @param data The data to process
	 * @param password The password to encrypt/decrypt with
	 * @param opmode Whether to encrypt or decrypt
	 */
	private static byte[] swap(byte[] data, String password, int opmode)
			throws NoSuchAlgorithmException, IllegalBlockSizeException,
			NoSuchPaddingException, InvalidKeyException, BadPaddingException {
		if (opmode != Cipher.DECRYPT_MODE && opmode != Cipher.ENCRYPT_MODE) {
			throw new IllegalArgumentException("Must encrypt or decrypt");
		}

		byte[] widthPassword = enforcePasswordLength(password);

		Key key = new SecretKeySpec(widthPassword, ALGO);

		Cipher c = Cipher.getInstance(ALGO);
		c.init(opmode, key);

		// Do the business
		return c.doFinal(data);
	}

	/**
	 * Ensures that passwords are the correct bit-length. <p> Passwords are
	 * hashed, and the hash is truncated to the correct number of bits.
	 * @param password Password to enforce length for
	 */
	private static byte[] enforcePasswordLength(String password) {
		return enforcePasswordLength(password.getBytes());
	}

	/**
	 * Ensures that passwords are the correct bit-length. <p> Passwords are
	 * hashed, and the hash is truncated to the correct number of bits.
	 * @param password Password to enforce length for
	 */
	private static byte[] enforcePasswordLength(byte[] password) {
		byte[] passwordHash;
		byte[] truncatedHash = new byte[KEY_LENGTH];

		passwordHash = SecurityService.getHash(password);

		System.arraycopy(passwordHash, 0, truncatedHash, 0, 16);
		return truncatedHash;
	}
}
