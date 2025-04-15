package org.example.Queue;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IQueue extends Remote {
    // Método para obter uma URL da fila
    String getURL() throws RemoteException;

    // Método para adicionar uma URL à fila
    void addURL(String url) throws RemoteException;

    void addFirst(String url) throws RemoteException;

    List<String> getStopwords() throws RemoteException;

    public void addStopWords(List<String> words) throws RemoteException;

}