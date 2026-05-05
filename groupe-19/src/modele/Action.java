package src.modele;

public enum Action {

    HOLMES("Holmes"),
    WATSON("Watson"),
    TOBY("Toby"),
    JOKER("Joker"),
    ROTATION("Rotation"),
    ECHANGE("Echange"),
    ALIBI("Alibi");

    public final String nom;

    Action(String nom){
        this.nom = nom;
    }
}
