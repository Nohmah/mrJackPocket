package src.modele;

import java.util.HashSet;
import java.util.Set;

public class EvaluateurIa {

    public static class dataPartie {
        public int nbTours;
        public int nbVisible;
        public int nbInvisible;
        public boolean JackVisible;
        public int nbSabliers;
        public int distanceEnqueteurs;
        public int visibiliteJack;

        public dataPartie(Partie partie) {
            this.nbTours = partie.numeroTour;
            this.nbSabliers = partie.sabliersDeJack;

            HashSet<Personnage> personnagesVisibles = new HashSet<>();
            personnagesVisibles.addAll(partie.district.personnagesVisiblesParDetective(partie.detectives.get(0)));
            personnagesVisibles.addAll(partie.district.personnagesVisiblesParDetective(partie.detectives.get(1)));
            personnagesVisibles.addAll(partie.district.personnagesVisiblesParDetective(partie.detectives.get(2)));

            this.nbVisible = personnagesVisibles.size();
            this.JackVisible = personnagesVisibles.contains(partie.identiteJack);
            this.nbInvisible = partie.suspects.size() - this.nbVisible;

            this.distanceEnqueteurs = 0;
            int distance = Math.abs(partie.detectives.get(0).getPosition() - partie.detectives.get(1).getPosition());
            if (distance > 6) distance = 12 - distance;
            this.distanceEnqueteurs += distance;
            distance = Math.abs(partie.detectives.get(0).getPosition() - partie.detectives.get(2).getPosition());
            if (distance > 6) distance = 12 - distance;
            this.distanceEnqueteurs += distance;
            distance = Math.abs(partie.detectives.get(2).getPosition() - partie.detectives.get(1).getPosition());
            if (distance > 6) distance = 12 - distance;
            this.distanceEnqueteurs += distance;
        }

        public int visibiliteCalcul(int dir, int x, int y, Partie partie) {
            if (dir == 0 && x == 0 && partie.district.get(x, y).getOrientationMur() != Orientation.NORD) return 1;
            if (dir == 1 && y == 2 && partie.district.get(x, y).getOrientationMur() != Orientation.EST) return 1;
            if (dir == 2 && x == 2 && partie.district.get(x, y).getOrientationMur() != Orientation.SUD) return 1;
            if (dir == 3 && y == 0 && partie.district.get(x, y).getOrientationMur() != Orientation.OUEST) return 1;

            if (dir % 2 == 0) {
                if (partie.district.get(x, y).getOrientationMur() != Orientation.CARDINAUX[dir] && partie.district.get(x + (dir - 1), y).getOrientationMur() != Orientation.CARDINAUX[(dir + 2) % 4]) {
                    return visibiliteCalcul(dir, x + (dir - 1), y, partie);
                }
                return 0;
            } else {
                if (partie.district.get(x, y).getOrientationMur() != Orientation.CARDINAUX[dir] && partie.district.get(x, y - (dir - 2)).getOrientationMur() != Orientation.CARDINAUX[(dir + 2) % 4]) {
                    return visibiliteCalcul(dir, x, y - (dir - 2), partie);
                }
                return 0;
            }
        }

        public void visibiliteJack(Partie partie) {
            int xjack = 0; // Correction warning initialisation
            int yjack = 0;
            boolean trouve = false;
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    if (partie.district.get(x, y).getPersonnage() == partie.identiteJack) {
                        xjack = x;
                        yjack = y;
                        trouve = true;
                        break;
                    }
                }
                if (trouve) break;
            }

            this.visibiliteJack = 0;
            for (int i = 0; i < 4; i++) {
                this.visibiliteJack += visibiliteCalcul(i, xjack, yjack, partie);
            }
        }
    }

    public static double jeSuisEnqueteur(Partie partie) {
        dataPartie data = new dataPartie(partie);
        double[] poids = PoidsIa.getPoidsEnqueteur(partie.numeroTour);

        double score = 0.0;
        score += poids[0] * calcDichotomie(data, partie);
        score += poids[1] * calcEfficaciteSpatiale(partie); // Modifié : retrait de 'data'
        score += poids[2] * calcProximiteTactique(data);    // Modifié : retrait de 'partie'
        score += poids[3] * calcPrivationRessources(partie);

        return score;
    }

    public static double jeSuisJack(Partie partie) {
        dataPartie data = new dataPartie(partie);
        data.visibiliteJack(partie);
        double[] poids = PoidsIa.getPoidsJack(partie.numeroTour);

        double score = 0.0;
        score += poids[0] * calcBonusInvisibilite(data);
        score += poids[1] * calcAnonymat(data, partie);
        score += poids[2] * calcExpositionGeometrique(data);
        score += poids[3] * calcVictoireVirtuelle(data); // Modifié : retrait de 'p'
        score += poids[4] * calcStabilite(partie);

        return score;
    }

    private static double calcDichotomie(dataPartie d, Partie p) {
        if (p.suspects.isEmpty()) return 1.0;
        double ratio = (double) d.nbVisible / p.suspects.size();
        return 1.0 - (2.0 * Math.abs(ratio - 0.5));
    }

    private static double calcEfficaciteSpatiale(Partie p) { // Retrait de 'd' inutilisé
        if (p.suspects.isEmpty()) return 0.0;
        int intersection = 0;
        Set<Personnage> union = new HashSet<>();
        for (Detective det : p.detectives) {
            union.addAll(p.district.personnagesVisiblesParDetective(det));
        }
        for (Personnage perso : union) {
            int voit = 0;
            for (Detective det : p.detectives) {
                if (p.district.personnagesVisiblesParDetective(det).contains(perso)) voit++;
            }
            if (voit > 1) intersection++;
        }
        return (double) (union.size() - intersection) / p.suspects.size();
    }

    private static double calcProximiteTactique(dataPartie d) { // Retrait de 'p' inutilisé
        return 1.0 - (Math.min(18.0, d.distanceEnqueteurs) / 18.0); // Retrait cast redondant
    }

    private static double calcPrivationRessources(Partie p) {
        return p.cartesAlibiPioche.size() < 8 ? 1.0 : 0.0;
    }

    private static double calcBonusInvisibilite(dataPartie d) {
        return d.JackVisible ? 0.0 : 1.0;
    }

    private static double calcAnonymat(dataPartie d, Partie p) {
        if (p.suspects.isEmpty()) return 0.0;
        int nbMemeEtat = 0;
        for (Personnage s : p.suspects) {
            boolean estVu = false;
            for (Detective det : p.detectives) {
                if (p.district.personnagesVisiblesParDetective(det).contains(s)) {
                    estVu = true;
                    break;
                }
            }
            if (estVu == d.JackVisible) nbMemeEtat++;
        }
        return (double) nbMemeEtat / p.suspects.size();
    }

    private static double calcExpositionGeometrique(dataPartie d) {
        return 1.0 - (d.visibiliteJack / 4.0);
    }

    private static double calcVictoireVirtuelle(dataPartie d) { // Retrait de 'p' inutilisé
        return Math.min(1.0, (double) d.nbSabliers / 6.0);
    }

    private static double calcStabilite(Partie p) {
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
    }}