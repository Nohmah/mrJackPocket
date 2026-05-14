package src.modele.ia;

import src.modele.*;
import java.util.Random;

public class Ia {
    Random r;
    public int difficulte; // 0 pour random, 1 pour facile, 2 pour moyen, 3 pour difficile

    public Ia(){
        r = new Random();
        difficulte = -1;
    }

    public Ia(int difficulte){
        r = new Random();
        this.difficulte = difficulte;
    }

    public void setDifficulte(int difficulte){
        this.difficulte = difficulte;
    }

    public CoupIa choisirAction(Partie partie, boolean estJack){
        switch (difficulte){
            case 0:
                return ChoixIa.choisirActionRandom(partie, estJack, r);
            case 1:
                return ChoixIa.choisirActionFacile(partie, estJack, r);
            case 2:
                return ChoixIa.choisirActionMoyen(partie, estJack, r);
            case 3:
                return ChoixIa.choisirActionDifficile(partie, estJack, r);
            case 4:
                if (!estJack) {
                    return ChoixIa.choisirActionFacile(partie, estJack, r);
                } else {
                    return ChoixIa.choisirActionMoyen(partie, estJack, r);
                }
            case 5:
                if (!estJack) {
                    return ChoixIa.choisirActionMoyen(partie, estJack, r);
                } else {
                    return ChoixIa.choisirActionFacile(partie, estJack, r);
                }
            default:
                return ChoixIa.choisirActionRandom(partie, estJack, r);
        }
    }

    public int GagnantEnqueteur(Partie partie){
        return 0;
    }

    public int GagnantJack(Partie partie){
        return 0;
    }
}