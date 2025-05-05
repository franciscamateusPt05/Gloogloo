package org.example.Queue;

import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;



public class QueueImp extends UnicastRemoteObject implements IQueue {
    private static final String CONFIG_FILE = "backend/src/main/java/org/example/Properties/queue.properties";
    private static String QUEUE_FILE;
    private static String STOPWORDS_FILE;
    private static int MAX_SIZE;
    private final Queue<String> queue;
    private final List<String> stopwords;

    static {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            prop.load(input);
            QUEUE_FILE = prop.getProperty("queue.file", "queue.txt");
            STOPWORDS_FILE = prop.getProperty("stopwords.file", "stopwords.txt");
            MAX_SIZE = Integer.parseInt(prop.getProperty("queue.max_size", "100"));
        } catch (IOException | NumberFormatException e) {
            QUEUE_FILE = "queue.txt";
            STOPWORDS_FILE = "stopwords.txt";
            MAX_SIZE = 100;
        }
    }

    protected QueueImp() throws RemoteException {
        super();
        this.queue = new LinkedList<>();
        this.stopwords = new ArrayList<>();
    }

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

    public synchronized void addURL(String url) throws RemoteException {
        loadQueueFromFile();
        if (!queue.contains(url)) {
            LinkedList<String> list = (LinkedList<String>) this.queue;
            if (list.size() >= MAX_SIZE) {
                list.removeLast();
            }
            list.addLast(url);
            saveQueueToFile();
            notifyAll(); // Notifica as threads que estão à espera de um URL
        }
    }

    public synchronized void addFirst(String url) throws RemoteException {
        loadQueueFromFile();
        if (!queue.contains(url)) {
            LinkedList<String> list = (LinkedList<String>) this.queue;
            if (list.size() >= MAX_SIZE) {
                list.removeLast(); 
            }
            list.addFirst(url);
            saveQueueToFile();
            notifyAll(); // Notifica as threads que estão à espera de um URL
        }
    }


    private void loadQueueFromFile() {
        queue.clear(); // Limpa a queue antes de carregar do ficheiro
        try (BufferedReader reader = new BufferedReader(new FileReader(QUEUE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                queue.add(line);
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar a queue: " + e.getMessage());
        }
    }

    private void saveQueueToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(QUEUE_FILE))) {
            for (String url : queue) {
                writer.write(url);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erro ao guardar a queue: " + e.getMessage());
        }
    }

    public void addStopWords(List<String> words) throws RemoteException{
        loadStopWordsFromFile();
        for (String word : words) {
            if (!stopwords.contains(word)) {
                stopwords.add(word);
            }
        }
        saveStopWordsToFile();
    }

    public void loadStopWordsFromFile() {
        stopwords.clear(); // Limpa as stop words antes de carregar do ficheiro
        try (BufferedReader reader = new BufferedReader(new FileReader(STOPWORDS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopwords.add(line);
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar as stopwords: " + e.getMessage());
        }
    }

    public void saveStopWordsToFile(){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(STOPWORDS_FILE))) {
            for (String word : stopwords) {
                writer.write(word);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erro ao guardar as stop words: " + e.getMessage());
        }
    }

    public List<String> getStopwords() {
        loadStopWordsFromFile();
        return stopwords;
    }
}
