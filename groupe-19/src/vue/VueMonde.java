package src.vue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import src.modele.*;

/**
 * VueMonde — Panneau de rendu du monde de jeu (façade fine).
 *
 * Conserve l'intégralité de l'API publique et délègue vers quatre classes :
 *   - RenduJoueurs      (rectangles joueurs)
 *   - RenduFeedback     (survol, sélection, clignotement)
 *   - GestionDetectives (anneau de détectives)
 *   - GestionJetons     (boules d'action, indicateurs de tour)
 *
 * VueJeu, Gameplay et IHMControler ne sont PAS modifiés.
 */
public class VueMonde extends JPanel {

    /** true si les interactions sont verrouillées (animation en cours, etc.) */
    public boolean jeuVerrouille = false;
    private Timer notifTimer = null;
    private boolean notifEnCours = false;
    /**
     * Référence vers le médiateur Gameplay, injectée après construction via
     * {@link #setGameplay(Gameplay)}. Utilisée pour déléguer les changements
     * d'état de verrou à {@code gameplay.setGlobalFreeze(...)}.
     */
    private Gameplay gameplay;

    // =========================================================================
    // Constantes monde (référencées statiquement depuis l'extérieur)
    // =========================================================================

    public static final int     WORLD_W      = 1200;
    public static final int     WORLD_H      = 800;
    public static final double  STRIP_W      = 100.0;
    public static final double  TILE_SIZE    = 150.0;
    static final        double  BOARD_SIZE   = 450.0;
    public static final Vector2 BOARD_ORIGIN = new Vector2(
            (WORLD_W - BOARD_SIZE) / 2.0 - 60,
            (WORLD_H - BOARD_SIZE) / 2.0
    );

    // =========================================================================
    // Fonds d'écran
    // =========================================================================

    public static final String BG_WHITE  = "BackgroundWhite";
    public static final String BG_PURPLE = "BackgroundPurple";
    public static final String BG_RED    = "BackgroundRed";
    public static final String BG_BLUE   = "BackgroundBlue";

    // =========================================================================
    // Overlay générique
    // =========================================================================

    private JPanel currentOverlayPanel = null;

    public void afficherOverlayAvecImage(String nomImage, String texteSousImage, int dureeMs) {
        if (currentOverlayPanel != null) {
            uiOverlay.remove(currentOverlayPanel);
            currentOverlayPanel = null;
        }
        if (gameplay != null) gameplay.setGlobalFreeze(true); else jeuVerrouille = true;
        Camera.AddSprite(nomImage);
        BufferedImage img = Camera.GetSpriteImage(nomImage);

        JPanel overlayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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
        int w = uiOverlay.getWidth();
        int h = uiOverlay.getHeight();
        if (w <= 0 || h <= 0) {
            java.awt.Container parent = uiOverlay.getParent();
            if (parent != null) { w = parent.getWidth(); h = parent.getHeight(); }
        }
        if (w <= 0 || h <= 0) { w = WORLD_W; h = WORLD_H; }
        overlayPanel.setBounds(0, 0, w, h);
        uiOverlay.add(overlayPanel, JLayeredPane.POPUP_LAYER);
        currentOverlayPanel = overlayPanel;
        uiOverlay.revalidate();
        uiOverlay.repaint();
        new javax.swing.Timer(dureeMs, e -> {
            uiOverlay.remove(overlayPanel);
            if (currentOverlayPanel == overlayPanel) currentOverlayPanel = null;
            if (gameplay != null) gameplay.setGlobalFreeze(false); else jeuVerrouille = false;
            uiOverlay.revalidate();
            uiOverlay.repaint();
        }).start();
    }

    public void afficherCarteAlibi(Personnage personnage) {
        afficherOverlayAvecImage(personnage.image, null, 2000);
    }

    public void showGameOverScreen(String vainqueur) {
        String imageName;
        if (vainqueur != null && vainqueur.toLowerCase().contains("jack")) {
            afficherOverlayAvecImage("VictoireJ", "Jack l'emporte", 3000);
        } else {
            afficherOverlayAvecImage("VictoireE", "Victoire de Sherlock Holmes", 3000);
        }
    }

    // =========================================================================
    // Composants Swing internes
    // =========================================================================

