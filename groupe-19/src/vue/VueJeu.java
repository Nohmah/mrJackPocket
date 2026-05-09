package src.vue;

import src.modele.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VueJeu — Fenetre principale du jeu (1200×800).
 *
 * Responsabilite UNIQUE : rendu visuel.
 *   - Creer et initialiser la JFrame et la Camera.
 *   - Instancier tous les Composant2D (tuiles, boules, indicateurs).
 *   - Exposer des methodes de mise a jour visuelle appelees par Gameplay.
 *   - Deleguer TOUTE logique metier a IHMControler / Gameplay.
 *
 * Ce que VueJeu NE fait plus :
 *   - Interpreter les actions (switch sur nomAction).
 *   - Appeler partie.actions.*.
 *   - Prendre des decisions de jeu.
 *
 * Layout :
 *   JFrame (BorderLayout)
 *   ├─ WEST  : leftStrip  (JPanel, BoxLayout Y, largeur fixe STRIP_W_PX)
 *   └─ CENTER: centerPane (JLayeredPane)
 *                ├─ Camera      (couche DEFAULT)
 *                └─ uiOverlay   (couche PALETTE, null-layout, transparent)
 */
public class VueJeu extends JFrame {

    // =========================================================================
    // Constantes monde
    // =========================================================================

    private static final int    WINDOW_W    = 1200;
    private static final int    WINDOW_H    = 800;
    /** Largeur en pixels de la bande latérale Swing (boutons). */
    private static final int    STRIP_W_PX  = 100;
    private static final double STRIP_W     = 100;   // conservé pour les Composant2D monde
    private static final double BOARD_SIZE  = 450;
    private static final double TILE_SIZE   = 150;

    private static final Vector2 BOARD_ORIGIN = new Vector2(
            (WINDOW_W - BOARD_SIZE) / 2.0,
            (WINDOW_H - BOARD_SIZE) / 2.0
    );

    // =========================================================================
    // Sprites
    // =========================================================================

    private static final String[] BALL_FACE_PILE = { "action_alibi", "action_toby", "action_rotation", "action_rotation" };
    private static final String[] BALL_FACE_FACE = { "action_holmes", "action_watson", "action_echange", "action_joker" };
    private static final String[] TURN_FACE_PILE = { "T1","T2","T3","T4","T5","T6","T7","T8" };
    private static final String   TURN_FACE_FACE = "T0";

    // =========================================================================
    // Positions anneau OuterBall
    // =========================================================================

    private static final Vector2[] OUTER_POSITIONS = buildOuterPositions();
    private static final double    OUTER_BALL_DIAM = 30.0;

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
    // etat interne — faces
    // =========================================================================

    private final boolean[]  ballFaceIsPile         = { true, true, true, true };
    private final String[]   actionBallCurrentSprite = new String[4];
    private final boolean[]  turnFaceIsPile          = { true, true, true, true, true, true, true, true };

    // =========================================================================
    // etat interne — detectives
    // =========================================================================

    private final int[]        detectivePosition   = { -1, -1, -1 };
    private final Composant2D[] detectiveComponents = new Composant2D[3];

    // =========================================================================
    // Composants Swing
    // =========================================================================

    /** Bande latérale gauche — vrai composant Swing (Objectif A). */
    private JPanel      leftStrip;
    /** Panneau central qui empile Camera + uiOverlay. */
    private JLayeredPane centerPane;

    private JPanel  uiOverlay;
    private JButton validateButton;
    private final JPanel[] playerRects = new JPanel[2];

    /** Panneau de regles actuellement affiche (null = ferme). */
    private JPanel panneauRegles = null;

    // =========================================================================
    // Letterbox / scale (Objectif B)
    // =========================================================================

    /** Scale courant calculé par le ComponentListener. */
    private double currentScale = 1.0;

    // =========================================================================
    // Composant2D vivants
    // =========================================================================

