package src.modele;

/**
 * Liste les orientations possibles des détectives et des murs des quartiers.
 * L'orientation 'AUCUN' n'est utilisé que pour les murs.
 * Un quartier dont le mur a pour orientation 'AUCUN' n'a pas de mur.
 **/

public enum Orientation {
    AUCUN(-1),
    NORD(0),
    EST(1),
    SUD(2),
    OUEST(3);

    public final int index;
    public static final Orientation[] CARDINAUX = {NORD, EST, SUD, OUEST};

    /** Constructeur **/
    Orientation(int index){
        this.index = index;
    }

    /** Applique une rotation horaire d'un nombre donné de quarts de tour **/
    public Orientation rotation(int quarts){
        if(this == AUCUN) return AUCUN;
        return CARDINAUX[(index + quarts + 4) % 4];
    }
}
