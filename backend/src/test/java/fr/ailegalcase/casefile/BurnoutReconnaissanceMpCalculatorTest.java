package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.BurnoutReconnaissanceMpCalculator.AnalyseBurnoutMp;
import static fr.ailegalcase.casefile.BurnoutReconnaissanceMpCalculator.CodeCritere;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-27 — tests unitaires du calculateur d'évaluation des chances de
 * reconnaissance du burn-out comme maladie professionnelle hors tableau
 * (F-DT-64-burnout-reconnaissance-mp, FRANCE — L. 461-1 al. 4 et 5 CSS).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : 3 verdicts
 * (CHANCES_IMPORTANTES / CHANCES_MODEREES / CHANCES_FAIBLES), seuil légal
 * IPP 25 %, alerte IPP insuffisante, délai d'instruction 4-6 mois, gate
 * country FRANCE, faisceau d'indices contextuel.</p>
 */
class BurnoutReconnaissanceMpCalculatorTest {

    /** Input baseline — dossier solide : diagnostic + IPP 30 % + lien causal + surcharge. */
    private static BurnoutReconnaissanceMpInput baseline() {
        return new BurnoutReconnaissanceMpInput(
                true,          // diagnostic posé
                30.0,          // taux IPP 30 % — au-dessus du seuil 25 %
                7,             // années d'exposition
                true,          // surcharge documentée
                true,          // manquements sécurité L. 4121-1
                false,         // pas de harcèlement concomitant
                true,          // arrêts maladie multiples
                true           // lien causal direct établi
        );
    }

    // ── AC1 : taux IPP < 25 % → CHANCES_FAIBLES + alerteIPPInsuffisante ───────

    @Test
    void tauxIPPInferieurA25_returnsChancesFaibles_etAlerteIPP() {
        var input = new BurnoutReconnaissanceMpInput(
                true, 15.0, 5, true, true, false, true, true);
        var r = BurnoutReconnaissanceMpCalculator.compute(input, "FRANCE");
        assertThat(r.analyseChancesCRRMP()).isEqualTo(AnalyseBurnoutMp.CHANCES_FAIBLES);
        assertThat(r.alerteIPPInsuffisante()).isTrue();
        assertThat(r.conditionIPPRemplie()).isFalse();
        assertThat(r.messages())
                .anyMatch(m -> m.contains("25 %"))
                .anyMatch(m -> m.contains("CHANCES FAIBLES"));
    }

    // ── AC2 : diagnostic + lien causal + surcharge → CHANCES_IMPORTANTES ─────

    @Test
    void dossierSolide_returnsChancesImportantes() {
        var r = BurnoutReconnaissanceMpCalculator.compute(baseline(), "FRANCE");
        assertThat(r.analyseChancesCRRMP()).isEqualTo(AnalyseBurnoutMp.CHANCES_IMPORTANTES);
        assertThat(r.conditionIPPRemplie()).isTrue();
        assertThat(r.alerteIPPInsuffisante()).isFalse();
        assertThat(r.scoreChances()).isGreaterThanOrEqualTo(70);
    }

    // ── AC3 : conditionIPPRemplie correcte au seuil 25 % ─────────────────────

    @Test
    void tauxIPPExactement25_returnsConditionIPPRempliieTrue() {
        var input = new BurnoutReconnaissanceMpInput(
                true, 25.0, 3, true, false, false, false, true);
        var r = BurnoutReconnaissanceMpCalculator.compute(input, "FRANCE");
        assertThat(r.conditionIPPRemplie()).isTrue();
        assertThat(r.alerteIPPInsuffisante()).isFalse();
    }

    @Test
    void tauxIPPJusteSousLeSeuil_returnsConditionIPPRempliieFalse() {
        var input = new BurnoutReconnaissanceMpInput(
                true, 24.9, 3, true, false, false, false, true);
        var r = BurnoutReconnaissanceMpCalculator.compute(input, "FRANCE");
        assertThat(r.conditionIPPRemplie()).isFalse();
        assertThat(r.alerteIPPInsuffisante()).isTrue();
    }

    // ── AC4 : délai d'instruction CRRMP toujours dans [4, 6] mois ────────────

    @Test
    void delaiInstructionMoisDansFourchetteLegale() {
        var r = BurnoutReconnaissanceMpCalculator.compute(baseline(), "FRANCE");
        assertThat(r.delaiInstructionMois())
                .isBetween(BurnoutReconnaissanceMpCalculator.DELAI_INSTRUCTION_MIN_MOIS,
                           BurnoutReconnaissanceMpCalculator.DELAI_INSTRUCTION_MAX_MOIS);
    }

    // ── AC5 : gate country FRANCE ────────────────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                BurnoutReconnaissanceMpCalculator.compute(baseline(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void countryNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                BurnoutReconnaissanceMpCalculator.compute(baseline(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                BurnoutReconnaissanceMpCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    // ── AC6 : score borné [0, 100] ───────────────────────────────────────────

