package org.example;

import java.rmi.*;
import java.rmi.server.*;
import java.sql.*;
import java.util.*;
import java.io.*;

public class Barrel extends UnicastRemoteObject implements IBarrel {
    private Connection connection;

    // Construtor do BarrelService que estabelece a conexão com o banco de dados
    public Barrel() throws RemoteException {
        super();
        try {
            // Carregar as propriedades do arquivo config.properties
            Properties config = new Properties();
            try (FileInputStream inputStream = new FileInputStream("config.properties")) {
                config.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RemoteException("Erro ao carregar o arquivo de configuração", e);
            }

            // Obter os detalhes de conexão do banco de dados
            String dbUrl = config.getProperty("db.url");
            String dbUser = config.getProperty("db.user");
            String dbPassword = config.getProperty("db.password");

            // Estabelecer a conexão com o banco de dados PostgreSQL
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao conectar com o banco de dados", e);
        }
    }

    @Override
    public void addToIndex(String word, String url) throws RemoteException {
        try {
            // Verificar se a palavra já está no índice e adicionar o URL
            String sql = "INSERT INTO word_index (word, url) VALUES (?, ?) ON CONFLICT (word, url) DO NOTHING";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, word);
                stmt.setString(2, url);
                stmt.executeUpdate();
                System.out.println("Índice atualizado com a palavra: " + word + " e URL: " + url);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao adicionar ao índice", e);
        }
    }

    @Override
    public List<String> search(String word) throws RemoteException {
        List<String> urls = new ArrayList<>();
        try {
            // Procurar URLs associados à palavra
            String sql = "SELECT url FROM word_index WHERE word = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, word);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    urls.add(rs.getString("url"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao pesquisar no índice", e);
        }
        return urls;
    }




    // Método principal para iniciar o serviço RMI
    public static void main(String[] args) {
        try {
            // Cria uma instância do BarrelService
            Barrel barrelService = new Barrel();

            // Registra o BarrelService no RMI Registry
            Naming.rebind("rmi://localhost:1100/BarrelService1", barrelService);
            System.out.println("BarrelService registrado com sucesso no RMI Registry.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

