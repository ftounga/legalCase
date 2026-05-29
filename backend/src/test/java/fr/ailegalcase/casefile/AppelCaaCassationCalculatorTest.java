package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-33 : tests unitaires de {@link AppelCaaCassationCalculator}.
 * Couvre le délai d'appel de droit commun (1 mois) vs le délai spécial OQTF (15 j),
 * le délai de cassation CE (2 mois, info), le filtre d'admission des pourvois (OQTF),
 * les statuts APPEL_POSSIBLE / URGENT / PRESCRIT, et la validation des entrées —
 * avec une date du jour ({@code today}) injectée pour le déterminisme.
 */
class AppelCaaCassationCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 29);

    // ── Délai de droit commun (1 mois) ──────────────────────────────────

    @Test
    void compute_droitCommun_echeance1Mois_statutAppelPossible() {
        // Jugement récent → échéance dans ~1 mois → APPEL_POSSIBLE
        LocalDate jugement = LocalDate.of(2026, 5, 20);
        AppelCaaCassationResult r = AppelCaaCassationCalculator.compute(
                jugement, AppelCaaCassationTypeDecisionEnum.REJET,
                AppelCaaCassationTypeContentieuxEnum.REFUS_TITRE, false, TODAY);

        assertThat(r.dateEcheanceAppelCaa()).isEqualTo(LocalDate.of(2026, 6, 20));
        assertThat(r.statut()).isEqualTo(AppelCaaCassationStatut.APPEL_POSSIBLE);
        assertThat(r.joursRestantsAppel()).isGreaterThan(15);
        assertThat(r.delaiSpecialOQTF()).isFalse();
        assertThat(r.delaiCassationCeMois()).isEqualTo(2);
        assertThat(r.filtrePourvoisCassation()).isFalse();
        assertThat(r.motifsAppelPossibles()).hasSize(4);
        assertThat(r.baseJuridique()).contains("R. 811-2");
    }

    @Test
    void compute_delaiSpecialOQTFNull_traiteCommeDroitCommun1Mois() {
        // delaiSpecialOQTF null → Boolean.TRUE.equals(null) == false → 1 mois
        LocalDate jugement = LocalDate.of(2026, 5, 20);
        AppelCaaCassationResult r = AppelCaaCassationCalculator.compute(
                jugement, AppelCaaCassationTypeDecisionEnum.REJET,
                AppelCaaCassationTypeContentieuxEnum.AUTRE, null, TODAY);

        assertThat(r.dateEcheanceAppelCaa()).isEqualTo(LocalDate.of(2026, 6, 20));
        assertThat(r.delaiSpecialOQTF()).isFalse();
    }

    // ── Délai spécial OQTF (15 jours) ───────────────────────────────────

    @Test
    void compute_oqtfDelaiSpecial_echeance15Jours() {
        // Jugement aujourd'hui, OQTF sans délai → échéance = today + 15 j → URGENT (== 15)
        AppelCaaCassationResult r = AppelCaaCassationCalculator.compute(
                TODAY, AppelCaaCassationTypeDecisionEnum.REJET,
                AppelCaaCassationTypeContentieuxEnum.OQTF, true, TODAY);

        assertThat(r.dateEcheanceAppelCaa()).isEqualTo(TODAY.plusDays(15));
        assertThat(r.joursRestantsAppel()).isEqualTo(15L);
        assertThat(r.statut()).isEqualTo(AppelCaaCassationStatut.URGENT);
        assertThat(r.delaiSpecialOQTF()).isTrue();
    }

    // ── Filtre d'admission des pourvois en cassation (OQTF) ─────────────

    @Test
    void compute_oqtf_filtrePourvoisCassationTrue() {
        AppelCaaCassationResult r = AppelCaaCassationCalculator.compute(
                LocalDate.of(2026, 5, 25), AppelCaaCassationTypeDecisionEnum.REJET,
                AppelCaaCassationTypeContentieuxEnum.OQTF, false, TODAY);

        assertThat(r.filtrePourvoisCassation()).isTrue();
        assertThat(r.recommandation()).contains("L. 821-2");
    }

    @Test
    void compute_refusTitre_filtrePourvoisCassationFalse() {
        AppelCaaCassationResult r = AppelCaaCassationCalculator.compute(
                LocalDate.of(2026, 5, 25), AppelCaaCassationTypeDecisionEnum.ANNULATION,
                AppelCaaCassationTypeContentieuxEnum.REFUS_TITRE, false, TODAY);

        assertThat(r.filtrePourvoisCassation()).isFalse();
    }

    // ── Statut URGENT (≤ 15 j) ──────────────────────────────────────────

    @Test
    void compute_echeanceDans10Jours_statutUrgent() {
        // Droit commun : échéance = today + 10 j → jugement = (today + 10 j) - 1 mois
        LocalDate echeance = TODAY.plusDays(10);
        LocalDate jugement = echeance.minusMonths(1);
        AppelCaaCassationResult r = AppelCaaCassationCalculator.compute(
                jugement, AppelCaaCassationTypeDecisionEnum.REJET,
                AppelCaaCassationTypeContentieuxEnum.EXPULSION, false, TODAY);

        assertThat(r.joursRestantsAppel()).isEqualTo(10L);
        assertThat(r.statut()).isEqualTo(AppelCaaCassationStatut.URGENT);
    }

    // ── Statut PRESCRIT ─────────────────────────────────────────────────

    @Test
    void compute_delaiDepasse_statutPrescrit() {
        // Jugement il y a 3 mois → échéance d'appel (1 mois) largement dépassée
        LocalDate jugement = LocalDate.of(2026, 2, 1);
        AppelCaaCassationResult r = AppelCaaCassationCalculator.compute(
                jugement, AppelCaaCassationTypeDecisionEnum.REJET,
                AppelCaaCassationTypeContentieuxEnum.REFUS_TITRE, false, TODAY);

        assertThat(r.joursRestantsAppel()).isLessThanOrEqualTo(0);
        assertThat(r.statut()).isEqualTo(AppelCaaCassationStatut.PRESCRIT);
        assertThat(r.recommandation()).contains("expiré");
    }

    @Test
    void compute_echeanceExactementAujourdhui_statutPrescrit() {
        // Jugement = today - 1 mois → échéance == today → joursRestants == 0 → PRESCRIT
        LocalDate today = LocalDate.of(2026, 5, 28);
        LocalDate jugement = LocalDate.of(2026, 4, 28);
        AppelCaaCassationResult r = AppelCaaCassationCalculator.compute(
                jugement, AppelCaaCassationTypeDecisionEnum.REJET,
                AppelCaaCassationTypeContentieuxEnum.AUTRE, false, today);

        assertThat(r.dateEcheanceAppelCaa()).isEqualTo(today);
        assertThat(r.joursRestantsAppel()).isEqualTo(0L);
        assertThat(r.statut()).isEqualTo(AppelCaaCassationStatut.PRESCRIT);
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void compute_dateJugementFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AppelCaaCassationCalculator.compute(
                TODAY.plusDays(1), AppelCaaCassationTypeDecisionEnum.REJET,
                AppelCaaCassationTypeContentieuxEnum.OQTF, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_dateJugementNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AppelCaaCassationCalculator.compute(
                null, AppelCaaCassationTypeDecisionEnum.REJET,
                AppelCaaCassationTypeContentieuxEnum.OQTF, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateJugementTA");
    }

    @Test
    void compute_typeDecisionNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AppelCaaCassationCalculator.compute(
                LocalDate.of(2026, 5, 20), null,
                AppelCaaCassationTypeContentieuxEnum.OQTF, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeDecisionTA");
    }

    @Test
    void compute_typeContentieuxNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AppelCaaCassationCalculator.compute(
                LocalDate.of(2026, 5, 20), AppelCaaCassationTypeDecisionEnum.REJET,
                null, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeContentieux");
    }
}
