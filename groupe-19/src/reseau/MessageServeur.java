package src.reseau;

import java.io.Serializable;

public class MessageServeur implements Serializable {
    private CommunicationLobbySC codeServeur = null;
    private CommunicationLobbyCS codeClient = null;
    private final String pseudo;
    private final Object contenue;

    public MessageServeur(CommunicationLobbySC codeServeur, String pseudo, Object contenue) {
        this.codeServeur = codeServeur;
        this.pseudo = pseudo;
        this.contenue = contenue;
    }

    public MessageServeur(CommunicationLobbyCS codeClient, String pseudo, Object contenue) {
        this.codeClient = codeClient;
        this.pseudo = pseudo;
        this.contenue = contenue;
    }


    public CommunicationLobbySC getCodeServeur() {
        return codeServeur;
    }
    public CommunicationLobbyCS getCodeClient() { return codeClient; }
    public String getPseudo() {
        return pseudo;
    }
    public Object getContenue() {
        return contenue;
    }

}
