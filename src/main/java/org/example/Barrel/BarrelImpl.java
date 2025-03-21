package org.example.Barrel;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BarrelImpl extends UnicastRemoteObject implements IBarrel {
    private static final Logger logger = Logger.getLogger(BarrelImpl.class.getName());
    private Connection conn;

    public BarrelImpl(String barrelName) throws RemoteException {
        super();

        Properties properties = new Properties();

        // Carregar propriedades do arquivo
        try (FileInputStream fis = new FileInputStream("src/main/java/org/example/Properties/barrel.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao carregar o arquivo de propriedades: " + e.getMessage());
            throw new RemoteException("Erro ao carregar o arquivo de propriedades", e);
        }

        // Escolher a configuração do Barrel com base no nome (barrel1 ou barrel2)
        String rmiUrl = properties.getProperty(barrelName + ".rmi.url");
        String dbUrl = properties.getProperty(barrelName + ".db.url");
        String dbUser = properties.getProperty(barrelName + ".db.user");
        String dbPassword = properties.getProperty(barrelName + ".db.password");

        // Log de depuração
        logger.info("Conectando ao Barrel: " + barrelName);
        logger.info("RMI URL: " + rmiUrl);
        logger.info("Banco de Dados URL: " + dbUrl);

        // Conectar ao banco de dados do Barrel
        try {
            this.conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao conectar ao banco de dados: " + e.getMessage());
            throw new RemoteException("Erro ao conectar ao banco de dados", e);
        }
    }

    // Método para adicionar uma palavra ao índice associada a uma URL
    @Override
    public void addToIndex(String word, String url) throws RemoteException {
        try (PreparedStatement stmtWord = conn.prepareStatement("INSERT INTO word (word) VALUES (?) ON CONFLICT (word) DO NOTHING");
             PreparedStatement stmtURL = conn.prepareStatement("INSERT INTO urls (url, ranking, titulo, citacao) VALUES (?, 0, 'Título', 'Citação') ON CONFLICT (url) DO NOTHING");
             PreparedStatement stmtWordUrl = conn.prepareStatement("INSERT INTO word_url (word, url) VALUES (?, ?) ON CONFLICT (word, url) DO NOTHING")) {

            // Adiciona a palavra ao índice, se não existir
            stmtWord.setString(1, word);
            stmtWord.executeUpdate();

            // Adiciona a URL, se não existir
            stmtURL.setString(1, url);
            stmtURL.executeUpdate();

            // Adiciona a relação entre a palavra e a URL
            stmtWordUrl.setString(1, word);
            stmtWordUrl.setString(2, url);
            stmtWordUrl.executeUpdate();

            logger.info("Palavra '" + word + "' associada à URL '" + url + "' no índice.");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao adicionar ao índice: " + e.getMessage());
            throw new RemoteException("Erro ao adicionar ao índice", e);
        }
    }

    // Método para procurar uma palavra no índice e retornar as URLs associadas
    @Override
    public List<String> search(String word) throws RemoteException {
        List<String> urls = new ArrayList<>();
        String query = "SELECT url FROM word_url WHERE word = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, word);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    urls.add(rs.getString("url"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar palavra no índice: " + e.getMessage());
            throw new RemoteException("Erro ao buscar palavra no índice", e);
        }
        return urls;
    }

    // Método para fechar a conexão (se necessário)
    public void closeConnection() throws RemoteException {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao fechar a conexão: " + e.getMessage());
            throw new RemoteException("Erro ao fechar a conexão", e);
        }
    }

    @Override
    public List<String> getConnections(String url) throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'getConnections'");
    }
}
