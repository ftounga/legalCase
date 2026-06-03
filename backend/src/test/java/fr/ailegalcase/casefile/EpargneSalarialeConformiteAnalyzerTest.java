package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-41 : tests unitaires de {@link EpargneSalarialeConformiteAnalyzer}
 * (F-DT-53, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.3322-2 CT + loi n° 2023-1107 du 29/11/2023) :
 * <ul>
 *   <li>effectif ≥ 50 + participation → CONFORME ;</li>
 *   <li>effectif ≥ 50 sans participation → OBLIGATION_NON_REMPLIE ;</li>
 *   <li>11–49 rentable sans aucun dispositif → OBLIGATION_NON_REMPLIE ;</li>
 *   <li>11–49 rentable avec intéressement → CONFORME ;</li>
 *   <li>&lt; 11 ou 11–49 non rentable → NON_REQUIS ;</li>
 *   <li>champ requis null / effectif ≤ 0 → IllegalArgument.</li>
 * </ul>
 */
class EpargneSalarialeConformiteAnalyzerTest {

    @Test
    void effectif50_avecParticipation_conforme() {
        EpargneSalarialeConformiteResult r = EpargneSalarialeConformiteAnalyzer.analyze(
                80, true, false, false, false);

        assertThat(r.statut()).isEqualTo(EpargneSalarialeConformiteStatut.CONFORME);
        assertThat(r.obligationsApplicables()).isEqualTo(1);
        assertThat(r.obligationsNonRemplies()).isEqualTo(0);
        assertThat(r.baseJuridique()).contains("L.3322-2").contains("2023-1107");
        assertThat(r.checklist())
                .anyMatch(i -> "OBLIGATION".equals(i.type()) && i.conforme());
    }

    @Test
    void effectif50_sansParticipation_obligationNonRemplie() {
        EpargneSalarialeConformiteResult r = EpargneSalarialeConformiteAnalyzer.analyze(
                80, false, false, false, false);

        assertThat(r.statut()).isEqualTo(EpargneSalarialeConformiteStatut.OBLIGATION_NON_REMPLIE);
        assertThat(r.obligationsApplicables()).isEqualTo(1);
        assertThat(r.obligationsNonRemplies()).isEqualTo(1);
        assertThat(r.checklist())
                .anyMatch(i -> "OBLIGATION".equals(i.type()) && !i.conforme());
    }

    @Test
    void seuilExact50_sansParticipation_obligationNonRemplie() {
        EpargneSalarialeConformiteResult r = EpargneSalarialeConformiteAnalyzer.analyze(
                50, false, false, false, false);

        assertThat(r.statut()).isEqualTo(EpargneSalarialeConformiteStatut.OBLIGATION_NON_REMPLIE);
        assertThat(r.obligationsApplicables()).isEqualTo(1);
    }

    @Test
    void pme11a49_rentable_sansDispositif_obligationNonRemplie() {
        EpargneSalarialeConformiteResult r = EpargneSalarialeConformiteAnalyzer.analyze(
                30, false, false, true, true);

        assertThat(r.statut()).isEqualTo(EpargneSalarialeConformiteStatut.OBLIGATION_NON_REMPLIE);
        assertThat(r.obligationsApplicables()).isEqualTo(1);
        assertThat(r.obligationsNonRemplies()).isEqualTo(1);
    }

    @Test
    void pme11a49_rentable_avecInteressement_conforme() {
        EpargneSalarialeConformiteResult r = EpargneSalarialeConformiteAnalyzer.analyze(
                30, false, true, true, true);

        assertThat(r.statut()).isEqualTo(EpargneSalarialeConformiteStatut.CONFORME);
        assertThat(r.obligationsApplicables()).isEqualTo(1);
        assertThat(r.obligationsNonRemplies()).isEqualTo(0);
    }

    @Test
    void pme11a49_rentable_avecParticipation_conforme() {
        EpargneSalarialeConformiteResult r = EpargneSalarialeConformiteAnalyzer.analyze(
                30, true, false, true, true);

        assertThat(r.statut()).isEqualTo(EpargneSalarialeConformiteStatut.CONFORME);
        assertThat(r.obligationsNonRemplies()).isEqualTo(0);
    }

    @Test
    void effectifMoins11_nonRequis() {
        EpargneSalarialeConformiteResult r = EpargneSalarialeConformiteAnalyzer.analyze(
                8, false, false, false, false);

        assertThat(r.statut()).isEqualTo(EpargneSalarialeConformiteStatut.NON_REQUIS);
        assertThat(r.obligationsApplicables()).isEqualTo(0);
        assertThat(r.obligationsNonRemplies()).isEqualTo(0);
        assertThat(r.checklist())
                .anyMatch(i -> "INFORMATION".equals(i.type()));
    }

    @Test
    void pme11a49_nonRentable_nonRequis() {
        EpargneSalarialeConformiteResult r = EpargneSalarialeConformiteAnalyzer.analyze(
                30, false, false, false, true);

        assertThat(r.statut()).isEqualTo(EpargneSalarialeConformiteStatut.NON_REQUIS);
        assertThat(r.obligationsApplicables()).isEqualTo(0);
    }

    @Test
    void cumulObligations_50plusEt11a49Rentable_compteDeux() {
        // effectif ≥ 50 (participation oblig.) ET entreprise11a49=true rentable :
        // les deux obligations sont applicables ; participation absente, aucun
        // dispositif → 2 non remplies.
        EpargneSalarialeConformiteResult r = EpargneSalarialeConformiteAnalyzer.analyze(
                60, false, false, true, true);

        assertThat(r.obligationsApplicables()).isEqualTo(2);
        assertThat(r.obligationsNonRemplies()).isEqualTo(2);
        assertThat(r.statut()).isEqualTo(EpargneSalarialeConformiteStatut.OBLIGATION_NON_REMPLIE);
    }

    @Test
    void champRequisNull_effectifNul_throwIllegalArgument() {
        assertThatThrownBy(() -> EpargneSalarialeConformiteAnalyzer.analyze(
                null, false, false, false, false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EpargneSalarialeConformiteAnalyzer.analyze(
                0, false, false, false, false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EpargneSalarialeConformiteAnalyzer.analyze(
                50, null, false, false, false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EpargneSalarialeConformiteAnalyzer.analyze(
                50, false, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EpargneSalarialeConformiteAnalyzer.analyze(
                50, false, false, null, false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EpargneSalarialeConformiteAnalyzer.analyze(
                50, false, false, false, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
