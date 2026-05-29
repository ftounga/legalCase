package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-41 : réponse de l'analyse de validité d'un retrait de titre de séjour
 * pour fraude (art. L. 412-7 CESEDA). Outil single-country FR.
 */
public record RetraitTitreFraudeResponse(
        UUID caseFileId,
        LocalDate dateRetrait,
        RetraitTitreFraudeMotifEnum motifRetrait,
        boolean miseEnDemeurePrealable,
        LocalDate dateMiseEnDemeure,
        List<String> vicesDeProcedure,
        List<String> motifsContestation,
        LocalDate delaiRecoursTA,
        RetraitTitreFraudeStatut statut,
        boolean recoursPossible,
        String recommandation,
        String country,
        String baseJuridique
) {}
