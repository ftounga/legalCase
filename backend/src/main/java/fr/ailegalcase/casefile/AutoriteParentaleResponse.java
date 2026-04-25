package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-19-01 : réponse de l'API autorité parentale (art. 372-373 Cciv).
 */
public record AutoriteParentaleResponse(
        UUID caseFileId,
        String regimeExerciceActuel,
        String regimeExerciceDemande,
        String motifChangement,
        boolean dangerCaracterise,
        List<String> preuvesProduites,
        List<Integer> ageEnfants,
        boolean consentementAutreParent,
        boolean interferenceVieEnfant,
        LocalDate dateRequete,
        int scoreEligibilite,
        String verdictProbabiliteAcceptation,
        boolean expertiseRecommandee,
        boolean auditionEnfantsRecommandee,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
