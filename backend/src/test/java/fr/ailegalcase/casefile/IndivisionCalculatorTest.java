package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-FA-22-01 : tests unitaires de IndivisionCalculator.
 * Couvre les 5 contributions du score, les 4 verdicts, l'indemnité
 * d'occupation, les recommandations dérivées et la validation.
 */
class IndivisionCalculatorTest {

    private static final LocalDate DATE_INDIVISION = LocalDate.of(2023, 4, 15);

    // ------------------------------------------------------------------
    // Score & verdicts
    // ------------------------------------------------------------------

    @Test
    void compute_score_partageJudiciaireRecommande_seuil80() {
        // ≥2 tentatives (+30) + conflit (+25) + !consentement (+20) +
        // durée 3 ans (+15) + occupation (+10) = 100, mais on saute la durée
        // → 30+25+20+10 = 85 ; ici on garde tout = 100, on contrôle ≥80.
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER"),
                new BigDecimal("250000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("MEDIATION", "PROPOSITION_NOTAIRE"),
                false,
                true,
                3,
                false,
                true);
        assertThat(r.scoreEligibilitePartageJudiciaire()).isGreaterThanOrEqualTo(80);
        assertThat(r.verdictRecommandation()).isEqualTo("PARTAGE_JUDICIAIRE_RECOMMANDE");
        assertThat(r.baseJuridique()).contains("815").contains("1364");
    }

