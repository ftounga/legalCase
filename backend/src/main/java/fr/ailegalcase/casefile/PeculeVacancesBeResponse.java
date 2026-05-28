package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-20 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/pecule-vacances-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link Semaine4JoursBeResponse} SF-219-18,
 * {@link ClauseEcolageBeResponse} SF-219-17).</p>
 */
public record PeculeVacancesBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        PeculeVacancesBeStatut statut,
        PeculeVacancesBeTypeCalcul typeCalcul,
        BigDecimal remunerationBruteAnnuelleExerciceEur,
        BigDecimal remunerationMensuelleBruteEur,
        BigDecimal joursCongesPris,
        boolean peculeDejaPaye,
        BigDecimal remunerationBruteAnnuelleExercicePrecedentEur,
        LocalDate dateSortie,
        LocalDate dateFinContrat,
        LocalDate dateReclamation,

        // Outputs calculés
        BigDecimal montantPeculeSimpleEur,
        BigDecimal montantDoublePeculeEur,
        BigDecimal montantPeculeDepartEur,
        BigDecimal montantTotalDuEur,
        BigDecimal fractionLegaleAppliquee,
        boolean prescrit,
        long joursDepuisFaitGenerateur,

        // Verdict + métadonnées légales
        PeculeVacancesBeVerdict verdict,
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
