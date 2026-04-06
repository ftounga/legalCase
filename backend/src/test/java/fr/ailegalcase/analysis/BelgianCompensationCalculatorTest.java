package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BelgianCompensationCalculatorTest {

    @Test
    void preavis_0mois_1semaine() {
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(0)).isEqualTo(1);
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(2)).isEqualTo(1);
    }

    @Test
    void preavis_4mois_3semaines() {
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(4)).isEqualTo(3);
    }

    @Test
    void preavis_7mois_4semaines() {
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(7)).isEqualTo(4);
    }

    @Test
    void preavis_10mois_5semaines() {
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(10)).isEqualTo(5);
    }

    @Test
    void preavis_24mois_10semaines() {
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(24)).isEqualTo(10);
    }

    @Test
    void preavis_3ans_12semaines() {
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(36)).isEqualTo(12);
    }

    @Test
    void preavis_5ans_15semaines() {
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(60)).isEqualTo(15);
    }

    @Test
    void preavis_10ans_30semaines() {
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(120)).isEqualTo(30);
    }

    @Test
    void preavis_20ans_62semaines() {
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(240)).isEqualTo(62);
    }

    @Test
    void preavis_25ans_67semaines() {
        assertThat(BelgianCompensationCalculator.calculerPreavisSemaines(300)).isEqualTo(67);
    }

    @Test
    void calculate_indemnite_compensatoire() {
        // 5 ans, 4500€/mois → préavis 15 semaines
        // Salaire hebdo = 4500 * 12 / 52 = 1038.46€
        // Indemnité = 1038.46 * 15 = 15576.92€
        var result = BelgianCompensationCalculator.calculate(5, 0, 4500.0);
        assertThat(result).isPresent();
        var est = result.get();
        assertThat(est.preavisSemaines()).isEqualTo(15);
        assertThat(est.salaireHebdomadaire()).isEqualTo(1038.46);
        assertThat(est.indemniteCompensatoire()).isEqualTo(15576.92); // 1038.46 * 15 rounded
        assertThat(est.donneesPartielles()).isFalse();
    }

    @Test
    void calculate_fourchette_cct109() {
        var result = BelgianCompensationCalculator.calculate(5, 0, 4500.0);
        assertThat(result).isPresent();
        var est = result.get();
        assertThat(est.cct109MinSemaines()).isEqualTo(3);
        assertThat(est.cct109MaxSemaines()).isEqualTo(17);
        // CCT 109 min = 1038.46 * 3 = 3115.38
        assertThat(est.cct109MinEuros()).isEqualTo(3115.38);
        // CCT 109 max = 1038.46 * 17 = 17653.85
        assertThat(est.cct109MaxEuros()).isGreaterThan(17000);
    }

    @Test
    void calculate_donnees_partielles_sans_salaire() {
        var result = BelgianCompensationCalculator.calculate(5, 0, null);
        assertThat(result).isPresent();
        assertThat(result.get().donneesPartielles()).isTrue();
        assertThat(result.get().preavisSemaines()).isEqualTo(15);
        assertThat(result.get().indemniteCompensatoire()).isEqualTo(0);
    }

    @Test
    void calculate_aucune_donnee_retourne_vide() {
        var result = BelgianCompensationCalculator.calculate(null, null, null);
        assertThat(result).isEmpty();
    }
}
