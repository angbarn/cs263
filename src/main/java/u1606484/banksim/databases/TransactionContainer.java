package u1606484.banksim.databases;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import u1606484.banksim.databases.FunctionalHelpers.DatabaseBinding;
import u1606484.banksim.databases.FunctionalHelpers.UncheckedConsumer;

class TransactionContainer {

    private final Connection conn;
    private final String[] queryStrings;
    private final DatabaseBinding[][] bindings;
    private final boolean[] resultsRequiredFlags;
    private final int size;


    TransactionContainer(Connection conn, String[] queryStrings,
            DatabaseBinding[][] bindings, boolean[] resultsRequired)
            throws IllegalArgumentException {
        // Error if the number of sets of bindings doesn't equal the
        // number of queries to ask
        if (queryStrings.length != bindings.length
                || queryStrings.length != resultsRequired.length) {
            throw new IllegalArgumentException(
                    "Query and binding length do not match");
        }

        this.conn = conn;
        this.queryStrings = queryStrings.clone();
        this.bindings = bindings.clone();
        this.resultsRequiredFlags = resultsRequired.clone();

        this.size = queryStrings.length;
    }

    void close(ResultSet[] rs) {
        Arrays.stream(rs)
                .forEach(UncheckedConsumer.escapeConsumer(ResultSet::close));

        try {
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Binds an array of {@link DatabaseBinding} to a {@link PreparedStatement}
     * created from given {@code queryString}.
     *
     * @param queryString The string for the query
     * @param bindings The bindings for the query
     * @return The result of the single query
     */
    private ResultSet executeSingle(String queryString,
            DatabaseBinding[] bindings, boolean results) {
        try {
            PreparedStatement statement = conn.prepareStatement(queryString);

            Arrays.stream(bindings)
                    .forEach(x -> x.performBinding(statement));

            // Behave differently if results are required or not
            if (results) {
                return statement.executeQuery();
            } else {
                statement.executeUpdate();
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes queries on the database in a single transaction, then returns
     * the result.
     *
     * @return Result of running queries on the database.
     */
    ResultSet[] executeTransaction() {
        try {
            ResultSet[] results = new ResultSet[size];
            for (int i = 0; i < size; i++) {
                String queryString = queryStrings[i];
                DatabaseBinding[] bindingSet = bindings[i];
                boolean resultsRequired = resultsRequiredFlags[i];

                results[i] = executeSingle(queryString, bindingSet,
                        resultsRequired);
            }

            conn.commit();

            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}