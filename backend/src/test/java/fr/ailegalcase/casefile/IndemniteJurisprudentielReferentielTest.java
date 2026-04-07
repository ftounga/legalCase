package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IndemniteJurisprudentielReferentielTest {

    @Test
    void baremeMacron_hasEntries0to29() {
        assertThat(IndemniteJurisprudentielReferentiel.getAllBaremeMacron()).hasSize(30);
    }

    @Test
    void baremeMacron_0ans_plafond1() {
        IndemniteBareme b = IndemniteJurisprudentielReferentiel.getBaremeMacron(0);
        assertThat(b.plancherMois()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(b.plafondMois()).isEqualByComparingTo(new BigDecimal("1"));
    }

    @Test
    void baremeMacron_10ans() {
        IndemniteBareme b = IndemniteJurisprudentielReferentiel.getBaremeMacron(10);
        assertThat(b.plancherMois()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(b.plafondMois()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    void baremeMacron_cappedAt29() {
        IndemniteBareme b = IndemniteJurisprudentielReferentiel.getBaremeMacron(50);
        assertThat(b.ancienneteAnnees()).isEqualTo(29);
        assertThat(b.plafondMois()).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    void cct109_minMax() {
        assertThat(IndemniteJurisprudentielReferentiel.getCct109MinSemaines())
                .isEqualByComparingTo(new BigDecimal("3"));
        assertThat(IndemniteJurisprudentielReferentiel.getCct109MaxSemaines())
                .isEqualByComparingTo(new BigDecimal("17"));
    }

    @Test
    void fourchetteFrance_returnsThreeValues_ordered() {
        BigDecimal[] f = IndemniteJurisprudentielReferentiel.getFourchetteFrance(10, 35);
        assertThat(f).hasSize(3);
        assertThat(f[0]).isLessThanOrEqualTo(f[1]);
        assertThat(f[1]).isLessThanOrEqualTo(f[2]);
    }

    @Test
    void fourchetteFrance_ageSenior_higherMediane() {
        BigDecimal[] young = IndemniteJurisprudentielReferentiel.getFourchetteFrance(10, 30);
        BigDecimal[] senior = IndemniteJurisprudentielReferentiel.getFourchetteFrance(10, 55);
        assertThat(senior[1]).isGreaterThanOrEqualTo(young[1]);
    }

    @Test
    void fourchetteBelgique_returnsThreeValues_ordered() {
        BigDecimal[] f = IndemniteJurisprudentielReferentiel.getFourchetteBelgique(5, 40);
        assertThat(f).hasSize(3);
        assertThat(f[0]).isLessThanOrEqualTo(f[1]);
        assertThat(f[1]).isLessThanOrEqualTo(f[2]);
    }

    @Test
    void fourchetteBelgique_highAnciennete_higherValues() {
        BigDecimal[] low = IndemniteJurisprudentielReferentiel.getFourchetteBelgique(2, 30);
        BigDecimal[] high = IndemniteJurisprudentielReferentiel.getFourchetteBelgique(15, 30);
        assertThat(high[1]).isGreaterThanOrEqualTo(low[1]);
    }
}
