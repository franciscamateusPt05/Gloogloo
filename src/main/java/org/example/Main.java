package org.example;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.example.Gateaway.*;
import org.example.Statistics.IStatistics;
import org.example.Statistics.SystemStatistics;

public class Main extends UnicastRemoteObject implements IStatistics {

    private static final String LINE_BREAK = "=".repeat(30);
    private static final String CONFIG_FILE = "src/main/java/org/example/Properties/gateway.properties";
    private static final String STOP_WORDS_FILE = "stopwords.txt";
    private static Scanner scanner = new Scanner(System.in);
    private static IGateway gateway;
    private static SystemStatistics latestStats;  // Store the latest statistics

    private static int menuOption;
    
    public Main() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        System.out.println(LINE_BREAK);
        System.out.println("Client connecting...");

        try {
            Properties prop = loadProperties();
            String host = prop.getProperty("rmi.host", "localhost");
            int port = Integer.parseInt(prop.getProperty("rmi.port", "1099"));
            String serviceName = prop.getProperty("rmi.service_name", "GatewayService");

            Registry registry = LocateRegistry.getRegistry(host, port);
            gateway = (IGateway) registry.lookup(serviceName);
            System.out.println("Connected to Gateway at rmi://" + host + ":" + port + "/" + serviceName);

            Main client = new Main();
            gateway.registerStatisticsListener(client);  // Register the client to receive statistics updates
            System.out.println("Client registered for statistics updates.");

        } catch (IOException | NotBoundException e) {
            System.err.println("Failed to connect to Gateway: " + e.getMessage());
        }

        boolean running = true;
        while (running) {
            menuOption = showMenu();
            if (menuOption == 0) {
                disconnect();
                running = false;
                break;
            }

            try {
                handleMenuOption(menuOption);
            } catch (RemoteException e) {
                System.out.println("RMI error: " + e.getMessage());
            }
        }
    }

    private static void openAdministrativePage() {
        if (latestStats != null) {
            System.out.println("Current Statistics:");
            System.out.println("Top 10 Searches: " + latestStats.getTopSearches());
            System.out.println("Active Barrels and Their Sizes: " + latestStats.getBarrelIndexSizes());
            System.out.println("Average Response Times per Barrel: " + latestStats.getAverageResponseTimes());
        } else {
            System.out.println("No statistics available yet.");
        }

        System.out.println("\nPress any key to Exit Administrative Page");

        try {
            String input = null;
            
            // Check if there is more input available
            if (scanner.hasNextLine()) {
                input = scanner.nextLine().trim();
            }

            if (input != null) {
                System.out.println("\nExiting Administrative Page...");
            } else {
                System.out.println("\nNo input available. Exiting.");
            }
        } catch (NoSuchElementException | IllegalStateException e) {
            System.out.println("Error reading input: " + e.getMessage());
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        }
        return properties;
    }

    private static int showMenu() {
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

    private static void handleMenuOption(int option) throws RemoteException {
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
                openAdministrativePage();
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private static void insertURL() {
        try {
            String url = readURL();
            if (url != null) {
                gateway.insertURL(url);
            }
        } catch (RemoteException e) {
            System.err.println("Failed to insert URL: " + e.getMessage());
        }
    }

    private static void performSearch() {
        try {
            String searchQuery = readSearch();
            if (searchQuery == null || searchQuery.isEmpty()) {
                System.out.println("Search query cannot be empty.");
                return;
            }
            Set<String> stopwords = loadStopWords();

            String[] search;
            
            search = searchQuery.split(" ");

            String[] filteredSearch = new String[search.length];
            int filteredCount = 0;

            // Iterate over each word in the search query
            for (String word : search) {
            if (!stopwords.contains(word.trim())) {
            filteredSearch[filteredCount] = word.trim();
            filteredCount++;
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

                // Safe iteration to avoid IndexOutOfBounds
                for (int i = start; i < end && i < results.size(); i++) {
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
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }
    
    private static void consultURLConnections() {
        System.out.println("Input a URL or enter 0 to return:");
        String input = scanner.nextLine().trim();

        if (input.equals("0")) {
            return;  // Exit the method or return to the main menu
        }

        // Validate the URL input before processing
        if (isValidURL(input)) {
            try {
                // Process the URL input (e.g., search, retrieve data, etc.)
                System.out.println("Processing the URL: " + input);
                // Add your URL handling code here
            } catch (Exception e) {
                System.out.println("An error occurred while processing the URL.");
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid URL format. Please enter a valid URL.");
        }
    }

    private static String readURL() {
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

    // URL validation method (simple example)
    private static boolean isValidURL(String url) {
        String regex = "^(https?|ftp)://[^\s/$.?#].[^\s]*$";
        return url.matches(regex);
    }

    private static String readSearch() {
        try {
            System.out.print("Search terms: ");
            return scanner.nextLine();
        } catch (NoSuchElementException | IllegalStateException e) {
            return null;
        }
    }

    private static void disconnect() {
        System.out.println(LINE_BREAK);
        scanner.close();
        System.out.println("Client disconnected successfully.");
        System.exit(0);
    }

    @Override
    public void updateStatistics(SystemStatistics stats) throws RemoteException {
        latestStats = stats;
        
        if (menuOption == 4) { 
            System.out.println("\n--- Statistics Updated ---");
            openAdministrativePage(); 
        }
    }

    private static Set<String> loadStopWords() throws IOException {
        Set<String> words = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(STOP_WORDS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim());
            }
        }
        return words;
    }

}
