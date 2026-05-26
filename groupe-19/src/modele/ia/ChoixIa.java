package src.modele.ia;

import src.modele.*;

import java.util.List;
import java.util.Random;

public class ChoixIa {


    public static CoupIa choisirActionRandom(Partie partie, boolean estJack){

        System.out.println("entre dans IA Random");

        Random r = new Random();

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

    public static CoupIa choisirActionFacile(Partie partie, boolean estJack){
        System.out.println("entre dans IA Facile");
        return choisirActionRandom(partie, estJack);
    }

    public static CoupIa choisirActionMoyen(Partie partie, boolean estJack) {
        System.out.println("entre dans IA Moyen");
        return IaMinMax.choisirActionMinMax(partie, estJack, 1);
    }

    public static CoupIa choisirActionDifficile(Partie partie, boolean estJack){
        System.out.println("entre dans IA Difficile");
        int profondeur = 8 - partie.totalActionsJouees ; //fait max 2 tour
        if(partie.numeroTour % 2 == 0){
            if(estJack && partie.totalActionsJouees == 0)profondeur -= 3;
            else profondeur -= 2;
        }
        if(!estJack) profondeur --;
        //verif si les 2 jeton long a calculer sont encore là
        JetonAction jeton3 = partie.jetonsAction.get(3);
        JetonAction jeton2 = partie.jetonsAction.get(2);
        if(!jeton2.isJoue()){
            if (jeton2.isFaceRectoVisible()) profondeur --;
            else profondeur -= 2;
        }
        if (jeton3.isFaceRectoVisible() && !jeton3.isJoue()) profondeur -- ;
        if(partie.numeroTour % 2 == 1){
            if(jeton2.isFaceRectoVisible() || !jeton3.isFaceRectoVisible()) profondeur --;
        }

        profondeur = Math.max(profondeur, 3);// minmum profondeur 3

        //System.out.println("on entre dans MinMax avec profondeur " + profondeur);
        return IaMinMax.choisirActionMinMax(partie, estJack, profondeur);
    }
}