    @Test
    void scoreBorneEntreZeroEtCent() {
        var maxInput = new BurnoutReconnaissanceMpInput(
                true, 90.0, 15, true, true, true, true, true);
        var r = BurnoutReconnaissanceMpCalculator.compute(maxInput, "FRANCE");
        assertThat(r.scoreChances()).isBetween(0, 100);

        var minInput = new BurnoutReconnaissanceMpInput(
                false, 0.0, 0, false, false, false, false, false);
        var r2 = BurnoutReconnaissanceMpCalculator.compute(minInput, "FRANCE");
        assertThat(r2.scoreChances()).isBetween(0, 100);
    }

    // ── AC7 : facteurs F-IA-03 — les 4 codes critères sont présents ──────────

    @Test
    void facteursDossier_contiennentLes4CodesCriteres() {
        var r = BurnoutReconnaissanceMpCalculator.compute(baseline(), "FRANCE");
        assertThat(r.facteursDossier())
                .extracting(BurnoutReconnaissanceMpCalculator.FacteurDossier::code)
                .containsExactlyInAnyOrder(
                        CodeCritere.DT64_DIAGNOSTIC_POSE,
                        CodeCritere.DT64_TAUX_IPP,
                        CodeCritere.DT64_LIEN_CAUSAL,
                        CodeCritere.DT64_SURCHARGE_DOCUMENTEE);
    }

    // ── AC8 : bases juridiques contiennent L. 461-1 CSS ──────────────────────

    @Test
    void basesJuridiques_contiennent_L461_1_CSS() {
        var r = BurnoutReconnaissanceMpCalculator.compute(baseline(), "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 461-1"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("Circulaire DGT"));
    }

    @Test
    void basesJuridiques_mentionnent_Tableau_57() {
        var r = BurnoutReconnaissanceMpCalculator.compute(baseline(), "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("57"));
    }

    // ── AC9 : verdict modéré quand pièces incomplètes ────────────────────────

    @Test
    void diagnosticPoseSansLienCausalSansSurcharge_returnsChancesModerees() {
        // IPP=30 rempli + diagnostic posé → score 50 (= 20+30). Modéré, pas faible :
        // condition légale OK mais faisceau d'indices incomplet → CHANCES_MODEREES.
        var input = new BurnoutReconnaissanceMpInput(
                true, 30.0, 2, false, false, false, false, false);
        var r = BurnoutReconnaissanceMpCalculator.compute(input, "FRANCE");
        assertThat(r.analyseChancesCRRMP()).isEqualTo(AnalyseBurnoutMp.CHANCES_MODEREES);
    }

    @Test
    void sansAucunElement_avecIPPSeul25_returnsChancesFaibles() {
        // IPP juste sous le seuil et rien d'autre → CHANCES_FAIBLES.
        var input = new BurnoutReconnaissanceMpInput(
                false, 24.0, 0, false, false, false, false, false);
        var r = BurnoutReconnaissanceMpCalculator.compute(input, "FRANCE");
        assertThat(r.analyseChancesCRRMP()).isEqualTo(AnalyseBurnoutMp.CHANCES_FAIBLES);
    }

    @Test
    void diagnosticEtLienCausalSansSurcharge_returnsChancesModerees() {
        // Diagnostic + lien causal + IPP rempli + arrêts maladie mais pas de surcharge / manquements
        var input = new BurnoutReconnaissanceMpInput(
                true, 30.0, 5, false, false, false, true, true);
        var r = BurnoutReconnaissanceMpCalculator.compute(input, "FRANCE");
        assertThat(r.analyseChancesCRRMP()).isEqualTo(AnalyseBurnoutMp.CHANCES_MODEREES);
    }

    // ── AC10 : harcèlement ajoute base juridique L. 1152-1 ───────────────────

    @Test
    void harcelementConcomitant_ajouteBaseL_1152_1() {
        var input = new BurnoutReconnaissanceMpInput(
                true, 35.0, 4, true, true, true, true, true);
        var r = BurnoutReconnaissanceMpCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1152-1"));
    }

    // ── AC11 : manquements sécurité ajoute base juridique L. 4121-1 ──────────

    @Test
    void manquementsSecurite_ajouteBaseL_4121_1() {
        var r = BurnoutReconnaissanceMpCalculator.compute(baseline(), "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 4121-1"));
    }

    // ── AC12 : message mention faute inexcusable F-DT-91 ─────────────────────

    @Test
    void chancesImportantesMentionnentFauteInexcusable_F_DT_91() {
        var r = BurnoutReconnaissanceMpCalculator.compute(baseline(), "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("faute inexcusable"));
    }

    // ── AC13 : tauxIPP null traité comme 0 → alerteIPPInsuffisante = true ────

    @Test
    void tauxIPPNull_traiteCommeZero_etAlerteIPP() {
        var input = new BurnoutReconnaissanceMpInput(
                true, null, 5, true, false, false, false, true);
        var r = BurnoutReconnaissanceMpCalculator.compute(input, "FRANCE");
        assertThat(r.alerteIPPInsuffisante()).isTrue();
        assertThat(r.conditionIPPRemplie()).isFalse();
    }
}
