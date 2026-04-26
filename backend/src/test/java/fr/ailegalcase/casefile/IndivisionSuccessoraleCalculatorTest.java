package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-FA-24-11 : tests unitaires de IndivisionSuccessoraleCalculator.
 * Couvre les 3 verdicts (HARMONIEUSE / CONFLICTUELLE / BLOCAGE), les
 * dispositifs recommandés, l'indemnité d'occupation art. 815-9 al. 2,
 * les frais de gestion et la validation des entrées.
 */
class IndivisionSuccessoraleCalculatorTest {

    private static final LocalDate DATE_DECES_24M = LocalDate.now().minusMonths(24);
    private static final LocalDate DATE_DECES_18M = LocalDate.now().minusMonths(18);

    // ------------------------------------------------------------------
    // Verdicts & dispositifs
    // ------------------------------------------------------------------

    @Test
    void compute_legale_harmonieuse_recommandeConvention() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                3,
                new BigDecimal("400000"),
                BigDecimal.ZERO,
                true,    // consentements
                false,   // pas d'occupation
                false,   // pas de contestation
                false);  // pas de demande partage
        assertThat(r.verdictGestion()).isEqualTo("HARMONIEUSE");
        assertThat(r.dispositifRecommande()).isEqualTo("CONVENTION_INDIVISION_5_ANS");
        assertThat(r.baseJuridique()).contains("815").contains("1873-1");
    }

    @Test
    void compute_legale_harmonieuse_demandePartage_recommandeAmiable() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_18M,
                "INDIVISION_LEGALE",
                2,
                new BigDecimal("200000"),
                BigDecimal.ZERO,
                true, false, false, true);
        assertThat(r.verdictGestion()).isEqualTo("HARMONIEUSE");
        assertThat(r.dispositifRecommande()).isEqualTo("PARTAGE_AMIABLE");
    }

    @Test
    void compute_occupationContestee_conflictuelle_indemniteDue() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                3,
                new BigDecimal("300000"),
                new BigDecimal("200000"),
                false,   // pas tous consentements
                true,    // occupation exclusive
                false, false);
        assertThat(r.verdictGestion()).isEqualTo("CONFLICTUELLE");
        assertThat(r.dispositifRecommande()).isEqualTo("MEDIATION_FAMILIALE");
        assertThat(r.indemniteOccupationDue()).isTrue();
        assertThat(r.indemniteOccupationDueEur().signum()).isPositive();
    }

    @Test
    void compute_actesContestes_conflictuelle_recommandeMediation() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                3,
                new BigDecimal("150000"),
                BigDecimal.ZERO,
                true, false, true, false);
        assertThat(r.verdictGestion()).isEqualTo("CONFLICTUELLE");
        assertThat(r.dispositifRecommande()).isEqualTo("MEDIATION_FAMILIALE");
    }

    @Test
    void compute_blocageTotal_recommandePartageJudiciaire() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                4,
                new BigDecimal("500000"),
                new BigDecimal("250000"),
                false,   // pas tous consentements
                true,    // occupation
                true,    // actes contestés
                true);   // demande partage
        assertThat(r.verdictGestion()).isEqualTo("BLOCAGE");
        assertThat(r.dispositifRecommande()).isEqualTo("PARTAGE_JUDICIAIRE");
        assertThat(r.scoreConflictualite()).isGreaterThanOrEqualTo(80);
    }

    @Test
    void compute_pasOccupation_indemniteZero() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                3,
                new BigDecimal("200000"),
                BigDecimal.ZERO,
                true, false, false, false);
        assertThat(r.indemniteOccupationDue()).isFalse();
        assertThat(r.indemniteOccupationDueEur())
                .isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void compute_indemniteOccupation_3heritiers_24mois_200000() {
        // valeur 200 000 × 0.04 / 12 × 24 × (1 - 1/3) = 200 000 × 0.04 × 2 × 2/3
        //  = 200 000 × 0.0533... = 10 666,67
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                3,
                new BigDecimal("200000"),
                new BigDecimal("200000"),
                false, true, false, false);
        assertThat(r.indemniteOccupationDueEur())
                .isEqualByComparingTo(new BigDecimal("10666.67"));
    }

    @Test
    void compute_conventionnelle_harmonieuse_maintientLegale() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_18M,
                "INDIVISION_CONVENTIONNELLE",
                3,
                new BigDecimal("250000"),
                BigDecimal.ZERO,
                true, false, false, false);
        assertThat(r.verdictGestion()).isEqualTo("HARMONIEUSE");
        assertThat(r.dispositifRecommande()).isEqualTo("MAINTIEN_INDIVISION_LEGALE");
    }

    @Test
    void compute_maintienForce_harmonieux_dispositifPreservation() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_18M,
                "MAINTIEN_FORCE",
                3,
                new BigDecimal("400000"),
                BigDecimal.ZERO,
                true, false, false, false);
        assertThat(r.dispositifRecommande()).isEqualTo("MAINTIEN_FORCE_PRESERVE");
    }

    @Test
    void compute_dureeIndivision_18mois() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_18M,
                "INDIVISION_LEGALE",
                3,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                true, false, false, false);
        assertThat(r.dureeIndivisionMois()).isBetween(17, 19);
    }

    @Test
    void compute_fraisGestion_100k_24mois_3600() {
        // fraisAnnuels = 100 000 × 0.01 + 800 = 1 800 €
        // fraisTotaux = 1 800 × 24 / 12 = 3 600 €
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                3,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                true, false, false, false);
        // Tolérance 1 mois car ChronoUnit.MONTHS peut donner 23 ou 24
        assertThat(r.fraisGestionEstimesEur())
                .isBetween(new BigDecimal("3450.00"), new BigDecimal("3750.00"));
    }

    @Test
    void compute_messages_contiennent_art815_et_hierarchie() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                3,
                new BigDecimal("200000"),
                BigDecimal.ZERO,
                true, false, false, false);
        assertThat(r.messages())
                .anyMatch(m -> m.contains("art. 815"));
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    @Test
    void compute_dateFuture_throws() {
        assertThatThrownBy(() -> IndivisionSuccessoraleCalculator.compute(
                LocalDate.now().plusDays(1),
                "INDIVISION_LEGALE",
                2,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                true, false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateOuvertureSuccession");
    }

    @Test
    void compute_dateNull_throws() {
        assertThatThrownBy(() -> IndivisionSuccessoraleCalculator.compute(
                null,
                "INDIVISION_LEGALE",
                2,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                true, false, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_typeIndivisionInvalide_throws() {
        assertThatThrownBy(() -> IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "FOO",
                2,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                true, false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeIndivision");
    }

    @Test
    void compute_nbHeritiersInferieur2_throws() {
        assertThatThrownBy(() -> IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                1,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                true, false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nbHeritiers");
    }

    @Test
    void compute_nbHeritiersSuperieur50_throws() {
        assertThatThrownBy(() -> IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                51,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                true, false, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_valeurPatrimoineNegative_throws() {
        assertThatThrownBy(() -> IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                2,
                new BigDecimal("-1"),
                BigDecimal.ZERO,
                true, false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valeurPatrimoineIndivisEur");
    }

    @Test
    void compute_valeurBienOccupeSuperieurePatrimoine_throws() {
        assertThatThrownBy(() -> IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                2,
                new BigDecimal("100000"),
                new BigDecimal("200000"),
                true, true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valeurBienOccupeEur");
    }

    @Test
    void compute_scoreConflictualite_borné_0_100() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                3,
                new BigDecimal("200000"),
                new BigDecimal("100000"),
                false, true, true, true);
        assertThat(r.scoreConflictualite()).isBetween(0, 100);
    }

    @Test
    void compute_formule_contientVerdictEtScore() {
        IndivisionSuccessoraleResult r = IndivisionSuccessoraleCalculator.compute(
                DATE_DECES_24M,
                "INDIVISION_LEGALE",
                3,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                true, false, false, false);
        assertThat(r.formule())
                .contains("HARMONIEUSE")
                .contains("Score conflictualité");
    }
}
