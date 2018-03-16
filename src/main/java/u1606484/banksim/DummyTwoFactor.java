package u1606484.banksim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
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
     * Transmits the provided content to the provided address.
     *
     * @param contactAddress The phone number / email / etc. to use to transmit
     * the OTAC
     * @param content The content to transmit
     */
    @Override
    public void sendMessage(String contactAddress, String content) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL filePath = Optional.ofNullable(classLoader.getResource(FILE_NAME))
                .orElseThrow(
                        () -> new IllegalStateException("Out file not found"));

        try (FileWriter f = new FileWriter(new File(filePath.getFile()))) {
            f.write("Send to " + contactAddress + ":\n" + content);
            System.out
                    .println("Write to " + filePath.getPath() + "->" + content);
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
