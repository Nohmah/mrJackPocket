package src.modele;

/**
 * Liste les 9 'personnages-suspects' qui ont chacun un nom et une couleur.
 **/

public enum Personnage {
    NORA_NOIRE("Nora Noire", Couleur.ROSE),
    SGT_GOODLEY("Sgt. Goodley", Couleur.NOIR),
    JEREMY_BERT("Jeremy Bert", Couleur.ORANGE),
    WILLIAM_GULL("William Gull", Couleur.VIOLET),
    MISS_STEALTHY("Miss Stealthy", Couleur.VERT),
    JOHN_SMITH("John Smith", Couleur.JAUNE),
    INSPECTEUR_LESTRADE("Insp. Lestrade", Couleur.BLEU),
    JOHN_PIZER("John Pizer", Couleur.BLANC),
    JOSEPH_LANE("Joseph Lane", Couleur.GRIS);

    public final String nom;
    public final Couleur couleur;

    /** Constructeur **/
    Personnage(String nom, Couleur couleur){
        this.nom = nom;
        this.couleur = couleur;
    }

    public enum Couleur{
        ROSE, NOIR, ORANGE, VIOLET, VERT, JAUNE, BLEU, BLANC, GRIS
    }
}
