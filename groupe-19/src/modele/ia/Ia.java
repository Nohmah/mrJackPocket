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
        CoupIa coup;
        int tempsMin = 5000; //temps minimum de l'ia avant quelle joue son coup (en ms)
        long debut = System.currentTimeMillis();
        switch (difficulte){
            case 0:
                coup = ChoixIa.choisirActionFacile(partie, estJack);
                break;
            case 1:
                coup = ChoixIa.choisirActionMoyen(partie, estJack);
                break;
            case 2:
                coup = ChoixIa.choisirActionDifficile(partie, estJack);
                break;
            default:
                coup = ChoixIa.choisirActionRandom(partie, estJack);
                break;
        }

        long duree = System.currentTimeMillis() - debut;
        //System.out.println("duree de l'ia = " + duree + "ms");
        if(duree < tempsMin){
            try {
                Thread.sleep(tempsMin - duree);
            } catch (InterruptedException e) {
                System.err.println("Erreur lors de la pause dans choisirAction (ia.java) : ");
            }
        }

        return coup;
    }
}