    private final Composant2D[][]  tileComponents = new Composant2D[3][3];
    private final int[][]          tileRotations  = new int[3][3];
    private final String[][]       tileBaseNames  = new String[3][3];
    private Composant2D            backgroundComponent;
    private final Composant2D[]    actionBalls     = new Composant2D[4];
    private final Composant2D[]    turnIndicators  = new Composant2D[8];
    private final Composant2D[]    outerBalls      = new Composant2D[12];

    /** Cache de sprites tournes (cle = "nom_angle"). */
    private final Map<String, Integer> rotatedSpriteCache = new HashMap<>();

    // =========================================================================
    // Reference au mediateur
    // =========================================================================

    /** Mediateur logique — seul interlocuteur de VueJeu côte logique. */
    private Gameplay gameplay;

    // =========================================================================
    // Constructeur
    // =========================================================================

    public VueJeu(Partie partie) {
        super("Mr. Jack Pocket");
        initFrame();
        initLeftStrip();     // Objectif A — bande latérale Swing
        initCenterPane();    // contient Camera + uiOverlay
        initCamera();
        initComponents();
        initUIOverlay();
        initResizeListener();// Objectif B — letterbox
        setVisible(true);
        this.gameplay = new Gameplay(this, partie);
        replaceOuterBall(12, 1);
        replaceOuterBall(4, 2);
        replaceOuterBall(8, 3);
    }

    // =========================================================================
    // API d'enregistrement du contrôleur (appelee par Gameplay)
    // =========================================================================

    /**
     * Branche le contrôleur sur la Camera (souris + clavier) et installe
     * le MouseAdapter leger qui delegue les clics sur les boules d'action
     * a {@link #notifyActionBallClick(int, int)}.
     */
    public void registerControler(IHMControler controler) {
        // La Camera est le premier composant du centerPane (couche DEFAULT)
        Component cam = centerPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)[0];
        cam.addMouseListener(controler);
        cam.addKeyListener(controler);
        cam.setFocusable(true);
        cam.requestFocusInWindow();

