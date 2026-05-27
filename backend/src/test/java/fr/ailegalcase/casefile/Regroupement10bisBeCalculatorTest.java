package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-215-05 — UT du calculator regroupement 10bis BE.
 *
 * <p>Source : Loi 15/12/1980 art. 10 et 10bis + AR 17/05/2007.
 * <p>Référence seuil ressources : 1 500 €/mois (par défaut, paramétrable application.properties).
 *
 * <p>Spécificité 10bis (vs 10ter) : règle dure {@code conditionTitreEnCours} —
 * si {@code dateFinCarteA &lt; today}, verdict INELIGIBLE peu importe le score.
 */
class Regroupement10bisBeCalculatorTest {

    private static final int SEUIL = Regroupement10bisBeCalculator.SEUIL_RESSOURCES_DEFAUT;

    /** Date pivot fixée pour reproductibilité des tests (carte A valide / expirée). */
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 27);
    private static final LocalDate CARTE_VALIDE = LocalDate.of(2027, 6, 30);
    private static final LocalDate CARTE_EXPIREE_HIER = TODAY.minusDays(1);
    private static final LocalDate CARTE_EXPIREE_TODAY = TODAY; // limite inclusive

    @Test
    void compute_toutConforme_carteValide_returnsEligible_score100() {
        Regroupement10bisBeResult r = Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.CONJOINT,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                1_600, 24, CARTE_VALIDE, true, true, false,
                SEUIL, TODAY);

        assertThat(r.scoreEligibilite()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo(Regroupement10bisBeVerdict.ELIGIBLE);
        assertThat(r.conditionTitreEnCours()).isTrue();
        assertThat(r.differentielRevenus()).isEqualTo(100);
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.seuilRessources()).isEqualTo(SEUIL);
        assertThat(r.baseJuridique()).contains("art. 10 et 10bis");
        assertThat(r.baseJuridique()).contains("AR du 17/05/2007");
        assertThat(r.dateFinCarteA()).isEqualTo(CARTE_VALIDE);
    }

    @Test
    void compute_score60_carteValide_returnsSousReserve() {
        // Ressources OK ; pas durée ; logement OK ; assurance OK ; OP OK = 90 ;
        // ici on simule score 60 par revenus KO uniquement
        Regroupement10bisBeResult r = Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.PARTENAIRE_ENREGISTRE,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                1_499, 24, CARTE_VALIDE, true, true, false,
                SEUIL, TODAY);

        assertThat(r.scoreEligibilite()).isEqualTo(60); // 0 + 20 + 20 + 10 + 10
        assertThat(r.verdict()).isEqualTo(Regroupement10bisBeVerdict.SOUS_RESERVE);
        assertThat(r.conditionTitreEnCours()).isTrue();
        assertThat(r.differentielRevenus()).isEqualTo(-1);
    }

    @Test
    void compute_score100_carteExpireeHier_returnsIneligible_overrideRegleDure() {
        // Toutes conditions remplies MAIS carte A expirée hier → INELIGIBLE forcé
        Regroupement10bisBeResult r = Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.CONJOINT,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                2_000, 24, CARTE_EXPIREE_HIER, true, true, false,
                SEUIL, TODAY);

        assertThat(r.scoreEligibilite()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo(Regroupement10bisBeVerdict.INELIGIBLE); // override
        assertThat(r.conditionTitreEnCours()).isFalse();
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Carte A expirée"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("regroupement impossible"));
    }

    @Test
    void compute_carteExpireToday_returnsConditionTitreTrue_inclusive() {
        // Limite inclusive : carte qui expire AUJOURD'HUI est encore valide
        Regroupement10bisBeResult r = Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.CONJOINT,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                1_600, 24, CARTE_EXPIREE_TODAY, true, true, false,
                SEUIL, TODAY);

        assertThat(r.conditionTitreEnCours()).isTrue();
        assertThat(r.verdict()).isEqualTo(Regroupement10bisBeVerdict.ELIGIBLE);
    }

    @Test
    void compute_revenusEgaleSeuil_returnsEligible_limiteInclusive() {
        Regroupement10bisBeResult r = Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.ENFANT_MOINS_21,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                SEUIL, 24, CARTE_VALIDE, true, true, false,
                SEUIL, TODAY);

        assertThat(r.scoreEligibilite()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo(Regroupement10bisBeVerdict.ELIGIBLE);
        assertThat(r.differentielRevenus()).isZero();
    }

    @Test
    void compute_toutInvalide_carteExpiree_returnsIneligible_score0_critereCarteEnTete() {
        Regroupement10bisBeResult r = Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.ASCENDANT_CHARGE,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                0, 0, CARTE_EXPIREE_HIER, false, false, true,
                SEUIL, TODAY);

        assertThat(r.scoreEligibilite()).isZero();
        assertThat(r.verdict()).isEqualTo(Regroupement10bisBeVerdict.INELIGIBLE);
        assertThat(r.criteresNonRemplis()).hasSize(6); // carte + 5 conditions standard
        assertThat(r.criteresNonRemplis().get(0)).contains("Carte A expirée");
    }

    @Test
    void compute_menaceOrdrePublic_score90_returnsEligible() {
        Regroupement10bisBeResult r = Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.CONJOINT,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                2_000, 24, CARTE_VALIDE, true, true, true,
                SEUIL, TODAY);

        assertThat(r.scoreEligibilite()).isEqualTo(90); // tout sauf OP
        assertThat(r.verdict()).isEqualTo(Regroupement10bisBeVerdict.ELIGIBLE);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("ordre public"));
    }

    @Test
    void compute_duree11mois_carteValide_returnsEligible80_limiteSeuilEligible() {
        // 1 critère manquant sur 100 = score 80 (durée), reste ELIGIBLE (limite haute)
        Regroupement10bisBeResult r = Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.ENFANT_21_PLUS_CHARGE,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                2_000, 11, CARTE_VALIDE, true, true, false,
                SEUIL, TODAY);

        assertThat(r.scoreEligibilite()).isEqualTo(80);
        assertThat(r.verdict()).isEqualTo(Regroupement10bisBeVerdict.ELIGIBLE);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Durée de séjour"));
    }

    @Test
    void compute_revenusNegatif_throwsIllegalArgument() {
        assertThatThrownBy(() -> Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.CONJOINT,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                -1, 24, CARTE_VALIDE, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenusMensuelsNetsRegroupant");
    }

    @Test
    void compute_revenusHorsBorne_throwsIllegalArgument() {
        assertThatThrownBy(() -> Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.CONJOINT,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                100_001, 24, CARTE_VALIDE, true, true, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dureeSejour601_throwsIllegalArgument() {
        assertThatThrownBy(() -> Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.CONJOINT,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                2_000, 601, CARTE_VALIDE, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeSejour");
    }

    @Test
    void compute_dateFinCarteANull_throwsIllegalArgument() {
        assertThatThrownBy(() -> Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.CONJOINT,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                2_000, 24, null, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFinCarteA");
    }

    @Test
    void compute_seuilParametrable_changeVerdict() {
        // Avec seuil 1500 : revenus 1499 → ressources KO (différentiel -1)
        Regroupement10bisBeResult low = Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.CONJOINT,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                1_499, 24, CARTE_VALIDE, true, true, false, 1_500, TODAY);
        assertThat(low.differentielRevenus()).isEqualTo(-1);

        // Avec seuil 1000 : revenus 1499 → ressources OK (différentiel +499)
        Regroupement10bisBeResult high = Regroupement10bisBeCalculator.compute(
                Regroupement10bisBeLienFamilialEnum.CONJOINT,
                Regroupement10bisBeTypeCarteEnum.CARTE_A,
                1_499, 24, CARTE_VALIDE, true, true, false, 1_000, TODAY);
        assertThat(high.differentielRevenus()).isEqualTo(499);
        assertThat(high.scoreEligibilite()).isEqualTo(100);
    }

    @Test
    void compute_constantePartagee_seuilEgalRegroupementBeConstants() {
        // Self-check refactor DRY SF-215-05 : la constante locale doit être strictement
        // alignée sur RegroupementBeConstants.SEUIL_RESSOURCES_MENSUEL
        assertThat(Regroupement10bisBeCalculator.SEUIL_RESSOURCES_DEFAUT)
                .isEqualTo(RegroupementBeConstants.SEUIL_RESSOURCES_MENSUEL)
                .isEqualTo(Regroupement10terBeCalculator.SEUIL_RESSOURCES_DEFAUT);
        assertThat(Regroupement10bisBeCalculator.DUREE_SEJOUR_MIN_MOIS)
                .isEqualTo(RegroupementBeConstants.DUREE_SEJOUR_MINIMALE_MOIS)
                .isEqualTo(Regroupement10terBeCalculator.DUREE_SEJOUR_MIN_MOIS);
    }
}
