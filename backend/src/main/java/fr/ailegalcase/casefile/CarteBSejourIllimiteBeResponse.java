package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-221-02 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/carte-b-sejour-illimite-be-analysis}.
 */
public record CarteBSejourIllimiteBeResponse(
        UUID caseFileId,
        LocalDate dateDebutSejourRegulier,
        boolean sejourIninterrompu,
        boolean absencesSuperieuresLimites,
        boolean motifSejourStable,
        boolean ordrePublicRisque,
        CarteBSejourIllimiteBeVerdict verdict,
        int dureeSejourMois,
        int moisRestants,
        List<String> basesJuridiques,
        List<String> messages
) {}
