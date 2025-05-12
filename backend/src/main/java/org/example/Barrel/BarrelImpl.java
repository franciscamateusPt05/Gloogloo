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

/**
 * Implementation of the IBarrel interface for RMI-based indexing and search services.
 * <p>
 * Handles word indexing, searching, synchronization, and connection with SQLite databases.
 * </p>
 */
public class BarrelImpl extends UnicastRemoteObject implements IBarrel {
    private static final Logger logger = Logger.getLogger(BarrelImpl.class.getName());
    private static final String BARREL_CONFIG_FILE = "backend/src/main/java/org/example/Properties/barrel.properties";
    public Connection conn;

    public String rmiUrl;
    public String dbUrl;
    public String ficheiro;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructs a new BarrelImpl instance using the specified barrel name to load configuration.
     *
     * @param barrelName Name identifier for the barrel (e.g., "barrel1" or "barrel2").
     * @throws RemoteException If RMI or file loading fails.
     */
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

        // Escolher a configuração do Barrel com base no nome
        rmiUrl = properties.getProperty(barrelName + ".rmi.url");
        dbUrl = properties.getProperty(barrelName + ".db.url");
        this.ficheiro = properties.getProperty(barrelName + ".db.file");

        logger.info("Conectando ao Barrel: " + barrelName);
        logger.info("RMI URL: " + rmiUrl);
        logger.info("Banco de Dados URL: " + dbUrl);

    }

    /**
     * Adds words and metadata to the inverse index database, ensuring thread-safety with write locking.
     *
     * @param words    Map of words and their frequencies.
     * @param url      URL the words are associated with.
     * @param toUrls   List of URLs linked from this URL.
     * @param titulo   Title of the document.
     * @param citaçao  Citation/snippet from the document.
     * @throws RemoteException If RMI or SQL operations fail.
     * @throws SQLException    If a database error occurs.
     */
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

    /**
     * Writes indexing data to the database. This includes:
     * <ul>
     *     <li>Inserting or updating words in the {@code word} table.</li>
     *     <li>Inserting the URL into the {@code urls} table if it doesn't exist,
     *         along with its title and citation.</li>
     *     <li>Inserting word-url-frequency mappings into the {@code word_url} table.</li>
     *     <li>Storing outgoing URL links in the {@code url_links} table.</li>
     * </ul>
     *
     * The method ensures all operations are wrapped in a single transaction and commits
     * the transaction at the end. If an error occurs, the transaction is rolled back.
     *
     * @param words     A map containing words as keys and their frequency as values.
     * @param url       The source URL associated with the words.
     * @param toUrls    A list of URLs that the source URL links to (outgoing links).
     * @param titulo    The title of the webpage at the given URL.
     * @param citaçao   A snippet or citation from the webpage.
     * @throws RemoteException if there's a problem with remote communication.
     * @throws SQLException if any SQL operation fails.
     */
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

            // Inserir o URL na tabela urls
            String selectUrlSQL = "SELECT url FROM urls WHERE url = ?";
            try (PreparedStatement stmt = this.conn.prepareStatement(selectUrlSQL)) {
                stmt.setString(1, url);
                var resultSet = stmt.executeQuery();

                if (!resultSet.next()) {
                    // Se a URL não existir, inserir novo URL com dados default
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

    /**
     * Searches the index for documents containing the given words.
     *
     * @param words List of search terms.
     * @return List of SearchResult objects matching the search criteria.
     * @throws RemoteException If a remote or database error occurs.
     */
    @Override
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

    /**
     * Updates the top words frequency for the given words in databse.
     *
     * @param words List of words to update.
     * @throws RemoteException If a remote or SQL error occurs.
     */
    @Override
    public synchronized void updateTopWords(ArrayList<String> words) throws RemoteException {
        // Starting the transaction
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

    /**
     * Retrieves URL connections from a specified URL.
     *
     * @param url Source URL to get linked destinations from.
     * @return SearchResult containing the URL and its connections.
     * @throws RemoteException If a database error occurs.
     */
    @Override
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

    /**
     * Checks whether the given URL exists in the index.
     *
     * @param url URL to check.
     * @return True if the URL exists, false otherwise.
     * @throws RemoteException If a database error occurs.
     */
    @Override
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

    /**
     * Retrieves the top 10 most searched words.
     *
     * @return List of top searched words.
     * @throws RemoteException If a database access error occurs.
     */
    @Override
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

    /**
     * Returns the number of indexed word entries.
     *
     * @return Integer representing the size of the word_url table.
     * @throws RemoteException If a database error occurs.
     */
    @Override
    public int getSize() throws RemoteException {

        String query = "SELECT COUNT(*) FROM word_url";  // Query to count rows in word_url table

        try (Connection connection = DriverManager.getConnection(dbUrl);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // If there is a result, return the count
            if (rs.next()) {
                return rs.getInt(1); 
            } else {
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Error querying the database", e);
        }
    }

    /**
     * Retrieves a list of the most frequent words indexed.
     * This is based on the top 5% of frequency totals.
     *
     * @return List of frequently used words.
     * @throws RemoteException If a database error occurs.
     */
    @Override
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

    /**
     * Returns the file path of the local database file.
     *
     * @return Path to the local database file.
     * @throws RemoteException If remote access fails.
     */
    @Override
    public String getFicheiro() throws RemoteException {
        return ficheiro;
    }

    /**
     * Connects to the barrel's database using the loaded configuration.
     *
     * @throws RemoteException If a connection error occurs.
     */
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

    /**
     * Retrieves the contents of the database file as a byte array.
     *
     * @return Byte array of the file contents.
     * @throws RemoteException If an IO error occurs.
     */
    @Override
    public synchronized byte[] getFile() throws RemoteException {
        try {
            Path path = Paths.get(getFicheiro());
            return Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sends a copy of the database file to another barrel.
     *
     * @param barrel Remote barrel to sync with.
     * @throws RemoteException If file reading or RMI transfer fails.
     */
    @Override
    public synchronized void sync(IBarrel barrel) throws RemoteException {
        lock.readLock().lock();  // Lock de leitura, porque vamos ler o ficheiro
        try {
            barrel.receberCopia(getFile());  // envia para o outro Barrel
            System.out.println("Cópia enviada para o Barrel de destino.");
        } catch (IOException e) {
            throw new RemoteException("Erro ao ler a base de dados local", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Receives and saves a copy of the database from another barrel.
     *
     * @param ficheiro Byte array representing the file to write.
     * @throws RemoteException If an IO or Remote error occurs during writing.
     */
    @Override
    public synchronized void receberCopia(byte[] ficheiro) throws RemoteException {
        try (FileOutputStream fos = new FileOutputStream(getFicheiro())) {
            fos.write(ficheiro);
            System.out.println("Cópia da base de dados recebida e gravada.");
        } catch (IOException e) {
            throw new RemoteException("Erro ao gravar a cópia recebida", e);
        }
    }


}


