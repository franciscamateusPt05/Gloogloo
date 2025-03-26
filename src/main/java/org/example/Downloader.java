package org.example;

import org.example.Gateaway.IGateway;
import org.example.Queue.IQueue;
import org.example.Barrel.IBarrel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Downloader {
    private static final Logger logger = Logger.getLogger(Downloader.class.getName());
    private IQueue queue;
    private IGateway gateaway;
    private Map<String, IBarrel> barrels;

    // Variáveis para armazenar as URLs de configuração
    private String queueURL;
    private String gateawayURL;

    // Caminho do arquivo de propriedades
    private static final String GATEWAY_CONFIG_FILE = "src/main/java/org/example/Properties/gateway.properties";
    private static final String QUEUE_CONFIG_FILE = "src/main/java/org/example/Properties/queue.properties";
    private static final String STOP_WORDS_FILE = "stopwords.txt";

    private Set<String> stopWords;  // Set to store stop words
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public Downloader() {
        this.stopWords = new HashSet<>();
        conectar();
        startStopWordsUpdater();

    }

    // Método para carregar as propriedades a partir do arquivo config.properties
    private synchronized void loadProperties() {
        try {
            // Carregar as propriedades de queue-config.properties
            Properties queueProps = new Properties();
            try (FileInputStream input = new FileInputStream(QUEUE_CONFIG_FILE)) {
                queueProps.load(input);
            }
            // Obter as URLs dos serviços RMI da Queue
            String host = queueProps.getProperty("rmi.host");
            String port = queueProps.getProperty("rmi.port");
            String serviceName = queueProps.getProperty("rmi.service_name");
            this.queueURL = "rmi://" + host + ":" + port + "/" + serviceName;

            // Carregar as propriedades de gateway-config.properties
            Properties gateProps = new Properties();
            try (FileInputStream input = new FileInputStream(GATEWAY_CONFIG_FILE)) {
                gateProps.load(input);
            }
            host = gateProps.getProperty("rmi.host");
            port = gateProps.getProperty("rmi.port");
            serviceName = gateProps.getProperty("rmi.service_name");
            this.gateawayURL = "rmi://" + host + ":" + port + "/" + serviceName;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao carregar as propriedades: " + e.getMessage());
        }
    }

    public void run() {
        try {
            String url = queue.getURL();
            if (url != null) {
                processContent(url);
            } else {
                System.out.println("Fila vazia. Aguardando novas URLs...");
            }
            Thread.sleep(2000);
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Erro ao obter URL da fila: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Downloader foi interrompido.");
        }
    }

    private synchronized void processContent(String url) throws RemoteException {
        this.barrels=this.gateaway.getBarrels();
        Random random = new Random();
        List<String> keys = new ArrayList<>(this.barrels.keySet());
        if (keys.isEmpty()) {
            System.out.println("Nenhum barrel disponível.");
            return;
        }
        String randomKey = keys.get(random.nextInt(keys.size()));
        IBarrel barrelc = this.barrels.get(randomKey);

        if (!barrelc.containsUrl(url)) {
            try {
                System.out.println("Processando URL: " + url);
                Document doc = Jsoup.connect(url).get();
                String title = doc.title();
                String bodyText = doc.body().text();
                Map<String, Integer> palavras = normalizeWords(bodyText);
                String citacao = doc.select("p").first() != null ? doc.select("p").first().text() : title;
                List<String> listaLinks = doc.select("a[href]").eachAttr("abs:href").stream()
                        .filter(link -> link.startsWith("http") && !link.contains("#") && !link.contains("login"))
                        .distinct().toList();

                List<Thread> threads = new ArrayList<>();
                List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

                for (Map.Entry<String, IBarrel> entry : this.barrels.entrySet()) {
                    String chave = entry.getKey();
                    IBarrel barrel = entry.getValue();

                    Thread thread = new Thread(() -> {
                        try {
                            barrel.addToIndex(palavras, url, listaLinks, title, citacao);
                            synchronized (results) {
                                results.add(true);
                            }
                        } catch (Exception e) {
                            try {
                                this.gateaway.unregisterBarrel(chave);
                                System.out.println("Foi removido a URL: " + chave);
                            } catch (Exception ex) {
//                                ex.printStackTrace();
                            }
//                            e.printStackTrace();
                        }
                    });
                    threads.add(thread);
                    thread.start();
                }
                for (Thread thread : threads) {
                    thread.join();
                }

                if (!results.isEmpty() && results.stream().anyMatch(Boolean::booleanValue)) {
                    for (String link : listaLinks) {
                        String keyAleatoria = keys.get(random.nextInt(keys.size()));
                        IBarrel barrelAleatorio = this.barrels.get(keyAleatoria);
                        if (!barrelAleatorio.containsUrl(link)) {
                            this.queue.addURL(link);
                        }
                    }
                } else {
                    this.queue.addURL(url);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao processar a URL: " + e.getMessage());
            }
        }
    }

    private Map<String, Integer> normalizeWords(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        text = text.replaceAll("[\\p{Punct}]", "").toLowerCase();
        String[] palavras = text.split("\\s+");
        Map<String, Integer> frequenciaPalavras = new HashMap<>();

        for (String palavra : palavras) {
            if (!palavra.isBlank() && !stopWords.contains(palavra)) {
                frequenciaPalavras.put(palavra, frequenciaPalavras.getOrDefault(palavra, 0) + 1);
            }
        }
        return frequenciaPalavras;
    }

    // Start the periodic stop words updater
    private void startStopWordsUpdater() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Set<String> newStopWords = loadStopWords();
                if (newStopWords.isEmpty()) {
                    System.out.println("stopwords.txt está vazio. Aguardando 5 minutos...");
                } else {
                    stopWords = newStopWords;
                    System.out.println("Stop words atualizadas.");
                    sendStopWordsToDatabase();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Erro ao carregar stop words: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    // Load stop words from stopwords.txt
    private Set<String> loadStopWords() throws IOException {
        Set<String> words = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(STOP_WORDS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim());
            }
        }
        return words;
    }

    // Send stop words to the database (placeholder method)
    private void sendStopWordsToDatabase() {
        // Placeholder for your database logic to send the stop words.
        System.out.println("Sending stop words to database...");
    }

    public void conectar() {
        try {
            loadProperties();
            this.queue = (IQueue) Naming.lookup(queueURL);
            this.gateaway = (IGateway) Naming.lookup(gateawayURL);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao conectar: " + e.getMessage());
        }
    }


    private static volatile boolean running = true;
    // Main method remains unchanged
    public static void main(String[] args) throws InterruptedException {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCtrl + C detetado! A terminar...");
            running = false; // Altera a variável de controlo
        }));
//
//        try {
//            for (int i = 0; i < 5; i++) {
//                new Downloader().run();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        // Simulação de ciclo contínuo
        while (running) {
            System.out.println("O programa está a correr...");
            new Downloader().run();
        }

        System.out.println("Programa terminado.");
    }
}
