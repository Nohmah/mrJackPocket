package src.vue.menus;

import src.reseau.Client;
import src.utils.utils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;

public class VueLobby extends JPanel {

    private final Client client;
    private final String pseudo;

    private JTextArea historiqueChat;
    private JLabel labelJ1;
    private JLabel labelJ2;
    private JLabel statutPret;
    private JButton boutonPret;

    private JRadioButton mrJackJ1, lEnqueteurJ1;
    private JRadioButton mrJackJ2, lEnqueteurJ2;

    private boolean jeSuisPret = false;
    private final boolean estHote;

    private final JFrame parent;
    private final JPanel panel;

    public VueLobby(JFrame parent, JPanel retourVers, String pseudoJoueur, boolean estHote, Client client) {
        this.client = client;
        this.pseudo = pseudoJoueur;
        this.estHote = estHote;
        this.parent = parent;
        this.panel = retourVers;

        setBackground(Color.GRAY);
        setLayout(new BorderLayout());

        JPanel contenuPrincipal = new JPanel(new GridLayout(1, 2, 20, 0));
        contenuPrincipal.setOpaque(false);

        Border contourColonnes = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 0),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        JPanel colonneGauche = new JPanel();
        colonneGauche.setLayout(new BoxLayout(colonneGauche, BoxLayout.Y_AXIS));
        colonneGauche.setOpaque(false);
        colonneGauche.setBorder(contourColonnes);

        retourFenetrePrecedent(parent, retourVers, colonneGauche);
        creerChatBox(colonneGauche);


        JPanel colonneDroite = new JPanel();
        colonneDroite.setLayout(new BoxLayout(colonneDroite, BoxLayout.Y_AXIS));
        colonneDroite.setOpaque(false);
        colonneDroite.setBorder(contourColonnes);

        creerPanneauJoueurs(colonneDroite);

        contenuPrincipal.add(colonneGauche);
        contenuPrincipal.add(colonneDroite);

        add(contenuPrincipal, BorderLayout.CENTER);

