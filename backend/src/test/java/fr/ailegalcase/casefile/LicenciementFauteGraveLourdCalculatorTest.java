package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.LicenciementFauteGraveLourdCalculator.CodeFacteur;
import static fr.ailegalcase.casefile.LicenciementFauteGraveLourdCalculator.QualificationFaute;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-01 — tests unitaires du calculateur de qualification de la faute
 * disciplinaire (faute simple / grave / lourde — FRANCE, L. 1234-1 CT ;
 * L. 1234-9 CT ; Cass. soc. 18/06/2013 n°11-14.393 pour les CP).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : qualification,
 * prescription, intention de nuire, procédure irrégulière, calcul financier,
 * règle CP maintenus, bornes de score, validation des entrées.</p>
 */
class LicenciementFauteGraveLourdCalculatorTest {

    /** Input de référence — faute grave, procédure régulière, sans prescription. */
    private static LicenciementFauteGraveLourdInput fauteGraveReguliere() {
        return new LicenciementFauteGraveLourdInput(
                "Abandon de poste répété sans justification",
                java.util.List.of(),
                null,
                false,
                36,       // 3 ans d'ancienneté
                3000.0,   // 3 000 € brut mensuel
                QualificationFaute.FAUTE_GRAVE,
                false,
                null,
                true,     // convocation régulière
                true      // lettre motivée
        );
    }

    /** Input de référence — faute simple, procédure régulière. */
    private static LicenciementFauteGraveLourdInput fauteSimpleReguliere() {
        return new LicenciementFauteGraveLourdInput(
                "Retard répété non justifié",
                java.util.List.of(),
                null,
                false,
                24,       // 2 ans d'ancienneté
                2500.0,
                QualificationFaute.FAUTE_SIMPLE,
                false,
                null,
                true,
                true
        );
    }

    // ── Tests de qualification ────────────────────────────────────────────────

