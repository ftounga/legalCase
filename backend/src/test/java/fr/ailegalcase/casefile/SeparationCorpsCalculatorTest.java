package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeparationCorpsCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_consentementMutuel_3ans_consentement_possible() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2023, 1, 1), null,
                3, true, true, 0, false,
                clockAt(today));

        assertThat(r.dureeSeparationOk()).isTrue();
        assertThat(r.delaiConversion2AnsAtteint()).isTrue();
        assertThat(r.conversionAutomatiquePossible()).isTrue();
        assertThat(r.scoreEligibiliteConversion()).isEqualTo(100);
        assertThat(r.verdictConversion()).isEqualTo("POSSIBLE");
        assertThat(r.baseJuridique()).contains("296").contains("306");
    }

    @Test
    void compute_acceptationPrincipe_2ans_consentement_possible() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "ACCEPTATION_PRINCIPE",
                LocalDate.of(2024, 1, 1), null,
                2, true, false, 1, false,
                clockAt(today));

        assertThat(r.dureeSeparationOk()).isTrue();
        assertThat(r.scoreEligibiliteConversion()).isEqualTo(100);
        assertThat(r.verdictConversion()).isEqualTo("POSSIBLE");
    }

    @Test
    void compute_faute_2ans_sansConsentement_sansEnfants_possible() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        // delai 40 + pas réconciliation 20 + pas enfants 15 = 75 ≥ 70
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "FAUTE",
                LocalDate.of(2024, 1, 1), null,
                2, false, true, 0, false,
                clockAt(today));

        assertThat(r.scoreEligibiliteConversion()).isEqualTo(75);
        assertThat(r.verdictConversion()).isEqualTo("POSSIBLE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("306 al. 2"));
    }

    @Test
    void compute_alterationDefinitive_5ans_consentement_possible() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "ALTERATION_DEFINITIVE",
                LocalDate.of(2021, 1, 1), null,
                5, true, false, 0, false,
                clockAt(today));

        assertThat(r.scoreEligibiliteConversion()).isEqualTo(100);
        assertThat(r.verdictConversion()).isEqualTo("POSSIBLE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("237"));
    }

    @Test
    void compute_delaiInferieur2Ans_prematuree() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2025, 6, 1), null,
                1, true, false, 0, false,
                clockAt(today));

        assertThat(r.dureeSeparationOk()).isFalse();
        assertThat(r.delaiConversion2AnsAtteint()).isFalse();
        assertThat(r.conversionAutomatiquePossible()).isFalse();
        assertThat(r.verdictConversion()).isEqualTo("PREMATUREE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Délai de 2 ans non atteint"));
    }

    @Test
    void compute_reconciliationFormulee_bloque() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2023, 1, 1), null,
                3, true, false, 0, true,
                clockAt(today));

        assertThat(r.verdictConversion()).isEqualTo("RECONCILIATION_BLOQUE");
        assertThat(r.conversionAutomatiquePossible()).isFalse();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("305"));
    }

    @Test
    void compute_reconciliationFormuleeMaisDelaiNonAtteint_bloquePrioritaire() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "FAUTE",
                LocalDate.of(2025, 6, 1), null,
                1, false, false, 0, true,
                clockAt(today));

        // Réconciliation prioritaire sur prématurée
        assertThat(r.verdictConversion()).isEqualTo("RECONCILIATION_BLOQUE");
    }

    @Test
    void compute_delaiExactement2Ans_ok() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2024, 4, 25), null,
                2, true, false, 0, false,
                clockAt(today));

        assertThat(r.dureeSeparationOk()).isTrue();
        assertThat(r.verdictConversion()).isEqualTo("POSSIBLE");
    }

    @Test
    void compute_enfantsMineursSansConsentement_perdLeBonus15() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        // delai 40 + pas réconciliation 20 = 60 (pas de +15 et pas de +25)
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "ACCEPTATION_PRINCIPE",
                LocalDate.of(2024, 1, 1), null,
                2, false, false, 2, false,
                clockAt(today));

        assertThat(r.scoreEligibiliteConversion()).isEqualTo(60);
        assertThat(r.verdictConversion()).isEqualTo("PREMATUREE");
    }

    @Test
    void compute_enfantsMineursAvecConsentement_conserveBonus15() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        // delai 40 + consentement 25 + pas réconciliation 20 + bonus 15 = 100
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "ACCEPTATION_PRINCIPE",
                LocalDate.of(2024, 1, 1), null,
                2, true, false, 2, false,
                clockAt(today));

        assertThat(r.scoreEligibiliteConversion()).isEqualTo(100);
        assertThat(r.verdictConversion()).isEqualTo("POSSIBLE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("388-1"));
    }

    @Test
    void compute_baseJuridique_contientArticles() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2023, 1, 1), null,
                3, true, false, 0, false,
                clockAt(today));

        assertThat(r.baseJuridique()).contains("296").contains("306").contains("2004");
    }

    @Test
    void compute_formule_contientModeEtVerdict() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2023, 1, 1), null,
                3, true, false, 0, false,
                clockAt(today));

        assertThat(r.formule()).contains("CONSENTEMENT_MUTUEL").contains("3 an")
                .contains("conversion possible").contains("100/100");
    }

    @Test
    void compute_messages_contiennentReferencesArticles() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        SeparationCorpsResult r = SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2023, 1, 1), null,
                3, true, true, 0, false,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("296"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("306"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Conversion possible"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("302"));
    }

    @Test
    void compute_modeNull_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> SeparationCorpsCalculator.compute(
                null, LocalDate.of(2023, 1, 1), null,
                3, true, false, 0, false,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modeProcedure");
    }

    @Test
    void compute_modeInconnu_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> SeparationCorpsCalculator.compute(
                "DIVORCE_PAR_LASSITUDE",
                LocalDate.of(2023, 1, 1), null,
                3, true, false, 0, false,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modeProcedure inconnu");
    }

    @Test
    void compute_dureeNegative_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2023, 1, 1), null,
                -1, true, false, 0, false,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeSeparationAnnees");
    }

    @Test
    void compute_enfantsNegatif_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2023, 1, 1), null,
                3, true, false, -2, false,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("enfantsMineurs");
    }

    @Test
    void compute_dateJugementFuture_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2026, 5, 1), null,
                3, true, false, 0, false,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateJugementSeparationCorps");
    }

    @Test
    void compute_dateConversionFuture_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2026, 5, 1),
                3, true, false, 0, false,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRequeteConversion");
    }

    @Test
    void compute_dateConversionAvantJugement_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> SeparationCorpsCalculator.compute(
                "CONSENTEMENT_MUTUEL",
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 1, 1),
                3, true, false, 0, false,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRequeteConversion");
    }
}
