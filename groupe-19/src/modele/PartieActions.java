package src.modele;

import java.util.ArrayList;
import java.util.List;

/**
 * Contient les méthodes en lien avec et pour réaliser les actions des jetons Actions
 **/

public class PartieActions {

    private final Partie partie;

    /** Constructeur **/
    public PartieActions(Partie partie) {
        this.partie = partie;
    }

    /** Renvoie la liste des actions possibles. Utilisé par l'IA **/
    public List<Action> getActionsPossibles() {
        List<Action> actionsPossibles = new ArrayList<>();
        for (JetonAction j : partie.jetonsAction) {
            if (!j.isJoue()) {
                actionsPossibles.add(j.getActionVisible());
            }
        }
        return actionsPossibles;
    }

    /** Enlève et renvoie (=pioche) une carte alibi de cartesAlibiPioche **/
    public CarteAlibi piocherCarteAlibi() {
        if (partie.cartesAlibiPioche.isEmpty()) return null;
        return partie.cartesAlibiPioche.remove(0);
    }

    /** Action d'un jeton action. Déplace un détective de [pas] dans le sens horaire autour du District **/
    public void deplacerDetective(Detective detective, int pas) {
        Action action;
        switch (detective.getType()) {
            case HOLMES -> action = Action.HOLMES;
            case WATSON -> action = Action.WATSON;
            case TOBY   -> action = Action.TOBY;
            default -> {
                return;
            }
        }
        JetonAction jeton = getSiJetonDisponible(action);
        if (jeton==null){
            return;
        }
        partie.saveEtat();
        detective.deplacer(pas);
        jeton.setJoue(true);
        partie.apresAction();
    }

    /** Action d'un jeton action. Déplace un détective de 1 pas dans le sens horaire
     * ou si Jack, peut aussi ne rien faire **/
    public void joker(Detective detective) {
        JetonAction jeton = getSiJetonDisponible(Action.JOKER);
        if (jeton==null){
            return;
        }
        partie.saveEtat();
        if (detective == null){
            if(!partie.estSimulation) System.out.println("Mr. Jack choisit de ne déplacer aucun détective.");
        } else {
            detective.deplacer(1);
            if(!partie.estSimulation) System.out.println(detective.getType() + " avance de 1");
        }
        jeton.setJoue(true);
        partie.apresAction();
    }

    /** Action d'un jeton action. Pivote un quartier de [quarts] quarts de tour **/
    public void rotationQuartier(int JetonIndex, int x, int y, int quarts) {
        JetonAction jeton = partie.jetonsAction.get(JetonIndex);
        if (jeton.isJoue()){
            return;
        }
        Quartier quartier = partie.district.get(x, y);
        if (quartier.getAPivote()) {
            System.out.println("Il est interdit de pivoter un quartier déjà pivoté dans le même tour de jeu");
            return;
        }
        partie.saveEtat();
        quartier.pivoter(quarts);
        if(!partie.estSimulation) System.out.println("Quartier pivoté");
        jeton.setJoue(true);
        partie.apresAction();
    }

    /** Action d'un jeton action. Échange deux quartiers **/
    public void echange(int x1, int y1, int x2, int y2) {
        JetonAction jeton = getSiJetonDisponible(Action.ECHANGE);
        if (jeton==null){
            return;
        }
        partie.saveEtat();
        partie.district.echanger(x1, y1, x2, y2);
        if(!partie.estSimulation) System.out.println("Échange de quartier");
        jeton.setJoue(true);
        partie.apresAction();
    }

    /** Action d'un jeton action. Pioche une carte alibi puis innocente si détective, ajoute les sabliers si Jack **/
    public void alibi() {
        JetonAction jeton = getSiJetonDisponible(Action.ALIBI);
        if (jeton==null){
            return;
        }
        partie.saveEtat();
        CarteAlibi carte = piocherCarteAlibi();
        if (carte == null) {
            System.out.println("Plus de carte alibi dans la pioche !");
            jeton.setJoue(true);
            return;
        }
        if(!partie.estSimulation) System.out.println("Carte alibi piochée : " + carte.getPersonnage());
        if (partie.joueurCourant == Joueur.JACK) {
            partie.sabliersDeJack += carte.getSabliers();
            if(!partie.estSimulation) System.out.println("Jack récupère les " + carte.getSabliers() + " sabliers de la carte. Il en est à " + partie.sabliersDeJack + " sabliers");
        } else {
            partie.suspects.remove(carte.getPersonnage());
            partie.district.innocenter(carte.getPersonnage());
        }
        jeton.setJoue(true);
        partie.apresAction();
    }

    /** Renvoie le JetonAction présent dans [jetonsAction] associé à l'action donnée en argument
     * s'il n'a pas été joué (et s'il est visible, protection) **/
    private JetonAction getSiJetonDisponible(Action action) {
        for (JetonAction jeton : partie.jetonsAction) {
            if (jeton.getActionVisible() == action && !jeton.isJoue()) {
                return jeton;
            }
        }
        System.out.println("Le jeton action" + action + " a déjà été utilisé ce tour (ou bien n'est pas sensé être visible...)");
        return null;
    }
}