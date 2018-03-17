package u1606484.banksim;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import u1606484.banksim.interfaces.IOtacGenerator;
import u1606484.banksim.interfaces.ITwoFactorService;

public class TwoFactorService implements ITwoFactorService {

    private final static String ACCOUNT_SID = System
            .getenv("twilio_account_sid");
    private final static String AUTH_TOKEN = System.getenv("twilio_auth_token");
    private final IOtacGenerator generator;
    private final int otacStepWindow;

    public TwoFactorService(int otacLength, int otacStep, int otacStepWindow) {
        this.generator = new OtacGenerator(otacLength, otacStep);
        this.otacStepWindow = otacStepWindow;
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
                new PhoneNumber("+441429450286"),
                messageBody);

        Message m = messageCreator.create();
        System.out.println(m.getBody());
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