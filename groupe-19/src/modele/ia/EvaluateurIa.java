package src.modele.ia;

import src.modele.*;

public class EvaluateurIa {

    public static double jeSuisEnqueteur(Partie partie) {
        DataPartie data = new DataPartie(partie);
        double[] poids;

        if (partie.adnInspecteur != null && !partie.forcerPoidsExternes) {
            poids = partie.adnInspecteur.getPoids(partie.numeroTour);
        } else {
            poids = PoidsIa.getPoidsEnqueteur(partie.numeroTour);
        }

        double score = 0.0;
        score += poids[0] * ParametresEvaluateur.qualiteDuSplit(data, partie);
        score += poids[1] * ParametresEvaluateur.efficaciteRepartitionInspecteurs(partie);
        score += poids[2] * ParametresEvaluateur.distanceEnqueteursSuspects(data);
        score += poids[3] * ParametresEvaluateur.alibisPioches(partie);
        score += poids[4] * ParametresEvaluateur.suspectsElimines(partie);
        score += poids[5] * ParametresEvaluateur.sabliersNonLaisses(partie);

        double borneMax = 0.0;
        if (partie.niveauEnqueteur == 0) {
            borneMax = 0.5;
        } else if (partie.niveauEnqueteur == 1) {
            borneMax = 0.3;
        } else if (partie.niveauEnqueteur == 2) {
            borneMax = 0.0;
        }
        score += Math.random() * borneMax;

        return score;
    }

    public static double jeSuisJack(Partie partie) {
        DataPartie data = new DataPartie(partie);
        data.visibiliteJack(partie);
        double[] poids;

        if (partie.adnJack != null && !partie.forcerPoidsExternes) {
            poids = partie.adnJack.getPoids(partie.numeroTour);
        } else {
            poids = PoidsIa.getPoidsJack(partie.numeroTour);
        }

        double score = 0.0;
        score += poids[0] * ParametresEvaluateur.visibilite(data);
        score += poids[1] * ParametresEvaluateur.visibilitePossible(data);
        score += poids[2] * ParametresEvaluateur.expositionDeJack(data);
        score += poids[3] * ParametresEvaluateur.anonymat(data, partie);
        score += poids[4] * ParametresEvaluateur.accumulationSabliers(partie);
        score += poids[5] * ParametresEvaluateur.stabiliteDuPlateau(partie);

        double borneMax = 0.0;
        if (partie.niveauJack == 0) {
            borneMax = 0.5;
        } else if (partie.niveauJack == 1) {
            borneMax = 0.3;
        } else if (partie.niveauJack == 2) {
            borneMax = 0.0;
        }
        score += Math.random() * borneMax;

        return score;
    }
}