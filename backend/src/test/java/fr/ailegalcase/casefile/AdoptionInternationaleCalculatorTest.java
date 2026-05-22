package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-17 : tests unitaires du calculateur Adoption internationale FR
 * (art. 370-3 à 370-5 Cciv + Convention La Haye du 29/5/1993).
 */
class AdoptionInternationaleCalculatorTest {

    private static AdoptionInternationaleRequest req(
            String pays,
            Boolean convention,
            Boolean agrement,
            Integer ageAdoptant, Integer ageAdopte,
            Boolean marie,
            VoieProcedureAdoptionEnum voie,
            FormeAdoptionEnum forme,
            Boolean exequatur) {
        return new AdoptionInternationaleRequest(
                pays, convention, agrement, ageAdoptant, ageAdopte,
                marie, voie, forme, exequatur);
    }

    // ── AC1 : pays signataire + agrément → voie OAA convention, délai estimé ──

    @Test
    void ac1_pays_signataire_agrement_voie_oaa_convention() {
        AdoptionInternationaleRequest r = req("VIETNAM", true, true,
                40, 4, true,
                VoieProcedureAdoptionEnum.OAA_AGREE,
                FormeAdoptionEnum.PLENIERE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.conditionsRemplies()).isTrue();
        assertThat(res.voieProcedure()).isEqualTo(VoieProcedureAdoptionEnum.OAA_AGREE);
        assertThat(res.conventionApplicable()).isTrue();
        assertThat(res.alerteKafala()).isFalse();
        assertThat(res.verdict()).isEqualTo("OAA_CONVENTION");
        assertThat(res.delaiEstime()).contains("2 à 5");
        assertThat(res.baseLegale()).contains("370-3");
    }

    // ── AC2 : kafala Maroc → alerte kafala incompatible ──

