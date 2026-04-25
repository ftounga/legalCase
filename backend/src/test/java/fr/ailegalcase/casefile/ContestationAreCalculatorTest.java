package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContestationAreCalculatorTest {

    private static final LocalDate D_NOTIF = LocalDate.of(2026, 3, 1);
    private static final LocalDate D_RECOURS_OK = LocalDate.of(2026, 4, 1); // +31 jours
    private static final LocalDate D_RECOURS_KO = LocalDate.of(2026, 6, 1); // +92 jours
    private static final BigDecimal MONTANT_SIGNIF = new BigDecimal("1500.00");
    private static final BigDecimal MONTANT_FAIBLE = new BigDecimal("100.00");

    // =========================================================================
    // Scoring
    // =========================================================================

    @Test
    void compute_scoreMaximal_100() {
        // preuves≥2 (+30) + motif reconnu (+25) + pas tribunal (+20) + delai (+15) + montant (+10) = 100
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS",
                "ERREUR_CALCUL_REMUNERATION_REFERENCE",
                D_NOTIF, D_RECOURS_OK,
                List.of("BULLETIN_PAIE", "ATTESTATION_EMPLOYEUR"),
                MONTANT_SIGNIF, false, true);
        assertThat(r.scoreSuccessProbable()).isEqualTo(100);
        assertThat(r.verdictRecommandation()).isEqualTo("RECOURS_HIERARCHIQUE_PRIORITAIRE");
    }

    @Test
    void compute_scoreMin_0() {
        // motif AUTRE (mais pas type AUTRE — donc pas verdict AUTRE_VOIE)
        // 0 (1 preuve) + 0 (motif AUTRE) + 0 (déjà tribunal) + 0 (délai non resp) + 0 (montant null)
        ContestationAreResult r = ContestationAreCalculator.compute(
                "MONTANT_INDEMNITE", "AUTRE",
                D_NOTIF, null, List.of(), null, true, false);
        assertThat(r.scoreSuccessProbable()).isEqualTo(0);
        assertThat(r.verdictRecommandation()).isEqualTo("INSUFFISAMMENT_FONDE");
    }

    @Test
    void verdict_recoursHierarchiquePrioritaire_seuil70() {
        // motif reconnu (+25) + preuves≥2 (+30) + delai (+15) = 70
        ContestationAreResult r = ContestationAreCalculator.compute(
                "MONTANT_INDEMNITE", "ERREUR_CALCUL_REMUNERATION_REFERENCE",
                D_NOTIF, D_RECOURS_OK,
                List.of("BULLETIN_PAIE", "ATTESTATION_EMPLOYEUR"),
                MONTANT_FAIBLE, true, true);
        assertThat(r.scoreSuccessProbable()).isEqualTo(70);
        assertThat(r.verdictRecommandation()).isEqualTo("RECOURS_HIERARCHIQUE_PRIORITAIRE");
    }

    @Test
    void verdict_contentieuxTaDirect_entre40Et69() {
        // preuves≥2 (+30) + delai (+15) = 45 → CONTENTIEUX_TA_DIRECT_POSSIBLE
        ContestationAreResult r = ContestationAreCalculator.compute(
                "RADIATION", "AUTRE",
                D_NOTIF, D_RECOURS_OK,
                List.of("BULLETIN_PAIE", "CERTIFICAT_TRAVAIL"),
                null, true, true);
        assertThat(r.scoreSuccessProbable()).isEqualTo(45);
        assertThat(r.verdictRecommandation()).isEqualTo("CONTENTIEUX_TA_DIRECT_POSSIBLE");
    }

    @Test
    void verdict_insuffisammentFonde_sousSeuil40() {
        // motif reconnu (+25) seul = 25 < 40 → INSUFFISAMMENT_FONDE
        ContestationAreResult r = ContestationAreCalculator.compute(
                "TROP_PERCU", "REFUS_INJUSTIFIE",
                D_NOTIF, null, List.of("BULLETIN_PAIE"),
                null, true, false);
        assertThat(r.scoreSuccessProbable()).isEqualTo(25);
        assertThat(r.verdictRecommandation()).isEqualTo("INSUFFISAMMENT_FONDE");
    }

    @Test
    void verdict_autreVoie_siTypeAutreEtMotifAutre() {
        // Même avec un score élevé : type AUTRE + motif AUTRE → AUTRE_VOIE
        ContestationAreResult r = ContestationAreCalculator.compute(
                "AUTRE", "AUTRE",
                D_NOTIF, D_RECOURS_OK,
                List.of("BULLETIN_PAIE", "ATTESTATION_EMPLOYEUR"),
                MONTANT_SIGNIF, false, true);
        assertThat(r.verdictRecommandation()).isEqualTo("AUTRE_VOIE");
    }

    // =========================================================================
    // Délais
    // =========================================================================

    @Test
    void delaiRecoursHierarchiqueOk_dansLes60Jours() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK, List.of(), null, false, true);
        assertThat(r.delaiRecoursHierarchiqueJoursOk()).isTrue();
    }

    @Test
    void delaiRecoursHierarchiqueKo_apres60Jours() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_KO, List.of(), null, false, true);
        assertThat(r.delaiRecoursHierarchiqueJoursOk()).isFalse();
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Délai recours hiérarchique dépassé"));
    }

    @Test
    void delaiRecoursHierarchiqueOk_siDateRecoursNull() {
        // Pas de proposition → considéré OK (pas de violation constatée)
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, null, List.of(), null, false, true);
        assertThat(r.delaiRecoursHierarchiqueJoursOk()).isTrue();
    }

    @Test
    void delaiContentieuxTa_reflechiInputDelaiContestationRespecte() {
        ContestationAreResult vrai = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK, List.of(), null, false, true);
        ContestationAreResult faux = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK, List.of(), null, false, false);
        assertThat(vrai.delaiRecoursContentieuxTaJoursOk()).isTrue();
        assertThat(faux.delaiRecoursContentieuxTaJoursOk()).isFalse();
    }

    // =========================================================================
    // Expertise salaire
    // =========================================================================

    @Test
    void expertiseSalaire_recommandeeSiMotifErreurCalcul() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "MONTANT_INDEMNITE", "ERREUR_CALCUL_REMUNERATION_REFERENCE",
                D_NOTIF, D_RECOURS_OK, List.of("BULLETIN_PAIE"),
                null, false, true);
        assertThat(r.expertiseTraitementSalaireRecommandee()).isTrue();
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("salaire journalier de référence"));
    }

    @Test
    void expertiseSalaire_pasRecommandee_siAutreMotif() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "RADIATION", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK, List.of("BULLETIN_PAIE"),
                null, false, true);
        assertThat(r.expertiseTraitementSalaireRecommandee()).isFalse();
    }

    // =========================================================================
    // Validation & erreurs
    // =========================================================================

    @Test
    void typeInvalide_throws() {
        assertThatThrownBy(() -> ContestationAreCalculator.compute(
                "INVALIDE", "REFUS_INJUSTIFIE",
                D_NOTIF, null, List.of(), null, false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Type décision contestée inconnu");
    }

    @Test
    void typeNull_throws() {
        assertThatThrownBy(() -> ContestationAreCalculator.compute(
                null, "REFUS_INJUSTIFIE",
                D_NOTIF, null, List.of(), null, false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeDecisionContestee");
    }

    @Test
    void motifInvalide_throws() {
        assertThatThrownBy(() -> ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "INVALIDE",
                D_NOTIF, null, List.of(), null, false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motif contestation inconnu");
    }

    @Test
    void motifNull_throws() {
        assertThatThrownBy(() -> ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", null,
                D_NOTIF, null, List.of(), null, false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifContestation");
    }

    @Test
    void preuveInconnue_throws() {
        assertThatThrownBy(() -> ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, null, List.of("VIDEO"), null, false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Preuve inconnue");
    }

    @Test
    void dateNotificationNull_throws() {
        assertThatThrownBy(() -> ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                null, null, List.of(), null, false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationDecision");
    }

    @Test
    void montantNegatif_throws() {
        assertThatThrownBy(() -> ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK, List.of(),
                new BigDecimal("-1.00"), false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantContesteEur");
    }

    @Test
    void preuvesDupliquees_sontDeduplique() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK,
                List.of("BULLETIN_PAIE", "BULLETIN_PAIE", "ATTESTATION_EMPLOYEUR"),
                null, false, true);
        assertThat(r.preuvesProduites()).hasSize(2)
                .containsExactly("BULLETIN_PAIE", "ATTESTATION_EMPLOYEUR");
    }

    @Test
    void preuvesBlank_estIgnoree() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK,
                List.of("BULLETIN_PAIE", "", "  "),
                null, false, true);
        assertThat(r.preuvesProduites()).hasSize(1).containsExactly("BULLETIN_PAIE");
    }

    // =========================================================================
    // Métadonnées
    // =========================================================================

    @Test
    void baseJuridique_contientArticles() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK, List.of(), null, false, true);
        assertThat(r.baseJuridique())
                .contains("L.5422-1")
                .contains("R.5422-1")
                .contains("France Travail")
                .contains("18/12/2023");
    }

    @Test
    void formule_contientScoreEtVerdict() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "ERREUR_CALCUL_REMUNERATION_REFERENCE",
                D_NOTIF, D_RECOURS_OK,
                List.of("BULLETIN_PAIE", "ATTESTATION_EMPLOYEUR"),
                MONTANT_SIGNIF, false, true);
        assertThat(r.formule())
                .contains("Score").contains("100")
                .contains("RECOURS_HIERARCHIQUE_PRIORITAIRE");
    }

    @Test
    void delaiInstruction_estTujours4Mois() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK, List.of(), null, false, true);
        assertThat(r.delaiInstructionMoisPrevisionnel()).isEqualTo(4);
    }

    @Test
    void messages_contientRecoursPrealable() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK, List.of(), null, false, true);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("R.421-1 CJA"));
    }

    @Test
    void messages_alerteSiTribunalDejaSaisi() {
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK, List.of(), null, true, true);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Tribunal déjà saisi"));
    }

    @Test
    void enumerations_couvrentToutesValeurs() {
        // Vérifie qu'on accepte chaque type décision et chaque motif sans exception
        for (ContestationAreTypeDecision t : ContestationAreTypeDecision.values()) {
            for (ContestationAreMotif m : ContestationAreMotif.values()) {
                ContestationAreResult r = ContestationAreCalculator.compute(
                        t.name(), m.name(),
                        D_NOTIF, D_RECOURS_OK, List.of(), null, false, true);
                assertThat(r).isNotNull();
                assertThat(r.typeDecisionContestee()).isEqualTo(t.name());
                assertThat(r.motifContestation()).isEqualTo(m.name());
            }
        }
    }

    @Test
    void preuvesEnums_septIncludingAutreReconnues() {
        List<String> all = List.of(
                "BULLETIN_PAIE", "ATTESTATION_EMPLOYEUR", "CERTIFICAT_TRAVAIL",
                "LETTRE_LICENCIEMENT", "JUGEMENT_PRUDHOMAL", "AUTRE");
        ContestationAreResult r = ContestationAreCalculator.compute(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                D_NOTIF, D_RECOURS_OK, all, null, false, true);
        assertThat(r.preuvesProduites()).containsExactlyElementsOf(all);
    }
}
