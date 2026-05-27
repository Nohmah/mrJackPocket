package src.modele;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;

import src.modele.ia.*;

public class Partie {

    public District district;
    public final PartieActions actions;
    public List<JetonAction> jetonsAction;
    public List<CarteAlibi> cartesAlibiPioche;
    public List<Detective> detectives;
    public List<Personnage> suspects;
    public Joueur joueurCourant;
    public Joueur joueurChoisi;
    public boolean IAChoisi;
    public String difficulteIAChoisi;
    public Personnage identiteJack;
    public CarteAlibi derniereCarteAlibiPiochee;
    private transient Runnable alibiListener;
    private transient Runnable appelTemoinListener;
    private transient Runnable tourEnqueteurListener;
    private transient Runnable tourJackListener;
    private transient Runnable finPartieListener;
    private transient Runnable tourChangeListener;

    public int sabliersDeJack = 0; // Le vrai nombre de sabliers de Jack
    public int sabliersMinimumDeJack = 0; // Le nombre de sabliers de Jack qu'on peut afficher pour tout le monde
    private final int MAX_SABLIER = 6;
    public volatile Joueur gagnant = null;
    public boolean coursePoursuiteActive = false;

    private final int MAX_TOUR = 8;
    public int numeroTour = 0;
    public int totalActionsJouees;
    public boolean jackVisibleCeTour;

    public Ia ia = new Ia();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    public boolean IaEnCours = false;
    public boolean estSimulation = false;
    public int niveauJack;
    public int niveauEnqueteur;
    public String pseudoEnqueteur;
    public String pseudoJack;
    public boolean changement;

    Deque<Partie> undo = new ArrayDeque<>();
    Deque<Partie> redo = new ArrayDeque<>();

    private volatile boolean freeze = false;

    public int simSabliers = 0;
    public int simAlibis = 0;
    public int simElimines = 0;
    public int simJackVis = 0;
    public int simTours = 0;

    public boolean isFreeze() { return freeze; }
    public void setFreeze(boolean freeze) { this.freeze = freeze; }
    public void toggleFreeze() { this.freeze = !this.freeze; }

    private transient Runnable preAppelTemoinListener;

    public void setPreAppelTemoinListener(Runnable listener) {
        this.preAppelTemoinListener = listener;
    }

    private void firePreAppelTemoinEvent() {
        if (preAppelTemoinListener != null) preAppelTemoinListener.run();
    }

    public src.modele.ia.laboratoire_genetique.ProfilGenetique adnJack = null;
    public src.modele.ia.laboratoire_genetique.ProfilGenetique adnInspecteur = null;
    public boolean forcerPoidsExternes = false;

    private boolean isSolo = true;

    public Partie(Joueur joueurChoisi, int niveauJack, int niveauEnqueteur, String pseudoEnqueteur, String pseudoJack){
        this.joueurChoisi = joueurChoisi;
        this.niveauJack = niveauJack;
        this.niveauEnqueteur = niveauEnqueteur;
        this.pseudoEnqueteur = pseudoEnqueteur;
        this.pseudoJack = pseudoJack;
        actions = new PartieActions(this);
        jetonsAction = new ArrayList<>();
        cartesAlibiPioche = new ArrayList<>();
        detectives = new ArrayList<>();
        suspects = new ArrayList<>();
        initialiserPartie();
        tourSuivant();
        changement = true;
        verifTourIa();
    }

    public String getPseudoEnqueteur() { return pseudoEnqueteur; }
    public String getPseudoJack() { return pseudoJack; }
    public void setPseudoEnqueteur(String pseudo) { this.pseudoEnqueteur = pseudo; }
    public void setPseudoJack(String pseudo) { this.pseudoJack = pseudo; }

    public void setTourChangeListener(Runnable listener) {
        this.tourChangeListener = listener;
    }

    public void fireTourChangeEvent() {
        if (tourChangeListener != null) tourChangeListener.run();
    }

    public void setFinPartieListener(Runnable listener) {
        this.finPartieListener = listener;
    }

