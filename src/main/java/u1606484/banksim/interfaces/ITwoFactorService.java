package u1606484.banksim.interfaces;

/**
 * Handles 2FA authentication for the webapp.
 *
 * <p>The {@code sendMessage} method will use the provided contact address to
 * send the provided message. This could theoretically be any form of contact
 * address, such as email or phone.
 *
 * <p>Furthermore, the {@code verifyOtac} and {@code generateOtac} methods are
 * useful for issuing and checking the accuracy of OTACs used for any part of
 * the system.
 */
public interface ITwoFactorService {

    /**
     * Transmits the provided message to the provided address. Since the concept
     * of a two factor authentication system is reasonably broad, this could
     * feasibly be an email, a phone number or something else entirely.
     *
     * @param contactAddress The phone number / email / etc. to use to transmit
     * the OTAC
     * @param message The OTAC to transmit
     */
    void sendMessage(String contactAddress, String message);

    /**
     * Generates an OTAC with a given offset in terms of "windows".
     *
     * <p>For example, assuming a window size of 30 seconds, an otac generated
     * with an offset of 1 is guaranteed to be the same as one generated 30
     * seconds in the future.
     *
     * @param secretKey The secret key to use for OTAC generation
     * @param offset The offset in terms of the number of windows
     * @return The generated OTAC
     */
    String generateOtac(byte[] secretKey, int offset);

    /**
     * An override for the {@code generateOtac} method which simply defaults to
     * using no offset
     *
     * @param secretKey The secret key to use for OTAC generation
     * @return The generated OTAC
     */
    default String generateOtac(byte[] secretKey) {
        return generateOtac(secretKey, 0);
    }

    /**
     * Gets the maximum number of windows away that an OTAC can be before it is
     * rejected. E.g., if a window is 30 seconds, and the window count is 2,
     * then as a minimum, a code will be accepted for 30 seconds.
     *
     * <p>The calculation {@code (getWindowCount() - 1) * WINDOW_SIZE} would
     * provide a minimum guaranteed acceptance time for a generated code.
     *
     * @return The maximum number of windows away that an OTAC can be.
     */
    int getWindowCount();

    /**
     * Checks all still-legal OTAC windows against the provided attempt.
     *
     * @param attempt The OTAC attempt
     * @param secretKey The secret key used for OTAC generation
     * @return If any comparisons match, then true. Otherwise false.
     */
    default boolean verifyOtac(String attempt, byte[] secretKey) {
        for (int i = 0; i > -getWindowCount(); i--) {
            String correctOtac = generateOtac(secretKey, i);
            if (attempt.equals(correctOtac)) {
                return true;
            }
        }
        return false;
    }
}
