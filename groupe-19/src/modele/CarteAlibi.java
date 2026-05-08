package src.modele;

/**
 * Représente une carte Alibi, une association d'un Personnage à un nombre de sabliers.
 **/

public class CarteAlibi {
    private final Personnage personnage;
    private final int sabliers;

    /** Constructeur **/
    private CarteAlibi(Personnage personnage, int sabliers){
        this.personnage = personnage;
        this.sabliers = sabliers;
    }

    /** Construit et renvoie l'objet CarteAlibi **/
    public static CarteAlibi creerCarteAlibi(Personnage personnage, int sabliers){
        return new CarteAlibi(personnage, sabliers);
    }

    /** Renvoie le personnage **/
    public Personnage getPersonnage() {
        return personnage;
    }

    /** Renvoie le nombre de sabliers **/
    public int getSabliers() {
        return sabliers;
    }
}
