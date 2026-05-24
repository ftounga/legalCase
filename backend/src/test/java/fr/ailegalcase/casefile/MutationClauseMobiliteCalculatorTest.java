package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.MutationClauseMobiliteCalculator.AnalyseMutation;
import static fr.ailegalcase.casefile.MutationClauseMobiliteCalculator.TypeConsequence;
import static fr.ailegalcase.casefile.MutationClauseMobiliteInput.ReponseSalarie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-13 — tests unitaires du calculateur d'analyse de la validité d'une
 * clause de mobilité et des conséquences d'un refus de mutation
 * (F-DT-71-mutation-clause-mobilite, FRANCE — Cass. soc. constante).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict
 * CLAUSE_VALIDE / CLAUSE_INVALIDE / ABSENCE_CLAUSE ; délai de prévenance
 * insuffisant ; situation familiale contraignante ; gate country FRANCE ;
 * conséquences du refus selon le verdict.</p>
 */
class MutationClauseMobiliteCalculatorTest {

    // ── AC1 : clause valide + zone précise + délai suffisant → CLAUSE_VALIDE ─

    @Test
    void clauseValide_zonePrecise_delaiSuffisant_returnsClauseValide() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 4, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMutation()).isEqualTo(AnalyseMutation.CLAUSE_VALIDE);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.scoreMutation()).isGreaterThanOrEqualTo(80);
    }

    @Test
    void clauseValide_refusSalarie_returnsRefusFauteLicenciementPossible() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 4, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.REFUS_FAUTE_LICENCIEMENT_POSSIBLE);
        assertThat(r.consequencesRefus()).contains("faute");
    }

    // ── AC2 : zone imprécise → CLAUSE_INVALIDE ──────────────────────────────

    @Test
    void clausePresente_zoneImprecise_returnsClauseInvalide() {
        var input = new MutationClauseMobiliteInput(
                true, false, true, 4, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMutation()).isEqualTo(AnalyseMutation.CLAUSE_INVALIDE);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("24/01/2008"));
    }

    @Test
    void clauseInvalide_refusSalarie_returnsRefusSansFaute() {
        var input = new MutationClauseMobiliteInput(
                true, false, true, 4, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.REFUS_SANS_FAUTE_CLAUSE_INVALIDE);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1235-3"));
    }

    // ── AC3 : délai de prévenance < 2 semaines → facteur invalidant ──────────

    @Test
    void clausePresente_delaiInsuffisant_returnsClauseInvalide() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 1, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMutation()).isEqualTo(AnalyseMutation.CLAUSE_INVALIDE);
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.DELAI_PREVENANCE_INSUFFISANT);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("03/03/2010"));
    }

    @Test
    void delaiPrevenanceZero_returnsClauseInvalide() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 0, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMutation()).isEqualTo(AnalyseMutation.CLAUSE_INVALIDE);
    }

    @Test
    void delaiPrevenanceNull_returnsClauseValide_avecPointADocumenter() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, null, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMutation()).isEqualTo(AnalyseMutation.CLAUSE_VALIDE);
        assertThat(r.pointsAnalyse())
                .anyMatch(p -> p.conclusion().contains("DOCUMENTER"));
    }

    // ── AC4 : absence de clause → ABSENCE_CLAUSE ─────────────────────────────

    @Test
    void absenceClause_returnsAbsenceClause() {
        var input = new MutationClauseMobiliteInput(
                false, false, false, null, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMutation()).isEqualTo(AnalyseMutation.ABSENCE_CLAUSE);
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.REFUS_SANS_FAUTE_ABSENCE_CLAUSE)
                .anyMatch(c -> c.type() == TypeConsequence.MODIFICATION_CONTRAT_REQUISE);
    }

    @Test
    void absenceClause_refusSalarie_returnsConsequenceModificationContrat() {
        var input = new MutationClauseMobiliteInput(
                false, false, false, null, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.consequencesRefus()).contains("avenant");
    }

    // ── Motif non professionnel → CLAUSE_INVALIDE (détournement de pouvoir) ─

    @Test
    void clausePresente_motifNonProfessionnel_returnsClauseInvalide() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 4, false, false, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMutation()).isEqualTo(AnalyseMutation.CLAUSE_INVALIDE);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("23/02/2005"));
    }

    @Test
    void clausePresente_sansInteretLegitime_returnsClauseInvalide() {
        var input = new MutationClauseMobiliteInput(
                true, true, false, 4, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.analyseMutation()).isEqualTo(AnalyseMutation.CLAUSE_INVALIDE);
    }

    // ── Situation familiale contraignante → atténuation du score ─────────────

    @Test
    void clauseValide_situationFamilialeContraignante_scoreReduit() {
        var inputSans = new MutationClauseMobiliteInput(
                true, true, true, 4, false, true, ReponseSalarie.REFUS);
        var inputAvec = new MutationClauseMobiliteInput(
                true, true, true, 4, true, true, ReponseSalarie.REFUS);
        var sansFam = MutationClauseMobiliteCalculator.compute(inputSans, "FRANCE");
        var avecFam = MutationClauseMobiliteCalculator.compute(inputAvec, "FRANCE");
        assertThat(avecFam.scoreMutation()).isLessThan(sansFam.scoreMutation());
        assertThat(avecFam.basesJuridiques())
                .anyMatch(b -> b.contains("14/10/2008"));
    }

    // ── Réponse salarié — branches ───────────────────────────────────────────

    @Test
    void acceptation_returnsAccordFormelRequis() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 4, false, true, ReponseSalarie.ACCEPTATION);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.ACCORD_FORMEL_REQUIS);
        assertThat(r.consequencesRefus()).contains("Acceptation");
    }

    @Test
    void enAttente_returnsAttenteReponseSalarie() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 4, false, true, ReponseSalarie.EN_ATTENTE);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.ATTENTE_REPONSE_SALARIE);
    }

    // ── Erreurs / pays ───────────────────────────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 4, false, true, ReponseSalarie.REFUS);
        assertThatThrownBy(() ->
                MutationClauseMobiliteCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                MutationClauseMobiliteCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    @Test
    void reponseSalarieNull_throwsIllegalArgument() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 4, false, true, null);
        assertThatThrownBy(() ->
                MutationClauseMobiliteCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Réponse");
    }

    @Test
    void delaiNegatif_throwsIllegalArgument() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, -1, false, true, ReponseSalarie.REFUS);
        assertThatThrownBy(() ->
                MutationClauseMobiliteCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négatif");
    }

    // ── Score borné ─────────────────────────────────────────────────────────

    @Test
    void score_borneEntreZeroEtCent() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 4, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.scoreMutation()).isBetween(0, 100);
    }

    // ── Bases juridiques de référence ───────────────────────────────────────

    @Test
    void basesJuridiques_contiennent_JurisprudenceMobilite() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 4, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("clause de mobilité"));
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1221-1"));
    }

    // ── Points d'analyse F-IA-03 ─────────────────────────────────────────────

    @Test
    void pointsAnalyse_clauseValide_couvrentLesQuatreCriteres() {
        var input = new MutationClauseMobiliteInput(
                true, true, true, 4, false, true, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.pointsAnalyse())
                .extracting(p -> p.code().name())
                .containsExactlyInAnyOrder(
                        "DT71_CLAUSE_PRESENTE",
                        "DT71_ZONE_PRECISE",
                        "DT71_DELAI_PREVENANCE",
                        "DT71_MOTIF_PROFESSIONNEL");
    }

    @Test
    void pointsAnalyse_absenceClause_uniquementCriterePresence() {
        var input = new MutationClauseMobiliteInput(
                false, false, false, null, false, false, ReponseSalarie.REFUS);
        var r = MutationClauseMobiliteCalculator.compute(input, "FRANCE");
        assertThat(r.pointsAnalyse())
                .hasSize(1)
                .extracting(p -> p.code().name())
                .containsExactly("DT71_CLAUSE_PRESENTE");
    }
}
