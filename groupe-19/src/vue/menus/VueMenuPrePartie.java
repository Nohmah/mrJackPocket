package src.vue.menus;

import src.modele.Joueur;
import src.modele.Partie;
import src.reseau.Client;
import src.reseau.Serveur;
import src.utils.utils;
import src.vue.VueJeu;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;

public class VueMenuPrePartie extends JPanel {

    private Joueur joueurChoisi = null;
    private int difficulteIA_GAUCHE = 0;
    private int difficulteIA_DROITE = 0;
    private String message;
    private String pseudo;
    private String pseudoGauche = "";
    private String pseudoDroite = "";
    private String serveur;
    private boolean iaEnqueteur = false;
    private boolean iaJack = false;

    private String pseudoParDefaut = "";
    private boolean pseudoGaucheValide = false;
    private boolean pseudoDroiteValide = false;

    private JButton boutonLancerGlobal;
    private VueMenuPrincipal menuPrincipal;

    public VueMenuPrePartie(JFrame parent, JPanel retourVers, VueMenuPrincipal menuPrincipal, String mode) {
        this.menuPrincipal = menuPrincipal;
        setBackground(Color.GRAY);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        switch(mode) {
            case "solo":
                setLayout(new BorderLayout());
                Background background = new Background();
                background.setLayout(new GridLayout(1, 2));
                add(background, BorderLayout.CENTER);
                pseudoParDefaut = utils.lirePseudoSauvegarde(); // récupère le pseudo (ou pas) sauvegardé

                // bordure noire entre les 2 colonnes
                Border contourColonnes = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK, 10),
                        BorderFactory.createEmptyBorder(10,10,10,10)
                );

                // colonne gauche (enquêteur)
                JPanel colonneGauche = new JPanel();
                colonneGauche.setOpaque(false);
                colonneGauche.setLayout(new BoxLayout(colonneGauche, BoxLayout.Y_AXIS));
                colonneGauche.setBorder(contourColonnes);
                JLabel titreColonneGauche = new JLabel("Personnalisation de l'Enquêteur");
                titreColonneGauche.setFont(new Font("Arial", Font.BOLD, 30));
                titreColonneGauche.setForeground(Color.WHITE);
                titreColonneGauche.setAlignmentX(Component.CENTER_ALIGNMENT);

                // bouton retour en haut
                retourFenetrePrecedent(parent, retourVers, colonneGauche);
                colonneGauche.add(titreColonneGauche);
                colonneGauche.add(Box.createVerticalStrut(30));

                // bouton pour lancer la partie
                boutonLancerGlobal = new JButton("Personnalisation non terminée");
                boutonLancerGlobal.setAlignmentX(Component.CENTER_ALIGNMENT);
                boutonLancerGlobal.setFont(new Font("Arial", Font.BOLD, 20));
                boutonLancerGlobal.setEnabled(false);

                // détermine à quoi ressemble le bouton pour lancer la partie
                // (cliquable ou non, et ce qui est écrit à l'intérieur)
                boutonLancerGlobal.addActionListener(e -> {

                    int niveauJack = iaJack ? difficulteIA_DROITE : -1;
                    int niveauEnqueteur = iaEnqueteur ? difficulteIA_GAUCHE : -1;
                    // détermine si on a choisi un rôle
                    Joueur joueurChoisi;
                    if (!iaEnqueteur && !iaJack) {
                        joueurChoisi = null;
                    } else if (iaEnqueteur && !iaJack) {
                        joueurChoisi = Joueur.JACK;
                    } else if (!iaEnqueteur && iaJack){
                        joueurChoisi = Joueur.ENQUETEUR;
                    } else {
                        joueurChoisi = null;
                    }
                    String pseudoEnqueteur = (pseudoGaucheValide && !iaEnqueteur) ? pseudoGauche : "Enquêteur";
                    String pseudoJack = (pseudoDroiteValide && !iaJack) ? pseudoDroite : "Mr. Jack";
                    Partie partie = new Partie(joueurChoisi, niveauJack, niveauEnqueteur,
                            pseudoEnqueteur, pseudoJack);
                    VueJeu jeu = new VueJeu(parent, menuPrincipal, partie);
                    parent.setContentPane(jeu);
                    parent.revalidate();
                    parent.repaint();
                });

                // suite colonne gauche
                JButton lancerBouton = soloMenu(parent, retourVers, colonneGauche, "gauche");

