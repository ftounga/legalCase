package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-211-02 : tests unitaires de {@link DivorceDdiBeCalculator}.
 */
class DivorceDdiBeCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_voie1_constatation_avecPreuves() {
        // preuves dispo → voie 1 ouverte, peu importe la durée de séparation
        DivorceDdiBeResult r = DivorceDdiBeCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "CONSTATATION_PAR_JUGE",
                true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.voie1Ouverte()).isTrue();
        assertThat(r.voieRecommandee()).isEqualTo("VOIE_1_CONSTATATION");
        assertThat(r.baseJuridique()).contains("229");
    }

    @Test
    void compute_voie2_ouverteApres6Mois() {
        // séparation 7 mois + commune → voie 2 ouverte
        DivorceDdiBeResult r = DivorceDdiBeCalculator.compute(
                LocalDate.of(2025, 10, 1),
                "COMMUNE_APRES_SEPARATION",
                false,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.joursSeparation()).isGreaterThanOrEqualTo(180);
        assertThat(r.voie2Ouverte()).isTrue();
        assertThat(r.voieRecommandee()).isEqualTo("VOIE_2_COMMUNE_6_MOIS");
    }

    @Test
    void compute_voie2_fermeeAvant6Mois() {
        // séparation 1 mois + commune → voie 2 fermée
        DivorceDdiBeResult r = DivorceDdiBeCalculator.compute(
                LocalDate.of(2026, 4, 11),
                "COMMUNE_APRES_SEPARATION",
                false,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.voie2Ouverte()).isFalse();
        assertThat(r.voieRecommandee()).isEqualTo("AUCUNE");
        assertThat(r.joursRestantsVoie2()).isGreaterThan(0);
    }

    @Test
    void compute_voie3_ouverteApres1An() {
        // séparation 13 mois + unilatérale → voie 3 ouverte
        DivorceDdiBeResult r = DivorceDdiBeCalculator.compute(
                LocalDate.of(2025, 4, 1),
                "UNILATERALE_APRES_SEPARATION",
                false,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.joursSeparation()).isGreaterThanOrEqualTo(365);
        assertThat(r.voie3Ouverte()).isTrue();
        assertThat(r.voieRecommandee()).isEqualTo("VOIE_3_UNILATERALE_1_AN");
    }

    @Test
    void compute_voie3_fermeeAvant1An() {
        // séparation 6 mois + unilatérale → voie 3 fermée
        DivorceDdiBeResult r = DivorceDdiBeCalculator.compute(
                LocalDate.of(2025, 11, 11),
                "UNILATERALE_APRES_SEPARATION",
                false,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.voie3Ouverte()).isFalse();
        assertThat(r.joursRestantsVoie3()).isGreaterThan(0);
        assertThat(r.voieRecommandee()).isEqualTo("AUCUNE");
    }

    @Test
    void compute_voie1_priorisee_quandPreuvesEtCommune() {
        // si preuves dispo, voie 1 prime sur voie 2
        DivorceDdiBeResult r = DivorceDdiBeCalculator.compute(
                LocalDate.of(2025, 10, 1),
                "COMMUNE_APRES_SEPARATION",
                true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.voie1Ouverte()).isTrue();
        assertThat(r.voieRecommandee()).isEqualTo("VOIE_1_CONSTATATION");
    }

    @Test
    void compute_joursRestants_correctsAvantSeuil() {
        // séparation 100 jours, commune → restant 80 j pour voie 2
        DivorceDdiBeResult r = DivorceDdiBeCalculator.compute(
                LocalDate.of(2026, 2, 1),
                "COMMUNE_APRES_SEPARATION",
                false,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.joursSeparation()).isEqualTo(99L);
        assertThat(r.joursRestantsVoie2()).isEqualTo(180L - 99L);
        assertThat(r.joursRestantsVoie3()).isEqualTo(365L - 99L);
    }

    @Test
    void compute_futureSeparation_throws() {
        assertThatThrownBy(() -> DivorceDdiBeCalculator.compute(
                LocalDate.of(2026, 6, 1),
                "COMMUNE_APRES_SEPARATION",
                false,
                clockAt(LocalDate.of(2026, 5, 11))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_unknownNature_throws() {
        assertThatThrownBy(() -> DivorceDdiBeCalculator.compute(
                LocalDate.of(2026, 1, 1),
                "NATURE_BIDON",
                false,
                clockAt(LocalDate.of(2026, 5, 11))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("natureDemande");
    }

    @Test
    void compute_nullSeparation_throws() {
        assertThatThrownBy(() -> DivorceDdiBeCalculator.compute(
                null,
                "COMMUNE_APRES_SEPARATION",
                false,
                clockAt(LocalDate.of(2026, 5, 11))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateSeparation");
    }

    @Test
    void compute_nullNature_throws() {
        assertThatThrownBy(() -> DivorceDdiBeCalculator.compute(
                LocalDate.of(2026, 1, 1),
                null,
                false,
                clockAt(LocalDate.of(2026, 5, 11))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("natureDemande");
    }
}
