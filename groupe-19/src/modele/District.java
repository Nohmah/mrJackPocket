package src.modele;
import java.util.*;

/**
 * Le District est la grille 3x3 de Quartiers.
 *
 * Coordonées (convention ligne-colonne) :
 *   (0,0) haut-gauche ... (0,2) haut-droite
 *   (1,0) milieu-gauche ... (1,2) milieu-droite
 *   (2,0) bas-gauche ... (2,2) bas-droite
 *
 *   Chaque quartier est placé aléatoirement avec une orientation aléatoire.
 *
 */

public class District {
    private final Quartier[][] grille = new Quartier[3][3];

    public District(){ initialiser(); }

    private void initialiser(){
        List<Personnage> personnages = new ArrayList<>(Arrays.asList(Personnage.values()));
        Collections.shuffle(personnages);

        Orientation[] orientations = Orientation.values();
        Random rng = new Random();

        int index = 0;
        for(int x = 0; x < 3; x++){
            for(int y = 0; y < 3; y++){
                Orientation orMur = Orientation.CARDINAUX[rng.nextInt(4)];
                grille[x][y] = new Quartier(personnages.get(index++), orMur);
                //Pour vérifier si le modele correspond à la vue future
                System.out.println(personnages.get(index-1).nom + " - Mur : " + orMur);
            }

        }
    }

    public Quartier get(int x, int y){
        return grille[x][y];
    }

    public void echanger(int x1, int y1, int x2, int y2){
        Quartier tmp = grille[x1][y1];
        grille[x1][y1] = grille[x2][y2];
        grille[x2][y2] = tmp;
    }

    public void innocenter(Personnage personnage){
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                if (grille[i][j].getPersonnage() == personnage){
                    grille[i][j].innocenter();
                }
            }
        }
    }

    public List<Personnage> personnagesVisiblesParDetective(Detective detective){
        List<Personnage> personnagesVisibles = new ArrayList<>();
        Quartier premierQuartier = get(detective.getQx(), detective.getQy());

        // Peut on voir le personnage du 1er quartier ? (Le mur du 1er quartier bloque-t-il la ligne de vue de son personnage ?)
        if (premierQuartier.getOrientationMur()!=detective.getOrientation()){
            personnagesVisibles.add(premierQuartier.getPersonnage());

            // Le mur du premier quartier bloque t-il la ligne de vue ? (Peut on "sortir" du quartier ?)
            if (premierQuartier.getOrientationMur().index != (detective.getOrientation().index + 2) % 4) {
                /* EXPLICATION (Equivalent):
                Si une de ces choses est vrai, alors le mur du premier quartier empêche le détective de voir la suite
                detective.getOrientation()==Orientation.NORD && premierQuartier.getOrientationMur() == Orientation.SUD ||
                detective.getOrientation()==Orientation.EST && premierQuartier.getOrientationMur() == Orientation.OUEST ||
                detective.getOrientation()==Orientation.SUD && premierQuartier.getOrientationMur() == Orientation.NORD ||
                detective.getOrientation()==Orientation.OUEST && premierQuartier.getOrientationMur() == Orientation.EST*/

                // Si on arrive ici, le 1er quartier est "traversable"
                // On détermine le prochain quartier qui est sur le champ de vision du détective
                int x1 = detective.getQx();
                int y1 = detective.getQy();
                int x2 = 0;
                int y2 = switch (detective.getOrientation()) {
                    case NORD -> {
                        x2 = x1 - 1;
                        yield y1;
                    }
                    case EST -> {
                        x2 = x1;
                        yield y1 + 1;
                    }
                    case SUD -> {
                        x2 = x1 + 1;
                        yield y1;
                    }
                    case OUEST -> {
                        x2 = x1;
                        yield y1 - 1;
                    }
                    default -> 0;
                }; //(trop fort IntelliJ)
                Quartier deuxiemeQuartier = get(x2, y2);
                // Peut on voir le personnage du 2eme quartier ? (Le mur du 2eme quartier bloque-t-il la ligne de vue de son personnage ?
                if (deuxiemeQuartier.getOrientationMur() != detective.getOrientation()) {
                    personnagesVisibles.add(deuxiemeQuartier.getPersonnage());

                    // Le mur du deuxième quartier bloque t-il la ligne de vue ? (Peut on "sortir" du quartier ?)
                    if (deuxiemeQuartier.getOrientationMur().index != (detective.getOrientation().index + 2) % 4) {
                        // Si on arrive ici, le deuxième quartier est "traversable"
                        // On détermine le prochain (dernier) quartier qui est sur le champ de vision du détective
                        int x3 = 0;
                        int y3 = switch (detective.getOrientation()) {
                            case NORD -> {
                                x3 = x2;
                                yield y2 + 1;
                            }
                            case EST -> {
                                x3 = x2 - 1;
                                yield y2;
                            }
                            case SUD -> {
                                x3 = x2;
                                yield y2 - 1;
                            }
                            case OUEST -> {
                                x3 = x2 + 1;
                                yield y2;
                            }
                            default -> 0;
                        };
                        Quartier troisiemeQuartier = get(x3, y3);
                        // Peut on voir le personnage du 3eme quartier ? (Le mur du 3eme quartier bloque-t-il la ligne de vue de son personnage ?
                        if (troisiemeQuartier.getOrientationMur() != detective.getOrientation()) {
                            personnagesVisibles.add(troisiemeQuartier.getPersonnage());
                        }
                    }
                }
            }
        }
        return personnagesVisibles;
    }
}
