package src.modele.ia;

import src.modele.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class IaMinMax{
    
    public static class resultatMinMax { // classe pour stocker le résultat du MinMax
        public double score;
        public CoupIa coup;

        public resultatMinMax(double score, CoupIa coup) {
            this.score = score;
            this.coup = coup;
        }
    }

    // ---- plus simple pour appeler MinMax
    public static CoupIa choisirActionMinMax(Partie partie, boolean estJack, int profondeur){
        //System.out.println("entre dans IA choisirActionMINMAX");

        if(profondeur < 6){ // petite attente pour que les ia trop rapides ne se fassent pas instantanement
            try {
                //Thread.sleep((6 - profondeur) * 500 );
                Thread.sleep(0);
            } catch (InterruptedException e) {
                System.err.println("Erreur lors de la pause dans choisirActionMinMax : ");
            }
        }   

        CoupIa coup = MinMax(partie, estJack, profondeur, 0, -8000, 8000).coup ;

        if(coup != null){
            return coup;
        } else {
            System.out.println("/!\\ Aucun coup trouvé en MinMax, renvoie random /!\\  Avec profondeur : " + profondeur);
            return ChoixIa.choisirActionRandom(partie, estJack);
        }
    }

    public static resultatMinMax MinMax(Partie partie, boolean estJack, int profondeur, int skip, double alpha, double beta){
        //if(profondeur != 1) System.out.println("Entre dans MinMax Profondeur : " + profondeur + " avec " + partie.actions.getActionsPossibles().size() + " actions possibles");
        //si mode max alors on cherche à maximiser le score, sinon on cherche à minimiser le score
        boolean modeMax = estJack ? partie.joueurCourant == Joueur.JACK : partie.joueurCourant == Joueur.ENQUETEUR;
        double scoreMax = modeMax ? -8000 : 8000;
        CoupIa meilleurCoup = null;

        // Verification de si la partie n'est pas déjà terminé 
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

        //Cas special de fin de tour 
        if(partie.actions.getActionsPossibles().size() == 0){// arrive seulement avec un autre appel de IaMinMax donc on peut modif partie
            //System.out.println("Aucun coup possible dans MinMax, donc on retourn les jetons");
            if(partie.numeroTour % 2 == 0){ 
                System.out.println("Pas encore fait de calc tour suivant quand on doit lancer les jetons");
                return new resultatMinMax(scoreMax, null);
            }
            if(estJack){//si on est jack on peut juste continuer normalement
                partie.appelATemoin();
                //System.out.println("Mise a jour des jetons : nb actions possibles :" + partie.actions.getActionsPossibles().size());
                return MinMax(partie, estJack, profondeur, 0, alpha, beta);
            }
            else{// si on est enquêteur, c'est plus compliqué car on connait pas le Jack
                return MinMaxDivision(partie, estJack, profondeur, alpha, beta);
            }
        }



        //boucle classique de MinMax et principal 

        int nbskip = 0; // la gestion des skips c'est pour eviter d'aller dans des branches deja calcule (jsp comment expliquer dsl)
        for(Action action : partie.actions.getActionsPossibles()){
            CoupIa coup = new CoupIa(action);
            // la partie skip est opti quand la meme personne joue 2 fois car faire action a puis b = faire b puis a
            if(skip > 0){
                skip --;
                continue;
            }
            
            if(nbskip == 2) continue ;

            //System.out.println("Action testée en MinMax : " + action);
            for (int param1 = 0; param1 <= coup.para1Max(action); param1++) {

                if(action == Action.ROTATION){
                    if (partie.district.get(param1 / 3, param1 % 3).getAPivote()) {
                        continue; // C'est illégal
                    }
                }

                for (int param2 = 0; param2 <= coup.para2Max(action); param2++) {

                    // --- Filtre anti coup illégal ---
                    if(action == Action.JOKER && param2 == 0 && !estJack){
                        continue; // illégal !
                    }

                    if(action == Action.ROTATION && param1 == param2){
                        continue ; //illégal !
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
                            //System.out.println("opti grace a beta, profondeur = " + profondeur + " beta =" + beta + " score = " + scoreMax);
                            return new resultatMinMax(scoreMax, meilleurCoup);
                        }
                    } else if (!modeMax && note <= scoreMax){
                        meilleurCoup = tentative;
                        scoreMax = note;
                        if(scoreMax < alpha){
                            //System.out.println("opti grace a alpha, profondeur = " + profondeur + " alpha =" + alpha + " score = " + scoreMax);
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
            System.out.println("/!\\ Aucun coup trouvé en MinMax, renvoie random /!\\  Avec profondeur : " + profondeur + " et scoreMax : " + scoreMax + " et nb actions possibles : " + partie.actions.getActionsPossibles().size() + " et tour actuell : " + partie.numeroTour);
            return new resultatMinMax(scoreMax, null);
        }
    }


    // UN minMax different pour un cas particulier 
    public static resultatMinMax MinMaxDivision(Partie partie, boolean estJack, int profondeur, double alpha, double beta){
        //boolean modeMax = estJack ? partie.joueurCourant == Joueur.JACK : partie.joueurCourant == Joueur.ENQUETEUR;
        //double scoreMax = modeMax ? -8000 : 8000;
        //boolean modeMax = false ; //si on entre dans cette fonction on est au debut d un tour pair (donc a jack de jouer) et on joue detective
        double scoreMax = 8000;
        CoupIa meilleurCoup = null;

        // On simule 2 cas avec Jack visible et Jack invisible
        //System.out.println("Création de 2 dimentions (bruit futuriste trop cool)");
        // Utiliser deux copies pour simuler les deux issues (Jack visible / invisible)
        Partie partieVis = new Partie(partie);
        Partie partieInv = new Partie(partie);

        int nbvisible = simuleAppelATemoin(partieVis, true);
        int nbinvisible = simuleAppelATemoin(partieInv, false);


        // Itérer sur les actions possibles de l'état "visible" (on pourrait aussi unionner les sets)
        List<Action> actionsVis = partieVis.actions.getActionsPossibles();
        List<Action> actionsInv = partieInv.actions.getActionsPossibles();
        
        double poidsVis;
        double poidsInv;

        //Verif du cas particulier ou une des partie est terminé 
        boolean visTerminee = partieVis.isPartieTerminee();
        boolean invTerminee = partieInv.isPartieTerminee();
        if(visTerminee && !invTerminee){
            poidsVis = 0.0;
            if(partieVis.gagnant == Joueur.JACK){
                poidsInv = 0.1;
            }
            else{
                poidsInv = 5.0;//grande priorité a cette partie car 1 chance sur 2 de gagner
            }
        } else if(!visTerminee && invTerminee){
            poidsInv = 0.0;
            if(partieInv.gagnant == Joueur.JACK){
                poidsVis = 0.1;
            }
            else{
                poidsVis = 5.0;//grande priorité a cette partie car 1 chance sur 2 de gagner
            }
        } else if(visTerminee && invTerminee){
            if(partieInv.gagnant == Joueur.JACK && partieVis.gagnant == Joueur.JACK){
                return new resultatMinMax(- 8000, null);
            }
            if(partieInv.gagnant == Joueur.ENQUETEUR && partieVis.gagnant == Joueur.ENQUETEUR){
                return new resultatMinMax(8000, null);
            }
            return new resultatMinMax(0, null); //si une fait gagner enqueteur et l'autre le Jack 
        } else {
            int total = nbvisible + nbinvisible;
            poidsVis = total == 0 ? 0.5 : (double) nbvisible / (double) total;
            poidsInv = total == 0 ? 0.5 : (double) nbinvisible / (double) total;
        }


        // Debut de la boucle du MinMax avec le cas particulier du changement de tour + on est enqueteur

        List<Action> actionsToTest = actionsVis.isEmpty() ? actionsInv : actionsVis; //si Vis est vide donc deja finit on doit test seulemtn Inv
        for(Action action : actionsToTest){
            CoupIa coup = new CoupIa(action);
            for (int param1 = 0; param1 <= coup.para1Max(action); param1++) {
                if(action == Action.ROTATION){
                    int ligne = param1 / 3;
                    int colonne = param1 % 3;
                    if (partieVis.district.get(ligne, colonne).getAPivote()) {
                        continue; // C'est illégal
                    }
                }
                for (int param2 = 0; param2 <= coup.para2Max(action); param2++) {
                    
                    // --- Filtre anti coup illégal ---
                    if(action == Action.JOKER && param2 == 0 && !estJack){
                        continue; // illégal !
                    }

                    if(action == Action.ROTATION && param1 == param2){
                        continue ; //illégal !
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
        // on ne passe pas dans la boucle principale
        if(meilleurCoup != null){
            return new resultatMinMax(scoreMax, meilleurCoup);
        } else {
            System.out.println("/!\\ Aucun coup trouvé en MinMax (dans le special), renvoie random /!\\");
            return new resultatMinMax(scoreMax, null);
        }
    }


    // ----- Pour simuler un appel a temoin quand on est detetctive car on ne doit pas connaitre la pos de Jack

    public static int simuleAppelATemoin(Partie partie, boolean visible){
        // simule un appel à témoin en enlvant les visible si visible = true
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

        avantAppel.removeAll(partie.suspects); // Obtention des gens plus suspects
        for(Personnage p : avantAppel){
            partie.district.innocenter(p);
        }

        try{
            partie.verifFinDePartie();
            partie.tourSuivant();
        } catch (Exception e) {
            System.err.println("Exception dans SimuleAppelATemoin : " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
        return partie.suspects.size();
    }

}