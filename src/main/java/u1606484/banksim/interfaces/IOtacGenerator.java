package u1606484.banksim.interfaces;

/**
 * Handles generation of time-based One-Time Access Codes used for loggin in.
 *
 * <p>The {@link IOtacGenerator#generateOtac} method takes a secret key and a
 * time in milliseconds, and produces a calculated Otac. Typically, a call to
 * {@link IOtacGenerator#getTimestamp} will also be necessary, which groups time
 * by "windows".
 *
 * @see u1606484.banksim.weblogic.LoginSystem for constants used in OTAC
 * generation.
 */
public interface IOtacGenerator {

    /**
     * Creates a one-time access code deterministically reproducible from the
     * same secret key and millisecond time value
     *
     * @param secretKey The secret key to use for code generation
     * @param timeMillis The exact time in milliseconds to create an OTAC for.
     * @return The OTAC generated from input parameters.
     * @see u1606484.banksim.weblogic.LoginSystem for constants used in OTAC
     * generation.
     */
    String generateOtac(byte[] secretKey, long timeMillis);

    /**
     * "Groups" a time in milliseconds, effectively converting a millisecond
     * time into a single window, which can be provided to {@link
     * IOtacGenerator#generateOtac} to produce OTACs reproducable over a window
     * of time, as opposed to for only one millisecond.
     *
     * @param rawTimeMillis The time in milliseconds to group
     * @param offset The number of groups away to shift the result
     * @return The OTAC that would be generated offset groups into the future
     * (allowing negatives) at the given time
     * @see u1606484.banksim.weblogic.LoginSystem for constants used in OTAC
     * generation.
     */
    long getTimestamp(long rawTimeMillis, int offset);

}
