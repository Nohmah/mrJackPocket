package src.modele;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;

/**
 * Représente une partie.
 **/

public class Partie {

    public enum Joueur {
        ENQUETEUR("l'Enquêteur"),
        JACK("Mr. Jack");

        private final String nom;

        Joueur(String nom) {
            this.nom = nom;
        }

        public String getNom() {
            return nom;
        }
    }

    public District district;
    public final PartieActions actions;
    public List<JetonAction> jetonsAction;
    public List<CarteAlibi> cartesAlibiPioche;
    public List<Detective> detectives;
    public List<Personnage> suspects;
    public Joueur joueurCourant;
    public Personnage identiteJack;
    public int sabliersDeJack;
    private final int MAX_SABLIER = 6;
    private Joueur gagnant = null;
    private boolean coursePoursuiteActive = false;

    //Suivi de tour
    private final int MAX_TOUR = 8;
    public int numeroTour = 0;
    private int totalActionsJouees;
    private boolean jackVisibleCeTour;

    //pour l'ia et le thread
    private Ia ia = new Ia();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean IaEnCours = false;

    /** Constructeur **/
    public Partie(){
        actions = new PartieActions(this);
        jetonsAction = new ArrayList<>();
        cartesAlibiPioche = new ArrayList<>();
        detectives = new ArrayList<>();
        suspects = new ArrayList<>();
        initialiserPartie();
        tourSuivant();
    }

    public Partie(Partie p) {
        this.district = p.district;
        this.actions = new PartieActions(this);
        this.jetonsAction = new ArrayList<>(p.jetonsAction);
        this.cartesAlibiPioche = new ArrayList<>(p.cartesAlibiPioche);
        this.detectives = new ArrayList<>(p.detectives);
        this.suspects = new ArrayList<>(p.suspects);
        this.joueurCourant = p.joueurCourant;
        this.identiteJack = p.identiteJack;
        this.sabliersDeJack = p.sabliersDeJack;
        this.numeroTour = p.numeroTour;
        this.totalActionsJouees = p.totalActionsJouees;
        this.jackVisibleCeTour = p.jackVisibleCeTour;
        this.coursePoursuiteActive = p.coursePoursuiteActive;
        this.gagnant = p.gagnant;
    }

    /** Initialise les élements de la partie **/
    private void initialiserPartie() {
        district = District.creerDistrict();
        PartieInit.initialiserJetons(this);
        PartieInit.initialiserCartesAlibis(this);
        PartieInit.initialiserDetectives(this);
        PartieInit.initialiserSuspects(this);
        PartieInit.initialiserIdentiteJack(this);
    }

    /** Réinitialise la partie **/
    public void reset() {
        // Réinitialiser les variables
        // IaEnCours = false; ? A voir avec la team IA
        gagnant = null;
        coursePoursuiteActive = false;
        numeroTour = 0;
        //totalActionsJouees = 0; Commenté car à chaque tourSuivant() on remet à 0 totalActionsJouees (donc redondant)
        jackVisibleCeTour = false;
        sabliersDeJack = 0;

        jetonsAction.clear();
        cartesAlibiPioche.clear();
        detectives.clear();
        suspects.clear();
        initialiserPartie();
        tourSuivant();
    }

    public void appelATemoin(){
        HashSet<Personnage> personnagesVisibles = new HashSet<>();
        for (Detective d : detectives) {
            personnagesVisibles.addAll(district.personnagesVisiblesParDetective(d));
        }

        List<Personnage> avantAppel = new ArrayList<>(suspects);

        if(personnagesVisibles.contains(identiteJack)){
            suspects.retainAll(personnagesVisibles);
            jackVisibleCeTour = true;
            // L'enquêteur prive Jack du sablier du tour
        } else {
            jackVisibleCeTour = false;
            sabliersDeJack ++; // Jack gagne le sablier du tour
            System.out.println("Jack n'est pas visible, il gagne le sablier du tour. Il est a " + sabliersDeJack + " sabliers");
            suspects.removeAll(personnagesVisibles);
        }
        //Apres réduction de suspects, on innocente les suspects supprimés.
        avantAppel.removeAll(suspects); // Obtention des gens plus suspects
        for(Personnage p : avantAppel){
            district.innocenter(p);
        }
        verifFinDePartie();
        if(!isPartieTerminee()) tourSuivant();
    }

    public void changerJoueur() {
        joueurCourant = (joueurCourant == Joueur.JACK) ? Joueur.ENQUETEUR : Joueur.JACK;
    }

    public void apresAction(){
        totalActionsJouees++;

        switch(totalActionsJouees){
            case 1, 3:
                System.out.println("Le joueur change. Le joueur est maintenant" +
                        ((joueurCourant == Joueur.JACK) ? Joueur.ENQUETEUR : Joueur.JACK));
                changerJoueur();
                //lanceIa();
                break;
            case 4:
                //appelATemoin(); Ne pas faire, en tout cas pour l'instant, car ça court circuite la vue et plus rien ne va...
                //lanceIa();
                break;
            default:
                break;
        }
        /*if (!isPartieTerminee()) {
            lanceIa();
        }*/
    }

