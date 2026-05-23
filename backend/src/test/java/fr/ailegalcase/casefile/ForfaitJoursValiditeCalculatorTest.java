package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static fr.ailegalcase.casefile.ForfaitJoursValiditeCalculator.CodeFacteur;
import static fr.ailegalcase.casefile.ForfaitJoursValiditeCalculator.ValiditeForfait;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-03 — tests unitaires du calculateur de validité du forfait jours
 * (FRANCE — L. 3121-58 à L. 3121-66 CT ; Cass. soc. 29/06/2011 n°09-71.107
 * ; L. 3245-1 CT prescription 3 ans).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : accord collectif
 * absent, accord sans garantie, entretien absent, dépassement plafond,
 * verdict VALIDE/PARTIELLEMENT_NULLE/NULLE, calcul du rappel HS, bornes
 * de score, validation des entrées.</p>
 */
class ForfaitJoursValiditeCalculatorTest {

    /** Input de référence — forfait valide (tous critères OK, 218 jours). */
    private static ForfaitJoursValiditeInput forfaitValide() {
        return new ForfaitJoursValiditeInput(
                true,    // accord collectif existe
                true,    // accord garantit suivi charge
                true,    // entretien annuel réalisé
                true,    // document de contrôle mensuel
                true,    // catégorie autonome confirmée
                218,     // nb jours = seuil légal
                36,      // 3 ans d'ancienneté
                4500.0,  // 4 500 € brut mensuel
                10,      // HS estimées/semaine (si forfait nul)
                45       // 45 semaines/an
        );
    }

    // ── Tests verdict ─────────────────────────────────────────────────────

