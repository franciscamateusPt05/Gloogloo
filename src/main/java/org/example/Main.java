package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;

import org.example.Gateaway.Gateway;

/**
 * The Main class serves as the entry point for the Gateway client.
 * It establishes an RMI connection with the Gateway service, provides
 * a user interface, and handles user input for interacting with the system.
 */
public class Main {

    /** Line separator used for menu display */
    private static final String LINE_BREAK = "=".repeat(30);

    /** To receive the Statistics from Gateway */
    private static final long serialVersionUID = 1L;

    /** Path to the configuration file */
    private static final String CONFIG_FILE = "src/main/java/org/example/Properties/gateway.properties";

    /** Scanner object for reading user input */
    private static Scanner scanner = new Scanner(System.in);

    /** Reference to the remote Gateway service */
    private static Gateway gateway;

    /**
     * The main method initializes the RMI connection and displays the user menu.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println(LINE_BREAK);
        System.out.println("Client connecting...");
        
        try {
            // Load configurations
            Properties prop = loadProperties();
            String host = prop.getProperty("rmi.host", "localhost");
            int port = Integer.parseInt(prop.getProperty("rmi.port", "1099"));
            String serviceName = prop.getProperty("rmi.service_name", "GatewayService");

            // Start or get existing RMI registry
            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(port);
                registry.list();
            } catch (RemoteException e) {
                registry = LocateRegistry.createRegistry(port);
            }

            // Initialize and bind the Gateway
            gateway = new Gateway();
            String url = "rmi://" + host + ":" + port + "/" + serviceName;
            registry.rebind(serviceName, gateway);

            System.out.println("RMI Gateway Server is ready on " + url);
        } catch (IOException e) {
            System.err.println("Failed to load properties file: " + e.getMessage());
        }

        // Start user interaction
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

    /**
     * Loads properties from the configuration file.
     *
     * @return A Properties object containing configuration values.
     * @throws IOException If the properties file cannot be loaded.
     */
    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        }
        return properties;
    }

    /**
     * Displays the main menu and returns the user's choice.
     *
     * @return The selected menu option.
     */
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

    /**
     * Handles the selected menu option by executing the corresponding action.
     *
     * @param option The selected menu option.
     * @throws RemoteException If an RMI error occurs.
     */
    private static void handleMenuOption(int option) throws RemoteException {
        switch (option) {
            case 1:
                System.out.println(LINE_BREAK + "\nInsert URL");
                insertURL();
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
                System.out.println(LINE_BREAK + "\nOpening administrative page:\n");
                System.out.println(gateway.getStatistics());
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    /**
     * Inserts a URL into the system.
     */
    private static void insertURL() {
        try {
            String url = readURL();
            if (url != null) {
                gateway.insertURL(url);
                System.out.println("URL inserted successfully into Gateway.");
            }
        } catch (RemoteException e) {
            System.err.println("Failed to insert URL: " + e.getMessage());
        }
    }

    /**
     * Performs a search based on user input.
     */
    private static void performSearch() {
        try {
            String searchQuery = readSearch();
            SearchResult result = gateway.getSearch(searchQuery);
            System.out.println("Search completed. Results: " + result);
        } catch (RemoteException e) {
            System.err.println("Search failed: " + e.getMessage());
        }
    }

    /**
     * Consults URL connections based on user input.
     */
    private static void consultURLConnections() {
        try {
            String url = readURL();
            if (url != null) {
                SearchResult result = gateway.getConnections(url);
                System.out.println("Connections retrieved: " + result);
            }
        }catch (RemoteException e) {
            System.err.println("Failed to retrieve connections: " + e.getMessage());
        }
    }

    /**
     * Reads a valid URL from user input.
     *
     * @return The validated URL string or null if input is invalid.
     */
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

    /**
     * Validates if the given URL follows a correct format.
     *
     * @param url The URL string to validate.
     * @return True if the URL is valid, false otherwise.
     */
    private static boolean isValidURL(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * Reads the user's search input.
     *
     * @return The search query entered by the user.
     */
    private static String readSearch() {
        try {
            System.out.print("Search: ");
            return scanner.nextLine();
        } catch (NoSuchElementException | IllegalStateException e) {
            return null;
        }
    }

    /**
     * Closes the scanner and exits the program.
     */
    private static void disconnect() {
        System.out.println(LINE_BREAK);
        scanner.close();
        System.out.println("Client disconnected successfully.");
        System.exit(0);
    }
}
