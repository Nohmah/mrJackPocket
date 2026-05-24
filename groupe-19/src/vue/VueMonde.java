package src.vue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import src.modele.*;

/**
 * VueMonde — Panneau de rendu du monde de jeu.
 *
 * Responsabilite UNIQUE : rendu visuel du plateau.
 *   - Encapsule le JLayeredPane (Camera + uiOverlay).
 *   - Gere tous les Composant2D (tuiles, jetons, detectives, anneau).
 *   - Expose des methodes de mise a jour visuelle appelees par VueJeu / Gameplay.
 *   - Conserve la logique letterbox et le calcul de currentScale.
 *
 * Ce que VueMonde NE fait pas :
 *   - Aucune logique metier.
 *   - Aucune reference directe a Partie ou Gameplay.
 *
 * Structure interne :
 *   JLayeredPane
 *     ├─ Camera    (couche DEFAULT)
 *     └─ uiOverlay (couche PALETTE, null-layout, transparent)
 */
public class VueMonde extends JPanel {

    /** true si il ne devrait plus être possible de cliquer sur les boutons, car par exemple il y a une animation en cours
     * Il faut étendre cette variable partout où c'est nécessaire**/
    public boolean jeuVerrouille = false;
    private JPanel alibiPanel;
    // =========================================================================
    // Constantes monde (proprietaires de VueMonde)
    // =========================================================================

    /** Largeur de reference du monde (pixels logiques). */
    public static final int     WORLD_W       = 1200;
    /** Hauteur de reference du monde (pixels logiques). */
    public static final int     WORLD_H       = 800;

    /** Largeur en pixels de la bande laterale (utilisee pour positionner les elements monde). */
    public static final double  STRIP_W       = 100.0;

    /** Cote d'une tuile en pixels logiques. */
    public static final double  TILE_SIZE     = 150.0;

    /** Taille du plateau (3 tuiles). */
    private static final double BOARD_SIZE    = 450.0;

    /** Origine du plateau (coin haut-gauche). */
    public static final Vector2 BOARD_ORIGIN  = new Vector2(
            (WORLD_W - BOARD_SIZE) / 2.0 - 60,
            (WORLD_H - BOARD_SIZE) / 2.0
    );
    private JLabel centerMessage;
    private JPanel centerMessagePanel;
    private JPanel currentOverlayPanel = null;

    public void showCenterMessage(String text) {
//        if (centerMessagePanel != null) {
//            uiOverlay.remove(centerMessagePanel);
//        }
//
//        jeuVerrouille = true;
//
//        centerMessagePanel = new JPanel(new GridBagLayout());
//        centerMessagePanel.setOpaque(false);
//        centerMessagePanel.setBounds(0, 0, WORLD_W, WORLD_H);
//
//        centerMessage = new JLabel(text, SwingConstants.CENTER);
//        centerMessage.setFont(new Font("SansSerif", Font.BOLD, 48));
//        centerMessage.setForeground(Color.WHITE);
//        centerMessage.setOpaque(true);
//        centerMessage.setBackground(new Color(0, 0, 0, 180));
//        centerMessage.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
//
//        centerMessagePanel.add(centerMessage);
//
//        uiOverlay.add(centerMessagePanel, JLayeredPane.POPUP_LAYER);
//        uiOverlay.revalidate();
//        uiOverlay.repaint();
//
//        new javax.swing.Timer(2000, e -> {
//            uiOverlay.remove(centerMessagePanel);
//            centerMessagePanel = null;
//            centerMessage = null;
//
//            jeuVerrouille = false;
//            uiOverlay.revalidate();
//            uiOverlay.repaint();
//        }) {{
//            setRepeats(false);
//            start();
//        }};
    }

    // =========================================================================
    // Overlay générique pour affichage d'image avec texte sous l'image
    // =========================================================================

