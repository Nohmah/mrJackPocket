package src.vue;

import src.modele.Detective;

public class ExecuteActionLocal implements ExecuteAction {

    @Override
    public void executePiocheAlibi(Gameplay gameplay) {
        if (gameplay.partie.isFreeze()) return;
        gameplay.partie.actions.alibi();
        gameplay.refreshView();
    }

    @Override
    public void executeJoker(Gameplay gameplay, Detective detective) {
        if (gameplay.partie.isFreeze()) return;
        gameplay.partie.actions.joker(detective);
        gameplay.refreshView();
    }

    @Override
    public void executeDetective(Gameplay gameplay, Detective detective, int pas) {
        if (gameplay.partie.isFreeze()) return;
        gameplay.partie.actions.deplacerDetective(detective, pas);
        gameplay.refreshView();
    }

    @Override
    public void executeRotationQuartier(Gameplay gameplay, int jetonIndex, int row, int col, int quarts) {
        if (gameplay.partie.isFreeze()) return;
        gameplay.partie.actions.rotationQuartier(jetonIndex, row, col, quarts);
        gameplay.refreshView();
    }

    @Override
    public void executeEchangeQuartier(Gameplay gameplay, int rowPremierChoix, int colPremierChoix, int row, int col) {
        if (gameplay.partie.isFreeze()) return;
        gameplay.partie.actions.echange(rowPremierChoix, colPremierChoix, row, col);
        gameplay.refreshView();
    }

    @Override
    public void executeQuitter(){}
}