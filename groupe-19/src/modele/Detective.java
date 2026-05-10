package src.modele;

/**
 * Représente et gère la position des (trois) détectives : Holmes, Watson et le chien Toby.
 * Les détectives se déplacent sur un circuit périphérique de 12 espaces
 * autour du district 3×3.
 *
 * Circuit (sens horaire, 0 = haut-gauche) :
 *
 *              [0]   [1]   [2]
 *
 *      [11]   (0,0) (0,1) (0,2)   [3]
 *      [10]   (1,0) (1,1) (1,2)   [4]
 *       [9]   (2,0) (2,1) (2,2)   [5]
 *
 *              [8]   [7]   [6]
 *
 * Chaque détective est placé sur un espace, d’où il fait face à un quartier (x,y) avec une certaine orientation.
 **/

public class Detective {

    public enum Type{
        HOLMES("Holmes"),
        WATSON("Watson"),
        TOBY("Toby");

        public final String nom;

        /** Constructeur **/
        Type(String nom){
            this.nom = nom;
        }
    }

    /**
     * Mapping des 12 positions possible des détectives autour du District.
     * pos <- {x, y, orientation} : x, y représente la position du quartier dans le district,
     * orientation représente l'orientation du détective.
     *
     * Index d'orientation 0 = NORD, 1 = EST, 2 = SUD, 3 = OUEST.
     **/
    public static final int[][] CIRCUIT = {
            /* 0 */ {0, 0, 0}, // face au quartier 0,0 depuis le NORD.
            /* 1 */ {0, 1, 0}, // face au quartier 0,1 depuis le NORD.
            /* 2 */ {0, 2, 0}, // face au quartier 0,2 depuis le NORD.

            /* 3 */ {0, 2, 1}, // face au quartier 0,2 depuis l'EST.
            /* 4 */ {1, 2, 1}, // face au quartier 1,2 depuis l'EST.
            /* 5 */ {2, 2, 1}, // face au quartier 2,2 depuis l'EST.

            /* 6 */ {2, 2, 2}, // face au quartier 2,2 depuis le SUD.
            /* 7 */ {2, 1, 2}, // face au quartier 2,1 depuis le SUD.
            /* 8 */ {2, 0, 2}, // face au quartier 2,0 depuis le SUD.

            /* 9 */ {2, 0, 3}, // face au quartier 2,0 depuis l'OUEST.
            /*10 */ {1, 0, 3}, // face au quartier 1,0 depuis l'OUEST.
            /*11 */ {0, 0, 3}, // face au quartier 0,0 depuis l'OUEST.
    };

    public static final int NB_POSITIONS = 12;

    private final Type type;
    private int position;

    /** Constructeur **/
    public Detective(Type type, int position){
        this.type = type;
        this.position = position;
    }

    /** Construit et renvoie l'objet Detective **/
    public static Detective creerDetective(Type type, int position){
        return new Detective(type, position);
    }

    /** Déplace le détective dans le sens horaire, de 1 ou 2 pas **/
    public void deplacer(int pas){
        position = (position + pas) % NB_POSITIONS;
    }

    /** Retourne le type de détective **/
    public Type getType(){
        return this.type;
    }

    /** Retourne la position du détective **/
    public int getPosition(){
        return this.position;
    }

    /** Renvoi la ligne du quartier observé par le détective **/
    public int getQx(){
        return CIRCUIT[position][0];
    }

    /** Renvoi la colonne du quartier observé par le détective */
    public int getQy(){
        return CIRCUIT[position][1];
    }

    /** Renvoi l'orientation du détective */
    public Orientation getOrientation(){
        return Orientation.CARDINAUX[CIRCUIT[position][2]];
    }

    /** Constructeur de copie pour l'IA **/
    public Detective(Detective d) {
        this.type = d.getType();
        this.position = d.getPosition();
    }
}