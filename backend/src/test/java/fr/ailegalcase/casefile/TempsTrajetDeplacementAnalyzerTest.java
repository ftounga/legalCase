package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-51 : tests unitaires de {@link TempsTrajetDeplacementAnalyzer}
 * (F-DT-81, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.3121-4 CT ; CJUE C-266/14 « Tyco ») :
 * <ul>
 *   <li>itinérant → TEMPS_TRAVAIL, pas de contrepartie distincte ;</li>
 *   <li>domicile-travail / domicile-client avec dépassement → contrepartie due
 *       (sauf déjà prévue par accord) ;</li>
 *   <li>sans dépassement → TRAJET_SANS_CONTREPARTIE ;</li>
 *   <li>type null / minutes &lt; 0 / null → IllegalArgument.</li>
 * </ul>
 */
class TempsTrajetDeplacementAnalyzerTest {

    @Test
    void itinerant_qualifieTempsTravail_sansContrepartieDistincte() {
        TempsTrajetDeplacementResult r = TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.ITINERANT_SANS_LIEU_FIXE, 50, 20, false);

        assertThat(r.qualification()).isEqualTo(TempsTrajetQualification.TEMPS_TRAVAIL);
        assertThat(r.contrepartieDue()).isFalse();
        assertThat(r.notes()).anyMatch(n -> n.contains("itinérant"));
        assertThat(r.baseJuridique()).contains("C-266/14");
    }

    @Test
    void domicileTravail_avecDepassement_sansContrepartiePrevue_contrepartieDue() {
        // 90 > 30 → dépassement 60 min, contrepartie due
        TempsTrajetDeplacementResult r = TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.DOMICILE_TRAVAIL_HABITUEL, 90, 30, false);

        assertThat(r.qualification()).isEqualTo(TempsTrajetQualification.TRAJET_AVEC_CONTREPARTIE);
        assertThat(r.contrepartieDue()).isTrue();
        assertThat(r.depassementMinutes()).isEqualTo(60);
        assertThat(r.notes()).anyMatch(n -> n.contains("contrepartie"));
    }

    @Test
    void domicileTravail_sansDepassement_pasDeContrepartie() {
        // 30 == 30 → pas de dépassement
        TempsTrajetDeplacementResult r = TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.DOMICILE_TRAVAIL_HABITUEL, 30, 30, false);

        assertThat(r.qualification()).isEqualTo(TempsTrajetQualification.TRAJET_SANS_CONTREPARTIE);
        assertThat(r.contrepartieDue()).isFalse();
        assertThat(r.depassementMinutes()).isZero();
        assertThat(r.notes()).anyMatch(n -> n.contains("n'excède pas"));
    }

    @Test
    void domicileTravail_avecDepassement_maisContrepartieDejaPrevue_nonDue() {
        TempsTrajetDeplacementResult r = TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.DOMICILE_TRAVAIL_HABITUEL, 90, 30, true);

        assertThat(r.qualification()).isEqualTo(TempsTrajetQualification.TRAJET_AVEC_CONTREPARTIE);
        assertThat(r.contrepartieDue()).isFalse();
        assertThat(r.depassementMinutes()).isEqualTo(60);
        assertThat(r.notes()).anyMatch(n -> n.contains("déjà prévue"));
    }

    @Test
    void domicileClient_avecDepassement_contrepartieDue() {
        // 120 > 40 → dépassement 80 min
        TempsTrajetDeplacementResult r = TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.DOMICILE_CLIENT_DEPASSEMENT, 120, 40, false);

        assertThat(r.qualification()).isEqualTo(TempsTrajetQualification.TRAJET_AVEC_CONTREPARTIE);
        assertThat(r.contrepartieDue()).isTrue();
        assertThat(r.depassementMinutes()).isEqualTo(80);
    }

    @Test
    void depassementMinutes_jamaisNegatif() {
        // quotidien < normal → dépassement borné à 0
        TempsTrajetDeplacementResult r = TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.DOMICILE_TRAVAIL_HABITUEL, 10, 30, false);

        assertThat(r.depassementMinutes()).isZero();
        assertThat(r.qualification()).isEqualTo(TempsTrajetQualification.TRAJET_SANS_CONTREPARTIE);
    }

    @Test
    void typeTrajetNull_leveIllegalArgument() {
        assertThatThrownBy(() -> TempsTrajetDeplacementAnalyzer.analyze(null, 30, 30, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void quotidienNull_leveIllegalArgument() {
        assertThatThrownBy(() -> TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.DOMICILE_TRAVAIL_HABITUEL, null, 30, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalNull_leveIllegalArgument() {
        assertThatThrownBy(() -> TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.DOMICILE_TRAVAIL_HABITUEL, 30, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void minutesNegatives_leveIllegalArgument() {
        assertThatThrownBy(() -> TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.DOMICILE_TRAVAIL_HABITUEL, -5, 30, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.DOMICILE_TRAVAIL_HABITUEL, 30, -5, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void contrepartiePrevueNull_traiteCommeFalse() {
        TempsTrajetDeplacementResult r = TempsTrajetDeplacementAnalyzer.analyze(
                TypeTrajet.DOMICILE_TRAVAIL_HABITUEL, 90, 30, null);

        assertThat(r.contrepartiePrevueAccord()).isFalse();
        assertThat(r.contrepartieDue()).isTrue();
    }
}
