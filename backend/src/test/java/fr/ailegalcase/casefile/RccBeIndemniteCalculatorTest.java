package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-207-07 : tests unitaires du calculateur d'indemnité complémentaire RCC
 * BE (CCT 17 art. 5 ; loi 03/07/1978 ; AR 03/05/2007).
 *
 * <p>Couvre : cas nominal, plancher 0, plancher sectoriel, précision
 * BigDecimal, mois restants en bornes (dateDebut futur, dateDebut après âge
 * de pension), défense en profondeur (champs null), formule textuelle, âge
 * légal pension custom (67 ans), etc.</p>
 */
class RccBeIndemniteCalculatorTest {

    /**
     * Cas mini-spec nominal — remunNette=3200, allocOnem=1500 →
     * (3200-1500)/2 = 850 €/mois. Mois restants 2026-09-01 → 2031-09-01
     * (66 ans pour naissance 1965-09-01) = 60 mois → total 51 000 €.
     */
    @Test
    void cas_nominal_3200_1500_850_par_mois() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200.00"),
                new BigDecimal("1500.00"),
                LocalDate.of(1965, 9, 1),
                LocalDate.of(2026, 9, 1),
                null,
                null);
        var r = RccBeIndemniteCalculator.compute(req);

        assertThat(r.indemniteMensuelleAvantPlancher()).isEqualByComparingTo("850.00");
        assertThat(r.indemniteMensuelleEmployeur()).isEqualByComparingTo("850.00");
        assertThat(r.planchersSectorielApplique()).isFalse();
        assertThat(r.moisRestantsJusquaPension()).isEqualTo(60L);
        assertThat(r.montantTotalEmployeur()).isEqualByComparingTo("51000.00");
        assertThat(r.ageLegalPensionApplique()).isEqualTo(66);
        assertThat(r.dateAgeLegalPension()).isEqualTo(LocalDate.of(2031, 9, 1));
        assertThat(r.baseJuridique())
                .contains("CCT 17 art. 5")
                .contains("3 juillet 1978")
                .contains("3 mai 2007");
        assertThat(r.formuleCalcul())
                .contains("850.00")
                .contains("60 mois")
                .contains("51000.00");
    }

    /**
     * Critère mini-spec : remunNette ≤ allocOnem → indemnité = 0 (plancher).
     */
    @Test
    void remunNette_inferieure_a_allocOnem_indemnite_plancher_0() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("1200.00"),
                new BigDecimal("1500.00"),                                // > remun
                LocalDate.of(1965, 1, 1),
                LocalDate.of(2026, 1, 1),
                null,
                null);
        var r = RccBeIndemniteCalculator.compute(req);

        assertThat(r.indemniteMensuelleAvantPlancher()).isEqualByComparingTo("0.00");
        assertThat(r.indemniteMensuelleEmployeur()).isEqualByComparingTo("0.00");
        assertThat(r.planchersSectorielApplique()).isFalse();
        assertThat(r.montantTotalEmployeur()).isEqualByComparingTo("0.00");
    }

    /**
     * Critère mini-spec : plancher sectoriel 1000 avec indemnité calculée 850
     * → indemnité = 1000, planchersSectorielApplique=true.
     */
    @Test
    void plancher_sectoriel_superieur_remplace_indemnite_cct17() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200.00"),
                new BigDecimal("1500.00"),
                LocalDate.of(1965, 1, 1),
                LocalDate.of(2026, 1, 1),
                null,
                new BigDecimal("1000.00"));                               // > 850
        var r = RccBeIndemniteCalculator.compute(req);

        assertThat(r.indemniteMensuelleAvantPlancher()).isEqualByComparingTo("850.00");
        assertThat(r.indemniteMensuelleEmployeur()).isEqualByComparingTo("1000.00");
        assertThat(r.planchersSectorielApplique()).isTrue();
        assertThat(r.formuleCalcul())
                .contains("plancher sectoriel")
                .contains("appliqué");
    }

    /**
     * Plancher sectoriel inférieur à l'indemnité CCT 17 → non applicable.
     */
    @Test
    void plancher_sectoriel_inferieur_non_applicable() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200.00"),
                new BigDecimal("1500.00"),
                LocalDate.of(1965, 1, 1),
                LocalDate.of(2026, 1, 1),
                null,
                new BigDecimal("500.00"));                                // < 850
        var r = RccBeIndemniteCalculator.compute(req);

        assertThat(r.indemniteMensuelleEmployeur()).isEqualByComparingTo("850.00");
        assertThat(r.planchersSectorielApplique()).isFalse();
        assertThat(r.formuleCalcul()).contains("non applicable");
    }

    /**
     * Plancher sectoriel égal à l'indemnité CCT 17 → non applicable (pas
     * strictement supérieur).
     */
    @Test
    void plancher_sectoriel_egal_non_applicable() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200.00"),
                new BigDecimal("1500.00"),
                LocalDate.of(1965, 1, 1),
                LocalDate.of(2026, 1, 1),
                null,
                new BigDecimal("850.00"));                                // = 850
        var r = RccBeIndemniteCalculator.compute(req);

        assertThat(r.indemniteMensuelleEmployeur()).isEqualByComparingTo("850.00");
        assertThat(r.planchersSectorielApplique()).isFalse();
    }

    /**
     * Critère mini-spec : précision BigDecimal — (3201.55 − 1500.20) / 2
     * = 1701.35 / 2 = 850.675 → 850.68 (HALF_EVEN, 2 décimales).
     */
    @Test
    void precision_bigdecimal_half_even_2_decimales() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3201.55"),
                new BigDecimal("1500.20"),
                LocalDate.of(1965, 1, 1),
                LocalDate.of(2026, 1, 1),
                null,
                null);
        var r = RccBeIndemniteCalculator.compute(req);

        // 1701.35 / 2 = 850.675 → HALF_EVEN → 850.68 (5 round to even -> .68)
        assertThat(r.indemniteMensuelleAvantPlancher()).isEqualByComparingTo("850.68");
        assertThat(r.indemniteMensuelleEmployeur()).isEqualByComparingTo("850.68");
    }

    /**
     * Précision HALF_EVEN — différence subtle vs HALF_UP. 0.125 → 0.12
     * (banker's rounding, round half to even). Cas concret : (1000.25 −
     * 1000.00) / 2 = 0.125 → 0.12.
     */
    @Test
    void precision_half_even_arrondi_banker() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("1000.25"),
                new BigDecimal("1000.00"),
                LocalDate.of(1965, 1, 1),
                LocalDate.of(2026, 1, 1),
                null,
                null);
        var r = RccBeIndemniteCalculator.compute(req);

        // 0.25 / 2 = 0.125 → HALF_EVEN → 0.12 (2 est pair)
        assertThat(r.indemniteMensuelleAvantPlancher()).isEqualByComparingTo("0.12");
    }

    /**
     * Critère mini-spec : dateDebutRcc APRÈS l'âge de pension → moisRestants
     * = 0 et montantTotal = 0.
     */
    @Test
    void dateDebutRcc_apres_age_pension_moisRestants_0() {
        // naissance 1955-01-01 → 66 ans = 2021 ; dateDebut 2026 → après pension
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200.00"),
                new BigDecimal("1500.00"),
                LocalDate.of(1955, 1, 1),
                LocalDate.of(2026, 1, 1),
                null,
                null);
        var r = RccBeIndemniteCalculator.compute(req);

        assertThat(r.moisRestantsJusquaPension()).isEqualTo(0L);
        assertThat(r.montantTotalEmployeur()).isEqualByComparingTo("0.00");
        // L'indemnité mensuelle reste calculée même si moisRestants = 0
        assertThat(r.indemniteMensuelleEmployeur()).isEqualByComparingTo("850.00");
    }

    /**
     * Âge légal pension custom — 67 ans (régime 2030+) au lieu du défaut 66.
     */
    @Test
    void age_legal_pension_67_custom() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200.00"),
                new BigDecimal("1500.00"),
                LocalDate.of(1965, 9, 1),
                LocalDate.of(2026, 9, 1),
                67,                                                       // custom
                null);
        var r = RccBeIndemniteCalculator.compute(req);

        assertThat(r.ageLegalPensionApplique()).isEqualTo(67);
        assertThat(r.dateAgeLegalPension()).isEqualTo(LocalDate.of(2032, 9, 1));
        // 6 ans = 72 mois
        assertThat(r.moisRestantsJusquaPension()).isEqualTo(72L);
        assertThat(r.montantTotalEmployeur()).isEqualByComparingTo("61200.00");
    }

    /**
     * Mois restants exactement 1 — dateDebut = dateAgePension − 1 mois.
     */
    @Test
    void moisRestants_egal_1() {
        // naissance 1965-09-01 → 66 ans = 2031-09-01 ; dateDebut = 2031-08-01
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200.00"),
                new BigDecimal("1500.00"),
                LocalDate.of(1965, 9, 1),
                LocalDate.of(2031, 8, 1),
                null,
                null);
        var r = RccBeIndemniteCalculator.compute(req);

        assertThat(r.moisRestantsJusquaPension()).isEqualTo(1L);
        assertThat(r.montantTotalEmployeur()).isEqualByComparingTo("850.00");
    }

    /**
     * Cas plancher sectoriel + remunNette = allocOnem (indemnité CCT 17 = 0)
     * → seul le plancher sectoriel s'applique.
     */
    @Test
    void plancher_sectoriel_couvre_indemnite_cct17_nulle() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("1500.00"),
                new BigDecimal("1500.00"),                                // diff = 0
                LocalDate.of(1965, 1, 1),
                LocalDate.of(2026, 1, 1),
                null,
                new BigDecimal("400.00"));                                // sectoriel
        var r = RccBeIndemniteCalculator.compute(req);

        assertThat(r.indemniteMensuelleAvantPlancher()).isEqualByComparingTo("0.00");
        assertThat(r.indemniteMensuelleEmployeur()).isEqualByComparingTo("400.00");
        assertThat(r.planchersSectorielApplique()).isTrue();
    }

    /**
     * Plancher sectoriel = 0 explicite, indemnité CCT 17 = 850 →
     * planchersSectorielApplique = false (0 < 850).
     */
    @Test
    void plancher_sectoriel_zero_non_applicable() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200.00"),
                new BigDecimal("1500.00"),
                LocalDate.of(1965, 1, 1),
                LocalDate.of(2026, 1, 1),
                null,
                BigDecimal.ZERO);                                         // 0
        var r = RccBeIndemniteCalculator.compute(req);

        assertThat(r.indemniteMensuelleEmployeur()).isEqualByComparingTo("850.00");
        assertThat(r.planchersSectorielApplique()).isFalse();
    }

    /**
     * Montant total grand — 1000 €/mois × 240 mois = 240 000 € (durée
     * exceptionnelle, salarié 46 ans à dateDebut, 20 ans avant pension).
     */
    @Test
    void montant_total_grand_240_mois() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3000.00"),
                new BigDecimal("1000.00"),
                LocalDate.of(1985, 1, 1),                                 // 41 ans en 2026
                LocalDate.of(2031, 1, 1),                                 // 46 ans
                null,
                null);
        var r = RccBeIndemniteCalculator.compute(req);

        // (3000-1000)/2 = 1000 ; 2031 → 2051 (66 ans) = 240 mois
        assertThat(r.indemniteMensuelleEmployeur()).isEqualByComparingTo("1000.00");
        assertThat(r.moisRestantsJusquaPension()).isEqualTo(240L);
        assertThat(r.montantTotalEmployeur()).isEqualByComparingTo("240000.00");
    }

    // =========================================================================
    // Défenses en profondeur — requête null, champs requis null
    // =========================================================================

    @Test
    void requestNull_leveIllegalArgument() {
        assertThatThrownBy(() -> RccBeIndemniteCalculator.compute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RCC BE indemnité");
    }

    @Test
    void remunNetteNull_leveIllegalArgument() {
        var req = new RccBeIndemniteRequest(
                null, new BigDecimal("1500"),
                LocalDate.of(1965, 1, 1), LocalDate.of(2026, 1, 1),
                null, null);
        assertThatThrownBy(() -> RccBeIndemniteCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationNetteReference");
    }

    @Test
    void allocOnemNull_leveIllegalArgument() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200"), null,
                LocalDate.of(1965, 1, 1), LocalDate.of(2026, 1, 1),
                null, null);
        assertThatThrownBy(() -> RccBeIndemniteCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allocationOnemMensuelle");
    }

    @Test
    void dateNaissanceNull_leveIllegalArgument() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200"), new BigDecimal("1500"),
                null, LocalDate.of(2026, 1, 1),
                null, null);
        assertThatThrownBy(() -> RccBeIndemniteCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNaissanceSalarie");
    }

    @Test
    void dateDebutRccNull_leveIllegalArgument() {
        var req = new RccBeIndemniteRequest(
                new BigDecimal("3200"), new BigDecimal("1500"),
                LocalDate.of(1965, 1, 1), null,
                null, null);
        assertThatThrownBy(() -> RccBeIndemniteCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDebutRcc");
    }
}
