package src.vue;

import src.modele.District;
import src.modele.JetonAction;
import src.modele.Partie;

import java.util.List;

/**
 * Gameplay — Pont logique (Mediator).
 *
 * Responsabilités :
 *   - Orchestrer le cycle de jeu : initialisation → tours → fin de partie.
 *   - Servir d'unique point de communication entre VueJeu, IHMControler,
 *     IntermediaryGameState et GameEngine.
 *   - Décider quand un tour est terminé et déclencher la validation officielle.
 *
 * Instancié par VueJeu après création de la Camera.
 */
public class Gameplay {

    // -------------------------------------------------------------------------
    // Références aux couches adjacentes
    // -------------------------------------------------------------------------

    private final VueJeu vue;
    private final IHMControler controler;
    private Partie partie;
    /**
     * Numéro du tour courant (1-indexé).
     * La partie comporte typiquement 8 tours (un par boule de tour).
     */
    private int currentTurn = 1;

    /**
     * Indique si la partie est terminée.
     */
    private boolean gameOver = false;

    // -------------------------------------------------------------------------
    // Constructeur
    // -------------------------------------------------------------------------

    public  Gameplay(VueJeu vue, Partie partie) {
        this.vue = vue;
        this.partie = partie;
        this.controler = new IHMControler(this);

        // Branche le contrôleur sur la Vue (écouteurs souris + clavier)
        vue.registerControler(controler);

        initGame();
    }

    // -------------------------------------------------------------------------
    // Accesseur (utilisé par VueJeu pour brancher les listeners)
    // -------------------------------------------------------------------------

    public IHMControler getControler() {
        return controler;
    }

    // -------------------------------------------------------------------------
    // Cycle de vie de la partie
    // -------------------------------------------------------------------------

    /**
     * Initialise la partie : charge l'état initial et prépare le premier tour.
     */
    private void initGame() {
        // RÉCEPTION : Récupère la configuration initiale depuis GameEngine
        // (type de joueurs, état de la grille, etc.) pour paramétrer la Vue.
        // GameConfig config = GameEngine.getInitialConfig();

        // RÉCEPTION : Récupère la grille initiale depuis IntermediaryGameState
        // pour instancier les Composant2D correspondant aux tuiles.
        // Grid initialGrid = IntermediaryGameState.getInitialGrid();
        // vue.buildBoardComponents(initialGrid);
        District district = partie.district;  // accède au district
        vue.updateDistrictView(district);
        startTurn(currentTurn);
    }

    /**
     * Démarre un tour : configure le contrôleur selon le type de joueur actif
     * et lance l'IA si nécessaire.
     *
     * @param turn Numéro du tour (1-8).
     */
    private void startTurn(int turn) {
        System.out.println("Gameplay — début du tour " + turn);

        // RÉCEPTION : Demande à IntermediaryGameState quel est le joueur actif
        // pour ce tour et son type (HUMAN / AI).
        // String playerType = IntermediaryGameState.getActivePlayerType(turn);
        String playerType = "HUMAN"; // Simulation — remplacer par l'appel réel

        controler.setActivePlayerType(playerType);
        vue.updateTurnIndicator(turn);
        vue.updateJetons(partie.actions.getJetonsActions()); // On met à jour la vue des jetons (apres un lancer ou un retourner)

        if ("AI".equals(playerType)) {
            // Lance l'action IA de façon asynchrone pour ne pas bloquer Swing
            javax.swing.SwingUtilities.invokeLater(controler::joueIa);
        }
    }

    /**
     * Appelé par VueJeu quand l'utilisateur appuie sur "Valider".
     * Déclenche la fin du tour et la validation officielle de l'état.
     */
    public void onValidatePressed() {
        if (gameOver) return;

        // Confirme l'éventuelle intention en attente avant de valider
        controler.confirmPendingIntent();

        endTurn();
    }

