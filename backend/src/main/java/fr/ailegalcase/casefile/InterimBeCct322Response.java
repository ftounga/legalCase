package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-14 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/interim-be-cct-322}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link FlexiJobBeResponse} SF-219-12).</p>
 */
public record InterimBeCct322Response(
        UUID caseFileId,

        // Inputs (snapshot)
        LocalDate dateDebutMission,
        InterimBeCct322MotifMission motifMission,
        boolean remplacementGreveOuLockout,
        int dureeTotaleMissionJours,
        int dureeMaxLegaleJours,
        BigDecimal salaireHoraireIntimaireBrut,
        BigDecimal salaireHoraireReferenceBrut,
        boolean contratEcritSigne,
        boolean dimonaDeclareeParEti,

        // Outputs
        InterimBeCct322Verdict verdict,
        boolean motifAutorise,
        boolean dureeRespectee,
        boolean pariteRespectee,
        boolean formalismeRespecte,
        int joursExcedentaires,
        BigDecimal ecartPariteSalariale,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
