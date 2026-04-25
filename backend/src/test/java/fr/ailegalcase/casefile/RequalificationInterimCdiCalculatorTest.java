package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequalificationInterimCdiCalculatorTest {

    private static RequalificationInterimCdiRequest.MissionInterim mission(String start, String end, String motif, String ue) {
        return new RequalificationInterimCdiRequest.MissionInterim(
                LocalDate.parse(start), LocalDate.parse(end), motif, ue);
    }

    private static RequalificationInterimCdiRequest baseRequest() {
        return new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE",
                false,
                null,
                List.of(mission("2024-01-01", "2024-06-30", "ACCROISSEMENT", "ENT_X")),
                true,
                6,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30"),
                true
        );
    }

    @Test
    void compute_nominal_motifInterdit_returnsScore50_verdictMoyenne() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE",
                true,
                "EMPLOI_PERMANENT",
                List.of(
                        mission("2024-01-01", "2024-06-30", "ACCROISSEMENT", "ENT_X"),
                        mission("2024-08-01", "2024-12-31", "REMPLACEMENT", "ENT_X")
                ),
                true,
                12,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-12-31"),
                true
        );

        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(req);

        // 50 (motif interdit) + 0 (succession 2 < 3) + 0 (carence respectée) + 0 (motif standard)
        assertThat(r.scoreRequalification()).isEqualTo(50);
        // 50 ≥ 30 → MOYENNE (frontière supérieure < 60)
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("MOYENNE");
        assertThat(r.indemniteRequalificationEur()).isEqualByComparingTo("2500.00");
        // fin mission = 2500 × 12 × 10 % = 3000.00
        assertThat(r.indemniteFinMissionInterimEur()).isEqualByComparingTo("3000.00");
        assertThat(r.totalDommagesIndemniteEur()).isEqualByComparingTo("5500.00");
        assertThat(r.baseJuridique())
                .contains("L.1251-40")
                .contains("L.1251-41")
                .contains("L.1251-32");
        assertThat(r.formule()).contains("2500,00").contains("Fin mission");
    }

    @Test
    void compute_motifInterditTrueWithoutType_throws() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", true, null,
                List.of(mission("2024-01-01", "2024-06-30", "ACCROISSEMENT", "ENT_X")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"), true);

        assertThatThrownBy(() -> RequalificationInterimCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Type de motif interdit");
    }

    @Test
    void compute_unknownMotifInterditType_throws() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", true, "INEXISTANT",
                List.of(mission("2024-01-01", "2024-06-30", "ACCROISSEMENT", "ENT_X")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"), true);

        assertThatThrownBy(() -> RequalificationInterimCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INEXISTANT");
    }

    @Test
    void compute_unknownMotifInterimInvoque_throws() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "MOTIF_INVENTE", false, null,
                List.of(mission("2024-01-01", "2024-06-30", "X", "ENT_X")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"), true);

        assertThatThrownBy(() -> RequalificationInterimCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MOTIF_INVENTE");
    }

    @Test
    void compute_emptySuccession_throws() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"), true);

        assertThatThrownBy(() -> RequalificationInterimCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Au moins une mission");
    }

    @Test
    void compute_invalidDates_throws() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(mission("2024-06-30", "2024-01-01", "X", "ENT_X")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"), true);

        assertThatThrownBy(() -> RequalificationInterimCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#1");
    }

    @Test
    void compute_zeroDureeMissionsTotaleMois_throws() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(mission("2024-01-01", "2024-06-30", "X", "ENT_X")),
                true, 0, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"), true);

        assertThatThrownBy(() -> RequalificationInterimCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Durée missions");
    }

    @Test
    void compute_zeroSalaire_throws() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(mission("2024-01-01", "2024-06-30", "X", "ENT_X")),
                true, 6, BigDecimal.ZERO, LocalDate.parse("2024-06-30"), true);

        assertThatThrownBy(() -> RequalificationInterimCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Salaire");
    }

    @Test
    void compute_3MissionsSameUEWithoutCarence_returnsScore45() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE",
                false,
                null,
                List.of(
                        mission("2023-01-01", "2023-06-30", "ACCROISSEMENT", "ENT_X"),
                        mission("2023-07-01", "2023-12-31", "REMPLACEMENT", "ENT_X"),
                        mission("2024-01-01", "2024-06-30", "ACCROISSEMENT", "ENT_X")
                ),
                false,  // carence non respectée
                18,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30"),
                true
        );

        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(req);

        // 0 (motif licite) + 25 (succession 3+ même UE) + 20 (carence non respectée) + 0 = 45
        assertThat(r.scoreRequalification()).isEqualTo(45);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_3MissionsDifferentUE_doesNotTriggerSuccession() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE",
                false,
                null,
                List.of(
                        mission("2023-01-01", "2023-06-30", "X", "ENT_A"),
                        mission("2023-07-01", "2023-12-31", "X", "ENT_B"),
                        mission("2024-01-01", "2024-06-30", "X", "ENT_C")
                ),
                false,
                18,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30"),
                false  // UE différentes
        );

        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(req);

        // 0 + 0 (UE différentes, succession non déclenchée) + 20 (carence) + 0 = 20
        assertThat(r.scoreRequalification()).isEqualTo(20);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_motifAutre_addsTen() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "AUTRE", false, null,
                List.of(mission("2024-01-01", "2024-06-30", "X", "ENT_X")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"), true);

        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(req);
        assertThat(r.scoreRequalification()).isEqualTo(10);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_allCriteriaMet_scoreCappedAt100_verdictElevee() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "AUTRE",
                true,
                "EMPLOI_PERMANENT",
                List.of(
                        mission("2023-01-01", "2023-06-30", "X", "ENT_X"),
                        mission("2023-07-01", "2023-12-31", "X", "ENT_X"),
                        mission("2024-01-01", "2024-06-30", "X", "ENT_X")
                ),
                false,
                18,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30"),
                true
        );

        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(req);

        // 50 + 25 + 20 + 10 = 105 → plafonné à 100
        assertThat(r.scoreRequalification()).isEqualTo(100);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_baseJuridique_includesL125140_andL125132() {
        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(baseRequest());
        assertThat(r.baseJuridique()).contains("L.1251-40").contains("L.1251-41").contains("L.1251-32");
    }

    @Test
    void compute_formule_containsCalculDetails() {
        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(baseRequest());
        assertThat(r.formule())
                .contains("Indemnité requalif")
                .contains("Fin mission")
                .contains("L.1251-41");
    }

    @Test
    void compute_score60_isVerdictElevee_inclusiveBoundary() {
        // motif interdit (50) + AUTRE (10) = 60 -> ELEVEE.
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "AUTRE",
                true,
                "EMPLOI_PERMANENT",
                List.of(mission("2024-01-01", "2024-06-30", "X", "ENT_X")),
                true,
                6,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30"),
                true
        );

        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(req);
        assertThat(r.scoreRequalification()).isEqualTo(60);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_score30_isVerdictMoyenne_inclusiveBoundary() {
        // succession 3+ même UE (25) + AUTRE? non, on veut 30.
        // Solution : motif AUTRE (10) + carence non respectée (20) = 30 -> MOYENNE.
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "AUTRE",
                false,
                null,
                List.of(mission("2024-01-01", "2024-06-30", "X", "ENT_X")),
                false,
                6,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30"),
                true
        );

        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(req);
        assertThat(r.scoreRequalification()).isEqualTo(30);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_score29Equivalent_isVerdictFaible() {
        // 0 (motif licite) + 25 (succession 3+ même UE) + 0 + 0 = 25 → FAIBLE
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE",
                false,
                null,
                List.of(
                        mission("2023-01-01", "2023-06-30", "X", "ENT_X"),
                        mission("2023-07-01", "2023-12-31", "X", "ENT_X"),
                        mission("2024-01-01", "2024-06-30", "X", "ENT_X")
                ),
                true,
                18,
                new BigDecimal("2500.00"),
                LocalDate.parse("2024-06-30"),
                true
        );

        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(req);
        assertThat(r.scoreRequalification()).isEqualTo(25);
        assertThat(r.verdictProbabiliteRequalification()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_indemniteFinMission_calculatedFromTotalRemunerations() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(mission("2024-01-01", "2024-06-30", "X", "ENT_X")),
                true, 6, new BigDecimal("3000.00"), LocalDate.parse("2024-06-30"), true);

        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(req);
        // Total = 3000 × 6 = 18000, fin mission 10 % = 1800.00
        assertThat(r.indemniteFinMissionInterimEur()).isEqualByComparingTo("1800.00");
        assertThat(r.indemniteRequalificationEur()).isEqualByComparingTo("3000.00");
        assertThat(r.totalDommagesIndemniteEur()).isEqualByComparingTo("4800.00");
    }

    @Test
    void compute_messages_includePrescriptionAndCassSoc() {
        RequalificationInterimCdiResult r = RequalificationInterimCdiCalculator.compute(baseRequest());
        assertThat(r.messages()).isNotEmpty();
        assertThat(r.messages()).anyMatch(m -> m.contains("L.1471-1"));
        assertThat(r.messages()).anyMatch(m -> m.contains("L.1251-32"));
        assertThat(r.messages()).anyMatch(m -> m.contains("entreprise utilisatrice"));
    }

    @Test
    void compute_nullRequest_throws() {
        assertThatThrownBy(() -> RequalificationInterimCdiCalculator.compute(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_nullDateFinDerniereMission_throws() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(mission("2024-01-01", "2024-06-30", "X", "ENT_X")),
                true, 6, new BigDecimal("2500.00"), null, true);

        assertThatThrownBy(() -> RequalificationInterimCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date de fin");
    }

    @Test
    void compute_nullMemeEntrepriseUtilisatrice_throws() {
        RequalificationInterimCdiRequest req = new RequalificationInterimCdiRequest(
                "ACCROISSEMENT_TEMPORAIRE", false, null,
                List.of(mission("2024-01-01", "2024-06-30", "X", "ENT_X")),
                true, 6, new BigDecimal("2500.00"), LocalDate.parse("2024-06-30"), null);

        assertThatThrownBy(() -> RequalificationInterimCdiCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memeEntrepriseUtilisatrice");
    }
}
