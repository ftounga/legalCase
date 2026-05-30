package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-13 : tests unitaires de {@link ParticulierEmployeurCesuAnalyzer}.
 *
 * <p>Valeurs déterministes calculées à partir des barèmes CCN :
 * <ul>
 *   <li>préavis par tranche d'ancienneté (1 semaine &lt; 6 mois ; 1 mois de 6
 *       mois à &lt; 2 ans ; 2 mois ≥ 2 ans) ;</li>
 *   <li>indemnité conventionnelle PE = 1/4 de mois par année (10 premières) ;</li>
 *   <li>indemnité de rupture assistant maternel = 1/80 du total des salaires ;</li>
 *   <li>seuil d'indemnité = 8 mois ; faute grave → indemnité non due.</li>
 * </ul>
 */
class ParticulierEmployeurCesuAnalyzerTest {

    private static final LocalDate ENTREE = LocalDate.of(2020, 1, 1);

    @Test
    void salariePe_3ans_motifPersonnel_preavis2mois_indemniteConventionnelle() {
        // 36 mois d'ancienneté → préavis 2 mois ; IL = 3 ans × 0,25 × 1500 = 1125.
        ParticulierEmployeurCesuResult r = ParticulierEmployeurCesuAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(3),
                ParticulierEmployeurCesuCategorie.SALARIE_PARTICULIER_EMPLOYEUR,
                ParticulierEmployeurCesuCause.LICENCIEMENT_MOTIF_PERSONNEL,
                new BigDecimal("1500.00"));