    public void fireFinPartieEvent() {
        if (finPartieListener != null) finPartieListener.run();
    }

    public void setTourEnqueteurListener(Runnable listener) {
        this.tourEnqueteurListener = listener;
    }

    public void fireTourEnqueteurEvent() {
        if (tourEnqueteurListener != null) tourEnqueteurListener.run();
    }

    public void setTourJackListener(Runnable listener) {
        this.tourJackListener = listener;
    }

    public void fireTourJackEvent() {
        if (tourJackListener != null) tourJackListener.run();
    }

    public void setAppelTemoinListener(Runnable listener) {
        this.appelTemoinListener = listener;
    }

    public void fireAppelTemoinEvent() {
        if (appelTemoinListener != null) appelTemoinListener.run();
        else{
            appelATemoin();
            tourSuivant();
            verifTourIa();
        }
    }

    public void setAlibiListener(Runnable listener) {
        this.alibiListener = listener;
    }

    public void fireAlibiEvent() {
        if (alibiListener != null) alibiListener.run();
    }

    private void initialiserPartie() {
        district = District.creerDistrict();
        PartieInit.initialiserJetons(this);
        PartieInit.initialiserCartesAlibis(this);
        PartieInit.initialiserDetectives(this);
        PartieInit.initialiserSuspects(this);
        PartieInit.initialiserIdentiteJack(this);
    }

    public void reset() {
        if (freeze) return;
        gagnant = null;
        coursePoursuiteActive = false;
        numeroTour = 0;
        jackVisibleCeTour = false;
        sabliersDeJack = 0;
        sabliersMinimumDeJack = 0;

        jetonsAction.clear();
        cartesAlibiPioche.clear();
        detectives.clear();
        suspects.clear();
        undo.clear();
        redo.clear();
        initialiserPartie();
        tourSuivant();
        changement = true;
        verifTourIa();
    }

    public void terminerTour() {
        if (freeze) return;
        appelATemoin();
    }

