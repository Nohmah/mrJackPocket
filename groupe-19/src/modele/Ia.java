package src.modele;
import java.util.*;

public class Ia {

    Random r;
    int difficulte; // 0 pour random, 1 pour facile, 2 pour moyen, 3 pour difficile

    public Ia(){
        r = new Random();
        difficulte = 0;
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
                return choisirActionRandom(partie, estJack);
            case 1:
                return choisirActionFacile(partie, estJack);
            case 2:
                return choisirActionMoyen(partie, estJack);
            case 3:
                return choisirActionDifficile(partie, estJack);
            default:
                return choisirActionRandom(partie, estJack);
        }
    }

    public int GagnantEnqueteur(Partie partie){// renvoie a quelle point le plateau est gagnant pour le detective selon les parametre choisis
        
        // renvoie nb de suspects  - la difference entre nb de visible et de non visible
        return 0;
    }

    public int GagnantJack(Partie partie){// renvoie a quelle point le plateau est gagnant pour le Jack selon les parametre choisis
        
        // renvoie le nb de suspects qui sont dans le même groupe que lui +1 si il est dans les non visible
        return 0;
    }

    public CoupIa choisirActionRandom(Partie partie, boolean estJack){

        try {
            Thread.sleep(3000); // pause de 3 seconde
        } catch (InterruptedException e) {
            System.err.println("Erreur lors de la pause dans IA Random : ");
        }
        List<Action> actionsPossibles = partie.actions.getActionsPossibles();
        Action actionsChoisis = actionsPossibles.get(r.nextInt(actionsPossibles.size()));
        CoupIa coup = new CoupIa(actionsChoisis);
        if(!estJack && (coup.action == Action.JOKER)){
            return new CoupIa(actionsChoisis, r.nextInt(3), 1);
        }
        return new CoupIa(actionsChoisis, r.nextInt(coup.para1Max(actionsChoisis)+1), r.nextInt(coup.para2Max(actionsChoisis)+1));

    }

    public CoupIa choisirActionFacile(Partie partie, boolean estJack){
        // à définir
        //return choisirActionRandom(partie, estJack);
        //vraiment pas encore fait 

        List<Action> actionsPossibles = partie.actions.getActionsPossibles();
        Action actionChoisie = null;
        int maxGagant = -8000;
        for(Action action : actionsPossibles){
            if(estJack){
                if(GagnantJack(partie) > maxGagant){
                    maxGagant = GagnantJack(partie);
                    actionChoisie = action;
                }
            } else {
                if(GagnantEnqueteur(partie) > maxGagant){
                    maxGagant = GagnantEnqueteur(partie);
                    actionChoisie = action;
                }
            }
        }
        CoupIa coup = new CoupIa(actionChoisie);
        return coup;
    }

    public CoupIa choisirActionMoyen(Partie partie, boolean estJack){
        //à définir
        return choisirActionRandom(partie, estJack);
    }

    public CoupIa choisirActionDifficile(Partie partie, boolean estJack){
        //à définir
        return choisirActionRandom(partie, estJack);
    }

}
