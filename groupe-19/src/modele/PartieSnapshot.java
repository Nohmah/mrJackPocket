package src.modele;

import java.io.Serializable;
import java.util.List;

public class PartieSnapshot implements Serializable {
    // Grille 3x3
    public Personnage[][] grillePersonnages;      // quel personnage en (i,j)
    public Orientation[][] grilleMurs;            // orientation du mur en (i,j)
    public boolean[][] grilleFaces;               // true = SUSPECT, false = VIDE
    public boolean[][] grilleAPivote;

    public List<Integer> positionsDetectives;     // position de chaque détective
    public List<Personnage> suspects;

    public List<Boolean> jetonsJoues;             // isJoue() de chaque jeton
    public List<Boolean> jetonsFaceRecto;         // faceRectoVisible de chaque jeton
    public List<CarteAlibi> cartesAlibiRestantes;

    public CarteAlibi derniereCarteAlibiPiochee;

    public int numeroTour;
    public int totalActionsJouees;
    public int sabliersDeJack;
    public Joueur joueurCourant;
    public Personnage identiteJack;
    public boolean jackVisibleCeTour;
    public boolean coursePoursuiteActive;
    public Joueur gagnant;
    public int niveauJack;
    public int niveauEnqueteur;
    public Joueur joueurChoisi;
    public boolean IAChoisi;
    public String difficulteIAChoisi;
}
