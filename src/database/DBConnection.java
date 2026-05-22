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
    private static final String URL = "jdbc:sqlite:lan_messenger.db";
    private static final String DRIVER = "org.sqlite.JDBC";
    private static boolean schemaInitialized = false;

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("Database Driver Missing: Ensure the SQLite JDBC driver is appended to your classpath.");
            throw new SQLException(e);
        }

        Connection connection = DriverManager.getConnection(URL);
        try (Statement pragmaStatement = connection.createStatement()) {
            pragmaStatement.execute("PRAGMA foreign_keys = ON");
        }
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
            throw new SQLException("Failed to load SQLite schema.", e);
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