    public void tourSuivant(){
        if(isPartieTerminee()) return;
        numeroTour++;
        totalActionsJouees = 0;
        district.reinitialiserFlagsRotation();
        if(numeroTour % 2 != 0){
            for(JetonAction j : jetonsAction){
                j.lancer();
                System.out.println("Jeton lancé sur : " + j.getActionVisible());
            }
            joueurCourant = Joueur.ENQUETEUR;
        } else {
            for(JetonAction j : jetonsAction){
                j.retourner();
                System.out.println("Jeton retourné sur : " + j.getActionVisible());
            }
            joueurCourant = Joueur.JACK;
        }
    }

    private void lanceIa() {
        //System.out.println("entre dans Ia");
        if(IaEnCours) return; // Empêche de lancer plusieurs tours d'IA en même temps
        IaEnCours = true;
        //System.out.println("Ia va calculer son coup...");
        boolean estJack = (joueurCourant == Joueur.JACK);

        CompletableFuture
            .supplyAsync(() -> ia.choisirAction(this, estJack), executor)
            .thenAccept(iaCoup -> {
                System.out.println("Ia a choisi son coup.");
                SwingUtilities.invokeLater(() -> {
                    IaEnCours = false;
                    jouerCoup(iaCoup); 
                });
            }); 
    }

    private void jouerCoup(CoupIa coupIa) {
        //coupIa.afficher(); // Affiche le coup choisi par l'IA dans la console pour le debug
        // Logique pour jouer l'action choisie par l'IA
        switch (coupIa.action) {
            case HOLMES:
                actions.deplacerDetective(detectives.get(0), coupIa.para1 + 1); // +1 car les déplacements commencent à 1
                // Logique pour l'action HOLMES
                break;
            case WATSON:
                // Logique pour l'action WATSON
                actions.deplacerDetective(detectives.get(1), coupIa.para1 + 1);
                break;
            case TOBY:
                // Logique pour l'action TOBY
                actions.deplacerDetective(detectives.get(2), coupIa.para1 + 1);
                break;
            case JOKER:
                if(coupIa.para2 == 0){
                    actions.joker(null); // Jack choisit de ne déplacer aucun détective
                } else {
                    actions.joker(detectives.get(coupIa.para1));
                }
                // Logique pour l'action JOKER
                break;
            case ROTATION:
                // (car il peut y avoir 2 jetons face rotation en même temps au cours d'un tour).
                for(int i = 0; i < jetonsAction.size(); i++){
                    if(jetonsAction.get(i).getActionVisible() == Action.ROTATION && !jetonsAction.get(i).isJoue()){
                        actions.rotationQuartier(i, coupIa.para1 / 3, coupIa.para1 % 3, coupIa.para2);
                        break;
                    }
                }
                // Logique pour l'action ROTATION
                break;
            case ECHANGE:
                actions.echange(coupIa.para1 / 3, coupIa.para1 % 3, coupIa.para2 / 3, coupIa.para2 % 3);
                // Logique pour l'action ECHANGE
                break;
            case ALIBI:
                //actions.piocherCarteAlibi();
                actions.alibi();
                // Logique pour l'action ALIBI
                break;
        }
        //lanceIa();
    }

    /**
     *
     * Logique de fin de partie.*
     *
     */

    public void verifFinDePartie(){
        if(gagnant != null) return;
        //En cas de fin de course poursuite, on vérifie si Jack a été trouvé ou non.
        if(coursePoursuiteActive){
            verifFinCoursePoursuite();
        } else {
            //Si les deux camps atteignent leur but en même temps, la course poursuite est lancée
            if((suspects.size() == 1) && sabliersDeJack >= MAX_SABLIER){
                coursePoursuiteActive = true;
                System.out.println("Début de course poursuite !!!");
            }else if(suspects.size() == 1){
                gagnant = Joueur.ENQUETEUR;
                System.out.println("----- Victoire des Détectives ! -----");
            }else if(sabliersDeJack >= MAX_SABLIER){
                gagnant = Joueur.JACK;
                System.out.println("----- Victoire de Jack ! -----");
            }
        }
    }

    public void verifFinCoursePoursuite(){
        if(jackVisibleCeTour){
            gagnant = Joueur.ENQUETEUR;
            System.out.println("Jack à été trouvé.\n ----- Victoire des Détectives -----\n");
            return;
        }
        if(numeroTour >= MAX_TOUR){
                gagnant = Joueur.JACK;
                System.out.println("Jack à pris la fuite.\n ----- Victoire de Jack -----\n");
        }
    }

    public boolean isPartieTerminee(){
        return gagnant != null;
    }

    public Joueur getGagnant(){
        return gagnant;
    }

}
