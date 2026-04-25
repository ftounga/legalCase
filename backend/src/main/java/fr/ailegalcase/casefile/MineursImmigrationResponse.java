package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-IM-19-01 : réponse HTTP pour l'analyse d'éligibilité mineur étranger.
 */
public record MineursImmigrationResponse(
        UUID caseFileId,
        String country,
        String dispositifVise,
        String dispositifRecommande,
        LocalDate dateNaissance,
        LocalDate dateEntreeFrance,
        boolean parentRegulier,
        boolean isolementAvere,
        boolean motifOrdrePublic,
        String nationalite,
        int ageAnnees,
        String verdictEligibilite,
        List<String> criteresNonRemplis,
        List<String> documentsRequis,
        int delaiInstructionMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
