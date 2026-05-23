package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-21 : tests unitaires du calculateur Recel successoral FR
 * (art. 778 Cciv + Cass. 1ère civ., 14/11/2012).
 */
class RecelSuccessionCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 23);

    private static RecelSuccessionRequest req(
            TypeRecelEnum type,
            Integer valeur,
            PreuveRecelEnum preuve,
            ReceleurQualiteEnum qualite,
            LocalDate dateOuverture,
            Boolean actionIntentee) {
        return new RecelSuccessionRequest(type, valeur, preuve, qualite, dateOuverture, actionIntentee);
    }

    // ── AC1 : dissimulation bien + preuve directe → sanction art. 778 ──

    @Test
    void ac1_dissimulation_bien_avec_aveux_recel_caracterise() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                50_000,
                PreuveRecelEnum.AVEUX,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2024, 6, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.qualificationRecel()).isEqualTo("CARACTERISE");
        assertThat(res.verdictRecel()).isEqualTo("RECEL_CARACTERISE");
        assertThat(res.sanctionCivile()).contains("778");
        assertThat(res.sanctionCivile()).contains("Privation");
        assertThat(res.baseLegale()).contains("778");
    }

    @Test
    void dissimulation_donation_avec_document_recel_caracterise() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_DONATION,
                30_000,
                PreuveRecelEnum.DOCUMENT,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 1, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.qualificationRecel()).isEqualTo("CARACTERISE");
        assertThat(res.messages()).anyMatch(m -> m.contains("850") || m.contains("donation"));
    }

    @Test
    void faisceau_indices_recel_probable() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                20_000,
                PreuveRecelEnum.FAISCEAU_INDICES,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2024, 11, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.qualificationRecel()).isEqualTo("PROBABLE");
        assertThat(res.verdictRecel()).isEqualTo("RECEL_PROBABLE_FAISCEAU");
        assertThat(res.messages()).anyMatch(m -> m.contains("14/11/2012") || m.contains("faisceau"));
    }

    @Test
    void temoignage_seul_recel_probable() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                null,
                PreuveRecelEnum.TEMOIGNAGE,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2024, 11, 1),
                false);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.qualificationRecel()).isEqualTo("PROBABLE");
    }

    // ── AC2 : destruction testament → articulation pénale signalée ──

    @Test
    void ac2_destruction_testament_volet_penal_signale() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DESTRUCTION_TESTAMENT,
                null,
                PreuveRecelEnum.TEMOIGNAGE,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 2, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.voletPenalSignale()).isTrue();
        assertThat(res.messages()).anyMatch(m -> m.contains("441-8"));
        assertThat(res.messages()).anyMatch(m -> m.contains("destruction de titre")
                || m.contains("Articulation"));
    }

    // ── AC3 : country=BELGIQUE → IAE ──

    @Test
    void ac3_belgique_throws_illegal_argument() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN, 10_000,
                PreuveRecelEnum.AVEUX, ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2024, 1, 1), true);
        assertThatThrownBy(() ->
                RecelSuccessionCalculator.compute(r, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Cas complémentaires ──

    @Test
    void sans_preuve_qualification_insuffisante() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                null,
                PreuveRecelEnum.AUCUNE,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 1, 1),
                false);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.qualificationRecel()).isEqualTo("INSUFFISANT");
        assertThat(res.verdictRecel()).isEqualTo("PREUVE_INSUFFISANTE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("preuve")
                || a.contains("Aucune preuve"));
    }

    @Test
    void preuve_null_qualification_insuffisante() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                null,
                null,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 1, 1),
                false);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.qualificationRecel()).isEqualTo("INSUFFISANT");
    }

    @Test
    void tiers_complice_hors_perimetre() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                10_000,
                PreuveRecelEnum.AVEUX,
                ReceleurQualiteEnum.TIERS_COMPLICITE,
                LocalDate.of(2025, 1, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.qualificationRecel()).isEqualTo("HORS_PERIMETRE");
        assertThat(res.verdictRecel()).isEqualTo("QUALITE_RECELEUR_EXCLUE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("tiers") || a.contains("321-1"));
    }

    @Test
    void delai_5_ans_ecoule_forclos() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                15_000,
                PreuveRecelEnum.DOCUMENT,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2019, 1, 1), // > 5 ans avant TODAY
                false);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.delaiForclos()).isTrue();
        assertThat(res.verdictRecel()).isEqualTo("DELAI_FORCLOS");
        assertThat(res.alertes()).anyMatch(a -> a.contains("découverte")
                || a.contains("prescr"));
    }

    @Test
    void delai_5_ans_non_ecoule_recevable() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                15_000,
                PreuveRecelEnum.DOCUMENT,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2024, 1, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.delaiForclos()).isFalse();
        assertThat(res.verdictRecel()).isEqualTo("RECEL_CARACTERISE");
    }

    @Test
    void type_null_pas_de_recel() {
        RecelSuccessionRequest r = req(
                null,
                null,
                PreuveRecelEnum.AVEUX,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 1, 1),
                false);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.qualificationRecel()).isEqualTo("PAS_DE_RECEL");
        assertThat(res.verdictRecel()).isEqualTo("PAS_DE_RECEL");
    }

    @Test
    void recel_creance_messages_specifiques() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.RECEL_CREANCE,
                12_000,
                PreuveRecelEnum.DOCUMENT,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 1, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.messages()).anyMatch(m -> m.contains("créance"));
    }

    @Test
    void type_autre_messages_specifiques() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.AUTRE,
                null,
                PreuveRecelEnum.FAISCEAU_INDICES,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 1, 1),
                false);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.messages()).anyMatch(m -> m.contains("autre")
                || m.contains("caractériser"));
    }

    @Test
    void action_non_intentee_alerte() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                10_000,
                PreuveRecelEnum.AVEUX,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 1, 1),
                false);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.alertes()).anyMatch(a -> a.contains("action")
                || a.contains("Aucune action"));
    }

    @Test
    void legataire_recel_caracterise_meme_qualite() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                25_000,
                PreuveRecelEnum.DOCUMENT,
                ReceleurQualiteEnum.LEGATAIRE,
                LocalDate.of(2025, 1, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.qualificationRecel()).isEqualTo("CARACTERISE");
    }

    @Test
    void donataire_recel_caracterise_meme_qualite() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_DONATION,
                40_000,
                PreuveRecelEnum.EXPERTISE,
                ReceleurQualiteEnum.DONATAIRE,
                LocalDate.of(2025, 1, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.qualificationRecel()).isEqualTo("CARACTERISE");
    }

    @Test
    void valeur_renseignee_inclus_dans_messages() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                42_000,
                PreuveRecelEnum.DOCUMENT,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 1, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.messages()).anyMatch(m -> m.contains("42000")
                || m.contains("42 000"));
    }

    @Test
    void base_legale_inclut_articles_clefs() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                10_000,
                PreuveRecelEnum.AVEUX,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 1, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.baseLegale()).contains("778");
        assertThat(res.baseLegale()).contains("14/11/2012");
        assertThat(res.baseLegale()).contains("441-8");
    }

    @Test
    void delai_action_message_inclut_5_ans() {
        RecelSuccessionRequest r = req(
                TypeRecelEnum.DISSIMULATION_BIEN,
                10_000,
                PreuveRecelEnum.AVEUX,
                ReceleurQualiteEnum.HERITIER,
                LocalDate.of(2025, 1, 1),
                true);
        RecelSuccessionResult res =
                RecelSuccessionCalculator.compute(r, "FRANCE", TODAY);
        assertThat(res.delaiAction()).contains("5 ans");
        assertThat(res.delaiAction()).contains("2224");
    }
}
