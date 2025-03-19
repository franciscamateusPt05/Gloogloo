package org.example;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueueImpl extends UnicastRemoteObject implements IQueue {
    private Connection connection;

    public QueueImpl() throws RemoteException {
        super();
        try {
            // Conectar à base de dados
            String url = "jdbc:postgresql://localhost:5432/queue";  // Ajuste para o seu DB
            String user = "postgres";
            String password = "2024";
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Falha na conexão com a base de dados.", e);
        }
    }

    @Override
    public synchronized String getURL() throws RemoteException {
        try {
            // Pega a próxima URL da base de dados
            String sql = "SELECT url FROM queue LIMIT 1";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String url = rs.getString("url");

                // Remover a URL da base de dados após obter
                String deleteSql = "DELETE FROM queue WHERE url = ?";
                PreparedStatement deleteStmt = connection.prepareStatement(deleteSql);
                deleteStmt.setString(1, url);
                deleteStmt.executeUpdate();

                return url;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;  // Se não houver mais URLs
    }

    @Override
    public synchronized void addURL(String url) throws RemoteException {
        try {
            // Adiciona a URL à base de dados
            String sql = "INSERT INTO queue (url) VALUES (?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, url);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

