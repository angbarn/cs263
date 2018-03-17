package u1606484.banksim.interfaces;

public interface ITwoFactorService {

    /**
     * Transmits the provided OTAC to the provided address.
     *
     * @param contactAddress The phone number / email / etc. to use to transmite
     * the OTAC
     * @param otac The OTAC to transmit
     */
    void sendMessage(String contactAddress, String otac);

    String generateOtac(byte[] secretKey, int offset);

    default String generateOtac(byte[] secretKey) {
        return generateOtac(secretKey, 0);
    }

    int getWindowSize();

    default boolean verifyOtac(String attempt, byte[] secretKey) {
        for (int i = 0; i > -getWindowSize(); i--) {
            String correctOtac = generateOtac(secretKey, i);
//            System.out.println(correctOtac + " vs " + attempt);
            if (attempt.equals(correctOtac)) {
                return true;
            }
        }
        return false;

//        return Stream
//                .iterate(0, x -> x - 1)
//                .limit(getWindowSize())
//                .map(i -> generateOtac(secretKey, i))
//                .peek(otac -> System.out
//                        .println("Otac test: " + attempt + " vs " + otac))
//                .map(attempt::equals)
//                .reduce((aBoolean, aBoolean2) -> aBoolean || aBoolean2)
//                .orElse(false);
    }
}
