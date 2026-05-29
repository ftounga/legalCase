package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-214-29 — calculateur du délai de recours devant le Tribunal judiciaire
 * contre un refus de déclaration de nationalité française : 6 mois à compter du
 * refus (Cciv 26-3). Outil single-country FR.
 *
 * <p>Le délai est calculé en mois calendaires : {@code dateRefusDeclaration +
 * 6 mois} (Cciv 26-3). Le statut bascule en URGENT lorsqu'il reste 30 jours ou
 * moins, et en PRESCRIT lorsque le délai est dépassé (joursRestants ≤ 0).
 *
 * <p>Procédure CIVILE (juridiction judiciaire) — distincte du recours décret de
 * naturalisation porté devant le TA de Nantes (SF-214-31).
 *
 * <p>Sources :
 * <ul>
 *   <li>Cciv 26-3 — refus d'enregistrement d'une déclaration de nationalité,
 *       recours devant le tribunal judiciaire dans les 6 mois ;</li>
 *   <li>Cciv 21-2 — acquisition par mariage ;</li>
 *   <li>Cciv 21-13 — acquisition par ascendance française ;</li>
 *   <li>Cciv 22-1 — nationalité du mineur ;</li>
 *   <li>CPC art. 1043 — procédure en matière de nationalité devant le TJ.</li>
 * </ul>
 */
public final class NaturalisationRecoursTjCalculator {

    /** Délai de recours devant le TJ — 6 mois calendaires (Cciv 26-3). */
    public static final int DELAI_RECOURS_MOIS = 6;

    /** Seuil (inclusif) en jours restants en deçà duquel le statut bascule en URGENT. */
    static final int SEUIL_URGENT_JOURS = 30;

    /** Borne haute d'ancienneté de la date de refus acceptée — 24 mois. */
    static final int MAX_ANCIENNETE_REFUS_MOIS = 24;

    static final String TRIBUNAL_COMPETENT =
            "Tribunal judiciaire du lieu de résidence (juridiction civile — distinct du TA de Nantes)";

    private static final String BASE_CCIV_26_3 =
            "Cciv 26-3 (recours devant le tribunal judiciaire contre le refus d'enregistrement "
                    + "d'une déclaration de nationalité, délai 6 mois)";
    private static final String BASE_CPC_1043 =
            "CPC art. 1043 (procédure en matière de nationalité devant le tribunal judiciaire)";

    private NaturalisationRecoursTjCalculator() {
    }

    /** Surcharge utilisant la date du jour système (UTC) comme référence. */
    public static NaturalisationRecoursTjResult compute(
            NaturalisationRecoursTjVoieEnum voieNaturalisation,
            LocalDate dateRefusDeclaration,
            NaturalisationRecoursTjTypeRefusEnum typeRefus) {
        return compute(voieNaturalisation, dateRefusDeclaration, typeRefus, LocalDate.now());
    }