                background.add(colonneGauche);

                // colonne droite
                JPanel colonneDroite = new JPanel();
                colonneDroite.setOpaque(false);
                colonneDroite.setLayout(new BoxLayout(colonneDroite, BoxLayout.Y_AXIS));
                colonneDroite.setBorder(contourColonnes);

                // espace pour aligner le contenu des 2 colonnes (à cause du bouton retour de la colonne de gauche)
                JPanel espaceHeader = new JPanel();
                espaceHeader.setOpaque(false);
                espaceHeader.setPreferredSize(new Dimension(160, 66));
                colonneDroite.add(espaceHeader);

                // titre
                JLabel titreColonneDroite = new JLabel("Personnalisation de Jack");
                titreColonneDroite.setFont(new Font("Arial", Font.BOLD, 32));
                titreColonneDroite.setForeground(Color.WHITE);
                titreColonneDroite.setAlignmentX(Component.CENTER_ALIGNMENT);

                colonneDroite.add(titreColonneDroite);
                colonneDroite.add(Box.createVerticalStrut(30));

                // contenu de la colonne de droite
                soloMenu(parent, retourVers, colonneDroite, "droite");

                background.add(colonneDroite);

                // Panel pour le bouton lancer partie en bas
                JPanel panelBas = new JPanel(new FlowLayout(FlowLayout.CENTER));
                panelBas.setBackground(Color.BLACK);
                panelBas.setOpaque(true);
                panelBas.add(lancerBouton);

