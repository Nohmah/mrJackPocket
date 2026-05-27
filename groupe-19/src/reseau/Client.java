package src.reseau;

import src.modele.*;
import src.vue.VueJeu;
import src.vue.menus.VueLobby;

import javax.swing.*;
import java.net.*;
import java.io.*;
import java.sql.SQLOutput;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static src.reseau.CommunicationLobbyCS.*;

public class Client {

    private static final int PORT_SERVEUR = 1201;

    private final String pseudoClient;
    private final Boolean estHote;
    private Socket serveurSocket;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final BlockingQueue<MessageServeur> receptionRequeteServeur;

    private VueLobby vueLobby;
    private VueJeu vueJeu = null;

    private boolean enJeu = false;
    private volatile boolean deconnexionForce = false;

    private boolean jeSuisJack;

    public Client(String pseudoClient, Boolean estHote, String serveur) {
        this.pseudoClient = pseudoClient;
        this.estHote = estHote;
        this.jeSuisJack = estHote;
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
            this.serveurSocket = new Socket(serveur, PORT_SERVEUR);
            this.out = new ObjectOutputStream(this.serveurSocket.getOutputStream());
            this.in = new ObjectInputStream(this.serveurSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("[CLIENT] rejoindreServeur(), la connexion a serveur n'a pas pu être établit !");
        }
    }

