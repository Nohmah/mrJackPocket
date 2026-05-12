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

    private final BufferedImage ImageNormale;
    private final BufferedImage ImageSurvolee;
    private boolean estSurvolee = false;

    public BoutonsMenu(BufferedImage normal, BufferedImage hover) {

        this.ImageNormale = normal;
        this.ImageSurvolee = hover;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);

        setPreferredSize(new Dimension(273, 113));
        setMaximumSize(new Dimension(273, 113));

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

        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;

        g2.drawImage(img, x, y, w, h, this);
        g2.dispose();
    }
}