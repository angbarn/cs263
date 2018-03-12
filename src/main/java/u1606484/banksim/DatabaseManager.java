package u1606484.banksim;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import u1606484.banksim.DatabaseManager.FunctionalHelpers.DatabaseBinding;
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
		DatabaseManager m = null;
		try {
			m = new DatabaseManager();
			m.newSecurity("bananarama", 10);

			String q;

			q = "INSERT INTO security (login_salt, support_in_salt, "
					+ "support_out_salt, password, password_salt, "
					+ "password_hash_passes) VALUES (?, ?, ?, ?, ?, ?)";

			q = "SELECT * FROM customer WHERE customer_id = ?";
			DatabaseBinding[] bindings = new DatabaseBinding[]{
					new FunctionalHelpers.bBytes(1, new byte[]{})
			};

			ResultSet r = m.query(q, null, id, null);
			while (r.next()) {
				System.out.println(r.getInt(0) + ": " + r.getString(1) + " "
						+ r
						.getString(2));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean newSecurity(String passwordPlaintext,
			int passwordHashPasses) {
		byte[] loginSalt = OtacGenerator.getSalt();
		byte[] supportInSalt = OtacGenerator.getSalt();
		byte[] supportOutSalt = OtacGenerator.getSalt();
		byte[] passwordSalt = OtacGenerator.getSalt();

		// Concatenate plaintext and password salt
		byte[] plaintextBytes = passwordPlaintext.getBytes();
		byte[] passwordHash = new byte[plaintextBytes.length
				+ passwordSalt.length];
		System.arraycopy(plaintextBytes, 0, passwordHash, 0,
				plaintextBytes.length);
		System.arraycopy(passwordSalt, 0, passwordHash, plaintextBytes.length,
				passwordSalt.length);

		passwordHash = OtacGenerator.getHash(passwordHash, passwordHashPasses);

		DatabaseBinding[] bindings = new DatabaseBinding[]{
				new bBytes(1, loginSalt),
				new bBytes(2, supportInSalt),
				new bBytes(3, supportOutSalt),
				new bBytes(4, passwordHash),
				new bBytes(5, passwordSalt),
				new bInteger(6, passwordHashPasses)
		};
		String q = "INSERT INTO security (login_salt, support_in_salt, "
				+ "support_out_salt, password, password_salt, "
				+ "password_hash_passes) VALUES (?, ?, ?, ?, ?, ?)";

		return (exec(q, bindings) == null);
	}

	public ResultSet[] transaction(TransactionContainer transactionData) {
		try {
			// Start a transaction
			conn.setAutoCommit(false);

			transactionData.
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public ResultSet exec(String query, DatabaseBinding[] bindings) {

		try {
			conn.setAutoCommit(true);
			PreparedStatement runQuery = conn.prepareStatement(query,
					Statement.RETURN_GENERATED_KEYS);
			Arrays.stream(bindings).forEach(b -> b.performBinding(runQuery));

			return runQuery.executeQuery();

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
		public interface UncheckedConsumer<T> {

			/**
			 * Attempts to run a consumer, preparing to catch for {@link
			 * SQLException} errors.
			 *
			 * @param <T> The type that the consumer will accept
			 * @param c The UncheckedConsumer to escape
			 * @return The result of executing c
			 * @throws RuntimeException When the UncheckedConsumer would have
			 * thrown a SQLException.
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

	public class TransactionContainer {

		private final Queue<Entry<String, DatabaseBinding>> queries;

		public TransactionContainer(String[] queryStrings,
				DatabaseBinding[] bindings) throws IllegalArgumentException {
			if (queryStrings.length != bindings.length) {
				throw new IllegalArgumentException(
						"Query and binding length do not match");
			}
			queries = new LinkedList<>();

			for (int i = 0; i < queryStrings.length; i++) {
				String s = queryStrings[i];
				DatabaseBinding b = bindings[i];
				queries.add(new SimpleImmutableEntry<>(s, b));
			}
		}

		public ResultSet[] executeTransaction() {
			try {
				conn.setAutoCommit(false);

				queries.stream().map()
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
