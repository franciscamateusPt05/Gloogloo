package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class Downloader extends Thread {
    private IQueue queue;

    public Downloader(IQueue queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Pede um URL à queue
                String url = queue.getURL();
                if (url == null) {
                    System.out.println("Downloader: Nenhum URL na queue. A terminar...");
                    break;
                }

                System.out.println("Downloader: Processando " + url);
                processURL(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processURL(String url) {
        try {
            // Fazer o download da página com JSoup
            Document doc = Jsoup.connect(url).get();
            String titulo = doc.title();
            String citacao = titulo; // Pode ser alterado para uma descrição mais específica

            // Guarda o URL nos barrels
            DatabaseManager.addURL(url, titulo, citacao);

            // Extrai e guarda as palavras
            String[] words = doc.text().toLowerCase().split("\\W+");
            for (String word : words) {
                if (word.length() > 2) { // Evita palavras muito curtas
                    DatabaseManager.addWord(word);
                    DatabaseManager.addWordURL(word, url);
                }
            }

            // Extrai e guarda os links encontrados
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String toURL = link.absUrl("href");
                if (!toURL.isEmpty()) {
                    DatabaseManager.addLink(url, toURL);
                    queue.addURL(toURL); // Adiciona novos URLs à queue para serem processados
                }
            }

            System.out.println("Downloader: Processado com sucesso -> " + url);

        } catch (Exception e) {
            System.out.println("Erro ao processar URL: " + url);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            // Inicia o servidor RMI da Queue
            IQueue queue = new QueueImpl();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("QueueService", queue);
            System.out.println("✅ Servidor RMI da Queue iniciado.");

            // Adiciona algumas URLs de teste
            queue.addURL("https://example.com");
            queue.addURL("https://google.com");
            queue.addURL("https://wikipedia.org");
            System.out.println("✅ URLs adicionadas à Queue.");

            // Criar e iniciar Downloaders (Threads)
            int numDownloaders = 3; // Quantidade de Downloaders
            for (int i = 0; i < numDownloaders; i++) {
                new Downloader(queue).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
