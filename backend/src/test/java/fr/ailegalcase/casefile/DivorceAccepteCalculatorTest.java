package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DivorceAccepteCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_pvSigneAvecDate_eligibiliteElevee() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 2, 15),
                12,
                new BigDecimal("60000"),
                new BigDecimal("30000"),
                true,
                LocalDate.of(2026, 3, 1),
                clockAt(today));

        assertThat(r.acceptationValide()).isTrue();
        assertThat(r.eligibilite()).isTrue();
        assertThat(r.ordrePublic()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictEligibilite()).isEqualTo("ELEVEE");
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(10);
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.baseJuridique()).contains("233-234").contains("1123 CPC");
    }

    @Test
    void compute_pvNonSigne_faible() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                false,
                null,
                5,
                new BigDecimal("40000"),
                new BigDecimal("35000"),
                false,
                null,
                clockAt(today));

        assertThat(r.acceptationValide()).isFalse();
        assertThat(r.eligibilite()).isFalse();
        assertThat(r.scoreGlobal()).isEqualTo(0);
        assertThat(r.verdictEligibilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).hasSize(2);
        assertThat(r.criteresNonRemplis()).anySatisfy(s -> assertThat(s).contains("PV"));
        assertThat(r.criteresNonRemplis()).anySatisfy(s -> assertThat(s).contains("Date"));
    }

    @Test
    void compute_pvSigneSansDate_acceptationInvalide() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                true,
                null,
                8,
                new BigDecimal("50000"),
                new BigDecimal("20000"),
                true,
                null,
                clockAt(today));

        assertThat(r.acceptationValide()).isFalse();
        assertThat(r.scoreGlobal()).isEqualTo(0);
        assertThat(r.verdictEligibilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(s -> assertThat(s).contains("Date d'acceptation"));
    }

    @Test
    void compute_prestationCompensatoire_formuleFR() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        // écart = |60000 - 30000| = 30000
        // base = 30000 × 0.30 × 12 × 8 = 864000
        // min = 864000 × 0.85 = 734400
        // max = 864000 × 1.15 = 993600
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 1, 1),
                10,
                new BigDecimal("60000"),
                new BigDecimal("30000"),
                true,
                null,
                clockAt(today));

        assertThat(r.prestationCompensatoireFourchetteMin())
                .isEqualByComparingTo(new BigDecimal("734400.00"));
        assertThat(r.prestationCompensatoireFourchetteMax())
                .isEqualByComparingTo(new BigDecimal("993600.00"));
    }

    @Test
    void compute_revenusEgaux_prestationZero() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 1, 1),
                7,
                new BigDecimal("45000"),
                new BigDecimal("45000"),
                false,
                null,
                clockAt(today));

        assertThat(r.prestationCompensatoireFourchetteMin())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.prestationCompensatoireFourchetteMax())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void compute_revenusNull_interprétésCommeZero() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 1, 1),
                3,
                null,
                null,
                false,
                null,
                clockAt(today));

        assertThat(r.revenusAnnuelsEpoux1Eur()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.revenusAnnuelsEpoux2Eur()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.prestationCompensatoireFourchetteMin())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void compute_messages_contiennentRedirectionFaute() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 2, 15),
                10,
                new BigDecimal("50000"),
                new BigDecimal("30000"),
                true,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("irrévocable"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("266"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("F-FA-09"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("270"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("avocat"));
    }

    @Test
    void compute_patrimoineCommun_ajouteMessageLiquidation() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 1, 15),
                15,
                new BigDecimal("55000"),
                new BigDecimal("25000"),
                true,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("F-FA-04"));
    }

    @Test
    void compute_pasPatrimoineCommun_pasMessageLiquidation() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 1, 15),
                15,
                new BigDecimal("55000"),
                new BigDecimal("25000"),
                false,
                null,
                clockAt(today));

        assertThat(r.messages()).noneSatisfy(m -> assertThat(m).contains("F-FA-04"));
    }

    @Test
    void compute_acceptationInvalide_messageComplementaire() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                false, null, 5,
                new BigDecimal("40000"), new BigDecimal("30000"),
                false, null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Acceptation invalide"));
    }

    @Test
    void compute_formule_contientDureeEtPrestation() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r = DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 1, 1),
                12,
                new BigDecimal("60000"),
                new BigDecimal("30000"),
                true,
                null,
                clockAt(today));

        assertThat(r.formule()).contains("ELEVEE").contains("100/100")
                .contains("10 mois").contains("12 ans");
    }

    @Test
    void compute_delaiProcedure_constant10Mois() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAccepteResult r1 = DivorceAccepteCalculator.compute(
                true, LocalDate.of(2026, 1, 1), 5,
                new BigDecimal("40000"), new BigDecimal("40000"),
                false, null, clockAt(today));
        DivorceAccepteResult r2 = DivorceAccepteCalculator.compute(
                false, null, 10,
                new BigDecimal("40000"), new BigDecimal("40000"),
                false, null, clockAt(today));
        assertThat(r1.delaiProcedureMoisPrevisionnel()).isEqualTo(10);
        assertThat(r2.delaiProcedureMoisPrevisionnel()).isEqualTo(10);
    }

    @Test
    void compute_dateAcceptationFuture_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 5, 1),
                10,
                new BigDecimal("40000"), new BigDecimal("40000"),
                false, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAcceptationPV");
    }

    @Test
    void compute_dateAssignationFuture_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 1, 1),
                10,
                new BigDecimal("40000"), new BigDecimal("40000"),
                false,
                LocalDate.of(2026, 5, 1),
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAssignation");
    }

    @Test
    void compute_dateAssignationBeforePV_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> DivorceAccepteCalculator.compute(
                true,
                LocalDate.of(2026, 3, 1),
                10,
                new BigDecimal("40000"), new BigDecimal("40000"),
                false,
                LocalDate.of(2026, 2, 1),
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAssignation");
    }

    @Test
    void compute_dureeMariageNegative_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> DivorceAccepteCalculator.compute(
                true, LocalDate.of(2026, 1, 1), -1,
                new BigDecimal("40000"), new BigDecimal("40000"),
                false, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeMariageAnnees");
    }

    @Test
    void compute_revenusNegatifs_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> DivorceAccepteCalculator.compute(
                true, LocalDate.of(2026, 1, 1), 5,
                new BigDecimal("-10"), new BigDecimal("40000"),
                false, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenusAnnuelsEpoux1Eur");
    }
}
