package src.vue;

import src.modele.*;
import javax.swing.*;

/**
 * Gameplay — Pont logique (Mediator).
 *
 * Responsabilités :
 *   - Orchestrer le cycle de jeu : initialisation → tours → fin de partie.
 *   - Servir d'unique point de communication entre VueJeu, IHMControler,
 *     IntermediaryGameState et GameEngine.
 *   - Décider quand un tour est terminé et déclencher la validation officielle.
 *   - Traiter les actions sur les boules (Holmes, Watson, Toby, Joker, Rotation, Échange, Alibi).
 */
public class Gameplay {

    private final VueJeu vue;
    private final IHMControler controler;
    public Partie partie;

    private int currentTurn = 1;
    private boolean gameOver = false;

    private boolean waitingForEchange = false;
    private int line = -1, column = -1;

    public boolean rotationMode = false;
    private int rotationRow = -1, rotationCol = -1;
    private int rotationsAccumulees = 0;
    private int rotationJetonIndex;

    public Gameplay(VueJeu vue, Partie partie) {
        this.vue = vue;
        this.partie = partie;
        this.controler = new IHMControler(this);
        vue.registerControler(controler);
        initGame();
    }

    private void initGame() {
        // Finalement, les 'updateX' sont gardé dans initGame, sinon les 'placeholders' sont visibles brièvement au début
        vue.updateBackground(partie.joueurCourant);
        vue.updateDistrictView(partie.district);
        vue.updateJetons(partie.jetonsAction);
        vue.updateDetectivesView(partie.detectives);
        startTurn(currentTurn);
    }

    private void startTurn(int turn) {
        System.out.println("Gameplay — début du tour " + turn);
        String playerType = "HUMAN";
        controler.setActivePlayerType(playerType);
        vue.updateTurnIndicator(turn);

        if ("AI".equals(playerType)) {
            SwingUtilities.invokeLater(controler::joueIa);
        }
    }

    public void resetGame() {
        partie.reset();
        currentTurn = 1;
        gameOver = false;
        vue.hideGameOverScreen();
        vue.updateTurnIndicator(currentTurn);
        vue.enableValidateButton(true);
        resetAllTurnIndicators();
        startTurn(currentTurn);
    }

    public void resetAllTurnIndicators() {
        for (int i = 0; i < 8; i++) {
            if (!vue.isTurnFacePile(i)) {
                vue.switchTurnFace(i);
            }
        }
    }

    public IHMControler getControler() {
        return controler;
    }

    public void onValidatePressed() {
        if (gameOver) return;
        controler.confirmPendingIntent();
        endTurn();
    }

    private void endTurn() {
        System.out.println("Gameplay — fin du tour " + currentTurn);
        if (currentTurn >= 1 && currentTurn <= 8) vue.switchTurnFace(currentTurn - 1);
        partie.appelATemoin();
        if (partie.getGagnant()!=null){
            onGameOver();
            return;
        }

        currentTurn++;
        if (currentTurn > 8) {
            onGameOver();
        } else {
            startTurn(currentTurn);
        }
    }

    private void onGameOver() {
        gameOver = true;
        System.out.println("Gameplay — fin de partie");
        vue.showGameOverScreen(partie.getGagnant().getNom());
    }

    // -------------------------------------------------------------------------
    // Actions des boules (jetons) – appelée par IHMControler
    // -------------------------------------------------------------------------

    public void onActionBallClicked(IHMControler.ActionIntent intent) {
        String actionName = intent.actionName();
        int ballIndex = intent.ballIndex();

        System.out.println("Gameplay — action sur boule " + ballIndex + " : " + actionName);

        switch (actionName) {
            case "action_holmes" -> demanderDeplacementEtDeplacer(Detective.Type.HOLMES);
            case "action_watson" -> demanderDeplacementEtDeplacer(Detective.Type.WATSON);
            case "action_toby"   -> demanderDeplacementEtDeplacer(Detective.Type.TOBY);
            case "action_joker"  -> gererJoker();
            case "action_rotation" -> startRotationMode(ballIndex);
            case "action_echange"  -> startEchange();
            case "action_alibi"    -> partie.actions.alibi();
            default -> System.out.println("Action inconnue : " + actionName);
        }

        //vue.updateDistrictView(partie.district);
        //vue.updateJetons(partie.jetonsAction);
        //vue.updateDetectivesView(partie.detectives);   //correction
        //vue.refreshBoardComponents();
    }

