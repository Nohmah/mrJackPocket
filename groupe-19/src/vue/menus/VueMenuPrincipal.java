package src.vue.menus;

import src.utils.utils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class VueMenuPrincipal extends JPanel {
    JPanel panneauDroitBoutons;
    /** Constructeur **/
    public VueMenuPrincipal(JFrame parent) {
        setLayout(new BorderLayout());
        Background background = new Background();
        add(background, BorderLayout.CENTER);

        // Création d'un Panel de 1 ligne et 2 colonnes
        // La colonne de gauche doit être vide (pour voir la tête des personnages)
        // et la colonne de droite contient les boutons
        JPanel panelGlobal = new JPanel(new GridLayout(1, 2));
        panelGlobal.setOpaque(false);
        background.add(panelGlobal);
        background.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panelGlobal.setBounds(0, 0,
                        background.getWidth(),
                        background.getHeight());
                panelGlobal.revalidate();
                panelGlobal.repaint();
            }
        });
        // Sans ce panel (vide), les 2 colonnes ne seraient pas 50/50 en terme d'occupation de l'espace
        JPanel panelGaucheVide = new JPanel();
        panelGaucheVide.setOpaque(false);
        panelGlobal.add(panelGaucheVide);

        panneauDroitBoutons = new JPanel();
        panneauDroitBoutons.setLayout(new BoxLayout(panneauDroitBoutons, BoxLayout.Y_AXIS));
        panneauDroitBoutons.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        // 10 pour que les boutons ne collent pas au bord de la fenêtre à droite
        panneauDroitBoutons.setOpaque(false);
        panelGlobal.add(panneauDroitBoutons);

        TitreMenu titrePanel = new TitreMenu(
                utils.loadImage("MenuTitre"),
                utils.loadImage("MenuTitreSurvole"));
        titrePanel.setPreferredSize(new Dimension(560, 250));

        JButton partieSolo = BoutonsMenu.creerBouton(
                "Boutonpartiesolo", () -> {
            VueMenuPrePartie prePartieSolo = new VueMenuPrePartie(parent, this, "solo");
            parent.setContentPane(prePartieSolo);
            parent.revalidate();
            parent.repaint();
        });

        JButton partieMulti = BoutonsMenu.creerBouton(
                "Boutonpartiemultijoueur", () -> {
            VueMenuPrePartie prePartieMulti = new VueMenuPrePartie(parent, this, "multi");
            parent.setContentPane(prePartieMulti);
            parent.revalidate();
            parent.repaint();
        });

        JPanel boutonsPartiePanel = new JPanel(new GridLayout(1, 2, 50, 50));
        boutonsPartiePanel.setOpaque(false);
        boutonsPartiePanel.add(partieSolo);
        boutonsPartiePanel.add(partieMulti);

        JButton regles = BoutonsMenu.creerBouton("Boutonreglesdujeu",() -> {
            VueRegles reglesPanel = new VueRegles(parent, this);
            parent.setContentPane(reglesPanel);
            parent.revalidate();
            parent.repaint();
        });

        JButton options = BoutonsMenu.creerBouton("Boutonoptions", () -> {
            VueOptions optionsPanel = new VueOptions(parent, this);
            parent.setContentPane(optionsPanel);
            parent.revalidate();
            parent.repaint();
        });

        JPanel boutonsSupPanel = new JPanel(new GridLayout(1, 2, 50, 50));
        boutonsSupPanel.setOpaque(false);
        boutonsSupPanel.add(regles);
        boutonsSupPanel.add(options);

        JButton quitter = BoutonsMenu.creerBouton("Boutonquitter",
                () -> System.exit(0));

        panneauDroitBoutons.add(titrePanel);
        panneauDroitBoutons.add(Box.createVerticalStrut(50));
        panneauDroitBoutons.add(boutonsPartiePanel);
        panneauDroitBoutons.add(Box.createVerticalStrut(50));
        panneauDroitBoutons.add(boutonsSupPanel);
        panneauDroitBoutons.add(Box.createVerticalStrut(50));
        panneauDroitBoutons.add(quitter);
        panneauDroitBoutons.add(Box.createVerticalGlue());
    }

    public void resetBoutonsSurvoles() {
        resetBoutonsSurvolesRecursif(panneauDroitBoutons);
    }

    private void resetBoutonsSurvolesRecursif(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof BoutonsMenu) {
                ((BoutonsMenu) comp).resetSurvole();
            } else if (comp instanceof Container) {
                resetBoutonsSurvolesRecursif((Container) comp);
            }
        }
    }
}