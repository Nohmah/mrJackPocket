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

    public District district;
    public final PartieActions actions;
    public List<JetonAction> jetonsAction;
    public List<CarteAlibi> cartesAlibiPioche;
    public List<Detective> detectives;
    public List<Personnage> suspects;
    public Joueur joueurCourant;
    public Joueur joueurChoisi;
    public boolean IAChoisi;
    public String difficulteIAChoisi;
    public Personnage identiteJack;
    public int sabliersDeJack;
    private final int MAX_SABLIER = 6;
    public volatile Joueur gagnant = null;
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
    public int niveauJack;  // niveau de son IA (-1 = manuel, 0 = random, 1 = facile, 2 = moyen)
    public int niveauEnqueteur; // niveau de l'IA de l'enquêteur (-1 = manuel, 0 = random, 1 = facile, 2 = moyen)

    /** Constructeur **/
    public Partie(int niveauJack, int niveauEnqueteur){
        this.niveauJack = niveauJack;
        this.niveauEnqueteur = niveauEnqueteur;
        actions = new PartieActions(this);
        jetonsAction = new ArrayList<>();
        cartesAlibiPioche = new ArrayList<>();
        detectives = new ArrayList<>();
        suspects = new ArrayList<>();
        initialiserPartie();
        tourSuivant();
        verifTourIa();
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
        gagnant = null;
        coursePoursuiteActive = false;
        numeroTour = 0;
        jackVisibleCeTour = false;
        sabliersDeJack = 0;

        jetonsAction.clear();
        cartesAlibiPioche.clear();
        detectives.clear();
        suspects.clear();
        initialiserPartie();
        tourSuivant();
        verifTourIa();
    }

    /**
     * Termine le tour courant : appel à témoin, vérification de fin de partie,
     * et préparation du tour suivant si la partie continue.
     * Point d'entrée unique depuis Gameplay — remplace la logique dispersée dans endTurn().
     */
    public void terminerTour() {
        appelATemoin();
        // appelATemoin() appelle déjà verifFinDePartie() puis tourSuivant() si la partie continue.
        // Gameplay lit ensuite isPartieTerminee() et numeroTour pour décider de la suite.
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
        } else {
            jackVisibleCeTour = false;
            sabliersDeJack++;
            if (!this.estSimulation) System.out.println("Jack n'est pas visible, il gagne le sablier du tour. Il est a " + sabliersDeJack + " sabliers");
            suspects.removeAll(personnagesVisibles);
        }
        // Après réduction de suspects, on innocente les suspects supprimés.
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
                break;
            case 4:
                appelATemoin();
                break;
            default:
                break;
        }
        if (!isPartieTerminee()) verifTourIa();
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
    }

    public void verifTourIa(){
        if(isPartieTerminee()) return;

        if(joueurCourant == Joueur.JACK){
            ia.setDifficulte(niveauJack);
        } else {
            ia.setDifficulte(niveauEnqueteur);
        }

        if((joueurCourant == Joueur.JACK && niveauJack != -1) || (joueurCourant == Joueur.ENQUETEUR && niveauEnqueteur != -1)){
            lanceIa();
        }
    }

    public void lanceIa() {
        if(IaEnCours || isPartieTerminee() || actions.getActionsPossibles().isEmpty()) return;
        if (estSimulation) return;

        boolean estJack = (joueurCourant == Joueur.JACK);

        if (ia.difficulte == -1) {
            return;
        }

        IaEnCours = true;
        CompletableFuture
                .supplyAsync(() -> {
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    CoupIa coupChoisi = ia.choisirAction(this, estJack);
                    return coupChoisi;
                }, executor)
                .thenAccept(iaCoup -> {
                    SwingUtilities.invokeLater(() -> {
                        IaEnCours = false;
                        if (iaCoup != null && iaCoup.action != null) {
                            jouerCoup(iaCoup);
                        }
                        else {
                            System.err.println("\t L'IA n'a pas choisi d'action valide !");
                        }
                    });
                });
    }

    public double simulerEtNoter(CoupIa coup, boolean estJack) {
        Partie copie = new Partie(this);
        copie.jouerCoup(coup);
        if(estJack){
            return EvaluateurIa.jeSuisJack(copie);
        } else {
            return EvaluateurIa.jeSuisEnqueteur(copie);
        }
    }

    private void jouerCoup(CoupIa coupIa) {
        switch (coupIa.action) {
            case HOLMES:
                actions.deplacerDetective(detectives.get(0), coupIa.para1 + 1);
                break;
            case WATSON:
                actions.deplacerDetective(detectives.get(1), coupIa.para1 + 1);
                break;
            case TOBY:
                actions.deplacerDetective(detectives.get(2), coupIa.para1 + 1);
                break;
            case JOKER:
                if(coupIa.para2 == 0){
                    actions.joker(null);
                } else {
                    actions.joker(detectives.get(coupIa.para1));
                }
                break;
            case ROTATION:
                for(int i = 0; i < jetonsAction.size(); i++){
                    if(jetonsAction.get(i).getActionVisible() == Action.ROTATION && !jetonsAction.get(i).isJoue()){
                        actions.rotationQuartier(i, coupIa.para1 / 3, coupIa.para1 % 3, coupIa.para2);
                        break;
                    }
                }
                break;
            case ECHANGE:
                actions.echange(coupIa.para1 / 3, coupIa.para1 % 3, coupIa.para2 / 3, coupIa.para2 % 3);
                break;
            case ALIBI:
                actions.alibi();
                break;
        }
    }

    /**
     *
     * Logique de fin de partie.
     *
     */

    /** Vérifie si la partie est terminée **/
    public void verifFinDePartie(){
        if(gagnant != null) return;
        if(coursePoursuiteActive){
            verifFinCoursePoursuite();
        } else {
            if((suspects.size() == 1) && sabliersDeJack >= MAX_SABLIER){
                if (jackVisibleCeTour){
                    declarerGagnant(Joueur.ENQUETEUR);
                }
                else if (numeroTour == MAX_TOUR){
                    declarerGagnant(Joueur.JACK);
                } else {
                    coursePoursuiteActive = true;
                    System.out.println("Début de course poursuite !!!");
                }
            }
            else if(suspects.size() == 1){
                declarerGagnant(Joueur.ENQUETEUR);
            }
            else if(sabliersDeJack >= MAX_SABLIER){
                declarerGagnant(Joueur.JACK);
            }
            else if (numeroTour == MAX_TOUR) {
                declarerGagnant(Joueur.JACK);
            }
        }
    }

    /** Affecte à [gagnant] le joueur - Jack ou Enquêteur - qui a gagné **/
    private void declarerGagnant(Joueur vainqueur) {
        this.gagnant = vainqueur;
        if (!this.estSimulation) {
            if (this.gagnant == Joueur.ENQUETEUR){
                System.out.println("----- Victoire de l'Enquêteur ! -----");
            } else {
                System.out.println("----- Victoire de Jack ! -----");
            }
        }
    }

    /** Vérifie si la course poursuite est terminée **/
    public void verifFinCoursePoursuite(){
        if(jackVisibleCeTour){
            System.out.println("Fin de la course poursuite.");
            declarerGagnant(Joueur.ENQUETEUR);
            coursePoursuiteActive = false;
            return;
        }
        if(numeroTour == MAX_TOUR){
            System.out.println("Fin de la course poursuite.");
            declarerGagnant(Joueur.JACK);
            coursePoursuiteActive = false;
        }
    }

    /** Vérifie s'il y a un gagnant **/
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

        this.jetonsAction = new ArrayList<>();
        for (JetonAction j : p.jetonsAction) {
            this.jetonsAction.add(new JetonAction(j));
        }

        this.detectives = new ArrayList<>();
        for (Detective d : p.detectives) {
            this.detectives.add(new Detective(d));
        }

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