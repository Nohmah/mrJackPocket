package src.modele;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public final class PartieSaveMapper {
    private PartieSaveMapper() {
    }

    public static PartieSnapshot toSnapshot(Partie partie) {
        PartieSnapshot snap = new PartieSnapshot();
        snap.grillePersonnages = new Personnage[3][3];
        snap.grilleMurs = new Orientation[3][3];
        snap.grilleFaces = new boolean[3][3];
        snap.grilleAPivote = new boolean[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Quartier q = partie.district.get(i, j);
                snap.grillePersonnages[i][j] = q.getPersonnage();
                snap.grilleMurs[i][j] = q.getOrientationMur();
                snap.grilleFaces[i][j] = q.estFaceSuspect();
                snap.grilleAPivote[i][j] = q.getAPivote();
            }
        }

        snap.positionsDetectives = new ArrayList<>();
        for (Detective d : partie.detectives) {
            snap.positionsDetectives.add(d.getPosition());
        }

        snap.suspects = new ArrayList<>(partie.suspects);

        snap.jetonsJoues = new ArrayList<>();
        snap.jetonsFaceRecto = new ArrayList<>();
        for (JetonAction j : partie.jetonsAction) {
            snap.jetonsJoues.add(j.isJoue());
            snap.jetonsFaceRecto.add(j.isFaceRectoVisible());
        }

        snap.cartesAlibiRestantes = new ArrayList<>();
        for (CarteAlibi c : partie.cartesAlibiPioche) {
            snap.cartesAlibiRestantes.add(c);
        }

        snap.derniereCarteAlibiPiochee = partie.derniereCarteAlibiPiochee;

        snap.numeroTour = partie.numeroTour;
        snap.coursePoursuiteActive = partie.coursePoursuiteActive;
        snap.gagnant = partie.gagnant;
        snap.niveauEnqueteur = partie.niveauEnqueteur;
        snap.niveauJack = partie.niveauJack;
        snap.sabliersDeJack = partie.sabliersDeJack;
        snap.identiteJack = partie.identiteJack;
        snap.jackVisibleCeTour = partie.jackVisibleCeTour;
        snap.totalActionsJouees = partie.totalActionsJouees;
        snap.joueurCourant = partie.joueurCourant;
        snap.IAChoisi = partie.IAChoisi;
        snap.joueurChoisi = partie.joueurChoisi;
        snap.difficulteIAChoisi = partie.difficulteIAChoisi;

        snap.isSolo = partie.getIsSolo();
        snap.pseudoEnqueteur = partie.getPseudoEnqueteur();
        snap.pseudoJack = partie.getPseudoJack();
        return snap;
    }

    public static void fromSnapshot(Partie partie, PartieSnapshot snap) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Quartier q = partie.district.get(i, j);
                q.setPersonnage(snap.grillePersonnages[i][j]);
                q.setOrientationMur(snap.grilleMurs[i][j]);
                q.setFace(snap.grilleFaces[i][j]);
                q.setAPivote(snap.grilleAPivote[i][j]);
            }
        }

        for (int i = 0; i < partie.detectives.size(); i++) {
            partie.detectives.get(i).setPosition(snap.positionsDetectives.get(i));
        }

        for (int i = 0; i < partie.jetonsAction.size(); i++) {
            partie.jetonsAction.get(i).setJoue(snap.jetonsJoues.get(i));
            partie.jetonsAction.get(i).setFaceRectoVisible(snap.jetonsFaceRecto.get(i));
        }

        partie.cartesAlibiPioche.clear();
        partie.cartesAlibiPioche.addAll(snap.cartesAlibiRestantes);

        partie.derniereCarteAlibiPiochee = snap.derniereCarteAlibiPiochee;

        partie.numeroTour = snap.numeroTour;
        partie.coursePoursuiteActive = snap.coursePoursuiteActive;
        partie.gagnant = snap.gagnant;
        partie.niveauEnqueteur = snap.niveauEnqueteur;
        partie.niveauJack = snap.niveauJack;
        partie.sabliersDeJack = snap.sabliersDeJack;
        partie.identiteJack = snap.identiteJack;
        partie.jackVisibleCeTour = snap.jackVisibleCeTour;
        partie.totalActionsJouees = snap.totalActionsJouees;
        partie.joueurCourant = snap.joueurCourant;
        partie.IAChoisi = snap.IAChoisi;
        partie.joueurChoisi = snap.joueurChoisi;
        partie.difficulteIAChoisi = snap.difficulteIAChoisi;

        partie.setIsSolo(snap.isSolo);
        partie.setPseudoEnqueteur(snap.pseudoEnqueteur);
        partie.setPseudoJack(snap.pseudoJack);
    }

    public static GameSave toGameSave(Partie partie) {
        GameSave save = new GameSave();
        save.current = toSnapshot(partie);
        save.undo = snapshotsFromDeque(partie.undo);
        save.redo = snapshotsFromDeque(partie.redo);
        return save;
    }

    public static void fromGameSave(Partie partie, GameSave save) {
        if (save == null) return;
        fromSnapshot(partie, save.current);
        restoreDequeFromSnapshots(partie, partie.undo, save.undo);
        restoreDequeFromSnapshots(partie, partie.redo, save.redo);
    }

    private static List<PartieSnapshot> snapshotsFromDeque(Deque<Partie> stack) {
        List<PartieSnapshot> snapshots = new ArrayList<>();
        if (stack == null) return snapshots;
        Iterator<Partie> it = stack.descendingIterator();
        while (it.hasNext()) {
            snapshots.add(it.next().toSnapshot());
        }
        return snapshots;
    }

    private static void restoreDequeFromSnapshots(Partie base, Deque<Partie> stack, List<PartieSnapshot> snapshots) {
        stack.clear();
        if (snapshots == null) return;
        for (PartieSnapshot snap : snapshots) {
            Partie p = new Partie(base);
            p.fromSnapshot(snap);
            stack.push(p);
        }
    }
}