    private final JLayeredPane layeredPane;
    private JPanel uiOverlay;
    private JLabel gameOverLabel  = null;
    private JLabel sablierLabel   = null;
    private JPanel sablierPanel   = null;
    private JPanel thinkingPanel  = null;
    private JLabel thinkingLabel  = null;
    private JPanel notifPanel = null;
    private JLabel notifLabel = null;
    private JLabel actionTooltip;

    private JButton settingsButton;
    private Runnable settingsClick;
    public void setSettingsClick(Runnable r) { this.settingsClick = r; }

    private boolean iaIsThinking = true;
    private double  currentScale = 1.0;

    // =========================================================================
    // Composants internes partagés
    // =========================================================================

    private final Composant2D[][] tileComponents  = new Composant2D[3][3];
    private final int[][]         tileRotations   = new int[3][3];
    private final String[][]      tileBaseNames   = new String[3][3];

    /** Cache de sprites tournés (clé = "nom_angle"). */
    private final Map<String, Integer> rotatedSpriteCache = new HashMap<>();

    // =========================================================================
    // Délégués
    // =========================================================================

    private final RenduJoueurs      renduJoueurs;
    private final GestionJetons     gestionJetons;
    private final GestionDetectives gestionDetectives;
    private final RenduFeedback     renduFeedback;

    // =========================================================================
    // Callback clics boules d'action
    // =========================================================================

    public interface ActionBallClickListener {
        void onActionBallHit(int ballIndex, String spriteName);
    }

    private ActionBallClickListener actionBallClickListener;

    public void setActionBallClickListener(ActionBallClickListener listener) {
        this.actionBallClickListener = listener;
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

        // La Camera doit être initialisée avant les délégués qui appellent Camera.AddSprite
        renduJoueurs      = new RenduJoueurs();
        initCamera();

        // GestionJetons et GestionDetectives appellent Camera.AddSprite dans leur constructeur
        gestionJetons     = new GestionJetons();
        gestionDetectives = new GestionDetectives();
        renduFeedback     = new RenduFeedback(gestionJetons, tileComponents, gestionDetectives);

        initTileComponents();
        initUIOverlay();
    }

