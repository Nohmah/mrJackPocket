package src.vue.menus;

import src.utils.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class VueOptions extends JPanel {

    private final BufferedImage fond;

    public VueOptions(JFrame parent, JPanel menu, BufferedImage fond) {
        this.fond = fond;

        setLayout(null);

        JButton retour = new BoutonsMenu(
                utils.loadImage("fast-forward"),
                null
        );
        retour.setBounds(0, 0, 120, 100);

        retour.addActionListener(e -> {
            parent.setContentPane(menu);
            parent.revalidate();
            parent.repaint();
        });

        add(retour);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(fond, 0, 0, getWidth(), getHeight(), this);
    }
}