package src.modele.ia;

import src.modele.*;

import java.util.List;
import java.util.Random;

public class ChoixIa {


    public static CoupIa choisirActionRandom(Partie partie, boolean estJack, Random r){
        System.out.println("entre dans IA Random");
        try {
            Thread.sleep(0);
        } catch (InterruptedException e) {
            System.err.println("Erreur lors de la pause dans IA Random : ");
        }

        List<Action> actionsPossibles = partie.actions.getActionsPossibles();
        Action actionsChoisis = actionsPossibles.get(r.nextInt(actionsPossibles.size()));
        CoupIa coup = new CoupIa(actionsChoisis);

        if(!estJack && (coup.action == Action.JOKER)){
            return new CoupIa(actionsChoisis, r.nextInt(3), 1);
        }

        if(coup.action == Action.ROTATION){
            coup = new CoupIa(actionsChoisis, r.nextInt(9), r.nextInt(4));
            while(partie.district.get(coup.para1/3, coup.para1%3).getAPivote()){
                coup = new CoupIa(actionsChoisis, r.nextInt(9), r.nextInt(4));
            }
            return coup;
        }

        return new CoupIa(actionsChoisis, r.nextInt(coup.para1Max(actionsChoisis)+1), r.nextInt(coup.para2Max(actionsChoisis)+1));
    }

    public static CoupIa choisirActionFacile(Partie partie, boolean estJack, Random r){
        return choisirActionRandom(partie, estJack, r);
    }

    public static CoupIa choisirActionMoyen(Partie partie, boolean estJack, Random r) {
        List<Action> listeActionsPossibles = partie.actions.getActionsPossibles();
        CoupIa meilleurCoup = null;
        double scoreMax = -8000;

        for (Action action : listeActionsPossibles) {
            CoupIa coup = new CoupIa(action);

            for (int param1 = 0; param1 <= coup.para1Max(action); param1++) {
                if (action == Action.ROTATION) {
                    int ligne = param1 / 3;
                    int colonne = param1 % 3;
                    if (partie.district.get(ligne, colonne).getAPivote()) {
                        continue;
                    }
                }

                for (int param2 = 0; param2 <= coup.para2Max(action); param2++) {
                    CoupIa tentative = new CoupIa(action, param1, param2);
                    double note = partie.simulerEtNoter(tentative, estJack);

                    if (note > scoreMax) {
                        scoreMax = note;
                        meilleurCoup = tentative;
                    }
                }
            }
        }

        if (meilleurCoup != null) {
            return meilleurCoup;
        } else {
            return choisirActionRandom(partie, estJack, r);
        }
    }

    public static CoupIa choisirActionDifficile(Partie partie, boolean estJack, Random r){
        System.out.println("entre dans IA Difficile");
        int profondeur = 4 - partie.totalActionsJouees;// jusqu'à la fin du tour actuel
        //if(partie.numeroTour % 2 == 1) profondeur += 2 ;// si on est a un tour impair on peut aller plus loin car pas de hasard sur les jetons
        CoupIa meilleurCoup = IaMinMax.choisirActionMinMax(partie, estJack, profondeur);
        if(meilleurCoup != null){
            return meilleurCoup;
        } else {
            System.out.println("/!\\ Aucun coup trouvé en MinMax, renvoie random /!\\  Avec profondeur : " + profondeur + " et nb actions possibles : " + partie.actions.getActionsPossibles().size() + " et tour actuell : " + partie.numeroTour);
            return choisirActionRandom(partie, estJack, r);
        }
    }
}