package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-220-01 : réponse de l'analyse du régime franco-tunisien (accord 17/03/1988).
 */
public record RegimeTunisienResponse(
        UUID caseFileId,
        String categorie,
        Integer dureeSejourEnvisageeMois,
        boolean titreEnCours,
        boolean dejaResident,
        String country,
        String regime,
        List<String> particularitesApplicables,
        List<String> basesJuridiques,
        boolean renvoiDroitCommun,
        List<String> messages
) {}
