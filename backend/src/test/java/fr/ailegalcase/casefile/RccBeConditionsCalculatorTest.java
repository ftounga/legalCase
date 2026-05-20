package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-207-06 : tests unitaires du calculateur d'éligibilité au RCC BE
 * (CCT n°17 ; CCT n°17/13 ; AR du 03/05/2007 art. 3 et 8).
 *
 * <p>Couvre les 4 régimes (GENERAL, METIERS_LOURDS, LONGUE_CARRIERE,
 * ENTREPRISE_DIFFICULTE) × 3 verdicts (ELIGIBLE, INCERTAIN, NON_ELIGIBLE)
 * + cumul de régimes éligibles + priorisation + bornes de seuils +
 * détection INCERTAIN proche des seuils + défense en profondeur.</p>
 */
class RccBeConditionsCalculatorTest {

    /**
     * Date envisagée du licenciement utilisée par défaut pour rendre les
     * tests déterministes (indépendants de l'horloge système).
     */
    private static final LocalDate LICENCIEMENT = LocalDate.of(2026, 12, 31);

    /**
     * Construit une date de naissance qui produit exactement {@code age}
     * années pleines à la date de licenciement {@link #LICENCIEMENT}.
     */
    private static LocalDate naissancePourAge(int age) {
        // age = 60 → naissance le 1 jour AVANT la date "anniversaire pivot"
        // pour garantir l'âge plein. On retire 1 jour pour ne pas tomber pile
        // sur l'anniversaire (cas border).
        return LICENCIEMENT.minusYears(age).minusDays(1);
    }

    private static RccBeConditionsRequest request(int age, int carriere,
                                                  boolean metierLourd,
                                                  boolean longueCarriere,
                                                  boolean entrepriseEnDifficulte) {
        return new RccBeConditionsRequest(
                naissancePourAge(age),
                carriere,
                metierLourd,
                longueCarriere,
                entrepriseEnDifficulte,
                LICENCIEMENT);
    }

    // =========================================================================
    // 1. GENERAL — âge 60 + carrière 40 → ELIGIBLE
    // =========================================================================

