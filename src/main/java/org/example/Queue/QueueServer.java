package org.example.Queue;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class QueueServer {
    private static final String CONFIG_FILE = "src/main/java/org/example/Properties/queue.properties";
    private static final String STOP_WORDS_FILE = "stopwords.txt"; 

    public static void main(String[] args) {
        try {
            // Carregar configurações do ficheiro properties
            Properties prop = new Properties();
            try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
                prop.load(input);
            }

            String host = prop.getProperty("rmi.host", "localhost");
            int port = Integer.parseInt(prop.getProperty("rmi.port", "1111"));
            String serviceName = prop.getProperty("rmi.service_name", "QueueService");

            // Criar e exportar o registo RMI
            LocateRegistry.createRegistry(port);

            Set<String> stopwords = loadStopWords(STOP_WORDS_FILE);

            // Criar instância da implementação da fila
            IQueue queue = new QueueImp(stopwords);

            // Registar o objeto no RMI Registry
            String url = "rmi://" + host + ":" + port + "/" + serviceName;
            Naming.rebind(url, queue);

            System.out.println("Servidor RMI da Queue está pronto em " + url);
        } catch (IOException | NumberFormatException e) {
            System.err.println("Erro ao carregar configurações: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erro no servidor RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Load the stopwords from a file and return a Set of them
    private static Set<String> loadStopWords(String filePath) throws IOException {
        Set<String> stopwords = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopwords.add(line.trim());
            }
        }
        return stopwords;
    }
}
