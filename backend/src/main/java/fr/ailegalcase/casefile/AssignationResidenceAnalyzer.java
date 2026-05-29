package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * SF-214-35 — analyseur de validité d'une mesure d'assignation à résidence
 * (alternative à la rétention administrative, art. L. 731-1 CESEDA). Outil
 * single-country FR.
 *
 * <p>L'assignation à résidence est prononcée pour 45 jours, renouvelable deux
 * fois, soit une durée maximale cumulée de <b>135 jours</b> (L. 731-1). Au-delà,
 * la mesure n'a plus de base légale. L'analyseur calcule l'échéance, indique si
 * un renouvellement reste possible et fournit les moyens de contestation devant
 * le tribunal administratif (recours dans les 48 h).
 *
 * <p>Sources :
 * <ul>
 *   <li>L. 731-1 CESEDA — assignation à résidence, durée 45 j renouvelable ;</li>
 *   <li>L. 732-1 et s. CESEDA — obligations et contrôle ;</li>
 *   <li>L. 733-1 et s. CESEDA — recours devant le tribunal administratif ;</li>
 *   <li>CE 13 mars 2006 n° 286669 — conditions de validité de l'assignation.</li>
 * </ul>
 */
public final class AssignationResidenceAnalyzer {

    /** Durée maximale cumulée d'une assignation à résidence — 135 j (45 j × 3, L. 731-1). */
    public static final int DUREE_TOTALE_AUTORISEE_JOURS = 135;

    /** Délai de saisine du tribunal administratif contre l'assignation — 48 h. */
    static final int DELAI_RECOURS_TA_HEURES = 48;

    /** Seuil (exclusif) en jours restants en deçà duquel le statut bascule en EXPIRATION_PROCHE. */
    static final int SEUIL_EXPIRATION_PROCHE_JOURS = 15;

    private static final String BASE_JURIDIQUE =
            "Art. L. 731-1 CESEDA (assignation à résidence, 45 j renouvelable 2 fois, "
                    + "soit 135 j maximum) ; L. 732-1 et s. CESEDA (obligations de présentation) ; "
                    + "L. 733-1 et s. CESEDA (recours devant le tribunal administratif) ; "
                    + "CE 13 mars 2006 n° 286669 (conditions de validité)";

    private static final List<String> MOTIFS_CONTESTATION = List.of(
            "Défaut ou insuffisance de motivation de l'arrêté d'assignation à résidence",
            "Absence de risque de fuite caractérisé justifiant la mesure",
            "Atteinte disproportionnée aux droits de l'intéressé (proportionnalité)",
            "Vice de procédure (compétence, contradictoire, notification, droits de la défense)");

    private AssignationResidenceAnalyzer() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static AssignationResidenceResult analyze(LocalDate dateNotificationAssignation,
                                                     int dureeAssignationJours,
                                                     AssignationResidenceMotifEnum motifAssignation,
                                                     String obligationsPresentation) {
        return analyze(dateNotificationAssignation, dureeAssignationJours, motifAssignation,
                obligationsPresentation, LocalDate.now());
    }

    /**
     * Analyse la validité de l'assignation et calcule l'échéance et le statut.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static AssignationResidenceResult analyze(LocalDate dateNotificationAssignation,
                                                     int dureeAssignationJours,
                                                     AssignationResidenceMotifEnum motifAssignation,
                                                     String obligationsPresentation,
                                                     LocalDate today) {
        validate(dateNotificationAssignation, dureeAssignationJours, motifAssignation, today);

        LocalDate dateEcheanceAssignation =
                dateNotificationAssignation.plusDays(dureeAssignationJours);

        boolean renouvellementPossible = dureeAssignationJours < DUREE_TOTALE_AUTORISEE_JOURS;

        long joursRestants = ChronoUnit.DAYS.between(today, dateEcheanceAssignation);
        AssignationResidenceStatut statut;
        if (joursRestants < 0) {
            statut = AssignationResidenceStatut.EXPIRE;
        } else if (joursRestants < SEUIL_EXPIRATION_PROCHE_JOURS) {
            statut = AssignationResidenceStatut.EXPIRATION_PROCHE;
        } else {
            statut = AssignationResidenceStatut.EN_COURS;
        }

        return new AssignationResidenceResult(
                dateNotificationAssignation,
                dureeAssignationJours,
                motifAssignation,
                obligationsPresentation,
                DUREE_TOTALE_AUTORISEE_JOURS,
                dateEcheanceAssignation,
                renouvellementPossible,
                MOTIFS_CONTESTATION,
                true,
                DELAI_RECOURS_TA_HEURES,
                statut,
                buildRecommandation(statut, renouvellementPossible),
                BASE_JURIDIQUE);
    }

    private static String buildRecommandation(AssignationResidenceStatut statut,
                                              boolean renouvellementPossible) {
        return switch (statut) {
            case EXPIRE -> "Échéance de l'assignation à résidence dépassée : la mesure n'a plus "
                    + "de base légale — vérifier l'absence de renouvellement régulier et, le cas "
                    + "échéant, demander la mainlevée au tribunal administratif.";
            case EXPIRATION_PROCHE -> renouvellementPossible
                    ? "Échéance imminente (< 15 j) : anticiper un éventuel renouvellement (plafond "
                            + "de 135 j non atteint) et préparer la contestation devant le TA si la "
                            + "mesure est reconduite."
                    : "Échéance imminente (< 15 j) et plafond légal de 135 j atteint : la mesure ne "
                            + "peut plus être renouvelée — préparer la sortie du dispositif.";
            case EN_COURS -> "Assignation à résidence en cours : vérifier la motivation et la "
                    + "proportionnalité de l'arrêté ; un recours devant le tribunal administratif "
                    + "reste ouvert (48 h) pour en contester la validité.";
        };
    }

    private static void validate(LocalDate dateNotificationAssignation,
                                 int dureeAssignationJours,
                                 AssignationResidenceMotifEnum motifAssignation,
                                 LocalDate today) {
        if (dateNotificationAssignation == null) {
            throw new IllegalArgumentException("dateNotificationAssignation est requise");
        }
        if (motifAssignation == null) {
            throw new IllegalArgumentException("motifAssignation est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dureeAssignationJours <= 0) {
            throw new IllegalArgumentException(
                    "dureeAssignationJours doit être strictement positive");
        }
        if (dureeAssignationJours > DUREE_TOTALE_AUTORISEE_JOURS) {
            throw new IllegalArgumentException(
                    "dureeAssignationJours (" + dureeAssignationJours + ") dépasse le maximum légal de "
                            + DUREE_TOTALE_AUTORISEE_JOURS + " jours (L. 731-1 CESEDA)");
        }
    }
}
