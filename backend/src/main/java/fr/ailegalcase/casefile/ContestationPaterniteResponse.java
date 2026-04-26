package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-18-03 : réponse de l'endpoint
 * {@code /contestation-paternite-analysis}.
 */
public record ContestationPaterniteResponse(
        UUID caseFileId,
        ContestationPaterniteCalculator.QualiteAagir qualiteAagir,
        ContestationPaterniteCalculator.VerdictRecevabilite verdictRecevabilite,
        int scoreRecevabilite,
        int delaiPrescriptionAns,
        long delaiPrescriptionRestantMois,
        boolean expertiseAdnRecommandee,
        List<String> risquesRefus,
        List<String> documentsRequis,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
