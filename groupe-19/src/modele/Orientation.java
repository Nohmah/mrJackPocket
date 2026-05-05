package src.modele;
public enum Orientation {
    AUCUN(-1), NORD(0), EST(1), SUD(2), OUEST(3);

    public final int index;
    public static final Orientation[] CARDINAUX = {NORD, EST, SUD, OUEST};

    Orientation(int index){
        this.index = index;
    }

    public Orientation rotation(int quarts){
        if(this == AUCUN ) return AUCUN;
        return CARDINAUX[(index + quarts + 8) % 4];
    }
}
