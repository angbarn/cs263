package u1606484.banksim.weblogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import u1606484.banksim.AesEncryption;
import u1606484.banksim.SecurityService;
import u1606484.banksim.TwoFactorService;
import u1606484.banksim.controllers.WebController;
import u1606484.banksim.databases.ApplicationDatabaseManager;
import u1606484.banksim.databases.PasswordData;
import u1606484.banksim.databases.UserAuthenticationPackage;
import u1606484.banksim.interfaces.ITwoFactorService;

/**
 * Effectively acts as a central point through which all data in the program
 * flows. Requests are interpreted by the WebController, who then calls upon
 * this class to carry out execution, and return necessary data to show to the
 * user.
 *
 * <p>Generally speaking, individual methods in this class will map relatively
 * closely to individual methods in another class of the program.
 */
public class LoginSystem {

    /**
     * The time in milliseconds that counts as one "step" of the OTAC algorithm
     */
    private static final int OTAC_STEP = 30 * 1000;
    /**
     * The number of "steps" of leeway when verifying a generated code.
     *
     * <p>E.g., with a value of 7, OTACs provided from up to 7 steps before now
     * will be accepted.
     */
    private static final int OTAC_STEP_WINDOW = 2;
    /**
     * The length in digits of an OTAC
     */
    private static final int OTAC_LENGTH = 8;

    /**
     * The service used for 2FA
     */
    private final ITwoFactorService twoFactorService;
    /**
     * The service used for accessing the database
     */
    private final ApplicationDatabaseManager databaseManager;

    public LoginSystem() {
        databaseManager = new ApplicationDatabaseManager();
        twoFactorService = new TwoFactorService(
                OTAC_LENGTH,
                OTAC_STEP,
                OTAC_STEP_WINDOW);
    }

    /**
     * Writes a log entry to the database with given content
     *
     * @param content The content of the entry
     */
    public void writeLog(String content) {
        byte[] encryptedContent = AesEncryption.encrypt(content.getBytes(),
                System.getenv("log_encryption_key"));
        long timestamp = System.currentTimeMillis();

        databaseManager.newLog(timestamp, encryptedContent);
    }

    /**
     * Generates an OTAC for a user, looking up their required secret key from
     * the database.
     *
     * @param userId The account ID of the customer to generate the OTAC for
     * @return The generated OTAC. In the case of failure, an empty string ("")
     * is returned.
     */
    private String generateOtac(int userId) {
        Optional<byte[]> otacKey = databaseManager.fetchLoginKey(userId);
        return otacKey.map(twoFactorService::generateOtac).orElse("");
    }

    /**
     * Verifies whether the attempted password for a user is correct.
     *
     * <p>Furthermore, if a password is found to be correct, but calculated
     * using fewer hashes than mandated by
     * {@link SecurityService#PASSWORD_HASH_PASSES},
     * then the password is recalculated and updated in the database.
     *
     * @param userId The account ID of the account to verify
     * @param passwordAttempt The attempted password in plaintext
     * @return If the attempt is correct, true. Otherwise false.
     */
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

    /**
     * Uses the 2FA service to verify an attempted OTAC
     *
     * @param userId The account ID of the account to verify the OTAC for
     * @param otacAttempt The attempted OTAC
     * @return If the OTAC is valid at this moment in time, true. Otherwise
     * false.
     */
    private boolean verifyOtac(int userId, String otacAttempt) {
        Optional<byte[]> secretKey = databaseManager.fetchLoginKey(userId);
        return secretKey
                .filter(key -> twoFactorService.verifyOtac(otacAttempt, key))
                .isPresent();
    }

    /**
     * Uses the 2FA service to transmit the OTAC to the user
     *
     * @param userId The account ID of the customer's account to notify of their
     * OTAC.
     */
    public void sendOtac(int userId) {
        Optional<String> phoneNumber = databaseManager.fetchPhoneNumber(userId);
        phoneNumber.ifPresent(s -> {
            String otac = generateOtac(userId);
            String message =
                    "NEVER share this code with anybody - not even Wondough "
                            + "Staff."
                            + "\nPlease use the code " + otac + " to log in.";
            twoFactorService.sendMessage(s, message);
        });
    }

