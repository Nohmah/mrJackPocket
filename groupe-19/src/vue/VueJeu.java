package src.vue;

import src.modele.*;
import src.modele.Action;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VueJeu — Fenêtre principale du jeu (1200×800).
 *
 * Responsabilités :
 *   - Créer et initialiser la JFrame ainsi que la Camera.
 *   - Instancier tous les Composant2D visuels (tuiles, boules, indicateurs).
 *   - Fournir les méthodes de mise à jour visuelle appelées par Gameplay.
 *   - Exposer le bouton "Terminer tour" et les listeners au contrôleur.
 *
 * IMPORTANT : Aucun fillRect/fillOval Swing n'est utilisé pour les éléments
 * du plateau ou les boules d'interface ; tout passe par des Composant2D
 * rendus par la Camera.
 *
 * Coordonnées monde utilisées :
 *   - Fenêtre : 1200 × 800
 *   - Plateau 3×3 centré : coin HG monde = (375, 175), taille = 450×450
 *   - Tuile            : 150×150 (taille de base, échelle 1×1)
 *   - Bande latérale   : x ∈ [0, 100] — rendue avec un Composant2D de fond
 */
public class VueJeu extends JFrame {
    // =========================================================================
    // Constantes monde
    // =========================================================================

    private static final int    WINDOW_W    = 1200;
    private static final int    WINDOW_H    = 800;

    private static final double STRIP_W     = 100;   // Bande noire gauche

    private static final double BOARD_SIZE  = 450;   // Plateau carré
    private static final double TILE_SIZE   = 150;   // Tuile unitaire

    // Coin HG du plateau dans l'espace monde
    private static final Vector2 BOARD_ORIGIN = new Vector2(
            (WINDOW_W - BOARD_SIZE) / 2.0,
            (WINDOW_H - BOARD_SIZE) / 2.0
    );

    // =========================================================================
    // Noms de fichiers pour les faces des balles d'action
    // =========================================================================

    /** Face Pile des boules A-D. */
    private static final String[] BALL_FACE_PILE = { "action_alibi", "action_toby", "action_rotation", "action_rotation" };
    /** Face Face des boules A-D. */
    private static final String[] BALL_FACE_FACE = { "action_holmes", "action_watson", "action_echange", "action_joker" };

    // =========================================================================
    // Noms de fichiers pour les faces des tours (indicateurs de tour)
    // =========================================================================

    /** Face Pile des tours T1-T8. */
    private static final String[] TURN_FACE_PILE = {
        "T1","T2","T3","T4","T5","T6","T7","T8"
    };
    /** Face Face de toutes les tours (image unique). */
    private static final String   TURN_FACE_FACE = "T0";

    // =========================================================================
    // Boules extérieures — anneau de 12 positions
    // =========================================================================

    /**
     * Positions monde des 12 boules de l'anneau OuterBall.
     * Ordre : Top-0, Top-1, Top-2, Right-0, Right-1, Right-2,
     *         Bottom-2, Bottom-1, Bottom-0, Left-2, Left-1, Left-0
     * (sens horaire, commençant en haut à gauche)
     */
    private static final Vector2[] OUTER_POSITIONS = buildOuterPositions();

    private static Vector2[] buildOuterPositions() {
        double ballDiam = 30;
        double gap      = ballDiam / 2.0 + 5;
        Vector2[] pos   = new Vector2[12];

        // Top : positions 0,1,2  (gauche → droite)
        for (int i = 0; i < 3; i++) {
            pos[i] = new Vector2(
                BOARD_ORIGIN.x + i * TILE_SIZE + TILE_SIZE / 2.0,
                BOARD_ORIGIN.y - gap
            );
        }
        // Right : positions 3,4,5  (haut → bas)
        for (int i = 0; i < 3; i++) {
            pos[3 + i] = new Vector2(
                BOARD_ORIGIN.x + BOARD_SIZE + gap,
                BOARD_ORIGIN.y + i * TILE_SIZE + TILE_SIZE / 2.0
            );
        }
        // Bottom : positions 6,7,8  (droite → gauche)
        for (int i = 0; i < 3; i++) {
            pos[6 + i] = new Vector2(
                BOARD_ORIGIN.x + (2 - i) * TILE_SIZE + TILE_SIZE / 2.0,
                BOARD_ORIGIN.y + BOARD_SIZE + gap
            );
        }
        // Left : positions 9,10,11  (bas → haut)
        for (int i = 0; i < 3; i++) {
            pos[9 + i] = new Vector2(
                BOARD_ORIGIN.x - gap,
                BOARD_ORIGIN.y + (2 - i) * TILE_SIZE + TILE_SIZE / 2.0
            );
        }
        return pos;
    }

