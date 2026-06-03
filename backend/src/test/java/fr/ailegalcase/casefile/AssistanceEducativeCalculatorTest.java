package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-222-04 : tests unitaires de l'analyseur assistance éducative — mineur en
 * danger (art. 375 et s. Cciv). Couvre les 4 verdicts + frontières
 * urgence / maintien + gate pays.
 */
class AssistanceEducativeCalculatorTest {

    private static AssistanceEducativeRequest req(Boolean danger, Boolean urgence, Boolean adhesion,
                                                  Boolean maintien, Boolean amiable) {
        return new AssistanceEducativeRequest(danger, urgence, adhesion, maintien, amiable);
    }

    // ── Verdict 1 : pas de danger → PAS_DE_MESURE ──
    @Test
    void pas_de_danger_renvoie_pas_de_mesure() {
        AssistanceEducativeResult r = AssistanceEducativeCalculator.compute(
                req(false, false, true, true, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAssistanceEducativeEnum.PAS_DE_MESURE);
        assertThat(r.juridiction()).isEqualTo(AssistanceEducativeCalculator.JURIDICTION_AUCUNE);
        assertThat(r.messages()).anyMatch(m -> m.contains("juge des enfants"));
    }

    // ── Verdict 2 : danger + urgence → OPP_PLACEMENT (juge des enfants) ──
    @Test
    void danger_urgence_renvoie_opp_placement() {
        AssistanceEducativeResult r = AssistanceEducativeCalculator.compute(
                req(true, true, true, true, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAssistanceEducativeEnum.OPP_PLACEMENT);
        assertThat(r.juridiction()).isEqualTo(AssistanceEducativeCalculator.JURIDICTION_JUGE_ENFANTS);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("375-5"));
    }

    // ── Verdict 2 bis : danger + maintien impossible (sans urgence) → OPP_PLACEMENT ──
    @Test
    void danger_maintien_impossible_renvoie_opp_placement() {
        AssistanceEducativeResult r = AssistanceEducativeCalculator.compute(
                req(true, false, true, false, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAssistanceEducativeEnum.OPP_PLACEMENT);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("375-3"));
    }

    // ── Verdict 3 : danger, maintien, adhésion + amiable → AED (administrative ASE) ──
    @Test
    void danger_adhesion_amiable_renvoie_aed() {
        AssistanceEducativeResult r = AssistanceEducativeCalculator.compute(
                req(true, false, true, true, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAssistanceEducativeEnum.AED);
        assertThat(r.juridiction()).isEqualTo(AssistanceEducativeCalculator.JURIDICTION_ASE);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 222-3 CASF"));
    }

    // ── Verdict 4 : danger, maintien, pas d'adhésion → AEMO (judiciaire) ──
    @Test
    void danger_sans_adhesion_renvoie_aemo() {
        AssistanceEducativeResult r = AssistanceEducativeCalculator.compute(
                req(true, false, false, true, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAssistanceEducativeEnum.AEMO);
        assertThat(r.juridiction()).isEqualTo(AssistanceEducativeCalculator.JURIDICTION_JUGE_ENFANTS);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("375-2"));
    }

    // ── Verdict 4 bis : danger, maintien, adhésion mais amiable non envisageable → AEMO ──
    @Test
    void danger_adhesion_mais_amiable_impossible_renvoie_aemo() {
        AssistanceEducativeResult r = AssistanceEducativeCalculator.compute(
                req(true, false, true, true, false), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAssistanceEducativeEnum.AEMO);
    }

    // ── Frontière : urgence prime sur adhésion + amiable (reste OPP) ──
    @Test
    void urgence_prime_sur_aed() {
        AssistanceEducativeResult r = AssistanceEducativeCalculator.compute(
                req(true, true, true, true, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAssistanceEducativeEnum.OPP_PLACEMENT);
    }

    // ── Nulls traités comme false (danger null → pas de mesure) ──
    @Test
    void nulls_traites_comme_false() {
        AssistanceEducativeResult r = AssistanceEducativeCalculator.compute(
                req(null, null, null, null, null), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAssistanceEducativeEnum.PAS_DE_MESURE);
    }

    // ── Gate pays : hors FRANCE → IllegalArgumentException ──
    @Test
    void hors_france_leve_exception() {
        assertThatThrownBy(() -> AssistanceEducativeCalculator.compute(
                req(true, false, true, true, true), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
