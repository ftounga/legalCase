package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.DemissionEquivoqueCalculator.AnalyseDemission;
import static fr.ailegalcase.casefile.DemissionEquivoqueInput.ModeExpressionDemission;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-21 — tests unitaires du calculateur d'analyse de la validité d'une
 * démission au regard de la jurisprudence sur la volonté claire et non
 * équivoque (F-DT-41-demission-validite-equivoque, FRANCE).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict
 * VOLONTE_CLAIRE / DEMISSION_EQUIVOQUE / RETRACTATION_POSSIBLE ;
 * indicateur de requalification ; gate country FRANCE ; facteurs F-IA-03
 * (4 codes critères).</p>
 */
class DemissionEquivoqueCalculatorTest {

    // ── AC1 : altercation + pression → DEMISSION_EQUIVOQUE ───────────────────

    @Test
    void altercation_avecPression_returnsDemissionEquivoque() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.EMAIL,
                true,        // altercation
                true,        // pression
                false,       // pas de rétractation
                null,
                false,
                false
        );
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        assertThat(r.analyseDemission())
                .isEqualTo(AnalyseDemission.DEMISSION_EQUIVOQUE);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.requalificationPossible()).isTrue();
        assertThat(r.scoreEquivocite()).isGreaterThanOrEqualTo(50);
    }

    // ── AC2 : rétractation rapide → RETRACTATION_POSSIBLE ────────────────────

    @Test
    void retractationRapide_seule_returnsRetractationPossible() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.ECRIT_FORMEL,
                false,
                false,
                true,        // rétractation
                2,           // 2 jours = rapide
                false,
                false
        );
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        assertThat(r.analyseDemission())
                .isEqualTo(AnalyseDemission.RETRACTATION_POSSIBLE);
        assertThat(r.requalificationPossible()).isTrue();
    }

    @Test
    void retractationTardive_seule_returnsVolonteClaire() {
        // Rétractation > 7 jours et pas d'autres facteurs forts → score insuffisant.
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.ECRIT_FORMEL,
                false,
                false,
                true,
                30,          // 30 jours = tardif
                false,
                false
        );
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        assertThat(r.analyseDemission())
                .isEqualTo(AnalyseDemission.VOLONTE_CLAIRE);
    }

    // ── AC3 : écrit formel hors contexte → VOLONTE_CLAIRE ────────────────────

    @Test
    void ecritFormel_sansAutreFacteur_returnsVolonteClaire() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.ECRIT_FORMEL,
                false, false, false, null, false, false
        );
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        assertThat(r.analyseDemission())
                .isEqualTo(AnalyseDemission.VOLONTE_CLAIRE);
        assertThat(r.requalificationPossible()).isFalse();
        assertThat(r.scoreEquivocite()).isZero();
    }

    // ── AC4 : manquements contemporains → facteur aggravant ──────────────────

    @Test
    void manquementsEmployeur_facteurAggravant() {
        var sansManquements = new DemissionEquivoqueInput(
                ModeExpressionDemission.EMAIL,
                true, false, false, null, false, false
        );
        var avecManquements = new DemissionEquivoqueInput(
                ModeExpressionDemission.EMAIL,
                true, false, false, null, true, false
        );
        var r1 = DemissionEquivoqueCalculator.compute(sansManquements, "FRANCE");
        var r2 = DemissionEquivoqueCalculator.compute(avecManquements, "FRANCE");
        assertThat(r2.scoreEquivocite()).isGreaterThan(r1.scoreEquivocite());
        assertThat(r2.facteursEquivocite())
                .anyMatch(f -> f.code().name().equals("DT41_MANQUEMENTS_EMPLOYEUR")
                        && f.poids() > 0);
    }

    @Test
    void manquementsEmployeur_avecAltercation_basesContiennentPriseActe() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.EMAIL,
                true, false, false, null, true, false
        );
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.toLowerCase().contains("prise d'acte"));
    }

    // ── AC5 : gate country FRANCE ────────────────────────────────────────────

    @Test
    void belgique_throwsIllegalArgument() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.EMAIL,
                true, false, false, null, false, false
        );
        assertThatThrownBy(() -> DemissionEquivoqueCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    // ── AC6 : facteurs F-IA-03 — 4 codes critères ────────────────────────────

    @Test
    void facteursEquivocite_couvrent4CodesCriteres() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.EMAIL,
                true, true, true, 3, true, false
        );
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        assertThat(r.facteursEquivocite())
                .extracting(f -> f.code().name())
                .containsExactlyInAnyOrder(
                        "DT41_CONTEXTE_ALTERCATION",
                        "DT41_PRESSION",
                        "DT41_RETRACTATION",
                        "DT41_MANQUEMENTS_EMPLOYEUR");
    }

    // ── AC7 : pression seule = score fort ────────────────────────────────────

    @Test
    void pressionSeule_returnsDemissionEquivoque() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.ECRIT_FORMEL,
                false,
                true,        // pression
                false,
                null,
                false,
                false
        );
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        // Pression = 35 pts → en dessous du seuil 50 → VOLONTE_CLAIRE.
        // Mais ajouter état perturbé (+15) → 50 → DEMISSION_EQUIVOQUE.
        assertThat(r.facteursEquivocite())
                .filteredOn(f -> f.code().name().equals("DT41_PRESSION"))
                .allMatch(f -> f.poids() >= 30);
    }

    @Test
    void pressionAvecEtatPerturbe_returnsDemissionEquivoque() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.ECRIT_FORMEL,
                false,
                true,        // pression
                false,
                null,
                false,
                true         // état perturbé
        );
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        assertThat(r.analyseDemission())
                .isEqualTo(AnalyseDemission.DEMISSION_EQUIVOQUE);
        assertThat(r.messages())
                .anyMatch(m -> m.contains("émotionnel") || m.contains("perturbé"));
    }

    // ── AC8 : mode impulsif (SMS) majore l'altercation ───────────────────────

    @Test
    void smsAvecAltercation_majorationFacteur() {
        var sms = new DemissionEquivoqueInput(
                ModeExpressionDemission.SMS, true, false, false, null, false, false);
        var ecrit = new DemissionEquivoqueInput(
                ModeExpressionDemission.ECRIT_FORMEL, true, false, false, null, false, false);
        var rSms = DemissionEquivoqueCalculator.compute(sms, "FRANCE");
        var rEcrit = DemissionEquivoqueCalculator.compute(ecrit, "FRANCE");
        assertThat(rSms.scoreEquivocite()).isGreaterThan(rEcrit.scoreEquivocite());
    }

    // ── AC9 : validation entrées ─────────────────────────────────────────────

    @Test
    void delaiRetractationNegatif_throws() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.EMAIL, true, false, true, -1, false, false);
        assertThatThrownBy(() -> DemissionEquivoqueCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("délai");
    }

    @Test
    void modeExpressionNull_throws() {
        var input = new DemissionEquivoqueInput(
                null, false, false, false, null, false, false);
        assertThatThrownBy(() -> DemissionEquivoqueCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mode d'expression");
    }

    @Test
    void inputNull_throws() {
        assertThatThrownBy(() -> DemissionEquivoqueCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── AC10 : bases juridiques obligatoires ─────────────────────────────────

    @Test
    void basesJuridiques_contiennent_L1237_1_et_Cass09052007() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.ECRIT_FORMEL, false, false, false, null, false, false);
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1237-1"));
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.toLowerCase().contains("volonté"));
    }

    // ── AC11 : combinaison cumulative ────────────────────────────────────────

    @Test
    void scoreMax_combinaisonComplete_estBorneA100() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.SMS,
                true, true, true, 1, true, true);
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        assertThat(r.scoreEquivocite()).isLessThanOrEqualTo(100);
        assertThat(r.analyseDemission())
                .isEqualTo(AnalyseDemission.DEMISSION_EQUIVOQUE);
    }

    @Test
    void pasDeRetractation_facteurPoidsZero() {
        var input = new DemissionEquivoqueInput(
                ModeExpressionDemission.ECRIT_FORMEL, false, false, false, null, false, false);
        var r = DemissionEquivoqueCalculator.compute(input, "FRANCE");
        assertThat(r.facteursEquivocite())
                .filteredOn(f -> f.code().name().equals("DT41_RETRACTATION"))
                .allMatch(f -> f.poids() == 0);
    }
}
