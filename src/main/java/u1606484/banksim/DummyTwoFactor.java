package u1606484.banksim;

import java.io.FileWriter;
import java.io.IOException;
import u1606484.banksim.interfaces.IOtacGenerator;
import u1606484.banksim.interfaces.ITwoFactorService;

public class DummyTwoFactor implements ITwoFactorService {

    private static final String FILE_NAME = "dummySend.txt";
    private final IOtacGenerator generator;
    private final int otacStepWindow;

    public DummyTwoFactor(int otacLength, int otacStep, int otacStepWindow) {
        this.generator = new OtacGenerator(otacLength, otacStep);
        this.otacStepWindow = otacStepWindow;
    }

    /**
     * Transmits the provided OTAC to the provided address.
     *
     * @param contactAddress The phone number / email / etc. to use to transmite
     * the OTAC
     * @param otac The OTAC to transmit
     */
    @Override
    public void sendTwoFactorCode(String contactAddress, String otac) {
        try (FileWriter f = new FileWriter(
                this.getClass().getResource(FILE_NAME).getPath())) {
            f.write("Send to " + contactAddress + ":\n" + otac);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String generateOtac(byte[] secretKey, int offset) {
        return generator.generateOtac(secretKey, offset);
    }

    @Override
    public int getWindowSize() {
        return otacStepWindow;
    }
}
