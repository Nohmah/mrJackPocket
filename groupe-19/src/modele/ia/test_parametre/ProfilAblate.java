package src.modele.ia.test_parametre;

import src.modele.ia.laboratoire_genetique.ProfilGenetique;

public class ProfilAblate extends ProfilGenetique {
    public int iSuppr;

    public ProfilAblate(ProfilGenetique p, int i) {
        super(p.role, p.poidsDebut, p.poidsMilieu, p.poidsFin);
        this.iSuppr = i;
    }

    @Override
    public double[] getPoids(int tour) {
        double[] p = super.getPoids(tour);
        double[] nouv = new double[6];
        double tot = 0;
        for (int i = 0; i < 6; i++) {
            if (i == iSuppr) {
                nouv[i] = 0;
            } else {
                nouv[i] = p[i];
                tot = tot + p[i];
            }
        }
        for (int i = 0; i < 6; i++) {
            if (tot > 0) {
                nouv[i] = nouv[i] / tot;
            }
        }
        return nouv;
    }
}