        mettreAJourRoles(true);
    }

    private void retourFenetrePrecedent(JFrame parent, JPanel retourVers, JPanel colonne) {
        JButton retour = new BoutonsMenu(utils.loadImage("fast-forward"), null);
        retour.setAlignmentX(Component.LEFT_ALIGNMENT);
        retour.addActionListener(e -> {
            client.joueurQuitte();
            parent.setContentPane(retourVers);
            parent.revalidate();
            parent.repaint();
        });

        JPanel panneauRetour = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panneauRetour.setOpaque(false);
        panneauRetour.add(retour);
        colonne.add(panneauRetour);
    }

    private void creerChatBox(JPanel positionnement) {
        JLabel titreChat = new JLabel("Chat du Lobby");
        titreChat.setFont(new Font("Arial", Font.BOLD, 20));
        titreChat.setForeground(Color.WHITE);
        titreChat.setAlignmentX(Component.CENTER_ALIGNMENT);

        historiqueChat = new JTextArea();
        historiqueChat.setEditable(false);
        historiqueChat.setLineWrap(true);
        historiqueChat.setWrapStyleWord(true);
        historiqueChat.setFont(new Font("Arial", Font.PLAIN, 14));
        historiqueChat.setText("Système : Serveur créé. En attente d'un adversaire...\n");

        JScrollPane scrollPane = new JScrollPane(historiqueChat);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        scrollPane.setPreferredSize(new Dimension(100, 400));

        JPanel zoneSaisie = new JPanel(new BorderLayout(5, 0));
        zoneSaisie.setOpaque(false);
        zoneSaisie.setMaximumSize(new Dimension(400, 40));

        JTextField champSaisie = new JTextField();
        champSaisie.setFont(new Font("Arial", Font.PLAIN, 16));

        ActionListener actionEnvoyer = e -> {
            String msg = champSaisie.getText().trim();
            if (!msg.isEmpty()) {
               client.nouveauMessage(msg);
                champSaisie.setText("");

            }
        };

        JButton boutonEnvoyer = new JButton("Envoyer");
        boutonEnvoyer.addActionListener(actionEnvoyer);
        champSaisie.addActionListener(actionEnvoyer);

        positionnement.add(titreChat);
        positionnement.add(scrollPane);
        positionnement.add(Box.createVerticalStrut(10));
        zoneSaisie.add(champSaisie, BorderLayout.CENTER);
        zoneSaisie.add(boutonEnvoyer, BorderLayout.EAST);
        positionnement.add(zoneSaisie);
        positionnement.add(Box.createVerticalGlue());
    }

    private void creerPanneauJoueurs(JPanel positionnement) {

        JLabel titre = new JLabel("Paramètres de la partie");
        titre.setFont(new Font("Arial", Font.BOLD, 24));
        titre.setForeground(Color.WHITE);
        titre.setAlignmentX(Component.CENTER_ALIGNMENT);


        mrJackJ1 = new JRadioButton("Mr. Jack");
        lEnqueteurJ1 = new JRadioButton("L'Enquêteur");
        mrJackJ2 = new JRadioButton("Mr. Jack");
        lEnqueteurJ2 = new JRadioButton("L'Enquêteur");

        mrJackJ2.setEnabled(false);
        lEnqueteurJ2.setEnabled(false);

        if (estHote) {
            labelJ1 = new JLabel("Joueur 1 (Hôte) : " + pseudo);
            labelJ1.setForeground(Color.WHITE);
            labelJ1.setFont(new Font("Arial", Font.PLAIN, 18));
            labelJ1.setAlignmentX(Component.CENTER_ALIGNMENT);
            labelJ2 = new JLabel("Joueur 2 : En attente d'une connexion...");
            labelJ2.setForeground(Color.WHITE);
            labelJ2.setFont(new Font("Arial", Font.PLAIN, 18));
            labelJ2.setAlignmentX(Component.CENTER_ALIGNMENT);

            mrJackJ1.setEnabled(true);
            lEnqueteurJ1.setEnabled(true);
        } else {

            labelJ1 = new JLabel("Joueur 1 (Hôte) : Récupération des infos...");
            labelJ1.setForeground(Color.WHITE);
            labelJ1.setFont(new Font("Arial", Font.PLAIN, 18));
            labelJ1.setAlignmentX(Component.CENTER_ALIGNMENT);
            labelJ2 = new JLabel("Joueur 2 : " + pseudo);
            labelJ2.setForeground(Color.WHITE);
            labelJ2.setFont(new Font("Arial", Font.PLAIN, 18));
            labelJ2.setAlignmentX(Component.CENTER_ALIGNMENT);

            mrJackJ1.setEnabled(false);
            lEnqueteurJ1.setEnabled(false);
        }

        ButtonGroup groupJ1 = new ButtonGroup();
        groupJ1.add(mrJackJ1); groupJ1.add(lEnqueteurJ1);
        ButtonGroup groupJ2 = new ButtonGroup();
        groupJ2.add(mrJackJ2); groupJ2.add(lEnqueteurJ2);

        ActionListener changementRoleJ1 = e -> {
            boolean j1VeutEtreJack = mrJackJ1.isSelected();
            client.nouveauChoix(j1VeutEtreJack);

        };
        mrJackJ1.addActionListener(changementRoleJ1);
        lEnqueteurJ1.addActionListener(changementRoleJ1);

        JPanel panelRolesJ1 = prepareRadioBouton(mrJackJ1, lEnqueteurJ1);
        JPanel panelRolesJ2 = prepareRadioBouton(mrJackJ2, lEnqueteurJ2);



        statutPret = new JLabel("Joueurs prêts : 0/2");
        statutPret.setFont(new Font("Arial", Font.BOLD, 18));
        statutPret.setForeground(Color.WHITE);
        statutPret.setAlignmentX(Component.CENTER_ALIGNMENT);

        boutonPret = new JButton("Je suis prêt !");
        boutonPret.setFont(new Font("Arial", Font.BOLD, 16));
        boutonPret.setAlignmentX(Component.CENTER_ALIGNMENT);
        boutonPret.setBackground(Color.GREEN);
        boutonPret.setForeground(Color.WHITE);

        boutonPret.addActionListener(e -> {
            jeSuisPret = !jeSuisPret;

            client.boutonPretAppuyer(jeSuisPret);

            if (jeSuisPret) {
                boutonPret.setText("Annuler");
                boutonPret.setBackground(Color.RED);
            } else {
                boutonPret.setText("Je suis prêt !");
                boutonPret.setBackground(Color.GREEN);
            }


        });

        positionnement.add(titre);
        positionnement.add(Box.createVerticalGlue());
        positionnement.add(labelJ1);
        positionnement.add(panelRolesJ1);
        positionnement.add(Box.createVerticalStrut(30));
        positionnement.add(labelJ2);
        positionnement.add(panelRolesJ2);
        positionnement.add(Box.createVerticalGlue());
        positionnement.add(statutPret);
        positionnement.add(Box.createVerticalStrut(10));
        positionnement.add(boutonPret);
    }

    private JPanel prepareRadioBouton(JRadioButton btn1, JRadioButton btn2) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);

        btn1.setForeground(Color.WHITE); btn1.setOpaque(false);
        btn2.setForeground(Color.WHITE); btn2.setOpaque(false);

        panel.add(btn1);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(btn2);
        return panel;
    }



    /**
     **     Méthodes à destination du serveur pour mettre à jour la fenêtre
     **/


    public void ajouterMessageChat(String pseudo, String message) {
        historiqueChat.append(pseudo + " : " + message + "\n");
    }


    public void mettreAJourRoles(boolean hoteEstJack) {
        if (hoteEstJack) {
            mrJackJ1.setSelected(true);
            lEnqueteurJ2.setSelected(true);
        } else {
            lEnqueteurJ1.setSelected(true);
            mrJackJ2.setSelected(true);
        }
    }


    public void mettreAJourCompteurPrets(int nbPrets) {
        statutPret.setText("Joueurs prêts : " + nbPrets + "/2");
        if(nbPrets == 2) {
            statutPret.setForeground(Color.GREEN);
            boutonPret.setEnabled(false);
        } else {
            statutPret.setForeground(Color.WHITE);
        }
    }


    public void setJoueur2Connecte(String pseudoJ1, String pseudoJ2) {
        SwingUtilities.invokeLater(() -> {
            if(estHote) {
                labelJ2.setText("Joueur 2 : " + pseudoJ2);
                labelJ2.setForeground(Color.WHITE);
                labelJ2.setFont(new Font("Arial", Font.PLAIN, 18));
                labelJ2.setAlignmentX(Component.CENTER_ALIGNMENT);
                ajouterMessageChat("Système", pseudoJ2 + " a rejoint le lobby !");
            } else {

                labelJ1.setText("Joueur 1 (Hôte) : " + pseudoJ1);
                labelJ1.setForeground(Color.WHITE);
                labelJ1.setFont(new Font("Arial", Font.PLAIN, 18));
                labelJ1.setAlignmentX(Component.CENTER_ALIGNMENT);

                ajouterMessageChat("Système", "Vous avez rejoint le lobby de " + pseudoJ1 + " !");
            }

            revalidate();
            repaint();
        });

    }

    public void setJoueur2Deconnecte(String message) {
        SwingUtilities.invokeLater(() -> {
            if(estHote) {
                labelJ2.setText("Joueur 2 : En attente d'une connexion...");
                ajouterMessageChat("Système", message);
                if(this.jeSuisPret){
                    this.jeSuisPret = false;
                    boutonPret.setText("Je suis prêt !");
                    boutonPret.setBackground(Color.GREEN);
                    client.boutonPretAppuyer(this.jeSuisPret);
                }
            }
            revalidate();
            repaint();
        });

    }

    public void hoteEstDeconnecter(){
        SwingUtilities.invokeLater(() -> {
            this.parent.setContentPane(this.panel);
            this.parent.revalidate();
            this.parent.repaint();
        });
    }

    public void lancementPartie(src.vue.VueJeu nouvellePartie) {
        ajouterMessageChat("Système", "Lancement de la partie dans :");
        JFrame parent = this.parent;
        Timer timer = new Timer(1000, new ActionListener() {
            int compteur = 3;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (compteur > 0) {
                    ajouterMessageChat("Système", String.valueOf(compteur));
                    compteur--;
                } else {
                    ajouterMessageChat("Système", "GO !");
                    ((Timer)e.getSource()).stop(); // On arrête le timer
                    parent.dispose();
                    nouvellePartie.setVisible(true);
                }
            }
        });

        timer.start();
    }

}