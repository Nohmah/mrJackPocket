package src.modele;

/**
 * Représente une carte Alibi, association d'un Personnage à un nombre de sabliers
 * */
public class CarteAlibi {
    private final Personnage personnage;
    private final int sabliers;

    /** Constructeur */
    private CarteAlibi(Personnage personnage, int sabliers){
        this.personnage = personnage;
        this.sabliers = sabliers;
    }

    /** Construit l'objet CarteAlibi */
    public static CarteAlibi creerCarteAlibi(Personnage personnage, int sabliers){
        return new CarteAlibi(personnage, sabliers);
    }

    /** Retourne le personnage */
    public Personnage getPersonnage() {
        return personnage;
    }

    /** Retourne le nombre de sabliers */
    public int getSabliers() {
        return sabliers;
    }
}
