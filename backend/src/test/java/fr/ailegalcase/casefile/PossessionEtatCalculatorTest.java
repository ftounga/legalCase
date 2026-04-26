package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.PossessionEtatCalculator.DispositifApplicable;
import fr.ailegalcase.casefile.PossessionEtatCalculator.VerdictRecevabilite;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PossessionEtatCalculatorTest {

    private static final LocalDate DEBUT_LONG = LocalDate.of(2018, 1, 1);
    private static final LocalDate FIN_LONG   = LocalDate.of(2026, 1, 1); // 8 ans
    private static final LocalDate DEBUT_COURT = LocalDate.of(2024, 6, 1);
    private static final LocalDate FIN_COURT   = LocalDate.of(2026, 1, 1); // ~1 an et demi
    private static final LocalDate DEBUT_TRES_COURT = LocalDate.of(2025, 9, 1);
    private static final LocalDate FIN_TRES_COURT   = LocalDate.of(2026, 1, 1); // 4 mois

    // ============ Verdict ELEVEE — Constat notaire ============

    @Test
    void tousCriteresRemplis_dureeLongue_returnsELEVEE_dispositifConstatNotaire() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.dispositifApplicable()).isEqualTo(DispositifApplicable.CONSTAT_NOTAIRE);
        assertThat(r.dureePossessionAnnees()).isEqualTo(8);
        assertThat(r.delaiContestationActeAns()).isEqualTo(5);
        assertThat(r.delaiContestationCessationAns()).isEqualTo(10);
    }

    // ============ Verdict MOYENNE — Preuve justice ============

    @Test
    void dureeCourte_avecTousCriteres_returnsMOYENNE_dispositifPreuveJustice() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_COURT, FIN_COURT,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.MOYENNE);
        assertThat(r.dispositifApplicable()).isEqualTo(DispositifApplicable.PREUVE_JUSTICE);
        assertThat(r.dureePossessionAnnees()).isLessThan(5);
    }

    // ============ Verdict FAIBLE ============

    @Test
    void aucunCritere_returnsFAIBLE() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                false, false, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.dispositifApplicable()).isEqualTo(DispositifApplicable.AUCUN);
    }

    @Test
    void nomenAbsent_maisAutresCriteres_resteRecevable() {
        // Nomen est facultatif depuis 2005 — son absence n'empêche pas un verdict positif
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, false, true, true, true,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isIn(
                VerdictRecevabilite.ELEVEE, VerdictRecevabilite.MOYENNE);
        assertThat(r.dispositifApplicable()).isNotEqualTo(DispositifApplicable.AUCUN);
    }

    @Test
    void nonContinue_classeCommeFAIBLE() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, false, true, true,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
    }

    @Test
    void nonPaisible_classeCommeFAIBLE() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, false, true,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
    }

    @Test
    void equivoque_classeCommeFAIBLE() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, false,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
    }

    // ============ Dispositif ============

    @Test
    void dureeMoinsDe5Ans_dispositifPreuveJustice() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_COURT, FIN_COURT,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.dispositifApplicable()).isEqualTo(DispositifApplicable.PREUVE_JUSTICE);
    }

    @Test
    void dureeSuperieureA5Ans_avecConditionsRemplies_dispositifConstatNotaire() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.dispositifApplicable()).isEqualTo(DispositifApplicable.CONSTAT_NOTAIRE);
    }

    @Test
    void dispositif_aucunCritere_resteFAIBLE() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                false, false, false, false, false, false,
                "FRANCE");
        assertThat(r.dispositifApplicable()).isEqualTo(DispositifApplicable.AUCUN);
    }

    // ============ Délais ============

    @Test
    void delaiContestation_acteNotaire_5ans() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.delaiContestationActeAns()).isEqualTo(5);
    }

    @Test
    void delaiContestation_cessation_10ans() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.delaiContestationCessationAns()).isEqualTo(10);
    }

    // ============ Critères ============

    @Test
    void criteresRemplisListNonVide_quandPossessionPositive() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.criteresRemplis()).isNotEmpty();
        assertThat(r.criteresRemplis()).anyMatch(s -> s.toLowerCase().contains("tractatus"));
        assertThat(r.criteresRemplis()).anyMatch(s -> s.toLowerCase().contains("fama"));
    }

    @Test
    void criteresManquantsListNonVide_quandManquements() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_TRES_COURT, FIN_TRES_COURT,
                false, true, false, false, true, true,
                "FRANCE");
        assertThat(r.criteresManquants()).isNotEmpty();
        assertThat(r.criteresManquants()).anyMatch(s -> s.toLowerCase().contains("tractatus"));
    }

    // ============ Country ============

    @Test
    void country_BELGIQUE_throws() {
        assertThatThrownBy(() -> PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void country_FRANCE_normalized_lowercase() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    // ============ Validations ============

    @Test
    void validation_dateDebut_null_throws() {
        assertThatThrownBy(() -> PossessionEtatCalculator.compute(
                null, FIN_LONG,
                true, true, true, true, true, true,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("début");
    }

    @Test
    void validation_dateFin_null_throws() {
        assertThatThrownBy(() -> PossessionEtatCalculator.compute(
                DEBUT_LONG, null,
                true, true, true, true, true, true,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fin");
    }

    @Test
    void validation_dateFin_avant_debut_throws() {
        assertThatThrownBy(() -> PossessionEtatCalculator.compute(
                FIN_LONG, DEBUT_LONG,
                true, true, true, true, true, true,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postérieure");
    }

    // ============ Booleans null ============

    @Test
    void booleanNull_traitesCommeFalse() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                null, null, null, null, null, null,
                "FRANCE");
        assertThat(r.tractatus()).isFalse();
        assertThat(r.fama()).isFalse();
        assertThat(r.nomen()).isFalse();
        assertThat(r.continueCondition()).isFalse();
        assertThat(r.paisible()).isFalse();
        assertThat(r.nonEquivoque()).isFalse();
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
    }

    // ============ Formule + messages ============

    @Test
    void formule_contient_score_et_verdict() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.formule()).contains("score");
        assertThat(r.formule()).contains("ELEVEE");
        assertThat(r.formule()).contains("CONSTAT_NOTAIRE");
    }

    @Test
    void messages_mentionnent_artNot317() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("317"));
    }

    @Test
    void messages_mentionnent_caractereFacultatifNomen() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, false, true, true, true,
                "FRANCE");
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("facultatif") || m.toLowerCase().contains("nomen"));
    }

    @Test
    void messages_mentionnent_tribunalCompetent() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("tribunal judiciaire")
                || m.toLowerCase().contains("notaire"));
    }

    @Test
    void baseJuridique_contient_311_et_317() {
        PossessionEtatResult r = PossessionEtatCalculator.compute(
                DEBUT_LONG, FIN_LONG,
                true, true, true, true, true, true,
                "FRANCE");
        assertThat(r.baseJuridique()).contains("311-1");
        assertThat(r.baseJuridique()).contains("311-2");
        assertThat(r.baseJuridique()).contains("317");
    }
}
