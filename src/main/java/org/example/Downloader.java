package org.example;

import org.example.Queue.IQueue;
import org.example.Barrel.IBarrel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.text.Normalizer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Downloader extends Thread {
    private static final Logger logger = Logger.getLogger(Downloader.class.getName());
    private IQueue queue;
    private List<IBarrel> barrels;

    // Variáveis para armazenar as URLs de configuração
    private String queueURL;
    private String barrel1URL;
    private String barrel2URL;
    private String barrel1DbURL;
    private String barrel2DbURL;

    private boolean sucesso = false;


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
            IBarrel barrel1 = (IBarrel) Naming.lookup(barrel1URL);
            IBarrel barrel2 = (IBarrel) Naming.lookup(barrel2URL);

            barrel1.setOutrosBarrel(barrel2);
            barrel2.setOutrosBarrel(barrel1);

            barrels = Arrays.asList(barrel1, barrel2);

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
                String url = queue.getURL();
                if (url != null) {
                    System.out.println("Processando URL: " + url);
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
    private void processContent(String url) throws RemoteException {
        Random random = new Random();
        IBarrel barrelc = this.barrels.get(random.nextInt(2));
        if (!barrelc.containsUrl(url)) {
            try {
                // Conectar-se à URL e obter o conteúdo HTML com Jsoup
                Document doc = Jsoup.connect(url).get();

                // Extrair o título da página
                String title = doc.title();
                System.out.println(title);

                // Extrair e normalizar palavras-chave do texto da página
                String bodyText = doc.body().text();
                Map<String, Integer> palavras = normalizeWords(bodyText);


                String citacao = doc.select("p").first() != null ? doc.select("p").first().text() : doc.title();
                System.out.println(citacao);


                // Extrair todas as URLs do conteúdo
                List<String> listaLinks = doc.select("a[href]").eachAttr("abs:href").stream()
                        .distinct()
                        .filter(link -> link.startsWith("http") && // Garante que é um URL válido
                                !link.contains("#") && // Exclui links com fragmentos
                                !link.contains("sessionid") && // Exclui links com parâmetros de sessão
                                !link.contains("login") && // Exclui links de login
                                !link.contains("404") && // Exclui links de páginas de erro
                                !link.contains("utm_")) // Exclui links de rastreamento
                        .toList();


                for (IBarrel barrel : this.barrels) {
                    sucesso = barrel.addToIndex(palavras, url, listaLinks, title, citacao);
                }

                if (sucesso) {
                    for (String link : listaLinks) {
                        this.queue.addURL(link);
                    }
                }

                if (!sucesso) {this.queue.addURL(url);}

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao processar a URL: " + e.getMessage());
            }
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

    public static void main (String[] args) {
        try {

            // Criar e iniciar a thread do Downloader para processar a URL
            Downloader downloader1 = new Downloader();
            downloader1.start(); // Inicia a primeira thread para processar a URL


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

