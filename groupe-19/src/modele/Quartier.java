package src.modele;

/**
 * Représente un quartier (une tuile de rue).
 * Chaque quartier a :
 *   - un personnage (qui est visible sur la face Suspect).
 *   - une orientation : direction vers laquelle le mur bloque la vue.
 *   - une face : SUSPECT (personnage visible) ou VIDE (personnage innocenté).
 * Le mur bloque la ligne de vue d'un Detective qui s'y trouve du côté de l'orientation du mur.
 **/

public class Quartier {

    public enum Face {SUSPECT, VIDE}
    private final Personnage personnage;
    private Orientation orientationMur;
    private Face face;
    private boolean aPivote;

    /** Constructeur **/
    public Quartier(Personnage personnage, Orientation orientationMur){
        this.personnage = personnage;
        this.orientationMur = orientationMur;
        this.face = Face.SUSPECT;
        this.aPivote = false;
    }

    /** Renvoie true si le quartier montre sa face 'suspect', false sinon **/
    public boolean estFaceSuspect(){
        return face == Face.SUSPECT;
    }

    /** Définit si le quartier a été pivoté **/
    public void setAPivote(boolean value) {
        this.aPivote = value;
    }

    // ---- Set/Get ----
    /** Renvoie si le quartier a été pivoté **/
    public boolean getAPivote(){
        return aPivote;
    }

    /** Revoie le personnage du quartier **/
    public Personnage getPersonnage(){
        return this.personnage;
    }

    /** Revoie l'orientation du mur **/
    public Orientation getOrientationMur(){
        return this.orientationMur;
    }

    /** Définit l'orientation du mur **/
    public void setOrientationMur(Orientation orientationMur) {
        this.orientationMur = orientationMur;
    }

    // ---- Mutation ----
    /** Change l'orientation du mur du nombre de quarts de tour donné (dans le sens horaire).
     * (1 = 90°, 2 = 180°, 3 = 270°, etc.) **/
    public void pivoter(int quarts){
        this.orientationMur = orientationMur.rotation(quarts);
        this.setAPivote(true);
    }

    /** Change la face du quartier pour VIDE
     * (doit être appelé sur le quartier dont le personnage a été innoncenté) **/
    public void retourner(){
        this.face = Face.VIDE;
    }
}
