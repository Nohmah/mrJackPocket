package src.modele.ia;

import src.modele.*;

public class EvaluateurIa {

    public static double jeSuisEnqueteur(Partie partie) {
        DataPartie data = new DataPartie(partie);
        double[] poids;

        if (partie.adnInspecteur != null) {
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

        return score;
    }

    public static double jeSuisJack(Partie partie) {
        DataPartie data = new DataPartie(partie);
        data.visibiliteJack(partie);
        double[] poids;

        if (partie.adnJack != null) {
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

        return score;
    }
}
