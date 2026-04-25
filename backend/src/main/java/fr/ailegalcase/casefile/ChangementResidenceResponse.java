package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-19-03 : réponse de l'API changement de résidence (art. 373-2 al. 3 Cciv).
 */
public record ChangementResidenceResponse(
        UUID caseFileId,
        LocalDate dateChangementPrevu,
        int distanceKm,
        String raisonChangement,
        boolean consentementAutreParent,
        boolean informePrealablement,
        int delaiInformationJours,
        String modeResidenceActuel,
        List<Integer> ageEnfants,
        boolean scolariteImpactee,
        boolean modificationDvhDemandee,
        int scoreAcceptabilite,
        String verdictProbabiliteAcceptation,
        boolean obligationInformationRespectee,
        boolean expertisePsyEnfantRecommandee,
        boolean delaiPreavisLegalOk,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
