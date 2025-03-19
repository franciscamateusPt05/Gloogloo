package org.example;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class DatabaseManager {
    private static final List<String> BARRELS = Arrays.asList(
            "jdbc:postgresql://localhost:5432/barrel1",
            "jdbc:postgresql://localhost:5432/barrel2"
    );

    private static final String USER = "postgres";
    private static final String PASSWORD = "2024";

    private static void executeOnBothBarrels(String query, String... params) {
        for (String barrel : BARRELS) {
            try (Connection conn = DriverManager.getConnection(barrel, USER, PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                for (int i = 0; i < params.length; i++) {
                    stmt.setString(i + 1, params[i]);
                }

                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addURL(String url, String titulo, String citacao) {
        executeOnBothBarrels("INSERT INTO urls (url, titulo, citacao) VALUES (?, ?, ?) ON CONFLICT (url) DO NOTHING",
                url, titulo, citacao);
    }

    public static void addWord(String word) {
        executeOnBothBarrels("INSERT INTO word (word) VALUES (?) ON CONFLICT (word) DO NOTHING",
                word);
    }

    public static void addWordURL(String word, String url) {
        executeOnBothBarrels("INSERT INTO word_url (word, url) VALUES (?, ?) ON CONFLICT (word, url) DO NOTHING",
                word, url);
    }

    public static void addLink(String fromURL, String toURL) {
        String sql = "INSERT INTO url_links (from_url, to_url) VALUES (?, ?) " +
                "ON CONFLICT (from_url, to_url) DO NOTHING";

        executeOnBothBarrels(sql, fromURL, toURL);
        System.out.println("Ligação adicionada: " + fromURL + " -> " + toURL);
    }
}
