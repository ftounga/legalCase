package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record RegimeAlgerienResponse(
        UUID caseFileId,
        String country,
        String voieDemande,
        String voieRecommandee,
        String verdictRecevabilite,
        String titreApplicable,
        int dureeTitreAnnees,
        List<String> criteresNonRemplis,
        List<String> documentsRequis,
        int delaiInstructionMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
