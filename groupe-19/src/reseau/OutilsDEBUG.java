package src.reseau;

import src.modele.Partie;
import src.modele.PartieSnapshot;

public class OutilsDEBUG {
    private void afficherServeurAlibiDEBUG(PartieSnapshot partieServeur){
        int i = 1;
        System.out.println("###############################################");
        System.out.println("[DEBUG] [SERVEUR] Verification Jack Serveur !");
        System.out.println("[DEBUG] [SERVEUR] Jack est "+ partieServeur.identiteJack.nom + " couleur : "+ partieServeur.identiteJack.couleur.toString());
        System.out.println("###############################################");
        /*
        for(CarteAlibi ca : partieServeur.cartesAlibiRestantes){
            System.out.println("[DEBUG] Carte alibi n°"+i+++" : "+ca.getPersonnage().couleur.toString());
        }

         */
        System.out.println("###############################################");
        System.out.println();
    }
    private void afficherClientAlibiDEBUG(PartieSnapshot partieServeur){
        int i = 1;
        System.out.println("###############################################");
        System.out.println("[DEBUG] [CLIENT] Verification Jack Serveur !");
        System.out.println("[DEBUG] [CLIENT] Jack est "+ partieServeur.identiteJack.nom + " couleur : "+ partieServeur.identiteJack.couleur.toString());
        System.out.println("###############################################");

        /*
        for(CarteAlibi ca : partieServeur.cartesAlibiRestantes){
            System.out.println("[DEBUG] Carte alibi n°"+i+++" : "+ca.getPersonnage().couleur.toString());
        }

         */
        System.out.println("###############################################");
        System.out.println();

    }

    private void afficherAlibiDEBUG(Partie partieClient, String message){
        int i = 1;
        System.out.println("###############################################");
        System.out.println(message);
        System.out.println("[DEBUG] [CLIENT] Jack est "+ partieClient.identiteJack.nom + " couleur : "+ partieClient.identiteJack.couleur.toString());
        System.out.println("###############################################");
        /*
        for(CarteAlibi ca : partieClient.cartesAlibiPioche){
            System.out.println("[DEBUG] Carte alibi n°"+i+++" : "+ca.getPersonnage().couleur.toString());
        }

         */
        System.out.println("###############################################");
        System.out.println();
    }
    /* utilisation dans la méthode receptionRequeteLANCEMENT_PARTIE de la Class Client
    afficherAlibiDEBUG(partieClient, "[DEBUG] [CLIENT] identité Jack AVANT mise à jour du serveur !");
    afficherAlibiDEBUG(versionServeur);
    afficherAlibiDEBUG(partieClient, "[DEBUG] [CLIENT] identité Jack APRES mise à jour du serveur !");
    */
}
