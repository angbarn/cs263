package u1606484.banksim.weblogic;

import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import u1606484.banksim.DummyTwoFactor;
import u1606484.banksim.SecurityService;
import u1606484.banksim.databases.ApplicationDatabaseManager;
import u1606484.banksim.databases.PasswordData;
import u1606484.banksim.databases.SessionKeyPackage;
import u1606484.banksim.databases.UserAuthenticationPackage;
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

    /*
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
    */

    private String generateOtac(int userId) {
        Optional<byte[]> otacKey = databaseManager.fetchLoginKey(userId);
        if (otacKey.isPresent()) {
            return twoFactorService.generateOtac(otacKey.get());
        } else {
            return "";
        }
    }

    private boolean verifyPassword(int userId, String passwordAttempt) {
        Optional<PasswordData> data = databaseManager.getPasswordData(userId);
        if (data.isPresent()) {
            boolean success = SecurityService.verifyPassword(passwordAttempt,
                    data.get().getPasswordSalt(), data.get().getPasswordHash(),
                    data.get().getPasses());

            // Update password if out of date
            if (success && data.get().getPasses()
                    < SecurityService.PASSWORD_HASH_PASSES) {
                databaseManager.updatePassword(userId, passwordAttempt,
                        SecurityService.getSalt(),
                        SecurityService.PASSWORD_HASH_PASSES);
            }

            return success;
        } else {
            return false;
        }
    }

    private boolean verifyOtac(int userId, String otacAttempt) {
        Optional<byte[]> secretKey = databaseManager.fetchLoginKey(userId);
        return secretKey
                .filter(key -> twoFactorService.verifyOtac(otacAttempt, key))
                .isPresent();
    }

    public void sendOtac(int userId) {
        Optional<String> phoneNumber = databaseManager.fetchPhoneNumber(userId);
        if (phoneNumber.isPresent()) {
            String otac = generateOtac(userId);
            twoFactorService.sendTwoFactorCode(phoneNumber.get(), otac);
        }
    }

    private void signOut(int accountId) {
        databaseManager.invalidateSessionKeys(accountId);
    }

    private void assignNewSessionToken(int accountId, String sessionKey,
            long expiry, int otacLevel, HttpServletResponse response) {
        databaseManager
                .assignSessionKey(accountId, sessionKey, expiry, otacLevel);

        Cookie newCookie = new Cookie("session_token", sessionKey);
        // Set security flags
        newCookie.setHttpOnly(true);
        newCookie.setSecure(true);
        response.addCookie(newCookie);
    }

    public boolean attemptBasicLogin(int accountId, String passwordAttempt,
            HttpServletResponse response) {
        boolean success = verifyPassword(accountId, passwordAttempt);
        // Assign session key if correct
        if (success) {
            String sessionKey = SecurityService
                    .generateSessionKey(SecurityService.SESSION_KEY_LENGTH);
            long expiry = System.currentTimeMillis()
                    + SecurityService.SESSION_EXPIRY_LENGTH;

            assignNewSessionToken(accountId, sessionKey, expiry, 0, response);
        }

        return success;
    }

    public boolean attemptOtacLogin(int accountId, String otacAttempt,
            HttpServletResponse response) {
        boolean success = verifyOtac(accountId, otacAttempt);

        // Update session key if correct
        if (success) {
            // Sign out of all other devices - we can only be signed in
            // through the web interface in once place
            signOut(accountId);

            // If logged in successfully, assign a new, logged in session key
            String newSessionKey = SecurityService
                    .generateSessionKey(SecurityService.SESSION_KEY_LENGTH);
            long newExpiry = System.currentTimeMillis()
                    + SecurityService.SESSION_EXPIRY_LENGTH;
            assignNewSessionToken(accountId, newSessionKey, newExpiry, 1,
                    response);
        }

        return success;
    }

    public Optional<UserAuthenticationPackage> getUserFromSession(
            String sessionKey) {
        return databaseManager.getUserData(sessionKey);
    }

    public Optional<SessionKeyPackage> getSessionFromUser(int userId) {
        return databaseManager.getSessionKeyData(userId);
    }
}
