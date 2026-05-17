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

    // -------------------------------------------------------------------------
    // Anneau OuterBall
    // -------------------------------------------------------------------------

    private static final double    OUTER_BALL_DIAM    = 30.0;
    private static final Vector2[] OUTER_POSITIONS    = buildOuterPositions();

    private static final double DETECTIVE_SCALE_1 = 1.0;
    private static final double DETECTIVE_SCALE_2 = 1.3;
    private static final double DETECTIVE_SCALE_3 = 1.6;

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

    /** Cache de sprites tournes (cle = "nom_angle"). */
    private final Map<String, Integer> rotatedSpriteCache = new HashMap<>();

    // =========================================================================
    // Composants Swing internes
    // =========================================================================

    /** Conteneur empilant Camera + uiOverlay. */
    private final JLayeredPane layeredPane;

    /** Calque transparent pour les indicateurs joueurs et le panneau de regles. */
    private JPanel     uiOverlay;
    private JPanel[]   playerRects   = new JPanel[2];
    private JLabel     gameOverLabel = null;

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
        if (index == hoveredTokenIndex) return;
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
        initOuterBalls();
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

    private void initOuterBalls() {
        for (int i = 0; i < 12; i++)
            outerBalls[i] = new Composant2D(
                    OUTER_POSITIONS[i], new Vector2(OUTER_BALL_DIAM, OUTER_BALL_DIAM), "OuterBall");
    }

    private void initUIOverlay() {
        uiOverlay = new JPanel(null);
        uiOverlay.setOpaque(false);
        uiOverlay.setBounds(0, 0, WORLD_W, WORLD_H);
        initPlayerRects();
        layeredPane.add(uiOverlay, JLayeredPane.PALETTE_LAYER);
    }

    private void initPlayerRects() {
        String[] labels = { "Joueur 1", "Joueur 2" };
        Color[]  colors = { new Color(180, 40, 40, 200), new Color(40, 80, 180, 200) };
        int rectW = 200, rectH = 50;
        for (int p = 0; p < 2; p++) {
            final int pi = p;
            JPanel rect = new JPanel(new BorderLayout());
            rect.setBackground(colors[p]);
            rect.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            JLabel name = new JLabel(labels[p], SwingConstants.CENTER);
            name.setForeground(Color.WHITE);
            name.setFont(new Font("SansSerif", Font.BOLD, 14));
            JLabel type = new JLabel("Humain", SwingConstants.CENTER);
            type.setForeground(new Color(220, 220, 220));
            type.setFont(new Font("SansSerif", Font.PLAIN, 11));
            rect.add(name, BorderLayout.CENTER);
            rect.add(type, BorderLayout.SOUTH);
            playerRects[p] = rect;
            uiOverlay.add(rect);
            rect.setSize(rectW, rectH);
            positionnerRectangle(rect, pi);
        }
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
            String spriteName = j.isJoue() ? earlyName + "_grisee" : earlyName;
            actionBalls[i].spriteId = Camera.AddSprite(spriteName);
            actionBallCurrentSprite[i] = spriteName;
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
        for (int i = 0; i < turnIndicators.length; i++) {
            if (turnIndicators[i] == null) continue;
            turnIndicators[i].echelle = (i == turn - 1)
                    ? new Vector2(1.3, 1.3) : new Vector2(1.0, 1.0);
        }
    }

    /** Adapte la couleur de fond au joueur courant. */
    public void updateBackground(Joueur joueurCourant) {
        setBackgroundColor((joueurCourant == Joueur.JACK) ? BG_RED : BG_BLUE);
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

    /** Modifie la couleur du rectangle d'indicateur d'un joueur. */
    public void updateRectColor(int player, Color color) {
        if (player < 0 || player >= 2 || playerRects[player] == null) return;
        playerRects[player].setBackground(color);
        playerRects[player].repaint();
    }

    public void showGameOverScreen(String vainqueur) {
        gameOverLabel = new JLabel(vainqueur + " a gagne !", SwingConstants.CENTER);
        gameOverLabel.setFont(new Font("SansSerif", Font.BOLD, 64));
        gameOverLabel.setForeground(Color.RED);
        gameOverLabel.setBounds(300, 350, 600, 100);
        uiOverlay.add(gameOverLabel);
        uiOverlay.revalidate();
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
        if (stackCount <= 1) {
            if (stackCount == 1) detectiveComponents[onCell[0]].echelle = new Vector2(DETECTIVE_SCALE_1, DETECTIVE_SCALE_1);
            return;
        }
        double[] scales = { DETECTIVE_SCALE_1, DETECTIVE_SCALE_2, DETECTIVE_SCALE_3 };
        for (int k = 0; k < stackCount; k++)
            detectiveComponents[onCell[k]].echelle = new Vector2(scales[k], scales[k]);
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

    private void positionnerRectangle(JPanel rect, int playerIndex) {
        java.awt.Container parent = rect.getParent();
        if (parent == null) return;
        int x = (parent.getWidth() - rect.getWidth()) / 2;
        int y = (playerIndex == 0) ? 0 : parent.getHeight() - rect.getHeight();
        rect.setLocation(x, y);
    }

    /**
     * Repositionne les elements de l'uiOverlay apres un resize.
     * Appele depuis applyLetterbox().
     */
    public void repositionOverlayElements(int camW, int camH) {
        if (uiOverlay == null) return;
        uiOverlay.setSize(camW, camH);
        for (int p = 0; p < 2; p++) {
            if (playerRects[p] == null) continue;
            positionnerRectangle(playerRects[p], p);
        }
        uiOverlay.revalidate();
    }

    // =========================================================================
    // Rendu feedback survol — appele depuis paintComponent de la Camera
    // =========================================================================

    private void paintHoverOverlay(Graphics2D g) {
        if (hoveredTokenIndex < 0) return;
        String sprite = actionBallCurrentSprite[hoveredTokenIndex];
        if (sprite == null) return;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (sprite) {
            case "action_rotation", "action_echange" -> dessinerLisereBoard(g);
            case "action_holmes"  -> dessinerDemiCercleDeMouvement(g, detectivePosition[0],
                                        new Color(200, 40,  40,  130), getDirection(detectivePosition[0]),1);
            case "action_watson"  -> dessinerDemiCercleDeMouvement(g, detectivePosition[1],
                                        new Color(120, 60,  20,  130), getDirection(detectivePosition[1]),1);
            case "action_toby"    -> dessinerDemiCercleDeMouvement(g, detectivePosition[2],
                                        new Color(40,  80,  200, 130), getDirection(detectivePosition[2]),1);
            case "action_joker"   -> {
                dessinerDemiCercleDeMouvement(g, detectivePosition[0], new Color(200, 40,  40,  130), getDirection(detectivePosition[0]),0);
                dessinerDemiCercleDeMouvement(g, detectivePosition[1], new Color(120, 60,  20,  130), getDirection(detectivePosition[1]),0);
                dessinerDemiCercleDeMouvement(g, detectivePosition[2], new Color(40,  80,  200, 130), getDirection(detectivePosition[2]),0);
            }
            // action_alibi et variantes _grisee : pas de feedback geometrique
        }
    }

        /**
     * Détermine la direction du demi-cercle en fonction de l'index de position (0 à 11).
     * @param positionIndex Index de la position du détective (0-based)
     * @return 0 = droite, 1 = bas, 2 = gauche, 3 = haut
     */
    private int getDirection(int positionIndex) {
        if (positionIndex >= 0 && positionIndex <= 2) {
            return 0; // droite (positions 1, 2, 3)
        } else if (positionIndex >= 3 && positionIndex <= 5) {
            return 1; // bas (positions 4, 5, 6)
        } else if (positionIndex >= 6 && positionIndex <= 8) {
            return 2; // gauche (positions 7, 8, 9)
        } else if (positionIndex >= 9 && positionIndex <= 11) {
            return 3; // haut (positions 10, 11, 12)
        }
        return 0; // Valeur par défaut
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

    /**
     * Dessine un ou deux demi-cercles de mouvement autour de la position courante d'un detective
     * sur l'anneau (1 pas et/ou 2 pas de distance visuelle).
     *
     * @param g             Graphics2D de la Camera (espace ecran).
     * @param positionIndex Index 0-based dans OUTER_POSITIONS (-1 → rien dessiné).
     * @param couleur       Couleur de base avec alpha souhaite.
     * @param direction     0 = droite, 1 = bas, 2 = gauche, 3 = haut
     * @param nbCercles     0 = dessine seulement le cercle à 1 pas, 1 = dessine les deux (1 et 2 pas)
     */
    private void dessinerDemiCercleDeMouvement(Graphics2D g, int positionIndex, Color couleur, int direction, int nbCercles) {
        if (positionIndex < 0 || positionIndex >= OUTER_POSITIONS.length) return;

        Vector2 centre = OUTER_POSITIONS[positionIndex];
        Vector2 screenCentre = centre.Sub(Camera.positionHG).Mult(Camera.zoom);

        // Rayon d'un pas = largeur d'une tuile en pixels ecran
        double rayonPas = TILE_SIZE * Camera.zoom.x;

        Stroke ancienStroke = g.getStroke();
        Color ancienneColor = g.getColor();
        g.setStroke(new java.awt.BasicStroke(2.5f));

        // Détermine combien de cercles dessiner
        int maxPas = (nbCercles == 0) ? 1 : 2;

        for (int pas = 1; pas <= maxPas; pas++) {
            double r = rayonPas * pas;
            int alpha = 255;
            g.setColor(new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), alpha));
            
            // Détermine l'angle de départ en fonction de la direction
            int startAngle = 0;
            switch (direction) {
                case 0: startAngle = 270; break;  // droite
                case 1: startAngle = 180; break;  // bas
                case 2: startAngle = 90;  break;  // gauche
                case 3: startAngle = 0;   break;  // haut
            }
            
            // Dessine un arc de 180 degrés (demi-cercle)
            g.drawArc(
                (int) (screenCentre.x - r),
                (int) (screenCentre.y - r),
                (int) (2 * r),
                (int) (2 * r),
                startAngle,
                180
            );
        }

        // Point central pour reperer la position exacte
        g.setColor(new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), 180));
        int dotR = 5;
        g.fillOval((int) screenCentre.x - dotR, (int) screenCentre.y - dotR, dotR * 2, dotR * 2);

        g.setStroke(ancienStroke);
        g.setColor(ancienneColor);
    }
}