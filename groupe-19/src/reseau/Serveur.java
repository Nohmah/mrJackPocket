package src.reseau;

import src.modele.*;
import src.vue.Gameplay;

import java.net.*;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static src.reseau.CommunicationLobbySC.*;

public class Serveur {

    private String pseudoClient1 = null;
    private String pseudoClient2 = null;

    private ServerSocket serveurSocket;
    private Socket clientSocket1;
    private Socket clientSocket2;

    private ObjectOutputStream outC1;
    private ObjectInputStream inC1;

    private ObjectOutputStream outC2;
    private ObjectInputStream inC2;

    private final BlockingQueue<MessageServeur> receptionRequeteClient;

    private int nombreJoueurPret = 0;
    private boolean joueur1pret;
    private boolean joueur2pret;
    private boolean j1Jack;
    private Joueur choixJ1;

    private boolean enJeu = false;
    private Partie partieServeur;

    public Serveur() {
        receptionRequeteClient = new LinkedBlockingQueue<>();
        initialisationServeur();
        accepterClient();
        consommerRequeteClient();

    }

    private void initialisationServeur() {
        try {
            this.serveurSocket = new ServerSocket(1201);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void accepterClient(){
        Thread threadClient = new Thread(() -> {
            try {
                this.clientSocket1 = this.serveurSocket.accept();
                this.outC1 = new ObjectOutputStream(this.clientSocket1.getOutputStream());
                this.inC1 = new ObjectInputStream(this.clientSocket1.getInputStream());
                lireRequeteClient1();

                while (true) {
                    Socket nouvelleConnection = this.serveurSocket.accept();

                    if(enJeu){
                        nouvelleConnection.close();
                        continue;
                    }

                    if(this.pseudoClient2 == null){
                        this.clientSocket2 = nouvelleConnection;
                        this.outC2 = new ObjectOutputStream(this.clientSocket2.getOutputStream());
                        this.inC2 = new ObjectInputStream(this.clientSocket2.getInputStream());

                        this.nombreJoueurPret = 0;
                        informerClients(MISE_A_JOUR_CONFIRMATION, "Système", "0");

                        lireRequeteClient2();
                    }else{
                        nouvelleConnection.close();
                    }

                }

            } catch (IOException e) {
                System.out.println("Fermeture du port d'écoute du serveur.");
            }
        });
        threadClient.start();
    }

    private void lireRequeteClient1(){
        Thread threadClient1 = new Thread(() -> {
            while (true)
            {
                try {
                    MessageServeur messageRecu = (MessageServeur) this.inC1.readObject();
                    this.receptionRequeteClient.put(messageRecu);

                } catch (ClassNotFoundException | InterruptedException | IOException e) {
                    System.out.println("Client 1 s'est déconnecté brutalement.");
                    break;
                }
            }
        });
        threadClient1.start();
    }

    private void lireRequeteClient2(){
        Thread threadClient2 = new Thread(() -> {
            while (true)
            {
                try {
                    MessageServeur messageRecu = (MessageServeur) this.inC2.readObject();
                    this.receptionRequeteClient.put(messageRecu);

                } catch (ClassNotFoundException | InterruptedException | IOException e) {
                    System.out.println("Client 2 s'est déconnecté brutalement.");
                    this.pseudoClient2 = null;
                    informerClients(MISE_A_JOUR_INFORMATIONS_JOUEURS, "Système", "Le Joueur 2 a crashé/quitté le jeu.");
                    break;
                }
            }
        });
        threadClient2.start();
    }

    private void consommerRequeteClient(){
        Thread threadConsommateurServeur = new Thread(() -> {
            while (true) {
                try {
                    MessageServeur messageServeur = this.receptionRequeteClient.take();
                    if(enJeu){
                        gestionEcoute(messageServeur);
                    }else{
                        gestionCommunicationVersClientLobby(messageServeur);
                    }
                } catch (InterruptedException e) {
                    System.out.println("Arrêt de la file d'attente du serveur.");
                    break;
                }
            }
        });

        threadConsommateurServeur.start();
    }

    private void gestionCommunicationVersClientLobby(MessageServeur messageServeur){
        switch (messageServeur.getCodeClient()){
            case INFORMATION_CLIENT:
                if(this.pseudoClient1 == null){
                    this.pseudoClient1 = messageServeur.getPseudo();
                }else{
                    this.pseudoClient2 = messageServeur.getPseudo();
                    try {
                        outC1.writeObject(new MessageServeur(MISE_A_JOUR_INFORMATIONS_JOUEURS, this.pseudoClient2, ""));
                        outC2.writeObject(new MessageServeur(MISE_A_JOUR_INFORMATIONS_JOUEURS, this.pseudoClient1, ""));
                    } catch (IOException e) {
                        System.out.println("Erreur de synchronisation des pseudos.");
                    }
                }
                break;
            case MESSAGE:
                informerClients(NOUVEAU_MESSAGE, messageServeur.getPseudo(), messageServeur.getContenue());
                break;
            case BOUTON_CHOIX:
                informerClients(MISE_A_JOUR_CHOIX, messageServeur.getPseudo(), messageServeur.getContenue());
                this.j1Jack = Boolean.parseBoolean(messageServeur.getContenue().toString());
                break;
            case BOUTON_PRET:
                if (messageServeur.getPseudo().equals(this.pseudoClient1)) {
                    this.joueur1pret = Boolean.parseBoolean(messageServeur.getContenue().toString());
                } else if (messageServeur.getPseudo().equals(this.pseudoClient2)) {
                    this.joueur2pret = Boolean.parseBoolean(messageServeur.getContenue().toString());
                }
                this.nombreJoueurPret = (this.joueur1pret ? 1 : 0) + (this.joueur2pret ? 1 : 0);
                informerClients(MISE_A_JOUR_CONFIRMATION, messageServeur.getPseudo(), String.valueOf(this.nombreJoueurPret));
                if(this.nombreJoueurPret == 2){
                    this.enJeu = true;
                    if(j1Jack){
                        partieServeur = new Partie(Joueur.JACK,-1,-1);
                        choixJ1 = Joueur.JACK;
                    }else{
                        partieServeur = new Partie(Joueur.ENQUETEUR,-1,-1);
                        choixJ1 = Joueur.ENQUETEUR;
                    }
                    PartieSnapshot partieInitiale = PartieSaveMapper.toSnapshot(partieServeur);

                    informerClients(LANCEMENT_PARTIE, "Systeme", partieInitiale);
                }

                break;
            case QUITTE:
                if((messageServeur.getPseudo()).equals(this.pseudoClient2)) {
                    informerClients(MISE_A_JOUR_INFORMATIONS_JOUEURS, messageServeur.getPseudo(), pseudoClient2 + " vient de quitter le lobby !");
                    this.pseudoClient2 = null;
                    this.joueur2pret = false;
                    System.out.println("Joueur 2 a quitter le lobby !");
                }else{
                    System.out.println("Le joueur 1 a quitter le lobby !");
                    informerClients(HOTE_QUITTE,"","");
                    fermerServeur();
                }
                break;
            default:
                throw new RuntimeException("Client n'a pas respecter le protocol");
        }
    }

    private void gestionEcoute(MessageServeur messageServeur){
        if(partieServeur.joueurCourant == choixJ1){
            if(messageServeur.getPseudo().equals(this.pseudoClient1)){
                System.out.println("[DEBUG] Client 1 à eu la main ! ");
                gestionCommunicationVersClientJeu(messageServeur);
            }
        }else{
            if(messageServeur.getPseudo().equals(this.pseudoClient2)){
                System.out.println("[DEBUG] Client 2 à eu la main ! ");
                gestionCommunicationVersClientJeu(messageServeur);
            }
        }
    }


    private void gestionCommunicationVersClientJeu(MessageServeur messageServeur){
        Object contenueRecu[] = (Object[]) messageServeur.getContenue();
        Detective.Type detectiveType;
        switch (messageServeur.getCodeClient()){
            case PIOCHE_ALIBI :
                this.partieServeur.actions.alibi();
                break;
            case JOKER:
                if(contenueRecu[0] != null) {
                    detectiveType = (Detective.Type) contenueRecu[0];
                    for (Detective d : this.partieServeur.detectives) {
                        if (d.getType() == detectiveType) this.partieServeur.actions.joker(d);
                    }
                }else{
                    this.partieServeur.actions.joker(null);
                }
                break;
            case DETECTIVE:
                detectiveType = (Detective.Type) contenueRecu[0];
                for (Detective d : this.partieServeur.detectives) {
                if (d.getType() == detectiveType) this.partieServeur.actions.deplacerDetective(d, (Integer) contenueRecu[1]);;
                }

                break;
            case ROTATION:
                this.partieServeur.actions.rotationQuartier((Integer) contenueRecu[0], (Integer) contenueRecu[1], (Integer) contenueRecu[2], (Integer) contenueRecu[3] );
                break;
            case ECHANGE:
                this.partieServeur.actions.echange((Integer) contenueRecu[0], (Integer) contenueRecu[1], (Integer) contenueRecu[2], (Integer) contenueRecu[3]);
                break;

        }

        PartieSnapshot partieEnCours = PartieSaveMapper.toSnapshot(partieServeur);
        informerClients(NOUVEAU_PLATEAU, "Systeme", partieEnCours);

    }


    private void informerClients(CommunicationLobbySC codeServeur,String pseudo, Object contenu){
        System.out.println("[DEBUG] Code serveur : "+ codeServeur);
        MessageServeur messageServeur = new MessageServeur(codeServeur,pseudo, contenu);
        try {
            if (outC1 != null) {
                System.out.println("[DEBUG] C1 message");
                outC1.writeObject(messageServeur);
            }
            if (outC2 != null) {
                outC2.writeObject(messageServeur);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fermerServeur() {
        System.out.println("Fermeture complète du serveur...");
        try {

            if (inC1 != null) inC1.close();
            if (outC1 != null) outC1.close();
            if (clientSocket1 != null) clientSocket1.close();

            if (inC2 != null) inC2.close();
            if (outC2 != null) outC2.close();
            if (clientSocket2 != null) clientSocket2.close();
            if (serveurSocket != null && !serveurSocket.isClosed()) {
                serveurSocket.close();
            }

        } catch (IOException e) {
            System.out.println("Erreur pendant la fermeture du serveur.");
        }
    }

}

