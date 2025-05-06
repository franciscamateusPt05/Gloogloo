package org.example.Barrel;

import org.example.common.IGateway;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Barrel2Server {

    private static final Logger logger = Logger.getLogger(Barrel2Server.class.getName());
    private static final String BARREL_CONFIG_FILE = "backend/src/main/java/org/example/Properties/barrel.properties";
    private static final String GATEWAY_CONFIG_FILE = "frontend/src/main/resources/gateway.properties";


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
            LocateRegistry.createRegistry(Integer.parseInt(properties.getProperty("barrel2.rmi.port")));

            // Registrar o Barrel 1
            String barrel1Host = properties.getProperty("barrel2.rmi.host");
            String barrel1Port = properties.getProperty("barrel2.rmi.port");

            String barrel1ServiceName = properties.getProperty("barrel2.rmi.service_name");
            String barrel1RmiUrl = String.format("rmi://%s:%s/%s", barrel1Host, barrel1Port, barrel1ServiceName);

            BarrelImpl barrelService1 = new BarrelImpl("barrel2","backend/Barrels/Barrel2/barrel2");
            Naming.rebind(barrel1RmiUrl, barrelService1);

            Properties prop = loadProperties(GATEWAY_CONFIG_FILE);
            String gateawayURL= getRmiUrl(prop);

            IGateway gateaway= (IGateway) Naming.lookup(gateawayURL);
            gateaway.registarBarrel(barrel1RmiUrl);
            logger.info("Serviço Barrel 2 registrado em " + barrel1RmiUrl);
            System.out.println("BarrelService2 em executado com sucesso!");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao registrar os serviços RMI: " + e.getMessage());
            e.printStackTrace();  // Imprimir o stack trace completo para mais detalhes.
        }
    }
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

    private static String getRmiUrl(Properties prop) {
        String host = prop.getProperty( "rmi.host", "localhost");
        String port = prop.getProperty("rmi.port", "1112");
        String service = prop.getProperty("rmi.service_name", "QueueService");

        if (host == null || port == null || service == null) {
            throw new IllegalArgumentException("Missing RMI configuration");
        }
        return "rmi://" + host + ":" + port + "/" + service;
    }
}
