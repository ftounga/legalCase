package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-221-02 — calculateur d'éligibilité au passage carte A → carte B
 * (séjour ILLIMITÉ d'un ressortissant tiers) en Belgique.
 *
 * <p>Sources <i>(à vérifier par avocat BE 2026)</i> :
 * <ul>
 *   <li>Loi du 15/12/1980 art. 14 — séjour illimité après 5 ans de séjour régulier
 *       ininterrompu (seuil indicatif, variantes selon le motif de séjour).</li>
 * </ul>
 *
 * <p>Seuil de durée : 5 ans = 60 mois de séjour régulier ininterrompu.
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — un outil = une situation : ne traite que le
 * passage au séjour ILLIMITÉ national. DISTINCT de F-IM-53 (prorogation carte A,
 * maintien temporaire du même motif) et de F-IM-55 (résident longue durée UE,
 * directive 2003/109/CE — conditions propres + mobilité intra-UE). La carte B ne
 * confère PAS la mobilité intra-UE.
 */
public final class CarteBSejourIllimiteBeCalculator {

    /** Seuil de durée — 5 ans = 60 mois de séjour régulier ininterrompu. */
    public static final int SEUIL_DUREE_MOIS = 60;

    private static final List<String> BASES_JURIDIQUES = List.of(
            "Loi du 15/12/1980 art. 14 (séjour illimité après 5 ans de séjour régulier "
                    + "ininterrompu) (à vérifier par avocat)");

    private CarteBSejourIllimiteBeCalculator() {
    }

    /** Surcharge utilisant la date du jour comme référence. */
    public static CarteBSejourIllimiteBeResult compute(LocalDate dateDebutSejourRegulier,
                                                       Boolean sejourIninterrompu,
                                                       Boolean absencesSuperieuresLimites,
                                                       Boolean motifSejourStable,
                                                       Boolean ordrePublicRisque) {
        return compute(dateDebutSejourRegulier, sejourIninterrompu, absencesSuperieuresLimites,
                motifSejourStable, ordrePublicRisque, LocalDate.now());
    }

    /**
     * Calcule le verdict d'éligibilité avec une date de référence injectée (testabilité).
     *
     * @param today date de référence (généralement {@code LocalDate.now()}).
     */
    public static CarteBSejourIllimiteBeResult compute(LocalDate dateDebutSejourRegulier,
                                                       Boolean sejourIninterrompu,
                                                       Boolean absencesSuperieuresLimites,
                                                       Boolean motifSejourStable,
                                                       Boolean ordrePublicRisque,
                                                       LocalDate today) {
        validate(dateDebutSejourRegulier, sejourIninterrompu, absencesSuperieuresLimites,
                motifSejourStable, ordrePublicRisque, today);

        boolean ininterrompu = sejourIninterrompu;
        boolean absencesExcessives = absencesSuperieuresLimites;
        boolean motifStable = motifSejourStable;
        boolean ordrePublic = ordrePublicRisque;

        int dureeSejourMois = (int) ChronoUnit.MONTHS.between(dateDebutSejourRegulier, today);
        int moisRestants = Math.max(0, SEUIL_DUREE_MOIS - dureeSejourMois);

        List<String> messages = new ArrayList<>();

        CarteBSejourIllimiteBeVerdict verdict;
        if (ordrePublic) {
            verdict = CarteBSejourIllimiteBeVerdict.RISQUE_ORDRE_PUBLIC;
            messages.add("Un risque d'ordre public est signalé : la délivrance de la carte B "
                    + "peut être refusée ou soumise à un examen renforcé.");
        } else if (!ininterrompu || absencesExcessives) {
            verdict = CarteBSejourIllimiteBeVerdict.CONTINUITE_ROMPUE;
            if (!ininterrompu) {
                messages.add("Le séjour régulier n'a pas été ininterrompu : la continuité de "
                        + "5 ans requise est rompue.");
            }
            if (absencesExcessives) {
                messages.add("Les absences du territoire dépassent les limites admises : la "
                        + "continuité du séjour est rompue.");
            }
        } else if (dureeSejourMois < SEUIL_DUREE_MOIS) {
            verdict = CarteBSejourIllimiteBeVerdict.DUREE_INSUFFISANTE;
            messages.add("Durée de séjour régulier insuffisante : " + dureeSejourMois
                    + " mois sur les 60 requis. Il reste " + moisRestants
                    + " mois avant l'ouverture du droit (seuil indicatif art. 14).");
        } else if (motifStable) {
            verdict = CarteBSejourIllimiteBeVerdict.ELIGIBLE;
            messages.add("Conditions réunies : au moins 5 ans (60 mois) de séjour régulier "
                    + "ininterrompu, motif de séjour stable et aucun risque d'ordre public. "
                    + "Le passage en séjour illimité (carte B) est en principe ouvert.");
        } else {
            verdict = CarteBSejourIllimiteBeVerdict.A_EXAMINER;
            messages.add("Durée et continuité réunies mais le motif de séjour n'est pas établi "
                    + "comme stable : examen au cas par cas du droit au séjour illimité.");
        }

        if (verdict != CarteBSejourIllimiteBeVerdict.DUREE_INSUFFISANTE) {
            messages.add("Durée de séjour régulier écoulée : " + dureeSejourMois + " mois "
                    + "(seuil indicatif art. 14 = 60 mois).");
        }

        return new CarteBSejourIllimiteBeResult(
                dateDebutSejourRegulier,
                ininterrompu,
                absencesExcessives,
                motifStable,
                ordrePublic,
                verdict,
                dureeSejourMois,
                moisRestants,
                BASES_JURIDIQUES,
                Collections.unmodifiableList(messages));
    }

    private static void validate(LocalDate dateDebutSejourRegulier,
                                 Boolean sejourIninterrompu,
                                 Boolean absencesSuperieuresLimites,
                                 Boolean motifSejourStable,
                                 Boolean ordrePublicRisque,
                                 LocalDate today) {
        if (dateDebutSejourRegulier == null) {
            throw new IllegalArgumentException("dateDebutSejourRegulier est requise");
        }
        if (sejourIninterrompu == null) {
            throw new IllegalArgumentException("sejourIninterrompu est requis");
        }
        if (absencesSuperieuresLimites == null) {
            throw new IllegalArgumentException("absencesSuperieuresLimites est requis");
        }
        if (motifSejourStable == null) {
            throw new IllegalArgumentException("motifSejourStable est requis");
        }
        if (ordrePublicRisque == null) {
            throw new IllegalArgumentException("ordrePublicRisque est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateDebutSejourRegulier.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateDebutSejourRegulier ne peut pas être dans le futur");
        }
    }
}
