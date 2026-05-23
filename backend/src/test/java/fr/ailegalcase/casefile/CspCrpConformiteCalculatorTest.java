package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static fr.ailegalcase.casefile.CspCrpConformiteCalculator.CodeNonConformite;
import static fr.ailegalcase.casefile.CspCrpConformiteCalculator.ConformiteCsp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-07 — tests unitaires du calculateur de conformité de la proposition
 * CSP (FRANCE — L. 1233-65 à L. 1233-70 CT ; ANI CSP 19/07/2011 ; DARES).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : effectif &gt; 1 000,
 * CSP non proposé, document non remis, délai non mentionné, calcul ASP,
 * verdict 3 niveaux, bornes de score, validation des entrées.</p>
 */
class CspCrpConformiteCalculatorTest {

    private static final LocalDate DATE_ENTRETIEN = LocalDate.of(2026, 4, 1);
    private static final LocalDate DATE_REMISE = LocalDate.of(2026, 4, 1);

    /** Input de référence — proposition CSP conforme (effectif < 1 000, tous critères OK). */
    private static CspCrpConformiteInput cspConforme() {
        return new CspCrpConformiteInput(
                250,             // effectif < 1000
                true,            // CSP proposé
                true,            // document remis
                true,            // délai mentionné
                DATE_REMISE,     // remise jour de l'entretien
                DATE_ENTRETIEN,
                true,            // adhésion
                3000.0,          // salaire mensuel
                36000.0          // rémunération 12 mois
        );
    }

    // ── Tests verdict ─────────────────────────────────────────────────────

