package src.utils;

/**
 * Une classe pour mettre des méthodes utilitaires
 **/

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class utils {

    public static BufferedImage loadImage(String name){
        try (InputStream in = new FileInputStream("res/Images/" + name + ".png")) {
            return ImageIO.read(in);
        } catch (IOException e) {
            System.err.println("Erreur chargement image " + name + " : " + e.getMessage());
            return null;
        }
    }
}
