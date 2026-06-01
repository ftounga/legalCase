package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-218-35 : réponse de l'analyse de validité d'un règlement intérieur
 * (art. L.1311-1 à L.1322-4, L.1321-1 et s. CT, F-DT-100). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 */
public record ReglementInterieurValiditeResponse(
        UUID caseFileId,
        int effectif,
        boolean reglementExiste,
        List<ReglementInterieurValiditeChecklistItem> checklist,
        int itemsObligatoiresManquants,
        int clausesInterditesPresentes,
        ReglementInterieurValiditeStatut statut,
        ReglementInterieurOpposabilite opposabilite,
        List<String> consequences,
        String country,
        String baseJuridique
) {}
