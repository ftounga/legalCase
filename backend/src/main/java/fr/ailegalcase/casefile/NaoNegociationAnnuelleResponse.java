package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-29 : réponse de l'analyse de conformité de la négociation annuelle
 * obligatoire (NAO, art. L.2242-1 à L.2242-8 CT, F-DT-66). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 */
public record NaoNegociationAnnuelleResponse(
        UUID caseFileId,
        int effectif,
        boolean delegueSyndicalPresent,
        boolean applicable,
        List<NaoChecklistItem> checklist,
        int periodiciteMois,
        LocalDate dateProchaineEcheance,
        Integer joursAvantEcheance,
        NaoStatutEcheance statutEcheance,
        int itemsObligatoiresManquants,
        NaoNegociationAnnuelleStatut statut,
        NaoRisqueEntrave risqueEntrave,
        List<String> consequences,
        String country,
        String baseJuridique
) {}
