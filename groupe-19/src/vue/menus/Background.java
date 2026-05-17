package src.vue.menus;

import src.utils.utils;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Background extends JPanel {
    private final BufferedImage backgroundImage;
    String nomImage = "Backgroundmenu";

    /** Constructeur **/
    public Background() {
        backgroundImage = utils.loadImage(nomImage);
        setLayout(new BorderLayout());
    }

    /** Revoie le fond en BufferedImage **/
    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            int imageWidth = backgroundImage.getWidth();
            int imageHeight = backgroundImage.getHeight();
            // getWidth() / imageWidth est le facteur à appliquer à la largeur de l'image
            // pour qu'elle vaille getWidth()
            // Même principe pour getHeight() / imageHeight
            // Comme on veut remplir toute la fenêtre avec l'image, on prend le facteur le plus grand
            double facteur = Math.max(
                    (double) getWidth() / imageWidth,
                    (double) getHeight() / imageHeight
            );
            int newWidth = (int) (imageWidth * facteur);
            int newHeight = (int) (imageHeight * facteur);
            // Calcul des coordonnées de l'image pour qu'elle soit centrée
            int x = (getWidth() - newWidth) / 2;
            int y = (getHeight() - newHeight) / 2;
            g.drawImage(backgroundImage, x, y, newWidth, newHeight, this);
        }
    }
}