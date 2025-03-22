package org.example.Barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface IBarrel extends Remote {
    /**
     * Adiciona uma palavra ao índice do Barrel associada a uma URL.
     *
     * @param url URL onde a palavra foi encontrada.
     * @return
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    boolean addToIndex(Map<String, Integer> words, String url, List<String> toUrls, String titulo, String citaçao) throws RemoteException;


        /**
         * Procura uma palavra no índice do Barrel e retorna uma lista de URLs associadas.
         *
         * @param word Palavra a procurar.
         * @return Lista de URLs onde a palavra foi encontrada.
         * @throws RemoteException Se ocorrer um erro na comunicação RMI.
         */
    List<String> search(String word) throws RemoteException;

    /**
     * Searches for all URL connections of the given URL.
     * 
     * @param url The URL given to search for its connections
     * @return the search results as a SearchResult object
     * @throws RemoteException if a remote error occurs during the search
     */
    List<String> getConnections (String url) throws RemoteException;

    boolean containsUrl(String url) throws RemoteException;


    IBarrel getOutroBarrel() throws RemoteException;

    void setOutroBarrel(IBarrel outroBarrel) throws RemoteException;

    boolean FinalizarOpe() throws Exception;

    boolean isSucess() throws RemoteException;

    void setSucess(boolean sucess) throws RemoteException;
}

