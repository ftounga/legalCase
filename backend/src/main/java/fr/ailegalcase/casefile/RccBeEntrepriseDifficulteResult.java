package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-03 : résultat brut produit par
 * {@link RccBeEntrepriseDifficulteValidator} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link RccBeEntrepriseDifficulteService} dans
 * {@link RccBeEntrepriseDifficulteResponse}).</p>
 *
 * <p>{@code indemniteComplementaireMensuelle} est {@code null} lorsque les
 * informations financières ({@code remunerationNetteMensuelleReference} et
 * {@code allocationChomageMensuelleEstimee}) ne sont pas fournies ou si le
 * verdict est négatif. {@code avertissement} est {@code null} si aucune
 * alerte n'est levée.</p>
 */
public record RccBeEntrepriseDifficulteResult(
        // Inputs (snapshot)
        RccBeEntrepriseDifficulteTypeEnum typeReconnaissance,
        Integer ageReduitPlan,
        Integer ageFinContrat,
        Integer anneesCarriereTotale,
        Integer anneesAncienneteSecteur,
        LocalDate dateFinContrat,
        boolean licenciementEffectif,
        BigDecimal remunerationNetteMensuelleReference,
        BigDecimal allocationChomageMensuelleEstimee,

        // Verdict et conditions cumulatives
        RccBeEntrepriseDifficulteVerdict verdict,
        boolean eligible,
        boolean conditionReconnaissanceRemplie,
        boolean conditionAgeRemplie,
        boolean conditionCarriereRemplie,
        boolean conditionAncienneteRemplie,
        boolean conditionLicenciementRemplie,

        // Calcul indicatif indemnité complémentaire
        BigDecimal indemniteComplementaireMensuelle,

        // Synthèse + base juridique
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