    // Taille de base d'une OuterBall
    private static final double OUTER_BALL_DIAM = 30.0;

    // Facteurs d'échelle pour l'empilement des détectives
    private static final double DETECTIVE_SCALE_1 = 1.0;  // seul sur la case
    private static final double DETECTIVE_SCALE_2 = 1.3;  // 2ème détective empilé
    private static final double DETECTIVE_SCALE_3 = 1.6;  // 3ème détective empilé

    // =========================================================================
    // État interne — faces des boules et tours
    // =========================================================================

    /** true = face Pile, false = face Face, pour chacune des 4 balles d'action. */
    private final boolean[] ballFaceIsPile   = { true, true, true, true };

    /** true = face Pile, false = face Face, pour chacun des 8 indicateurs de tour. */
    private final boolean[] turnFaceIsPile   = { true, true, true, true, true, true, true, true };

    // =========================================================================
    // État interne — détectives sur l'anneau
    // =========================================================================

    /**
     * Position courante (0-11) de chacun des 3 détectives sur l'anneau.
     * -1 signifie "pas encore placé".
     */
    private final int[] detectivePosition = { -1, -1, -1 };

    /**
     * Composant2D représentant chaque détective sur l'anneau.
     * null si non encore placé.
     */
    private final Composant2D[] detectiveComponents = new Composant2D[3];

    // =========================================================================
    // Composants Swing non-Camera (overlay UI)
    // =========================================================================

    private JPanel  uiOverlay;
    private JButton validateButton;   // "Terminer tour"

    /** Rectangles Swing d'indicateurs joueurs (0 = joueur 1 rouge, 1 = joueur 2 bleu). */
    private final JPanel[] playerRects = new JPanel[2];

    // =========================================================================
    // Références Composant2D vivantes (pour mises à jour dynamiques)
    // =========================================================================

    /** Grille 3×3 des composants de tuiles. */
    private final Composant2D[][] tileComponents = new Composant2D[3][3];

    /** Boules d'action A-D (4 boules, côté gauche). */
    private final Composant2D[] actionBalls = new Composant2D[4];

    /** Indicateurs de tour 1-8 (côté droit). */
    private final Composant2D[] turnIndicators = new Composant2D[8];

    /** 12 boules de contour autour du plateau. */
    private final Composant2D[] outerBalls = new Composant2D[12];

    /** Cache d'images tournées (clé = "nom_angle"). */
    private final Map<String, Integer> rotatedSpriteCache = new HashMap<>();

    /** Référence au pont logique — instanciée après la Camera. */
    private Gameplay gameplay;

    // =========================================================================
    // Constructeur
    // =========================================================================

    public VueJeu(Partie partie) {
        super("Mr. Jack Pocket");
        initFrame();
        initCamera();
        initComponents();
        initUIOverlay();
        this.gameplay = new Gameplay(this, partie);
    }

    // =========================================================================
    // 1. Chargement d'images flexible
    // =========================================================================

