package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-29 : reponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/at-mp-rente-capital-be}.
 *
 * <p>Snapshot complet (inputs + outputs) - pattern uniforme avec les
 * autres outils decisionnels BE de la F-219 (miroir
 * {@link MpFedrisReconnaissanceResponse} SF-219-28).</p>
 */
public record AtMpRenteCapitalBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        AtMpRenteCapitalBeOrigine origine,
        AtMpRenteCapitalBeStatutReconnaissance statutReconnaissance,
        LocalDate dateConsolidation,
        BigDecimal tauxIpp,
        BigDecimal remunerationBaseAnnuelle,
        LocalDate dateNaissance,
        boolean demandeConversionPartielle,
        LocalDate dateDemandeConversion,
        Integer ageConsolidationOverride,

        // Outputs
        AtMpRenteCapitalBeVerdict verdict,
        boolean capitalisationDoffice,
        boolean conversionPartiellePossible,
        Integer ageConsolidationCalcule,
        BigDecimal coefficientAge,
        BigDecimal renteAnnuelle,
        BigDecimal capitalCapitalisation,
        BigDecimal capitalConversionPartielle,
        BigDecimal renteResiduelleApresConversion,

        // Synthese + metadonnees legales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
