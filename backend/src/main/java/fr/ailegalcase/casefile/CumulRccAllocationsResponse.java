package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-04 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/cumul-rcc-allocations}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-213 / F-219 (miroir
 * {@link RccBeLongueCarriereResponse} SF-219-02).</p>
 */
public record CumulRccAllocationsResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        LocalDate dateNaissance,
        LocalDate dateDebutRcc,
        BigDecimal allocationChomageMensuelle,
        BigDecimal indemniteComplementaireMensuelle,
        BigDecimal remunerationNetteReference,
        Integer anneesCarriereTotale,
        CumulRccAllocationsActiviteAutorisee activiteComplementaire,
        Integer ageLegalPension,

        // Outputs
        CumulRccAllocationsVerdict verdict,
        boolean conforme,
        int ageADateDebutRcc,
        BigDecimal cumulMensuelTotal,
        boolean plafondRespecte,
        BigDecimal montantReductionIndemnite,
        CumulRccAllocationsDisponibiliteRegime regimeDisponibilite,
        boolean ageLegalPensionAtteint,

        // Synthèse + métadonnées légales
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
