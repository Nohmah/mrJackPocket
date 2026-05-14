package src.vue;
import src.vue.VueMonde;

import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.Deque;


/**
 * IHMControler — Interpréteur d'intentions utilisateur.
 *
 * Responsabilités :
 *   - Intercepter les clics souris / touches clavier et les traduire en
 *     intentions typées ({@link ClickIntent}, {@link ActionIntent}).
 *   - Gérer la pile Undo/Redo locale.
 *   - Transmettre immédiatement les intentions à {@link Gameplay} sans
 *     prendre aucune décision métier.
 *
 * Ce que IHMControler NE fait plus :
 *   - Afficher des dialogues JOptionPane.
 *   - Appeler partie.actions.*.
 *   - Décider quoi faire d'une action (c'est Gameplay qui décide).
 */
public class IHMControler implements MouseListener, KeyListener {

    // =========================================================================
    // Records d'intentions
    // =========================================================================

    /**
     * ClickIntent — clic sur une cellule du plateau.
     *
     * @param cellRow      Ligne (0-2).
     * @param cellCol      Colonne (0-2).
     * @param worldPos     Position monde du clic.
     * @param isRightClick Vrai si clic droit.
     */
    public record ClickIntent(int cellRow, int cellCol, Vector2 worldPos, boolean isRightClick) {}

    /**
     * ActionIntent — clic confirmé sur une boule d'action.
     *
     * @param ballIndex  Index de la boule (0-3).
     * @param actionName Nom du sprite courant (ex: "action_holmes").
     */
    public record ActionIntent(int ballIndex, String actionName) {}

    // =========================================================================
    // Constantes plateau
    // =========================================================================

    //private static final double  TILE_WORLD_SIZE   = 150.0;
    //private static final Vector2 BOARD_WORLD_ORIGIN = new Vector2(375, 175);

    // =========================================================================
    // État interne
    // =========================================================================

    private final Deque<ClickIntent> undoStack    = new ArrayDeque<>();
    private final Deque<ClickIntent> redoStack    = new ArrayDeque<>();
    private ClickIntent              pendingIntent = null;
    private final Gameplay           gameplay;
    private String                   activePlayerType = "HUMAN";

    // =========================================================================
    // Constructeur
    // =========================================================================

    public IHMControler(Gameplay gameplay) {
        this.gameplay = gameplay;
    }

    // =========================================================================
    // API publique — configuration
    // =========================================================================

    /** Définit le type du joueur actif avant chaque tour ("HUMAN" ou "AI"). */
    public void setActivePlayerType(String type) {
        this.activePlayerType = type;
    }

    public ClickIntent getPendingIntent() { return pendingIntent; }

    // =========================================================================
    // Point d'entrée : hit sur une boule d'action (appelé par VueJeu)
    // =========================================================================

    /**
     * Reçoit la notification d'un hit sur une boule d'action depuis VueJeu.
     * Construit un {@link ActionIntent} et le transmet immédiatement à Gameplay.
     *
     * VueJeu a déjà fait la conversion écran→monde et le hitTest.
     * IHMControler n'interprète pas le sens de l'action.
     *
     * @param ballIndex  Index de la boule touchée (0-3).
     * @param actionName Nom du sprite courant (ex: "action_holmes").
     */
    public void onActionBallHit(int ballIndex, String actionName) {
        ActionIntent intent = new ActionIntent(ballIndex, actionName);
        System.out.println("IHMControler — ActionIntent créé : [" + ballIndex + "] " + actionName);

        // Transmission directe à Gameplay : aucune décision ici.
        gameplay.onActionBallClicked(intent);
    }

    // =========================================================================
    // Gestion Undo / Redo / Confirmation
    // =========================================================================

    /** Confirme l'intention en cours et la pousse dans undoStack. */
    public void confirmPendingIntent() {
        if (pendingIntent == null) return;
        undoStack.push(pendingIntent);
        redoStack.clear();
        gameplay.onActionConfirmed(pendingIntent);
        pendingIntent = null;
    }

    /** Annule la dernière action confirmée. */
    public void undo() {
        if (undoStack.isEmpty()) return;
        ClickIntent last = undoStack.pop();
        redoStack.push(last);
        gameplay.onUndoRequested(last);
    }

    /** Rétablit la dernière action annulée. */
    public void redo() {
        if (redoStack.isEmpty()) return;
        ClickIntent next = redoStack.pop();
        undoStack.push(next);
        gameplay.onRedoRequested(next);
    }

    // =========================================================================
    // Action IA
    // =========================================================================

    /**
     * Simule une action IA sur la cellule (0,0) par défaut.
     * À brancher sur la vraie logique de sélection de coup.
     */
    public void joueIa() {
        if (!"AI".equals(activePlayerType)) return;
        Vector2 pos = VueMonde.BOARD_ORIGIN.Add(new Vector2(VueMonde.TILE_SIZE / 2, VueMonde.TILE_SIZE / 2));
        pendingIntent = new ClickIntent(0, 0, pos, false);
        confirmPendingIntent();
    }

    // =========================================================================
    // MouseListener
    // =========================================================================

    @Override
    public void mouseClicked(MouseEvent e) {
        Vector2 world = screenToWorld(new Vector2(e.getX(), e.getY()));
        int col = (int) ((world.x - VueMonde.BOARD_ORIGIN.x) / VueMonde.TILE_SIZE);
        int row = (int) ((world.y - VueMonde.BOARD_ORIGIN.y) / VueMonde.TILE_SIZE);
        //int col = (int) ((world.x - BOARD_WORLD_ORIGIN.x) / TILE_WORLD_SIZE);
        //int row = (int) ((world.y - BOARD_WORLD_ORIGIN.y) / TILE_WORLD_SIZE);

        if (col < 0 || col > 2 || row < 0 || row > 2) {
            // Clic hors plateau — Gameplay décide si c'est significatif
            // (ex: sortir du mode rotation).
            gameplay.clicHorsDistrict();
            return;
        }

        boolean isRight = (e.getButton() == MouseEvent.BUTTON3);
        pendingIntent = new ClickIntent(row, col, world, isRight);

        // Notification plateau — Gameplay orchestre la réponse.
        gameplay.onCellHovered(row, col);

        System.out.println("IHMControler — ClickIntent : (" + row + "," + col + ")"
                + (isRight ? " [droit]" : "") + " monde=" + world.ToString());
    }

    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}

    // =========================================================================
    // KeyListener
    // =========================================================================

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Z -> { if (e.isControlDown()) undo(); }
            case KeyEvent.VK_Y -> { if (e.isControlDown()) redo(); }
            case KeyEvent.VK_ENTER  -> confirmPendingIntent();
            case KeyEvent.VK_ESCAPE -> pendingIntent = null;
        }
    }

    @Override public void keyTyped(KeyEvent e)    {}
    @Override public void keyReleased(KeyEvent e) {}

    // =========================================================================
    // Helper
    // =========================================================================

    /** Coordonnées écran → monde (inverse de la projection Camera). */
    private Vector2 screenToWorld(Vector2 screen) {
        return screen.Div(Camera.zoom).Add(Camera.positionHG);
    }
}