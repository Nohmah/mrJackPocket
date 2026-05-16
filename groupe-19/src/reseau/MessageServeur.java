package src.reseau;

import java.io.Serializable;

public class MessageServeur implements Serializable {
    private CommunicationLobbySC codeServeur = null;
    private CommunicationLobbyCS codeClient = null;
    private final String pseudo;
    private final String message;

    public MessageServeur(CommunicationLobbySC codeServeur, String pseudo, String message) {
        this.codeServeur = codeServeur;
        this.pseudo = pseudo;
        this.message = message;
    }

    public MessageServeur(CommunicationLobbyCS codeClient, String pseudo, String message) {
        this.codeClient = codeClient;
        this.pseudo = pseudo;
        this.message = message;
    }

    public CommunicationLobbySC getCodeServeur() {
        return codeServeur;
    }
    public CommunicationLobbyCS getCodeClient() { return codeClient; }
    public String getPseudo() {
        return pseudo;
    }

    public String getMessage() {
        return message;
    }

}
