package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-15 : tests unitaires du calculateur Adoption intra-familiale FR
 * (art. 343-1 al. 2 + 345-1 + 360-362 Cciv + Loi n°2022-219 du 21/2/2022).
 */
class AdoptionIntraFrCalculatorTest {

    private static AdoptionIntraFrRequest req(
            FormeAdoptionEnum forme,
            Integer ageAdoptant, Integer ageAdopte,
            Boolean marieOuPacse,
            Boolean consentementEnfant,
            Boolean consentementAutreParent,
            Boolean decheance,
            Boolean autreParentDecede) {
        return new AdoptionIntraFrRequest(forme, ageAdoptant, ageAdopte, marieOuPacse,
                consentementEnfant, consentementAutreParent, decheance, autreParentDecede);
    }

    // ── AC1 : marié + autre parent décédé → PLENIERE et SIMPLE possibles ──

    @Test
    void ac1_marie_autre_parent_decede_les_deux_formes_possibles() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.PLENIERE, 40, 8,
                true, null, null, false, true);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.formesPossibles())
                .contains(FormeAdoptionEnum.PLENIERE, FormeAdoptionEnum.SIMPLE);
        assertThat(res.conditionsRemplies()).isTrue();
        assertThat(res.alerteIrreversibilite()).isTrue();
        assertThat(res.alertes()).anyMatch(a -> a.contains("Irréversibilité"));
        assertThat(res.baseLegale()).contains("345-1");
    }

    // ── AC2 : autre parent vivant sans consentement → SIMPLE seulement ──

    @Test
    void ac2_autre_parent_vivant_sans_consentement_simple_seule() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.SIMPLE, 45, 10,
                true, null, false, false, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.formesPossibles()).containsExactly(FormeAdoptionEnum.SIMPLE);
        assertThat(res.conditionsRemplies()).isTrue();
        assertThat(res.alerteIrreversibilite()).isFalse();
        assertThat(res.alertes()).anyMatch(a -> a.contains("plénière fermée"));
    }

    // ── AC3 : alerte irréversibilité plénière systématique ──

    @Test
    void ac3_alerte_irreversibilite_plenière_systematique() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.PLENIERE, 40, 5,
                true, null, true, false, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.alerteIrreversibilite()).isTrue();
        assertThat(res.alertes()).anyMatch(a -> a.contains("356") || a.contains("Irréversibilité"));
    }

    // ── AC4 : enfant ≥ 13 ans → consentement requis ──

    @Test
    void ac4_enfant_quatorze_ans_consentement_requis() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.SIMPLE, 45, 14,
                true, null, false, false, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.consentementEnfantRequis()).isTrue();
        assertThat(res.messages()).anyMatch(m -> m.contains("360 al. 2") || m.contains("≥ 13"));
    }

    @Test
    void enfant_quatorze_ans_consentement_refuse_genere_alerte() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.SIMPLE, 45, 14,
                true, false, false, false, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("refusé") || a.contains("Consentement"));
    }

    @Test
    void enfant_dix_ans_pas_de_consentement_requis() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.SIMPLE, 40, 10,
                true, null, true, false, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.consentementEnfantRequis()).isFalse();
    }

    // ── AC5 : pays BELGIQUE → IllegalArgumentException ──

    @Test
    void ac5_pays_belgique_leve_exception() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.SIMPLE, 40, 8,
                true, null, true, false, false);
        assertThatThrownBy(() -> AdoptionIntraFrCalculator.compute(r, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Pré-requis bloquant : ni marié ni pacsé ──

    @Test
    void non_marie_non_pacse_aucune_forme_ouverte() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.SIMPLE, 40, 6,
                false, null, true, false, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.formesPossibles()).isEmpty();
        assertThat(res.conditionsRemplies()).isFalse();
        assertThat(res.alertes()).anyMatch(a -> a.contains("mariage") || a.contains("PACS"));
    }

    // ── Plénière par déchéance ──

    @Test
    void plenière_par_decheance_autre_parent() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.PLENIERE, 40, 6,
                true, null, false, true, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.formesPossibles()).contains(FormeAdoptionEnum.PLENIERE);
        assertThat(res.conditionsRemplies()).isTrue();
    }

    // ── Forme demandée PLENIERE non recevable → alerte ──

    @Test
    void forme_plenière_non_recevable_genere_alerte() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.PLENIERE, 40, 8,
                true, null, false, false, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.conditionsRemplies()).isFalse();
        assertThat(res.alertes()).anyMatch(a -> a.contains("PLÉNIÈRE") || a.contains("345-1"));
    }

    // ── Différence d'âge < 10 ans → alerte mais pas bloquant ──

    @Test
    void difference_age_inferieure_dix_ans_genere_alerte() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.SIMPLE, 30, 25,
                true, null, true, false, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("Différence d'âge") || a.contains("344"));
        // SIMPLE reste recevable
        assertThat(res.conditionsRemplies()).isTrue();
    }

    @Test
    void difference_age_correcte_pas_alerte_age() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.SIMPLE, 40, 8,
                true, null, true, false, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.alertes()).noneMatch(a -> a.contains("Différence d'âge"));
    }

    // ── Message réforme 2022 systématique ──

    @Test
    void reforme_2022_mentionnee_dans_messages() {
        AdoptionIntraFrRequest r = req(FormeAdoptionEnum.SIMPLE, 40, 8,
                true, null, true, false, false);
        AdoptionIntraFrResult res = AdoptionIntraFrCalculator.compute(r, "FRANCE");
        assertThat(res.messages()).anyMatch(m -> m.contains("2022-219") || m.contains("Réforme 2022"));
    }
}
