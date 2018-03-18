package u1606484.banksim.databases;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Effectively a container to separate a lot of slightly verbose functional
 * programming code from the rest of the program.
 *
 * <p>The contents generally allow for the creation of more generic
 * "super-methods" to operate on the database.
 */
class FunctionalHelpers {

    /**
     * Binds a string to a prepared statement
     *
     * <p>{@code Integer -> String -> PreparedStatement}
     */
    private static final Function<Integer, Function<String,
            Consumer<PreparedStatement>>> BIND_STRING = (i) -> (v) ->
            UncheckedConsumer
                    .escapeConsumer((s) -> s.setString(i, v));
    /**
     * Binds an integer to a prepared statement
     *
     * <p>{@code Integer -> Integer -> PreparedStatement}
     */
    private static final Function<Integer, Function<Integer,
            Consumer<PreparedStatement>>> BIND_INTEGER = (i) -> (v) ->
            UncheckedConsumer
                    .escapeConsumer((s) -> s.setInt(i, v));
    // Integer -> byte[] -> PreparedStatement
    /**
     * Binds a byte array to a prepared statement
     *
     * <p>{@code Integer -> byte[] -> PreparedStatement}
     */
    private static final Function<Integer, Function<byte[],
            Consumer<PreparedStatement>>> BIND_BYTES = (i) -> (v) ->
            UncheckedConsumer
                    .escapeConsumer((s) -> s.setBytes(i, v));
    /**
     * Binds a long to a prepared statement
     *
     * <p>{@code Integer -> Long -> PreparedStatement}
     */
    private static final Function<Integer, Function<Long,
            Consumer<PreparedStatement>>> BIND_LONG = (i) -> (v) ->
            UncheckedConsumer
                    .escapeConsumer((s) -> s.setLong(i, v));

    /**
     * Attempts to retrieve a single record of data from a {@link ResultSet}.
     * This operation accounts for the majority of all reading from the
     * database.
     *
     * <p>A handler function is applied to the first row of the database in
     * order to control what sort of data this generic method retrieves.
     *
     * @param rs The result set to retrieve from
     * @param handler The handler function to apply to the first row
     * @param <T> The type of data the returned optional contains
     * @return An optional containing the returned data, or if no rows were
     * returned by the query, an empty optional.
     */
    static <T> Optional<T> attemptSingleRetrieval(ResultSet rs,
            UncheckedFunction<ResultSet, T> handler) {
        try {
            if (rs.next()) {
                Optional<T> o = Optional.of(handler.apply(rs));
                rs.close();
                return o;
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A consumer whose accept method is marked as throwing a {@link
     * SQLException}, allowing lambda functions using SQL methods to be
     * written.
     *
     * @param <T> The type that the consumer will accept
     */
    @FunctionalInterface
    interface UncheckedConsumer<T> {

        /**
         * Attempts to run a consumer, preparing to catch for {@link
         * SQLException} errors
         *
         * @param <T> The type that the consumer will accept
         * @param c The UncheckedConsumer to escape
         * @return The result of executing c
         * @throws RuntimeException When the UncheckedConsumer would have thrown
         * a SQLException
         */
        static <T> Consumer<T> escapeConsumer(UncheckedConsumer<T> c) {
            return t -> {
                try {
                    c.accept(t);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            };
        }

        void accept(T t) throws SQLException;
    }

    /**
     * A function whose {@link Function#apply(Object)} method is marked as
     * throwing a {@link SQLException}, allowing lambda functions using SQL
     * methods to be written.
     *
     * @param <T> The type of data the function takes
     * @param <R> The type of data the function returns
     */
    @FunctionalInterface
    interface UncheckedFunction<T, R> {

        /**
         * Attempts to run a function, preparing to catch for {@link
         * SQLException} errors
         *
         * @param <T> The type that the function will accept
         * @param <R> The type that the function will return
         * @return Whatever f returns when a value would normally be applied
         * @throws RuntimeException When the UncheckedFunction would have thrown
         * a SQLException
         */
        static <T, R> Function<T, R> escapeFunction(UncheckedFunction<T,
                R> f) {
            return t -> {
                try {
                    return f.apply(t);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            };
        }

        R apply(T t) throws SQLException;
    }

    /**
     * Contains an index, value and binding function, containing all parameters
     * to bind a single value to a {@link PreparedStatement}. This allows an
     * array or {@link Collection} of objects to be given to a method to bind
     * all of them to a statement.
     */
    abstract static class DatabaseBinding<T> {

        /**
         * Bind position in the prepared statement
         */
        private final int index;
        /**
         * The value to bind to the statement
         */
        private final T value;
        /**
         * The "handler function" to manage the binding of the value to the
         * prepared statement.
         */
        private final Function<Integer, Function<T,
                Consumer<PreparedStatement>>>
                binder;

        private DatabaseBinding(int index, T value,
                Function<Integer, Function<T, Consumer<PreparedStatement>>>
                        binder) {
            this.index = index;
            this.value = value;
            this.binder = binder;
        }

        /**
         * Binds the provided value to the query, at provided position using
         * provided binding function.
         * @param s The PreparedStatement to bind to
         */
        void performBinding(PreparedStatement s) {
            binder.apply(index).apply(value).accept(s);
        }
    }

    /**
     * A version of {@link DatabaseBinding} specified to use only strings,
     * yielding more consise code
     */
    static class bString extends DatabaseBinding<String> {

        bString(int index, String value) {
            super(index, value, BIND_STRING);
        }
    }


    /**
     * A version of {@link DatabaseBinding} specified to use only integers, yielding more consise code
     */
    static class bInteger extends DatabaseBinding<Integer> {

        bInteger(int index, Integer value) {
            super(index, value, BIND_INTEGER);
        }
    }


    /**
     * A version of {@link DatabaseBinding} specified to use only byte
     * arrays, yielding more consise code
     */
    static class bBytes extends DatabaseBinding<byte[]> {

        bBytes(int index, byte[] value) {
            super(index, value, BIND_BYTES);
        }
    }


    /**
     * A version of {@link DatabaseBinding} specified to use only longs, yielding more consise code
     */
    static class bLong extends DatabaseBinding<Long> {

        bLong(int index, Long value) {
            super(index, value, BIND_LONG);
        }
    }
}