package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-17 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/clause-ecolage-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-213 / F-219 (miroir
 * {@link ClauseNonConcurrenceBeResponse} SF-213-01).</p>
 */
public record ClauseEcolageBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        ClauseEcolageBeTypeFormation typeFormation,
        boolean clauseEcriteAvantEntreeFormation,
        BigDecimal coutReelFormationEuros,
        BigDecimal rmmmgMensuelEuros,
        int dureeEfficaciteMois,
        LocalDate dateFinFormation,
        LocalDate dateDepartTravailleur,
        ClauseEcolageBeMotifDepart motifDepart,

        // Outputs
        ClauseEcolageBeVerdict verdict,
        BigDecimal coutMinLegalEuros,
        int moisEcoulesDepuisFinFormation,
        int tierDureeAuDepart,
        BigDecimal quotiteDueRatio,
        BigDecimal montantBrutDueEuros,
        BigDecimal plafond80Euros,
        BigDecimal montantDuFinalEuros,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
