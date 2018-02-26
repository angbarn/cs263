package u1606484.banksim;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class OtacGenerator {
    /**
     * Lookup table used for converting binary chunks to human-readable
     * characters
     */
    private static final char[] lookup = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
        'G', 'H', 'J', 'K', 'L', 'M', 'P', 'Q',
        'R', 'S', 'T', 'U', 'W', 'X', 'Y', 'Z'
    };

    private static final int CHUNK_BIT_COUNT = 5;
    private static final int OTAC_LENGTH = 8;

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

    private static byte[] getHash(String message)
            throws NoSuchAlgorithmException {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        return d.digest(message.getBytes());
    }

    /**
     * Maps a hash to an 8 character code
     */
    private static String hashToCode(byte[] hash) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < OTAC_LENGTH; i++) {
            int lookupIndex = (int) byteSubstring(hash, i*5);
            out.append(lookup[lookupIndex]);
        }

        return out.toString();
    }

    public static String generateOtac(int precision, String content) {
        long timeValue = System.currentTimeMillis() / precision / 1000;
        try {
            byte[] totalHash = getHash(timeValue + content);
            return hashToCode(totalHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalStateException("Invalid hash algorithm");
        }
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
           sb.append(String.format("%02x", b));
        return sb.toString();
     }

    public static void main(String[] arguments)
            throws InterruptedException {
        // 00000001 10000000
        long lastSecond = 0;
        int precision = 10;
        for (int i = 0; i < 10000; i++) {
            long currentSecond = System.currentTimeMillis() / 1000 % 60 / precision;
            if (currentSecond != lastSecond) {
                lastSecond = currentSecond;
                System.out.println(generateOtac(precision, "bananarama"));
            }
            Thread.sleep(1000);
        }
    }
}