        // Adaptateur leger : VueJeu traduit le clic ecran → monde,
        // fait le hitTest et notifie IHMControler. Aucune logique metier ici.
        cam.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                notifyActionBallClick(e.getX(), e.getY());
            }
        });
    }

    // =========================================================================
    // Detection de clic sur les boules d'action — RENDU PUR, zero logique metier
    // =========================================================================

    /**
     * Convertit les coordonnees ecran en espace monde, effectue le hitTest
     * sur chaque boule d'action et delegue a {@code IHMControler} si un hit
     * est detecte.
     *
     * VueJeu ne connaît pas la signification de l'action : elle transmet
     * uniquement l'index de la boule et son sprite courant (nom de l'action).
     *
     * @param sx Coordonnee X du clic en pixels ecran.
     * @param sy Coordonnee Y du clic en pixels ecran.
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
                // Notification pure — VueJeu ne sait pas ce qui se passera ensuite.
                gameplay.getControler().onActionBallHit(i, actionBallCurrentSprite[i]);
                return;
            }
        }
    }

    /**
     * Convertit des coordonnees ecran (pixels Swing) en coordonnees monde.
     * Centralise la formule inverse de la Camera pour eviter toute duplication.
     */
    private Vector2 toWorld(Vector2 screen) {
        return screen.Div(Camera.zoom).Add(Camera.positionHG);
    }

    // =========================================================================
    // Mise a jour visuelle (API appelee par Gameplay — lecture seule du modele)
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
        Camera.Repaint();
    }

    /** Met a jour les 4 jetons d'action selon leur face visible. */
    public void updateJetons(List<JetonAction> jetons) {
        for (int i = 0; i < 4 && i < actionBalls.length; i++) {
            src.modele.Action a = jetons.get(i).getActionVisible();
            String spriteName = switch (a) {
                case HOLMES   -> "action_holmes";
                case WATSON   -> "action_watson";
                case TOBY     -> "action_toby";
                case JOKER    -> "action_joker";
                case ROTATION -> "action_rotation";
                case ECHANGE  -> "action_echange";
                case ALIBI    -> "action_alibi";
            };
            actionBalls[i].spriteId = Camera.AddSprite(spriteName);
            actionBallCurrentSprite[i] = spriteName;
        }
        Camera.Repaint();
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
        Camera.Repaint();
    }

    /** Adapte la couleur de fond au numero de tour. */
    public void updateBackground(Partie.Joueur joueurCourant) {
        setBackgroundColor((joueurCourant == Partie.Joueur.JACK) ? BG_RED : BG_BLUE);
    }

    /** Bascule la face d'un indicateur de tour (Pile ↔ Face). */
    public void switchTurnFace(int turnIndex) {
        if (turnIndex < 0 || turnIndex >= 8) return;
        turnFaceIsPile[turnIndex] = !turnFaceIsPile[turnIndex];
        String sprite = turnFaceIsPile[turnIndex] ? TURN_FACE_PILE[turnIndex] : TURN_FACE_FACE;
        turnIndicators[turnIndex].spriteId = Camera.AddSprite(sprite);
        Camera.Repaint();
    }

    public boolean isTurnFacePile(int turnIndex) {
        if (turnIndex < 0 || turnIndex >= 8) return false;
        return turnFaceIsPile[turnIndex];
    }

    /** Bascule la face d'une boule d'action (Pile ↔ Face). */
    public void switchBallFace(int ballIndex) {
        if (ballIndex < 0 || ballIndex >= 4) return;
        ballFaceIsPile[ballIndex] = !ballFaceIsPile[ballIndex];
        String sprite = ballFaceIsPile[ballIndex] ? BALL_FACE_PILE[ballIndex] : BALL_FACE_FACE[ballIndex];
        actionBalls[ballIndex].spriteId = Camera.AddSprite(sprite);
        actionBallCurrentSprite[ballIndex] = sprite;
        Camera.Repaint();
    }

    /** Modifie la couleur du rectangle d'indicateur d'un joueur. */
    public void updateRectColor(int player, Color color) {
        if (player < 0 || player >= 2 || playerRects[player] == null) return;
        playerRects[player].setBackground(color);
        playerRects[player].repaint();
    }

    public void enableValidateButton(boolean enabled) { validateButton.setEnabled(enabled); }

    public void refreshBoardComponents() { Camera.Repaint(); }

    public void showGameOverScreen() {
        validateButton.setEnabled(false);
        JLabel lbl = new JLabel("FIN DE PARTIE", SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 64));
        lbl.setForeground(Color.RED);
        lbl.setBounds(300, 350, 600, 100);
        uiOverlay.add(lbl);
        uiOverlay.revalidate();
        uiOverlay.repaint();
    }

    public void hideGameOverScreen() {
        for (Component c : uiOverlay.getComponents()) {
            if (c instanceof JLabel lbl && "FIN DE PARTIE".equals(lbl.getText())) {
                uiOverlay.remove(c);
                break;
            }
        }
        uiOverlay.repaint();
    }

    // =========================================================================
    // Manipulation des tuiles (appelees par Gameplay apres decision)
    // =========================================================================

    /** echange deux tuiles visuellement (positions col/row). */
    public void swapTiles(Vector2 pos1, Vector2 pos2) {
        int r1 = (int) pos1.y, c1 = (int) pos1.x;
        int r2 = (int) pos2.y, c2 = (int) pos2.x;
        if (r1 < 0 || r1 > 2 || c1 < 0 || c1 > 2 || r2 < 0 || r2 > 2 || c2 < 0 || c2 > 2) return;
        if (r1 == r2 && c1 == c2) return;

        int     tmpS = tileComponents[r1][c1].spriteId; tileComponents[r1][c1].spriteId = tileComponents[r2][c2].spriteId; tileComponents[r2][c2].spriteId = tmpS;
        String  tmpB = tileBaseNames[r1][c1];           tileBaseNames[r1][c1] = tileBaseNames[r2][c2];                     tileBaseNames[r2][c2] = tmpB;
        int     tmpR = tileRotations[r1][c1];           tileRotations[r1][c1] = tileRotations[r2][c2];                     tileRotations[r2][c2] = tmpR;
        Camera.RecalculateZoom();
    }

    /** Fait pivoter une tuile d'un angle supplementaire (multiples de 90°). */
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
        Camera.Repaint();
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
        if (backgroundComponent == null) return;
        int id = Camera.AddSprite(colorName);
        if (id < 0) return;
        backgroundComponent.spriteId = id;
        Camera.Repaint();
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
        at.translate((nw - w) / 2.0, (nh - h) / 2.0);
        at.rotate(angle, w / 2.0, h / 2.0);
        g2.drawRenderedImage(img, at);
        g2.dispose();
        return rot;
    }

    // =========================================================================
    // Initialisation Swing
    // =========================================================================

    private void initFrame() {
        setSize(WINDOW_W, WINDOW_H);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        // ── Objectif A : BorderLayout remplace null-layout ──────────────────
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(Color.BLACK); // bandes letterbox
        setLocationRelativeTo(null);
    }

    // -------------------------------------------------------------------------
    // Objectif A — Bande latérale Swing (WEST)
    // -------------------------------------------------------------------------

    /**
     * Crée le JPanel leftStrip positionné en BorderLayout.WEST.
     * Contient les boutons via BoxLayout vertical.
     * Sa largeur est fixée par setPreferredSize ; sa hauteur suit le resize.
     */
    private void initLeftStrip() {
        leftStrip = new JPanel();
        leftStrip.setLayout(new BoxLayout(leftStrip, BoxLayout.Y_AXIS));
        leftStrip.setPreferredSize(new Dimension(STRIP_W_PX, WINDOW_H));
        leftStrip.setBackground(new Color(30, 30, 40));
        leftStrip.setBorder(BorderFactory.createEmptyBorder(20, 5, 10, 5));

        // ── Boutons ────────────────────────────────────────────────────────
        JButton retour  = makeStripButton("Retour");
        retour.addActionListener(e -> System.out.println("VueJeu — Retour (non implemente)"));

        JButton newGame = makeStripButton("Nv. Partie");
        newGame.addActionListener(e -> { if (gameplay != null) gameplay.resetGame(); });

        JButton ia      = makeStripButton("IA");
        ia.addActionListener(e -> { if (gameplay != null) gameplay.getControler().joueIa(); });

        JButton regles  = makeStripButton("Regles");
        regles.addActionListener(e -> onReglesPressed());

        validateButton  = makeStripButton("Terminer tour");
        validateButton.addActionListener(e -> { if (gameplay != null) gameplay.onValidatePressed(); });

        leftStrip.add(retour);
        leftStrip.add(Box.createVerticalStrut(8));
        leftStrip.add(newGame);
        leftStrip.add(Box.createVerticalStrut(8));
        leftStrip.add(ia);
        leftStrip.add(Box.createVerticalStrut(8));
        leftStrip.add(regles);
        leftStrip.add(Box.createVerticalGlue());   // pousse "Terminer tour" vers le bas
        leftStrip.add(validateButton);
        leftStrip.add(Box.createVerticalStrut(10));

        getContentPane().add(leftStrip, BorderLayout.WEST);
    }

    // -------------------------------------------------------------------------
    // Objectif A — centerPane (JLayeredPane, CENTER) : Camera + uiOverlay
    // -------------------------------------------------------------------------

    private void initCenterPane() {
        centerPane = new JLayeredPane();
        centerPane.setBackground(Color.BLACK);
        centerPane.setOpaque(true);
        getContentPane().add(centerPane, BorderLayout.CENTER);
    }

    // -------------------------------------------------------------------------
    // Camera (dans centerPane, couche DEFAULT)
    // -------------------------------------------------------------------------

    private void initCamera() {
        Camera cam = new Camera(new Vector2(WINDOW_W, WINDOW_H), this) {
            /** Ajoute VALUE_INTERPOLATION_BILINEAR lors du rendu (Objectif C). */
            @Override
            public void paintComponent(Graphics g) {
                ((Graphics2D) g).setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                super.paintComponent(g);
            }
        };
        cam.setBounds(0, 0, WINDOW_W, WINDOW_H);
        centerPane.add(cam, JLayeredPane.DEFAULT_LAYER);
    }

    // -------------------------------------------------------------------------
    // Objectif B — Letterbox resize listener
    // -------------------------------------------------------------------------

    /**
     * À chaque redimensionnement de la JFrame :
     *  1. Calcule scale = min(availW / WINDOW_W, availH / WINDOW_H)
     *  2. Redimensionne la Camera en centrant avec des bandes noires (letterbox)
     *  3. Applique le zoom à la Camera via Camera.SetZoomCamera()
     *  4. Repositionne les éléments du uiOverlay proportionnellement
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
        // Espace disponible dans centerPane (exclut déjà leftStrip via BorderLayout)
        int availW = centerPane.getWidth();
        int availH = centerPane.getHeight();
        if (availW <= 0 || availH <= 0) return;

        // Scale uniforme conservant le ratio WINDOW_W × WINDOW_H
        double scaleX = (double) availW / WINDOW_W;
        double scaleY = (double) availH / WINDOW_H;
        currentScale  = Math.min(scaleX, scaleY);

        int camW = (int) Math.round(WINDOW_W * currentScale);
        int camH = (int) Math.round(WINDOW_H * currentScale);
        int camX = (availW - camW) / 2;
        int camY = (availH - camH) / 2;

        // Repositionner la Camera (couche DEFAULT)
        Component[] defaultLayer = centerPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER);
        if (defaultLayer.length > 0) {
            defaultLayer[0].setBounds(camX, camY, camW, camH);
        }

        // Repositionner l'uiOverlay de façon identique (couche PALETTE)
        Component[] paletteLayer = centerPane.getComponentsInLayer(JLayeredPane.PALETTE_LAYER);
        if (paletteLayer.length > 0) {
            paletteLayer[0].setBounds(camX, camY, camW, camH);
        }

        // Mise à jour du zoom Camera (utilise le mécanisme existant)
        Camera.SetZoomCamera(new Vector2(currentScale, currentScale));

        // Repositionnement proportionnel des playerRects (Objectif C)
        repositionOverlayElements(camW, camH);
    }

    // =========================================================================
    // Composants du monde
    // =========================================================================

    private void initComponents() {
        initBackgroundComponent();
        initTileComponents();
        initActionBalls();
        initTurnIndicators();
        initOuterBalls();
    }

    private void initBackgroundComponent() {
        backgroundComponent = new Composant2D(
                new Vector2(WINDOW_W / 2.0, WINDOW_H / 2.0),
                new Vector2(WINDOW_W, WINDOW_H), "Background");
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
        double diam = 100, cx = STRIP_W + diam / 2.0 + 10;
        double spacing = BOARD_SIZE / BALL_FACE_PILE.length;
        for (int i = 0; i < BALL_FACE_PILE.length; i++) {
            double cy = BOARD_ORIGIN.y + spacing / 2.0 + i * spacing;
            actionBalls[i] = new Composant2D(
                    new Vector2(cx, cy), new Vector2(diam, diam), BALL_FACE_PILE[i]);
            actionBallCurrentSprite[i] = BALL_FACE_PILE[i];
        }
    }

    private void initTurnIndicators() {
        double diam = 60;
        double radius = diam / 2.0;
        double cx = WINDOW_W - radius - 20 - (1.25 * radius);
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

    // =========================================================================
    // Objectif A — uiOverlay résiduel (indicateurs joueurs uniquement)
    // =========================================================================

    private void initUIOverlay() {
        uiOverlay = new JPanel(null);
        uiOverlay.setOpaque(false);
        uiOverlay.setBounds(0, 0, WINDOW_W, WINDOW_H);

        // Seuls les playerRects restent dans l'overlay ; les boutons sont dans leftStrip
        initPlayerRects();

        centerPane.add(uiOverlay, JLayeredPane.PALETTE_LAYER);
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

    private void positionnerRectangle(JPanel rect, int playerIndex) {
        java.awt.Container parent = rect.getParent();
        if (parent == null) return;
        int x = (parent.getWidth() - rect.getWidth()) / 2;
        int y = (playerIndex == 0) ? 0 : parent.getHeight() - rect.getHeight();
        rect.setLocation(x, y);
    }

    // -------------------------------------------------------------------------
    // Objectif C — repositionnement proportionnel des éléments overlay
    // -------------------------------------------------------------------------

    /**
     * Redimensionne et repositionne les éléments de l'uiOverlay en fonction
     * des nouvelles dimensions de la zone Camera (camW × camH).
     * Appelé depuis applyLetterbox() à chaque resize.
     */
    private void repositionOverlayElements(int camW, int camH) {
        if (uiOverlay == null) return;
        // L'uiOverlay occupe désormais exactement la surface de la Camera
        // (setBounds déjà fait dans applyLetterbox), donc on repositionne
        // les playerRects relativement à cette nouvelle taille.
        uiOverlay.setSize(camW, camH);
        for (int p = 0; p < 2; p++) {
            if (playerRects[p] == null) continue;
            positionnerRectangle(playerRects[p], p);
        }
        // Si le panneau de règles est ouvert, on l'adapte aussi
        if (panneauRegles != null) {
            panneauRegles.setBounds(0, 0, camW, camH);
        }
        uiOverlay.revalidate();
        uiOverlay.repaint();
    }

    // =========================================================================
    // Bouton helper
    // =========================================================================

    private JButton makeStripButton(String label) {
        JButton btn = new JButton("<html><center>" + label + "</center></html>");
        btn.setFont(new Font("SansSerif", Font.BOLD, 10));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(50, 50, 60));
        btn.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 120), 1));
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(STRIP_W_PX - 10, 38));
        btn.setPreferredSize(new Dimension(STRIP_W_PX - 10, 38));
        return btn;
    }

    // =========================================================================
    // Panneau de regles — affichage purement visuel, zero logique metier
    // =========================================================================

    private void onReglesPressed() {
        // Effet Toggle : si deja ouvert, on ferme
        if (panneauRegles != null && panneauRegles.isShowing()) {
            fermerPanneauRegles();
            return;
        }

        // 1. Creation du panneau semi-transparent
        panneauRegles = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 230));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panneauRegles.setOpaque(false);
        // Dimensionné sur la surface courante de l'uiOverlay
        panneauRegles.setBounds(0, 0, uiOverlay.getWidth(), uiOverlay.getHeight());
        panneauRegles.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 2. Contenu HTML complet
        String html = "<html>"
            + "<body style='color:white; font-family:SansSerif;'>"

            + "<h1 style='color:red; text-align:center; margin-bottom:6px;'>"
            + "&nbsp; Regles de Mr. Jack Pocket &nbsp;</h1>"
            + "<hr style='border:1px solid red; margin-bottom:14px;'/>"

            + "<h2 style='color:red; margin-bottom:4px;'>But du Jeu</h2>"
            + "<ul>"
            + "<li><b>Victoire de l'Enqueteur :</b> Un seul suspect reste en jeu.</li>"
            + "<li><b>Victoire de Jack :</b> Il possede 6 sabliers "
            + "<i>ou</i> n'a pas ete capture apres 8 tours.</li>"
            + "</ul>"

            + "<h2 style='color:red; margin-top:10px; margin-bottom:4px;'>"
            + "Deroulement d'un Tour</h2>"
            + "<p>Chaque tour se compose de deux etapes : "
            + "<b>La Traque</b> et <b>l'Appel a Temoins</b>.</p>"

            + "<h3 style='color:#FFD700; margin-bottom:2px;'>1. La Traque (Actions)</h3>"
            + "<p>L'ordre de jeu change selon la parite du tour :</p>"
            + "<ul>"
            + "<li><b>Tours Impairs (1, 3, 5, 7) :</b> L'Enqueteur choisit 1 action, "
            + "Jack en choisit 2, l'Enqueteur joue la derniere.</li>"
            + "<li><b>Tours Pairs (2, 4, 6, 8) :</b> Jack choisit 1 action, "
            + "l'Enqueteur en choisit 2, Jack joue la derniere.</li>"
            + "</ul>"
            + "<p><b>Detail des actions disponibles :</b></p>"
            + "<ul>"
            + "<li><b> Deplacement :</b> Avancer Holmes, Watson ou Toby "
            + "de 1 ou 2 cases <i>(sens horaire)</i>.</li>"
            + "<li><b> echange :</b> echanger deux tuiles de place "
            + "sans changer leur orientation.</li>"
            + "<li><b> Rotation :</b> Faire pivoter une tuile "
            + "(90 ou 180). <i>Une seule fois par tour.</i></li>"
            + "<li><b> Joker :</b> Deplacer l'enqueteur de son choix de 0 "
            + "<i>(Jack seulement)</i> ou 1 case.</li>"
            + "<li><b> Alibi :</b> Piocher une carte Alibi. "
            + "L'Enqueteur innocente le personnage ; Jack gagne des sabliers.</li>"
            + "</ul>"

            + "<h3 style='color:#FFD700; margin-top:8px; margin-bottom:2px;'>"
            + "2. L'Appel a Temoins</h3>"
            + "<p>Jack annonce s'il est <b>visible</b> "
            + "(ligne de mire d'un enqueteur sans mur) :</p>"
            + "<ul>"
            + "<li><b>Jack visible :</b> On elimine les suspects invisibles. "
            + "L'Enqueteur prend le jeton Temps.</li>"
            + "<li><b>Jack invisible :</b> On elimine les suspects visibles. "
            + "Jack prend le jeton Temps <i>(côte sablier)</i>.</li>"
            + "</ul>"

            + "<p style='text-align:center; color:#888; margin-top:16px; font-size:11px;'>"
            + ";&nbsp; Cliquez n'importe pour fermer</p>"
            + "</body></html>";

        JLabel labelRegles = new JLabel(html);
        labelRegles.setVerticalAlignment(SwingConstants.TOP);

        JScrollPane scroll = new JScrollPane(labelRegles,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 140, 0, 180), 2));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        panneauRegles.add(scroll, BorderLayout.CENTER);

        // 3. MouseListener : clic n'importe → fermer
        java.awt.event.MouseAdapter fermetureListener = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                fermerPanneauRegles();
            }
        };
        panneauRegles.addMouseListener(fermetureListener);
        scroll.addMouseListener(fermetureListener);
        labelRegles.addMouseListener(fermetureListener);

        // 4. Ajout au premier plan dans l'uiOverlay (devant les playerRects)
        uiOverlay.add(panneauRegles, 0);
        uiOverlay.revalidate();
        uiOverlay.repaint();

        System.out.println("VueJeu — Panneau de regles affiche");
    }

    private void fermerPanneauRegles() {
        if (panneauRegles != null) {
            uiOverlay.remove(panneauRegles);
            panneauRegles = null;
            uiOverlay.revalidate();
            uiOverlay.repaint();
            System.out.println("VueJeu — Panneau de regles ferme");
        }
    }
}