package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrdonnanceProtectionResponse(
        UUID caseFileId,
        LocalDate dateRequete,
        List<String> violencesAlleguees,
        List<String> preuvesViolences,
        boolean dangerImmediat,
        boolean presenceEnfants,
        List<Integer> ageEnfants,
        boolean logementCommun,
        boolean victimeFinanciairementDependante,
        boolean demandeurDejaProtege,
        List<String> demandeMesures,
        int scoreVraisemblance,
        String verdictProbabiliteOctroi,
        List<String> mesuresRecommandees,
        int delaiTraitementJoursPrevisionnel,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
