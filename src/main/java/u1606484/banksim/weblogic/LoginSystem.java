package u1606484.banksim.weblogic;

import u1606484.banksim.DummyTwoFactor;
import u1606484.banksim.databases.ApplicationDatabaseManager;
import u1606484.banksim.interfaces.ITwoFactorService;

public class LoginSystem {

    /**
     * The time in milliseconds that counts as one "step" of the OTAC algorithm
     */
    private static final int OTAC_STEP = 30 * 1000;
    /**
     * The number of "steps" leeway when accepting a generated code.
     *
     * <p>E.g., with a value of 7, OTACs provided from up to 7 steps before now
     * will be accepted.
     */
    private static final int OTAC_STEP_WINDOW = 2;
    /**
     * The length in digits of an OTAC
     */
    private static final int OTAC_LENGTH = 8;

    private final ITwoFactorService twoFactorService;
    private final ApplicationDatabaseManager databaseManager;

    public LoginSystem() {
        databaseManager = new ApplicationDatabaseManager();
        twoFactorService = new DummyTwoFactor(
                OTAC_LENGTH,
                OTAC_STEP,
                OTAC_STEP_WINDOW);
    }

    public static void main(String[] arguments) {
        LoginSystem l = new LoginSystem();

        int accountId = 1;
        String passwordAttempt = "jess continues to be a disappointment";
        String otacAttempt = "79109224";

        if (l.attemptBasicLogin(accountId, passwordAttempt)) {
            l.sendOtac(accountId);

            if (l.attemptOtacLogin(accountId, passwordAttempt, otacAttempt)) {
                System.out.println("Success");
            } else {
                System.out.println("Rejected on OTAC");
            }
        } else {
            System.out.println("Rejected on password");
        }
    }

    private String generateOtac(int userId) {
        byte[] otacKey = databaseManager.fetchLoginKey(userId);

        return twoFactorService.generateOtac(otacKey);
    }

    private boolean verifyPassword(int userId, String passwordAttempt) {
        return databaseManager.verifyPassword(userId, passwordAttempt);
    }

    private boolean verifyOtac(int userId, String otacAttempt) {
        byte[] secretKey = databaseManager.fetchLoginKey(userId);
        return twoFactorService.verifyOtac(otacAttempt, secretKey);
    }

    private void sendOtac(int userId) {
        String phoneNumber = databaseManager.fetchPhoneNumber(userId);
        String otac = generateOtac(userId);

        twoFactorService.sendTwoFactorCode(phoneNumber, otac);
    }

    public boolean attemptBasicLogin(int accountId, String passwordAttempt) {
        return verifyPassword(accountId, passwordAttempt);
    }

    public boolean attemptOtacLogin(int accountId, String passwordAttempt,
            String otacAttempt) {
        boolean stageOne = verifyPassword(accountId, passwordAttempt);
        boolean stageTwo = verifyOtac(accountId, otacAttempt);

        return stageOne && stageTwo;
    }

}
