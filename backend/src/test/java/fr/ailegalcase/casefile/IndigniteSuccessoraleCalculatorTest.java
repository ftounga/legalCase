package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-19 : tests unitaires du calculateur Indignité successorale FR
 * (art. 726-729-1 Cciv + Loi 2022-1617 du 23/12/2022).
 */
class IndigniteSuccessoraleCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 23);

    private static IndigniteSuccessoraleRequest req(
            MotifIndigniteEnum motif,
            Boolean condamne,
            Boolean indigniteDemandee,
            Boolean pardon,
            LocalDate dateOuverture,
            Integer nbCoheritiers) {
        return new IndigniteSuccessoraleRequest(
                motif, condamne, indigniteDemandee, pardon, dateOuverture, nbCoheritiers);
    }

    // ── AC1 : meurtre sans pardon + condamnation → indignité plein droit ──

    @Test
    void ac1_meurtre_sans_pardon_condamne_indignite_plein_droit() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.MEURTRE,
                true,
                false,
                false,
                LocalDate.of(2025, 1, 15),
                2);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("PLEIN_DROIT");
        assertThat(res.verdictIndignite()).isEqualTo("INDIGNITE_PLEIN_DROIT");
        assertThat(res.pardonNeutralisant()).isFalse();
        assertThat(res.representationPossible()).isFalse();
        assertThat(res.alertes()).anyMatch(a -> a.contains("Représentation"));
        assertThat(res.effetDevolution()).contains("écarté");
        assertThat(res.baseLegale()).contains("726");
    }

    @Test
    void meurtre_tente_sans_pardon_condamne_indignite_plein_droit() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.MEURTRE_TENTE,
                true, false, false,
                LocalDate.of(2024, 6, 1), 1);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("PLEIN_DROIT");
        assertThat(res.verdictIndignite()).isEqualTo("INDIGNITE_PLEIN_DROIT");
        assertThat(res.representationPossible()).isFalse();
    }

    // ── AC2 : pardon testamentaire → neutralisation ──

    @Test
    void ac2_pardon_testamentaire_neutralise_indignite_plein_droit() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.MEURTRE,
                true,
                false,
                true,
                LocalDate.of(2024, 8, 10),
                2);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("PARDONNEE");
        assertThat(res.verdictIndignite()).isEqualTo("PARDON_NEUTRALISANT");
        assertThat(res.pardonNeutralisant()).isTrue();
        assertThat(res.representationPossible()).isTrue();
        assertThat(res.messages()).anyMatch(m -> m.contains("728"));
        assertThat(res.effetDevolution()).contains("conserve");
    }

    @Test
    void pardon_neutralise_aussi_indignite_judiciaire() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.TEMOIGNAGE_FAUX,
                false,
                true,
                true,
                LocalDate.of(2025, 11, 1),
                3);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("PARDONNEE");
        assertThat(res.verdictIndignite()).isEqualTo("PARDON_NEUTRALISANT");
        assertThat(res.pardonNeutralisant()).isTrue();
    }

    // ── AC3 : loi 2022 violences graves → indignité judiciaire ──

    @Test
    void ac3_loi_2022_violences_graves_indignite_judiciaire() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.VIOLENCES_GRAVES,
                false,
                true,
                false,
                LocalDate.of(2024, 12, 1),
                2);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("JUDICIAIRE");
        assertThat(res.verdictIndignite()).isEqualTo("INDIGNITE_JUDICIAIRE_POSSIBLE");
        assertThat(res.representationPossible()).isTrue();
        assertThat(res.messages()).anyMatch(m -> m.contains("2022-1617")
                || m.contains("violences intrafamiliales"));
        assertThat(res.baseLegale()).contains("2022-1617");
    }

    @Test
    void temoignage_faux_indignite_judiciaire_envisageable() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.TEMOIGNAGE_FAUX,
                false,
                true,
                false,
                LocalDate.of(2025, 2, 5),
                2);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("JUDICIAIRE");
        assertThat(res.verdictIndignite()).isEqualTo("INDIGNITE_JUDICIAIRE_POSSIBLE");
        assertThat(res.messages()).anyMatch(m -> m.contains("témoignage"));
        assertThat(res.representationPossible()).isTrue();
    }

    @Test
    void obstruction_testament_indignite_judiciaire() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.OBSTRUCTION_TESTAMENT,
                false, true, false,
                LocalDate.of(2025, 3, 10), 1);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("JUDICIAIRE");
        assertThat(res.messages()).anyMatch(m -> m.contains("obstruction"));
    }

    @Test
    void non_signalement_homicide_indignite_judiciaire() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.NON_SIGNALEMENT_HOMICIDE,
                false, true, false,
                LocalDate.of(2025, 4, 1), 2);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("JUDICIAIRE");
        assertThat(res.messages()).anyMatch(m -> m.contains("non-signalement"));
    }

    // ── AC4 : country=BELGIQUE → IAE ──

    @Test
    void ac4_belgique_throws_illegal_argument() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.MEURTRE,
                true, false, false,
                LocalDate.of(2025, 1, 1), 1);
        assertThatThrownBy(() ->
                IndigniteSuccessoraleCalculator.compute(r, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Cas complémentaires ──

    @Test
    void meurtre_sans_condamnation_pas_de_plein_droit() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.MEURTRE,
                false,
                true,
                false,
                LocalDate.of(2025, 1, 1),
                2);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("AUCUNE");
        assertThat(res.verdictIndignite()).isEqualTo("CONDAMNATION_REQUISE");
        assertThat(res.representationPossible()).isTrue();
        assertThat(res.alertes()).anyMatch(a -> a.contains("condamnation"));
    }

    @Test
    void delai_5_ans_ecoule_judiciaire_forclos() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.TEMOIGNAGE_FAUX,
                false, true, false,
                LocalDate.of(2019, 1, 1), // > 5 ans avant TODAY 2026-05-23
                2);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("JUDICIAIRE");
        assertThat(res.verdictIndignite()).isEqualTo("DELAI_FORCLOS");
        assertThat(res.delaiForclos()).isTrue();
        assertThat(res.effetDevolution()).contains("forclose");
    }

    @Test
    void delai_5_ans_non_ecoule_judiciaire_recevable() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.VIOLENCES_GRAVES,
                false, true, false,
                LocalDate.of(2024, 1, 1), // 2 ans avant TODAY
                2);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.delaiForclos()).isFalse();
        assertThat(res.verdictIndignite()).isEqualTo("INDIGNITE_JUDICIAIRE_POSSIBLE");
    }

    @Test
    void indignite_judiciaire_envisageable_mais_pas_demandee_alerte() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.VIOLENCES_GRAVES,
                false,
                false, // pas encore demandée
                false,
                LocalDate.of(2025, 6, 1),
                2);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.verdictIndignite()).isEqualTo("INDIGNITE_JUDICIAIRE_POSSIBLE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("demande"));
    }

    @Test
    void delai_action_message_inclut_annees_ecoulees() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.TEMOIGNAGE_FAUX,
                false, true, false,
                LocalDate.of(2024, 5, 23), // exactement 2 ans
                1);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.delaiAction()).contains("5 ans");
        assertThat(res.delaiAction()).contains("729");
    }

    @Test
    void plein_droit_exclut_representation_des_descendants() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.MEURTRE,
                true, false, false,
                LocalDate.of(2025, 1, 1), 1);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.representationPossible()).isFalse();
        assertThat(res.alertes()).anyMatch(a -> a.contains("729-1"));
    }

    @Test
    void judiciaire_autorise_representation_des_descendants() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.VIOLENCES_GRAVES,
                false, true, false,
                LocalDate.of(2024, 1, 1), 2);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.representationPossible()).isTrue();
        assertThat(res.messages()).anyMatch(m -> m.contains("729-1"));
    }

    @Test
    void motif_autre_indignite_judiciaire_envisageable() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.AUTRE,
                false, true, false,
                LocalDate.of(2025, 1, 1), 1);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.typeIndignite()).isEqualTo("JUDICIAIRE");
        assertThat(res.verdictIndignite()).isEqualTo("INDIGNITE_JUDICIAIRE_POSSIBLE");
    }

    @Test
    void base_legale_inclut_articles_clefs() {
        IndigniteSuccessoraleRequest r = req(
                MotifIndigniteEnum.MEURTRE,
                true, false, false,
                LocalDate.of(2025, 1, 1), 1);
        IndigniteSuccessoraleResult res =
                IndigniteSuccessoraleCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.baseLegale()).contains("726-729-1");
        assertThat(res.baseLegale()).contains("2001-1135");
        assertThat(res.baseLegale()).contains("2022-1617");
    }
}
