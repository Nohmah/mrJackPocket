package src.vue.menus;

import src.modele.Joueur;
import src.modele.Partie;
import src.utils.utils;
import src.vue.VueJeu;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;

public class VueMenuPrePartie extends JPanel {

    private Joueur joueurChoisi = null;
    private boolean IAChoisi = false;
    private int difficulteIAChoisi = -1;
    private String message;
    private String pseudo;
    private String serveur;

    public VueMenuPrePartie(JFrame parent, JPanel retourVers, String mode) {
        setBackground(Color.GRAY);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        switch(mode) {
            case "solo":
                soloMenu(parent, retourVers);
                break;
            case "multi":
                multiMenu(parent, retourVers);
                break;
            default:
                throw new RuntimeException("Appel constructeur 'VueMenuPrePartie' avec paramètre 'String mode' invalide : "+mode+" !");
        }


    }

    private void retourFenetrePrecedent(JFrame parent, JPanel retourVers, JPanel positionnement) {
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

        positionnement.add(panneauRetour);
    }

    private void demandePseudonyme(JPanel position){

        JPanel zoneSaisie = new JPanel();
        zoneSaisie.setLayout(new BoxLayout(zoneSaisie, BoxLayout.Y_AXIS));
        zoneSaisie.setOpaque(false);

        JLabel label = new JLabel("Entrer votre pseudo :");
        label.setFont(new Font("Arial", Font.PLAIN, 20));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField pseudoField = new JTextField(15);
        pseudoField.setFont(new Font("Arial", Font.PLAIN, 20));
        pseudoField.setAlignmentX(Component.CENTER_ALIGNMENT);
        pseudoField.setMaximumSize(new Dimension(200, pseudoField.getPreferredSize().height));

        JButton confirmationPseudo = new JButton("Confirmer votre pseudo");
        confirmationPseudo.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmationPseudo.setFont(new Font("Arial", Font.PLAIN, 18));

        zoneSaisie.add(label);
        zoneSaisie.add(pseudoField);
        zoneSaisie.add(confirmationPseudo);
        zoneSaisie.add(Box.createVerticalGlue());

        ActionListener actionPseudoRecu = e -> {
            this.pseudo = pseudoField.getText();
            System.out.println("[DEBUG] reception pseudo : "+pseudo);
            if(!this.pseudo.isEmpty()){
                zoneSaisie.removeAll();

                JLabel pseudoValide = new JLabel("Votre pseudo : " + pseudo);
                pseudoValide.setFont(new Font("Arial", Font.PLAIN, 22));
                pseudoValide.setForeground(Color.WHITE);

                pseudoValide.setAlignmentX(Component.CENTER_ALIGNMENT);

                zoneSaisie.add(pseudoValide);
                zoneSaisie.add(Box.createVerticalGlue());
                zoneSaisie.revalidate();
                zoneSaisie.repaint();
            }
        };
        confirmationPseudo.addActionListener(actionPseudoRecu);
        pseudoField.addActionListener(actionPseudoRecu);
        position.add(zoneSaisie);




    }

