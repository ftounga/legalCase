package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-FA-19-01 : tests unitaires de AutoriteParentaleCalculator.
 *
 * <p>Couvre les 5 contributions du score (motif, preuves, danger, ingérence,
 * consentement), les seuils de verdict, les recommandations expertise et
 * audition (art. 388-1), et les erreurs de validation.
 */
class AutoriteParentaleCalculatorTest {

    private static final LocalDate REQUETE = LocalDate.of(2026, 5, 10);

    @Test
    void compute_nominal_eleveeScore75() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "MISE_EN_DANGER",
                true,
                List.of("JUGEMENT_PENAL", "TEMOIGNAGES_ECOLE"),
                List.of(8, 12),
                false,
                false,
                REQUETE);

        // 25 motif + 25 preuves + 25 danger = 75
        assertThat(r.scoreEligibilite()).isEqualTo(75);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.expertiseRecommandee()).isTrue();
        assertThat(r.auditionEnfantsRecommandee()).isFalse(); // 8 < 13
        assertThat(r.baseJuridique()).contains("372").contains("373");
    }

    @Test
    void compute_motifAutre_pasPreuves_pasDanger_score0_faible() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "AUTRE",
                false,
                List.of(),
                List.of(),
                false,
                false,
                REQUETE);
        assertThat(r.scoreEligibilite()).isEqualTo(0);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.expertiseRecommandee()).isFalse();
    }

    @Test
    void compute_tousLesCriteres_score100_plafonne() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_PERE",
                "ALIENATION_PARENTALE",
                true,
                List.of("JUGEMENT_PENAL", "EXPERTISE_PSYCHIATRIQUE",
                        "TEMOIGNAGES_PROCHES"),
                List.of(14, 15),
                true,
                true,
                REQUETE);
        // 25 + 25 + 25 + 15 + 10 = 100
        assertThat(r.scoreEligibilite()).isEqualTo(100);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.expertiseRecommandee()).isTrue();
        assertThat(r.auditionEnfantsRecommandee()).isTrue();
    }

    @Test
    void compute_motifReconnu_unePreuve_score25_faible() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "DESINTERET_PROLONGE",
                false,
                List.of("MAINS_COURANTES"),
                List.of(10),
                false,
                false,
                REQUETE);
        assertThat(r.scoreEligibilite()).isEqualTo(25);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.expertiseRecommandee()).isFalse();
    }

    @Test
    void compute_seuilMoyenne_40() {
        // motif reconnu + 2 preuves = 50 → MOYENNE
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "DELEGATION_TIERS",
                "IMPOSSIBILITE_FAIT",
                false,
                List.of("RAPPORT_AED", "CERTIFICAT_MEDICAL"),
                List.of(),
                false,
                false,
                REQUETE);
        assertThat(r.scoreEligibilite()).isEqualTo(50);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("377"));
    }

    @Test
    void compute_expertiseRecommandee_miseEnDanger() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "MISE_EN_DANGER",
                false, List.of(), List.of(),
                false, false, REQUETE);
        assertThat(r.expertiseRecommandee()).isTrue();
    }

    @Test
    void compute_expertiseRecommandee_alienationParentale() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_PERE",
                "ALIENATION_PARENTALE",
                false, List.of(), List.of(),
                false, false, REQUETE);
        assertThat(r.expertiseRecommandee()).isTrue();
    }

    @Test
    void compute_expertiseNonRecommandee_motifNonSensible() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "DESINTERET_PROLONGE",
                false, List.of(), List.of(),
                false, false, REQUETE);
        assertThat(r.expertiseRecommandee()).isFalse();
    }

    @Test
    void compute_auditionRecommandee_tousEnfants13Plus() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "DESINTERET_PROLONGE",
                false, List.of(), List.of(13, 16),
                false, false, REQUETE);
        assertThat(r.auditionEnfantsRecommandee()).isTrue();
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("388-1"));
    }

    @Test
    void compute_auditionNonRecommandee_unEnfantSous13() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "DESINTERET_PROLONGE",
                false, List.of(), List.of(12, 14),
                false, false, REQUETE);
        assertThat(r.auditionEnfantsRecommandee()).isFalse();
    }

    @Test
    void compute_auditionNonRecommandee_aucunEnfant() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "DESINTERET_PROLONGE",
                false, List.of(), List.of(),
                false, false, REQUETE);
        assertThat(r.auditionEnfantsRecommandee()).isFalse();
    }

    @Test
    void compute_consentementAutreParent_ajoute10() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "AUTRE",
                false, List.of(), List.of(),
                true, // consentement
                false, REQUETE);
        // motif AUTRE → 0 ; consentement → +10
        assertThat(r.scoreEligibilite()).isEqualTo(10);
    }

    @Test
    void compute_interferenceVieEnfant_ajoute15() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "AUTRE",
                false, List.of(), List.of(),
                false,
                true, // interference
                REQUETE);
        assertThat(r.scoreEligibilite()).isEqualTo(15);
    }

    @Test
    void compute_dangerCaracterise_ajoute25() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "AUTRE",
                true,
                List.of(), List.of(),
                false, false, REQUETE);
        assertThat(r.scoreEligibilite()).isEqualTo(25);
    }

    @Test
    void compute_formuleContientScoreEtCriteres() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "MISE_EN_DANGER",
                true,
                List.of("JUGEMENT_PENAL", "TEMOIGNAGES_ECOLE"),
                List.of(8),
                false, false, REQUETE);
        assertThat(r.formule()).contains("Score 75").contains("MISE_EN_DANGER")
                .contains("danger");
    }

    @Test
    void compute_messagesContiennentBaseJuridique() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "MISE_EN_DANGER",
                true,
                List.of("JUGEMENT_PENAL", "TEMOIGNAGES_ECOLE"),
                List.of(8),
                false, false, REQUETE);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("372"));
    }

    @Test
    void compute_regimesIdentiques_throws() {
        assertThatThrownBy(() -> AutoriteParentaleCalculator.compute(
                "CONJOINT", "CONJOINT",
                "MISE_EN_DANGER",
                false, List.of(), List.of(),
                false, false, REQUETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("différer");
    }

    @Test
    void compute_regimeActuelInvalide_throws() {
        assertThatThrownBy(() -> AutoriteParentaleCalculator.compute(
                "INVALIDE", "EXCLUSIF_MERE",
                "AUTRE",
                false, List.of(), List.of(),
                false, false, REQUETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regimeExerciceActuel");
    }

    @Test
    void compute_regimeDemandeInvalide_throws() {
        assertThatThrownBy(() -> AutoriteParentaleCalculator.compute(
                "CONJOINT", "INVALIDE",
                "AUTRE",
                false, List.of(), List.of(),
                false, false, REQUETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regimeExerciceDemande");
    }

    @Test
    void compute_motifInvalide_throws() {
        assertThatThrownBy(() -> AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "INVALIDE",
                false, List.of(), List.of(),
                false, false, REQUETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifChangement");
    }

    @Test
    void compute_preuveInvalide_throws() {
        assertThatThrownBy(() -> AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "AUTRE",
                false, List.of("INVALIDE"), List.of(),
                false, false, REQUETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preuvesProduites");
    }

    @Test
    void compute_ageEnfantInvalide_throws() {
        assertThatThrownBy(() -> AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "AUTRE",
                false, List.of(), List.of(18),
                false, false, REQUETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageEnfants");
    }

    @Test
    void compute_dateRequeteNull_throws() {
        assertThatThrownBy(() -> AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "AUTRE",
                false, List.of(), List.of(),
                false, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRequete");
    }

    @Test
    void compute_preuvesDoublons_dedupliquees() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "AUTRE",
                false,
                List.of("JUGEMENT_PENAL", "JUGEMENT_PENAL", "MAINS_COURANTES"),
                List.of(),
                false, false, REQUETE);
        // 2 preuves uniques → +25 ; AUTRE → 0 motif
        assertThat(r.preuvesProduites()).containsExactly("JUGEMENT_PENAL", "MAINS_COURANTES");
        assertThat(r.scoreEligibilite()).isEqualTo(25);
    }

    @Test
    void compute_messageDelegationTiers_present() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "DELEGATION_TIERS",
                "IMPOSSIBILITE_FAIT",
                false,
                List.of("RAPPORT_AED", "CERTIFICAT_MEDICAL"),
                List.of(),
                false, false, REQUETE);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("377"));
    }

    @Test
    void compute_dangerSansMotifMiseEnDanger_messageRequalification() {
        AutoriteParentaleResult r = AutoriteParentaleCalculator.compute(
                "CONJOINT", "EXCLUSIF_MERE",
                "DESINTERET_PROLONGE",
                true,
                List.of(), List.of(),
                false, false, REQUETE);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("375"));
    }
}