    private void receptionRequeteServeur(){
        Thread threadReception = new Thread(() -> {
            while (true)
            {
                try {
                    MessageServeur messageRecu = (MessageServeur) this.in.readObject();
                    this.receptionRequeteServeur.put(messageRecu);

                }  catch (IOException e) {
                    if(!this.deconnexionForce) {
                        System.out.println("[CLIENT] receptionRequeteServeur(), le serveur s'est arrêté sans prévenir !");
                        fermerConnexion();

                        if (this.vueJeu == null) {
                            SwingUtilities.invokeLater(() -> {
                                this.vueLobby.ajouterMessageChat("System", "L'hôte s'est déconnecté, redirection dans 5 secondes !");
                                javax.swing.Timer timer = new javax.swing.Timer(5000, event -> {
                                    if (!estHote) {
                                        this.vueLobby.revenirMenuPrincipal();
                                    }
                                });
                                timer.setRepeats(false);
                                timer.start();
                            });
                        }else{
                            if(!estHote) {
                                JOptionPane.showMessageDialog(vueJeu,"L'hôte s'est déconnecté, vous allez être rediriger vers le menu principale !");
                            }
                            this.vueJeu.stopGameLoop();
                            this.vueJeu = null;
                            this.vueLobby.revenirMenuPrincipal();
                            this.vueLobby = null;
                            fermerConnexion();
                        }
                    }
                    break;
                } catch (ClassNotFoundException e){
                    System.out.println("[CLIENT] receptionRequeteServeur(), le cast n'a pas fonctionnait correctement !");
                    break;
                } catch (InterruptedException e){
                    System.out.println("[CLIENT] receptionRequeteServeur() a été interromptue !");
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
                    System.out.println("[CLIENT] consommerMessagesDeLaQueue() a rencontré une erreur dans la consommation de la Queue 'receptionRequeteServeur' !");
                    break;
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
                    receptionRequeteNOUVEAU_MESSAGE(messageServeur);
                    break;
                case MISE_A_JOUR_CONFIRMATION:
                    receptionRequeteMISE_A_JOUR_CONFIRMATION(messageServeur);
                    break;
                case MISE_A_JOUR_CHOIX:
                    receptionRequeteMISE_A_JOUR_CHOIX(messageServeur);
                    break;
                case MISE_A_JOUR_INFORMATIONS_JOUEURS:
                    receptionRequeteMISE_A_JOUR_INFORMATIONS_JOUEURS(messageServeur);
                    break;
                case HOTE_QUITTE:
                    receptionRequeteHOTE_QUITTE();
                    break;
                case LANCEMENT_PARTIE:
                    receptionRequeteLANCEMENT_PARTIE(messageServeur);
                    break;
                case ERREUR_PSEUDO:
                    receptionRequeteERREUR_PSEUDO(messageServeur);
                    break;
                default:
                    System.out.println("[CLIENT] gestionCommunicationVersVueLobby() a rencontré une erreur dans le switch avec le code : " + messageServeur.getCodeServeur() + " !");
            }
        });
    }

    private void receptionRequeteNOUVEAU_MESSAGE(MessageServeur messageServeur) {
        this.vueLobby.ajouterMessageChat(messageServeur.getPseudo(), messageServeur.getContenue().toString());
    }
     private void receptionRequeteMISE_A_JOUR_CONFIRMATION(MessageServeur messageServeur) {
         this.vueLobby.mettreAJourCompteurPrets(Integer.parseInt(messageServeur.getContenue().toString()));
     }

    private void receptionRequeteMISE_A_JOUR_CHOIX(MessageServeur messageServeur) {
        boolean veutEtreJack = Boolean.parseBoolean(messageServeur.getContenue().toString());
        if (this.estHote) {
            this.jeSuisJack = veutEtreJack;
        } else{
            this.jeSuisJack = !veutEtreJack;
        }
        this.vueLobby.mettreAJourRoles(veutEtreJack);
    }

    private void receptionRequeteMISE_A_JOUR_INFORMATIONS_JOUEURS(MessageServeur messageServeur) {

        if (estHote) {
            if(!messageServeur.getContenue().toString().isEmpty()){
                this.vueLobby.setJoueur2Deconnecte(messageServeur.getContenue().toString());
                return;
            }
            this.vueLobby.setJoueur2Connecte(pseudoClient, messageServeur.getPseudo());
        } else {
            this.vueLobby.setJoueur2Connecte(messageServeur.getPseudo(), pseudoClient);
        }
    }

    private void receptionRequeteHOTE_QUITTE() {
        System.out.println("[CLIENT] L'hôte a quitté le lobby !");
        this.deconnexionForce = true ;
        fermerConnexion();
    }

    private void receptionRequeteLANCEMENT_PARTIE(MessageServeur messageServeur) {
        PartieSnapshot versionServeur = (PartieSnapshot) messageServeur.getContenue();
        Partie partieClient = new Partie(Joueur.JACK, -1, -1, "ENQUETEUR", "JACK");
        PartieSaveMapper.fromSnapshot(partieClient,versionServeur);
        if(jeSuisJack){
            partieClient.joueurChoisi = Joueur.JACK;
        }else{
            partieClient.joueurChoisi = Joueur.ENQUETEUR;
        }
        this.vueLobby.lancementPartie(partieClient);
        this.enJeu = true;
    }

    private void receptionRequeteERREUR_PSEUDO(MessageServeur messageServeur) {
        JOptionPane.showMessageDialog(vueLobby,messageServeur.getContenue().toString());
        this.vueLobby.revenirMenuPrincipal();
        this.vueLobby = null;
        this.deconnexionForce = true;
        fermerConnexion();
    }


    private void gestionCommunicationVersVueJeu(MessageServeur messageServeur) {
        if(vueJeu==null){
            return;
        }
        SwingUtilities.invokeLater(()-> {
            switch (messageServeur.getCodeServeur()) {
                case NOUVEAU_PLATEAU:
                    receptionRequeteNOUVEAU_PLATEAU(messageServeur);
                    break;
                case AFFICHE_APPEL_TEMOIN:
                    receptionRequeteAFFICE_APPEL_TEMOIN(messageServeur);
                case ACTION_IMPOSSIBLE:
                    System.out.println("Non non non");
                    break;
                case DECONNEXION:
                    receptionRequeteDECONNEXION(messageServeur);
                    break;
                case HOTE_QUITTE:
                    receptionRequeteHOTE_QUITTE();
                    break;
                default:
                    System.out.println("[CLIENT] gestionCommunicationVersVueJeu() a rencontré une erreur dans le switch avec le code : " + messageServeur.getCodeServeur() + " !");
            }
        });
    }

    private void receptionRequeteNOUVEAU_PLATEAU(MessageServeur messageServeur) {
        PartieSnapshot partieClient = (PartieSnapshot) messageServeur.getContenue();
        if(jeSuisJack){
            partieClient.joueurChoisi = Joueur.JACK;
        }else{
            partieClient.joueurChoisi = Joueur.ENQUETEUR;
        }
        this.vueJeu.getGameplay().refreshFromSnap(partieClient);
    }

    private void receptionRequeteAFFICE_APPEL_TEMOIN(MessageServeur messageServeur) {
        PartieSnapshot partieClient = (PartieSnapshot) messageServeur.getContenue();
        if(jeSuisJack){
            partieClient.joueurChoisi = Joueur.JACK;
        }else{
            partieClient.joueurChoisi = Joueur.ENQUETEUR;
        }
        this.vueJeu.getGameplay().animationAppelTemoinReseau(partieClient);
        if(partieClient.gagnant != null){
            this.enJeu = false;
            this.vueJeu.stopGameLoop();
            this.vueJeu =  null;
            javax.swing.Timer timer = new javax.swing.Timer(5000, e->{
                this.vueLobby.revenirAuLobby();
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void receptionRequeteDECONNEXION(MessageServeur messageServeur) {
        this.enJeu = false;
        if(estHote){
            JOptionPane.showMessageDialog(vueLobby,messageServeur.getContenue().toString());
            if(this.vueJeu != null) {
                this.vueJeu.stopGameLoop();
                this.vueJeu = null;
            }
            this.vueLobby.setJoueur2Deconnecte(messageServeur.getContenue().toString());
            this.vueLobby.revenirAuLobby();
        }else{
            this.deconnexionForce = true;
            fermerConnexion();
        }
    }

    private void informerServeur(CommunicationLobbyCS codePOurServeur, Object contenue) {

        MessageServeur messageServeur = new MessageServeur(codePOurServeur, this.pseudoClient,contenue);

        try {
            out.writeObject(messageServeur);
        } catch (IOException e) {
            System.out.println("[CLIENT] informerServeur(), le message n'a pas pu être envoyé !");
        }
    }

    private void fermerConnexion() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (serveurSocket != null && !serveurSocket.isClosed()) serveurSocket.close();
        } catch (IOException e) {
            System.out.println("[CLIENT] fermerConnexion(), erreur lors de la fermeture de la connexion.");
        }
    }


    // Méthodes pour le lobby
    public void boutonPretAppuyer(Boolean etatBouton){
        informerServeur(BOUTON_PRET, etatBouton.toString());
    }

    public void nouveauMessage(String message){
        informerServeur(MESSAGE, message);
    }

    public void nouveauChoix(Boolean veutEtreJack) {
        informerServeur(BOUTON_CHOIX,veutEtreJack.toString());
    }

    public void joueurQuitte(){
        informerServeur(QUITTE, null);
        this.deconnexionForce = true;
    }


    // Méthodes pour le jeu
    public void clientPiocheAlibi(Object contenue){
        informerServeur(PIOCHE_ALIBI,contenue);
    }

    public void clientJoueJoker(Object contenue){
        informerServeur(JOKER,contenue);
    }

    public void clientDeplaceHolmes(Object contenue){
        informerServeur(HOLMES,contenue);
    }
    public void clientDeplaceWatson(Object contenue){
        informerServeur(WATSON,contenue);
    }
    public void clientDeplaceToby(Object contenue){
        informerServeur(TOBY,contenue);
    }



    public void clientTourneQuartier(Object contenue){
        informerServeur(ROTATION,contenue);
    }

    public void clientEchangeQuartier(Object contenue){
        informerServeur(ECHANGE,contenue);
    }

}