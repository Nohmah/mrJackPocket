package src.modele.ia.laboratoire_genetique;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UsineGenetique {
    private double TAUX_MUTATION = 0.15;
    private double FORCE_MUTATION = 0.05;

    public void evoluer(PoolProfils pool) {
        pool.trierParScore();
        List<ProfilGenetique> nouvelleGen = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ProfilGenetique p = pool.getProfil(i);
            nouvelleGen.add(new ProfilGenetique(p.role, p.poidsDebut, p.poidsMilieu, p.poidsFin));
        }
        List<ProfilGenetique> urne = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            urne.add(pool.getProfil(i));
        }
        Random r = new Random();
        while (nouvelleGen.size() < 100) {
            ProfilGenetique p1 = urne.get(r.nextInt(urne.size()));
            ProfilGenetique p2 = urne.get(r.nextInt(urne.size()));
            ProfilGenetique p3 = urne.get(r.nextInt(urne.size()));
            ProfilGenetique enfant = croiser(p1, p2, p3);
            muter(enfant);
            nouvelleGen.add(enfant);
        }
        pool.listeProfils = nouvelleGen;
    }

    public ProfilGenetique croiser(ProfilGenetique p1, ProfilGenetique p2, ProfilGenetique p3) {
        return new ProfilGenetique(p1.role, p1.poidsDebut, p2.poidsMilieu, p3.poidsFin);
    }

    public void muter(ProfilGenetique p) {
        Random r = new Random();
        if (r.nextDouble() < TAUX_MUTATION) appliquerMutation(p.poidsDebut, r);
        if (r.nextDouble() < TAUX_MUTATION) appliquerMutation(p.poidsMilieu, r);
        if (r.nextDouble() < TAUX_MUTATION) appliquerMutation(p.poidsFin, r);
        p.normaliser(p.poidsDebut);
        p.normaliser(p.poidsMilieu);
        p.normaliser(p.poidsFin);
    }

    private void appliquerMutation(double[] tab, Random r) {
        int index = r.nextInt(tab.length);
        double modif = (r.nextDouble() * 2 - 1) * FORCE_MUTATION;
        tab[index] = Math.max(0, tab[index] + modif);
    }
}