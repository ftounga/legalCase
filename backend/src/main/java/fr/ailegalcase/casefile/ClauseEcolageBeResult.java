package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-17 : résultat brut produit par {@link ClauseEcolageBeValidator}
 * — fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * ClauseEcolageBeService} dans {@link ClauseEcolageBeResponse}).</p>
 *
 * <p>Outputs dérivés :
 * <ul>
 *   <li>{@code coutMinLegalEuros} = ½ × RMMMG (seuil de validité du
 *       coût réel) ;</li>
 *   <li>{@code tierDureeAuDepart} = 1, 2, 3 ou 4 (au-delà — la clause
 *       est expirée) — détermine la quotité due ;</li>
 *   <li>{@code quotiteDueRatio} = 1.00, 0.6667, 0.3333 ou 0 selon le
 *       tiers ;</li>
 *   <li>{@code montantBrutDueEuros} = quotité × coût réel ;</li>
 *   <li>{@code plafond80Euros} = 0.80 × coût réel ;</li>
 *   <li>{@code montantDuFinalEuros} = min(montantBrutDue, plafond 80 %).</li>
 * </ul>
 * </p>
 */
public record ClauseEcolageBeResult(
        // Inputs (snapshot)
        ClauseEcolageBeTypeFormation typeFormation,
        boolean clauseEcriteAvantEntreeFormation,
        BigDecimal coutReelFormationEuros,
        BigDecimal rmmmgMensuelEuros,
        int dureeEfficaciteMois,
        LocalDate dateFinFormation,
        LocalDate dateDepartTravailleur,
        ClauseEcolageBeMotifDepart motifDepart,

        // Outputs analytiques
        ClauseEcolageBeVerdict verdict,
        BigDecimal coutMinLegalEuros,
        int moisEcoulesDepuisFinFormation,
        int tierDureeAuDepart,
        BigDecimal quotiteDueRatio,
        BigDecimal montantBrutDueEuros,
        BigDecimal plafond80Euros,
        BigDecimal montantDuFinalEuros,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
