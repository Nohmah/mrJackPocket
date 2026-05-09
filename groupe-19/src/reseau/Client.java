package src.reseau;

import java.net.*;
import java.io.*;

public class Client {
    public static void main(String[] args) {
        try {
            // Ouverture d'un socket de connexion
            Socket echosocket = new Socket("127.0.0.1", 1201);

            PrintWriter out = new PrintWriter(echosocket.getOutputStream(), true);
            out.println("Bonjour depuis le client !");

            echosocket.close();
        } catch (IOException e) {
            System.out.println("Erreur client : " + e.getMessage());
        }
    }
}