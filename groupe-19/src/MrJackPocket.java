package src;
import src.vue.VueJeu;
import javax.swing.SwingUtilities;

/**
 * Fichier pour lancer Mr. Jack Pocket.
 */
public class MrJackPocket {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VueJeu vue = new VueJeu();
            vue.setVisible(true);
        });
    }
}