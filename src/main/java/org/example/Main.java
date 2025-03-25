package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;

import org.example.Gateaway.*;
import org.example.Statistics.IStatistics;
import org.example.Statistics.SystemStatistics;

public class Main extends UnicastRemoteObject implements IStatistics {

    private static final String LINE_BREAK = "=".repeat(30);
    private static final String CONFIG_FILE = "src/main/java/org/example/Properties/gateway.properties";
    private static Scanner scanner = new Scanner(System.in);
    private static IGateway gateway;
    private static SystemStatistics latestStats;  // Store the latest statistics

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
            int menuOption = showMenu();
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

        // Display the latest statistics received from the Gateway
        if (latestStats != null) {
            System.out.println("Current Statistics:");
            System.out.println("Top Searches: " + latestStats.getTopSearches());
            System.out.println("Barrel Sizes: " + latestStats.getBarrelIndexSizes());
            System.out.println("Response Times: " + latestStats.getAverageResponseTimes());
        } else {
            System.out.println("No statistics available yet.");
        }

        System.out.println();
        System.out.println("[0] Exit Administrative Page");

        // Ask the user if they want to stay or exit
        String input = scanner.nextLine().trim();

        if (input.equals("0")) {
            System.out.println("Exiting Administrative Page...");
        } else {
            System.out.println("Invalid input. Please enter [0] to exit.");
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
                openAdministrativePage();  // Show statistics
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
            List<SearchResult> results = gateway.search(searchQuery);

            if (results.isEmpty()) {
                System.out.println("No results found for: " + searchQuery);
                return;
            }

            int totalResults = results.size();
            int currentPage = 1;
            int resultsPerPage = 10;
            int totalPages = (int) Math.ceil((double) totalResults / resultsPerPage);

            while (true) {
                int start = (currentPage - 1) * resultsPerPage;
                int end = Math.min(start + resultsPerPage, totalResults);

                System.out.println(LINE_BREAK);
                System.out.println("\nPage " + currentPage + " of " + totalPages + "\n");

                for (int i = start; i < end; i++) {
                    SearchResult result = results.get(i);
                    System.out.println("URL: " + result.getUrl());
                    System.out.println("Title: " + result.getTitle());
                    System.out.println("Snippet: " + result.getSnippet());
                    System.out.println(LINE_BREAK);
                }

                System.out.println(LINE_BREAK);
                System.out.println("[P] Previous Page | [N] Next Page | [0] Return to Menu");
                String input = scanner.nextLine().trim().toUpperCase();

                if (input.equals("N") && currentPage < totalPages) {
                    currentPage++;
                } else if (input.equals("P") && currentPage > 1) {
                    currentPage--;
                } else if (input.equals("0")) {
                    break;
                } else {
                    System.out.println("Invalid input. Please try again.");
                }
            }

        } catch (RemoteException e) {
            System.err.println("Search failed: " + e.getMessage());
        }
    }

    private static void consultURLConnections() {
        try {
            String url = readURL();
            if (url != null) {
                SearchResult result = gateway.getConnections(url);
                System.out.println("Connections retrieved: " + result);
            }
        } catch (RemoteException e) {
            System.err.println("Failed to retrieve connections: " + e.getMessage());
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

    private static boolean isValidURL(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
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
        // This method will be called by the Gateway when new statistics are available
        latestStats = stats;

        // Optionally, you could also call openAdministrativePage to show updated stats
        // openAdministrativePage(); // Automatically show updated stats
    }
}
