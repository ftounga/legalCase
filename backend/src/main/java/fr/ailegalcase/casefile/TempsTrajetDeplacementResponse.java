package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-218-51 : réponse de l'outil "Temps de trajet / déplacement professionnel"
 * (art. L.3121-4 CT ; CJUE C-266/14, F-DT-81). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record TempsTrajetDeplacementResponse(
        UUID caseFileId,
        TempsTrajetQualification qualification,
        TypeTrajet typeTrajet,
        int tempsTrajetQuotidienMinutes,
        int tempsTrajetNormalMinutes,
        boolean contrepartiePrevueAccord,
        boolean contrepartieDue,
        int depassementMinutes,
        String base,
        List<String> notes,
        String country,
        String baseJuridique
) {}
