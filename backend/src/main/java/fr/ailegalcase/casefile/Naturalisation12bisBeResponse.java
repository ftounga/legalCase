package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-215-07 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/naturalisation-12bis-be-analysis}.
 */
public record Naturalisation12bisBeResponse(
        UUID caseFileId,
        int dureeSejour,
        NaturalisationBeTypeSejourEnum typeSejour,
        NaturalisationBeNiveauLangueEnum niveauLangue,
        boolean preuveIntegration,
        boolean preuveEmploi,
        boolean menaceOrdrePublic,
        boolean condamnationPenale,
        boolean voie5Ans,
        boolean voie10Ans,
        Naturalisation12bisBeVoieEligible voieEligible,
        int dureeManquante,
        List<String> criteresNonRemplis,
        String procedureReference,
        String baseJuridique
) {}
