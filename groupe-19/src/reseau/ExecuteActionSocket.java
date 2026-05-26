package src.reseau;

import src.modele.Detective;
import src.vue.ExecuteAction;
import src.vue.Gameplay;

import static src.modele.Detective.Type.HOLMES;
import static src.modele.Detective.Type.WATSON;

public class ExecuteActionSocket implements ExecuteAction {
    private Client client;

    public ExecuteActionSocket(Client client) {
        this.client = client;
    }

    @Override
    public void executePiocheAlibi(Gameplay gameplay) {
        client.clientPiocheAlibi(null);
    }

    @Override
    public void executeJoker(Gameplay gameplay, Detective detective) {
        Object[] contenue = new Object[1];
        if(detective != null) {
            contenue[0] = detective.getType();
        }else{
            contenue[0] = null;
        }
        client.clientJoueJoker(contenue);
    }

    @Override
    public void executeDetective(Gameplay gameplay, Detective detective, int pas) {
        Object[] contenue = new Object[2];
        contenue[0] = detective.getType();
        contenue[1] = pas;
        if(contenue[0] == HOLMES) {
            client.clientDeplaceHolmes(contenue);
        }else if(contenue[0] == WATSON){
            client.clientDeplaceWatson(contenue);
        }else{
            client.clientDeplaceToby(contenue);
        }
    }

    @Override
    public void executeRotationQuartier(Gameplay gameplay, int jetonIndex, int row, int col, int quarts) {
        Object[] contenue = new Object[4];
        contenue[0] = jetonIndex;
        contenue[1] = row;
        contenue[2] = col;
        contenue[3] = quarts;
        client.clientTourneQuartier(contenue);
    }

    @Override
    public void executeEchangeQuartier(Gameplay gameplay, int rowPremierChoix, int colPremierChoix, int row, int col) {
        Object[] contenue = new Object[4];
        contenue[0] = rowPremierChoix;
        contenue[1] = colPremierChoix;
        contenue[2] = row;
        contenue[3] = col;
        client.clientEchangeQuartier(contenue);
    }
}
