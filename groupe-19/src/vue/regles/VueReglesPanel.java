package src.vue.regles;

public class VueReglesPanel {

    public static String getHtml() {
        return "<html>"
                + "<body style='color:white; font-family:SansSerif;'>"

                + "<h1 style='color:red; text-align:center; margin-bottom:6px;'>"
                + "&nbsp; Regles de Mr. Jack Pocket &nbsp;</h1>"
                + "<hr style='border:1px solid red; margin-bottom:14px;'/>"

                + "<h2 style='color:red; margin-bottom:4px;'>But du Jeu</h2>"
                + "<ul>"
                + "<li><b>Victoire de l'Enqueteur :</b> Un seul suspect reste en jeu.</li>"
                + "<li><b>Victoire de Jack :</b> Il possede 6 sabliers "
                + "<i>ou</i> n'a pas ete capture apres 8 tours.</li>"
                + "</ul>"

                + "<h2 style='color:red; margin-top:10px; margin-bottom:4px;'>"
                + "Deroulement d'un Tour</h2>"
                + "<p>Chaque tour se compose de deux etapes : "
                + "<b>La Traque</b> et <b>l'Appel a Temoins</b>.</p>"

                + "<h3 style='color:#FFD700; margin-bottom:2px;'>1. La Traque (Actions)</h3>"
                + "<p>L'ordre de jeu change selon la parite du tour :</p>"
                + "<ul>"
                + "<li><b>Tours Impairs (1, 3, 5, 7) :</b> L'Enqueteur choisit 1 action, "
                + "Jack en choisit 2, l'Enqueteur joue la derniere.</li>"
                + "<li><b>Tours Pairs (2, 4, 6, 8) :</b> Jack choisit 1 action, "
                + "l'Enqueteur en choisit 2, Jack joue la derniere.</li>"
                + "</ul>"
                + "<p><b>Detail des actions disponibles :</b></p>"
                + "<ul>"
                + "<li><b> Deplacement :</b> Avancer Holmes, Watson ou Toby "
                + "de 1 ou 2 cases <i>(sens horaire)</i>.</li>"
                + "<li><b> echange :</b> echanger deux tuiles de place "
                + "sans changer leur orientation.</li>"
                + "<li><b> Rotation :</b> Faire pivoter une tuile "
                + "(90 ou 180). <i>Une seule fois par tour.</i></li>"
                + "<li><b> Joker :</b> Deplacer l'enqueteur de son choix de 0 "
                + "<i>(Jack seulement)</i> ou 1 case.</li>"
                + "<li><b> Alibi :</b> Piocher une carte Alibi. "
                + "L'Enqueteur innocente le personnage ; Jack gagne des sabliers.</li>"
                + "</ul>"

                + "<h3 style='color:#FFD700; margin-top:8px; margin-bottom:2px;'>"
                + "2. L'Appel a Temoins</h3>"
                + "<p>Jack annonce s'il est <b>visible</b> "
                + "(ligne de mire d'un enqueteur sans mur) :</p>"
                + "<ul>"
                + "<li><b>Jack visible :</b> On elimine les suspects invisibles. "
                + "L'Enqueteur prend le jeton Temps.</li>"
                + "<li><b>Jack invisible :</b> On elimine les suspects visibles. "
                + "Jack prend le jeton Temps <i>(côte sablier)</i>.</li>"
                + "</ul>"

                + "<p style='text-align:center; color:#888; margin-top:16px; font-size:11px;'>"
                + ";&nbsp; Cliquez n'importe pour fermer</p>"
                + "</body></html>";
    }
}