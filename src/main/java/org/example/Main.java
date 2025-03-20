package org.example;

import java.util.Scanner;

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

        connect();

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
        System.out.println(lineBreak);
        scanner = new Scanner(System.in);
        System.out.println("Client connected successfully");
    }

    private static void disconnect(){
        System.out.println(lineBreak);
        scanner.close();
        System.out.println("Client disconnected successfully");
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
