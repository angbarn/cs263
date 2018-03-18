package u1606484.banksim;

import java.util.Base64;

/**
 * A utility class containing methods to convert between different binary
 * representational systems.
 */
class Base64Controller {

    /**
     * Converts a string to bytes, and then to a base 64 representing those
     * bytes
     * @param data The data to convert
     * @return The conversion result
     */
    @SuppressWarnings("unused") // Debug method
    public static String toBase64(String data) {
        return toBase64(data.getBytes());
    }

    /**
     * Converts bytes to a base 64 representation
     * @param data The data to convert
     * @return The conversion result
     */
    @SuppressWarnings("unused") // Debug method
    private static String toBase64(byte[] data) {
        return new String(Base64.getEncoder().encode(data));
    }

    /**
     * Converts a set of bytes from base 64 to a byte array
     * @param data Base 64 bytes to convert
     * @return Byte array equivalent to base 64 bytes
     */
    @SuppressWarnings("unused") // Debug method
    private static byte[] fromBase64(byte[] data) {
        return Base64.getDecoder().decode(data);
    }

    /**
     * Converts a string from base 64 to a byte array
     * @param data Base 64 string to convert
     * @return Byte array equivalent to base 64 string
     */
    @SuppressWarnings("unused") // Debug method
    public static byte[] fromBase64(String data) {
        return fromBase64(data.getBytes());
    }

    /**
     * Converts a series of bytes to a hexadecimal string
     * @param secretKey The bytes to convert
     * @return A hexadecimal string equivalent to the provided bytes
     */
    static String toHex(byte[] secretKey) {
        StringBuilder result = new StringBuilder();
        for (byte keyChunk : secretKey) {
            result.append(String.format("%02x", keyChunk));
        }
        return result.toString();
    }
}
