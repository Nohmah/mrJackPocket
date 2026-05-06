package src.modele;

import java.util.Arrays;
import java.util.Collections;

public class PartieInit {

    public static void initialiserJetons(Partie partie) {
        partie.jetonsAction.add(JetonAction.creerJeton(Action.ALIBI, Action.HOLMES));
        partie.jetonsAction.add(JetonAction.creerJeton(Action.TOBY, Action.WATSON));
        partie.jetonsAction.add(JetonAction.creerJeton(Action.ROTATION, Action.ECHANGE));
        partie.jetonsAction.add(JetonAction.creerJeton(Action.ROTATION, Action.JOKER));
    }

    public static void initialiserCartes(Partie partie) {
        partie.cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.NORA_NOIRE, 2));
        partie.cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.SGT_GOODLEY, 0));
        partie.cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.JEREMY_BERT, 1));
        partie.cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.WILLIAM_GULL, 1));
        partie.cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.MISS_STEALTHY, 1));
        partie.cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.JOHN_SMITH, 1));
        partie.cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.INSPECTEUR_LESTRADE, 0));
        partie.cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.JOHN_PIZER, 1));
        partie.cartesAlibiPioche.add(CarteAlibi.creerCarteAlibi(Personnage.JOSEPH_LANE, 1));
        Collections.shuffle(partie.cartesAlibiPioche);
    }

    public static void initialiserDetectives(Partie partie) {
        partie.detectives.add(Detective.creerDetective(Detective.Type.HOLMES, 11));
        partie.detectives.add(Detective.creerDetective(Detective.Type.WATSON, 3));
        partie.detectives.add(Detective.creerDetective(Detective.Type.TOBY, 7));
    }

    public static void initialiserSuspects(Partie partie) {
        partie.suspects.addAll(Arrays.asList(Personnage.values()));
    }
}