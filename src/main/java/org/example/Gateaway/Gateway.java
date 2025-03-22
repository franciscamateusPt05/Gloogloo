package org.example.Gateaway;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import org.example.Barrel.*;
import org.example.Queue.*;
import org.example.Statistics.IStatistics;
import org.example.Statistics.SystemStatistics;
import org.example.SearchResult;

/**
 * The Gateway class manages communication between clients, the Queue, and active Barrels.
 */
public class Gateway extends UnicastRemoteObject implements IGateway {

    /** Reference to the selected Barrel */
    private IBarrel selectedBarrel;

    /** Reference to the Queue service */
    private IQueue queue;

    /** Random instance for selecting barrels */
    private Random random = new Random();

    /** Paths to the properties files */
    private static final String QUEUE_CONFIG_FILE = "src/main/java/org/example/Properties/queue.properties";
    private static final String BARREL_CONFIG_FILE = "src/main/java/org/example/Properties/barrel.properties";

    private static final long serialVersionUID = 1L;
    private final List<IStatistics> listeners = new ArrayList<>();
    private SystemStatistics currentStats;

    /**
     * Constructs a new Gateway instance.
     *
     * @throws RemoteException If an RMI error occurs.
     */
    public Gateway() throws RemoteException {
        super();
        this.currentStats = new SystemStatistics(new ArrayList<>(), new HashMap<>(), new HashMap<>());
        try {
            // Load properties
            Properties queueProp = loadProperties(QUEUE_CONFIG_FILE);
            Properties barrelProp = loadProperties(BARREL_CONFIG_FILE);

            // Connect to Queue
            String queueUrl = getRmiUrl(queueProp, "queue");
            queue = (IQueue) Naming.lookup(queueUrl);
            System.out.println("Connected to Queue: " + queueUrl);

            // Check available barrels
            checkActiveBarrels(barrelProp);

        } catch (Exception e) {
            System.err.println("Failed to connect to Queue or Barrels: " + e.getMessage());
        }
    }

    /**
     * Loads properties from a file.
     *
     * @param filePath The path to the properties file.
     * @return A Properties object with the loaded values.
     * @throws IOException If an error occurs while reading the file.
     */
    private Properties loadProperties(String filePath) throws IOException {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath)) {
            prop.load(input);
        }
        return prop;
    }

    /**
     * Generates an RMI URL from properties for a given prefix.
     *
     * @param prop   The properties object.
     * @param prefix The prefix of the service (e.g., "barrel1", "queue").
     * @return The complete RMI URL.
     */
    private String getRmiUrl(Properties prop, String prefix) {
        String host = prop.getProperty(prefix + ".rmi.host", "localhost");
        String port = prop.getProperty(prefix + ".rmi.port");
        String service = prop.getProperty(prefix + ".rmi.service_name");
        return "rmi://" + host + ":" + port + "/" + service;
    }

    /**
     * Checks which barrels are active and connects to one.
     */
    private void checkActiveBarrels(Properties prop) {
        List<IBarrel> availableBarrels = new ArrayList<>();

        for (int i = 1; i <= 2; i++) {
            String barrelPrefix = "barrel" + i;
            String barrelUrl = getRmiUrl(prop, barrelPrefix);

            try {
                IBarrel barrel = (IBarrel) Naming.lookup(barrelUrl);
                availableBarrels.add(barrel);
                System.out.println("Connected to active barrel: " + barrelUrl);
            } catch (Exception e) {
                System.err.println("Barrel not available: " + barrelUrl);
            }
        }

        if (availableBarrels.isEmpty()) {
            System.err.println("No active barrels available!");
        } else {
            selectedBarrel = availableBarrels.get(random.nextInt(availableBarrels.size()));
            System.out.println("Connected to barrel: " + selectedBarrel);
        }
    }

    public void insertURL(String url) throws RemoteException {
        if (queue == null) {
            throw new RemoteException("Queue service is not initialized.");
        }
        queue.addURL(url);
        System.out.println("URL inserted!");
    }

    public List<SearchResult> search(String search) throws RemoteException {
        if (selectedBarrel == null) {
            System.err.println("No barrel selected for search.");
            return new ArrayList<>();
        }

        try {
            return selectedBarrel.search(search);
        } catch (RemoteException e) {
            System.err.println("Error during search: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public SearchResult getConnections(String url) throws RemoteException {
        if (selectedBarrel == null) {
            System.err.println("No barrel selected for getting connections.");
            return new SearchResult(url, List.of());
        }

        try {
            SearchResult connections = selectedBarrel.getConnections(url);
            return connections;
        } catch (RemoteException e) {
            System.err.println("Error getting connections: " + e.getMessage());
            return new SearchResult(url, List.of());
        }
    }

    public synchronized SystemStatistics getStatistics() throws RemoteException {
        return currentStats;
    }

    public synchronized void registerStatisticsListener(IStatistics listener) throws RemoteException {
        listeners.add(listener);
        listener.updateStatistics(currentStats);
    }

    public synchronized void updateStatistics(List<String> topSearches, HashMap<String, Integer> barrelSizes, HashMap<String, Double> responseTimes) throws RemoteException {
        this.currentStats = new SystemStatistics(topSearches, barrelSizes, responseTimes);
        notifyListeners();
    }

    private void notifyListeners() throws RemoteException {
        System.out.println("Notifying " + listeners.size() + " listeners.");
        for (IStatistics listener : listeners) {
            try {
                listener.updateStatistics(currentStats);
                System.out.println("Listener notified.");
            } catch (RemoteException e) {
                System.err.println("Failed to notify a listener: " + e.getMessage());
            }
        }
    }
}