    /**
     * Injecte la référence vers le médiateur Gameplay.
     * Doit être appelé depuis VueJeu immédiatement après la création de Gameplay,
     * afin que VueMonde puisse déléguer les changements de verrou via
     * {@code gameplay.setGlobalFreeze(...)}.
     *
     * @param gameplay le médiateur Gameplay
     */
    public void setGameplay(Gameplay gameplay) {
        this.gameplay = gameplay;
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
                renduFeedback.paintHoverOverlay((Graphics2D) g);
                renduFeedback.paintSelectedOverlay((Graphics2D) g);
                renduJoueurs.paintPlayerRects((Graphics2D) g);
            }
        };
        cam.setBounds(0, 0, WORLD_W, WORLD_H);
        layeredPane.add(cam, JLayeredPane.DEFAULT_LAYER);

        cam.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                notifyActionBallClick(e.getX(), e.getY());
            }
        });
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

    private void initUIOverlay() {
        uiOverlay = new JPanel(null);
        uiOverlay.setOpaque(false);
        uiOverlay.setBounds(0, 0, WORLD_W, WORLD_H);
        layeredPane.add(uiOverlay, JLayeredPane.PALETTE_LAYER);

        actionTooltip = new JLabel("", SwingConstants.LEFT);
        actionTooltip.setOpaque(true);
        actionTooltip.setBackground(new Color(0, 0, 0, 100));
        actionTooltip.setFont(new Font("SansSerif", Font.PLAIN, 12));
        actionTooltip.setForeground(new Color(255, 240, 180));
        actionTooltip.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        actionTooltip.setVisible(false);
        uiOverlay.add(actionTooltip);
        initSettings();
        initThinking();
        initNotifPanel();
    }

    private void initSettings(){
        BufferedImage img = Camera.GetSpriteImage("Settings");
        if (img == null) Camera.AddSprite("Settings");
        img = Camera.GetSpriteImage("Settings");

        if (img != null) {
            Image scaled = img.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            settingsButton = new JButton(new ImageIcon(scaled));
            settingsButton.setBorderPainted(false);
            settingsButton.setContentAreaFilled(false);
            settingsButton.setFocusPainted(false);
            settingsButton.setOpaque(false);
            settingsButton.setMargin(new Insets(0,0,0,0));
            settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            settingsButton.addActionListener(e -> { if (settingsClick != null) settingsClick.run(); });
            uiOverlay.add(settingsButton);
        }
    }

    private void initThinking() {
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
        thinkingPanel.setBounds(60, 44, 220, 28);
        thinkingPanel.setVisible(false);
    }

    private void initNotifPanel() {
        notifPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        notifPanel.setOpaque(true);
        notifPanel.setBackground(new Color(0, 0, 0, 160));
        notifPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200, 120), 1));
        notifLabel = new JLabel("");
        notifLabel.setForeground(Color.WHITE);
        notifLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        notifPanel.add(notifLabel);
        uiOverlay.add(notifPanel);
        notifPanel.setPreferredSize(new Dimension(320, 28));
        notifPanel.setBounds(60, 10, 220, 28);
        notifPanel.setVisible(false);
    }

    public void initialiserSab(boolean jackHumain) {
            if (jackHumain) {
                initSablierLabel();
                // Autres éléments spécifiques au joueur humain
            }
        }

    /**
     * Affiche un message temporaire pour un certain temps où infini
     */
    public void showNotifMessage(String text, int dureeMs) {
        if (notifPanel == null || notifLabel == null) return;

        // Si il y a un affichage sous un timer (jack est visible/jack n'est pas visible) on refuse
        if (notifEnCours) {
            return;
        }

        if (notifTimer != null && notifTimer.isRunning()) {
            notifTimer.stop();
        }

        notifLabel.setText(text);
        notifPanel.setVisible(true);
        uiOverlay.revalidate();
        uiOverlay.repaint();

        if (dureeMs > 0) {
            notifEnCours = true;
            notifTimer = new Timer(dureeMs, e -> {
                notifEnCours = false;
                notifPanel.setVisible(false);
                uiOverlay.revalidate();
                uiOverlay.repaint();
                // affiche qui joue
                String notif = (gameplay.partie.joueurCourant == Joueur.ENQUETEUR) ? "Tour de l'Enquêteur" : "Tour de Jack";
                showNotifMessage(notif, -1);
                });
            notifTimer.setRepeats(false);
            notifTimer.start();
        }
    }

    public void hideNotifMessage() {
        if (notifPanel != null) {
            notifPanel.setVisible(false);
            uiOverlay.revalidate();
            uiOverlay.repaint();
        }
    }

    public void setJetonActif(int index) {
        renduFeedback.setJetonActif(index);
    }

    private void initSablierLabel() {
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
        if (Camera.GetSpriteImage("T0") == null) Camera.AddSprite("T0");

        sablierLabel = new JLabel("0/6", SwingConstants.CENTER);
        sablierLabel.setForeground(Color.WHITE);
        sablierLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        sablierLabel.setOpaque(true);
        sablierLabel.setBackground(new Color(0, 0, 0, 180));
        sablierLabel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        sablierLabel.setPreferredSize(new Dimension(70, 50));

        sablierPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        sablierPanel.setOpaque(false);
        sablierPanel.add(turnIndicatorPanel);
        sablierPanel.add(sablierLabel);
        uiOverlay.add(sablierPanel);
    }

    // =========================================================================
    // Accès au JLayeredPane / uiOverlay
    // =========================================================================

    public JLayeredPane getLayeredPane() { return layeredPane; }
    public JPanel       getUiOverlay()  { return uiOverlay; }

    // =========================================================================
    // Letterbox
    // =========================================================================

    public void applyLetterbox(int availW, int availH) {
        if (availW <= 0 || availH <= 0) return;
        double scaleX = (double) availW / WORLD_W;
        double scaleY = (double) availH / WORLD_H;
        currentScale = Math.min(scaleX, scaleY);
        int camW = (int) Math.round(WORLD_W * currentScale);
        int camH = (int) Math.round(WORLD_H * currentScale);
        int camX = (availW - camW) / 2;
        int camY = (availH - camH) / 2;

        Component[] defaultLayer = layeredPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER);
        if (defaultLayer.length > 0) defaultLayer[0].setBounds(camX, camY, camW, camH);
        Component[] paletteLayer = layeredPane.getComponentsInLayer(JLayeredPane.PALETTE_LAYER);
        if (paletteLayer.length > 0) paletteLayer[0].setBounds(camX, camY, camW, camH);

        Camera.taille = new Vector2(camW, camH);
        Camera.SetZoomCamera(new Vector2(currentScale, currentScale));
        repositionOverlayElements(camW, camH);
    }

    public double getCurrentScale() { return currentScale; }

    public void createSettingsButton(){

    }

    // =========================================================================
    // API de mise à jour visuelle (appelée par VueJeu / Gameplay)
    // =========================================================================

    public void updateSabliers(int sabliers, int maxSabliers) {
        if (sablierLabel == null) return;
        sablierLabel.setText(sabliers + "+?/" + maxSabliers);
        sablierLabel.setBackground(new Color(180, 0, 0, 200));
        sablierLabel.setForeground(Color.RED);
        sablierLabel.repaint();
    }

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

    /** Délégation → GestionJetons */
    public void updateJetons(List<JetonAction> jetons) {
        gestionJetons.updateJetons(jetons);
    }

    /** Délégation → GestionDetectives */
    public void updateDetectivesView(List<Detective> detectives) {
        gestionDetectives.updateDetectivesView(detectives);
    }

    /** Délégation → GestionJetons */
    public void updateTurnIndicator(int turn) {
        gestionJetons.updateTurnIndicator(turn);
    }

    public void updateBackground(Joueur joueurCourant, boolean coursePoursuiteActive) {
        renduJoueurs.updatePlayerRectangles(joueurCourant);
        String couleurFond;
        if (coursePoursuiteActive) {
            couleurFond = BG_PURPLE;
        } else {
            couleurFond = (joueurCourant == Joueur.JACK) ? BG_RED : BG_BLUE;
        }
        setBackgroundColor(couleurFond);
    }

    public void updateThinking(boolean enReflexion, boolean iaEnJeo) {
        this.iaIsThinking = iaEnJeo;
        thinkingLabel.setText(iaIsThinking
                ? "IA en train de chercher un coup..."
                : "Votre adversaire est entrain de réfléchir...");
        if (enReflexion) showThinking();
        else             hideThinking();
    }

    // =========================================================================
    // Délégations — RenduJoueurs
    // =========================================================================

    public void setPlayerName(int player, String newName) {
        renduJoueurs.setPlayerName(player, newName);
    }

    public void setPlayerType(int player, String newType) {
        renduJoueurs.setPlayerType(player, newType);
    }

    public void updateRectColor(int player, Color color) {
        renduJoueurs.updateRectColor(player, color);
    }

    // =========================================================================
    // Délégations — RenduFeedback
    // =========================================================================

    public void setTuileSelected(int row, int col) {
        renduFeedback.setTuileSelected(row, col);
        Camera.Repaint();
    }

    public void setHoveredToken(int index) {
        if (actionTooltip == null) return;
        if (index < 0 || gestionJetons.actionBallIsUsed[index]) {
            actionTooltip.setVisible(false);
        } else {
            String sprite = gestionJetons.actionBallCurrentSprite[index];
            String text   = renduFeedback.getActionTooltipText(sprite);
            if (text.isEmpty()) {
                actionTooltip.setVisible(false);
            } else {
                actionTooltip.setText(text);
                actionTooltip.setSize(actionTooltip.getPreferredSize());
                Point p = renduFeedback.getActionBallScreenPos(index);
                actionTooltip.setLocation(p.x + 20, p.y - 40);
                actionTooltip.setVisible(true);
            }
        }
        renduFeedback.setHoveredTokenIndex(index);
        Camera.Repaint();
    }

    // =========================================================================
    // Délégations — GestionJetons
    // =========================================================================

    public void switchBallFace(int ballIndex) {
        gestionJetons.switchBallFace(ballIndex);
    }

    public void switchTurnFace(int turnIndex) {
        gestionJetons.switchTurnFace(turnIndex);
    }

    public boolean isTurnFacePile(int turnIndex) {
        return gestionJetons.isTurnFacePile(turnIndex);
    }

    public int actionBallAt(int sx, int sy) {
        Vector2 worldPos = toWorld(new Vector2(sx, sy));
        return gestionJetons.actionBallAt(worldPos);
    }

    // =========================================================================
    // Délégations — GestionDetectives
    // =========================================================================

    public void replaceOuterBall(int position, int detectiveNum) {
        gestionDetectives.replaceOuterBall(position, detectiveNum);
    }

    // =========================================================================
    // Manipulation des tuiles
    // =========================================================================

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
    // Assombrissement
    // =========================================================================

    public void appliquerAssombrissement(boolean[][] masqueTuiles) {
        java.util.List<Composant2D> exempts = new java.util.ArrayList<>();
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                if (masqueTuiles[row][col] && tileComponents[row][col] != null)
                    exempts.add(tileComponents[row][col]);
        for (Composant2D det : gestionDetectives.detectiveComponents)
            if (det != null) exempts.add(det);
        Camera.ActiverDimming(exempts);
    }

    public void retirerAssombrissement() {
        Camera.DesactiverDimming();
    }

    // =========================================================================
    // Fond d'écran
    // =========================================================================

    public void setBackgroundColor(String colorName) {
        Camera.setBackgroundImage(colorName);
    }

    // =========================================================================
    // Thinking / réflexion
    // =========================================================================

    public void showThinking() {
        if (thinkingPanel == null) return;
        thinkingPanel.setLocation(60, 54);
        thinkingPanel.setVisible(true);
        uiOverlay.revalidate();
        uiOverlay.repaint();
    }

    public void hideThinking() {
        if (thinkingPanel == null) return;
        thinkingPanel.setVisible(false);
        uiOverlay.revalidate();
        uiOverlay.repaint();
    }

    public void hideGameOverScreen() {
        if (gameOverLabel != null) {
            uiOverlay.remove(gameOverLabel);
            gameOverLabel = null;
        }
    }

    public void repaintWorld() {
        Camera.Repaint();
        uiOverlay.repaint();
    }

    // =========================================================================
    // Repositionnement overlay
    // =========================================================================

    public void repositionOverlayElements(int camW, int camH) {
        if (uiOverlay == null) return;
        uiOverlay.setSize(camW, camH);

        if (sablierPanel != null) {
            int panelW = sablierPanel.getPreferredSize().width;
            int panelH = sablierPanel.getPreferredSize().height;
            sablierPanel.setBounds(camW - panelW - 10, 10, panelW, panelH);
        }
        if (thinkingPanel != null) {
            int fontSize = camH / 45;
            thinkingLabel.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            thinkingPanel.revalidate();
            thinkingPanel.setBounds(60, 54, camW / 4, camH / 18);
        }
        if (notifPanel != null) {
            int fontSize = camH / 45;
            notifLabel.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            notifPanel.revalidate();
            notifPanel.setBounds(60, 10, camW / 4, camH / 18);
        }
        if (currentOverlayPanel != null) {
            currentOverlayPanel.setBounds(0, 0, camW, camH);
            currentOverlayPanel.revalidate();
        }
        if (settingsButton != null) {
            int w = settingsButton.getPreferredSize().width;
            int h = settingsButton.getPreferredSize().height;
            int x = 12;
            int y = 12;
            settingsButton.setBounds(x, y, w, h);
        }
        uiOverlay.revalidate();
    }

    // =========================================================================
    // Utilitaires
    // =========================================================================

    private Vector2 toWorld(Vector2 screen) {
        return screen.Div(Camera.zoom).Add(Camera.positionHG);
    }

    private void notifyActionBallClick(int sx, int sy) {
        if (jeuVerrouille) return;
        Vector2 worldPos = toWorld(new Vector2(sx, sy));
        gestionJetons.notifyActionBallClick(worldPos, actionBallClickListener);
    }

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

    /**
     *
     * Fait apparaitre un pop up pour indiquer un message à l'écran
     * @param message : texte à afficher à l'écran
     */
    public void showInfoMessage(String message){
        if(uiOverlay == null) return;

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBounds(0,0,uiOverlay.getWidth(), uiOverlay.getHeight());

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        label.setForeground(Color.WHITE);
        label.setOpaque(true);
        label.setBackground(new Color(0,0,0,180));
        label.setBorder(BorderFactory.createEmptyBorder(12,24,12,24));

        panel.add(label);

        uiOverlay.add(panel,JLayeredPane.POPUP_LAYER);
        uiOverlay.revalidate();
        uiOverlay.repaint();

        new Timer(2000, e -> {
            uiOverlay.remove(panel);
            uiOverlay.revalidate();
            uiOverlay.repaint();
        }) {{setRepeats(false);
            start();
        }};
    }
}