package org.example;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Queue;

public class QueueService extends UnicastRemoteObject implements IQueue {
    private final Queue<String> urlQueue;

    public QueueService() throws RemoteException {
        super();
        this.urlQueue = new LinkedList<>();
        System.out.println("✅ Queue inicializada.");
    }

    @Override
    public synchronized void addURL(String url) throws RemoteException {
        urlQueue.add(url);
        System.out.println("📥 URL adicionado à Queue: " + url);
    }

    @Override
    public synchronized String getURL() throws RemoteException {
        return urlQueue.poll();
    }

    public static void main(String[] args) {
        try {
            QueueService queue = new QueueService();
            Naming.rebind("rmi://localhost:1200/QueueService", queue);
            System.out.println("✅ QueueService registado no RMI no porto 1200.");
        } catch (Exception e) {
            System.err.println("❌ Erro ao iniciar a QueueService:");
            e.printStackTrace();
        }
    }
}
