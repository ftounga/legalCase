package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-45 : réponse de l'analyse du congé parental d'éducation (art. L.1225-47
 * à L.1225-60 CT, F-DT-78). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record CongeParentalEducationResponse(
        UUID caseFileId,
        CongeParentalEducationStatut statut,
        int ancienneteMois,
        CongeParentalEducationModalite modaliteRetenue,
        int nombreEnfants,
        LocalDate dateNaissanceOuAdoption,
        LocalDate dateFinMax,
        int dureeMaxMois,
        boolean protectionReintegration,
        boolean mentionPreparE,
        List<String> notes,
        String country,
        String baseJuridique
) {}
