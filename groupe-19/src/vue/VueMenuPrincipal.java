package src.vue;

import src.modele.Partie;
import src.utils.utils;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class VueMenuPrincipal extends JFrame {
    public VueMenuPrincipal() {
        super("Mr. Jack Pocket");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.GRAY);

        setLayout(new BorderLayout());

        BufferedImage imageIllustration = utils.loadImage("MrJackPocket");
        if (imageIllustration !=null){
            ImageIcon icon = new ImageIcon(imageIllustration);
            JLabel imageLabel = new JLabel(icon);
            add(imageLabel, BorderLayout.WEST);
        }

        JPanel panneauBoutons = new JPanel();
        panneauBoutons.setLayout(new BoxLayout(panneauBoutons, BoxLayout.Y_AXIS));
        panneauBoutons.setBackground(Color.GRAY);
        add(panneauBoutons);

        JButton partieSolo = new JButton("Partie Solo");
        partieSolo.addActionListener(e -> {
            VueMenuPrePartie prePartie = new VueMenuPrePartie("solo");
            getContentPane().removeAll();
            getContentPane().add(prePartie);
            revalidate();
            repaint();
            //Partie partie = new Partie();
            //VueJeu jeu = new VueJeu(partie);
            //dispose();
        });
        partieSolo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton partieMulti = new JButton("Partie Multijoueur");
        partieMulti.addActionListener(e -> {
            System.out.println("Partie Multijoueur");
        });
        partieMulti.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton regles = new JButton("Règles");
        regles.addActionListener(e->{
            System.out.println("Les règles");
        });
        regles.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton quitter = new JButton("Quitter");
        quitter.addActionListener(e -> {
            System.exit(0);
        });
        quitter.setAlignmentX(Component.CENTER_ALIGNMENT);

        panneauBoutons.add(Box.createVerticalGlue());
        panneauBoutons.add(partieSolo);
        panneauBoutons.add(partieMulti);
        panneauBoutons.add(regles);
        panneauBoutons.add(quitter);
        panneauBoutons.add(Box.createVerticalGlue());
    }
}
