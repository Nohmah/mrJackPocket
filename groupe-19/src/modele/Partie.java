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
    public boolean estSimulation = false;

    /** Constructeur **/
    public Partie(){
        actions = new PartieActions(this);
        jetonsAction = new ArrayList<>();
        cartesAlibiPioche = new ArrayList<>();
        detectives = new ArrayList<>();
        suspects = new ArrayList<>();
        initialiserPartie();
        tourSuivant();
        //lanceIa();
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

    /** Réalise l'appel à témoin (deuxième étape du jeu) et vérifie si la partie est terminée **/
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
            if (!this.estSimulation) System.out.println("Jack n'est pas visible, il gagne le sablier du tour. Il est a " + sabliersDeJack + " sabliers");
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

    /** Change [joueurCourant] pour l'autre joueur **/
    public void changerJoueur() {
        joueurCourant = (joueurCourant == Joueur.JACK) ? Joueur.ENQUETEUR : Joueur.JACK;
    }

    /** Réalise le changement de joueur après un certain nombre de jetons Actions utilisés **/
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
                appelATemoin(); // Ne pas faire, en tout cas pour l'instant, car ça court circuite la vue et plus rien ne va...
                //lanceIa();
                break;
            default:
                if (!isPartieTerminee()) lanceIa();
                break;
        }

    }

    /** Prépare le tour suivant : incrémente [numeroTour], flag les tuiles comme n'ayant pas pivoté,
     * lance ou retourne les jetons Actions et définit le joueur qui va commencer le tour **/
    public void tourSuivant(){
        if(isPartieTerminee()) return;
        numeroTour++;
        totalActionsJouees = 0;
        district.reinitialiserFlagsRotation();
        if(numeroTour % 2 != 0){
            for(JetonAction j : jetonsAction){
                j.lancer();
                if (!this.estSimulation) System.out.println("Jeton lancé sur : " + j.getActionVisible());
            }
            joueurCourant = Joueur.ENQUETEUR;
            if (!this.estSimulation) System.out.println("Le joueur est l'Enquêteur");
        } else {
            for(JetonAction j : jetonsAction){
                j.retourner();
                if (!this.estSimulation) System.out.println("Jeton retourné sur : " + j.getActionVisible());
            }
            joueurCourant = Joueur.JACK;
            if (!this.estSimulation) System.out.println("Le joueur est Jack");
        }
        //lanceIa();
    }

    public void lanceIa() {
        // Empêche de lancer plusieurs tours d'IA en même temps, si la partie est finie, ou s'il n'y a plus d'actions
        if(IaEnCours || isPartieTerminee() || actions.getActionsPossibles().isEmpty()) return;
        if (estSimulation) return;

        // --- NOUVEAU BLOC DE SÉCURITÉ ---
        // Vérifie si le joueur courant doit bien être contrôlé par l'IA selon la difficulté choisie
        boolean estJack = (joueurCourant == Joueur.JACK);

        // Exemple de logique : Si on est en difficulté "Manuel" (par ex: difficulte = 5), on ne lance pas l'IA
        if (ia.difficulte == -1) { // Remplace -1 par la valeur de ton mode "Manuel" si tu en as créé un
            return;
        }
        // --------------------------------

        IaEnCours = true;

        CompletableFuture
                .supplyAsync(() -> {
                    try { Thread.sleep(1000); } catch (Exception e) {}
                    return ia.choisirAction(this, estJack);
                }, executor)
                .thenAccept(iaCoup -> {
                    SwingUtilities.invokeLater(() -> {
                        IaEnCours = false;
                        if (iaCoup != null && iaCoup.action != null) {
                            jouerCoup(iaCoup);
                        }
                    });
                });
    }

    public double simulerEtNoter(CoupIa coup, boolean estJack) {
        // Simule un coup sur une copie de la partie et renvoie la note de l'évaluateur
        Partie copie = new Partie(this);
        copie.jouerCoup(coup);
        if(estJack){
            return EvaluateurIa.jeSuisJack(copie);
        } else {
            return EvaluateurIa.jeSuisEnqueteur(copie);
        }
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

    /** Vérifie si la partie est terminé **/
    public void verifFinDePartie(){
        if(gagnant != null) return;
        //En cas de fin de course poursuite, on vérifie si Jack a été trouvé ou non.
        if(coursePoursuiteActive){
            verifFinCoursePoursuite();
        } else {
            // "Il arrive parfois que les deux joueurs atteignent leur but en même temps."
            if((suspects.size() == 1) && sabliersDeJack >= MAX_SABLIER){
                // "Si cela se produit à la fin du huitième tour : L’Enquêteur gagne la partie si
                // Mr. Jack est visible. Mr. Jack gagne la partie s’il est invisible."
                if (numeroTour==MAX_TOUR){
                    if (jackVisibleCeTour){
                        declarerGagnant(Joueur.ENQUETEUR);
                    } else {
                        declarerGagnant(Joueur.JACK);
                    }
                } else {
                    // "Si cela se produit avant le huitième tour, la partie continue et une course poursuite sans
                    // merci s’engage entre l’Enquêteur et Mr. Jack !"
                    coursePoursuiteActive = true;
                    System.out.println("Début de course poursuite !!!");
                }
            }
            // "Pour l’Enquêteur : Après l’Appel à témoin, il ne doit subsister qu’un seul Suspect.
            // Ce suspect est forcément le coupable !"
            else if(suspects.size() == 1){
                declarerGagnant(Joueur.ENQUETEUR);
            }
            // "Pour Mr. Jack : Après l’appel à témoin, il doit totaliser au moins six sabliers"
            else if(sabliersDeJack >= MAX_SABLIER){
                declarerGagnant(Joueur.JACK);
            }
            // "Si aucun des deux joueurs n’a atteint son objectif à la fin du tour 8, Jack est vainqueur"
            else if (numeroTour==MAX_TOUR) {
                declarerGagnant(Joueur.JACK);
            }
        }
    }

    /** Affecte à [gagnant] le joueur - Jack ou Enquêteur - qui a gagné, et print cette information **/
    private void declarerGagnant(Joueur vainqueur) {
        this.gagnant = vainqueur;
        if (!this.estSimulation) {
            if (this.gagnant==Joueur.ENQUETEUR){
                System.out.println("----- Victoire de l'Enquêteur ! -----");
            } else {
                System.out.println("----- Victoire de Jack ! -----");
            }
        }
    }

    /** Vérifie si la course poursuite est terminé **/
    public void verifFinCoursePoursuite(){
        // "L’Enquêteur gagne dès qu’il termine un tour avec Mr. Jack visible."
        if(jackVisibleCeTour){
            System.out.println("Fin de la course poursuite.");
            declarerGagnant(Joueur.ENQUETEUR);
            coursePoursuiteActive = false;
            return;
        }
        // "Mr. Jack gagne s'il est resté invisible jusqu’à la fin du huitième tour."
        if(numeroTour == MAX_TOUR){
            System.out.println("Fin de la course poursuite.");
            declarerGagnant(Joueur.JACK);
            coursePoursuiteActive = false;
        }
    }

    /** Vérifie si il y a un gagnant **/
    public boolean isPartieTerminee(){
        return gagnant != null;
    }

    /** Renvoie le joueur qui a gagné **/
    public Joueur getGagnant(){
        return gagnant;
    }

    // Constructeur de copie pour l'IA
    public Partie(Partie p) {
        this.estSimulation = true;
        this.district = new District(p.district);
        this.actions = new PartieActions(this);

        // --- COPIE PROFONDE DES JETONS ---
        this.jetonsAction = new ArrayList<>();
        for (JetonAction j : p.jetonsAction) {
            this.jetonsAction.add(new JetonAction(j));
        }

        // --- COPIE PROFONDE DES DÉTECTIVES ---
        this.detectives = new ArrayList<>();
        for (Detective d : p.detectives) {
            this.detectives.add(new Detective(d));
        }

        // Les listes suivantes peuvent rester en copie superficielle car
        // on ne modifie pas l'état interne des cartes ou des suspects.
        this.cartesAlibiPioche = new ArrayList<>(p.cartesAlibiPioche);
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
}
