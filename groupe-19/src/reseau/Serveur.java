package src.reseau;

import src.modele.*;

import java.net.*;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static src.reseau.CommunicationLobbySC.*;

public class Serveur {

    private static final int PORTSERVEUR = 1201;
    
    private String pseudoClient1 = null;
    private String pseudoClient2 = null;

    private ServerSocket serveurSocket;
    private Socket clientSocket1;
    private Socket clientSocket2;
    private Socket tempConnection;

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
            this.serveurSocket = new ServerSocket(PORTSERVEUR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void accepterClient(){
        Thread threadClient = new Thread(() -> {

            accepteClient1();
            ouvertureCanalCommunicationsClient1();
            lireRequeteClient1();

            while (true) {
                accepteClient2();

                if(enJeu){
                    fermerSocketClient(tempConnection);
                    continue;
                }

                if(this.pseudoClient2 == null){
                    this.clientSocket2 = tempConnection;
                    ouvertureCanalCommunicationsClient2();

                    this.nombreJoueurPret = 0;
                    informerClients(MISE_A_JOUR_CONFIRMATION, "Système", "0");

                    lireRequeteClient2();
                }else{
                    fermerSocketClient(tempConnection);
                }

            }

        });
        threadClient.start();
    }

    private void ouvertureCanalCommunicationsClient1() {
        try {
            this.inC1 = new ObjectInputStream(clientSocket1.getInputStream());
            this.outC1 =  new ObjectOutputStream(clientSocket1.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void ouvertureCanalCommunicationsClient2() {
        try {
            this.inC2 = new ObjectInputStream(clientSocket2.getInputStream());
            this.outC2 =  new ObjectOutputStream(clientSocket2.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void accepteClient1() {
        try {
            this.clientSocket1 = this.serveurSocket.accept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void accepteClient2() {
        try {
            this.tempConnection = this.serveurSocket.accept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fermerSocketClient(Socket clientSocket) {
        try {
            clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void lireRequeteClient1(){
        Thread threadClient1 = new Thread(() -> {
            while (true)
            {
                try {
                    MessageServeur messageRecu = (MessageServeur) this.inC1.readObject();
                    this.receptionRequeteClient.put(messageRecu);

                } catch ( IOException e) {
                    System.out.println("[SERVEUR] lireRequeteClient1(), l'hôte s'est déconnecté sans prévenir, fermeture du serveur !");
                    fermerServeur();
                    break;
                } catch (ClassNotFoundException e){
                    System.out.println("[SERVEUR] lireRequeteClient1(), le cast n'a pas fonctionnait correctement !");
                    break;
                } catch (InterruptedException e){
                    System.out.println("[SERVEUR] lireRequeteClient1() a été interromptue !");
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

                } catch (IOException e) {
                    System.out.println("[SERVEUR] lireRequeteClient2(), le Client s'est déconnecté sans prévenir !");
                    this.pseudoClient2 = null;
                    informerClients(MISE_A_JOUR_INFORMATIONS_JOUEURS, "Système", "Le Joueur 2 a crashé/quitté le jeu.");
                    break;
                } catch (ClassNotFoundException e){
                    System.out.println("[SERVEUR] lireRequeteClient2(), le cast n'a pas fonctionnait correctement !");
                    break;
                } catch (InterruptedException e){
                    System.out.println("[SERVEUR] lireRequeteClient2() a été interromptue !");
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
                    System.out.println("[SERVEUR] consommerRequeteClient() a rencontré une erreur dans la consommation de la Queue 'receptionRequeteClient' !");
                    break;
                }
            }
        });

        threadConsommateurServeur.start();
    }

    private void gestionCommunicationVersClientLobby(MessageServeur messageServeur){
        CommunicationLobbyCS codeClient = messageServeur.getCodeClient();
        switch (codeClient){
            case INFORMATION_CLIENT:
                receptionInformationClient(messageServeur);
                break;
            case MESSAGE:
                informerClients(NOUVEAU_MESSAGE, messageServeur.getPseudo(), messageServeur.getContenue());
                break;
            case BOUTON_CHOIX:
                receptionBoutonChoix(messageServeur);
                break;
            case BOUTON_PRET:
                receptionBoutonPret(messageServeur);
                break;
            case QUITTE:
                receptionQuitte(messageServeur);
                break;
            default:
                System.out.println("[SERVEUR] gestionCommunicationVersClientLobby() a rencontré une erreur dans le switch avec le code : " + codeClient + " !");
        }
    }

    private void receptionQuitte(MessageServeur messageServeur) {
        if((messageServeur.getPseudo()).equals(this.pseudoClient2)) {
            informerClients(MISE_A_JOUR_INFORMATIONS_JOUEURS, messageServeur.getPseudo(), pseudoClient2 + " vient de quitter le lobby !");
            this.pseudoClient2 = null;
            this.joueur2pret = false;
            System.out.println("[SERVEUR] Joueur 2 a quitté le lobby !");
        }else{
            System.out.println("[SERVEUR] Joueur 1 a quitté le lobby, donc fermeture du serveur!");
            informerClients(HOTE_QUITTE,"","");
            fermerServeur();
        }
    }

    private void receptionBoutonPret(MessageServeur messageServeur) {
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
    }

    private void receptionBoutonChoix(MessageServeur messageServeur) {
        informerClients(MISE_A_JOUR_CHOIX, messageServeur.getPseudo(), messageServeur.getContenue());
        this.j1Jack = Boolean.parseBoolean(messageServeur.getContenue().toString());
    }

    private void receptionInformationClient(MessageServeur messageServeur) {
        if(this.pseudoClient1 == null){
            this.pseudoClient1 = messageServeur.getPseudo();
        }else{
            if(messageServeur.getPseudo().equals(this.pseudoClient1)){
                try {
                    outC2.writeObject(new MessageServeur(ERREUR_PSEUDO, "Systeme", "Vous avez le même pseudonyme que l'hôte, vous êtes refuser du lobby. "));
                } catch (IOException e) {
                    System.out.println("[SERVEUR] receptionInformationClient() a rencontré une erreur dans l'envoie d'erreur du pseudonyme identique au deuxième Client !");
                }
            }
            this.pseudoClient2 = messageServeur.getPseudo();
            try {
                outC1.writeObject(new MessageServeur(MISE_A_JOUR_INFORMATIONS_JOUEURS, this.pseudoClient2, ""));
                outC2.writeObject(new MessageServeur(MISE_A_JOUR_INFORMATIONS_JOUEURS, this.pseudoClient1, ""));
            } catch (IOException e) {
                System.out.println("[SERVEUR] receptionInformationClient() a rencontré une erreur dans l'envoie du pseudonyme aux Clients !");
            }
        }
    }

    private void gestionEcoute(MessageServeur messageServeur){
        if(partieServeur.joueurCourant == choixJ1){
            if(messageServeur.getPseudo().equals(this.pseudoClient1)){
                gestionCommunicationVersClientJeu(messageServeur);
            }
        }else{
            if(messageServeur.getPseudo().equals(this.pseudoClient2)){
                gestionCommunicationVersClientJeu(messageServeur);
            }
        }
    }


    private void gestionCommunicationVersClientJeu(MessageServeur messageServeur){
        Object[] contenueRecu = (Object[]) messageServeur.getContenue();
        CommunicationLobbyCS codeClient = messageServeur.getCodeClient();
        switch (codeClient){
            case PIOCHE_ALIBI :
                receptionActionPioche();
                break;
            case JOKER:
                receptionActionJoker(contenueRecu);
                break;
            case DETECTIVE:
                receptionActionDetective(contenueRecu);
                break;
            case ROTATION:
            case ECHANGE:
                receptionActionQuartier(codeClient, contenueRecu);
                break;
            default:
                System.out.println("[SERVEUR] gestionCommunicationVersClientJeu() a rencontré une erreur dans le switch avec le code : " + codeClient + " !");
        }

        PartieSnapshot partieEnCours = PartieSaveMapper.toSnapshot(partieServeur);
        informerClients(NOUVEAU_PLATEAU, "Systeme", partieEnCours);

    }

    private void receptionActionPioche(){
        this.partieServeur.actions.alibi();
    }

    private void receptionActionJoker(Object[] contenueRecu){
        Detective.Type detectiveType;
        if(contenueRecu[0] != null) {
            detectiveType = (Detective.Type) contenueRecu[0];
            for (Detective d : this.partieServeur.detectives) {
                if (d.getType() == detectiveType) this.partieServeur.actions.joker(d);
            }
        }else{
            this.partieServeur.actions.joker(null);
        }
    }

    private void receptionActionDetective(Object[] contenueRecu){
        Detective.Type detectiveType;
        detectiveType = (Detective.Type) contenueRecu[0];
        for (Detective d : this.partieServeur.detectives) {
            if (d.getType() == detectiveType) this.partieServeur.actions.deplacerDetective(d, (Integer) contenueRecu[1]);
        }
    }

    private void receptionActionQuartier(CommunicationLobbyCS codeClient, Object[] contenueRecu){
        int premierEntierRecu =  (Integer) contenueRecu[0];
        int deuxiemeEntierRecu =  (Integer) contenueRecu[1];
        int troisiemeEntierRecu =  (Integer) contenueRecu[2];
        int quartierEntierRecu =  (Integer) contenueRecu[3];

        switch (codeClient){
            case ROTATION:
                this.partieServeur.actions.rotationQuartier(premierEntierRecu,deuxiemeEntierRecu,troisiemeEntierRecu,quartierEntierRecu);
                break;
            case ECHANGE:
                this.partieServeur.actions.echange(premierEntierRecu,deuxiemeEntierRecu,troisiemeEntierRecu,quartierEntierRecu);
                break;
            default:
                System.out.println("[SERVEUR] receptionActionQuartier() a rencontré une erreur dans le switch avec le code : " + codeClient + " !");
        }

    }

    private void informerClients(CommunicationLobbySC codeServeur, String pseudo, Object contenu){
        MessageServeur messageServeur = new MessageServeur(codeServeur,pseudo, contenu);
        try {
            if (outC1 != null) {
                outC1.writeObject(messageServeur);
            }
            if (outC2 != null) {
                outC2.writeObject(messageServeur);
            }
        } catch (IOException e) {
            System.out.println("[SERVEUR] informerClients() a rencontré une erreur lors de l'envoie du message (code : " + codeServeur + " ) !");
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
            System.out.println("[SERVEUR] fermerServeur() a rencontré une erreur lors de la fermeture des cannaux de communications !");
        }
    }

}

