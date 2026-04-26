package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record AsileAvanceResponse(
        UUID caseFileId,
        String country,
        String dispositifAsile,
        String dispositifLibelle,
        String verdictRecevabilite,
        double delaiInstructionMois,
        String recoursPossible,
        List<String> documentsRequis,
        List<String> risqueRefus,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
