package src.vue;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

// Cette classe est statique, attention
public class Camera extends JComponent {
    public static JFrame frame;

    public static Vector2 position;   // Centre de la caméra (espace monde)
    public static Vector2 positionHG; // Coin Haut-Gauche de la caméra (espace monde)
    public static Vector2 taille;     // Taille en pixels de la fenêtre
    public static Vector2 zoom;       // Facteur de zoom (1,1 = normal)
    public static Vector2 tailleRel;  // Portion du monde visible (taille / zoom)

    public static List<List<Composant2D>> composants;
    public static List<String> spriteNames;
    public static List<Image> sprites;
    public static int nbreSprites;

    //Pour les calculs de temps entre chaque redessinement, utilisés dans paintComponent
    //Cela servira à avoir des animations consistantes
    //Imprécisions car on calcule à la miliseconde, mais ça devrait le faire ?
    static long debut;
    static long fin;
    public static double delta;

    public int testCounter;


    // -------------------------------------------------------------------------
    // Déplacements et zoom
    // -------------------------------------------------------------------------

    public static void MoveCamera(Vector2 move)
    {
        Camera.position = Camera.position.Add(move);
        RecalculateZoom();
    }

    public static void AddZoomCamera(double z)
    {
        Camera.zoom = Camera.zoom.Mult(z);
        RecalculateZoom();
    }

    public static void SetZoomCamera(Vector2 zoom_)
    {
        Camera.zoom = zoom_;
        RecalculateZoom();
    }

    // T
    // RecalculateZoom : recalcule les grandeurs dérivées du zoom ET de la position.
    //
    // tailleRel  = combien d'unités monde tiennent dans la fenêtre
    //              → plus le zoom est grand, moins on voit de monde
    //              → plus le zoom est petit, plus on voit de monde
    //
    // positionHG = coin haut-gauche de ce qu'on voit
    //              = centre caméra - moitié de la zone visible
    //
    // Enfin on demande à Swing de redessiner la fenêtre.
    public static void RecalculateZoom()  // RENDU PUBLIC
    {
        Camera.tailleRel  = Camera.taille.Div(Camera.zoom);
        Camera.positionHG = Camera.position.Sub(Camera.tailleRel.Div(2));
        Camera.Repaint();
    }
    // T


    public static void Repaint()  // RENDU PUBLIC
    {
        if (frame != null) frame.repaint();
    }


    // -------------------------------------------------------------------------
    // Rendu
    // -------------------------------------------------------------------------

    @Override
    public void paintComponent(Graphics g)
    {
        System.out.println("Entree dans paintComponent : " + testCounter++);

        Graphics2D drawable = (Graphics2D) g;
        int width  = getSize().width;
        int height = getSize().height;
        drawable.clearRect(0, 0, width, height);

        Camera.fin = System.currentTimeMillis();
        Camera.delta = Math.abs((Camera.fin - Camera.debut) / 1e9);

        // CORRECTION : Utiliser composants.size() au lieu de nbreSprites
        // car nbreSprites compte le nombre d'images différentes chargées,
        // alors que composants.size() compte le nombre d'objets à dessiner.
        for (int l = 0; l < composants.size(); l++)
        {
            List<Composant2D> compList = composants.get(l);
            for (int i = 0; i < compList.size(); i++)
            {
                Composant2D comp = compList.get(i);
                if (comp instanceof Animation2D animation2D)
                {
                    animation2D.AddDelta(delta);
                }
                
                // Vérification supplémentaire : s'assurer que le spriteId est valide
                if (comp.spriteId < 0 || comp.spriteId >= sprites.size()) {
                    System.err.println("Sprite invalide pour composant : " + comp.spriteId);
                    continue; // Ignorer ce composant
                }

                // Coin haut-gauche du composant dans l'espace monde
                Vector2 worldHG = comp.CoinHG();

                // Conversion monde → écran
                Vector2 screenPos   = worldHG.Sub(Camera.positionHG).Mult(Camera.zoom);
                Vector2 screenTaille = comp.TailleRel().Mult(Camera.zoom);

                if (InCamera(screenPos, screenTaille))
                {
                    drawable.drawImage(
                        sprites.get(comp.spriteId),
                        (int) screenPos.x,    (int) screenPos.y,
                        (int) screenTaille.x, (int) screenTaille.y,
                        null
                    );
                }
            }
        }
        Camera.debut = System.currentTimeMillis();
    }


