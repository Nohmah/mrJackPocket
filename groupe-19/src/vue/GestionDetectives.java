package src.vue;

import java.util.List;
import src.modele.Detective;

/**
 * GestionDetectives Gère les pions détectives sur l'anneau extérieur du plateau.
 *
 * Instanciée et possédée par VueMonde ; les appelants extérieurs passent
 * toujours par VueMonde.
 */
public class GestionDetectives {

    // 
    // Constantes
    // 

    private static final double    OUTER_BALL_DIAM = 80.0;
    private static final Vector2[] OUTER_POSITIONS = buildOuterPositions();

    private static final double DETECTIVE_SCALE_1 = 1.0;
    private static final double DETECTIVE_SCALE_2 = 0.9;
    private static final double DETECTIVE_SCALE_3 = 0.8;

    private static Vector2[] buildOuterPositions() {
        double gap = OUTER_BALL_DIAM / 2.0 + 5;
        Vector2[] pos = new Vector2[12];
        for (int i = 0; i < 3; i++)
            pos[i] = new Vector2(VueMonde.BOARD_ORIGIN.x + i * VueMonde.TILE_SIZE + VueMonde.TILE_SIZE / 2.0,
                                 VueMonde.BOARD_ORIGIN.y - gap);
        for (int i = 0; i < 3; i++)
            pos[3 + i] = new Vector2(VueMonde.BOARD_ORIGIN.x + VueMonde.BOARD_SIZE + gap,
                                     VueMonde.BOARD_ORIGIN.y + i * VueMonde.TILE_SIZE + VueMonde.TILE_SIZE / 2.0);
        for (int i = 0; i < 3; i++)
            pos[6 + i] = new Vector2(VueMonde.BOARD_ORIGIN.x + (2 - i) * VueMonde.TILE_SIZE + VueMonde.TILE_SIZE / 2.0,
                                     VueMonde.BOARD_ORIGIN.y + VueMonde.BOARD_SIZE + gap);
        for (int i = 0; i < 3; i++)
            pos[9 + i] = new Vector2(VueMonde.BOARD_ORIGIN.x - gap,
                                     VueMonde.BOARD_ORIGIN.y + (2 - i) * VueMonde.TILE_SIZE + VueMonde.TILE_SIZE / 2.0);
        return pos;
    }

    // 
    // État interne
    // 

    private final int[]         detectivePosition   = { -1, -1, -1 };
    final         Composant2D[] detectiveComponents = new Composant2D[3];
    final         Composant2D[] outerBalls          = new Composant2D[12];

    // 
    // API publique (délégation depuis VueMonde)
    // 

    /** Met à jour l'affichage des 3 détectives à partir des positions du modèle. */
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

    /** Déplace un détective sur l'anneau (position 1–12). */
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
    }

    /** Retourne la position courante (0-based) du détective donné (0-based). */
    public int getDetectivePosition(int detIdx) {
        if (detIdx < 0 || detIdx >= 3) return -1;
        return detectivePosition[detIdx];
    }

    // 
    // Privé
    // 

    private void refreshOuterBallScale(int posIdx) {
        int stackCount = 0;
        int[] onCell = new int[3];
        for (int d = 0; d < 3; d++)
            if (detectivePosition[d] == posIdx && detectiveComponents[d] != null)
                onCell[stackCount++] = d;
        Vector2[] offsets = getStackOffsets(stackCount);
        double scale = getStackScale(stackCount);
        for (int k = 0; k < stackCount; k++) {
            detectiveComponents[onCell[k]].echelle  = new Vector2(scale, scale);
            detectiveComponents[onCell[k]].position = OUTER_POSITIONS[posIdx].Add(offsets[k]);
        }
    }

    private double getStackScale(int stackCount) {
        if (stackCount >= 3) return DETECTIVE_SCALE_3;
        if (stackCount == 2) return DETECTIVE_SCALE_2;
        return DETECTIVE_SCALE_1;
    }

    private Vector2[] getStackOffsets(int stackCount) {
        double delta = OUTER_BALL_DIAM * 0.4;
        if (stackCount == 2) {
            return new Vector2[] { new Vector2(-delta, 0), new Vector2(delta, 0) };
        }
        if (stackCount >= 3) {
            return new Vector2[] {
                new Vector2(0, -delta),
                new Vector2(-delta, delta * 0.6),
                new Vector2( delta, delta * 0.6)
            };
        }
        return new Vector2[] { new Vector2(0, 0) };
    }
}
