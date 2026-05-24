package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.FauteInexcusableEmployeurCalculator.CodeFacteur;
import static fr.ailegalcase.casefile.FauteInexcusableEmployeurCalculator.EvaluationFauteInexcusable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-09 — tests unitaires du calculateur d'évaluation de la faute
 * inexcusable de l'employeur (F-DT-91, FRANCE — L. 452-1 à L. 452-5 CSS ;
 * Cass. ass. plén. 24/06/2005 ; L. 4121-1 CT).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict PROBABLE
 * (conscience + absence mesures), POSSIBLE / PEU_PROBABLE, alerte procédure
 * pôle social toujours présente, majoration de rente, facteurs détectés,
 * gate country FRANCE.</p>
 */
class FauteInexcusableEmployeurCalculatorTest {

    /** Input baseline — pas de faute inexcusable a priori (mesures prises + DUER + formation). */
    private static FauteInexcusableEmployeurInput baseline() {
        return new FauteInexcusableEmployeurInput(
                false,   // conscience danger non établie
                false,   // pas de signalement
                true,    // mesures prises
                true,    // DUER évalué
                true,    // formation sécurité
                10.0,    // IPP 10%
                500.0,   // rente 500 €/mois
                3500.0   // salaire brut mensuel
        );
    }

    // ── AC1 : conscience du danger + pas de mesures → PROBABLE ───────────────

