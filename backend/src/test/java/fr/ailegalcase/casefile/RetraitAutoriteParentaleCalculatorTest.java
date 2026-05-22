package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-11 : tests unitaires du calculateur retrait autorité parentale FR
 * (art. 378-381 Cciv + loi n°2022-140 du 7 février 2022 LMVSS +
 * art. 343-1 al. 2 Cciv + Cass. 1ère civ., 26/10/2011).
 */
class RetraitAutoriteParentaleCalculatorTest {

    private static RetraitAutoriteParentaleRequest req(
            TypeRetraitApEnum type,
            MotifRetraitApEnum motif,
            Boolean condamnationPenale,
            Boolean danger,
            Boolean violencesConjugales,
            Integer ageEnfant,
            Boolean decisionsPrec) {
        return new RetraitAutoriteParentaleRequest(type, motif, condamnationPenale,
                danger, violencesConjugales, ageEnfant, decisionsPrec);
    }

    // ── AC1 : condamnation pénale → retrait de plein droit ─────────────────

    @Test
    void ac1_condamnation_penale_renvoie_retrait_plein_droit() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.TOTAL,
                MotifRetraitApEnum.CONDAMNATION_PENALE,
                true, false, false, 8, false);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRetrait()).isEqualTo(VerdictRetraitApEnum.RETRAIT_PLEIN_DROIT);
        assertThat(res.voieProcedurale())
                .isEqualTo(VoieProceduraleRetraitApEnum.JURIDICTION_PENALE_ACCESSOIRE);
        assertThat(res.baseLegale()).contains("378");
        assertThat(res.etapes()).isNotEmpty();
    }

    // ── AC2 : violences loi 2022 → suspension accélérée ────────────────────

    @Test
    void ac2_violences_loi_2022_genere_suspension_acceleree() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.TOTAL,
                MotifRetraitApEnum.VIOLENCES_LMVSS_2022,
                false, false, true, 6, false);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRetrait())
                .isEqualTo(VerdictRetraitApEnum.SUSPENSION_ACCELEREE_LMVSS_2022);
        assertThat(res.voieProcedurale())
                .isEqualTo(VoieProceduraleRetraitApEnum.LMVSS_2022_SUSPENSION_AUTOMATIQUE);
        assertThat(res.alertes()).anyMatch(a -> a.contains("2022") || a.contains("LMVSS"));
    }

    // ── AC3 : retrait total → admissibilité adoption art. 343-1 al. 2 ──────

    @Test
    void ac3_retrait_total_renvoie_admissibilite_adoption_true() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.TOTAL,
                MotifRetraitApEnum.DANGER_CARACTERISE_VIOLENCES,
                false, true, false, 5, false);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.admissibiliteAdoption()).isTrue();
        assertThat(res.consequencesJuridiques())
                .anyMatch(c -> c.contains("adoptable") || c.contains("343-1"));
    }

    // ── AC4 : pays BELGIQUE → IllegalArgumentException ─────────────────────

    @Test
    void ac4_pays_belgique_leve_exception() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.TOTAL,
                MotifRetraitApEnum.CONDAMNATION_PENALE,
                true, false, false, 8, false);
        assertThatThrownBy(() -> RetraitAutoriteParentaleCalculator.compute(r, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Enfant majeur → IRRECEVABLE_ENFANT_MAJEUR ──────────────────────────

    @Test
    void enfant_age_18_renvoie_irrecevable_enfant_majeur() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.TOTAL,
                MotifRetraitApEnum.CONDAMNATION_PENALE,
                true, false, false, 18, false);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRetrait()).isEqualTo(VerdictRetraitApEnum.IRRECEVABLE_ENFANT_MAJEUR);
        assertThat(res.voieProcedurale()).isEqualTo(VoieProceduraleRetraitApEnum.SANS_OBJET);
        assertThat(res.dureeEstimeeJours()).isZero();
        assertThat(res.admissibiliteAdoption()).isFalse();
    }

    // ── Motif condamnation pénale sans pièces → IRRECEVABLE ────────────────

    @Test
    void condamnation_penale_invoquee_sans_piece_renvoie_irrecevable() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.TOTAL,
                MotifRetraitApEnum.CONDAMNATION_PENALE,
                false, false, false, 8, false);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRetrait())
                .isEqualTo(VerdictRetraitApEnum.IRRECEVABLE_MOTIF_NON_CARACTERISE);
        assertThat(res.alertes()).anyMatch(a -> a.contains("condamnation"));
    }

    // ── Désintérêt non documenté → IRRECEVABLE_MOTIF_NON_CARACTERISE ───────

    @Test
    void desinteret_non_documente_renvoie_irrecevable_motif_non_caracterise() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.TOTAL,
                MotifRetraitApEnum.DESINTERET_GRAVE,
                false, false, false, 9, false);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRetrait())
                .isEqualTo(VerdictRetraitApEnum.IRRECEVABLE_MOTIF_NON_CARACTERISE);
        assertThat(res.alertes()).anyMatch(a -> a.contains("désintérêt") || a.contains("2 ans"));
    }

    // ── Désintérêt avec décisions antérieures → RETRAIT_CIVIL_JAF ──────────

    @Test
    void desinteret_avec_decisions_anterieures_renvoie_retrait_civil() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.TOTAL,
                MotifRetraitApEnum.DESINTERET_GRAVE,
                false, false, false, 9, true);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRetrait()).isEqualTo(VerdictRetraitApEnum.RETRAIT_CIVIL_JAF);
        assertThat(res.voieProcedurale())
                .isEqualTo(VoieProceduraleRetraitApEnum.JAF_TRIBUNAL_JUDICIAIRE);
    }

    // ── Danger caractérisé → orientation procureur + assistance éducative ──

    @Test
    void danger_caracterise_oriente_procureur_assistance_educative() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.TOTAL,
                MotifRetraitApEnum.DANGER_CARACTERISE_VIOLENCES,
                false, true, false, 6, false);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.voieProcedurale())
                .isEqualTo(VoieProceduraleRetraitApEnum.PROCUREUR_REPUBLIQUE_ASSISTANCE_EDUCATIVE);
        assertThat(res.alertes()).anyMatch(a -> a.contains("Procureur") || a.contains("375"));
    }

    // ── Retrait PARTIEL_EXERCICE → admissibiliteAdoption = false ───────────

    @Test
    void retrait_partiel_exercice_admissibilite_adoption_false() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.PARTIEL_EXERCICE,
                MotifRetraitApEnum.DANGER_CARACTERISE_VIOLENCES,
                false, false, false, 7, true);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.admissibiliteAdoption()).isFalse();
        assertThat(res.consequencesJuridiques()).anyMatch(c -> c.contains("EXERCICE") || c.contains("exercice"));
    }

    // ── Retrait PARTIEL_ATTRIBUTS → conséquence dédiée ─────────────────────

    @Test
    void retrait_partiel_attributs_genere_consequence_dediee() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.PARTIEL_ATTRIBUTS,
                MotifRetraitApEnum.COMPORTEMENT_GRAVEMENT_COMPROMETTANT,
                false, false, false, 10, true);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.admissibiliteAdoption()).isFalse();
        assertThat(res.consequencesJuridiques()).anyMatch(c -> c.contains("ATTRIBUTS") || c.contains("attributs"));
    }

    // ── Violences conjugales détectées (sans motif explicite 2022) → LMVSS ─

    @Test
    void violences_conjugales_flag_meme_motif_civil_genere_suspension_lmvss() {
        RetraitAutoriteParentaleRequest r = req(TypeRetraitApEnum.TOTAL,
                MotifRetraitApEnum.DANGER_CARACTERISE_VIOLENCES,
                false, true, true, 4, false);
        RetraitAutoriteParentaleResult res =
                RetraitAutoriteParentaleCalculator.compute(r, "FRANCE");
        assertThat(res.verdictRetrait())
                .isEqualTo(VerdictRetraitApEnum.SUSPENSION_ACCELEREE_LMVSS_2022);
    }
}
