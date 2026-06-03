package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdonnanceProtectionCalculatorTest {

    private static final LocalDate D = LocalDate.of(2026, 4, 20);

    // =========================================================================
    // Enums : violences (5)
    // =========================================================================

    @Test
    void violences_physiques_isRecognized() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of(), false);
        assertThat(r.violencesAlleguees()).containsExactly("PHYSIQUES");
    }

    @Test
    void violences_psychologiques_isRecognized() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PSYCHOLOGIQUES"), List.of(), false, false, null,
                false, false, false, List.of(), false);
        assertThat(r.violencesAlleguees()).containsExactly("PSYCHOLOGIQUES");
    }

    @Test
    void violences_sexuelles_isRecognized() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("SEXUELLES"), List.of(), false, false, null,
                false, false, false, List.of(), false);
        assertThat(r.violencesAlleguees()).containsExactly("SEXUELLES");
    }

    @Test
    void violences_economiques_isRecognized() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("ECONOMIQUES"), List.of(), false, false, null,
                false, false, false, List.of(), false);
        assertThat(r.violencesAlleguees()).containsExactly("ECONOMIQUES");
    }

    @Test
    void violences_menacesMort_isRecognized() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("MENACES_MORT"), List.of(), false, false, null,
                false, false, false, List.of(), false);
        assertThat(r.violencesAlleguees()).containsExactly("MENACES_MORT");
    }

    // =========================================================================
    // Enums : preuves (8)
    // =========================================================================

    @Test
    void preuves_huitTypes_recognized() {
        List<String> all = List.of(
                "CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL",
                "TEMOIGNAGES", "PHOTOS", "PLAINTE_DEPOSEE",
                "JUGEMENT_CORRECTIONNEL", "AUTRE");
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), all,
                false, false, null, false, false, false, List.of(), false);
        assertThat(r.preuvesViolences()).hasSize(8).containsAll(all);
    }

    // =========================================================================
    // Enums : mesures (7)
    // =========================================================================

    @Test
    void mesures_septTypes_recognized() {
        List<String> all = List.of(
                "EVICTION_CONJOINT", "INTERDICTION_APPROCHER", "TGD", "BAR",
                "INTERDICTION_PARAITRE", "OBLIGATION_SOIN", "RESIDENCE_ENFANTS");
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), true, true, null,
                true, false, false, all, false);
        assertThat(r.demandeMesures()).hasSize(7).containsAll(all);
    }

    // =========================================================================
    // Scoring
    // =========================================================================

    @Test
    void compute_scoreMaximal_100() {
        // 3 violences + 4 preuves + danger + enfants + dépendance + 1ère demande
        // 30 + 30 + 20 + 10 + 5 + 5 = 100
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D,
                List.of("PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"),
                List.of("CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL", "TEMOIGNAGES"),
                true, true, List.of(5, 8),
                true, true, false,
                List.of("EVICTION_CONJOINT", "INTERDICTION_APPROCHER", "TGD", "BAR"), false);
        assertThat(r.scoreVraisemblance()).isEqualTo(100);
        assertThat(r.verdictProbabiliteOctroi()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_scoreFaible_unViolence_uneSeulePreuve() {
        // 0 (1 violence < 3) + 0 (1 preuve < 3) + 0 + 0 + 0 + 5 (1ère) = 5
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of("MAIN_COURANTE"),
                false, false, null, false, false, false, List.of(), false);
        assertThat(r.scoreVraisemblance()).isEqualTo(5);
        assertThat(r.verdictProbabiliteOctroi()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_scoreMoyenne_dangerEnfants_avecPreuves() {
        // 0 (2 violences < 3) + 30 (3 preuves) + 20 (danger) + 10 (enfants) + 0 + 5 (1ère) = 65
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES", "PSYCHOLOGIQUES"),
                List.of("CONSTAT_HUISSIER", "CERTIFICAT_MEDICAL", "TEMOIGNAGES"),
                true, true, List.of(3),
                false, false, false, List.of(), false);
        assertThat(r.scoreVraisemblance()).isEqualTo(65);
        assertThat(r.verdictProbabiliteOctroi()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_borneMaxScore100() {
        // Forcer toutes les conditions max
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D,
                List.of("PHYSIQUES", "PSYCHOLOGIQUES", "SEXUELLES", "ECONOMIQUES", "MENACES_MORT"),
                List.of("CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL",
                        "TEMOIGNAGES", "PHOTOS", "PLAINTE_DEPOSEE", "JUGEMENT_CORRECTIONNEL", "AUTRE"),
                true, true, List.of(5),
                true, true, false, List.of(), false);
        assertThat(r.scoreVraisemblance()).isLessThanOrEqualTo(100);
    }

    @Test
    void compute_dejaProtege_noPlusDe5() {
        // 30 + 30 + 20 + 10 + 5 + 0 (déjà protégé) = 95
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"),
                List.of("CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL"),
                true, true, null, true, true, true, List.of(), false);
        assertThat(r.scoreVraisemblance()).isEqualTo(95);
    }

    // =========================================================================
    // Verdict
    // =========================================================================

    @Test
    void verdict_eleveeAuSeuil75() {
        // 30 + 30 + 20 (danger) + 0 + 0 + 0 (déjà protégé) = 80 ELEVEE
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"),
                List.of("CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL"),
                true, false, null, false, false, true, List.of(), false);
        assertThat(r.scoreVraisemblance()).isEqualTo(80);
        assertThat(r.verdictProbabiliteOctroi()).isEqualTo("ELEVEE");
    }

    @Test
    void verdict_faibleSous50() {
        // 30 (3 violences) + 0 + 0 + 0 + 0 + 5 = 35 FAIBLE
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"),
                List.of(), false, false, null, false, false, false, List.of(), false);
        assertThat(r.scoreVraisemblance()).isEqualTo(35);
        assertThat(r.verdictProbabiliteOctroi()).isEqualTo("FAIBLE");
    }

    // =========================================================================
    // Mesures recommandées (intersection contextuelle)
    // =========================================================================

    @Test
    void mesures_tgd_seulementSiDangerImmediat() {
        OrdonnanceProtectionResult avec = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), true, false, null,
                false, false, false, List.of("TGD"), false);
        OrdonnanceProtectionResult sans = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of("TGD"), false);
        assertThat(avec.mesuresRecommandees()).contains("TGD");
        assertThat(sans.mesuresRecommandees()).doesNotContain("TGD");
    }

    @Test
    void mesures_bar_seulementSiDangerImmediat() {
        OrdonnanceProtectionResult avec = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), true, false, null,
                false, false, false, List.of("BAR"), false);
        OrdonnanceProtectionResult sans = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of("BAR"), false);
        assertThat(avec.mesuresRecommandees()).contains("BAR");
        assertThat(sans.mesuresRecommandees()).doesNotContain("BAR");
    }

    // =========================================================================
    // SF-222-05 : DEC (Dispositif Électronique de Contrôle)
    // =========================================================================

    @Test
    void dec_recommande_siEnvisageEtDangerImmediat() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), true, false, null,
                false, false, false, List.of("INTERDICTION_APPROCHER"), true);
        assertThat(r.decEnvisage()).isTrue();
        assertThat(r.mesuresRecommandees()).contains("DEC");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("DEC").contains("rapprochement"));
    }

    @Test
    void dec_nonRecommande_siEnvisageMaisPasDeDangerImmediat() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of("INTERDICTION_APPROCHER"), true);
        assertThat(r.decEnvisage()).isTrue();
        assertThat(r.mesuresRecommandees()).doesNotContain("DEC");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("DEC").contains("danger immédiat"));
    }

    @Test
    void dec_nonRecommande_siNonEnvisageMemeAvecDangerImmediat() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), true, false, null,
                false, false, false, List.of("BAR"), false);
        assertThat(r.decEnvisage()).isFalse();
        assertThat(r.mesuresRecommandees()).doesNotContain("DEC");
        // anti-régression : le BAR reste recommandé indépendamment du DEC.
        assertThat(r.mesuresRecommandees()).contains("BAR");
    }

    @Test
    void mesures_eviction_seulementSiLogementCommun() {
        OrdonnanceProtectionResult avec = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                true, false, false, List.of("EVICTION_CONJOINT"), false);
        OrdonnanceProtectionResult sans = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of("EVICTION_CONJOINT"), false);
        assertThat(avec.mesuresRecommandees()).contains("EVICTION_CONJOINT");
        assertThat(sans.mesuresRecommandees()).doesNotContain("EVICTION_CONJOINT");
    }

    @Test
    void mesures_residenceEnfants_seulementSiPresenceEnfants() {
        OrdonnanceProtectionResult avec = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, true, List.of(5),
                false, false, false, List.of("RESIDENCE_ENFANTS"), false);
        OrdonnanceProtectionResult sans = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of("RESIDENCE_ENFANTS"), false);
        assertThat(avec.mesuresRecommandees()).contains("RESIDENCE_ENFANTS");
        assertThat(sans.mesuresRecommandees()).doesNotContain("RESIDENCE_ENFANTS");
    }

    @Test
    void mesures_interdictionApprocher_toujoursRetenue() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of("INTERDICTION_APPROCHER"), false);
        assertThat(r.mesuresRecommandees()).containsExactly("INTERDICTION_APPROCHER");
    }

    @Test
    void mesures_interdictionParaitre_etObligationSoin_toujoursRetenue() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false,
                List.of("INTERDICTION_PARAITRE", "OBLIGATION_SOIN"), false);
        assertThat(r.mesuresRecommandees()).contains("INTERDICTION_PARAITRE", "OBLIGATION_SOIN");
    }

    // =========================================================================
    // Délai, base juridique, formule, messages
    // =========================================================================

    @Test
    void delai_estTujours6Jours() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of(), false);
        assertThat(r.delaiTraitementJoursPrevisionnel()).isEqualTo(6);
    }

    @Test
    void baseJuridique_contientArticles() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of(), false);
        assertThat(r.baseJuridique())
                .contains("515-9").contains("515-13").contains("30/07/2020");
    }

    @Test
    void formule_contientScoreEtVerdict() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"),
                List.of("CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL"),
                true, true, null, true, false, false,
                List.of("EVICTION_CONJOINT", "TGD"), false);
        assertThat(r.formule())
                .contains("Score").contains(String.valueOf(r.scoreVraisemblance()));
    }

    @Test
    void messages_audienceUrgente_estPresent() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of(), false);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("urgence").contains("515-11"));
    }

    @Test
    void messages_tgd24h_siTgdRecommande() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), true, false, null,
                false, false, false, List.of("TGD"), false);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("TGD").contains("24h"));
    }

    @Test
    void messages_bar_siBarRecommande() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), true, false, null,
                false, false, false, List.of("BAR"), false);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("BAR").contains("30/07/2020"));
    }

    // =========================================================================
    // Validation & erreurs
    // =========================================================================

    @Test
    void violencesVides_throws() {
        assertThatThrownBy(() -> OrdonnanceProtectionCalculator.compute(
                D, List.of(), List.of(), false, false, null,
                false, false, false, List.of(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("violencesAlleguees");
    }

    @Test
    void violenceInconnue_throws() {
        assertThatThrownBy(() -> OrdonnanceProtectionCalculator.compute(
                D, List.of("HARCELEMENT"), List.of(), false, false, null,
                false, false, false, List.of(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Violence inconnue");
    }

    @Test
    void preuveInconnue_throws() {
        assertThatThrownBy(() -> OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of("VIDEO"),
                false, false, null, false, false, false, List.of(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Preuve inconnue");
    }

    @Test
    void mesureInconnue_throws() {
        assertThatThrownBy(() -> OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"), List.of(), false, false, null,
                false, false, false, List.of("ASSIGNATION_RESIDENCE"), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mesure inconnue");
    }

    @Test
    void violencesDupliquees_estDeduplique() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES", "PHYSIQUES"), List.of(),
                false, false, null, false, false, false, List.of(), false);
        assertThat(r.violencesAlleguees()).hasSize(1);
    }

    @Test
    void preuveBlank_estIgnoree() {
        OrdonnanceProtectionResult r = OrdonnanceProtectionCalculator.compute(
                D, List.of("PHYSIQUES"),
                List.of("CONSTAT_HUISSIER", "", "  "),
                false, false, null, false, false, false, List.of(), false);
        assertThat(r.preuvesViolences()).hasSize(1);
    }
}
