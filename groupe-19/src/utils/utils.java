package src.utils;

/**
 * Une classe pour mettre des méthodes utilitaires
 **/

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class utils {

    /** Charge et renvoie une image **/
    public static BufferedImage loadImage(String name){
        try (InputStream in = new FileInputStream("res/Images/" + name + ".png")) {
            return ImageIO.read(in);
        } catch (IOException e) {
            System.err.println("Erreur chargement image " + name + " : " + e.getMessage());
            return null;
        }
    }

    /** Calcule les dimensions maximales d'une image pour qu'elle tienne entièrement dans un espace donné
     * en ayant conservé son ratio (son format) **/
    public static Dimension fitToRatio(int l, int h, double imageRatio) {
        double EspaceLibreRatio = (double) l / h;
        int fl, fh;
        if (imageRatio > EspaceLibreRatio) {
            // L'image est plus "large" que l'espace libre : on impose la largeur
            fl = l;
            fh = (int) (l / imageRatio);
        } else {
            // L'image est plus "haute" que l'espace libre : on impose la hauteur
            fh = h;
            fl = (int) (h * imageRatio);
        }
        return new Dimension(fl, fh);
    }

    public static String lirePseudoSauvegarde() {
        File fichier = new File("pseudo.txt");

        if (fichier.exists()) {
            try (Scanner sc = new Scanner(fichier)) {
                if (sc.hasNextLine()) {
                    String ligne = sc.nextLine().trim();
                    return ligne.split("\\s")[0]; // jusqu'au premier espace
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

}
