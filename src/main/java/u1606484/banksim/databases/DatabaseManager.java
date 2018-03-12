package u1606484.banksim.databases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import u1606484.banksim.databases.FunctionalHelpers.DatabaseBinding;

class DatabaseManager {

	/**
	 * Location of the database file on disk.
	 */
	private static final String DATABASE_URL = "jdbc:sqlite:C:/Users/angus"
			+ "/Documents/year2_local/cs263/cs263/src/main/sql/accounts.db";
	private final Connection conn;

	DatabaseManager() {
		try {
			conn = DriverManager.getConnection(DATABASE_URL);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	Connection getConnection() {
		return conn;
	}

	ResultSet exec(String query, DatabaseBinding[] bindings,
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
}
