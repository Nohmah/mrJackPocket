package src.vue.menus;

import src.utils.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

public class VueMenuPrincipal extends JPanel {

    private static class BackgroundPanel extends JPanel {
        private final BufferedImage backgroundImage;
        private Rectangle imageBounds = new Rectangle();

        public BackgroundPanel() {
            String nomImage = "MenuFond";
            backgroundImage = utils.loadImage(nomImage);
            setLayout(null);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (backgroundImage != null) {

                int imageWidth = backgroundImage.getWidth();
                int imageHeight = backgroundImage.getHeight();

                double scale = Math.min(
                        (double) getWidth() / imageWidth,
                        (double) getHeight() / imageHeight
                );

                int newWidth = (int) (imageWidth * scale);
                int newHeight = (int) (imageHeight * scale);

                int x = (getWidth() - newWidth) / 2;
                int y = (getHeight() - newHeight) / 2;

                imageBounds.setBounds(x, y, newWidth, newHeight);

                g.drawImage(backgroundImage, x, y, newWidth, newHeight, this);

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
        setOpaque(false);

        add(background, BorderLayout.CENTER);

        JPanel contenu = new JPanel(new GridLayout(1, 2));
        contenu.setOpaque(false);
        background.add(contenu);
        background.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {

                Rectangle r = background.imageBounds;

                contenu.setBounds(r);
                contenu.revalidate();
                contenu.repaint();
            }
        });

        JPanel vide = new JPanel();
        vide.setOpaque(false);
        contenu.add(vide);

        JPanel panneauBoutons = new JPanel();
        panneauBoutons.setLayout(new BoxLayout(panneauBoutons, BoxLayout.Y_AXIS));
        panneauBoutons.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        panneauBoutons.setOpaque(false);
        contenu.add(panneauBoutons);

        BufferedImage titreNormal = utils.loadImage("MenuTitre");
        BufferedImage titreSurvole = utils.loadImage("MenuTitreSurvole");
        TitreMenu titrePanel = new TitreMenu(titreNormal, titreSurvole);
        titrePanel.setPreferredSize(new Dimension(560, 250));
        titrePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton partieSolo = creerBouton("Boutonpartiesolo", () -> {
            VueMenuPrePartie prePartieSolo =
                    new VueMenuPrePartie(parent, this, "solo");
            parent.setContentPane(prePartieSolo);
            parent.revalidate();
            parent.repaint();
        });

        JButton partieMulti = creerBouton("Boutonpartiemultijoueur", () -> {
            VueMenuPrePartie prePartieMulti =
                    new VueMenuPrePartie(parent, this, "multi");
            parent.setContentPane(prePartieMulti);
            parent.revalidate();
            parent.repaint();
        });

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