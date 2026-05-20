package src.vue;

import src.modele.*;
import javax.swing.*;
import java.awt.Color;

/**
 * Gameplay — Médiateur (Mediator).
 *
 * Responsabilités :
 *   - Orchestrer le cycle de jeu : initialisation → tours → fin de partie.
 *   - Servir d'unique point de communication entre VueJeu et le modèle (Partie).
 *   - NE PAS dupliquer l'état du modèle : toujours lire depuis partie.*.
 */
public class Gameplay {

    private final VueJeu vue;
    private final IHMControler controler;
    public Partie partie;

    // -------------------------------------------------------------------------
    // États temporaires d'interaction — encapsulés dans des classes dédiées
    // -------------------------------------------------------------------------

    /** État d'une opération d'échange en attente de deux clics. */
    private EchangeState echangeState = null;

    /** État d'une opération de rotation en attente de clics. */
    private RotationState rotationState = null;

    /** Vrai si le mode rotation est actif (utilisé par IHMControler pour le curseur). */
    public boolean rotationMode = false;

    // -------------------------------------------------------------------------
    // Constructeur
    // -------------------------------------------------------------------------

    public Gameplay(VueJeu vue, Partie partie) {
        this.vue = vue;
        this.partie = partie;
        this.controler = new IHMControler(this);
        vue.registerControler(controler);
        // Listener pour mettre à jour les jetons Temps à droite
        partie.setTourChangeListener(() -> {
            SwingUtilities.invokeLater(() -> {
                if (partie.numeroTour >= 1 && partie.numeroTour <= 8) {
                    vue.switchTurnFace(partie.numeroTour - 2);
                }
            });
        });
        //Listener pour afficher dès que quelqu'un a gagné
        partie.setFinPartieListener(() -> {
            SwingUtilities.invokeLater(() -> {
                vue.showGameOverScreen(partie.getGagnant().getNom());
            });
        });
        // Listener pour l'affichage de la carte alibi piochée
        SwingUtilities.invokeLater(this::updatePlayerRectangles);
        partie.setAlibiListener(() -> {
            CarteAlibi carte = partie.derniereCarteAlibiPiochee;
            if (carte == null) return;
            SwingUtilities.invokeLater(() -> {
                vue.getMonde().afficherCarteAlibi(carte.getPersonnage());
            });
        });
        // Listener pour l'affichage de la carte alibi piochée
        partie.setAppelTemoinListener(() -> {
            SwingUtilities.invokeLater(() -> {
                vue.getMonde().showCenterMessage("Appel à témoin !");
            });
        });
        // Listener pour l'affichage de la carte alibi piochée
        partie.setTourEnqueteurListener(() -> {
            SwingUtilities.invokeLater(() -> {
                vue.getMonde().showCenterMessage("Tour de l'enquêteur");
            });
        });
        // Listener pour l'affichage de la carte alibi piochée
        partie.setTourJackListener(() -> {
            SwingUtilities.invokeLater(() -> {
                vue.getMonde().showCenterMessage("Tour de Jack");
            });
        });

        initGame();
        // On affiche ou pas l'identité de jack
        // On affiche que c'est le tour de l'enqueteur (c'est toujours le cas)

        afficherIdentiteJack(() -> {

            SwingUtilities.invokeLater(() -> {
                vue.getMonde().showCenterMessage("Tour de l'enquêteur");
            });

        });
    }

    private void afficherIdentiteJack(Runnable onFinished) {
        // n'affiche correctement l'identité de jack que si une seule carte alibi a été piochée

        // if partie solo and
        boolean jackHumain = (partie.niveauJack == -1);
        // humain vs humain → Jack humain → affichage
        // humain vs IA avec joueur = Jack → Jack humain → affichage
        // IA vs humain avec Jack IA → pas affiché
        // IA vs IA → pas affiché
        if (jackHumain) {
            System.out.println("Affichage de l'identité de Jack");
            partie.fireAlibiEvent();
            // on attend 2 secondes puis on continue
            new javax.swing.Timer(2000, e -> {
                onFinished.run();
            }) {{
                setRepeats(false);
                start();
            }};
        } else {
            System.out.println("Refus de l'affichage de l'identité de Jack");
            onFinished.run();
        }
    }

    private void initGame() {
        refreshView();
        vue.updateTurnIndicator(partie.numeroTour);
        // Injecte la sonde de survol dans IHMControler une fois VueMonde initialisé
        controler.setHoverProbe((sx, sy) -> vue.getMonde().actionBallAt(sx, sy));
        startTurn();
    }

    private void startTurn() {
        System.out.println("Gameplay — début du tour " + partie.numeroTour);
        // Le type de joueur (humain/IA) est déterminé par le modèle
        boolean isIa = (partie.joueurCourant == Joueur.JACK && partie.niveauJack != -1)
                    || (partie.joueurCourant == Joueur.ENQUETEUR && partie.niveauEnqueteur != -1);
        controler.setActivePlayerType(isIa ? "AI" : "HUMAN");
        vue.updateTurnIndicator(partie.numeroTour);

        if (isIa) {
            SwingUtilities.invokeLater(controler::joueIa);
        }
    }

