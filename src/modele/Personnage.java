package src.modele;

/**
 * Liste les 9 'personnages-suspects' qui ont chacun un nom et une couleur.
 **/

public enum Personnage {
    NORA_NOIRE("Nora Noire", Couleur.ROSE, "A1"),
    SGT_GOODLEY("Sgt. Goodley", Couleur.NOIR, "A3"),
    JEREMY_BERT("Jeremy Bert", Couleur.ORANGE, "A2"),
    WILLIAM_GULL("William Gull", Couleur.VIOLET, "A4"),
    MISS_STEALTHY("Miss Stealthy", Couleur.VERT, "A7"),
    JOHN_SMITH("John Smith", Couleur.JAUNE, "A8"),
    INSPECTEUR_LESTRADE("Insp. Lestrade", Couleur.BLEU, "A9"),
    JOHN_PIZER("John Pizer", Couleur.BLANC, "A5"),
    JOSEPH_LANE("Joseph Lane", Couleur.GRIS, "A6");

    public final String nom;
    public final Couleur couleur;
    public final String image;

    /** Constructeur **/
    Personnage(String nom, Couleur couleur, String image){
        this.nom = nom;
        this.couleur = couleur;
        this.image = image;
    }

    public enum Couleur{
        ROSE, NOIR, ORANGE, VIOLET, VERT, JAUNE, BLEU, BLANC, GRIS
    }
}