    /**
     * Clôture le tour courant et demande à GameEngine de valider officiellement.
     *
     * C'est ici que le signal de "Fin de tour" est reçu pour envoyer l'état
     * intermédiaire vers FinalGameState via GameEngine.
     */
    private void endTurn() {
        System.out.println("Gameplay — fin du tour " + currentTurn + " : validation en cours");

        partie.appelATemoin();
        vue.updateDistrictView(partie.district);
        vue.updateJetons(partie.actions.getJetonsActions());
        // ENVOI : Transmet la liste des actions du tour à GameEngine pour qu'il
        // les valide officiellement et mette à jour FinalGameState.
        // List<IHMControler.ClickIntent> actions = IntermediaryGameState.getPendingActions();
        // GameEngine.validateTurn(actions);Validation officielle via FinalGameState

        // RÉCEPTION : Récupère le nouvel état depuis FinalGameState après validation
        // afin de mettre à jour les Composant2D affichés par la Camera.
        // FinalGameState finalState = GameEngine.getFinalState();
        // vue.refreshBoardComponents(finalState);

        // Vérifie si la partie est terminée
        // gameOver = GameEngine.isGameOver();
        if (gameOver) {
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

    /**
     * Gère la fin de partie.
     */
    private void onGameOver() {
        gameOver = true;
        System.out.println("Gameplay — fin de partie");

        // RÉCEPTION : Récupère le score final depuis FinalGameState.
        // GameResult result = FinalGameState.getResult();
        // vue.showGameOverScreen(result);
        vue.showGameOverScreen();
    }

    // -------------------------------------------------------------------------
    // Callbacks reçus depuis IHMControler
    // -------------------------------------------------------------------------

    /**
     * Notifié quand l'utilisateur a confirmé une action (clic + validation).
     *
     * @param intent Intention de jeu confirmée.
     */
    public void onActionConfirmed(IHMControler.ClickIntent intent) {
        System.out.println("Gameplay — action confirmée : "
                + "cellule (" + intent.cellRow() + "," + intent.cellCol() + ")");

        // ENVOI : Transmet l'intention à IntermediaryGameState pour application
        // provisoire (avant la validation officielle de fin de tour).
        // IntermediaryGameState.applyProvisionalAction(intent);

        // Rafraîchit la Vue pour refléter l'état provisoire
        vue.refreshBoardHighlight(intent.cellRow(), intent.cellCol());
        Camera.RecalculateZoom(); // Force un repaint via RecalculateZoom (proxy de Repaint)
    }

    /**
     * Notifié quand un Undo est demandé par le contrôleur.
     *
     * @param intent Intention annulée.
     */
    public void onUndoRequested(IHMControler.ClickIntent intent) {
        System.out.println("Gameplay — undo cellule ("
                + intent.cellRow() + "," + intent.cellCol() + ")");

        // ENVOI : Demande à IntermediaryGameState d'annuler l'action provisoire.
        // IntermediaryGameState.rollbackAction(intent);

        vue.clearBoardHighlight(intent.cellRow(), intent.cellCol());
        Camera.RecalculateZoom();
    }

    /**
     * Notifié quand un Redo est demandé par le contrôleur.
     *
     * @param intent Intention rétablie.
     */
    public void onRedoRequested(IHMControler.ClickIntent intent) {
        System.out.println("Gameplay — redo cellule ("
                + intent.cellRow() + "," + intent.cellCol() + ")");

        // ENVOI : Demande à IntermediaryGameState de ré-appliquer l'action.
        // IntermediaryGameState.reapplyAction(intent);

        vue.refreshBoardHighlight(intent.cellRow(), intent.cellCol());
        Camera.RecalculateZoom();
    }

    /**
     * Notifié quand la souris survole une cellule (pour le retour visuel).
     *
     * @param row Ligne survolée.
     * @param col Colonne survolée.
     */
    public void onCellHovered(int row, int col) {
        // RÉCEPTION : Interroge IntermediaryGameState pour savoir si la cellule
        // (row, col) est un coup légal, afin d'adapter le curseur ou la couleur.
        // boolean legal = IntermediaryGameState.isMoveLegal(row, col);
        // vue.setCellHighlight(row, col, legal);
    }
}
