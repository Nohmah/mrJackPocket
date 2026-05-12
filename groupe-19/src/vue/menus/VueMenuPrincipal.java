package src.vue.menus;

import src.utils.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class VueMenuPrincipal extends JPanel {

    private static class BackgroundPanel extends JPanel {
        public int nb = (int)(Math.random() * 9) + 1;
        private final BufferedImage backgroundImage;

        public BackgroundPanel() {
            String nomImage = "MenuBackground" + nb;
            backgroundImage = utils.loadImage(nomImage);
            setLayout(new BorderLayout());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(Color.GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    private JButton creerBouton(String baseName, Runnable action) {

        JButton bouton = new BoutonsMenu(
                utils.loadImage(baseName),
                utils.loadImage(baseName + "Survole")
        );

        if (action != null) {
            bouton.addActionListener(e -> action.run());
        }

        return bouton;
    }

    /** Constructeur **/
    public VueMenuPrincipal(JFrame parent) {

        setLayout(new BorderLayout());

        BackgroundPanel background = new BackgroundPanel();
        setLayout(new BorderLayout());
        setOpaque(false);

        add(background, BorderLayout.CENTER);

        JPanel contenu = new JPanel(new GridLayout(1, 2));
        contenu.setOpaque(false);
        background.add(contenu, BorderLayout.CENTER);

        BufferedImage imageIllustration = utils.loadImage("MenuIllustration");

        if (imageIllustration != null) {
            IllustrationMenu illustrationMenu = new IllustrationMenu(imageIllustration);
            illustrationMenu.setOpaque(false);
            contenu.add(illustrationMenu);
        }

        JPanel panneauBoutons = new JPanel();
        panneauBoutons.setLayout(new BoxLayout(panneauBoutons, BoxLayout.Y_AXIS));
        panneauBoutons.setOpaque(false);
        contenu.add(panneauBoutons);

        BufferedImage titreNormal = utils.loadImage("MenuTitre");
        BufferedImage titreSurvole = utils.loadImage("MenuTitreSurvole");
        TitreMenu titrePanel = new TitreMenu(titreNormal, titreSurvole);
        titrePanel.setPreferredSize(new Dimension(560, 250));
        titrePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton partieSolo = creerBouton("Boutonpartiesolo", () -> {
            VueMenuPrePartie prePartie = new VueMenuPrePartie("solo");
            parent.setContentPane(prePartie);
            parent.revalidate();
            parent.repaint();
        });

        JButton partieMulti = creerBouton("Boutonpartiemultijoueur",
                () -> System.out.println("Partie Multijoueur"));

        JPanel panneauHaut = new JPanel(new GridLayout(1, 2, 50, 50));
        panneauHaut.setOpaque(false);
        panneauHaut.add(partieSolo);
        panneauHaut.add(partieMulti);

        JButton regles = creerBouton("Boutonreglesdujeu",() -> {
            VueRegles reglesPanel = new VueRegles(parent, this, background.backgroundImage);
            parent.setContentPane(reglesPanel);
            parent.revalidate();
            parent.repaint();
        });

        JButton options = creerBouton("Boutonoptions", () -> {
            VueOptions optionsPanel = new VueOptions(parent, this, background.backgroundImage);
            parent.setContentPane(optionsPanel);
            parent.revalidate();
            parent.repaint();
        });

        JPanel panneauMilieu = new JPanel(new GridLayout(1, 2, 50, 50));
        panneauMilieu.setOpaque(false);
        panneauMilieu.add(regles);
        panneauMilieu.add(options);

        JButton quitter = creerBouton("Boutonquitter",
                () -> System.exit(0));

        panneauBoutons.add(titrePanel);
        panneauBoutons.add(Box.createVerticalStrut(50));
        panneauBoutons.add(panneauHaut);
        panneauBoutons.add(Box.createVerticalStrut(50));
        panneauBoutons.add(panneauMilieu);
        panneauBoutons.add(Box.createVerticalStrut(50));
        panneauBoutons.add(quitter);
        panneauBoutons.add(Box.createVerticalGlue());
    }
}