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
import javax.swing.JOptionPane;

/**
 * VueJeu — Fenetre principale du jeu (1200×800).
 *
 * Responsabilites :
 *   - Creer et initialiser la JFrame ainsi que la Camera.
 *   - Instancier tous les Composant2D visuels (tuiles, boules, indicateurs).
 *   - Fournir les methodes de mise à jour visuelle appelees par Gameplay.
 *   - Exposer le bouton "Terminer tour" et les listeners au contrôleur.
 *
 * IMPORTANT : Aucun fillRect/fillOval Swing n'est utilise pour les elements
 * du plateau ou les boules d'interface ; tout passe par des Composant2D
 * rendus par la Camera.
 *
 * Coordonnees monde utilisees :
 *   - Fenetre : 1200 × 800
 *   - Plateau 3×3 centre : coin HG monde = (375, 175), taille = 450×450
 *   - Tuile            : 150×150 (taille de base, echelle 1×1)
 *   - Bande laterale   : x ∈ [0, 100] — rendue avec un Composant2D de fond
 */
public class VueJeu extends JFrame {
    // =========================================================================
    // Constantes monde
    // =========================================================================

    private static final int    WINDOW_W    = 1200;
    private static final int    WINDOW_H    = 800;

    private static final double STRIP_W     = 100;   // Bande noire gauche

    private static final double BOARD_SIZE  = 450;   // Plateau carre
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
    // Boules exterieures — anneau de 12 positions
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

    // Facteurs d'echelle pour l'empilement des detectives
    private static final double DETECTIVE_SCALE_1 = 1.0;  // seul sur la case
    private static final double DETECTIVE_SCALE_2 = 1.3;  // 2eme detective empile
    private static final double DETECTIVE_SCALE_3 = 1.6;  // 3eme detective empile

    // =========================================================================
    // etat interne — faces des boules et tours
    // =========================================================================

    /** true = face Pile, false = face Face, pour chacune des 4 balles d'action. */
    private final boolean[] ballFaceIsPile   = { true, true, true, true };

    /** Nom du sprite actuellement affiche pour chacune des 4 boules d'action. */
    private final String[] actionBallCurrentSprite = new String[4];

    /** true = face Pile, false = face Face, pour chacun des 8 indicateurs de tour. */
    private final boolean[] turnFaceIsPile   = { true, true, true, true, true, true, true, true };

    // =========================================================================
    // etat interne — detectives sur l'anneau
    // =========================================================================

    /**
     * Position courante (0-11) de chacun des 3 detectives sur l'anneau.
     * -1 signifie "pas encore place".
     */
    private final int[] detectivePosition = { -1, -1, -1 };

    /**
     * Composant2D representant chaque detective sur l'anneau.
     * null si non encore place.
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
    // References Composant2D vivantes (pour mises à jour dynamiques)
    // =========================================================================

    /** Grille 3×3 des composants de tuiles. */
    private final Composant2D[][] tileComponents = new Composant2D[3][3];

    /** Angle de rotation courant (0 / 90 / 180 / 270) pour chaque tuile. */
    private final int[][] tileRotations = new int[3][3];

    /**
     * Nom de base du sprite de chaque tuile (sans suffixe d'angle).
     * Correspond au nom du fichier image, ex: "NoraNoire", "JohnSmith".
     * Initialise dans updateDistrictView et mis à jour par swapTiles.
     */
    private final String[][] tileBaseNames = new String[3][3];

    /** Composant du fond plein-ecran (modifie par setBackgroundColor). */
    private Composant2D backgroundComponent;

    /** Boules d'action A-D (4 boules, côte gauche). */
    private final Composant2D[] actionBalls = new Composant2D[4];

    /** Indicateurs de tour 1-8 (côte droit). */
    private final Composant2D[] turnIndicators = new Composant2D[8];

    /** 12 boules de contour autour du plateau. */
    private final Composant2D[] outerBalls = new Composant2D[12];

    /** Cache d'images tournees (cle = "nom_angle"). */
    private final Map<String, Integer> rotatedSpriteCache = new HashMap<>();

