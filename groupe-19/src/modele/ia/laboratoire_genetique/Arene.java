package src.modele.ia.laboratoire_genetique;

import java.io.OutputStream;
import java.io.PrintStream;
import src.modele.Partie;
import src.modele.Joueur;
import src.modele.ia.IaMinMax;
import src.modele.ia.CoupIa;

public class Arene {
    private int profondeurMinMax = 2;

    public void evaluerPool(PoolProfils poolMutant, PoolProfils poolAdverse) {
        System.out.print("Evaluation en cours : ");
        int compteur = 0;
        for (ProfilGenetique mutant : poolMutant.listeProfils) {
            mutant.resetScore();
            for (int i = 0; i < 40; i++) {
                ProfilGenetique adv = poolAdverse.getAdversaireAleatoire();
                if (lancerMatch(mutant, adv)) {
                    mutant.incrementerScore();
                }
            }
            compteur++;
            if (compteur % 10 == 0) {
                System.out.print(".");
            }
        }
        System.out.println(" Termine !");
    }

    public boolean lancerMatch(ProfilGenetique mutant, ProfilGenetique adv) {
        PrintStream consoleOriginaleOut = System.out;
        PrintStream consoleOriginaleErr = System.err;
        PrintStream fluxSilencieux = new PrintStream(new OutputStream() {
            public void write(int b) {}
        });
        System.setOut(fluxSilencieux);
        System.setErr(fluxSilencieux);
        try {
            Partie p = new Partie(Joueur.ENQUETEUR, -1, -1, "ENQUETEUR", "JACK");
            if (mutant.role == Joueur.JACK) {
                p.adnJack = mutant;
                p.adnInspecteur = adv;
            } else {
                p.adnJack = adv;
                p.adnInspecteur = mutant;
            }
            int security = 0;
            while (!p.isPartieTerminee() && security < 100) {
                p.changement = false;
                CoupIa coup = IaMinMax.choisirActionMinMax(p, p.joueurCourant == Joueur.JACK, profondeurMinMax);
                if (coup != null) {
                    p.jouerCoup(coup);
                }
                p.apresAction();
                security++;
            }
            System.setOut(consoleOriginaleOut);
            System.setErr(consoleOriginaleErr);
            return p.getGagnant() == mutant.role;
        } catch (Exception e) {
            System.setOut(consoleOriginaleOut);
            System.setErr(consoleOriginaleErr);
            return false;
        }
    }
}