    public boolean[][] getMasqueTuilesVisibles() {
        boolean[][] masque = new boolean[3][3];
        for (Detective d : detectives) {
            for (Personnage p : district.personnagesVisiblesParDetective(d)) {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        if (district.get(row, col).getPersonnage() == p) {
                            masque[row][col] = true;
                        }
                    }
                }
            }
        }
        return masque;
    }

    public void appelATemoin(){
        if (freeze) return;
        int sabliersAvant = sabliersDeJack;
        int suspectsAvant = suspects.size();

        HashSet<Personnage> personnagesVisibles = new HashSet<>();
        for (Detective d : detectives) {
            personnagesVisibles.addAll(district.personnagesVisiblesParDetective(d));
        }

        List<Personnage> avantAppel = new ArrayList<>(suspects);

        if(personnagesVisibles.contains(identiteJack)){
            suspects.retainAll(personnagesVisibles);
            jackVisibleCeTour = true;
        } else {
            jackVisibleCeTour = false;
            sabliersDeJack++;
            sabliersMinimumDeJack++;
            if (!this.estSimulation) System.out.println("Jack n'est pas visible, il gagne le sablier du tour. Il est a " + sabliersDeJack + " sabliers");
            suspects.removeAll(personnagesVisibles);
        }
        avantAppel.removeAll(suspects);
        for(Personnage p : avantAppel){
            district.innocenter(p);
        }
        verifFinDePartie();

        simTours++;
        if (jackVisibleCeTour) {
            simJackVis++;
        }
        simSabliers += (sabliersDeJack - sabliersAvant);
        simElimines += (suspectsAvant - suspects.size());
    }

    public void changerJoueur() {
        if (freeze) return;
        joueurCourant = (joueurCourant == Joueur.JACK) ? Joueur.ENQUETEUR : Joueur.JACK;
    }

    public void apresAction(){
        if (freeze) return;
        totalActionsJouees++;
        switch(totalActionsJouees){
            case 1, 3:
                if(!estSimulation) System.out.println("Le joueur change. Le joueur est maintenant " +
                        ((joueurCourant == Joueur.JACK) ? Joueur.ENQUETEUR : Joueur.JACK));
                changerJoueur();
                if (joueurCourant == Joueur.ENQUETEUR) {
                    fireTourEnqueteurEvent();
                } else {
                    fireTourJackEvent();
                }
                if (!isPartieTerminee()) verifTourIa();
                break;

            case 4:
                if(isSolo) {
                    if (!estSimulation) {
                        fireAppelTemoinEvent();
                        firePreAppelTemoinEvent();
                    }
                }
                break;

            default:
                if (!isPartieTerminee()) verifTourIa();
                break;
        }
    }

    public void tourSuivant(){
        if (freeze) return;
        if(isPartieTerminee()) return;
        numeroTour++;
        totalActionsJouees = 0;
        district.reinitialiserFlagsRotation();
        if(numeroTour % 2 != 0){
            for(JetonAction j : jetonsAction){
                j.lancer();
                if (!this.estSimulation) System.out.println("Jeton lancé sur : " + j.getActionVisible());
            }
            joueurCourant = Joueur.ENQUETEUR;
            if (!this.estSimulation) System.out.println("Le joueur est l'Enquêteur");
        } else {
            for(JetonAction j : jetonsAction){
                j.retourner();
                if (!this.estSimulation) System.out.println("Jeton retourné sur : " + j.getActionVisible());
            }
            joueurCourant = Joueur.JACK;
            if (!this.estSimulation) System.out.println("Le joueur est Jack");
        }
        if (joueurCourant == Joueur.ENQUETEUR) fireTourEnqueteurEvent();
        else fireTourJackEvent();
        fireTourChangeEvent();
    }

    public void verifTourIa(){
        if(isPartieTerminee()) return;

        if(joueurCourant == Joueur.JACK){
            ia.setDifficulte(niveauJack);
        } else {
            ia.setDifficulte(niveauEnqueteur);
        }

        if((joueurCourant == Joueur.JACK && niveauJack != -1) || (joueurCourant == Joueur.ENQUETEUR && niveauEnqueteur != -1)){
            lanceIa();
        }
    }

    public void lanceIa() {
        //if (freeze) return;
        if(IaEnCours || isPartieTerminee() || actions.getActionsPossibles().isEmpty()) return;
        if (estSimulation) return;
        changement = false;

        boolean estJack = (joueurCourant == Joueur.JACK);

        if (ia.difficulte == -1) {
            return;
        }

        IaEnCours = true;
        CompletableFuture
                .supplyAsync(() -> {
                    CoupIa coupChoisi = ia.choisirAction(this, estJack);
                    while(freeze){
                        try {
                            System.out.println("attente a cause du freeze");
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            System.err.println("Erreur lors de la pause du freeze dans lanceIa : ");
                        }
                    }
                    return coupChoisi;
                }, executor)
                .thenAccept(iaCoup -> {
                    SwingUtilities.invokeLater(() -> {
                        
                        IaEnCours = false;
                        if(changement){
                            verifTourIa();
                            return;
                        }
                        if (iaCoup != null && iaCoup.action != null) {
                            jouerCoup(iaCoup);
                        }
                        else {
                            System.err.println("\t L'IA n'a pas choisi d'action valide !");
                        }
                    });
                });
    }

    public double simulerEtNoter(CoupIa coup, boolean estJack) {
        System.out.println("DEBUG: Je suis dans Partie.java, je simule le coup...");
        Partie copie = new Partie(this);
        copie.jouerCoup(coup);
        if(estJack){
            return EvaluateurIa.jeSuisJack(copie);
        } else {
            return EvaluateurIa.jeSuisEnqueteur(copie);
        }
    }

    public void jouerCoup(CoupIa coupIa) {
        if (freeze) return;
        int sabliersAvant = sabliersDeJack;
        int suspectsAvant = suspects.size();

        switch (coupIa.action) {
            case HOLMES:
                actions.deplacerDetective(detectives.get(0), coupIa.para1 + 1);
                break;
            case WATSON:
                actions.deplacerDetective(detectives.get(1), coupIa.para1 + 1);
                break;
            case TOBY:
                actions.deplacerDetective(detectives.get(2), coupIa.para1 + 1);
                break;
            case JOKER:
                if(coupIa.para2 == 0){
                    actions.joker(null);
                } else {
                    actions.joker(detectives.get(coupIa.para1));
                }
                break;
            case ROTATION:
                for(int i = 0; i < jetonsAction.size(); i++){
                    if(jetonsAction.get(i).getActionVisible() == Action.ROTATION && !jetonsAction.get(i).isJoue()){
                        actions.rotationQuartier(i, coupIa.para1 / 3, coupIa.para1 % 3, coupIa.para2 + 1);
                        break;
                    }
                }
                break;
            case ECHANGE:
                actions.echange(coupIa.para1 / 3, coupIa.para1 % 3, coupIa.para2 / 3, coupIa.para2 % 3);
                break;
            case ALIBI:
                actions.alibi();
                break;
        }

        if (coupIa.action == Action.ALIBI) {
            simAlibis++;
        }
        simSabliers += (sabliersDeJack - sabliersAvant);
        simElimines += (suspectsAvant - suspects.size());
    }

    public void verifFinDePartie(){
        if (freeze) return;
        if(gagnant != null) return;
        if(coursePoursuiteActive){
            verifFinCoursePoursuite();
        } else {
            if((suspects.size() == 1) && sabliersDeJack >= MAX_SABLIER){
                if (jackVisibleCeTour){
                    declarerGagnant(Joueur.ENQUETEUR);
                }
                else if (numeroTour >= MAX_TOUR){
                    declarerGagnant(Joueur.JACK);
                } else {
                    coursePoursuiteActive = true;
                    if(!estSimulation) System.out.println("Début de course poursuite !!!");
                    verifFinCoursePoursuite();
                }
            }
            else if(suspects.size() == 1){
                declarerGagnant(Joueur.ENQUETEUR);
            }
            else if(sabliersDeJack >= MAX_SABLIER){
                declarerGagnant(Joueur.JACK);
            }
            else if (numeroTour >= MAX_TOUR) {
                declarerGagnant(Joueur.JACK);
            }
        }
    }

    private void declarerGagnant(Joueur vainqueur) {
        if (freeze) return;
        this.gagnant = vainqueur;
        if (!this.estSimulation) {
            if (this.gagnant == Joueur.ENQUETEUR){
                if(!estSimulation) System.out.println("----- Victoire de l'Enquêteur ! -----");
            } else {
                if(!estSimulation) System.out.println("----- Victoire de Jack ! -----");
            }
            fireFinPartieEvent();
        }
    }

    public void verifFinCoursePoursuite(){
        if (freeze) return;
        if(jackVisibleCeTour){
            if(!estSimulation) System.out.println("Fin de la course poursuite.");
            declarerGagnant(Joueur.ENQUETEUR);
            coursePoursuiteActive = false;
            return;
        }
        if(numeroTour >= MAX_TOUR){
            if(!estSimulation) System.out.println("Fin de la course poursuite.");
            declarerGagnant(Joueur.JACK);
            coursePoursuiteActive = false;
        }
    }

    public boolean isPartieTerminee(){
        return gagnant != null;
    }

    public Joueur getGagnant(){
        return gagnant;
    }

    public Partie(Partie p) {
        this.estSimulation = true;
        this.adnJack = p.adnJack;
        this.adnInspecteur = p.adnInspecteur;
        this.district = new District(p.district);
        this.actions = new PartieActions(this);

        this.jetonsAction = new ArrayList<>();
        for (JetonAction j : p.jetonsAction) {
            this.jetonsAction.add(new JetonAction(j));
        }

        this.detectives = new ArrayList<>();
        for (Detective d : p.detectives) {
            this.detectives.add(new Detective(d));
        }

        this.cartesAlibiPioche = new ArrayList<>(p.cartesAlibiPioche);
        this.suspects = new ArrayList<>(p.suspects);

        this.joueurCourant = p.joueurCourant;
        this.identiteJack = p.identiteJack;
        this.sabliersDeJack = p.sabliersDeJack;
        this.numeroTour = p.numeroTour;
        this.totalActionsJouees = p.totalActionsJouees;
        this.jackVisibleCeTour = p.jackVisibleCeTour;
        this.coursePoursuiteActive = p.coursePoursuiteActive;
        this.gagnant = p.gagnant;
        this.joueurChoisi = p.joueurChoisi;
        this.IAChoisi = p.IAChoisi;
        this.difficulteIAChoisi = p.difficulteIAChoisi;
        this.niveauJack = p.niveauJack;
        this.niveauEnqueteur = p.niveauEnqueteur;
        this.undo = new ArrayDeque<>();
        this.redo = new ArrayDeque<>();

        this.simSabliers = p.simSabliers;
        this.simAlibis = p.simAlibis;
        this.simElimines = p.simElimines;
        this.simJackVis = p.simJackVis;
        this.simTours = p.simTours;
    }

    public void kill() {
        IaEnCours = false;
        gagnant = Joueur.ENQUETEUR;
    }

    private boolean estTourHumain(){
        return (joueurCourant == Joueur.JACK && niveauJack == -1) ||
                (joueurCourant == Joueur.ENQUETEUR && niveauEnqueteur == -1);
    }

    public void saveEtat() {
        if(estSimulation) return;
        if(estTourHumain()){
            undo.push(new Partie(this));
            redo.clear();
        }
    }

    public void annuler(){
        if (freeze) return;
        if(undo.isEmpty()) return;
        boolean joueurCourantEstIa = (joueurCourant == Joueur.JACK && niveauJack != -1)
                || (joueurCourant == Joueur.ENQUETEUR && niveauEnqueteur != -1);
        if(joueurCourantEstIa) return;
        redo.push(new Partie(this));
        Partie ancien = undo.pop();
        changement = true;
        restaurer(ancien);
        verifTourIa();
    }

    public void refaire(){
        if (freeze) return;
        if(redo.isEmpty()) return;
        undo.push(new Partie(this));
        Partie nouveau = redo.pop();
        changement = true;
        restaurer(nouveau);
        verifTourIa();
    }

    private void restaurer(Partie p){
        if (freeze) return;
        this.district = p.district;
        this.detectives = p.detectives;
        this.suspects = p.suspects;
        this.identiteJack = p.identiteJack;
        this.sabliersDeJack = p.sabliersDeJack;
        this.joueurCourant = p.joueurCourant;
        this.totalActionsJouees = p.totalActionsJouees;
        this.numeroTour = p.numeroTour;
        this.jetonsAction = p.jetonsAction;
        this.cartesAlibiPioche = p.cartesAlibiPioche;
        this.gagnant = p.gagnant;
        this.jackVisibleCeTour = p.jackVisibleCeTour;
        this.coursePoursuiteActive = p.coursePoursuiteActive;
        this.IAChoisi = p.IAChoisi;
        this.difficulteIAChoisi = p.difficulteIAChoisi;
        this.joueurChoisi = p.joueurChoisi;
        this.niveauEnqueteur = p.niveauEnqueteur;
        this.niveauJack = p.niveauJack;

        if (!estSimulation) verifTourIa();
    }

    public PartieSnapshot toSnapshot() {
        return PartieSaveMapper.toSnapshot(this);
    }

    public void fromSnapshot(PartieSnapshot snap){
        if (freeze) return;
        PartieSaveMapper.fromSnapshot(this, snap);
    }

    public GameSave toGameSave() {
        return PartieSaveMapper.toGameSave(this);
    }

    public void fromGameSave(GameSave save) {
        if (freeze) return;
        PartieSaveMapper.fromGameSave(this, save);
        changement = true;
        verifTourIa();
    }

    public void setIsSolo(boolean isSolo) {
        this.isSolo = isSolo;
    }
    public boolean getIsSolo() {
        return isSolo;
    }
}
