package org.example;

import java.sql.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.sql.DriverManager;
import java.sql.SQLException;

import static java.sql.DriverManager.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        // Configuração da base de dados
        String url = "jdbc:postgresql://localhost:5432/postgres"; // Muda 'nomedabase' para o nome correto
        String user = "postgres";  // Muda para o teu utilizador
        String password = "2024";   // Muda para a tua senha

        // Conectar à base de dados
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            if (conn != null) {
                System.out.println("Conexão com PostgreSQL estabelecida com sucesso!");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao conectar à base de dados:");
            e.printStackTrace();
        }
        }
    }
