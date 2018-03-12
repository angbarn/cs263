package u1606484.banksim;

import java.util.Base64;

public class Base64Controller {

    public static String toBase64(String data) {
        return toBase64(data.getBytes());
    }

    public static String toBase64(byte[] data) {
        return new String(Base64.getEncoder().encode(data));
    }

    public static byte[] fromBase64(byte[] data) {
        return Base64.getDecoder().decode(data);
    }

    public static byte[] fromBase64(String data) {
        return fromBase64(data.getBytes());
    }

    public static String bytesToRawString(byte[] data) {
        return new String(data);
    }

    public static String toHex(byte[] secretKey) {
        StringBuilder result = new StringBuilder();
        for (byte keyChunk : secretKey) {
            result.append(String.format("%02x", keyChunk));
        }
        return result.toString();
    }
}
