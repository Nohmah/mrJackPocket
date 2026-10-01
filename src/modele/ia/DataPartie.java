package src.modele.ia;

import src.modele.*;
import java.util.HashSet;

public class DataPartie {
    public int nbTours;
    public int nbVisible;
    public int nbInvisible;
    public boolean JackVisible;
    public int nbSabliers;
    public double distanceEnqueteursSuspects;
    public int positionsPossibles;
    public double moyenneMursExposition;

    public DataPartie(Partie partie) {
        this.nbTours = partie.numeroTour;
        this.nbSabliers = partie.sabliersDeJack;

        HashSet<Personnage> personnagesVisibles = new HashSet<>();
        for (Detective det : partie.detectives) {
            personnagesVisibles.addAll(partie.district.personnagesVisiblesParDetective(det));
        }

        this.nbVisible = personnagesVisibles.size();
        this.JackVisible = personnagesVisibles.contains(partie.identiteJack);
        this.nbInvisible = partie.suspects.size() - this.nbVisible;

        double sommeDistancesMin = 0;
        for (Detective det : partie.detectives) {
            int posX = getXDetective(det.getPosition());
            int posY = getYDetective(det.getPosition());
            int distMin = Integer.MAX_VALUE;

            for (Personnage s : partie.suspects) {
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        if (partie.district.get(x, y).getPersonnage() == s) {
                            int d = Math.abs(x - posX) + Math.abs(y - posY);
                            if (d < distMin) distMin = d;
                        }
                    }
                }
            }
            sommeDistancesMin += (distMin == Integer.MAX_VALUE) ? 0 : distMin;
        }
        this.distanceEnqueteursSuspects = sommeDistancesMin / 3.0;
    }

    public void visibiliteJack(Partie partie) {
        int xj = 0, yj = 0;
        boolean trouve = false;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (partie.district.get(x, y).getPersonnage() == partie.identiteJack) {
                    xj = x; yj = y; trouve = true; break;
                }
            }
            if (trouve) break;
        }

        Quartier qJack = partie.district.get(xj, yj);
        Orientation murNat = qJack.getOrientationMur();
        int poss = 0;
        int totMurs = 0;

        for (int i = 0; i < 4; i++) {
            int m = compterMurs(i, xj, yj, partie);
            totMurs += m;
            if (Orientation.CARDINAUX[i] != murNat && m == 0) poss++;
        }
        this.positionsPossibles = poss;
        this.moyenneMursExposition = (totMurs / 4.0) / 3.0;
    }

    private int compterMurs(int dir, int x, int y, Partie partie) {
        int m = 0;
        Quartier q = partie.district.get(x, y);
        if (q.getOrientationMur() == Orientation.CARDINAUX[dir]) m++;

        int nx = x, ny = y;
        if (dir == 0) nx--;
        else if (dir == 1) ny++;
        else if (dir == 2) nx++;
        else if (dir == 3) ny--;

        if (nx >= 0 && nx < 3 && ny >= 0 && ny < 3) {
            Quartier s = partie.district.get(nx, ny);
            if (s.getOrientationMur() == Orientation.CARDINAUX[(dir + 2) % 4]) m++;
            m += compterMurs(dir, nx, ny, partie);
        }
        return m;
    }

    private int getXDetective(int pos) {
        if (pos <= 2) return -1;
        if (pos <= 5) return pos - 3;
        if (pos <= 8) return 3;
        return 11 - pos;
    }

    private int getYDetective(int pos) {
        if (pos <= 2) return pos;
        if (pos <= 5) return 3;
        if (pos <= 8) return 8 - pos;
        return -1;
    }
}