package src.modele.ia.laboratoire_genetique;

import src.modele.Joueur;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PoolProfils {
    public List<ProfilGenetique> listeProfils;
    public Joueur roleDuPool;

    public PoolProfils(Joueur role, int nbPoids) {
        this.roleDuPool = role;
        this.listeProfils = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            this.listeProfils.add(new ProfilGenetique(role, nbPoids));
        }
    }

    public ProfilGenetique getProfil(int i) {
        return listeProfils.get(i);
    }

    public void setProfil(int i, ProfilGenetique p) {
        listeProfils.set(i, p);
    }

    public ProfilGenetique getAdversaireAleatoire() {
        Random r = new Random();
        return listeProfils.get(r.nextInt(listeProfils.size()));
    }

    public void trierParScore() {
        this.listeProfils.sort((p1, p2) -> Integer.compare(p2.score, p1.score));
    }
}