    @Test
    void compute_score_partageAmiable_consentement_score_inferieur_40() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("30000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                true,    // consentement global
                false,   // pas d'occupation
                1,       // durée 1 an
                false,
                false);
        assertThat(r.scoreEligibilitePartageJudiciaire()).isLessThan(40);
        assertThat(r.verdictRecommandation()).isEqualTo("PARTAGE_AMIABLE_POSSIBLE");
    }

    @Test
    void compute_score_licitationRequise_immobilier_seuilMoyen() {
        // 2 tentatives (+30) + occupation (+10) = 40 → seuil bas zone moyenne
        // Immobilier + ≥2 tentatives → LICITATION_REQUISE
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER"),
                new BigDecimal("180000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("MEDIATION", "EXPERTISE_VALORISATION"),
                true,    // consentement → ne contribue pas
                true,    // occupation +10
                1,
                false,
                false);
        assertThat(r.scoreEligibilitePartageJudiciaire()).isBetween(40, 69);
        assertThat(r.verdictRecommandation()).isEqualTo("LICITATION_REQUISE");
    }

    @Test
    void compute_score_mediationPrealable_pasImmobilier_seuilMoyen() {
        // !consentement (+20) + durée >2 (+15) + occupation (+10) = 45
        // Pas d'immobilier → MEDIATION_PREALABLE (pas LICITATION)
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER", "COMPTES_BANCAIRES"),
                new BigDecimal("50000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                false,
                true,
                3,
                false,
                false);
        assertThat(r.scoreEligibilitePartageJudiciaire()).isBetween(40, 69);
        assertThat(r.verdictRecommandation()).isEqualTo("MEDIATION_PREALABLE");
        assertThat(r.licitationRecommandee()).isFalse();
    }

    @Test
    void compute_score_mediationPrealable_scoreBas_sansConsentement() {
        // occupation +10 = 10 → score < 40, pas de consentement
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("20000"),
                2,
                List.of(new BigDecimal("60"), new BigDecimal("40")),
                List.of("AUCUNE"),
                false,
                true,
                1,
                false,
                false);
        assertThat(r.scoreEligibilitePartageJudiciaire()).isLessThan(40);
        // pas consentement → MEDIATION_PREALABLE (pas AMIABLE)
        assertThat(r.verdictRecommandation()).isEqualTo("MEDIATION_PREALABLE");
    }

    // ------------------------------------------------------------------
    // Indemnité d'occupation (art. 815-9 al. 2)
    // ------------------------------------------------------------------

    @Test
    void compute_indemniteOccupation_50_50_3ans_250k() {
        // valeur 250 000 × 0.04 / 12 × 36 mois × 0.5 quote-part lésée
        // = 250 000 × 0.0033333 × 36 × 0.5 = 15 000.00
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER"),
                new BigDecimal("250000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("MEDIATION"),
                false,
                true,
                3,
                false,
                true);
        assertThat(r.indemniteOccupationDueEur()).isEqualByComparingTo("15000.00");
    }

    @Test
    void compute_indemniteOccupation_zero_si_pasOccupation() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER"),
                new BigDecimal("250000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("MEDIATION"),
                false,
                false,   // pas d'occupation
                3,
                false,
                true);
        assertThat(r.indemniteOccupationDueEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_indemniteOccupation_quotePart_asymetrique() {
        // quote-part max = 70, lésée = 30 % = 0.30
        // 200 000 × 0.04/12 × 24 × 0.30 = 4 800.00
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER"),
                new BigDecimal("200000"),
                2,
                List.of(new BigDecimal("70"), new BigDecimal("30")),
                List.of("MEDIATION"),
                false,
                true,
                2,
                false,
                false);
        assertThat(r.indemniteOccupationDueEur()).isEqualByComparingTo("4800.00");
    }

    // ------------------------------------------------------------------
    // Recommandations dérivées
    // ------------------------------------------------------------------

    @Test
    void compute_expertiseRecommandee_si_3_indivisaires() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("50000"),
                3,
                List.of(new BigDecimal("33.33"),
                        new BigDecimal("33.33"),
                        new BigDecimal("33.34")),
                List.of("AUCUNE"),
                true,
                false,
                1,
                false,
                false);
        assertThat(r.expertiseNotarialeRecommandee()).isTrue();
    }

    @Test
    void compute_expertiseRecommandee_si_valeur_superieure_100k() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("150000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                true,
                false,
                1,
                false,
                false);
        assertThat(r.expertiseNotarialeRecommandee()).isTrue();
    }

    @Test
    void compute_expertiseNonRecommandee_2indivisaires_valeur_petite() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("50000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                true,
                false,
                1,
                false,
                false);
        assertThat(r.expertiseNotarialeRecommandee()).isFalse();
    }

    @Test
    void compute_licitationRecommandee_si_immobilier_et_verdict_judiciaire() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER", "MOBILIER"),
                new BigDecimal("250000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("MEDIATION", "PROPOSITION_NOTAIRE"),
                false, true, 3, false, true);
        assertThat(r.verdictRecommandation()).isEqualTo("PARTAGE_JUDICIAIRE_RECOMMANDE");
        assertThat(r.licitationRecommandee()).isTrue();
    }

    @Test
    void compute_licitationNonRecommandee_si_pasImmobilier() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER", "COMPTES_BANCAIRES"),
                new BigDecimal("80000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("MEDIATION", "PROPOSITION_NOTAIRE"),
                false, true, 3, false, true);
        assertThat(r.verdictRecommandation()).isEqualTo("PARTAGE_JUDICIAIRE_RECOMMANDE");
        assertThat(r.licitationRecommandee()).isFalse();
    }

    @Test
    void compute_delaiMois_12_si_2_indivisaires_pasImmobilier() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("30000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                true, false, 1, false, false);
        assertThat(r.delaiProcedurePartageJudiciaireMois()).isEqualTo(12);
    }

    @Test
    void compute_delaiMois_18_si_immobilier() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER"),
                new BigDecimal("180000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("MEDIATION"),
                false, true, 3, false, false); // pas de conflit → 18 mois
        assertThat(r.delaiProcedurePartageJudiciaireMois()).isEqualTo(18);
    }

    @Test
    void compute_delaiMois_24_si_4_indivisaires() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("80000"),
                4,
                List.of(new BigDecimal("25"), new BigDecimal("25"),
                        new BigDecimal("25"), new BigDecimal("25")),
                List.of("AUCUNE"),
                true, false, 1, false, false);
        assertThat(r.delaiProcedurePartageJudiciaireMois()).isEqualTo(24);
    }

    @Test
    void compute_delaiMois_24_si_conflit_immobilier() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER"),
                new BigDecimal("250000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("MEDIATION", "PROPOSITION_NOTAIRE"),
                false, true, 3, false, true);
        assertThat(r.delaiProcedurePartageJudiciaireMois()).isEqualTo(24);
    }

    @Test
    void compute_score_plafonne_a_100() {
        // toutes les contribs : 30+25+20+15+10 = 100
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER", "FONDS_COMMERCE", "TITRES_FINANCIERS"),
                new BigDecimal("500000"),
                3,
                List.of(new BigDecimal("33.33"), new BigDecimal("33.33"),
                        new BigDecimal("33.34")),
                List.of("MEDIATION", "PROPOSITION_NOTAIRE", "EXPERTISE_VALORISATION"),
                false, true, 5, true, true);
        assertThat(r.scoreEligibilitePartageJudiciaire()).isEqualTo(100);
    }

    @Test
    void compute_messages_inclut_815_et_1364() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER"),
                new BigDecimal("180000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("MEDIATION", "PROPOSITION_NOTAIRE"),
                false, true, 3, false, true);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("815"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("1364"));
    }

    @Test
    void compute_formule_inclut_score_et_verdict() {
        IndivisionResult r = IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("IMMOBILIER"),
                new BigDecimal("250000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("MEDIATION", "PROPOSITION_NOTAIRE"),
                false, true, 3, false, true);
        assertThat(r.formule()).contains("Score").contains("Verdict")
                .contains("PARTAGE_JUDICIAIRE_RECOMMANDE");
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    @Test
    void compute_dateOrigineNull_throws() {
        assertThatThrownBy(() -> IndivisionCalculator.compute(
                null,
                List.of("MOBILIER"),
                new BigDecimal("30000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                true, false, 1, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateOrigineIndivision");
    }

    @Test
    void compute_dateOrigineFuture_throws() {
        assertThatThrownBy(() -> IndivisionCalculator.compute(
                LocalDate.now().plusDays(30),
                List.of("MOBILIER"),
                new BigDecimal("30000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                true, false, 1, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_natureBiensVide_throws() {
        assertThatThrownBy(() -> IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of(),
                new BigDecimal("30000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                true, false, 1, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("natureBiens");
    }

    @Test
    void compute_natureBiensInvalide_throws() {
        assertThatThrownBy(() -> IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("XXXX"),
                new BigDecimal("30000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                true, false, 1, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("natureBiens");
    }

    @Test
    void compute_valeurNegative_throws() {
        assertThatThrownBy(() -> IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("-100"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                true, false, 1, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valeur");
    }

    @Test
    void compute_nbIndivisairesInvalide_throws() {
        assertThatThrownBy(() -> IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("30000"),
                1,
                List.of(new BigDecimal("100")),
                List.of("AUCUNE"),
                true, false, 1, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nbIndivisaires");
    }

    @Test
    void compute_quotesPart_taille_incorrecte_throws() {
        assertThatThrownBy(() -> IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("30000"),
                2,
                List.of(new BigDecimal("50")),       // taille 1 vs 2 indivisaires
                List.of("AUCUNE"),
                true, false, 1, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quotesPart");
    }

    @Test
    void compute_quotesPart_somme_non_100_throws() {
        assertThatThrownBy(() -> IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("30000"),
                2,
                List.of(new BigDecimal("60"), new BigDecimal("30")),
                List.of("AUCUNE"),
                true, false, 1, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100");
    }

    @Test
    void compute_tentatives_invalide_throws() {
        assertThatThrownBy(() -> IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("30000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("XXX"),
                true, false, 1, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tentativesPartageAmiable");
    }

    @Test
    void compute_dureeNegative_throws() {
        assertThatThrownBy(() -> IndivisionCalculator.compute(
                DATE_INDIVISION,
                List.of("MOBILIER"),
                new BigDecimal("30000"),
                2,
                List.of(new BigDecimal("50"), new BigDecimal("50")),
                List.of("AUCUNE"),
                true, false, -1, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("indivisionDureeAnnees");
    }
}
