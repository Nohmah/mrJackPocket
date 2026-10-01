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
 * 
 * VueMonde — Panneau principal de rendu du monde de jeu.
 * 
 *
 * RÔLE : Cette classe est le "panneau racine" de l'interface graphique du jeu.
 * Elle hérite de JPanel (composant Swing rectangulaire) et orchestre
 * l'affichage de toutes les parties visuelles : plateau, détectives,
 * jetons, overlays.
 *
 * PATRON FAÇADE : VueMonde ne fait presque rien elle-même ; elle délègue
 * à quatre sous-classes spécialisées :
 *
 *
 *VueMonde  (façade publique) :                             
 *     RenduJoueurs       dessine les rectangles joueur 
 *      RenduFeedback      survol, sélection, clignotement
 *      GestionDetectives  anneau de pions détective     
 *       GestionJetons      boules d'action et tour       
 * 
 *
 * IMPORTANT : VueJeu, Gameplay et IHMControler ne sont PAS modifiés.
 */
public class VueMonde extends JPanel {

    // 
    //  CHAMPS D'ÉTAT GÉNÉRAL 
    // 

    /**
     * Verrou global : si true, les clics de l'utilisateur sont ignorés.
     * Utilisé par exemple pendant une animation ou un overlay.
     * Public car d'autres classes (Gameplay) peuvent le lire directement.
     */
    public boolean jeuVerrouille = false;

    /**
     * Timer utilisé pour masquer automatiquement le panneau de notification
     * après un certain délai.
     */
    private Timer notifTimer = null;

    /**
     * Indique si une notification "temporisée" est actuellement affichée.
     * Empêche d'en afficher une nouvelle pendant ce temps.
     */
    private boolean notifEnCours = false;

    /**
     * Référence vers le médiateur Gameplay, injectée après la construction
     * via {@link #setGameplay(Gameplay)}.
     *
     * On ne passe pas Gameplay dans le constructeur pour éviter une dépendance
     * circulaire (Gameplay a besoin de VueMonde pour exister).
     */
    private Gameplay gameplay;

    // 
    //  CONSTANTES DU MONDE 
    // Ces valeurs définissent la géométrie de l'espace de jeu "virtuel".
    // La caméra peut zoomer/déplacer cette zone dans la fenêtre réelle.
    // 

    /** Largeur du monde virtuel en pixels. */
    public static final int WORLD_W = 1200;

    /** Hauteur du monde virtuel en pixels. */
    public static final int WORLD_H = 800;

    /** Largeur d'une bande latérale (zone joueurs). */
    public static final double STRIP_W = 100.0;

    /** Taille d'une tuile du plateau (carré). */
    public static final double TILE_SIZE = 150.0;

    /** Taille totale du plateau 3×3 (3 × TILE_SIZE = 450). */
    static final double BOARD_SIZE = 450.0;

    /**
     * Coordonnées du coin supérieur-gauche du plateau dans le monde virtuel.
     * Le plateau est centré horizontalement avec un léger décalage de -60
     * pour laisser de la place à l'UI à gauche.
     */
    public static final Vector2 BOARD_ORIGIN = new Vector2(
            (WORLD_W - BOARD_SIZE) / 2.0 - 60,
            (WORLD_H - BOARD_SIZE) / 2.0
    );

    // 
    //  CONSTANTES DE FOND D'ÉCRAN 
    // Noms des sprites de fond, utilisés par setBackgroundColor().
    // 

    public static final String BG_WHITE  = "BackgroundWhite";
    public static final String BG_PURPLE = "BackgroundPurple";
    public static final String BG_RED    = "BackgroundRed";
    public static final String BG_BLUE   = "BackgroundBlue";

    // 
    //  OVERLAY GÉNÉRIQUE (carte alibi, game-over, etc.) 
    // Un "overlay" est un panneau semi-transparent affiché par-dessus le jeu.
    // 

    /**
     * Référence vers le dernier panneau d'overlay affiché,
     * pour pouvoir le retirer avant d'en afficher un nouveau.
     */
    private JPanel currentOverlayPanel = null;

    // 
    //  COMPOSANTS SWING INTERNES 
    // 

    /**
     * JLayeredPane : conteneur Swing qui permet d'empiler des composants
     * en couches numérotées (DEFAULT < PALETTE < POPUP …).
     * Ici : la caméra est en couche DEFAULT, l'UI en couche PALETTE.
     */
    private final JLayeredPane layeredPane;

    /**
     * Panneau transparent superposé à la caméra.
     * Contient tous les composants Swing "flottants" :
     * bouton réglages, sablier, bulles de notification, overlays.
     */
    private JPanel uiOverlay;

    // Étiquettes et panneaux de l'UI flottante
    private JLabel gameOverLabel = null;   // Label "game over" (non utilisé actuellement)
    private JLabel sablierLabel  = null;   // Compteur de sabliers (ex: "2+?/6")
    private JPanel sablierPanel  = null;   // Conteneur du sablier
    private JPanel thinkingPanel = null;   // Bandeau "IA en réflexion..."
    private JLabel thinkingLabel = null;
    private JPanel notifPanel    = null;   // Bandeau de notification temporaire
    private JLabel notifLabel    = null;
    private JLabel actionTooltip;          // Info-bulle d'action (sur les jetons)

    /** Bouton d'accès aux réglages (engrenage). */
    private JButton settingsButton;

    /** Action à exécuter quand on clique sur le bouton réglages. */
    private Runnable settingsClick;

    /** Permet à VueJeu d'injecter l'action du bouton réglages. */
    public void setSettingsClick(Runnable r) {
        this.settingsClick = r;
    }

    /** Indique si c'est l'IA qui réfléchit (pour choisir le texte affiché). */
    private boolean iaIsThinking = true;

    /** Facteur de zoom courant, mis à jour par applyLetterbox(). */
    private double currentScale = 1.0;

    // 
    //  TUILES DU PLATEAU 3×3 
    // Le plateau est une grille de 3 lignes × 3 colonnes.
    // Chaque cellule (i,j) a un Composant2D (position + sprite),
    // un angle de rotation et le nom "de base" du sprite.
    // 

    /** Composants 2D des 9 tuiles — [ligne][colonne]. */
    private final Composant2D[][] tileComponents = new Composant2D[3][3];

