package src.modele.ia;

import src.modele.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

//import javax.swing.Action; // interdit 
public class IaMinMax{
    
    public static class resultatMinMax { // classe pour stocker le résultat du MinMax
        public double score;
        public CoupIa coup;

        public resultatMinMax(double score, CoupIa coup) {
            this.score = score;
            this.coup = coup;
        }
    }

    public static CoupIa choisirActionMinMax(Partie partie, boolean estJack, int profondeur){
        //System.out.println("entre dans IA choisirActionMINMAX");
        return MinMax(partie, estJack, profondeur).coup;
    }

    public static resultatMinMax MinMax(Partie partie, boolean estJack, int profondeur){
        //System.out.println("Entre dans MinMax Profondeur : " + profondeur + " avec " + partie.actions.getActionsPossibles().size() + " actions possibles");
        //si mode max alors on cherche à maximiser le score, sinon on cherche à minimiser le score
        boolean modeMax = estJack ? partie.joueurCourant == Joueur.JACK : partie.joueurCourant == Joueur.ENQUETEUR;
        double scoreMax = modeMax ? -8000 : 8000;
        CoupIa meilleurCoup = null;

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

        if(partie.actions.getActionsPossibles().size() == 0){// arrive seulement avec un autre appel de IaMinMax donc on peut modif partie
            System.out.println("Aucun coup possible dans MinMax, donc on retourn les jetons");
            if(partie.numeroTour % 2 == 0){ 
                System.out.println("Pas encore fait de calc tour suivant quand on doit lancer les jetons");
                return new resultatMinMax(scoreMax, null);
            }
            if(estJack){//si on est jack on peut juste continuer normalement
                partie.appelATemoin();
                System.out.println("Mise a jour des jetons : nb actions possibles :" + partie.actions.getActionsPossibles().size());
                return MinMax(partie, estJack, profondeur);
            }
            else{// si on est enquêteur, c'est plus compliqué car on connait pas le Jack

                // On simule 2 cas avec Jack visible et Jack invisible
                System.out.println("Création de 2 dimentions (bruit futuriste trop cool)");
                Partie partie2 = new Partie(partie);
                int nbvisible = simuleAppelATemoin(partie, true);
                int nbinvisible = simuleAppelATemoin(partie2, false);

                for(Action action : partie.actions.getActionsPossibles()){
                CoupIa coup = new CoupIa(action);
                //System.out.println("Action testée en MinMax : " + action);
                    for (int param1 = 0; param1 <= coup.para1Max(action); param1++) {
                        for (int param2 = 0; param2 <= coup.para2Max(action); param2++) {
                            
                            // --- Filtre anti coup illégal ---
                            if(action == Action.JOKER && param2 == 0 && !estJack){
                                continue; // illégal !
                            }

                            if(action == Action.ROTATION){
                                int ligne = param1 / 3;
                                int colonne = param1 % 3;
                                if (partie.district.get(ligne, colonne).getAPivote()) {
                                    continue; // C'est illégal
                                }
                            }

                            CoupIa tentative = new CoupIa(action, param1, param2);
                            
                            // a modif et tester
                            double note = (nbvisible / (nbvisible + nbinvisible)) * partie.simulerEtNoter(tentative, estJack);
                            note += (nbinvisible / (nbvisible + nbinvisible)) * partie2.simulerEtNoter(tentative, estJack);
                            if(modeMax && note >= scoreMax){
                                meilleurCoup = tentative;
                                scoreMax = note;
                            } else if (!modeMax && note <= scoreMax){
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
                    System.out.println("/!\\ Aucun coup trouvé en MinMax, renvoie random /!\\");
                    return new resultatMinMax(scoreMax, null);
                }
            }
        }


        //boucle classique de MinMax
        
        for(Action action : partie.actions.getActionsPossibles()){
            CoupIa coup = new CoupIa(action);
            //System.out.println("Action testée en MinMax : " + action);
            for (int param1 = 0; param1 <= coup.para1Max(action); param1++) {
                for (int param2 = 0; param2 <= coup.para2Max(action); param2++) {

                    // --- Filtre anti coup illégal ---
                    if(action == Action.JOKER && param2 == 0 && !estJack){
                        continue; // illégal !
                    }

                    if(action == Action.ROTATION){
                        int ligne = param1 / 3;
                        int colonne = param1 % 3;
                        if (partie.district.get(ligne, colonne).getAPivote()) {
                            continue; // C'est illégal
                        }
                    }

                    CoupIa tentative = new CoupIa(action, param1, param2);
                    //System.out.println("Joue coup : ");
                    //tentative.afficher();
                    double note;

                    if(profondeur == 1){
                        note = partie.simulerEtNoter(tentative, estJack);
                    }
                    else{
                        Partie partieSimulee = new Partie(partie);
                        partieSimulee.jouerCoup(tentative);
                        note = MinMax(partieSimulee, estJack, profondeur - 1).score;
                    }

                    if(modeMax && note >= scoreMax){
                        meilleurCoup = tentative;
                        scoreMax = note;
                    } else if (!modeMax && note <= scoreMax){
                        meilleurCoup = tentative;
                        scoreMax = note;
                    }
                }
            }
        }

        if(meilleurCoup != null){
            return new resultatMinMax(scoreMax, meilleurCoup);
        } else {
            System.out.println("/!\\ Aucun coup trouvé en MinMax, renvoie random /!\\  Avec profondeur : " + profondeur + " et scoreMax : " + scoreMax + " et nb actions possibles : " + partie.actions.getActionsPossibles().size() + " et tour actuell : " + partie.numeroTour);
            return new resultatMinMax(scoreMax, null);
        }
    }

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
        partie.verifFinDePartie();
        partie.tourSuivant();
        return partie.suspects.size();
    }

}