package src.modele;
import java.util.*;

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
    public List<JetonAction> jetonsAction;
    public List<CarteAlibi> cartesAlibiPioche;
    public List<Detective> detectives;
    public List<Personnage> suspects;
    private Joueur joueurCourant;
    private Personnage identiteJack;
    public int sabliersDeJack;
    private final int MAX_SABLIER = 6;
    private Joueur gagnant = null;
    private boolean coursePoursuiteActive = false;

    //Suivi de tour
    private final int MAX_TOUR = 8;
    public int numeroTour = 0;
    private int totalActionsJouees;
    private boolean jackVisibleCeTour;


    public Partie(){
        district = new District();

        // Création des listes
        jetonsAction = new ArrayList<>();
        cartesAlibiPioche = new ArrayList<>();
        detectives = new ArrayList<>();
        suspects = new ArrayList<>();

        // Création et ajout dans les listes des objets
        PartieInit.initialiserJetons(this);
        PartieInit.initialiserCartes(this);
        PartieInit.initialiserDetectives(this);
        PartieInit.initialiserSuspects(this);

        initialiserIdentiteJack();
        tourSuivant();
    }

    public void initialiserIdentiteJack(){
        identiteJack = piocherCarteAlibi().getPersonnage();
        System.out.println("Mr Jack est "+ identiteJack.nom);
    }

    // -------------------------------------------------------------------------
    // Méthodes pour réaliser les actions au niveau du modèle
    // -------------------------------------------------------------------------
    public void jouerActionDeplacerDetective(Detective detective, int pas) {
        detective.deplacer(pas);
        System.out.println(detective.getType() + "avance de" + pas);
        apresAction();
    }

    public void jouerActionJoker(Detective detective) {
        if (joueurCourant == Joueur.ENQUETEUR) {
            jouerActionDeplacerDetective(detective, 1);
        }
        else {
            if (detective == null) {
                System.out.println("Mr. Jack choisit de ne déplacer aucun détective.");
            } else {
                jouerActionDeplacerDetective(detective, 1);
            }
        }
        verifFinDePartie();
        if(isPartieTerminee()) return;
        apresAction();
    }

    public void jouerActionRotationQuartier(int x, int y, int quarts) {
        Quartier q = district.get(x, y);
        if (q.getADejaPivoteTour()) {
            System.out.println("INTERDIT");
            return;
        }
        q.pivoter(quarts);
        q.setADejaPivoteTour(true);
        System.out.println("Quartier pivoté");
        apresAction();
    }

    public void jouerActionEchange(int x1, int y1, int x2, int y2) {
        district.echanger(x1, y1, x2, y2);
        System.out.println("Échange");
        apresAction();
    }

    public void jouerActionAlibi(){
        CarteAlibi cartePiochee = piocherCarteAlibi();
        if (cartePiochee == null) return;
        System.out.println("Carte alibi piochée:" + cartePiochee.getPersonnage());
        if (joueurCourant==Joueur.JACK){
            sabliersDeJack += cartePiochee.getSabliers();
        } else {
            suspects.remove(cartePiochee.getPersonnage());
            district.innocenter(cartePiochee.getPersonnage());
        }
        apresAction();
    }
    // -------------------------------------------------------------------------
    // Fin des méthodes pour réaliser les actions au niveau du modèle
    // -------------------------------------------------------------------------

    private void reinitialiserFlagsRotation() {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                district.get(x, y).setADejaPivoteTour(false);
            }
        }
    }

    public void appelATemoin(){
        HashSet<Personnage> personnagesVisibles = new HashSet<>();
        personnagesVisibles.addAll(district.personnagesVisiblesParDetective(detectives.get(0)));
        personnagesVisibles.addAll(district.personnagesVisiblesParDetective(detectives.get(1)));
        personnagesVisibles.addAll(district.personnagesVisiblesParDetective(detectives.get(2)));

        List<Personnage> avantAppel = new ArrayList<>(suspects);

        if(personnagesVisibles.contains(identiteJack)){
            suspects.retainAll(personnagesVisibles);
            jackVisibleCeTour = true;
            // L'enquêteur prive Jack du sablier du tour
        } else {
            jackVisibleCeTour = false;
            sabliersDeJack ++; // Jack gagne le sablier du tour
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

    public CarteAlibi piocherCarteAlibi(){
        if (cartesAlibiPioche.isEmpty()) return null;
        return cartesAlibiPioche.remove(0);
    }

    public void changerJoueur() {
        joueurCourant = (joueurCourant == Joueur.JACK) ? Joueur.ENQUETEUR : Joueur.JACK;
    }

    public void apresAction(){
        totalActionsJouees++;

        switch(totalActionsJouees){
            case 1, 3:
                changerJoueur();
                break;
            case 4:
                //appelATemoin();
                break;
            default:
                break;

        }
    }

    public void tourSuivant(){
        if(isPartieTerminee()) return;
        numeroTour++;
        reinitialiserFlagsRotation();
        totalActionsJouees = 0;
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

    public List<JetonAction> getJetonsActions(){;
        return jetonsAction;
    }

    public List<Action> getActionsPossibles(){
        List<Action> actionsPossibles = new ArrayList<>();
        for(JetonAction j : jetonsAction){
            if(!j.isJouee()){
                actionsPossibles.add(j.getActionVisible());
            }
        }
        return actionsPossibles;
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
            //Si les deux camps atteignent leur but en même temps, on
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
