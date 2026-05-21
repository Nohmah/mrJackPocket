package src.vue;

import src.modele.Detective;
import src.modele.Partie;

public interface ExecuteAction {
    void executePiocheAlibi(Gameplay gameplay);
    void executeJoker(Gameplay gameplay, Detective detective);
    void executeDetective(Gameplay gameplay, Detective detective, int pas);
    void executeRotationQuartier(Gameplay gameplay, int jetonIndex,int row, int col, int quarts);
    void executeEchangeQuartier(Gameplay gameplay, int rowPremierChoix, int colPremierChoix, int row, int col);
}



