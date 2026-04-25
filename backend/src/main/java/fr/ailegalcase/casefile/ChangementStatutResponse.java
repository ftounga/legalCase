package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ChangementStatutResponse(
        UUID caseFileId,
        String country,
        String titreActuel,
        String titreEnvisage,
        String nouveauTitreEnvisage,
        int dureeRestanteMois,
        boolean documentJustificatifFourni,
        BigDecimal remunerationContratEur,
        boolean casierJudiciaireVierge,
        String verdictTransition,
        List<String> documentsRequis,
        List<String> risqueRefus,
        int delaiInstructionMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
