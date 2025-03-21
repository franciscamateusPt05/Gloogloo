package org.example.Gateaway;

import org.example.Queue.IQueue;
import org.example.SearchResult;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

public class Gateway extends UnicastRemoteObject implements IGateway {
    private static final String CONFIG_FILE = "config.properties";
    
    private static IQueue queue;
    private static IGateway gateway;

    protected Gateway() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        try {
            Properties properties = loadProperties();
            gateway = new Gateway();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("GATEWAY", gateway);
            queue = (IQueue) Naming.lookup(properties.getProperty("queue.rmi.url"));

            System.out.println("Gateway started at " + gateway);
        } catch (IOException e) {
            System.err.println("Failed to load properties file: " + e.getMessage());
        } catch (NotBoundException e) {
            System.err.println("Error connecting to remote services: " + e.getMessage());
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        }
        return properties;
    }

    public void insertURL(URL url) throws RemoteException {
        if (queue != null) 
            //queue.addUrl(url);
            System.out.print("Experiencia");
    }

    public SearchResult getSearch(String search) throws RemoteException {
        return new SearchResult();
    }

    public SearchResult getConnections(URL url) throws RemoteException {
        return new SearchResult();
    }


    //IMPLEMENTAÇÃO


    @Override
    public void insertURL(org.example.URL url) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'insertURL'");
    }

    @Override
    public SearchResult getConnections(org.example.URL url) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getConnections'");
    }
}
