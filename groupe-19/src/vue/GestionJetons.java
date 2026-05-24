package src.vue;

import java.util.List;
import src.modele.Action;
import src.modele.JetonAction;

/**
 * GestionJetons — Gère les boules d'action et les indicateurs de tour.
 *
 * Instanciée et possédée par VueMonde ; les appelants extérieurs passent
 * toujours par VueMonde.
 */
public class GestionJetons {

    // =========================================================================
    // Constantes — noms de sprites
    // =========================================================================

    static final String[] BALL_FACE_PILE = { "action_alibi", "action_toby", "action_rotation", "action_rotation" };
    static final String[] BALL_FACE_FACE = { "action_holmes", "action_watson", "action_echange", "action_joker" };
    static final String[] TURN_FACE_PILE = { "T1","T2","T3","T4","T5","T6","T7","T8" };
    static final String   TURN_FACE_FACE = "T0";

    // =========================================================================
    // État interne
    // =========================================================================

    private final boolean[]     ballFaceIsPile         = { true, true, true, true };
    final         String[]      actionBallCurrentSprite = new String[4];
    /** Vrai si le jeton i a été utilisé (sprite grisé), faux sinon. */
    final         boolean[]     actionBallIsUsed        = { false, false, false, false };
    private final boolean[]     turnFaceIsPile          = { true, true, true, true, true, true, true, true };

    final Composant2D[] actionBalls    = new Composant2D[4];
    final Composant2D[] turnIndicators = new Composant2D[8];

    // =========================================================================
    // Constructeur
    // =========================================================================

    public GestionJetons() {
        initActionBalls();
        initTurnIndicators();
    }

    // =========================================================================
    // Initialisation
    // =========================================================================

    private void initActionBalls() {
        double diam    = 100;
        double cx      = VueMonde.STRIP_W + diam / 2.0 - 50;
        double spacing = VueMonde.BOARD_SIZE / BALL_FACE_PILE.length;
        for (int i = 0; i < BALL_FACE_PILE.length; i++) {
            double cy = VueMonde.BOARD_ORIGIN.y + spacing / 2.0 + i * spacing;
            actionBalls[i] = new Composant2D(
                    new Vector2(cx, cy), new Vector2(diam, diam), BALL_FACE_PILE[i]);
            actionBallCurrentSprite[i] = BALL_FACE_PILE[i];
        }
    }

    private void initTurnIndicators() {
        double diam    = 60;
        double radius  = diam / 2.0;
        double cx      = VueMonde.WORLD_W - radius - 20 - (1.25 * radius) - 100;
        double spacing = VueMonde.BOARD_SIZE / 8.0;
        for (int i = 0; i < 8; i++) {
            double cy = VueMonde.BOARD_ORIGIN.y + spacing / 2.0 + i * spacing;
            turnIndicators[i] = new Composant2D(
                    new Vector2(cx, cy), new Vector2(diam, diam), TURN_FACE_PILE[i]);
        }
    }

    // =========================================================================
    // Mises à jour
    // =========================================================================

    /** Met à jour les 4 jetons d'action selon leur face visible. */
    public void updateJetons(List<JetonAction> jetons) {
        for (int i = 0; i < 4 && i < actionBalls.length; i++) {
            JetonAction j = jetons.get(i);
            Action a = j.getActionVisible();
            String earlyName = switch (a) {
                case HOLMES   -> "action_holmes";
                case WATSON   -> "action_watson";
                case TOBY     -> "action_toby";
                case JOKER    -> "action_joker";
                case ROTATION -> "action_rotation";
                case ECHANGE  -> "action_echange";
                case ALIBI    -> "action_alibi";
            };
            boolean joue = j.isJoue();
            String spriteName = joue ? earlyName + "_grisee" : earlyName;
            actionBalls[i].spriteId = Camera.AddSprite(spriteName);
            actionBallCurrentSprite[i] = spriteName;
            actionBallIsUsed[i] = joue;
        }
    }

    /** Met en évidence l'indicateur de tour courant. */
    public void updateTurnIndicator(int turn) {
        for (int i = 0; i < 8; i++) {
            boolean doitEtreFace = (i < turn - 1);
            if (isTurnFacePile(i) == doitEtreFace) {
                switchTurnFace(i);
            }
        }
        for (int i = 0; i < turnIndicators.length; i++) {
            if (turnIndicators[i] == null) continue;
            turnIndicators[i].echelle = (i == turn - 1)
                    ? new Vector2(1.3, 1.3) : new Vector2(1.0, 1.0);
        }
    }

    /** Bascule la face d'un indicateur de tour (Pile ↔ Face). */
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

    /** Bascule la face d'une boule d'action (Pile ↔ Face). */
    public void switchBallFace(int ballIndex) {
        if (ballIndex < 0 || ballIndex >= 4) return;
        ballFaceIsPile[ballIndex] = !ballFaceIsPile[ballIndex];
        String sprite = ballFaceIsPile[ballIndex] ? BALL_FACE_PILE[ballIndex] : BALL_FACE_FACE[ballIndex];
        actionBalls[ballIndex].spriteId = Camera.AddSprite(sprite);
        actionBallCurrentSprite[ballIndex] = sprite;
    }

    /**
     * Retourne l'index (0-3) de la boule d'action sous les coordonnées monde
     * données, ou -1 si aucune boule n'est touchée.
     */
    public int actionBallAt(Vector2 worldPos) {
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
     * Notifie le listener si un clic touche une boule d'action.
     *
     * @param worldPos  Position en coordonnées monde du clic.
     * @param listener  Listener à notifier (peut être null).
     */
    public void notifyActionBallClick(Vector2 worldPos,
                                      VueMonde.ActionBallClickListener listener) {
        int i = actionBallAt(worldPos);
        if (i >= 0 && listener != null)
            listener.onActionBallHit(i, actionBallCurrentSprite[i]);
    }
}
