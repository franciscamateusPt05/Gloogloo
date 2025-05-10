package org.example.Barrel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.example.common.*;

public class BarrelImpl extends UnicastRemoteObject implements IBarrel {
    private static final Logger logger = Logger.getLogger(BarrelImpl.class.getName());
    private static final String BARREL_CONFIG_FILE = "backend/src/main/java/org/example/Properties/barrel.properties";
    public Connection conn;

    public String rmiUrl;
    public String dbUrl;
    public String ficheiro;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    public BarrelImpl(String barrelName) throws RemoteException {
        super();

        Properties properties = new Properties();

        // Carregar propriedades do arquivo
        try (FileInputStream fis = new FileInputStream(BARREL_CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao carregar o arquivo de propriedades: " + e.getMessage());
            throw new RemoteException("Erro ao carregar o arquivo de propriedades", e);
        }

        // Escolher a configuração do Barrel com base no nome (barrel1 ou barrel2)
        rmiUrl = properties.getProperty(barrelName + ".rmi.url");
        dbUrl = properties.getProperty(barrelName + ".db.url");
        this.ficheiro = properties.getProperty(barrelName + ".db.file");

        // Log de depuração
        logger.info("Conectando ao Barrel: " + barrelName);
        logger.info("RMI URL: " + rmiUrl);
        logger.info("Banco de Dados URL: " + dbUrl);

    }

    // Método para adicionar uma palavra ao índice associada a uma URL
    @Override
    public synchronized void addToIndex(Map<String, Integer> words, String url, List<String> toUrls, String titulo, String citaçao) throws RemoteException, SQLException {
            lock.writeLock().lock();
            try {
                WriteData(words, url, toUrls, titulo, citaçao);
            } catch (SQLException e) {
                e.printStackTrace();
                try {
                    if (this.conn != null && !this.conn.getAutoCommit()) {
                        this.conn.rollback();
                    }
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
                throw new RemoteException("Erro ao adicionar informações ao índice", e);
            }finally {
                lock.writeLock().unlock();
            }

    }

    private void WriteData(Map<String, Integer> words, String url, List<String> toUrls, String titulo, String citaçao) throws RemoteException, SQLException {

        if (this.conn == null || this.conn.isClosed()) {
            logger.warning("Conexão com o banco está fechada. Tentando reconectar...");
            connect();
        }
        this.conn.setAutoCommit(false);

        // Inserir ou atualizar as palavras no banco de dados
        for (Map.Entry<String, Integer> entry : words.entrySet()) {
            String word = entry.getKey();
            int frequency = entry.getValue();

            String insertWordSQL = "INSERT INTO word (word) VALUES (?) ON CONFLICT (word) DO NOTHING";
            try (PreparedStatement stmt = this.conn.prepareStatement(insertWordSQL)) {
                stmt.setString(1, word);
                stmt.executeUpdate();
            }

            // Inserir a URL na tabela urls
            String selectUrlSQL = "SELECT url FROM urls WHERE url = ?";
            try (PreparedStatement stmt = this.conn.prepareStatement(selectUrlSQL)) {
                stmt.setString(1, url);
                var resultSet = stmt.executeQuery();

                if (!resultSet.next()) {
                    // Se a URL não existir, insira a nova URL com dados default
                    String insertUrlSQL = "INSERT INTO urls (url, ranking, titulo, citacao) VALUES (?, 0, ?, ?)";
                    try (PreparedStatement insertStmt = this.conn.prepareStatement(insertUrlSQL)) {
                        insertStmt.setString(1, url);
                        insertStmt.setString(2, titulo);
                        insertStmt.setString(3, citaçao);
                        insertStmt.executeUpdate();
                    }
                }
            }

            String insertWordUrlSQL = "INSERT INTO word_url (word, url, frequency) VALUES (?, ?, ?) ";
            try (PreparedStatement stmt = this.conn.prepareStatement(insertWordUrlSQL)) {
                stmt.setString(1, word);
                stmt.setString(2, url);
                stmt.setInt(3, frequency);
                stmt.executeUpdate();
            }
        }


        // Inserir links de URLs
        for (String toUrl : toUrls) {
            String insertLinkSQL = "INSERT INTO url_links (from_url, to_url) VALUES (?, ?)";
            try (PreparedStatement stmt = this.conn.prepareStatement(insertLinkSQL)) {
                stmt.setString(1, url);
                stmt.setString(2, toUrl);
                stmt.executeUpdate();
            }
        }

        conn.commit();
    }

    public List<SearchResult> search(ArrayList<String> words) throws RemoteException {
        List<SearchResult> results = new ArrayList<>();

        if (words == null || words.size() == 0) {
            return results;
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT u.url, u.titulo, u.citacao, u.ranking, ")
                .append("GROUP_CONCAT(DISTINCT ul.from_url) AS incoming_links, ")
                .append("COUNT(DISTINCT w.word) AS matched_words ")
                .append("FROM word_url w ")
                .append("JOIN urls u ON w.url = u.url ")
                .append("LEFT JOIN url_links ul ON u.url = ul.to_url ")
                .append("WHERE w.word IN (");

        // Dynamically add placeholders
        for (int i = 0; i < words.size(); i++) {
            queryBuilder.append("?");
            if (i < words.size() - 1) {
                queryBuilder.append(", ");
            }
        }
        queryBuilder.append(") ")
                .append("GROUP BY u.url, u.titulo, u.citacao, u.ranking ")
                .append("HAVING matched_words = ? ")
                .append("ORDER BY u.ranking DESC, matched_words DESC;");

        try {
            if (this.conn == null || this.conn.isClosed()) {
                logger.warning("Conexão com o banco está fechada. Tentando reconectar...");
                connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao verificar conexão com o banco", e);
        }

        try (PreparedStatement stmt = this.conn.prepareStatement(queryBuilder.toString())) {

            int index = 1;
            for (String word : words) {
                stmt.setString(index++, word);
            }

            stmt.setInt(index, words.size());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String url = rs.getString("url");
                    String title = rs.getString("titulo");
                    String snippet = rs.getString("citacao");
                    String incomingLinksStr = rs.getString("incoming_links");

                    List<String> incomingLinks = new ArrayList<>();
                    if (incomingLinksStr != null) {
                        String[] links = incomingLinksStr.split(",");
                        Collections.addAll(incomingLinks, links);
                    }

                    results.add(new SearchResult(title, url, snippet));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Database query failed", e);
        }

        return results;
    }

    public synchronized void updateTopWords(ArrayList<String> words) throws RemoteException {
        // Start a transaction
        try {
            if (this.conn == null || this.conn.isClosed()) {
                logger.warning("Conexão com o banco está fechada. Tentando reconectar...");
                connect();
            }

            this.conn.setAutoCommit(false);

            try (PreparedStatement updateStmt = this.conn.prepareStatement(
                    "UPDATE word SET top = top + 1 WHERE word = ?")) {

                for (String word : words) {
                    System.out.println("Updating top for word: " + word);
                    updateStmt.setString(1, word);
                    int rowsAffected = updateStmt.executeUpdate();
                    System.out.println("Rows affected: " + rowsAffected);

                    if (rowsAffected == 0) {
                        System.out.println("No rows updated for word: " + word);
                    }
                }
            }

            this.conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (this.conn != null) {
                    this.conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw new RemoteException("Failed to update word table", e);
        }
    }


    public SearchResult getConnections(String url) throws RemoteException {
        List<String> connectedUrls = new ArrayList<>();
        String query = "SELECT to_url FROM url_links WHERE from_url = ?;";

        try {
            if (this.conn == null || this.conn.isClosed()) {
                logger.warning("Conexão com o banco está fechada. Tentando reconectar...");
                connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao verificar conexão com o banco", e);
        }

        try (PreparedStatement stmt = this.conn.prepareStatement(query)) {
            stmt.setString(1, url);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    connectedUrls.add(rs.getString(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Database query failed", e);
        }

        return new SearchResult(url, connectedUrls);
    }

    public boolean containsUrl(String url) throws RemoteException {
        String query = "SELECT COUNT(*) FROM urls WHERE url = ?";
        boolean resposta = true;

        try {
            if (this.conn == null || this.conn.isClosed()) {
                logger.warning("Conexão com o banco está fechada. Tentando reconectar...");
                connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao verificar conexão com o banco", e);
        }

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, url);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    resposta = rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resposta;
    }

    public List<String> getTopSearches() throws RemoteException {
        List<String> topSearches = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(dbUrl)) {

            String query = "SELECT word FROM word WHERE top IS NOT NULL AND top <> 0 ORDER BY top DESC LIMIT 10;";
            try (Statement stmt = connection.createStatement()) {
                ResultSet resultSet = stmt.executeQuery(query);

                while (resultSet.next()) {
                    topSearches.add(resultSet.getString("word"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Error retrieving top searches from the database.", e);
        }

        return topSearches;
    }

    public int getSize() throws RemoteException {

        String query = "SELECT COUNT(*) FROM word_url";  // Query to count rows in word_url table

        try (Connection connection = DriverManager.getConnection(dbUrl);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // If there is a result, return the count
            if (rs.next()) {
                return rs.getInt(1);  // Return the count from the query result
            } else {
                return 0;  // Return 0 if no rows are found
            }
        } catch (Exception e) {
            // Handle exceptions, such as connection errors or SQL issues
            e.printStackTrace();
            throw new RemoteException("Error querying the database", e);
        }
    }

    public List<String> getFrequentWords() throws RemoteException {
        List<String> frequentWords = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl)) {

            // Get total word count
            int wordCount = 0;
            try (PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) FROM word")) {
                ResultSet countRs = countStmt.executeQuery();
                if (countRs.next()) {
                    wordCount = countRs.getInt(1);
                }
            }

            // Set Limit
            int limit = Math.max(1, (int) Math.ceil(wordCount * 0.05));

            // Get top frequent words up to that limit
            String query = """
                    SELECT wu.word
                    FROM word_url wu
                    GROUP BY wu.word
                    ORDER BY SUM(wu.frequency) DESC
                    LIMIT ?
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    frequentWords.add(rs.getString("word"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Database error while fetching frequent words", e);
        }

        return frequentWords;
    }


    public String getFicheiro() throws RemoteException {
        return ficheiro;
    }

    public void connect() throws RemoteException {
        // Conectar ao banco de dados do Barrel
        try {
            this.conn = DriverManager.getConnection(dbUrl);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao conectar ao banco de dados: " + e.getMessage());
            throw new RemoteException("Erro ao conectar ao banco de dados", e);
        }
    }

    @Override
    public byte[] getFile() throws RemoteException {
        try {
            Path path = Paths.get(getFicheiro()); // substitui com o caminho real
            return Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void sync(String destino) throws RemoteException {
        lock.writeLock().lock();  // Lock de escrita porque vais gravar ficheiro
        try (FileOutputStream fos = new FileOutputStream(destino)) {
            fos.write(getFile());
            System.out.println("Base de dados recebida e gravada em: " + destino);
        } catch (IOException e) {
            throw new RemoteException("Erro ao gravar o ficheiro de base de dados", e);
        } finally {
            lock.writeLock().unlock();  // Liberta o lock no final, mesmo com erro
        }
    }


}


