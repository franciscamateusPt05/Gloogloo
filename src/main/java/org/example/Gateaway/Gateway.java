package org.example.Gateaway;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import org.example.Barrel.*;
import org.example.Queue.*;
import org.example.Statistics.BarrelStats;
import org.example.Statistics.IStatistics;
import org.example.Statistics.SystemStatistics;
import org.example.SearchResult;

public class Gateway extends UnicastRemoteObject implements IGateway {

    private IBarrel selectedBarrel;
    private String selectedBarrelId;
    private IQueue queue;
    private Random random = new Random();

    private static final String GATEWAY_CONFIG_FILE = "src/main/java/org/example/Properties/gateway.properties";
    private static final String QUEUE_CONFIG_FILE = "src/main/java/org/example/Properties/queue.properties";
    private static final String BARREL_CONFIG_FILE = "src/main/java/org/example/Properties/barrel.properties";
    private static final long serialVersionUID = 1L;

    private final List<IStatistics> listeners = new ArrayList<>();
    private SystemStatistics currentStats;

    private Map<String, IBarrel> activeBarrels = new HashMap<>();
    private Map<String, BarrelStats> responseTimes = new HashMap<>();

    public Gateway() throws RemoteException {
        super();
        this.currentStats = new SystemStatistics(new ArrayList<>(), new HashMap<>(), new HashMap<>());
    }

