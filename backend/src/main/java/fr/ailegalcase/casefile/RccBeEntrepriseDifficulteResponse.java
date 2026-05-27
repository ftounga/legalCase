package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-03 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-entreprise-difficulte}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les autres
 * outils décisionnels BE de la F-213 / F-219 (miroir SF-219-01 / SF-219-02).</p>
 */
public record RccBeEntrepriseDifficulteResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        RccBeEntrepriseDifficulteTypeEnum typeReconnaissance,
        Integer ageReduitPlan,
        Integer ageFinContrat,
        Integer anneesCarriereTotale,
        Integer anneesAncienneteSecteur,
        LocalDate dateFinContrat,
        boolean licenciementEffectif,
        BigDecimal remunerationNetteMensuelleReference,
        BigDecimal allocationChomageMensuelleEstimee,

        // Verdict et conditions
        RccBeEntrepriseDifficulteVerdict verdict,
        boolean eligible,
        boolean conditionReconnaissanceRemplie,
        boolean conditionAgeRemplie,
        boolean conditionCarriereRemplie,
        boolean conditionAncienneteRemplie,
        boolean conditionLicenciementRemplie,

        // Indemnité complémentaire indicative
        BigDecimal indemniteComplementaireMensuelle,

        // Synthèse + métadonnées légales
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
