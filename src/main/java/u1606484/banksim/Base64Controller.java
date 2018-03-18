package u1606484.banksim;

import java.util.Base64;

class Base64Controller {

    @SuppressWarnings("unused") // Debug method
    public static String toBase64(String data) {
        return toBase64(data.getBytes());
    }

    @SuppressWarnings("unused") // Debug method
    private static String toBase64(byte[] data) {
        return new String(Base64.getEncoder().encode(data));
    }

    @SuppressWarnings("unused") // Debug method
    private static byte[] fromBase64(byte[] data) {
        return Base64.getDecoder().decode(data);
    }

    @SuppressWarnings("unused") // Debug method
    public static byte[] fromBase64(String data) {
        return fromBase64(data.getBytes());
    }

    static String toHex(byte[] secretKey) {
        StringBuilder result = new StringBuilder();
        for (byte keyChunk : secretKey) {
            result.append(String.format("%02x", keyChunk));
        }
        return result.toString();
    }
}
