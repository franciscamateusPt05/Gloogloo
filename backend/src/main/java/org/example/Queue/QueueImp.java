package org.example.Queue;

import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;


/**
 * Implementation of the IQueue interface using file storage and Java RMI.
 * Handles URL queue management and stopword persistence.
 * 
 * Loads configuration from a properties file and manages persistence.
 * 
 */
public class QueueImp extends UnicastRemoteObject implements IQueue {
    private static final String CONFIG_FILE = "backend/src/main/java/org/example/Properties/queue.properties";
    private static String QUEUE_FILE;
    private static String STOPWORDS_FILE;
    private static int MAX_SIZE;
    private final Queue<String> queue;
    private final List<String> stopwords;

    /**
     * Loads configuration from properties file on class initialization.
     * Sets paths and limits for queue and stopwords.
     */
    static {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            prop.load(input);
            QUEUE_FILE = prop.getProperty("queue.file", "queue.txt");
            STOPWORDS_FILE = prop.getProperty("stopwords.file", "backend/stopwords.txt");
            MAX_SIZE = Integer.parseInt(prop.getProperty("queue.max_size"));
        } catch (IOException | NumberFormatException e) {
            QUEUE_FILE = "backend/queue.txt";
            STOPWORDS_FILE = "backend/stopwords.txt";
            MAX_SIZE = 100;
        }
        System.out.println(STOPWORDS_FILE);
    }

    /**
     * Constructs a new QueueImp instance with an empty queue and stopword list.
     * 
     * @throws RemoteException if RMI setup fails
     */
    protected QueueImp() throws RemoteException {
        super();
        this.queue = new LinkedList<>();
        this.stopwords = new ArrayList<>();
    }

    /**
     * Retrieves and removes the next URL from the queue.
     * Waits if the queue is empty.
     * 
     * @return the next URL
     * @throws RemoteException if interrupted or RMI error
     */
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

    /**
     * Adds a URL to the end of the queue, if not already present.
     * Removes the last element if max size is exceeded.
     * 
     * @param url the URL to add
     * @throws RemoteException if RMI error occurs
     */
    public synchronized void addURL(String url) throws RemoteException {
        loadQueueFromFile();
        if (!queue.contains(url)) {
            LinkedList<String> list = (LinkedList<String>) this.queue;
            if (list.size() >= MAX_SIZE) {
                list.removeLast();
            }
            list.addLast(url);
            saveQueueToFile();
            System.out.println("Adicionando novo link: " + url);
            notifyAll(); // Notifica as threads que estão à espera de um URL
        }
    }

    /**
     * Adds a URL to the front of the queue, if not already present.
     * Removes the last element if max size is exceeded.
     * 
     * @param url the URL to add to the front
     * @throws RemoteException if RMI error occurs
     */
    public synchronized void addFirst(String url) throws RemoteException {
        loadQueueFromFile();
        if (!queue.contains(url)) {
            LinkedList<String> list = (LinkedList<String>) this.queue;
            if (list.size() >= MAX_SIZE) {
                list.removeLast(); 
            }
            list.addFirst(url);
            saveQueueToFile();
            System.out.println("Adicionando novo link: " + url);
            notifyAll(); // Notifica as threads que estão à espera de um URL
        }
    }

    /**
     * Loads the current queue from the queue file.
     * Clears the existing in-memory queue first.
     */
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

    /**
     * Saves the current in-memory queue to the queue file.
     */
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

    /**
     * Adds new stopwords to the stopwords list if not already present.
     * Then saves them to file.
     * 
     * @param words the list of new stopwords
     * @throws RemoteException if RMI error occurs
     */
    public void addStopWords(List<String> words) throws RemoteException{
        loadStopWordsFromFile();
        for (String word : words) {
            if (!stopwords.contains(word)) {
                stopwords.add(word);
            }
        }
        saveStopWordsToFile();
    }

    /**
     * Loads the stopwords from the stopwords file.
     * Clears the existing in-memory list first.
     */
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

    /**
     * Saves the current in-memory stopwords list to the stopwords file.
     */
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

    /**
     * Returns the current list of stopwords after loading from file.
     * 
     * @return the list of stopwords
     */
    public List<String> getStopwords() {
        loadStopWordsFromFile();
        return stopwords;
    }
}
