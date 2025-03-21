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
     * @throws RemoteException Se ocorrer um erro na comunicação RMI.
     */
    void addToIndex(Map<String, Integer> words, String url, List<String> toUrls, String titulo, String citaçao) throws RemoteException;


        /**
         * Procura uma palavra no índice do Barrel e retorna uma lista de URLs associadas.
         *
         * @param word Palavra a procurar.
         * @return Lista de URLs onde a palavra foi encontrada.
         * @throws RemoteException Se ocorrer um erro na comunicação RMI.
         */
    List<String> search(String word) throws RemoteException;

}

