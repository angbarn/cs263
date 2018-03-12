package u1606484.banksim;

import u1606484.banksim.databases.ApplicationDatabaseManager;
import u1606484.banksim.interfaces.ITwoFactorService;

public class LoginSystem {

    private static final int OTAC_PRECISION = 60 * 5;
    private final ITwoFactorService twoFactorService;
    private final ApplicationDatabaseManager databaseManager;

    public LoginSystem() {
        databaseManager = new ApplicationDatabaseManager();
        twoFactorService = new DummyTwoFactor();
    }

    private static byte[] getArbitraryBytes(int len) {
        byte[] ret = new byte[len];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = 0;
        }
        return ret;
    }

    // TODO
    private String generateOtac(int userId) {
        byte[] otacKey = databaseManager.fetchLoginKey(userId);
        String otacKeyBase64 = Base64Controller.toBase64(otacKey);

        return SecurityService
                .generateOtac(OTAC_PRECISION, otacKeyBase64);
    }

    private boolean verifyPassword(int userId, String passwordAttempt) {
        return databaseManager.verifyPassword(userId, passwordAttempt);
    }

    private void sendOtac(int userId) {
        String phoneNumber = databaseManager.fetchPhoneNumber(userId);
        String otac = generateOtac(userId);

        twoFactorService.sendTwoFactorCode(phoneNumber, otac);
    }
    /*
    // TODO
    private String generateOtac(int userId) {
        StringBuilder content = new StringBuilder();
        byte[] nonce = getLoginNonce(userId);

        content.append(userId);
        content.append(Base64Controller.toBase64(nonce));

        return SecurityService.generateOtac(OTAC_PRECISION, content.toString());
    }
    */

    /*
    // TODO
    private byte[] getLoginNonce(int userId) {
        int t = 20;
        return getArbitraryBytes(t);
    }
    */

    /*
    // TODO
    private void sendOtac(int userId) {
        String email = userId + "@host.domain";
        System.out.println(email + ": " + generateOtac(userId));
    }
    */

    public void main(String[] arguments) {
        int targetUserId = 1;
        String passwordAttempt = "hello";
        String otacAttempt = generateOtac(targetUserId);

        if (verifyPassword(targetUserId, passwordAttempt)) {
            String otac = generateOtac(targetUserId);
            sendOtac(targetUserId);

            if (otac.equals(otacAttempt)) {
                System.out.println("lottery");
            } else {
                System.out.println("Computer says no");
            }
        } else {
            System.out.println("Computer says no");
        }
    }

}