    @Test
    void accordCollectifAbsent_renvoieNULLE() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                false, false, false, false, false,
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(r.validiteForfait()).isEqualTo(ValiditeForfait.NULLE);
        assertThat(codes(r.facteursInvalidite()))
                .contains(CodeFacteur.DT50_ACCORD_COLLECTIF);
    }

    @Test
    void accordSansGarantieSuiviCharge_renvoiePartiellementNulle() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true,   // accord existe
                false,  // mais sans garantie suivi charge (Cass. 29/06/2011)
                true, true, true,
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(r.validiteForfait()).isEqualTo(ValiditeForfait.PARTIELLEMENT_NULLE);
        assertThat(codes(r.facteursInvalidite()))
                .contains(CodeFacteur.DT50_SUIVI_CHARGE);
    }

    @Test
    void entretienAnnuelAbsent_detecteFacteur() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, true, false, true, true,  // entretien absent
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(codes(r.facteursInvalidite()))
                .contains(CodeFacteur.DT50_ENTRETIEN_ANNUEL);
        assertThat(r.validiteForfait()).isNotEqualTo(ValiditeForfait.VALIDE);
    }

    @Test
    void documentControleAbsent_detecteFacteur() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, true, true, false, true,  // document contrôle absent
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(codes(r.facteursInvalidite()))
                .contains(CodeFacteur.DT50_SUIVI_JOURS);
        assertThat(r.validiteForfait()).isNotEqualTo(ValiditeForfait.VALIDE);
    }

    @Test
    void categorieAutonomeNonConfirmee_detecteFacteur() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, true, true, true, false,  // catégorie autonome non confirmée
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(codes(r.facteursInvalidite()))
                .contains(CodeFacteur.DT50_CATEGORIE_AUTONOME);
        assertThat(r.validiteForfait()).isNotEqualTo(ValiditeForfait.VALIDE);
    }

    @Test
    void nbJoursDepasse218_detecteFacteur() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, true, true, true, true,
                225,  // > 218 mais ≤ 235
                36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(codes(r.facteursInvalidite()))
                .contains(CodeFacteur.DT50_NB_JOURS);
        assertThat(r.validiteForfait()).isEqualTo(ValiditeForfait.PARTIELLEMENT_NULLE);
    }

    @Test
    void nbJoursDepasse235_renvoiePartiellementNulle() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, true, true, true, true,
                240,  // > 235 (max accord majoré)
                36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(codes(r.facteursInvalidite()))
                .contains(CodeFacteur.DT50_NB_JOURS);
        assertThat(r.validiteForfait()).isEqualTo(ValiditeForfait.PARTIELLEMENT_NULLE);
    }

    @Test
    void carencesMultiplesGarantiesMaterielles_renvoieNULLE() {
        // Entretien absent + Document absent + Catégorie autonome non
        // confirmée — carences multiples ⇒ NULLE.
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, true,
                false, false, false,
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(r.validiteForfait()).isEqualTo(ValiditeForfait.NULLE);
    }

    @Test
    void tousCriteresOk_renvoieValide() {
        var r = ForfaitJoursValiditeCalculator.compute(forfaitValide(), "FRANCE");
        assertThat(r.validiteForfait()).isEqualTo(ValiditeForfait.VALIDE);
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    // ── Tests rappel HS ────────────────────────────────────────────────────

    @Test
    void forfaitValide_rappelHsNul() {
        var r = ForfaitJoursValiditeCalculator.compute(forfaitValide(), "FRANCE");
        assertThat(r.rappelHsEstimeEuros()).isNull();
    }

    @Test
    void forfaitNul_rappelHsCalcule() {
        // Forfait nul → rappel HS calculé > 0
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                false, false, false, false, false,
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(r.validiteForfait()).isEqualTo(ValiditeForfait.NULLE);
        assertThat(r.rappelHsEstimeEuros()).isNotNull();
        assertThat(r.rappelHsEstimeEuros()).isGreaterThan(0.0);
    }

    @Test
    void forfaitPartiellementNul_rappelHsCalcule() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, false,  // accord sans garantie ⇒ partiellement nulle
                true, true, true,
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(r.validiteForfait()).isEqualTo(ValiditeForfait.PARTIELLEMENT_NULLE);
        assertThat(r.rappelHsEstimeEuros()).isNotNull();
        assertThat(r.rappelHsEstimeEuros()).isGreaterThan(0.0);
    }

    @Test
    void rappelHs_majorationsAppliquees() {
        // 10 HS/sem = 8 à 25 % + 2 à 50 %, 45 sem × 3 ans, salaire 4500 € → 151,67h
        // tauxHoraire = 4500 / 151.67 ≈ 29.67 €/h
        // par sem : (8 × 29.67 × 1.25) + (2 × 29.67 × 1.50) ≈ 296.7 + 89.0 ≈ 385.7 €
        // par an : 385.7 × 45 ≈ 17 357 € → sur 3 ans ≈ 52 070 €
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                false, false, false, false, false,
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        // tolérance : on vérifie l'ordre de grandeur (entre 40 000 et 60 000 €)
        assertThat(r.rappelHsEstimeEuros()).isBetween(40000.0, 60000.0);
    }

    @Test
    void prescriptionRappelAns_estEgaleA3() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                false, false, false, false, false,
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(r.prescriptionRappelAns()).isEqualTo(3);
    }

    // ── Tests bases juridiques ─────────────────────────────────────────────

    @Test
    void basesJuridiques_forfaitValide_contiennentL312158() {
        var r = ForfaitJoursValiditeCalculator.compute(forfaitValide(), "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("3121-58"));
    }

    @Test
    void basesJuridiques_forfaitNul_contiennentL32451() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                false, false, false, false, false,
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("3245-1"));
    }

    @Test
    void basesJuridiques_sansGarantie_contiennentCass2011() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, false, true, true, true,
                218, 36, 4500.0, 10, 45);
        var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("29/06/2011"));
    }

    // ── Tests score ────────────────────────────────────────────────────────

    @Test
    void scoreValidite_borneEntreZeroEt100() {
        for (boolean[] flags : List.of(
                new boolean[]{true, true, true, true, true},
                new boolean[]{false, false, false, false, false},
                new boolean[]{true, false, true, false, true})) {
            ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                    flags[0], flags[1], flags[2], flags[3], flags[4],
                    218, 36, 4500.0, 10, 45);
            var r = ForfaitJoursValiditeCalculator.compute(input, "FRANCE");
            assertThat(r.scoreValidite()).isBetween(0, 100);
        }
    }

    @Test
    void scoreValidite_forfaitValide_egal100() {
        var r = ForfaitJoursValiditeCalculator.compute(forfaitValide(), "FRANCE");
        assertThat(r.scoreValidite()).isEqualTo(100);
    }

    // ── Tests de validation ────────────────────────────────────────────────

    @Test
    void paysBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ForfaitJoursValiditeCalculator.compute(forfaitValide(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void ancienneteNegative_throwsIllegalArgument() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, true, true, true, true, 218, -1, 4500.0, 10, 45);
        assertThatThrownBy(() ->
                ForfaitJoursValiditeCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void salaireNegatif_throwsIllegalArgument() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, true, true, true, true, 218, 12, -100.0, 10, 45);
        assertThatThrownBy(() ->
                ForfaitJoursValiditeCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nbJoursNegatif_throwsIllegalArgument() {
        ForfaitJoursValiditeInput input = new ForfaitJoursValiditeInput(
                true, true, true, true, true, -1, 12, 4500.0, 10, 45);
        assertThatThrownBy(() ->
                ForfaitJoursValiditeCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ForfaitJoursValiditeCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static List<CodeFacteur> codes(
            List<ForfaitJoursValiditeCalculator.FacteurInvalidite> facteurs) {
        return facteurs.stream()
                .map(ForfaitJoursValiditeCalculator.FacteurInvalidite::code)
                .toList();
    }
}
