package src.modele;
import java.util.*;

public class Ia {

    public class resultatMinMax { // classe pour stocker le résultat du MinMax
        public double score;
        public CoupIa coup;

        public resultatMinMax(double score, CoupIa coup) {
            this.score = score;
            this.coup = coup;
        }
    }
    
    Random r;
    int difficulte; // 0 pour random, 1 pour facile, 2 pour moyen, 3 pour difficile

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
                return choisirActionRandom(partie, estJack);
            case 1:
                return choisirActionFacile(partie, estJack);
            case 2:
                return choisirActionMoyen(partie, estJack);
            case 3:
                return choisirActionDifficile(partie, estJack);
            case 4:
                // Mode 4 : L'Enquêteur joue en Facile, Jack joue en Moyen
                if (!estJack) {
                    return choisirActionFacile(partie, estJack);
                } else {
                    return choisirActionMoyen(partie, estJack);
                }
            case 5:
                // Mode 5 : L'Enquêteur joue en Moyen, Jack joue en Facile
                if (!estJack) {
                    return choisirActionMoyen(partie, estJack);
                } else {
                    return choisirActionFacile(partie, estJack);
                }
            default:
                return choisirActionRandom(partie, estJack);
        }
    }

    public int GagnantEnqueteur(Partie partie){
        // renvoie a quel point le plateau est gagnant pour le detective selon les parametres choisis
        return 0;
    }

    public int GagnantJack(Partie partie){
        // renvoie a quel point le plateau est gagnant pour Jack selon les parametres choisis
        return 0;
    }

    public CoupIa choisirActionRandom(Partie partie, boolean estJack){
        try {
            // J'ai mis 1 seconde (1000ms) pour que ce soit fluide mais que tu aies le temps de voir !
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
        if(coup.action == Action.ROTATION){//verifie que la rotation est possible
            coup = new CoupIa(actionsChoisis, r.nextInt(9), r.nextInt(4));
            while(partie.district.get(coup.para1/3, coup.para1%3).getAPivote()){
                coup = new CoupIa(actionsChoisis, r.nextInt(9), r.nextInt(4));
            }
            return coup;
        }

        return new CoupIa(actionsChoisis, r.nextInt(coup.para1Max(actionsChoisis)+1), r.nextInt(coup.para2Max(actionsChoisis)+1));
    }

    public CoupIa choisirActionFacile(Partie partie, boolean estJack){
        // TANT QUE TON IA FACILE N'EST PAS FINIE :
        // On la redirige vers Random pour éviter le bug de la case (0,0) qui tourne en boucle !
        return choisirActionRandom(partie, estJack);
    }

    public CoupIa choisirActionMoyen(Partie partie, boolean estJack) {

         try {
            // J'ai mis 1 seconde (1000ms) pour que ce soit fluide mais que tu aies le temps de voir !
            Thread.sleep(0);
        } catch (InterruptedException e) {
            System.err.println("Erreur lors de la pause dans IA Moyen : ");
        }

        List<Action> listeActionsPossibles = partie.actions.getActionsPossibles();
        CoupIa meilleurCoup = null;

        // Le score est initialisé très bas pour forcer l'IA à toujours trouver un "meilleur" coup
        double scoreMax = -8000;

        for (Action action : listeActionsPossibles) {
            CoupIa coup = new CoupIa(action);

            // cherche tous les paramètres possibles pour le jeton
            for (int param1 = 0; param1 <= coup.para1Max(action); param1++) {

                // --- LE FILTRE MAGIQUE POUR LA ROTATION ---
                if (action == Action.ROTATION) {
                    // D'après ton code : param1/3 = x (ligne) et param1%3 = y (colonne)
                    int ligne = param1 / 3;
                    int colonne = param1 % 3;

                    // On vérifie si la tuile a déjà pivoté ce tour-ci
                    if (partie.district.get(ligne, colonne).getAPivote()) {
                        continue; // C'est illégal, l'IA passe immédiatement au coup suivant !
                    }
                }
                // ------------------------------------------

                for (int param2 = 0; param2 <= coup.para2Max(action); param2++) {

                    if (action == Action.JOKER && param2 == 0 && !estJack) {
                        continue; // illégal !
                    }

                    CoupIa tentative = new CoupIa(action, param1, param2);

                    // On simule et on note le coup
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
            return choisirActionRandom(partie, estJack);
        }
    }

    public CoupIa choisirActionDifficile(Partie partie, boolean estJack){
        
        int profondeur = 4 - partie.totalActionsJouees;// jusqu'à la fin du tour actuel
        return IaMinMax(partie, estJack, profondeur).coup;
    }

    public resultatMinMax IaMinMax(Partie partie, boolean estJack, int profondeur){
        //System.out.println("Entre dans MinMax Profondeur : " + profondeur);
        //si mode max alors on cherche à maximiser le score, sinon on cherche à minimiser le score
        boolean modeMax = estJack ? partie.joueurCourant == Joueur.JACK : partie.joueurCourant == Joueur.ENQUETEUR;
        double scoreMax = modeMax ? -8000 : 8000;
        CoupIa meilleurCoup = null;
        if(profondeur == 1){ 
            for(Action action : partie.actions.getActionsPossibles()){
                CoupIa coup = new CoupIa(action);
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
                        double note = partie.simulerEtNoter(tentative, estJack);
                        if(modeMax && note > scoreMax){
                            meilleurCoup = tentative;
                            scoreMax = note;
                        } else if (!modeMax && note < scoreMax){
                            meilleurCoup = tentative;
                            scoreMax = note;
                        }
                    }
                }
            }
        }
        else{
            for(Action action : partie.actions.getActionsPossibles()){
                CoupIa coup = new CoupIa(action);
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
                        Partie partieSimulee = new Partie(partie);
                        partieSimulee.jouerCoup(tentative);
                        double note = IaMinMax(partieSimulee, estJack, profondeur - 1).score;
                        if(modeMax && note > scoreMax){
                            meilleurCoup = tentative;
                            scoreMax = note;
                        } else if (!modeMax && note < scoreMax){
                            meilleurCoup = tentative;
                            scoreMax = note;
                        }
                    }
                }
            }
        }

        if(meilleurCoup != null){
            return new resultatMinMax(scoreMax, meilleurCoup);
        } else {
            System.out.println("/!\\ Aucun coup trouvé en MinMax, renvoie random /!\\");
            return new resultatMinMax(scoreMax, choisirActionRandom(partie, estJack));
        }
    }
}