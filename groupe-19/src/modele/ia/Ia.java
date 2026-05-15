package src.modele.ia;

import src.modele.*;
import java.util.Random;

public class Ia {
    Random r;
    public int difficulte; //0 pour facile, 1 pour moyen, 2 pour difficile

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
        //System.out.println("Choix de l'action de l'ia avec difficulte : " + difficulte);
        switch (difficulte){
            case 0:
                return ChoixIa.choisirActionFacile(partie, estJack);
            case 1:
                return ChoixIa.choisirActionMoyen(partie, estJack);
            case 2:
                return ChoixIa.choisirActionDifficile(partie, estJack);
            case 4:
                if (!estJack) {
                    return ChoixIa.choisirActionFacile(partie, estJack);
                } else {
                    return ChoixIa.choisirActionMoyen(partie, estJack);
                }
            case 5:
                if (!estJack) {
                    return ChoixIa.choisirActionMoyen(partie, estJack);
                } else {
                    return ChoixIa.choisirActionFacile(partie, estJack);
                }
            default:
                return ChoixIa.choisirActionRandom(partie, estJack);
        }
    }
}