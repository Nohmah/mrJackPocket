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
            Partie partie = new Partie();
            VueJeu jeu = new VueJeu(partie);
            dispose();
        });
        partieSolo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton partieMulti = new JButton("Partie Multijoueur");
        partieMulti.addActionListener(e -> {
            System.out.println("blabla");
        });
        partieMulti.setAlignmentX(Component.CENTER_ALIGNMENT);
        panneauBoutons.add(Box.createVerticalGlue());
        panneauBoutons.add(partieSolo);
        panneauBoutons.add(partieMulti);
        panneauBoutons.add(Box.createVerticalGlue());
    }
}