    @Test
    void regimeGeneral_age60_carriere40_ELIGIBLE() {
        var result = RccBeConditionsCalculator.compute(request(60, 40, false, false, false));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.ELIGIBLE);
        assertThat(result.regimeApplicable()).isEqualTo(RccBeConditionsRegime.GENERAL);
        assertThat(result.regimesEligibles()).containsExactly(RccBeConditionsRegime.GENERAL);
        assertThat(result.ageALaDateLicenciement()).isEqualTo(60);
        assertThat(result.anneesCarriereCalculees()).isEqualTo(40);
        assertThat(result.conditionsManquantes()).isEmpty();
        assertThat(result.baseJuridique()).contains("CCT n°17").contains("CCT n°17/13").contains("03/05/2007");
        assertThat(result.formuleCalcul()).contains("60 ans").contains("40 ans").contains("GENERAL");
    }

    // =========================================================================
    // 2. METIERS_LOURDS — âge 58 + carrière 35 + métier lourd → ELIGIBLE
    // =========================================================================

    @Test
    void regimeMetiersLourds_age58_carriere35_metierLourd_ELIGIBLE() {
        var result = RccBeConditionsCalculator.compute(request(58, 35, true, false, false));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.ELIGIBLE);
        assertThat(result.regimeApplicable()).isEqualTo(RccBeConditionsRegime.METIERS_LOURDS);
        assertThat(result.regimesEligibles()).containsExactly(RccBeConditionsRegime.METIERS_LOURDS);
        assertThat(result.ageALaDateLicenciement()).isEqualTo(58);
        assertThat(result.anneesCarriereCalculees()).isEqualTo(35);
        assertThat(result.conditionsManquantes()).isEmpty();
    }

    // =========================================================================
    // 3. LONGUE_CARRIERE — âge 59 + carrière 40 + flag → ELIGIBLE
    // =========================================================================

    @Test
    void regimeLongueCarriere_age59_carriere40_flag_ELIGIBLE() {
        var result = RccBeConditionsCalculator.compute(request(59, 40, false, true, false));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.ELIGIBLE);
        assertThat(result.regimeApplicable()).isEqualTo(RccBeConditionsRegime.LONGUE_CARRIERE);
        assertThat(result.regimesEligibles()).containsExactly(RccBeConditionsRegime.LONGUE_CARRIERE);
    }

    // =========================================================================
    // 4. ENTREPRISE_DIFFICULTE — âge 60 + flag → ELIGIBLE
    // =========================================================================

    @Test
    void regimeEntrepriseDifficulte_age60_flag_ELIGIBLE_avecCarriereFaible() {
        // 60 ans + 20 ans de carrière + entreprise en difficulté : seul ce
        // régime matche (le régime GENERAL exige 40 ans, pas remplis).
        var result = RccBeConditionsCalculator.compute(request(60, 20, false, false, true));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.ELIGIBLE);
        assertThat(result.regimeApplicable()).isEqualTo(RccBeConditionsRegime.ENTREPRISE_DIFFICULTE);
        assertThat(result.regimesEligibles()).containsExactly(RccBeConditionsRegime.ENTREPRISE_DIFFICULTE);
    }

    // =========================================================================
    // 5. Cumul régimes + priorisation — GENERAL prioritaire sur METIERS_LOURDS
    // =========================================================================

    @Test
    void cumulRegimes_age60_carriere40_metierLourd_priorise_GENERAL() {
        var result = RccBeConditionsCalculator.compute(request(60, 40, true, false, false));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.ELIGIBLE);
        assertThat(result.regimeApplicable()).isEqualTo(RccBeConditionsRegime.GENERAL);
        // Ordre = ordre d'évaluation (GENERAL, LONGUE_CARRIERE, METIERS_LOURDS,
        // ENTREPRISE_DIFFICULTE). LONGUE_CARRIERE non éligible (flag false).
        assertThat(result.regimesEligibles()).containsExactly(
                RccBeConditionsRegime.GENERAL,
                RccBeConditionsRegime.METIERS_LOURDS);
        assertThat(result.formuleCalcul()).contains("priorité");
    }

    // =========================================================================
    // 6. Cumul 3 régimes — GENERAL prioritaire sur LONGUE_CARRIERE et METIERS_LOURDS
    // =========================================================================

    @Test
    void cumulTroisRegimes_age60_carriere40_tousFlags_priorise_GENERAL() {
        // GENERAL (60/40) + LONGUE_CARRIERE (59/40/flag) + METIERS_LOURDS
        // (58/35/flag) + ENTREPRISE_DIFFICULTE (60/flag) tous matchent.
        var result = RccBeConditionsCalculator.compute(request(60, 40, true, true, true));

        assertThat(result.regimeApplicable()).isEqualTo(RccBeConditionsRegime.GENERAL);
        // Ordre d'évaluation EVALUATION_ORDER : GENERAL, LONGUE_CARRIERE,
        // METIERS_LOURDS, ENTREPRISE_DIFFICULTE.
        assertThat(result.regimesEligibles()).containsExactly(
                RccBeConditionsRegime.GENERAL,
                RccBeConditionsRegime.LONGUE_CARRIERE,
                RccBeConditionsRegime.METIERS_LOURDS,
                RccBeConditionsRegime.ENTREPRISE_DIFFICULTE);
    }

    // =========================================================================
    // 7. INCERTAIN — âge 56 + carrière 32 (≥ 55 + ≥ 30, aucun régime matche)
    // =========================================================================

    @Test
    void age56_carriere32_INCERTAIN_avecConditionsManquantes() {
        var result = RccBeConditionsCalculator.compute(request(56, 32, false, false, false));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.INCERTAIN);
        assertThat(result.regimeApplicable()).isNull();
        assertThat(result.regimesEligibles()).isEmpty();
        // Régime le plus proche : METIERS_LOURDS (58/35) — distance √(4+9) ≈ 3.6
        // vs GENERAL √(16+64) ≈ 8.9 → METIERS_LOURDS sélectionné.
        assertThat(result.conditionsManquantes()).contains(
                RccBeConditionsCondition.AGE_METIERS_LOURDS_58,
                RccBeConditionsCondition.CARRIERE_METIERS_LOURDS_35,
                RccBeConditionsCondition.METIER_LOURD_FLAG);
    }

    // =========================================================================
    // 8. NON_ELIGIBLE — âge 50 + carrière 20 (loin des seuils)
    // =========================================================================

    @Test
    void age50_carriere20_NON_ELIGIBLE() {
        var result = RccBeConditionsCalculator.compute(request(50, 20, false, false, false));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.NON_ELIGIBLE);
        assertThat(result.regimeApplicable()).isNull();
        assertThat(result.regimesEligibles()).isEmpty();
        assertThat(result.conditionsManquantes()).isNotEmpty();
    }

    // =========================================================================
    // 9. METIERS_LOURDS sans flag — age 58 + carrière 35, métier lourd=false → INCERTAIN
    // =========================================================================

    @Test
    void age58_carriere35_metierLourdFalse_INCERTAIN_METIER_LOURD_FLAG_listee() {
        var result = RccBeConditionsCalculator.compute(request(58, 35, false, false, false));

        // 58 ≥ 55 + 35 ≥ 30 → INCERTAIN
        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.INCERTAIN);
        assertThat(result.regimeApplicable()).isNull();
        // Régime le plus proche : METIERS_LOURDS (distance 0 sur âge + carrière)
        // → seule la condition METIER_LOURD_FLAG manque.
        assertThat(result.conditionsManquantes())
                .containsExactly(RccBeConditionsCondition.METIER_LOURD_FLAG);
    }

    // =========================================================================
    // 10. Bornes — âge 59 + carrière 40 SANS flag longue carrière → INCERTAIN
    // =========================================================================

    @Test
    void borneSeuils_age59_carriere40_sansFlagLongueCarriere_INCERTAIN() {
        // LONGUE_CARRIERE exige le flag, qui est false → aucun régime matche
        // (GENERAL exige 60 ans). âge 59 ≥ 55 + carrière 40 ≥ 30 → INCERTAIN.
        var result = RccBeConditionsCalculator.compute(request(59, 40, false, false, false));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.INCERTAIN);
        // Régime le plus proche par distance : LONGUE_CARRIERE (distance 0 sur
        // âge + carrière, flag manquant). Priorité GENERAL >
        // LONGUE_CARRIERE en cas d'égalité, GENERAL est à distance 1 sur l'âge
        // → LONGUE_CARRIERE est strictement plus proche, donc sélectionné.
        assertThat(result.conditionsManquantes())
                .containsExactly(RccBeConditionsCondition.LONGUE_CARRIERE_FLAG);
    }

    // =========================================================================
    // 11. Borne haute carrière — carrière 60 (max) + âge 60 → ELIGIBLE GENERAL
    // =========================================================================

    @Test
    void borneHauteCarriere_age60_carriere60_ELIGIBLE_GENERAL() {
        var result = RccBeConditionsCalculator.compute(request(60, 60, false, false, false));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.ELIGIBLE);
        assertThat(result.regimeApplicable()).isEqualTo(RccBeConditionsRegime.GENERAL);
    }

    // =========================================================================
    // 12. Default today — dateLicenciementEnvisagee null
    // =========================================================================

    @Test
    void dateLicenciementNull_utiliseAujourdhui_pasDeNPE() {
        // 80 ans révolus → naissance 1980 + carriere 35 + métier lourd
        LocalDate naissance = LocalDate.now().minusYears(80);
        var req = new RccBeConditionsRequest(
                naissance,
                35,
                true,                                                   // métier lourd
                false, false,
                null);                                                  // licenciement par défaut → today
        var result = RccBeConditionsCalculator.compute(req);

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.ELIGIBLE);
        // 80 ans + 35 ans carrière + métier lourd → METIERS_LOURDS au moins
        assertThat(result.regimesEligibles()).contains(RccBeConditionsRegime.METIERS_LOURDS);
        assertThat(result.dateLicenciementEnvisagee()).isNotNull();
    }

    // =========================================================================
    // 13. ENTREPRISE_DIFFICULTE — sans flag + âge 60 + carrière 35 → INCERTAIN
    // =========================================================================

    @Test
    void age60_carriere35_sansFlags_INCERTAIN() {
        // Aucun régime matche (GENERAL exige carriere 40, METIERS_LOURDS exige
        // flag, LONGUE_CARRIERE exige flag, ENTREPRISE_DIFFICULTE exige flag).
        // 60 ≥ 55 + 35 ≥ 30 → INCERTAIN.
        var result = RccBeConditionsCalculator.compute(request(60, 35, false, false, false));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.INCERTAIN);
        assertThat(result.regimeApplicable()).isNull();
        assertThat(result.conditionsManquantes()).isNotEmpty();
    }

    // =========================================================================
    // 14. Défenses en profondeur — requête null, champs requis null
    // =========================================================================

    @Test
    void requestNull_leveIllegalArgument() {
        assertThatThrownBy(() -> RccBeConditionsCalculator.compute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RCC BE");
    }

    @Test
    void dateNaissanceNull_leveIllegalArgument() {
        var req = new RccBeConditionsRequest(
                null, 40, false, false, false, LICENCIEMENT);
        assertThatThrownBy(() -> RccBeConditionsCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNaissance");
    }

    @Test
    void carriereNull_leveIllegalArgument() {
        var req = new RccBeConditionsRequest(
                naissancePourAge(60), null, false, false, false, LICENCIEMENT);
        assertThatThrownBy(() -> RccBeConditionsCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesCarriereProfessionnelle");
    }

    // =========================================================================
    // 15. Verdict NON_ELIGIBLE — détaillage des conditions manquantes
    // =========================================================================

    @Test
    void nonEligible_age40_carriere10_conditionsManquantesPresentes() {
        var result = RccBeConditionsCalculator.compute(request(40, 10, false, false, false));

        assertThat(result.verdict()).isEqualTo(RccBeConditionsResult.Verdict.NON_ELIGIBLE);
        // Régime le plus proche (par distance) : METIERS_LOURDS (58/35) → 3
        // conditions manquent (AGE, CARRIERE, FLAG).
        assertThat(result.conditionsManquantes()).isNotEmpty();
    }

    // =========================================================================
    // 16. Formule textuelle — contient la base juridique pertinente
    // =========================================================================

    @Test
    void formuleCalcul_estLisible_etCiteLeRegime() {
        var result = RccBeConditionsCalculator.compute(request(60, 40, false, false, false));
        assertThat(result.formuleCalcul())
                .contains("60 ans")
                .contains("40 ans")
                .contains("GENERAL applicable");
    }
}
