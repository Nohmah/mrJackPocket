package src.vue.menus;

import src.vue.regles.VueReglesPanel;

import javax.swing.*;
import java.awt.*;

public class VueRegles extends JPanel {

    public VueRegles(JFrame parent, JPanel retourVers) {

        setLayout(new BorderLayout());

        Background background = new Background();
        add(background, BorderLayout.CENTER);

        JPanel panelGlobal = new JPanel(new BorderLayout());
        panelGlobal.setOpaque(false);

        JLabel label = new JLabel(VueReglesPanel.getHtml());
        label.setOpaque(false);
        label.setVerticalAlignment(SwingConstants.TOP);
        label.setHorizontalAlignment(SwingConstants.LEFT);

        JScrollPane scroll = new JScrollPane(label);
        scroll.setOpaque(false);
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        scroll.getViewport().setOpaque(false);

        JPanel centre = new JPanel(new BorderLayout());
        centre.setOpaque(false);
        centre.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel fondNoir = new JPanel(new BorderLayout());
        fondNoir.setBackground(new Color(0, 0, 0, 200));
        fondNoir.setOpaque(true);

        fondNoir.add(scroll, BorderLayout.CENTER);

        centre.add(fondNoir, BorderLayout.CENTER);

        panelGlobal.add(centre, BorderLayout.CENTER);

        JButton retour = BoutonsMenu.creerBouton(
                "Boutonretour",
                () -> {
                    parent.setContentPane(retourVers);
                    parent.revalidate();
                    parent.repaint();
                },
                new Dimension(160, 66)
        );

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(retour);

        panelGlobal.add(top, BorderLayout.NORTH);

        background.add(panelGlobal);
    }
}