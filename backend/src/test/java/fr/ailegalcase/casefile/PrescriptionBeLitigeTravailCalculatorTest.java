package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-207-01 : tests unitaires du calculateur de prescription Travail BE
 * (9 cas mini-spec).
 */
class PrescriptionBeLitigeTravailCalculatorTest {

    private static PrescriptionBeLitigeTravailRequest req(LocalDate dateRupture,
                                                          PrescriptionBeLitigeTravailTypeCreance type,
                                                          LocalDate dateAction) {
        return new PrescriptionBeLitigeTravailRequest(dateRupture, type.name(), dateAction);
    }

    // =========================================================================
    // EX_CONTRAT_GENERAL — délai 1 an post-rupture (Loi 03/07/1978 art. 15 al. 1)
    // =========================================================================

    @Test
    void exContratGeneral_1anExactEcoule_PRESCRIT() {
        // dateRupture = -1 an, action aujourd'hui → joursRestants = 0 → PRESCRIT
        LocalDate dateAction = LocalDate.of(2026, 5, 19);
        LocalDate dateRupture = dateAction.minusYears(1); // 2025-05-19
        var result = PrescriptionBeLitigeTravailCalculator.compute(
                req(dateRupture, PrescriptionBeLitigeTravailTypeCreance.EX_CONTRAT_GENERAL, dateAction));
        assertThat(result.verdict()).isEqualTo("PRESCRIT");
        assertThat(result.joursRestants()).isEqualTo(0);
        assertThat(result.dateLimitePrescription()).isEqualTo(dateAction);
        assertThat(result.regleAppliquee()).isEqualTo("1_AN_POST_RUPTURE_LOI_1978_ART_15");
        assertThat(result.baseJuridique()).contains("3 juillet 1978");
        assertThat(result.formuleCalcul()).contains("1 an").contains("joursRestants = 0");
    }

    @Test
    void exContratGeneral_environ11moisEcoules_IMMINENT() {
        // Calage déterministe : dateLimite = dateAction + 20 jours, donc dateRupture
        // = dateLimite - 1 an. joursRestants = 20 ∈ ]0; 30] → IMMINENT.
        LocalDate dateAction = LocalDate.of(2026, 5, 19);
        LocalDate dateLimite = dateAction.plusDays(20);     // 2026-06-08
        LocalDate dateRupture = dateLimite.minusYears(1);   // 2025-06-08
        var result = PrescriptionBeLitigeTravailCalculator.compute(
                req(dateRupture, PrescriptionBeLitigeTravailTypeCreance.EX_CONTRAT_GENERAL, dateAction));
        assertThat(result.verdict()).isEqualTo("IMMINENT");
        assertThat(result.joursRestants()).isEqualTo(20);
    }

    @Test
    void exContratGeneral_3moisEcoules_NON_PRESCRIT() {
        // dateRupture = -3 mois, action aujourd'hui → joursRestants ≈ 9 mois
        LocalDate dateAction = LocalDate.of(2026, 5, 19);
        LocalDate dateRupture = dateAction.minusMonths(3); // 2026-02-19
        var result = PrescriptionBeLitigeTravailCalculator.compute(
                req(dateRupture, PrescriptionBeLitigeTravailTypeCreance.EX_CONTRAT_GENERAL, dateAction));
        assertThat(result.verdict()).isEqualTo("NON_PRESCRIT");
        assertThat(result.joursRestants()).isGreaterThan(30L);
        // dateLimite = 2027-02-19
        assertThat(result.dateLimitePrescription()).isEqualTo(LocalDate.of(2027, 2, 19));
    }

    // =========================================================================
    // EX_CONTRAT_CCT_109 — délai 1 an, base CCT 109
    // =========================================================================

    @Test
    void exContratCct109_delai1An_regleSpecifiqueAppliquee() {
        LocalDate dateAction = LocalDate.of(2026, 5, 19);
        LocalDate dateRupture = LocalDate.of(2026, 5, 1);
        var result = PrescriptionBeLitigeTravailCalculator.compute(
                req(dateRupture, PrescriptionBeLitigeTravailTypeCreance.EX_CONTRAT_CCT_109, dateAction));
        assertThat(result.regleAppliquee()).isEqualTo("1_AN_CCT_109_ART_11");
        assertThat(result.baseJuridique()).contains("CCT 109 art. 11");
        assertThat(result.dateLimitePrescription()).isEqualTo(LocalDate.of(2027, 5, 1));
    }

