package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-218-41 : réponse de l'analyse de conformité aux obligations d'épargne
 * salariale (intéressement / participation / dispositif de partage de la valeur
 * — F-DT-53). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record EpargneSalarialeConformiteResponse(
        UUID caseFileId,
        int effectif,
        boolean accordParticipationPresent,
        boolean accordInteressementPresent,
        boolean beneficeNetFiscalPositif3Ans,
        boolean entreprise11a49,
        List<EpargneSalarialeConformiteItem> checklist,
        int obligationsApplicables,
        int obligationsNonRemplies,
        EpargneSalarialeConformiteStatut statut,
        List<String> notes,
        String country,
        String baseJuridique
) {}
