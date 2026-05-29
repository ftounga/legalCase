package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-17 : tests unitaires de {@link OfpraIntroductionCalculator}.
 * Couvre les 3 statuts de délai (A_DEPOSER, URGENT ≤ 21 j, EXPIRE), le calcul de
 * l'échéance à 90 jours (R. 521-1 et s.), le risque de procédure accélérée (pays
 * sûr L. 531-24), les étapes avec/sans ADA, les pièces requises et la validation
 * des entrées — avec une date du jour ({@code today}) injectée pour le déterminisme.
 */
class OfpraIntroductionCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 29);

    // ── Statut A_DEPOSER ─────────────────────────────────────────────────

    @Test
    void compute_arriveeRecente_statutADeposer_avecEcheance90Jours() {
        // Arrivée il y a 10 jours → échéance dans 80 j → A_DEPOSER
        LocalDate arrivee = TODAY.minusDays(10);
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                arrivee, false, null, false, null, TODAY);

        assertThat(r.statutDelai()).isEqualTo(OfpraIntroductionStatut.A_DEPOSER);
        assertThat(r.dateEcheanceIntroduction()).isEqualTo(arrivee.plusDays(90));
        assertThat(r.joursRestantsIntroduction()).isEqualTo(80L);
        assertThat(r.procedureAccelereeRisque()).isFalse();
        assertThat(r.baseJuridique()).contains("R. 521-1");
    }

    // ── Statut URGENT (≤ 21 j) ──────────────────────────────────────────

    @Test
    void compute_echeanceDans15Jours_statutUrgent() {
        // Échéance = today + 15 j → arrivée = today + 15 j - 90 j
        LocalDate echeance = TODAY.plusDays(15);
        LocalDate arrivee = echeance.minusDays(90);
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                arrivee, true, arrivee.plusDays(2), false, null, TODAY);

        assertThat(r.statutDelai()).isEqualTo(OfpraIntroductionStatut.URGENT);
        assertThat(r.joursRestantsIntroduction()).isEqualTo(15L);
    }

    @Test
    void compute_echeanceDans21Jours_resteUrgent_borneInclusive() {
        LocalDate echeance = TODAY.plusDays(21);
        LocalDate arrivee = echeance.minusDays(90);
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                arrivee, false, null, false, null, TODAY);

        assertThat(r.statutDelai()).isEqualTo(OfpraIntroductionStatut.URGENT);
        assertThat(r.joursRestantsIntroduction()).isEqualTo(21L);
    }

    // ── Statut EXPIRE ────────────────────────────────────────────────────

    @Test
    void compute_delaiDepasse_statutExpire() {
        // Arrivée il y a 120 jours → échéance dépassée de 30 j
        LocalDate arrivee = TODAY.minusDays(120);
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                arrivee, true, arrivee.plusDays(5), false, null, TODAY);

        assertThat(r.statutDelai()).isEqualTo(OfpraIntroductionStatut.EXPIRE);
        assertThat(r.joursRestantsIntroduction()).isLessThanOrEqualTo(0L);
        assertThat(r.recommandation()).contains("90 jours dépassé");
    }

    @Test
    void compute_echeanceExactementAujourdhui_statutExpire_borneZero() {
        LocalDate arrivee = TODAY.minusDays(90);
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                arrivee, false, null, false, null, TODAY);

        assertThat(r.dateEcheanceIntroduction()).isEqualTo(TODAY);
        assertThat(r.joursRestantsIntroduction()).isEqualTo(0L);
        assertThat(r.statutDelai()).isEqualTo(OfpraIntroductionStatut.EXPIRE);
    }

    // ── Procédure accélérée (pays sûr L. 531-24) ────────────────────────

    @Test
    void compute_paysSur_risqueProcedureAcceleree_true() {
        LocalDate arrivee = TODAY.minusDays(10);
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                arrivee, false, null, false, "Sénégal", TODAY);

        assertThat(r.procedureAccelereeRisque()).isTrue();
        assertThat(r.recommandation()).contains("pays sûrs");
    }

    @Test
    void compute_paysSurAvecAccents_normalise_risqueTrue() {
        // « Géorgie » doit matcher « GEORGIE » après normalisation des diacritiques
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                TODAY.minusDays(10), false, null, false, "  géorgie ", TODAY);

        assertThat(r.procedureAccelereeRisque()).isTrue();
    }

    @Test
    void compute_paysNonSur_risqueProcedureAcceleree_false() {
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                TODAY.minusDays(10), false, null, false, "Syrie", TODAY);

        assertThat(r.procedureAccelereeRisque()).isFalse();
    }

    @Test
    void compute_paysOrigineNull_risqueFalse() {
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                TODAY.minusDays(10), false, null, false, null, TODAY);

        assertThat(r.procedureAccelereeRisque()).isFalse();
    }

    // ── Étapes & pièces ──────────────────────────────────────────────────

    @Test
    void compute_avecAda_etapesIncluentAda_5Etapes() {
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                TODAY.minusDays(10), false, null, true, null, TODAY);

        assertThat(r.etapesAPrendre()).hasSize(5);
        assertThat(r.etapesAPrendre()).anyMatch(e -> e.contains("ADA"));
        assertThat(r.etapesAPrendre().get(4)).contains("Dépôt du dossier OFPRA");
    }

    @Test
    void compute_sansAda_etapesSansAda_4Etapes() {
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                TODAY.minusDays(10), false, null, false, null, TODAY);

        assertThat(r.etapesAPrendre()).hasSize(4);
        assertThat(r.etapesAPrendre()).noneMatch(e -> e.contains("ADA"));
    }

    @Test
    void compute_gudaEffectue_etapeGudaAnnotee() {
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                TODAY.minusDays(10), true, TODAY.minusDays(8), false, null, TODAY);

        assertThat(r.passageGudaEffectue()).isTrue();
        assertThat(r.etapesAPrendre().get(0)).contains("déjà effectué");
    }

    @Test
    void compute_piecesRequises_contiennentFormulaireEtRecit() {
        OfpraIntroductionResult r = OfpraIntroductionCalculator.compute(
                TODAY.minusDays(10), false, null, false, null, TODAY);

        assertThat(r.piecesRequises()).hasSize(4);
        assertThat(r.piecesRequises()).anyMatch(p -> p.contains("Formulaire"));
        assertThat(r.piecesRequises()).anyMatch(p -> p.toLowerCase().contains("persécution")
                || p.toLowerCase().contains("récit"));
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void compute_dateArriveeNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> OfpraIntroductionCalculator.compute(
                null, false, null, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateArriveeEnFrance");
    }

    @Test
    void compute_dateArriveeFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> OfpraIntroductionCalculator.compute(
                TODAY.plusDays(1), false, null, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_dateArriveePlusDe36Mois_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> OfpraIntroductionCalculator.compute(
                TODAY.minusMonths(37), false, null, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("36 mois");
    }

    @Test
    void compute_datePassageGudaAvantArrivee_lanceIllegalArgumentException() {
        LocalDate arrivee = TODAY.minusDays(10);
        assertThatThrownBy(() -> OfpraIntroductionCalculator.compute(
                arrivee, true, arrivee.minusDays(2), false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datePassageGuda");
    }
}
