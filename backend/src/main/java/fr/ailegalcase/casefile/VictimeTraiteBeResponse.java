package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-221-06 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/victime-traite-be-analysis}.
 */
public record VictimeTraiteBeResponse(
        UUID caseFileId,
        VictimeTraiteBePhase phaseProcedure,
        boolean ruptureAvecReseau,
        boolean cooperationJudiciaire,
        boolean accompagnementCentreSpecialise,
        LocalDate dateDebutAccompagnement,
        VictimeTraiteBeVerdict verdict,
        String etapeProcedure,
        List<String> basesJuridiques,
        List<String> messages
) {}