    /**
     * Calcule le délai et le statut du recours TJ.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static NaturalisationRecoursTjResult compute(
            NaturalisationRecoursTjVoieEnum voieNaturalisation,
            LocalDate dateRefusDeclaration,
            NaturalisationRecoursTjTypeRefusEnum typeRefus,
            LocalDate today) {
        validate(voieNaturalisation, dateRefusDeclaration, typeRefus, today);

        LocalDate dateEcheanceRecoursJudicaire = dateRefusDeclaration.plusMonths(DELAI_RECOURS_MOIS);
        long joursRestants = ChronoUnit.DAYS.between(today, dateEcheanceRecoursJudicaire);

        NaturalisationRecoursTjStatut statut;
        if (joursRestants <= 0) {
            statut = NaturalisationRecoursTjStatut.PRESCRIT;
        } else if (joursRestants <= SEUIL_URGENT_JOURS) {
            statut = NaturalisationRecoursTjStatut.URGENT;
        } else {
            statut = NaturalisationRecoursTjStatut.RECOURS_POSSIBLE;
        }

        List<String> basesJuridiques = buildBasesJuridiques(voieNaturalisation);

        boolean prescrit = statut == NaturalisationRecoursTjStatut.PRESCRIT;
        List<String> motifsRecoursDisponibles = prescrit
                ? List.of()
                : buildMotifs(voieNaturalisation);
        String messagePrescription = prescrit
                ? "Délai de recours de 6 mois (Cciv 26-3) dépassé — le recours devant le tribunal "
                        + "judiciaire est prescrit et serait jugé irrecevable. Vérifier l'existence "
                        + "d'une nouvelle voie (nouvelle déclaration, demande par décret)."
                : null;

        String recommandation = buildRecommandation(statut, voieNaturalisation);

        return new NaturalisationRecoursTjResult(
                voieNaturalisation,
                dateRefusDeclaration,
                typeRefus,
                dateEcheanceRecoursJudicaire,
                joursRestants,
                TRIBUNAL_COMPETENT,
                basesJuridiques,
                motifsRecoursDisponibles,
                statut,
                messagePrescription,
                recommandation
        );
    }

    private static List<String> buildBasesJuridiques(NaturalisationRecoursTjVoieEnum voie) {
        List<String> bases = new ArrayList<>();
        bases.add(BASE_CCIV_26_3);
        switch (voie) {
            case MARIAGE -> bases.add("Cciv 21-2 (acquisition de la nationalité française par mariage)");
            case ASCENDANT -> bases.add("Cciv 21-13 (acquisition par possession d'état / ascendance française)");
            case MINEUR_22_1 -> bases.add("Cciv 22-1 (nationalité française du mineur)");
        }
        bases.add(BASE_CPC_1043);
        return List.copyOf(bases);
    }

    private static List<String> buildMotifs(NaturalisationRecoursTjVoieEnum voie) {
        return switch (voie) {
            case MARIAGE -> List.of(
                    "Erreur d'appréciation sur la communauté de vie affective et matérielle (Cciv 21-2)",
                    "Erreur sur la durée de mariage exigée (Cciv 21-2)",
                    "Vice de forme ou de motivation de la décision de refus",
                    "Défaut de prise en compte de pièces probantes (assimilation, communauté de vie)");
            case ASCENDANT -> List.of(
                    "Erreur sur la justification de l'ascendance française (Cciv 21-13)",
                    "Erreur d'appréciation sur la possession d'état de Français",
                    "Vice de forme ou de motivation de la décision de refus",
                    "Défaut de prise en compte de pièces d'état civil probantes");
            case MINEUR_22_1 -> List.of(
                    "Erreur d'appréciation sur l'effet collectif de l'acquisition (Cciv 22-1)",
                    "Erreur sur la résidence habituelle du mineur avec le parent",
                    "Vice de forme ou de motivation de la décision de refus",
                    "Défaut de prise en compte de pièces d'état civil probantes");
        };
    }

    private static String buildRecommandation(NaturalisationRecoursTjStatut statut,
                                               NaturalisationRecoursTjVoieEnum voie) {
        return switch (statut) {
            case RECOURS_POSSIBLE -> "Saisir le tribunal judiciaire du lieu de résidence par assignation "
                    + "dirigée contre le ministère public (CPC art. 1043) dans le délai de 6 mois "
                    + "(Cciv 26-3) ; constituer le dossier de pièces et invoquer les motifs adaptés à la voie.";
            case URGENT -> "Échéance des 6 mois imminente — préparer et délivrer sans délai l'assignation "
                    + "devant le tribunal judiciaire (Cciv 26-3 ; CPC art. 1043) pour éviter la prescription.";
            case PRESCRIT -> "Délai de 6 mois dépassé : le recours devant le tribunal judiciaire est prescrit. "
                    + "Étudier une nouvelle voie d'accès à la nationalité (nouvelle déclaration ou demande par décret).";
        };
    }

    private static void validate(NaturalisationRecoursTjVoieEnum voieNaturalisation,
                                 LocalDate dateRefusDeclaration,
                                 NaturalisationRecoursTjTypeRefusEnum typeRefus,
                                 LocalDate today) {
        if (voieNaturalisation == null) {
            throw new IllegalArgumentException("voieNaturalisation est requise");
        }
        if (dateRefusDeclaration == null) {
            throw new IllegalArgumentException("dateRefusDeclaration est requise");
        }
        if (typeRefus == null) {
            throw new IllegalArgumentException("typeRefus est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateRefusDeclaration.isAfter(today)) {
            throw new IllegalArgumentException("dateRefusDeclaration ne peut pas être dans le futur");
        }
        if (dateRefusDeclaration.isBefore(today.minusMonths(MAX_ANCIENNETE_REFUS_MOIS))) {
            throw new IllegalArgumentException(
                    "dateRefusDeclaration est antérieure à " + MAX_ANCIENNETE_REFUS_MOIS
                            + " mois — refus hors périmètre de cet outil");
        }
    }
}
