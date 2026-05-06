package src.modele;
/**
 * Représente une tuile de rue (Quartier).
 * Chaque quartier a :
 *   - un personnage (face Suspect).
 *   - une orientation : direction vers laquelle le MUR bloque la vue.
 *   - une face : SUSPECT (personnage visible) ou VIDE(innoncenté).
 *
 * Le mur bloque la ligne de vue d'un Detective qui s'y trouve du côté de l'orientation du mur.
 */

public class Quartier {

    public enum Face { SUSPECT, VIDE}
    private final Personnage personnage;
    private Orientation orientationMur;
    private Face face;
    private boolean aDejaPivoteTour;

    public Quartier(Personnage perso, Orientation mur){
        personnage = perso;
        orientationMur = mur;
        face = Face.SUSPECT;
        aDejaPivoteTour = false;
    }

    // ---- Set/Get ----
    public boolean getADejaPivoteTour(){
        return aDejaPivoteTour;
    }

    public Personnage getPersonnage(){
        return this.personnage;
    }

    public Orientation getOrientationMur(){
        return this.orientationMur;
    }

    public boolean estSuspect(){
        return face == Face.SUSPECT;
    }

    // ---- Mutation ----

    public void pivoter(int quarts){
        orientationMur = orientationMur.rotation(quarts);
    }

    public void innocenter(){
        face = Face.VIDE;
    }

    public void setADejaPivoteTour(boolean value) {
        aDejaPivoteTour = value;
    }
}
