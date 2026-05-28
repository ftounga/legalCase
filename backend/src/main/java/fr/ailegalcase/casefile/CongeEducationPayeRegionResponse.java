package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * SF-219-11 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/conge-education-paye-region}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir {@link
 * DelegueSyndicalCct5Response} SF-219-10).</p>
 */
public record CongeEducationPayeRegionResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        CongeEducationPayeRegionRegion region,
        CongeEducationPayeRegionTypeFormation typeFormation,
        int heuresFormationAnnuelles,
        BigDecimal tauxOccupation,
        boolean publicFragilise,
        boolean formationAgreeeRegion,

        // Outputs
        CongeEducationPayeRegionVerdict verdict,
        int plafondRegionalHeures,
        int heuresCongeAllouees,
        String regimeApplicable,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
