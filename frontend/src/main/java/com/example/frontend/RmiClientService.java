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

@Service
    class RmiClientService extends UnicastRemoteObject implements IStatistics  {

        protected RmiClientService() throws RemoteException {
        super();
    }

        @Autowired
        private SimpMessagingTemplate messagingTemplate; 

        private static final String CONFIG_FILE = "src/main/resources/gateway.properties";
        private static final long serialVersionUID = 1L;

        @Value("${rmi.host:localhost}")
        private String host;

        @Value("${rmi.port:1020}")
        private int port;

        @Value("${rmi.service_name:GatewayService}")
        private String serviceName;

        private IGateway gateway;

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
                client.gateway.registerStatisticsListener(client);
                System.out.println("Client registered for statistics updates.");

            } catch (IOException | NotBoundException e) {
                System.err.println("Failed to connect to Gateway: " + e.getMessage());
            }
        }

        private Properties loadProperties() throws IOException {
            Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                properties.load(fis);
            }
            return properties;
        }

        @Override
        public synchronized void updateStatistics(SystemStatistics stats) throws RemoteException {
            try {
                messagingTemplate.convertAndSend("/topicGloogloo/statistics", stats);
            } catch (Exception e) {
                System.out.println("Error pushing statistics to WebSocket: " + e.getMessage());
            }
        }

        public IGateway getGateway() {
            return gateway;
        }
}