    /** Angle de rotation actuel de chaque tuile, en degrés. */
    private final int[][] tileRotations = new int[3][3];

    /**
     * Nom "de base" du sprite de chaque tuile (sans suffixe d'angle).
     * Ex : "ElizabethOliver", "TileDefault", "JosephLane-verso".
     */
    private final String[][] tileBaseNames = new String[3][3];

    /**
     * Cache des sprites déjà tournés.
     * Clé : "nomSprite_angle" (ex: "TileDefault_90").
     * Valeur : identifiant du sprite dans la Camera.
     * Évite de recalculer la rotation à chaque frame.
     */
    private final Map<String, Integer> rotatedSpriteCache = new HashMap<>();

    // 
    //  DÉLÉGUÉS (sous-systèmes spécialisés) 
    // 

    private final RenduJoueurs      renduJoueurs;      // rectangles de fond joueur
    private final GestionJetons     gestionJetons;     // boules d'action + indicateur de tour
    private final GestionDetectives gestionDetectives; // anneau de pions détective
    private final RenduFeedback     renduFeedback;     // survol / sélection / clignotement

    // 
    //  INTERFACE DE CALLBACK : clics sur les boules d'action 
    // 

    /**
     * Interface fonctionnelle (1 méthode) que le contrôleur implémente
     * pour réagir quand le joueur clique sur une boule d'action.
     *
     * PATRON OBSERVATEUR : VueMonde est "observée" ; le contrôleur s'abonne
     * via setActionBallClickListener().
     */
    public interface ActionBallClickListener {
        /**
         * Appelé quand une boule d'action est cliquée.
         *
         * @param ballIndex  indice de la boule (0 à 8)
         * @param spriteName nom du sprite actif de cette boule
         */
        void onActionBallHit(int ballIndex, String spriteName);
    }

    /** Abonné courant pour les clics de boule. */
    private ActionBallClickListener actionBallClickListener;

    /** Enregistre (ou remplace) l'abonné aux clics de boule. */
    public void setActionBallClickListener(ActionBallClickListener listener) {
        this.actionBallClickListener = listener;
    }

    // 
    // CONSTRUCTEUR
    // 

    /**
     * Construit et câble tous les composants graphiques.
     *
     * Ordre d'initialisation important :
     *  1. Créer le layeredPane (conteneur maître)
     *  2. Créer RenduJoueurs (pas de dépendance Camera)
     *  3. Initialiser la Camera (doit exister avant les AddSprite)
     *  4. Créer GestionJetons et GestionDetectives (appellent Camera.AddSprite)
     *  5. Créer RenduFeedback (dépend des trois précédents)
     *  6. Initialiser les tuiles
     *  7. Construire l'UI overlay
     */
    public VueMonde() {
        // JPanel utilise un BorderLayout pour que le layeredPane
        // occupe tout l'espace disponible.
        super(new BorderLayout());
        setBackground(Color.BLACK);
        setOpaque(true);

        // Le layeredPane est le seul enfant direct de ce JPanel.
        layeredPane = new JLayeredPane();
        layeredPane.setBackground(Color.BLACK);
        layeredPane.setOpaque(true);
        add(layeredPane, BorderLayout.CENTER);

        // Étape 1 : RenduJoueurs n'a pas besoin de Camera.
        renduJoueurs = new RenduJoueurs();

        // Étape 2 : Camera doit être créée avant tous les AddSprite.
        initCamera();

        // Étapes 3-4 : les délégués font des AddSprite dans leur constructeur.
        gestionJetons     = new GestionJetons();
        gestionDetectives = new GestionDetectives();

        // Étape 5 : RenduFeedback a besoin des trois délégués précédents.
        renduFeedback = new RenduFeedback(gestionJetons, tileComponents, gestionDetectives);

        // Étapes 6-7 : plateau et UI.
        initTileComponents();
        initUIOverlay();
    }

    /**
     * Injecte la référence vers le médiateur Gameplay.
     *
     * Doit être appelé depuis VueJeu immédiatement après la création de
     * Gameplay, afin que VueMonde puisse déléguer le verrou via
     * {@code gameplay.setGlobalFreeze(...)}.
     *
     * @param gameplay le médiateur Gameplay
     */
    public void setGameplay(Gameplay gameplay) {
        this.gameplay = gameplay;
    }

    // 
    // INITIALISATIONS INTERNES
    // 

