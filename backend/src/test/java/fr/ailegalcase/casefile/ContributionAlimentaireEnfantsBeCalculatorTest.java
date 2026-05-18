package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-06 : tests unitaires de {@link ContributionAlimentaireEnfantsBeCalculator}.
 */
class ContributionAlimentaireEnfantsBeCalculatorTest {

    private ContributionAlimentaireEnfantsBeInput input(
            int nbEnfants, ContributionAlimentaireEnfantsBeCalculator.TrancheAge tranche,
            String revenu1, String revenu2, String coutGlobal,
            int nuits1, int nuits2, String allocations, String frais) {
        return new ContributionAlimentaireEnfantsBeInput(
                nbEnfants, tranche,
                new BigDecimal(revenu1), new BigDecimal(revenu2),
                coutGlobal == null ? null : new BigDecimal(coutGlobal),
                nuits1, nuits2,
                allocations == null ? null : new BigDecimal(allocations),
                frais == null ? null : new BigDecimal(frais),
                null, null);
    }

    @Test
    void compute_deuxEnfants_hebergementDesequilibre_contributionDue() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(2, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2800", "1900", null, 110, 255, "340", "80"),
                "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(ContributionAlimentaireEnfantsBeCalculator.Verdict.CONTRIBUTION_DUE);
        // Forfait 350 × 2 = 700.
        assertThat(r.coutMensuelRetenu()).isEqualByComparingTo("700.00");
        // Coût net = 700 - 340 = 360.
        assertThat(r.coutNetApresAllocations()).isEqualByComparingTo("360.00");
        // Parent 1 a un revenu plus élevé mais moins de nuits → il est débiteur.
        assertThat(r.parentDebiteur())
                .isEqualTo(ContributionAlimentaireEnfantsBeCalculator.ParentDebiteur.PARENT_1);
        assertThat(r.contributionMensuelleNette()).isGreaterThan(BigDecimal.ZERO);
        assertThat(r.country()).isEqualTo("BELGIQUE");
    }

    @Test
    void compute_coutForfaitaire_appliqueParTrancheAge() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_12_17,
                        "2000", "2000", null, 182, 183, null, null),
                "BELGIQUE");
        // Forfait 420 × 1 enfant.
        assertThat(r.coutMensuelRetenu()).isEqualByComparingTo("420.00");
    }

    @Test
    void compute_coutExplicite_estUtiliseTelQuel() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(2, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "3000", "2000", "1100", 100, 265, null, null),
                "BELGIQUE");
        assertThat(r.coutMensuelRetenu()).isEqualByComparingTo("1100.00");
    }

    @Test
    void compute_revenusEgaux_hebergementEquilibre_contributionEquilibree() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2500", "2500", null, 182, 183, null, null),
                "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(ContributionAlimentaireEnfantsBeCalculator.Verdict.CONTRIBUTION_EQUILIBREE);
        assertThat(r.parentDebiteur())
                .isEqualTo(ContributionAlimentaireEnfantsBeCalculator.ParentDebiteur.AUCUN);
    }

    @Test
    void compute_revenusNuls_donneesInsuffisantes() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_0_5,
                        "0", "0", null, 182, 183, null, null),
                "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(ContributionAlimentaireEnfantsBeCalculator.Verdict.DONNEES_INSUFFISANTES);
    }

    @Test
    void compute_allocationsSuperieuresAuCout_coutNetPlafonneAZero() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_0_5,
                        "2000", "1500", null, 182, 183, "500", null),
                "BELGIQUE");
        // Forfait 280, allocations 500 → coût net plancher 0.
        assertThat(r.coutNetApresAllocations()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_fraisExtraordinaires_repartisAuProrataRenard() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(2, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "3000", "1000", null, 182, 183, null, "200"),
                "BELGIQUE");
        // Prorata revenus : Parent 1 = 75 %, Parent 2 = 25 %.
        assertThat(r.fraisExtraordinairesQuotePartParent1()).isEqualByComparingTo("150.00");
        assertThat(r.fraisExtraordinairesQuotePartParent2()).isEqualByComparingTo("50.00");
    }

    @Test
    void compute_sommeNuitsDifferenteDe365_normaliseEtAvertit() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2000", "2000", null, 100, 100, null, null),
                "BELGIQUE");
        assertThat(r.messages()).anyMatch(m -> m.contains("normalisées"));
    }

    @Test
    void compute_montantsArrondisADeuxDecimales() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(3, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_12_17,
                        "2733", "1817", null, 121, 244, "271", "97"),
                "BELGIQUE");
        assertThat(r.contributionMensuelleNette().scale()).isEqualTo(2);
        assertThat(r.partContributiveParent1().scale()).isEqualTo(2);
        assertThat(r.fraisExtraordinairesQuotePartParent1().scale()).isEqualTo(2);
    }

    @Test
    void compute_paysFrance_rejette() {
        assertThatThrownBy(() -> ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2000", "2000", null, 182, 183, null, null),
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void compute_nombreEnfantsInvalide_rejette() {
        assertThatThrownBy(() -> ContributionAlimentaireEnfantsBeCalculator.compute(
                input(0, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2000", "2000", null, 182, 183, null, null),
                "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre d'enfants");
    }

    @Test
    void compute_revenuNegatif_rejette() {
        assertThatThrownBy(() -> ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "-100", "2000", null, 182, 183, null, null),
                "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_nuitsHorsPlage_rejette() {
        assertThatThrownBy(() -> ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2000", "2000", null, 400, 100, null, null),
                "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("365");
    }
}
