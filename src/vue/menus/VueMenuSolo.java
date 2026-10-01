package src.vue.menus;

import javax.swing.*;
import java.awt.*;
import src.modele.Partie;
import src.modele.GameSave;
import src.utils.SaveManager;
import src.vue.VueJeu;
import javax.swing.JOptionPane;

public class VueMenuSolo extends JPanel {

    public VueMenuSolo(JFrame parent, VueMenuPrincipal menuPrincipal) {
        setLayout(new BorderLayout());
        Background background = new Background();
        background.setLayout(new BorderLayout());
        add(background, BorderLayout.CENTER);

        JButton retour = BoutonsMenu.creerBouton(
                "Boutonretour",
                () -> {
                    menuPrincipal.resetBoutonsSurvoles();
                    parent.setContentPane(menuPrincipal);
                    parent.revalidate();
                    parent.repaint();
                },
                new Dimension(160, 66)
        );

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        topPanel.add(retour);
        background.add(topPanel, BorderLayout.NORTH);

        JPanel panelGlobal = new JPanel();
        panelGlobal.setLayout(new BoxLayout(panelGlobal, BoxLayout.Y_AXIS));
        panelGlobal.setOpaque(false);

        JButton nouvellePartie = BoutonsMenu.creerBouton(
                "Boutonnouvellepartiesolo",
                () -> {
                    VueMenuPrePartie prePartie = new VueMenuPrePartie(parent, this, menuPrincipal, "solo");
                    parent.setContentPane(prePartie);
                    parent.revalidate();
                    parent.repaint();
                }
        );
        // on détermine si il y a une save à charger
        boolean sauvegardeExiste;
        try {
            SaveManager.load("save.dat");
            sauvegardeExiste = true;
        } catch (Exception e) {
            sauvegardeExiste = false;
        }
        boolean finalSauvegardeExiste = sauvegardeExiste;
        JButton chargerPartie = BoutonsMenu.creerBouton(
                sauvegardeExiste ? "Boutonchargerpartie" : "Boutonchargerpartiegrise",
            () -> {
                if (!finalSauvegardeExiste) return;
                try {
                    GameSave saveFile = SaveManager.load("save.dat");
                    Partie partie = new Partie(
                            saveFile.current.joueurChoisi,
                            saveFile.current.niveauJack,
                            saveFile.current.niveauEnqueteur,
                            saveFile.current.pseudoEnqueteur,
                            saveFile.current.pseudoJack
                    );
                    partie.fromGameSave(saveFile);
                    VueJeu jeu = new VueJeu(parent, menuPrincipal, partie);
                    parent.setContentPane(jeu);
                    parent.revalidate();
                    parent.repaint();
                } catch (Exception ex) {
                    System.err.println("Chargement echoue: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this,
                            "Aucune sauvegarde trouvée",
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        );

        panelGlobal.add(nouvellePartie);
        panelGlobal.add(Box.createVerticalStrut(50));
        panelGlobal.add(chargerPartie);

        JPanel centreWrapper = new JPanel(new GridBagLayout());
        centreWrapper.setOpaque(false);
        centreWrapper.add(panelGlobal);
        background.add(centreWrapper, BorderLayout.CENTER);
    }
}