    @Test
    void conscienceDanger_etPasDeMesures_returnsProbable() {
        var input = new FauteInexcusableEmployeurInput(
                true,    // conscience danger ÉTABLIE
                false,
                false,   // mesures NON prises
                true, true,
                15.0, 600.0, 3500.0
        );
        var r = FauteInexcusableEmployeurCalculator.compute(input, "FRANCE");
        assertThat(r.evaluationFauteInexcusable())
                .isEqualTo(EvaluationFauteInexcusable.FAUTE_INEXCUSABLE_PROBABLE);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.facteursFauteInexcusable())
                .anyMatch(f -> f.code() == CodeFacteur.DT91_CONSCIENCE_DANGER)
                .anyMatch(f -> f.code() == CodeFacteur.DT91_MESURES_PREVENTION);
    }

    // ── AC2 : signalement → facteur DT91_SIGNALEMENT_PRIOR ───────────────────

    @Test
    void signalementPrior_ajouteFacteurSignalement() {
        var input = new FauteInexcusableEmployeurInput(
                true, true, false, true, true,
                20.0, 700.0, 3500.0
        );
        var r = FauteInexcusableEmployeurCalculator.compute(input, "FRANCE");
        assertThat(r.facteursFauteInexcusable())
                .anyMatch(f -> f.code() == CodeFacteur.DT91_SIGNALEMENT_PRIOR);
        // Le facteur signalement doit avoir un poids fort.
        var facteurSignalement = r.facteursFauteInexcusable().stream()
                .filter(f -> f.code() == CodeFacteur.DT91_SIGNALEMENT_PRIOR)
                .findFirst().orElseThrow();
        assertThat(facteurSignalement.poids()).isGreaterThanOrEqualTo(20);
    }

    // ── AC3 : DUER non évalué → facteur DT91_DUER ────────────────────────────

    @Test
    void duerNonEvalue_ajouteFacteurDuer() {
        var input = new FauteInexcusableEmployeurInput(
                false, false, true,
                false,   // DUER non évalué
                true,
                0.0, null, 3500.0
        );
        var r = FauteInexcusableEmployeurCalculator.compute(input, "FRANCE");
        assertThat(r.facteursFauteInexcusable())
                .anyMatch(f -> f.code() == CodeFacteur.DT91_DUER);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("R. 4121-1"));
    }

    // ── AC4 : alerte procédure pôle social TOUJOURS présente (invariant) ─────

    @Test
    void alerteProcedurePolesSocial_toujoursPresente_baseline() {
        var r = FauteInexcusableEmployeurCalculator.compute(baseline(), "FRANCE");
        assertThat(r.alerteProcedurePolesSocial())
                .isNotBlank()
                .contains("pôle social")
                .contains("TJ")
                .contains("non devant le CPH");
    }

    @Test
    void alerteProcedurePolesSocial_toujoursPresente_fauteProbable() {
        var input = new FauteInexcusableEmployeurInput(
                true, true, false, false, false, 30.0, 800.0, 3500.0);
        var r = FauteInexcusableEmployeurCalculator.compute(input, "FRANCE");
        assertThat(r.alerteProcedurePolesSocial())
                .isNotBlank()
                .contains("pôle social")
                .contains("non devant le CPH");
    }

    // ── AC5 : majoration de rente calculée si IPP > 0 + rente existante ──────

    @Test
    void fauteProbable_avecRenteEtIpp_calculeMajoration() {
        var input = new FauteInexcusableEmployeurInput(
                true, true, false, false, true,
                50.0,    // IPP 50%
                800.0,   // rente 800 €/mois
                3500.0
        );
        var r = FauteInexcusableEmployeurCalculator.compute(input, "FRANCE");
        assertThat(r.evaluationFauteInexcusable())
                .isEqualTo(EvaluationFauteInexcusable.FAUTE_INEXCUSABLE_PROBABLE);
        assertThat(r.majorationRenteEstimeeEuros())
                .isNotNull()
                .isGreaterThan(0.0);
        // 800 × 50% = 400
        assertThat(r.majorationRenteEstimeeEuros()).isEqualTo(400.0);
    }

    @Test
    void faute_peuProbable_pasDeMajoration() {
        var r = FauteInexcusableEmployeurCalculator.compute(baseline(), "FRANCE");
        assertThat(r.evaluationFauteInexcusable())
                .isEqualTo(EvaluationFauteInexcusable.FAUTE_INEXCUSABLE_PEU_PROBABLE);
        assertThat(r.majorationRenteEstimeeEuros()).isNull();
    }

    @Test
    void fauteProbable_sansRente_pasDeMajoration() {
        var input = new FauteInexcusableEmployeurInput(
                true, false, false, true, true,
                15.0,
                null,   // pas de rente
                3500.0
        );
        var r = FauteInexcusableEmployeurCalculator.compute(input, "FRANCE");
        assertThat(r.evaluationFauteInexcusable())
                .isEqualTo(EvaluationFauteInexcusable.FAUTE_INEXCUSABLE_PROBABLE);
        assertThat(r.majorationRenteEstimeeEuros()).isNull();
    }

    // ── AC6 : country != FRANCE → exception ──────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                FauteInexcusableEmployeurCalculator.compute(baseline(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                FauteInexcusableEmployeurCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    @Test
    void tauxIppNegatif_throwsIllegalArgument() {
        var input = new FauteInexcusableEmployeurInput(
                false, false, true, true, true, -1.0, null, 3500.0);
        assertThatThrownBy(() ->
                FauteInexcusableEmployeurCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IPP");
    }

    @Test
    void tauxIppSuperieurA100_throwsIllegalArgument() {
        var input = new FauteInexcusableEmployeurInput(
                false, false, true, true, true, 150.0, null, 3500.0);
        assertThatThrownBy(() ->
                FauteInexcusableEmployeurCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IPP");
    }

    @Test
    void salaireNegatif_throwsIllegalArgument() {
        var input = new FauteInexcusableEmployeurInput(
                false, false, true, true, true, 10.0, null, -1.0);
        assertThatThrownBy(() ->
                FauteInexcusableEmployeurCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négatif");
    }

    @Test
    void renteNegative_throwsIllegalArgument() {
        var input = new FauteInexcusableEmployeurInput(
                false, false, true, true, true, 10.0, -10.0, 3500.0);
        assertThatThrownBy(() ->
                FauteInexcusableEmployeurCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négative");
    }

    // ── Verdict POSSIBLE ────────────────────────────────────────────────────

    @Test
    void mesuresPrisesMaisSignalement_returnsPossibleOuMieux() {
        var input = new FauteInexcusableEmployeurInput(
                false,   // pas de conscience
                true,    // signalement
                false,   // pas de mesures
                false, false,
                25.0, null, 3500.0
        );
        var r = FauteInexcusableEmployeurCalculator.compute(input, "FRANCE");
        // Sans conscience + sans mesures → on tombe sur la branche score >= 40
        // → POSSIBLE (signalement 25 + pas mesures 30 + pas DUER 15 + pas
        // formation 10 = 80).
        assertThat(r.evaluationFauteInexcusable())
                .isEqualTo(EvaluationFauteInexcusable.FAUTE_INEXCUSABLE_POSSIBLE);
    }

    // ── Préjudices personnels rappelés en cas de probable/possible ───────────

    @Test
    void fauteProbable_messagesIncluentPrejudicesPersonnels() {
        var input = new FauteInexcusableEmployeurInput(
                true, true, false, false, true, 20.0, 600.0, 3500.0);
        var r = FauteInexcusableEmployeurCalculator.compute(input, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("Préjudices personnels"))
                .anyMatch(m -> m.contains("subrogation") || m.contains("CPAM"));
    }

    // ── Score borné ──────────────────────────────────────────────────────────

    @Test
    void score_borneEntreZeroEtCent() {
        var input = new FauteInexcusableEmployeurInput(
                true, true, false, false, false, 100.0, 1000.0, 3500.0);
        var r = FauteInexcusableEmployeurCalculator.compute(input, "FRANCE");
        assertThat(r.scoreFauteInexcusable()).isBetween(0, 100);
    }

    // ── Bases juridiques L. 4121-1 / L. 452-X ────────────────────────────────

    @Test
    void basesJuridiques_contiennent_L4121_1_etL452_2() {
        var input = new FauteInexcusableEmployeurInput(
                true, false, false, true, true, 25.0, 700.0, 3500.0);
        var r = FauteInexcusableEmployeurCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 4121-1"))
                .anyMatch(b -> b.contains("L. 452-2"));
    }
}
