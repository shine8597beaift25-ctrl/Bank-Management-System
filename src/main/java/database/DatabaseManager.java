package database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Creates database connections and builds the tables on first run.
 *
 * JDBC architecture in this project:
 *
 *   Java code (repositories)
 *        |  uses the JDBC API (java.sql.*)
 *   DriverManager
 *        |  finds the right driver for "jdbc:sqlite:..."
 *   SQLite JDBC driver (org.sqlite.JDBC, from lib/sqlite-jdbc-*.jar)
 *        |
 *   data/bank_management.db (the SQLite database file)
 *
 * A NEW connection is opened for each operation and closed automatically by
 * try-with-resources. SQLite opens connections very quickly, and this keeps
 * resource handling simple and safe.
 */
public final class DatabaseManager {

    public static final String DEFAULT_DATABASE_PATH = "data/bank_management.db";
    private static final String SCHEMA_RESOURCE = "/schema.sql";
    private static final String SCHEMA_FILE_FALLBACK = "src/main/resources/schema.sql";

    /** Can be changed with -Dbank.db.path=... (the test runner uses a separate test database). */
    private static final String DATABASE_PATH = System.getProperty("bank.db.path", DEFAULT_DATABASE_PATH);

    // static block: runs once when the class is first used. Loads the JDBC driver class.
    // (Modern drivers register themselves automatically, but loading it explicitly
    //  shows the classic JDBC step and gives a clear message if the jar is missing.)
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "SQLite JDBC driver not found. Add lib/sqlite-jdbc-3.50.3.0.jar to the classpath.");
        }
    }

    private DatabaseManager() {
    }

    /**
     * Opens a new connection. The caller must close it (use try-with-resources).
     * foreign_keys=true makes SQLite enforce FOREIGN KEY rules (it is off by default in SQLite).
     */
    public static Connection getConnection() throws SQLException {
        Properties settings = new Properties();
        settings.setProperty("foreign_keys", "true");
        settings.setProperty("busy_timeout", "5000");   // wait up to 5 s if another connection is writing
        return DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH, settings);
    }

    /** Creates the data folder and all tables (only if they do not exist yet). */
    public static void initialiseDatabase() throws SQLException, IOException {
        Path databaseFile = Paths.get(DATABASE_PATH).toAbsolutePath();
        if (databaseFile.getParent() != null) {
            Files.createDirectories(databaseFile.getParent());
        }

        String schemaSql = loadSchema();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            // The schema file has several statements separated by ';'. Run them one by one.
            for (String sql : schemaSql.split(";")) {
                if (!sql.trim().isEmpty()) {
                    statement.executeUpdate(sql);   // plain Statement is fine here: no user input
                }
            }
        }
    }

    public static String getDatabasePath() {
        return DATABASE_PATH;
    }

    /**
     * Reads schema.sql from the classpath (Maven / IDE builds), or from
     * src/main/resources when the program is started from the project folder.
     * Comment lines (starting with --) are removed.
     */
    private static String loadSchema() throws IOException {
        InputStream resource = DatabaseManager.class.getResourceAsStream(SCHEMA_RESOURCE);
        if (resource == null) {
            Path fallback = Paths.get(SCHEMA_FILE_FALLBACK);
            if (!Files.exists(fallback)) {
                throw new IOException("schema.sql not found on the classpath or at " + fallback.toAbsolutePath()
                        + ". Run the program from the project folder.");
            }
            resource = Files.newInputStream(fallback);
        }

        StringBuilder sql = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
        }
        return sql.toString();
    }
}
