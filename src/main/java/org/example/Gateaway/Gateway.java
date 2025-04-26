package org.example.Gateaway;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private List<IStatistics> listeners = new ArrayList<>();
    private SystemStatistics currentStats;

    private Map<String, IBarrel> activeBarrels = new HashMap<>();
    private Map<String, BarrelStats> responseTimes = new HashMap<>();

    private final AtomicBoolean isSynchronizing = new AtomicBoolean(false);

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
            System.out.println("Queue URL: " + queueUrl);
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
        int i = 1;

        while (true) {
            String barrelPrefix = "barrel" + i;
            String host = prop.getProperty(barrelPrefix + ".rmi.host");
            String port = prop.getProperty(barrelPrefix + ".rmi.port");
            String serviceName = prop.getProperty(barrelPrefix + ".rmi.service_name");

            // No more barrels defined in properties
            if (host == null || port == null || serviceName == null) {
                break;
            }

            String barrelUrl = String.format("rmi://%s:%s/%s", host, port, serviceName);

            try {
                IBarrel barrel = (IBarrel) Naming.lookup(barrelUrl);
                activeBarrels.put(barrelPrefix, barrel);

                // Initialize stats if not already
                responseTimes.putIfAbsent(barrelPrefix, new BarrelStats());

                System.out.println("Connected to active barrel: " + barrelUrl);
            } catch (Exception e) {
                System.err.println("Could not connect to barrel at: " + barrelUrl);
                System.err.println("Reason: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }

            i++;
        }

        if (activeBarrels.isEmpty()) {
            System.err.println("No active barrels available!");
        } else {
            selectRandomBarrel(); // pick one from available ones
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

    public void addFirst(String url) throws RemoteException{
        if (queue == null) {
            throw new RemoteException("Queue service is not initialized.");
        }
        queue.addFirst(url);
        System.out.println("URL inserted successfully into Queue");
    }

    public List<SearchResult> search(ArrayList<String> search) throws RemoteException {
        // Refresh barrels and select a random barrel every time a search is performed
        refreshAndSelectBarrel();

        if (selectedBarrel == null) {
            System.out.println("[Gateway] No barrel available after refreshing.");
            return new ArrayList<>();
        }

        List<SearchResult> results = new ArrayList<>();
        boolean searchSucceeded = false;

        // update top words in database
        for (Map.Entry<String, IBarrel> barrelEntry : activeBarrels.entrySet()) {
            IBarrel barrel = barrelEntry.getValue();

            barrel.updateTopWords(search);
        }

        // Try searching with all active barrels
        for (Map.Entry<String, IBarrel> barrelEntry : activeBarrels.entrySet()) {
            String barrelId = barrelEntry.getKey();
            IBarrel barrel = barrelEntry.getValue();

            System.out.println("[Gateway] Trying search on barrel: " + barrelId);

            try {
                long startTime = System.nanoTime();
                results = barrel.search(search);
                long endTime = System.nanoTime();
                double responseTime = (endTime - startTime) / 1_000_000.0;
                responseTime = (double) Math.round(responseTime * 100) / 100;

                // Update BarrelStats for the current barrel
                BarrelStats stats = responseTimes.get(barrelId);
                stats.addResponseTime(responseTime);

                updateStatistics(search, responseTime);

                if (!results.isEmpty()) {
                    selectedBarrel = barrel;
                    selectedBarrelId = barrelId;
                    searchSucceeded = true;
                    break;
                }
            } catch (IOException e) {
                System.err.println("[Gateway] Error during search on barrel " + barrelId + ": " + e.getMessage());
            }
        }

        if (!searchSucceeded) {
            System.err.println("[Gateway] All barrels failed for search: " + search);
        }

        return results;
    }

    // Refresh active barrels and select a random barrel
    private void refreshAndSelectBarrel() {
        System.out.println("[Gateway] Refreshing active barrels...");
        try {
            checkActiveBarrels(loadProperties(BARREL_CONFIG_FILE));
            if (selectedBarrel == null) {
                System.err.println("[Gateway] No active barrels available.");
            } else {
                selectRandomBarrel();
            }
        } catch (IOException e) {
            System.err.println("[Gateway] Error refreshing active barrels: " + e.getMessage());
        }
    }

    private List<String> getTopSearches() {
        if (selectedBarrel == null || selectedBarrelId == null) {
            System.out.println("[Gateway] No selected barrel available.");
            return new ArrayList<>();
        }

        try {
            long startTime = System.nanoTime();
            List<String> topSearches = selectedBarrel.getTopSearches();
            long endTime = System.nanoTime();
            double responseTime = (endTime - startTime) / 1_000_000.0;
            responseTime = (double) Math.round(responseTime * 100) / 100;

            BarrelStats stats = responseTimes.get(selectedBarrelId);
            if (stats != null) {
                stats.addResponseTime(responseTime);
            }

            return topSearches != null ? topSearches : new ArrayList<>();

        } catch (IOException e) {
            System.err.println("[Gateway] Error during getTopSearches on selected barrel " + selectedBarrelId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }


    private void updateStatistics(ArrayList<String> search, double responseTime) {
        List<String> topSearches = new ArrayList<>();
        HashMap<String, Double> response = new HashMap<>();
        HashMap<String, Integer> barrelSizes = new HashMap<>();

        response.put(selectedBarrelId, responseTime);
        topSearches = getTopSearches();

        for (Map.Entry<String, IBarrel> barrelEntry : activeBarrels.entrySet()) {
            String barrelId = barrelEntry.getKey();
            IBarrel barrel = barrelEntry.getValue();

            try {
                // Query the real barrel size dynamically from the barrel itself
                int barrelSize = barrel.getSize(); // Assuming IBarrel has a method getBarrelSize
                barrelSizes.put(barrelId, barrelSize);
            } catch (RemoteException e) {
                System.err.println("[Gateway] Failed to get size for barrel: " + barrelId);
                barrelSizes.put(barrelId, 0);  // Default to 0 if error occurs
            }
        }

        // Add average response times to the statistics
        HashMap<String, Double> averageResponseTimes = new HashMap<>();
        for (Map.Entry<String, BarrelStats> entry : responseTimes.entrySet()) {
            String barrelId = entry.getKey();
            BarrelStats stats = entry.getValue();
            averageResponseTimes.put(barrelId, stats.getAverageResponseTime());
        }

        // Set the updated data to currentStats
        currentStats.setTopSearches(topSearches);
        currentStats.setResponseTimes(averageResponseTimes);
        currentStats.setBarrelIndexSizes(barrelSizes);  // Add barrel sizes

        // Notify listeners after updating
        try {
            notifyListeners();  // Notify clients of the updated stats
        } catch (RemoteException e) {
            System.err.println("Failed to notify listeners: " + e.getMessage());
        }
    }


    public synchronized SystemStatistics getStatistics() throws RemoteException {
        HashMap<String, Integer> barrelSizes = new HashMap<>();
        for (Map.Entry<String, IBarrel> barrelEntry : activeBarrels.entrySet()) {
            String barrelId = barrelEntry.getKey();
            IBarrel barrel = barrelEntry.getValue();

            try {
                // Query the real barrel size dynamically from the barrel itself
                int barrelSize = barrel.getSize(); // Assuming IBarrel has a method getBarrelSize
                barrelSizes.put(barrelId, barrelSize);
            } catch (RemoteException e) {
                System.err.println("[Gateway] Failed to get size for barrel: " + barrelId);
                barrelSizes.put(barrelId, 0);  // Default to 0 if error occurs
            }
        }
        this.currentStats.setBarrelIndexSizes(barrelSizes);
        return currentStats;
    }

    @Override
    public SearchResult getConnections(String url) throws RemoteException {
        refreshAndSelectBarrel(); // Refresh barrels and select one randomly

        if (selectedBarrel == null) {
            System.out.println("[Gateway] No barrel available after refreshing.");
            return new SearchResult("No barrel available", Collections.emptyList());
        }

        try {
            long startTime = System.nanoTime();
            SearchResult result = selectedBarrel.getConnections(url);
            long endTime = System.nanoTime();
            double responseTime = (endTime - startTime) / 1_000_000.0;
            responseTime = (double) Math.round(responseTime * 100) / 100;

            BarrelStats stats = responseTimes.get(selectedBarrelId);
            stats.addResponseTime(responseTime);

            ArrayList<String> url_ = new ArrayList<>();
            url_.add(url);

            updateStatistics(url_, responseTime);

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
        Iterator<IStatistics> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            IStatistics listener = iterator.next();
            try {
                listener.updateStatistics(currentStats);
            } catch (RemoteException e) {
                System.err.println("Failed to notify a listener: " + e.getMessage());
                iterator.remove();
            }
        }
    }

    public synchronized void broadcastStatistics(SystemStatistics stats) throws RemoteException {
        for (IStatistics listener : listeners) {
            try {
                listener.updateStatistics(stats);
            } catch (RemoteException e) {
                System.err.println("Failed to notify a listener: " + e.getMessage());
            }
        }
    }

    public void registarBarrel(String rmi) throws RemoteException {
        try {
            try{
                unregisterBarrel(rmi);
            } catch (RemoteException e) {
                System.err.println("Failed to unregister barrel: " + e.getMessage());
            }

            IBarrel barrel = (IBarrel) Naming.lookup(rmi);
            IBarrel sinc = null;

            System.out.println(activeBarrels.size());

            if (!activeBarrels.isEmpty()) {

                for (String chave : activeBarrels.keySet()) {
                    IBarrel sincc = activeBarrels.get(chave);
                    sinc = sincc;
                    break; // Remove este break se quiser processar todos os barrels
                }
                isSynchronizing.set(true);
                sincronizar(sinc.getFicheiro(), barrel.getFicheiro());
                isSynchronizing.set(false);
            }
            barrel.connect();

            this.activeBarrels.put(rmi, barrel);
            System.out.println("Barrel registado com sucesso: " + rmi);

        } catch (NotBoundException e) {
            System.err.println("Erro ao registar o Barrel: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
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

    public static void sincronizar(String caminhoOrigem, String caminhoDestino) {
        File bancoOrigem = new File(caminhoOrigem);
        File bancoDestino = new File(caminhoDestino);

        try {
            // Verifica se o banco de dados de destino existe e tenta removê-lo
            if (bancoDestino.exists()) {
                if (!bancoDestino.delete()) {
                    System.err.println("Não foi possível remover o banco de dados de destino existente.");
                    return;
                }
            }

            // Copia o banco de dados de origem para o destino, substituindo-o se necessário
            Files.copy(bancoOrigem.toPath(), bancoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Thread.sleep(5000);
            System.out.println("Banco de dados copiado com sucesso!");
        } catch (IOException e) {
            System.err.println("Erro ao copiar o banco de dados: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Erro ao copiar o banco de dados: " + e.getMessage());
        }
    }

    public Map<String, IBarrel> getBarrels() throws RemoteException{
        return new HashMap<>(this.activeBarrels);
    }

    public List<String> getStopwords() throws RemoteException {
        if (queue != null) {
            return queue.getStopwords();
        }
        throw new RemoteException("QueueServer not connected.");
    }

    public boolean isFlag() throws RemoteException {
        return isSynchronizing.get();
    }
}

