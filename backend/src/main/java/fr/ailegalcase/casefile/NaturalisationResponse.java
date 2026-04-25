package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record NaturalisationResponse(
        UUID caseFileId,
        String country,
        String voieNaturalisation,
        String voieRecommandee,
        String verdictRecevabilite,
        List<String> criteresNonRemplis,
        List<String> documentsAFournir,
        int delaiInstructionMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