    // -------------------------------------------------------------------------
    // Mise à jour de la vue — point unique
    // -------------------------------------------------------------------------

    /** Rafraîchit tous les composants visuels depuis l'état courant du modèle. */
    private void refreshView() {
        vue.updateBackground(partie.joueurCourant);
        vue.updateDistrictView(partie.district);
        vue.updateJetons(partie.jetonsAction);
        vue.updateDetectivesView(partie.detectives);
        vue.updateSabliers(partie.sabliersDeJack, 6);
    }

    // -------------------------------------------------------------------------
    // Cycle de jeu
    // -------------------------------------------------------------------------

    public void resetGame() {
        partie.reset();
        vue.hideGameOverScreen();
        resetAllTurnIndicators();
        refreshView();
        initGame();
        updatePlayerRectangles();
        afficherIdentiteJack(() -> {

            SwingUtilities.invokeLater(() -> {
                vue.getMonde().showCenterMessage("Tour de l'enquêteur");
            });

        });
    }

    public void resetAllTurnIndicators() {
        for (int i = 0; i < 8; i++) {
            if (!vue.isTurnFacePile(i)) vue.switchTurnFace(i);
        }
    }

    public IHMControler getControler() { return controler; }

    private void onGameOver() {
        System.out.println("Gameplay — fin de partie");
        vue.showGameOverScreen(partie.getGagnant().getNom());
    }

    // -------------------------------------------------------------------------
    // Actions des boules (jetons) — appelées par IHMControler
    // -------------------------------------------------------------------------

    public void onActionBallClicked(IHMControler.ActionIntent intent) {
        String actionName = intent.actionName();
        int ballIndex = intent.ballIndex();

        System.out.println("Gameplay — action sur boule " + ballIndex + " : " + actionName);

        // L'utilisateur a cliqué : on efface le survol
        vue.getMonde().setHoveredToken(-1);

        switch (actionName) {
            case "action_holmes"   -> demanderDeplacementEtDeplacer(Detective.Type.HOLMES);
            case "action_watson"   -> demanderDeplacementEtDeplacer(Detective.Type.WATSON);
            case "action_toby"     -> demanderDeplacementEtDeplacer(Detective.Type.TOBY);
            case "action_joker"    -> gererJoker();
            case "action_rotation" -> startRotationMode(ballIndex);
            case "action_echange"  -> startEchange();
            case "action_alibi"    -> {
                partie.actions.alibi();
                refreshView();
                vue.updateSabliers(partie.sabliersDeJack, 6);
            }
            default -> System.out.println("Action inconnue : " + actionName);
        }
    }

