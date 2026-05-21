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
 */
public class IHMControler implements MouseListener, MouseMotionListener, KeyListener {

    // =========================================================================
    // Records d'intentions
    // =========================================================================

    public record ClickIntent(int cellRow, int cellCol, Vector2 worldPos, boolean isRightClick) {}
    public record ActionIntent(int ballIndex, String actionName) {}

    // =========================================================================
    // Interface fonctionnelle — sonde de survol
    // =========================================================================

    @FunctionalInterface
    public interface HoverProbe {
        int ballAt(int sx, int sy);
    }

    // =========================================================================
    // État interne
    // =========================================================================

    private final Deque<ClickIntent> undoStack = new ArrayDeque<>();
    private final Deque<ClickIntent> redoStack = new ArrayDeque<>();
    private ClickIntent pendingIntent = null;
    private final Gameplay gameplay;
    private String activePlayerType = "HUMAN";
    private HoverProbe hoverProbe = null;

    // =========================================================================
    // Constructeur
    // =========================================================================

    public IHMControler(Gameplay gameplay) {
        this.gameplay = gameplay;
    }

    // =========================================================================
    // API publique — configuration
    // =========================================================================

    public void setActivePlayerType(String type) {
        this.activePlayerType = type;
    }

    public ClickIntent getPendingIntent() { return pendingIntent; }

    public void setHoverProbe(HoverProbe probe) {
        this.hoverProbe = probe;
    }

    public void onActionBallHit(int ballIndex, String actionName) {
        ActionIntent intent = new ActionIntent(ballIndex, actionName);
        System.out.println("IHMControler — ActionIntent créé : [" + ballIndex + "] " + actionName);
        gameplay.onActionBallClicked(intent);
    }

    // =========================================================================
    // Gestion Undo / Redo / Confirmation
    // =========================================================================

    public void confirmPendingIntent() {
        if (pendingIntent == null) return;
        undoStack.push(pendingIntent);
        redoStack.clear();
        gameplay.onActionConfirmed(pendingIntent);
        pendingIntent = null;
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        ClickIntent last = undoStack.pop();
        redoStack.push(last);
        gameplay.onUndoRequested(last);
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        ClickIntent next = redoStack.pop();
        undoStack.push(next);
        gameplay.onRedoRequested(next);
    }

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
        if (gameplay.partie.isFreeze()) return;
        Vector2 world = screenToWorld(new Vector2(e.getX(), e.getY()));

        // Calcul des limites du plateau (3 tuiles de TILE_SIZE chacune)
        double boardMinX = VueMonde.BOARD_ORIGIN.x;
        double boardMinY = VueMonde.BOARD_ORIGIN.y;
        double boardMaxX = boardMinX + 3 * VueMonde.TILE_SIZE;
        double boardMaxY = boardMinY + 3 * VueMonde.TILE_SIZE;

        // Vérifier si le clic est à l'intérieur du rectangle du plateau
        if (world.x < boardMinX || world.x > boardMaxX ||
            world.y < boardMinY || world.y > boardMaxY) {
            gameplay.clicHorsDistrict();
            return;
        }

        // Calcul des indices bruts (peuvent être 0,1,2,3)
        int colRaw = (int) ((world.x - boardMinX) / VueMonde.TILE_SIZE);
        int rowRaw = (int) ((world.y - boardMinY) / VueMonde.TILE_SIZE);

        // Ajustement pour les bords droits et bas (raw == 3 → clamp à 2)
        int col = (colRaw == 3) ? 2 : colRaw;
        int row = (rowRaw == 3) ? 2 : rowRaw;

        boolean isRight = (e.getButton() == MouseEvent.BUTTON3);
        pendingIntent = new ClickIntent(row, col, world, isRight);

        // Notification plateau — Gameplay orchestre la réponse
        gameplay.onCellHovered(row, col);

        System.out.println("IHMControler — ClickIntent : (" + row + "," + col + ")"
                + (isRight ? " [droit]" : "") + " monde=" + world.ToString());
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {
        if (gameplay.partie.isFreeze()) return;
        gameplay.onTokenHovered(-1);
    }

    // =========================================================================
    // MouseMotionListener
    // =========================================================================

    @Override
    public void mouseMoved(MouseEvent e) {
        if (gameplay.partie.isFreeze()) return;
        if (hoverProbe == null) return;
        int index = hoverProbe.ballAt(e.getX(), e.getY());
        gameplay.onTokenHovered(index);
    }

    @Override
    public void mouseDragged(MouseEvent e) {}

    // =========================================================================
    // KeyListener
    // =========================================================================

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameplay.partie.isFreeze()) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Z -> { if (e.isControlDown()) undo(); }
            case KeyEvent.VK_Y -> { if (e.isControlDown()) redo(); }
            case KeyEvent.VK_ENTER -> confirmPendingIntent();
            case KeyEvent.VK_ESCAPE -> {
                pendingIntent = null;
                gameplay.onTokenHovered(-1);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    // =========================================================================
    // Helper
    // =========================================================================

    private Vector2 screenToWorld(Vector2 screen) {
        return screen.Div(Camera.zoom).Add(Camera.positionHG);
    }
}