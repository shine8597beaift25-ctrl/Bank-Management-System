package database;

import java.io.BufferedReader;
import java.io.FileInputStream;
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
 * Creates MySQL connections and builds the database and tables on first run.
 *
 * JDBC architecture in this project:
 *
 *   Java code (repositories)
 *        |  uses the JDBC API (java.sql.*)
 *   DriverManager
 *        |  finds the right driver for "jdbc:mysql://..."
 *   MySQL Connector/J driver (com.mysql.cj.jdbc.Driver, mysql-connector-j-*.jar)
 *        |  talks to the server over the network (port 3306)
 *   MySQL server  ->  database "bank_management"
 *
 * Connection settings (host, port, database, user, password) are read from
 * db.properties in the project folder. Any of them can also be given on the
 * command line, e.g. -Ddb.name=bank_management_test (used by the test runner).
 *
 * A NEW connection is opened for each operation and closed automatically by
 * try-with-resources. This keeps resource handling simple and safe for a
 * small desktop application.
 */
public final class DatabaseManager {

    public static final String SETTINGS_FILE = "db.properties";
    private static final String MYSQL_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String SCHEMA_RESOURCE = "/schema.sql";
    private static final String SCHEMA_FILE_FALLBACK = "src/main/resources/schema.sql";

    /** Database names cannot be sent as "?" parameters, so we only allow safe characters. */
    private static final String SAFE_NAME_PATTERN = "[A-Za-z0-9_]+";

    private static final Properties SETTINGS = loadSettings();

    // static block: runs once when the class is first used.
    // Since JDBC 4.0, drivers on the classpath register themselves automatically,
    // so this step is optional. We still try it to show the classic JDBC step.
    static {
        try {
            Class.forName(MYSQL_DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            // Not fatal here: getConnection() gives a clear message if no driver is found at all.
        }
    }

    private DatabaseManager() {
    }

    // =====================================================================
    // Connections
    // =====================================================================

    /** Opens a new connection to the bank database. The caller must close it (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return connect(buildUrl(getDatabaseName()));
    }

    /**
     * Opens a connection to the MySQL SERVER without choosing a database.
     * Used to create the database the first time, and by the test runner.
     */
    public static Connection getServerConnection() throws SQLException {
        return connect(buildUrl(""));
    }

    /** Creates the database (if missing) and all tables (if missing). */
    public static void initialiseDatabase() throws SQLException, IOException {
        try (Connection server = getServerConnection();
             Statement statement = server.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + getDatabaseName()
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        String schemaSql = loadSchema();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            // schema.sql has several statements separated by ';'. Run them one by one.
            for (String sql : schemaSql.split(";")) {
                if (!sql.trim().isEmpty()) {
                    statement.executeUpdate(sql);   // plain Statement is fine here: no user input
                }
            }
        }
    }

    // =====================================================================
    // Settings
    // =====================================================================

    public static String getDatabaseName() {
        String name = setting("db.name", "bank_management");
        if (!name.matches(SAFE_NAME_PATTERN)) {
            throw new IllegalStateException("Invalid db.name '" + name + "': use only letters, digits and _.");
        }
        return name;
    }

    /** Short text for the start-up message, e.g. "MySQL database bank_management on localhost:3306 (user root)". */
    public static String getDatabaseDescription() {
        return "MySQL database " + getDatabaseName() + " on " + setting("db.host", "localhost") + ":"
                + setting("db.port", "3306") + " (user " + setting("db.user", "root") + ")";
    }

    /** jdbc:mysql://localhost:3306/bank_management?useSSL=false&allowPublicKeyRetrieval=true */
    private static String buildUrl(String databaseName) {
        String options = setting("db.options", "useSSL=false&allowPublicKeyRetrieval=true");
        return "jdbc:mysql://" + setting("db.host", "localhost") + ":" + setting("db.port", "3306") + "/"
                + databaseName + (options.isEmpty() ? "" : "?" + options);
    }

    private static Connection connect(String url) throws SQLException {
        try {
            return DriverManager.getConnection(url, setting("db.user", "root"), setting("db.password", ""));
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("No suitable driver")) {
                throw new SQLException("MySQL JDBC driver not found. Put mysql-connector-j-9.4.0.jar in the lib "
                        + "folder (see README) or run the project with Maven.", e);
            }
            throw e;
        }
    }

    /** A command-line value (-Ddb.name=...) wins over db.properties; otherwise the default is used. */
    private static String setting(String key, String defaultValue) {
        String fromCommandLine = System.getProperty(key);
        if (fromCommandLine != null) {
            return fromCommandLine.trim();
        }
        return SETTINGS.getProperty(key, defaultValue).trim();
    }

    /** Reads db.properties with a byte stream (FileInputStream). Missing file = use defaults. */
    private static Properties loadSettings() {
        Properties properties = new Properties();
        Path file = Paths.get(SETTINGS_FILE);
        if (Files.exists(file)) {
            try (InputStream input = new FileInputStream(file.toFile())) {
                properties.load(input);
            } catch (IOException e) {
                System.out.println("[WARNING] Could not read " + SETTINGS_FILE + ": " + e.getMessage()
                        + ". Using default settings.");
            }
        }
        return properties;
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
