package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-02 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-longue-carriere}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les autres
 * outils décisionnels BE de la F-213 / F-219 (miroir
 * {@link LicenciementBeProtectionDelegueeResponse} SF-213-08).</p>
 */
public record RccBeLongueCarriereResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        Integer ageFinContrat,
        Integer anneesCarriereTotale,
        LocalDate dateFinContrat,
        boolean licenciementEffectif,
        BigDecimal remunerationNetteMensuelleReference,
        BigDecimal allocationChomageMensuelleEstimee,

        // Verdict et conditions
        RccBeLongueCarriereVerdict verdict,
        boolean eligible,
        boolean conditionAgeRemplie,
        boolean conditionCarriereRemplie,
        boolean conditionLicenciementRemplie,

        // Indemnité complémentaire indicative
        BigDecimal indemniteComplementaireMensuelle,

        // Synthèse + métadonnées légales
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
