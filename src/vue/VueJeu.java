package src.vue;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.List;
import src.modele.*;
import src.utils.SaveManager;
import src.vue.menus.VueMenuPrincipal;
import src.vue.regles.VueReglesPanel;

/**
 * VueJeu — Panneau principal de la fenêtre de jeu.
 *
 * RÔLE UNIQUE : orchestrer la fenêtre et coordonner les composants de haut niveau.
 *
 * Ce que VueJeu FAIT :
 *   - Configurer le layout de la JFrame (bande gauche + zone centrale).
 *   - Gérer la bande latérale gauche (leftStrip) et ses boutons Swing.
 *   - Posséder une instance de {@link VueMonde} et lui déléguer tout le rendu.
 *   - Appliquer la logique "letterbox" (mise à l'échelle préservant le ratio).
 *   - Exposer une API de mise à jour pour le médiateur (Gameplay / IHMControler).
 *   - Gérer le ticker (boucle de rafraîchissement à 60 FPS).
 *
 * STRUCTURE LAYOUT :
 *
 *   JFrame (BorderLayout)
 *    WEST   : leftStrip  (JPanel — BoxLayout vertical, largeur fixe 100px)
 *        boutons : Retour, IA, Annuler, Refaire, Visible, Règles, Identité
 *    CENTER : vueMonde   (VueMonde — JPanel contenant le JLayeredPane caméra)
 *
 * PATRON MÉDIATEUR :
 *   VueJeu  Gameplay : VueJeu crée Gameplay et lui passe «this».
 *   Gameplay pilote ensuite VueJeu via son API publique (updateXxx).
 */
public class VueJeu extends JPanel {

    //  RÉFÉRENCES VERS LES ACTEURS EXTERNES 

    /** La JFrame parente (fenêtre OS) — utilisée pour le GlassPane et le changement de panneau. */
    private final JFrame parent;

    /** Le menu principal — pour pouvoir y revenir quand on quitte la partie. */
    private final VueMenuPrincipal menuPrincipal;

    //  CONSTANTES DE LA FENÊTRE 

    /** Largeur du monde virtuel (référence pour le letterbox). */
    private static final int WINDOW_W   = 1200;

    /** Hauteur du monde virtuel (référence pour le letterbox). */
    private static final int WINDOW_H   = 800;

    /** Largeur en pixels de la bande latérale gauche (boutons). */
    private static final int STRIP_W_PX = 100;

    /**
     * Taille (largeur = hauteur) des boutons carrés de la bande latérale.
     */
    private static final int BTN_SIZE   = 60;

    /**
     * Nombre d'images par seconde du ticker.
     * 60 FPS  le ticker dort 1000/60 ≈ 16 ms entre chaque frame.
     */
    private static final int FPS        = 60;

    //  ÉTAT 

    /**
     * Contrôle la boucle du ticker.
     * Mis à false par stopGameLoop() pour arrêter proprement le thread.
     */
    private boolean partieEnCours = true;

    //  COMPOSANTS SWING DE LA FENÊTRE 

    /**
     * Bande latérale gauche contenant les boutons de contrôle.
     * Utilise un BoxLayout vertical pour empiler les boutons.
     */
    private JPanel leftStrip;

    /**
     * Panneau des règles, affiché en overlay via le GlassPane.
     * null quand le panneau est fermé.
     */
    private JPanel panneauRegles = null;

    /**
     * Panneau des paramètres (pause/settings), affiché en overlay via le GlassPane.
     * null quand le panneau est fermé.
     */
    private JPanel panneauSettings = null;

    //  DÉLÉGUÉ PRINCIPAL : VueMonde 

    /**
     * Le panneau de rendu du plateau de jeu.
     * VueJeu délègue tout le rendu graphique (tuiles, détectives, jetons…) à cette classe.
     *
     * PATRON DÉLÉGATION : au lieu d'hériter de VueMonde, VueJeu la possède
     * et redirige les appels  découplage, facilite les tests.
     */
    private final VueMonde vueMonde;

    //  MÉDIATEUR 

    /**
     * Le médiateur Gameplay.
     * Il reçoit les événements de VueJeu (clics boutons) et pilote VueJeu
     * en retour (updateXxx).
     *
     * PATRON MÉDIATEUR : VueJeu et les autres composants ne se connaissent pas
     * directement ; ils passent tous par Gameplay.
     */
    private Gameplay gameplay;

    // CONSTRUCTEUR

