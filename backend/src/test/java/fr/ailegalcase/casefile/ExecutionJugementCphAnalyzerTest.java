package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-03 : tests unitaires de {@link ExecutionJugementCphAnalyzer}. Couvre
 * les 3 verdicts (EXECUTION_DIRECTE, RELAIS_AGS, BLOQUE_INFO_MANQUANTE), le
 * calcul des plafonds AGS (4 / 5 / 6 × PMSS), la checklist (signification
 * préalable, exécution provisoire de droit, démarches in bonis vs procédure
 * collective) et la validation.
 */
class ExecutionJugementCphAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 30);

    // ── EXECUTION_DIRECTE : employeur in bonis ──

    @Test
    void analyze_inBonis_executionDirecte_agsNonEligible() {
        ExecutionJugementCphResult r = ExecutionJugementCphAnalyzer.analyze(
                TODAY.minusDays(10), 18_000.0, true,
                ExecutionJugementCphSituationEmployeur.IN_BONIS,
                null, 36, null, TODAY);

        assertThat(r.verdict()).isEqualTo(ExecutionJugementCphVerdict.EXECUTION_DIRECTE);
        assertThat(r.agsEligible()).isFalse();
        assertThat(r.relaisAgsRecommande()).isFalse();
        assertThat(r.agsPlafondEuros()).isZero();
        // checklist orientée voie d'exécution directe (commandement / huissier)
        assertThat(r.checklist()).anySatisfy(i ->
                assertThat(i.libelle()).containsIgnoringCase("commandement"));
        assertThat(r.baseJuridique()).contains("514");
    }

    // ── RELAIS_AGS : liquidation judiciaire ──

    @Test
    void analyze_liquidation_relaisAgs_agsEligible_plafondCalcule() {
        ExecutionJugementCphResult r = ExecutionJugementCphAnalyzer.analyze(
                TODAY.minusDays(20), 25_000.0, true,
                ExecutionJugementCphSituationEmployeur.LIQUIDATION,
                TODAY.minusDays(40), 36, 4_500.0, TODAY);

        assertThat(r.verdict()).isEqualTo(ExecutionJugementCphVerdict.RELAIS_AGS);
        assertThat(r.agsEligible()).isTrue();
        assertThat(r.relaisAgsRecommande()).isTrue();
        // ancienneté 36 mois (> 24) → coefficient 6
        assertThat(r.agsCoefficientPlafond()).isEqualTo(6);
        assertThat(r.agsPlafondEuros()).isEqualTo(6 * AgsBareme.AGS_PLAFOND_MENSUEL_SS);
        assertThat(r.agsPlafondMensuelSs()).isEqualTo(AgsBareme.AGS_PLAFOND_MENSUEL_SS);
        // checklist orientée AGS / CGEA
        assertThat(r.checklist()).anySatisfy(i ->
                assertThat(i.libelle()).containsIgnoringCase("CGEA"));
        assertThat(r.checklist()).anySatisfy(i ->
                assertThat(i.libelle()).containsIgnoringCase("mandataire"));
    }

    // ── RELAIS_AGS : redressement, plafond intermédiaire selon ancienneté ──

    @Test
    void analyze_redressement_ancienneteIntermediaire_coefficient5() {
        ExecutionJugementCphResult r = ExecutionJugementCphAnalyzer.analyze(
                TODAY.minusDays(5), 12_000.0, true,
                ExecutionJugementCphSituationEmployeur.REDRESSEMENT,
                TODAY.minusDays(10), 12, null, TODAY);

        assertThat(r.verdict()).isEqualTo(ExecutionJugementCphVerdict.RELAIS_AGS);
        // ancienneté 12 mois (entre 6 et 24) → coefficient 5
        assertThat(r.agsCoefficientPlafond()).isEqualTo(5);
        assertThat(r.agsPlafondEuros()).isEqualTo(5 * AgsBareme.AGS_PLAFOND_MENSUEL_SS);
    }

    // ── Plafond AGS le plus bas : ancienneté < 6 mois → coefficient 4 ──

    @Test
    void analyze_redressement_ancienneteCourte_coefficient4() {
        ExecutionJugementCphResult r = ExecutionJugementCphAnalyzer.analyze(
                TODAY.minusDays(5), 8_000.0, true,
                ExecutionJugementCphSituationEmployeur.REDRESSEMENT,
                TODAY.minusDays(10), 3, null, TODAY);

        assertThat(r.agsCoefficientPlafond()).isEqualTo(4);
        assertThat(r.agsPlafondEuros()).isEqualTo(4 * AgsBareme.AGS_PLAFOND_MENSUEL_SS);
    }

    // ── Ancienneté absente → coefficient maximal protecteur (6) ──

    @Test
    void analyze_liquidation_ancienneteNull_coefficientMaximal() {
        ExecutionJugementCphResult r = ExecutionJugementCphAnalyzer.analyze(
                TODAY.minusDays(5), 8_000.0, true,
                ExecutionJugementCphSituationEmployeur.LIQUIDATION,
                TODAY.minusDays(10), null, null, TODAY);

        assertThat(r.agsCoefficientPlafond()).isEqualTo(6);
    }

    // ── BLOQUE_INFO_MANQUANTE : procédure collective sans date d'ouverture ──

    @Test
    void analyze_redressementSansDateOuverture_bloqueInfoManquante_itemBloquant() {
        ExecutionJugementCphResult r = ExecutionJugementCphAnalyzer.analyze(
                TODAY.minusDays(5), 10_000.0, true,
                ExecutionJugementCphSituationEmployeur.REDRESSEMENT,
                null, 24, null, TODAY);

        assertThat(r.verdict()).isEqualTo(ExecutionJugementCphVerdict.BLOQUE_INFO_MANQUANTE);
        assertThat(r.agsEligible()).isTrue();
        assertThat(r.relaisAgsRecommande()).isFalse();
        assertThat(r.agsPlafondEuros()).isZero();
        assertThat(r.checklist()).anySatisfy(item -> {
            assertThat(item.libelle()).containsIgnoringCase("date d'ouverture");
            assertThat(item.bloquant()).isTrue();
        });
    }

    // ── Checklist : signification préalable + exécution provisoire de droit ──

    @Test
    void analyze_checklist_contientSignificationEtExecutionProvisoire() {
        ExecutionJugementCphResult r = ExecutionJugementCphAnalyzer.analyze(
                TODAY.minusDays(5), 15_000.0, true,
                ExecutionJugementCphSituationEmployeur.IN_BONIS,
                null, 36, null, TODAY);

        assertThat(r.checklist()).anySatisfy(i ->
                assertThat(i.libelle()).containsIgnoringCase("signifier"));
        assertThat(r.checklist()).anySatisfy(i ->
                assertThat(i.baseJuridique()).contains("R. 1454-28"));
    }

    // ── Exécution provisoire défaut true quand null ──

    @Test
    void analyze_executionProvisoireNull_defautTrue() {
        ExecutionJugementCphResult r = ExecutionJugementCphAnalyzer.analyze(
                TODAY.minusDays(5), 15_000.0, null,
                ExecutionJugementCphSituationEmployeur.IN_BONIS,
                null, 36, null, TODAY);

        assertThat(r.executionProvisoireOrdonnee()).isTrue();
    }

    // ── Validation ──

    @Test
    void analyze_dateJugementNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> ExecutionJugementCphAnalyzer.analyze(
                null, 10_000.0, true,
                ExecutionJugementCphSituationEmployeur.IN_BONIS, null, 24, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateJugement");
    }

    @Test
    void analyze_dateJugementFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> ExecutionJugementCphAnalyzer.analyze(
                TODAY.plusDays(1), 10_000.0, true,
                ExecutionJugementCphSituationEmployeur.IN_BONIS, null, 24, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateJugement");
    }

    @Test
    void analyze_montantCondamnationZero_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> ExecutionJugementCphAnalyzer.analyze(
                TODAY.minusDays(1), 0.0, true,
                ExecutionJugementCphSituationEmployeur.IN_BONIS, null, 24, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantCondamnation");
    }

    @Test
    void analyze_situationEmployeurNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> ExecutionJugementCphAnalyzer.analyze(
                TODAY.minusDays(1), 10_000.0, true,
                null, null, 24, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("situationEmployeur");
    }
}
