package src.modele.ia.test_parametre;

import src.modele.Joueur;

public class AffichageNomAblation {
    public String afficheNom(Joueur r, int i) {
        if (r == Joueur.JACK) {
            if (i == 0) return "Visibilite actuelle";
            if (i == 1) return "Visibilite possible";
            if (i == 2) return "Exposition";
            if (i == 3) return "Troupeau";
            if (i == 4) return "Sabliers";
            return "Stabilite";
        } else {
            if (i == 0) return "Split 50-50";
            if (i == 1) return "Inspecteurs";
            if (i == 2) return "Distance";
            if (i == 3) return "Alibis";
            if (i == 4) return "Elimination";
            return "Sabliers";
        }
    }
}
