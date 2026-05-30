package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-19 : tests unitaires de {@link CadreDirigeantStatutAnalyzer}.
 *
 * <p>Logique déterministe (art. L.3111-2 CT — 3 critères cumulatifs) :
 * <ul>
 *   <li>3 critères + participation effective → CADRE_DIRIGEANT (EXCLU, FAIBLE) ;</li>
 *   <li>3 critères sans participation → CADRE_DIRIGEANT_FRAGILE (EXCLU, MODERE) ;</li>
 *   <li>&lt; 3 critères → NON_CADRE_DIRIGEANT (NON_EXCLU, ELEVE) ;</li>
 *   <li>criteresRemplis compté sur 0..3.</li>
 * </ul>
 */
class CadreDirigeantStatutAnalyzerTest {

    @Test
    void troisCriteres_avecParticipation_cadreDirigeant_exclu_risqueFaible() {
        CadreDirigeantStatutResult r = CadreDirigeantStatutAnalyzer.analyze(
                true, true, true, true, new BigDecimal("9000.00"));

        assertThat(r.criteresRemplis()).isEqualTo(3);
        assertThat(r.qualification()).isEqualTo(CadreDirigeantQualification.CADRE_DIRIGEANT);
        assertThat(r.exclusionDureeTravail()).isEqualTo(CadreDirigeantExclusionDureeTravail.EXCLU);
        assertThat(r.risqueRappelHeuresSupp()).isEqualTo(CadreDirigeantRisqueRappelHeuresSupp.FAIBLE);
        assertThat(r.criteres()).hasSize(3).allMatch(CadreDirigeantCritere::rempli);
        assertThat(r.baseJuridique()).contains("L.3111-2");
    }

    @Test
    void troisCriteres_sansParticipation_cadreDirigeantFragile_exclu_risqueModere() {
        CadreDirigeantStatutResult r = CadreDirigeantStatutAnalyzer.analyze(
                true, true, true, false, null);

        assertThat(r.criteresRemplis()).isEqualTo(3);
        assertThat(r.qualification()).isEqualTo(CadreDirigeantQualification.CADRE_DIRIGEANT_FRAGILE);
        assertThat(r.exclusionDureeTravail()).isEqualTo(CadreDirigeantExclusionDureeTravail.EXCLU);
        assertThat(r.risqueRappelHeuresSupp()).isEqualTo(CadreDirigeantRisqueRappelHeuresSupp.MODERE);
        assertThat(r.participationDirectionEntreprise()).isFalse();
    }

    @Test
    void deuxCriteres_nonCadreDirigeant_nonExclu_risqueEleve() {
        // 2 critères remplis (indépendance + autonomie), rémunération non parmi les plus élevées.
        CadreDirigeantStatutResult r = CadreDirigeantStatutAnalyzer.analyze(
                true, true, false, true, null);

        assertThat(r.criteresRemplis()).isEqualTo(2);
        assertThat(r.qualification()).isEqualTo(CadreDirigeantQualification.NON_CADRE_DIRIGEANT);
        assertThat(r.exclusionDureeTravail()).isEqualTo(CadreDirigeantExclusionDureeTravail.NON_EXCLU);
        assertThat(r.risqueRappelHeuresSupp()).isEqualTo(CadreDirigeantRisqueRappelHeuresSupp.ELEVE);
    }

    @Test
    void zeroCritere_nonCadreDirigeant_risqueEleve() {
        CadreDirigeantStatutResult r = CadreDirigeantStatutAnalyzer.analyze(
                false, false, false, false, null);

        assertThat(r.criteresRemplis()).isZero();
        assertThat(r.qualification()).isEqualTo(CadreDirigeantQualification.NON_CADRE_DIRIGEANT);
        assertThat(r.exclusionDureeTravail()).isEqualTo(CadreDirigeantExclusionDureeTravail.NON_EXCLU);
        assertThat(r.risqueRappelHeuresSupp()).isEqualTo(CadreDirigeantRisqueRappelHeuresSupp.ELEVE);
        assertThat(r.criteres()).hasSize(3).noneMatch(CadreDirigeantCritere::rempli);
    }

    @Test
    void comptageCriteresRemplis_unCritere() {
        CadreDirigeantStatutResult r = CadreDirigeantStatutAnalyzer.analyze(
                false, false, true, false, null);

        assertThat(r.criteresRemplis()).isEqualTo(1);
        assertThat(r.criteres().get(2).rempli()).isTrue();
        assertThat(r.criteres().get(0).rempli()).isFalse();
        assertThat(r.qualification()).isEqualTo(CadreDirigeantQualification.NON_CADRE_DIRIGEANT);
    }

    @Test
    void participationNull_traiteeCommeFalse_fragileSiTroisCriteres() {
        // participationDirectionEntreprise null → false → fragile.
        CadreDirigeantStatutResult r = CadreDirigeantStatutAnalyzer.analyze(
                true, true, true, null, null);

        assertThat(r.participationDirectionEntreprise()).isFalse();
        assertThat(r.qualification()).isEqualTo(CadreDirigeantQualification.CADRE_DIRIGEANT_FRAGILE);
        assertThat(r.risqueRappelHeuresSupp()).isEqualTo(CadreDirigeantRisqueRappelHeuresSupp.MODERE);
    }

    @Test
    void critereRequisNull_leveIllegalArgument() {
        assertThatThrownBy(() -> CadreDirigeantStatutAnalyzer.analyze(
                null, true, true, true, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CadreDirigeantStatutAnalyzer.analyze(
                true, null, true, true, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CadreDirigeantStatutAnalyzer.analyze(
                true, true, null, true, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void remunerationNegative_leveIllegalArgument() {
        assertThatThrownBy(() -> CadreDirigeantStatutAnalyzer.analyze(
                true, true, true, true, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
