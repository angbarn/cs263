package u1606484.banksim;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import u1606484.banksim.DatabaseManager.FunctionalHelpers.DatabaseBinding;
import u1606484.banksim.DatabaseManager.FunctionalHelpers.UncheckedFunction;
import u1606484.banksim.DatabaseManager.FunctionalHelpers.bBytes;
import u1606484.banksim.DatabaseManager.FunctionalHelpers.bInteger;

public class DatabaseManager {

	/**
	 * Location of the database file on disk.
	 */
	private static final String DATABASE_URL = "jdbc:sqlite:C:/Users/angus"
			+ "/Documents/year2_local/cs263/cs263/src/main/sql/accounts.db";
	private final Connection conn;

	public DatabaseManager() {
		try {
			conn = DriverManager.getConnection(DATABASE_URL);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] arguments) {
		DatabaseManager m;
		int iterationsUsed = 1;

		try {
			m = new DatabaseManager();
			int insertionIndex = m.newSecurity("banana", iterationsUsed);
			System.out.println(insertionIndex);

			String q = "SELECT password, password_salt, password_hash_passes "
					+ "FROM security WHERE security_id = " + insertionIndex;
			ResultSet s = m.exec(q, new DatabaseBinding[]{}, true);
			byte[] passwordHash = s.getBytes(1);
			byte[] passwordSalt = s.getBytes(2);
			int readIterations = s.getInt(3);

			System.out.println(Arrays.toString(OtacGenerator
					.getPasswordHash("banana", passwordSalt, readIterations)));

			System.out.println(Arrays.toString(passwordHash));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int newSecurity(String passwordPlaintext,
			int passwordHashPasses) {
		byte[] loginSalt = OtacGenerator.getSalt();
		byte[] supportInSalt = OtacGenerator.getSalt();
		byte[] supportOutSalt = OtacGenerator.getSalt();
		byte[] passwordSalt = OtacGenerator.getSalt();
		byte[] passwordHash = OtacGenerator
				.getPasswordHash(passwordPlaintext, passwordSalt,
						passwordHashPasses);

		DatabaseBinding[] insertionBindings = new DatabaseBinding[]{
				new bBytes(1, loginSalt),
				new bBytes(2, supportInSalt),
				new bBytes(3, supportOutSalt),
				new bBytes(4, passwordHash),
				new bBytes(5, passwordSalt),
				new bInteger(6, passwordHashPasses)
		};
		String insertionQuery = "INSERT INTO security (login_salt, "
				+ "support_in_salt, support_out_salt, password, password_salt,"
				+ " password_hash_passes) VALUES (?, ?, ?, ?, ?, ?)";
		String retrieveIdQuery = "SELECT last_insert_rowid()";

		TransactionContainer transaction = new TransactionContainer(
				conn,
				new String[]{insertionQuery, retrieveIdQuery},
				new DatabaseBinding[][]{insertionBindings, {}},
				new boolean[]{false, true});

		// Return the row ID of the newly created security entry
		ResultSet[] results = transaction.executeTransaction();
		return UncheckedFunction.<ResultSet, Integer>escapeFunction(
				x -> x.getInt(1)).apply(results[1]);
	}

	public ResultSet exec(String query, DatabaseBinding[] bindings,
			boolean resultsRequired) {

		try {
			// Don't want a transaction
			conn.setAutoCommit(true);
			PreparedStatement runQuery = conn.prepareStatement(query,
					Statement.RETURN_GENERATED_KEYS);
			Arrays.stream(bindings).forEach(b -> b.performBinding(runQuery));

			if (resultsRequired) {
				return runQuery.executeQuery();
			} else {
				runQuery.executeUpdate();
				return null;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static class FunctionalHelpers {

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

		/**
		 * A consumer whose accept method is marked as throwing a {@link
		 * SQLException}, allowing lambda functions using SQL methods to be
		 * written.
		 *
		 * @param <T> The type that the consumer will accept
		 */
		@FunctionalInterface
		public interface UncheckedConsumer<T> {

			/**
			 * Attempts to run a consumer, preparing to catch for {@link
			 * SQLException} errors
			 *
			 * @param <T> The type that the consumer will accept
			 * @param c The UncheckedConsumer to escape
			 * @return The result of executing c
			 * @throws RuntimeException When the UncheckedConsumer would have
			 * thrown a SQLException
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
		public interface UncheckedBiConsumer<T, U> {

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
		public interface UncheckedFunction<T, R> {

			/**
			 * Attempts to run a function, preparing to catch for {@link
			 * SQLException} errors
			 *
			 * @param <T> The type that the function will accept
			 * @param <R> The type that the function will return
			 * @return Whatever f returns when a value would normally be
			 * applied
			 * @throws RuntimeException When the UncheckedFunction would have
			 * thrown a SQLException
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
		 * Contains an index, value and binding function, containing all
		 * parameters to bind a single value to a {@link PreparedStatement}.
		 * This allows an array or {@link Collection} of objects to be given to
		 * a method to bind all of them to a statement.
		 */
		public abstract static class DatabaseBinding<T> {

			private int index;
			private T value;
			private Function<Integer, Function<T, Consumer<PreparedStatement>>>
					binder;

			public DatabaseBinding(int index, T value,
					Function<Integer, Function<T, Consumer<PreparedStatement>>>
							binder) {
				this.index = index;
				this.value = value;
				this.binder = binder;
			}

			public void performBinding(PreparedStatement s) {
				binder.apply(index).apply(value).accept(s);
			}
		}

		public static class bString extends DatabaseBinding<String> {

			public bString(int index, String value) {
				super(index, value, BIND_STRING);
			}
		}

		public static class bInteger extends DatabaseBinding<Integer> {

			public bInteger(int index, Integer value) {
				super(index, value, BIND_INTEGER);
			}
		}

		public static class bBytes extends DatabaseBinding<byte[]> {

			public bBytes(int index, byte[] value) {
				super(index, value, BIND_BYTES);
			}
		}
	}

	public static class TransactionContainer {

		private final Connection conn;
		private final String[] queryStrings;
		private final DatabaseBinding[][] bindings;
		private final boolean[] resultsRequiredFlags;
		private final int size;


		public TransactionContainer(Connection conn, String[] queryStrings,
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
		 * Binds an array of {@link DatabaseBinding} to a {@link
		 * PreparedStatement} created from given {@code queryString}.
		 *
		 * @param queryString The string for the query
		 * @param bindings The bindings for the query
		 * @return The result of the single query
		 */
		private ResultSet executeSingle(String queryString,
				DatabaseBinding[] bindings, boolean results) {
			try {
				PreparedStatement statement = conn
						.prepareStatement(queryString);

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
		 * Executes queries on the database in a single transaction, then
		 * returns the result.
		 *
		 * @return Result of running queries on the database.
		 */
		public ResultSet[] executeTransaction() {
			try {
				conn.setAutoCommit(false);

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
}
