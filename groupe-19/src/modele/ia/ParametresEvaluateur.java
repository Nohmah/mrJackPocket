package src.modele.ia;

import src.modele.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParametresEvaluateur {

    public static double qualiteDuSplit(DataPartie d, Partie p) {
        if (p.suspects.isEmpty()) return 1.0;
        double ratio = (double) d.nbVisible / p.suspects.size();
        return 1.0 - (2.0 * Math.abs(ratio - 0.5));
    }

    public static double efficaciteRepartitionInspecteurs(Partie p) {
        if (p.suspects.isEmpty()) return 0.0;
        int intersection = 0;
        List<Personnage> union = new ArrayList<>();
        for (Detective det : p.detectives) {
            List<Personnage> vus = p.district.personnagesVisiblesParDetective(det);
            for (Personnage perso : vus) {
                if (union.contains(perso)) intersection++;
                union.add(perso);
            }
        }
        return (double) (union.size() - intersection) / p.suspects.size();
    }

    public static double distanceEnqueteursSuspects(DataPartie d) {
        return 1.0 - (Math.min(6.0, d.distanceEnqueteursSuspects) / 6.0);
    }

    public static double bonusAlibi(Partie p) {
        return (p.cartesAlibiPioche.size() < 8) ? 1.0 : 0.0;
    }

    public static double visibilite(DataPartie d) {
        return d.JackVisible ? 0.0 : 1.0;
    }

    public static double visibilitePossible(DataPartie d) {
        return 1.0 - (d.positionsPossibles / 3.0);
    }

    public static double expositionDeJack(DataPartie d) {
        return d.moyenneMursExposition;
    }

    public static double anonymat(DataPartie d, Partie p) {
        if (p.suspects.isEmpty()) return 0.0;
        int nbMemeEtat = d.JackVisible ? d.nbVisible : d.nbInvisible;
        return (double) nbMemeEtat / 9.0;
    }

    public static double stabiliteDuPlateau(Partie p) {
        int nbChangements = 0;
        if (p.suspects.isEmpty()) return 1.0;
        Set<Personnage> vusInit = new HashSet<>();
        for (Detective det : p.detectives) vusInit.addAll(p.district.personnagesVisiblesParDetective(det));

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                Quartier q = p.district.get(x, y);
                q.pivoter(1);
                Set<Personnage> vusApres = new HashSet<>();
                for (Detective det : p.detectives) vusApres.addAll(p.district.personnagesVisiblesParDetective(det));
                if (!vusApres.equals(vusInit)) nbChangements++;
                q.pivoter(3);
            }
        }
        return 1.0 - ((double) nbChangements / 9.0);
    }

    public static double accumulationSabliers(Partie p) {
        double maxSabliers = p.simTours * 1.0 + p.simAlibis * 2.0;
        if (maxSabliers <= 0) return 0.0;
        return Math.min(1.0, (double) p.simSabliers / maxSabliers);
    }

    public static double suspectsElimines(Partie p) {
        return Math.min(1.0, (double) p.simElimines / 8.0);
    }

    public static double alibisPioches(Partie p) {
        double maxAlibis = p.simTours > 0 ? p.simTours : 1.0;
        return Math.min(1.0, (double) p.simAlibis / maxAlibis);
    }

    public static double sabliersNonLaisses(Partie p) {
        if (p.simTours <= 0) return 0.0;
        return (double) p.simJackVis / p.simTours;
    }
}
