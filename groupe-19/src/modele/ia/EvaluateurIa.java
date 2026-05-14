package src.modele.ia;

import src.modele.*;

public class EvaluateurIa {

    public static double jeSuisEnqueteur(Partie partie) {
        DataPartie data = new DataPartie(partie);
        double[] poids = PoidsIa.getPoidsEnqueteur(partie.numeroTour);

        double score = 0.0;
        score += poids[0] * ParametresEvaluateur.qualiteDuSplit(data, partie);
        score += poids[1] * ParametresEvaluateur.efficaciteRepartitionInspecteurs(partie);
        score += poids[2] * ParametresEvaluateur.distanceEnqueteursSuspects(data);
        score += poids[3] * ParametresEvaluateur.bonusAlibi(partie);

        return score;
    }

    public static double jeSuisJack(Partie partie) {
        DataPartie data = new DataPartie(partie);
        data.visibiliteJack(partie);
        double[] poids = PoidsIa.getPoidsJack(partie.numeroTour);

        double score = 0.0;
        score += poids[0] * ParametresEvaluateur.visibilite(data);
        score += poids[1] * ParametresEvaluateur.visibilitePossible(data);
        score += poids[2] * ParametresEvaluateur.expositionDeJack(data);
        score += poids[3] * ParametresEvaluateur.anonymat(data, partie);
        score += poids[4] * ParametresEvaluateur.bonusAlibi(partie);
        score += poids[5] * ParametresEvaluateur.stabiliteDuPlateau(partie);

        return score;
    }
}