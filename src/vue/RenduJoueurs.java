package src.vue;

import java.awt.*;

/**
 * RenduJoueurs  Dessine les deux rectangles joueurs (Enquêteur et Jack)
 * directement dans l'espace caméra (rendu vectoriel).
 *
 * Instanciée et possédée par VueMonde ; les appelants extérieurs passent
 * toujours par VueMonde.
 */
public class RenduJoueurs {

    // 
    // Constantes de position (espace monde)
    // 

    static final double PLAYER_RECT_W_WORLD = 200.0;
    static final double PLAYER_RECT_H_WORLD =  55.0;

    /** Centre X des deux rectangles : milieu horizontal du plateau. */
    static final double PLAYER_RECT_CX =
            VueMonde.BOARD_ORIGIN.x + VueMonde.BOARD_SIZE / 2.0;

    /** Centre Y du rectangle Enquêteur (au-dessus du plateau, marge 110 px). */
    static final double PLAYER_RECT_CY_TOP =
            VueMonde.BOARD_ORIGIN.y - PLAYER_RECT_H_WORLD / 2.0 - 110.0;

    /** Centre Y du rectangle Jack (en dessous du plateau, marge 110 px). */
    static final double PLAYER_RECT_CY_BOT =
            VueMonde.BOARD_ORIGIN.y + VueMonde.BOARD_SIZE + PLAYER_RECT_H_WORLD / 2.0 + 110.0;

    // 
    // État interne
    // 

    /** Noms affichés dans les rectangles joueurs (index 0 = Enquêteur, 1 = Jack). */
    private final String[] playerNames  = { "", "" };

    /** Types affichés sous les noms (ex : "Humain", "IA Facile"). */
    private final String[] playerTypes  = { "", "" };

    /** Couleurs des rectangles joueurs. */
    private final Color[]  playerColors = {
        new Color( 40,  80, 180, 200),   // Enquêteur (bleu)
        new Color(180,  40,  40, 200)    // Jack (rouge)
    };

    // 
    // Setters
    // 

    public void setPlayerName(int player, String name) {
        if (player >= 0 && player < 2)
            playerNames[player] = (name != null) ? name : "";
    }

    public void setPlayerType(int player, String type) {
        if (player >= 0 && player < 2)
            playerTypes[player] = (type != null) ? type : "";
    }

    public void updateRectColor(int player, Color color) {
        if (player >= 0 && player < 2)
            playerColors[player] = color;
    }

    public void updatePlayerRectangles(src.modele.Joueur joueurCourant) {
        if (joueurCourant == src.modele.Joueur.JACK) {
            updateRectColor(0, Color.GRAY);
            updateRectColor(1, new Color(180, 40, 40, 200));
        } else {
            updateRectColor(1, Color.GRAY);
            updateRectColor(0, new Color(40, 80, 180, 200));
        }
    }

    // 
    // Rendu
    // 

    /**
     * Dessine les deux rectangles joueurs dans l'espace caméra.
     * Doit être appelé depuis paintComponent() de la Camera.
     *
     * @param g Contexte graphique 2D de la Camera (coordonnées écran).
     */
    public void paintPlayerRects(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double[] worldCY = { PLAYER_RECT_CY_TOP, PLAYER_RECT_CY_BOT };

        for (int p = 0; p < 2; p++) {
            //  Transformation monde écran 
            double worldX = PLAYER_RECT_CX - PLAYER_RECT_W_WORLD / 2.0;
            double worldY = worldCY[p]      - PLAYER_RECT_H_WORLD / 2.0;

            double sx = (worldX - Camera.positionHG.x) * Camera.zoom.x;
            double sy = (worldY - Camera.positionHG.y) * Camera.zoom.y;
            double sw = PLAYER_RECT_W_WORLD * Camera.zoom.x;
            double sh = PLAYER_RECT_H_WORLD * Camera.zoom.y;

            int rx  = (int) Math.round(sx);
            int ry  = (int) Math.round(sy);
            int rw  = (int) Math.round(sw);
            int rh  = (int) Math.round(sh);
            int arc = (int) Math.round(14 * Camera.zoom.x);

            //  Fond semi-transparent avec coins arrondis ---
            g.setColor(playerColors[p]);
            g.fillRoundRect(rx, ry, rw, rh, arc, arc);

            //  Bordure contrastée -
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke((float) Math.max(1.0, 2.0 * Camera.zoom.x)));
            g.drawRoundRect(rx, ry, rw, rh, arc, arc);
            g.setStroke(new BasicStroke(1f));

            //  Textes (nom + type) centrés -
            float baseFontSize  = (float) (13.0 * Camera.zoom.x);
            float smallFontSize = (float) Math.max(8.0, 10.0 * Camera.zoom.x);

            Font nameFont = new Font("SansSerif", Font.BOLD,  Math.max(8, (int) baseFontSize));
            Font typeFont = new Font("SansSerif", Font.PLAIN, Math.max(7, (int) smallFontSize));

            String  name    = playerNames[p];
            String  type    = playerTypes[p];
            boolean hasType = type != null && !type.isEmpty();

            FontMetrics fmName = g.getFontMetrics(nameFont);
            FontMetrics fmType = hasType ? g.getFontMetrics(typeFont) : null;

            int totalTextH = fmName.getAscent() + fmName.getDescent()
                           + (hasType ? (int)(2 * Camera.zoom.y) + fmType.getAscent() + fmType.getDescent() : 0);

            int textStartY = ry + (rh - totalTextH) / 2 + fmName.getAscent();

            // Nom
            g.setFont(nameFont);
            g.setColor(Color.WHITE);
            int nameX = rx + (rw - fmName.stringWidth(name)) / 2;
            g.drawString(name, nameX, textStartY);

            // Type
            if (hasType) {
                g.setFont(typeFont);
                g.setColor(new Color(220, 220, 220, 220));
                int typeY = textStartY + fmName.getDescent() + (int)(2 * Camera.zoom.y) + fmType.getAscent();
                int typeX = rx + (rw - fmType.stringWidth(type)) / 2;
                g.drawString(type, typeX, typeY);
            }
        }
    }
}