    @Test
    void ac2_kafala_maroc_alerte_incompatible() {
        AdoptionInternationaleRequest r = req("MAROC", false, true,
                42, 5, true,
                VoieProcedureAdoptionEnum.VOIE_AUTONOME,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.alerteKafala()).isTrue();
        assertThat(res.conditionsRemplies()).isFalse();
        assertThat(res.verdict()).isEqualTo("KAFALA_INCOMPATIBLE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("kafala"));
    }

    @Test
    void kafala_algerie_alerte() {
        AdoptionInternationaleRequest r = req("ALGERIE", false, true,
                40, 5, true,
                VoieProcedureAdoptionEnum.VOIE_AUTONOME,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.alerteKafala()).isTrue();
        assertThat(res.verdict()).isEqualTo("KAFALA_INCOMPATIBLE");
    }

    @Test
    void kafala_tunisie_alerte() {
        AdoptionInternationaleRequest r = req("TUNISIE", false, true,
                40, 5, true,
                VoieProcedureAdoptionEnum.VOIE_AUTONOME,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.alerteKafala()).isTrue();
        assertThat(res.verdict()).isEqualTo("KAFALA_INCOMPATIBLE");
    }

    @Test
    void kafala_avec_accents_normalises() {
        // Le nom avec accent doit être normalisé en MAJUSCULE_SANS_ACCENT.
        AdoptionInternationaleRequest r = req("Algérie", null, true,
                40, 5, true,
                VoieProcedureAdoptionEnum.VOIE_AUTONOME,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.alerteKafala()).isTrue();
    }

    // ── AC3 : sans agrément → verdict AGREMENT_REQUIS ──

    @Test
    void ac3_sans_agrement_verdict_agrement_requis() {
        AdoptionInternationaleRequest r = req("COLOMBIE", true, false,
                40, 4, true,
                VoieProcedureAdoptionEnum.OAA_AGREE,
                FormeAdoptionEnum.PLENIERE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.conditionsRemplies()).isFalse();
        assertThat(res.verdict()).isEqualTo("AGREMENT_REQUIS");
        assertThat(res.alertes()).anyMatch(a -> a.contains("Agrément"));
        assertThat(res.delaiEstime()).contains("agrément");
    }

    // ── AC4 : country = BELGIQUE → IllegalArgumentException ──

    @Test
    void ac4_pays_belgique_leve_exception() {
        AdoptionInternationaleRequest r = req("COLOMBIE", true, true,
                40, 4, true,
                VoieProcedureAdoptionEnum.OAA_AGREE,
                FormeAdoptionEnum.PLENIERE, false);
        assertThatThrownBy(() -> AdoptionInternationaleCalculator.compute(r, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Voie autonome → alerte risque ──

    @Test
    void voie_autonome_genere_alerte_risque() {
        AdoptionInternationaleRequest r = req("COLOMBIE", true, true,
                40, 4, true,
                VoieProcedureAdoptionEnum.VOIE_AUTONOME,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.verdict()).isEqualTo("VOIE_AUTONOME_RISQUE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("autonome"));
    }

    // ── Exequatur requis → verdict EXEQUATUR_REQUIS ──

    @Test
    void exequatur_requis_change_verdict() {
        AdoptionInternationaleRequest r = req("VIETNAM", true, true,
                40, 4, true,
                VoieProcedureAdoptionEnum.OAA_AGREE,
                FormeAdoptionEnum.PLENIERE, true);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.exequaturRequis()).isTrue();
        assertThat(res.verdict()).isEqualTo("EXEQUATUR_REQUIS");
        assertThat(res.messages()).anyMatch(m -> m.contains("370-5") || m.contains("exequatur"));
    }

    // ── Pays hors convention → alerte risque transcription ──

    @Test
    void pays_hors_convention_alerte_transcription() {
        AdoptionInternationaleRequest r = req("PAYS_INCONNU_TEST", false, true,
                40, 4, true,
                VoieProcedureAdoptionEnum.VOIE_AUTONOME,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.conventionApplicable()).isFalse();
        assertThat(res.alertes()).anyMatch(a -> a.contains("transcription") || a.contains("hcch"));
    }

    // ── Plénière → alerte irréversibilité ──

    @Test
    void forme_pleniere_genere_alerte_irreversibilite() {
        AdoptionInternationaleRequest r = req("VIETNAM", true, true,
                40, 4, true,
                VoieProcedureAdoptionEnum.OAA_AGREE,
                FormeAdoptionEnum.PLENIERE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("PLÉNIÈRE") || a.contains("356"));
    }

    // ── Différence d'âge < 15 ans → alerte ──

    @Test
    void difference_age_inferieure_quinze_genere_alerte() {
        AdoptionInternationaleRequest r = req("VIETNAM", true, true,
                30, 20, true,
                VoieProcedureAdoptionEnum.OAA_AGREE,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("Différence d'âge") || a.contains("344"));
    }

    // ── Convention applicable auto-déterminée si flag null + pays connu ──

    @Test
    void convention_auto_determinee_si_pays_connu() {
        AdoptionInternationaleRequest r = req("COLOMBIE", null, true,
                40, 4, true,
                VoieProcedureAdoptionEnum.OAA_AGREE,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.conventionApplicable()).isTrue();
    }

    // ── Voie procédure null → OAA_AGREE si convention applicable ──

    @Test
    void voie_null_default_oaa_si_convention_applicable() {
        AdoptionInternationaleRequest r = req("VIETNAM", true, true,
                40, 4, true,
                null,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.voieProcedure()).isEqualTo(VoieProcedureAdoptionEnum.OAA_AGREE);
    }

    // ── Voie procédure null + hors convention → VOIE_AUTONOME par défaut ──

    @Test
    void voie_null_default_autonome_si_hors_convention() {
        AdoptionInternationaleRequest r = req("PAYS_INCONNU_TEST", false, true,
                40, 4, true,
                null,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.voieProcedure()).isEqualTo(VoieProcedureAdoptionEnum.VOIE_AUTONOME);
    }

    // ── Réforme 2022 mentionnée dans les messages ──

    @Test
    void reforme_2022_mentionnee_dans_messages() {
        AdoptionInternationaleRequest r = req("VIETNAM", true, true,
                40, 4, true,
                VoieProcedureAdoptionEnum.OAA_AGREE,
                FormeAdoptionEnum.SIMPLE, false);
        AdoptionInternationaleResult res =
                AdoptionInternationaleCalculator.compute(r, "FRANCE");
        assertThat(res.messages()).anyMatch(m -> m.contains("2022-219") || m.contains("Réforme 2022"));
    }

    // ── Helper normalizePays ──

    @Test
    void normalize_pays_supprime_accents_et_majuscule() {
        assertThat(AdoptionInternationaleCalculator.normalizePays("Algérie")).isEqualTo("ALGERIE");
        assertThat(AdoptionInternationaleCalculator.normalizePays("  Vietnam  ")).isEqualTo("VIETNAM");
        assertThat(AdoptionInternationaleCalculator.normalizePays("Burkina Faso")).isEqualTo("BURKINA_FASO");
        assertThat(AdoptionInternationaleCalculator.normalizePays("")).isNull();
        assertThat(AdoptionInternationaleCalculator.normalizePays(null)).isNull();
    }
}
