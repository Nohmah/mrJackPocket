package src.modele;

import java.util.HashSet;

public class EvaluateurIa {

    public static class dataPartie{
        public int nbTours;
        public int nbVisible;
        public int nbInvisible;
        public boolean JackVisible;
        public int nbSabliers;
        public int distanceEnqueteurs;// somme des distances entre les enquêteurs
        public int visibilitéJack; //a quelle point Jack est visible (nb de couloir ou il peut être vu)

        public dataPartie(Partie partie){
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
            if(distance > 6) distance = 12 - distance;
            this.distanceEnqueteurs += distance;
            distance = Math.abs(partie.detectives.get(0).getPosition() - partie.detectives.get(2).getPosition());
            if(distance > 6) distance = 12 - distance;
            this.distanceEnqueteurs += distance;
            distance = Math.abs(partie.detectives.get(2).getPosition() - partie.detectives.get(1).getPosition());
            if(distance > 6) distance = 12 - distance;
            this.distanceEnqueteurs += distance;
            
        }

        public int visibiliteCalcul(int dir,int x, int y, Partie partie){
            //fin de la recursion
            //System.out.println("Entre dans visibiliteCalcul avec dir : " + dir + " et position : " + x + "," + y);
            if(dir == 0 && x == 0 && partie.district.get(x, y).getOrientationMur() != Orientation.NORD){
                return 1;
            }
            if(dir == 1 && y == 2 && partie.district.get(x, y).getOrientationMur() != Orientation.EST){
                return 1;
            }
            if(dir == 2 && x == 2 && partie.district.get(x, y).getOrientationMur() != Orientation.SUD){
                return 1;
            }
            if(dir == 3 && y == 0 && partie.district.get(x, y).getOrientationMur() != Orientation.OUEST){
                return 1;
            }

            if(dir%2 == 0){//NOrd ou Sud
                if(partie.district.get(x, y).getOrientationMur() != Orientation.CARDINAUX[dir] && partie.district.get(x + (dir - 1), y).getOrientationMur() != Orientation.CARDINAUX[(dir + 2) % 4]){
                    return visibiliteCalcul(dir, x + (dir - 1), y, partie);
                }
                return 0;
            }
            else{//Est ou Ouest
                if(partie.district.get(x, y).getOrientationMur() != Orientation.CARDINAUX[dir] && partie.district.get(x, y - (dir - 2)).getOrientationMur() != Orientation.CARDINAUX[(dir + 2) % 4]){
                    return visibiliteCalcul(dir, x, y - (dir - 2), partie);
                }
                return 0;
            }
            
        }

        public void visibiliteJack(Partie partie){//mis dans une autre fonction pour éviter des calcul inutile
            System.out.println("Entre dans visibiliteJack (EvaluateurIa) : ");
            System.out.println("Identité de Jack : " + partie.identiteJack.nom + "avec couleur " + partie.identiteJack.couleur);
            int xjack = 0; int yjack = 0;
            //trouver la position de Jack
            for(xjack = 0; xjack < 3; xjack++){
                for(yjack = 0; yjack < 3; yjack++){
                    if(partie.district.get(xjack, yjack).getPersonnage() == partie.identiteJack){
                        break;
                    }
                }
                if(yjack < 3){
                    break;
                }
            }
            System.out.println("Position de Jack : " + xjack + "," + yjack);

            this.visibilitéJack = 0; // Initialisation de la visibilité de Jack
            for(int i = 0; i < 4; i++){
                this.visibilitéJack += visibiliteCalcul(i, xjack, yjack, partie);
            }
            System.out.println("Visibilité de Jack : " + this.visibilitéJack);
        }
    }

    public static double jeSuisEnqueteur(Partie partie) {
        dataPartie data = new dataPartie(partie);
        double score = 0.0;
        score -= 2 * Math.abs(data.nbInvisible - data.nbVisible); // essayer de max le 50/50
        score -= data.nbSabliers; // essayer de minimiser les sabliers
        score += data.distanceEnqueteurs; // essayer de separer les enquêteurs (pas sur)
        score -= data.nbSabliers;

        return score;
    }

    public static double jeSuisJack(Partie partie) {
        dataPartie data = new dataPartie(partie);
        data.visibiliteJack(partie);
        double score = 0.0;
        if(data.JackVisible){
            score -= 1; // éviter d'être visible
            score += data.nbVisible; // essayer de max le nombre de visible si jack est visible
        }
        else {
            score += 1; // essayer d'être invisible
            score += data.nbInvisible; // essayer de max le nombre d'invisible si jack est invisible
        }
        score += data.nbSabliers; // essayer de max les sabliers
        score -= data.distanceEnqueteurs; // essayer de rapprocher les enquêteurs (pas sur)
        score -= data.visibilitéJack; // essayer de minimiser la visibilité de Jack (nb de couloir ou il peut être vu)
        return score;
    }
}