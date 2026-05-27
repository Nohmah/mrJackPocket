package src.modele.ia.test_parametre;

import src.modele.Joueur;
import src.modele.ia.laboratoire_genetique.ProfilGenetique;

public class MainTestParametre {
    public static void main(String[] args) {
        // Remplacer ces tableaux avec tes rÃ©sultats du CSV demain matin
        double[] jackD =  {0.0, 0.12, 0.23, 0.19, 0.37, 0.08};
        double[] jackM =  {0.0, 0.12, 0.23, 0.19, 0.37, 0.08};
        double[] jackF =  {0.0, 0.12, 0.23, 0.19, 0.37, 0.08};

        double[] enqD = {0.03, 0.03, 0.0, 0.21, 0.70, 0.01};
        double[] enqM = {0.03, 0.03, 0.0, 0.21, 0.70, 0.01};
        double[] enqF = {0.03, 0.03, 0.0, 0.21, 0.70, 0.01};

        ProfilGenetique championJack = new ProfilGenetique(Joueur.JACK, jackD, jackM, jackF);
        ProfilGenetique championEnq = new ProfilGenetique(Joueur.ENQUETEUR, enqD, enqM, enqF);

        AreneTest arene = new AreneTest();
        AffichageNomAblation aff = new AffichageNomAblation();
        int nbMatchs = 20000;

        int victoiresJackRef = 0;
        for (int i = 0; i < nbMatchs; i++) {
            if (arene.match(championJack, championEnq)) {
                victoiresJackRef++;
            }
        }
        double refJack = (double) victoiresJackRef / nbMatchs;
        double refEnq = 1.0 - refJack;

        System.out.println("Taux de victoire reference Jack : " + (int)(refJack * 100) + "%");
        System.out.println("Taux de victoire reference Enqueteur : " + (int)(refEnq * 100) + "%");

        System.out.println("\nTest de parametres Jack :");
        for (int i = 0; i < 6; i++) {
            ProfilAblate mutant = new ProfilAblate(championJack, i);
            int victoires = 0;
            for (int m = 0; m < nbMatchs; m++) {
                if (arene.match(mutant, championEnq)) victoires++;
            }
            double taux = (double) victoires / nbMatchs;
            System.out.println(aff.afficheNom(Joueur.JACK, i) + " : " + (int)(taux * 100) + "% (chute de " + (int)((refJack - taux) * 100) + "%)");
        }

        System.out.println("\nTest de parametres Enqueteur :");
        for (int i = 0; i < 6; i++) {
            ProfilAblate mutant = new ProfilAblate(championEnq, i);
            int victoires = 0;
            for (int m = 0; m < nbMatchs; m++) {
                if (!arene.match(championJack, mutant)) victoires++;
            }
            double taux = (double) victoires / nbMatchs;
            System.out.println(aff.afficheNom(Joueur.ENQUETEUR, i) + " : " + (int)(taux * 100) + "% (chute de " + (int)((refEnq - taux) * 100) + "%)");
        }
    }
}