package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-215-17 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/annexe13quinquies-be-analysis}.
 */
public record Annexe13quinquiesBeResponse(
        UUID caseFileId,
        LocalDate dateNotificationAnnexe,
        Annexe13quinquiesBeMotifEnum motifInterdictionEntree,
        boolean precedentSejour,
        int dureeInterdiction,
        LocalDate dateFinInterdiction,
        LocalDate datePossibleLevePrecoce,
        LocalDate dateLimiteRecoursAnnulation,
        long joursRestantsRecours,
        boolean recoursForme,
        LocalDate dateRecours,
        Annexe13quinquiesBeStatut statutRecours,
        List<String> conditionsLevee,
        String recommandation,
        String baseJuridique
) {}
