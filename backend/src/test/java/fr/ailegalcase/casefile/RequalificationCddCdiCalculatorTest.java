package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequalificationCddCdiCalculatorTest {

    private static RequalificationCddCdiRequest.CddSuccessionEntry entry(String start, String end, String motif) {
        return new RequalificationCddCdiRequest.CddSuccessionEntry(
                LocalDate.parse(start), LocalDate.parse(end), motif);
    }

    private static RequalificationCddCdiRequest baseRequest() {
        return new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE",
                false,
                null,
                List.of(entry("2024-01-01", "2024-06-30", "ACCROISSEMENT")),
                true,
                6,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30")
        );
    }

    @Test
    void compute_nominal_motifInterdit_returnsScore50_verdictMoyenne() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE",
                true,
                "EMPLOI_PERMANENT",
                List.of(
                        entry("2024-01-01", "2024-06-30", "ACCROISSEMENT"),
                        entry("2024-08-01", "2024-12-31", "REMPLACEMENT")
                ),
                true,
                12,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-12-31")
        );

        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(req);

        // 50 (motif interdit) + 0 (succession 2 < 3) + 0 (carence respectée) + 0 (motif standard)
        assertThat(r.scoreRequalification()).isEqualTo(50);
        // 50 ≥ 30 → MOYENNE (frontière supérieure < 60)
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("MOYENNE");
        assertThat(r.indemniteRequalificationEur()).isEqualByComparingTo("2500.00");
        // précarité = 2500 × 12 × 10 % = 3000.00
        assertThat(r.indemnitePrecariteEur()).isEqualByComparingTo("3000.00");
        assertThat(r.totalDommagesIndemniteEur()).isEqualByComparingTo("5500.00");
        assertThat(r.baseJuridique())
                .contains("L.1245-2")
                .contains("L.1243-8");
        assertThat(r.formule()).contains("2500,00").contains("Précarité");
    }

    @Test
    void compute_motifInterditTrueWithoutType_throws() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", true, null,
                List.of(entry("2024-01-01", "2024-06-30", "ACCROISSEMENT")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"));

        assertThatThrownBy(() -> RequalificationCddCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Type de motif interdit");
    }

    @Test
    void compute_unknownMotifInterditType_throws() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", true, "INEXISTANT",
                List.of(entry("2024-01-01", "2024-06-30", "ACCROISSEMENT")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"));

        assertThatThrownBy(() -> RequalificationCddCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INEXISTANT");
    }

    @Test
    void compute_unknownMotifCddInvoque_throws() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "MOTIF_INVENTE", false, null,
                List.of(entry("2024-01-01", "2024-06-30", "X")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"));

        assertThatThrownBy(() -> RequalificationCddCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MOTIF_INVENTE");
    }

    @Test
    void compute_emptySuccession_throws() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"));

        assertThatThrownBy(() -> RequalificationCddCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Au moins un CDD");
    }

    @Test
    void compute_invalidDates_throws() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(entry("2024-06-30", "2024-01-01", "X")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"));

        assertThatThrownBy(() -> RequalificationCddCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#1");
    }

    @Test
    void compute_zeroDureeContratMois_throws() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(entry("2024-01-01", "2024-06-30", "X")),
                true, 0, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"));

        assertThatThrownBy(() -> RequalificationCddCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Durée contrat");
    }

    @Test
    void compute_zeroSalaire_throws() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(entry("2024-01-01", "2024-06-30", "X")),
                true, 6, BigDecimal.ZERO, LocalDate.parse("2024-06-30"));

        assertThatThrownBy(() -> RequalificationCddCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Salaire");
    }

    @Test
    void compute_3CddSuccessionsWithoutCarence_returnsScore40() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE",
                false,
                null,
                List.of(
                        entry("2023-01-01", "2023-06-30", "ACCROISSEMENT"),
                        entry("2023-07-01", "2023-12-31", "REMPLACEMENT"),
                        entry("2024-01-01", "2024-06-30", "ACCROISSEMENT")
                ),
                false,  // carence non respectée
                18,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30")
        );

        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(req);

        // 0 (motif licite) + 20 (succession ≥ 3) + 20 (carence non respectée) + 0 = 40
        assertThat(r.scoreRequalification()).isEqualTo(40);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_motifAutre_addsTen() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "AUTRE", false, null,
                List.of(entry("2024-01-01", "2024-06-30", "X")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"));

        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(req);
        assertThat(r.scoreRequalification()).isEqualTo(10);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_motifEmploiUsage_addsTen() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "EMPLOI_USAGE", false, null,
                List.of(entry("2024-01-01", "2024-06-30", "X")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"));

        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(req);
        assertThat(r.scoreRequalification()).isEqualTo(10);
    }

    @Test
    void compute_allCriteriaMet_scoreCappedAt100_verdictElevee() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "AUTRE",
                true,
                "EMPLOI_PERMANENT",
                List.of(
                        entry("2023-01-01", "2023-06-30", "X"),
                        entry("2023-07-01", "2023-12-31", "X"),
                        entry("2024-01-01", "2024-06-30", "X")
                ),
                false,
                18,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30")
        );

        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(req);

        // 50 + 20 + 20 + 10 = 100 (plafonné)
        assertThat(r.scoreRequalification()).isEqualTo(100);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_baseJuridique_includesL12452_andL12438() {
        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(baseRequest());
        assertThat(r.baseJuridique()).contains("L.1245-2").contains("L.1243-8");
    }

    @Test
    void compute_formule_containsCalculDetails() {
        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(baseRequest());
        assertThat(r.formule())
                .contains("Indemnité requalification")
                .contains("Précarité")
                .contains("L.1245-2");
    }

    @Test
    void compute_score60_isVerdictElevee_inclusiveBoundary() {
        // motif interdit (50) + succession 3+ (20) = 70 -> ELEVEE.
        // Pour atteindre exactement 60, on prend motif interdit (50) + AUTRE (10) = 60.
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "AUTRE",
                true,
                "EMPLOI_PERMANENT",
                List.of(entry("2024-01-01", "2024-06-30", "X")),
                true,
                6,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30")
        );

        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(req);
        assertThat(r.scoreRequalification()).isEqualTo(60);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_score30_isVerdictMoyenne_inclusiveBoundary() {
        // succession 3+ (20) + AUTRE (10) = 30 -> MOYENNE.
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "AUTRE",
                false,
                null,
                List.of(
                        entry("2023-01-01", "2023-06-30", "X"),
                        entry("2023-07-01", "2023-12-31", "X"),
                        entry("2024-01-01", "2024-06-30", "X")
                ),
                true,
                18,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30")
        );

        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(req);
        assertThat(r.scoreRequalification()).isEqualTo(30);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_score29_isVerdictFaible() {
        // Pas de combinaison qui donne exactement 29 (granularité 10/20/50).
        // On vérifie un score 20 (succession 3+) → FAIBLE.
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE",
                false,
                null,
                List.of(
                        entry("2023-01-01", "2023-06-30", "X"),
                        entry("2023-07-01", "2023-12-31", "X"),
                        entry("2024-01-01", "2024-06-30", "X")
                ),
                true,
                18,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30")
        );

        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(req);
        assertThat(r.scoreRequalification()).isEqualTo(20);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_indemnitePrecarite_calculatedFromTotalRemunerations() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(entry("2024-01-01", "2024-06-30", "X")),
                true, 6, new BigDecimal("3000.00"), LocalDate.parse("2024-06-30"));

        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(req);
        // Total = 3000 × 6 = 18000, précarité 10 % = 1800.00
        assertThat(r.indemnitePrecariteEur()).isEqualByComparingTo("1800.00");
        assertThat(r.indemniteRequalificationEur()).isEqualByComparingTo("3000.00");
        assertThat(r.totalDommagesIndemniteEur()).isEqualByComparingTo("4800.00");
    }

    @Test
    void compute_messages_includePrescriptionAndPrecarite() {
        RequalificationCddCdiResult r = RequalificationCddCdiCalculator.compute(baseRequest());
        assertThat(r.messages()).isNotEmpty();
        assertThat(r.messages()).anyMatch(m -> m.contains("L.1471-1"));
        assertThat(r.messages()).anyMatch(m -> m.contains("L.1243-8"));
    }

    @Test
    void compute_nullRequest_throws() {
        assertThatThrownBy(() -> RequalificationCddCdiCalculator.compute(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_nullDateFinDernierContrat_throws() {
        RequalificationCddCdiRequest req = new RequalificationCddCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(entry("2024-01-01", "2024-06-30", "X")),
                true, 6, new BigDecimal("2500.00"), null);

        assertThatThrownBy(() -> RequalificationCddCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date de fin");
    }
}
