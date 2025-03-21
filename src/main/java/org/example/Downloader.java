package org.example;

import org.example.Queue.IQueue;
import org.example.Barrel.IBarrel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Downloader extends Thread {
    private static final Logger logger = Logger.getLogger(Downloader.class.getName());
    private IQueue queue;
    private IBarrel barrel1;
    private IBarrel barrel2;

    // Variáveis para armazenar as URLs de configuração
    private String queueURL;
    private String barrel1URL;
    private String barrel2URL;
    private String barrel1DbURL;
    private String barrel2DbURL;


    // Caminho do arquivo de propriedades
    private static final String CONFIG_FILE = "src/main/java/org/example/Properties/barrel.properties";
    private static final String QUEUE_CONFIG_FILE = "src/main/java/org/example/Properties/queue.properties";

    public Downloader() {
        try {
            // Carregar as propriedades do arquivo
            loadProperties();

            // Conectar à Queue usando a URL configurada no arquivo de propriedades
            this.queue = (IQueue) Naming.lookup(queueURL);

            // Conectar aos Barrels usando as URLs configuradas
            this.barrel1 = (IBarrel) Naming.lookup(barrel1URL);
            this.barrel2 = (IBarrel) Naming.lookup(barrel2URL);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao conectar à Queue ou aos Barrels: " + e.getMessage());
        }
    }

    // Método para carregar as propriedades a partir do arquivo config.properties
    private void loadProperties() {
        try {
            // Carregar as propriedades de barrel.properties
            Properties rmiProps = new Properties();
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                rmiProps.load(fis);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Erro ao carregar o arquivo de propriedades: " + e.getMessage());
                return;
            }


            // Obter URLs dos Barrels
            this.barrel1URL = "rmi://" + rmiProps.getProperty("barrel1.rmi.host") + ":" + rmiProps.getProperty("barrel1.rmi.port") + "/" + rmiProps.getProperty("barrel1.rmi.service_name");
            this.barrel2URL = "rmi://" + rmiProps.getProperty("barrel2.rmi.host") + ":" + rmiProps.getProperty("barrel2.rmi.port") + "/" + rmiProps.getProperty("barrel2.rmi.service_name");

            // Obter URLs dos Bancos de Dados dos Barrels
            this.barrel1DbURL = rmiProps.getProperty("barrel1.db.url");
            this.barrel2DbURL = rmiProps.getProperty("barrel2.db.url");

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



        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao carregar as propriedades: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        // Este método será executado quando a thread for iniciada
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Obter URL da fila
                String url = queue.getURL();
                if (url != null) {
                    // Processar a URL
                    System.out.println("Processando URL: " + url);

                    // Simulação do processamento
                    processContent(url);

                } else {
                    System.out.println("Fila vazia. Aguardando novas URLs...");
                }

                // Esperar um pouco antes de tentar obter uma nova URL
                Thread.sleep(2000); // Intervalo de 2 segundos
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
    private void processContent(String url) {
        try {
            // Conectar-se à URL e obter o conteúdo HTML com Jsoup
            Document doc = Jsoup.connect(url).get();

            // Extrair o título da página
            String title = doc.title();

            // Extrair e normalizar palavras-chave do texto da página
            String bodyText = doc.body().text();
            Map<String, Integer> keywords = normalizeWords(bodyText);

            // Extrair e filtrar citações válidas
            Elements citação = doc.select("blockquote, q"); // Pode incluir <q> para citações curtas


            // Extrair todas as URLs do conteúdo
            Elements links = doc.select("a[href]");


        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar a URL: " + e.getMessage());
        }
    }

    private Map<String, Integer> normalizeWords(String text) {
        // Remover pontuação e converter para minúsculas
        text = text.replaceAll("[^a-zA-Záéíóúãõâêôç]", " ").toLowerCase();

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

        public static void main (String[] args) {
            try {
                // Conectar à Queue usando RMI
                IQueue queue = (IQueue) Naming.lookup("rmi://localhost:1111/QueueService"); // Substitua localhost pelo endereço do servidor

                // Definir a URL que será processada
                String url = "http://www.amazon.es"; // Substitua por qualquer URL que deseja processar

                // Adicionar a URL à fila
                queue.addURL(url);
                System.out.println("URL adicionada à fila: " + url);

                // Criar e iniciar a thread do Downloader para processar a URL
                Downloader downloader1 = new Downloader();
                downloader1.start(); // Inicia a primeira thread para processar a URL

                // Criar outro Downloader para processamento paralelo
                Downloader downloader2 = new Downloader();
                downloader2.start(); // Inicia a segunda thread para processar a URL

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