    /**
     * Cherche une image dans res/Images/ en testant successivement .png puis .jpg.
     * Retourne null si aucune variante n'est trouvée.
     *
     * @param name Nom sans extension (ex: "BallAP").
     * @return Image chargée, ou null.
     */
    public static BufferedImage findImage(String name) { //MARCHE PAS
        String[] extensions = { ".png", ".jpg" };
        for (String ext : extensions) {
            String path = "res/Images/" + name + ext;
            try (InputStream in = new FileInputStream(path)) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) {
                    System.out.println("findImage — chargé : " + path);
                    return img;
                }
            } catch (IOException ignored) {
                // Extension non trouvée, on essaie la suivante
            }
        }
        System.err.println("findImage — introuvable : " + name + " (.png / .jpg)");
        return null;
    }

    /**
     * Fait pivoter une BufferedImage d'un angle donné (en radians) autour de
     * son centre. Retourne une nouvelle BufferedImage de dimensions ajustées
     * pour que l'image pivotée tienne entièrement dedans.
     *
     * @param img   Image source.
     * @param angle Angle de rotation en radians (ex: Math.PI / 2 = 90°).
     * @return Nouvelle image pivotée.
     */
    public static BufferedImage rotateImage(BufferedImage img, double angle) {
        if (img == null) return null;

        int w = img.getWidth();
        int h = img.getHeight();

        // Calcule les nouvelles dimensions pour absorber la rotation
        double sin = Math.abs(Math.sin(angle));
        double cos = Math.abs(Math.cos(angle));
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform at = new AffineTransform();
        at.translate((newW - w) / 2.0, (newH - h) / 2.0);
        at.rotate(angle, w / 2.0, h / 2.0);
        g2d.drawRenderedImage(img, at);
        g2d.dispose();

        return rotated;
    }

    // =========================================================================
    // 2. Gestion des faces (Balles & Tours)
    // =========================================================================

    /**
     * Bascule la face d'une balle d'action (Pile ↔ Face) et met à jour le sprite.
     *
     * @param ballIndex Index de la balle (0-3 correspondant à A-D).
     */
    public void switchBallFace(int ballIndex) {
        if (ballIndex < 0 || ballIndex >= 4) return;

        ballFaceIsPile[ballIndex] = !ballFaceIsPile[ballIndex];
        String spriteName = ballFaceIsPile[ballIndex]
                ? BALL_FACE_PILE[ballIndex]
                : BALL_FACE_FACE[ballIndex];

        actionBalls[ballIndex].spriteId = Camera.AddSprite(spriteName);
        Camera.Repaint();
        System.out.println("switchBallFace[" + ballIndex + "] → " + spriteName);
    }

    /**
     * Bascule la face d'un indicateur de tour (Pile ↔ Face) et met à jour le sprite.
     *
     * @param turnIndex Index du tour (0-7 correspondant à T1-T8).
     */
    public void switchTurnFace(int turnIndex) {
        if (turnIndex < 0 || turnIndex >= 8) return;

        turnFaceIsPile[turnIndex] = !turnFaceIsPile[turnIndex];
        String spriteName = turnFaceIsPile[turnIndex]
                ? TURN_FACE_PILE[turnIndex]
                : TURN_FACE_FACE;

        turnIndicators[turnIndex].spriteId = Camera.AddSprite(spriteName);
        Camera.Repaint();
        System.out.println("switchTurnFace[" + turnIndex + "] → " + spriteName);
    }

    // =========================================================================
    // 3. Interface Utilisateur (IHM)
    // =========================================================================

    /**
     * Modifie la couleur du rectangle d'indicateur d'un joueur.
     *
     * @param player 0 = Joueur 1, 1 = Joueur 2.
     * @param color  Nouvelle couleur de fond.
     */
    public void updateRectColor(int player, Color color) {
        if (player < 0 || player >= 2 || playerRects[player] == null) return;
        playerRects[player].setBackground(color);
        playerRects[player].repaint();
    }

    // =========================================================================
    // 4. Logique des détectives sur l'anneau OuterBall
    // =========================================================================

    /**
     * Remplace l'affichage d'une OuterBall (position 1-12) par l'image d'un détective.
     * Le tableau outerBalls utilise un index 0-11 en interne.
     *
     * @param position     Position sur l'anneau (1-12, sens horaire depuis le coin HG).
     * @param detectiveNum Numéro du détective (1-3 → "detective1", "detective2", "detective3").
     */
    public void replaceOuterBall(int position, int detectiveNum) {
        int idx = position - 1; // Conversion 1-indexé → 0-indexé
        if (idx < 0 || idx >= 12) {
            System.err.println("replaceOuterBall — position invalide : " + position);
            return;
        }
        if (detectiveNum < 1 || detectiveNum > 3) {
            System.err.println("replaceOuterBall — numéro de détective invalide : " + detectiveNum);
            return;
        }

        int detIdx = detectiveNum - 1;

        // Si ce détective était déjà quelque part, on retire son effet d'échelle là-bas
        if (detectivePosition[detIdx] != -1) {
            refreshOuterBallScale(detectivePosition[detIdx]);
        }

        // Met à jour la position mémorisée
        detectivePosition[detIdx] = idx;

        // Crée ou réutilise le Composant2D du détective
        String spriteName = "detective" + detectiveNum;
        if (detectiveComponents[detIdx] == null) {
            detectiveComponents[detIdx] = new Composant2D(
                OUTER_POSITIONS[idx],
                new Vector2(OUTER_BALL_DIAM, OUTER_BALL_DIAM),
                spriteName
            );
        } else {
            detectiveComponents[detIdx].position = OUTER_POSITIONS[idx];
            detectiveComponents[detIdx].spriteId = Camera.AddSprite(spriteName);
        }

        // Applique l'empilement (Z-Scale) sur la nouvelle case
        refreshOuterBallScale(idx);

        Camera.Repaint();
        System.out.println("replaceOuterBall — détective " + detectiveNum + " → position " + position);
    }

    /**
     * Avance un détective d'une case sur l'anneau (sens horaire).
     * Si le détective n'a pas encore été placé, il démarre à la position 0.
     *
     * @param detectiveID Identifiant du détective (1-3).
     */
    public void advanceDetective(int detectiveID) {
        int detIdx = detectiveID - 1;
        if (detIdx < 0 || detIdx >= 3) return;

        int currentPos = detectivePosition[detIdx];
        int nextPos    = (currentPos < 0) ? 0 : (currentPos + 1) % 12;

        replaceOuterBall(nextPos + 1, detectiveID);
        System.out.println("advanceDetective(" + detectiveID + ") : " + currentPos + " → " + nextPos);
    }

    /**
     * Recalcule l'échelle de tous les détectives présents sur la case `posIdx`
     * selon la règle d'empilement :
     *   - 1 détective : échelle 1.0
     *   - 2ème détective sur la même case : échelle 1.3
     *   - 3ème détective sur la même case : échelle 1.6
     *
     * @param posIdx Index de la case (0-11).
     */
    private void refreshOuterBallScale(int posIdx) {
        // Collecte les détectives présents sur cette case
        int stackCount = 0;
        int[] onCell   = new int[3]; // Indices des détectives sur cette case
        for (int d = 0; d < 3; d++) {
            if (detectivePosition[d] == posIdx && detectiveComponents[d] != null) {
                onCell[stackCount++] = d;
            }
        }

        // Applique les facteurs d'échelle selon l'ordre d'empilement
        double[] scales = { DETECTIVE_SCALE_1, DETECTIVE_SCALE_2, DETECTIVE_SCALE_3 };
        for (int k = 0; k < stackCount; k++) {
            double s = scales[k];
            detectiveComponents[onCell[k]].echelle = new Vector2(s, s);
        }
    }

    // =========================================================================
    // 5. Manipulation du plateau
    // =========================================================================

    /**
     * Échange deux tuiles du plateau identifiées par leur position de grille
     * (Vector2 dont x = col, y = row) et rafraîchit l'affichage via la Camera.
     *
     * Exemple : swapTiles(new Vector2(0,0), new Vector2(2,1))
     * échange la tuile (ligne 0, col 0) avec (ligne 1, col 2).
     *
     * @param pos1 Position (col, row) de la première tuile  (0-2 chacune).
     * @param pos2 Position (col, row) de la deuxième tuile.
     */
    public void swapTiles(Vector2 pos1, Vector2 pos2) {
        int row1 = (int) pos1.y, col1 = (int) pos1.x;
        int row2 = (int) pos2.y, col2 = (int) pos2.x;

        if (row1 < 0 || row1 > 2 || col1 < 0 || col1 > 2
         || row2 < 0 || row2 > 2 || col2 < 0 || col2 > 2) {
            System.err.println("swapTiles — coordonnées hors-plateau : " + pos1.ToString() + " / " + pos2.ToString());
            return;
        }

        if (row1 == row2 && col1 == col2) return; // Rien à faire

        // On échange uniquement les spriteId (les Composant2D restent en place,
        // c'est leur image qui change — c'est la sémantique voulue dans Camera).
        int tmpSprite = tileComponents[row1][col1].spriteId;
        tileComponents[row1][col1].spriteId = tileComponents[row2][col2].spriteId;
        tileComponents[row2][col2].spriteId = tmpSprite;

        Camera.RecalculateZoom(); // Force le repaint via RecalculateZoom (proxy de Repaint)
        System.out.println("swapTiles (" + row1 + "," + col1 + ") ↔ (" + row2 + "," + col2 + ")");
    }

    // =========================================================================
    // Mise à jour de la vue du district (appelée par Gameplay)
    // =========================================================================

    public void updateDistrictView(District district) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Quartier q = district.get(i, j);
                if (q.estSuspect()) {
                    String spriteName = q.getPersonnage().nom.replace(" ", "");
                    tileComponents[i][j].spriteId = Camera.AddSprite(spriteName);
                } else {
                    if (q.getPersonnage() == Personnage.JOSEPH_LANE) {
                        tileComponents[i][j].spriteId = Camera.AddSprite("JosephLane-verso");
                    } else {
                        tileComponents[i][j].spriteId = Camera.AddSprite("TileDefault");
                    }
                }
            }
        }
        Camera.Repaint();
    }

    // =========================================================================
    // Initialisation Swing
    // =========================================================================

    private void initFrame() {
        setSize(WINDOW_W, WINDOW_H);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        setLayout(null);
        setLocationRelativeTo(null);
    }

    private void initCamera() {
        Camera camera = new Camera(
                new Vector2(WINDOW_W, WINDOW_H),
                this
        );
        camera.setBounds(0, 0, WINDOW_W, WINDOW_H);
        getContentPane().add(camera);
    }

    // =========================================================================
    // Initialisation des Composant2D
    // =========================================================================

    private void initComponents() {
        initBackgroundComponent();
        initStripComponent();
        initTileComponents();
        initActionBalls();
        initTurnIndicators();
        initOuterBalls();
    }

    /**
     * Background plein-écran (rendu par Camera comme tout autre composant).
     */
    private void initBackgroundComponent() {
        new Composant2D(
                new Vector2(WINDOW_W / 2.0, WINDOW_H / 2.0),
                new Vector2(WINDOW_W, WINDOW_H),
                "Background"
        );
    }

    /**
     * Bande latérale gauche (LeftStrip) avec les boutons.
     */
    private void initStripComponent() {
        new Composant2D(
                new Vector2(STRIP_W / 2.0, WINDOW_H / 2.0),
                new Vector2(STRIP_W, WINDOW_H),
                "LeftStrip"
        );
    }

    private void initTileComponents() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Vector2 center = new Vector2(
                        BOARD_ORIGIN.x + col * TILE_SIZE + TILE_SIZE / 2.0,
                        BOARD_ORIGIN.y + row * TILE_SIZE + TILE_SIZE / 2.0
                );
                tileComponents[row][col] = new Composant2D(
                        center,
                        new Vector2(TILE_SIZE, TILE_SIZE),
                        "TileDefault"
                );
            }
        }
    }

    private void initActionBalls() {
        double ballDiam = 100;
        double cx       = STRIP_W + ballDiam / 2.0 + 10;
        double spacing  = BOARD_SIZE / BALL_FACE_PILE.length;

        for (int i = 0; i < BALL_FACE_PILE.length; i++) {
            double cy = BOARD_ORIGIN.y + spacing / 2.0 + i * spacing;
            // Affiche la face Pile par défaut
            actionBalls[i] = new Composant2D(
                    new Vector2(cx, cy),
                    new Vector2(ballDiam, ballDiam),
                    BALL_FACE_PILE[i]
            );
        }
    }

    /**Pour afficher les jetons qui ont étés lancés/retournés*/
    public void updateJetons(List<JetonAction> jetons) {
        for (int i = 0; i < 4 && i < actionBalls.length; i++) {
            Action a = jetons.get(i).getActionVisible();
            String spriteName = switch (a) {
                case HOLMES    -> "action_holmes";
                case WATSON    -> "action_watson";
                case TOBY      -> "action_toby";
                case JOKER     -> "action_joker";
                case ROTATION  -> "action_rotation";
                case ECHANGE   -> "action_echange";
                case ALIBI     -> "action_alibi";
            };
            actionBalls[i].spriteId = Camera.AddSprite(spriteName);
        }
        Camera.Repaint();
    }

    private void initTurnIndicators() {
        double ballDiam = 60;
        double cx       = WINDOW_W - ballDiam / 2.0 - 20;
        double spacing  = BOARD_SIZE / 8.0;

        for (int i = 0; i < 8; i++) {
            double cy = BOARD_ORIGIN.y + spacing / 2.0 + i * spacing;
            // Affiche la face Pile par défaut (T1-T8)
            turnIndicators[i] = new Composant2D(
                    new Vector2(cx, cy),
                    new Vector2(ballDiam, ballDiam),
                    TURN_FACE_PILE[i]
            );
        }
    }

    /**
     * Instancie les 12 boules de contour (OuterBall) en suivant l'ordre
     * horaire défini par OUTER_POSITIONS.
     */
    private void initOuterBalls() {
        double ballDiam = OUTER_BALL_DIAM;
        for (int i = 0; i < 12; i++) {
            outerBalls[i] = new Composant2D(
                    OUTER_POSITIONS[i],
                    new Vector2(ballDiam, ballDiam),
                    "OuterBall"
            );
        }
    }

    // =========================================================================
    // Overlay UI Swing
    // =========================================================================

    private void initUIOverlay() {
        uiOverlay = new JPanel(null);
        uiOverlay.setOpaque(false);
        uiOverlay.setBounds(0, 0, WINDOW_W, WINDOW_H);

        // --- Indicateurs joueurs ---
        initPlayerRects();

        // --- Boutons LeftStrip ---
        initLeftStripButtons();

        getContentPane().add(uiOverlay);
        getContentPane().setComponentZOrder(uiOverlay, 0);
    }