    /**
     * Crée et configure la Camera (surface de rendu 2D du monde).
     *
     * La Camera est une sous-classe de JComponent. On la surclasse ici avec
     * une classe anonyme pour y ajouter nos couches de dessin custom
     * (feedback de hover/sélection + rectangles joueurs).
     *
     * On y attache aussi le MouseListener qui détecte les clics sur les jetons.
     */
    private void initCamera() {
        // Classe anonyme : on surcharge paintComponent pour dessiner
        // nos couches par-dessus le rendu standard de la Camera.
        Camera cam = new Camera(new Vector2(WORLD_W, WORLD_H), null) {
            @Override
            public void paintComponent(Graphics g) {
                // Activer le rendu bilinéaire pour éviter les pixels grossiers
                ((Graphics2D) g).setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // 1. Dessin standard de la Camera (fond + sprites)
                super.paintComponent(g);

                // 2. Surcouche feedback : highlight de la tuile survolée
                renduFeedback.paintHoverOverlay((Graphics2D) g);

                // 3. Surcouche feedback : highlight de la tuile sélectionnée
                renduFeedback.paintSelectedOverlay((Graphics2D) g);

                // 4. Rectangles de couleur derrière chaque joueur
                renduJoueurs.paintPlayerRects((Graphics2D) g);
            }
        };

        // Positionner la caméra dans le layeredPane (couche la plus basse).
        cam.setBounds(0, 0, WORLD_W, WORLD_H);
        layeredPane.add(cam, JLayeredPane.DEFAULT_LAYER);

        // Écouter les clics de souris sur la caméra.
        // MouseAdapter implémente MouseListener avec des méthodes vides :
        // on ne surcharge que ce dont on a besoin (mouseClicked).
        cam.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Convertir les coordonnées écran  monde, puis notifier.
                notifyActionBallClick(e.getX(), e.getY());
            }
        });
    }

    /**
     * Crée les 9 Composant2D du plateau.
     *
     * Chaque composant est centré dans sa cellule (i, j).
     * On calcule le centre ainsi :
     *   centreX = BOARD_ORIGIN.x + col * TILE_SIZE + TILE_SIZE / 2
     *   centreY = BOARD_ORIGIN.y + row * TILE_SIZE + TILE_SIZE / 2
     */
    private void initTileComponents() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Vector2 centre = new Vector2(
                        BOARD_ORIGIN.x + col * TILE_SIZE + TILE_SIZE / 2.0,
                        BOARD_ORIGIN.y + row * TILE_SIZE + TILE_SIZE / 2.0
                );
                tileComponents[row][col] = new Composant2D(
                        centre,
                        new Vector2(TILE_SIZE, TILE_SIZE),
                        "TileDefault"   // sprite par défaut
                );
            }
        }
    }

    /**
     * Construit l'overlay UI (couche transparente par-dessus la caméra).
     *
     * Contient :
     *  - info-bulle d'action (texte flottant près des jetons)
     *  - bouton réglages
     *  - bandeau "IA en réflexion..."
     *  - bandeau de notification
     */
    private void initUIOverlay() {
        // null layout : on positionne manuellement chaque composant avec setBounds().
        uiOverlay = new JPanel(null);
        uiOverlay.setOpaque(false); // transparent (on voit la caméra dessous)
        uiOverlay.setBounds(0, 0, WORLD_W, WORLD_H);
        layeredPane.add(uiOverlay, JLayeredPane.PALETTE_LAYER); // couche au-dessus de la caméra

        //  Info-bulle d'action (ex: "Déplacer un détective") 
        actionTooltip = new JLabel("", SwingConstants.LEFT);
        actionTooltip.setOpaque(true);
        actionTooltip.setBackground(new Color(0, 0, 0, 100));       // noir semi-transparent
        actionTooltip.setFont(new Font("SansSerif", Font.PLAIN, 12));
        actionTooltip.setForeground(new Color(255, 240, 180));       // jaune crème
        actionTooltip.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        actionTooltip.setVisible(false); // caché par défaut
        uiOverlay.add(actionTooltip);

        initSettings();
        initThinking();
        initNotifPanel();
    }

    /**
     * Crée le bouton "réglages" (icône engrenage, coin supérieur gauche).
     *
     * Si le sprite "Settings" n'existe pas encore, on le charge.
     * On redimensionne l'image à 32×32 pixels pour le bouton.
     */
    private void initSettings() {
        // Charger l'image si elle n'est pas déjà en cache
        BufferedImage img = Camera.GetSpriteImage("Settings");
        if (img == null) {
            Camera.AddSprite("Settings");
        }
        img = Camera.GetSpriteImage("Settings");

        if (img != null) {
            // Redimensionner à 32×32 (taille affichée)
            Image scaledImg = img.getScaledInstance(32, 32, Image.SCALE_SMOOTH);

            settingsButton = new JButton(new ImageIcon(scaledImg));
            // Supprimer l'apparence "bouton" standard pour ne garder que l'icône
            settingsButton.setBorderPainted(false);
            settingsButton.setContentAreaFilled(false);
            settingsButton.setFocusPainted(false);
            settingsButton.setOpaque(false);
            settingsButton.setMargin(new Insets(0, 0, 0, 0));
            settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Déléguer l'action au Runnable injecté de l'extérieur
            settingsButton.addActionListener(e -> {
                if (settingsClick != null) {
                    settingsClick.run();
                }
            });

            uiOverlay.add(settingsButton);
        }
    }

    /**
     * Crée le bandeau "Votre adversaire est en réflexion…" / "IA en réflexion".
     *
     * Ce panneau est caché par défaut et rendu visible par showThinking().
     */
    private void initThinking() {
        thinkingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        thinkingPanel.setOpaque(true);
        thinkingPanel.setBackground(new Color(0, 0, 0, 160)); // fond noir semi-transparent
        thinkingPanel.setBorder(BorderFactory.createLineBorder(
                new Color(200, 200, 200, 120), 1)); // bordure grise subtile

        thinkingLabel = new JLabel("");
        thinkingLabel.setForeground(Color.WHITE);
        thinkingLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        thinkingPanel.add(thinkingLabel);

        uiOverlay.add(thinkingPanel);
        thinkingPanel.setPreferredSize(new Dimension(320, 28));
        thinkingPanel.setBounds(60, 44, 220, 28);
        thinkingPanel.setVisible(false); // caché par défaut
    }

    /**
     * Crée le bandeau de notification (messages temporaires ou permanents).
     *
     * Structurellement identique au bandeau "thinking", mais utilisé pour
     * d'autres messages ("Jack est visible", "C'est à …", etc.).
     */
    private void initNotifPanel() {
        notifPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        notifPanel.setOpaque(true);
        notifPanel.setBackground(new Color(0, 0, 0, 160));
        notifPanel.setBorder(BorderFactory.createLineBorder(
                new Color(200, 200, 200, 120), 1));

        notifLabel = new JLabel("");
        notifLabel.setForeground(Color.WHITE);
        notifLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        notifPanel.add(notifLabel);

        uiOverlay.add(notifPanel);
        notifPanel.setPreferredSize(new Dimension(320, 28));
        notifPanel.setBounds(60, 10, 220, 28);
        notifPanel.setVisible(false); // caché par défaut
    }

    // 
    // OVERLAY GÉNÉRIQUE (affiché au-dessus de tout le jeu)
    // 

    /**
     * Affiche un overlay plein-écran semi-transparent avec une image centrée
     * et éventuellement un texte dessous, pendant {@code dureeMs} millisecondes.
     *
     * Pendant l'affichage, le jeu est gelé (jeuVerrouille / globalFreeze).
     *
     * @param nomImage       nom du sprite à afficher au centre
     * @param texteSousImage texte à afficher sous l'image (null ou vide = aucun)
     * @param dureeMs        durée d'affichage en millisecondes
     */
    public void afficherOverlayAvecImage(String nomImage, String texteSousImage, int dureeMs) {
        // Retirer un éventuel overlay précédent
        if (currentOverlayPanel != null) {
            uiOverlay.remove(currentOverlayPanel);
            currentOverlayPanel = null;
        }

        // Geler le jeu pendant l'affichage
        if (gameplay != null) {
            gameplay.setGlobalFreeze(true);
        } else {
            jeuVerrouille = true;
        }

        // Charger l'image en cache (si pas encore chargée)
        Camera.AddSprite(nomImage);
        BufferedImage imgCache = Camera.GetSpriteImage(nomImage);

        // Créer un JPanel personnalisé qui se dessine lui-même.
        // On utilise une classe anonyme car la logique de dessin est
        // spécifique à cet overlay et n'a pas besoin d'être réutilisée.
        JPanel overlayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // 1. Fond noir semi-transparent (alpha = 200 / 255 ≈ 78 %)
                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // 2. Récupérer l'image (peut avoir changé entre-temps)
                BufferedImage img = Camera.GetSpriteImage(nomImage);
                if (img == null) {
                    img = imgCache; // repli sur la version mise en cache à la création
                }

                if (img != null) {
                    // Calculer la position pour centrer l'image
                    int imgW = img.getWidth();
                    int imgH = img.getHeight();
                    int x = (getWidth() - imgW) / 2;
                    int y = (getHeight() - imgH) / 2;

                    // Si on a du texte, laisser de la place en bas pour lui
                    boolean aTexte = (texteSousImage != null && !texteSousImage.isEmpty());
                    if (aTexte) {
                        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
                        FontMetrics fm = g2.getFontMetrics();
                        int textH = fm.getHeight();
                        // On remonte l'image pour que l'ensemble image+texte soit centré
                        y = (getHeight() - imgH - textH - 30) / 2;
                    }

                    g2.drawImage(img, x, y, imgW, imgH, null);

                    // Dessiner le texte sous l'image
                    if (aTexte) {
                        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
                        g2.setColor(Color.WHITE);
                        FontMetrics fm = g2.getFontMetrics();
                        int textX = (getWidth() - fm.stringWidth(texteSousImage)) / 2;
                        int textY = y + imgH + fm.getHeight() + 10;
                        g2.drawString(texteSousImage, textX, textY);
                    }
                } else {
                    // Pas d'image : afficher le texte seul (mode dégradé)
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

        // Dimensionner l'overlay à la taille courante du uiOverlay
        int w = uiOverlay.getWidth();
        int h = uiOverlay.getHeight();
        if (w <= 0 || h <= 0) {
            // Si uiOverlay n'a pas encore été disposé, remonter au parent
            java.awt.Container parent = uiOverlay.getParent();
            if (parent != null) {
                w = parent.getWidth();
                h = parent.getHeight();
            }
        }
        if (w <= 0 || h <= 0) {
            // Ultime repli : taille du monde virtuel
            w = WORLD_W;
            h = WORLD_H;
        }

        overlayPanel.setBounds(0, 0, w, h);
        uiOverlay.add(overlayPanel, JLayeredPane.POPUP_LAYER);
        currentOverlayPanel = overlayPanel;
        uiOverlay.revalidate();
        uiOverlay.repaint();

        // Timer Swing (thread EDT) : retire l'overlay après dureeMs
        new javax.swing.Timer(dureeMs, e -> {
            uiOverlay.remove(overlayPanel);
            if (currentOverlayPanel == overlayPanel) {
                currentOverlayPanel = null;
            }
            // Dégeler le jeu
            if (gameplay != null) {
                gameplay.setGlobalFreeze(false);
            } else {
                jeuVerrouille = false;
            }
            uiOverlay.revalidate();
            uiOverlay.repaint();
        }).start();
    }

    /**
     * Raccourci : affiche la carte alibi d'un personnage (son image) pendant 2 s.
     *
     * @param personnage le personnage dont on montre la carte
     */
    public void afficherCarteAlibi(Personnage personnage) {
        afficherOverlayAvecImage(personnage.image, null, 2000);
    }

    /**
     * Affiche l'écran de fin de partie selon le vainqueur.
     *
     * - Si le vainqueur est "Jack" : overlay "VictoireJ" pendant 3 s.
     * - Sinon (Sherlock) : overlay "VictoireE" pendant 3 s.
     *
     * @param vainqueur nom du vainqueur (peut être null)
     */
    public void showGameOverScreen(String vainqueur) {
        if (vainqueur != null && vainqueur.toLowerCase().contains("jack")) {
            afficherOverlayAvecImage("VictoireJ", "Jack l'emporte", 3000);
        } else {
            afficherOverlayAvecImage("VictoireE", "Victoire de Sherlock Holmes", 3000);
        }
    }

    // 
    // SABLIER (indicateur de tours restants)
    // 

    /**
     * Initialise le panneau sablier (appelé uniquement pour le joueur humain).
     *
     * Le panneau contient :
     *  - un mini-panneau custom qui dessine le sprite "T0" (indicateur de tour)
     *  - une JLabel avec le compteur "x/max"
     */
    public void initialiserSab() {
        initSablierLabel();
    }

    /**
     * Crée et ajoute le panneau sablier à l'uiOverlay.
     * Ce panneau est positionné en haut à droite par repositionOverlayElements().
     */
    private void initSablierLabel() {
        // Panneau custom qui dessine le sprite "T0" (image du tour)
        JPanel turnIndicatorPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                BufferedImage img = Camera.GetSpriteImage("T0");
                if (img != null) {
                    // Dessiner l'image en remplissant tout le panneau
                    g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                } else {
                    // Repli texte si le sprite est manquant
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 20));
                    g2.drawString("T0", getWidth() / 2 - 10, getHeight() / 2 + 7);
                }
            }
        };
        turnIndicatorPanel.setPreferredSize(new Dimension(50, 50));
        turnIndicatorPanel.setOpaque(false);

        // Charger le sprite si nécessaire
        if (Camera.GetSpriteImage("T0") == null) {
            Camera.AddSprite("T0");
        }

        // Label compteur "sabliers / max"
        sablierLabel = new JLabel("0/6", SwingConstants.CENTER);
        sablierLabel.setForeground(Color.WHITE);
        sablierLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        sablierLabel.setOpaque(true);
        sablierLabel.setBackground(new Color(0, 0, 0, 180));
        sablierLabel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        sablierLabel.setPreferredSize(new Dimension(70, 50));

        // Panneau conteneur (row layout : indicateur | compteur)
        sablierPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        sablierPanel.setOpaque(false);
        sablierPanel.add(turnIndicatorPanel);
        sablierPanel.add(sablierLabel);
        uiOverlay.add(sablierPanel);
    }

    // 
    // NOTIFICATIONS TEMPORAIRES ET PERMANENTES
    // 

    /**
     * Affiche un message dans le bandeau de notification.
     *
     * @param text    texte à afficher
     * @param dureeMs durée d'affichage en ms ; -1 = permanent (jusqu'à hideNotifMessage)
     */
    public void showNotifMessage(String text, int dureeMs) {
        if (notifPanel == null || notifLabel == null) return;

        // Si la partie est terminée, ne pas afficher de notifications
        if (gameplay != null && gameplay.partie.isPartieTerminee()) {
            notifPanel.setVisible(false);
            return;
        }

        // Si une notification temporisée est déjà en cours, on refuse la nouvelle
        if (notifEnCours) {
            return;
        }

        // Arrêter un éventuel timer précédent
        if (notifTimer != null && notifTimer.isRunning()) {
            notifTimer.stop();
        }

        notifLabel.setText(text);
        notifPanel.setVisible(true);
        uiOverlay.revalidate();
        uiOverlay.repaint();

        if (dureeMs > 0) {
            // Mode temporaire : masquer après dureeMs, puis afficher le joueur courant
            notifEnCours = true;
            notifTimer = new Timer(dureeMs, e -> {
                notifEnCours = false;
                notifPanel.setVisible(false);
                uiOverlay.revalidate();
                uiOverlay.repaint();

                // Après la notification, afficher "C'est à [pseudo]"
                String nom;
                if (gameplay.partie.joueurCourant == Joueur.ENQUETEUR) {
                    nom = gameplay.partie.pseudoEnqueteur;
                    if (gameplay.partie.niveauEnqueteur == -1) {
                        nom += " (Enquêteur)";
                    }
                } else {
                    nom = gameplay.partie.pseudoJack;
                    if (gameplay.partie.niveauJack == -1) {
                        nom += " (Jack)";
                    }
                }

                if (!gameplay.partie.isPartieTerminee()) {
                    showNotifMessage("C'est à " + nom, -1); // message permanent
                }
            });
            notifTimer.setRepeats(false);
            notifTimer.start();
        }
        // Si dureeMs <= 0 : message permanent, pas de timer
    }

    /** Masque le bandeau de notification. */
    public void hideNotifMessage() {
        if (notifPanel != null) {
            notifPanel.setVisible(false);
            uiOverlay.revalidate();
            uiOverlay.repaint();
        }
    }

    // 
    // MESSAGE INFO (popup centré temporaire)
    // 

    /**
     * Affiche un message informatif centré (popup) pendant 2 secondes.
     *
     * Différent des notifications (bandeau haut) : ce message est centré
     * verticalement et horizontalement dans l'écran.
     *
     * @param message texte à afficher
     */
    public void showInfoMessage(String message) {
        if (uiOverlay == null) return;

        // GridBagLayout centre automatiquement son unique composant
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBounds(0, 0, uiOverlay.getWidth(), uiOverlay.getHeight());

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        label.setForeground(Color.WHITE);
        label.setOpaque(true);
        label.setBackground(new Color(0, 0, 0, 180));
        label.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        panel.add(label);
        uiOverlay.add(panel, JLayeredPane.POPUP_LAYER);
        uiOverlay.revalidate();
        uiOverlay.repaint();

        // Timer anonyme de 2 secondes — syntaxe double-accolade (inner class + bloc init)
        // Equivalent à : Timer t = new Timer(2000, e -> ...); t.setRepeats(false); t.start();
        new Timer(2000, e -> {
            uiOverlay.remove(panel);
            uiOverlay.revalidate();
            uiOverlay.repaint();
        }) {{
            setRepeats(false);
            start();
        }};
    }

    // 
    // INDICATEUR "IA EN RÉFLEXION"
    // 

    /** Rend visible le bandeau "IA en réflexion…". */
    public void showThinking() {
        if (thinkingPanel == null) return;
        thinkingPanel.setLocation(60, 54);
        thinkingPanel.setVisible(true);
        uiOverlay.revalidate();
        uiOverlay.repaint();
    }

    /** Cache le bandeau "IA en réflexion…". */
    public void hideThinking() {
        if (thinkingPanel == null) return;
        thinkingPanel.setVisible(false);
        uiOverlay.revalidate();
        uiOverlay.repaint();
    }

    /** Supprime le label "game over" (si affiché). */
    public void hideGameOverScreen() {
        if (gameOverLabel != null) {
            uiOverlay.remove(gameOverLabel);
            gameOverLabel = null;
        }
    }

    /** Force le re-dessin de la Camera et de l'overlay. */
    public void repaintWorld() {
        Camera.Repaint();
        uiOverlay.repaint();
    }

    // 
    // LETTERBOX (mise à l'échelle avec bandes noires)
    // 

    /**
     * Adapte la taille de rendu à la fenêtre disponible en préservant
     * le ratio WORLD_W / WORLD_H (mode "letterbox").
     *
     * Calcul du zoom :
     *   scaleX = availW / WORLD_W
     *   scaleY = availH / WORLD_H
     *   scale  = min(scaleX, scaleY)   le facteur limitant
     *
     * La Camera est centrée dans la zone disponible.
     *
     * @param availW largeur disponible (pixels fenêtre)
     * @param availH hauteur disponible (pixels fenêtre)
     */
    public void applyLetterbox(int availW, int availH) {
        if (availW <= 0 || availH <= 0) return;

        double scaleX = (double) availW / WORLD_W;
        double scaleY = (double) availH / WORLD_H;
        currentScale  = Math.min(scaleX, scaleY);

        // Taille réelle de la Camera après mise à l'échelle
        int camW = (int) Math.round(WORLD_W * currentScale);
        int camH = (int) Math.round(WORLD_H * currentScale);

        // Décalage pour centrer la Camera (bandes noires)
        int camX = (availW - camW) / 2;
        int camY = (availH - camH) / 2;

        // Repositionner la Camera (couche DEFAULT)
        Component[] defaultLayer = layeredPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER);
        if (defaultLayer.length > 0) {
            defaultLayer[0].setBounds(camX, camY, camW, camH);
        }

        // Repositionner l'overlay (couche PALETTE, superposé exactement à la Camera)
        Component[] paletteLayer = layeredPane.getComponentsInLayer(JLayeredPane.PALETTE_LAYER);
        if (paletteLayer.length > 0) {
            paletteLayer[0].setBounds(camX, camY, camW, camH);
        }

        // Informer la Camera du nouveau zoom
        Camera.taille = new Vector2(camW, camH);
        Camera.SetZoomCamera(new Vector2(currentScale, currentScale));

        // Reposisionner les éléments UI selon la nouvelle taille
        repositionOverlayElements(camW, camH);
    }

    /** Retourne le facteur de zoom courant. */
    public double getCurrentScale() {
        return currentScale;
    }

    /**
     * Méthode vide conservée pour compatibilité API.
     * (Le bouton réglages est créé dans initSettings())
     */
    public void createSettingsButton() {
        // Intentionnellement vide
    }

    // 
    // REPOSITIONNEMENT DES ÉLÉMENTS OVERLAY
    // 

    /**
     * Replace tous les composants de l'overlay après un redimensionnement.
     *
     * Appelé par applyLetterbox() à chaque redimensionnement de fenêtre.
     *
     * @param camW nouvelle largeur de la Camera (pixels)
     * @param camH nouvelle hauteur de la Camera (pixels)
     */
    public void repositionOverlayElements(int camW, int camH) {
        if (uiOverlay == null) return;

        uiOverlay.setSize(camW, camH);

        // Sablier  coin supérieur droit
        if (sablierPanel != null) {
            int panelW = sablierPanel.getPreferredSize().width;
            int panelH = sablierPanel.getPreferredSize().height;
            sablierPanel.setBounds(camW - panelW - 10, 10, panelW, panelH);
        }

        // Bandeau "thinking"  haut gauche, adapté en taille
        if (thinkingPanel != null) {
            int fontSize = camH / 45; // taille de police proportionnelle à la hauteur
            thinkingLabel.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            thinkingPanel.revalidate();
            thinkingPanel.setBounds(60, 54, camW / 4, camH / 18);
        }

        // Bandeau notification  haut gauche (légèrement au-dessus de thinking)
        if (notifPanel != null) {
            int fontSize = camH / 45;
            notifLabel.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            notifPanel.revalidate();
            notifPanel.setBounds(60, 10, camW / 4, camH / 18);
        }

        // Overlay (carte alibi, game over)  plein écran
        if (currentOverlayPanel != null) {
            currentOverlayPanel.setBounds(0, 0, camW, camH);
            currentOverlayPanel.revalidate();
        }

        // Bouton réglages  coin supérieur gauche
        if (settingsButton != null) {
            int w = settingsButton.getPreferredSize().width;
            int h = settingsButton.getPreferredSize().height;
            settingsButton.setBounds(12, 12, w, h);
        }

        uiOverlay.revalidate();
    }

    // 
    // MISE À JOUR VISUELLE (appelée par VueJeu / Gameplay)
    // 

    /**
     * Met à jour l'affichage du compteur de sabliers.
     *
     * @param sabliers    nombre de sabliers actuels
     * @param maxSabliers nombre maximum de sabliers
     */
    public void updateSabliers(int sabliers, int maxSabliers) {
        if (sablierLabel == null) return;
        sablierLabel.setText(sabliers + "+?/" + maxSabliers);
        sablierLabel.setBackground(new Color(180, 0, 0, 200));
        sablierLabel.setForeground(Color.RED);
        sablierLabel.repaint();
    }

    /**
     * Met à jour l'affichage de toutes les tuiles du plateau.
     *
     * Pour chaque tuile (i, j) du District :
     *  - détermine le bon sprite (face recto ou verso)
     *  - applique la rotation correspondant à l'orientation du mur
     *
     * @param district le modèle du plateau à afficher
     */
    public void updateDistrictView(District district) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Quartier q = district.get(i, j);

                // Choisir le bon sprite selon la face visible de la tuile
                String baseName;
                if (q.estFaceSuspect()) {
                    // Face "suspect" : nom du personnage sans espaces
                    baseName = q.getPersonnage().nom.replace(" ", "");
                } else {
                    // Face "cachée" : Joseph Lane a son propre verso
                    baseName = (q.getPersonnage() == Personnage.JOSEPH_LANE)
                            ? "JosephLane-verso"
                            : "TileDefault";
                }

                // Changer le sprite seulement si nécessaire (optimisation)
                if (!baseName.equals(tileBaseNames[i][j])) {
                    tileBaseNames[i][j]        = baseName;
                    tileRotations[i][j]        = 0;
                    tileComponents[i][j].spriteId = Camera.AddSprite(baseName);
                }

                // Calculer l'angle cible selon l'orientation du mur de la tuile
                // Convention : SUD = 0°, EST = -90°, NORD = 180°, OUEST = 90°
                int angleCible;
                switch (q.getOrientationMur()) {
                    case EST:   angleCible = -90; break;
                    case NORD:  angleCible = 180; break;
                    case OUEST: angleCible = 90;  break;
                    case SUD:
                    default:    angleCible = 0;   break;
                }

                // Calculer la rotation à appliquer (delta depuis l'angle actuel)
                int delta = (angleCible - tileRotations[i][j]) % 360;
                if (delta < 0) {
                    delta += 360; // garantir un résultat positif
                }

                if (delta != 0) {
                    rotateTile(i, j, delta);
                }
            }
        }
    }

    /**
     * Met à jour la vue des jetons d'action (délégation  GestionJetons).
     *
     * @param jetons liste des jetons d'action
     */
    public void updateJetons(List<JetonAction> jetons) {
        gestionJetons.updateJetons(jetons);
    }

    /**
     * Met à jour la vue des détectives (délégation  GestionDetectives).
     *
     * @param detectives liste des détectives
     */
    public void updateDetectivesView(List<Detective> detectives) {
        gestionDetectives.updateDetectivesView(detectives);
    }

    /**
     * Met à jour l'indicateur de tour (délégation  GestionJetons).
     *
     * @param turn numéro du tour courant
     */
    public void updateTurnIndicator(int turn) {
        gestionJetons.updateTurnIndicator(turn);
    }

    /**
     * Met à jour le fond d'écran et les rectangles joueurs.
     *
     * Règles fond :
     *  - Poursuite active  violet
     *  - Tour de Jack      rouge
     *  - Tour Enquêteur    bleu
     *
     * @param joueurCourant          quel joueur joue actuellement
     * @param coursePoursuiteActive  true si le mode poursuite est actif
     */
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

    /**
     * Met à jour le bandeau "IA en réflexion".
     *
     * @param enReflexion true = montrer, false = cacher
     * @param iaEnJeo     true = c'est l'IA qui joue, false = adversaire humain
     */
    public void updateThinking(boolean enReflexion, boolean iaEnJeo) {
        this.iaIsThinking = iaEnJeo;
        thinkingLabel.setText(iaIsThinking
                ? "IA en train de chercher un coup..."
                : "Votre adversaire est en réflexion...");
        if (enReflexion) {
            showThinking();
        } else {
            hideThinking();
        }
    }

    // 
    // DÉLÉGATIONS — RenduJoueurs
    // 

    /**
     * Modifie le nom affiché pour un joueur.
     *
     * @param player  indice du joueur (0 ou 1)
     * @param newName nouveau pseudo
     */
    public void setPlayerName(int player, String newName) {
        renduJoueurs.setPlayerName(player, newName);
    }

    /**
     * Modifie le type (humain / IA) affiché pour un joueur.
     *
     * @param player  indice du joueur (0 ou 1)
     * @param newType libellé du type
     */
    public void setPlayerType(int player, String newType) {
        renduJoueurs.setPlayerType(player, newType);
    }

    /**
     * Change la couleur du rectangle d'un joueur.
     *
     * @param player indice du joueur
     * @param color  nouvelle couleur
     */
    public void updateRectColor(int player, Color color) {
        renduJoueurs.updateRectColor(player, color);
    }

    // 
    // DÉLÉGATIONS — RenduFeedback
    // 

    /**
     * Sélectionne visuellement la tuile (row, col).
     * Un highlight est dessiné autour d'elle au prochain repaint.
     */
    public void setTuileSelected(int row, int col) {
        renduFeedback.setTuileSelected(row, col);
        Camera.Repaint();
    }

    /**
     * Met à jour l'info-bulle lorsque la souris survole un jeton.
     *
     * - Si index < 0 ou jeton déjà utilisé  cache l'info-bulle.
     * - Sinon  calcule le texte, positionne et affiche l'info-bulle.
     *
     * @param index indice du jeton survolé (-1 = aucun)
     */
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
                // Positionner l'info-bulle légèrement décalée par rapport au jeton
                Point p = renduFeedback.getActionBallScreenPos(index);
                actionTooltip.setLocation(p.x + 20, p.y - 40);
                actionTooltip.setVisible(true);
            }
        }

        renduFeedback.setHoveredTokenIndex(index);
        Camera.Repaint();
    }

    /**
     * Marque visuellement un jeton comme "actif" (sélectionné pour une action).
     *
     * @param index indice du jeton (-1 = aucun)
     */
    public void setJetonActif(int index) {
        renduFeedback.setJetonActif(index);
    }

    // 
    // DÉLÉGATIONS — GestionJetons
    // 

    /**
     * Retourne l'indice de la boule d'action aux coordonnées écran (sx, sy),
     * ou -1 si aucune boule n'est à cet endroit.
     */
    public void switchBallFace(int ballIndex) {
        gestionJetons.switchBallFace(ballIndex);
    }

    /** Bascule la face de l'indicateur de tour à l'indice donné. */
    public void switchTurnFace(int turnIndex) {
        gestionJetons.switchTurnFace(turnIndex);
    }

    /** Retourne true si l'indicateur de tour à l'indice donné est sur "pile". */
    public boolean isTurnFacePile(int turnIndex) {
        return gestionJetons.isTurnFacePile(turnIndex);
    }

    /**
     * Retourne l'indice de la boule d'action qui se trouve aux coordonnées
     * écran (sx, sy), ou -1 si aucune.
     */
    public int actionBallAt(int sx, int sy) {
        Vector2 worldPos = toWorld(new Vector2(sx, sy));
        return gestionJetons.actionBallAt(worldPos);
    }

    // 
    // DÉLÉGATIONS — GestionDetectives
    // 

    /**
     * Remplace le pion détective à la position donnée par le détective numéro
     * {@code detectiveNum}.
     */
    public void replaceOuterBall(int position, int detectiveNum) {
        gestionDetectives.replaceOuterBall(position, detectiveNum);
    }

    // 
    // MANIPULATION DES TUILES
    // 

    /**
     * Échange visuellement deux tuiles du plateau.
     *
     * Les deux tuiles sont identifiées par leurs coordonnées (col, row)
     * encapsulées dans des Vector2.
     *
     * @param pos1 (col, row) de la première tuile
     * @param pos2 (col, row) de la deuxième tuile
     */
    public void swapTiles(Vector2 pos1, Vector2 pos2) {
        int r1 = (int) pos1.y, c1 = (int) pos1.x;
        int r2 = (int) pos2.y, c2 = (int) pos2.x;

        // Vérifier que les indices sont valides
        if (r1 < 0 || r1 > 2 || c1 < 0 || c1 > 2 || r2 < 0 || r2 > 2 || c2 < 0 || c2 > 2) return;
        if (r1 == r2 && c1 == c2) return; // même tuile, rien à faire

        // Échanger les trois tableaux en parallèle (via variables temporaires)
        int    tmpSpriteId  = tileComponents[r1][c1].spriteId;
        tileComponents[r1][c1].spriteId = tileComponents[r2][c2].spriteId;
        tileComponents[r2][c2].spriteId = tmpSpriteId;

        String tmpBaseName  = tileBaseNames[r1][c1];
        tileBaseNames[r1][c1] = tileBaseNames[r2][c2];
        tileBaseNames[r2][c2] = tmpBaseName;

        int    tmpRotation  = tileRotations[r1][c1];
        tileRotations[r1][c1] = tileRotations[r2][c2];
        tileRotations[r2][c2] = tmpRotation;

        Camera.RecalculateZoom();
    }

    /**
     * Fait tourner la tuile (row, col) d'un angle additionnel {@code angleToAdd}.
     *
     * Algorithme :
     *  1. Calculer le nouvel angle total (modulo 360, toujours positif)
     *  2. Si angle = 0  utiliser directement le sprite de base
     *  3. Sinon  chercher un sprite pré-tourné en cache ; si absent, le créer
     *
     * @param row        ligne de la tuile (0-2)
     * @param col        colonne de la tuile (0-2)
     * @param angleToAdd angle à ajouter en degrés (peut être négatif)
     */
    public void rotateTile(int row, int col, int angleToAdd) {
        if (row < 0 || row > 2 || col < 0 || col > 2) return;

        // Calcul du nouvel angle, normalisé dans [0, 360[
        int newAngle = ((tileRotations[row][col] + angleToAdd) % 360 + 360) % 360;
        tileRotations[row][col] = newAngle;

        String baseName = tileBaseNames[row][col];

        if (newAngle == 0) {
            // Pas de rotation : utiliser le sprite original
            tileComponents[row][col].spriteId = Camera.AddSprite(baseName);
            Camera.RecalculateZoom();
            return;
        }

        // Chercher un sprite déjà tourné dans le cache de la Camera
        String spriteName = baseName + "_" + newAngle;
        int spriteId = Camera.GetSpriteId(spriteName);

        if (spriteId == -1) {
            // Pas en cache  créer le sprite tourné
            BufferedImage original = Camera.GetSpriteImage(baseName);
            if (original == null) {
                Camera.AddSprite(baseName);
                original = Camera.GetSpriteImage(baseName);
            }
            if (original == null) return; // sprite introuvable, abandonner

            // rotateImage() retourne une nouvelle BufferedImage tournée
            spriteId = Camera.AddSprite(spriteName,
                    rotateImage(original, Math.toRadians(newAngle)));
        }

        tileComponents[row][col].spriteId = spriteId;
        Camera.RecalculateZoom();
    }

    // 
    // ASSOMBRISSEMENT (dimming) DES TUILES
    // 

    /**
     * Assombrit toutes les tuiles sauf celles indiquées dans le masque,
     * et sauf les pions détectives.
     *
     * Utilisé pour mettre en évidence les tuiles accessibles lors d'un déplacement.
     *
     * @param masqueTuiles [row][col] = true  cette tuile reste visible (non assombrie)
     */
    public void appliquerAssombrissement(boolean[][] masqueTuiles) {
        // Construire la liste des composants exemptés d'assombrissement
        java.util.List<Composant2D> exempts = new java.util.ArrayList<>();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (masqueTuiles[row][col] && tileComponents[row][col] != null) {
                    exempts.add(tileComponents[row][col]);
                }
            }
        }

        // Les détectives sont toujours exemptés
        for (Composant2D det : gestionDetectives.detectiveComponents) {
            if (det != null) {
                exempts.add(det);
            }
        }

        Camera.ActiverDimming(exempts);
    }

    /** Retire l'assombrissement (remet toutes les tuiles à leur luminosité normale). */
    public void retirerAssombrissement() {
        Camera.DesactiverDimming();
    }

    // 
    // FOND D'ÉCRAN
    // 

    /**
     * Change l'image de fond d'écran.
     *
     * @param colorName une des constantes BG_WHITE, BG_PURPLE, BG_RED, BG_BLUE
     */
    public void setBackgroundColor(String colorName) {
        Camera.setBackgroundImage(colorName);
    }

    // 
    // ACCÈS AUX COMPOSANTS INTERNES
    // 

    /** Retourne le JLayeredPane (utilisé par VueJeu pour ajouter d'autres couches). */
    public JLayeredPane getLayeredPane() {
        return layeredPane;
    }

    /** Retourne l'overlay UI (utilisé par VueJeu pour y ajouter des composants). */
    public JPanel getUiOverlay() {
        return uiOverlay;
    }

    // 
    // UTILITAIRES PRIVÉS
    // 

    /**
     * Convertit des coordonnées écran (pixels de la Camera) en coordonnées monde.
     *
     * Formule :  monde = (écran / zoom) + positionHautGauche
     *
     * @param screen coordonnées en pixels sur l'écran
     * @return coordonnées dans le monde virtuel
     */
    private Vector2 toWorld(Vector2 screen) {
        return screen.Div(Camera.zoom).Add(Camera.positionHG);
    }

    /**
     * Convertit le clic écran en coordonnées monde et notifie l'abonné
     * si une boule d'action se trouve à cet endroit.
     *
     * Ne fait rien si le jeu est verrouillé.
     *
     * @param sx coordonnée X du clic (pixels écran)
     * @param sy coordonnée Y du clic (pixels écran)
     */
    private void notifyActionBallClick(int sx, int sy) {
        if (jeuVerrouille) return;
        Vector2 worldPos = toWorld(new Vector2(sx, sy));
        gestionJetons.notifyActionBallClick(worldPos, actionBallClickListener);
    }

    /**
     * Crée une nouvelle BufferedImage en faisant tourner {@code img} de {@code angle} radians.
     *
     * La taille de l'image résultante est calculée pour contenir entièrement
     * l'image tournée (pas de découpage).
     *
     * Formule taille : nw = w·|cos| + h·|sin| ; nh = h·|cos| + w·|sin|
     *
     * @param img   image source (null retourne null)
     * @param angle angle en radians (sens trigonométrique)
     * @return nouvelle image tournée, ou null si img est null
     */
    public static BufferedImage rotateImage(BufferedImage img, double angle) {
        if (img == null) return null;

        int w = img.getWidth();
        int h = img.getHeight();

        // Calcul de la nouvelle taille pour ne pas couper les coins
        double sinA = Math.abs(Math.sin(angle));
        double cosA = Math.abs(Math.cos(angle));
        int nw = (int) Math.floor(w * cosA + h * sinA);
        int nh = (int) Math.floor(h * cosA + w * sinA);

        // Créer l'image de destination (ARGB = avec canal alpha/transparence)
        BufferedImage rotated = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = rotated.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);

        // Construire la transformation affine :
        //   1. Translater pour centrer dans la nouvelle image
        //   2. Faire pivoter autour du centre de l'image originale
        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
        at.translate((nw - w) / 2.0, (nh - h) / 2.0);
        at.rotate(angle, w / 2.0, h / 2.0);

        g2.drawRenderedImage(img, at);
        g2.dispose(); // libérer les ressources graphiques

        return rotated;
    }
}