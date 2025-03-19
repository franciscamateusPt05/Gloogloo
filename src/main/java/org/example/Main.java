package org.example;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.sql.DriverManager;
import java.sql.SQLException;

import static java.sql.DriverManager.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            // Criar a fila de URLs
            IQueue queue = new QueueImpl();


            // Simulando a adição de URLs à fila
            System.out.println("Adicionando URLs à fila...");
            queue.addURL("https://www.example.com");
            queue.addURL("https://www.google.com");
            queue.addURL("https://www.github.com");



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    }
