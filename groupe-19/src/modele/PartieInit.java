package src.modele;

import java.util.Arrays;
import java.util.Collections;

/**
 * Contient les méthodes pour initialiser une partie
 **/

public class PartieInit {

    /** Crée les 4 jetons et les ajoute dans la liste des jetons actions [jetonsAction] **/
    public static void initialiserJetons(Partie partie) {
        partie.jetonsAction.add(JetonAction.creerJetonAction(Action.ALIBI, Action.HOLMES));
        partie.jetonsAction.add(JetonAction.creerJetonAction(Action.TOBY, Action.WATSON));
        partie.jetonsAction.add(JetonAction.creerJetonAction(Action.ROTATION, Action.ECHANGE));
        partie.jetonsAction.add(JetonAction.creerJetonAction(Action.ROTATION, Action.JOKER));
    }

    /** Crée les 9 cartes alibis et les ajoute dans la liste des cartes Alibis [cartesAlibiPioche] **/
    public static void initialiserCartesAlibis(Partie partie) {
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

    /** Crée les 3 détectives et les ajoute dans la liste des détectives [detectives] **/
    public static void initialiserDetectives(Partie partie) {
        partie.detectives.add(Detective.creerDetective(Detective.Type.HOLMES, 11));
        partie.detectives.add(Detective.creerDetective(Detective.Type.WATSON, 3));
        partie.detectives.add(Detective.creerDetective(Detective.Type.TOBY, 7));
    }

    /** Remplis la liste des suspects [suspects] avec les 9 personnages **/
    public static void initialiserSuspects(Partie partie) {
        partie.suspects.addAll(Arrays.asList(Personnage.values()));
    }

    /** Définit quel personnage est Mr. Jack et l'affecte à [identiteJack] **/
    public static void initialiserIdentiteJack(Partie partie){
        partie.identiteJack = partie.actions.piocherCarteAlibi().getPersonnage();
        System.out.println("Mr Jack est "+ partie.identiteJack.nom + " couleur : " + partie.identiteJack.couleur);
    }
}