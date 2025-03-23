package org.example.Barrel;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.example.SearchResult;

public class BarrelImpl extends UnicastRemoteObject implements IBarrel {
    private static final Logger logger = Logger.getLogger(BarrelImpl.class.getName());
    private Connection conn;
    private boolean sucess = false;
    private IBarrel outroBarrel;
    private String outro;



    public BarrelImpl(String barrelName, String outro) throws RemoteException {
        super();
        this.outro = outro;
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
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao conectar ao banco de dados: " + e.getMessage());
            throw new RemoteException("Erro ao conectar ao banco de dados", e);
        }

    }

    // Método para adicionar uma palavra ao índice associada a uma URL
    @Override
    public boolean addToIndex(Map<String, Integer> words, String url, List<String> toUrls, String titulo, String citaçao) throws RemoteException {
        try {
            outroBarrel = (IBarrel) Naming.lookup(this.outro);
        } catch (NotBoundException e) {
            return false;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        try {

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

            outroBarrel.setSucess(true); //Notifico que já foi feito

            long tempoInicial = System.currentTimeMillis();
            while (System.currentTimeMillis() - tempoInicial < 5000) {
                if (outroBarrel.isSucess() && this.sucess) {
                    this.conn.commit();
                    return true;
                }
            }
            conn.rollback();
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (this.conn != null) {
                    this.conn.rollback();
                    return false;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                return false;
            }
            throw new RemoteException("Erro ao adicionar informações ao índice", e);
        }

    }

    // Método para procurar uma palavra no índice e retornar as URLs associadas
    @Override
    public List<SearchResult> search(String word) throws RemoteException {

        // MODIFICAR

        List<SearchResult> urls = new ArrayList<SearchResult>();
        String query = "SELECT url FROM word_url WHERE word = ?";
       
        return urls;
    }

    // Método para fechar a conexão (se necessário)
    public void closeConnection(Connection conn) throws RemoteException {
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
    public SearchResult getConnections(String url) throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'getConnections'");
    }

    @Override
    public boolean containsUrl(String url) throws RemoteException {
        String query = "SELECT COUNT(*) FROM urls WHERE url = ?";
        boolean resposta = true;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, url);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    resposta = rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return resposta;
    }

    @Override
    public IBarrel getOutroBarrel() throws RemoteException {
        return outroBarrel;
    }

    @Override
    public void setOutroBarrel(IBarrel outroBarrel) throws RemoteException {
        this.outroBarrel = outroBarrel;
    }

    @Override
    public boolean FinalizarOpe() throws Exception {
        try {
            if (this.sucess && this.outroBarrel.isSucess()) {
                if (this.conn != null) {
                    this.conn.commit();
                }
                return true;
            } else {
                if (this.conn != null) {
                    this.conn.rollback();
                }
                return false;
            }
        } catch (SQLException e) {
            try {
                if (this.conn != null) {
                    this.conn.rollback(); // Caso aconteça algum erro, desfaz a transação
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw new RemoteException("Erro ao finalizar operação", e);
        }
    }


    @Override
    public boolean isSucess() throws RemoteException {
        return sucess;
    }

    @Override
    public void setSucess(boolean sucess) throws RemoteException {
        this.sucess = sucess;
    }
}
