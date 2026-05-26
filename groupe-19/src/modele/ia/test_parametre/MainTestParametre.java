package src.modele.ia.test_parametre;

import src.modele.Joueur;
import src.modele.ia.PoidsIa;
import src.modele.ia.laboratoire_genetique.ProfilGenetique;

public class MainTestParametre {
    public static void main(String[] args) {
        ProfilGenetique championJack = new ProfilGenetique(Joueur.JACK, PoidsIa.POIDS_JACK_DEBUT, PoidsIa.POIDS_JACK_MILIEU, PoidsIa.POIDS_JACK_FIN);
        ProfilGenetique championEnq = new ProfilGenetique(Joueur.ENQUETEUR, PoidsIa.POIDS_ENQ_DEBUT, PoidsIa.POIDS_ENQ_MILIEU, PoidsIa.POIDS_ENQ_FIN);

        AreneTest arene = new AreneTest();
        AffichageNomAblation aff = new AffichageNomAblation();
        int nbMatchs = 40;

        int victoiresJackRef = 0;
        for (int i = 0; i < nbMatchs; i++) {
            if (arene.match(championJack, championEnq)) {
                victoiresJackRef++;
            }
        }
        double refJack = (double) victoiresJackRef / nbMatchs;
        double refEnq = 1.0 - refJack;

        System.out.println("Taux de victoire de reference Jack : " + (int)(refJack * 100) + "%");
        System.out.println("Taux de victoire de reference Enqueteur : " + (int)(refEnq * 100) + "%");

        System.out.println("\nTest de parametres Jack :");
        for (int i = 0; i < 6; i++) {
            ProfilAblate mutant = new ProfilAblate(championJack, i);
            int victoires = 0;
            for (int m = 0; m < nbMatchs; m++) {
                if (arene.match(mutant, championEnq)) {
                    victoires++;
                }
            }
            double taux = (double) victoires / nbMatchs;
            double chute = refJack - taux;
            System.out.println(aff.afficheNom(Joueur.JACK, i) + " : " + (int)(taux * 100) + "% (chute de " + (int)(chute * 100) + "%)");
        }

        System.out.println("\nTest de parametres Enqueteur :");
        for (int i = 0; i < 6; i++) {
            ProfilAblate mutant = new ProfilAblate(championEnq, i);
            int victoires = 0;
            for (int m = 0; m < nbMatchs; m++) {
                if (!arene.match(championJack, mutant)) {
                    victoires++;
                }
            }
            double taux = (double) victoires / nbMatchs;
            double chute = refEnq - taux;
            System.out.println(aff.afficheNom(Joueur.ENQUETEUR, i) + " : " + (int)(taux * 100) + "% (chute de " + (int)(chute * 100) + "%)");
        }
    }
}

