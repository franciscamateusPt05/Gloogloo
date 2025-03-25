package org.example.Gateaway;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import org.example.Barrel.*;
import org.example.Queue.*;
import org.example.Statistics.IStatistics;
import org.example.Statistics.SystemStatistics;
import org.example.SearchResult;

public class Gateway extends UnicastRemoteObject implements IGateway {

    private IBarrel selectedBarrel;
    private String selectedBarrelId; // To store the ID of the selected barrel (e.g., "barrel1", "barrel2")
    private IQueue queue;
    private Random random = new Random();

    private static final String GATEWAY_CONFIG_FILE = "src/main/java/org/example/Properties/gateway.properties";
    private static final String QUEUE_CONFIG_FILE = "src/main/java/org/example/Properties/queue.properties";
    private static final String BARREL_CONFIG_FILE = "src/main/java/org/example/Properties/barrel.properties";
    private static final long serialVersionUID = 1L;

    private final List<IStatistics> listeners = new ArrayList<>();
    private SystemStatistics currentStats;

    // To store active barrels with their ID
    private Map<String, IBarrel> activeBarrels = new HashMap<>();

    public Gateway() throws RemoteException {
        super();
        this.currentStats = new SystemStatistics(new ArrayList<>(), new HashMap<>(), new HashMap<>());
    }

    public static void main(String[] args) {
        try {
            Properties prop = new Properties();
            try (FileInputStream fis = new FileInputStream("src/main/java/org/example/Properties/gateway.properties")) {
                prop.load(fis);
            }

            String host = prop.getProperty("rmi.host", "localhost");
            int port = Integer.parseInt(prop.getProperty("rmi.port", "1099"));
            String serviceName = prop.getProperty("rmi.service_name", "GatewayService");

            Gateway gateway = new Gateway();

            // Start RMI Registry
            LocateRegistry.createRegistry(port);
            Naming.rebind("rmi://" + host + ":" + port + "/" + serviceName, gateway);
            System.out.println("✅ Gateway is running on rmi://" + host + ":" + port + "/" + serviceName);

            // Initialize services
            gateway.initialize();

        } catch (Exception e) {
            System.err.println("Failed to start Gateway: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private void initialize() {
        try {
            Properties queueProp = loadProperties(QUEUE_CONFIG_FILE);
            Properties barrelProp = loadProperties(BARREL_CONFIG_FILE);

            // Connect to Queue
            String queueUrl = getRmiUrl(queueProp, "queue");
            queue = (IQueue) Naming.lookup(queueUrl);
            System.out.println("Connected to Queue: " + queueUrl);

            // Check available barrels
            checkActiveBarrels(barrelProp); // Now properly handles IOException

        } catch (IOException e) {
            System.err.println("Error loading properties: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Properties loadProperties(String filePath) throws IOException {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath)) {
            prop.load(input);
        }
        if (prop.isEmpty()) {
            throw new IOException("Properties file is empty or not loaded: " + filePath);
        }
        return prop;
    }

    private String getRmiUrl(Properties prop, String prefix) {
        String host = prop.getProperty(prefix + ".rmi.host", "localhost");
        String port = prop.getProperty(prefix + ".rmi.port", "1112");
        String service = prop.getProperty(prefix + ".rmi.service_name", "QueueService");

        if (host == null || port == null || service == null) {
            throw new IllegalArgumentException("Missing RMI configuration for " + prefix);
        }
        return "rmi://" + host + ":" + port + "/" + service;
    }

    private void checkActiveBarrels(Properties prop) throws IOException {  // Declare throws IOException
        activeBarrels.clear(); // Clear previous active barrels

        for (int i = 1; i <= 2; i++) {
            String barrelPrefix = "barrel" + i;
            String barrelUrl = getRmiUrl(prop, barrelPrefix);

            try {
                IBarrel barrel = (IBarrel) Naming.lookup(barrelUrl);
                activeBarrels.put(barrelPrefix, barrel); // Store active barrel with its ID
                System.out.println("Connected to active barrel: " + barrelUrl);
            } catch (Exception e) {
                System.err.println("Barrel not available: " + barrelUrl);
            }
        }

        if (activeBarrels.isEmpty()) {
            System.err.println("No active barrels available!");
        } else {
            // Randomly select a barrel from the available active barrels
            selectRandomBarrel();
        }
    }

    private void selectRandomBarrel() {
        List<String> barrelIds = new ArrayList<>(activeBarrels.keySet());
        if (!barrelIds.isEmpty()) {
            selectedBarrelId = barrelIds.get(random.nextInt(barrelIds.size())); // Randomly select barrel ID
            selectedBarrel = activeBarrels.get(selectedBarrelId); // Get the selected barrel reference
            System.out.println("Connected to selected barrel: " + selectedBarrelId);
        }
    }

    public void insertURL(String url) throws RemoteException {
        if (queue == null) {
            throw new RemoteException("Queue service is not initialized.");
        }
        queue.addURL(url);
        System.out.println("URL inserted successfully into Queue");
    }

    public List<SearchResult> search(String search) throws RemoteException {
        if (selectedBarrel == null) {
            System.err.println("No barrel selected for search.");
            return new ArrayList<>();
        }

        try {
            // Dynamically update active barrels if necessary
            checkActiveBarrels(loadProperties(BARREL_CONFIG_FILE));  // Ensure barrel properties are reloaded

            List<SearchResult> results = selectedBarrel.search(search);
            if (results.isEmpty()) {
                System.out.println("[Gateway] No results found for: " + search);
            }
            return results;
        } catch (RemoteException e) {
            System.err.println("Error during search: " + e.getMessage());
            return new ArrayList<>();
        } catch (IOException e) {  // Handle IOException in search as well
            System.err.println("Error loading barrel properties during search: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public SearchResult getConnections(String url) throws RemoteException {
        if (selectedBarrel == null) {
            System.err.println("No barrel selected for getting connections.");
            return new SearchResult(url, new ArrayList<>());
        }

        try {
            // Dynamically update active barrels if necessary
            checkActiveBarrels(loadProperties(BARREL_CONFIG_FILE));  // Ensure barrel properties are reloaded

            return selectedBarrel.getConnections(url);
        } catch (RemoteException e) {
            System.err.println("Error getting connections: " + e.getMessage());
            return new SearchResult(url, new ArrayList<>());
        } catch (IOException e) {  // Handle IOException in getConnections
            System.err.println("Error loading barrel properties during getConnections: " + e.getMessage());
            return new SearchResult(url, new ArrayList<>());
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
