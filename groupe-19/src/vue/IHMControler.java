package src.vue;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * IHMControler — Contrôleur des interactions utilisateur.
 *
 * Responsabilités :
 *   - Intercepter les clics souris et les traduire en intentions d'action.
 *   - Gérer la pile d'annulation/rétablissement locale (Undo/Redo).
 *   - Lancer l'action automatique de l'IA si le joueur actif n'est pas humain.
 *
 * Ce contrôleur est instancié par Gameplay et reçoit les événements
 * délégués par VueJeu (MouseListener, KeyListener).
 */
public class IHMControler implements MouseListener, KeyListener {

    // -------------------------------------------------------------------------
    // Record Java 16 : représente un clic utilisateur normalisé
    // -------------------------------------------------------------------------

    /**
     * ClickIntent — données immuables d'un clic sur le plateau.
     *
     * @param cellRow    Ligne de la grille (0-2) sur laquelle le joueur a cliqué.
     * @param cellCol    Colonne de la grille (0-2).
     * @param worldPos   Position monde du clic (en pixels, espace Camera).
     * @param isRightClick Vrai si clic droit (action secondaire).
     */
    public record ClickIntent(int cellRow, int cellCol, Vector2 worldPos, boolean isRightClick) {}

    // -------------------------------------------------------------------------
    // Constantes de mapping plateau → monde
    // -------------------------------------------------------------------------

    /** Taille d'une tuile en unités monde (doit correspondre à VueJeu). */
    private static final double TILE_WORLD_SIZE = 150.0;

    /** Coin haut-gauche du plateau en unités monde (doit correspondre à VueJeu). */
    private static final Vector2 BOARD_WORLD_ORIGIN = new Vector2(375, 175);

    // -------------------------------------------------------------------------
    // État interne
    // -------------------------------------------------------------------------

    /** Pile d'actions réalisées (pour Undo). */
    private final Deque<ClickIntent> undoStack = new ArrayDeque<>();

    /** Pile d'actions annulées (pour Redo). */
    private final Deque<ClickIntent> redoStack = new ArrayDeque<>();

    /** Intention en cours de construction (avant validation). */
    private ClickIntent pendingIntent = null;

    /** Référence au pont logique. */
    private final Gameplay gameplay;

    /** Type du joueur actif : "HUMAN" ou "AI". */
    private String activePlayerType = "HUMAN";

    // -------------------------------------------------------------------------
    // Constructeur
    // -------------------------------------------------------------------------

    public IHMControler(Gameplay gameplay) {
        this.gameplay = gameplay;
    }

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Définit le type du joueur actif avant chaque tour.
     * Appelé par Gameplay au début de chaque tour.
     *
     * @param type "HUMAN" ou "AI"
     */
    public void setActivePlayerType(String type) {
        this.activePlayerType = type;
    }

    /**
     * Retourne l'intention en attente (peut être null si aucun clic en cours).
     */
    public ClickIntent getPendingIntent() {
        return pendingIntent;
    }

    /**
     * Confirme l'intention en cours : la pousse dans undoStack et la signale à
     * Gameplay pour qu'il la transmette à la logique.
     */
    public void confirmPendingIntent() {
        if (pendingIntent == null) return;

        undoStack.push(pendingIntent);
        redoStack.clear(); // Toute nouvelle action efface le Redo

        // ENVOI : Transmet le ClickIntent validé à Gameplay,
        // qui le propagera vers IntermediaryGameState pour application provisoire.
        gameplay.onActionConfirmed(pendingIntent);

        pendingIntent = null;
    }

    /**
     * Annule la dernière action confirmée (Undo local).
     * Ne touche pas encore à FinalGameState ; seul l'état intermédiaire est affecté.
     */
    public void undo() {
        if (undoStack.isEmpty()) return;

        ClickIntent last = undoStack.pop();
        redoStack.push(last);

        // ENVOI : Notifie Gameplay d'annuler la dernière action dans
        // IntermediaryGameState (rollback de l'état provisoire).
        gameplay.onUndoRequested(last);
    }

