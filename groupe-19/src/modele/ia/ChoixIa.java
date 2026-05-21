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
        int profondeur = 4 - partie.totalActionsJouees;// jusqu'à la fin du tour actuel
        if(partie.numeroTour % 2 == 1){ // si on est a un tour impair on peut aller plus loin car pas de hasard sur les jetons
            if(profondeur <= 2) profondeur ++;
            JetonAction jeton = partie.jetonsAction.get(3);
            if (jeton.isFaceRectoVisible() && !jeton.isJoue() && !partie.jetonsAction.get(2).isJoue()){
                profondeur -- ;//le cas ou le 3 ème jeton (rotation/echange est dispo en meme temps que le 4 ème qui est sur rotation)
            }
            if(estJack)  profondeur += 3 ;
            else profondeur += 2 ; //on va plus loin en jouant jack car c'est plus long les calcul chez Enqueteur et que jack c'est plus dure de gagner
        }
        return IaMinMax.choisirActionMinMax(partie, estJack, profondeur);
    }
}