    /** Reference au pont logique — instanciee apres la Camera. */
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
        replaceOuterBall(12, 1);
        replaceOuterBall(4, 2);
        replaceOuterBall(8, 3);
    }


    /**
     * Fait pivoter une BufferedImage d'un angle donne (en radians) autour de
     * son centre. Retourne une nouvelle BufferedImage de dimensions ajustees
     * pour que l'image pivotee tienne entierement dedans.
     *
     * @param img   Image source.
     * @param angle Angle de rotation en radians (ex: Math.PI / 2 = 90°).
     * @return Nouvelle image pivotee.
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
        actionBallCurrentSprite[ballIndex] = spriteName; // maintient la synchronisation
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


        /**
     * Indique si l'indicateur de tour 'turnIndex' affiche la face Pile.
     * @param turnIndex 0‑7 correspondant à T1‑T8
     * @return true si la face Pile (T1..T8) est affichee, false si c'est la face Face (T0)
     */
    public boolean isTurnFacePile(int turnIndex) {
        if (turnIndex < 0 || turnIndex >= 8) return false;
        return turnFaceIsPile[turnIndex];
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
    // 4. Logique des detectives sur l'anneau OuterBall
    // =========================================================================

    /**
     * Remplace l'affichage d'une OuterBall (position 1-12) par l'image d'un detective.
     * Le tableau outerBalls utilise un index 0-11 en interne.
     *
     * @param position     Position sur l'anneau (1-12, sens horaire depuis le coin HG).
     * @param detectiveNum Numero du detective (1-3 → "detective1", "detective2", "detective3").
     */
    public void replaceOuterBall(int position, int detectiveNum) {
    int idx = position - 1; // Conversion 1-indexe → 0-indexe
    if (idx < 0 || idx >= 12) {
        System.err.println("replaceOuterBall — position invalide : " + position);
        return;
    }
    if (detectiveNum < 1 || detectiveNum > 3) {
        System.err.println("replaceOuterBall — numero de detective invalide : " + detectiveNum);
        return;
    }

    int detIdx = detectiveNum - 1;

    // je corrige 
    // 1. On memorise l'ancienne position
    int oldPos = detectivePosition[detIdx];

    // 2. On met à jour la position AVANT de rafraîchir les echelles
    detectivePosition[detIdx] = idx;

    // 3. Gestion du composant graphique
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

    // 4. On rafraîchit l'ancienne case (si elle existait)
    if (oldPos != -1) {
        refreshOuterBallScale(oldPos); 
    }

    // 5. On rafraîchit la nouvelle case
    refreshOuterBallScale(idx);

    Camera.Repaint();
    System.out.println("replaceOuterBall — detective " + detectiveNum + " → position " + position);
}
    /**
     * Avance un detective d'une case sur l'anneau (sens horaire).
     * Si le detective n'a pas encore ete place, il demarre à la position 0.
     *
     * @param detectiveID Identifiant du detective (1-3).
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
     * Recalcule l'echelle de tous les detectives presents sur la case `posIdx`
     * selon la regle d'empilement :
     *   - 1 detective : echelle 1.0
     *   - 2eme detective sur la meme case : echelle 1.3
     *   - 3eme detective sur la meme case : echelle 1.6
     *
     * @param posIdx Index de la case (0-11).
     */
    private void refreshOuterBallScale(int posIdx) {
        // Collecte les detectives presents sur cette case
        int stackCount = 0;
        int[] onCell   = new int[3]; // Indices des detectives sur cette case
        for (int d = 0; d < 3; d++) {
            if (detectivePosition[d] == posIdx && detectiveComponents[d] != null) {
                onCell[stackCount++] = d;
            }
        }

        // Si un seul detective sur la case, echelle forcee à 1.0 — aucun grossissement.
        // Le grossissement (1.3, 1.6) ne s'applique qu'à partir du 2eme detective empile.
        if (stackCount <= 1) {
            if (stackCount == 1) {
                detectiveComponents[onCell[0]].echelle = new Vector2(DETECTIVE_SCALE_1, DETECTIVE_SCALE_1);
            }
            return;
        }

        // 2 ou 3 detectives : applique les facteurs d'echelle selon l'ordre d'empilement
        // onCell[0] → echelle 1.0 (en dessous), onCell[1] → 1.3, onCell[2] → 1.6
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
     * echange deux tuiles du plateau identifiees par leur position de grille
     * (Vector2 dont x = col, y = row) et rafraîchit l'affichage via la Camera.
     *
     * Exemple : swapTiles(new Vector2(0,0), new Vector2(2,1))
     * echange la tuile (ligne 0, col 0) avec (ligne 1, col 2).
     *
     * @param pos1 Position (col, row) de la premiere tuile  (0-2 chacune).
     * @param pos2 Position (col, row) de la deuxieme tuile.
     */
    public void swapTiles(Vector2 pos1, Vector2 pos2) {
        int row1 = (int) pos1.y, col1 = (int) pos1.x;
        int row2 = (int) pos2.y, col2 = (int) pos2.x;

        if (row1 < 0 || row1 > 2 || col1 < 0 || col1 > 2
         || row2 < 0 || row2 > 2 || col2 < 0 || col2 > 2) {
            System.err.println("swapTiles — coordonnees hors-plateau : " + pos1.ToString() + " / " + pos2.ToString());
            return;
        }

        if (row1 == row2 && col1 == col2) return; // Rien à faire

        // echange des spriteId
        int tmpSprite = tileComponents[row1][col1].spriteId;
        tileComponents[row1][col1].spriteId = tileComponents[row2][col2].spriteId;
        tileComponents[row2][col2].spriteId = tmpSprite;

        // echange des noms de base
        String tmpBase = tileBaseNames[row1][col1];
        tileBaseNames[row1][col1] = tileBaseNames[row2][col2];
        tileBaseNames[row2][col2] = tmpBase;

        // echange des angles de rotation
        int tmpRot = tileRotations[row1][col1];
        tileRotations[row1][col1] = tileRotations[row2][col2];
        tileRotations[row2][col2] = tmpRot;

        Camera.RecalculateZoom(); // Force le repaint via RecalculateZoom (proxy de Repaint)
        System.out.println("swapTiles (" + row1 + "," + col1 + ") ↔ (" + row2 + "," + col2 + ")");
    }

    /**
     * Fait pivoter la tuile en position (row, col) d'un angle supplementaire.
     *
     * <p>L'angle courant est incremente de {@code angleToAdd} puis reduit modulo 360.
     * Seuls les multiples de 90 sont significatifs (0, 90, 180, 270).
     *
     * <p>Le sprite tourne est nomme {@code "<baseName>_<angle>"} (ex: {@code "NoraNoire_90"}).
     * S'il n'existe pas encore dans la Camera, l'image originale est recuperee,
     * pivotee avec {@link #rotateImage}, puis enregistree.
     *
     * @param row        Ligne de la grille (0-2).
     * @param col        Colonne de la grille (0-2).
     * @param angleToAdd Angle à ajouter en degres (typiquement 90, 180 ou 270).
     */
    public void rotateTile(int row, int col, int angleToAdd) {
        if (row < 0 || row > 2 || col < 0 || col > 2) {
            System.err.println("rotateTile — coordonnees hors-plateau : (" + row + "," + col + ")");
            return;
        }

        // 1. Mise à jour de l'angle (modulo 360, toujours positif)
        int newAngle = ((tileRotations[row][col] + angleToAdd) % 360 + 360) % 360;
        tileRotations[row][col] = newAngle;

        String baseName = tileBaseNames[row][col];

        // 2. Pour l'angle 0 on reutilise directement le sprite de base
        if (newAngle == 0) {
            tileComponents[row][col].spriteId = Camera.AddSprite(baseName);
            Camera.RecalculateZoom();
            System.out.println("rotateTile (" + row + "," + col + ") → 0° (sprite de base)");
            return;
        }

        // 3. Nom unique du sprite tourne
        String newName = baseName + "_" + newAngle;

        // 4. Verification du cache
        int spriteId = Camera.GetSpriteId(newName);
        if (spriteId == -1) {
            // 4a. Recupere l'image originale (sprite de base, jamais tourne)
            java.awt.image.BufferedImage original = Camera.GetSpriteImage(baseName);
            if (original == null) {
                // Image de base absente : on tente de la charger d'abord
                Camera.AddSprite(baseName);
                original = Camera.GetSpriteImage(baseName);
            }
            if (original == null) {
                System.err.println("rotateTile — impossible de charger l'image de base : " + baseName);
                return;
            }

            // 4b. Rotation de l'image
            java.awt.image.BufferedImage rotated = rotateImage(original, Math.toRadians(newAngle));

            // 4c. Enregistrement dans la Camera
            spriteId = Camera.AddSprite(newName, rotated);
        }

        // 5. Application du nouveau sprite au composant
        tileComponents[row][col].spriteId = spriteId;

        Camera.RecalculateZoom();
        System.out.println("rotateTile (" + row + "," + col + ") → " + newAngle + "° [sprite: " + newName + "]");
    }

    // =========================================================================
    // Mise à jour de la vue du district (appelee par Gameplay)
    // =========================================================================

    public void updateDistrictView(District district) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Quartier q = district.get(i, j);
                String baseName;
                if (q.estSuspect()) {
                    baseName = q.getPersonnage().nom.replace(" ", "");
                } else {
                    if (q.getPersonnage() == Personnage.JOSEPH_LANE) {
                        baseName = "JosephLane-verso";
                    } else {
                        baseName = "TileDefault";
                    }
                }
                // Si le nom de base change (nouveau personnage), on met à jour le sprite
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

                // Calcul du delta (rotation supplementaire à appliquer)
                int angleActuel = tileRotations[i][j];
                int delta = (angle - angleActuel) % 360;
                if (delta < 0) delta += 360;

                if (delta != 0) {
                    rotateTile(i, j, delta);
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
     * Background plein-ecran (rendu par Camera comme tout autre composant).
     */
    private void initBackgroundComponent() {
        backgroundComponent = new Composant2D(
                new Vector2(WINDOW_W / 2.0, WINDOW_H / 2.0),
                new Vector2(WINDOW_W, WINDOW_H),
                "Background"
        );
    }

    /**
     * Bande laterale gauche (LeftStrip) avec les boutons.
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
            // Affiche la face Pile par defaut
            actionBalls[i] = new Composant2D(
                    new Vector2(cx, cy),
                    new Vector2(ballDiam, ballDiam),
                    BALL_FACE_PILE[i]
            );
            actionBallCurrentSprite[i] = BALL_FACE_PILE[i]; // etat initial
        }
    }

    /**Pour afficher les jetons qui ont etes lances/retournes*/
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
            actionBallCurrentSprite[i] = spriteName; // memorise le sprite courant
        }
        Camera.Repaint();
    }

    private void initTurnIndicators() {
        double ballDiam = 60;
        double cx       = WINDOW_W - ballDiam / 2.0 - 20;
        double spacing  = BOARD_SIZE / 8.0;

        for (int i = 0; i < 8; i++) {
            double cy = BOARD_ORIGIN.y + spacing / 2.0 + i * spacing;
            // Affiche la face Pile par defaut (T1-T8)
            turnIndicators[i] = new Composant2D(
                    new Vector2(cx, cy),
                    new Vector2(ballDiam, ballDiam),
                    TURN_FACE_PILE[i]
            );
        }
    }

    /**
     * Instancie les 12 boules de contour (OuterBall) en suivant l'ordre
     * horaire defini par OUTER_POSITIONS.
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
 * Cree les deux rectangles d'indicateur de joueurs :
 *   - Joueur 1 (rouge) colle en haut au milieu de l'interface
 *   - Joueur 2 (bleu)  colle en bas au milieu de l'interface
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

            // Positionnement centre horizontalement et colle en haut/bas de l'interface
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
     *   - playerIndex == 0 : colle en haut au milieu de l'interface
     *   - playerIndex == 1 : colle en bas au milieu de l'interface
     */
    private void positionnerRectangle(JPanel rect, int playerIndex) {
        java.awt.Container parent = rect.getParent();
        if (parent == null) return;
        
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        int rectW = rect.getWidth();
        int rectH = rect.getHeight();
        
        // Centre horizontalement
        int rectX = (parentWidth - rectW) / 2;
        
        // Colle en haut (joueur 1) ou en bas (joueur 2)
        int rectY = (playerIndex == 0) 
            ? 0                           // Colle en haut
            : parentHeight - rectH - 40;       // Colle en bas
        
        rect.setLocation(rectX, rectY);
    }
    /**
     * Ajoute les boutons dans la bande laterale gauche :
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

        // "Regles"
        JButton reglesBtn = makeStripButton("Regles");
        reglesBtn.setBounds(bx, 170, bw, bh);
        reglesBtn.addActionListener(e -> onReglesPressed());
        uiOverlay.add(reglesBtn);

        // "Terminer tour" (Valider)
        validateButton = makeStripButton("Terminer tour");
        validateButton.setBounds(bx, WINDOW_H - 75, bw, bh);
        validateButton.addActionListener(e -> {
            if (gameplay != null) gameplay.onValidatePressed();
        });
        uiOverlay.add(validateButton);
    }

    /** Cree un JButton stylise pour la bande laterale. */
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

    /** Panneau de regles actuellement affiche (null = ferme). */
    private JPanel panneauRegles = null;

    private void onReglesPressed() {
        // --- Effet Toggle : si dejà ouvert, on ferme ---
        if (panneauRegles != null && panneauRegles.isShowing()) {
            fermerPanneauRegles();
            return;
        }

        // ----------------------------------------------------------------
        // 1. Creation du panneau semi-transparent plein-ecran
        // ----------------------------------------------------------------
        panneauRegles = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // Fond noir semi-transparent avec support alpha
                g.setColor(new Color(0, 0, 0, 230));
                g.fillRect(0, 0, getWidth(), getHeight());
                // Ne pas appeler super pour eviter que le L&F repeigne par-dessus
            }
        };
        panneauRegles.setOpaque(false);
        panneauRegles.setBounds(0, 0, WINDOW_W, WINDOW_H);
        panneauRegles.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // ----------------------------------------------------------------
        // 2. Contenu HTML — regles completes
        // ----------------------------------------------------------------
        String html = "<html>"
            + "<body style='color:white; font-family:SansSerif;'>"

            // Titre principal
            + "<h1 style='color:red; text-align:center; margin-bottom:6px;'>"
            + "&#x1F50E;&nbsp; Regles de Mr. Jack Pocket &nbsp;&#x1F50E;</h1>"
            + "<hr style='border:1px solid red; margin-bottom:14px;'/>"

            // ---- BUT DU JEU ----
            + "<h2 style='color:red; margin-bottom:4px;'>&#x1F3AF; But du Jeu</h2>"
            + "<ul>"
            + "<li><b>Victoire de l'Enqueteur :</b> Un seul suspect reste en jeu.</li>"
            + "<li><b>Victoire de Jack :</b> Il possede 6 sabliers "
            + "<i>ou</i> n'a pas ete capture apres 8 tours.</li>"
            + "</ul>"

            // ---- DeROULEMENT ----
            + "<h2 style='color:red; margin-top:10px; margin-bottom:4px;'>"
            + "&#x23F1; Deroulement d'un Tour</h2>"
            + "<p>Chaque tour se compose de deux etapes : "
            + "<b>La Traque</b> et <b>l'Appel à Temoins</b>.</p>"

            // La Traque
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
            + "de 1 ou 2 cases (leurs tete est sur le jeton) <i>(sens horaire)</i>.</li>"
            + "<li><b> echange :</b> echanger deux tuiles de place "
            + "sans changer leur orientation.</li>"
            + "<li><b> Rotation :</b> Faire pivoter une tuile "
            + "(90 ou 180). <i>Une seule fois par tour.</i></li>"
            + "<li><b> Joker :</b> Deplacer l'enqueteur de son choix de 0 "
            + "<i>(Jack seulement)</i> ou 1 case.</li>"
            + "<li><b> Alibi :</b> Piocher une carte Alibi. "
            + "L'Enqueteur innocente le personnage ; Jack gagne des sabliers.</li>"
            + "</ul>"

            // Appel à Temoins
            + "<h3 style='color:#FFD700; margin-top:8px; margin-bottom:2px;'>"
            + "2. L'Appel à Temoins</h3>"
            + "<p>Jack annonce s'il est <b>visible</b> "
            + "(ligne de mire d'un enqueteur sans mur) :</p>"
            + "<ul>"
            + "<li></b> Jack visible :</b> On elimine les suspects invisibles. "
            + "L'Enqueteur prend le jeton Temps.</li>"
            + "<li><b> Jack invisible :</b> On elimine les suspects visibles. "
            + "Jack prend le jeton Temps <i>(côte sablier)</i>.</li>"
            + "</ul>"

            // Fermeture
            + "<p style='text-align:center; color:#888; margin-top:16px; font-size:11px;'>"
            + "&#x2715;&nbsp; Cliquez n'importe où pour fermer</p>"
            + "</body></html>";

        JLabel labelRegles = new JLabel(html);
        labelRegles.setVerticalAlignment(SwingConstants.TOP);

        // Scroll pour les petits ecrans
        JScrollPane scroll = new JScrollPane(labelRegles,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 140, 0, 180), 2));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        panneauRegles.add(scroll, BorderLayout.CENTER);

        // ----------------------------------------------------------------
        // 3. MouseListener : clic anywhere → fermer
        // ----------------------------------------------------------------
        java.awt.event.MouseAdapter fermetureListener = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                fermerPanneauRegles();
            }
        };
        panneauRegles.addMouseListener(fermetureListener);
        // Propagation aux enfants directs pour que le clic sur le label ferme aussi
        scroll.addMouseListener(fermetureListener);
        labelRegles.addMouseListener(fermetureListener);

        // ----------------------------------------------------------------
        // 4. Ajout au premier plan (devant Camera ET uiOverlay)
        // ----------------------------------------------------------------
        this.add(panneauRegles, 0);
        this.revalidate();
        this.repaint();

        System.out.println("VueJeu — Panneau de regles affiche");
    }

    /** Retire proprement le panneau de regles de la hierarchie de composants. */
    private void fermerPanneauRegles() {
        if (panneauRegles != null) {
            this.remove(panneauRegles);
            panneauRegles = null;
            this.revalidate();
            this.repaint();
            System.out.println("VueJeu — Panneau de regles ferme");
        }
    }

    private void onRetourPressed() {
        //switchBallFace(0);
        //switchBallFace(1);
        //switchBallFace(2);
        //switchBallFace(3);
        //swapTiles(new Vector2(0,0),new Vector2(1,0));
        System.out.println("VueJeu — Retour (non implemente)");
        // À connecter à une eventuelle navigation entre ecrans
    }

    private void onNewGamePressed() {
        System.out.println("VueJeu — Nouvelle partie");
        // Recree la partie via Gameplay si necessaire
        gameplay.resetGame();
    }

    public void enableValidateButton(boolean enabled) {
        validateButton.setEnabled(enabled);
    }

    public void hideGameOverScreen() {
        for (Component comp : uiOverlay.getComponents()) {
            if (comp instanceof JLabel && "FIN DE PARTIE".equals(((JLabel) comp).getText())) {
                uiOverlay.remove(comp);
                break;
            }
        }
        uiOverlay.repaint();
    }

    private void onIAPressed() {
        System.out.println("VueJeu — Basculer mode IA");
        advanceDetective(1);
        // Exemple : demande au contrôleur de jouer l'IA pour le tour en cours
        if (gameplay != null) {
            gameplay.getControler().joueIa();
        }
    }

    // =========================================================================
    // API publique appelee par Gameplay
    // =========================================================================

    public void registerControler(IHMControler controler) {
        Component cameraComp = getContentPane().getComponent(
                getContentPane().getComponentCount() - 1
        );
        cameraComp.addMouseListener(controler);
        cameraComp.addKeyListener(controler);
        cameraComp.setFocusable(true);
        cameraComp.requestFocusInWindow();

        // Branche un MouseListener dedie aux jetons d'action (actionBalls).
        // Il est separe du contrôleur principal pour ne pas melanger
        // la logique plateau et la logique jetons.
        cameraComp.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                checkActionBallClick(e.getX(), e.getY());
            }
        });
    }

    /**
     * Verifie si les coordonnees ecran (sx, sy) tombent dans le rayon d'une
     * boule d'action. Si c'est le cas, affiche le nom de la face visible.
     *
     * <p>Les boules sont des Composant2D dont la position est en espace monde.
     * On convertit d'abord le clic ecran en espace monde via la Camera, puis
     * on compare la distance au centre de chaque boule avec son rayon monde.</p>
     *
     * @param sx Coordonnee X du clic en pixels ecran.
     * @param sy Coordonnee Y du clic en pixels ecran.
     */
    private void checkActionBallClick(int sx, int sy) {
        // Conversion ecran → monde (inverse de la projection Camera)
        Vector2 screenPos = new Vector2(sx, sy);
        Vector2 worldPos  = screenPos.Div(Camera.zoom).Add(Camera.positionHG);

        for (int i = 0; i < actionBalls.length; i++) {
            Composant2D ball = actionBalls[i];
            if (ball == null) continue;

            // Rayon en espace monde = moitie de la taille reelle (taille × echelle)
            double rayonX = (ball.taille.x * ball.echelle.x) / 2.0;
            double rayonY = (ball.taille.y * ball.echelle.y) / 2.0;
            double rayon  = Math.min(rayonX, rayonY); // boules supposees circulaires

            double dx = worldPos.x - ball.position.x;
            double dy = worldPos.y - ball.position.y;

            if (dx * dx + dy * dy <= rayon * rayon) {
                // Utilise l'etat interne synchronise plutôt que les tableaux statiques
                String nomAction = actionBallCurrentSprite[i];
                System.out.println("J'ai clique sur " + nomAction);
                String input;
                int pas;
                switch (nomAction) {
                    case "action_holmes":
                        input = JOptionPane.showInputDialog(this, "Deplacer Holmes de combien de pas ? (1 ou 2)");
                        pas = Integer.parseInt(input);
                        if (pas != 1 && pas != 2) pas = 1;
                        // Recuperer le detective Holmes (premier de la liste)
                        Detective holmes = gameplay.partie.detectives.get(0);
                        gameplay.partie.actions.deplacerDetective(holmes, pas);
                        updateDetectivesView();
                        break;
                    case "action_watson":
                        input = JOptionPane.showInputDialog(this, "Deplacer Watson de combien de pas ? (1 ou 2)");
                        pas = Integer.parseInt(input);
                        if (pas != 1 && pas != 2) pas = 1;
                        Detective watson = gameplay.partie.detectives.get(1);
                        gameplay.partie.actions.deplacerDetective(watson, pas);
                        updateDetectivesView();
                        break;
                    case "action_toby":
                        input = JOptionPane.showInputDialog(this, "Deplacer le chien tout mignon Toby de combien de pas ? (1 ou 2)");
                        pas = Integer.parseInt(input);
                        if (pas != 1 && pas != 2) pas = 1;
                        Detective toby = gameplay.partie.detectives.get(2);
                        gameplay.partie.actions.deplacerDetective(toby, pas);
                        updateDetectivesView();
                        break;
                    case "action_joker":
                        Partie.Joueur joueur = gameplay.partie.joueurCourant;
                        if (joueur == Partie.Joueur.ENQUETEUR) {
                            String[] options = {"Holmes", "Watson", "Toby"};
                            int choix = JOptionPane.showOptionDialog(this,
                                    "Quel detective voulez-vous deplacer d’un pas ?",
                                    "Action Joker",
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    options,
                                    options[0]);
                            if (choix < 0) break;
                            Detective detective = switch (choix) {
                                case 0 -> gameplay.partie.detectives.get(0);
                                case 1 -> gameplay.partie.detectives.get(1);
                                case 2 -> gameplay.partie.detectives.get(2);
                                default -> null;
                            };
                            if (detective != null) {
                                gameplay.partie.actions.joker(detective);
                                updateDetectivesView();
                            }
                        } else { // Mr. Jack
                            String[] options = {"Holmes", "Watson", "Toby", "Ne rien deplacer"};
                            int choix = JOptionPane.showOptionDialog(this,
                                    "Choisissez une action (deplacer un detective d'un pas ou rien)",
                                    "Action Joker - Mr. Jack",
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    options,
                                    options[0]);
                            if (choix < 0) break;
                            Detective detective = switch (choix) {
                                case 0 -> gameplay.partie.detectives.get(0);
                                case 1 -> gameplay.partie.detectives.get(1);
                                case 2 -> gameplay.partie.detectives.get(2);
                                default -> null;
                            };
                            gameplay.partie.actions.joker(detective);
                            updateDetectivesView();
                        }
                        break;
                    case "action_rotation":
                        gameplay.startRotationMode(i);
                        break;
                    case "action_echange":
                        gameplay.startEchange();
                        break;
                    case "action_alibi":
                        gameplay.partie.actions.alibi();
                        updateDistrictView(gameplay.partie.district);
                        //Ici on pourrait faire quelque chose pour griser le jeton qui a ete utilise
                        break;
                    default:
                        System.out.println("Action inconnue: " + nomAction);
                        break;
                }
                return; // Un seul hit par clic
            }
        }
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

    /**
     * Met à jour l'affichage des 3 detectives à partir des positions du modele.
     */
    public void updateDetectivesView() {
        List<Detective> detectives = gameplay.partie.detectives;
        for (Detective d : detectives) {
            int position = d.getPosition() + 1;
            int nom = switch (d.getType()) {
                case HOLMES -> 1;
                case WATSON -> 2;
                case TOBY  -> 3;
            };
            replaceOuterBall(position, nom);
        }
    }

    /**
     * Noms de fichiers image disponibles pour le fond d'ecran.
     * Chaque constante correspond à un fichier {@code res/Images/<nom>.png}.
     */
    public static final String BG_WHITE  = "BackgroundWhite";
    public static final String BG_PURPLE = "BackgroundPurple";
    public static final String BG_RED    = "BackgroundRed";
    public static final String BG_BLUE   = "BackgroundBlue";

    /**
     * Change le sprite du fond plein-ecran.
     *
     * @param colorName Nom du fichier sans extension (utiliser les constantes
     *                  {@link #BG_WHITE}, {@link #BG_PURPLE}, {@link #BG_RED},
     *                  {@link #BG_BLUE}).
     */
    public void setBackgroundColor(String colorName) {
        if (backgroundComponent == null) return;
        int id = Camera.AddSprite(colorName);
        if (id < 0) {
            System.err.println("setBackgroundColor — sprite introuvable : " + colorName);
            return;
        }
        backgroundComponent.spriteId = id;
        Camera.Repaint();
        System.out.println("setBackgroundColor → " + colorName);
    }

    /**
     * Adapte la couleur du fond au numero de tour au debut de celui-ci :
     * <ul>
     *   <li>Tour <b>pair</b>  (2, 4, 6, 8) → {@link #BG_RED}</li>
     *   <li>Tour <b>impair</b> (1, 3, 5, 7) → {@link #BG_BLUE}</li>
     * </ul>
     *
     * @param turn Numero du tour courant (1-8).
     */
    public void updateBackgroundForTurn(int turn) {
        if (turn +1 % 2 == 0) {
            setBackgroundColor(BG_RED);
        } else {
            setBackgroundColor(BG_BLUE);
        }
    }

    public void refreshBoardComponents() {
        Camera.Repaint();
    }

    public void showGameOverScreen() {
        validateButton.setEnabled(false);
        JLabel gameOverLabel = new JLabel("FIN DE PARTIE", SwingConstants.CENTER);
        gameOverLabel.setFont(new Font("SansSerif", Font.BOLD, 64));
        gameOverLabel.setForeground(Color.RED);
        gameOverLabel.setBounds(300, 350, 600, 100);
        uiOverlay.add(gameOverLabel);
        uiOverlay.revalidate();
        uiOverlay.repaint();
    }
}