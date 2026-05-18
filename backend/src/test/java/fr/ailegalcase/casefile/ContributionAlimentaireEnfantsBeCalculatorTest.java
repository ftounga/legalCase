package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-06 / SF-217-10 : tests unitaires de
 * {@link ContributionAlimentaireEnfantsBeCalculator}.
 *
 * <p>SF-217-10 : le coût de l'enfant suit désormais le <b>vrai modèle Renard</b>
 * — coefficient statistique d'âge × revenus cumulés des parents (allocations
 * incluses dans l'assiette), et non plus un forfait fixe.</p>
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

    // --- Coût de l'enfant : modèle Renard indexé sur les revenus (SF-217-10) ---

    @Test
    void compute_coutRenard_indexeSurRevenus() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2000", "2000", null, 182, 183, null, null),
                "BELGIQUE");
        // Coefficient 6-11 = 0,2032 ; revenuBase = 2000 + 2000 + 0 = 4000.
        // Coût = 0,2032 × 4000 × 1 = 812,80.
        assertThat(r.coutMensuelRetenu()).isEqualByComparingTo("812.80");
    }

    @Test
    void compute_coutRenard_inclutAllocationsDansAssiette() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2000", "2000", null, 182, 183, "200", null),
                "BELGIQUE");
        // revenuBase = 2000 + 2000 + 200 = 4200 ; coût = 0,2032 × 4200 = 853,44.
        assertThat(r.coutMensuelRetenu()).isEqualByComparingTo("853.44");
        // Coût net = coût Renard - allocations = 853,44 - 200 = 653,44.
        assertThat(r.coutNetApresAllocations()).isEqualByComparingTo("653.44");
    }

    @Test
    void compute_coutRenard_proportionnelAuxRevenus() {
        ContributionAlimentaireEnfantsBeResult bas = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "1500", "1500", null, 182, 183, null, null),
                "BELGIQUE");
        ContributionAlimentaireEnfantsBeResult haut = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "3000", "3000", null, 182, 183, null, null),
                "BELGIQUE");
        // Revenus doublés → coût doublé (cœur de la correction Renard).
        assertThat(haut.coutMensuelRetenu())
                .isEqualByComparingTo(bas.coutMensuelRetenu().multiply(new BigDecimal("2")));
    }

    @Test
    void compute_coutRenard_sommeParNombreEnfants() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(3, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2000", "2000", null, 182, 183, null, null),
                "BELGIQUE");
        // 0,2032 × 4000 × 3 enfants = 2438,40.
        assertThat(r.coutMensuelRetenu()).isEqualByComparingTo("2438.40");
    }

    @Test
    void compute_coefficientParTranche_appliqueLaTableRenard() {
        assertThat(coutUnEnfant(ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_0_5))
                .isEqualByComparingTo("636.40");   // 0,1591 × 4000
        assertThat(coutUnEnfant(ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11))
                .isEqualByComparingTo("812.80");   // 0,2032 × 4000
        assertThat(coutUnEnfant(ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_12_17))
                .isEqualByComparingTo("989.60");   // 0,2474 × 4000
        assertThat(coutUnEnfant(ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_18_PLUS))
                .isEqualByComparingTo("1078.00");  // 0,2695 × 4000
    }

    private BigDecimal coutUnEnfant(ContributionAlimentaireEnfantsBeCalculator.TrancheAge tranche) {
        return ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, tranche, "2000", "2000", null, 182, 183, null, null),
                "BELGIQUE").coutMensuelRetenu();
    }

    @Test
    void compute_coutExplicite_primeSurLeModeleRenard() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(2, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "3000", "2000", "1100", 100, 265, null, null),
                "BELGIQUE");
        // Coût global explicite → retenu tel quel, le modèle Renard n'est pas appliqué.
        assertThat(r.coutMensuelRetenu()).isEqualByComparingTo("1100.00");
    }

    @Test
    void compute_detail_neMentionnePasForfait() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(2, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2800", "1900", null, 110, 255, "340", "80"),
                "BELGIQUE");
        assertThat(r.detailCalcul()).noneMatch(d -> d.toLowerCase().contains("forfait"));
        assertThat(r.detailCalcul()).anyMatch(d -> d.contains("modèle Renard"));
    }

    // --- Répartition / verdict (parties préservées de SF-217-06) ---

    @Test
    void compute_deuxEnfants_hebergementDesequilibre_contributionDue() {
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(2, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_6_11,
                        "2800", "1900", null, 110, 255, "340", "80"),
                "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(ContributionAlimentaireEnfantsBeCalculator.Verdict.CONTRIBUTION_DUE);
        // Parent 1 a un revenu plus élevé mais moins de nuits → il est débiteur.
        assertThat(r.parentDebiteur())
                .isEqualTo(ContributionAlimentaireEnfantsBeCalculator.ParentDebiteur.PARENT_1);
        assertThat(r.contributionMensuelleNette()).isGreaterThan(BigDecimal.ZERO);
        assertThat(r.country()).isEqualTo("BELGIQUE");
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
        // Coût Renard 0-5 = 0,1591 × revenuBase. revenuBase = 700 + 600 + 800 = 2100
        // → coût = 0,1591 × 2100 = 334,11 ; allocations 800 → coût net plancher 0.
        ContributionAlimentaireEnfantsBeResult r = ContributionAlimentaireEnfantsBeCalculator.compute(
                input(1, ContributionAlimentaireEnfantsBeCalculator.TrancheAge.ENFANT_0_5,
                        "700", "600", null, 182, 183, "800", null),
                "BELGIQUE");
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
        assertThat(r.coutMensuelRetenu().scale()).isEqualTo(2);
    }

    // --- Validation des inputs (préservée) ---

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
