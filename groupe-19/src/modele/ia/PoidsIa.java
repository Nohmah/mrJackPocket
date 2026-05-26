package src.modele.ia;

import src.modele.*;

public class PoidsIa {
    public enum Phase { DEBUT, MILIEU, FIN }

    public static final double[] POIDS_ENQ_DEBUT  = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
    public static final double[] POIDS_ENQ_MILIEU = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
    public static final double[] POIDS_ENQ_FIN    = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

    public static final double[] POIDS_JACK_DEBUT  = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
    public static final double[] POIDS_JACK_MILIEU = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
    public static final double[] POIDS_JACK_FIN    = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

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