    @Test
    void fauteSimple_returnsQualificationFauteSimple() {
        var r = LicenciementFauteGraveLourdCalculator.compute(fauteSimpleReguliere(), "FRANCE");
        assertThat(r.qualificationRetenue()).isEqualTo(QualificationFaute.FAUTE_SIMPLE);
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void fauteGrave_returnsQualificationFauteGrave() {
        var r = LicenciementFauteGraveLourdCalculator.compute(fauteGraveReguliere(), "FRANCE");
        assertThat(r.qualificationRetenue()).isEqualTo(QualificationFaute.FAUTE_GRAVE);
    }

    @Test
    void intentionNuireDocumentee_eleveFauteEnLourde() {
        var input = new LicenciementFauteGraveLourdInput(
                "Divulgation de secrets commerciaux à la concurrence",
                java.util.List.of(),
                null,
                false,
                48,
                4000.0,
                QualificationFaute.FAUTE_GRAVE,
                true,     // intention de nuire alléguée
                "Email prouvant la transmission de données à la société concurrente",
                true,
                true
        );
        var r = LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE");
        assertThat(r.qualificationRetenue()).isEqualTo(QualificationFaute.FAUTE_LOURDE);
    }

    @Test
    void qualificationLourdeExplicite_sansPruve_retientLourde() {
        var input = new LicenciementFauteGraveLourdInput(
                "Vol avec violence",
                java.util.List.of(),
                null,
                false,
                12,
                2000.0,
                QualificationFaute.FAUTE_LOURDE,
                false,
                null,
                true,
                true
        );
        var r = LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE");
        assertThat(r.qualificationRetenue()).isEqualTo(QualificationFaute.FAUTE_LOURDE);
    }

    // ── Tests financiers ────────────────────────────────────────────────────

    @Test
    void fauteGrave_indemnitePreavisEtIlZero() {
        var r = LicenciementFauteGraveLourdCalculator.compute(fauteGraveReguliere(), "FRANCE");
        assertThat(r.indemnitePreavisEuros()).isZero();
        assertThat(r.indemniteLegaleEuros()).isZero();
    }

    @Test
    void fauteGrave_indemniteCpMaintenu() {
        // Règle Cass. soc. 18/06/2013 : CP maintenus même en faute grave
        var r = LicenciementFauteGraveLourdCalculator.compute(fauteGraveReguliere(), "FRANCE");
        assertThat(r.indemnitesCongesPayesEuros()).isGreaterThan(0.0);
    }

    @Test
    void fauteLourde_indemnitePreavisEtIlZeroCpMaintenu() {
        // Règle Cass. soc. 18/06/2013 n°11-14.393 : CP maintenus même en faute lourde
        var input = new LicenciementFauteGraveLourdInput(
                "Sabotage des équipements",
                java.util.List.of(),
                null,
                false,
                60,
                5000.0,
                QualificationFaute.FAUTE_LOURDE,
                true,
                "PV de constat de sabotage",
                true,
                true
        );
        var r = LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE");
        assertThat(r.qualificationRetenue()).isEqualTo(QualificationFaute.FAUTE_LOURDE);
        assertThat(r.indemnitePreavisEuros()).isZero();
        assertThat(r.indemniteLegaleEuros()).isZero();
        assertThat(r.indemnitesCongesPayesEuros()).isGreaterThan(0.0);
    }

    @Test
    void fauteSimple_preavisEtIlNonZero() {
        // Faute simple — 24 mois d'ancienneté → 2 mois de préavis
        var r = LicenciementFauteGraveLourdCalculator.compute(fauteSimpleReguliere(), "FRANCE");
        assertThat(r.indemnitePreavisEuros()).isGreaterThan(0.0);
        assertThat(r.indemniteLegaleEuros()).isGreaterThan(0.0);
        assertThat(r.indemnitesCongesPayesEuros()).isGreaterThan(0.0);
    }

    @Test
    void ancienneteInferieure8Mois_ilLegaleZero() {
        // L. 1234-9 CT : IL légale dès 8 mois d'ancienneté
        var input = new LicenciementFauteGraveLourdInput(
                "Faute légère",
                java.util.List.of(),
                null,
                false,
                6,       // < 8 mois → pas d'IL légale
                1800.0,
                QualificationFaute.FAUTE_SIMPLE,
                false,
                null,
                true,
                true
        );
        var r = LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE");
        assertThat(r.indemniteLegaleEuros()).isZero();
    }

    @Test
    void totalIndemnites_egalesSommePreavisIlCp() {
        var r = LicenciementFauteGraveLourdCalculator.compute(fauteSimpleReguliere(), "FRANCE");
        double attendu = r.indemnitePreavisEuros() + r.indemniteLegaleEuros() + r.indemnitesCongesPayesEuros();
        assertThat(r.totalIndemnitesDuesEuros()).isEqualTo(attendu);
    }

    // ── Tests prescription ───────────────────────────────────────────────────

    @Test
    void prescriptionVerifiee_levieAlerte() {
        var input = new LicenciementFauteGraveLourdInput(
                "Faits remontant à 3 mois",
                java.util.List.of(),
                null,
                true,    // prescription vérifiée
                24,
                2000.0,
                QualificationFaute.FAUTE_GRAVE,
                false,
                null,
                true,
                true
        );
        var r = LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE");
        assertThat(r.alertePrescriptionFaute()).isTrue();
        assertThat(codes(r.facteursQualification()))
                .contains(CodeFacteur.DT36_PRESCRIPTION_FAUTE);
    }

    // ── Tests procédure ────────────────────────────────────────────────────

    @Test
    void convocationAbsente_detecteIrregularite() {
        var input = new LicenciementFauteGraveLourdInput(
                "Insubordination",
                java.util.List.of(),
                null,
                false,
                12,
                2000.0,
                QualificationFaute.FAUTE_GRAVE,
                false,
                null,
                false,   // pas de convocation
                true
        );
        var r = LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE");
        assertThat(codes(r.facteursQualification()))
                .contains(CodeFacteur.DT36_PROCEDURE_DISCIPLINAIRE);
    }

    @Test
    void lettreNonMotive_detecteIrregularite() {
        var input = new LicenciementFauteGraveLourdInput(
                "Insubordination",
                java.util.List.of(),
                null,
                false,
                12,
                2000.0,
                QualificationFaute.FAUTE_GRAVE,
                false,
                null,
                true,
                false    // lettre non motivée
        );
        var r = LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE");
        assertThat(codes(r.facteursQualification()))
                .contains(CodeFacteur.DT36_PROCEDURE_DISCIPLINAIRE);
    }

    // ── Tests score ───────────────────────────────────────────────────────

    @Test
    void scoreQualification_borneEntreZeroEt100() {
        for (var qi : new QualificationFaute[]{
                QualificationFaute.FAUTE_SIMPLE,
                QualificationFaute.FAUTE_GRAVE,
                QualificationFaute.FAUTE_LOURDE
        }) {
            var input = new LicenciementFauteGraveLourdInput(
                    "Faits", java.util.List.of(), null, false, 24, 2000.0, qi, false, null, true, true);
            var r = LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE");
            assertThat(r.scoreQualification())
                    .as("Score pour %s doit être [0, 100]", qi)
                    .isBetween(0, 100);
        }
    }

    // ── Tests bases juridiques ─────────────────────────────────────────────

    @Test
    void basesJuridiques_contiennentFondementQualification() {
        var r = LicenciementFauteGraveLourdCalculator.compute(fauteGraveReguliere(), "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1234-1"));
    }

    @Test
    void basesJuridiques_fauteLourde_contiennentCass2013() {
        var input = new LicenciementFauteGraveLourdInput(
                "Vol", java.util.List.of(), null, false, 12, 2000.0,
                QualificationFaute.FAUTE_LOURDE, true, "Témoignage", true, true);
        var r = LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("2013"));
    }

    // ── Tests de validation ─────────────────────────────────────────────────

    @Test
    void paysBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                LicenciementFauteGraveLourdCalculator.compute(fauteGraveReguliere(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void ancienneteNegative_throwsIllegalArgument() {
        var input = new LicenciementFauteGraveLourdInput(
                "Faits", java.util.List.of(), null, false, -1, 2000.0,
                QualificationFaute.FAUTE_GRAVE, false, null, true, true);
        assertThatThrownBy(() ->
                LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void salaireNegatif_throwsIllegalArgument() {
        var input = new LicenciementFauteGraveLourdInput(
                "Faits", java.util.List.of(), null, false, 12, -100.0,
                QualificationFaute.FAUTE_GRAVE, false, null, true, true);
        assertThatThrownBy(() ->
                LicenciementFauteGraveLourdCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                LicenciementFauteGraveLourdCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private static java.util.List<CodeFacteur> codes(
            java.util.List<LicenciementFauteGraveLourdCalculator.FacteurQualification> facteurs) {
        return facteurs.stream()
                .map(LicenciementFauteGraveLourdCalculator.FacteurQualification::code)
                .toList();
    }
}
