package src;

import src.modele.Joueur;
import src.modele.Partie;
import src.vue.VueMenuPrincipal;
import src.vue.VueJeu;
import javax.swing.SwingUtilities;

public class MrJackPocket {
    public static boolean menu = true;
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (menu){
                VueMenuPrincipal menu = new VueMenuPrincipal();
                menu.setVisible(true);
            } else {
                Partie partie = new Partie(Joueur.ENQUETEUR, false, null);
                VueJeu jeu = new VueJeu(partie);
                jeu.setVisible(true);
            }
        });
    }
}