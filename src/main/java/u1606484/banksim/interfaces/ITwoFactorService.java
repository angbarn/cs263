package u1606484.banksim.interfaces;

public interface ITwoFactorService {

    /**
     * Transmits the provided OTAC to the provided address.
     *
     * @param contactAddress The phone number / email / etc. to use to transmite
     * the OTAC
     * @param otac The OTAC to transmit
     */
    void sendTwoFactorCode(String contactAddress, String otac);
}
