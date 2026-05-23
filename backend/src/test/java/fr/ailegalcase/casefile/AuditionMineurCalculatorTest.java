package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-13 : tests unitaires du calculateur Audition du mineur par le JAF
 * FR (art. 388-1 Cciv + art. 1074-1 à 1074-3 CPC + CIDE art. 12).
 */
class AuditionMineurCalculatorTest {

    private static AuditionMineurRequest req(
            Integer age,
            CapaciteDiscernementEnum disc,
            Boolean demandeFormalisee,
            Boolean demandeParEnfant,
            Boolean refusMotive,
            String motivationRefus,
            ProcedureAuditionEnum procedure) {
        return new AuditionMineurRequest(
                age, disc, demandeFormalisee, demandeParEnfant,
                refusMotive, motivationRefus, procedure);
    }

    // ── AC1 : enfant 10 ans + discernement probable → droit reconnu ──

    @Test
    void ac1_enfant_10_ans_discernement_probable_audition_recommandee() {
        AuditionMineurRequest r = req(10, CapaciteDiscernementEnum.PROBABLE,
                true, false, false, null, ProcedureAuditionEnum.DIVORCE);
        AuditionMineurResult res = AuditionMineurCalculator.compute(r, "FRANCE");
        assertThat(res.conditionsRemplies()).isTrue();
        assertThat(res.droitAuditionReconnu()).isTrue();
        assertThat(res.verdict()).isEqualTo("AUDITION_RECOMMANDEE");
        assertThat(res.modaliteRecommandee()).isNotNull();
        assertThat(res.baseLegale()).contains("388-1");
        assertThat(res.refusContestable()).isFalse();
    }

    // ── AC2 : enfant 3 ans → alerte discernement improbable ──

    @Test
    void ac2_enfant_3_ans_alerte_discernement_improbable() {
        AuditionMineurRequest r = req(3, CapaciteDiscernementEnum.PROBABLE,
                true, false, false, null, ProcedureAuditionEnum.DIVORCE);
        AuditionMineurResult res = AuditionMineurCalculator.compute(r, "FRANCE");
        assertThat(res.alertes())
                .anyMatch(a -> a.contains("discernement") && a.contains("improbable"));
    }

    // ── AC3 : refus non motivé → voie de recours suggérée ──

    @Test
    void ac3_refus_non_motive_voie_de_recours() {
        AuditionMineurRequest r = req(12, CapaciteDiscernementEnum.CERTAINE,
                true, false, true, null, ProcedureAuditionEnum.AUTORITE_PARENTALE);
        AuditionMineurResult res = AuditionMineurCalculator.compute(r, "FRANCE");
        assertThat(res.refusContestable()).isTrue();
        assertThat(res.verdict()).isEqualTo("REFUS_CONTESTABLE");
        assertThat(res.alertes())
                .anyMatch(a -> a.contains("motivation") || a.contains("recours"));
    }

    @Test
    void refus_avec_motivation_blanche_voie_de_recours() {
        AuditionMineurRequest r = req(12, CapaciteDiscernementEnum.CERTAINE,
                true, false, true, "   ", ProcedureAuditionEnum.GARDE);
        AuditionMineurResult res = AuditionMineurCalculator.compute(r, "FRANCE");
        assertThat(res.refusContestable()).isTrue();
        assertThat(res.verdict()).isEqualTo("REFUS_CONTESTABLE");
    }

    @Test
    void refus_motive_serieux_audition_refusee_valablement() {
        AuditionMineurRequest r = req(12, CapaciteDiscernementEnum.CERTAINE,
                true, false, true,
                "Discernement insuffisant caractérisé par expertise psy",
                ProcedureAuditionEnum.GARDE);
        AuditionMineurResult res = AuditionMineurCalculator.compute(r, "FRANCE");
        assertThat(res.refusContestable()).isFalse();
        assertThat(res.verdict()).isEqualTo("AUDITION_REFUSEE_VALABLEMENT");
    }

    // ── AC4 : country=BELGIQUE → 400 (illegal argument calculator) ──

