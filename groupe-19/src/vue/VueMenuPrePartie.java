package src.vue;

import src.modele.Joueur;
import src.modele.Partie;

import javax.swing.*;
import java.awt.*;

public class VueMenuPrePartie extends JPanel {
    private Joueur joueurChoisi = null;
    private boolean IAChoisi = false;
    private String difficulteIAChoisi = null;
    private String message;

    public VueMenuPrePartie(String mode) {
        setBackground(Color.GRAY);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(Box.createVerticalGlue());

//        JLabel label = new JLabel("Entrer votre pseudo :");
//        label.setFont(new Font("Arial", Font.PLAIN, 20));
//        label.setForeground(Color.WHITE);
//        label.setAlignmentX(Component.CENTER_ALIGNMENT);
//        add(label);
//
//        JTextField pseudoField = new JTextField(15);
//        pseudoField.setFont(new Font("Arial", Font.PLAIN, 20));
//        pseudoField.setAlignmentX(Component.CENTER_ALIGNMENT);
//        //Pour limiter la taille de cette barre de saisie du pseudo
//        pseudoField.setMaximumSize(new Dimension(200, pseudoField.getPreferredSize().height));
//        add(pseudoField);

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
        facile.addActionListener(e->{difficulteIAChoisi = "facile";});
        JRadioButton intermediaire = new JRadioButton("Intermédiaire");
        intermediaire.addActionListener(e->{difficulteIAChoisi = "intermedaire";});
        JRadioButton difficile = new JRadioButton("Difficile");
        facile.addActionListener(e->{difficulteIAChoisi = "difficile";});
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

        oui.addActionListener(e->{
            choixDifficulteIALabel.setVisible(true);
            radioPanelDifficulteIA.setVisible(true);
            IAChoisi = true;
        });
        non.addActionListener(e->{
            choixDifficulteIALabel.setVisible(false);
            radioPanelDifficulteIA.setVisible(false);
            IAChoisi = false;
        });

        JButton lancerBouton = new JButton();
        lancerBouton.setAlignmentX(Component.CENTER_ALIGNMENT);
        lancerBouton.setFont(new Font("Arial", Font.BOLD, 18));
        lancerBouton.setEnabled(false);
        lancerBouton.setVisible(false);
        lancerBouton.addActionListener(e->{
            Partie partie = new Partie(joueurChoisi, IAChoisi, difficulteIAChoisi);
            VueJeu jeu = new VueJeu(partie);
            jeu.setVisible(true);
            SwingUtilities.getWindowAncestor(this).dispose();
            // On ferme la fenêtre
            // Actuellement VueJeu crée une nouvelle fenêtre
            // Il faudra à terme faire que la partie s'affiche dans la même fenêtre.
        });
        add(Box.createVerticalStrut(20));
        add(lancerBouton);

        mrJack.addActionListener(e->{
            joueurChoisi = Joueur.JACK;
            message = "Vite ! Je dois m'échapper !";
            lancerBouton.setText(message);
            lancerBouton.setVisible(true);
            lancerBouton.setEnabled(true);
        });

        lEnqueteur.addActionListener(e->{
            joueurChoisi = Joueur.ENQUETEUR;
            message = "Jack, nous te trouverons !";
            lancerBouton.setText(message);
            lancerBouton.setVisible(true);
            lancerBouton.setEnabled(true);
        });

        add(Box.createVerticalGlue());


    }
}