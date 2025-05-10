import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException; /**
 * فئة مفردة لإدارة اتصالات قاعدة البيانات
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private static final String DB_URL = "jdbc:sqlite:investment_simulator.db";

    private DatabaseManager() {
        // مُنشئ خاص لفرض نمط المفرد
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
