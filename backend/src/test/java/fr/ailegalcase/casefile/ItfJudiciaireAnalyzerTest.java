package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-37 : tests unitaires de {@link ItfJudiciaireAnalyzer}. Couvre les
 * 4 statuts (APPEL_POSSIBLE, RECOURS_PRESCRIT, RELEVE_POSSIBLE, EN_COURS_PURGE),
 * les voies de recours (délais 10 j / 5 j), les conditions de relèvement,
 * la distinction ITF / IRTF, l'échéance de relèvement (+5 ans) et la validation.
 */
class ItfJudiciaireAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 29);

    // ── APPEL_POSSIBLE : condamnation non définitive, délai d'appel non expiré ──

    @Test
    void analyze_condamnationNonDefinitive_recente_apppelPossible_voiesRecours10j5j() {
        ItfJudiciaireResult r = ItfJudiciaireAnalyzer.analyze(
                TODAY.minusDays(3), 10, "Trafic de stupéfiants", false, null, TODAY);

        assertThat(r.statut()).isEqualTo(ItfJudiciaireStatut.APPEL_POSSIBLE);
        assertThat(r.voiesRecours()).anySatisfy(v ->
                assertThat(v).contains("10").containsIgnoringCase("appel"));
        assertThat(r.voiesRecours()).anySatisfy(v ->
                assertThat(v).contains("5").containsIgnoringCase("cassation"));
    }

    // ── RECOURS_PRESCRIT : condamnation non définitive mais délai d'appel expiré ──

    @Test
    void analyze_condamnationNonDefinitive_delaiAppelExpire_recoursPrescrit() {
        ItfJudiciaireResult r = ItfJudiciaireAnalyzer.analyze(
                TODAY.minusDays(60), 5, "Vol aggravé", false, null, TODAY);

        assertThat(r.statut()).isEqualTo(ItfJudiciaireStatut.RECOURS_PRESCRIT);
        assertThat(r.voiesRecours()).anySatisfy(v ->
                assertThat(v).containsIgnoringCase("relèvement"));
    }

    // ── RELEVE_POSSIBLE : condamnation définitive et 5 ans écoulés ──

    @Test
    void analyze_condamnationDefinitive_5ansEcoules_relevePossible_requisReleve() {
        ItfJudiciaireResult r = ItfJudiciaireAnalyzer.analyze(
                TODAY.minusYears(6), 10, "Récidive", true, null, TODAY);

        assertThat(r.statut()).isEqualTo(ItfJudiciaireStatut.RELEVE_POSSIBLE);
        assertThat(r.requisReleve()).isNotEmpty();
        assertThat(r.requisReleve()).anySatisfy(c ->
                assertThat(c).containsIgnoringCase("bonne conduite"));
        assertThat(r.requisReleve()).anySatisfy(c ->
                assertThat(c).containsIgnoringCase("intégration"));
        assertThat(r.requisReleve()).anySatisfy(c ->
                assertThat(c).containsIgnoringCase("familiale"));
    }

    // ── EN_COURS_PURGE : condamnation définitive mais 5 ans non écoulés ──

    @Test
    void analyze_condamnationDefinitive_avant5ans_enCoursPurge() {
        ItfJudiciaireResult r = ItfJudiciaireAnalyzer.analyze(
                TODAY.minusYears(2), 10, "Infraction grave", true, null, TODAY);

        assertThat(r.statut()).isEqualTo(ItfJudiciaireStatut.EN_COURS_PURGE);
    }

    // ── Distinction ITF vs IRTF ──

    @Test
    void analyze_fournitDistinctionItfVsIrtf() {
        ItfJudiciaireResult r = ItfJudiciaireAnalyzer.analyze(
                TODAY.minusDays(2), 5, "Délit", false, null, TODAY);

        assertThat(r.distinctionItfVsIrtf()).containsIgnoringCase("juge pénal");
        assertThat(r.distinctionItfVsIrtf()).contains("131-30");
        assertThat(r.distinctionItfVsIrtf()).containsIgnoringCase("IRTF");
        assertThat(r.distinctionItfVsIrtf()).contains("L. 612-6");
        assertThat(r.distinctionItfVsIrtf()).containsIgnoringCase("préfet");
    }

    // ── dateEcheanceReleve = condamnation + 5 ans ──

    @Test
    void analyze_dateEcheanceReleve_egaleCondamnationPlus5Ans() {
        LocalDate condamnation = LocalDate.of(2024, 1, 15);
        ItfJudiciaireResult r = ItfJudiciaireAnalyzer.analyze(
                condamnation, 10, "Infraction", true, null, TODAY);

        assertThat(r.dateEcheanceReleve()).isEqualTo(condamnation.plusYears(5));
        assertThat(r.baseJuridique()).contains("131-30");
    }

    // ── dateEcheanceRecoursPenal explicite respectée ──

    @Test
    void analyze_dateEcheanceRecoursPenalExplicite_pilotteAppelPossible() {
        ItfJudiciaireResult r = ItfJudiciaireAnalyzer.analyze(
                TODAY.minusDays(40), 5, "Délit", false, TODAY.plusDays(2), TODAY);

        assertThat(r.statut()).isEqualTo(ItfJudiciaireStatut.APPEL_POSSIBLE);
    }

    // ── Validation ──

    @Test
    void analyze_dateCondamnationNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> ItfJudiciaireAnalyzer.analyze(
                null, 5, "X", false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateCondamnation");
    }

    @Test
    void analyze_dateCondamnationFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> ItfJudiciaireAnalyzer.analyze(
                TODAY.plusDays(1), 5, "X", false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateCondamnation");
    }

    @Test
    void analyze_infractionTropLongue_lanceIllegalArgumentException() {
        String tropLong = "x".repeat(201);
        assertThatThrownBy(() -> ItfJudiciaireAnalyzer.analyze(
                TODAY.minusDays(1), 5, tropLong, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("infractionPrincipale");
    }
}
