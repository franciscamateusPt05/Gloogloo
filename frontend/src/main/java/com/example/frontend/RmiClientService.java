package com.example.frontend;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.example.common.IStatistics;
import org.example.common.IGateway;
import org.example.common.SystemStatistics;

/**
 * <p>Service class responsible for connecting to the Gateway RMI server and receiving 
 * real-time system statistics updates. The class implements the {@link IStatistics} interface 
 * to handle statistics updates.</p>
 * 
 * <p>This service connects to a Gateway RMI server specified by the configuration file and 
 * registers for receiving updates on system statistics. It then uses WebSocket to send these 
 * updates to subscribed clients.</p>
 */
@Service
public class RmiClientService extends UnicastRemoteObject implements IStatistics {

    private static final long serialVersionUID = 1L;

    /**
     * The {@link SimpMessagingTemplate} instance used to send WebSocket messages.
     */
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final String CONFIG_FILE = "src/main/resources/gateway.properties";

    /**
     * RMI server host.
     */
    @Value("${rmi.host:localhost}")
    private String host;

    /**
     * RMI server port.
     */
    @Value("${rmi.port:1020}")
    private int port;

    /**
     * Name of the RMI service to look up in the registry.
     */
    @Value("${rmi.service_name:GatewayService}")
    private String serviceName;

    private IGateway gateway;

    /**
     * Default constructor.
     * 
     * @throws RemoteException if there is an issue with remote communication.
     */
    protected RmiClientService() throws RemoteException {
        super();
    }

    /**
     * <p>Initializes the connection to the RMI Gateway server upon bean initialization.</p>
     * <p>This method loads configuration settings, attempts to connect to the RMI registry, 
     * and registers this client to receive real-time statistics updates.</p>
     */
    @PostConstruct
    public void connect() {
        RmiClientService client = null;

        try {
            client = new RmiClientService();

            Properties prop = client.loadProperties();
            String host = prop.getProperty("rmi.host", "localhost");
            int port = Integer.parseInt(prop.getProperty("rmi.port", "1020"));
            String serviceName = prop.getProperty("rmi.service_name", "GatewayService");

            Registry registry = LocateRegistry.getRegistry(host, port);
            client.gateway = (IGateway) registry.lookup(serviceName);
            this.gateway = client.gateway;
            System.out.println("Connected to Gateway at rmi://" + host + ":" + port + "/" + serviceName);

            // Register the client to receive statistics updates
            this.gateway.registerStatisticsListener(this);
            System.out.println("Client registered for statistics updates.");

        } catch (IOException | NotBoundException e) {
            System.err.println("Failed to connect to Gateway: " + e.getMessage());
        }
    }

    /**
     * <p>Loads configuration properties from the specified file.</p>
     * 
     * @return the loaded properties.
     * @throws IOException if the configuration file cannot be loaded.
     */
    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        }
        return properties;
    }

    /**
     * <p>Receives system statistics updates from the Gateway and sends them to subscribed 
     * clients via WebSocket.</p>
     * 
     * @param stats the updated system statistics.
     * @throws RemoteException if there is a communication issue during the update.
     */
    @Override
    public synchronized void updateStatistics(SystemStatistics stats) throws RemoteException {
        try {
            String payload = stats.toString();  // Send as plain string
            messagingTemplate.convertAndSend("/topicGloogloo/statistics", payload);
        } catch (Exception e) {
            System.out.println("Error pushing statistics to WebSocket: " + e.getMessage());
        }
    }

    /**
     * Retrieves the {@link IGateway} instance for interacting with the Gateway service.
     * 
     * @return the {@link IGateway} instance.
     */
    public IGateway getGateway() {
        return gateway;
    }
}
