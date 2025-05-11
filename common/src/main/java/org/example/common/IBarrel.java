package org.example.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The {@code IBarrel} interface defines the remote methods that can be invoked by clients on databases.
 * It provides operations to interact with the index, search results, and perform data synchronization.
 * 
 * This interface extends {@link Remote} to enable remote method invocations (RMI).
 */
public interface IBarrel extends Remote {
    /**
     * Adds words to the index associated with a URL.
     * This method inserts the words and their frequencies into the database, 
     * along with the associated URL and its metadata.
     *
     * @param words   A map of words and their corresponding frequencies.
     * @param url     The URL where the words were found.
     * @param toUrls  A list of URLs linked from the given URL.
     * @param titulo  The title of the webpage associated with the URL.
     * @param citaçao A citation or snippet from the webpage.
     * @throws RemoteException If there is a communication issue with the remote server.
     * @throws SQLException If an SQL error occurs while processing the data.
     */
    void addToIndex(Map<String, Integer> words, String url, List<String> toUrls, String titulo, String citaçao) throws RemoteException, SQLException;


    /**
     * Searches for a list of words in the index and returns a list of search results.
     * Each result contains a URL, its associated title, citation, and the number of matches.
     *
     * @param search A list of words to search for in the index.
     * @return A list of {@link SearchResult} objects containing matching URLs and their details.
     * @throws RemoteException If there is a communication issue with the remote server.
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

    /**
     * Checks whether a URL is contained in the index.
     *
     * @param url The URL to check.
     * @return {@code true} if the URL is indexed; {@code false} otherwise.
     * @throws RemoteException If there is a communication issue with the remote server.
     */
    boolean containsUrl(String url) throws RemoteException;

    /**
     * Retrieves the top searched words in the index.
     * This method returns the most frequently searched words in the index.
     *
     * @return A list of the top searched words.
     * @throws RemoteException If there is a communication issue with the remote server.
     */
    List<String> getTopSearches() throws RemoteException;

    /**
     * Retrieves the size of the index.
     * This method returns the number of entries in the word_url table.
     *
     * @return The number of entries in the index.
     * @throws RemoteException If there is a communication issue with the remote server.
     */
    int getSize() throws RemoteException;

    /**
     * Retrieves the most frequent words in the index.
     * This method returns the most frequent words in the word_url table.
     *
     * @return A list of the most frequent words.
     * @throws RemoteException If there is a communication issue with the remote server.
     */
    List<String> getFrequentWords() throws RemoteException;

    /**
     * Retrieves the filename or file path associated with the Barrel's data.
     *
     * @return The file path of the Barrel's data file.
     * @throws RemoteException If there is a communication issue with the remote server.
     */
    String getFicheiro() throws RemoteException;

    /**
     * Updates the top frequency of a list of words in the index.
     * This method increments the "top" value of each word in the word table.
     *
     * @param words A list of words whose top value should be incremented.
     * @throws RemoteException If there is a communication issue with the remote server.
     */
    void updateTopWords(ArrayList<String> words) throws RemoteException;

    /**
     * Connects to the database of the Barrel.
     * This method establishes a connection to the Barrel's database.
     *
     * @throws RemoteException If there is a communication issue with the remote server.
     */
    void connect() throws RemoteException;

    /**
     * Retrieves the contents of the Barrel's data file.
     * This method returns the content of the Barrel's file as a byte array.
     *
     * @return The content of the Barrel's data file as a byte array.
     * @throws RemoteException If there is a communication issue with the remote server.
     */
    byte[] getFile() throws RemoteException;

    /**
     * Synchronizes data between two Barrels.
     * This method transfers the data from the current Barrel to the given Barrel.
     *
     * @param barrel The Barrel to synchronize data with.
     * @throws RemoteException If there is a communication issue with the remote server.
     */
    void sync(IBarrel barrel) throws RemoteException;

    /**
     * Receives a copy of the data file from another Barrel.
     * This method writes the received file content to the Barrel's data file.
     *
     * @param ficheiro The byte array containing the data to be written.
     * @throws RemoteException If there is a communication issue with the remote server.
     */
    void receberCopia(byte[] ficheiro) throws RemoteException;
}

