package u1606484.banksim;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import java.security.SecureRandom;
import u1606484.banksim.interfaces.IOtacGenerator;
import u1606484.banksim.interfaces.ITwoFactorService;

/**
 * An implementation of the {@link ITwoFactorService} interface using Twilio's
 * SMS API.
 *
 * <p>The bulk of the work is handlded by Twilio's API, so this class
 * effectively acts as a wrapper around that API.
 */
public class TwoFactorService implements ITwoFactorService {

    /**
     * The account SID to sign in to Twilio with
     */
    private final static String ACCOUNT_SID = System
            .getenv("twilio_account_sid");
    /**
     * The API key assigned to the account by twilio
     */
    private final static String AUTH_TOKEN = System.getenv("twilio_auth_token");
    /**
     * The OTAC generator to use when providing OTACs.
     */
    private final IOtacGenerator generator;
    /**
     * The maximum acceptable number of windows away from a current OTAC a
     * provided OTAC may be to still be accepted.
     */
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
     * @param contactAddress The phone number / email / etc. to use to transmit
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

    /**
     * Calls upon the generator to provide an OTAC at a certain offset
     *
     * @param secretKey The secret key to use for OTAC generation
     * @param offset The offset in terms of the number of windows
     * @return A generated OTAC.
     */
    @Override
    public String generateOtac(byte[] secretKey, int offset) {
        return generator.generateOtac(secretKey,
                generator.getTimestamp(System.currentTimeMillis(), offset));
    }

    /**
     * Gets the window count
     *
     * @return The window count
     * @see ITwoFactorService#getWindowCount()
     */
    @Override
    public int getWindowCount() {
        return otacStepWindowCount;
    }
}