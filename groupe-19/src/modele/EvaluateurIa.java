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
        public int distanceEnqueteurs;// somme des distances entre les enquêteurs
        public int visibiliteJack; //a quelle point Jack est visible (nb de couloir ou il peut être vu)

        public dataPartie(Partie partie) {
            this.nbTours = partie.numeroTour;
            this.nbSabliers = partie.sabliersDeJack;

            //methode utiliser pour voir tout les personnages visible
            HashSet<Personnage> personnagesVisibles = new HashSet<>();
            personnagesVisibles.addAll(partie.district.personnagesVisiblesParDetective(partie.detectives.get(0)));
            personnagesVisibles.addAll(partie.district.personnagesVisiblesParDetective(partie.detectives.get(1)));
            personnagesVisibles.addAll(partie.district.personnagesVisiblesParDetective(partie.detectives.get(2)));

            this.nbVisible = personnagesVisibles.size();
            this.JackVisible = personnagesVisibles.contains(partie.identiteJack);
            this.nbInvisible = partie.suspects.size() - this.nbVisible;

            this.distanceEnqueteurs = 0;
            //calcul de la distance entre les enquêteurs
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
            //fin de la recursion
            //System.out.println("Entre dans visibiliteCalcul avec dir : " + dir + " et position : " + x + "," + y);
            if (dir == 0 && x == 0 && partie.district.get(x, y).getOrientationMur() != Orientation.NORD) {
                return 1;
            }
            if (dir == 1 && y == 2 && partie.district.get(x, y).getOrientationMur() != Orientation.EST) {
                return 1;
            }
            if (dir == 2 && x == 2 && partie.district.get(x, y).getOrientationMur() != Orientation.SUD) {
                return 1;
            }
            if (dir == 3 && y == 0 && partie.district.get(x, y).getOrientationMur() != Orientation.OUEST) {
                return 1;
            }

            if (dir % 2 == 0) {//NOrd ou Sud
                if (partie.district.get(x, y).getOrientationMur() != Orientation.CARDINAUX[dir] && partie.district.get(x + (dir - 1), y).getOrientationMur() != Orientation.CARDINAUX[(dir + 2) % 4]) {
                    return visibiliteCalcul(dir, x + (dir - 1), y, partie);
                }
                return 0;
            } else {//Est ou Ouest
                if (partie.district.get(x, y).getOrientationMur() != Orientation.CARDINAUX[dir] && partie.district.get(x, y - (dir - 2)).getOrientationMur() != Orientation.CARDINAUX[(dir + 2) % 4]) {
                    return visibiliteCalcul(dir, x, y - (dir - 2), partie);
                }
                return 0;
            }

        }

        public void visibiliteJack(Partie partie) {//mis dans une autre fonction pour éviter des calcul inutile
            System.out.println("Entre dans visibiliteJack (EvaluateurIa) : ");
            System.out.println("Identité de Jack : " + partie.identiteJack.nom + "avec couleur " + partie.identiteJack.couleur);
            int xjack = 0;
            int yjack = 0;
            //trouver la position de Jack
            for (xjack = 0; xjack < 3; xjack++) {
                for (yjack = 0; yjack < 3; yjack++) {
                    if (partie.district.get(xjack, yjack).getPersonnage() == partie.identiteJack) {
                        break;
                    }
                }
                if (yjack < 3) {
                    break;
                }
            }
            System.out.println("Position de Jack : " + xjack + "," + yjack);

            this.visibiliteJack = 0; // Initialisation de la visibilité de Jack
            for (int i = 0; i < 4; i++) {
                this.visibiliteJack += visibiliteCalcul(i, xjack, yjack, partie);
            }
            System.out.println("Visibilité de Jack : " + this.visibiliteJack);
        }
    }

    public static double jeSuisEnqueteur(Partie partie) {
        dataPartie data = new dataPartie(partie);
        double score = 0.0;

        // Application des 4 formules de l'inspecteur (Somme normalisée)
        score += calcDichotomie(data, partie);
        score += calcEfficaciteSpatiale(data, partie);
        score += calcProximiteTactique(data, partie);
        score += calcPrivationRessources(partie);

        return score;
    }

    public static double jeSuisJack(Partie partie) {
        dataPartie data = new dataPartie(partie);
        data.visibiliteJack(partie);
        double score = 0.0;

        // Application des 5 formules de Jack (Somme normalisée)
        score += calcBonusInvisibilite(data);
        score += calcAnonymat(data, partie);
        score += calcExpositionGeometrique(data);
        score += calcVictoireVirtuelle(data, partie);
        score += calcStabilite(partie);

        return score;
    }

    private static double calcDichotomie(dataPartie d, Partie p) {
        if (p.suspects.isEmpty()) return 1.0;
        double ratio = (double) d.nbVisible / p.suspects.size();
        return 1.0 - (2.0 * Math.abs(ratio - 0.5));
    }

    private static double calcEfficaciteSpatiale(dataPartie d, Partie p) {
        if (p.suspects.isEmpty()) return 0.0;
        int intersection = 0;
        // On récupère l'ensemble des suspects vus
        Set<Personnage> union = new HashSet<>();
        for (Detective det : p.detectives) {
            union.addAll(p.district.personnagesVisiblesParDetective(det));
        }
        // On identifie ceux vus par plus d'un inspecteur
        for (Personnage perso : union) {
            int voit = 0;
            for (Detective det : p.detectives) {
                if (p.district.personnagesVisiblesParDetective(det).contains(perso)) voit++;
            }
            if (voit > 1) intersection++;
        }
        return (double) (union.size() - intersection) / p.suspects.size();
    }

    private static double calcProximiteTactique(dataPartie d, Partie p) {
        // Normalisation de la distance (Max théorique autour de 18 pour 3 détectives)
        return 1.0 - (Math.min(18.0, (double) d.distanceEnqueteurs) / 18.0);
    }

    private static double calcPrivationRessources(Partie p) {
        // 1.0 si une carte a été piochée ce tour
        return p.cartesAlibiPioche.size() < 8 ? 1.0 : 0.0;
    }

    private static double calcBonusInvisibilite(dataPartie d) {
        return d.JackVisible ? 0.0 : 1.0;
    }

    private static double calcAnonymat(dataPartie d, Partie p) {
        if (p.suspects.isEmpty()) return 0.0;
        int nbMemeEtat = 0;
        for (Personnage s : p.suspects) {
            // On vérifie si le suspect a le même état (vu/caché) que Jack
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
        // Utilise ta méthode visibilitéJack qui calcule sur 4 directions
        return 1.0 - (d.visibiliteJack / 4.0);
    }

    private static double calcVictoireVirtuelle(dataPartie d, Partie p) {
        return Math.min(1.0, (double) d.nbSabliers / 6.0);
    }

    private static double calcStabilite(Partie p) {
        int nbChangements = 0;
        if (p.suspects.isEmpty()) return 1.0;

        // État initial
        Set<Personnage> vusInit = new HashSet<>();
        for (Detective det : p.detectives) vusInit.addAll(p.district.personnagesVisiblesParDetective(det));

        // Simulation des rotations sur les 9 tuiles
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                Quartier q = p.district.get(x, y);
                q.pivoter(1);

                Set<Personnage> vusApres = new HashSet<>();
                for (Detective det : p.detectives) vusApres.addAll(p.district.personnagesVisiblesParDetective(det));

                if (!vusApres.equals(vusInit)) nbChangements++;
                q.pivoter(3); // Remise en place
            }
        }
        return 1.0 - ((double) nbChangements / 9.0);
    }
}