package src.reseau;

import java.net.*;
import java.io.*;

public class Serveur {
    public static void main(String[] args) {
        try (ServerSocket serveurSocket = new ServerSocket(1201);
             Socket clientSocket = serveurSocket.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String messageRecu = in.readLine();
            System.out.println("Message reçu : " + messageRecu);

        } catch (IOException e) {
            System.out.println("Erreur");
        }
    }
}
