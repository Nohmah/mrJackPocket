package src.vue;

public class Vector2
{
    public double x, y;

    // T
    // CORRECTION : 'norme' est maintenant calculée automatiquement à la
    // construction et mise à jour à chaque opération qui crée un nouveau
    // vecteur. Avant, elle valait toujours 0.0 sauf si on appelait
    // manuellement CalculerNorme(), ce qui rendait Normaliser() bogué.
    //
    // On garde CalculerNorme() publique pour les cas où l'on modifie
    // x ou y directement (accès public aux champs).
    public double norme;

    public void CalculerNorme()
    {
        norme = Math.sqrt(x * x + y * y);
    }
    // T

    // --- Constructeurs ---

    public Vector2(double x_, double y_)
    {
        x = x_;
        y = y_;
        // T
        CalculerNorme(); // On calcule la norme dès la création
        // T
    }

    public Vector2(float x_, float y_)
    {
        x = (double) x_;
        y = (double) y_;
        CalculerNorme();
    }

    public Vector2(int x_, int y_)
    {
        x = (double) x_;
        y = (double) y_;
        CalculerNorme();
    }


    // --- Opérations géométriques ---

    public Vector2 Normaliser()
    {
        // T
        // GUARD : si la norme est nulle (vecteur zéro), on retourne (0,0)
        // pour éviter une division par zéro.
        if (norme == 0.0) return new Vector2(0.0, 0.0);
        // T
        return new Vector2(x / norme, y / norme);
    }

    public double DistanceVers(Vector2 dest)
    {
        return Math.sqrt(Math.pow(dest.x - this.x, 2) + Math.pow(dest.y - this.y, 2));
    }

    public Vector2 Copier()
    {
        return new Vector2(x, y);
    }

    public String ToString()
    {
        return "(" + this.x + ", " + this.y + ")";
    }


    // --- Arithmétique ---

    public Vector2 Add(Vector2 op)
    {
        return new Vector2(this.x + op.x, this.y + op.y);
    }

    public Vector2 Sub(Vector2 op)
    {
        return new Vector2(this.x - op.x, this.y - op.y);
    }

    public Vector2 Mult(int op)
    {
        return new Vector2(this.x * op, this.y * op);
    }

    public Vector2 Mult(float op)
    {
        return new Vector2(this.x * op, this.y * op);
    }

    public Vector2 Mult(double op)
    {
        return new Vector2(this.x * op, this.y * op);
    }

    public Vector2 Mult(Vector2 mult)
    {
        return new Vector2(this.x * mult.x, this.y * mult.y);
    }

    public Vector2 Div(int op)
    {
        return new Vector2(this.x / op, this.y / op);
    }

    public Vector2 Div(float op)
    {
        return new Vector2(this.x / op, this.y / op);
    }

    public Vector2 Div(double op)
    {
        return new Vector2(this.x / op, this.y / op);
    }

    public Vector2 Div(Vector2 mult)
    {
        return new Vector2(this.x / mult.x, this.y / mult.y);
    }

    public boolean Equals(Vector2 vec)
    {
        return vec.x == this.x && vec.y == this.y;
    }
}