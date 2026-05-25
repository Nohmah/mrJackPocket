package src.modele;
import java.util.*;

/**
 * Représente le District, la grille 3x3 composée de Quartiers (tuiles).
 * (ligne, colonne)
 *
 *      (0,0) (0,1) (0,2)
 *      (1,0) (1,1) (1,2)
 *      (2,0) (2,1) (2,2)
 *
 **/

public class District {
    private final Quartier[][] grille = new Quartier[3][3];

    /** Constructeur
     * Fabrique un District composé de quartiers placés à une position aléatoire avec une orientation aléatoire
     * (à part les quartiers à côté des détectives qui doivent leur montrer un mur au commencement de la partie)
     **/
    private District(){
        List<Personnage> personnages = new ArrayList<>(Arrays.asList(Personnage.values()));
        Collections.shuffle(personnages);
        Random rng = new Random();
        int index = 0;
        for(int x = 0; x < 3; x++){
            for(int y = 0; y < 3; y++){
                Orientation orMur;
                if (x==0 && y==0){
                    orMur = Orientation.OUEST;
                } else if (x==0 && y==2){
                    orMur = Orientation.EST;
                } else if (x==2 && y==1){
                    orMur = Orientation.SUD;
                } else {
                    orMur = Orientation.CARDINAUX[rng.nextInt(4)];
                }
                grille[x][y] = new Quartier(personnages.get(index++), orMur);
                //System.out.println(personnages.get(index - 1).nom + " - Mur : " + orMur);
            }
        }
    }

    /** Construit et renvoie l'objet District **/
    public static District creerDistrict() {
        return new District();
    }

    /** Renvoie le quartier du district ayant les coordonnées fournies **/
    public Quartier get(int x, int y){
        if (x<0 || x>2 || y<0 || y>2){
           System.out.println("Un get(int x, int y) quartier a été appelé avec des coordonnées invalides");
           return null;
        }
        return grille[x][y];
    }

    /** Renvoie le quartier du district ayant les coordonnées fournies **/
    public void echanger(int x1, int y1, int x2, int y2){
        Quartier tmp = grille[x1][y1];
        grille[x1][y1] = grille[x2][y2];
        grille[x2][y2] = tmp;
    }

    /** Innocente un personnage (retourne le quartier du personnage sur sa face vide) **/
    public void innocenter(Personnage personnage){
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                if (grille[i][j].getPersonnage() == personnage){
                    grille[i][j].retourner();
                    if (grille[i][j].getPersonnage() == Personnage.JOSEPH_LANE){
                        grille[i][j].setOrientationMur(Orientation.AUCUN);
                    }
                }
            }
        }
    }

    /** Flag tous les quartiers comme n'ayant pas pivoté **/
    public void reinitialiserFlagsRotation() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                grille[i][j].setAPivote(false);
            }
        }
    }

    /** Renvoie une liste des coordonnées du prochain quartier dans le champ de vision d'un détective **/
    private int[] getCoordonneesQuartierSuivant(int x, int y, Orientation orientation) {
        return switch (orientation) {
            case NORD -> new int[]{x+1, y};
            case EST -> new int[]{x, y-1};
            case SUD -> new int[]{x-1, y};
            case OUEST -> new int[]{x, y+1};
            default -> new int[]{-1, -1};
        };
    }

    /** Retourne une liste de tous les personnages visibles par le détective donné en argument **/
    public List<Personnage> personnagesVisiblesParDetective(Detective detective) {
        List<Personnage> personnagesVisibles = new ArrayList<>();
        Orientation orientationDetective = detective.getOrientation();
        int x = detective.getQx();
        int y = detective.getQy();

        for (int i = 0; i < 3; i++) {
            Quartier quartier = get(x, y);

            // Le mur du quartier bloque-t-il la vue de son personnage ? (mêmes orientations)
            if (quartier.getOrientationMur() == orientationDetective) break;

            if (quartier.estFaceSuspect()) {
                personnagesVisibles.add(quartier.getPersonnage());
            }

            // Le mur du quartier bloque-t-il la vue du quartier suivant ? (orientations opposées)
            if (quartier.getOrientationMur().index == (orientationDetective.index + 2) % 4) break;

            // Calcul des coordonnées du quartier suivant
            int[] coordonneesQuartierSuivant = getCoordonneesQuartierSuivant(x, y, orientationDetective);
            x = coordonneesQuartierSuivant[0];
            y = coordonneesQuartierSuivant[1];
        }
        //System.out.println(detective.getType() + " voit : " + personnagesVisibles);
        return personnagesVisibles;
    }

    /** Constructeur de copie pour l'IA **/
    public District(District d) {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                this.grille[x][y] = new Quartier(d.get(x, y));
            }
        }
    }
}
