package src.modele;

/**
 * Représente un jeton Action. Chaque jeton a deux faces avec une action (différente) sur chaque face.
 * Chaque jeton a une seule face visible, l'autre face est cachée.
 * Lors d'une partie, à chaque début de tours Impairs (1-3-5-7) les jetons sont lancés.
 * À chaque début de tours Pairs (2-4-6-8) les jetons préalablement lancés sont retournés,
 * et la face cachée devient visible (et inversement).
 * Les jetons Actions ne peuvent être joués qu'une fois dans un tour.
 **/

public class JetonAction {
    private final Action faceRecto;
    private final Action faceVerso;

    private boolean faceRectoVisible;
    private boolean joue;

    /** Constructeur
     * Assigne une action à chaque face du jeton
     * Définit le jeton comme non joué
     **/
    private JetonAction(Action faceRecto, Action faceVerso){
        this.faceRecto = faceRecto;
        this.faceVerso = faceVerso;
        joue = false;
    }

    /** Construit l'objet JetonAction **/
    public static JetonAction creerJetonAction(Action faceRecto, Action faceVerso){
        return new JetonAction(faceRecto, faceVerso);
    }

    /** Lance le jeton (determine la face visible) **/
    public void lancer(){
        this.faceRectoVisible = Math.random() < 0.5;
        this.setJoue(false);
    }

    /** Retourne le jeton **/
    public void retourner() {
        this.faceRectoVisible = !faceRectoVisible;
        this.setJoue(false);
    }

    /** Renvoie si le jeton a été joué **/
    public boolean isJoue(){
        return joue;
    }

    /** Définit la face visible du jeton action */
    public void setFaceRectoVisible(boolean faceRectoVisible) {
        this.faceRectoVisible = faceRectoVisible;
    }

    /** Récupère la face visible du jeton action */
    public boolean isFaceRectoVisible() {
        return faceRectoVisible;
    }

    /** Renvoie l'action de la face visible du jeton **/
    public Action getActionVisible(){
        return faceRectoVisible ? faceRecto : faceVerso;
    }


    /** Définit le jeton comme joué ou non joué **/
    public void setJoue(boolean value){
        this.joue = value;
    }

    // Constructeur de copie pour l'IA
    public JetonAction(JetonAction j) {
        this.faceRecto = j.faceRecto;
        this.faceVerso = j.faceVerso;
        this.faceRectoVisible = j.faceRectoVisible;
        this.joue = j.joue;
    }
}