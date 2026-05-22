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
    private String serveur;
    private boolean iaEnqueteur = false;
    private boolean iaJack = false;

    private String pseudoParDefaut = "";
    private boolean pseudoGaucheValide = false;
    private boolean pseudoDroiteValide = false;

    private JButton boutonLancerGlobal;

    public VueMenuPrePartie(JFrame parent, JPanel retourVers, String mode) {
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
                    } else {
                        joueurChoisi = Joueur.ENQUETEUR;
                    }
                    Partie partie = new Partie(joueurChoisi, niveauJack, niveauEnqueteur);
                    VueJeu jeu = new VueJeu(partie);
                    jeu.setVisible(true);
                    SwingUtilities.getWindowAncestor(this).dispose();
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
                    ((VueMenuPrincipal) retourVers).resetBoutonsSurvoles();
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
                    pseudoGaucheValide = true;
                } else {
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
        // variables pour simplifier la logique
        boolean humainEnqueteur = pseudoGaucheValide && !iaEnqueteur;
        boolean humainJack = pseudoDroiteValide && !iaJack;
        boolean iaEnqueteurActive = iaEnqueteur;
        boolean iaJackActive = iaJack;
        // ===== DEBUG =====
        System.out.println("===== DEBUG CONFIG PARTIE =====");
        System.out.println("pseudoGaucheValide = " + pseudoGaucheValide);
        System.out.println("pseudoDroiteValide = " + pseudoDroiteValide);
        System.out.println("iaEnqueteur (gauche) = " + iaEnqueteur);
        System.out.println("iaJack (droite) = " + iaJack);
        System.out.println("humainGauche = " + humainEnqueteur);
        System.out.println("humainDroite = " + humainJack);
        System.out.println("IA gauche active = " + iaEnqueteurActive);
        System.out.println("IA droite active = " + iaJackActive);
        System.out.println("================================");

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
            if(this.pseudo == null || this.pseudo.trim().isEmpty()) {
                //TODO ; afficher message d'erreur, pour dire qu'il faut un pseudo
                return;
            }
            System.out.println("[DEBUG] création serveur confirmé :) ");
            Serveur serveur = new Serveur();
            Client clientHote = new Client(pseudo, true, "localhost");
            VueLobby lobby = new VueLobby(parent, retourVers, pseudo, true, clientHote);
            clientHote.setVueLobby(lobby);
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
            if (this.pseudo == null || this.pseudo.trim().isEmpty()) {
                //TODO ; afficher message d'erreur, pour dire qu'il faut un pseudo (code refacorisable)
                return;
            }
            this.serveur = serveurField.getText();
            System.out.println("[DEBUG] reception serveur  : "+serveur);
            Client client = new Client(pseudo, false, serveur);
            VueLobby lobby = new VueLobby(parent, retourVers, pseudo, false,client);
            client.setVueLobby(lobby);
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

    /** Celui pour le multi **/
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
}