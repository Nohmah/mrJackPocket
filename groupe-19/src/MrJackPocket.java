package src;

import src.modele.Partie;
import src.vue.VueMenuPrincipal;
import src.vue.VueJeu;
import javax.swing.SwingUtilities;

public class MrJackPocket {
    public static boolean menu = false;
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (menu){
                VueMenuPrincipal menu = new VueMenuPrincipal();
                menu.setVisible(true);
            } else {
                Partie partie = new Partie();
                VueJeu jeu = new VueJeu(partie);
                jeu.setVisible(true);
            }
        });
    }
}