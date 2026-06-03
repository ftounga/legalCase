package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-221-03 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/residence-longue-duree-ue-be-analysis}.
 */
public record ResidenceLongueDureeUeBeResponse(
        UUID caseFileId,
        LocalDate dateDebutSejourLegal,
        boolean sejourLegalIninterrompu,
        boolean ressourcesStablesSuffisantes,
        boolean assuranceMaladie,
        boolean conditionIntegrationRemplie,
        boolean absencesHorsUeExcessives,
        ResidenceLongueDureeUeBeVerdict verdict,
        int dureeSejourMois,
        int moisRestants,
        List<String> conditionsManquantes,
        List<String> basesJuridiques,
        List<String> messages
) {}
