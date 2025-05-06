package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;

import org.example.common.*;

public class Main extends UnicastRemoteObject implements IStatistics {

    private static final String LINE_BREAK = "=".repeat(30);
    private static final String CONFIG_FILE = "frontend/src/main/resources/gateway.properties";

    private IGateway gateway;
    private boolean isAdminPageOpen = false;
    private Scanner scanner;

    private int menuOption;

    public Main() throws RemoteException {
        super();
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {

        System.out.println(LINE_BREAK);
        System.out.println("Client connecting...");

        Main client = null;

        try {
            client = new Main();

            Properties prop = client.loadProperties();
            String host = prop.getProperty("rmi.host", "localhost");
            int port = Integer.parseInt(prop.getProperty("rmi.port", "1099"));
            String serviceName = prop.getProperty("rmi.service_name", "GatewayService");

            Registry registry = LocateRegistry.getRegistry(host, port);
            client.gateway = (IGateway) registry.lookup(serviceName);
            System.out.println("Connected to Gateway at rmi://" + host + ":" + port + "/" + serviceName);

            // Register the client to receive statistics updates
            client.gateway.registerStatisticsListener(client);
            System.out.println("Client registered for statistics updates.");

        } catch (IOException | NotBoundException e) {
            System.err.println("Failed to connect to Gateway: " + e.getMessage());
        }

        boolean running = true;
        while (running) {
            client.menuOption = client.showMenu(); 
            if (client.menuOption == 0) {
                client.disconnect();
                running = false;
                break;
            }

            try {
                client.handleMenuOption(client.menuOption);
            } catch (RemoteException e) {
                System.out.println("RMI error: " + e.getMessage());
            }
        }
    }

    private void openAdministrativePage() {

        while (true) {
            try {
                SystemStatistics stats = gateway.getStatistics();  // Request stats from the Gateway
                if (stats != null) {
                    System.out.println(LINE_BREAK + "\nCurrent Statistics:");
                    System.out.println("Top 10 Searches: " + stats.getTopSearches());
                    System.out.println("Active Barrels and Their Sizes: " + stats.getBarrelIndexSizes());
                    System.out.println("Average Response Times per Barrel: " + stats.getAverageResponseTimes());
                } else {
                    System.out.println("No statistics available yet.");
                }
            } catch (RemoteException e) {
                System.out.println("Error fetching statistics: " + e.getMessage());
            }
    
            System.out.println("\nPress [0] to Exit Administrative Page\n" + LINE_BREAK);
            

            try {
                if (scanner.hasNextLine()) {
                    String input = scanner.nextLine().trim();
                    if (input.equals("0")) {
                        isAdminPageOpen = false;
                        System.out.println("Exiting Administrative Page...");
                        break;
                    }
                } else {
                    System.out.println("No input detected. Exiting.");
                    break;
                }
            } catch (NoSuchElementException | IllegalStateException e) {
                System.out.println("Input error: " + e.getMessage());
                break;
            }
        }
        isAdminPageOpen = false;
    }

    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        }
        return properties;
    }

    private int showMenu() {
        int returnValue = -1;

        while (returnValue == -1) {
            System.out.println(LINE_BREAK
                    + "\nMenu:\n"
                    + "[1] Insert URL\n"
                    + "[2] Search\n"
                    + "[3] Consult URL connections\n"
                    + "[4] Administrative page\n\n"
                    + "[0] Exit\n"
                    + LINE_BREAK);

            String reader = scanner.nextLine().trim();

            try {
                returnValue = Integer.parseInt(reader);
                if (returnValue < 0 || returnValue > 4) {
                    System.out.println("Invalid option. Please choose a valid option.");
                    returnValue = -1;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        return returnValue;
    }

    private void handleMenuOption(int option) throws RemoteException {
        switch (option) {
            case 1:
                System.out.println(LINE_BREAK + "\nInsert URL");
                insertURL();
                System.out.println("URL inserted into Queue");
                break;
            case 2:
                System.out.println(LINE_BREAK + "\nSearch");
                performSearch();
                break;
            case 3:
                System.out.println(LINE_BREAK + "\nConsult URL connections");
                consultURLConnections();
                break;
            case 4:
                System.out.println(LINE_BREAK + "\nOpening administrative page:");
                isAdminPageOpen = true;
                openAdministrativePage();
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private void insertURL() {
        try {
            String url = readURL();
    
            if (url != null) {
                while (true) {
                    System.out.println("The URL should be processed right now? [y/n]");
    
                    String input = scanner.nextLine().trim().toUpperCase();
    
                    if (input.equals("Y")) {
                        gateway.addFirst(url);
                        break;
                    } else if (input.equals("N")) {
                        gateway.insertURL(url);
                        break;
                    } else {
                        System.out.println("Invalid input. Please try again.");
                    }
                }
            }
        } catch (RemoteException e) {
            System.err.println("Failed to insert URL: " + e.getMessage());
        }
    }
    

    private void performSearch() {
        try {
            String searchQuery = readSearch();
            if (searchQuery == null || searchQuery.isEmpty()) {
                System.out.println("Search query cannot be empty.");
                return;
            }
            searchQuery = normalizeWords(searchQuery);

            List<String> stopwords = gateway.getStopwords();

            String[] search = searchQuery.trim().split("\\s+");

            ArrayList<String> filteredSearch = new ArrayList<>();

            // Iterate over each word in the search query
            for (String word : search) {
                if (!stopwords.contains(word.trim())) {
                    filteredSearch.add(word.trim());
                }
            }

            List<SearchResult> results = gateway.search(filteredSearch);

            if (results.isEmpty()) {
                System.out.println("No results found for: " + searchQuery);
                return;
            }

            int totalResults = results.size();
            int resultsPerPage = 10;
            int totalPages = (int) Math.ceil((double) totalResults / resultsPerPage);
            int currentPage = 1;

            while (true) {
                if (currentPage < 1 || currentPage > totalPages) {
                    System.out.println("No more pages available. Returning to menu.");
                    break;
                }

                int start = (currentPage - 1) * resultsPerPage;
                int end = Math.min(start + resultsPerPage, totalResults);

                System.out.println(LINE_BREAK);
                System.out.println("\nPage " + currentPage + " of " + totalPages + "\n");

                for (int i = start; i < Math.min(end, results.size()); i++) {
                    SearchResult result = results.get(i);
                    System.out.println("URL: " + result.getUrl());
                    System.out.println("Title: " + result.getTitle());
                    System.out.println("Snippet: " + result.getSnippet());
                    System.out.println(LINE_BREAK);
                }

                System.out.println("[P] Previous Page | [N] Next Page | [0] Return to Menu");
                String input = scanner.nextLine().trim().toUpperCase();

                if (input.equals("N")) {
                    if (currentPage < totalPages) {
                        currentPage++;
                    } else {
                        System.out.println("You are already on the last page.");
                    }
                } else if (input.equals("P")) {
                    if (currentPage > 1) {
                        currentPage--;
                    } else {
                        System.out.println("You are already on the first page.");
                    }
                } else if (input.equals("0")) {
                    break;
                } else {
                    System.out.println("Invalid input. Please try again.");
                }
            }

        } catch (RemoteException e) {
            System.err.println("Search failed: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void consultURLConnections() {
        System.out.println("Input a URL or enter 0 to return:");
        String input = scanner.nextLine().trim();

        if (input.equals("0")) {
            return;
        }

        // Validate the URL input before processing
        if (isValidURL(input)) {
            try {
                // Process the URL input (e.g., search, retrieve data, etc.)
                System.out.println("Processing the URL: " + input);
                
                SearchResult results = gateway.getConnections(input);
                List<String> urls = results.getUrls();

                if(urls.isEmpty())
                    System.out.println("Results not found");

                for (String url : urls) {
                    System.out.println(url);
                }

            } catch (Exception e) {
                System.out.println("An error occurred while processing the URL.");
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid URL format. Please enter a valid URL.");
        }
    }

    private String readURL() {
        try {
            System.out.println("Input a URL or enter 0 to return:");
            String input = scanner.nextLine().trim();

            if (input.equals("0"))
                return null;

            if (isValidURL(input))
                return input;
            else
                System.out.println("Invalid URL format. Please enter a valid URL.");
        } catch (Exception e) {
            return null;
        }
        return readURL();
    }

    private boolean isValidURL(String url) {
        String regex = "^(https?|ftp)://[^\s/$.?#].[^\s]*$";
        return url.matches(regex);
    }

    private String readSearch() {
        try {
            System.out.print("Search terms: ");
            String read = scanner.nextLine();
            return read;
        } catch (NoSuchElementException | IllegalStateException e) {
            return null;
        }
    }

    private void disconnect() {
        System.out.println(LINE_BREAK);
        System.out.println("Client disconnected successfully.");
        System.exit(0);
    }

    public synchronized void updateStatistics(SystemStatistics stats) throws RemoteException {

        if (isAdminPageOpen) { 
            try {
                stats = gateway.getStatistics();  // Request stats from the Gateway
                if (stats != null) {
                    System.out.println(LINE_BREAK + "\nCurrent Statistics:");
                    System.out.println("Top 10 Searches: " + stats.getTopSearches());
                    System.out.println("Active Barrels and Their Sizes: " + stats.getBarrelIndexSizes());
                    System.out.println("Average Response Times per Barrel: " + stats.getAverageResponseTimes());
                } else {
                    System.out.println("No statistics available yet.");
                }
            } catch (RemoteException e) {
                System.out.println("Error fetching statistics: " + e.getMessage());
            }
            System.out.println("\nPress [0] to Exit Administrative Page\n" + LINE_BREAK);
        }
    }

    private String normalizeWords(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        text = text.replaceAll("[\\p{Punct}]", "").toLowerCase();
        return text;
    }
}