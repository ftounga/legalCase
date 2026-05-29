package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-19 — analyseur d'éligibilité à l'aide juridictionnelle (AJ) devant la
 * Cour nationale du droit d'asile (CNDA) et des délais associés.
 *
 * <p>Logique :
 * <ul>
 *   <li>{@code eligibleAJ} : {@code ressourcesMensuellesNettes ≤ PLAFOND_AJ_MENSUEL_EUR} ;</li>
 *   <li>{@code dateEcheanceRecoursCNDA} : {@code dateDecisionOFPRA + 1 mois}, ou
 *       {@code + 15 j} en procédure accélérée (L. 532-4 CESEDA) ;</li>
 *   <li>{@code dateEcheanceDemandeAJ} : {@code dateDecisionOFPRA + 15 j} — la demande
 *       d'AJ doit précéder le recours pour interrompre le délai (R. 532-1 et s.) ;</li>
 *   <li>{@code statut} : NON_ELIGIBLE_RESSOURCES si non éligible ; AJ_DEPOSEE si la
 *       demande est déjà déposée ; HORS_DELAI_AJ si {@code today > dateEcheanceDemandeAJ}
 *       et demande non déposée ; AJ_A_DEMANDER sinon.</li>
 * </ul>
 *
 * <p>Sources :
 * <ul>
 *   <li>Loi n° 91-647 du 10/07/1991 — aide juridictionnelle (art. 2, 4, 9-12) ;</li>
 *   <li>Décret n° 2020-1717 du 28/12/2020 — plafonds AJ (annexe I) ;</li>
 *   <li>L. 532-1 à L. 532-35 CESEDA — procédure CNDA ;</li>
 *   <li>L. 532-4 CESEDA — délai de recours CNDA (1 mois / 15 j accélérée) ;</li>
 *   <li>R. 532-1 à R. 532-7 CESEDA — modalités AJ devant la CNDA.</li>
 * </ul>
 *
 * <p>Outil single-country FR.
 */
public final class AjCndaAnalyzer {

    /** Plafond de ressources mensuelles ouvrant droit à l'AJ (€). */
    // plafond AJ personne seule, décret 2020-1717 — à actualiser
    public static final double PLAFOND_AJ_MENSUEL_EUR = 1082.0;

    /** Délai de demande d'AJ devant la CNDA — 15 jours (R. 532-1 et s.). */
    static final int DELAI_DEMANDE_AJ_JOURS = 15;

    /** Délai de recours CNDA réduit en procédure accélérée — 15 jours (L. 532-4). */
    static final int DELAI_RECOURS_ACCELERE_JOURS = 15;

    private static final String BASE_JURIDIQUE =
            "Loi n° 91-647 du 10/07/1991 (aide juridictionnelle, art. 2, 4, 9-12) ; "
                    + "décret n° 2020-1717 du 28/12/2020 (plafonds AJ) ; "
                    + "L. 532-4 CESEDA (délai de recours CNDA — 1 mois, 15 j en procédure accélérée) ; "
                    + "R. 532-1 à R. 532-7 CESEDA (modalités AJ devant la CNDA)";

    private AjCndaAnalyzer() {
    }

    /** Surcharge utilisant la date du jour système (UTC) comme référence. */
    public static AjCndaResult analyze(LocalDate dateDecisionOFPRA,
                                       double ressourcesMensuellesNettes,
                                       boolean procedureAcceleree,
                                       boolean demandeAJDeposee,
                                       LocalDate dateDepotAJ) {
        return analyze(dateDecisionOFPRA, ressourcesMensuellesNettes, procedureAcceleree,
                demandeAJDeposee, dateDepotAJ, LocalDate.now());
    }

