package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-214-05 : réponse de l'analyse d'éligibilité à la VPF liens personnels et
 * familiaux L. 423-23 CESEDA (art. 8 CEDH).
 */
public record VpfLiensPersonnelsResponse(
        UUID caseFileId,
        int dureeResidenceFranceMois,
        boolean entreeEnFranceMineur,
        boolean enfantsEnFrance,
        boolean conjointEnFrance,
        boolean parentsEnFrance,
        String situationFamilialeAlEtranger,
        String niveauIntegration,
        boolean ancienneConvictionPenale,
        String country,
        int score,
        boolean dureeResidenceRemplie,
        String verdict,
        List<String> chipsCriteresNonRemplis,
        List<String> recommandations,
        String baseJuridique
) {}
