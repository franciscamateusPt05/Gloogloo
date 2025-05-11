package org.example.Queue;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface for a queue that handles URLs and stopwords in a remote context.
 * Provides methods to add URLs, retrieve URLs, and manage stopwords in the queue.
 * This interface extends {@link Remote} to allow remote invocation of these methods.
 * 
 */
public interface IQueue extends Remote {
    
    /**
     * Retrieves a URL from the queue.
     * 
     * @return a URL as a {@link String} from the queue.
     * @throws RemoteException if there is a communication error during the remote method invocation.
     */
    String getURL() throws RemoteException;

    /**
     * Adds a URL to the queue.
     * 
     * @param url the URL to be added to the queue.
     * @throws RemoteException if there is a communication error during the remote method invocation.
     */
    void addURL(String url) throws RemoteException;

    /**
     * Adds a URL to the front of the queue.
     * 
     * @param url the URL to be added to the front of the queue.
     * @throws RemoteException if there is a communication error during the remote method invocation.
     */
    void addFirst(String url) throws RemoteException;

    /**
     * Retrieves the list of stopwords from the queue.
     * 
     * @return a {@link List} of stopwords as {@link String}s.
     * @throws RemoteException if there is a communication error during the remote method invocation.
     */
    List<String> getStopwords() throws RemoteException;

    /**
     * Adds a list of stopwords to the queue.
     * 
     * @param words the list of stopwords to be added.
     * @throws RemoteException if there is a communication error during the remote method invocation.
     */
    public void addStopWords(List<String> words) throws RemoteException;

}