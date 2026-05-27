package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-215-03 — UT du calculator regroupement 10ter BE.
 *
 * <p>Source : Loi 15/12/1980 art. 10 et 10ter + AR 17/05/2007.
 * <p>Référence seuil ressources : 1 500 €/mois (par défaut, paramétrable application.properties).
 */
class Regroupement10terBeCalculatorTest {

    private static final int SEUIL = Regroupement10terBeCalculator.SEUIL_RESSOURCES_DEFAUT;

    @Test
    void compute_toutConforme_revenus1600_returnsEligible_score100_differentiel100() {
        Regroupement10terBeResult r = Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.CONJOINT,
                Regroupement10terBeTypeCarteEnum.CARTE_B,
                1_600, 24, true, true, false);

        assertThat(r.scoreEligibilite()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo(Regroupement10terBeVerdict.ELIGIBLE);
        assertThat(r.differentielRevenus()).isEqualTo(100);
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.seuilRessources()).isEqualTo(SEUIL);
        assertThat(r.baseJuridique()).contains("Loi du 15/12/1980 art. 10 et 10ter");
        assertThat(r.baseJuridique()).contains("AR du 17/05/2007");
    }

    @Test
    void compute_revenus1499_borderlineDessousSeuil_returnsSousReserve_score60_differentielNegatif() {
        Regroupement10terBeResult r = Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.CONJOINT,
                Regroupement10terBeTypeCarteEnum.CARTE_C,
                1_499, 24, true, true, false);

        assertThat(r.scoreEligibilite()).isEqualTo(60); // 0 (ressources) + 20 + 20 + 10 + 10
        assertThat(r.verdict()).isEqualTo(Regroupement10terBeVerdict.SOUS_RESERVE);
        assertThat(r.differentielRevenus()).isEqualTo(-1);
        assertThat(r.criteresNonRemplis()).hasSize(1);
        assertThat(r.criteresNonRemplis().get(0)).contains("Revenus mensuels nets <");
    }

    @Test
    void compute_revenusEgaleSeuil_returnsEligible_score100_differentielZero() {
        Regroupement10terBeResult r = Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.PARTENAIRE_ENREGISTRE,
                Regroupement10terBeTypeCarteEnum.CARTE_B,
                SEUIL, 24, true, true, false);

        assertThat(r.scoreEligibilite()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo(Regroupement10terBeVerdict.ELIGIBLE);
        assertThat(r.differentielRevenus()).isZero();
    }

    @Test
    void compute_duree11mois_returnsEligibleLimite_score80() {
        // 1 critère manquant sur 100 = score 80 (durée), reste ELIGIBLE (limite haute)
        Regroupement10terBeResult r = Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.ENFANT_MOINS_21,
                Regroupement10terBeTypeCarteEnum.CARTE_B,
                2_000, 11, true, true, false);

        assertThat(r.scoreEligibilite()).isEqualTo(80);
        assertThat(r.verdict()).isEqualTo(Regroupement10terBeVerdict.ELIGIBLE);
        assertThat(r.criteresNonRemplis()).hasSize(1);
        assertThat(r.criteresNonRemplis().get(0)).contains("Durée de séjour");
    }

    @Test
    void compute_toutInvalide_returnsIneligible_score0() {
        Regroupement10terBeResult r = Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.ASCENDANT_CHARGE,
                Regroupement10terBeTypeCarteEnum.CARTE_C,
                0, 0, false, false, true);

        assertThat(r.scoreEligibilite()).isZero();
        assertThat(r.verdict()).isEqualTo(Regroupement10terBeVerdict.INELIGIBLE);
        assertThat(r.criteresNonRemplis()).hasSize(5);
        assertThat(r.differentielRevenus()).isEqualTo(-SEUIL);
    }

    @Test
    void compute_menaceOrdrePublic_returnsConditionOrdrePublicFalse() {
        Regroupement10terBeResult r = Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.CONJOINT,
                Regroupement10terBeTypeCarteEnum.CARTE_B,
                2_000, 24, true, true, true);

        assertThat(r.scoreEligibilite()).isEqualTo(90); // tout sauf OP
        assertThat(r.verdict()).isEqualTo(Regroupement10terBeVerdict.ELIGIBLE);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("ordre public"));
    }

    @Test
    void compute_troisCriteresEnEchec_returnsIneligible_score20() {
        // Ressources OK (40) ; durée KO ; logement KO ; assurance KO ; OP OK (10) = 50 → SOUS_RESERVE
        Regroupement10terBeResult r = Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.ENFANT_21_PLUS_CHARGE,
                Regroupement10terBeTypeCarteEnum.CARTE_C,
                2_000, 6, false, false, false);

        assertThat(r.scoreEligibilite()).isEqualTo(50); // 40 + 0 + 0 + 0 + 10
        assertThat(r.verdict()).isEqualTo(Regroupement10terBeVerdict.SOUS_RESERVE);
        assertThat(r.criteresNonRemplis()).hasSize(3);
    }

    @Test
    void compute_seulMenaceOPMaisRestePARFAIT_returnsEligible90() {
        // 4 conditions OK + OP KO = 90 → ELIGIBLE
        Regroupement10terBeResult r = Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.CONJOINT,
                Regroupement10terBeTypeCarteEnum.CARTE_B,
                1_700, 14, true, true, true);

        assertThat(r.scoreEligibilite()).isEqualTo(90);
        assertThat(r.verdict()).isEqualTo(Regroupement10terBeVerdict.ELIGIBLE);
    }

    @Test
    void compute_revenusNegatif_throwsIllegalArgument() {
        assertThatThrownBy(() -> Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.CONJOINT,
                Regroupement10terBeTypeCarteEnum.CARTE_B,
                -1, 24, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenusMensuelsNetsRegroupant");
    }

    @Test
    void compute_revenusHorsBorne_throwsIllegalArgument() {
        assertThatThrownBy(() -> Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.CONJOINT,
                Regroupement10terBeTypeCarteEnum.CARTE_B,
                100_001, 24, true, true, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dureeSejour601_throwsIllegalArgument() {
        assertThatThrownBy(() -> Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.CONJOINT,
                Regroupement10terBeTypeCarteEnum.CARTE_B,
                2_000, 601, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeSejour");
    }

    @Test
    void compute_seuilParametrable_changeVerdict() {
        // Avec seuil 1500 : revenus 1499 → ressources KO
        Regroupement10terBeResult low = Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.CONJOINT,
                Regroupement10terBeTypeCarteEnum.CARTE_B,
                1_499, 24, true, true, false, 1_500);
        assertThat(low.differentielRevenus()).isEqualTo(-1);

        // Avec seuil 1000 : revenus 1499 → ressources OK
        Regroupement10terBeResult high = Regroupement10terBeCalculator.compute(
                Regroupement10terBeLienFamilialEnum.CONJOINT,
                Regroupement10terBeTypeCarteEnum.CARTE_B,
                1_499, 24, true, true, false, 1_000);
        assertThat(high.differentielRevenus()).isEqualTo(499);
        assertThat(high.scoreEligibilite()).isEqualTo(100);
    }
}
