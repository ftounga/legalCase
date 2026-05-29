package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-214-07 — calculateur du délai de validation du VLS-TS (visa long séjour
 * valant titre de séjour) auprès de l'OFII : 3 mois à compter de l'entrée en
 * France (art. R. 311-3 CESEDA). Au-delà, le séjour devient irrégulier faute de
 * validation. Outil single-country FR.
 *
 * <p>Le délai est calculé en mois calendaires : {@code dateEntreeFrance + 3 mois}
 * (R. 311-3). Le statut bascule en URGENT lorsqu'il reste 15 jours ou moins.
 *
 * <p>Sources :
 * <ul>
 *   <li>R. 311-3 CESEDA — obligation de validation du VLS-TS dans les 3 mois de l'entrée ;</li>
 *   <li>R. 311-13 CESEDA — validité du VLS-TS comme titre de séjour ;</li>
 *   <li>Arrêté du 27/04/2021 (ANEF) — modalités dématérialisées de validation ;</li>
 *   <li>CE 16 juillet 2014 n° 375479 — responsabilité de l'administration en cas
 *       d'impossibilité de valider (analogie panne ANEF).</li>
 * </ul>
 */
public final class VlsTsValidationCalculator {

    /** Délai de validation du VLS-TS — 3 mois calendaires (art. R. 311-3). */
    public static final int DELAI_VALIDATION_MOIS = 3;

    /** Seuil (inclusif) en jours restants en deçà duquel le statut bascule en URGENT. */
    static final int SEUIL_URGENT_JOURS = 15;

    /** Borne haute d'ancienneté de la date d'entrée acceptée — 24 mois. */
    static final int MAX_ANCIENNETE_ENTREE_MOIS = 24;

    private static final String BASE_JURIDIQUE =
            "Art. R. 311-3 CESEDA (validation du VLS-TS dans les 3 mois de l'entrée en France) ; "
                    + "R. 311-13 CESEDA (validité du VLS-TS comme titre de séjour) ; "
                    + "arrêté du 27/04/2021 (ANEF, validation dématérialisée)";

    private static final String PROCEDURE_RECOURS =
            "Délai de validation dépassé sans validation effectuée : si l'absence de validation "
                    + "résulte d'une indisponibilité de la plateforme ANEF ou d'une défaillance de "
                    + "l'administration, conserver les preuves d'indisponibilité (captures horodatées, "
                    + "tickets de support) et solliciter une régularisation auprès de l'OFII / de la "
                    + "préfecture ; à défaut, engager un recours pour faute de l'administration "
                    + "(CE 16 juillet 2014 n° 375479) afin de neutraliser l'irrégularité de séjour.";

    private VlsTsValidationCalculator() {
    }

    /** Surcharge utilisant la date du jour système (UTC) comme référence. */
    public static VlsTsValidationResult compute(LocalDate dateEntreeFrance,
                                                VlsTsValidationTypeEnum typeVlsTs,
                                                Boolean validationOFIIEffectuee,
                                                LocalDate dateValidationOFII) {
        return compute(dateEntreeFrance, typeVlsTs, validationOFIIEffectuee, dateValidationOFII,
                LocalDate.now());
    }

    /**
     * Calcule le délai et le statut de validation du VLS-TS.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static VlsTsValidationResult compute(LocalDate dateEntreeFrance,
                                                VlsTsValidationTypeEnum typeVlsTs,
                                                Boolean validationOFIIEffectuee,
                                                LocalDate dateValidationOFII,
                                                LocalDate today) {
        validate(dateEntreeFrance, typeVlsTs, validationOFIIEffectuee, dateValidationOFII, today);

        boolean validee = validationOFIIEffectuee;

        LocalDate dateEcheanceValidation = dateEntreeFrance.plusMonths(DELAI_VALIDATION_MOIS);

        Long joursRestantsValidation;
        VlsTsValidationStatut statut;
        if (validee) {
            // La validation effectuée est prioritaire : plus de décompte.
            joursRestantsValidation = null;
            statut = VlsTsValidationStatut.VALIDE;
        } else {
            long jours = ChronoUnit.DAYS.between(today, dateEcheanceValidation);
            joursRestantsValidation = jours;
            if (jours <= 0) {
                statut = VlsTsValidationStatut.EXPIRE;
            } else if (jours <= SEUIL_URGENT_JOURS) {
                statut = VlsTsValidationStatut.URGENT;
            } else {
                statut = VlsTsValidationStatut.A_VALIDER;
            }
        }

        boolean risqueIrregularite = statut == VlsTsValidationStatut.EXPIRE;
        String procedureRecours = (statut == VlsTsValidationStatut.EXPIRE && !validee)
                ? PROCEDURE_RECOURS
                : null;
        String recommandation = buildRecommandation(statut);

        return new VlsTsValidationResult(
                dateEntreeFrance,
                typeVlsTs,
                validee,
                dateValidationOFII,
                dateEcheanceValidation,
                joursRestantsValidation,
                statut,
                risqueIrregularite,
                procedureRecours,
                recommandation,
                BASE_JURIDIQUE
        );
    }

    private static String buildRecommandation(VlsTsValidationStatut statut) {
        return switch (statut) {
            case VALIDE -> "VLS-TS validé auprès de l'OFII — le visa vaut titre de séjour, suivre "
                    + "l'échéance de renouvellement (checklist F-IM-01).";
            case EXPIRE -> "Délai de validation des 3 mois dépassé sans validation : séjour "
                    + "irrégulier — régulariser sans délai et documenter toute indisponibilité ANEF "
                    + "(recours pour faute de l'administration).";
            case URGENT -> "Échéance des 3 mois imminente — valider le VLS-TS sur la plateforme "
                    + "ANEF (taxe OFII) avant l'expiration.";
            case A_VALIDER -> "Valider le VLS-TS sur la plateforme ANEF dans les 3 mois de l'entrée "
                    + "en France (art. R. 311-3 CESEDA) en s'acquittant de la taxe OFII.";
        };
    }

    private static void validate(LocalDate dateEntreeFrance,
                                 VlsTsValidationTypeEnum typeVlsTs,
                                 Boolean validationOFIIEffectuee,
                                 LocalDate dateValidationOFII,
                                 LocalDate today) {
        if (dateEntreeFrance == null) {
            throw new IllegalArgumentException("dateEntreeFrance est requise");
        }
        if (typeVlsTs == null) {
            throw new IllegalArgumentException("typeVlsTs est requis");
        }
        if (validationOFIIEffectuee == null) {
            throw new IllegalArgumentException("validationOFIIEffectuee est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateEntreeFrance.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateEntreeFrance ne peut pas être dans le futur");
        }
        if (dateEntreeFrance.isBefore(today.minusMonths(MAX_ANCIENNETE_ENTREE_MOIS))) {
            throw new IllegalArgumentException(
                    "dateEntreeFrance est antérieure à " + MAX_ANCIENNETE_ENTREE_MOIS
                            + " mois — VLS-TS hors périmètre de cet outil");
        }
        if (dateValidationOFII != null && dateValidationOFII.isBefore(dateEntreeFrance)) {
            throw new IllegalArgumentException(
                    "dateValidationOFII ne peut pas être antérieure à dateEntreeFrance");
        }
    }
}
