package src.vue.menus;

import javax.swing.*;
import java.awt.*;

public class VueOptions extends JPanel {

    public VueOptions(JFrame parent, JPanel menu) {
        setLayout(new BorderLayout());
        Background background = new Background();
        add(background, BorderLayout.CENTER);

        JPanel panelGlobal = new JPanel(new BorderLayout());
        panelGlobal.setOpaque(false);

        // Panel pour y mettre le bouton retour
        JPanel coinGauche = new JPanel(new FlowLayout(FlowLayout.LEFT));
        coinGauche.setOpaque(false);
        // bouton retour
        JButton retour = BoutonsMenu.creerBouton("Boutonretour",
                () -> {
                    parent.setContentPane(menu);
                    parent.revalidate();
                    parent.repaint();
                },
                new Dimension(160, 66)
        );
        coinGauche.add(retour);
        panelGlobal.add(coinGauche, BorderLayout.NORTH);

        // Panel pour les options
        JPanel centre = new JPanel();
        centre.setOpaque(false);
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));

        // Pour descendre le panel
        centre.add(Box.createVerticalStrut(60));

        JPanel fondTexte = new JPanel();
        fondTexte.setBackground(new Color(0, 0, 0, 160));
        fondTexte.setOpaque(true);
        fondTexte.setLayout(new BorderLayout());
        fondTexte.setAlignmentX(Component.LEFT_ALIGNMENT);
        fondTexte.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        fondTexte.setMaximumSize(new Dimension(700, 1000));

        JLabel texte = new JLabel(
                "<html><div style='font-size:20px;'>"
                        + "Vous pouvez entrer votre pseudo ici puis cliquer sur 'Valider' et il sera utilisé "
                        + "par défaut pour toutes vos prochaines parties"
                        + "</div></html>"
        );
        texte.setForeground(Color.WHITE);
        fondTexte.add(texte);

        // Pour rentrer son pseudo
        JTextField pseudoField = new JTextField();
        try (java.util.Scanner sc = new java.util.Scanner(new java.io.File("pseudo.txt"))) {
            if (sc.hasNextLine()) {
                String ligne = sc.nextLine().trim();
                String pseudo = ligne.split("\\s")[0]; // On regarde jusqu'au 1er espace
                pseudoField.setText(pseudo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Bouton valider
        JButton valider = BoutonsMenu.creerBouton("Boutonvalider", () -> {
                String pseudo = pseudoField.getText().trim();
                try (java.io.FileWriter fw = new java.io.FileWriter("pseudo.txt", false)) {
                    fw.write(pseudo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },
            new Dimension(145, 60));
        pseudoField.setFont(pseudoField.getFont().deriveFont(22f));

        pseudoField.setForeground(Color.WHITE);
        pseudoField.setBackground(new Color(40, 40, 40));
        pseudoField.setCaretColor(Color.WHITE);
        pseudoField.setBorder(
                BorderFactory.createEmptyBorder(0, 10, 0, 0)
        );

        pseudoField.setPreferredSize(new Dimension(500, 45));
        pseudoField.setMinimumSize(new Dimension(300, 45));

        JPanel combinaison = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        combinaison.setOpaque(false);
        combinaison.setAlignmentX(Component.LEFT_ALIGNMENT);
        combinaison.add(pseudoField);
        combinaison.add(valider);

        centre.add(fondTexte);
        centre.add(Box.createVerticalStrut(10));
        centre.add(combinaison);

        panelGlobal.add(centre, BorderLayout.CENTER);

        background.add(panelGlobal);
    }
}