package src.modele.ia.laboratoire_genetique;

import src.modele.Partie;
import src.modele.Joueur;
import src.modele.ia.IaMinMax;
import src.modele.ia.CoupIa;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Arene {
    private int profondeurMinMax = 2;
    private static final ThreadLocal<Boolean> silencieux = ThreadLocal.withInitial(() -> false);

    static {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                if (!silencieux.get()) {
                    originalOut.write(b);
                }
            }
            @Override
            public void write(byte[] b, int off, int len) {
                if (!silencieux.get()) {
                    originalOut.write(b, off, len);
                }
            }
        }));

        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                if (!silencieux.get()) {
                    originalErr.write(b);
                }
            }
            @Override
            public void write(byte[] b, int off, int len) {
                if (!silencieux.get()) {
                    originalErr.write(b, off, len);
                }
            }
        }));
    }

    public void evaluerPool(PoolProfils poolMutant, PoolProfils poolAdverse) {
        System.out.print("Evaluation en cours : ");
        AtomicInteger compteur = new AtomicInteger(0);

        ExecutorService customExecutor = Executors.newFixedThreadPool(3);

        for (ProfilGenetique mutant : poolMutant.listeProfils) {
            customExecutor.submit(() -> {
                silencieux.set(true);
                try {
                    mutant.resetScore();
                    for (int i = 0; i < 50; i++) {
                        ProfilGenetique adv = poolAdverse.getAdversaireAleatoire();
                        if (lancerMatch(mutant, adv)) {
                            synchronized (mutant) {
                                mutant.incrementerScore();
                            }
                        }
                    }
                    int val = compteur.incrementAndGet();
                    if (val % 10 == 0) {
                        silencieux.set(false);
                        System.out.print(".");
                        silencieux.set(true);
                    }
                } finally {
                    silencieux.set(false);
                }
            });
        }

        customExecutor.shutdown();
        try {
            customExecutor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            System.err.println("Arene interrompue");
        }
        System.out.println(" Termine !");
    }

    public boolean lancerMatch(ProfilGenetique mutant, ProfilGenetique adv) {
        try {
            Partie p = new Partie(Joueur.ENQUETEUR, -1, -1, "Inspecteur", "Jack");
            p.estSimulation = true;

            if (mutant.role == Joueur.JACK) {
                p.adnJack = mutant;
                p.adnInspecteur = adv;
            } else {
                p.adnJack = adv;
                p.adnInspecteur = mutant;
            }

            int security = 0;
            while (!p.isPartieTerminee() && security < 100) {
                security++;
                p.changement = false;

                if (p.actions.getActionsPossibles().isEmpty()) {
                    p.appelATemoin();
                    if (!p.isPartieTerminee()) {
                        p.tourSuivant();
                    }
                    continue;
                }

                CoupIa coup = IaMinMax.choisirActionMinMax(p, p.joueurCourant == Joueur.JACK, profondeurMinMax);
                if (coup != null) {
                    p.jouerCoup(coup);
                } else {
                    break;
                }

                if (p.totalActionsJouees >= 4) {
                    p.appelATemoin();
                    if (!p.isPartieTerminee()) {
                        p.tourSuivant();
                    }
                }
            }
            return p.getGagnant() == mutant.role;
        } catch (Exception e) {
            return false;
        }
    }
}
