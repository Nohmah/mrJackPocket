package src.modele.ia;

import src.modele.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class IaMinMax {

    public static class resultatMinMax {
        public double score;
        public CoupIa coup;

        public resultatMinMax(double score, CoupIa coup) {
            this.score = score;
            this.coup = coup;
        }
    }

    public static CoupIa choisirActionMinMax(Partie partie, boolean estJack, int profondeur){
        CoupIa coup = MinMax(partie, estJack, profondeur, 0, -8000, 8000).coup ;
        if(coup != null){
            return coup;
        } else {
            return ChoixIa.choisirActionRandom(partie, estJack);
        }
    }

    public static resultatMinMax MinMax(Partie partie, boolean estJack, int profondeur, int skip, double alpha, double beta){
        boolean modeMax = estJack ? partie.joueurCourant == Joueur.JACK : partie.joueurCourant == Joueur.ENQUETEUR;
        double scoreMax = modeMax ? -8000 : 8000;
        CoupIa meilleurCoup = null;

        if(partie.changement) return new resultatMinMax(scoreMax, null);

        if(partie.isPartieTerminee()){
            if(estJack){
                if(partie.gagnant == Joueur.JACK){
                    return new resultatMinMax(8000, null);
                } else {
                    return new resultatMinMax(-8000, null);
                }
            }
            else{
                if(partie.gagnant == Joueur.ENQUETEUR){
                    return new resultatMinMax(8000, null);
                } else {
                    return new resultatMinMax(-8000, null);
                }
            }
        }

        if(partie.actions.getActionsPossibles().size() == 0){
            if(partie.numeroTour % 2 == 0){
                return new resultatMinMax(scoreMax, null);
            }
            if(estJack){
                partie.appelATemoin();
                if (!partie.isPartieTerminee()) {
                    partie.tourSuivant();
                }
                return MinMax(partie, estJack, profondeur, 0, alpha, beta);
            }
            else{
                return MinMaxDivision(partie, estJack, profondeur, alpha, beta);
            }
        }

        int nbskip = 0;
        for(Action action : partie.actions.getActionsPossibles()){
            CoupIa coup = new CoupIa(action);
            if(skip > 0){
                skip --;
                continue;
            }

            if(nbskip == 2) continue ;

            for (int param1 = 0; param1 <= coup.para1Max(action); param1++) {
                if(action == Action.ROTATION){
                    if (partie.district.get(param1 / 3, param1 % 3).getAPivote()) {
                        continue;
                    }
                }

                for (int param2 = 0; param2 <= coup.para2Max(action); param2++) {
                    if(action == Action.JOKER && param2 == 0 && !estJack){
                        continue;
                    }

                    if(action == Action.ROTATION && param1 == param2){
                        continue ;
                    }

                    CoupIa tentative = new CoupIa(action, param1, param2);
                    double note;

                    if(profondeur == 1){
                        note = partie.simulerEtNoter(tentative, estJack);
                    }
                    else{
                        Partie partieSimulee = new Partie(partie);
                        partieSimulee.jouerCoup(tentative);
                        if(modeMax) note = MinMax(partieSimulee, estJack, profondeur - 1, nbskip, scoreMax, 8000).score;
                        else note = MinMax(partieSimulee, estJack, profondeur - 1, nbskip, -8000, scoreMax).score;
                    }

                    if(modeMax && note >= scoreMax){
                        meilleurCoup = tentative;
                        scoreMax = note;
                        if(scoreMax > beta){
                            return new resultatMinMax(scoreMax, meilleurCoup);
                        }
                    } else if (!modeMax && note <= scoreMax){
                        meilleurCoup = tentative;
                        scoreMax = note;
                        if(scoreMax < alpha){
                            return new resultatMinMax(scoreMax, meilleurCoup);
                        }
                    }
                }
            }
            if(partie.totalActionsJouees == 1) nbskip ++;
        }

        if(meilleurCoup != null){
            return new resultatMinMax(scoreMax, meilleurCoup);
        } else {
            return new resultatMinMax(scoreMax, null);
        }
    }

    public static resultatMinMax MinMaxDivision(Partie partie, boolean estJack, int profondeur, double alpha, double beta){
        double scoreMax = 8000;
        CoupIa meilleurCoup = null;

        Partie partieVis = new Partie(partie);
        Partie partieInv = new Partie(partie);

        int nbvisible = simuleAppelATemoin(partieVis, true);
        int nbinvisible = simuleAppelATemoin(partieInv, false);

        List<Action> actionsVis = partieVis.actions.getActionsPossibles();
        List<Action> actionsInv = partieInv.actions.getActionsPossibles();

        double poidsVis;
        double poidsInv;

        boolean visTerminee = partieVis.isPartieTerminee();
        boolean invTerminee = partieInv.isPartieTerminee();
        if(visTerminee && !invTerminee){
            poidsVis = 0.0;
            if(partieVis.gagnant == Joueur.JACK){
                poidsInv = 0.1;
            }
            else{
                poidsInv = 5.0;
            }
        } else if(!visTerminee && invTerminee){
            poidsInv = 0.0;
            if(partieInv.gagnant == Joueur.JACK){
                poidsVis = 0.1;
            }
            else{
                poidsVis = 5.0;
            }
        } else if(visTerminee && invTerminee){
            if(partieInv.gagnant == Joueur.JACK && partieVis.gagnant == Joueur.JACK){
                return new resultatMinMax(- 8000, null);
            }
            if(partieInv.gagnant == Joueur.ENQUETEUR && partieVis.gagnant == Joueur.ENQUETEUR){
                return new resultatMinMax(8000, null);
            }
            return new resultatMinMax(0, null);
        } else {
            int total = nbvisible + nbinvisible;
            poidsVis = total == 0 ? 0.5 : (double) nbvisible / (double) total;
            poidsInv = total == 0 ? 0.5 : (double) nbinvisible / (double) total;
        }

        List<Action> actionsToTest = actionsVis.isEmpty() ? actionsInv : actionsVis;
        for(Action action : actionsToTest){
            CoupIa coup = new CoupIa(action);
            for (int param1 = 0; param1 <= coup.para1Max(action); param1++) {
                if(action == Action.ROTATION){
                    int ligne = param1 / 3;
                    int colonne = param1 % 3;
                    if (partieVis.district.get(ligne, colonne).getAPivote()) {
                        continue;
                    }
                }
                for (int param2 = 0; param2 <= coup.para2Max(action); param2++) {
                    if(action == Action.JOKER && param2 == 0 && !estJack){
                        continue;
                    }

                    if(action == Action.ROTATION && param1 == param2){
                        continue ;
                    }

                    CoupIa tentative = new CoupIa(action, param1, param2);
                    double note = 0;
                    if(profondeur == 1){
                        if(!visTerminee) note += poidsVis * partieVis.simulerEtNoter(tentative, estJack);
                        if(!invTerminee) note += poidsInv * partieInv.simulerEtNoter(tentative, estJack);
                    }
                    else{
                        if(!visTerminee){
                            Partie partieSimulee1 = new Partie(partieVis);
                            partieSimulee1.jouerCoup(tentative);
                            note += poidsVis * MinMax(partieSimulee1, estJack, profondeur - 1, 0, -8000, scoreMax * poidsVis).score;
                        }

                        if(!invTerminee){
                            Partie partieSimulee2 = new Partie(partieInv);
                            partieSimulee2.jouerCoup(tentative);
                            note += poidsInv * MinMax(partieSimulee2, estJack, profondeur - 1, 0, -8000, scoreMax * poidsInv).score;
                        }
                    }

                    if (note <= scoreMax){
                        meilleurCoup = tentative;
                        scoreMax = note;
                    }
                }
            }
        }
        if(meilleurCoup != null){
            return new resultatMinMax(scoreMax, meilleurCoup);
        } else {
            return new resultatMinMax(scoreMax, null);
        }
    }

    public static int simuleAppelATemoin(Partie partie, boolean visible){
        HashSet<Personnage> personnagesVisibles = new HashSet<>();
        for (Detective d : partie.detectives) {
            personnagesVisibles.addAll(partie.district.personnagesVisiblesParDetective(d));
        }

        List<Personnage> avantAppel = new ArrayList<>(partie.suspects);

        if(visible){
            partie.suspects.retainAll(personnagesVisibles);
            partie.jackVisibleCeTour = true;
        } else {
            partie.jackVisibleCeTour = false;
            partie.sabliersDeJack++;
            partie.suspects.removeAll(personnagesVisibles);
        }

        avantAppel.removeAll(partie.suspects);
        for(Personnage p : avantAppel){
            partie.district.innocenter(p);
        }

        try{
            partie.verifFinDePartie();
            partie.tourSuivant();
        } catch (Exception e) {
            return 0;
        }
        return partie.suspects.size();
    }
}
