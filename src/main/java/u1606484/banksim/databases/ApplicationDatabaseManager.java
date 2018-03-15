package u1606484.banksim.databases;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import u1606484.banksim.SecurityService;
import u1606484.banksim.databases.FunctionalHelpers.DatabaseBinding;
import u1606484.banksim.databases.FunctionalHelpers.UncheckedFunction;
import u1606484.banksim.databases.FunctionalHelpers.bBytes;
import u1606484.banksim.databases.FunctionalHelpers.bInteger;
import u1606484.banksim.databases.FunctionalHelpers.bLong;
import u1606484.banksim.databases.FunctionalHelpers.bString;

public class ApplicationDatabaseManager extends DatabaseManager {

    private static final int HASH_PASSES = 1;

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

    public Optional<SessionKeyPackage> getSessionKeyData(int userId) {
        String retrievalQuery = ""
                + "SELECT session_key, otac_authenticated "
                + "FROM session "
                + "WHERE expiry > ? AND customer_id = ?";
        DatabaseBinding[] retrievalBindings = new DatabaseBinding[]{
                new bLong(1, System.currentTimeMillis() / 2),
                new bInteger(2, userId)
        };
        ResultSet keyResult = exec(retrievalQuery, retrievalBindings, true);

        SessionKeyPackage sessionKey = null;

        try {
            keyResult.next();
            if (!keyResult.isClosed()) {
                String key = keyResult.getString(1);
                int authenticationStage = keyResult.getInt(2);

                sessionKey = new SessionKeyPackage(key, authenticationStage);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.ofNullable(sessionKey);
    }

    public Optional<UserAuthenticationPackage> getUserData(String sessionKey) {
        String retrievalQuery = ""
                + "SELECT customer_id, otac_authenticated "
                + "FROM session "
                + "WHERE expiry > ? AND session_key = ?";
        DatabaseBinding[] retrievalBindings = new DatabaseBinding[]{
                new bLong(1, System.currentTimeMillis() / 2),
                new bString(2, sessionKey)
        };
        ResultSet idResult = exec(retrievalQuery, retrievalBindings, true);

        UserAuthenticationPackage data = null;

        try {
            idResult.next();
            if (!idResult.isClosed()) {
                int userId = idResult.getInt(1);
                int userOtac = idResult.getInt(2);

                data = new UserAuthenticationPackage(userId, userOtac);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.ofNullable(data);
    }

    public void assignSessionKey(int userId, String sessionKey,
            long expiry) {
        if (getSessionKeyData(userId) == null) {
            String insertionQuery = ""
                    + "INSERT INTO session "
                    + "(session_key, customer_id, expiry) "
                    + "VALUES (?, ?, ?)";
            DatabaseBinding[] insertionBindings = new DatabaseBinding[]{
                    new bString(1, sessionKey),
                    new bInteger(2, userId),
                    new bLong(3, expiry)};

            exec(insertionQuery, insertionBindings, false);
        } else {
            String updateQuery = ""
                    + "UPDATE session "
                    + "SET session_key = ?, expiry = ? "
                    + "WHERE customer_id = ?";
            DatabaseBinding[] updateBindings = new DatabaseBinding[]{
                    new bString(1, sessionKey),
                    new bLong(2, expiry),
                    new bInteger(3, userId)};

            exec(updateQuery, updateBindings, false);
        }
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

        String retrieveIdQuery = "SELECT last_insert_rowid()";

        TransactionContainer transaction = new TransactionContainer(
                getConnection(),
                new String[]{insertionQuery, retrieveIdQuery},
                new DatabaseBinding[][]{insertionBindings, {}},
                new boolean[]{false, true});
        ResultSet[] results = transaction.executeTransaction();
        return UncheckedFunction.<ResultSet, Integer>escapeFunction(
                x -> x.getInt(1)).apply(results[1]);
    }

    public int newAddress(String addressLine1, String addressLine2,
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

        String retrieveIdQuery = "SELECT last_insert_rowid()";

        TransactionContainer transaction = new TransactionContainer(
                getConnection(),
                new String[]{insertionQuery, retrieveIdQuery},
                new DatabaseBinding[][]{insertionBindings, {}},
                new boolean[]{false, true});

        // Return ID of newly created address entry
        ResultSet[] results = transaction.executeTransaction();
        return UncheckedFunction.<ResultSet, Integer>escapeFunction(
                x -> x.getInt(1)).apply(results[1]);
    }

    public int newSecurity(String passwordPlaintext,
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

        String retrieveIdQuery = "SELECT last_insert_rowid()";

        TransactionContainer transaction = new TransactionContainer(
                getConnection(),
                new String[]{insertionQuery, retrieveIdQuery},
                new DatabaseBinding[][]{insertionBindings, {}},
                new boolean[]{false, true});

        // Return the row ID of the newly created security entry
        ResultSet[] results = transaction.executeTransaction();
        return UncheckedFunction.<ResultSet, Integer>escapeFunction(
                x -> x.getInt(1)).apply(results[1]);
    }

    public void updatePassword(int securityId, String passwordPlaintext,
            byte[] passwordSalt, int hashIterations) {
        byte[] newPassword = SecurityService
                .getPasswordHash(passwordPlaintext, passwordSalt,
                        hashIterations);
        DatabaseBinding[] retrievalBindings = new DatabaseBinding[]{
                new bInteger(2, hashIterations),
                new bInteger(3, securityId),
                new bBytes(1, newPassword)};
        String updateQuery = ""
                + "UPDATE "
                + "SET password=?, password_hash_passes=?"
                + "WHERE security_id=?";

        exec(updateQuery, retrievalBindings, false);
    }

    public boolean verifyPassword(int userId, String passwordAttempt) {
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

        int securityId = 0;
        byte[] password = null;
        byte[] salt;
        int passes = 0;

        boolean success;

        try {
            if (rs.next()) {
                securityId = rs.getInt(1);
                password = rs.getBytes(2);
                salt = rs.getBytes(3);
                passes = rs.getInt(4);

                success = SecurityService.verifyPassword(
                        passwordAttempt, salt, password, passes);
            } else {
                success = false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Update password if necessary
        if (success && passes < HASH_PASSES) {
            updatePassword(securityId, passwordAttempt, password, HASH_PASSES);
        }

        return success;
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

        try {
            rs.next();
            if (!rs.isClosed()) {
                return Optional.of(rs.getBytes(1));
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<String> fetchPhoneNumber(int userId) {
        String retrievalQuery = ""
                + "SELECT c.phone_number "
                + "FROM customer c "
                + "WHERE c.customer_id = ?";
        DatabaseBinding[] retrievalBindings = new DatabaseBinding[]{
                new bInteger(1, userId)};
        ResultSet rs = exec(retrievalQuery, retrievalBindings, true);

        try {
            rs.next();
            if (!rs.isClosed()) {
                return Optional.of(rs.getString(1));
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
