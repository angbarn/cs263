package u1606484.banksim.databases;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import u1606484.banksim.AesEncryption;
import u1606484.banksim.SecurityService;
import u1606484.banksim.databases.FunctionalHelpers.DatabaseBinding;
import u1606484.banksim.databases.FunctionalHelpers.UncheckedFunction;
import u1606484.banksim.databases.FunctionalHelpers.bBytes;
import u1606484.banksim.databases.FunctionalHelpers.bInteger;
import u1606484.banksim.databases.FunctionalHelpers.bLong;
import u1606484.banksim.databases.FunctionalHelpers.bString;

/**
 * Manages application-specific database operations. Effectively a higher-level,
 * more specific wrapper for {@link DatabaseManager}.
 *
 * <p>A lot of the "heavy lifting" for this class is performed by the {@link
 * FunctionalHelpers} class, and the {@link TransactionContainer} class.
 */
public class ApplicationDatabaseManager extends DatabaseManager {

    /**
     * Initialises the database class using the parent class.
     *
     * @see DatabaseManager
     */
    public ApplicationDatabaseManager() {
        super();
    }

    /**
     * Attempts to fetch all logs from the database, decrypt their content, and
     * then return them as a {@code List<String>} instance.
     *
     * <p>Realistically, this method would not exist in production. Therefore,
     * some of the data flow in the method is incorrect. For example, decryption
     * should be performed in the {@link u1606484.banksim.weblogic.LoginSystem}
     * class. To maintain a more simple implementation of a temporary operation,
     * more functionality was moved into the method for this class.
     *
     * @param key The decryption key to use
     * @return An optional list of strings of log entries - filled on success
     */
    public Optional<List<String>> dumpLogs(String key) {
        List<String> logDump = new ArrayList<>();

        String retrievalQuery = ""
                + "SELECT time_created, content "
                + "FROM log "
                + "ORDER BY time_created DESC";
        ResultSet logDumpResult = exec(retrievalQuery, new DatabaseBinding[]{},
                true);

        try {
            DateTimeFormatter f = DateTimeFormatter.ISO_DATE_TIME;
            ZoneId z = TimeZone.getDefault().toZoneId();

            while (logDumpResult.next()) {
                // Fetch raw database data
                long timestamp = logDumpResult.getLong(1);
                byte[] encryptedContent = logDumpResult.getBytes(2);
                // Decrypt content and format date
                byte[] decryptedContent = AesEncryption
                        .decrypt(encryptedContent, key);
                String content = new String(decryptedContent);
                LocalDateTime d = LocalDateTime
                        .ofInstant(Instant.ofEpochMilli(timestamp), z);
                // Construct log line
                logDump.add(d.format(f) + ": " + content);
            }

            return Optional.of(logDump);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Attempts to fetch a single {@link SessionKeyPackage} linked to the
     * account ID provided.
     *
     * @param userId The account ID to search for a session for
     * @return A single SessionKeyPackage instance if a session exists for the
     * account. Otherwise, an empty optional.
     * @see SessionKeyPackage
     */
    public Optional<SessionKeyPackage> getSessionKeyData(int userId) {
        String retrievalQuery = ""
                + "SELECT session_key, otac_authenticated "
                + "FROM session "
                + "WHERE expiry > ? AND customer_id = ?";
        DatabaseBinding[] retrievalBindings = new DatabaseBinding[]{
                new bLong(1, System.currentTimeMillis()),
                new bInteger(2, userId)
        };
        ResultSet keyResult = exec(retrievalQuery, retrievalBindings, true);

        return FunctionalHelpers.attemptSingleRetrieval(keyResult, r -> {
            String key = keyResult.getString(1);
            int authenticationStage = keyResult.getInt(2);
            return new SessionKeyPackage(key, authenticationStage);
        });
    }

    /**
     * Attempts to fetch information on a user's session, provided in a {@link
     * UserAuthenticationPackage} instance.
     *
     * <p>This should never return an empty optional due to database
     * constraints.
     *
     * @param sessionKey The session key to find information for
     * @return A single UserAuthenticationPackage instance if a matching user
     * can be found. Otherwise, an empty optional.
     */
    public Optional<UserAuthenticationPackage> getUserData(String sessionKey) {
        String retrievalQuery = ""
                + "SELECT customer_id, otac_authenticated "
                + "FROM session "
                + "WHERE expiry > ? AND session_key = ?";
        DatabaseBinding[] retrievalBindings = new DatabaseBinding[]{
                new bLong(1, System.currentTimeMillis()),
                new bString(2, sessionKey)
        };
        ResultSet idResult = exec(retrievalQuery, retrievalBindings, true);

        return FunctionalHelpers.attemptSingleRetrieval(idResult, r -> {
            int userId = r.getInt(1);
            int userOtac = r.getInt(2);
            return new UserAuthenticationPackage(userId, userOtac);
        });
    }

    /**
     * Sets the expiry for all session keys for the provided account ID to the
     * current time, thus invalidating them.
     *
     * @param userId The account ID for which to invalidate logins.
     */
    public void invalidateSessionKeys(int userId) {
        String updateQuery = ""
                + "UPDATE session "
                + "SET expiry = ? "
                + "WHERE customer_id = ?";
        DatabaseBinding[] updateBindings = new DatabaseBinding[]{
                new bLong(1, System.currentTimeMillis() - 1),
                new bInteger(2, userId)
        };

        exec(updateQuery, updateBindings, false);
    }

    /**
     * Writes a session to the database, with state specified by parameters.
     *
     * @param userId The account ID to link the session to
     * @param sessionKey The session key for the new session entry
     * @param expiry When the session will expire
     * @param otacLevel 0 if the user has only authenticated via password. 1 if
     * the user has also authenticated via OTAC.
     */
    public void assignSessionKey(int userId, String sessionKey,
            long expiry, int otacLevel) {
        String insertionQuery = ""
                + "INSERT INTO session "
                + "(session_key, customer_id, expiry, otac_authenticated) "
                + "VALUES (?, ?, ?, ?)";
        DatabaseBinding[] insertionBindings = new DatabaseBinding[]{
                new bString(1, sessionKey),
                new bInteger(2, userId),
                new bLong(3, expiry),
                new bInteger(4, otacLevel)};

        exec(insertionQuery, insertionBindings, false);
    }

    /**
     * Updates the OTAC-level of an existing session within the database.
     *
     * @param sessionKey The session key of the session to update
     * @param otacStage The new OTAC-level to set
     */
    public void setOtacAuthenticated(String sessionKey, int otacStage) {
        String updateQuery = ""
                + "UPDATE session SET "
                + "otac_authenticated = ? "
                + "WHERE session_key = ?";
        DatabaseBinding[] updateBindings = new DatabaseBinding[]{
                new bInteger(1, otacStage),
                new bString(2, sessionKey)
        };
        exec(updateQuery, updateBindings, false);
    }

    /**
     * Inserts a new customer into the database.
     *
     * <p>The {@link ApplicationDatabaseManager#newSecurity(String, int)} and
     * {@link ApplicationDatabaseManager#newAddress(String, String, String,
     * String)} methods handle insertion of data into linked tables.
     *
     * @param phoneNumber The customer's phone number
     * @param firstName The customer's first nmae
     * @param lastName The customer's last nmae
     * @param passwordPlaintext The customer's password in plaintext
     * @param passwordHashPasses The number of passes of the hashing function
     * used to apply over the plaintext
     * @param addressLine1 The first address line for the customer
     * @param addressLine2 The second address line for the customer
     * @param postcode The customer's postcode
     * @param county The customer's county
     * @return The ID of the newly inserted customer
     */
    public int newCustomer(String phoneNumber, String firstName,
            String lastName, String passwordPlaintext, int passwordHashPasses,
            String addressLine1, String addressLine2, String postcode,
            String county) {
        int addressId = newAddress(addressLine1, addressLine2, postcode,
                county);
        int securityId = newSecurity(passwordPlaintext, passwordHashPasses);

        String insertionQuery = ""
                + "INSERT INTO customer"
                + "(phone_number, first_name, last_name, address_id, "
                + "    security_id) "
                + "VALUES (?, ?, ?, ?, ?)";
        DatabaseBinding[] insertionBindings = new DatabaseBinding[]{
                new bString(1, phoneNumber),
                new bString(2, firstName),
                new bString(3, lastName),
                new bInteger(4, addressId),
                new bInteger(5, securityId)};

        return newGenericDatabaseRecord(insertionQuery, insertionBindings);
    }

    /**
     * Inserts a generic record into the database, and fetches its ID.
     *
     * <p>Insertion is handled by a {@link TransactionContainer}, which executes
     * an insertion and an ID getting query in a single transaction to prevent
     * database concurrency issues.
     *
     * @param insertionQuery The query to use for insertion
     * @param insertionBindings Parameters to use for insertion
     * @return The ID of the newly created record
     * @throws IllegalStateException The ID for the new record could not be
     * found
     */
    private int newGenericDatabaseRecord(String insertionQuery,
            DatabaseBinding[] insertionBindings) {
        String retrieveIdQuery = "SELECT last_insert_rowid()";

        TransactionContainer transaction = new TransactionContainer(
                getConnection(),
                new String[]{insertionQuery, retrieveIdQuery},
                new DatabaseBinding[][]{insertionBindings, {}},
                new boolean[]{false, true});

        // Return ID of newly created address entry
        List<Optional<ResultSet>> results = transaction.executeTransaction();

        // Fetch the first row from results, and select the ID column
        int newId = UncheckedFunction.<ResultSet, Integer>escapeFunction(
                x -> x.getInt(1)).apply(results.get(1).orElseThrow(
                () -> new IllegalStateException(
                        "Failed to get last insert id")));
        transaction.close(results);

        return newId;
    }

    /**
     * Inserts a new address into the database.
     *
     * @param addressLine1 The customer's first address line
     * @param addressLine2 The customer's second address line
     * @param postcode The customer's postcode
     * @param county The customer's county
     * @return The ID of the newly inserted customer record
     */
    private int newAddress(String addressLine1, String addressLine2,
            String postcode, String county) {
        String insertionQuery = ""
                + "INSERT INTO address "
                + "(address_1, address_2, postcode, county) "
                + "VALUES (?, ?, ?, ?)";
        DatabaseBinding[] insertionBindings = new DatabaseBinding[]{
                new bString(1, addressLine1),
                new bString(2, addressLine2),
                new bString(3, postcode),
                new bString(4, county)};

        return newGenericDatabaseRecord(insertionQuery, insertionBindings);
    }

    /**
     * Inserts a new security entry into the database. Generation of secure
     * random data is performed in this class, as well as hashing the plaintext
     * password.
     *
     * @param passwordPlaintext The plaintext of the password. It will be hashed
     * before storage.
     * @param passwordHashPasses The number of passes to use for the hashing
     * algorithm when hashing the plaintext.
     * @return The ID of the newly created security entry.
     */
    private int newSecurity(String passwordPlaintext,
            int passwordHashPasses) {
        byte[] loginSalt = SecurityService.getSalt();
        byte[] supportInSalt = SecurityService.getSalt();
        byte[] supportOutSalt = SecurityService.getSalt();
        byte[] passwordSalt = SecurityService.getSalt();
        byte[] passwordHash = SecurityService
                .getPasswordHash(passwordPlaintext, passwordSalt,
                        passwordHashPasses);

        DatabaseBinding[] insertionBindings = new DatabaseBinding[]{
                new bBytes(1, loginSalt),
                new bBytes(2, supportInSalt),
                new bBytes(3, supportOutSalt),
                new bBytes(4, passwordHash),
                new bBytes(5, passwordSalt),
                new bInteger(6, passwordHashPasses)};
        String insertionQuery = ""
                + "INSERT INTO security "
                + "(login_salt, support_in_salt, support_out_salt, password, "
                + "    password_salt, password_hash_passes) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        return newGenericDatabaseRecord(insertionQuery, insertionBindings);
    }

    /**
     * Inserts a new log entry into the database.
     *
     * @param creationDate The date of creation for the log entry
     * @param content The content for the log entry
     */
    public void newLog(long creationDate, byte[] content) {
        DatabaseBinding[] insertionBindings = new DatabaseBinding[]{
                new bLong(1, creationDate),
                new bBytes(2, content)};
        String insertionQuery = ""
                + "INSERT INTO log "
                + "(time_created, content) "
                + "VALUES (?, ?)";

        newGenericDatabaseRecord(insertionQuery, insertionBindings);
    }

    /**
     * Writes a change to a user's password to the database. This could
     * potentially include running more passes of the hashing function on the
     * password in the case of a security upgrade.
     *
     * @param securityId The ID of the user's security record
     * @param passwordPlaintext The plaintext of the new password
     * @param passwordSalt The salt of the new password
     * @param hashIterations The number of iterations to use for the new
     * password
     */
    public void updatePassword(int securityId, String passwordPlaintext,
            byte[] passwordSalt, int hashIterations) {
        byte[] newPassword = SecurityService
                .getPasswordHash(passwordPlaintext, passwordSalt,
                        hashIterations);
        DatabaseBinding[] retrievalBindings = new DatabaseBinding[]{
                new bBytes(1, newPassword),
                new bBytes(2, passwordSalt),
                new bInteger(3, hashIterations),
                new bInteger(4, securityId)};
        String updateQuery = ""
                + "UPDATE security "
                + "SET password=?, password_salt=?, password_hash_passes=? "
                + "WHERE security_id=?";

        exec(updateQuery, retrievalBindings, false);
    }

    /**
     * Attempts to fetch data about a user's password
     *
     * <p>This should never return an empty optional due to database
     * constraints.
     *
     * @param userId The account ID to fetch data for
     * @return An optional containing a PasswordData instance if a password
     * could be located. Otherwise, an empty optional.
     * @see PasswordData
     */
    public Optional<PasswordData> getPasswordData(int userId) {
        DatabaseBinding[] retrievalBindings = new DatabaseBinding[]{
                new bInteger(1, userId)};

        // Fetch password data
        String retrievalQuery =
                "SELECT s.security_id, s.password, s.password_salt, "
                        + "    s.password_hash_passes "
                        + "FROM customer c "
                        + "JOIN security s ON c.security_id = s.security_id "
                        + "WHERE c.customer_id = ?";

        ResultSet rs = exec(retrievalQuery, retrievalBindings, true);

        return FunctionalHelpers.attemptSingleRetrieval(rs, r -> {
            byte[] password = rs.getBytes(2);
            byte[] salt = rs.getBytes(3);
            int passes = rs.getInt(4);

            return new PasswordData(password, salt, passes);
        });
    }

    /**
     * Attempts to fetch a user's secret key to use for OTAC generation.
     *
     * <p>This should never return an empty optional due to database
     * constraints.
     *
     * @param userId The account ID to fetch the login secret key for
     * @return An optional containing the login secret key, if available.
     * Otherwise, an empty optional.
     */
    public Optional<byte[]> fetchLoginKey(int userId) {
        String retrievalQuery = ""
                + "SELECT s.login_salt "
                + "FROM customer c "
                + "JOIN security s ON c.security_id "
                + "WHERE c.customer_id = ?";
        DatabaseBinding[] retrievalBindings = new DatabaseBinding[]{
                new bInteger(1, userId)};
        ResultSet rs = exec(retrievalQuery, retrievalBindings, true);

        return FunctionalHelpers.attemptSingleRetrieval(rs, r -> r.getBytes(1));
    }

    /**
     * Attempts to fetch a user's phone number to use for 2FA.
     *
     * <p>This should never return an empty optional due to database
     * constraints.
     *
     * @param userId The account ID to fetch the phone number
     * @return An optional containing the phone number, if available. Otherwise,
     * an empty optional.
     */
    public Optional<String> fetchPhoneNumber(int userId) {
        String retrievalQuery = ""
                + "SELECT c.phone_number "
                + "FROM customer c "
                + "WHERE c.customer_id = ?";
        DatabaseBinding[] retrievalBindings = new DatabaseBinding[]{
                new bInteger(1, userId)};
        ResultSet rs = exec(retrievalQuery, retrievalBindings, true);

        return FunctionalHelpers
                .attemptSingleRetrieval(rs, r -> r.getString(1));
    }
}