/**
 * Crée les deux rectangles d'indicateur de joueurs :
 *   - Joueur 1 (rouge) collé en haut au milieu de l'interface
 *   - Joueur 2 (bleu)  collé en bas au milieu de l'interface
 */
    private void initPlayerRects() {
        String[] labels     = { "Joueur 1", "Joueur 2" };
        Color[]  colors     = { new Color(180, 40, 40, 200), new Color(40, 80, 180, 200) };
        String[] typeLabels = { "Humain", "Humain" }; // Mis à jour via Gameplay

        int rectW = 200, rectH = 50;

        for (int p = 0; p < 2; p++) {
            final int playerIndex = p; // Variable effectivement finale pour la classe interne
            
            JPanel rect = new JPanel(new BorderLayout());
            rect.setBackground(colors[p]);
            rect.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

            JLabel name = new JLabel(labels[p], SwingConstants.CENTER);
            name.setForeground(Color.WHITE);
            name.setFont(new Font("SansSerif", Font.BOLD, 14));

            JLabel type = new JLabel(typeLabels[p], SwingConstants.CENTER);
            type.setForeground(new Color(220, 220, 220));
            type.setFont(new Font("SansSerif", Font.PLAIN, 11));

            rect.add(name, BorderLayout.CENTER);
            rect.add(type, BorderLayout.SOUTH);

            // Positionnement centré horizontalement et collé en haut/bas de l'interface
            rect.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    positionnerRectangle(rect, playerIndex); // Utilise playerIndex au lieu de p
                }
            });

            playerRects[p] = rect;
            uiOverlay.add(rect);
            
            // Positionnement initial
            rect.setSize(rectW, rectH);
            positionnerRectangle(rect, p);
        }
    }

    /**
     * Positionne un rectangle d'indicateur joueur :
     *   - playerIndex == 0 : collé en haut au milieu de l'interface
     *   - playerIndex == 1 : collé en bas au milieu de l'interface
     */
    private void positionnerRectangle(JPanel rect, int playerIndex) {
        java.awt.Container parent = rect.getParent();
        if (parent == null) return;
        
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        int rectW = rect.getWidth();
        int rectH = rect.getHeight();
        
        // Centré horizontalement
        int rectX = (parentWidth - rectW) / 2;
        
        // Collé en haut (joueur 1) ou en bas (joueur 2)
        int rectY = (playerIndex == 0) 
            ? 0                           // Collé en haut
            : parentHeight - rectH - 40;       // Collé en bas
        
        rect.setLocation(rectX, rectY);
    }
    /**
     * Ajoute les boutons dans la bande latérale gauche :
     *   - "Retour"
     *   - "Nouvelle partie"
     *   - "IA"
     *   - "Terminer tour" (ex-"Valider")
     */
    private void initLeftStripButtons() {
        int bx = 5, bw = (int) STRIP_W - 10, bh = 38;

        // "Retour"
        JButton retourBtn = makeStripButton("Retour");
        retourBtn.setBounds(bx, 20, bw, bh);
        retourBtn.addActionListener(e -> onRetourPressed());
        uiOverlay.add(retourBtn);

        // "Nouvelle partie"
        JButton newGameBtn = makeStripButton("Nv. Partie");
        newGameBtn.setBounds(bx, 70, bw, bh);
        newGameBtn.addActionListener(e -> onNewGamePressed());
        uiOverlay.add(newGameBtn);

        // "IA"
        JButton iaBtn = makeStripButton("IA");
        iaBtn.setBounds(bx, 120, bw, bh);
        iaBtn.addActionListener(e -> onIAPressed());
        uiOverlay.add(iaBtn);

        // "Terminer tour" (Valider)
        validateButton = makeStripButton("Terminer tour");
        validateButton.setBounds(bx, WINDOW_H - 75, bw, bh);
        validateButton.addActionListener(e -> {
            if (gameplay != null) gameplay.onValidatePressed();
        });
        uiOverlay.add(validateButton);
    }

    /** Crée un JButton stylisé pour la bande latérale. */
    private JButton makeStripButton(String label) {
        JButton btn = new JButton("<html><center>" + label + "</center></html>");
        btn.setFont(new Font("SansSerif", Font.BOLD, 10));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(50, 50, 60));
        btn.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 120), 1));
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // =========================================================================
    // Handlers des boutons LeftStrip
    // =========================================================================

    private void onRetourPressed() {
        System.out.println("VueJeu — Retour (non implémenté)");
        // À connecter à une éventuelle navigation entre écrans
    }

    private void onNewGamePressed() {
        System.out.println("VueJeu — Nouvelle partie");
        // Recrée la partie via Gameplay si nécessaire
        // gameplay.resetGame();
    }

    private void onIAPressed() {
        System.out.println("VueJeu — Basculer mode IA");
        // Exemple : demande au contrôleur de jouer l'IA pour le tour en cours
        if (gameplay != null) {
            gameplay.getControler().joueIa();
        }
    }

    // =========================================================================
    // API publique appelée par Gameplay
    // =========================================================================

    public void registerControler(IHMControler controler) {
        Component cameraComp = getContentPane().getComponent(
                getContentPane().getComponentCount() - 1
        );
        cameraComp.addMouseListener(controler);
        cameraComp.addKeyListener(controler);
        cameraComp.setFocusable(true);
        cameraComp.requestFocusInWindow();
    }

    public void updateTurnIndicator(int turn) {
        for (int i = 0; i < turnIndicators.length; i++) {
            if (turnIndicators[i] == null) continue;
            turnIndicators[i].echelle = (i == turn - 1)
                    ? new Vector2(1.3, 1.3)
                    : new Vector2(1.0, 1.0);
        }
        Camera.Repaint();
    }

    public void refreshBoardHighlight(int row, int col) {
        if (tileComponents[row][col] == null) return;
        tileComponents[row][col].spriteId = Camera.AddSprite("TileHighlight");
        Camera.Repaint();
    }

    public void clearBoardHighlight(int row, int col) {
        if (tileComponents[row][col] == null) return;
        tileComponents[row][col].spriteId = Camera.AddSprite("TileDefault");
        Camera.Repaint();
    }

    public void refreshBoardComponents() {
        Camera.Repaint();
    }

    public void showGameOverScreen() {
        validateButton.setEnabled(false);
        JLabel gameOverLabel = new JLabel("FIN DE PARTIE", SwingConstants.CENTER);
        gameOverLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        gameOverLabel.setForeground(Color.YELLOW);
        gameOverLabel.setBounds(300, 350, 600, 100);
        uiOverlay.add(gameOverLabel);
        uiOverlay.revalidate();
        uiOverlay.repaint();
    }
}