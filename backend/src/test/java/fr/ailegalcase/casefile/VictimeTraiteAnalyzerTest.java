package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-21 : tests unitaires de {@link VictimeTraiteAnalyzer}.
 * Couvre les 4 verdicts (ELIGIBLE_PROBABLE, ELIGIBLE_SOUS_RESERVE_PLAINTE,
 * NON_ELIGIBLE, EN_COURS_IDENTIFICATION), l'alerte de mise en danger, les mesures
 * de protection et la validation des entrées.
 */
class VictimeTraiteAnalyzerTest {

    @Test
    void analyze_plainte_et_collaboration_donne_eligible_probable_avec_aps_et_travail() {
        VictimeTraiteResult r = VictimeTraiteAnalyzer.analyze(
                true, true, LocalDate.of(2026, 1, 10), "Récépissé en cours", false);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteAnalyzer.VERDICT_ELIGIBLE_PROBABLE);
        assertThat(r.chipsCriteresManquants()).isEmpty();
        assertThat(r.mesuresProtection())
                .anyMatch(m -> m.contains("APS de 6 mois"))
                .anyMatch(m -> m.contains("Droit au travail"));
        assertThat(r.baseJuridique()).contains("L. 425-1");
        assertThat(r.baseJuridique()).contains("Palerme");
        assertThat(r.risqueVictimeEnDanger()).isFalse();
        assertThat(r.datePlainte()).isEqualTo("2026-01-10");
    }

    @Test
    void analyze_collaboration_sans_plainte_donne_eligible_sous_reserve_avec_chip_plainte() {
        VictimeTraiteResult r = VictimeTraiteAnalyzer.analyze(
                false, true, null, null, false);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteAnalyzer.VERDICT_ELIGIBLE_SOUS_RESERVE_PLAINTE);
        assertThat(r.chipsCriteresManquants())
                .contains(VictimeTraiteAnalyzer.CHIP_PLAINTE_NON_DEPOSEE);
        assertThat(r.mesuresProtection()).isNotEmpty();
    }

    @Test
    void analyze_plainte_sans_collaboration_donne_en_cours_identification() {
        VictimeTraiteResult r = VictimeTraiteAnalyzer.analyze(
                true, false, null, null, false);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteAnalyzer.VERDICT_EN_COURS_IDENTIFICATION);
        assertThat(r.chipsCriteresManquants())
                .contains(VictimeTraiteAnalyzer.CHIP_COLLABORATION_OCRTEH_ABSENTE,
                        VictimeTraiteAnalyzer.CHIP_IDENTIFICATION_VICTIME_A_CONFIRMER);
    }

    @Test
    void analyze_ni_plainte_ni_collaboration_donne_non_eligible_sans_mesures() {
        VictimeTraiteResult r = VictimeTraiteAnalyzer.analyze(
                false, false, null, null, false);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteAnalyzer.VERDICT_NON_ELIGIBLE);
        assertThat(r.chipsCriteresManquants())
                .contains(VictimeTraiteAnalyzer.CHIP_PLAINTE_NON_DEPOSEE,
                        VictimeTraiteAnalyzer.CHIP_COLLABORATION_OCRTEH_ABSENTE);
        assertThat(r.mesuresProtection()).isEmpty();
    }

    @Test
    void analyze_presence_auteur_sans_plainte_signale_risque_victime_en_danger() {
        VictimeTraiteResult r = VictimeTraiteAnalyzer.analyze(
                false, false, null, null, true);

        assertThat(r.risqueVictimeEnDanger()).isTrue();
        assertThat(r.mesuresProtection())
                .anyMatch(m -> m.contains("Hébergement d'urgence"));
        assertThat(r.recommandations())
                .anyMatch(m -> m.contains("mise en sécurité"));
    }

    @Test
    void analyze_presence_auteur_avec_plainte_ne_signale_pas_danger() {
        VictimeTraiteResult r = VictimeTraiteAnalyzer.analyze(
                true, true, null, null, true);

        assertThat(r.risqueVictimeEnDanger()).isFalse();
        assertThat(r.verdict()).isEqualTo(VictimeTraiteAnalyzer.VERDICT_ELIGIBLE_PROBABLE);
    }

    @Test
    void analyze_titreActuel_trop_long_leve_illegalArgument() {
        String tooLong = "x".repeat(VictimeTraiteAnalyzer.TITRE_ACTUEL_MAX_LENGTH + 1);
        assertThatThrownBy(() -> VictimeTraiteAnalyzer.analyze(true, true, null, tooLong, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