    @Test
    void effectifSuperieur1000_obligationNonApplicable() {
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                1500, true, true, true, DATE_REMISE, DATE_ENTRETIEN,
                true, 3000.0, 36000.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.obligationCspApplicable()).isFalse();
        assertThat(r.messages())
                .anyMatch(m -> m.contains("1500"));
        // Pas d'ASP calculée si CSP non applicable.
        assertThat(r.aspEstimeeAnnuelleEuros()).isNull();
    }

    @Test
    void effectifExactement1000_obligationNonApplicable() {
        // Seuil strict L. 1233-66 CT : entreprises de moins de 1 000.
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                1000, true, true, true, DATE_REMISE, DATE_ENTRETIEN,
                true, 3000.0, 36000.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.obligationCspApplicable()).isFalse();
    }

    @Test
    void cspNonPropose_renvoieNON_CONFORME_avecFacteurObligation() {
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                250, false, false, false, null, null,
                null, 3000.0, 36000.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.obligationCspApplicable()).isTrue();
        assertThat(r.conformiteCsp()).isEqualTo(ConformiteCsp.NON_CONFORME);
        assertThat(codes(r.pointsNonConformite()))
                .contains(CodeNonConformite.DT44_OBLIGATION_CSP);
    }

    @Test
    void documentNonRemis_facteurDocumentRemis() {
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                250, true, false, true, DATE_REMISE, DATE_ENTRETIEN,
                true, 3000.0, 36000.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(codes(r.pointsNonConformite()))
                .contains(CodeNonConformite.DT44_DOCUMENT_REMIS);
        assertThat(r.conformiteCsp()).isEqualTo(ConformiteCsp.PARTIELLEMENT_CONFORME);
    }

    @Test
    void delaiReflexionNonMentionne_facteurDelaiReflexion() {
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                250, true, true, false, DATE_REMISE, DATE_ENTRETIEN,
                true, 3000.0, 36000.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(codes(r.pointsNonConformite()))
                .contains(CodeNonConformite.DT44_DELAI_REFLEXION);
        assertThat(r.conformiteCsp()).isEqualTo(ConformiteCsp.PARTIELLEMENT_CONFORME);
    }

    @Test
    void dateRemiseAvantEntretien_facteurDateRemise() {
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                250, true, true, true,
                LocalDate.of(2026, 3, 15),    // remise avant entretien
                LocalDate.of(2026, 4, 1),
                true, 3000.0, 36000.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(codes(r.pointsNonConformite()))
                .contains(CodeNonConformite.DT44_DATE_REMISE);
    }

    @Test
    void dateRemiseLargementApresEntretien_facteurDateRemise() {
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                250, true, true, true,
                LocalDate.of(2026, 4, 20),    // remise > 7 jours après l'entretien
                LocalDate.of(2026, 4, 1),
                true, 3000.0, 36000.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(codes(r.pointsNonConformite()))
                .contains(CodeNonConformite.DT44_DATE_REMISE);
    }

    @Test
    void tousCriteresOk_renvoieConforme() {
        var r = CspCrpConformiteCalculator.compute(cspConforme(), "FRANCE");
        assertThat(r.conformiteCsp()).isEqualTo(ConformiteCsp.CONFORME);
        assertThat(r.pointsNonConformite()).isEmpty();
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void documentEtDelaiEtDateIncoherente_renvoieNonConforme() {
        // 3 vices cumulés → verdict global NON_CONFORME.
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                250, true, false, false,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 1),
                false, 3000.0, 36000.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.conformiteCsp()).isEqualTo(ConformiteCsp.NON_CONFORME);
    }

    // ── Tests ASP ──────────────────────────────────────────────────────────

    @Test
    void aspCalculee_75PourCentSjrSur12Mois() {
        // 36 000 € brut / 365 j ≈ 98.63 €/j
        // ASP = 98.63 × 0.75 ≈ 73.97 €/j
        // ASP annuelle ≈ 73.97 × 365 ≈ 27 000 €
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                250, true, true, true, DATE_REMISE, DATE_ENTRETIEN,
                true, 3000.0, 36000.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.aspEstimeeJournaliereEuros()).isNotNull();
        assertThat(r.aspEstimeeJournaliereEuros()).isBetween(70.0, 80.0);
        assertThat(r.aspEstimeeAnnuelleEuros()).isNotNull();
        assertThat(r.aspEstimeeAnnuelleEuros()).isBetween(25_000.0, 30_000.0);
        assertThat(r.dureeAspMois()).isEqualTo(12);
    }

    @Test
    void aspNulle_siRemuneration12moisNulle() {
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                250, true, true, true, DATE_REMISE, DATE_ENTRETIEN,
                true, 3000.0, 0.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.aspEstimeeJournaliereEuros()).isNull();
        assertThat(r.aspEstimeeAnnuelleEuros()).isNull();
    }

    @Test
    void dureeAspMois_egal12() {
        var r = CspCrpConformiteCalculator.compute(cspConforme(), "FRANCE");
        assertThat(r.dureeAspMois()).isEqualTo(12);
    }

    // ── Tests bases juridiques ─────────────────────────────────────────────

    @Test
    void basesJuridiques_conforme_contiennent123365() {
        var r = CspCrpConformiteCalculator.compute(cspConforme(), "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("1233-65"));
    }

    @Test
    void basesJuridiques_obligation_contient123366() {
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                250, false, false, false, null, null,
                null, 3000.0, 36000.0);
        var r = CspCrpConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("1233-66"));
    }

    @Test
    void basesJuridiques_adhesion_contient123367() {
        var r = CspCrpConformiteCalculator.compute(cspConforme(), "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("1233-67"));
    }

    // ── Tests score ────────────────────────────────────────────────────────

    @Test
    void score_borneEntreZeroEt100() {
        var r1 = CspCrpConformiteCalculator.compute(cspConforme(), "FRANCE");
        assertThat(r1.scoreConformite()).isBetween(0, 100);

        CspCrpConformiteInput violationsMultiples = new CspCrpConformiteInput(
                250, false, false, false, null, null,
                false, 3000.0, 36000.0);
        var r2 = CspCrpConformiteCalculator.compute(violationsMultiples, "FRANCE");
        assertThat(r2.scoreConformite()).isBetween(0, 100);
    }

    @Test
    void score_conforme_egal100() {
        var r = CspCrpConformiteCalculator.compute(cspConforme(), "FRANCE");
        assertThat(r.scoreConformite()).isEqualTo(100);
    }

    // ── Tests de validation ────────────────────────────────────────────────

    @Test
    void paysBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                CspCrpConformiteCalculator.compute(cspConforme(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void effectifNegatif_throwsIllegalArgument() {
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                -5, true, true, true, DATE_REMISE, DATE_ENTRETIEN,
                true, 3000.0, 36000.0);
        assertThatThrownBy(() ->
                CspCrpConformiteCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void salaireNegatif_throwsIllegalArgument() {
        CspCrpConformiteInput input = new CspCrpConformiteInput(
                250, true, true, true, DATE_REMISE, DATE_ENTRETIEN,
                true, -100.0, 36000.0);
        assertThatThrownBy(() ->
                CspCrpConformiteCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                CspCrpConformiteCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static List<CodeNonConformite> codes(
            List<CspCrpConformiteCalculator.PointNonConformite> points) {
        return points.stream()
                .map(CspCrpConformiteCalculator.PointNonConformite::code)
                .toList();
    }
}
