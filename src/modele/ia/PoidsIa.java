package src.modele.ia;

import src.modele.*;

public class PoidsIa {
    public enum Phase { DEBUT, MILIEU, FIN }

    public static final double[] POIDS_ENQ_DEBUT  = {0.01, 0.30, 0.09, 0.12, 0.22, 0.22};
    public static final double[] POIDS_ENQ_MILIEU = {0.13, 0.22, 0.18, 0.28, 0.13, 0.03};
    public static final double[] POIDS_ENQ_FIN    = { 0.17, 0.15, 0.16, 0.9, 0.09, 0.30};

    public static final double[] POIDS_JACK_DEBUT  = {0.03, 0.03, 0.0, 0.41, 0.51, 0.01};
    public static final double[] POIDS_JACK_MILIEU = {0.26, 0.0, 0.08, 0.36, 0.27, 0.01};
    public static final double[] POIDS_JACK_FIN    = {0.15, 0.13, 0.33, 0.11, 0.23, 0.05};

    private static Phase determinerPhase(int tour) {
        if (tour <= 3) return Phase.DEBUT;
        if (tour <= 5) return Phase.MILIEU;
        return Phase.FIN;
    }

    public static double[] getPoidsEnqueteur(int tour) {
        Phase p = determinerPhase(tour);
        if (p == Phase.DEBUT) return POIDS_ENQ_DEBUT;
        if (p == Phase.MILIEU) return POIDS_ENQ_MILIEU;
        return POIDS_ENQ_FIN;
    }

    public static double[] getPoidsJack(int tour) {
        Phase p = determinerPhase(tour);
        if (p == Phase.DEBUT) return POIDS_JACK_DEBUT;
        if (p == Phase.MILIEU) return POIDS_JACK_MILIEU;
        return POIDS_JACK_FIN;
    }
}
