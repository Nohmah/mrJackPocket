package src.modele;
/**
 *
 * Jeton Action : Deux faces, deux actions.
 * Choix aléatoire de la face lors des tours impairs (detective)
 * Au tour pair, on retourne le jeton, sa 2ème face devient visible.
 *
 */
public class JetonAction {
    private final Action faceA;
    private final Action faceB;
    private boolean faceAVisible; //true = le face A visible, false = face B visible.
    private boolean jouee; // le jeton à déjà été jouée a ce tour ?

    private JetonAction(Action faceA, Action faceB){
        this.faceA = faceA;
        this.faceB = faceB;
        faceAVisible = true;
        jouee = false;
    }

    public static JetonAction creerJeton(Action faceA, Action faceB){
        return new JetonAction(faceA, faceB);
    }

    /** Lance le jeton - determine la face visible. **/
    public void lancer(){
        faceAVisible = Math.random() < 0.5;
        jouee = false;
    }

    /** Retourne le jeton - uniquement pour le tour pair (Jack) **/
    public void retourner() {
        faceAVisible = !faceAVisible;
        jouee = false;
    }

    /** Retourne l'action du jeton sur sa face visible **/
    public Action getActionVisible(){
        return faceAVisible ? faceA : faceB;
    }

    /** Retourne l'action cachée **/
    public Action getActionCache(){
        return faceAVisible ? faceB : faceA;
    }

    public boolean isJouee(){
        return jouee;
    }

    public void setJouee(boolean jouee){
        this.jouee = jouee;
    }
}
