package src.utils;

import src.modele.GameSave;

import java.io.*;

public class SaveManager {
    public static void save(GameSave save, String chemin) throws IOException {
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(chemin))){
            oos.writeObject(save);
        }
    }

    public static GameSave load(String chemin)throws IOException, ClassNotFoundException{
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(chemin))){
            return (GameSave) ois.readObject();
        }
    }
}
