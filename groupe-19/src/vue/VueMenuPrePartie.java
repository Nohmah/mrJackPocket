package src.vue;

import src.modele.Partie;
import javax.swing.*;
import java.awt.*;

public class VueMenuPrePartie extends JPanel {
    private final CardLayout cardLayout;
    private final JPanel parentPanel;   // le mainPanel du CardLayout
    private final String type;           // "Solo" ou "Multi"

    /**
     * @param parentPanel   le panneau qui contient le CardLayout (mainPanel)
     * @param cardLayout    le CardLayout pour changer de vue
     * @param type          "Solo" ou "Multi"
     */
    public VueMenuPrePartie(JPanel parentPanel, CardLayout cardLayout, String type) {
        this.parentPanel = parentPanel;
        this.cardLayout = cardLayout;
        this.type = type;
        setBackground(Color.GRAY);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Contenu selon le type
        if ("Solo".equals(type)) {
            gbc.gridx = 0; gbc.gridy = 0;
            add(new JLabel("Difficulté :"), gbc);
            String[] difficulties = {"Facile", "Moyen", "Difficile"};
            JComboBox<String> diffCombo = new JComboBox<>(difficulties);
            gbc.gridx = 1;
            add(diffCombo, gbc);
        } else {
            gbc.gridx = 0; gbc.gridy = 0;
            add(new JLabel("Rôle :"), gbc);
            String[] roles = {"Serveur (créer la partie)", "Client (rejoindre)"};
            JComboBox<String> roleCombo = new JComboBox<>(roles);
            gbc.gridx = 1;
            add(roleCombo, gbc);
        }

        // Bouton Lancer
        JButton startButton = new JButton("Lancer la partie");
        startButton.addActionListener(e -> {
            // Créer la partie et lancer le jeu
            Partie partie = new Partie();
            VueJeu jeu = new VueJeu(partie);
            jeu.setVisible(true);
            // Fermer la fenêtre principale (le menu)
            SwingUtilities.getWindowAncestor(this).dispose();
        });
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        add(startButton, gbc);

        // Bouton Retour
        JButton backButton = new JButton("Retour");
        backButton.addActionListener(e -> cardLayout.show(parentPanel, "MENU"));
        gbc.gridy = 2;
        add(backButton, gbc);
    }
}