    // T
    // CORRECTION de InCamera :
    //
    // On veut savoir si le rectangle du composant (coin HG + taille) est
    // au moins partiellement visible dans la fenêtre (0,0) → (taille.x, taille.y).
    //
    // Un rectangle A est HORS champ si :
    //   - son bord droit  est à gauche de 0              (pos.x + tail.x < 0)
    //   - son bord gauche est à droite de la fenêtre     (pos.x > taille.x)
    //   - son bord bas    est au-dessus de 0             (pos.y + tail.y < 0)
    //   - son bord haut   est en-dessous de la fenêtre   (pos.y > taille.y)
    //
    // On retourne vrai si AUCUNE de ces conditions n'est remplie.
    // Ici pos et tail sont déjà en coordonnées écran (pixels).
    private static boolean InCamera(Vector2 pos, Vector2 tail)
    {
        boolean horsX = (pos.x + tail.x < 0) || (pos.x > Camera.taille.x);
        boolean horsY = (pos.y + tail.y < 0) || (pos.y > Camera.taille.y);
        boolean visible = !horsX && !horsY;
        //System.out.println("In camera : " + visible);
        return visible;
    }
    // T


    // -------------------------------------------------------------------------
    // Resize de la fenêtre
    // -------------------------------------------------------------------------

