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
    private List<JetonAction> jetonsAction;
    private List<CarteAlibi> cartesAlibiPioche;
    private List<Detective> detectives;
    private List<Personnage> suspects;
    private Joueur joueurCourant;
    private Personnage identiteJack;
    public int sabliersDeJack;
    public int numeroTour = 0;

    //Suivi de tour
    private int totalActionsJouees;

    public Partie(){
        district = new District();

        // Création des listes
        jetonsAction = new ArrayList<>();
        cartesAlibiPioche = new ArrayList<>();
        detectives = new ArrayList<>();
        suspects = new ArrayList<>();

        // Création et ajout dans les listes des objets
        initialiserJetons();
        initialiserCartes();
        initialiserDetectives();
        initialiserSuspects();

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
            // L'enquêteur prive Jack du sablier du tour
        } else {
            sabliersDeJack ++; // Jack gagne le sablier du tour
            suspects.removeAll(personnagesVisibles);
        }
        //Apres réduction de suspects, on innocente les suspects supprimés.
        avantAppel.removeAll(suspects); // Obtention des gens plus suspects
        for(Personnage p : avantAppel){
            district.innocenter(p);
        }
        tourSuivant();
    }

    public void initialiserJetons() {
        jetonsAction.add(JetonAction.creerJeton(Action.ALIBI, Action.HOLMES));
        jetonsAction.add(JetonAction.creerJeton(Action.TOBY, Action.WATSON));
        jetonsAction.add(JetonAction.creerJeton(Action.ROTATION, Action.ECHANGE));
        jetonsAction.add(JetonAction.creerJeton(Action.ROTATION, Action.JOKER));
    }

    public void initialiserCartes(){
        cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.NORA_NOIRE, 2));
        cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.SGT_GOODLEY, 0));
        cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.JEREMY_BERT, 1));
        cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.WILLIAM_GULL, 1));
        cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.MISS_STEALTHY, 1));
        cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.JOHN_SMITH, 1));
        cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.INSPECTEUR_LESTRADE, 0));
        cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.JOHN_PIZER, 1));
        cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.JOSEPH_LANE, 1));
        Collections.shuffle(cartesAlibiPioche);
    }

    public void initialiserDetectives(){
        detectives.add(Detective.creerDetective(Detective.Type.HOLMES, 11));
        detectives.add(Detective.creerDetective(Detective.Type.WATSON, 3));
        detectives.add(Detective.creerDetective(Detective.Type.TOBY, 7));
    }

    public void initialiserSuspects(){
        suspects.addAll(Arrays.asList(Personnage.values()));
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
}
