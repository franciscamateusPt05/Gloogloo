package org.example.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The {@code IGateway} interface defines the methods for a remote Gateway object
 * that provides various operations related to URL management, search, statistics, and Barrel management.
 * This interface is designed to be used with RMI for remote communication and to serve clients.
 */
public interface IGateway extends Remote {

    /**
     * Inserts a URL into the queue.
     *
     * @param url the URL to be inserted
     * @throws RemoteException if a remote communication error occurs
     */
    public void insertURL(String url) throws RemoteException;

    /**
     * Inserts a URL into the beggining of the queue.
     *
     * @param url the URL to be inserted
     * @throws RemoteException if a remote communication error occurs
     */
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

    /**
     * Broadcasts the current statistics to all registered listeners.
     * This method can be used to send the current system statistics to all clients that are listening for updates.
     *
     * @param stats the {@link SystemStatistics} to be broadcast.
     * @throws RemoteException if a remote communication error occurs during the broadcast.
     */
    void broadcastStatistics(SystemStatistics stats) throws RemoteException;
    
    /**
     * Registers a Barrel instance with the Gateway using the provided RMI address.
     * This method associates a Barrel with the Gateway for communication and management.
     *
     * @param rmi the RMI address of the Barrel to be registered.
     * @throws RemoteException if a remote communication error occurs during registration.
     */
    void registarBarrel(String rmi) throws RemoteException;

    /**
     * Unregisters a Barrel instance from the Gateway using the provided RMI address.
     * This method dissociates a Barrel from the Gateway, effectively removing it from management.
     *
     * @param rmi the RMI address of the Barrel to be unregistered.
     * @throws RemoteException if a remote communication error occurs during unregistration.
     */
    void unregisterBarrel(String rmi) throws RemoteException;

    /**
     * Retrieves all currently registered Barrels.
     * This method returns a map of RMI addresses to Barrel objects.
     *
     * @return a map of RMI addresses to {@link IBarrel} objects representing the registered Barrels.
     * @throws RemoteException if a remote communication error occurs while retrieving the Barrels.
     */
    Map<String, IBarrel> getBarrels() throws RemoteException;

    /**
     * Retrieves a list of stopwords currently used by the system.
     * These stopwords are words that are typically ignored in searches due to their common usage.
     *
     * @return a list of stopwords used by the system.
     * @throws RemoteException if a remote communication error occurs while retrieving the stopwords.
     */
    List<String> getStopwords() throws RemoteException;

    /**
     * Performs a search on Hacker News top stories and adds URLs to the queue if any
     * of the search terms are found in the article's title, text, or HTML content.
     * <p>
     * This method queries the Hacker News API for the top stories, normalizes the search 
     * title by removing diacritical marks and punctuation, and then searches for each 
     * term in the title and text of the articles. If no match is found, it fetches the 
     * HTML content of the article and checks there as well.
     * </p>
     *
     * @param title the search title used to match against Hacker News articles' titles, texts, and HTML content.
     * @throws RemoteException if there is a remote communication error, including failures to connect to the Hacker News API.
     */
    void hacker(String title) throws RemoteException;

    /**
     * Performs an AI-based search and analysis on the provided search term and search results.
     * The method utilizes AI to enhance the search results or provide recommendations based on the input.
     *
     * @param search the search term to analyze.
     * @param result a list of {@link SearchResult} objects to be used for AI analysis.
     * @return the AI-generated response based on the search and results.
     * @throws RemoteException if a remote communication error occurs during the AI search.
     */
    String getAI(String search,List<SearchResult> result) throws RemoteException;

}
