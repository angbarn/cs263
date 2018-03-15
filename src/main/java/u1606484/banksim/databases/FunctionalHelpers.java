package u1606484.banksim.databases;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

class FunctionalHelpers {

    // Integer -> String -> PreparedStatement
    private static final Function<Integer, Function<String,
            Consumer<PreparedStatement>>> BIND_STRING = (i) -> (v) ->
            UncheckedConsumer
                    .escapeConsumer((s) -> s.setString(i, v));
    // Integer -> Integer -> PreparedStatement
    private static final Function<Integer, Function<Integer,
            Consumer<PreparedStatement>>> BIND_INTEGER = (i) -> (v) ->
            UncheckedConsumer
                    .escapeConsumer((s) -> s.setInt(i, v));
    // Integer -> byte[] -> PreparedStatement
    private static final Function<Integer, Function<byte[],
            Consumer<PreparedStatement>>> BIND_BYTES = (i) -> (v) ->
            UncheckedConsumer
                    .escapeConsumer((s) -> s.setBytes(i, v));
    // Integer -> Long -> PreparedStatement
    private static final Function<Integer, Function<Long,
            Consumer<PreparedStatement>>> BIND_LONG = (i) -> (v) ->
            UncheckedConsumer
                    .escapeConsumer((s) -> s.setLong(i, v));

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
     * A bi-consumer whose accept method is marked as throwing a {@link
     * SQLException}, allowing lambda functions using SQL methods to be
     * written.
     *
     * @param <T> The first type that the bi-consumer will accept
     * @param <U> The second type that the bi-consumer will accept
     */
    @FunctionalInterface
    interface UncheckedBiConsumer<T, U> {

        static <T, U> BiConsumer<T, U> escapeBiConsumer(
                UncheckedBiConsumer<T, U> c) {
            return (t, u) -> {
                try {
                    c.accept(t, u);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            };
        }

        void accept(T t, U u) throws SQLException;
    }

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

        private final int index;
        private final T value;
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

        void performBinding(PreparedStatement s) {
            binder.apply(index).apply(value).accept(s);
        }
    }

    static class bString extends DatabaseBinding<String> {

        bString(int index, String value) {
            super(index, value, BIND_STRING);
        }
    }

    static class bInteger extends DatabaseBinding<Integer> {

        bInteger(int index, Integer value) {
            super(index, value, BIND_INTEGER);
        }
    }

    static class bBytes extends DatabaseBinding<byte[]> {

        bBytes(int index, byte[] value) {
            super(index, value, BIND_BYTES);
        }
    }

    static class bLong extends DatabaseBinding<Long> {

        bLong(int index, Long value) {
            super(index, value, BIND_LONG);
        }
    }
}