    /**
     * Affiche une image en plein écran (overlay) avec un texte optionnel en dessous,
     * bloque toutes les actions (jeuVerrouille = true) pendant la durée spécifiée.
     *
     * @param nomImage        Nom de la sprite à afficher (ex: "VictoireJ").
     * @param texteSousImage  Texte à afficher sous l'image (peut être null).
     * @param dureeMs         Durée d'affichage en millisecondes.
     */
    private void afficherOverlayAvecImage(String nomImage, String texteSousImage, int dureeMs) {
    // Supprime overlay précédent existe
        if (currentOverlayPanel != null) {
            uiOverlay.remove(currentOverlayPanel);
            currentOverlayPanel = null;
        }

        jeuVerrouille = true;

        Camera.AddSprite(nomImage);
        BufferedImage img = Camera.GetSpriteImage(nomImage);

        JPanel overlayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // Fond semi-transparent
                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRect(0, 0, getWidth(), getHeight());

                BufferedImage currentImg = Camera.GetSpriteImage(nomImage);
                if (currentImg == null) currentImg = img;

                if (currentImg != null) {
                    int imgW = currentImg.getWidth();
                    int imgH = currentImg.getHeight();
                    int x = (getWidth() - imgW) / 2;
                    int y = (getHeight() - imgH) / 2;

                    if (texteSousImage != null && !texteSousImage.isEmpty()) {
                        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
                        FontMetrics fm = g2.getFontMetrics();
                        int textH = fm.getHeight();
                        y = (getHeight() - imgH - textH - 30) / 2;
                    }

                    g2.drawImage(currentImg, x, y, imgW, imgH, null);

                    if (texteSousImage != null && !texteSousImage.isEmpty()) {
                        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
                        g2.setColor(Color.WHITE);
                        FontMetrics fm = g2.getFontMetrics();
                        int textX = (getWidth() - fm.stringWidth(texteSousImage)) / 2;
                        int textY = y + imgH + fm.getHeight() + 10;
                        g2.drawString(texteSousImage, textX, textY);
                    }
                } else {
                    String fallback = (texteSousImage != null) ? texteSousImage : nomImage;
                    g2.setFont(new Font("SansSerif", Font.BOLD, 48));
                    g2.setColor(Color.WHITE);
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = (getWidth() - fm.stringWidth(fallback)) / 2;
                    int textY = (getHeight() - fm.getHeight()) / 2;
                    g2.drawString(fallback, textX, textY);
                }
            }
        };
        overlayPanel.setOpaque(false);

        // Dimensions initiales : utiliser la taille réelle de uiOverlay si disponible
        int w = uiOverlay.getWidth();
        int h = uiOverlay.getHeight();
        if (w <= 0 || h <= 0) {
            java.awt.Container parent = uiOverlay.getParent();
            if (parent != null) {
                w = parent.getWidth();
                h = parent.getHeight();
            }
        }
        if (w <= 0 || h <= 0) {
            w = WORLD_W;
            h = WORLD_H;
        }
        overlayPanel.setBounds(0, 0, w, h);
        uiOverlay.add(overlayPanel, JLayeredPane.POPUP_LAYER);
        currentOverlayPanel = overlayPanel;   
        uiOverlay.revalidate();
        uiOverlay.repaint();

        // Suppression automatique après la durée demandée
        new javax.swing.Timer(dureeMs, e -> {
            uiOverlay.remove(overlayPanel);
            if (currentOverlayPanel == overlayPanel) currentOverlayPanel = null;
            jeuVerrouille = false;
            uiOverlay.revalidate();
            uiOverlay.repaint();
        }).start();
    }
            // Méthode afficherCarteAlibi mise à jour
        public void afficherCarteAlibi(Personnage personnage) {
            String spriteName = personnage.image;
            afficherOverlayAvecImage(spriteName, null, 2000);
        }

        // Méthode showGameOverScreen mise à jour
        public void showGameOverScreen(String vainqueur) {
            String imageName;
            if (vainqueur != null && vainqueur.toLowerCase().contains("jack")) {
                imageName = "VictoireJ";
                String message = "Jack l'emporte";
                afficherOverlayAvecImage(imageName, message, 3000);
            } else {
                imageName = "VictoireE";
                String message = "Victoire de Sherlock Holmes";
                afficherOverlayAvecImage(imageName, message, 3000);
            }
        }

    // -------------------------------------------------------------------------
    // Anneau OuterBall
    // -------------------------------------------------------------------------

    private static final double    OUTER_BALL_DIAM    = 80.0;
    private static final Vector2[] OUTER_POSITIONS    = buildOuterPositions();

    private static final double DETECTIVE_SCALE_1 = 1.0;
    private static final double DETECTIVE_SCALE_2 = 0.9;
    private static final double DETECTIVE_SCALE_3 = 0.8;

    private static Vector2[] buildOuterPositions() {
        double gap = OUTER_BALL_DIAM / 2.0 + 5;
        Vector2[] pos = new Vector2[12];
        for (int i = 0; i < 3; i++)
            pos[i] = new Vector2(BOARD_ORIGIN.x + i * TILE_SIZE + TILE_SIZE / 2.0, BOARD_ORIGIN.y - gap);
        for (int i = 0; i < 3; i++)
            pos[3 + i] = new Vector2(BOARD_ORIGIN.x + BOARD_SIZE + gap, BOARD_ORIGIN.y + i * TILE_SIZE + TILE_SIZE / 2.0);
        for (int i = 0; i < 3; i++)
            pos[6 + i] = new Vector2(BOARD_ORIGIN.x + (2 - i) * TILE_SIZE + TILE_SIZE / 2.0, BOARD_ORIGIN.y + BOARD_SIZE + gap);
        for (int i = 0; i < 3; i++)
            pos[9 + i] = new Vector2(BOARD_ORIGIN.x - gap, BOARD_ORIGIN.y + (2 - i) * TILE_SIZE + TILE_SIZE / 2.0);
        return pos;
    }

    // =========================================================================
    // Sprites — noms de faces (boules d'action, indicateurs de tour)
    // =========================================================================

    private static final String[] BALL_FACE_PILE = { "action_alibi", "action_toby", "action_rotation", "action_rotation" };
    private static final String[] BALL_FACE_FACE = { "action_holmes", "action_watson", "action_echange", "action_joker" };
    private static final String[] TURN_FACE_PILE = { "T1","T2","T3","T4","T5","T6","T7","T8" };
    private static final String   TURN_FACE_FACE = "T0";

    // =========================================================================
    // Etat interne — faces
    // =========================================================================

    private final boolean[]  ballFaceIsPile          = { true, true, true, true };
    private final String[]   actionBallCurrentSprite = new String[4];
    /** Vrai si le jeton i a été utilisé (sprite grisé), faux sinon. */
    private final boolean[]  actionBallIsUsed        = { false, false, false, false };
    private final boolean[]  turnFaceIsPile           = { true, true, true, true, true, true, true, true };

    // =========================================================================
    // Etat interne — detectives
    // =========================================================================

    private final int[]         detectivePosition    = { -1, -1, -1 };
    private final Composant2D[] detectiveComponents  = new Composant2D[3];

    // =========================================================================
    // Composant2D vivants
    // =========================================================================

    private final Composant2D[][]  tileComponents     = new Composant2D[3][3];
    private final int[][]          tileRotations      = new int[3][3];
    private final String[][]       tileBaseNames      = new String[3][3];
    private final Composant2D[]    actionBalls        = new Composant2D[4];
    private final Composant2D[]    turnIndicators     = new Composant2D[8];
    private final Composant2D[]    outerBalls         = new Composant2D[12];

    // =========================================================================
    // État des rectangles joueurs (rendu vectoriel dans la Camera)
    // =========================================================================

    /** Noms affichés dans les rectangles joueurs (index 0 = Enquêteur, 1 = Jack). */
    private final String[] playerNames = { "", "" };

    /** Types affichés sous les noms (ex: "Humain", "IA Facile"). */
    private final String[] playerTypes = { "", "" };

    /** Couleurs des rectangles joueurs (gérées par updatePlayerRectangles). */
    private final Color[] playerColors = {
        new Color(40,  80, 180, 200),   // Enquêteur (bleu)
        new Color(180, 40,  40, 200)    // Jack (rouge)
    };

    /** Cache de sprites tournes (cle = "nom_angle"). */
    private final Map<String, Integer> rotatedSpriteCache = new HashMap<>();

    // =========================================================================
    // Composants Swing internes
    // =========================================================================

    /** Conteneur empilant Camera + uiOverlay. */
    private final JLayeredPane layeredPane;

    /** Calque transparent pour les indicateurs joueurs et le panneau de regles. */
    private JPanel     uiOverlay;
    private JLabel     gameOverLabel = null;

    private JLabel     sablierLabel = null;
    private JPanel sablierPanel = null;
    // Panneau indiquant que l'IA réfléchit (haut-gauche) ou joueur en réseau
    private JPanel thinkingPanel = null;
    private JLabel thinkingLabel = null;

    //Label pour info bulle jeton action.
    private JLabel actionTooltip;

    // =========================================================================
    // Comportement du panneau de reflexion
    // =========================================================================

    // Tant que le jeu n'est pas en réseau, c'est l'IA qui réfléchit par défaut

    private boolean iaIsThinking = true;

    // =========================================================================
    // Letterbox / scale
    // =========================================================================

    /** Scale courant calcule lors du redimensionnement. */
    private double currentScale = 1.0;

    // =========================================================================
    // Callback vers VueJeu pour les clics sur les boules d'action
    // =========================================================================

    /** Interface fonctionnelle pour remonter les clics vers le mediateur. */
    public interface ActionBallClickListener {
        void onActionBallHit(int ballIndex, String spriteName);
    }

    private ActionBallClickListener actionBallClickListener;

    public void setActionBallClickListener(ActionBallClickListener listener) {
        this.actionBallClickListener = listener;
    }

    // =========================================================================
    // Etat survol — feedback visuel
    // =========================================================================

    /** Index du jeton d'action actuellement survole (-1 = aucun). */
    private int hoveredTokenIndex = -1;

    //Pour clignotement des déplacements possibles des détectives.
    private float clignotePhase = 0.0f;
    private final Timer timerClignote = new Timer(33, e -> {
        clignotePhase += 0.08f;
        if(clignotePhase > Math.PI * 2) clignotePhase -= (float) (Math.PI * 2);
        Camera.Repaint();
    });

    // =========================================================================
    // Etat selected — feedback visuel
    // =========================================================================
    private int selectedRow = -1;
    private int selectedCol = -1;

    public void setTuileSelected(int row, int col){
        selectedRow = row;
        selectedCol = col;
        Camera.Repaint();
    }

    /**
     * Active l'assombrissement global en exemptant les tuiles visibles et les détectives.
     * @param masqueTuiles tableau 3×3 — true = tuile visible (exemptée du dimming)
     */
    public void appliquerAssombrissement(boolean[][] masqueTuiles) {
        java.util.List<Composant2D> composantsAExempter = new java.util.ArrayList<>();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (masqueTuiles[row][col] && tileComponents[row][col] != null) {
                    composantsAExempter.add(tileComponents[row][col]);
                }
            }
        }

        for (Composant2D det : detectiveComponents) {
            if (det != null) composantsAExempter.add(det);
        }

        Camera.ActiverDimming(composantsAExempter);
    }

    /** Désactive l'assombrissement global. */
    public void retirerAssombrissement() {
        Camera.DesactiverDimming();
    }

    /**
     * Retourne l'index (0-3) de la boule d'action sous les coordonnees ecran
     * donnees, ou -1 si aucune boule n'est touchee.
     * Methode intentionnellement publique pour IHMControler.
     */
    public int actionBallAt(int sx, int sy) {
        Vector2 worldPos = toWorld(new Vector2(sx, sy));
        for (int i = 0; i < actionBalls.length; i++) {
            Composant2D ball = actionBalls[i];
            if (ball == null) continue;
            double rx = (ball.taille.x * ball.echelle.x) / 2.0;
            double ry = (ball.taille.y * ball.echelle.y) / 2.0;
            double r  = Math.min(rx, ry);
            double dx = worldPos.x - ball.position.x;
            double dy = worldPos.y - ball.position.y;
            if (dx * dx + dy * dy <= r * r) return i;
        }
        return -1;
    }

    /**
     * Definit le jeton survole et rafraichit l'affichage.
     * Appele par Gameplay (lui-meme notifie par IHMControler).
     *
     * @param index Index 0-3, ou -1 pour annuler le survol.
     */
    public void setHoveredToken(int index) {
        if (actionTooltip == null) return;

        if (index < 0 || actionBallIsUsed[index]) {
            actionTooltip.setVisible(false);
        } else {
            String sprite = actionBallCurrentSprite[index];
            String text = getActionTooltipText(sprite);
            if (text.isEmpty()) {
                actionTooltip.setVisible(false);
            } else {
                actionTooltip.setText(text);
                actionTooltip.setSize(actionTooltip.getPreferredSize());

                Point p = getActionBallScreenPos(index);
                int x = p.x + 20;
                int y = p.y - 40;
                actionTooltip.setLocation(x, y);
                actionTooltip.setVisible(true);
            }
        }
        hoveredTokenIndex = index;
        Camera.Repaint();
    }

    // =========================================================================
    // Constructeur
    // =========================================================================

    public VueMonde() {
        super(new BorderLayout());
        setBackground(Color.BLACK);
        setOpaque(true);

        layeredPane = new JLayeredPane();
        layeredPane.setBackground(Color.BLACK);
        layeredPane.setOpaque(true);
        add(layeredPane, BorderLayout.CENTER);

        initCamera();
        initComponents();
        initUIOverlay();
        timerClignote.start();
    }

    // =========================================================================
    // Initialisation interne
    // =========================================================================

    private void initCamera() {
        Camera cam = new Camera(new Vector2(WORLD_W, WORLD_H), null) {
            @Override
            public void paintComponent(Graphics g) {
                ((Graphics2D) g).setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                super.paintComponent(g);
                paintHoverOverlay((Graphics2D) g);
                paintSelectedOverlay((Graphics2D) g);
                paintPlayerRects((Graphics2D) g);
            }
        };
        cam.setBounds(0, 0, WORLD_W, WORLD_H);
        layeredPane.add(cam, JLayeredPane.DEFAULT_LAYER);

        // Adaptateur leger : convertit clic ecran → monde, hitTest sur les boules
        cam.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                notifyActionBallClick(e.getX(), e.getY());
            }
        });
    }

    private void initComponents() {
        initTileComponents();
        initActionBalls();
        initTurnIndicators();
    }

    private void initTileComponents() {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++) {
                Vector2 center = new Vector2(
                        BOARD_ORIGIN.x + col * TILE_SIZE + TILE_SIZE / 2.0,
                        BOARD_ORIGIN.y + row * TILE_SIZE + TILE_SIZE / 2.0);
                tileComponents[row][col] = new Composant2D(
                        center, new Vector2(TILE_SIZE, TILE_SIZE), "TileDefault");
            }
    }

    private void initActionBalls() {
        double diam = 100, cx = STRIP_W + diam / 2.0 - 50;
        double spacing = BOARD_SIZE / BALL_FACE_PILE.length;
        for (int i = 0; i < BALL_FACE_PILE.length; i++) {
            double cy = BOARD_ORIGIN.y + spacing / 2.0 + i * spacing;
            actionBalls[i] = new Composant2D(
                    new Vector2(cx, cy), new Vector2(diam, diam), BALL_FACE_PILE[i]);
            actionBallCurrentSprite[i] = BALL_FACE_PILE[i];
        }
    }

    private void initTurnIndicators() {
        double diam    = 60;
        double radius  = diam / 2.0;
        double cx      = WORLD_W - radius - 20 - (1.25 * radius) - 100;
        double spacing = BOARD_SIZE / 8.0;
        for (int i = 0; i < 8; i++) {
            double cy = BOARD_ORIGIN.y + spacing / 2.0 + i * spacing;
            turnIndicators[i] = new Composant2D(
                    new Vector2(cx, cy), new Vector2(diam, diam), TURN_FACE_PILE[i]);
        }
    }

    private void initUIOverlay() {
        uiOverlay = new JPanel(null);
        uiOverlay.setOpaque(false);
        uiOverlay.setBounds(0, 0, WORLD_W, WORLD_H);
        layeredPane.add(uiOverlay, JLayeredPane.PALETTE_LAYER);

        actionTooltip = new JLabel("", SwingConstants.LEFT);
        actionTooltip.setOpaque(true);
        actionTooltip.setBackground(new Color( 0,0,0,100));
        actionTooltip.setFont(new Font("SansSerif", Font.PLAIN,12));
        actionTooltip.setForeground(new Color(255,240, 180));
        actionTooltip.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
        actionTooltip.setVisible(false);
        uiOverlay.add(actionTooltip);

        initSablierLabel();
        initThinking();
    }

    private void initThinking(){
        thinkingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        thinkingPanel.setOpaque(true);
        thinkingPanel.setBackground(new Color(0, 0, 0, 160));
        thinkingPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200, 120), 1));

        thinkingLabel = new JLabel("");
        thinkingLabel.setForeground(Color.WHITE);
        thinkingLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        thinkingPanel.add(thinkingLabel);

        uiOverlay.add(thinkingPanel);
        thinkingPanel.setPreferredSize(new Dimension(320, 28));
        thinkingPanel.setBounds(10, 10, 220, 28);
        thinkingPanel.setVisible(false);
    }


    private void initSablierLabel() {
        // Panneau personnalisé pour l'icône T0
        JPanel turnIndicatorPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                BufferedImage img = Camera.GetSpriteImage("T0");
                if (img != null) {
                    g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 20));
                    g2.drawString("T0", getWidth() / 2 - 10, getHeight() / 2 + 7);
                }
            }
        };
        turnIndicatorPanel.setPreferredSize(new Dimension(50, 50));
        turnIndicatorPanel.setOpaque(false);

        // Chargement de l'image T0 si absente
        if (Camera.GetSpriteImage("T0") == null) {
            Camera.AddSprite("T0");
        }

        // Label du compteur de sabliers
        sablierLabel = new JLabel("0/6", SwingConstants.CENTER);
        sablierLabel.setForeground(Color.WHITE);
        sablierLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        sablierLabel.setOpaque(true);
        sablierLabel.setBackground(new Color(0, 0, 0, 180));
        sablierLabel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        sablierLabel.setPreferredSize(new Dimension(70, 50));

        // Conteneur horizontal
        sablierPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        sablierPanel.setOpaque(false);
        sablierPanel.add(turnIndicatorPanel);
        sablierPanel.add(sablierLabel);

        uiOverlay.add(sablierPanel);
    }

    // =========================================================================
    // Acces au JLayeredPane (pour VueJeu : registerControler + letterbox)
    // =========================================================================

    /** Retourne le JLayeredPane interne (Camera + overlay). */
    public JLayeredPane getLayeredPane() {
        return layeredPane;
    }

    /** Retourne le panel uiOverlay (pour VueJeu : repaint, panneauRegles). */
    public JPanel getUiOverlay() {
        return uiOverlay;
    }

    // =========================================================================
    // Letterbox — appelee par VueJeu depuis son ComponentListener
    // =========================================================================

    /**
     * Applique la transformation letterbox sur la Camera et l'uiOverlay.
     * Calcule le scale uniforme a partir des dimensions disponibles.
     *
     * @param availW Largeur disponible dans la zone centrale (sans leftStrip).
     * @param availH Hauteur disponible.
     */
    public void applyLetterbox(int availW, int availH) {
    if (availW <= 0 || availH <= 0) return;

    double scaleX = (double) availW / WORLD_W;
    double scaleY = (double) availH / WORLD_H;
    currentScale = Math.min(scaleX, scaleY);

    int camW = (int) Math.round(WORLD_W * currentScale);
    int camH = (int) Math.round(WORLD_H * currentScale);
    int camX = (availW - camW) / 2;
    int camY = (availH - camH) / 2;

    // Repositionner la Camera
    Component[] defaultLayer = layeredPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER);
    if (defaultLayer.length > 0) {
        defaultLayer[0].setBounds(camX, camY, camW, camH);
    }

    // Repositionner l'uiOverlay
    Component[] paletteLayer = layeredPane.getComponentsInLayer(JLayeredPane.PALETTE_LAYER);
    if (paletteLayer.length > 0) {
        paletteLayer[0].setBounds(camX, camY, camW, camH);
    }

    //mettre à jour Camera.taille avec la taille RÉELLE (pas WORLD_W)
    Camera.taille = new Vector2(camW, camH);

    // Appliquer le zoom
    Camera.SetZoomCamera(new Vector2(currentScale, currentScale));

    repositionOverlayElements(camW, camH);
    }

    /** Retourne le scale courant (utilise par VueJeu si necessaire). */
    public double getCurrentScale() {
        return currentScale;
    }

    // =========================================================================
    // Mise a jour visuelle (API appelee par VueJeu / Gameplay)
    // =========================================================================

    /** Met à jour l'affichage des sabliers de Jack. */
    public void updateSabliers(int sabliers, int maxSabliers) {
        if (sablierLabel == null) return;
        sablierLabel.setText(sabliers + "/" + maxSabliers);
        sablierLabel.setBackground(new Color(180, 0, 0, 200));
        sablierLabel.setForeground(Color.RED);
        sablierLabel.repaint();
    }

    /** Met a jour les 9 tuiles du plateau selon l'etat du district. */
    public void updateDistrictView(District district) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Quartier q = district.get(i, j);
                String baseName;
                if (q.estFaceSuspect()) {
                    baseName = q.getPersonnage().nom.replace(" ", "");
                } else {
                    baseName = (q.getPersonnage() == Personnage.JOSEPH_LANE)
                            ? "JosephLane-verso" : "TileDefault";
                }
                if (!baseName.equals(tileBaseNames[i][j])) {
                    tileBaseNames[i][j] = baseName;
                    tileRotations[i][j] = 0;
                    tileComponents[i][j].spriteId = Camera.AddSprite(baseName);
                }
                int angle = switch (q.getOrientationMur()) {
                    case SUD   -> 0;
                    case EST   -> -90;
                    case NORD  -> 180;
                    case OUEST -> 90;
                    default    -> 0;
                };
                int delta = (angle - tileRotations[i][j]) % 360;
                if (delta < 0) delta += 360;
                if (delta != 0) rotateTile(i, j, delta);
            }
        }
    }

    /** Met a jour les 4 jetons d'action selon leur face visible. */
    public void updateJetons(List<JetonAction> jetons) {
        for (int i = 0; i < 4 && i < actionBalls.length; i++) {
            JetonAction j = jetons.get(i);
            src.modele.Action a = j.getActionVisible();
            String earlyName = switch (a) {
                case HOLMES   -> "action_holmes";
                case WATSON   -> "action_watson";
                case TOBY     -> "action_toby";
                case JOKER    -> "action_joker";
                case ROTATION -> "action_rotation";
                case ECHANGE  -> "action_echange";
                case ALIBI    -> "action_alibi";
            };
            boolean joue = j.isJoue();
            String spriteName = joue ? earlyName + "_grisee" : earlyName;
            actionBalls[i].spriteId = Camera.AddSprite(spriteName);
            actionBallCurrentSprite[i] = spriteName;
            actionBallIsUsed[i] = joue;
        }
    }

    /** Met a jour l'affichage des 3 detectives a partir des positions du modele. */
    public void updateDetectivesView(List<Detective> detectives) {
        for (Detective d : detectives) {
            int position = d.getPosition() + 1;
            int num = switch (d.getType()) {
                case HOLMES -> 1;
                case WATSON -> 2;
                case TOBY   -> 3;
            };
            replaceOuterBall(position, num);
        }
    }

    /** Met en evidence l'indicateur de tour courant. */
    public void updateTurnIndicator(int turn) {
        for(int i = 0; i < 8; i++){
            boolean doitEtreFace = (i < turn - 1);
            if(isTurnFacePile(i) == doitEtreFace){
                switchTurnFace(i);
            }
        }
        for (int i = 0; i < turnIndicators.length; i++) {
            if (turnIndicators[i] == null) continue;
            turnIndicators[i].echelle = (i == turn - 1)
                    ? new Vector2(1.3, 1.3) : new Vector2(1.0, 1.0);
        }
    }

    // À ajouter dans VueMonde.java (après les autres méthodes)

    /**
     * Met à jour uniquement les deux rectangles joueurs selon le joueur courant.
     * Le joueur actif a un rectangle coloré, l'inactif devient gris.
     */
    public void updatePlayerRectangles(Joueur joueurCourant) {
        if (joueurCourant == Joueur.JACK) {
            updateRectColor(0, Color.GRAY);
            updateRectColor(1, new Color(180, 40, 40, 200));
        } else {
            updateRectColor(1, Color.GRAY);
            updateRectColor(0, new Color(40, 80, 180, 200));
        }
    }

    /**
     * Ancienne méthode maintenue pour compatibilité.
     * Appelle la nouvelle méthode avec coursePoursuiteActive = false.
     */
    public void updateBackground(Joueur joueurCourant) {
        updateBackground(joueurCourant, false);
    }

    /**
     * Nouvelle méthode : met à jour le fond d'écran (violet si course poursuite,
     * sinon rouge/bleu selon le joueur) et met à jour les rectangles.
     */
    public void updateBackground(Joueur joueurCourant, boolean coursePoursuiteActive) {
        updatePlayerRectangles(joueurCourant);
        String couleurFond;
        if (coursePoursuiteActive) {
            couleurFond = BG_PURPLE;
        } else {
            couleurFond = (joueurCourant == Joueur.JACK) ? BG_RED : BG_BLUE;
        }
        setBackgroundColor(couleurFond);
    }

    /** Bascule la face d'un indicateur de tour (Pile <-> Face). */
    public void switchTurnFace(int turnIndex) {
        if (turnIndex < 0 || turnIndex >= 8) return;
        turnFaceIsPile[turnIndex] = !turnFaceIsPile[turnIndex];
        String sprite = turnFaceIsPile[turnIndex] ? TURN_FACE_PILE[turnIndex] : TURN_FACE_FACE;
        turnIndicators[turnIndex].spriteId = Camera.AddSprite(sprite);
    }

    public boolean isTurnFacePile(int turnIndex) {
        if (turnIndex < 0 || turnIndex >= 8) return false;
        return turnFaceIsPile[turnIndex];
    }

    /** Bascule la face d'une boule d'action (Pile <-> Face). */
    public void switchBallFace(int ballIndex) {
        if (ballIndex < 0 || ballIndex >= 4) return;
        ballFaceIsPile[ballIndex] = !ballFaceIsPile[ballIndex];
        String sprite = ballFaceIsPile[ballIndex] ? BALL_FACE_PILE[ballIndex] : BALL_FACE_FACE[ballIndex];
        actionBalls[ballIndex].spriteId = Camera.AddSprite(sprite);
        actionBallCurrentSprite[ballIndex] = sprite;
    }

    /** Modifie la couleur du rectangle vectoriel d'un joueur. */
    public void updateRectColor(int player, Color color) {
        if (player < 0 || player >= 2) return;
        playerColors[player] = color;
        // Pas de repaint forcé : le cycle de rendu Camera s'en charge.
    }

    public void setPlayerName(int player, String newName) {
        if (player >= 0 && player < 2) {
            playerNames[player] = (newName != null) ? newName : "";
        }
    }

    public void setPlayerType(int player, String newType) {
        if (player >= 0 && player < 2) {
            playerTypes[player] = (newType != null) ? newType : "";
        }
    }

    public void hideGameOverScreen() {
        if (gameOverLabel != null) {
            uiOverlay.remove(gameOverLabel);
            gameOverLabel = null;
        }
    }

    /** Repeint la camera et l'overlay. */
    public void repaintWorld() {
        Camera.Repaint();
        uiOverlay.repaint();
    }

    public void updateThinking(boolean enReflexion, boolean iaEnJeo){
        this.iaIsThinking = iaEnJeo;
        if(iaIsThinking){
            thinkingLabel.setText("IA en train de chercher un coup...");
        }else{
            thinkingLabel.setText("Votre adversaire est entrain de réfléchir...");
        }
        if(enReflexion) showThinking();
        else hideThinking();
    }
    /** Affiche le panneau "IA en train de réfléchir". */
    public void showThinking() {
        if (thinkingPanel == null) return;
        //repositionOverlayElements(uiOverlay.getWidth(), uiOverlay.getHeight());
        thinkingPanel.setLocation(10, 10);
        thinkingPanel.setVisible(true);
        uiOverlay.revalidate();
        uiOverlay.repaint();
    }

    /** Masque le panneau "IA en train de réfléchir". */
    public void hideThinking() {
        if (thinkingPanel == null) return;
        thinkingPanel.setVisible(false);
        uiOverlay.revalidate();
        uiOverlay.repaint();
    }

    // =========================================================================
    // Manipulation des tuiles
    // =========================================================================

    /** Echange deux tuiles visuellement (positions col/row). */
    public void swapTiles(Vector2 pos1, Vector2 pos2) {
        int r1 = (int) pos1.y, c1 = (int) pos1.x;
        int r2 = (int) pos2.y, c2 = (int) pos2.x;
        if (r1 < 0 || r1 > 2 || c1 < 0 || c1 > 2 || r2 < 0 || r2 > 2 || c2 < 0 || c2 > 2) return;
        if (r1 == r2 && c1 == c2) return;

        int    tmpS = tileComponents[r1][c1].spriteId; tileComponents[r1][c1].spriteId = tileComponents[r2][c2].spriteId; tileComponents[r2][c2].spriteId = tmpS;
        String tmpB = tileBaseNames[r1][c1];           tileBaseNames[r1][c1] = tileBaseNames[r2][c2];                     tileBaseNames[r2][c2] = tmpB;
        int    tmpR = tileRotations[r1][c1];           tileRotations[r1][c1] = tileRotations[r2][c2];                     tileRotations[r2][c2] = tmpR;
        Camera.RecalculateZoom();
    }

    /** Fait pivoter une tuile d'un angle supplementaire (multiples de 90 deg). */
    public void rotateTile(int row, int col, int angleToAdd) {
        if (row < 0 || row > 2 || col < 0 || col > 2) return;
        int newAngle = ((tileRotations[row][col] + angleToAdd) % 360 + 360) % 360;
        tileRotations[row][col] = newAngle;
        String baseName = tileBaseNames[row][col];
        if (newAngle == 0) {
            tileComponents[row][col].spriteId = Camera.AddSprite(baseName);
            Camera.RecalculateZoom();
            return;
        }
        String newName = baseName + "_" + newAngle;
        int spriteId = Camera.GetSpriteId(newName);
        if (spriteId == -1) {
            BufferedImage original = Camera.GetSpriteImage(baseName);
            if (original == null) { Camera.AddSprite(baseName); original = Camera.GetSpriteImage(baseName); }
            if (original == null) return;
            spriteId = Camera.AddSprite(newName, rotateImage(original, Math.toRadians(newAngle)));
        }
        tileComponents[row][col].spriteId = spriteId;
        Camera.RecalculateZoom();
    }

    // =========================================================================
    // Detectives sur l'anneau
    // =========================================================================

    /** Deplace un detective sur l'anneau (position 1-12). */
    public void replaceOuterBall(int position, int detectiveNum) {
        int idx    = position - 1;
        int detIdx = detectiveNum - 1;
        if (idx < 0 || idx >= 12 || detIdx < 0 || detIdx >= 3) return;

        int oldPos = detectivePosition[detIdx];
        detectivePosition[detIdx] = idx;

        String sprite = "detective" + detectiveNum;
        if (detectiveComponents[detIdx] == null) {
            detectiveComponents[detIdx] = new Composant2D(
                    OUTER_POSITIONS[idx], new Vector2(OUTER_BALL_DIAM, OUTER_BALL_DIAM), sprite);
        } else {
            detectiveComponents[detIdx].position = OUTER_POSITIONS[idx];
            detectiveComponents[detIdx].spriteId = Camera.AddSprite(sprite);
        }
        if (oldPos != -1) refreshOuterBallScale(oldPos);
        refreshOuterBallScale(idx);
    }

    private void refreshOuterBallScale(int posIdx) {
        int stackCount = 0;
        int[] onCell = new int[3];
        for (int d = 0; d < 3; d++)
            if (detectivePosition[d] == posIdx && detectiveComponents[d] != null)
                onCell[stackCount++] = d;
        Vector2[] offsets = getStackOffsets(stackCount);
        double scale = getStackScale(stackCount);
        for (int k = 0; k < stackCount; k++){
            detectiveComponents[onCell[k]].echelle = new Vector2(scale, scale);
            detectiveComponents[onCell[k]].position = OUTER_POSITIONS[posIdx].Add(offsets[k]);
        }
    }

    /**
     * Retourne l'échelle à utiliser pour afficher les detectives lorsqu'ils sont empilés.
     * */
    private double getStackScale(int stackCount){
        if(stackCount >= 3) return DETECTIVE_SCALE_3;
        if(stackCount == 2) return DETECTIVE_SCALE_2;
        return DETECTIVE_SCALE_1;
    }

    /**
     *  Retourne le décalage à utiliser pour afficher les detectives lorsqu'ils sont empilés.
     * */
    private Vector2[] getStackOffsets(int stackCount){
        double delta = OUTER_BALL_DIAM * 0.4;
        //affiche les detectives côte à côte.
        if(stackCount == 2){
            return new Vector2[] { new Vector2(-delta, 0), new Vector2(delta, 0)};
        }
        //affiche les detectives en triangle.
        if(stackCount >= 3){
            return new Vector2[] {
                    new Vector2(0, -delta),
                    new Vector2(-delta, delta * 0.6),
                    new Vector2(delta, delta * 0.6)
            };
        }
        //Si seul, pas de décalage.
        return new Vector2[] { new Vector2(0,0)};
    }

    // =========================================================================
    // Fond d'ecran
    // =========================================================================

    public static final String BG_WHITE  = "BackgroundWhite";
    public static final String BG_PURPLE = "BackgroundPurple";
    public static final String BG_RED    = "BackgroundRed";
    public static final String BG_BLUE   = "BackgroundBlue";

    public void setBackgroundColor(String colorName) {
        Camera.setBackgroundImage(colorName);
    }

    // =========================================================================
    // Utilitaire image
    // =========================================================================

    public static BufferedImage rotateImage(BufferedImage img, double angle) {
        if (img == null) return null;
        int w = img.getWidth(), h = img.getHeight();
        double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
        int nw = (int) Math.floor(w * cos + h * sin);
        int nh = (int) Math.floor(h * cos + w * sin);
        BufferedImage rot = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = rot.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
        at.translate((nw - w) / 2.0, (nh - h) / 2.0);
        at.rotate(angle, w / 2.0, h / 2.0);
        g2.drawRenderedImage(img, at);
        g2.dispose();
        return rot;
    }

    // =========================================================================
    // Detection de clic sur les boules d'action
    // =========================================================================

    /**
     * Convertit les coordonnees ecran en espace monde, effectue le hitTest
     * sur chaque boule d'action et notifie le listener si un hit est detecte.
     */
    private void notifyActionBallClick(int sx, int sy) {
        if (jeuVerrouille) return;
        Vector2 worldPos = toWorld(new Vector2(sx, sy));
        for (int i = 0; i < actionBalls.length; i++) {
            Composant2D ball = actionBalls[i];
            if (ball == null) continue;
            double rx = (ball.taille.x * ball.echelle.x) / 2.0;
            double ry = (ball.taille.y * ball.echelle.y) / 2.0;
            double r  = Math.min(rx, ry);
            double dx = worldPos.x - ball.position.x;
            double dy = worldPos.y - ball.position.y;
            if (dx * dx + dy * dy <= r * r) {
                if (actionBallClickListener != null)
                    actionBallClickListener.onActionBallHit(i, actionBallCurrentSprite[i]);
                return;
            }
        }
    }

    /** Convertit des coordonnees ecran en coordonnees monde. */
    private Vector2 toWorld(Vector2 screen) {
        return screen.Div(Camera.zoom).Add(Camera.positionHG);
    }

    // =========================================================================
    // Positionnement overlay
    // =========================================================================

    /**
     * Repositionne les elements de l'uiOverlay apres un resize.
     * Appele depuis applyLetterbox().
     */
    public void repositionOverlayElements(int camW, int camH) {
        if (uiOverlay == null) return;
        uiOverlay.setSize(camW, camH);

        // Repositionner le panneau des sabliers
        if (sablierPanel != null) {
            int panelW = sablierPanel.getPreferredSize().width;
            int panelH = sablierPanel.getPreferredSize().height;
            int x = camW - panelW - 10;
            int y = 10;
            sablierPanel.setBounds(x, y, panelW, panelH);
        }

        // Repositionner le panneau "réflexion"
        if (thinkingPanel != null) {
            int fontSize = camH / 45;
            thinkingLabel.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            thinkingPanel.revalidate();
            Dimension preferred = thinkingPanel.getPreferredSize();
            int w = camW / 4;
            int h = camH / 18;
            thinkingPanel.setBounds(10, 10, w, h);
        }

        //Redimensionner l'overlay actif (carte alibi, etc.)
        if (currentOverlayPanel != null) {
            currentOverlayPanel.setBounds(0, 0, camW, camH);
            currentOverlayPanel.revalidate();
        }

        uiOverlay.revalidate();
    }

    // =========================================================================
    // Rendu vectoriel des rectangles joueurs — intégré dans la boucle Camera
    // =========================================================================

    /**
     * Coordonnées fixes en espace monde pour les deux rectangles joueurs.
     * Index 0 = Enquêteur (au-dessus du plateau), Index 1 = Jack (en dessous).
     */
    private static final double PLAYER_RECT_W_WORLD = 200.0;
    private static final double PLAYER_RECT_H_WORLD =  55.0;

    /** Centre X des deux rectangles : milieu horizontal du plateau. */
    private static final double PLAYER_RECT_CX =
            BOARD_ORIGIN.x + BOARD_SIZE / 2.0;

    /** Centre Y du rectangle Enquêteur (au-dessus du plateau, marge 10 px). */
    private static final double PLAYER_RECT_CY_TOP =
            BOARD_ORIGIN.y - PLAYER_RECT_H_WORLD / 2.0 - 110.0;

    /** Centre Y du rectangle Jack (en dessous du plateau, marge 10 px). */
    private static final double PLAYER_RECT_CY_BOT =
            BOARD_ORIGIN.y + BOARD_SIZE + PLAYER_RECT_H_WORLD / 2.0 + 110.0;

    /**
     * Dessine les deux rectangles joueurs directement dans l'espace caméra.
     * Doit être appelé depuis paintComponent() de la Camera après tous les
     * autres calques.
     *
     * @param g Contexte graphique 2D de la Camera (coordonnées écran).
     */
    void paintPlayerRects(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double[] worldCY = { PLAYER_RECT_CY_TOP, PLAYER_RECT_CY_BOT };

        for (int p = 0; p < 2; p++) {
            // ---- Transformation monde → écran --------------------------------
            double worldX = PLAYER_RECT_CX - PLAYER_RECT_W_WORLD / 2.0;
            double worldY = worldCY[p]      - PLAYER_RECT_H_WORLD / 2.0;

            double sx = (worldX - Camera.positionHG.x) * Camera.zoom.x;
            double sy = (worldY - Camera.positionHG.y) * Camera.zoom.y;
            double sw = PLAYER_RECT_W_WORLD * Camera.zoom.x;
            double sh = PLAYER_RECT_H_WORLD * Camera.zoom.y;

            int rx = (int) Math.round(sx);
            int ry = (int) Math.round(sy);
            int rw = (int) Math.round(sw);
            int rh = (int) Math.round(sh);
            int arc = (int) Math.round(14 * Camera.zoom.x);

            // ---- Fond semi-transparent avec coins arrondis -------------------
            Color baseColor = playerColors[p];
            g.setColor(baseColor);
            g.fillRoundRect(rx, ry, rw, rh, arc, arc);

            // ---- Bordure contrastée -----------------------------------------
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke((float) Math.max(1.0, 2.0 * Camera.zoom.x)));
            g.drawRoundRect(rx, ry, rw, rh, arc, arc);
            g.setStroke(new BasicStroke(1f));

            // ---- Textes (nom + type) centrés ---------------------------------
            float baseFontSize = (float) (13.0 * Camera.zoom.x);
            float smallFontSize = (float) Math.max(8.0, 10.0 * Camera.zoom.x);

            Font nameFont = new Font("SansSerif", Font.BOLD,  Math.max(8, (int) baseFontSize));
            Font typeFont = new Font("SansSerif", Font.PLAIN, Math.max(7, (int) smallFontSize));

            String name = playerNames[p];
            String type = playerTypes[p];
            boolean hasType = type != null && !type.isEmpty();

            // Mesures pour centrage vertical
            FontMetrics fmName = g.getFontMetrics(nameFont);
            FontMetrics fmType = hasType ? g.getFontMetrics(typeFont) : null;

            int totalTextH = fmName.getAscent() + fmName.getDescent()
                           + (hasType ? (int)(2 * Camera.zoom.y) + fmType.getAscent() + fmType.getDescent() : 0);

            int textStartY = ry + (rh - totalTextH) / 2 + fmName.getAscent();

            // Nom
            g.setFont(nameFont);
            g.setColor(Color.WHITE);
            int nameX = rx + (rw - fmName.stringWidth(name)) / 2;
            g.drawString(name, nameX, textStartY);

            // Type
            if (hasType) {
                g.setFont(typeFont);
                g.setColor(new Color(220, 220, 220, 220));
                int typeY = textStartY + fmName.getDescent() + (int)(2 * Camera.zoom.y) + fmType.getAscent();
                int typeX = rx + (rw - fmType.stringWidth(type)) / 2;
                g.drawString(type, typeX, typeY);
            }
        }
    }

    // =========================================================================
    // Rendu feedback survol — appele depuis paintComponent de la Camera
    // =========================================================================

    private void paintHoverOverlay(Graphics2D g) {
        // Liseré permanent sur les jetons non utilisés et non survolés
        drawUnusedActionBorders(g);

        if (hoveredTokenIndex < 0) return;
        String sprite = actionBallCurrentSprite[hoveredTokenIndex];
        if (sprite == null) return;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (sprite) {
            case "action_rotation", "action_echange" -> dessinerLisereBoard(g);
            case "action_holmes"  -> dessinerCibleDeplacement(g, detectivePosition[0],
                                        new Color(200, 40,  40,  130), 2,false);
            case "action_watson"  -> dessinerCibleDeplacement(g, detectivePosition[1],
                                        new Color(120, 60,  20,  130), 2,false);
            case "action_toby"    -> dessinerCibleDeplacement(g, detectivePosition[2],
                                        new Color(40,  80,  200, 130), 2,false);
            case "action_joker"   -> {
                dessinerCibleDeplacement(g, detectivePosition[0], new Color(200, 40,  40,  130), 1,true);
                dessinerCibleDeplacement(g, detectivePosition[1], new Color(120, 60,  20,  130), 1,true);
                dessinerCibleDeplacement(g, detectivePosition[2], new Color(40,  80,  200, 130), 1,true);
            }
            // action_alibi et variantes _grisee : pas de feedback geometrique
        }
    }

    private void paintSelectedOverlay(Graphics2D g){
        if(selectedRow >= 0) dessinerTuileFeedback(g, selectedRow, selectedCol, new Color(255, 220, 0, 150));

    }
    
    /**
     * Surligne les tuiles selectionnées avant échange.
     * */
    private void dessinerTuileFeedback(Graphics2D g, int row, int col, Color couleur){
        Composant2D comp = tileComponents[row][col];
        if(comp == null) return;
        Stroke ancienneStroke = g.getStroke();
        Color ancienneCouleur = g.getColor();

        Vector2 ecranPosition = comp.CoinHG().Sub(Camera.positionHG).Mult(Camera.zoom);
        Vector2 screenTail = comp.TailleRel().Mult(Camera.zoom);

        g.setColor(couleur);
        g.setStroke(new java.awt.BasicStroke(5f));

        g.drawRect((int) ecranPosition.x, (int) ecranPosition.y,
                (int) screenTail.x, (int) screenTail.y);

        g.setStroke(ancienneStroke);
        g.setColor(ancienneCouleur);
    }
    /**
     * Dessine un liseré jaune permanent autour des jetons d'action non encore utilisés,
     * sauf celui qui est actuellement survolé (le feedback de survol prend le relais).
     */
    private void drawUnusedActionBorders(Graphics2D g) {
        Stroke ancienStroke = g.getStroke();
        Color  ancienneColor = g.getColor();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new java.awt.BasicStroke(3f));
        boolean hasHover = hoveredTokenIndex >= 0;

        //Si un jeton est hovered, on dessine un feedforward statique et on désactive le clignotement des autres.
        if(hasHover && hoveredTokenIndex < actionBalls.length && !actionBallIsUsed[hoveredTokenIndex]){
            drawActionTokenBorder(g,hoveredTokenIndex, new Color(255,220,0,150));
            g.setColor(ancienneColor);
            g.setStroke(ancienStroke);
            return;
        }
        //Si pas de jeton hovered, fait clignoté les jetons non utilisés.
        float pulse = (float) ((Math.sin(clignotePhase) + 1.0) / 2.0);
        int alpha = 80 + Math.round(140 * pulse);
        Color jaune = new Color(255, 220, 0, alpha);
        g.setColor(jaune);

        for (int i = 0; i < actionBalls.length; i++) {
            if(!actionBallIsUsed[i]) drawActionTokenBorder(g, i, jaune);
        }
        g.setStroke(ancienStroke);
        g.setColor(ancienneColor);
    }

    /**
     * Dessine les rayons
     * */
    private void drawActionTokenBorder(Graphics2D g, int index, Color color){
        // Liseré visible uniquement si jeton non utilisé
        if(index < 0 || index >= actionBalls.length) return;

        Composant2D ball = actionBalls[index];
        if (ball == null) return;

        // Rayon du cercle en espace monde (min des demi-dimensions * échelle)
        double rx = (ball.taille.x * ball.echelle.x) / 2.0;
        double ry = (ball.taille.y * ball.echelle.y) / 2.0;
        double r  = Math.min(rx, ry);

        // Centre du jeton → coordonnées écran
        Vector2 screenCenter = ball.position.Sub(Camera.positionHG).Mult(Camera.zoom);
        double  screenR      = r * Camera.zoom.x;

        g.setColor(color);
        g.drawOval(
                (int) Math.round(screenCenter.x - screenR),
                (int) Math.round(screenCenter.y - screenR),
                (int) Math.round(screenR * 2),
                (int) Math.round(screenR * 2)
        );
    }

    /**
     * Dessine un liseré jaune semi-transparent autour des 9 tuiles du plateau.
     * Utilise les positions de tileComponents (espace monde → espace ecran via Camera).
     */
    private void dessinerLisereBoard(Graphics2D g) {
        Color jaune = new Color(255, 220, 0, 150);
        Stroke ancienStroke = g.getStroke();
        Color ancienneColor = g.getColor();

        g.setStroke(new java.awt.BasicStroke(3f));
        g.setColor(jaune);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Composant2D comp = tileComponents[row][col];
                if (comp == null) continue;
                Vector2 coinHG    = comp.CoinHG();
                Vector2 screenPos = coinHG.Sub(Camera.positionHG).Mult(Camera.zoom);
                Vector2 screenTail = comp.TailleRel().Mult(Camera.zoom);
                g.drawRect(
                    (int) screenPos.x,
                    (int) screenPos.y,
                    (int) screenTail.x,
                    (int) screenTail.y
                );
            }
        }

        g.setStroke(ancienStroke);
        g.setColor(ancienneColor);
    }

   private void dessinerCibleDeplacement(Graphics2D g, int positionIndex, Color couleur, int maxPas, boolean inclureZero){
        if(positionIndex < 0 || positionIndex >= OUTER_POSITIONS.length) return;

        Stroke ancienneStroke = g.getStroke();
        Color ancienneCouleur = g.getColor();
        g.setStroke(new BasicStroke(2.0f));

        double baseR = (OUTER_BALL_DIAM * 0.5) * Camera.zoom.x;

        if(inclureZero) dessinerCibleFeedforward(g, positionIndex, new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), 80), baseR * 0.8);

        for(int pas = 1; pas <= maxPas; pas++){
            int cibleIndex = (positionIndex + pas) % OUTER_POSITIONS.length;
            dessinerCibleFeedforward(g, cibleIndex, couleur, baseR);
        }

        g.setStroke(ancienneStroke);
        g.setColor(ancienneCouleur);
    }

    private void dessinerCibleFeedforward(Graphics2D g, int positionIndex, Color couleur, double radius){
        Vector2 centre = OUTER_POSITIONS[positionIndex];
        Vector2 ecranCentre = centre.Sub(Camera.positionHG).Mult(Camera.zoom);
        float pulse = (float) ((Math.sin(clignotePhase) + 1.0) / 2.0);
        int alphaRemplissage = 30 + Math.round(90 * pulse);
        int alphaContour = 80 + Math.round(140 * pulse);

        g.setColor(new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), alphaRemplissage));
        g.fillOval((int) Math.round(ecranCentre.x - radius), (int) Math.round(ecranCentre.y - radius), (int) Math.round(radius * 2), (int) Math.round(radius * 2));

        g.setColor(new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), alphaContour));
        g.drawOval((int) Math.round(ecranCentre.x - radius), (int) Math.round(ecranCentre.y - radius),
                (int) Math.round(radius * 2), (int) Math.round(radius * 2));
    }

    private String getActionTooltipText(String action){
        return switch (action){
            case "action_holmes"   -> "Déplacer Holmes (1 ou 2 pas).";
            case "action_watson"   -> "Déplacer Watson (1 ou 2 pas).";
            case "action_toby"     -> "Déplacer Toby (1 ou 2 pas).";
            case "action_joker"    -> "Déplacer un des détective (0 ou 1 pas).";
            case "action_rotation" -> "Pivoter un quartier de 90°.";
            case "action_echange"  -> "Échanger deux quartiers.";
            case "action_alibi"    -> "Piocher une carte alibi.";
            default -> "";
        };
    }

    private Point getActionBallScreenPos(int index){
        Composant2D ball = actionBalls[index];
        if (ball == null) return new Point(0, 0);
        Vector2 screenCenter = ball.position.Sub(Camera.positionHG).Mult(Camera.zoom);
        return new Point((int) Math.round(screenCenter.x), (int) Math.round(screenCenter.y));
    }
}