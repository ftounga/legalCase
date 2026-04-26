package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-18-05 : réponse de l'endpoint
 * {@code /recherche-paternite-analysis}.
 */
public record RecherchePaterniteResponse(
        UUID caseFileId,
        RecherchePaterniteCalculator.QualiteDuDemandeur qualiteDuDemandeur,
        RecherchePaterniteCalculator.VerdictRecevabilite verdictRecevabilite,
        int scoreRecevabilite,
        int delaiPrescriptionAns,
        long delaiPrescriptionRestantMois,
        boolean expertiseAdnRecommandee,
        boolean presomptionRefusADN,
        List<String> risquesRefus,
        List<String> documentsRequis,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
