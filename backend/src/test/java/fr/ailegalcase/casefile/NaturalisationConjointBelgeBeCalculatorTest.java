package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-215-09 — UT du calculator naturalisation conjoint Belge BE
 * (Code de la nationalité belge art. 16 — déclaration par mariage avec un(e) Belge).
 *
 * <p>Source : art. 16 §1 2° CNB + AR 14/01/2013 (preuve langue) + loi modificative
 * 04/12/2012.
 */
class NaturalisationConjointBelgeBeCalculatorTest {

    private static final LocalDate MARRIAGE_2019 = LocalDate.of(2019, 6, 15);
    private static final LocalDate MARRIAGE_2024 = LocalDate.of(2024, 1, 1);

    // ===== Cas nominal — ELIGIBLE =====

    @Test
    void compute_toutOK_returnsEligible_dureeManquante0() {
        NaturalisationConjointBelgeBeResult r = NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019,
                true,
                60,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false);

        assertThat(r.eligible()).isTrue();
        assertThat(r.verdict()).isEqualTo(NaturalisationConjointBelgeBeVerdict.ELIGIBLE);
        assertThat(r.dureeManquante()).isZero();
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.delaiDeclaration()).contains("officier d'état civil");
        assertThat(r.baseJuridique()).contains("art. 16");
        assertThat(r.baseJuridique()).contains("AR 14/01/2013");
    }

    @Test
    void compute_duree60exact_returnsEligible_limiteInclusive() {
        // Bord inclusif sur les 5 ans (60 mois) — art. 16 §1 2°
        NaturalisationConjointBelgeBeResult r = NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, true, 60,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false);
        assertThat(r.verdict()).isEqualTo(NaturalisationConjointBelgeBeVerdict.ELIGIBLE);
        assertThat(r.dureeManquante()).isZero();
    }

    @Test
    void compute_niveauLangueSuperieurA2_returnsEligible() {
        NaturalisationConjointBelgeBeResult r = NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, true, 72,
                NaturalisationBeNiveauLangueEnum.SUPERIEUR_A2,
                true, false, false);
        assertThat(r.verdict()).isEqualTo(NaturalisationConjointBelgeBeVerdict.ELIGIBLE);
    }

    // ===== INELIGIBLE — durée insuffisante =====

    @Test
    void compute_duree59_returnsIneligible_dureeManquante1() {
        NaturalisationConjointBelgeBeResult r = NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2024, true, 59,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false);

        assertThat(r.verdict()).isEqualTo(NaturalisationConjointBelgeBeVerdict.INELIGIBLE);
        assertThat(r.eligible()).isFalse();
        assertThat(r.dureeManquante()).isEqualTo(1);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Cohabitation < 5 ans"));
    }

    @Test
    void compute_duree0_returnsIneligible_dureeManquante60() {
        NaturalisationConjointBelgeBeResult r = NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2024, true, 0,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false);

        assertThat(r.verdict()).isEqualTo(NaturalisationConjointBelgeBeVerdict.INELIGIBLE);
        assertThat(r.dureeManquante()).isEqualTo(60);
    }

    // ===== INELIGIBLE — autres conditions =====

    @Test
    void compute_niveauLangueInferieurA2_returnsIneligible() {
        NaturalisationConjointBelgeBeResult r = NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, true, 72,
                NaturalisationBeNiveauLangueEnum.INFERIEUR_A2,
                true, false, false);
        assertThat(r.verdict()).isEqualTo(NaturalisationConjointBelgeBeVerdict.INELIGIBLE);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Niveau de langue insuffisant"));
        assertThat(r.dureeManquante()).isZero(); // condition non-temporelle
    }

    @Test
    void compute_sansPreuveIntegration_returnsIneligible() {
        NaturalisationConjointBelgeBeResult r = NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, true, 72,
                NaturalisationBeNiveauLangueEnum.A2,
                false, false, false);
        assertThat(r.verdict()).isEqualTo(NaturalisationConjointBelgeBeVerdict.INELIGIBLE);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("intégration"));
    }

    @Test
    void compute_menaceOrdrePublic_returnsIneligible() {
        NaturalisationConjointBelgeBeResult r = NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, true, 72,
                NaturalisationBeNiveauLangueEnum.A2,
                true, true, false);
        assertThat(r.verdict()).isEqualTo(NaturalisationConjointBelgeBeVerdict.INELIGIBLE);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("ordre public"));
    }

    @Test
    void compute_condamnationPenale_returnsIneligible() {
        NaturalisationConjointBelgeBeResult r = NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, true, 72,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, true);
        assertThat(r.verdict()).isEqualTo(NaturalisationConjointBelgeBeVerdict.INELIGIBLE);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Condamnation pénale"));
    }

    @Test
    void compute_toutInvalide_returnsIneligible_5criteresNonRemplis() {
        NaturalisationConjointBelgeBeResult r = NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2024, true, 0,
                NaturalisationBeNiveauLangueEnum.INFERIEUR_A2,
                false, true, true);
        assertThat(r.verdict()).isEqualTo(NaturalisationConjointBelgeBeVerdict.INELIGIBLE);
        assertThat(r.criteresNonRemplis()).hasSize(5);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Cohabitation"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Niveau de langue"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("intégration"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("ordre public"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Condamnation"));
    }

    // ===== Validation =====

    @Test
    void compute_dateMarriageNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> NaturalisationConjointBelgeBeCalculator.compute(
                null, true, 60,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateMarriage");
    }

    @Test
    void compute_dateMarriageFuture_throwsIllegalArgument() {
        assertThatThrownBy(() -> NaturalisationConjointBelgeBeCalculator.compute(
                LocalDate.now().plusDays(1), true, 60,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mariage non encore célébré");
    }

    @Test
    void compute_dureeNegatif_throwsIllegalArgument() {
        assertThatThrownBy(() -> NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, true, -1,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeCohabitationMois");
    }

    @Test
    void compute_duree601_throwsIllegalArgument() {
        assertThatThrownBy(() -> NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, true, 601,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeCohabitationMois");
    }

    @Test
    void compute_cohabitationLegaleNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, null, 60,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cohabitationLegale");
    }

    @Test
    void compute_niveauLangueNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, true, 60, null,
                true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("niveauLangue");
    }

    @Test
    void compute_preuveIntegrationNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> NaturalisationConjointBelgeBeCalculator.compute(
                MARRIAGE_2019, true, 60,
                NaturalisationBeNiveauLangueEnum.A2,
                null, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preuveIntegration");
    }
}