    // =========================================================================
    // PENDANT_CONTRAT — délai 5 ans
    // =========================================================================

    @Test
    void pendantContrat_5ansAppliques() {
        LocalDate dateAction = LocalDate.of(2026, 5, 19);
        LocalDate faitGenerateur = LocalDate.of(2026, 5, 1);
        var result = PrescriptionBeLitigeTravailCalculator.compute(
                req(faitGenerateur, PrescriptionBeLitigeTravailTypeCreance.PENDANT_CONTRAT, dateAction));
        assertThat(result.regleAppliquee()).isEqualTo("5_ANS_PENDANT_CONTRAT");
        assertThat(result.dateLimitePrescription()).isEqualTo(LocalDate.of(2031, 5, 1));
        assertThat(result.verdict()).isEqualTo("NON_PRESCRIT");
        assertThat(result.formuleCalcul()).contains("5 ans");
    }

    // =========================================================================
    // ARRIERES_SALAIRE — délai 5 ans
    // =========================================================================

    @Test
    void arrieresSalaire_5ansAppliques() {
        LocalDate dateAction = LocalDate.of(2026, 5, 19);
        LocalDate echeance = LocalDate.of(2026, 4, 30);
        var result = PrescriptionBeLitigeTravailCalculator.compute(
                req(echeance, PrescriptionBeLitigeTravailTypeCreance.ARRIERES_SALAIRE, dateAction));
        assertThat(result.regleAppliquee()).isEqualTo("5_ANS_ARRIERES_SALAIRE");
        assertThat(result.baseJuridique()).contains("art. 2262bis");
        assertThat(result.dateLimitePrescription()).isEqualTo(LocalDate.of(2031, 4, 30));
        assertThat(result.verdict()).isEqualTo("NON_PRESCRIT");
    }

    // =========================================================================
    // Erreurs de validation
    // =========================================================================

    @Test
    void dateActionAvantDateRupture_levee400() {
        LocalDate dateRupture = LocalDate.of(2026, 5, 19);
        LocalDate dateAction = LocalDate.of(2026, 5, 18); // strictement avant
        assertThatThrownBy(() -> PrescriptionBeLitigeTravailCalculator.compute(
                req(dateRupture, PrescriptionBeLitigeTravailTypeCreance.EX_CONTRAT_GENERAL, dateAction)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postérieure");
    }

    // =========================================================================
    // Frontières IMMINENT / NON_PRESCRIT
    // =========================================================================

    @Test
    void frontiereExacte_joursRestants30_IMMINENT() {
        // dateRupture + 1 an = dateAction + 30 jours exact → joursRestants = 30
        LocalDate dateAction = LocalDate.of(2026, 5, 19);
        LocalDate dateLimite = dateAction.plusDays(30);     // 2026-06-18
        LocalDate dateRupture = dateLimite.minusYears(1);   // 2025-06-18
        var result = PrescriptionBeLitigeTravailCalculator.compute(
                req(dateRupture, PrescriptionBeLitigeTravailTypeCreance.EX_CONTRAT_GENERAL, dateAction));
        assertThat(result.joursRestants()).isEqualTo(30);
        assertThat(result.verdict()).isEqualTo("IMMINENT"); // 30 inclus
    }

    @Test
    void frontiereExacte_joursRestants31_NON_PRESCRIT() {
        // joursRestants = 31 → NON_PRESCRIT (frontière inclusive à 30)
        LocalDate dateAction = LocalDate.of(2026, 5, 19);
        LocalDate dateLimite = dateAction.plusDays(31);     // 2026-06-19
        LocalDate dateRupture = dateLimite.minusYears(1);   // 2025-06-19
        var result = PrescriptionBeLitigeTravailCalculator.compute(
                req(dateRupture, PrescriptionBeLitigeTravailTypeCreance.EX_CONTRAT_GENERAL, dateAction));
        assertThat(result.joursRestants()).isEqualTo(31);
        assertThat(result.verdict()).isEqualTo("NON_PRESCRIT");
    }
}
