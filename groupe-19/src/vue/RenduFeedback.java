package src.vue;

import java.awt.*;
import javax.swing.Timer;

/**
 * RenduFeedback — Feedback visuel interactif sur le plateau.
 *
 * Gère : survol des jetons d'action, sélection de tuile, clignotement,
 * lisérés de board, cibles de déplacement, info-bulles.
 *
 * Reçoit ses dépendances par injection en constructeur.
 * Instanciée et possédée par VueMonde ; les appelants extérieurs passent
 * toujours par VueMonde.
 */
public class RenduFeedback {

    // =========================================================================
    // Dépendances injectées
    // =========================================================================

    /** Accès aux jetons d'action (sprites et état « utilisé »). */
    private final GestionJetons gestionJetons;

    /** Accès aux composants tuile pour les lisérés et le feedback sélection. */
    private final Composant2D[][] tileComponents;

    /** Accès aux positions courantes des détectives (0-based). */
    private final GestionDetectives gestionDetectives;

    // =========================================================================
    // État feedback
    // =========================================================================

    /** Index du jeton d'action actuellement survolé (-1 = aucun). */
    private int hoveredTokenIndex = -1;

    private int selectedRow = -1;
    private int selectedCol = -1;

    private int indexJetonClique = -1;
    private float clignotePhase = 0.0f;

    private final Timer timerClignote = new Timer(33, e -> {
        clignotePhase += 0.08f;
        if (clignotePhase > Math.PI * 2) clignotePhase -= (float)(Math.PI * 2);
        Camera.Repaint();
    });

    // =========================================================================
    // Constructeur
    // =========================================================================

    /**
     * @param gestionJetons      Instance de GestionJetons pour lire les sprites/états.
     * @param tileComponents     Tableau 3×3 des composants tuile du plateau.
     * @param gestionDetectives  Instance de GestionDetectives pour les positions.
     */
    public RenduFeedback(GestionJetons gestionJetons,
                         Composant2D[][] tileComponents,
                         GestionDetectives gestionDetectives) {
        this.gestionJetons     = gestionJetons;
        this.tileComponents    = tileComponents;
        this.gestionDetectives = gestionDetectives;
        timerClignote.start();
    }

    // =========================================================================
    // Setters (délégation depuis VueMonde)
    // =========================================================================

    public void setHoveredTokenIndex(int index) {
        hoveredTokenIndex = index;
    }

    public int getHoveredTokenIndex() {
        return hoveredTokenIndex;
    }

    public void setTuileSelected(int row, int col) {
        selectedRow = row;
        selectedCol = col;
    }

    public void setJetonActif(int index) {
        this.indexJetonClique = index;
        Camera.Repaint();
    }

    // =========================================================================
    // Rendu — appelé depuis paintComponent() de la Camera
    // =========================================================================

    /** Dessine le calque de survol + lisérés permanents. */
    public void paintHoverOverlay(Graphics2D g) {
        drawUnusedActionBorders(g);

        if (hoveredTokenIndex < 0) return;
        String sprite = gestionJetons.actionBallCurrentSprite[hoveredTokenIndex];
        if (sprite == null) return;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (sprite) {
            case "action_rotation", "action_echange" -> dessinerLisereBoard(g);
            case "action_holmes" -> dessinerCibleDeplacement(g,
                    gestionDetectives.getDetectivePosition(0),
                    new Color(200, 40,  40,  130), 2, false);
            case "action_watson" -> dessinerCibleDeplacement(g,
                    gestionDetectives.getDetectivePosition(1),
                    new Color(120, 60,  20,  130), 2, false);
            case "action_toby"   -> dessinerCibleDeplacement(g,
                    gestionDetectives.getDetectivePosition(2),
                    new Color( 40, 80, 200, 130), 2, false);
            case "action_joker"  -> {
                dessinerCibleDeplacement(g, gestionDetectives.getDetectivePosition(0),
                        new Color(200, 40,  40,  130), 1, true);
                dessinerCibleDeplacement(g, gestionDetectives.getDetectivePosition(1),
                        new Color(120, 60,  20,  130), 1, true);
                dessinerCibleDeplacement(g, gestionDetectives.getDetectivePosition(2),
                        new Color( 40, 80, 200, 130), 1, true);
            }
            // action_alibi et variantes _grisee : pas de feedback géométrique
        }
    }

