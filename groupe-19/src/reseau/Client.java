package src.reseau;

import src.modele.Joueur;
import src.modele.Partie;
import src.modele.PartieSaveMapper;
import src.modele.PartieSnapshot;
import src.vue.VueJeu;
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
    private VueJeu vueJeu;

    private Partie partieClient;

    private boolean enJeu = false;

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

    public void setVueJeu(VueJeu vueJeu){
        this.vueJeu = vueJeu;
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
                    if(enJeu){
                        gestionCommunicationVersVueJeu(messageServeur);
                    }else {
                        gestionCommunicationVersVueLobby(messageServeur);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        threadConsommateurClient.start();
    }

    private void gestionCommunicationVersVueLobby(MessageServeur messageServeur) {
        if(this.vueLobby == null){
            return;
        }
        SwingUtilities.invokeLater(()-> {
            switch (messageServeur.getCodeServeur()) {
                case NOUVEAU_MESSAGE:
                    this.vueLobby.ajouterMessageChat(messageServeur.getPseudo(), messageServeur.getContenue().toString());
                    break;
                case MISE_A_JOUR_CONFIRMATION:
                    this.vueLobby.mettreAJourCompteurPrets(Integer.parseInt(messageServeur.getContenue().toString()));
                    break;
                case MISE_A_JOUR_CHOIX:
                    this.vueLobby.mettreAJourRoles(Boolean.parseBoolean(messageServeur.getContenue().toString()));
                    break;
                case MISE_A_JOUR_INFORMATIONS_JOUEURS:
                    if (estHote) {
                        if(!messageServeur.getContenue().toString().isEmpty()){
                            this.vueLobby.setJoueur2Deconnecte(messageServeur.getContenue().toString());
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
                    PartieSnapshot versionServeur = (PartieSnapshot) messageServeur.getContenue();
                    partieClient = new Partie(Joueur.JACK, -1 ,-1);
                    PartieSaveMapper.fromSnapshot(partieClient,versionServeur);
                    vueJeu = new VueJeu(partieClient);
                    this.vueLobby.lancementPartie(vueJeu);
                    ExecuteActionSocket liaisons = new ExecuteActionSocket(this);

                    vueJeu.getGameplay().setExecuteAction(liaisons);
                    this.enJeu = true;
                    break;
                case ERREUR_PSEUDO:
                    System.out.println(messageServeur.getContenue().toString());;
                    try {
                        this.out.close();
                        this.in.close();
                        this.serveurSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    throw new RuntimeException("Erreur serveur !");
            }
        });
    }

    private void gestionCommunicationVersVueJeu(MessageServeur messageServeur) {
        if(vueJeu==null){
            return;
        }
        SwingUtilities.invokeLater(()-> {
            switch (messageServeur.getCodeServeur()) {
                case NOUVEAU_PLATEAU:
                    this.vueJeu.getGameplay().refreshFromSnap((PartieSnapshot) messageServeur.getContenue());
                    break;
                case ACTION_IMPOSSIBLE:
                    break;
                default:
                    throw new RuntimeException("Erreur serveur !");
            }
        });
    }


    private void informerServeur(CommunicationLobbyCS codePOurServeur, Object contenue) {

        MessageServeur messageServeur = new MessageServeur(codePOurServeur, this.pseudoClient,contenue);

        try {
            out.writeObject(messageServeur);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    // Méthodes pour le lobby
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


    // Méthodes pour le jeu
    public void clientPiocheAlibi(Object contenue){
        informerServeur(PIOCHE_ALIBI,contenue);
    }

    public void clientJoueJoker(Object contenue){
        informerServeur(JOKER,contenue);
    }

    public void clientDeplaceDetective(Object contenue){
        informerServeur(DETECTIVE,contenue);
    }

    public void clientTourneQuartier(Object contenue){
        informerServeur(ROTATION,contenue);
    }

    public void clientEchangeQuartier(Object contenue){
        informerServeur(ECHANGE,contenue);
    }

}