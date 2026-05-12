package src.vue.menus;

import src.utils.utils;
import src.vue.regles.VueReglesPanel;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class VueRegles extends JPanel {

    private final BufferedImage fond;
    private final BufferedImage imageDroite;

    public VueRegles(JFrame parent, JPanel retourVers, BufferedImage fond) {
        this.fond = fond;

        this.imageDroite = utils.loadImage("Regles");

        setLayout(new BorderLayout());

        JLabel label = new JLabel(VueReglesPanel.getHtml());
        label.setVerticalAlignment(SwingConstants.TOP);

        JScrollPane scroll = new JScrollPane(label);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        JPanel gauche = new JPanel(new BorderLayout());
        gauche.setOpaque(false);
        gauche.add(scroll, BorderLayout.CENTER);

        JLabel imageLabel = new JLabel();

        if (imageDroite != null) {
            Image scaled = imageDroite.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaled));
        }

        JPanel droite = new JPanel(new BorderLayout());
        droite.setOpaque(false);
        droite.add(imageLabel, BorderLayout.CENTER);
        droite.setPreferredSize(new Dimension(400, 0));

        add(gauche, BorderLayout.CENTER);
        add(droite, BorderLayout.EAST);

        JButton retour = new JButton("Retour");
        retour.addActionListener(e -> {
            parent.setContentPane(retourVers);
            parent.revalidate();
            parent.repaint();
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(retour);

        add(top, BorderLayout.NORTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (fond != null) {
            g.drawImage(fond, 0, 0, getWidth(), getHeight(), this);
        }
    }
}