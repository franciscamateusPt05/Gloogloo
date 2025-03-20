package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;

public class Downloader extends Thread {
    private IQueue queueService;

    public Downloader(IQueue queueService) {
        this.queueService = queueService;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String url = queueService.getURL();
                if (url == null) {
                    System.out.println(Thread.currentThread().getName() + " - Nenhum URL disponível, a aguardar...");
                    Thread.sleep(2000);
                } else {
                    System.out.println(Thread.currentThread().getName() + " - Processando URL: " + url);
                    // Simular o download do URL
                    Thread.sleep(3000);
                    System.out.println(Thread.currentThread().getName() + " - Download concluído: " + url);
                }
            }
        } catch (RemoteException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Properties config = new Properties();

        // Carregar config.properties
        try (FileInputStream inputStream = new FileInputStream("config.properties")) {
            config.load(inputStream);
            System.out.println("✅ Configurações carregadas com sucesso.");
        } catch (IOException e) {
            System.err.println("❌ Erro ao carregar config.properties:");
            e.printStackTrace();
            return;
        }

        // Obter URL do serviço da Queue
        String queueUrl = config.getProperty("queue.rmi.url");
        if (queueUrl == null) {
            System.err.println("❌ Erro: queue.rmi.url não encontrado no config.properties.");
            return;
        }

        try {
            // Conectar ao serviço da Queue via RMI
            IQueue queueService = (IQueue) Naming.lookup(queueUrl);
            System.out.println("✅ Ligação RMI à Queue estabelecida com sucesso.");

            // Criar múltiplos Downloaders (threads) para processar URLs
            for (int i = 0; i < 5; i++) {
                new Downloader(queueService).start();
            }

        } catch (Exception e) {
            System.err.println("❌ Erro ao ligar ao serviço RMI da Queue:");
            e.printStackTrace();
        }
    }
}
