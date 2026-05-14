package src.vue.menus;

import src.modele.Joueur;
import src.modele.Partie;
import src.utils.utils;
import src.vue.VueJeu;

import javax.swing.*;
import java.awt.*;

public class VueMenuPrePartie extends JPanel {

    private Joueur joueurChoisi = null;
    private boolean IAChoisi = false;
    private int difficulteIAChoisi = -1;
    private String message;

    public VueMenuPrePartie(JFrame parent, JPanel retourVers, String mode) {

        setBackground(Color.GRAY);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JButton retour = new BoutonsMenu(
                utils.loadImage("fast-forward"),
                null
        );

        retour.setAlignmentX(Component.LEFT_ALIGNMENT);

        retour.addActionListener(e -> {
            parent.setContentPane(retourVers);
            parent.revalidate();
            parent.repaint();
        });

        JPanel panneauRetour = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panneauRetour.setOpaque(false);

        panneauRetour.add(retour);

        add(panneauRetour);

        add(Box.createVerticalGlue());

        JLabel label = new JLabel("Entrer votre pseudo :");
        label.setFont(new Font("Arial", Font.PLAIN, 20));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(label);

        JTextField pseudoField = new JTextField(15);
        pseudoField.setFont(new Font("Arial", Font.PLAIN, 20));
        pseudoField.setAlignmentX(Component.CENTER_ALIGNMENT);
        pseudoField.setMaximumSize(new Dimension(200, pseudoField.getPreferredSize().height));
        add(pseudoField);

        JLabel choixUnLabel = new JLabel("Qui voulez-vous incarnez ?");
        choixUnLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        choixUnLabel.setForeground(Color.WHITE);
        choixUnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(choixUnLabel);

        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.X_AXIS));
        radioPanel.setBackground(Color.GRAY);

        JRadioButton mrJack = new JRadioButton("Mr. Jack");
        JRadioButton lEnqueteur = new JRadioButton("L'Enquêteur");

        ButtonGroup group = new ButtonGroup();
        group.add(mrJack);
        group.add(lEnqueteur);

        radioPanel.add(mrJack);
        radioPanel.add(lEnqueteur);

        add(radioPanel);

        if (mode.equals("solo")) {

            JLabel choixIALabel = new JLabel("Jouer contre l'IA ?");
            choixIALabel.setFont(new Font("Arial", Font.PLAIN, 20));
            choixIALabel.setForeground(Color.WHITE);
            choixIALabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            add(choixIALabel);

            JPanel radioPanelIA = new JPanel();
            radioPanelIA.setLayout(new BoxLayout(radioPanelIA, BoxLayout.X_AXIS));
            radioPanelIA.setBackground(Color.GRAY);

            JRadioButton oui = new JRadioButton("Oui");
            JRadioButton non = new JRadioButton("Non");

            non.setSelected(true);

            ButtonGroup groupIA = new ButtonGroup();
            groupIA.add(oui);
            groupIA.add(non);

            radioPanelIA.add(oui);
            radioPanelIA.add(non);

            add(radioPanelIA);

            JLabel choixDifficulteIALabel = new JLabel("Choisir la difficulté de l'IA");
            choixDifficulteIALabel.setFont(new Font("Arial", Font.PLAIN, 20));
            choixDifficulteIALabel.setForeground(Color.WHITE);
            choixDifficulteIALabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            choixDifficulteIALabel.setVisible(false);

            JPanel radioPanelDifficulteIA = new JPanel();
            radioPanelDifficulteIA.setLayout(new BoxLayout(radioPanelDifficulteIA, BoxLayout.X_AXIS));
            radioPanelDifficulteIA.setBackground(Color.GRAY);

            JRadioButton facile = new JRadioButton("Facile");
            facile.addActionListener(e -> difficulteIAChoisi = 0);

            JRadioButton intermediaire = new JRadioButton("Intermédiaire");
            intermediaire.addActionListener(e -> difficulteIAChoisi = 1);

            JRadioButton difficile = new JRadioButton("Difficile");
            difficile.addActionListener(e -> difficulteIAChoisi = 2);

            ButtonGroup groupDifficulteIA = new ButtonGroup();
            groupDifficulteIA.add(facile);
            groupDifficulteIA.add(intermediaire);
            groupDifficulteIA.add(difficile);

            radioPanelDifficulteIA.add(facile);
            radioPanelDifficulteIA.add(intermediaire);
            radioPanelDifficulteIA.add(difficile);

            radioPanelDifficulteIA.setVisible(false);

            add(choixDifficulteIALabel);
            add(radioPanelDifficulteIA);

            oui.addActionListener(e -> {
                choixDifficulteIALabel.setVisible(true);
                radioPanelDifficulteIA.setVisible(true);
                IAChoisi = true;
            });

            non.addActionListener(e -> {
                choixDifficulteIALabel.setVisible(false);
                radioPanelDifficulteIA.setVisible(false);
                IAChoisi = false;
            });
        }

        JButton lancerBouton = new JButton();

        lancerBouton.setAlignmentX(Component.CENTER_ALIGNMENT);
        lancerBouton.setFont(new Font("Arial", Font.BOLD, 18));
        lancerBouton.setEnabled(false);
        lancerBouton.setVisible(false);

        lancerBouton.addActionListener(e -> {

            Partie partie;

            if (!IAChoisi) {
                partie = new Partie(-1, -1);
            }
            else if (joueurChoisi == Joueur.JACK) {
                partie = new Partie(-1, difficulteIAChoisi);
            }
            else if (joueurChoisi == Joueur.ENQUETEUR) {
                partie = new Partie(difficulteIAChoisi, -1);
            }
            else {
                partie = new Partie(-1, -1);
            }

            VueJeu jeu = new VueJeu(partie);
            jeu.setVisible(true);

            SwingUtilities.getWindowAncestor(this).dispose();
        });

        add(Box.createVerticalStrut(20));
        add(lancerBouton);

        mrJack.addActionListener(e -> {
            joueurChoisi = Joueur.JACK;
            if (mode.equals("multi")) {
                message = "Lancer un serveur en tant que Jack.";
            } else {
                message = "Vite ! Je dois m'échapper !";
            }
            lancerBouton.setText(message);
            lancerBouton.setVisible(true);
            lancerBouton.setEnabled(true);
        });

        lEnqueteur.addActionListener(e -> {
            joueurChoisi = Joueur.ENQUETEUR;
            // String pseudo = pseudoField.getText(); pour avoir le pseudo entré par l'utilisateur
            if (mode.equals("multi")){
                message = "Lancer un serveur en tant que l'Enquêteur";
                // ça crée un serveur.
            } else {
                message = "Jack, nous te trouverons !";
            }
            lancerBouton.setText(message);
            lancerBouton.setVisible(true);
            lancerBouton.setEnabled(true);
        });

        add(Box.createVerticalGlue());
    }
}