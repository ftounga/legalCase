package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record PmaGpaBioethiqueResponse(
        UUID caseFileId,
        String dispositif,
        String verdictRecevabilite,
        List<String> proceduresAFollow,
        String risqueRefus,
        List<String> documentsRequis,
        int delaiInstructionMois,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
