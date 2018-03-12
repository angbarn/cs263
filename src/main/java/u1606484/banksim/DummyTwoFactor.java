package u1606484.banksim;

import java.io.FileWriter;
import java.io.IOException;
import u1606484.banksim.interfaces.ITwoFactorService;

public class DummyTwoFactor implements ITwoFactorService {

    private static final String FILE_LOCATION =
            "C:\\Users\\angus\\Documents\\year2_local\\cs263\\cs263"
                    + "\\dummySend.txt";

    /**
     * Transmits the provided OTAC to the provided address.
     *
     * @param contactAddress The phone number / email / etc. to use to transmite
     * the OTAC
     * @param otac The OTAC to transmit
     */
    @Override
    public void sendTwoFactorCode(String contactAddress, String otac) {
        try (FileWriter f = new FileWriter(FILE_LOCATION)) {
            f.write("Send to " + contactAddress + ":\n" + otac);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
