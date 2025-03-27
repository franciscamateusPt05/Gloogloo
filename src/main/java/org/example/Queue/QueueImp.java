package org.example.Queue;

import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;



public class QueueImp extends UnicastRemoteObject implements IQueue {
    private static final String CONFIG_FILE = "src/main/java/org/example/Properties/queue.properties";
    private static String FILE_NAME;
    private static int MAX_SIZE;
    private final Queue<String> queue;
    private final Set<String> stopwords;

    static {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            prop.load(input);
            FILE_NAME = prop.getProperty("queue.file", "queue.txt");
            MAX_SIZE = Integer.parseInt(prop.getProperty("queue.max_size", "100"));
        } catch (IOException | NumberFormatException e) {
            FILE_NAME = "queue.txt";
            MAX_SIZE = 100;
        }
    }

    protected QueueImp(Set<String> stopwords) throws RemoteException {
        super();
        this.queue = new LinkedList<>();
        this.stopwords = stopwords;
    }

    @Override
    public synchronized String getURL() throws RemoteException {
        loadQueueFromFile();
        while (queue.isEmpty()) {
            try {
                wait(); // Aguarda até que uma URL seja adicionada à fila
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura o estado de interrupção
                throw new RemoteException("Thread interrompida enquanto esperava por uma URL.", e);
            }
        }
        String url = queue.poll();
        saveQueueToFile();
        return url;
    }

    @Override
    public synchronized void addURL(String url) throws RemoteException {
        loadQueueFromFile();
        if (this.queue.size() < MAX_SIZE && !queue.contains(url)) {
            queue.add(url);
            saveQueueToFile();
            notifyAll(); // Notifica as threads que estão à espera de uma URL
        }
    }

    private void loadQueueFromFile() {
        queue.clear(); // Limpa a queue antes de carregar do ficheiro
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                queue.add(line);
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar a queue: " + e.getMessage());
        }
    }


    private void saveQueueToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (String url : queue) {
                writer.write(url);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erro ao guardar a queue: " + e.getMessage());
        }
    }

    public Set<String> getStopwords() {
        return stopwords;
    }
}