    /**
     * Sets all sessions for a given user to "expired", thus signing the user
     * out of all locations.
     *
     * @param accountId The account ID of the account to sign out
     */
    private void signOut(int accountId) {
        databaseManager.invalidateSessionKeys(accountId);
    }

    /**
     * Assigns a brand new session key to the account, as well as supplying a
     * cookie to the client with the session token in it.
     *
     * @param accountId The account ID to set up a session for
     * @param sessionKey The session key to use for this new session
     * @param expiry When the session will expire
     * @param otacLevel The OTAC-level of this session (level of authentication
     * account is logged in at)
     * @param response The response to send to the user, which is used to set
     * cookies
     */
    private void assignNewSessionToken(int accountId, String sessionKey,
            long expiry, int otacLevel, HttpServletResponse response) {
        databaseManager
                .assignSessionKey(accountId, sessionKey, expiry, otacLevel);

        Cookie newCookie = new Cookie("session_token", sessionKey);
        // Set security flags
        newCookie.setHttpOnly(true);
        // todo - re-add this once you deploy
        //       newCookie.setSecure(true);
        response.addCookie(newCookie);
    }

    /**
     * Attempts to log a user in with provided credentials. If they are
     * successful, they are assigned a session and session key at OTAC level 0.
     *
     * @param accountId The account ID of the account to attempt to log in as
     * @param passwordAttempt The attempted password
     * @param response The response to send to the user, which is used to set
     * cookies
     * @return If the credentials are valid, then true. Otherwise false.
     */
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

    /**
     * Attempts to log a user in with provided OTAC. If they are successful,
     * they are assigned a session and session key at OTAC level 1.
     * @param accountId The account ID of the account to attempt to sign in as
     * @param otacAttempt The attempt for the OTAC
     * @param response The response to send to the user, which is used to set
     * cookies
     * @return If the credentials are valid, then true. Otherwise false.
     */
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

    /**
     * Attempts to fetch a {@link UserAuthenticationPackage} based on a
     * provided session key, enabling getting an account ID from a session.
     * @param sessionKey The session key to match against an account
     * @return An optional containing a UserAuthenticationPackage.
     * @see ApplicationDatabaseManager#getUserData(String)
     */
    public Optional<UserAuthenticationPackage> getUserFromSession(
            String sessionKey) {
        return databaseManager.getUserData(sessionKey);
    }

    /**
     * Invalidates all sessions
     * @param userId The account ID of the account to sign out
     * @see ApplicationDatabaseManager#invalidateSessionKeys(int)
     */
    public void terminateSessions(int userId) {
        databaseManager.invalidateSessionKeys(userId);
    }

    /**
     * Inserts a new user into the database.
     * @param firstName The customer's first name
     * @param lastName The customer's last name
     * @param phoneNumber The customer's phone number
     * @param addressLine1 The customer's first address line
     * @param addressLine2 The customer's second address line
     * @param postcode The customer's post code
     * @param county The customer's county
     * @param password1 The customer's password
     * @return The ID of the newly created user
     * @see ApplicationDatabaseManager#newCustomer
     */
    public int createUser(String firstName, String lastName,
            String phoneNumber, String addressLine1, String addressLine2,
            String postcode, String county, String password1) {
        return databaseManager
                .newCustomer(phoneNumber, firstName, lastName, password1,
                        SecurityService.PASSWORD_HASH_PASSES, addressLine1,
                        addressLine2, postcode, county);
    }

    /**
     * Uses the 2FA service to send a customer their account ID, enabling
     * them to log in.
     * @param address The address to use to contact the customer
     * @param newUserId The ID to transmit to the customer
     */
    public void sendAccountId(String address, int newUserId) {
        twoFactorService.sendMessage(address,
                "Welcome to Wondough! Your new account number is " + newUserId
                        + ". Please use this to log in.");
    }

    /**
     * Temporary, debugging function to fetch and display logging information
     * @return List of log entries
     * @see WebController#dumpLogs()
     */
    public List<String> dumpLogs() {
        return databaseManager.dumpLogs(System.getenv("log_encryption_key"))
                .orElse(new ArrayList<>());
    }
}
