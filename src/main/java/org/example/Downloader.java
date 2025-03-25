package org.example;

import org.example.Gateaway.IGateway;
import org.example.Queue.IQueue;
import org.example.Barrel.IBarrel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Downloader extends Thread {
    private static final Logger logger = Logger.getLogger(Downloader.class.getName());
    private IQueue queue;
    private IGateway gateaway;
    private Map<String,IBarrel> barrels;

    // Variáveis para armazenar as URLs de configuração
    private String queueURL;
    private String gateawayURL;



    // Caminho do arquivo de propriedades
    private static final String GATEWAY_CONFIG_FILE = "src/main/java/org/example/Properties/gateway.properties";
    private static final String QUEUE_CONFIG_FILE = "src/main/java/org/example/Properties/queue.properties";

    public Downloader() {
        conectar();
        start();
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

            // Carregar as propriedades de queue-config.properties
            Properties gateProps = new Properties();
            try (FileInputStream input = new FileInputStream(GATEWAY_CONFIG_FILE)) {
                gateProps.load(input);
            }
            // Obter as URLs dos serviços RMI da Queue
            host = gateProps.getProperty("rmi.host");
            port = gateProps.getProperty("rmi.port");
            serviceName = gateProps.getProperty("rmi.service_name");
            this.gateawayURL = "rmi://" + host + ":" + port + "/" + serviceName;



        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao carregar as propriedades: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        // Este método será executado quando a thread for iniciada
        while (!Thread.currentThread().isInterrupted()) {
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
                break; // Se der erro ao tentar obter a URL, a thread pode ser interrompida
            } catch (InterruptedException e) {
                // A thread foi interrompida
                System.out.println("Downloader foi interrompido.");
                break;
            }
        }
    }

    // Método para processar o conteúdo da URL com Jsoup
    private synchronized void processContent(String url) throws RemoteException {
        Random random = new Random();

        // Selecionar um barrel aleatório
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

                // Obter o conteúdo HTML com Jsoup
                Document doc = Jsoup.connect(url).get();
                String title = doc.title();
                System.out.println(title);

                // Extrair e normalizar palavras-chave do texto
                String bodyText = doc.body().text();
                Map<String, Integer> palavras = normalizeWords(bodyText);

                // Extrair citação
                String citacao = doc.select("p").first() != null ? doc.select("p").first().text() : title;
                System.out.println(citacao);

                // Extrair URLs válidas
                List<String> listaLinks = doc.select("a[href]").eachAttr("abs:href").stream()
                        .filter(link -> link.startsWith("http") &&
                                !link.contains("#") &&
                                !link.contains("sessionid") &&
                                !link.contains("login") &&
                                !link.contains("404") &&
                                !link.contains("utm_"))
                        .distinct()
                        .toList();

                List<Thread> threads = new ArrayList<>();
                List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

                // Enviar os dados para cada barrel
                for (Map.Entry<String, IBarrel> entry : this.barrels.entrySet()) {
                    String chave = entry.getKey();
                    IBarrel barrel = entry.getValue();

                    Thread thread = new Thread(() -> {
                        try {
                            barrel.addToIndex(palavras, url, listaLinks, title, citacao);
                            synchronized (results) {
                                results.add(true);
                            }
                        } catch (RemoteException e) {
                            try {
                                this.gateaway.unregisterBarrel(chave);
                            } catch (RemoteException ex) {
                                throw new RuntimeException(ex);
                            }
                            e.printStackTrace();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    threads.add(thread);
                    thread.start();
                }

                // Esperar todas as threads terminarem
                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Adicionar os links à queue conforme os resultados
                if (!results.isEmpty() && results.stream().allMatch(Boolean::booleanValue)) {
                    for (String link : listaLinks) {
                        // Escolher um barrel aleatório para verificar a existência do URL
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
        } else {
            System.out.println("O " + url + " está na BD: " + barrelc.containsUrl(url));
        }
    }


    private Map<String, Integer> normalizeWords(String text) {
        // Remove acentuação e normaliza caracteres especiais
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        // Remove pontuação
        text = text.replaceAll("[\\p{Punct}]", "");

        // Converte para minúsculas
        text = text.toLowerCase();


        // Dividir o texto em palavras
        String[] palavras = text.split("\\s+");

        // Criar um mapa para armazenar a frequência das palavras
        Map<String, Integer> frequenciaPalavras = new HashMap<>();

        for (String palavra : palavras) {
            if (!palavra.isBlank()) { // Evitar palavras vazias
                frequenciaPalavras.put(palavra, frequenciaPalavras.getOrDefault(palavra, 0) + 1);
            }
        }
        return frequenciaPalavras;
    }

    public void conectar(){
        try {
            // Carregar as propriedades do arquivo
            loadProperties();

            this.queue = (IQueue) Naming.lookup(queueURL);

            this.gateaway = (IGateway) Naming.lookup(gateawayURL);


        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao conectar à Queue ou aos Barrels: " + e.getMessage());
        }
    }

    public static void main (String[] args) {
        try {
            for(int i=0;i<10;i++){
                new Downloader();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

