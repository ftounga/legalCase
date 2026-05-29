package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-27 : tests unitaires de {@link MnaEvaluationAgeAnalyzer}. Couvre le
 * gate âge ≥ 18 ans, le calcul de l'échéance de saisine du juge des enfants
 * (5 j), la contestation de l'examen osseux, les statuts et les droits attachés.
 * {@code today} est figé pour des assertions déterministes.
 */
class MnaEvaluationAgeAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 29);

    private MnaEvaluationAgeAnalyzer analyzer() {
        return new MnaEvaluationAgeAnalyzer(TODAY);
    }

    /** Date de naissance correspondant à un âge donné (anniversaire passé). */
    private LocalDate naissancePourAge(int age) {
        return TODAY.minusYears(age).minusDays(1);
    }

    // ── Gate âge ─────────────────────────────────────────────────────────

    @Test
    void analyze_ageSuperieurOuEgal18_lanceIllegalArgumentException() {
        LocalDate majeur = naissancePourAge(18);
        assertThatThrownBy(() -> analyzer().analyze(majeur, false, null, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("18");
    }

    @Test
    void analyze_dateNaissanceFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> analyzer().analyze(
                TODAY.plusDays(1), false, null, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void analyze_dateNaissanceNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> analyzer().analyze(null, false, null, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNaissanceDeclaree");
    }

    // ── RECOURS_JE_URGENT + échéance saisine JE 5 j ──────────────────────

    @Test
    void analyze_refusASE_statutRecoursJeUrgent_echeance5Jours() {
        LocalDate dateRefus = LocalDate.of(2026, 5, 20);
        MnaEvaluationAgeResult r = analyzer().analyze(
                naissancePourAge(16), true, dateRefus, false, null);

        assertThat(r.statut()).isEqualTo(MnaEvaluationAgeStatut.RECOURS_JE_URGENT);
        assertThat(r.dateEcheanceSaisineJE()).isEqualTo(dateRefus.plusDays(5));
        assertThat(r.ageDeclare()).isEqualTo(16);
    }

    @Test
    void analyze_refusASE_sansDateRefus_echeanceNull() {
        MnaEvaluationAgeResult r = analyzer().analyze(
                naissancePourAge(15), true, null, false, null);

        assertThat(r.statut()).isEqualTo(MnaEvaluationAgeStatut.RECOURS_JE_URGENT);
        assertThat(r.dateEcheanceSaisineJE()).isNull();
    }

    // ── EXAMEN_OSSEUX_CONTESTE + liste de contestation ───────────────────

    @Test
    void analyze_examenOsseuxOrdonne_statutContesteEtMoyens() {
        MnaEvaluationAgeResult r = analyzer().analyze(
                naissancePourAge(17), false, null, true, "Âge estimé > 18 ans");

        assertThat(r.statut()).isEqualTo(MnaEvaluationAgeStatut.EXAMEN_OSSEUX_CONTESTE);
        assertThat(r.contestationExamenOsseux()).isNotEmpty();
        assertThat(r.contestationExamenOsseux()).anySatisfy(m ->
                assertThat(m).containsIgnoringCase("Greulich-Pyle"));
        assertThat(r.contestationExamenOsseux()).anySatisfy(m ->
                assertThat(m).contains("371334"));
        assertThat(r.contestationExamenOsseux()).anySatisfy(m ->
                assertThat(m).containsIgnoringCase("doute"));
    }

    @Test
    void analyze_sansExamenOsseux_contestationVide() {
        MnaEvaluationAgeResult r = analyzer().analyze(
                naissancePourAge(16), true, LocalDate.of(2026, 5, 25), false, null);
        assertThat(r.contestationExamenOsseux()).isEmpty();
    }

    // ── PRIS_EN_CHARGE + procédure ASE + droits attachés ─────────────────

    @Test
    void analyze_pasDeRefusPasExamen_statutPrisEnCharge_procedureEtDroits() {
        MnaEvaluationAgeResult r = analyzer().analyze(
                naissancePourAge(14), false, null, false, null);

        assertThat(r.statut()).isEqualTo(MnaEvaluationAgeStatut.PRIS_EN_CHARGE);
        assertThat(r.procedureASE()).hasSize(4);
        assertThat(r.procedureASE()).anySatisfy(e ->
                assertThat(e).containsIgnoringCase("juge des enfants"));
        assertThat(r.droitsAttaches()).anySatisfy(d ->
                assertThat(d).containsIgnoringCase("hébergement"));
        assertThat(r.droitsAttaches()).anySatisfy(d ->
                assertThat(d).containsIgnoringCase("scolaris"));
        assertThat(r.droitsAttaches()).anySatisfy(d ->
                assertThat(d).contains("L. 425-3"));
        assertThat(r.baseJuridique()).contains("371334");
    }
}
