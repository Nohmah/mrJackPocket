package src.modele;

import java.io.Serializable;
import java.util.List;

public class GameSave implements Serializable {
    //Etat courant de la partie
    PartieSnapshot current;
    //Historique
    List<PartieSnapshot> undo;
    List<PartieSnapshot> redo;
}
