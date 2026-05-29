package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * SF-214-31 — calculateur du délai de recours devant le Tribunal administratif
 * de Nantes contre un refus de naturalisation par décret : 2 mois à compter de
 * la notification du refus (CJA droit commun). Outil single-country FR.
 *
 * <p>Le délai est calculé en mois calendaires : {@code dateRefusDecret + 2 mois}.
 * Le statut bascule en URGENT lorsqu'il reste 15 jours ou moins, et en PRESCRIT
 * lorsque le délai est dépassé (joursRestants ≤ 0).
 *
 * <p>Procédure ADMINISTRATIVE — le TA de Nantes dispose d'une compétence
 * territoriale exclusive nationale pour les recours contre les refus de
 * naturalisation par décret. Distinct du recours TJ refus de déclaration de
 * nationalité (SF-214-29, juridiction civile).
 *
 * <p>Sources :
 * <ul>
 *   <li>Cciv 21-15 — naturalisation par décret (faveur de l'État) ;</li>
 *   <li>CJA L. 213-1 — délai de droit commun du recours contentieux (2 mois) ;</li>
 *   <li>CJA R. 312-4 — compétence territoriale exclusive du TA de Nantes pour les
 *       décisions relatives à la nationalité.</li>
 * </ul>
 */
public final class NaturalisationRecoursTaNantesCalculator {

    /** Délai de recours devant le TA — 2 mois calendaires (CJA droit commun). */
    public static final int DELAI_RECOURS_MOIS = 2;

    /** Seuil (inclusif) en jours restants en deçà duquel le statut bascule en URGENT. */
    static final int SEUIL_URGENT_JOURS = 15;

    /** Borne haute d'ancienneté de la date de refus acceptée — 24 mois. */
    static final int MAX_ANCIENNETE_REFUS_MOIS = 24;

    /** Longueur maximale du champ libre de motivation du refus. */
    static final int MAX_MOTIVATION_LENGTH = 500;

    static final String TRIBUNAL_COMPETENT =
            "Tribunal administratif de Nantes (compétence exclusive nationale — refus décret naturalisation)";

    private static final String BASE_CJA_L213_1 =
            "CJA L. 213-1 (délai de droit commun du recours contentieux administratif — 2 mois)";
    private static final String BASE_CCIV_21_15 =
            "Cciv 21-15 (naturalisation par décret)";
    private static final String BASE_CJA_R312_4 =
            "CJA R. 312-4 (compétence territoriale exclusive du TA de Nantes en matière de nationalité)";

    private NaturalisationRecoursTaNantesCalculator() {
    }

    /** Surcharge utilisant la date du jour système (UTC) comme référence. */
    public static NaturalisationRecoursTaNantesResult compute(
            LocalDate dateRefusDecret,
            String motivationRefus,
            boolean recoursPrerequis) {
        return compute(dateRefusDecret, motivationRefus, recoursPrerequis, LocalDate.now());
    }

    /**
     * Calcule le délai et le statut du recours TA.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static NaturalisationRecoursTaNantesResult compute(
            LocalDate dateRefusDecret,
            String motivationRefus,
            boolean recoursPrerequis,
            LocalDate today) {
        validate(dateRefusDecret, motivationRefus, today);

        LocalDate dateEcheanceRecoursTa = dateRefusDecret.plusMonths(DELAI_RECOURS_MOIS);
        long joursRestants = ChronoUnit.DAYS.between(today, dateEcheanceRecoursTa);

        NaturalisationRecoursTaNantesStatut statut;
        if (joursRestants <= 0) {
            statut = NaturalisationRecoursTaNantesStatut.PRESCRIT;
        } else if (joursRestants <= SEUIL_URGENT_JOURS) {
            statut = NaturalisationRecoursTaNantesStatut.URGENT;
        } else {
            statut = NaturalisationRecoursTaNantesStatut.RECOURS_POSSIBLE;
        }

        List<String> basesJuridiques = List.of(BASE_CJA_L213_1, BASE_CCIV_21_15, BASE_CJA_R312_4);

        boolean prescrit = statut == NaturalisationRecoursTaNantesStatut.PRESCRIT;
        List<String> motifsRecoursDisponibles = prescrit ? List.of() : buildMotifs();
        String messagePrescription = prescrit
                ? "Délai de recours de 2 mois (CJA L. 213-1) dépassé — le recours pour excès de pouvoir "
                        + "devant le Tribunal administratif de Nantes est prescrit et serait jugé "
                        + "irrecevable. Étudier le dépôt d'une nouvelle demande de naturalisation par décret."
                : null;

        String recommandation = buildRecommandation(statut);

        return new NaturalisationRecoursTaNantesResult(
                dateRefusDecret,
                motivationRefus,
                recoursPrerequis,
                dateEcheanceRecoursTa,
                joursRestants,
                TRIBUNAL_COMPETENT,
                basesJuridiques,
                motifsRecoursDisponibles,
                statut,
                messagePrescription,
                recommandation
        );
    }

    private static List<String> buildMotifs() {
        return List.of(
                "Défaut ou insuffisance de motivation de la décision de refus (loi du 11 juillet 1979)",
                "Excès de pouvoir — incompétence, vice de forme, détournement de pouvoir",
                "Erreur manifeste d'appréciation des critères d'intégration républicaine (Cciv 21-24)",
                "Vice de procédure dans l'instruction de la demande de naturalisation par décret");
    }

    private static String buildRecommandation(NaturalisationRecoursTaNantesStatut statut) {
        return switch (statut) {
            case RECOURS_POSSIBLE -> "Introduire une requête en excès de pouvoir devant le Tribunal "
                    + "administratif de Nantes (CJA L. 213-1 ; R. 312-4) dans le délai de 2 mois suivant "
                    + "la notification du refus de naturalisation par décret ; un recours gracieux ou "
                    + "hiérarchique préalable proroge le délai contentieux.";
            case URGENT -> "Échéance des 2 mois imminente — déposer sans délai la requête en excès de "
                    + "pouvoir devant le Tribunal administratif de Nantes (CJA L. 213-1) pour éviter la "
                    + "prescription ; envisager un référé suspension si l'urgence le justifie.";
            case PRESCRIT -> "Délai de 2 mois dépassé : le recours pour excès de pouvoir est prescrit. "
                    + "Étudier le dépôt d'une nouvelle demande de naturalisation par décret en corrigeant "
                    + "les motifs ayant fondé le refus.";
        };
    }

    private static void validate(LocalDate dateRefusDecret, String motivationRefus, LocalDate today) {
        if (dateRefusDecret == null) {
            throw new IllegalArgumentException("dateRefusDecret est requise");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (motivationRefus != null && motivationRefus.length() > MAX_MOTIVATION_LENGTH) {
            throw new IllegalArgumentException(
                    "motivationRefus ne peut pas dépasser " + MAX_MOTIVATION_LENGTH + " caractères");
        }
        if (dateRefusDecret.isAfter(today)) {
            throw new IllegalArgumentException("dateRefusDecret ne peut pas être dans le futur");
        }
        if (dateRefusDecret.isBefore(today.minusMonths(MAX_ANCIENNETE_REFUS_MOIS))) {
            throw new IllegalArgumentException(
                    "dateRefusDecret est antérieure à " + MAX_ANCIENNETE_REFUS_MOIS
                            + " mois — refus hors périmètre de cet outil");
        }
    }
}
