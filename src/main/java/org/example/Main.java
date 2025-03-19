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
import java.util.NoSuchElementException;
import java.util.Scanner;

import static java.sql.DriverManager.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    /**
    * String used as a line break separator
    */
    private static final String lineBreak = "=".repeat(30);


    /**
    *  Scanner object used for input reading
    */ 
    private static Scanner scanner;

    /**
     * 
     *  Class main (...)
     * 
     * @param args
     */

    public static void main(String[] args) {

        /*
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

        */

        connect();

        scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
    
            int menuOption = showMenu();
            if(menuOption == 0){
                disconnect();
                break;
            }
            
            switch (menuOption) {
                case 1:
                    System.out.println(lineBreak+"\nInsert URL");
                    URL url_insert = new URL(readURL());
                    //gateway.InsertURL(url);
                    break;
                case 2:
                    System.out.println(lineBreak+"\nSearch");
                    /*
                     * String search = Search();
                        SearchResult searchResult;
                        try {
                            searchResult = gateway.Search(search);
                            ShowResult(searchResult);
                        } catch (InformationNotAccessibleException e) {
                            System.out.println(lineBreak);
                            System.out.println(e.getMessage());
                        }
                     */
                    break;
                case 3:
                    System.out.println(lineBreak+"\nConsult URL connections");
                    URL url_consult = new URL(readURL());
                    /*
                     * if(bInter.getConnections(url) != null){
                            HashSet<String> a = bInter.getConnections(url);
                            System.out.println(bInter.getConnections(url));
                        }
                        else
                            System.out.println("Link nao conhecido");
                        break;
                     */
                    break;
                case 4:
                    System.out.println(lineBreak+"\nOpening administrative page");
                    /*
                     * AdministrationPage administrationPage = gateway.getAdministrationPage();
                        ShowAdministrationPage(administrationPage);
                     */
                    break;
                default:
                    break;
            }

            // PUT TRY AND CATCH! 
            /*
             * }
            }catch (java.rmi.ConnectException e) {
                CannotReachGateway();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
             */
        }   

    }

    private static void connect(){

    }

    private static void disconnect(){
        System.out.println(lineBreak);
        scanner.close();
        System.out.println("Client successfully disconnected");
    }

    private static int showMenu() {
        int returnValue = -1;

        String menu = lineBreak
                    + "\nMenu:\n"
                    + "[1] Insert URL\n"
                    + "[2] Search\n"
                    + "[3] Consult URL connections\n"
                    + "[4] Administrative page\n\n"
                    + "[0] Exit\n"
                    + lineBreak;
            
        System.out.println(menu);
    
        String reader = scanner.nextLine().trim();

        try {
            returnValue = Integer.parseInt(reader);

            if (returnValue >= 0 && returnValue <= 4) 
                    return returnValue;
            else 
                 System.out.println("Invalid option. Please choose a valid option");
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid option");
        }
    
        return returnValue;
    }

    private static boolean URLValid(String url) {
            return url.startsWith("http://") || url.startsWith("https://");
    }
    
    private static String readURL() {
        try {
            System.out.println("Input a URL or go back to main menu by entering 0:");
            String input = scanner.nextLine().trim();

            if(input.equals("0")) 
                return null;
    
            if (URLValid(input)) 
              return input;
            else
                System.out.println("Invalid URL format. Please enter a valid URL\n"+lineBreak);

        } catch (Exception e) {
            return null;
        }
        return readURL();
    }

}