    /** Dessine le calque de sélection de tuile. */
    public void paintSelectedOverlay(Graphics2D g) {
        if (selectedRow >= 0)
            dessinerTuileFeedback(g, selectedRow, selectedCol, new Color(255, 220, 0, 150));
    }

    // =========================================================================
    // Tooltip texte
    // =========================================================================

    public String getActionTooltipText(String action) {
        return switch (action) {
            case "action_holmes"   -> "Déplacer Holmes (1 ou 2 pas).";
            case "action_watson"   -> "Déplacer Watson (1 ou 2 pas).";
            case "action_toby"     -> "Déplacer Toby (1 ou 2 pas).";
            case "action_joker"    -> "Déplacer un des détective (0 ou 1 pas).";
            case "action_rotation" -> "Pivoter un quartier de 90°.";
            case "action_echange"  -> "Échanger deux quartiers.";
            case "action_alibi"    -> "Piocher une carte alibi.";
            default -> "";
        };
    }

    public Point getActionBallScreenPos(int index) {
        Composant2D ball = gestionJetons.actionBalls[index];
        if (ball == null) return new Point(0, 0);
        Vector2 screenCenter = ball.position.Sub(Camera.positionHG).Mult(Camera.zoom);
        return new Point((int) Math.round(screenCenter.x), (int) Math.round(screenCenter.y));
    }

    // =========================================================================
    // Méthodes de dessin privées
    // =========================================================================

    private void dessinerTuileFeedback(Graphics2D g, int row, int col, Color couleur) {
        Composant2D comp = tileComponents[row][col];
        if (comp == null) return;
        Stroke ancienneStroke = g.getStroke();
        Color ancienneCouleur = g.getColor();

        Vector2 ecranPosition = comp.CoinHG().Sub(Camera.positionHG).Mult(Camera.zoom);
        Vector2 screenTail    = comp.TailleRel().Mult(Camera.zoom);

        g.setColor(couleur);
        g.setStroke(new BasicStroke(5f));
        g.drawRect((int) ecranPosition.x, (int) ecranPosition.y,
                   (int) screenTail.x,    (int) screenTail.y);

        g.setStroke(ancienneStroke);
        g.setColor(ancienneCouleur);
    }

    private void drawUnusedActionBorders(Graphics2D g) {
        Stroke ancienStroke  = g.getStroke();
        Color  ancienneColor = g.getColor();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(3f));
        boolean hasHover = hoveredTokenIndex >= 0;

        // Contour rouge pour le jeton action cliqué tant qu'il n'a pas été utilisé
        if (indexJetonClique >= 0 && !gestionJetons.actionBallIsUsed[indexJetonClique]){
            g.setStroke(new BasicStroke(5f));
            drawActionTokenBorder(g, indexJetonClique, new Color(50, 205, 50, 200));
            g.setStroke(new BasicStroke(3f));
        }


        // Contour jaune
        if (hasHover
                && hoveredTokenIndex < gestionJetons.actionBalls.length
                && !gestionJetons.actionBallIsUsed[hoveredTokenIndex]
                && hoveredTokenIndex != indexJetonClique) {
            drawActionTokenBorder(g, hoveredTokenIndex, new Color(255, 220, 0, 150));
            g.setColor(ancienneColor);
            g.setStroke(ancienStroke);
            return;
        }

        float pulse = (float)((Math.sin(clignotePhase) + 1.0) / 2.0);
        int alpha = 80 + Math.round(140 * pulse);
        Color jaune = new Color(255, 220, 0, alpha);
        g.setColor(jaune);

