package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-215-03 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/regroupement-10ter-be-analysis}.
 */
public record Regroupement10terBeResponse(
        UUID caseFileId,
        Regroupement10terBeLienFamilialEnum lienFamilial,
        Regroupement10terBeTypeCarteEnum typeCarteRegroupant,
        int revenusMensuelsNetsRegroupant,
        int dureeSejour,
        boolean logementConforme,
        boolean assuranceMaladie,
        boolean menaceOrdrePublic,
        int seuilRessources,
        int differentielRevenus,
        int scoreEligibilite,
        Regroupement10terBeVerdict verdict,
        List<String> criteresNonRemplis,
        String baseJuridique
) {}
