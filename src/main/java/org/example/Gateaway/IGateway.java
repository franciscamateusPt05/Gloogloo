package org.example.Gateaway;

import org.example.SearchResult;
import org.example.URL;

import java.rmi.Remote;
import java.rmi.RemoteException;

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
    public void insertURL(URL url) throws RemoteException;

    /**
     * Searches for a specified terms and returns the searched results.
     *
     * @param search the search terms
     * @return the searched results as a SearchResult object
     * @throws RemoteException if a remote communication error occurs
     */
    public SearchResult getSearch(String search) throws RemoteException;

    /**
     * Searches for all URL connections of the given URL.
     * 
     * @param url The URL given to search for its connections
     * @return the search results as a SearchResult object
     * @throws RemoteException if a remote error occurs during the search
     */
    public SearchResult getConnections(URL url) throws RemoteException;

    //PROVAVELMENTE NECESSÁRIO: VERIFICAR
    //public void SubscribeBarrel(IBarrel barrel, String barrelUID) throws RemoteException;

    // Verificar como podemos fazer esta parte das estatísticas
    //public AdministrationPage getAdministrationPage() throws RemoteException;
}
