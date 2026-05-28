package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-219-11 : résultat brut produit par {@link
 * CongeEducationPayeRegionChecker} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * CongeEducationPayeRegionService} dans {@link
 * CongeEducationPayeRegionResponse}).</p>
 *
 * <p>Outputs dérivés : verdict (éligibilité), plafond régional
 * applicable selon type de formation, heures de congé allouées
 * (proratisées si temps partiel), nom du décret régional applicable.</p>
 */
public record CongeEducationPayeRegionResult(
        // Inputs (snapshot)
        CongeEducationPayeRegionRegion region,
        CongeEducationPayeRegionTypeFormation typeFormation,
        int heuresFormationAnnuelles,
        BigDecimal tauxOccupation,
        boolean publicFragilise,
        boolean formationAgreeeRegion,

        // Outputs
        CongeEducationPayeRegionVerdict verdict,
        int plafondRegionalHeures,
        int heuresCongeAllouees,
        String regimeApplicable,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