    /**
     * Construit la vue de jeu et câble tous ses composants.
     *
     * Ordre d'initialisation obligatoire :
     *  1. Créer VueMonde (initialise la Camera en interne)
     *  2. Câbler les callbacks sur VueMonde
     *  3. Construire le layout de la JFrame
     *  4. Créer Gameplay (nécessite que la vue soit prête)
     *  5. Injecter Gameplay dans VueMonde (pour le verrou global)
     *  6. Positionner les détectives initiaux
     *  7. Lancer le ticker dans un thread séparé
     *
     * @param parent        la JFrame hôte
     * @param menuPrincipal le menu principal (pour le bouton Retour)
     * @param partie        le modèle de partie déjà initialisé
     */
    public VueJeu(JFrame parent, VueMenuPrincipal menuPrincipal, Partie partie) {
        this.parent        = parent;
        this.menuPrincipal = menuPrincipal;

        //  Étape 1 : créer VueMonde avant tout (initialise la Camera) 
        vueMonde = new VueMonde();

        //  Étape 2 : câbler les callbacks 

        // Quand l'utilisateur clique sur le bouton ⚙ dans VueMonde, ouvrir les settings.
        vueMonde.setSettingsClick(this::onSettingsPressed);

        // Quand l'utilisateur clique sur une boule d'action, notifier le contrôleur.
        // On utilise une expression lambda (raccourci pour une classe anonyme fonctionnelle).
        vueMonde.setActionBallClickListener(new VueMonde.ActionBallClickListener() {
            @Override
            public void onActionBallHit(int ballIndex, String spriteName) {
                if (gameplay != null) {
                    gameplay.getControler().onActionBallHit(ballIndex, spriteName);
                }
            }
        });

        //  Étape 3 : construire le layout 
        initFrame();       // configure le BorderLayout du JPanel racine
        initLeftStrip();   // crée la bande gauche avec les boutons
        initCenterPane();  // positionne vueMonde en CENTER
        initResizeListener(); // écoute les redimensionnements pour le letterbox

        //  Étape 4 : créer le médiateur (nécessite que la vue soit prête) 
        vueMonde.initialiserSab(); // initialise le compteur de sabliers AVANT Gameplay
        this.gameplay = new Gameplay(this, partie);

        //  Étape 5 : injecter Gameplay dans VueMonde 
        // Nécessaire pour que VueMonde puisse appeler gameplay.setGlobalFreeze()
        vueMonde.setGameplay(this.gameplay);

        //  Étape 6 : positions initiales des 3 détectives sur l'anneau 
        // L'anneau comporte 16 positions (1-16) ; les détectives démarrent en 12, 4, 8.
        vueMonde.replaceOuterBall(12, 1); // détective n°1  position 12
        vueMonde.replaceOuterBall(4,  2); // détective n°2  position 4
        vueMonde.replaceOuterBall(8,  3); // détective n°3  position 8

        //  Étape 7 : lancer le ticker dans un thread daemon 
        // Un Thread séparé évite de bloquer l'EDT (Event Dispatch Thread) de Swing.
        new Thread(this::startTicker).start();
    }

    // ENREGISTREMENT DU CONTRÔLEUR

    /**
     * Branche le contrôleur IHMControler sur la Camera (souris + clavier).
     *
     * Pourquoi passer par VueMonde ?
     * La Camera (surface de rendu) est enfouie dans le JLayeredPane de VueMonde.
     * VueJeu n'y a accès qu'indirectement via getLayeredPane().
     *
     * @param controler le contrôleur à enregistrer
     */
    public void registerControler(IHMControler controler) {
        // Récupérer le premier composant de la couche DEFAULT (= la Camera)
        Component cam = vueMonde.getLayeredPane()
                .getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)[0];

        cam.addMouseListener(controler);       // clics
        cam.addMouseMotionListener(controler); // déplacements (survol des jetons)
        cam.addKeyListener(controler);         // touches clavier
        cam.setFocusable(true);                // obligatoire pour recevoir les keyEvents
        cam.requestFocusInWindow();

