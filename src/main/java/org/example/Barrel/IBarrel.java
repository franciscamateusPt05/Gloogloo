package org.example.Barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.example.SearchResult;

public interface IBarrel extends Remote {
    /**
     * Adiciona uma palavra ao índice do Barrel associada a uma URL.
     *
     * @param url URL onde a palavra foi encontrada.
     * @return
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    void addToIndex(Map<String, Integer> words, String url, List<String> toUrls, String titulo, String citaçao) throws RemoteException, SQLException;


    /**
     * Procura uma palavra no índice do Barrel e retorna uma lista de URLs associadas.
     *
     * @return Lista de URLs onde a palavra foi encontrada.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    List<SearchResult> search(ArrayList<String> search) throws RemoteException;

    /**
     * Searches for all URL connections of the given URL.
     *
     * @param url The URL given to search for its connections
     * @return the search results as a SearchResult object
     * @throws RemoteException if a remote error occurs during the search
     */
    SearchResult getConnections (String url) throws RemoteException;

    boolean containsUrl(String url) throws RemoteException;

    List<String> getTopSearches() throws RemoteException;

    int getSize() throws RemoteException;

    List<String> getFrequentWords() throws RemoteException;

    String getFicheiro() throws RemoteException;

    void updateTopWords(ArrayList<String> words) throws RemoteException;

    void connect() throws RemoteException;

    void darLock() throws RemoteException;

    void darUnlock() throws RemoteException, SQLException;
}