    private void demanderDeplacementEtDeplacer(Detective.Type type) {
        Detective detective = trouverDetective(type);
        if (detective == null) return;

        String[] options = {"1", "2", "Annuler"};

        int choix = JOptionPane.showOptionDialog(
                vue,
                "Déplacer " + type + " de combien de pas ?",
                "Déplacement",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        // Annuler ou fermeture de fenêtre
        if (choix == 2 || choix == JOptionPane.CLOSED_OPTION) {
            return;
        }

        int pas = (choix == 0) ? 1 : 2;

        partie.actions.deplacerDetective(detective, pas);
        refreshView();
    }

    private void gererJoker() {
        Joueur joueur = partie.joueurCourant;
        if (joueur == Joueur.ENQUETEUR) {
            String[] options = {"Holmes", "Watson", "Toby"};
            int choix = JOptionPane.showOptionDialog(vue,
                    "Quel détective voulez-vous déplacer d'un pas ?", "Action Joker",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
            if (choix < 0) return;
            Detective det = switch (choix) {
                case 0 -> trouverDetective(Detective.Type.HOLMES);
                case 1 -> trouverDetective(Detective.Type.WATSON);
                case 2 -> trouverDetective(Detective.Type.TOBY);
                default -> null;
            };
            if (det != null) {
                partie.actions.joker(det);
                refreshView();
            }
        } else {
            String[] options = {"Holmes", "Watson", "Toby", "Ne rien déplacer"};
            int choix = JOptionPane.showOptionDialog(vue,
                    "Choisissez une action (déplacer un détective d'un pas ou rien)",
                    "Action Joker - Mr. Jack",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
            if (choix < 0) return;
            Detective det = switch (choix) {
                case 0 -> trouverDetective(Detective.Type.HOLMES);
                case 1 -> trouverDetective(Detective.Type.WATSON);
                case 2 -> trouverDetective(Detective.Type.TOBY);
                default -> null;
            };
            partie.actions.joker(det); // null = ne rien faire, géré dans PartieActions
            refreshView();
        }
    }

    private Detective trouverDetective(Detective.Type type) {
        for (Detective d : partie.detectives) {
            if (d.getType() == type) return d;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Échange et Rotation — gestion propre via des objets d'état dédiés
    // -------------------------------------------------------------------------

    public void startEchange() {
        echangeState = new EchangeState();
        System.out.println("Action échange : cliquez sur la première tuile");
    }

    public void startRotationMode(int jetonIndex) {
        rotationState = new RotationState(jetonIndex);
        System.out.println("Mode rotation actif. Cliquez sur une tuile, recliquez pour accumuler, cliquez ailleurs pour confirmer.");
    }

    public void exitRotationMode() {
        if (rotationState != null && rotationState.hasTarget()) {
            partie.actions.rotationQuartier(
                rotationState.jetonIndex,
                rotationState.row,
                rotationState.col,
                rotationState.quarts
            );
            refreshView();
        }
        rotationState = null;
        rotationMode = false;
        System.out.println("Mode rotation terminé.");
    }

    public void clicHorsDistrict() {
        if (rotationMode) exitRotationMode();
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

    /**
     * Relaye le survol d'un jeton d'action à VueMonde pour le rendu du feedback.
     * Appelé depuis IHMControler à chaque événement mouseMoved/mouseExited.
     *
     * @param index Index 0-3 du jeton survol, ou -1 (aucun survol).
     */
    public void onTokenHovered(int index) {
        vue.getMonde().setHoveredToken(index);
    }

    public void onCellHovered(int row, int col) {
        if (echangeState != null) {
            if (!echangeState.hasFirstTile()) {
                echangeState.setFirstTile(row, col);
                System.out.println("Échange — première tuile : (" + row + "," + col + ")");
            } else if (!echangeState.isSameTile(row, col)) {
                System.out.println("Échange — deuxième tuile : (" + row + "," + col + ")");
                partie.actions.echange(echangeState.row, echangeState.col, row, col);
                echangeState = null;
                refreshView();
            }
        } else if (rotationState != null) {
            if (!rotationState.hasTarget()) {
                rotationState.setTarget(row, col);
                rotationMode = true;
                vue.rotateTile(row, col, 90);
                System.out.println("Rotation — tuile sélectionnée : (" + row + "," + col + ")");
            } else if (rotationState.isSameTile(row, col)) {
                rotationState.addQuart();
                vue.rotateTile(row, col, 90);
                System.out.println("Rotation — quart supplémentaire (" + rotationState.quarts + " total)");
            } else {
                exitRotationMode();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Classes d'état temporaire — encapsulées, cycle de vie limité à l'interaction
    // -------------------------------------------------------------------------

    /** Encapsule l'état d'une opération d'échange en cours (attente de 2 clics). */
    private static class EchangeState {
        int row = -1, col = -1;

        boolean hasFirstTile() { return row != -1; }

        void setFirstTile(int r, int c) { row = r; col = c; }

        boolean isSameTile(int r, int c) { return row == r && col == c; }
    }

    /** Encapsule l'état d'une opération de rotation en cours. */
    private static class RotationState {
        final int jetonIndex;
        int row = -1, col = -1;
        int quarts = 0;

        RotationState(int jetonIndex) { this.jetonIndex = jetonIndex; }

        boolean hasTarget() { return row != -1; }

        void setTarget(int r, int c) { row = r; col = c; quarts = 1; }

        boolean isSameTile(int r, int c) { return row == r && col == c; }

        void addQuart() { quarts = (quarts + 1) % 4; }
    }

    /**
     * Convertit un niveau d'IA en texte lisible pour l'affichage.
     * 
     * @param niveau -1 = Humain, 0 = IA Random, 1 = IA Facile, 2 = IA Moyenne
     * @return une chaîne comme "Humain", "IA Random", "IA Facile" ou "IA Moyenne"
     */
    private String getTypeTexte(int niveau) {
        if (niveau == -1) {
            return "Humain";
        }
        switch (niveau) {
            case 0: return "IA Facile";
            case 1: return "IA Moyenne";
            case 2: return "IA Abominable";
            default: return "IA";
        }
    }

    /**
     * Met à jour l'affichage des deux rectangles dans VueMonde.
     * 
     * Index 0 = Enquêteur (toujours en bleu)
     * Index 1 = Mr. Jack (toujours en rouge)
     * 
     * Le texte sous le nom (Humain / IA Random / IA Facile / IA Moyenne)
     * change en fonction de la configuration de la partie.
     */
    private void updatePlayerRectangles() {
        VueMonde monde = vue.getMonde();
        
        // ----- Enquêteur (index 0, toujours en bleu) -----
        String enqueteurNom = "Enqueteur";
        String enqueteurType = getTypeTexte(partie.niveauEnqueteur);
        monde.setPlayerName(0, enqueteurNom);
        monde.setPlayerType(0, enqueteurType);
        monde.updateRectColor(0, new Color(40, 80, 180, 200));  // Bleu personnalisé
        
        // ----- Mr. Jack (index 1, toujours en rouge) -----
        String jackNom = "Mr. Jack";
        String jackType = getTypeTexte(partie.niveauJack);
        monde.setPlayerName(1, jackNom);
        monde.setPlayerType(1, jackType);
        monde.updateRectColor(1, new Color(180, 40, 40, 200));  // Rouge personnalisé
    }

}