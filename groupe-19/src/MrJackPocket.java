package src;
import src.modele.Partie;
import src.vue.VueJeu;
import javax.swing.SwingUtilities;

/**
 * Fichier pour lancer Mr. Jack Pocket.
 */
public class MrJackPocket {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Partie partie = new Partie();
            VueJeu vue = new VueJeu(partie);
            vue.setVisible(true);
        });
    }
}