        for (int i = 0; i < gestionJetons.actionBalls.length; i++) {
            if (!gestionJetons.actionBallIsUsed[i] && i!=indexJetonClique) drawActionTokenBorder(g, i, jaune);
        }
        g.setStroke(ancienStroke);
        g.setColor(ancienneColor);
    }

    private void drawActionTokenBorder(Graphics2D g, int index, Color color) {
        if (index < 0 || index >= gestionJetons.actionBalls.length) return;
        Composant2D ball = gestionJetons.actionBalls[index];
        if (ball == null) return;

        double rx = (ball.taille.x * ball.echelle.x) / 2.0;
        double ry = (ball.taille.y * ball.echelle.y) / 2.0;
        double r  = Math.min(rx, ry);

        Vector2 screenCenter = ball.position.Sub(Camera.positionHG).Mult(Camera.zoom);
        double  screenR      = r * Camera.zoom.x;

        g.setColor(color);
        g.drawOval(
            (int) Math.round(screenCenter.x - screenR),
            (int) Math.round(screenCenter.y - screenR),
            (int) Math.round(screenR * 2),
            (int) Math.round(screenR * 2)
        );
    }

    private void dessinerLisereBoard(Graphics2D g) {
        Color  jaune       = new Color(255, 220, 0, 150);
        Stroke ancienStroke  = g.getStroke();
        Color  ancienneColor = g.getColor();

        g.setStroke(new BasicStroke(3f));
        g.setColor(jaune);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Composant2D comp = tileComponents[row][col];
                if (comp == null) continue;
                Vector2 screenPos  = comp.CoinHG().Sub(Camera.positionHG).Mult(Camera.zoom);
                Vector2 screenTail = comp.TailleRel().Mult(Camera.zoom);
                g.drawRect((int) screenPos.x, (int) screenPos.y,
                           (int) screenTail.x, (int) screenTail.y);
            }
        }

        g.setStroke(ancienStroke);
        g.setColor(ancienneColor);
    }

    private void dessinerCibleDeplacement(Graphics2D g, int positionIndex,
                                          Color couleur, int maxPas, boolean inclureZero) {
        if (positionIndex < 0 || positionIndex >= 12) return;

        Stroke ancienneStroke = g.getStroke();
        Color  ancienneCouleur = g.getColor();
        g.setStroke(new BasicStroke(2.0f));

        double baseR = (80.0 * 0.5) * Camera.zoom.x; // OUTER_BALL_DIAM = 80

        if (inclureZero)
            dessinerCibleFeedforward(g, positionIndex,
                    new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), 80),
                    baseR * 0.8);

        for (int pas = 1; pas <= maxPas; pas++) {
            int cibleIndex = (positionIndex + pas) % 12;
            dessinerCibleFeedforward(g, cibleIndex, couleur, baseR);
        }

        g.setStroke(ancienneStroke);
        g.setColor(ancienneCouleur);
    }

    private void dessinerCibleFeedforward(Graphics2D g, int positionIndex,
                                           Color couleur, double radius) {
        // Reconstruit les positions de l'anneau à partir de VueMonde
        Vector2 centre = computeOuterPosition(positionIndex);
        Vector2 ecranCentre = centre.Sub(Camera.positionHG).Mult(Camera.zoom);

        float pulse = (float)((Math.sin(clignotePhase) + 1.0) / 2.0);
        int alphaRemplissage = 30 + Math.round(90  * pulse);
        int alphaContour     = 80 + Math.round(140 * pulse);

        g.setColor(new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), alphaRemplissage));
        g.fillOval((int) Math.round(ecranCentre.x - radius),
                   (int) Math.round(ecranCentre.y - radius),
                   (int) Math.round(radius * 2),
                   (int) Math.round(radius * 2));

        g.setColor(new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), alphaContour));
        g.drawOval((int) Math.round(ecranCentre.x - radius),
                   (int) Math.round(ecranCentre.y - radius),
                   (int) Math.round(radius * 2),
                   (int) Math.round(radius * 2));
    }

    /**
     * Recalcule la position de l'anneau à l'index donné (miroir de
     * GestionDetectives.buildOuterPositions()).
     */
    private static Vector2 computeOuterPosition(int idx) {
        double gap  = 80.0 / 2.0 + 5; // OUTER_BALL_DIAM / 2 + 5
        double bx   = VueMonde.BOARD_ORIGIN.x;
        double by   = VueMonde.BOARD_ORIGIN.y;
        double ts   = VueMonde.TILE_SIZE;
        double bs   = VueMonde.BOARD_SIZE;
        if (idx < 3)
            return new Vector2(bx + idx * ts + ts / 2.0, by - gap);
        if (idx < 6)
            return new Vector2(bx + bs + gap, by + (idx - 3) * ts + ts / 2.0);
        if (idx < 9)
            return new Vector2(bx + (8 - idx) * ts + ts / 2.0, by + bs + gap);
        return new Vector2(bx - gap, by + (11 - idx) * ts + ts / 2.0);
    }
}
