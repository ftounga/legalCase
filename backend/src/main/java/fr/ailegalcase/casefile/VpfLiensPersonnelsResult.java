package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-214-05 : résultat de l'analyse d'éligibilité à la VPF liens personnels et
 * familiaux L. 423-23 CESEDA (art. 8 CEDH). Outil single-country FR.
 */
public record VpfLiensPersonnelsResult(
        int dureeResidenceFranceMois,
        boolean entreeEnFranceMineur,
        boolean enfantsEnFrance,
        boolean conjointEnFrance,
        boolean parentsEnFrance,
        String situationFamilialeAlEtranger,
        String niveauIntegration,
        boolean ancienneConvictionPenale,
        int score,
        boolean dureeResidenceRemplie,
        String verdict,
        List<String> chipsCriteresNonRemplis,
        List<String> recommandations,
        String baseJuridique
) {}
