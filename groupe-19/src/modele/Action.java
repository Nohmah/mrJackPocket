package src.modele;

/**
 * Liste les 7 actions des jetons Action.
 **/

public enum Action {

    HOLMES("Holmes"),
    WATSON("Watson"),
    TOBY("Toby"),
    JOKER("Joker"),
    ROTATION("Rotation"),
    ECHANGE("Échange"),
    ALIBI("Alibi");

    public final String nom;

    /** Constructeur **/
    Action(String nom){
        this.nom = nom;
    }
}