    private void demanderDeplacementEtDeplacer(Detective.Type type) {
        Detective detective = trouverDetective(type);
        if (detective == null) return;

        String input = JOptionPane.showInputDialog(vue,
                "Déplacer " + type + " de combien de pas ? (1 ou 2)",
                "Déplacement",
                JOptionPane.QUESTION_MESSAGE);
        if (input == null) return;

        try {
            int pas = Integer.parseInt(input);
            if (pas != 1 && pas != 2) pas = 1;
            partie.actions.deplacerDetective(detective, pas);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vue, "Veuillez entrer 1 ou 2.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void gererJoker() {
        Partie.Joueur joueur = partie.joueurCourant;
        if (joueur == Partie.Joueur.ENQUETEUR) {
            String[] options = {"Holmes", "Watson", "Toby"};
            int choix = JOptionPane.showOptionDialog(vue,
                    "Quel détective voulez-vous déplacer d’un pas ?",
                    "Action Joker",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (choix < 0) return;
            Detective detective = switch (choix) {
                case 0 -> trouverDetective(Detective.Type.HOLMES);
                case 1 -> trouverDetective(Detective.Type.WATSON);
                case 2 -> trouverDetective(Detective.Type.TOBY);
                default -> null;
            };
            if (detective != null) partie.actions.joker(detective);
        } else {
            String[] options = {"Holmes", "Watson", "Toby", "Ne rien déplacer"};
            int choix = JOptionPane.showOptionDialog(vue,
                    "Choisissez une action (déplacer un détective d'un pas ou rien)",
                    "Action Joker - Mr. Jack",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (choix < 0) return;
            Detective detective = switch (choix) {
                case 0 -> trouverDetective(Detective.Type.HOLMES);
                case 1 -> trouverDetective(Detective.Type.WATSON);
                case 2 -> trouverDetective(Detective.Type.TOBY);
                default -> null;
            };
            partie.actions.joker(detective);
        }
    }

    private Detective trouverDetective(Detective.Type type) {
        for (Detective d : partie.detectives) {
            if (d.getType() == type) return d;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Actions Échange et Rotation
    // -------------------------------------------------------------------------

    public void startEchange() {
        waitingForEchange = true;
        line = column = -1;
        System.out.println("Action échange de tuiles : cliquez sur la première tuile");
    }

    public void startRotationMode(int jetonIndex) {
        rotationMode = true;
        rotationJetonIndex = jetonIndex;
        System.out.println("Mode rotation, cliquez sur une tuile pour la faire pivoter. Cliquez hors plateau pour sortir.");
    }

    public void exitRotationMode() {
        if (rotationRow != -1 && rotationCol != -1) {
            partie.actions.rotationQuartier(rotationJetonIndex, rotationRow, rotationCol, rotationsAccumulees);
        }
        rotationMode = false;
        rotationRow = -1;
        rotationCol = -1;
        rotationsAccumulees = 0;
        System.out.println("Mode rotation terminé.");
    }

    // -------------------------------------------------------------------------
    // Callbacks depuis IHMControler (clics sur plateau)
    // -------------------------------------------------------------------------

    public void onActionConfirmed(IHMControler.ClickIntent intent) {
        System.out.println("Gameplay — action confirmée : (" + intent.cellRow() + "," + intent.cellCol() + ")");
        Camera.RecalculateZoom();
    }

    public void onUndoRequested(IHMControler.ClickIntent intent) {
        System.out.println("Gameplay — undo (" + intent.cellRow() + "," + intent.cellCol() + ")");
        Camera.RecalculateZoom();
    }

    public void onRedoRequested(IHMControler.ClickIntent intent) {
        System.out.println("Gameplay — redo (" + intent.cellRow() + "," + intent.cellCol() + ")");
        Camera.RecalculateZoom();
    }

    public void clicHorsDistrict() {
        if (rotationMode) exitRotationMode();
    }

    public void onCellHovered(int row, int col) {
        if (waitingForEchange) {
            if (line == -1) {
                line = row;
                column = col;
                System.out.println("Première tuile sélectionnée : (" + line + "," + column + ")");
            } else if (line != row || column != col) {
                System.out.println("Deuxième tuile sélectionnée : (" + row + "," + col + ")");
                partie.actions.echange(line, column, row, col);
                waitingForEchange = false;
            }
        } else if (rotationMode) {
            if (rotationRow == -1) {
                rotationsAccumulees = 1;
                rotationRow = row;
                rotationCol = col;
                vue.rotateTile(row, col, 90);
                System.out.println("Tuile sélectionnée pour rotation visuelle");
            } else if (row == rotationRow && col == rotationCol) {
                rotationsAccumulees = (rotationsAccumulees + 1) % 4;
                vue.rotateTile(row, col, 90);
                System.out.println("Rotation visuelle supplémentaire");
            } else {
                exitRotationMode();
            }
        }
    }
}