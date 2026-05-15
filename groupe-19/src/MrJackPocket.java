package src;

import src.modele.Joueur;
import src.modele.Partie;
import src.vue.menus.VueMenuPrincipal;
import src.vue.VueJeu;

import javax.swing.*;
import java.awt.*;

public class MrJackPocket {
    public static boolean menu = false;
    public static boolean simulePartieIa = false;
    public static void main(String[] args) {
        // Simulation de parties entre IA
        if(simulePartieIa){
            Partie partie ;
            int nbVictoiresJack = 0;
            int nbVictoiresEnqueteur = 0;
            for(int i = 0; i < 1000; i++){
                partie = new Partie(2, 2);
                System.out.println("\nDébut de la partie " + (i+1));
                while (!partie.isPartieTerminee()){
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        System.err.println("Erreur lors de la pause dans Main simulePartieIa : ");
                    }
                } // tant que la partie n'est pas terminée, on continue
                if (partie.gagnant == Joueur.JACK) {
                    System.out.println("Mr Jack gagne la partie " + (i+1));
                    nbVictoiresJack++;
                } else {
                    System.out.println("L'Enquêteur gagne la partie " + (i+1));
                    nbVictoiresEnqueteur++;
                }
            }
            System.out.println("Victoires de Mr Jack : " + nbVictoiresJack);
            System.out.println("Victoires de l'Enquêteur : " + nbVictoiresEnqueteur);
        }
        // Lancement du menu principal classique
        else {
            SwingUtilities.invokeLater(() -> {
                if (menu){
                    SwingUtilities.invokeLater(() -> {
                        JFrame frame = new JFrame("Mr. Jack Pocket");
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        frame.setSize(1080, 720);
                        frame.setMinimumSize(new Dimension(1080, 720));
                        frame.setLocationRelativeTo(null);
                        VueMenuPrincipal menu = new VueMenuPrincipal(frame);
                        frame.setContentPane(menu);
                        frame.setVisible(true);
                    });
                } else {
                    Partie partie = new Partie(-1, -1);//lance par defaut une partie humain contre humain
                    VueJeu jeu = new VueJeu(partie);
                    jeu.setVisible(true);
                }
            });
        }
    }
}