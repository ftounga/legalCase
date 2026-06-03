package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-222-01 : tests unitaires du calculateur ASF (allocation de soutien familial,
 * art. L. 523-1 CSS). Couvre les 4 verdicts + l'ASF différentielle.
 */
class AsfCafCalculatorTest {

    private static AsfCafRequest req(Boolean parentIsole, Boolean pensionFixee, Integer montant,
                                     PensionPayeeEnum payee, Integer nbEnfants, Boolean decesOuInconnu) {
        return new AsfCafRequest(parentIsole, pensionFixee, montant, payee, nbEnfants, decesOuInconnu);
    }

    // ── Verdict 1 : autre parent décédé / inconnu → ASF taux plein, sans recouvrement ──
    @Test
    void deces_ou_inconnu_renvoie_asf_plein_sans_recouvrement() {
        AsfCafResult r = AsfCafCalculator.compute(
                req(false, false, null, null, 2, true), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAsfEnum.DROIT_ASF_PLEIN);
        assertThat(r.montantMensuelEstime()).isEqualTo(AsfCafCalculator.ASF_TAUX_PLEIN_PAR_ENFANT_EUR * 2);
        assertThat(r.recouvrementApplicable()).isFalse();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 523-1"));
    }

    // ── Verdict 1bis : parent isolé + pension non fixée → ASF taux plein ──
    @Test
    void parent_isole_pension_non_fixee_renvoie_asf_plein() {
        AsfCafResult r = AsfCafCalculator.compute(
                req(true, false, null, null, 1, false), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAsfEnum.DROIT_ASF_PLEIN);
        assertThat(r.montantMensuelEstime()).isEqualTo(AsfCafCalculator.ASF_TAUX_PLEIN_PAR_ENFANT_EUR);
        assertThat(r.recouvrementApplicable()).isFalse();
    }

    // ── Verdict 2 : ASF différentielle (pension partielle < montant ASF) ──
    @Test
    void pension_partielle_inferieure_renvoie_differentielle() {
        AsfCafResult r = AsfCafCalculator.compute(
                req(true, true, 100, PensionPayeeEnum.PARTIELLE, 1, false), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAsfEnum.DROIT_ASF_DIFFERENTIELLE);
        // complément = 187 - 100 = 87
        assertThat(r.montantMensuelEstime())
                .isEqualTo(AsfCafCalculator.ASF_TAUX_PLEIN_PAR_ENFANT_EUR - 100);
        assertThat(r.recouvrementApplicable()).isFalse();
    }

    // ── Verdict 2bis : pension partielle ≥ montant ASF → pas de complément ──
    @Test
    void pension_partielle_superieure_renvoie_pas_de_droit() {
        AsfCafResult r = AsfCafCalculator.compute(
                req(true, true, 500, PensionPayeeEnum.PARTIELLE, 1, false), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAsfEnum.PAS_DE_DROIT);
        assertThat(r.montantMensuelEstime()).isZero();
    }

    // ── Verdict 3 : pension fixée non payée → ASF récupérable + renvoi ARIPA ──
    @Test
    void pension_fixee_non_payee_renvoie_avec_recouvrement() {
        AsfCafResult r = AsfCafCalculator.compute(
                req(true, true, 300, PensionPayeeEnum.NON_PAYEE, 1, false), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAsfEnum.DROIT_AVEC_RECOUVREMENT);
        assertThat(r.montantMensuelEstime()).isEqualTo(AsfCafCalculator.ASF_TAUX_PLEIN_PAR_ENFANT_EUR);
        assertThat(r.recouvrementApplicable()).isTrue();
        assertThat(r.messages()).anyMatch(m -> m.contains("F-FA-ARIPA-RECOUVREMENT"));
    }

    // ── Verdict 4 : parent non isolé (hors décès/inconnu) → pas de droit ──
    @Test
    void parent_non_isole_renvoie_pas_de_droit() {
        AsfCafResult r = AsfCafCalculator.compute(
                req(false, true, 300, PensionPayeeEnum.NON_PAYEE, 1, false), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAsfEnum.PAS_DE_DROIT);
        assertThat(r.montantMensuelEstime()).isZero();
        assertThat(r.recouvrementApplicable()).isFalse();
    }

    // ── Verdict 4bis : pension fixée et payée intégralement → pas de droit ──
    @Test
    void pension_payee_integralement_renvoie_pas_de_droit() {
        AsfCafResult r = AsfCafCalculator.compute(
                req(true, true, 300, PensionPayeeEnum.PAYEE, 1, false), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictAsfEnum.PAS_DE_DROIT);
    }

    // ── Gate pays : hors FRANCE → IllegalArgumentException ──
    @Test
    void hors_france_leve_exception() {
        assertThatThrownBy(() -> AsfCafCalculator.compute(
                req(true, false, null, null, 1, false), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