                add(panelBas, BorderLayout.SOUTH);
                break;
            case "multi":
                multiMenu(parent, retourVers);
                break;
            default:
                throw new RuntimeException("Appel constructeur 'VueMenuPrePartie' avec paramètre 'String mode' invalide : "+mode+" !");
        }
    }

    private void retourFenetrePrecedent(JFrame parent, JPanel retourVers, JPanel positionnement) {

        JButton retour = BoutonsMenu.creerBouton(
                "Boutonretour",
                () -> {
                    parent.setContentPane(retourVers);
                    parent.revalidate();
                    parent.repaint();
                },
                new Dimension(160, 66)
        );

        JPanel panneauRetour = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panneauRetour.setOpaque(false);

        panneauRetour.add(retour);

        positionnement.add(panneauRetour);
    }

    /** Version pour le solo **/
    private void demandePseudonyme(JPanel position, String role){

        JPanel zoneSaisie = new JPanel();
        zoneSaisie.setLayout(new BoxLayout(zoneSaisie, BoxLayout.Y_AXIS));
        zoneSaisie.setOpaque(false);

        JLabel label = new JLabel("Pseudo :");
        label.setFont(new Font("Arial", Font.PLAIN, 20));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField pseudoField = new JTextField(15);
        if (pseudoParDefaut != null) {
            pseudoField.setText(pseudoParDefaut);
        }
        pseudoField.setFont(new Font("Arial", Font.PLAIN, 20));
        pseudoField.setAlignmentX(Component.CENTER_ALIGNMENT);
        pseudoField.setMaximumSize(new Dimension(200, pseudoField.getPreferredSize().height));

        JButton confirmationPseudo = new JButton("Confirmer le pseudo");
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
                if(role.equals("gauche")) {
                    pseudoGauche = this.pseudo;
                    pseudoGaucheValide = true;
                } else {
                    pseudoDroite = this.pseudo;
                    pseudoDroiteValide = true;
                }

                verifierConfigurationComplete();
                zoneSaisie.removeAll();

                JLabel pseudoValide = new JLabel("Pseudo : " + pseudo);
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

    /** Utilisé pour le contenu de la colonne de gauche et de droite, le comportement change en fonction de "role" **/
    private JButton soloMenu(JFrame parent, JPanel retourVers, JPanel container, String role){
        container.add(Box.createVerticalGlue());

        demandePseudonyme(container, role);
        Component zonePseudo = container.getComponent(container.getComponentCount() - 1);

        JLabel choixIALabel = new JLabel("Contrôlé par l'IA ?");
        choixIALabel.setFont(new Font("Arial", Font.PLAIN, 20));
        choixIALabel.setForeground(Color.WHITE);
        choixIALabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        container.add(choixIALabel);

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

        container.add(radioPanelIA);

        JLabel choixDifficulteIALabel = new JLabel("Choisir la difficulté de l'IA");
        choixDifficulteIALabel.setFont(new Font("Arial", Font.PLAIN, 20));
        choixDifficulteIALabel.setForeground(Color.WHITE);
        choixDifficulteIALabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        choixDifficulteIALabel.setVisible(false);

        JPanel radioPanelDifficulteIA = new JPanel();
        radioPanelDifficulteIA.setLayout(new BoxLayout(radioPanelDifficulteIA, BoxLayout.X_AXIS));
        radioPanelDifficulteIA.setBackground(Color.GRAY);

        JRadioButton facile = new JRadioButton("Facile");
        JRadioButton intermediaire = new JRadioButton("Intermédiaire");
        JRadioButton difficile = new JRadioButton("Difficile");

        ButtonGroup groupDifficulteIA = new ButtonGroup();
        groupDifficulteIA.add(facile);
        groupDifficulteIA.add(intermediaire);
        groupDifficulteIA.add(difficile);

        facile.addActionListener(e -> {
            if (role.equals("gauche")) {
                difficulteIA_GAUCHE = 0;
            } else {
                difficulteIA_DROITE = 0;
            }
        });

        intermediaire.addActionListener(e -> {
            if (role.equals("gauche")) {
                difficulteIA_GAUCHE = 1;
            } else {
                difficulteIA_DROITE = 1;
            }
        });

        difficile.addActionListener(e -> {
            if (role.equals("gauche")) {
                difficulteIA_GAUCHE = 2;
            } else {
                difficulteIA_DROITE = 2;
            }
        });

        radioPanelDifficulteIA.add(facile);
        radioPanelDifficulteIA.add(intermediaire);
        radioPanelDifficulteIA.add(difficile);

        radioPanelDifficulteIA.setVisible(false);

        container.add(choixDifficulteIALabel);
        container.add(radioPanelDifficulteIA);


        oui.addActionListener(e -> {

            zonePseudo.setVisible(false);

            choixDifficulteIALabel.setVisible(true);
            radioPanelDifficulteIA.setVisible(true);
            facile.setSelected(true);

            if(role.equals("gauche")) {
                iaEnqueteur = true;
            } else {
                iaJack = true;
            }

            verifierConfigurationComplete();
        });

        non.addActionListener(e -> {

            zonePseudo.setVisible(true);

            choixDifficulteIALabel.setVisible(false);
            radioPanelDifficulteIA.setVisible(false);
            if(role.equals("gauche")) {
                iaEnqueteur = false;
            } else {
                iaJack = false;
            }

            verifierConfigurationComplete();
        });
        container.add(Box.createVerticalGlue()); // Pour que ça ne colle pas au bas de la fenetre
        return boutonLancerGlobal;
    }

    private void verifierConfigurationComplete() {
        // variables pour améliorer la compréhension des conditions
        boolean humainEnqueteur = pseudoGaucheValide && !iaEnqueteur;
        boolean humainJack = pseudoDroiteValide && !iaJack;
        boolean iaEnqueteurActive = iaEnqueteur;
        boolean iaJackActive = iaJack;

        // humain vs humain
        if (humainEnqueteur && humainJack) {
            boutonLancerGlobal.setEnabled(true);
            boutonLancerGlobal.setText("Que le meilleur gagne !");
            return;
        }
        // IA vs IA
        if (iaEnqueteurActive && iaJackActive) {
            boutonLancerGlobal.setEnabled(true);
            boutonLancerGlobal.setText("Que la meilleure IA gagne !");
            return;
        }
        // Humain vs IA
        if (iaEnqueteurActive && humainJack) {
            boutonLancerGlobal.setEnabled(true);
            boutonLancerGlobal.setText("Vite ! Je dois m'enfuir !");
            return;
        }
        if (humainEnqueteur && iaJackActive) {
            boutonLancerGlobal.setEnabled(true);
            boutonLancerGlobal.setText("Jack, je te trouverai !");
            return;
        }
        boutonLancerGlobal.setEnabled(false);
        boutonLancerGlobal.setText("Personnalisation non terminée");
    }

//    private void soloMenu(JFrame parent, JPanel retourVers, JPanel container){
//
//        container.add(Box.createVerticalGlue());
//
//        demandePseudonyme(container);
//
//        JLabel choixUnLabel = new JLabel("Qui voulez-vous incarnez ?");
//        choixUnLabel.setFont(new Font("Arial", Font.PLAIN, 20));
//        choixUnLabel.setForeground(Color.WHITE);
//        choixUnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
//        container.add(choixUnLabel);
//
//        JPanel radioPanel = new JPanel();
//        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.X_AXIS));
//        radioPanel.setBackground(Color.GRAY);
//
//        JRadioButton mrJack = new JRadioButton("Mr. Jack");
//        JRadioButton lEnqueteur = new JRadioButton("L'Enquêteur");
//
//        ButtonGroup group = new ButtonGroup();
//        group.add(mrJack);
//        group.add(lEnqueteur);
//
//        radioPanel.add(mrJack);
//        radioPanel.add(lEnqueteur);
//
//        container.add(radioPanel);
//
//
//
//        JLabel choixIALabel = new JLabel("Jouer contre l'IA ?");
//        choixIALabel.setFont(new Font("Arial", Font.PLAIN, 20));
//        choixIALabel.setForeground(Color.WHITE);
//        choixIALabel.setAlignmentX(Component.CENTER_ALIGNMENT);
//
//        container.add(choixIALabel);
//
//        JPanel radioPanelIA = new JPanel();
//        radioPanelIA.setLayout(new BoxLayout(radioPanelIA, BoxLayout.X_AXIS));
//        radioPanelIA.setBackground(Color.GRAY);
//
//        JRadioButton oui = new JRadioButton("Oui");
//        JRadioButton non = new JRadioButton("Non");
//
//        non.setSelected(true);
//
//        ButtonGroup groupIA = new ButtonGroup();
//        groupIA.add(oui);
//        groupIA.add(non);
//
//        radioPanelIA.add(oui);
//        radioPanelIA.add(non);
//
//        container.add(radioPanelIA);
//
//        JLabel choixDifficulteIALabel = new JLabel("Choisir la difficulté de l'IA");
//        choixDifficulteIALabel.setFont(new Font("Arial", Font.PLAIN, 20));
//        choixDifficulteIALabel.setForeground(Color.WHITE);
//        choixDifficulteIALabel.setAlignmentX(Component.CENTER_ALIGNMENT);
//        choixDifficulteIALabel.setVisible(false);
//
//        JPanel radioPanelDifficulteIA = new JPanel();
//        radioPanelDifficulteIA.setLayout(new BoxLayout(radioPanelDifficulteIA, BoxLayout.X_AXIS));
//        radioPanelDifficulteIA.setBackground(Color.GRAY);
//
//        JRadioButton facile = new JRadioButton("Facile");
//        facile.addActionListener(e -> difficulteIAChoisi = 0);
//
//        JRadioButton intermediaire = new JRadioButton("Intermédiaire");
//        intermediaire.addActionListener(e -> difficulteIAChoisi = 1);
//
//        JRadioButton difficile = new JRadioButton("Difficile");
//        difficile.addActionListener(e -> difficulteIAChoisi = 2);
//
//        ButtonGroup groupDifficulteIA = new ButtonGroup();
//        groupDifficulteIA.add(facile);
//        groupDifficulteIA.add(intermediaire);
//        groupDifficulteIA.add(difficile);
//
//        radioPanelDifficulteIA.add(facile);
//        radioPanelDifficulteIA.add(intermediaire);
//        radioPanelDifficulteIA.add(difficile);
//
//        radioPanelDifficulteIA.setVisible(false);
//
//        container.add(choixDifficulteIALabel);
//        container.add(radioPanelDifficulteIA);
//
//        oui.addActionListener(e -> {
//            choixDifficulteIALabel.setVisible(true);
//            radioPanelDifficulteIA.setVisible(true);
//            IAChoisi = true;
//        });
//
//        non.addActionListener(e -> {
//            choixDifficulteIALabel.setVisible(false);
//            radioPanelDifficulteIA.setVisible(false);
//            IAChoisi = false;
//        });
//
//
//        JButton lancerBouton = new JButton();
//
//        lancerBouton.setAlignmentX(Component.CENTER_ALIGNMENT);
//        lancerBouton.setFont(new Font("Arial", Font.BOLD, 18));
//        lancerBouton.setEnabled(false);
//        lancerBouton.setVisible(false);
//
//        lancerBouton.addActionListener(e -> {
//
//            Partie partie;
//
//            if (!IAChoisi) {
//                partie = new Partie(joueurChoisi, -1, -1);
//            }
//            else if (joueurChoisi == Joueur.JACK) {
//                partie = new Partie(joueurChoisi, -1, difficulteIAChoisi);
//            }
//            else if (joueurChoisi == Joueur.ENQUETEUR) {
//                partie = new Partie(joueurChoisi, difficulteIAChoisi, -1);
//            }
//            else {
//                partie = new Partie(joueurChoisi, -1, -1);
//            }
//
//            VueJeu jeu = new VueJeu(partie);
//            jeu.setVisible(true);
//
//            SwingUtilities.getWindowAncestor(this).dispose();
//        });
//
//        container.add(Box.createVerticalStrut(20));
//        container.add(lancerBouton);
//        mrJack.addActionListener(e -> {
//            joueurChoisi = Joueur.JACK;
//
//            message = "Vite ! Je dois m'échapper !";
//
//            lancerBouton.setText(message);
//            lancerBouton.setVisible(true);
//            lancerBouton.setEnabled(true);
//        });
//
//        lEnqueteur.addActionListener(e -> {
//            joueurChoisi = Joueur.ENQUETEUR;
//            // String pseudo = pseudoField.getText(); pour avoir le pseudo entré par l'utilisateur
//
//            message = "Jack, nous te trouverons !";
//
//            lancerBouton.setText(message);
//            lancerBouton.setVisible(true);
//            lancerBouton.setEnabled(true);
//        });
//
//
//        container.add(Box.createVerticalGlue());
//    }

    private void multiMenu(JFrame parent, JPanel retourVers) {
        pseudoParDefaut = utils.lirePseudoSauvegarde();

        setLayout(new BorderLayout());
        Background background = new Background();
        background.setLayout(new GridLayout(1, 2));
        add(background, BorderLayout.CENTER);

        Border contourColonnes = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 10),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        JPanel zoneErreur = new JPanel();
        zoneErreur.setOpaque(false);

        JPanel colonneGauche = new JPanel();
        colonneGauche.setOpaque(false);
        colonneGauche.setLayout(new BoxLayout(colonneGauche, BoxLayout.Y_AXIS));
        colonneGauche.setBorder(contourColonnes);

        retourFenetrePrecedent(parent, retourVers, colonneGauche);

        JLabel titreGauche = new JLabel("Votre identification");
        titreGauche.setFont(new Font("Arial", Font.BOLD, 30));
        titreGauche.setForeground(Color.WHITE);
        titreGauche.setAlignmentX(Component.CENTER_ALIGNMENT);
        colonneGauche.add(titreGauche);
        colonneGauche.add(Box.createVerticalStrut(50));

        demandePseudonyme(colonneGauche, zoneErreur);

        colonneGauche.add(Box.createVerticalGlue());

        JPanel colonneDroite = new JPanel();
        colonneDroite.setOpaque(false);
        colonneDroite.setLayout(new BoxLayout(colonneDroite, BoxLayout.Y_AXIS));
        colonneDroite.setBorder(contourColonnes);

        JPanel espaceHeader = new JPanel();
        espaceHeader.setOpaque(false);
        espaceHeader.setPreferredSize(new Dimension(160, 106));

        JLabel titreDroite = new JLabel("Multijoueur");
        titreDroite.setFont(new Font("Arial", Font.BOLD, 30));
        titreDroite.setForeground(Color.WHITE);
        titreDroite.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel labelIp = new JLabel("Tapez l'IP du serveur pour le rejoindre :");
        labelIp.setFont(new Font("Arial", Font.PLAIN, 20));
        labelIp.setForeground(Color.WHITE);
        labelIp.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField serveurField = new JTextField(15);
        serveurField.setFont(new Font("Arial", Font.PLAIN, 20));
        serveurField.setMaximumSize(new Dimension(200, 40));
        serveurField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton rejoindre = new JButton("Rejoindre !");
        rejoindre.setAlignmentX(Component.CENTER_ALIGNMENT);
        rejoindre.setFont(new Font("Arial", Font.BOLD, 18));
        rejoindre.addActionListener(e -> {
            if (this.pseudo == null || this.pseudo.trim().isEmpty()) {
                erreurPseudoNonMis(zoneErreur);
            }else {
                this.serveur = serveurField.getText();
                Client client = new Client(pseudo, false, serveur);
                VueLobby lobby = new VueLobby(parent, retourVers, pseudo, false, client);
                client.setVueLobby(lobby);
                parent.setContentPane(lobby);
                parent.revalidate();
                parent.repaint();
            }
        });

        JLabel labelServ = new JLabel("Devenez hôte d'un serveur :");
        labelServ.setFont(new Font("Arial", Font.PLAIN, 20));
        labelServ.setForeground(Color.WHITE);
        labelServ.setAlignmentX(Component.CENTER_ALIGNMENT);



        JButton creeServeur = new JButton("Créez un serveur !");
        creeServeur.setAlignmentX(Component.CENTER_ALIGNMENT);
        creeServeur.setFont(new Font("Arial", Font.BOLD, 18));
        creeServeur.addActionListener(e -> {
            if (this.pseudo == null || this.pseudo.trim().isEmpty()){
                erreurPseudoNonMis(zoneErreur);
            }else {
                System.out.println("[DEBUG] création serveur confirmé :) ");
                Serveur serveur = new Serveur();
                Client clientHote = new Client(pseudo, true, "localhost");
                VueLobby lobby = new VueLobby(parent, retourVers, pseudo, true, clientHote);
                clientHote.setVueLobby(lobby);
                parent.setContentPane(lobby);
                parent.revalidate();
                parent.repaint();
            }
        });

        colonneDroite.add(espaceHeader);
        colonneDroite.add(Box.createVerticalGlue());
        colonneDroite.add(titreDroite);
        colonneDroite.add(Box.createVerticalStrut(50));
        colonneDroite.add(labelIp);
        colonneDroite.add(Box.createVerticalStrut(20));
        colonneDroite.add(serveurField);
        colonneDroite.add(Box.createVerticalStrut(20));
        colonneDroite.add(rejoindre);
        colonneDroite.add(Box.createVerticalStrut(50));
        colonneDroite.add(labelServ);
        colonneDroite.add(Box.createVerticalStrut(20));
        colonneDroite.add(creeServeur);
        colonneDroite.add(Box.createVerticalGlue());
        colonneDroite.add(zoneErreur);
        colonneDroite.add(Box.createVerticalGlue());

        background.add(colonneGauche);
        background.add(colonneDroite);
    }

    private void erreurPseudoNonMis(JPanel parent) {
        parent.removeAll();

        JLabel messageErreur = new JLabel("Vous devez saisir un pseudonym pour pouvoir jouer en Multijoueur ! ");
        messageErreur.setFont(new Font("Arial", Font.BOLD, 15));
        messageErreur.setForeground(Color.RED);
        messageErreur.setAlignmentX(Component.CENTER_ALIGNMENT);
        parent.add(messageErreur);
        parent.revalidate();
        parent.repaint();
    }

    /** Celui pour le multi **/
    private void demandePseudonyme(JPanel position, JPanel messageErreur) {

        JPanel zoneSaisie = new JPanel();
        zoneSaisie.setLayout(new BoxLayout(zoneSaisie, BoxLayout.Y_AXIS));
        zoneSaisie.setOpaque(false);

        JLabel label = new JLabel("Entrer votre pseudo :");
        label.setFont(new Font("Arial", Font.PLAIN, 20));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField pseudoField = new JTextField(15);

        if(pseudoParDefaut != null && !pseudoParDefaut.trim().isEmpty()){
            pseudoField.setText(pseudoParDefaut);
        }

        pseudoField.setFont(new Font("Arial", Font.PLAIN, 20));
        pseudoField.setAlignmentX(Component.CENTER_ALIGNMENT);
        pseudoField.setMaximumSize(new Dimension(200, pseudoField.getPreferredSize().height));

        JButton confirmationPseudo = new JButton("Confirmer votre pseudo");
        confirmationPseudo.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmationPseudo.setFont(new Font("Arial", Font.PLAIN, 18));

        zoneSaisie.add(label);
        zoneSaisie.add(Box.createVerticalStrut(20));
        zoneSaisie.add(pseudoField);
        zoneSaisie.add(Box.createVerticalStrut(20));
        zoneSaisie.add(confirmationPseudo);
        zoneSaisie.add(Box.createVerticalStrut(20));
        zoneSaisie.add(Box.createVerticalGlue());

        ActionListener actionPseudoRecu = e -> {
            this.pseudo = pseudoField.getText();
            if(!this.pseudo.isEmpty()){
                zoneSaisie.removeAll();
                messageErreur.removeAll();
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
}