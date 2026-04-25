package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-26-01 : réponse de l'API changement d'état civil
 * (art. 60 / 61-3-1 / 61-5 Cciv).
 */
public record ChangementEtatCivilResponse(
        UUID caseFileId,
        String typeChangement,
        String motifInvoque,
        List<String> preuvesProduites,
        boolean majeurDemandeur,
        boolean consentementParental,
        boolean datesDocsConcordants,
        boolean dejaChangeAuparavant,
        LocalDate dateNaissanceDemandeur,
        String departementDeclaration,
        String competenceProcedure,
        int delaiInstructionMoisPrevisionnel,
        int scoreAcceptabilite,
        String verdictAcceptabilite,
        List<String> documentsRequisManquants,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
