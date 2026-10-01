package src.vue;

public class Animation2D extends Composant2D
{
    AnimationContainer animation;
    public boolean doAnimation;


    public void AddDelta(double delta)
    {
        if (doAnimation) this.spriteId = this.animation.AddDelta(delta);
    }

    public void NextFrame()
    {
        this.spriteId = animation.NextFrame();
    }



    public AnimationContainer CopierAnimation()
    {
        return this.animation.Copier();
    }



    //Mes noms de variables oscillent entre anglais et français, c'est instinctif désolé
    public Animation2D(Vector2 pos, Vector2 tail, AnimationContainer ani)
    {
        this.position = pos;
        this.taille = tail;
        this.echelle = new Vector2(1, 1);
        this.animation = ani;
        this.spriteId = ani.spriteList.get(0);
        this.couche = 0;
        Camera.AddComposant(this);
    }

    public Animation2D(Vector2 pos, Vector2 tail, AnimationContainer ani, int cou)
    {
        this.position = pos;
        this.taille = tail;
        this.echelle = new Vector2(1, 1);
        this.animation = ani;
        this.spriteId = ani.spriteList.get(0);
        this.couche = cou;
        Camera.AddComposant(this);
    }
}