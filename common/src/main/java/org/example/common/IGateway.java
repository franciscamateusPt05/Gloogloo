package org.example.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The IGateway interface defines the methods for a remote Gateway object
 * that provides various methods described below. 
 */
public interface IGateway extends Remote {

    /**
     * Inserts a URL into the queue.
     *
     * @param url the URL to be inserted
     * @throws RemoteException if a remote communication error occurs
     */
    public void insertURL(String url) throws RemoteException;

    void addFirst(String url) throws RemoteException;

    /**
     * Searches for a specified terms and returns the searched results.
     *
     * @param search the search terms
     * @return the searched results as a SearchResult object
     * @throws RemoteException if a remote communication error occurs
     */
    List<SearchResult> search(ArrayList<String> search) throws RemoteException;

    /**
     * Searches for all URL connections of the given URL.
     * 
     * @param url The URL given to search for its connections
     * @return the search results as a SearchResult object
     * @throws RemoteException if a remote error occurs during the search
     */
    SearchResult getConnections(String url) throws RemoteException;

    /**
     * Retrieves the latest system statistics.
     * @return The latest statistics.
     * @throws RemoteException If an RMI error occurs.
     */
    SystemStatistics getStatistics() throws RemoteException;

    /**
     * Registers a client to receive live statistics updates.
     * @param listener The listener that will receive updates.
     * @throws RemoteException If an RMI error occurs.
     */
    void registerStatisticsListener(IStatistics listener) throws RemoteException;

    void broadcastStatistics(SystemStatistics stats) throws RemoteException;
    
    void registarBarrel(String rmi) throws RemoteException;

    void unregisterBarrel(String rmi) throws RemoteException;

    Map<String, IBarrel> getBarrels() throws RemoteException;
    List<String> getStopwords() throws RemoteException;

    boolean isFlag() throws RemoteException;

    void hacker(String title) throws RemoteException;

    String getAI(String search,List<SearchResult> result) throws RemoteException;


}