    // T
    // NOUVEAU : OnResize
    //
    // Quand l'utilisateur redimensionne la fenêtre, Swing appelle
    // componentResized(). On met à jour Camera.taille avec les nouvelles
    // dimensions, puis on recalcule tailleRel et positionHG.
    // Sans ça, la caméra continuerait à utiliser l'ancienne taille et
    // les composants seraient mal cadrés.
    //
    // On utilise un ComponentListener (interface Swing) plutôt qu'un
    // override de reshape() car c'est l'approche moderne recommandée.
    private void InitResizeListener()
    {
        frame.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                Dimension d = frame.getContentPane().getSize();
                Camera.taille = new Vector2(d.width, d.height);
                RecalculateZoom();
                System.out.println("Fenetre redimensionnee : " + Camera.taille.ToString());
            }
        });
    }
    // T


    // -------------------------------------------------------------------------
    // Debug
    // -------------------------------------------------------------------------

    private static void DebugInfo(Vector2 vec)
    {
        System.out.println("Vecteur : "     + vec.ToString());
        System.out.println("Position : "    + Camera.position.ToString());
        System.out.println("PositionHG : "  + Camera.positionHG.ToString());
        System.out.println("Taille : "      + Camera.taille.ToString());
        System.out.println("Zoom : "        + Camera.zoom.ToString());
        System.out.println("TailleRel : "   + Camera.tailleRel.ToString());
    }


    // -------------------------------------------------------------------------
    // Constructeur et initialisation
    // -------------------------------------------------------------------------

    public Camera(Vector2 taille_, JFrame frame_)
    {
        Camera.frame   = frame_;
        Camera.taille  = taille_;
        Camera.position = Camera.taille.Div(2);
        Camera.zoom    = new Vector2(1, 1);
        Camera.nbreSprites = 0;
        Camera.RecalculateZoom();
        InitLists();
        InitResizeListener(); // T — branchement du listener de resize
        testCounter = 0;
        Test();
    }

    private void InitLists()
    {
        composants  = new ArrayList<>();
        for (int i = 0; i < 8; i++)
        {
            composants.add(new ArrayList<>());
        }
        spriteNames = new ArrayList<>();
        sprites     = new ArrayList<>();
    }

    public static void Test()
    {
        new Composant2D(new Vector2(500, 250), new Vector2(40, 40), "Pousseur");
    }


    // -------------------------------------------------------------------------
    // API publique : ajout de composants et sprites
    // -------------------------------------------------------------------------

    // Vous ne devriez pas avoir à utiliser ces fonctions
    // Elles servent à ajouter des composants à la liste de dessinage
    public static void AddComposant(Composant2D comp)
    {
        Camera.composants.get(comp.couche).add(comp);
    }

    /**
     * Retourne l'index d'un sprite déjà chargé, ou -1 s'il n'existe pas.
     * Utilisé par VueJeu pour vérifier si un sprite tourné est déjà en cache.
     */
    public static int GetSpriteId(String name)
    {
        return Camera.spriteNames.indexOf(name);
    }

    /**
     * Retourne l'Image associée à un sprite déjà chargé, ou null s'il est absent.
     * Utilisé par VueJeu pour récupérer l'image originale avant rotation.
     */
    public static java.awt.image.BufferedImage GetSpriteImage(String name)
    {
        int index = Camera.spriteNames.indexOf(name);
        if (index < 0) return null;
        Image img = Camera.sprites.get(index);
        if (img instanceof java.awt.image.BufferedImage) {
            return (java.awt.image.BufferedImage) img;
        }
        // Convertit en BufferedImage si nécessaire
        java.awt.image.BufferedImage bimg = new java.awt.image.BufferedImage(
            img.getWidth(null), img.getHeight(null), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = bimg.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return bimg;
    }

    /**
     * Variante d'AddSprite qui accepte directement une BufferedImage déjà construite.
     * Utilisée pour enregistrer les sprites tournés générés par VueJeu.
     *
     * @param name  Clé unique du sprite (ex: "NoraNoire_90").
     * @param image Image déjà construite à stocker.
     * @return      Index du sprite dans la liste (nouveau ou existant).
     */
    public static int AddSprite(String name, java.awt.image.BufferedImage image)
    {
        int index = Camera.spriteNames.indexOf(name);
        if (index > -1) return index;

        Camera.sprites.add(image);
        Camera.spriteNames.add(name);
        index = Camera.nbreSprites;
        Camera.nbreSprites++;
        return index;
    }

    public static int AddSprite(String name)
    {
        int index = Camera.spriteNames.indexOf(name);
        if (index > -1) return index;

        Image img;
        try {
            InputStream in = new FileInputStream("res/Images/" + name + ".png");
            img = ImageIO.read(in);
        } catch (FileNotFoundException e) {
            System.err.println("ERREUR : impossible de trouver le fichier : " + name);
            return -1;
        } catch (IOException e) {
            System.err.println("ERREUR : impossible de charger l'image : " + name);
            return -2;
        }

        Camera.sprites.add(img);
        Camera.spriteNames.add(name);
        index = Camera.nbreSprites;
        Camera.nbreSprites++;
        return index;
    }

    public static List<Integer> AddSpriteList
    (List<String> nameList, List<java.awt.image.BufferedImage> image)
    {
        List<Integer> intList = new ArrayList<>();
        for (int i = 0; i < nameList.size(); i++)
        {
            int index = Camera.spriteNames.indexOf(nameList.get(i));
            if (index > -1) intList.add(index);

            Camera.sprites.add(image.get(i));
            Camera.spriteNames.add(nameList.get(i));
            index = Camera.nbreSprites;
            Camera.nbreSprites++;
            intList.add(index);
        }
        return intList;
    }

    public static List<Integer> AddSpriteList(List<String> nameList)
    {
        List<Integer> intList = new ArrayList<>();
        for (int i = 0; i < nameList.size(); i++)
        {
            String name = nameList.get(i);
            int index = Camera.spriteNames.indexOf(name);
            if (index > -1)
            {
                intList.add(index);
                break;
            }

            Image img;
            try {
                InputStream in = new FileInputStream("Images/" + name + ".png");
                img = ImageIO.read(in);
            } catch (FileNotFoundException e) {
                System.err.println("ERREUR : impossible de trouver le fichier : " + name);
                return intList;
            } catch (IOException e) {
                System.err.println("ERREUR : impossible de charger l'image : " + name);
                return intList;
            }

            Camera.sprites.add(img);
            Camera.spriteNames.add(name);
            index = Camera.nbreSprites;
            Camera.nbreSprites++;
            intList.add(index);
        }
        return intList;
    }
}