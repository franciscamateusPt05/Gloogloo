package org.example.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for receiving real-time statistics updates.
 */
public interface IStatistics extends Remote {
    void updateStatistics(SystemStatistics stats) throws RemoteException;
}