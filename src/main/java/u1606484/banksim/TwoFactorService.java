package u1606484.banksim;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import java.security.SecureRandom;
import u1606484.banksim.interfaces.IOtacGenerator;
import u1606484.banksim.interfaces.ITwoFactorService;

public class TwoFactorService implements ITwoFactorService {

    private final static String ACCOUNT_SID = System
            .getenv("twilio_account_sid");
    private final static String AUTH_TOKEN = System.getenv("twilio_auth_token");
    private final IOtacGenerator generator;
    private final int otacStepWindowCount;

    public TwoFactorService(int otacLength, int otacStepSize,
            int otacStepWindow) {
        this.generator = new OtacGenerator(otacLength, otacStepSize);
        this.otacStepWindowCount = otacStepWindow;
    }

    public static void main(String[] arguments) throws InterruptedException {
        ITwoFactorService s = new TwoFactorService(8, 5000, 2);
        byte[] key = new byte[20];
        new SecureRandom().nextBytes(key);

        String otac1 = s.generateOtac(key, 0);
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            System.out.println(s.verifyOtac(otac1, key));
        }
    }

    /**
     * Transmits the provided OTAC to the provided address.
     *
     * @param contactAddress The phone number / email / etc. to use to transmite
     * the OTAC
     * @param messageBody The OTAC to transmit
     */
    @Override
    public void sendMessage(String contactAddress, String messageBody) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        MessageCreator messageCreator = Message.creator(
                new PhoneNumber(contactAddress),
                new PhoneNumber(System.getenv("twilio_phone_number")),
                messageBody);

        Message m = messageCreator.create();
        System.out.println(m.getBody());
    }

    @Override
    public String generateOtac(byte[] secretKey, int offset) {
        return generator.generateOtac(secretKey,
                generator.getTimestamp(System.currentTimeMillis(), offset));
    }

    @Override
    public int getWindowSize() {
        return otacStepWindowCount;
    }
}