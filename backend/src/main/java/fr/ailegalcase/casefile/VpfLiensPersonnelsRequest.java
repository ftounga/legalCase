package fr.ailegalcase.casefile;

/**
 * SF-214-05 : requête POST pour l'analyse d'éligibilité à la VPF liens
 * personnels et familiaux L. 423-23 CESEDA (art. 8 CEDH). Outil single-country FR.
 */
public record VpfLiensPersonnelsRequest(
        Integer dureeResidenceFranceMois,
        Boolean entreeEnFranceMineur,
        Boolean enfantsEnFrance,
        Boolean conjointEnFrance,
        Boolean parentsEnFrance,
        String situationFamilialeAlEtranger,
        String niveauIntegration,
        Boolean ancienneConvictionPenale
) {}
