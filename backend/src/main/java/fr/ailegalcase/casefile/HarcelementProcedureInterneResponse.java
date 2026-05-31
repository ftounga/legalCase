package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-218-27 : réponse de l'analyse de conformité de la procédure interne de
 * traitement d'un signalement de harcèlement (art. L.1153-5-1, L.2314-1,
 * L.1152-4, L.4121-1 CT, F-DT-59). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record HarcelementProcedureInterneResponse(
        UUID caseFileId,
        int effectif,
        boolean signalementRecu,
        List<HarcelementChecklistItem> checklist,
        Integer delaiReactionJours,
        HarcelementProcedureInterneDelaiRaisonnable delaiRaisonnable,
        int itemsObligatoiresManquants,
        HarcelementProcedureInterneVerdict statut,
        HarcelementProcedureInterneRisque risqueResponsabiliteEmployeur,
        List<String> consequences,
        String country,
        String baseJuridique
) {}
