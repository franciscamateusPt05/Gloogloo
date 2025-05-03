package org.example.frontend;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.NotBoundException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@SpringBootApplication
public class FrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}

@Service
class RmiClientService implements IStatistics {

    private static final String CONFIG_FILE = "src/main/resources/gateway.properties";

    @Value("${rmi.host:localhost}")
    private String host;

    @Value("${rmi.port:1099}")
    private int port;

    @Value("${rmi.service_name:GatewayService}")
    private String serviceName;

    private IGateway gateway;

    public void connect() {
        try {
            Properties prop = loadProperties();
            Registry registry = LocateRegistry.getRegistry(host, port);
            gateway = (IGateway) registry.lookup(serviceName);
            System.out.println("Connected to Gateway at rmi://" + host + ":" + port + "/" + serviceName);

            // Register the client to receive statistics updates
            gateway.registerStatisticsListener(this);
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
}
