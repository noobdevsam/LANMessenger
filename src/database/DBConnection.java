package database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static final String SQLITE_URL = "jdbc:sqlite:lan_messenger.db";
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static boolean schemaInitialized = false;

    public static Connection getConnection() throws SQLException {
        // If environment variable LANM_DB_URL is set, use it as MySQL connection
        String envUrl = System.getenv("LANM_DB_URL");
        if (envUrl != null && !envUrl.isEmpty()) {
            String user = System.getenv("LANM_DB_USER");
            String pass = System.getenv("LANM_DB_PASS");
            try {
                Class.forName(MYSQL_DRIVER);
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL Driver Missing: add mysql-connector-java to classpath.");
                throw new SQLException(e);
            }
            Connection conn = DriverManager.getConnection(envUrl, user, pass);
            initializeSchema(conn);
            return conn;
        }

        // Fallback to SQLite if no env provided
        try {
            Class.forName(SQLITE_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite Driver Missing: Ensure the SQLite JDBC driver is appended to your classpath.");
            throw new SQLException(e);
        }

        Connection connection = DriverManager.getConnection(SQLITE_URL);
        initializeSchema(connection);
        return connection;
    }

    private static synchronized void initializeSchema(Connection connection) throws SQLException {
        if (schemaInitialized) {
            return;
        }

        try (Statement statement = connection.createStatement();
             InputStream schemaStream = DBConnection.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (schemaStream == null) {
                throw new SQLException("Schema resource schema.sql was not found on the classpath.");
            }

            for (String sql : readSchemaStatements(schemaStream)) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty()) {
                    statement.execute(trimmedSql);
                }
            }
        } catch (IOException e) {
            throw new SQLException("Failed to load schema.", e);
        }

        schemaInitialized = true;
    }

    private static String[] readSchemaStatements(InputStream schemaStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(schemaStream, StandardCharsets.UTF_8))) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append('\n');
            }
            return sql.toString().split(";");
        }
    }
}