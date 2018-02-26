import java.nio.ByteBuffer;
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
    // /** How many bytes of the hash are used for a single code in total */
    // private static final int hashSampleSize;
    // /** How many bits make up a single chunk */
    // private static final int chunkBits;
    // /** How many chunks there are in total */
    // private static final int chunkCount;

    // static {
    //     hashSampleSize = 5;
    //     chunkCount = 8;
    //     lookupBytes = (int) (Math.log(lookup.length) / Math.log(2));

    //     assert (hashSampleSize * 8) == (chunkCount * lookupBytes);
    // }

    private static byte byteSubstring(byte[] bytes, int startPos, int size) {
        byte newByte = 0;

        if (size > 8) {
            throw new IllegalStateException("8 bits in a byte");
        }

        for (int i = 0; i < size; i++) {
            int targetGlobalBit = startPos + i;
            int targetByte = targetGlobalBit / 8;
            int targetByteBit = targetGlobalBit % 8;
            byte singleBit = (byte)
                    ((bytes[targetByte] >> (7 - targetByteBit)) & 1);
            
            newByte += singleBit << (size - 1 - i);
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
    private static String hashToCode(byte[] hash, int codeLength) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            int lookupIndex = (int) byteSubstring(hash, i*5, 5);
            out.append(lookup[lookupIndex]);
        }

        return out.toString();
    }

    public static String generateOtac(int precision, String content) {
        long timeValue = System.currentTimeMillis() / precision / 1000;
        try {
            byte[] totalHash = getHash(timeValue + content);
            return hashToCode(totalHash, 8);
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
        while (true) {
            long currentSecond = System.currentTimeMillis() / 1000 % 60 / precision;
            if (currentSecond != lastSecond) {
                lastSecond = currentSecond;
                System.out.println(generateOtac(precision, "bananarama"));
            }
            Thread.sleep(1000);
        }
    }
}