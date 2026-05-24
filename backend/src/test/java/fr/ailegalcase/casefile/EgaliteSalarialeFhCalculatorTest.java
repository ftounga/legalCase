package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.EgaliteSalarialeFhCalculator.AnalyseEgaliteSalariale;
import static fr.ailegalcase.casefile.EgaliteSalarialeFhInput.SexeSalarie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-23 — tests unitaires du calculateur d'analyse d'une discrimination
 * salariale fondée sur le sexe (F-DT-56-egalite-salariale-femmes-hommes,
 * FRANCE).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict
 * DISCRIMINATION_PROBABLE / POSSIBLE / PAS_DE_DISCRIMINATION_APPARENTE ;
 * charge de la preuve aménagée (L. 1144-1) ; index égalité ; comparants ;
 * justifications objectives ; prescription 5 ans (L. 1134-5) ; alerte
 * non-plafonnement toujours présente ; gate country FRANCE.</p>
 */
class EgaliteSalarialeFhCalculatorTest {

    // ── AC1 : écart > 20 % + comparants + pas de justification → PROBABLE ────

    @Test
    void ecartFort_avecComparants_sansJustifications_returnsProbable() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME,
                2800.0,   // salaire
                60,       // 5 ans
                "Chef de projet",
                3,        // 3 comparants mieux payés
                500.0,    // écart €
                22.5,     // écart %
                false, null,
                false     // pas de justifications objectives
        );
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        assertThat(r.analyseEgaliteSalariale())
                .isEqualTo(AnalyseEgaliteSalariale.DISCRIMINATION_PROBABLE);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.scoreDiscrimination()).isGreaterThanOrEqualTo(60);
    }

    @Test
    void ecartFort_messageMentionneChargeAmenagee() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2500.0, 36, "Ingé", 2, 600.0, 25.0,
                false, null, false);
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("L. 1144-1") || m.contains("charge de la preuve"));
    }

    // ── AC2 : justifications objectives + écart modéré → PAS_APPARENT ────────

    @Test
    void ecartModere_avecJustifications_returnsPasDeDiscriminationApparente() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.HOMME, 3500.0, 24, "Comptable",
                1, 200.0, 6.0,
                false, null,
                true // justifications produites
        );
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        assertThat(r.analyseEgaliteSalariale())
                .isEqualTo(AnalyseEgaliteSalariale.PAS_DE_DISCRIMINATION_APPARENTE);
    }

    @Test
    void ecartNegligeable_returnsPasDeDiscriminationApparente() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 12, "Assistant",
                0, 30.0, 1.5,
                false, null, false);
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        assertThat(r.analyseEgaliteSalariale())
                .isEqualTo(AnalyseEgaliteSalariale.PAS_DE_DISCRIMINATION_APPARENTE);
    }

    // ── AC3 : écart modéré ou index faible → POSSIBLE ────────────────────────

    @Test
    void ecartModere_sansJustifications_returnsPossible() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2600.0, 36, "Chef d'équipe",
                2, 250.0, 10.0,
                false, null, false);
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        assertThat(r.analyseEgaliteSalariale())
                .isEqualTo(AnalyseEgaliteSalariale.DISCRIMINATION_POSSIBLE);
    }

    @Test
    void indexEgaliteFaible_returnsPossibleOuProbable() {
        // Index < 75/100 → facteur aggravant. Écart modéré.
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 24, "Tech",
                1, 100.0, 5.0,
                true, 60, // index 60/100
                false);
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        assertThat(r.analyseEgaliteSalariale())
                .isIn(AnalyseEgaliteSalariale.DISCRIMINATION_POSSIBLE,
                      AnalyseEgaliteSalariale.DISCRIMINATION_PROBABLE);
        assertThat(r.facteursDisparite())
                .anyMatch(f -> f.code().name().equals("DT56_INDEX_EGALITE")
                        && f.poids() > 0);
    }

    @Test
    void indexEgaliteConforme_pasDeFacteurAggravant() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 24, "Tech",
                0, 50.0, 2.0,
                true, 90, // index 90/100 conforme
                true);
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        assertThat(r.facteursDisparite())
                .filteredOn(f -> f.code().name().equals("DT56_INDEX_EGALITE"))
                .allMatch(f -> f.poids() == 0);
    }

    // ── AC4 : alerte non-plafonnement TOUJOURS présente (invariant) ──────────

    @Test
    void alerteNonPlafonnement_toujoursPresente_quelQueSoitLeVerdict() {
        // Cas 1 — PROBABLE
        var probable = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2500.0, 36, "X", 3, 500.0, 25.0, false, null, false);
        var r1 = EgaliteSalarialeFhCalculator.compute(probable, "FRANCE");
        assertThat(r1.alerteNonPlafonnement()).isNotBlank();
        assertThat(r1.messages()).anyMatch(m -> m.contains("non plafonné") || m.contains("barème Macron"));

        // Cas 2 — POSSIBLE
        var possible = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 24, "Y", 1, 100.0, 8.0, false, null, false);
        var r2 = EgaliteSalarialeFhCalculator.compute(possible, "FRANCE");
        assertThat(r2.alerteNonPlafonnement()).isNotBlank();
        assertThat(r2.messages()).anyMatch(m -> m.contains("non plafonné") || m.contains("barème Macron"));

        // Cas 3 — PAS_DE_DISCRIMINATION_APPARENTE
        var pasApparent = new EgaliteSalarialeFhInput(
                SexeSalarie.HOMME, 3000.0, 12, "Z", 0, 0.0, 0.0, false, null, true);
        var r3 = EgaliteSalarialeFhCalculator.compute(pasApparent, "FRANCE");
        assertThat(r3.alerteNonPlafonnement()).isNotBlank();
        assertThat(r3.messages()).anyMatch(m -> m.contains("non plafonné") || m.contains("barème Macron"));
    }

    // ── AC5 : prescription 5 ans (L. 1134-5) ─────────────────────────────────

    @Test
    void prescriptionActionAns_estToujours5() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2500.0, 24, "X", 1, 100.0, 5.0, false, null, false);
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        assertThat(r.prescriptionActionAns()).isEqualTo(5);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1134-5"));
    }

    // ── AC6 : critères justifié / non justifié ───────────────────────────────

    @Test
    void justificationsObjectives_baisseScore() {
        var sansJustif = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 24, "X", 2, 300.0, 15.0, false, null, false);
        var avecJustif = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 24, "X", 2, 300.0, 15.0, false, null, true);
        var r1 = EgaliteSalarialeFhCalculator.compute(sansJustif, "FRANCE");
        var r2 = EgaliteSalarialeFhCalculator.compute(avecJustif, "FRANCE");
        assertThat(r2.scoreDiscrimination()).isLessThan(r1.scoreDiscrimination());
    }

    // ── AC7 : comparants ─────────────────────────────────────────────────────

    @Test
    void aucunComparant_pasDeBonusComparants() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 24, "X", 0, 0.0, 22.0, false, null, false);
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        // Aucun comparant → la "discrimination probable" n'est pas atteignable
        // (besoin de comparants).
        assertThat(r.analyseEgaliteSalariale())
                .isNotEqualTo(AnalyseEgaliteSalariale.DISCRIMINATION_PROBABLE);
    }

    @Test
    void unSeulComparant_apporteUnFacteurAvecPoidsReduit() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 24, "X", 1, 200.0, 8.0, false, null, false);
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        assertThat(r.facteursDisparite())
                .filteredOn(f -> f.code().name().equals("DT56_COMPARANTS"))
                .allMatch(f -> f.poids() > 0 && f.poids() < 25);
    }

    // ── AC8 : gate country ───────────────────────────────────────────────────

    @Test
    void belgique_throwsIllegalArgument() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 24, "X", 1, 100.0, 5.0, false, null, false);
        assertThatThrownBy(() -> EgaliteSalarialeFhCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    // ── AC9 : validation entrées ─────────────────────────────────────────────

    @Test
    void salaireNegatif_throws() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, -100.0, 24, "X", 1, 100.0, 5.0, false, null, false);
        assertThatThrownBy(() -> EgaliteSalarialeFhCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaire");
    }

    @Test
    void scoreIndexHorsBornes_throws() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 24, "X", 1, 100.0, 5.0, true, 150, false);
        assertThatThrownBy(() -> EgaliteSalarialeFhCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("index");
    }

    @Test
    void inputNull_throws() {
        assertThatThrownBy(() -> EgaliteSalarialeFhCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── AC10 : facteurs F-IA-03 — 4 codes critères ───────────────────────────

    @Test
    void facteursDisparite_couvrent4CodesCriteres() {
        var input = new EgaliteSalarialeFhInput(
                SexeSalarie.FEMME, 2400.0, 24, "X", 2, 300.0, 22.0, true, 60, false);
        var r = EgaliteSalarialeFhCalculator.compute(input, "FRANCE");
        assertThat(r.facteursDisparite())
                .extracting(f -> f.code().name())
                .containsExactlyInAnyOrder(
                        "DT56_ECART_SALAIRE",
                        "DT56_COMPARANTS",
                        "DT56_INDEX_EGALITE",
                        "DT56_JUSTIFICATIONS_OBJECTIVES");
    }
}
