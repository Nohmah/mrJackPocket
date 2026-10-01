package src.vue.menus;

import src.utils.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Gère les boutons du menu
 **/

public class BoutonsMenu extends JButton {

    private final BufferedImage ImageNormale; // Image du bouton par défaut
    private final BufferedImage ImageSurvolee; // Image quand survolée par la souris
    private boolean estSurvolee = false; // True si la souris est sur le bouton

    public BoutonsMenu(BufferedImage normal, BufferedImage survolee) {
        this(normal, survolee, new Dimension(273, 113));
        // taille optimale pour le visuel et le redimensionnement
    }

    public BoutonsMenu(BufferedImage normal, BufferedImage survolee, Dimension size) {
        this.ImageNormale = normal;
        this.ImageSurvolee = survolee;
        setBorderPainted(false);
        setOpaque(false);
        setPreferredSize(size);
        setMaximumSize(size);
        // Pour centrer les boutons
        setAlignmentX(Component.CENTER_ALIGNMENT);
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                estSurvolee = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                estSurvolee = false;
                repaint();
            }
        });
    }

    public static JButton creerBouton(String baseName, Runnable action, Dimension size) {
        JButton bouton = new BoutonsMenu(
                utils.loadImage(baseName),
                utils.loadImage(baseName + "survole"),
                size
        );
        if (action != null) {
            bouton.addActionListener(e -> action.run());
        }
        return bouton;
    }

    public static JButton creerBouton(String baseName, Runnable action) {
        JButton bouton = new BoutonsMenu(
                utils.loadImage(baseName),
                utils.loadImage(baseName + "survole")
        );
        if (action != null) {
            bouton.addActionListener(e -> action.run());
        }
        return bouton;
    }

    /** Méthode pour set les boutons comme non survolés **/
    public void resetSurvole() {
        estSurvolee = false;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        BufferedImage img = estSurvolee && ImageSurvolee != null ? ImageSurvolee : ImageNormale;
        if (img == null) return;
        Graphics2D g2 = (Graphics2D) g.create();

        double ratio = (double) img.getWidth() / img.getHeight();

        Dimension d = utils.fitToRatio(getWidth(), getHeight(), ratio);

        double modificateurDeTaille = getModel().isPressed() ? 0.95 : 1.0;

        int w = (int)(d.width * modificateurDeTaille);
        int h = (int)(d.height * modificateurDeTaille);

        // Pour que l'effet du clic soit centré
        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;

        g2.drawImage(img, x, y, w, h, this);
        g2.dispose();
    }
}