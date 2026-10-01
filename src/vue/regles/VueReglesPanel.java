package src.vue.regles;

public class VueReglesPanel {

    public static String getHtml() {
        return "<html>"
                + "<body style='width:700px; color:white; font-family:SansSerif; padding-left:10px;'>"

                + "<h1 style='color:#FFA500; text-align:center; margin-bottom:6px;'>"
                + "Règles de Mr. Jack Pocket</h1>"

                + "<hr style='border:1px solid #FFA500; margin-bottom:14px;'/>"

                + "<h2 style='color:#FFA500; margin-bottom:4px;'>But du Jeu</h2>"

                + "<h3 style='color:#87CEFA;'>Pour l'Enquêteur</h3>"
                + "<p>"
                + "Découvrir parmi les neuf suspects sous quelle identité se cache Mr Jack."
                + "</p>"

                + "<p>"
                + "Pour gagner, il ne doit subsister "
                + "<span style='color:#87CEFA'>qu’un seul suspect</span> "
                + "(c'est-à-dire une seule tuile sur sa face personnage) "
                + "avant la fin du huitième tour."
                + "</p>"
                + "L'Enquêteur retourne les tuiles de leur face personnage à leur face vide en <span style='color:#87CEFA'>"
                + "piochant des cartes alibis</span> "
                + "(à condition que le personnage de la carte n’ait pas déjà été innocenté) et en <span style='color:#87CEFA'>"
                + "terminant les tours</span>, selon une règle vue plus loin dans l'Appel à Témoins."
                + "<h3 style='color:#FF4C4C;'>Pour Jack</h3>"
                + "<p>"
                + "Conserver son identité secrète en faisant perdre un maximum de temps à l'Enquêteur."
                + "</p>"

                + "<p>"
                + "Pour Mr. Jack, il s’agit d’obtenir "
                + "<span style='color:#FF4C4C;'>six sabliers</span> "
                + "avant que l’inspecteur ne découvre son identité, "
                + "matérialisant ainsi le fait que l’Enquêteur a perdu trop de temps pour mener à bien son enquête."
                + "<p>Jack obtient un sablier en <span style='color:#FF4C4C;'>terminant un tour non visible par les 3 détectives</span>, et en <span style='color:#FF4C4C;'>récupérant les sabliers "
                + "des cartes alibis</span> qu'il pioche."
                + "</p>"

                + "<h2 style='color:#FFA500; margin-top:10px; margin-bottom:4px;'>"
                + "Déroulement d'un Tour</h2>"

                + "<p>Chaque tour se compose de deux étapes : "
                + "<b>La Traque</b> et <b>L'Appel à Témoins</b>.</p>"

                + "<h3 style='color:#FFD700;'>1. La Traque (Actions)</h3>"

                + "<p>L'ordre de jeu change selon la parité du tour :</p>"

                + "<ul>"
                + "<li><b>Tours Impairs (1, 3, 5, 7) :</b> Les 4 jetons Action sont lancés "
                + "et les actions sur leur faces visibles deviennent possible ce tour. "
                + "L'Enquêteur choisit une action et la joue, "
                + "Jack en choisit deux et les joue et l'Enquêteur joue la dernière.</li>"

                + "<li><b>Tours Pairs (2, 4, 6, 8) :</b> Les 4 jetons Action lancé lors du tour précédent sont retournés. "
                + "Jack choisit une action et la joue, l'Enquêteur en choisit deux et les joue, Jack joue la dernière.</li>"
                + "</ul>"

                + "<p><b>Détail des actions disponibles :</b></p>"

                + "<ul>"
                + "<li><b>Déplacement :</b> Avancer Holmes, Watson ou Toby "
                + "de 1 ou 2 cases <i>(sens horaire)</i>.</li>"

                + "<li><b>Échange :</b> échanger deux tuiles de place sans changer leur orientation.</li>"

                + "<li><b>Rotation :</b> faire pivoter une tuile (90 ou 180). <i>Chaque tour une tuile ne peut subir qu'une seule rotation.</i></li>"

                + "<li><b>Joker :</b> déplacer l'Enquêteur de son choix de 0 "
                + "<i>(Jack seulement)</i> ou 1 case.</li>"

                + "<li><b>Alibi :</b> piocher une carte Alibi. "
                + "L'Enquêteur innocente le personnage ; Jack gagne des sabliers.</li>"
                + "</ul>"

                + "<h3 style='color:#FFD700;'>2. L'Appel à Témoins</h3>"

                + "<p>Une fois que les 4 actions du tour on été réalisées, Jack annonce s'il est <b>visible</b> "
                + "(s'il est dans ligne de mire d'au moins un des 3 détectives) :</p>"

                + "<ul>"
                + "<li><b>Jack visible :</b> Les tuiles qui contiennent les suspects non visibles par les 3 détectives sont retournés sur leur face vide. (Ils ne peuvent pas être Jack.) "
                + "L'Enquêteur prend le jeton Temps du tour, privant Jack du sablier du tour.</li>"

                + "<li><b>Jack invisible :</b> Les tuiles qui contiennent les suspects visibles par au moins 1 détective sont retournés. (Ils ne peuvent pas être Jack.) "
                + "Jack prend le jeton Temps du tour <i>(côté sablier)</i>.</li>"
                + "<h2 style='color:#FFA500; margin-top:10px; margin-bottom:4px;'>"
                + "Conditions de victoire</h2>"

                + "<h3 style='color:#87CEFA;'>Pour l'Enquêteur</h3>"
                + "<p>"
                + "Après l'Appel à témoin, il ne doit subsister qu’un seul suspect.<br>"
                + "Ce suspect est forcément le coupable !"
                + "</p>"

                + "<h3 style='color:#FF4C4C;'>Pour Mr Jack</h3>"
                + "<p>"
                + "Après l'Appel à témoin, il doit totaliser au moins <b>six sabliers</b> "
                + "en additionnant les sabliers des jetons Temps et ceux des cartes Alibi obtenues en cours de partie."
                + "</p>"

                + "<h3 style='color:#FFA500;'>Cas particuliers</h3>"
                + "<p>"
                + "- Il arrive parfois que les deux joueurs atteignent leur but en même temps.<br>"
                + "Si cela se produit à la fin du huitième tour : "
                + "l’Enquêteur gagne si Mr Jack est visible, sinon Mr Jack gagne.<br><br>"

                + "- Si cela se produit avant le huitième tour, la partie continue et une course-poursuite s’engage :<br>"
                + "l’Enquêteur gagne dès qu’il termine un tour avec Mr Jack visible.<br>"
                + "Mr Jack gagne s’il reste invisible jusqu’à la fin du huitième tour.<br><br>"

                + "- Si aucun des deux joueurs n’a atteint son objectif à la fin du tour 8, Mr Jack est vainqueur."
                + "</p>"

                + "</body></html>";
    }
}