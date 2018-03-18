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

public class ApplicationDatabaseManager extends DatabaseManager {

    public ApplicationDatabaseManager() {
        super();
    }

    public static void main(String[] arguments) {
        ApplicationDatabaseManager m = new ApplicationDatabaseManager();

        try {
            m.newCustomer("07000000000", "Jeremy", "Irons",
                    "jess continues to be a disappointment", 1,
                    "31 Cherry Street", "", "gu76 5pq", "Cambridgeshire");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                long timestamp = logDumpResult.getLong(1);
                byte[] encryptedContent = logDumpResult.getBytes(2);

                byte[] decryptedContent = AesEncryption
                        .decrypt(encryptedContent, key);
                String content = new String(decryptedContent);

                LocalDateTime d = LocalDateTime
                        .ofInstant(Instant.ofEpochMilli(timestamp), z);

                logDump.add(d.format(f) + ": " + content);
            }

            return Optional.of(logDump);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

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

        int newId = UncheckedFunction.<ResultSet, Integer>escapeFunction(
                x -> x.getInt(1)).apply(results.get(1).orElseThrow(
                () -> new IllegalStateException(
                        "Failed to get last insert id")));
        transaction.close(results);

        return newId;
    }

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
