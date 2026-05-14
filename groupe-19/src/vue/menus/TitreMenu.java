package src.vue.menus;

import src.utils.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Gère l'affichage du titre du jeu
 **/

public class TitreMenu extends JPanel {

    private final BufferedImage ImageNormale;
    private final BufferedImage ImageSurvolee;
    private boolean estSurvolee = false;
    // Pour que le survol s'active uniquement lorsque la souris est sur l'image,
    // et non pas dans la surface du JPanel qui contient l'image
    private final Rectangle imagePosition = new Rectangle();

    public TitreMenu(BufferedImage ImageNormale, BufferedImage ImageSurvolee) {
        this.ImageNormale = ImageNormale;
        this.ImageSurvolee = ImageSurvolee;
        setOpaque(false);

        // Listener qui traque si la souris entre le JPanel
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // La souris survole t-elle aussi l'image ?
                boolean survolee = imagePosition.contains(e.getPoint());
                if (survolee) {
                    estSurvolee = true;
                    repaint();
                } else {
                    estSurvolee = false;
                    repaint();
                }
            }
        });
        // Listener qui traque si la souris quitte le JPanel
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                estSurvolee = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage image = estSurvolee ? ImageSurvolee : ImageNormale;
        if (image == null) return;

        double ratio = (double) image.getWidth() / image.getHeight();
        Dimension d = utils.fitToRatio(getWidth(), getHeight(), ratio);
        // Calculs des coordonnées
        int x = (getWidth() - d.width) / 2;
        int y = 10 + (getHeight() - d.height) / 2;
        // sauvegarde dans imagePosition le rectangle occupé par l'image dans le JPanel
        imagePosition.setBounds(x, y, d.width, d.height);
        g.drawImage(image, x, y, d.width, d.height, this);
    }
}