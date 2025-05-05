package org.example.common;

import org.json.JSONException;

import java.io.IOException;
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

    public void addFirst(String url) throws RemoteException;

    /**
     * Searches for a specified terms and returns the searched results.
     *
     * @param search the search terms
     * @return the searched results as a SearchResult object
     * @throws RemoteException if a remote communication error occurs
     */
    public List<SearchResult> search(ArrayList<String> search) throws RemoteException;

    /**
     * Searches for all URL connections of the given URL.
     * 
     * @param url The URL given to search for its connections
     * @return the search results as a SearchResult object
     * @throws RemoteException if a remote error occurs during the search
     */
    public SearchResult getConnections(String url) throws RemoteException;

    /**
     * Retrieves the latest system statistics.
     * @return The latest statistics.
     * @throws RemoteException If an RMI error occurs.
     */
    public SystemStatistics getStatistics() throws RemoteException;

    /**
     * Registers a client to receive live statistics updates.
     * @param listener The listener that will receive updates.
     * @throws RemoteException If an RMI error occurs.
     */
    public void registerStatisticsListener(IStatistics listener) throws RemoteException;

    void broadcastStatistics(SystemStatistics stats) throws RemoteException;
    
    public void registarBarrel(String rmi) throws RemoteException;

    public void unregisterBarrel(String rmi) throws RemoteException;

    public Map<String, IBarrel> getBarrels() throws RemoteException;

    public List<String> getStopwords() throws RemoteException;

    public  boolean isFlag() throws RemoteException;

    public void hackerNews(String termoPesquisa) throws IOException, RemoteException, JSONException;


}