    /**
     * Calcule l'éligibilité, les délais et le statut de la demande d'AJ CNDA.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static AjCndaResult analyze(LocalDate dateDecisionOFPRA,
                                       double ressourcesMensuellesNettes,
                                       boolean procedureAcceleree,
                                       boolean demandeAJDeposee,
                                       LocalDate dateDepotAJ,
                                       LocalDate today) {
        validate(dateDecisionOFPRA, today);

        boolean eligibleAJ = ressourcesMensuellesNettes <= PLAFOND_AJ_MENSUEL_EUR;

        LocalDate dateEcheanceRecoursCNDA = procedureAcceleree
                ? dateDecisionOFPRA.plusDays(DELAI_RECOURS_ACCELERE_JOURS)
                : dateDecisionOFPRA.plusMonths(1);
        LocalDate dateEcheanceDemandeAJ = dateDecisionOFPRA.plusDays(DELAI_DEMANDE_AJ_JOURS);

        AjCndaStatut statut;
        if (!eligibleAJ) {
            statut = AjCndaStatut.NON_ELIGIBLE_RESSOURCES;
        } else if (demandeAJDeposee) {
            statut = AjCndaStatut.AJ_DEPOSEE;
        } else if (today.isAfter(dateEcheanceDemandeAJ)) {
            statut = AjCndaStatut.HORS_DELAI_AJ;
        } else {
            statut = AjCndaStatut.AJ_A_DEMANDER;
        }

        List<String> piecesAJ = buildPieces();
        String recommandation = buildRecommandation(statut, procedureAcceleree);

        return new AjCndaResult(
                dateDecisionOFPRA,
                ressourcesMensuellesNettes,
                procedureAcceleree,
                demandeAJDeposee,
                dateDepotAJ,
                eligibleAJ,
                dateEcheanceRecoursCNDA,
                dateEcheanceDemandeAJ,
                procedureAcceleree,
                statut,
                piecesAJ,
                recommandation,
                BASE_JURIDIQUE
        );
    }

    private static List<String> buildPieces() {
        return List.of(
                "Formulaire Cerfa de demande d'aide juridictionnelle (n° 16146*03)",
                "Justificatif de ressources (avis d'imposition / attestation de non-imposition / justificatifs de revenus)",
                "Notification de la décision de l'OFPRA"
        );
    }

    private static String buildRecommandation(AjCndaStatut statut, boolean procedureAcceleree) {
        String delaiRecours = procedureAcceleree
                ? "15 jours (procédure accélérée, L. 532-4 CESEDA)"
                : "1 mois (procédure normale, L. 532-4 CESEDA)";
        return switch (statut) {
            case AJ_A_DEMANDER -> "Demandeur éligible à l'AJ — déposer la demande d'aide "
                    + "juridictionnelle dans les 15 jours de la notification OFPRA, avant le "
                    + "recours CNDA (délai de recours : " + delaiRecours + "). Le dépôt de la "
                    + "demande d'AJ interrompt le délai de recours (R. 532-1 et s. CESEDA).";
            case AJ_DEPOSEE -> "Demande d'AJ déjà déposée — suivre l'instruction par le bureau "
                    + "d'aide juridictionnelle et préparer le recours CNDA (délai de recours : "
                    + delaiRecours + ").";
            case HORS_DELAI_AJ -> "Délai de demande d'AJ (15 jours) dépassé — déposer la demande "
                    + "sans délai ; un dépôt tardif reste possible mais n'interrompt plus le délai "
                    + "de recours. Sécuriser en priorité le recours CNDA (délai : " + delaiRecours + ").";
            case NON_ELIGIBLE_RESSOURCES -> "Ressources supérieures au plafond d'AJ (≈ "
                    + (int) PLAFOND_AJ_MENSUEL_EUR + " €/mois, décret 2020-1717) — l'aide "
                    + "juridictionnelle de plein droit n'est pas ouverte. Vérifier une éventuelle "
                    + "AJ partielle ou la prise en charge par un tiers, et respecter le délai de "
                    + "recours CNDA (" + delaiRecours + ").";
        };
    }

    private static void validate(LocalDate dateDecisionOFPRA, LocalDate today) {
        if (dateDecisionOFPRA == null) {
            throw new IllegalArgumentException("dateDecisionOFPRA est requise");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateDecisionOFPRA.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateDecisionOFPRA ne peut pas être dans le futur");
        }
    }
}
