package src.modele;

import java.util.ArrayList;
import java.util.List;

public class PartieActions {

    private final Partie partie;

    public PartieActions(Partie partie) {
        this.partie = partie;
    }

    public List<JetonAction> getJetonsActions() {
        return partie.jetonsAction;
    }

    public List<Action> getActionsPossibles() {
        List<Action> actionsPossibles = new ArrayList<>();
        for (JetonAction j : partie.jetonsAction) {
            if (!j.isJouee()) {
                actionsPossibles.add(j.getActionVisible());
            }
        }
        return actionsPossibles;
    }

    public CarteAlibi piocherCarteAlibi() {
        if (partie.cartesAlibiPioche.isEmpty()) return null;
        return partie.cartesAlibiPioche.remove(0);
    }

    public void deplacerDetective(Detective detective, int pas) {
        detective.deplacer(pas);
        System.out.println(detective.getType() + " avance de " + pas);
        partie.apresAction();
    }

    public void joker(Detective detective) {
        if (partie.joueurCourant == Partie.Joueur.ENQUETEUR) {
            detective.deplacer(1);
            System.out.println(detective.getType() + " avance de 1");
        } else {
            if (detective == null) {
                System.out.println("Mr. Jack choisit de ne déplacer aucun détective.");
            } else {
                detective.deplacer(1);
                System.out.println(detective.getType() + " avance de 1");
            }
        }
        partie.verifFinDePartie();
        if (partie.isPartieTerminee()) return;
        partie.apresAction();
    }

    public void rotationQuartier(int x, int y, int quarts) {
        Quartier q = partie.district.get(x, y);
        if (q.getADejaPivoteTour()) {
            System.out.println("INTERDIT");
            return;
        }
        q.pivoter(quarts);
        q.setADejaPivoteTour(true);
        System.out.println("Quartier pivoté");
        partie.apresAction();
    }

    public void echange(int x1, int y1, int x2, int y2) {
        partie.district.echanger(x1, y1, x2, y2);
        System.out.println("Échange");
        partie.apresAction();
    }

    public void alibi() {
        CarteAlibi carte = piocherCarteAlibi();
        if (carte == null) return;
        System.out.println("Carte alibi piochée : " + carte.getPersonnage());
        if (partie.joueurCourant == Partie.Joueur.JACK) {
            partie.sabliersDeJack += carte.getSabliers();
        } else {
            partie.suspects.remove(carte.getPersonnage());
            partie.district.innocenter(carte.getPersonnage());
        }
        partie.apresAction();
    }
}