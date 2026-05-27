package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-02 : résultat brut produit par
 * {@link RccBeLongueCarriereValidator} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link RccBeLongueCarriereService} dans
 * {@link RccBeLongueCarriereResponse}).</p>
 *
 * <p>{@code indemniteComplementaireMensuelle} est {@code null} lorsque les
 * informations financières ({@code remunerationNetteMensuelleReference} et
 * {@code allocationChomageMensuelleEstimee}) ne sont pas fournies ou si le
 * verdict est négatif. {@code avertissement} est {@code null} si aucune
 * alerte n'est levée.</p>
 */
public record RccBeLongueCarriereResult(
        // Inputs (snapshot)
        Integer ageFinContrat,
        Integer anneesCarriereTotale,
        LocalDate dateFinContrat,
        boolean licenciementEffectif,
        BigDecimal remunerationNetteMensuelleReference,
        BigDecimal allocationChomageMensuelleEstimee,

        // Verdict et raisons
        RccBeLongueCarriereVerdict verdict,
        boolean eligible,
        boolean conditionAgeRemplie,
        boolean conditionCarriereRemplie,
        boolean conditionLicenciementRemplie,

        // Calcul indicatif indemnité complémentaire
        BigDecimal indemniteComplementaireMensuelle,

        // Synthèse + base juridique
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
