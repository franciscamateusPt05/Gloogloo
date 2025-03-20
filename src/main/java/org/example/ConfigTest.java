package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConfigTest {
    public static void main(String[] args) {
        Properties config = new Properties();

        // Tenta carregar o ficheiro config.properties
        try (FileInputStream inputStream = new FileInputStream("/Users/francisca_mateus/Desktop/Universidade/2º Ano/2º Semestre/SD/Projeto/SD_EXP/src/main/java/org/example/config.properties")) {
            config.load(inputStream);
            System.out.println("✅ Configurações carregadas com sucesso.");
        } catch (IOException e) {
            System.err.println("❌ Erro ao carregar o arquivo de configuração:");
            e.printStackTrace();
            return;
        }

        // Testar conexão para cada barrel
        for (int i = 1; i <= 2; i++) {
            String barrelName = "barrel" + i;
            String dbUrl = config.getProperty(barrelName + ".db.url");
            String dbUser = config.getProperty(barrelName + ".db.user");
            String dbPassword = config.getProperty(barrelName + ".db.password");

            // Verifica se alguma propriedade está nula
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                System.err.println("❌ Erro: Algumas configurações da base de dados " + barrelName + " são nulas!");
                continue;
            }

            System.out.println("\n🔄 Testando conexão com " + barrelName + "...");
            System.out.println("DB URL: " + dbUrl);
            System.out.println("DB User: " + dbUser);

            // Testar conexão
            try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                if (connection != null) {
                    System.out.println("✅ Conexão bem-sucedida com " + barrelName);
                } else {
                    System.err.println("❌ Falha ao conectar com " + barrelName);
                }
            } catch (SQLException e) {
                System.err.println("❌ Erro ao conectar com " + barrelName + ":");
                e.printStackTrace();
            }
        }
    }
}
