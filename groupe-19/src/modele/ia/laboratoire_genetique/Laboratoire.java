package src.modele.ia.laboratoire_genetique;

import src.modele.Joueur;
import java.io.*;
import java.util.Arrays;

public class Laboratoire {
    private PoolProfils poolJack = new PoolProfils(Joueur.JACK, 6);
    private PoolProfils poolInsp = new PoolProfils(Joueur.ENQUETEUR, 4);
    private UsineGenetique usine = new UsineGenetique();
    private Arene arene = new Arene();
    private int NB_CYCLES_MAX = 100;
    private int NB_GEN_PAR_ROLE = 50;

    public static void main(String[] args) {
        Laboratoire lab = new Laboratoire();
        for (int c = 1; c <= lab.NB_CYCLES_MAX; c++) {
            System.out.println("Debut Cycle " + c);
            lab.evoluerRole(Joueur.JACK, c);
            if (lab.verifierArretManuel()) break;
            lab.evoluerRole(Joueur.ENQUETEUR, c);
            if (lab.verifierArretManuel()) break;
        }
    }

    public void evoluerRole(Joueur role, int cycle) {
        PoolProfils cible = (role == Joueur.JACK) ? poolJack : poolInsp;
        PoolProfils adversaire = (role == Joueur.JACK) ? poolInsp : poolJack;
        for (int g = 1; g <= NB_GEN_PAR_ROLE; g++) {
            arene.evaluerPool(cible, adversaire);
            cible.trierParScore();
            exporterStats(role, cycle, g, cible);
            usine.evoluer(cible);
        }
    }

    public void exporterStats(Joueur role, int c, int g, PoolProfils pool) {
        ProfilGenetique meilleur = pool.getProfil(0);
        double somme = 0;
        for (ProfilGenetique p : pool.listeProfils) somme += p.score;
        double moyenne = somme / 100.0;
        String data = role + ";Cycle" + c + ";Gen" + g + ";MaxScore:" + meilleur.score + ";Moyenne:" + moyenne + ";Poids:" + Arrays.toString(meilleur.poidsDebut);
        //System.out.println(data);
        try (FileWriter fw = new FileWriter("stats_genetiques.csv", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(data);
        } catch (IOException e) {
            System.err.println("Erreur fichier stats");
        }
    }

    public boolean verifierArretManuel() {
        File f = new File("stop.txt");
        return f.exists();
    }
}