    public static void main(String[] args) {
        try {
            Properties prop = new Properties();
            try (FileInputStream fis = new FileInputStream(GATEWAY_CONFIG_FILE)) {
                prop.load(fis);
            }

            String host = prop.getProperty("rmi.host", "localhost");
            int port = Integer.parseInt(prop.getProperty("rmi.port", "1099"));
            String serviceName = prop.getProperty("rmi.service_name", "GatewayService");

            Gateway gateway = new Gateway();

            LocateRegistry.createRegistry(port);
            Naming.rebind("rmi://" + host + ":" + port + "/" + serviceName, gateway);
            System.out.println("Gateway is running on rmi://" + host + ":" + port + "/" + serviceName);

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
            checkActiveBarrels(barrelProp); 

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

    private void checkActiveBarrels(Properties prop) throws IOException {
        activeBarrels.clear();
        for (int i = 1; i <= 2; i++) {
            String barrelPrefix = "barrel" + i;
            String barrelUrl = getRmiUrl(prop, barrelPrefix);

            try {
                IBarrel barrel = (IBarrel) Naming.lookup(barrelUrl);
                activeBarrels.put(barrelPrefix, barrel);
                responseTimes.put(barrelPrefix, new BarrelStats());  // Initialize BarrelStats for each barrel
                System.out.println("Connected to active barrel: " + barrelUrl);
            } catch (Exception e) {
                System.err.println("Barrel not available: " + barrelUrl);
            }
        }

        if (activeBarrels.isEmpty()) {
            System.err.println("No active barrels available!");
        } else {
            selectRandomBarrel();
        }
    }

    private void selectRandomBarrel() {
        List<String> barrelIds = new ArrayList<>(activeBarrels.keySet());
        if (!barrelIds.isEmpty()) {
            selectedBarrelId = barrelIds.get(random.nextInt(barrelIds.size()));
            selectedBarrel = activeBarrels.get(selectedBarrelId);
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
            System.out.println("[Gateway] No barrel selected. Attempting reconnection...");
            try {
                checkActiveBarrels(loadProperties(BARREL_CONFIG_FILE));
                if (selectedBarrel == null) {
                    System.err.println("[Gateway] No barrels available after reconnection attempt.");
                    return new ArrayList<>();
                }
            } catch (IOException e) {
                System.err.println("[Gateway] Error loading barrel properties: " + e.getMessage());
                return new ArrayList<>();
            }
        }
    
        try {
            long startTime = System.nanoTime();
            List<SearchResult> results = selectedBarrel.search(search);
            long endTime = System.nanoTime();
            double responseTime = (endTime - startTime) / 1_000_000.0; 
    
            // Update BarrelStats for the selected barrel
            BarrelStats stats = responseTimes.get(selectedBarrelId);
            stats.addResponseTime(responseTime);
    
            updateStatistics(search, responseTime);
    
            if (results.isEmpty()) {
                System.out.println("[Gateway] No results found for: " + search);
            }
            return results;
        } catch (RemoteException e) {
            System.err.println("[Gateway] Error during search: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    

    private void updateStatistics(String search, double responseTime) {
        List<String> topSearches = new ArrayList<>();
        HashMap<String, Double> response = new HashMap<>();
        response.put(selectedBarrelId, responseTime);

        for (IBarrel barrel : activeBarrels.values()) {
            try {
                List<String> barrelTopSearches = barrel.getTopSearches();
                topSearches.addAll(barrelTopSearches);
            } catch (RemoteException e) {
                System.err.println("Error fetching top searches from barrel: " + e.getMessage());
            }
        }

        // Add average response times for each barrel
        HashMap<String, Double> averageResponseTimes = new HashMap<>();
        for (Map.Entry<String, BarrelStats> entry : responseTimes.entrySet()) {
            String barrelId = entry.getKey();
            BarrelStats stats = entry.getValue();
            averageResponseTimes.put(barrelId, stats.getAverageResponseTime());
        }

        currentStats.setTopSearches(topSearches);
        currentStats.setResponseTimes(averageResponseTimes);

        // Now notify all registered listeners about the updated statistics
        try {
            notifyListeners(); // This sends the updated stats to all listeners
        } catch (RemoteException e) {
            System.err.println("Failed to notify listeners: " + e.getMessage());
        }
    }

    public synchronized SystemStatistics getStatistics() throws RemoteException {
        return currentStats;
    }

    @Override
    public SearchResult getConnections(String url) throws RemoteException {
        if (selectedBarrel == null) {
            System.out.println("[Gateway] No barrel selected. Attempting reconnection...");
            try {
                checkActiveBarrels(loadProperties(BARREL_CONFIG_FILE));
                if (selectedBarrel == null) {
                    System.err.println("[Gateway] No barrels available after reconnection attempt.");
                    return new SearchResult("No barrel available", Collections.emptyList());
                }
            } catch (IOException e) {
                System.err.println("[Gateway] Error loading barrel properties: " + e.getMessage());
                return new SearchResult("Error loading barrel properties", Collections.emptyList());
            }
        }

        try {
            long startTime = System.nanoTime();
            SearchResult result = selectedBarrel.getConnections(url);
            long endTime = System.nanoTime();
            double responseTime = (endTime - startTime) / 1_000_000.0; 

            // Update BarrelStats for the selected barrel
            BarrelStats stats = responseTimes.get(selectedBarrelId);
            stats.addResponseTime(responseTime);

            updateStatistics(url, responseTime);

            if (result == null || result.getUrls().isEmpty()) {
                return new SearchResult(url, Collections.emptyList());
            }
            return result;
        } catch (RemoteException e) {
            System.err.println("[Gateway] Error during getConnections: " + e.getMessage());
            return new SearchResult("Error occurred for: " + url, Collections.emptyList());
        } catch (@SuppressWarnings("hiding") IOException e) {
            e.printStackTrace();
            return new SearchResult("Error occurred for: " + url, Collections.emptyList());
        }
    }


    public synchronized void registerStatisticsListener(IStatistics listener) throws RemoteException {
        listeners.add(listener);
        System.out.println("Client registered for statistics updates.");
    }

    private void notifyListeners() throws RemoteException {
        for (IStatistics listener : listeners) {
            try {
                listener.updateStatistics(currentStats);
            } catch (RemoteException e) {
                System.err.println("Failed to notify a listener: " + e.getMessage());
            }
        }
    }

    public void registarBarrel(String rmi) throws RemoteException {
        try {
            IBarrel barrel = (IBarrel) Naming.lookup(rmi);
            this.activeBarrels.put(rmi, barrel);
            System.out.println("Barrel registado com sucesso: " + rmi);
        } catch (NotBoundException | MalformedURLException e) {
            System.err.println("Erro ao registar o Barrel: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void unregisterBarrel(String rmi) throws RemoteException {
        if (this.activeBarrels.remove(rmi) != null) {
            System.out.println("Barrel removido com sucesso: " + rmi);
        } else {
            System.err.println("Erro: Barrel não encontrado para remoção: " + rmi);
        }
    }

    public Map<String, IBarrel> getBarrels() throws RemoteException{
        return new HashMap<>(this.activeBarrels);
    }
}
