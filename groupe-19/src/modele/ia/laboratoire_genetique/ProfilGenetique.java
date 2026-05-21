package src.modele.ia.laboratoire_genetique;

import src.modele.Joueur;
import java.util.Random;

public class ProfilGenetique {
    public Joueur role;
    public double[] poidsDebut, poidsMilieu, poidsFin;
    public int score = 0;

    public ProfilGenetique(Joueur role, int nbPoids) {
        this.role = role;
        this.poidsDebut = new double[nbPoids];
        this.poidsMilieu = new double[nbPoids];
        this.poidsFin = new double[nbPoids];
        Random r = new Random();
        for (int i = 0; i < nbPoids; i++) {
            this.poidsDebut[i] = r.nextDouble();
            this.poidsMilieu[i] = r.nextDouble();
            this.poidsFin[i] = r.nextDouble();
        }
        normaliser(this.poidsDebut);
        normaliser(this.poidsMilieu);
        normaliser(this.poidsFin);
    }

    public ProfilGenetique(Joueur role, double[] d, double[] m, double[] f) {
        this.role = role;
        this.poidsDebut = d.clone();
        this.poidsMilieu = m.clone();
        this.poidsFin = f.clone();
        normaliser(this.poidsDebut);
        normaliser(this.poidsMilieu);
        normaliser(this.poidsFin);
    }

    public void normaliser(double[] tab) {
        double somme = 0;
        for (int i = 0; i < tab.length; i++) somme += tab[i];
        if (somme > 0) {
            for (int i = 0; i < tab.length; i++) tab[i] /= somme;
        }
    }

    public double[] getPoids(int tour) {
        if (tour <= 3) return poidsDebut;
        if (tour <= 6) return poidsMilieu;
        return poidsFin;
    }

    public void incrementerScore() {
        this.score++;
    }

    public void resetScore() {
        this.score = 0;
    }
}