package src.vue;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import src.modele.*;
import src.utils.SaveManager;
import src.vue.menus.VueMenuPrincipal;
import src.vue.regles.VueReglesPanel;

/**
 * VueJeu — Fenetre principale du jeu (1200×800).
 *
 * Responsabilite UNIQUE : gestion de la fenetre et coordination globale.
 *   - Creer et initialiser la JFrame avec un BorderLayout.
 *   - Gerer la bande laterale gauche (leftStrip) avec ses boutons Swing.
 *   - Posseder une instance de {@link VueMonde} et lui deleguer tout le rendu.
 *   - Appliquer la logique letterbox en transmettant les dimensions a VueMonde.
 *   - Exposer les methodes de mise a jour du mediateur (Gameplay / IHMControler).
 *
 * Ce que VueJeu NE fait plus :
 *   - Aucun Composant2D, Camera, sprite, detective, tuile.
 *   - Aucune logique metier.
 *
 * Layout :
 *   JFrame (BorderLayout)
 *   ├─ WEST  : leftStrip (JPanel BoxLayout Y, largeur fixe STRIP_W_PX)
 *   └─ CENTER: vueMonde  (VueMonde — JPanel contenant le JLayeredPane)
 */
public class VueJeu extends JPanel {
    private final JFrame parent;
    private final VueMenuPrincipal menuPrincipal;
    private boolean partieEnCours = true;
    // =========================================================================
    // Constantes fenetre
    // =========================================================================
    private static final int WINDOW_W   = 1200;
    private static final int WINDOW_H   = 800;
    /** Largeur en pixels de la bande laterale Swing. */
    private static final int STRIP_W_PX = 100;
    /** Cote des boutons carres (largeur dispo moins marge). 90 × 90 px. */
    private static final int BTN_SIZE   = 55;
    private static final int FPS        = 60;

    // =========================================================================
    // Composants Swing propres a la fenetre
    // =========================================================================

    private JPanel  leftStrip;

    /** Panneau de regles actuellement affiche (null = ferme). */
    private JPanel panneauRegles = null;

    // =========================================================================
    // Delegation vers VueMonde
    // =========================================================================

    /** Panneau du monde — unique point de rendu du plateau. */
    private final VueMonde vueMonde;

    // =========================================================================
    // Reference au mediateur
    // =========================================================================

    /** Mediateur logique — seul interlocuteur de VueJeu cote logique. */
    private Gameplay gameplay;

    // =========================================================================
    // Constructeur
    // =========================================================================

    public VueJeu(JFrame parent, VueMenuPrincipal menuPrincipal, Partie partie) {
        this.parent = parent;
        this.menuPrincipal = menuPrincipal;

        // 1. Creer le panneau monde AVANT d'initialiser la fenetre
        vueMonde = new VueMonde();

        // 2. Brancher le callback "clic boule d'action" -> mediateur
        vueMonde.setActionBallClickListener((ballIndex, spriteName) -> {
            if (gameplay != null)
                gameplay.getControler().onActionBallHit(ballIndex, spriteName);
        });

        // 3. Construire la fenetre
        initFrame();
        initLeftStrip();
        initCenterPane();
        initResizeListener();

        // 4. Creer le mediateur (necessite que la vue soit prete)
        this.gameplay = new Gameplay(this, partie);

        // 5. Positions initiales des detectives
        vueMonde.replaceOuterBall(12, 1);
        vueMonde.replaceOuterBall(4,  2);
        vueMonde.replaceOuterBall(8,  3);

        // 6. Ticker — lance l'affichage a la fin pour que tout soit initialise
        new Thread(this::startTicker).start();
    }

    // =========================================================================
    // API d'enregistrement du controleur (appelee par Gameplay)
    // =========================================================================

