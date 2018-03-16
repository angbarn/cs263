package u1606484.banksim.databases;

import java.sql.SQLException;

public class TemporaryDatabase extends ApplicationDatabaseManager {

    public void close() {
        try {
            this.getConnection().close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}