package org.example.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * <p>Remote interface for receiving real-time statistics updates from a remote server.
 * This interface allows clients to receive updates related to system statistics. 
 * It extends {@link Remote} to allow remote communication via RMI.</p>
 *
 * <p>Implementing classes should define the method for updating statistics, which can 
 * then be called by a remote server to notify clients of new or updated statistics.</p>
 * 
 * <p>The {@link #updateStatistics(SystemStatistics)} method is the primary mechanism 
 * for pushing statistics updates from the remote server to the client.</p>
 */
public interface IStatistics extends Remote {
    /**
     * <p>Updates the client with the latest system statistics.</p>
     *
     * <p>This method is called by the server to push the current statistics to the client. 
     * The {@code SystemStatistics} object contains the statistical data that will be displayed 
     * or processed by the client.</p>
     * 
     * @param stats the {@link SystemStatistics} object containing the updated statistics.
     * @throws RemoteException if there is an error during the RMI communication when receiving the statistics.
     */
    void updateStatistics(SystemStatistics stats) throws RemoteException;
}