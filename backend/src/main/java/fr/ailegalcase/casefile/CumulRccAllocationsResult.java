package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-04 : résultat brut produit par
 * {@link CumulRccAllocationsCalculator} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link CumulRccAllocationsService} dans
 * {@link CumulRccAllocationsResponse}).</p>
 *
 * <p>{@code montantReductionIndemnite} est non null uniquement lorsque
 * {@link CumulRccAllocationsVerdict#CUMUL_PLAFOND_DEPASSE} : montant de
 * la réduction à appliquer à l'indemnité complémentaire pour respecter
 * le plafond (rémunération nette de référence).</p>
 */
public record CumulRccAllocationsResult(
        // Inputs (snapshot)
        LocalDate dateNaissance,
        LocalDate dateDebutRcc,
        BigDecimal allocationChomageMensuelle,
        BigDecimal indemniteComplementaireMensuelle,
        BigDecimal remunerationNetteReference,
        Integer anneesCarriereTotale,
        CumulRccAllocationsActiviteAutorisee activiteComplementaire,
        Integer ageLegalPension,

        // Outputs
        CumulRccAllocationsVerdict verdict,
        boolean conforme,

        // Indicateurs calculés
        int ageADateDebutRcc,
        BigDecimal cumulMensuelTotal,
        boolean plafondRespecte,
        BigDecimal montantReductionIndemnite,
        CumulRccAllocationsDisponibiliteRegime regimeDisponibilite,
        boolean ageLegalPensionAtteint,

        // Synthèse + base juridique
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
