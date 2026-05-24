package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.MiseAPiedDisciplinaireCalculator.AnalyseMiseAPied;
import static fr.ailegalcase.casefile.MiseAPiedDisciplinaireInput.NatureMiseAPied;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-19 — tests unitaires du calculateur d'analyse de la régularité d'une
 * mise à pied disciplinaire (F-DT-48-mise-a-pied-disciplinaire, FRANCE).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict
 * REGULIERE / IRREGULIERE_FORME / IRREGULIERE_FOND / CONSERVATOIRE ;
 * salaire dû selon le verdict ; gate country FRANCE ; prescription des faits ;
 * double sanction ; durée non prévue par règlement intérieur.</p>
 */
class MiseAPiedDisciplinaireCalculatorTest {

    // ── AC1 : mise à pied disciplinaire régulière → REGULIERE, salaire dû = 0 ─

    @Test
    void disciplinaireRegulier_returnsReguliereEtSalaireDuZero() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true,  // procédure suivie
                true,  // prescription ok
                true,  // durée dans RI
                3,     // 3 jours
                true,  // salaire suspendu
                false, // pas de double sanction
                3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMiseAPied()).isEqualTo(AnalyseMiseAPied.REGULIERE);
        assertThat(r.salaireDuPeriodeMiseAPiedEuros()).isEqualTo(0.0);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.scoreMiseAPied()).isGreaterThanOrEqualTo(80);
    }

    @Test
    void disciplinaireRegulier_salaireNonSuspendu_baisseScoreEtMessage() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true, true, true, 3,
                false, // salaire non suspendu = incohérence
                false, 3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMiseAPied()).isEqualTo(AnalyseMiseAPied.REGULIERE);
        assertThat(r.messages())
                .anyMatch(m -> m.contains("salaire non suspendu"));
        assertThat(r.scoreMiseAPied()).isLessThanOrEqualTo(75);
    }

    // ── AC2 : nature conservatoire → CONSERVATOIRE, salaire dû ───────────────

    @Test
    void conservatoire_returnsConservatoireEtSalaireDu() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.CONSERVATOIRE,
                false, false, false, 5,
                true, false, 2100.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMiseAPied()).isEqualTo(AnalyseMiseAPied.CONSERVATOIRE);
        // salaire dû = (2100 / 21.67) * 5 ≈ 484.5
        assertThat(r.salaireDuPeriodeMiseAPiedEuros()).isGreaterThan(0.0);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("26/10/2010"));
    }

    @Test
    void conservatoire_aucunPointProcedureNiPrescription() {
        // Pour CONSERVATOIRE, seul le critère NATURE doit être présent.
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.CONSERVATOIRE,
                false, false, false, 5, true, false, 2100.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.pointsRegularite())
                .hasSize(1)
                .extracting(p -> p.code().name())
                .containsExactly("DT48_NATURE_MISE_A_PIED");
    }

    // ── AC3 : double sanction mêmes faits → IRREGULIERE_FOND ─────────────────

    @Test
    void doubleSanction_returnsIrreguliereFondEtSalaireDu() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true, true, true, 3, true,
                true, // sanction antérieure pour les mêmes faits
                3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMiseAPied()).isEqualTo(AnalyseMiseAPied.IRREGULIERE_FOND);
        assertThat(r.salaireDuPeriodeMiseAPiedEuros()).isGreaterThan(0.0);
        assertThat(r.messages())
                .anyMatch(m -> m.contains("Double sanction") || m.contains("double sanction"));
    }

    // ── AC4 : prescription dépassée → IRREGULIERE_FOND ───────────────────────

    @Test
    void prescriptionDepassee_returnsIrreguliereFond() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true,
                false, // prescription dépassée
                true, 3, true, false, 3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMiseAPied()).isEqualTo(AnalyseMiseAPied.IRREGULIERE_FOND);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1332-4"));
        assertThat(r.salaireDuPeriodeMiseAPiedEuros()).isGreaterThan(0.0);
    }

    // ── Vice de forme : procédure non suivie ─────────────────────────────────

    @Test
    void procedureNonSuivie_returnsIrreguliereForme() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                false, // procédure non suivie
                true, true, 3, true, false, 3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMiseAPied()).isEqualTo(AnalyseMiseAPied.IRREGULIERE_FORME);
        assertThat(r.salaireDuPeriodeMiseAPiedEuros()).isGreaterThan(0.0);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1332-2"));
    }

    // ── Vice de forme : durée non prévue au RI ───────────────────────────────

    @Test
    void dureeNonDansRi_returnsIrreguliereForme() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true, true,
                false, // durée non prévue par règlement intérieur
                3, true, false, 3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMiseAPied()).isEqualTo(AnalyseMiseAPied.IRREGULIERE_FORME);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1311-2"));
    }

    // ── Cumul vices : fond prime sur forme ───────────────────────────────────

    @Test
    void prescriptionEtProcedure_irreguliereFondPrimeSurForme() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                false, false, false, 3, true, true, 3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        // Le fond (prescription + double sanction) prime sur la forme.
        assertThat(r.analyseMiseAPied()).isEqualTo(AnalyseMiseAPied.IRREGULIERE_FOND);
    }

    // ── Nature INCONNUE → REGULIERE avec score réduit ────────────────────────

    @Test
    void natureInconnue_returnsReguliereAvecScoreReduitEtMessage() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.INCONNUE,
                true, true, true, 3, true, false, 3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMiseAPied()).isEqualTo(AnalyseMiseAPied.REGULIERE);
        assertThat(r.scoreMiseAPied()).isLessThan(85);
        assertThat(r.messages())
                .anyMatch(m -> m.contains("requalifier") || m.contains("requalification"));
        // Salaire dû = 0 uniquement si DISCIPLINAIRE — pour INCONNUE,
        // le calcul renvoie le salaire dû proratisé.
        assertThat(r.salaireDuPeriodeMiseAPiedEuros()).isGreaterThan(0.0);
    }

    // ── Calcul du salaire dû proratisé ───────────────────────────────────────

    @Test
    void salaireDu_calculProratise() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.CONSERVATOIRE,
                false, false, false, 10, true, false, 2167.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        // 2167 / 21.67 * 10 = 1000.0
        assertThat(r.salaireDuPeriodeMiseAPiedEuros()).isEqualTo(1000.0);
    }

    @Test
    void salaireDu_dureeNulle_returnsZero() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.CONSERVATOIRE,
                false, false, false, 0, true, false, 2100.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.salaireDuPeriodeMiseAPiedEuros()).isEqualTo(0.0);
    }

    // ── Erreurs / pays ───────────────────────────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true, true, true, 3, true, false, 3000.0);
        assertThatThrownBy(() ->
                MiseAPiedDisciplinaireCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                MiseAPiedDisciplinaireCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    @Test
    void natureNull_throwsIllegalArgument() {
        var input = new MiseAPiedDisciplinaireInput(
                null, true, true, true, 3, true, false, 3000.0);
        assertThatThrownBy(() ->
                MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nature");
    }

    @Test
    void dureeNegative_throwsIllegalArgument() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true, true, true, -1, true, false, 3000.0);
        assertThatThrownBy(() ->
                MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négative");
    }

    @Test
    void salaireNegatif_throwsIllegalArgument() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true, true, true, 3, true, false, -100.0);
        assertThatThrownBy(() ->
                MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négatif");
    }

    // ── Score borné ─────────────────────────────────────────────────────────

    @Test
    void score_borneEntreZeroEtCent() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true, true, true, 3, true, false, 3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.scoreMiseAPied()).isBetween(0, 100);
    }

    // ── Points d'analyse F-IA-03 ─────────────────────────────────────────────

    @Test
    void pointsRegularite_disciplinaireRegulier_couvrentLesQuatreCriteres() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true, true, true, 3, true, false, 3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.pointsRegularite())
                .extracting(p -> p.code().name())
                .contains(
                        "DT48_NATURE_MISE_A_PIED",
                        "DT48_PROCEDURE_SUIVIE",
                        "DT48_PRESCRIPTION_FAUTE",
                        "DT48_DOUBLE_SANCTION");
    }

    // ── Bases juridiques de référence ───────────────────────────────────────

    @Test
    void basesJuridiques_contiennentL1331_etCassSoc() {
        var input = new MiseAPiedDisciplinaireInput(
                NatureMiseAPied.DISCIPLINAIRE,
                true, true, true, 3, true, false, 3000.0);
        var r = MiseAPiedDisciplinaireCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1331-1"));
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("Cass. soc."));
    }
}
