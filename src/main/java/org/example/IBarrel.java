package org.example;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IBarrel extends Remote {
    /**
     * Adiciona uma palavra ao índice do Barrel associada a uma URL.
     *
     * @param word Palavra a indexar.
     * @param url URL onde a palavra foi encontrada.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    void addToIndex(String word, String url) throws RemoteException;

    /**
     * Procura uma palavra no índice do Barrel e retorna uma lista de URLs associadas.
     *
     * @param word Palavra a procurar.
     * @return Lista de URLs onde a palavra foi encontrada.
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    List<String> search(String word) throws RemoteException;

}

