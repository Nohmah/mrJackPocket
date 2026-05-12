package src.vue.menus;

import src.utils.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Gère l'affichage de l'illustration du menu
 **/

public class IllustrationMenu extends JPanel {
    private final BufferedImage image;
    private final double imageRatio;
    private final int marge = 50; // Pour que l'image ne soit pas collé aux bords de la fenêtre

    /** Constructeur **/
    public IllustrationMenu(BufferedImage image) {
        this.image = image;
        this.imageRatio = (double) image.getWidth() / image.getHeight(); // Le ratio de l'image originale
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) return;

        // La taille de l'espace libre pour l'image
        int l = getWidth() - 2 * marge;
        int h = getHeight() - 2 * marge;
        // Calcul des dimensions finales
        Dimension d = utils.fitToRatio(l, h, imageRatio);
        int lf = d.width;
        int hf = d.height;
        // Calcul de la position de l'image
        int x = marge + (l - lf) / 2;
        int y = marge + (h - hf) / 2;
        // Dessin
        g.drawImage(image, x, y, lf, hf, this);
    }
}