package u1606484.banksim.databases;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import u1606484.banksim.databases.FunctionalHelpers.DatabaseBinding;
import u1606484.banksim.databases.FunctionalHelpers.UncheckedConsumer;

/**
 * Some operations run on the database must be run in a single transaction to
 * prevent concurrency issues. This class assists in doing just this, taking an
 * array of queries, and an array of arrays of associated bindings for said
 * queries. Finally, the class requires a connection, and an array of "required"
 * flags, which mark a query as DML ({@code false}) or not ({@code true}).
 */
class TransactionContainer {

    /**
     * Connection to the database
     */
    private final Connection conn;
    /**
     * Array of queries - these must be in the same order as {@code bindings}
     */
    private final String[] queryStrings;
    /**
     * Array of arrays of bindings - these must be in the same order as {@code
     * queryStrings}
     */
    private final DatabaseBinding[][] bindings;
    /**
     * Array of booleans - these must be in the same order as {@code bindings}
     * and {@code queryStrings}
     */
    private final boolean[] resultsRequiredFlags;
    /**
     * The number of queries, which should match the length of the two other
     * arrays also.
     */
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

    /**
     * Closes {@link ResultSet}s from the un-filtered list given by this class
     * upon execution. Also commits the {@link Connection}.
     *
     * @param rs The List of ResultSet Optionals to close
     */
    void close(List<Optional<ResultSet>> rs) {
        rs.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
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
     * @param results If the query is a DML query, this should be false.
     * Otherwise true.
     * @return If the result is required (not DML), an optional containing a
     * ResultSet for the query. Otherwise, an empty optional.
     */
    private Optional<ResultSet> executeSingle(String queryString,
            DatabaseBinding[] bindings, boolean results) {
        try {
            PreparedStatement statement = conn.prepareStatement(queryString);

            Arrays.stream(bindings)
                    .forEach(x -> x.performBinding(statement));

            // Behave differently if results are required or not
            if (results) {
                return Optional.of(statement.executeQuery());
            } else {
                statement.executeUpdate();
                return Optional.empty();
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
    List<Optional<ResultSet>> executeTransaction() {
        try {
            List<Optional<ResultSet>> results = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String queryString = queryStrings[i];
                DatabaseBinding[] bindingSet = bindings[i];
                boolean resultsRequired = resultsRequiredFlags[i];

                results.add(executeSingle(queryString, bindingSet,
                        resultsRequired));
            }

            conn.commit();

            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}