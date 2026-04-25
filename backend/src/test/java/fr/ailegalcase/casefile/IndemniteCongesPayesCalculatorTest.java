package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-DT-26-01 : tests du calculateur Indemnite compensatrice de conges payes
 * (art. L.3141-26 et L.3141-28 Code du travail).
 */
class IndemniteCongesPayesCalculatorTest {

    private static final LocalDate DATE_RUPTURE = LocalDate.of(2026, 4, 30);

    @Test
    void methode_dix_pourcent_calcule_dix_pourcent_de_la_remuneration() {
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, 5,
                new BigDecimal("2500.00"), DATE_RUPTURE, null);

        assertThat(r.montantMethodeDixPourcentEur())
                .isEqualByComparingTo("3000.00");
    }

    @Test
    void methode_maintien_calcule_salaire_sur_30_fois_jours_dus() {
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, 5,
                new BigDecimal("2500.00"), DATE_RUPTURE, null);

        // (2500 / 30) * 20 = 1666.67
        assertThat(r.montantMethodeMaintienEur())
                .isEqualByComparingTo("1666.67");
    }

    @Test
    void joursDus_egale_acquis_moins_pris() {
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, 5,
                new BigDecimal("2500.00"), DATE_RUPTURE, null);

        assertThat(r.joursDus()).isEqualTo(20);
    }

    @Test
    void comparaison_auto_retient_dixPourcent_si_plus_eleve() {
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, 5,
                new BigDecimal("2500.00"), DATE_RUPTURE, null);

        // 3000 vs 1666.67 -> DIX_POURCENT
        assertThat(r.methodeRetenue()).isEqualTo(IndemniteCongesPayesMethode.DIX_POURCENT);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("3000.00");
    }

    @Test
    void comparaison_auto_retient_maintien_si_plus_eleve() {
        // Total faible, salaire mensuel eleve, beaucoup de jours dus
        // Total 6000 -> 10 % = 600. Maintien (8000/30) * 25 = 6666.67 -> MAINTIEN gagne
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("6000.00"), 25, 0,
                new BigDecimal("8000.00"), DATE_RUPTURE, null);

        assertThat(r.methodeRetenue()).isEqualTo(IndemniteCongesPayesMethode.MAINTIEN);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("6666.67");
    }

    @Test
    void comparaison_auto_egalite_retient_dixPourcent_par_defaut() {
        // Cas construit pour egalite parfaite :
        // 10 % * total = 1500
        // (salaire / 30) * 30 = salaire = 1500 -> maintien identique 1500
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("15000.00"), 30, 0,
                new BigDecimal("1500.00"), DATE_RUPTURE, null);

        assertThat(r.montantMethodeDixPourcentEur()).isEqualByComparingTo("1500.00");
        assertThat(r.montantMethodeMaintienEur()).isEqualByComparingTo("1500.00");
        assertThat(r.methodeRetenue()).isEqualTo(IndemniteCongesPayesMethode.DIX_POURCENT);
    }

    @Test
    void methodeForcee_dixPourcent_ignore_meilleur_maintien() {
        // Cas ou maintien serait plus favorable (6666.67 vs 600), mais l'avocat force DIX_POURCENT
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("6000.00"), 25, 0,
                new BigDecimal("8000.00"), DATE_RUPTURE,
                IndemniteCongesPayesMethode.DIX_POURCENT);

        assertThat(r.methodeRetenue()).isEqualTo(IndemniteCongesPayesMethode.DIX_POURCENT);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("600.00");
        assertThat(r.messages()).anyMatch(m -> m.contains("forcee"));
    }

    @Test
    void methodeForcee_maintien_ignore_meilleur_dixPourcent() {
        // Cas ou DIX_POURCENT serait plus favorable, mais l'avocat force MAINTIEN
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, 5,
                new BigDecimal("2500.00"), DATE_RUPTURE,
                IndemniteCongesPayesMethode.MAINTIEN);

        assertThat(r.methodeRetenue()).isEqualTo(IndemniteCongesPayesMethode.MAINTIEN);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("1666.67");
    }

    @Test
    void tous_jours_pris_donne_joursDus_zero_et_maintien_zero() {
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, 25,
                new BigDecimal("2500.00"), DATE_RUPTURE, null);

        assertThat(r.joursDus()).isZero();
        assertThat(r.montantMethodeMaintienEur()).isEqualByComparingTo("0.00");
        // 10 % reste positif -> DIX_POURCENT retenu
        assertThat(r.methodeRetenue()).isEqualTo(IndemniteCongesPayesMethode.DIX_POURCENT);
        assertThat(r.messages()).anyMatch(m -> m.contains("Aucun jour"));
    }

    @Test
    void totalRemuneration_zero_leve_exception() {
        assertThatThrownBy(() -> IndemniteCongesPayesCalculator.compute(
                BigDecimal.ZERO, 25, 5, new BigDecimal("2500.00"), DATE_RUPTURE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalRemunerationPeriodeEur");
    }

    @Test
    void salaireMensuel_zero_leve_exception() {
        assertThatThrownBy(() -> IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, 5, BigDecimal.ZERO, DATE_RUPTURE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireMensuelBrutEur");
    }

    @Test
    void joursAcquis_negatif_leve_exception() {
        assertThatThrownBy(() -> IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), -1, 5,
                new BigDecimal("2500.00"), DATE_RUPTURE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joursAcquisAnnee");
    }

    @Test
    void joursPris_negatif_leve_exception() {
        assertThatThrownBy(() -> IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, -1,
                new BigDecimal("2500.00"), DATE_RUPTURE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joursPris");
    }

    @Test
    void joursPris_superieur_acquis_leve_exception() {
        assertThatThrownBy(() -> IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, 26,
                new BigDecimal("2500.00"), DATE_RUPTURE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joursPris");
    }

    @Test
    void formule_contient_les_deux_montants_compares() {
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, 5,
                new BigDecimal("2500.00"), DATE_RUPTURE, null);

        assertThat(r.formule())
                .contains("10 %")
                .contains("maintien")
                .contains("3000")
                .contains("1666");
    }

    @Test
    void baseJuridique_referent_articles_legaux() {
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("30000.00"), 25, 5,
                new BigDecimal("2500.00"), DATE_RUPTURE, null);

        assertThat(r.baseJuridique())
                .isEqualTo("Art. L.3141-26 et L.3141-28 Code du travail");
    }

    @Test
    void arrondi_half_up_sur_montant_indemnite() {
        // total = 12345.67 -> 10 % = 1234.567 -> arrondi 1234.57
        IndemniteCongesPayesResult r = IndemniteCongesPayesCalculator.compute(
                new BigDecimal("12345.67"), 25, 5,
                new BigDecimal("2000.00"), DATE_RUPTURE,
                IndemniteCongesPayesMethode.DIX_POURCENT);

        assertThat(r.montantMethodeDixPourcentEur()).isEqualByComparingTo("1234.57");
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("1234.57");
    }
}
