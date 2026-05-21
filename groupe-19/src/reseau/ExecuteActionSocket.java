package src.reseau;

import src.modele.Detective;
import src.vue.ExecuteAction;
import src.vue.Gameplay;
import src.reseau.Client;

//TODO Mettre à jour dans Gameplay et client/serveur pour enlever gampelay
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
        client.clientDeplaceDetective(contenue);
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
