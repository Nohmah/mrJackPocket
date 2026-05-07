package src.vue;

public class Composant2D
{
    public Vector2 position;  // Centre du composant dans l'espace monde
    public Vector2 taille;    // Taille de base (en pixels, avant échelle)
    public Vector2 echelle;   // Facteur d'agrandissement/réduction

    public boolean visible;
    public boolean immobile; //N'est pas affecté par la caméra
    public int spriteId;
    public int couche; // 0 =< x =< 7, plus petit = en dessous

    // T
    // CORRECTION de CoinHG() :
    // Le coin Haut-Gauche se calcule en partant du CENTRE (position) et en
    // reculant de la moitié de la taille MISE À L'ÉCHELLE.
    // Avant, l'échelle était appliquée sur toute la position, ce qui déplaçait
    // le composant au lieu de juste le redimensionner.
    //
    // Formule :
    //   coinHG.x = position.x - (taille.x * echelle.x) / 2
    //   coinHG.y = position.y - (taille.y * echelle.y) / 2
    public Vector2 CoinHG()
    {
        return new Vector2(
            position.x - (taille.x * echelle.x) / 2.0,
            position.y - (taille.y * echelle.y) / 2.0
        );
    }
    // T

    // Retourne la taille réelle du composant (taille de base × échelle)
    public Vector2 TailleRel()
    {
        return taille.Mult(echelle);
    }

    public Composant2D(Vector2 pos, Vector2 tail, String spriteName)
    {
        this.position = pos;
        this.taille = tail;
        this.echelle = new Vector2(1, 1);
        this.spriteId = Camera.AddSprite(spriteName);
        this.couche = 0;
        this.visible = true;
        this.immobile = false;
        Camera.AddComposant(this);
    }

    public Composant2D(Vector2 pos, Vector2 tail, String spriteName, int cou)
    {
        this.position = pos;
        this.taille = tail;
        this.echelle = new Vector2(1, 1);
        this.spriteId = Camera.AddSprite(spriteName);
        this.couche = cou;
        this.visible = true;
        this.immobile = false;
        Camera.AddComposant(this);
    }

    public Composant2D() {
        //Nécessaire pour que Animation2D arrête de se plaindre de ses constructeurs
    }
}