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
    public int numeroTour;

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

        //La suite est à revoir...
        initialiserIdentiteJack();
        numeroTour = 1;
        joueurCourant = Joueur.ENQUETEUR;
    }

    public void initialiserIdentiteJack(){
        identiteJack = piocherCarteAlibi().getPersonnage();
    }

    /**Méthode pour réaliser les actions au niveau du modele*/
    public void jouerAction(Action action){
        if (action==Action.ALIBI){
            CarteAlibi cartePiochee = piocherCarteAlibi();
            if (cartePiochee == null) return;
            if (joueurCourant==Joueur.JACK){
                sabliersDeJack += cartePiochee.getSabliers();
            } else {
                suspects.remove(cartePiochee.getPersonnage());
                district.innocenter(cartePiochee.getPersonnage());
                //Trouver le quartier qui a ce personnage et si ça n'est pas déjà fait, le retourner
                // (à l'aide d'une nouvelle méthode de District)
                //Il faut repaint
            }
        }
    }

    public boolean appelATemoin(){
        List<Personnage> personnagesVisibles = new ArrayList<>();
        for (int i=0; i<3; i++){
            personnagesVisibles.addAll(district.personnagesVisiblesParDetective(detectives.get(i)));
        }
        return personnagesVisibles.contains(identiteJack);
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
}