    @Test
    void ac4_country_belgique_throws() {
        AuditionMineurRequest r = req(10, CapaciteDiscernementEnum.PROBABLE,
                true, false, false, null, ProcedureAuditionEnum.DIVORCE);
        assertThatThrownBy(() -> AuditionMineurCalculator.compute(r, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Cas spécifique art. 388-1 al. 1 : demande par l'enfant lui-même ──

    @Test
    void demande_par_enfant_lui_meme_avec_discernement_certain_audition_de_droit() {
        AuditionMineurRequest r = req(13, CapaciteDiscernementEnum.CERTAINE,
                false, true, false, null, ProcedureAuditionEnum.DIVORCE);
        AuditionMineurResult res = AuditionMineurCalculator.compute(r, "FRANCE");
        assertThat(res.conditionsRemplies()).isTrue();
        assertThat(res.droitAuditionReconnu()).isTrue();
        assertThat(res.verdict()).isEqualTo("AUDITION_DE_DROIT");
        assertThat(res.messages()).anyMatch(m -> m.contains("388-1 al. 1")
                || m.contains("demandé lui-même"));
    }

    @Test
    void demande_par_enfant_lui_meme_discernement_douteux_attente_evaluation() {
        AuditionMineurRequest r = req(8, CapaciteDiscernementEnum.DOUTEUSE,
                false, true, false, null, ProcedureAuditionEnum.GARDE);
        AuditionMineurResult res = AuditionMineurCalculator.compute(r, "FRANCE");
        assertThat(res.droitAuditionReconnu()).isTrue();
        assertThat(res.verdict()).isEqualTo("DISCERNEMENT_DOUTEUX");
        assertThat(res.alertes()).anyMatch(a -> a.contains("psychologue")
                || a.contains("expert"));
    }

    // ── Contexte conflictuel : alerte manipulation ──

    @Test
    void contexte_divorce_alerte_manipulation() {
        AuditionMineurRequest r = req(10, CapaciteDiscernementEnum.PROBABLE,
                true, false, false, null, ProcedureAuditionEnum.DIVORCE);
        AuditionMineurResult res = AuditionMineurCalculator.compute(r, "FRANCE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("manipulation")
                || a.contains("conflictuelle"));
    }

    // ── Sans demande formalisée → rappel d'opportunité ──

    @Test
    void sans_demande_renvoie_ok_avec_message_opportunite() {
        AuditionMineurRequest r = req(10, CapaciteDiscernementEnum.PROBABLE,
                false, false, false, null, ProcedureAuditionEnum.AUTRE);
        AuditionMineurResult res = AuditionMineurCalculator.compute(r, "FRANCE");
        assertThat(res.conditionsRemplies()).isFalse();
        assertThat(res.droitAuditionReconnu()).isFalse();
        assertThat(res.verdict()).isEqualTo("OK");
        assertThat(res.messages()).anyMatch(m -> m.contains("opportunité")
                || m.contains("aucune demande")
                || m.contains("Aucune demande"));
    }

    // ── Demande formalisée + discernement douteux ──

    @Test
    void demande_formalisee_discernement_douteux_renvoie_discernement_douteux() {
        AuditionMineurRequest r = req(7, CapaciteDiscernementEnum.DOUTEUSE,
                true, false, false, null, ProcedureAuditionEnum.GARDE);
        AuditionMineurResult res = AuditionMineurCalculator.compute(r, "FRANCE");
        assertThat(res.conditionsRemplies()).isFalse();
        assertThat(res.verdict()).isEqualTo("DISCERNEMENT_DOUTEUX");
    }

    // ── Modalités recommandées ──

    @Test
    void modalite_avec_tiers_enfant_jeune_conflictuel() {
        ModaliteAuditionEnum m = AuditionMineurCalculator.recommendModalite(
                6, ProcedureAuditionEnum.DIVORCE, true);
        assertThat(m).isEqualTo(ModaliteAuditionEnum.AVEC_TIERS);
    }

    @Test
    void modalite_avec_avocat_enfant_moyen_demande_formalisee() {
        ModaliteAuditionEnum m = AuditionMineurCalculator.recommendModalite(
                10, ProcedureAuditionEnum.AUTRE, true);
        assertThat(m).isEqualTo(ModaliteAuditionEnum.AVEC_AVOCAT);
    }

    @Test
    void modalite_seul_adolescent() {
        ModaliteAuditionEnum m = AuditionMineurCalculator.recommendModalite(
                15, ProcedureAuditionEnum.AUTRE, false);
        assertThat(m).isEqualTo(ModaliteAuditionEnum.SEUL);
    }

    // ── Bornes d'âge ──

    @Test
    void age_negatif_throws() {
        AuditionMineurRequest r = req(-1, CapaciteDiscernementEnum.PROBABLE,
                true, false, false, null, ProcedureAuditionEnum.DIVORCE);
        assertThatThrownBy(() -> AuditionMineurCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void age_majeur_throws() {
        AuditionMineurRequest r = req(18, CapaciteDiscernementEnum.CERTAINE,
                true, false, false, null, ProcedureAuditionEnum.DIVORCE);
        assertThatThrownBy(() -> AuditionMineurCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void age_null_throws() {
        AuditionMineurRequest r = req(null, CapaciteDiscernementEnum.PROBABLE,
                true, false, false, null, ProcedureAuditionEnum.DIVORCE);
        assertThatThrownBy(() -> AuditionMineurCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
