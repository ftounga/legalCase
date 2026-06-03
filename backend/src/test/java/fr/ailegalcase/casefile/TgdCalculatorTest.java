package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-222-02 : tests unitaires de l'analyseur d'éligibilité TGD (téléphone grave
 * danger, art. 41-3-1 CPP). Couvre les 3 verdicts + les critères manquants.
 */
class TgdCalculatorTest {

    private static TgdRequest req(Boolean dangerGrave, Boolean violences,
                                  Boolean interdiction, Boolean nonCohabitation,
                                  Boolean consentement) {
        return new TgdRequest(dangerGrave, violences, interdiction, nonCohabitation, consentement);
    }

    // ── Verdict 1 : tous les critères réunis → ELIGIBLE_TGD ──
    @Test
    void tous_criteres_reunis_renvoie_eligible() {
        TgdResult r = TgdCalculator.compute(req(true, true, true, true, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictTgdEnum.ELIGIBLE_TGD);
        assertThat(r.criteresManquants()).isEmpty();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("41-3-1"));
        assertThat(r.messages()).anyMatch(m -> m.contains("procureur"));
    }

    // ── Verdict 2 : seule l'interdiction de contact manque → ELIGIBLE_SOUS_RESERVE + renvoi F-FA-14 ──
    @Test
    void seule_interdiction_manque_renvoie_sous_reserve() {
        TgdResult r = TgdCalculator.compute(req(true, true, false, true, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictTgdEnum.ELIGIBLE_SOUS_RESERVE);
        assertThat(r.criteresManquants()).containsExactly(TgdCalculator.CRIT_INTERDICTION);
        assertThat(r.messages()).anyMatch(m -> m.contains("F-FA-14"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("515-11"));
    }

    // ── Verdict 3 : un critère dur (danger grave) manque → NON_ELIGIBLE ──
    @Test
    void danger_grave_manquant_renvoie_non_eligible() {
        TgdResult r = TgdCalculator.compute(req(false, true, true, true, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictTgdEnum.NON_ELIGIBLE);
        assertThat(r.criteresManquants()).containsExactly(TgdCalculator.CRIT_DANGER_GRAVE);
    }

    // ── Verdict 3bis : interdiction manquante + un autre critère dur → NON_ELIGIBLE (pas sous réserve) ──
    @Test
    void interdiction_plus_autre_critere_manquant_renvoie_non_eligible() {
        TgdResult r = TgdCalculator.compute(req(true, false, false, true, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictTgdEnum.NON_ELIGIBLE);
        assertThat(r.criteresManquants()).containsExactlyInAnyOrder(
                TgdCalculator.CRIT_VIOLENCES, TgdCalculator.CRIT_INTERDICTION);
    }

    // ── Verdict 3ter : tout manquant → NON_ELIGIBLE avec les 5 critères ──
    @Test
    void aucun_critere_renvoie_non_eligible_avec_5_manquants() {
        TgdResult r = TgdCalculator.compute(req(false, false, false, false, false), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictTgdEnum.NON_ELIGIBLE);
        assertThat(r.criteresManquants()).hasSize(5);
    }

    // ── Null traité comme false ──
    @Test
    void champs_null_traites_comme_false() {
        TgdResult r = TgdCalculator.compute(req(null, null, null, null, null), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictTgdEnum.NON_ELIGIBLE);
        assertThat(r.criteresManquants()).hasSize(5);
    }

    // ── La décision relève toujours du procureur (mention présente sur les 3 verdicts) ──
    @Test
    void mention_procureur_presente_sur_tous_verdicts() {
        assertThat(TgdCalculator.compute(req(true, true, true, true, true), "FRANCE").messages())
                .anyMatch(m -> m.contains("procureur"));
        assertThat(TgdCalculator.compute(req(true, true, false, true, true), "FRANCE").messages())
                .anyMatch(m -> m.contains("procureur"));
        assertThat(TgdCalculator.compute(req(false, false, false, false, false), "FRANCE").messages())
                .anyMatch(m -> m.contains("procureur"));
    }

    // ── Gate pays : hors FRANCE → IllegalArgumentException ──
    @Test
    void hors_france_leve_exception() {
        assertThatThrownBy(() -> TgdCalculator.compute(
                req(true, true, true, true, true), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
