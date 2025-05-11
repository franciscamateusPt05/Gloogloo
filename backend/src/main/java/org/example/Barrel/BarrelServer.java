package org.example.Barrel;

import org.example.common.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Initializes and registers the Barrel1 RMI server.
 * <p>
 * Loads configuration from property files, creates an RMI registry,
 * binds the Barrel1 service, and registers it with the Gateway.
 * </p>
 */
public class BarrelServer {

    private static final Logger logger = Logger.getLogger(Barrel2Server.class.getName());
    private static final String BARREL_CONFIG_FILE = "backend/src/main/java/org/example/Properties/barrel.properties";
    private static final String GATEWAY_CONFIG_FILE = "frontend/src/main/resources/gateway.properties";

    /**
     * Entry point for the Barrel1 RMI server.
     * <p>
     * Loads the barrel configuration, starts the RMI registry, binds the Barrel1 service,
     * and registers it with the gateway service.
     * </p>
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        // Carregar as propriedades
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(BARREL_CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao carregar o arquivo de propriedades: " + e.getMessage());
            return;
        }

        try {
            // Criar o registro RMI na porta padrão
            logger.info("Criando o registro RMI na porta para o barrel 1...");
            LocateRegistry.createRegistry(Integer.parseInt(properties.getProperty("barrel1.rmi.port")));

            // Registrar o Barrel 1
            String barrel1Host = properties.getProperty("barrel1.rmi.host");
            String barrel1Port = properties.getProperty("barrel1.rmi.port");

            String barrel1ServiceName = properties.getProperty("barrel1.rmi.service_name");
            String barrel1RmiUrl = String.format("rmi://%s:%s/%s", barrel1Host, barrel1Port, barrel1ServiceName);

            BarrelImpl barrelService1 = new BarrelImpl("barrel1");
            Naming.rebind(barrel1RmiUrl, barrelService1);

            Properties prop = loadProperties(GATEWAY_CONFIG_FILE);
            String gateawayURL= getRmiUrl(prop,"");

            IGateway gateaway= (IGateway) Naming.lookup(gateawayURL);
            gateaway.registarBarrel(barrel1RmiUrl);
            logger.info("Serviço Barrel 1 registrado em " + barrel1RmiUrl);
            System.out.println("BarrelService1 em executado com sucesso!");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao registrar os serviços RMI: " + e.getMessage());
            e.printStackTrace();  // Imprimir o stack trace completo para mais detalhes.
        }
    }

    /**
     * Loads properties from the specified file path.
     *
     * @param filePath the path to the properties file.
     * @return a {@link Properties} object containing the loaded properties.
     * @throws IOException if the file cannot be read or is empty.
     */
    private static Properties loadProperties(String filePath) throws IOException {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath)) {
            prop.load(input);
        }
        if (prop.isEmpty()) {
            throw new IOException("Properties file is empty or not loaded: " + filePath);
        }
        return prop;
    }

    /**
     * Constructs the RMI URL from the provided properties and optional prefix.
     *
     * @param prop   a {@link Properties} object containing RMI configuration properties.
     * @param prefix a string used for error messaging to indicate which service the config is for.
     * @return the constructed RMI URL as a {@link String}.
     * @throws IllegalArgumentException if any required RMI property is missing.
     */
    private static String getRmiUrl(Properties prop, String prefix) {
        String host = prop.getProperty( "rmi.host", "localhost");
        String port = prop.getProperty("rmi.port", "1112");
        String service = prop.getProperty("rmi.service_name", "QueueService");

        if (host == null || port == null || service == null) {
            throw new IllegalArgumentException("Missing RMI configuration for " + prefix);
        }
        return "rmi://" + host + ":" + port + "/" + service;
    }
}