    /**
     * Branche le controleur sur la Camera (souris + clavier).
     * VueJeu delegue le cablage a VueMonde via son JLayeredPane.
     */
    public void registerControler(IHMControler controler) {
        Component cam = vueMonde.getLayeredPane()
                .getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)[0];
        cam.addMouseListener(controler);
        cam.addMouseMotionListener(controler);  // Survol des jetons d'action
        cam.addKeyListener(controler);
        cam.setFocusable(true);
        cam.requestFocusInWindow();
        // Le MouseAdapter de detection des boules est deja pose dans VueMonde.initCamera()
    }

    // =========================================================================
    // API de mise a jour visuelle — pure delegation vers VueMonde BRICOLAGE POUR MAINTENANCE
    // =========================================================================

    /** Met à jour l'affichage des sabliers de Jack. */
    public void updateSabliers(int sabliers, int maxSabliers) {
        vueMonde.updateSabliers(sabliers, maxSabliers);
    }

    /** Met a jour les 9 tuiles du plateau selon l'etat du district. */
    public void updateDistrictView(District district) {
        vueMonde.updateDistrictView(district);
    }

    /** Met a jour les 4 jetons d'action selon leur face visible. */
    public void updateJetons(List<JetonAction> jetons) {
        vueMonde.updateJetons(jetons);
    }

    /** Met a jour l'affichage des 3 detectives a partir des positions du modele. */
    public void updateDetectivesView(List<Detective> detectives) {
        vueMonde.updateDetectivesView(detectives);
    }

    /** Met en evidence l'indicateur de tour courant. */
    public void updateTurnIndicator(int turn) {
        vueMonde.updateTurnIndicator(turn);
    }

    /** Adapte la couleur de fond au joueur courant. */
    public void updateBackground(Joueur joueurCourant, boolean coursePoursuiteActive) {
    vueMonde.updateBackground(joueurCourant, coursePoursuiteActive);
    }

    /** Bascule la face d'un indicateur de tour (Pile <-> Face). */
    public void switchTurnFace(int turnIndex) {
        vueMonde.switchTurnFace(turnIndex);
    }

    public boolean isTurnFacePile(int turnIndex) {
        return vueMonde.isTurnFacePile(turnIndex);
    }

    /** Bascule la face d'une boule d'action (Pile <-> Face). */
    public void switchBallFace(int ballIndex) {
        vueMonde.switchBallFace(ballIndex);
    }

    /** Modifie la couleur du rectangle d'indicateur d'un joueur. */
    public void updateRectColor(int player, Color color) {
        vueMonde.updateRectColor(player, color);
    }

    public void showGameOverScreen(String vainqueur) {
        vueMonde.showGameOverScreen(vainqueur);
    }

    public void hideGameOverScreen() {
        vueMonde.hideGameOverScreen();
    }

    /** Echange deux tuiles visuellement (positions col/row). */
    public void swapTiles(Vector2 pos1, Vector2 pos2) {
        vueMonde.swapTiles(pos1, pos2);
    }

    /** Fait pivoter une tuile d'un angle supplementaire (multiples de 90 deg). */
    public void rotateTile(int row, int col, int angleToAdd) {
        vueMonde.rotateTile(row, col, angleToAdd);
    }

    /** Deplace un detective sur l'anneau (position 1-12). */
    public void replaceOuterBall(int position, int detectiveNum) {
        vueMonde.replaceOuterBall(position, detectiveNum);
    }

    /** Retourne l'instance VueMonde (pour les acces directs si necessaire). */
    public VueMonde getVueMonde() {
        return vueMonde;
    }

    /**
     * Alias de {@link #getVueMonde()} — utilise par Gameplay pour injecter
     * la sonde de survol et piloter le feedback visuel des jetons.
     */
    public VueMonde getMonde() {
        return vueMonde;
    }

    /** Donne le médiateur */
    public Gameplay getGameplay() { return gameplay; }

    // =========================================================================
    // Fond d'ecran — constantes exposees pour compatibilite avec Gameplay
    // =========================================================================

    public static final String BG_WHITE  = VueMonde.BG_WHITE;
    public static final String BG_PURPLE = VueMonde.BG_PURPLE;
    public static final String BG_RED    = VueMonde.BG_RED;
    public static final String BG_BLUE   = VueMonde.BG_BLUE;

    public void setBackgroundColor(String colorName) {
        vueMonde.setBackgroundColor(colorName);
    }

    // =========================================================================
    // Utilitaire image — conserve pour compatibilite (ex : appels externes)
    // =========================================================================

    public static BufferedImage rotateImage(BufferedImage img, double angle) {
        return VueMonde.rotateImage(img, angle);
    }

    // =========================================================================
    // Ticker
    // =========================================================================

    private void startTicker() {
        int timeSleep = 1000 / FPS;
        while (partieEnCours) {
            try {
                Thread.sleep(timeSleep);
                tick();
            } catch (InterruptedException e) {
                System.err.println("Ticker interrompu");
                return;
            }
        }
    }

    private void tick() {
        refreshBoardComponents();
    }

    /** Rafraichit toutes les vues a chaque frame. */
    public void refreshBoardComponents() {
        vueMonde.updateTurnIndicator(gameplay.partie.numeroTour);
        vueMonde.updateBackground(gameplay.partie.joueurCourant, gameplay.partie.coursePoursuiteActive); //BRICOLAGE + ajout BG Violet
        if (!gameplay.rotationMode) {
            vueMonde.updateDistrictView(gameplay.partie.district);
        }
        vueMonde.updateJetons(gameplay.partie.jetonsAction);
        vueMonde.updateDetectivesView(gameplay.partie.detectives);
        vueMonde.updateThinking(gameplay.enReflexion(), gameplay.partie.IaEnCours);

        vueMonde.repaintWorld();
    }

    // =========================================================================
    // Initialisation Swing
    // =========================================================================

    private void initFrame() {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
    }

    // -------------------------------------------------------------------------
    // Bande laterale Swing (WEST)
    // -------------------------------------------------------------------------

    /**
     * Cree le JPanel leftStrip positionne en BorderLayout.WEST.
     * Contient les boutons via BoxLayout vertical.
     */
    private void initLeftStrip() {
        leftStrip = new JPanel();
        leftStrip.setLayout(new BoxLayout(leftStrip, BoxLayout.Y_AXIS));
        leftStrip.setPreferredSize(new Dimension(STRIP_W_PX, WINDOW_H));
        leftStrip.setBackground(new Color(30, 30, 40));
        leftStrip.setBorder(BorderFactory.createEmptyBorder(20, 5, 10, 5));

        JButton retour = makeStripButton("Retour", "Retour.png");
        retour.addActionListener(e -> {
            if (vueMonde.jeuVerrouille) return;
            stopGameLoop();
            menuPrincipal.resetBoutonsSurvoles();
            parent.setContentPane(menuPrincipal);
            parent.revalidate();
            parent.repaint();
        });

        JButton newGame = makeStripButton("Nv. Partie", "Niv.Partie.png");
        newGame.addActionListener(e -> {
            if (vueMonde.jeuVerrouille) return;
            if (gameplay != null) gameplay.resetGame(); });

        JButton ia      = makeStripButton("IA", "IA.png");
        ia.addActionListener(e -> {
            if (vueMonde.jeuVerrouille) return;
            if (gameplay != null) gameplay.partie.lanceIa(); });

        JButton annuler = makeStripButton("Annuler", "Annuler.png");
        annuler.addActionListener(e -> {
            if (vueMonde.jeuVerrouille) return;
            if (gameplay != null) {
                gameplay.partie.annuler();
                refreshBoardComponents();
            }
        });

        JButton visibleBtn = makeStripButton("Visible", "Visible.png");
        visibleBtn.addActionListener(e -> {
            if (vueMonde.jeuVerrouille) return;  // Évite les clics pendant freeze
            if (gameplay != null) {
                gameplay.afficherVisibiliteTemporaire();
            }
        });
        leftStrip.add(visibleBtn);

        JButton refaire = makeStripButton("Refaire", "Refaire.png");
        refaire.addActionListener(e -> {
            if (vueMonde.jeuVerrouille) return;
            if (gameplay != null) {
                gameplay.partie.refaire();
                refreshBoardComponents();
            }
        });

        JButton save = makeStripButton("Save", "Save.png");
        save.addActionListener(e -> {
            if (vueMonde.jeuVerrouille) return;
            if (gameplay != null) {
                try {
                    SaveManager.save(gameplay.partie.toGameSave(), "save.dat");
                } catch (Exception ex) {
                    System.err.println("Save echoue: " + ex.getMessage());
                }
            }
        });

        JButton load = makeStripButton("Load", "Load.png");
        load.addActionListener(e -> {
            if (vueMonde.jeuVerrouille) return;
            if (gameplay != null) {
                try {
                    GameSave saveFile = SaveManager.load("save.dat");
                    gameplay.partie.fromGameSave(saveFile);
                    refreshBoardComponents();
                } catch (Exception ex) {
                    System.err.println("Load echoue: " + ex.getMessage());
                }
            }
        });

        JButton regles  = makeStripButton("Regles", "ReglesB.png");
        regles.addActionListener(e -> {
            if (vueMonde.jeuVerrouille) return;
            onReglesPressed();
        });

        leftStrip.add(retour);
        leftStrip.add(Box.createVerticalStrut(4));
        leftStrip.add(newGame);
        leftStrip.add(Box.createVerticalStrut(4));
        leftStrip.add(ia);
        leftStrip.add(Box.createVerticalStrut(4));
        leftStrip.add(annuler);
        leftStrip.add(Box.createVerticalStrut(4));
        leftStrip.add(refaire);
        leftStrip.add(Box.createVerticalStrut(4));
        leftStrip.add(save);
        leftStrip.add(Box.createVerticalStrut(4));
        leftStrip.add(load);
        leftStrip.add(Box.createVerticalStrut(4));
        leftStrip.add(regles);
        leftStrip.add(Box.createVerticalGlue());

        add(leftStrip, BorderLayout.WEST);
    }

    public void stopGameLoop() {
        partieEnCours = false;
    }

    // -------------------------------------------------------------------------
    // Zone centrale (CENTER) — VueMonde
    // -------------------------------------------------------------------------

    private void initCenterPane() {
        add(vueMonde, BorderLayout.CENTER);
    }

    // -------------------------------------------------------------------------
    // Letterbox resize listener
    // -------------------------------------------------------------------------

    /**
     * A chaque redimensionnement de la JFrame, transmet les dimensions
     * disponibles a VueMonde pour qu'il recalcule le letterbox.
     */
    private void initResizeListener() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyLetterbox();
            }
        });
    }

    private void applyLetterbox() {
        // VueMonde occupe tout le CENTER ; ses dimensions reflètent la zone disponible
        int availW = vueMonde.getWidth();
        int availH = vueMonde.getHeight();
        vueMonde.applyLetterbox(availW, availH);
        repositionnerPanneauRegles();
    }

    /**
     * Repositionne le panneau de règles pour qu'il occupe tout l'overlay.
     */
    private void repositionnerPanneauRegles() {
        if (panneauRegles != null) {
            JPanel overlay = vueMonde.getUiOverlay();
            panneauRegles.setBounds(0, 0, overlay.getWidth(), overlay.getHeight());
            panneauRegles.revalidate();
        }
    }

    // =========================================================================
    // Boutons helpers — chargement icones et creation des boutons de bande
    // =========================================================================

    /**
     * Charge une image depuis res/Images/ et la redimensionne aux dimensions cibles.
     * Suit exactement le meme modele que Camera.AddSprite().
     *
     * @param fileName nom du fichier PNG (ex : "Retour.png")
     * @param width    largeur cible en pixels
     * @param height   hauteur cible en pixels
     * @return un ImageIcon redimensionne, ou null si le fichier est introuvable
     */
    private ImageIcon loadIcon(String fileName, int width, int height) {
        File f = new File("res/Images/" + fileName);
        if (!f.exists()) {
            System.err.println("VueJeu.loadIcon — fichier introuvable : res/Images/" + fileName);
            return null;
        }
        try {
            InputStream in = new FileInputStream(f);
            BufferedImage raw = ImageIO.read(in);
            if (raw == null) {
                System.err.println("VueJeu.loadIcon — ImageIO n'a pas pu lire : " + fileName);
                return null;
            }
            Image scaled = raw.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ex) {
            System.err.println("VueJeu.loadIcon — exception sur " + fileName + " : " + ex.getMessage());
            return null;
        }
    }

    /**
     * Cree un bouton de la bande laterale.
     * Tente d'afficher l'icone PNG ; bascule sur le texte HTML si elle est indisponible.
     *
     * @param label    texte de secours (aussi utilise comme tooltip quand l'icone est presente)
     * @param pngFile  nom du fichier PNG a charger depuis res/Images/
     */
    private JButton makeStripButton(String label, String pngFile) {
        int btnW = BTN_SIZE;
        int btnH = BTN_SIZE;

        JButton btn = new JButton();
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension square = new Dimension(btnW, btnH);
        btn.setPreferredSize(square);
        btn.setMinimumSize(square);
        btn.setMaximumSize(square);

        ImageIcon icon = loadIcon(pngFile, btnW, btnH);
        if (icon != null) {
            // Mode image : icone + tooltip textuel
            btn.setIcon(icon);
            btn.setText("");
            btn.setToolTipText(label);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
        } else {
            // Mode secours : texte HTML avec style original
            btn.setText("<html><center>" + label + "</center></html>");
            btn.setFont(new Font("SansSerif", Font.BOLD, 10));
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(50, 50, 60));
            btn.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 120), 1));
            btn.setOpaque(true);
        }
        return btn;
    }

    // =========================================================================
    // Panneau de regles — affichage purement visuel, zero logique metier
    // =========================================================================

    private void onReglesPressed() {
        gameplay.partie.toggleFreeze();
        /*for (Detective d : gameplay.partie.detectives) {
            d.setPosition(0);  // 0 correspond à la case 1 (car position 0-based)
            }
       
        vueMonde.replaceOuterBall(1,1);
        vueMonde.replaceOuterBall(1,2);
        vueMonde.replaceOuterBall(1,3);
        
         gameplay.activerCoursePoursuiteTest();*/
        if (panneauRegles != null && panneauRegles.isShowing()) {
            fermerPanneauRegles();
            return;
        }

        JPanel overlay = vueMonde.getUiOverlay();

        panneauRegles = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 230));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panneauRegles.setOpaque(false);
        panneauRegles.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        String html = VueReglesPanel.getHtml();

        JLabel labelRegles = new JLabel(html);
        labelRegles.setVerticalAlignment(SwingConstants.TOP);

        JScrollPane scroll = new JScrollPane(labelRegles,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 140, 0, 180), 2));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panneauRegles.add(scroll, BorderLayout.CENTER);

        java.awt.event.MouseAdapter fermetureListener = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                fermerPanneauRegles();
            }
        };
        panneauRegles.addMouseListener(fermetureListener);
        scroll.addMouseListener(fermetureListener);
        labelRegles.addMouseListener(fermetureListener);

        overlay.add(panneauRegles, 0);
        repositionnerPanneauRegles();
        overlay.revalidate();

        // Adapter le panneau si la taille a ete mise a jour entre-temps
        vueMonde.repositionOverlayElements(overlay.getWidth(), overlay.getHeight());
        System.out.println("VueJeu — Panneau de regles affiche");
    }

    private void fermerPanneauRegles() {
        if (panneauRegles != null) {
            vueMonde.getUiOverlay().remove(panneauRegles);
            panneauRegles = null;
            vueMonde.getUiOverlay().revalidate();
            System.out.println("VueJeu — Panneau de regles ferme");
        }
    }
}