        assertThat(r.dureePreavisLibelle()).isEqualTo("2 mois");
        assertThat(r.dureePreavisJours()).isEqualTo(60);
        assertThat(r.eligibiliteIndemnite()).isEqualTo(ParticulierEmployeurCesuEligibilite.DUE);
        assertThat(r.methodeCalcul()).isEqualTo(ParticulierEmployeurCesuMethode.CONVENTIONNEL_PE);
        assertThat(r.indemniteLicenciement()).isEqualByComparingTo(new BigDecimal("1125.00"));
        assertThat(r.verdict()).isEqualTo(ParticulierEmployeurCesuVerdict.RUPTURE_A_SECURISER);
    }

    @Test
    void assmat_retraitEnfant_indemniteRupture1sur80() {
        // 12 mois d'ancienneté, salaire 800 → total estimé 9600, /80 = 120.
        ParticulierEmployeurCesuResult r = ParticulierEmployeurCesuAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(1),
                ParticulierEmployeurCesuCategorie.ASSISTANT_MATERNEL,
                ParticulierEmployeurCesuCause.RETRAIT_ENFANT,
                new BigDecimal("800.00"));

        assertThat(r.eligibiliteIndemnite()).isEqualTo(ParticulierEmployeurCesuEligibilite.DUE);
        assertThat(r.methodeCalcul()).isEqualTo(ParticulierEmployeurCesuMethode.INDEMNITE_RUPTURE_ASSMAT);
        assertThat(r.indemniteLicenciement()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(r.dureePreavisLibelle()).isEqualTo("1 mois");
    }

    @Test
    void fauteGrave_indemniteNonDue() {
        ParticulierEmployeurCesuResult r = ParticulierEmployeurCesuAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(5),
                ParticulierEmployeurCesuCategorie.SALARIE_PARTICULIER_EMPLOYEUR,
                ParticulierEmployeurCesuCause.FAUTE_GRAVE,
                new BigDecimal("1500.00"));

        assertThat(r.eligibiliteIndemnite()).isEqualTo(ParticulierEmployeurCesuEligibilite.NON_DUE);
        assertThat(r.motifNonDue()).containsIgnoringCase("faute grave");
        assertThat(r.methodeCalcul()).isEqualTo(ParticulierEmployeurCesuMethode.AUCUNE);
        assertThat(r.indemniteLicenciement()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.verdict()).isEqualTo(ParticulierEmployeurCesuVerdict.INDEMNITE_NON_DUE);
    }

    @Test
    void anciennete4mois_preavis1semaine_indemniteNonDueSeuil8mois() {
        ParticulierEmployeurCesuResult r = ParticulierEmployeurCesuAnalyzer.analyze(
                ENTREE, ENTREE.plusMonths(4),
                ParticulierEmployeurCesuCategorie.SALARIE_PARTICULIER_EMPLOYEUR,
                ParticulierEmployeurCesuCause.LICENCIEMENT_MOTIF_PERSONNEL,
                new BigDecimal("1500.00"));

        assertThat(r.dureePreavisLibelle()).isEqualTo("1 semaine");
        assertThat(r.dureePreavisJours()).isEqualTo(7);
        assertThat(r.eligibiliteIndemnite()).isEqualTo(ParticulierEmployeurCesuEligibilite.NON_DUE);
        assertThat(r.motifNonDue()).contains("8 mois");
        assertThat(r.verdict()).isEqualTo(ParticulierEmployeurCesuVerdict.INDEMNITE_NON_DUE);
    }

    @Test
    void anciennete1an_preavis1mois() {
        ParticulierEmployeurCesuResult r = ParticulierEmployeurCesuAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(1),
                ParticulierEmployeurCesuCategorie.SALARIE_PARTICULIER_EMPLOYEUR,
                ParticulierEmployeurCesuCause.LICENCIEMENT_MOTIF_PERSONNEL,
                new BigDecimal("1500.00"));

        assertThat(r.dureePreavisLibelle()).isEqualTo("1 mois");
        assertThat(r.dureePreavisJours()).isEqualTo(30);
        assertThat(r.eligibiliteIndemnite()).isEqualTo(ParticulierEmployeurCesuEligibilite.DUE);
    }

    @Test
    void indemnitePe_auDela10ans_coefficientUnTiers() {
        // 12 ans : 10 × 0,25 + 2 × (1/3) = 2,5 + 0,6667 = 3,1667 mois × 1200 = 3800,00.
        ParticulierEmployeurCesuResult r = ParticulierEmployeurCesuAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(12),
                ParticulierEmployeurCesuCategorie.SALARIE_PARTICULIER_EMPLOYEUR,
                ParticulierEmployeurCesuCause.LICENCIEMENT_MOTIF_PERSONNEL,
                new BigDecimal("1200.00"));

        assertThat(r.methodeCalcul()).isEqualTo(ParticulierEmployeurCesuMethode.CONVENTIONNEL_PE);
        assertThat(r.indemniteLicenciement()).isEqualByComparingTo(new BigDecimal("3800.00"));
    }

    @Test
    void retraitEnfant_avecSalariePe_leveException() {
        assertThatThrownBy(() -> ParticulierEmployeurCesuAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(2),
                ParticulierEmployeurCesuCategorie.SALARIE_PARTICULIER_EMPLOYEUR,
                ParticulierEmployeurCesuCause.RETRAIT_ENFANT,
                new BigDecimal("1500.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RETRAIT_ENFANT");
    }

    @Test
    void dateRuptureAvantDateEntree_leveException() {
        assertThatThrownBy(() -> ParticulierEmployeurCesuAnalyzer.analyze(
                ENTREE, ENTREE.minusDays(1),
                ParticulierEmployeurCesuCategorie.SALARIE_PARTICULIER_EMPLOYEUR,
                ParticulierEmployeurCesuCause.LICENCIEMENT_MOTIF_PERSONNEL,
                new BigDecimal("1500.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void salaireNegatif_leveException() {
        assertThatThrownBy(() -> ParticulierEmployeurCesuAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(2),
                ParticulierEmployeurCesuCategorie.SALARIE_PARTICULIER_EMPLOYEUR,
                ParticulierEmployeurCesuCause.LICENCIEMENT_MOTIF_PERSONNEL,
                new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
