package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-222-03 : tests unitaires de l'analyseur des conditions de l'habilitation
 * familiale (art. 494-1 et s. Cciv). Couvre les 3 verdicts + la modalité +
 * l'orientation F-FA-25.
 */
class HabilitationFamilialeCalculatorTest {

    private static HabilitationFamilialeRequest req(Boolean alteration,
                                                    LienFamilialHabilitationEnum lien,
                                                    Boolean consensus,
                                                    Boolean actesPatrimoniaux,
                                                    Boolean actesPersonnels,
                                                    EtendueHabilitationEnum etendue) {
        return new HabilitationFamilialeRequest(alteration, lien, consensus,
                actesPatrimoniaux, actesPersonnels, etendue);
    }

    // ── Verdict 1 : conditions réunies + étendue générale → ELIGIBLE_HABILITATION_GENERALE ──
    @Test
    void conditions_reunies_generale_renvoie_eligible_generale() {
        HabilitationFamilialeResult r = HabilitationFamilialeCalculator.compute(
                req(true, LienFamilialHabilitationEnum.DESCENDANT, true, true, true,
                        EtendueHabilitationEnum.GENERALE), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictHabilitationFamilialeEnum.ELIGIBLE_HABILITATION_GENERALE);
        assertThat(r.conditionsManquantes()).isEmpty();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("494-6"));
        assertThat(r.messages()).anyMatch(m -> m.contains("juge des contentieux de la protection"));
    }

    // ── Verdict 2 : conditions réunies + étendue ponctuelle → ELIGIBLE_HABILITATION_SPECIALE ──
    @Test
    void conditions_reunies_ponctuelle_renvoie_eligible_speciale() {
        HabilitationFamilialeResult r = HabilitationFamilialeCalculator.compute(
                req(true, LienFamilialHabilitationEnum.ASCENDANT, true, true, false,
                        EtendueHabilitationEnum.PONCTUELLE), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictHabilitationFamilialeEnum.ELIGIBLE_HABILITATION_SPECIALE);
        assertThat(r.actesCouverts()).anyMatch(a -> a.contains("patrimoniaux"));
    }

    // ── Modalité ASSISTANCE : besoin léger (un seul type d'actes, étendue ponctuelle) ──
    @Test
    void besoin_leger_renvoie_modalite_assistance() {
        HabilitationFamilialeResult r = HabilitationFamilialeCalculator.compute(
                req(true, LienFamilialHabilitationEnum.FRERE_SOEUR, true, true, false,
                        EtendueHabilitationEnum.PONCTUELLE), "FRANCE");
        assertThat(r.modalite()).isEqualTo(ModaliteHabilitationEnum.ASSISTANCE);
    }

    // ── Modalité REPRESENTATION : besoin lourd (actes patrimoniaux + personnels) ──
    @Test
    void besoin_lourd_renvoie_modalite_representation() {
        HabilitationFamilialeResult r = HabilitationFamilialeCalculator.compute(
                req(true, LienFamilialHabilitationEnum.CONJOINT_PARTENAIRE, true, true, true,
                        EtendueHabilitationEnum.PONCTUELLE), "FRANCE");
        assertThat(r.modalite()).isEqualTo(ModaliteHabilitationEnum.REPRESENTATION);
    }

    // ── Modalité REPRESENTATION : habilitation générale → représentation ──
    @Test
    void etendue_generale_force_representation() {
        HabilitationFamilialeResult r = HabilitationFamilialeCalculator.compute(
                req(true, LienFamilialHabilitationEnum.DESCENDANT, true, true, false,
                        EtendueHabilitationEnum.GENERALE), "FRANCE");
        assertThat(r.modalite()).isEqualTo(ModaliteHabilitationEnum.REPRESENTATION);
    }

    // ── Verdict 3 : altération non constatée → ORIENTER_VERS_MESURE_JUDICIAIRE ──
    @Test
    void alteration_non_constatee_oriente_vers_mesure_judiciaire() {
        HabilitationFamilialeResult r = HabilitationFamilialeCalculator.compute(
                req(false, LienFamilialHabilitationEnum.DESCENDANT, true, true, true,
                        EtendueHabilitationEnum.GENERALE), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictHabilitationFamilialeEnum.ORIENTER_VERS_MESURE_JUDICIAIRE);
        assertThat(r.modalite()).isNull();
        assertThat(r.conditionsManquantes()).contains(HabilitationFamilialeCalculator.COND_ALTERATION);
        assertThat(r.messages()).anyMatch(m -> m.contains("F-FA-25"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("F-FA-25"));
    }

    // ── Verdict 3 : lien familial AUTRE → ORIENTER_VERS_MESURE_JUDICIAIRE ──
    @Test
    void lien_autre_oriente_vers_mesure_judiciaire() {
        HabilitationFamilialeResult r = HabilitationFamilialeCalculator.compute(
                req(true, LienFamilialHabilitationEnum.AUTRE, true, true, true,
                        EtendueHabilitationEnum.GENERALE), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictHabilitationFamilialeEnum.ORIENTER_VERS_MESURE_JUDICIAIRE);
        assertThat(r.conditionsManquantes()).contains(HabilitationFamilialeCalculator.COND_LIEN);
    }

    // ── Verdict 3 : absence de consensus → ORIENTER_VERS_MESURE_JUDICIAIRE ──
    @Test
    void absence_consensus_oriente_vers_mesure_judiciaire() {
        HabilitationFamilialeResult r = HabilitationFamilialeCalculator.compute(
                req(true, LienFamilialHabilitationEnum.DESCENDANT, false, true, true,
                        EtendueHabilitationEnum.GENERALE), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictHabilitationFamilialeEnum.ORIENTER_VERS_MESURE_JUDICIAIRE);
        assertThat(r.conditionsManquantes()).contains(HabilitationFamilialeCalculator.COND_CONSENSUS);
    }

    // ── Lien null traité comme inéligible ──
    @Test
    void lien_null_traite_comme_ineligible() {
        HabilitationFamilialeResult r = HabilitationFamilialeCalculator.compute(
                req(true, null, true, true, true, EtendueHabilitationEnum.GENERALE), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictHabilitationFamilialeEnum.ORIENTER_VERS_MESURE_JUDICIAIRE);
        assertThat(r.conditionsManquantes()).contains(HabilitationFamilialeCalculator.COND_LIEN);
    }

    // ── Toutes conditions manquantes → 3 conditions manquantes ──
    @Test
    void aucune_condition_renvoie_3_manquantes() {
        HabilitationFamilialeResult r = HabilitationFamilialeCalculator.compute(
                req(false, LienFamilialHabilitationEnum.AUTRE, false, false, false,
                        EtendueHabilitationEnum.PONCTUELLE), "FRANCE");
        assertThat(r.verdict()).isEqualTo(VerdictHabilitationFamilialeEnum.ORIENTER_VERS_MESURE_JUDICIAIRE);
        assertThat(r.conditionsManquantes()).hasSize(3);
    }

    // ── Gate pays : hors FRANCE → IllegalArgumentException ──
    @Test
    void hors_france_leve_exception() {
        assertThatThrownBy(() -> HabilitationFamilialeCalculator.compute(
                req(true, LienFamilialHabilitationEnum.DESCENDANT, true, true, true,
                        EtendueHabilitationEnum.GENERALE), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
