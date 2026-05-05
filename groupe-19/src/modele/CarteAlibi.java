package src.modele;

public class CarteAlibi {
    private final Personnage personnage;
    private final int sabliers;

    private CarteAlibi(Personnage personnage, int sabliers){
        this.personnage = personnage;
        this.sabliers = sabliers;
    }

    public static CarteAlibi creerCarteAlibi(Personnage personnage, int sabliers){
        return new CarteAlibi(personnage, sabliers);
    }

    public Personnage getPersonnage() {
        return personnage;
    }

    public int getSabliers() {
        return sabliers;
    }
}
