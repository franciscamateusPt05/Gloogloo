package org.example.Barrel;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BarrelServer2 {
    private static final Logger logger = Logger.getLogger(BarrelServer.class.getName());

    public static void main(String[] args) {
        // Carregar as propriedades
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("src/main/java/org/example/Properties/barrel.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao carregar o arquivo de propriedades: " + e.getMessage());
            return;
        }

        try {
            // Registrar o Barrel 2
            logger.info("Criando o registro RMI na porta para o barrel 1...");
            LocateRegistry.createRegistry(Integer.parseInt(properties.getProperty("barrel2.rmi.port")));
            String barrel1Host = properties.getProperty("barrel1.rmi.host");
            String barrel1Port = properties.getProperty("barrel1.rmi.port");
            String barrel2Host = properties.getProperty("barrel2.rmi.host");
            String barrel2Port = properties.getProperty("barrel2.rmi.port");
            String barrel1ServiceName = properties.getProperty("barrel1.rmi.service_name");
            String barrel2ServiceName = properties.getProperty("barrel2.rmi.service_name");
            String barrel1RmiUrl = String.format("rmi://%s:%s/%s", barrel1Host, barrel1Port, barrel1ServiceName);
            String barrel2RmiUrl = String.format("rmi://%s:%s/%s", barrel2Host, barrel2Port, barrel2ServiceName);

            BarrelImpl barrelService2 = new BarrelImpl("barrel2", barrel1RmiUrl);
            Naming.rebind(barrel2RmiUrl, barrelService2);
            logger.info("Serviço Barrel 2 registrado em " + barrel2RmiUrl);

            System.out.println("BarrelService1 e BarrelService2 estão em execução...");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao registrar os serviços RMI: " + e.getMessage());
            e.printStackTrace();  // Imprimir o stack trace completo para mais detalhes.
        }
    }
}
