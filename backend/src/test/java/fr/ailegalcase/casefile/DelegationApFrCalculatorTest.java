package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-09 : tests unitaires du calculateur délégation autorité parentale FR
 * (art. 376-1 Cciv + art. L. 228-1 CASF + Cass. 1ère civ., 22/11/2005).
 */
class DelegationApFrCalculatorTest {

    private static DelegationApFrRequest req(
            TypeDelegationApEnum type,
            TiersLienFamilialEnum tiers,
            Boolean accord,
            String motivation,
            Integer ageEnfant,
            Boolean danger,
            Boolean decisionsPrec,
            Boolean desinteret) {
        return new DelegationApFrRequest(type, tiers, accord, motivation, ageEnfant,
                danger, decisionsPrec, desinteret);
    }

    // ── AC1 : accord deux parents + voie volontaire → RECEVABLE_VOLONTAIRE ──

    @Test
    void ac1_accord_parents_voie_volontaire_renvoie_recevable_volontaire() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.VOLONTAIRE_CONJOINTE,
                TiersLienFamilialEnum.GRANDS_PARENTS,
                true, "Parents souhaitent confier la garde aux grands-parents",
                8, false, false, false);
        DelegationApFrResult res = DelegationApFrCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRecevabilite())
                .isEqualTo(VerdictRecevabiliteDelegationApEnum.RECEVABLE_VOLONTAIRE);
        assertThat(res.voieProcedurale()).isEqualTo(TypeDelegationApEnum.VOLONTAIRE_CONJOINTE);
        assertThat(res.etapes()).isNotEmpty();
        assertThat(res.baseLegale()).contains("376-1");
        assertThat(res.dureeEstimeeJours()).isPositive();
    }

    // ── AC2 : danger caractérisé → alerte retrait AP ───────────────────────

    @Test
    void ac2_danger_caracterise_genere_alerte_retrait_ap() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.JUDICIAIRE_TIERS,
                TiersLienFamilialEnum.FAMILLE_ELARGIE,
                false, "Tiers exerçant l'AP de fait depuis 2 ans",
                6, true, true, false);
        DelegationApFrResult res = DelegationApFrCalculator.compute(r, "FRANCE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("retrait") || a.contains("378"));
    }

    // ── AC3 : enfant >= 18 → IRRECEVABLE_ENFANT_MAJEUR ─────────────────────
    // Note : ageEnfant > 18 est rejeté en 400 par le service ; le calculateur
    // est appelé pour exactement 18 (limite cas).

    @Test
    void ac3_enfant_age_18_renvoie_irrecevable_enfant_majeur() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.VOLONTAIRE_CONJOINTE,
                TiersLienFamilialEnum.GRANDS_PARENTS,
                true, "Demande tardive", 18, false, false, false);
        DelegationApFrResult res = DelegationApFrCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRecevabilite())
                .isEqualTo(VerdictRecevabiliteDelegationApEnum.IRRECEVABLE_ENFANT_MAJEUR);
        assertThat(res.dureeEstimeeJours()).isZero();
        assertThat(res.messages()).anyMatch(m -> m.contains("371-1") || m.contains("majorité"));
    }

    // ── AC4 : pays BELGIQUE → IllegalArgumentException ─────────────────────

    @Test
    void ac4_pays_belgique_leve_exception() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.VOLONTAIRE_CONJOINTE,
                TiersLienFamilialEnum.GRANDS_PARENTS,
                true, "test", 8, false, false, false);
        assertThatThrownBy(() -> DelegationApFrCalculator.compute(r, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Volontaire sans accord → bascule sur JUDICIAIRE_TIERS ──────────────

    @Test
    void volontaire_sans_accord_bascule_judiciaire_tiers() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.VOLONTAIRE_CONJOINTE,
                TiersLienFamilialEnum.GRANDS_PARENTS,
                false, "Parents en désaccord", 10, false, false, false);
        DelegationApFrResult res = DelegationApFrCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRecevabilite())
                .isEqualTo(VerdictRecevabiliteDelegationApEnum.RECEVABLE_JUDICIAIRE_TIERS);
        assertThat(res.voieProcedurale()).isEqualTo(TypeDelegationApEnum.JUDICIAIRE_TIERS);
        assertThat(res.alertes()).anyMatch(a -> a.contains("VOLONTAIRE_CONJOINTE") || a.contains("accord"));
    }

    // ── Désintérêt > 1 an documenté → RECEVABLE_JUDICIAIRE_DESINTERET ──────

    @Test
    void desinteret_avere_renvoie_recevable_judiciaire_desinteret() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.JUDICIAIRE_DESINTERET,
                TiersLienFamilialEnum.GRANDS_PARENTS,
                false, "Père absent depuis 3 ans", 9, false, false, true);
        DelegationApFrResult res = DelegationApFrCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRecevabilite())
                .isEqualTo(VerdictRecevabiliteDelegationApEnum.RECEVABLE_JUDICIAIRE_DESINTERET);
        assertThat(res.etapes()).anyMatch(e -> e.contains("désintérêt") || e.contains("376-1 al. 2"));
    }

    // ── Désintérêt demandé sans preuve → IRRECEVABLE_VOIE_INADEQUATE ───────

    @Test
    void desinteret_non_documente_renvoie_irrecevable_voie_inadequate() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.JUDICIAIRE_DESINTERET,
                TiersLienFamilialEnum.GRANDS_PARENTS,
                false, "Soupçon de désintérêt", 9, false, false, false);
        DelegationApFrResult res = DelegationApFrCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRecevabilite())
                .isEqualTo(VerdictRecevabiliteDelegationApEnum.IRRECEVABLE_VOIE_INADEQUATE);
        assertThat(res.alertes()).anyMatch(a -> a.contains("désintérêt") || a.contains("documenté"));
    }

    // ── JUDICIAIRE_TIERS direct → RECEVABLE_JUDICIAIRE_TIERS ───────────────

    @Test
    void judiciaire_tiers_direct_renvoie_recevable_tiers() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.JUDICIAIRE_TIERS,
                TiersLienFamilialEnum.ONCLE_TANTE,
                false, "Oncle exerce l'AP de fait", 12, false, false, false);
        DelegationApFrResult res = DelegationApFrCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRecevabilite())
                .isEqualTo(VerdictRecevabiliteDelegationApEnum.RECEVABLE_JUDICIAIRE_TIERS);
        assertThat(res.etapes()).anyMatch(e -> e.contains("376-1 al. 2") || e.contains("requête"));
    }

    // ── Association tierce → alerte habilitation L. 228-1 CASF ─────────────

    @Test
    void association_habilitee_genere_alerte_verification() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.VOLONTAIRE_CONJOINTE,
                TiersLienFamilialEnum.ASSOCIATION_HABILITEE,
                true, "Association tutorale habilitée", 7, false, false, false);
        DelegationApFrResult res = DelegationApFrCalculator.compute(r, "FRANCE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("habilitation") || a.contains("L. 228-1"));
    }

    // ── Enfant en bas âge (0 an) → alerte intérêt enfant ───────────────────

    @Test
    void enfant_en_bas_age_genere_alerte_jurisprudence() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.VOLONTAIRE_CONJOINTE,
                TiersLienFamilialEnum.GRANDS_PARENTS,
                true, "Parents en grande détresse", 0, false, false, false);
        DelegationApFrResult res = DelegationApFrCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRecevabilite())
                .isEqualTo(VerdictRecevabiliteDelegationApEnum.RECEVABLE_VOLONTAIRE);
        assertThat(res.alertes()).anyMatch(a -> a.contains("bas âge") || a.contains("18 mois"));
    }

    // ── Décisions judiciaires antérieures → message historique ─────────────

    @Test
    void decisions_judiciaires_precedentes_genere_message_historique() {
        DelegationApFrRequest r = req(TypeDelegationApEnum.JUDICIAIRE_TIERS,
                TiersLienFamilialEnum.GRANDS_PARENTS,
                false, "Décision antérieure", 10, false, true, false);
        DelegationApFrResult res = DelegationApFrCalculator.compute(r, "FRANCE");
        assertThat(res.messages()).anyMatch(m -> m.contains("historique") || m.contains("JE"));
    }
}
