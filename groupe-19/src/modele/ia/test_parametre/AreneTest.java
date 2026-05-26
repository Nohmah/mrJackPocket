package src.modele.ia.test_parametre;

import src.modele.Partie;
import src.modele.Joueur;
import src.modele.ia.IaMinMax;
import src.modele.ia.CoupIa;
import src.modele.ia.laboratoire_genetique.ProfilGenetique;
import java.io.OutputStream;
import java.io.PrintStream;

public class AreneTest {
    public boolean match(ProfilGenetique j, ProfilGenetique e) {
        PrintStream original = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        try {
            Partie p = new Partie(Joueur.ENQUETEUR, -1, -1, "E", "J");
            p.estSimulation = true;
            p.adnJack = j;
            p.adnInspecteur = e;
            int s = 0;
            while (p.getGagnant() == null && s < 100) {
                s++;
                CoupIa c = IaMinMax.choisirActionMinMax(p, p.joueurCourant == Joueur.JACK, 2);
                if (c != null) {
                    p.jouerCoup(c);
                }
                if (p.totalActionsJouees >= 4) {
                    p.appelATemoin();
                    if (p.getGagnant() == null) {
                        p.tourSuivant();
                    }
                }
            }
            System.setOut(original);
            return p.getGagnant() == Joueur.JACK;
        } catch (Exception ex) {
            System.setOut(original);
            return false;
        }
    }
}
