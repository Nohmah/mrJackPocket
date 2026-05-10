package src.modele;

/**
 * Liste les deux types de joueurs possibles
 **/

public enum Joueur {
    ENQUETEUR("L'Enquêteur"),
    JACK("Mr. Jack");

    private final String nom;

    Joueur(String nom) {
        this.nom = nom;
    }

    public String getNom() {
        return nom;
    }
}