    private void soloMenu(JFrame parent, JPanel retourVers){
        retourFenetrePrecedent(parent,retourVers,this);

        add(Box.createVerticalGlue());

        demandePseudonyme(this);

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

            message = "Vite ! Je dois m'échapper !";

            lancerBouton.setText(message);
            lancerBouton.setVisible(true);
            lancerBouton.setEnabled(true);
        });

        lEnqueteur.addActionListener(e -> {
            joueurChoisi = Joueur.ENQUETEUR;
            // String pseudo = pseudoField.getText(); pour avoir le pseudo entré par l'utilisateur

            message = "Jack, nous te trouverons !";

            lancerBouton.setText(message);
            lancerBouton.setVisible(true);
            lancerBouton.setEnabled(true);
        });


        add(Box.createVerticalGlue());
    }

    private void multiMenu(JFrame parent, JPanel retourVers){

        JPanel contenuPrincipal = new JPanel();
        contenuPrincipal.setLayout(new GridLayout(1, 2));
        contenuPrincipal.setOpaque(false);



        Border contourColonnes = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 10),
                BorderFactory.createEmptyBorder(10,10,10,10)
        );

        JPanel contenueColonneGauche = new JPanel();
        contenueColonneGauche.setOpaque(false);
        contenueColonneGauche.setLayout(new BoxLayout(contenueColonneGauche, BoxLayout.Y_AXIS));

        retourFenetrePrecedent(parent,retourVers,contenueColonneGauche);

        JLabel titreColonneGauche = new JLabel("Créé un serveur !");
        titreColonneGauche.setFont(new Font("Arial", Font.PLAIN, 40));
        titreColonneGauche.setForeground(Color.WHITE);
        titreColonneGauche.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton creeServeur = new JButton("Créé un nouveau serveur !");
        creeServeur.setAlignmentX(Component.CENTER_ALIGNMENT);
        creeServeur.setFont(new Font("Arial", Font.PLAIN, 18));
        creeServeur.addActionListener(e -> {
            System.out.println("[DEBUG] création serveur confirmé :) ");
            VueLobby lobby = new VueLobby(parent, retourVers, pseudo, true);
            parent.setContentPane(lobby);
            parent.revalidate();
            parent.repaint();
        });

        contenueColonneGauche.add(titreColonneGauche);
        contenueColonneGauche.add(Box.createVerticalGlue());
        demandePseudonyme(contenueColonneGauche);

        contenueColonneGauche.add(Box.createVerticalGlue());
        contenueColonneGauche.add(creeServeur);
        contenueColonneGauche.add(Box.createVerticalGlue());
        contenueColonneGauche.setBorder(contourColonnes);
        contenuPrincipal.add(contenueColonneGauche);


        JPanel contenueColonneDroite = new JPanel();
        contenueColonneDroite.setOpaque(false);
        contenueColonneDroite.setLayout(new BoxLayout(contenueColonneDroite, BoxLayout.Y_AXIS));

        JLabel titreColonneDroite = new JLabel("Rejoindre un serveur !");
        titreColonneDroite.setFont(new Font("Arial", Font.PLAIN, 40));
        titreColonneDroite.setForeground(Color.WHITE);
        titreColonneDroite.setAlignmentX(Component.CENTER_ALIGNMENT);


        JLabel label = new JLabel("Entrer l'IP d'un serveur :");
        label.setFont(new Font("Arial", Font.PLAIN, 20));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField serveurField = new JTextField(15);
        serveurField.setFont(new Font("Arial", Font.PLAIN, 20));
        serveurField.setAlignmentX(Component.CENTER_ALIGNMENT);
        serveurField.setMaximumSize(new Dimension(200, serveurField.getPreferredSize().height));

        JButton confirmationServeur = new JButton("Rejoindre le serveur !");
        confirmationServeur.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmationServeur.setFont(new Font("Arial", Font.PLAIN, 18));
        confirmationServeur.addActionListener(e -> {
            this.serveur = serveurField.getText();
            System.out.println("[DEBUG] reception serveur  : "+serveur);
            VueLobby lobby = new VueLobby(parent, retourVers, pseudo, false);
            parent.setContentPane(lobby);
            parent.revalidate();
            parent.repaint();
        });


        // Ajout des élements dans la colonne de droite
        contenueColonneDroite.add(titreColonneDroite);
        contenueColonneDroite.add(Box.createVerticalGlue());
        contenueColonneDroite.add(label);
        contenueColonneDroite.add(serveurField);
        contenueColonneDroite.add(confirmationServeur);
        contenueColonneDroite.add(Box.createVerticalGlue());

        contenueColonneDroite.setBorder(contourColonnes);

        contenuPrincipal.add(contenueColonneDroite);


        add(contenuPrincipal);
    }






}