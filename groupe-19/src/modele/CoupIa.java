package src.modele;

public class CoupIa {
    Action action;
    int para1;
    int para2;

    //si action = holmes, watson ou toby : para1 = deplacement, para2 = veut rien dire
    //si action = joker : para1 = choix du detective (0,1,2), para2 = 0(seulement pour jack) ou 1 nb deplacement
    //si action = rotation : para1 = quartier (0-8), para2 = nombre de quart de tour (0-3)
    //si action = echange : para1 = quartier 1 (0-8), para2 = quartier 2 (0-8)
    //si action = les para veulent rien dire

    public CoupIa(Action action, int para1, int para2){
        this.action = action;
        this.para1 = para1;
        this.para2 = para2;
    }

    public CoupIa(Action action){
        this.action = action;
        this.para1 = 0;
        this.para2 = 0;
    }

    public void afficher(){
        System.out.println("Action : " + action + " Para1 : " + para1 + " Para2 : " + para2);
    }

    public int para1Max(Action action){
        switch (action){
            case HOLMES:
                return 1;
            case WATSON:
                return 1;
            case TOBY:
                return 1;
            case JOKER:
                return 2;
            case ROTATION:
                return 9;
            case ECHANGE:
                return 9;
            case ALIBI:
                return 1;
        }
        return 0;
    }

    public int para2Max(Action action){
        switch (action){
            case HOLMES:
                return 0;
            case WATSON:
                return 0;
            case TOBY:
                return 0;
            case JOKER:
                return 1;
            case ROTATION:
                return 3;
            case ECHANGE:
                return 9;
            case ALIBI:
                return 0;
        }
        return 0;
    }
}