    /**
     * Rétablit la dernière action annulée (Redo local).
     */
    public void redo() {
        if (redoStack.isEmpty()) return;

        ClickIntent next = redoStack.pop();
        undoStack.push(next);

        // ENVOI : Notifie Gameplay de ré-appliquer l'action dans
        // IntermediaryGameState.
        gameplay.onRedoRequested(next);
    }

    /**
     * Simule une action automatique si le joueur actif est de type IA.
     * Doit être appelée par Gameplay en début de tour IA.
     *
     * La méthode calcule une action fictive, la pré-valide, puis appelle
     * confirmPendingIntent() pour l'intégrer dans le flux normal.
     */
    public void joueIa() {
        if (!"AI".equals(activePlayerType)) return;

        // RÉCEPTION : Récupère depuis IntermediaryGameState la liste des
        // mouvements légaux disponibles pour l'IA afin de choisir parmi eux.
        // List<ClickIntent> legalMoves = IntermediaryGameState.getLegalMoves();

        // Simulation : l'IA choisit la cellule (0,0) par défaut.
        // À remplacer par la logique de sélection de GameEngine.
        Vector2 iaWorldPos = BOARD_WORLD_ORIGIN.Add(new Vector2(
                TILE_WORLD_SIZE / 2,
                TILE_WORLD_SIZE / 2
        ));
        pendingIntent = new ClickIntent(0, 0, iaWorldPos, false);

        // ENVOI : L'action IA est traitée de façon identique à un clic humain ;
        // GameEngine sera notifié via Gameplay.onActionConfirmed().
        confirmPendingIntent();
    }

    // -------------------------------------------------------------------------
    // MouseListener
    // -------------------------------------------------------------------------

    @Override
    public void mouseClicked(MouseEvent e) {
        // Convertit les coordonnées écran en coordonnées monde via la Camera
        Vector2 screenPos = new Vector2(e.getX(), e.getY());
        Vector2 worldPos  = screenToWorld(screenPos);

        // Détermine la cellule de grille correspondante
        int col = (int) ((worldPos.x - BOARD_WORLD_ORIGIN.x) / TILE_WORLD_SIZE);
        int row = (int) ((worldPos.y - BOARD_WORLD_ORIGIN.y) / TILE_WORLD_SIZE);

        // Ignore les clics hors du plateau 3x3
        if (col < 0 || col > 2 || row < 0 || row > 2) return;

        boolean isRight = (e.getButton() == MouseEvent.BUTTON3);
        pendingIntent = new ClickIntent(row, col, worldPos, isRight);

        // RÉCEPTION : Demande à IntermediaryGameState de tester la validité
        // du mouvement temporaire (row, col) AVANT tout envoi à GameEngine.
        // boolean valid = IntermediaryGameState.isMoveLegal(row, col);
        // if (!valid) { pendingIntent = null; return; }

        // Notifie la Vue pour un retour visuel immédiat (highlight de la cellule)
        gameplay.onCellHovered(row, col);

        System.out.println("Clic détecté : cellule (" + row + "," + col + ") "
                + (isRight ? "[droit]" : "[gauche]")
                + " → monde " + worldPos.ToString());
    }

    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}

    // -------------------------------------------------------------------------
    // KeyListener (raccourcis clavier)
    // -------------------------------------------------------------------------

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Z -> { if (e.isControlDown()) undo(); }
            case KeyEvent.VK_Y -> { if (e.isControlDown()) redo(); }
            case KeyEvent.VK_ENTER -> confirmPendingIntent();
            case KeyEvent.VK_ESCAPE -> pendingIntent = null;
        }
    }

    @Override public void keyTyped(KeyEvent e)    {}
    @Override public void keyReleased(KeyEvent e) {}

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    /**
     * Convertit des coordonnées écran (pixels Swing) en coordonnées monde.
     * Inverse de la projection Camera : worldPos = screenPos / zoom + positionHG
     */
    private Vector2 screenToWorld(Vector2 screenPos) {
        return screenPos.Div(Camera.zoom).Add(Camera.positionHG);
    }
}
