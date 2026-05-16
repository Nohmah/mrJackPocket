package src.reseau;

import src.vue.menus.VueLobby;

import javax.swing.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static src.reseau.CommunicationLobbyCS.*;

public class Client {

    private final String pseudoClient;
    private final Boolean estHote;
    private Socket serveurSocket;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final BlockingQueue<MessageServeur> receptionRequeteServeur;

    private VueLobby vueLobby;

    public Client(String pseudoClient, Boolean estHote, String serveur) {
        this.pseudoClient = pseudoClient;
        this.estHote = estHote;
        receptionRequeteServeur = new LinkedBlockingQueue<>();

        rejoindreServeur(serveur);

        receptionRequeteServeur();
        consommerMessagesDeLaQueue();

    }

    public void setVueLobby(VueLobby vueLobby){
        this.vueLobby = vueLobby;
        informerServeur(INFORMATION_CLIENT,null);

    }

    private void rejoindreServeur(String serveur){
        try {
            this.serveurSocket = new Socket(serveur, 1201);
            this.out = new ObjectOutputStream(this.serveurSocket.getOutputStream());
            this.in = new ObjectInputStream(this.serveurSocket.getInputStream());
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
    }

    private void receptionRequeteServeur(){
        Thread threadReception = new Thread(() -> {
            while (true)
            {
                try {
                    MessageServeur messageRecu = (MessageServeur) this.in.readObject();
                    this.receptionRequeteServeur.put(messageRecu);

                } catch (ClassNotFoundException | InterruptedException | IOException e) {
                    System.out.println("Le Serveur s'est arrêté ou la connexion est perdue.");
                    this.vueLobby.ajouterMessageChat("System","L'hôte s'est déconnecté, vous allez être rediriger vers le menu automatiquement dans 5 seconds !");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    if(!estHote){
                        this.vueLobby.hoteEstDeconnecter();
                    }
                    break;
                }
            }
        });
        threadReception.start();
    }

    private void consommerMessagesDeLaQueue(){
        Thread threadConsommateurClient = new Thread(() -> {
            while (true) {
                try {
                    MessageServeur messageServeur = this.receptionRequeteServeur.take();
                    gestionCommunicationVersVue(messageServeur);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        threadConsommateurClient.start();
    }

    private void gestionCommunicationVersVue(MessageServeur messageServeur) {
        if(this.vueLobby == null){
            return;
        }
        SwingUtilities.invokeLater(()-> {
            switch (messageServeur.getCodeServeur()) {
                case NOUVEAU_MESSAGE:
                    this.vueLobby.ajouterMessageChat(messageServeur.getPseudo(), messageServeur.getMessage());
                    break;
                case MISE_A_JOUR_CONFIRMATION:
                    this.vueLobby.mettreAJourCompteurPrets(Integer.parseInt(messageServeur.getMessage()));
                    if(Integer.parseInt(messageServeur.getMessage()) == 2){
                        this.vueLobby.lancementPartie();
                        //TODO Changer d'état comme le serveur
                    }
                    break;
                case MISE_A_JOUR_CHOIX:
                    this.vueLobby.mettreAJourRoles(Boolean.parseBoolean(messageServeur.getMessage()));
                    break;
                case MISE_A_JOUR_INFORMATIONS_JOUEURS:
                    if (estHote) {
                        if(!messageServeur.getMessage().isEmpty()){
                            this.vueLobby.setJoueur2Deconnecte(messageServeur.getMessage());
                            break;
                        }
                        this.vueLobby.setJoueur2Connecte(pseudoClient, messageServeur.getPseudo());
                    } else {
                        this.vueLobby.setJoueur2Connecte(messageServeur.getPseudo(), pseudoClient);
                    }
                    break;
                case HOTE_QUITTE:
                    System.out.println("L'Hôte a quitté le lobby !");
                    try {
                        this.in.close();
                        this.out.close();
                        serveurSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case LANCEMENT_PARTIE:
                    break;
                default:
                    throw new RuntimeException("Erreur serveur !");
            }
        });
    }

    private void informerServeur(CommunicationLobbyCS codePOurServeur, String message) {

        MessageServeur messageServeur = new MessageServeur(codePOurServeur, this.pseudoClient,message);

        try {
            out.writeObject(messageServeur);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void boutonPretAppuyer(Boolean etatBouton){
        informerServeur(BOUTON_PRET, etatBouton.toString());
    }

    public void nouveauMessage(String message){
        informerServeur(MESSAGE, message);
    }

    public void nouveauChoix(Boolean veutEtreJack){
        informerServeur(BOUTON_CHOIX,veutEtreJack.toString());
    }

    public void joueurQuitte(){
        informerServeur(QUITTE, null);
    }

}