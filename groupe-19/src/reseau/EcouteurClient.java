package src.reseau;

import src.modele.Action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class EcouteurClient implements Runnable{
    private final Socket clientSocket;
    private final BlockingQueue<Action> boiteAuxLettres;
    // TODO : Juste un entrainement pour l'implémentation du réseau
    public EcouteurClient(Socket clientSocket, BlockingQueue<Action> boiteAuxLettres) {
        this.clientSocket = clientSocket;
        this.boiteAuxLettres = boiteAuxLettres;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream()))
        {
            while (true)
            {
                try {
                    Action messageRecu = (Action) in.readObject();
                    if(messageRecu == null){
                        break;
                    }
                    boiteAuxLettres.put(messageRecu);
                    System.out.println("Action reçu !");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }


            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
