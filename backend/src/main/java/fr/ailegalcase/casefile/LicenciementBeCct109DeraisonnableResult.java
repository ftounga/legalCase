package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-213-10 : résultat brut produit par
 * {@link LicenciementBeCct109DeraisonnableCalculator} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link LicenciementBeCct109DeraisonnableService} dans
 * {@link LicenciementBeCct109DeraisonnableResponse}).</p>
 *
 * <p>{@code indemniteCct109} vaut {@code 0} lorsque {@code echelonCct109} est
 * {@link LicenciementBeCct109EchelonEnum#NON_DERAISONNABLE} (motif valable
 * prouvé + procédures respectées). {@code avertissement} reste <b>toujours
 * non-null</b> et rappelle le cumul avec l'ICP.</p>
 */
public record LicenciementBeCct109DeraisonnableResult(
        Boolean motifCommunique,
        LicenciementBeCct109MotifLieAPersonneEnum motifLieAPersonne,
        boolean discriminationSuspectee,
        boolean represaillesSuspectees,
        boolean proceduresRespectees,
        BigDecimal remunerationHebdomadaireBrute,
        String argumentsPatronal,

        LicenciementBeCct109EchelonEnum echelonCct109,
        int nombreSemaines,
        BigDecimal indemniteCct109,
        String justificationEchelon,
        boolean cumulAvecIcp,
        String baseJuridique,
        String avertissement
) {
}