        // Note : le MouseAdapter de détection des boules d'action est déjà posé
        // dans VueMonde.initCamera() — pas besoin de le remettre ici.
    }

    // API DE MISE À JOUR VISUELLE (appelée par Gameplay / IHMControler)
    // Toutes ces méthodes sont de pures délégations vers VueMonde.
    // VueJeu ne fait que transférer l'appel : aucune logique ici.

    /**
     * Met à jour l'affichage du compteur de sabliers de Jack.
     *
     * @param sabliers    nombre actuel de sabliers
     * @param maxSabliers maximum possible
     */
    public void updateSabliers(int sabliers, int maxSabliers) {
        vueMonde.updateSabliers(sabliers, maxSabliers);
    }

    /**
     * Met à jour les 9 tuiles du plateau selon l'état du district.
     *
     * @param district le modèle du district (3×3 quartiers)
     */
    public void updateDistrictView(District district) {
        vueMonde.updateDistrictView(district);
    }

    /**
     * Met à jour les 4 jetons d'action selon leur face visible.
     *
     * @param jetons liste des jetons d'action
     */
    public void updateJetons(List<JetonAction> jetons) {
        vueMonde.updateJetons(jetons);
    }

    /**
     * Met à jour l'affichage des 3 détectives à partir des positions du modèle.
     *
     * @param detectives liste des détectives
     */
    public void updateDetectivesView(List<Detective> detectives) {
        vueMonde.updateDetectivesView(detectives);
    }

    /**
     * Met en évidence l'indicateur du tour courant.
     *
     * @param turn numéro du tour (0-based)
     */
    public void updateTurnIndicator(int turn) {
        vueMonde.updateTurnIndicator(turn);
    }

    /**
     * Adapte la couleur de fond au joueur courant et à l'état de la poursuite.
     *
     * @param joueurCourant         le joueur dont c'est le tour
     * @param coursePoursuiteActive true si le mode poursuite est actif
     */
    public void updateBackground(Joueur joueurCourant, boolean coursePoursuiteActive) {
        vueMonde.updateBackground(joueurCourant, coursePoursuiteActive);
    }

    /**
     * Bascule la face d'un indicateur de tour (Pile  Face).
     *
     * @param turnIndex indice de l'indicateur
     */
    public void switchTurnFace(int turnIndex) {
        vueMonde.switchTurnFace(turnIndex);
    }

    /**
     * Retourne true si l'indicateur de tour à l'indice donné est sur "pile".
     *
     * @param turnIndex indice de l'indicateur
     */
    public boolean isTurnFacePile(int turnIndex) {
        return vueMonde.isTurnFacePile(turnIndex);
    }

    /**
     * Bascule la face d'une boule d'action (Pile  Face).
     *
     * @param ballIndex indice de la boule (0-8)
     */
    public void switchBallFace(int ballIndex) {
        vueMonde.switchBallFace(ballIndex);
    }

    /**
     * Modifie la couleur du rectangle d'indicateur d'un joueur.
     *
     * @param player indice du joueur (0 ou 1)
     * @param color  nouvelle couleur
     */
    public void updateRectColor(int player, Color color) {
        vueMonde.updateRectColor(player, color);
    }

    /**
     * Affiche l'écran de fin de partie.
     *
     * @param vainqueur nom du vainqueur
     */
    public void showGameOverScreen(String vainqueur) {
        vueMonde.showGameOverScreen(vainqueur);
    }

    /** Masque l'écran de fin de partie. */
    public void hideGameOverScreen() {
        vueMonde.hideGameOverScreen();
    }

    /**
     * Échange deux tuiles visuellement (coordonnées col/row dans un Vector2).
     *
     * @param pos1 (col, row) de la première tuile
     * @param pos2 (col, row) de la deuxième tuile
     */
    public void swapTiles(Vector2 pos1, Vector2 pos2) {
        vueMonde.swapTiles(pos1, pos2);
    }

    /**
     * Fait pivoter une tuile d'un angle supplémentaire (multiples de 90°).
     *
     * @param row        ligne de la tuile (0-2)
     * @param col        colonne de la tuile (0-2)
     * @param angleToAdd angle à ajouter en degrés
     */
    public void rotateTile(int row, int col, int angleToAdd) {
        vueMonde.rotateTile(row, col, angleToAdd);
    }

    /**
     * Déplace un pion détective sur l'anneau à une position donnée.
     *
     * @param position     position sur l'anneau (1-16)
     * @param detectiveNum numéro du détective (1-3)
     */
    public void replaceOuterBall(int position, int detectiveNum) {
        vueMonde.replaceOuterBall(position, detectiveNum);
    }

    // ACCÈS AUX COMPOSANTS INTERNES

    /**
     * Retourne l'instance VueMonde (pour les accès directs si nécessaire).
     * Alias de getMonde() — les deux noms coexistent pour la compatibilité.
     */
    public VueMonde getVueMonde() {
        return vueMonde;
    }

    /**
     * Alias de getVueMonde() — utilisé par Gameplay pour injecter la sonde
     * de survol et piloter le feedback visuel des jetons.
     */
    public VueMonde getMonde() {
        return vueMonde;
    }

    /** Retourne le médiateur Gameplay (accès direct parfois nécessaire). */
    public Gameplay getGameplay() {
        return gameplay;
    }

    // CONSTANTES DE FOND D'ÉCRAN (re-exposées pour compatibilité avec Gameplay)
    // Ces constantes existent dans VueMonde ; VueJeu les réexpose pour éviter
    // de modifier les références existantes dans Gameplay.

    public static final String BG_WHITE  = VueMonde.BG_WHITE;
    public static final String BG_PURPLE = VueMonde.BG_PURPLE;
    public static final String BG_RED    = VueMonde.BG_RED;
    public static final String BG_BLUE   = VueMonde.BG_BLUE;

    /**
     * Change l'image de fond d'écran.
     *
     * @param colorName une des constantes BG_WHITE, BG_PURPLE, BG_RED, BG_BLUE
     */
    public void setBackgroundColor(String colorName) {
        vueMonde.setBackgroundColor(colorName);
    }

    // UTILITAIRE IMAGE (conservé pour compatibilité avec les appels externes)

    /**
     * Délègue la rotation d'image à VueMonde.rotateImage().
     * Méthode statique conservée pour ne pas casser les appels existants.
     *
     * @param img   image source
     * @param angle angle en radians
     * @return nouvelle image tournée
     */
    public static BufferedImage rotateImage(BufferedImage img, double angle) {
        return VueMonde.rotateImage(img, angle);
    }

    // TICKER (boucle de rafraîchissement à ~60 FPS)

    /**
     * Boucle principale de rafraîchissement graphique.
     * Tourne dans un Thread séparé pour ne pas bloquer l'EDT Swing.
     *
     * Toutes les ~16 ms (= 1000 / 60 FPS) :
     *  1. Attendre le délai (Thread.sleep)
     *  2. Appeler refreshBoardComponents()
     *
     * S'arrête quand partieEnCours passe à false (stopGameLoop()).
     */
    private void startTicker() {
        int delaiMs = 1000 / FPS; // ≈ 16 ms à 60 FPS
        while (partieEnCours) {
            try {
                Thread.sleep(delaiMs);
                refreshBoardComponents();
            } catch (InterruptedException e) {
                // Le thread a été interrompu de l'extérieur  sortie propre
                System.err.println("Ticker interrompu");
                return;
            }
        }
    }

    /**
     * Rafraîchit toutes les vues à chaque frame.
     *
     * Lit l'état courant du modèle (gameplay.partie) et met à jour
     * les composants visuels correspondants.
     *
     * Appelé par le ticker ~60 fois par seconde.
     */
    public void refreshBoardComponents() {
        // Indicateur de tour (numéro affiché en haut)
        updateTurnIndicator(gameplay.partie.numeroTour);

        // Fond d'écran (rouge/bleu/violet selon le joueur et la poursuite)
        updateBackground(gameplay.partie.joueurCourant, gameplay.partie.coursePoursuiteActive);

        // Tuiles du plateau (sauf en mode rotation où c'est géré différemment)
        if (!gameplay.rotationMode) {
            updateDistrictView(gameplay.partie.district);
        }

        // Jetons d'action (face pile ou face)
        updateJetons(gameplay.partie.jetonsAction);

        // Pions détectives (positions sur l'anneau)
        updateDetectivesView(gameplay.partie.detectives);

        // Compteur de sabliers de Jack
        updateSabliers(gameplay.partie.sabliersMinimumDeJack, 6);

        // Bandeau "IA en réflexion" (visible/caché + texte adapté)
        vueMonde.updateThinking(gameplay.enReflexion(), gameplay.partie.IaEnCours);

        // Forcer le re-dessin de la Camera et de l'overlay
        vueMonde.repaintWorld();
    }

    /**
     * Arrête la boucle ticker (appelé quand on retourne au menu).
     */
    public void stopGameLoop() {
        partieEnCours = false;
    }

    // INITIALISATION DU LAYOUT SWING

    /**
     * Configure le BorderLayout du JPanel racine (this).
     * Fond noir pour que les bandes letterbox soient invisibles.
     */
    private void initFrame() {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
    }

    /**
     * Crée la bande latérale gauche (WEST) avec les boutons de contrôle.
     *
     * Utilise un BoxLayout vertical pour empiler les boutons du haut vers le bas.
     * Box.createVerticalStrut(4) insère 4px d'espace entre chaque bouton.
     * Box.createVerticalGlue() pousse tout vers le haut (espace élastique en bas).
     */
    private void initLeftStrip() {
        leftStrip = new JPanel();
        leftStrip.setLayout(new BoxLayout(leftStrip, BoxLayout.Y_AXIS));
        leftStrip.setPreferredSize(new Dimension(STRIP_W_PX, WINDOW_H));
        leftStrip.setBackground(new Color(30, 30, 40)); // gris très foncé
        leftStrip.setBorder(BorderFactory.createEmptyBorder(20, 5, 10, 5));

        // Créer tous les boutons dans des méthodes dédiées (un bouton = une méthode)
        //  chaque méthode configure l'icône ET l'action du bouton.
        JButton[] boutons = {
                makeRetourButton(),
                makeIaButton(),
                makeAnnulerButton(),
                makeRefaireButton(),
                makeVisibleButton(),
                makeReglesButton(),
                makeIdentiteJackButton()
        };

        // Ajouter chaque bouton avec un espace de 4px entre eux
        for (JButton btn : boutons) {
            leftStrip.add(btn);
            leftStrip.add(Box.createVerticalStrut(4));
        }

        // Glue : pousse tous les boutons vers le haut en remplissant l'espace restant
        leftStrip.add(Box.createVerticalGlue());

        add(leftStrip, BorderLayout.WEST);
    }

    /**
     * Positionne VueMonde dans la zone centrale (CENTER).
     * BorderLayout fait occuper à CENTER tout l'espace restant après WEST.
     */
    private void initCenterPane() {
        add(vueMonde, BorderLayout.CENTER);
    }

    /**
     * Attache un ComponentListener pour recalculer le letterbox
     * à chaque redimensionnement de la fenêtre.
     *
     * ComponentAdapter implémente ComponentListener avec des méthodes vides :
     * on ne surcharge que componentResized.
     */
    private void initResizeListener() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyLetterbox();
            }
        });
    }

    /**
     * Recalcule le letterbox après un redimensionnement.
     *
     * VueMonde occupe tout le CENTER ; ses dimensions (getWidth/getHeight)
     * reflètent l'espace disponible après que la bande gauche a pris sa place.
     */
    private void applyLetterbox() {
        int availW = vueMonde.getWidth();
        int availH = vueMonde.getHeight();
        vueMonde.applyLetterbox(availW, availH);
        // Swing gère le repositionnement des overlays via son propre layout —
        // pas besoin d'appel manuel supplémentaire ici.
    }

    /**
     * Repositionne les overlays actifs (règles, settings) pour qu'ils occupent
     * toute la fenêtre après un redimensionnement.
     */
    private void repositionnerOverlays() {
        JPanel glassPane = getFullscreenGlassPane();

        if (panneauRegles != null) {
            panneauRegles.setBounds(0, 0, glassPane.getWidth(), glassPane.getHeight());
            panneauRegles.revalidate();
            panneauRegles.repaint();
        }

        if (panneauSettings != null) {
            panneauSettings.setBounds(0, 0, glassPane.getWidth(), glassPane.getHeight());
            panneauSettings.revalidate();
            panneauSettings.repaint();
        }
    }

    // CRÉATION DES BOUTONS DE LA BANDE LATÉRALE
    // Chaque méthode makeXxxButton() :
    //   1. Crée un JButton stylisé via makeStripButton()
    //   2. Lui attache un ActionListener (lambda = action au clic)
    //   3. Retourne le bouton configuré

    /**
     * Bouton "Retour au Menu".
     * Arrête le ticker, tue la partie, et retourne au menu principal.
     * Ce bouton est TOUJOURS accessible (pas de garde isGameFrozen).
     */
    private JButton makeRetourButton() {
        JButton btn = makeStripButton("Retour au Menu", "Retour.png");
        btn.addActionListener(e -> {
            // Pas de garde isGameFrozen ici : le retour doit toujours être possible.
            stopGameLoop();
            gameplay.partie.kill();
            gameplay.quitterPartie(); // nettoyage réseau éventuel
            menuPrincipal.resetBoutonsSurvoles();
            parent.setContentPane(menuPrincipal);
            parent.revalidate();
            parent.repaint();
        });
        return btn;
    }

    /**
     * Bouton "Nouvelle Partie".
     * Réinitialise la partie en cours.
     * (non utilisé dans la bande latérale principale mais créé pour compatibilité)
     */
    private JButton makeNewGameButton() {
        JButton btn = makeStripButton("Nouvelle Partie", "Niv.Partie.png");
        btn.addActionListener(e -> {
            if (gameplay.isGameFrozen()) return; // bloqué pendant animations/overlays
            if (gameplay != null) {
                gameplay.resetGame();
            }
        });
        return btn;
    }

    /**
     * Bouton "L'IA prend ta place".
     * Remplace le joueur courant par l'IA (niveau 2).
     * Empêche la substitution si l'IA joue déjà (IaEnCours).
     */
    private JButton makeIaButton() {
        JButton btn = makeStripButton("L'IA prend ta place", "IA.png");
        btn.addActionListener(e -> {
            if (gameplay.isGameFrozen()) return;
            if (gameplay == null)        return;
            System.out.println("Le bouton IA a été cliqué");

            if (gameplay.partie.IaEnCours) return; // l'IA joue déjà, rien à faire

            // Déterminer quel joueur substituer et ajuster joueurChoisi
            if (gameplay.partie.joueurCourant == Joueur.JACK) {
                gameplay.partie.niveauJack = 2;
                if (gameplay.partie.joueurChoisi == null) {
                    gameplay.partie.joueurChoisi = Joueur.ENQUETEUR;
                }
            } else {
                gameplay.partie.niveauEnqueteur = 2;
                if (gameplay.partie.joueurChoisi == null) {
                    gameplay.partie.joueurChoisi = Joueur.JACK;
                }
            }

            gameplay.partie.verifTourIa();
            gameplay.updatePlayerRectangles();
        });
        return btn;
    }

    /**
     * Bouton "Annuler un coup".
     * Revient à l'état précédent dans l'historique de la partie.
     */
    private JButton makeAnnulerButton() {
        JButton btn = makeStripButton("Annuler un coup", "Annuler.png");
        btn.addActionListener(e -> {
            if (gameplay.isGameFrozen()) return;
            if (gameplay != null) {
                gameplay.partie.annuler();
                refreshBoardComponents(); // mise à jour immédiate de la vue
            }
        });
        return btn;
    }

    /**
     * Bouton "Refaire un coup".
     * Rejoue le coup annulé (redo).
     */
    private JButton makeRefaireButton() {
        JButton btn = makeStripButton("Refaire un coup Annuler", "Refaire.png");
        btn.addActionListener(e -> {
            if (gameplay.isGameFrozen()) return;
            if (gameplay != null) {
                gameplay.partie.refaire();
                refreshBoardComponents();
            }
        });
        return btn;
    }

    /**
     * Bouton "Les suspects visibles par les détectives".
     * Affiche temporairement quels personnages sont visibles.
     */
    private JButton makeVisibleButton() {
        JButton btn = makeStripButton("Les suspects visibles par les détéctives", "Visible.png");
        btn.addActionListener(e -> {
            if (gameplay.isGameFrozen()) return;
            if (gameplay != null) {
                gameplay.afficherVisibiliteTemporaire();
            }
        });
        return btn;
    }

    /**
     * Bouton "Sauvegarder".
     * Sérialise la partie dans le fichier "save.dat".
     */
    private JButton makeSaveButton() {
        JButton btn = makeStripButton("Save", "Sauvegarder.png");
        btn.addActionListener(e -> {
            if (gameplay.isGameFrozen()) return;
            if (gameplay != null) {
                try {
                    SaveManager.save(gameplay.partie.toGameSave(), "save.dat");
                } catch (Exception ex) {
                    System.err.println("Save échoué : " + ex.getMessage());
                }
            }
        });
        return btn;
    }

    /**
     * Bouton "Charger".
     * Désérialise une partie depuis "save.dat" et met à jour la vue.
     */
    private JButton makeLoadButton() {
        JButton btn = makeStripButton("Load", "Charger.png");
        btn.addActionListener(e -> {
            if (gameplay.isGameFrozen()) return;
            if (gameplay != null) {
                try {
                    GameSave saveFile = SaveManager.load("save.dat");
                    gameplay.partie.fromGameSave(saveFile);
                    refreshBoardComponents();
                } catch (Exception ex) {
                    System.err.println("Load échoué : " + ex.getMessage());
                }
            }
        });
        return btn;
    }

    /**
     * Bouton "Règles du jeu".
     * Bascule l'affichage du panneau de règles (toggle).
     */
    private JButton makeReglesButton() {
        JButton btn = makeStripButton("Règles du jeu", "ReglesB.png");
        btn.addActionListener(e -> {
            if (gameplay.isGameFrozen()) return;
            onReglesPressed();
        });
        return btn;
    }

    /**
     * Bouton "Rappel identité Jack".
     * Affiche discrètement à qui appartient le rôle de Jack
     * (utile en jeu local quand les deux joueurs partagent l'écran).
     */
    private JButton makeIdentiteJackButton() {
        JButton btn = makeStripButton(
                "Pour vous rappeler de votre identité (uniquement si Mr. Jack joue sur cette machine et est humain)",
                "MrJack.png");
        btn.addActionListener(e -> {
            if (gameplay.isGameFrozen()) return;
            if (gameplay != null) {
                gameplay.afficherRappelIdentiteJack();
            }
        });
        return btn;
    }

    // HELPERS — CHARGEMENT D'ICÔNES ET CRÉATION DE BOUTONS

    /**
     * Charge une image PNG depuis les ressources et la redimensionne.
     *
     * Suit le même modèle que Camera.AddSprite() :
     *  - Cherche d'abord dans le classpath (jar/resources)
     *  - Retourne null si le fichier est introuvable ou illisible
     *
     * @param fileName nom du fichier PNG (ex : "Retour.png")
     * @param width    largeur cible en pixels
     * @param height   hauteur cible en pixels
     * @return un ImageIcon redimensionné, ou null si indisponible
     */
    private ImageIcon loadIcon(String fileName, int width, int height) {
        InputStream in = VueJeu.class.getResourceAsStream("/res/Images/" + fileName);
        if (in == null) {
            System.err.println("VueJeu.loadIcon — fichier introuvable : /res/Images/" + fileName);
            return null;
        }
        try {
            BufferedImage raw = ImageIO.read(in);
            if (raw == null) {
                System.err.println("VueJeu.loadIcon — ImageIO n'a pas pu lire : " + fileName);
                return null;
            }
            // Image.SCALE_SMOOTH  interpolation bilinéaire, meilleure qualité visuelle
            Image scaled = raw.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ex) {
            System.err.println("VueJeu.loadIcon — exception sur " + fileName + " : " + ex.getMessage());
            return null;
        }
    }

    /**
     * Crée un bouton pour la bande latérale.
     *
     * Stratégie d'affichage en deux modes :
     *
     *   Mode IMAGE (normal) :
     *     - Affiche l'icône PNG redimensionnée à BTN_SIZE × BTN_SIZE
     *     - Texte vide, tooltip = label
     *     - Bouton "invisible" (pas de bordure, pas de fond)
     *
     *   Mode TEXTE (secours, si le PNG est introuvable) :
     *     - Texte HTML centré (retour à la ligne automatique)
     *     - Fond gris foncé + bordure fine
     *     - Moins joli mais toujours fonctionnel
     *
     * @param label   texte de secours et tooltip
     * @param pngFile nom du fichier PNG à charger depuis res/Images/
     * @return le bouton configuré, prêt à recevoir un ActionListener
     */
    private JButton makeStripButton(String label, String pngFile) {
        JButton btn = new JButton();
        btn.setFocusable(false); // évite que le bouton vole le focus clavier du jeu
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT); // centrage dans le BoxLayout

        // Forcer une taille carrée fixe (BTN_SIZE × BTN_SIZE)
        Dimension tailleCarree = new Dimension(BTN_SIZE, BTN_SIZE);
        btn.setPreferredSize(tailleCarree);
        btn.setMinimumSize(tailleCarree);
        btn.setMaximumSize(tailleCarree);

        ImageIcon icone = loadIcon(pngFile, BTN_SIZE, BTN_SIZE);
        if (icone != null) {
            //  Mode image 
            btn.setIcon(icone);
            btn.setText("");
            btn.setToolTipText(label);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
        } else {
            //  Mode texte (repli) 
            // <html><center> permet le retour à la ligne automatique en Swing
            btn.setText("<html><center>" + label + "</center></html>");
            btn.setFont(new Font("SansSerif", Font.BOLD, 10));
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(50, 50, 60));
            btn.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 120), 1));
            btn.setOpaque(true);
        }
        return btn;
    }

    /**
     * Crée un bouton stylisé pour le panneau Settings (menu pause).
     *
     * Style différent de makeStripButton : fond coloré, grande taille, effet hover.
     *
     * @param label texte affiché sur le bouton
     * @return le bouton configuré
     */
    private JButton makeSettingsButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(70, 70, 90));
        btn.setBorder(BorderFactory.createLineBorder(new Color(160, 160, 200), 2));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setPreferredSize(new Dimension(260, 50));
        btn.setMaximumSize(new Dimension(260, 50));

        // Effet hover : changer la couleur quand la souris est au-dessus.
        // On écoute le ChangeListener du ButtonModel pour détecter isRollover().
        btn.setRolloverEnabled(true);
        btn.getModel().addChangeListener(e -> {
            ButtonModel model = (ButtonModel) e.getSource();
            if (model.isRollover()) {
                // Survol : fond plus clair + bordure plus lumineuse
                btn.setBackground(new Color(90, 90, 120));
                btn.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 255), 2));
            } else {
                // Normal : couleurs d'origine
                btn.setBackground(new Color(70, 70, 90));
                btn.setBorder(BorderFactory.createLineBorder(new Color(160, 160, 200), 2));
            }
        });
        return btn;
    }

    // PANNEAU DES RÈGLES (overlay via GlassPane)

    /**
     * Affiche ou ferme le panneau des règles (comportement bascule).
     *
     *  
     *   Le GlassPane est une couche transparente superposée à TOUTE la JFrame.
     *   En le rendant visible et en y ajoutant notre panneau, on obtient
     *   un overlay plein-écran sans perturber le layout normal.
     *
     * Si le panneau est déjà ouvert  le fermer.
     * Sinon  créer et afficher le panneau.
     */
    private void onReglesPressed() {
        // Bascule : si déjà visible, fermer
        if (panneauRegles != null && panneauRegles.isShowing()) {
            fermerPanneauRegles();
            return;
        }

        if (gameplay.isGameFrozen()) return; // pas d'overlay si le jeu est gelé

        JPanel glassPane = getFullscreenGlassPane();
        glassPane.removeAll(); // nettoyage de sécurité (au cas où)

        // Créer le panneau d'overlay (fond semi-transparent dessiné custom).
        // Classe anonyme car paintComponent doit être surchargé ponctuellement.
        panneauRegles = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // Fond noir à 90% d'opacité (alpha 230/255)
                g.setColor(new Color(0, 0, 0, 230));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panneauRegles.setOpaque(false);
        // Marges intérieures pour aérer le contenu
        panneauRegles.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        // Fermer les règles si l'utilisateur clique sur le fond sombre
        java.awt.event.MouseAdapter fermetureListener = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                fermerPanneauRegles();
            }
        };
        panneauRegles.addMouseListener(fermetureListener);

        // Contenu : texte HTML des règles dans un JScrollPane
        String html = VueReglesPanel.getHtml();
        JLabel labelRegles = new JLabel(html);
        labelRegles.setVerticalAlignment(SwingConstants.TOP);
        labelRegles.addMouseListener(fermetureListener); // clic sur le texte = fermeture aussi

        JScrollPane scroll = new JScrollPane(labelRegles,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 140, 0, 180), 2)); // bordure dorée
        scroll.getVerticalScrollBar().setUnitIncrement(16); // défilement plus rapide à la molette
        scroll.addMouseListener(fermetureListener);

        panneauRegles.add(scroll, BorderLayout.CENTER);

        // Injecter dans le GlassPane et le rendre visible
        glassPane.add(panneauRegles, BorderLayout.CENTER);
        glassPane.setVisible(true);
        glassPane.revalidate();
        glassPane.repaint();
    }

    /**
     * Retire le panneau des règles du GlassPane et dégèle le jeu.
     */
    private void fermerPanneauRegles() {
        if (panneauRegles != null) {
            JPanel glassPane = getFullscreenGlassPane();
            glassPane.remove(panneauRegles);
            panneauRegles = null;

            // Masquer le GlassPane s'il est maintenant vide
            if (glassPane.getComponentCount() == 0) {
                glassPane.setVisible(false);
            }

            // Dégeler le jeu
            if (gameplay != null) {
                gameplay.setGlobalFreeze(false);
            }
            vueMonde.jeuVerrouille = false;

            glassPane.revalidate();
            glassPane.repaint();
        }
    }

    // PANNEAU DES SETTINGS / PAUSE (overlay via GlassPane)

    /**
     * Affiche le menu pause (settings) en overlay plein-écran.
     *
     * Structure visuelle :
     *
     *
     *     [Logo du jeu]                     image "Titre" centrée
     *
     *     [ Continuer          ]         
     *     [ Nouvelle partie    ]  (solo) 
     *     [ Sauvegarder        ]         
     *     [ Charger            ]  (solo) 
     *     [ Retour menu        ]         
     *
     *
     * Le fond sombre est géré par un JPanel à paintComponent surchargé.
     * Le panneau central est centré par un GridBagLayout sans contraintes.
     */
    private void onSettingsPressed() {
        // Bascule : si déjà visible, fermer
        if (panneauSettings != null && panneauSettings.isShowing()) {
            fermerPanneauSettings();
            return;
        }

        if (gameplay.isGameFrozen()) return;

        JPanel glassPane = getFullscreenGlassPane();
        glassPane.removeAll(); // nettoyage de sécurité

        // Fond noir semi-transparent avec GridBagLayout pour centrer le contenu
        panneauSettings = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 230)); // fond noir à 90%
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panneauSettings.setOpaque(false);

        // Clic sur le fond sombre  fermer le menu
        panneauSettings.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                fermerPanneauSettings();
            }
        });

        // Panneau central vertical : logo + boutons empilés
        JPanel panelCentral = new JPanel();
        panelCentral.setOpaque(false);
        panelCentral.setLayout(new BoxLayout(panelCentral, BoxLayout.Y_AXIS));
        // Clic sur le panneau central  NE PAS fermer (consume l'événement)
        panelCentral.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                e.consume(); // bloquer la propagation vers le fond qui fermerait le menu
            }
        });

        //  Logo du jeu (image "Titre") 
        BufferedImage imgTitre = src.utils.utils.loadImage("Titre");
        if (imgTitre != null) {
            // Limiter à 360×180 max pour ne pas surcharger le menu
            int logoW = Math.min(360, imgTitre.getWidth());
            int logoH = Math.min(180, imgTitre.getHeight());
            Image logoScaled = imgTitre.getScaledInstance(logoW, logoH, Image.SCALE_SMOOTH);
            JLabel logo = new JLabel(new ImageIcon(logoScaled));
            logo.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelCentral.add(logo);
            panelCentral.add(Box.createVerticalStrut(16));
        }

        //  Bouton Continuer 
        JButton continuer = makeSettingsButton("Continuer");
        continuer.addActionListener(e -> fermerPanneauSettings());

        //  Bouton Sauvegarder 
        JButton sauvegarder = makeSettingsButton("Sauvegarder");
        sauvegarder.addActionListener(e -> {
            try {
                SaveManager.save(gameplay.partie.toGameSave(), "save.dat");
                JOptionPane.showMessageDialog(
                        this,
                        "Partie sauvegardée avec succès !",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                System.err.println("Save échoué : " + ex.getMessage());
            }
        });

        //  Bouton Charger 
        JButton charger = makeSettingsButton("Charger");
        charger.addActionListener(e -> {
            try {
                GameSave saveFile = SaveManager.load("save.dat");
                gameplay.partie.fromGameSave(saveFile);
                gameplay.updatePlayerRectangles();
                refreshBoardComponents();
                fermerPanneauSettings();
            } catch (Exception ex) {
                System.err.println("Load échoué : " + ex.getMessage());
            }
        });

        //  Bouton Nouvelle partie 
        JButton nvPartie = makeSettingsButton("Nouvelle partie");
        nvPartie.addActionListener(e -> {
            fermerPanneauSettings();
            if (gameplay != null) {
                gameplay.resetGame();
            }
        });

        //  Bouton Retour menu 
        JButton retourMenu = makeSettingsButton("Retour menu");
        retourMenu.addActionListener(e -> {
            fermerPanneauSettings();
            stopGameLoop();
            gameplay.partie.kill();
            gameplay.quitterPartie(); // nettoyage réseau éventuel
            menuPrincipal.resetBoutonsSurvoles();
            parent.setContentPane(menuPrincipal);
            parent.revalidate();
            parent.repaint();
        });

        //  Assemblage du panneau central 
        panelCentral.add(continuer);
        panelCentral.add(Box.createVerticalStrut(12));

        // Certains boutons sont cachés en mode réseau (solo seulement)
        if (gameplay.partie.getIsSolo()) {
            panelCentral.add(nvPartie);
        }
        panelCentral.add(Box.createVerticalStrut(12));
        panelCentral.add(sauvegarder);
        panelCentral.add(Box.createVerticalStrut(12));
        if (gameplay.partie.getIsSolo()) {
            panelCentral.add(charger);
        }
        panelCentral.add(Box.createVerticalStrut(12));
        panelCentral.add(retourMenu);

        // GridBagConstraints par défaut  centre automatiquement dans les 100% d'espace
        panneauSettings.add(panelCentral, new GridBagConstraints());

        // Injection dans le GlassPane
        glassPane.add(panneauSettings, BorderLayout.CENTER);
        glassPane.setVisible(true);
        glassPane.revalidate();
        glassPane.repaint();
    }

    /**
     * Retire le panneau settings du GlassPane et dégèle le jeu.
     */
    private void fermerPanneauSettings() {
        if (panneauSettings != null) {
            JPanel glassPane = getFullscreenGlassPane();
            glassPane.remove(panneauSettings);
            panneauSettings = null;

            // Masquer le GlassPane s'il est maintenant vide
            if (glassPane.getComponentCount() == 0) {
                glassPane.setVisible(false);
            }

            // Dégeler le jeu
            if (gameplay != null) {
                gameplay.setGlobalFreeze(false);
            }
            vueMonde.jeuVerrouille = false;

            glassPane.revalidate();
            glassPane.repaint();
        }
    }

    // UTILITAIRE GLASSPANE

    /**
     * Retourne le GlassPane de la JFrame parent, configuré avec un BorderLayout.
     *
     * LE GLASSPANE est une couche transparente qui couvre toute la JFrame.
     * En le rendant visible et en y ajoutant un composant, on obtient un overlay
     * plein-écran sans toucher au layout normal du contenu.
     *
     * Si le GlassPane existant est déjà un JPanel avec BorderLayout  le réutiliser.
     * Sinon  en créer un neuf et l'installer (setGlassPane).
     *
     * @return le GlassPane prêt à recevoir des composants
     */
    private JPanel getFullscreenGlassPane() {
        Component glass = parent.getGlassPane();

        // Vérifier si le GlassPane est déjà configuré correctement
        if (glass instanceof JPanel && ((JPanel) glass).getLayout() instanceof BorderLayout) {
            return (JPanel) glass;
        }

        // Sinon, créer un nouveau GlassPane propre
        JPanel nouveauGlassPane = new JPanel(new BorderLayout());
        nouveauGlassPane.setOpaque(false); // transparent (on voit le jeu dessous)
        parent.setGlassPane(nouveauGlassPane);
        return nouveauGlassPane;
    }

    // MODE RÉSEAU — DÉSACTIVATION DE CERTAINS BOUTONS

    /**
     * Désactive et cache les boutons non pertinents en mode réseau (multijoueur).
     *
     * En réseau, sauvegarder/charger/annuler/refaire/IA n'ont pas de sens
     * car les deux joueurs n'ont pas le même état local.
     *
     * @param isMulti true si la partie est en mode réseau
     */
    public void setJeuReseau(boolean isMulti) {
        if (!isMulti) return; // en mode solo, rien à faire

        // Parcourir tous les composants de la bande latérale
        for (Component comp : leftStrip.getComponents()) {
            // Vérifier que c'est bien un JButton (pas un Box.Strut ou Glue)
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                String tooltip = btn.getToolTipText();

                // Désactiver les boutons problématiques en réseau
                if ("Annuler un coup".equals(tooltip)
                        || "Refaire un coup Annuler".equals(tooltip)
                        || "L'IA prend ta place".equals(tooltip)
                        || "Save".equals(tooltip)
                        || "Load".equals(tooltip)) {

                    btn.setEnabled(false);
                    btn.setVisible(false);
                }

                // Cacher le bouton "Identité Jack" si c'est l'enquêteur qui joue ici
                // (l'enquêteur ne doit pas voir l'identité de Jack)
                if (gameplay.partie.joueurChoisi == Joueur.ENQUETEUR
                        && "Pour vous rappeler de votre identité (uniquement si Mr. Jack joue sur cette machine et est humain)".equals(tooltip)) {
                    btn.setEnabled(false);
                    btn.setVisible(false);
                }
            }
        }
    }
}