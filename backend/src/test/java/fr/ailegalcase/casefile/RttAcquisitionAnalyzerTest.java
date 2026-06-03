package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * SF-218-49 : tests unitaires de {@link RttAcquisitionAnalyzer}
 * (F-DT-80, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.3121-41 à L.3121-44 CT — JRTT sans
 * majoration) :
 * <ul>
 *   <li>accord présent → statut CALCULE, nombreJrttTheorique =
 *       ((horaire − 35) × semaines) / (horaire / 5) ;</li>
 *   <li>pas d'accord → statut RENVOI_HEURES_SUP, pas de calcul (F-DT-19) ;</li>
 *   <li>horaire ≤ 35 ou &gt; 48 / requis null → IllegalArgument.</li>
 * </ul>
 */
class RttAcquisitionAnalyzerTest {

    @Test
    void accord37h_47semaines_calcule_environ12JrttSansMajoration() {
        // (37 − 35) × 47 = 94 h ; journée = 37 / 5 = 7,4 h ; 94 / 7,4 ≈ 12,7 JRTT
        RttAcquisitionResult r = RttAcquisitionAnalyzer.analyze(37d, true, 47);

        assertThat(r.statut()).isEqualTo(RttAcquisitionStatut.CALCULE);
        assertThat(r.nombreJrttTheorique()).isCloseTo(12.7, within(0.05));
        assertThat(r.accordCollectifPresent()).isTrue();
        assertThat(r.notes()).anyMatch(n -> n.contains("aucune majoration"));
        assertThat(r.base()).contains("sans majoration");
        assertThat(r.baseJuridique()).contains("L.3121-41");
    }

    @Test
    void accord39h_47semaines_calcule_environ24Jrtt() {
        // (39 − 35) × 47 = 188 h ; journée = 39 / 5 = 7,8 h ; 188 / 7,8 ≈ 24,1 JRTT
        RttAcquisitionResult r = RttAcquisitionAnalyzer.analyze(39d, true, 47);

        assertThat(r.statut()).isEqualTo(RttAcquisitionStatut.CALCULE);
        assertThat(r.nombreJrttTheorique()).isCloseTo(24.1, within(0.05));
    }

    @Test
    void semainesNull_defaut47Applique() {
        RttAcquisitionResult r = RttAcquisitionAnalyzer.analyze(37d, true, null);

        assertThat(r.statut()).isEqualTo(RttAcquisitionStatut.CALCULE);
        assertThat(r.semainesTravailleesAn()).isEqualTo(47);
        assertThat(r.nombreJrttTheorique()).isCloseTo(12.7, within(0.05));
    }

    @Test
    void moinsDeSemaines_moinsDeJrtt() {
        RttAcquisitionResult complet = RttAcquisitionAnalyzer.analyze(37d, true, 47);
        RttAcquisitionResult reduit = RttAcquisitionAnalyzer.analyze(37d, true, 40);

        assertThat(reduit.nombreJrttTheorique())
                .isLessThan(complet.nombreJrttTheorique());
    }

    @Test
    void pasDAccord_renvoiHeuresSup_sansCalcul() {
        RttAcquisitionResult r = RttAcquisitionAnalyzer.analyze(37d, false, 47);

        assertThat(r.statut()).isEqualTo(RttAcquisitionStatut.RENVOI_HEURES_SUP);
        assertThat(r.nombreJrttTheorique()).isNull();
        assertThat(r.notes()).anyMatch(n -> n.contains("F-DT-19"));
        assertThat(r.notes()).anyMatch(n -> n.contains("heures supplémentaires"));
    }

    @Test
    void horaire35_leveIllegalArgument() {
        assertThatThrownBy(() -> RttAcquisitionAnalyzer.analyze(35d, true, 47))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void horaireSuperieur48_leveIllegalArgument() {
        assertThatThrownBy(() -> RttAcquisitionAnalyzer.analyze(50d, true, 47))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void horaireNull_leveIllegalArgument() {
        assertThatThrownBy(() -> RttAcquisitionAnalyzer.analyze(null, true, 47))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accordNull_leveIllegalArgument() {
        assertThatThrownBy(() -> RttAcquisitionAnalyzer.analyze(37d, null, 47))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void semainesZeroOuNegatif_leveIllegalArgument() {
        assertThatThrownBy(() -> RttAcquisitionAnalyzer.analyze(37d, true, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RttAcquisitionAnalyzer.analyze(37d, true, -3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void horaire48_borneInclusive_calcule() {
        RttAcquisitionResult r = RttAcquisitionAnalyzer.analyze(48d, true, 47);

        assertThat(r.statut()).isEqualTo(RttAcquisitionStatut.CALCULE);
        assertThat(r.nombreJrttTheorique()).isNotNull();
    }
}
