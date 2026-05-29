package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-07 : tests unitaires de {@link VlsTsValidationCalculator}.
 * Couvre les 4 statuts (A_VALIDER, URGENT ≤ 15 j, EXPIRE, VALIDE), le calcul de
 * l'échéance à 3 mois (R. 311-3), le risque d'irrégularité, la procédure de
 * recours, et la validation des entrées — avec une date du jour ({@code today})
 * injectée pour le déterminisme.
 */
class VlsTsValidationCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 29);

    // ── Statut A_VALIDER ─────────────────────────────────────────────────

    @Test
    void compute_entreeRecente_statutAValider_avecEcheance3Mois() {
        // Entrée il y a ~1 mois → échéance dans ~2 mois → A_VALIDER
        LocalDate entree = LocalDate.of(2026, 4, 29);
        VlsTsValidationResult r = VlsTsValidationCalculator.compute(
                entree, VlsTsValidationTypeEnum.ETUDIANT, false, null, TODAY);

        assertThat(r.statut()).isEqualTo(VlsTsValidationStatut.A_VALIDER);
        assertThat(r.dateEcheanceValidation()).isEqualTo(LocalDate.of(2026, 7, 29));
        assertThat(r.joursRestantsValidation()).isNotNull().isGreaterThan(15);
        assertThat(r.risqueIrregularite()).isFalse();
        assertThat(r.procedureRecours()).isNull();
        assertThat(r.baseJuridique()).contains("R. 311-3");
    }

    // ── Statut URGENT (≤ 15 j) ──────────────────────────────────────────

    @Test
    void compute_echeanceDans10Jours_statutUrgent() {
        // Échéance = today + 10 j → entrée = today + 10 j - 3 mois
        LocalDate echeance = TODAY.plusDays(10);
        LocalDate entree = echeance.minusMonths(3);
        VlsTsValidationResult r = VlsTsValidationCalculator.compute(
                entree, VlsTsValidationTypeEnum.SALARIE, false, null, TODAY);

        assertThat(r.statut()).isEqualTo(VlsTsValidationStatut.URGENT);
        assertThat(r.joursRestantsValidation()).isEqualTo(10L);
        assertThat(r.risqueIrregularite()).isFalse();
    }

    @Test
    void compute_echeanceDans15Jours_resteUrgent_borneInclusive() {
        LocalDate echeance = TODAY.plusDays(15);
        LocalDate entree = echeance.minusMonths(3);
        VlsTsValidationResult r = VlsTsValidationCalculator.compute(
                entree, VlsTsValidationTypeEnum.VISITEUR, false, null, TODAY);

        assertThat(r.statut()).isEqualTo(VlsTsValidationStatut.URGENT);
        assertThat(r.joursRestantsValidation()).isEqualTo(15L);
    }

    // ── Statut EXPIRE ────────────────────────────────────────────────────

    @Test
    void compute_delaiDepasse_statutExpire_avecRisqueEtRecours() {
        // Entrée il y a ~5 mois → échéance dépassée
        LocalDate entree = LocalDate.of(2025, 12, 29);
        VlsTsValidationResult r = VlsTsValidationCalculator.compute(
                entree, VlsTsValidationTypeEnum.CONJOINT_FRANCAIS, false, null, TODAY);

        assertThat(r.statut()).isEqualTo(VlsTsValidationStatut.EXPIRE);
        assertThat(r.joursRestantsValidation()).isNotNull().isLessThanOrEqualTo(0);
        assertThat(r.risqueIrregularite()).isTrue();
        assertThat(r.procedureRecours()).isNotNull().contains("ANEF");
    }

    @Test
    void compute_echeanceExactementAujourdhui_statutExpire() {
        // Choix d'une date d'entrée dont l'échéance (+3 mois) tombe exactement
        // aujourd'hui pour valider la borne joursRestants == 0 → EXPIRE.
        // 2026-02-28 + 3 mois = 2026-05-28 ; on aligne TODAY de référence dessus.
        LocalDate today = LocalDate.of(2026, 5, 28);
        LocalDate entree = LocalDate.of(2026, 2, 28);
        VlsTsValidationResult r = VlsTsValidationCalculator.compute(
                entree, VlsTsValidationTypeEnum.AUTRE, false, null, today);

        assertThat(r.dateEcheanceValidation()).isEqualTo(today);
        assertThat(r.joursRestantsValidation()).isEqualTo(0L);
        assertThat(r.statut()).isEqualTo(VlsTsValidationStatut.EXPIRE);
        assertThat(r.risqueIrregularite()).isTrue();
    }

    // ── Statut VALIDE (prioritaire) ─────────────────────────────────────

    @Test
    void compute_validationEffectuee_statutValide_joursRestantsNull() {
        LocalDate entree = LocalDate.of(2026, 4, 29);
        VlsTsValidationResult r = VlsTsValidationCalculator.compute(
                entree, VlsTsValidationTypeEnum.ETUDIANT, true,
                LocalDate.of(2026, 5, 15), TODAY);

        assertThat(r.statut()).isEqualTo(VlsTsValidationStatut.VALIDE);
        assertThat(r.joursRestantsValidation()).isNull();
        assertThat(r.risqueIrregularite()).isFalse();
        assertThat(r.procedureRecours()).isNull();
        assertThat(r.dateValidationOFII()).isEqualTo(LocalDate.of(2026, 5, 15));
    }

    @Test
    void compute_validationEffectuee_prioritaireMemeSiDelaiDepasse() {
        // Délai dépassé MAIS validation effectuée → VALIDE prioritaire, pas EXPIRE
        LocalDate entree = LocalDate.of(2025, 12, 29);
        VlsTsValidationResult r = VlsTsValidationCalculator.compute(
                entree, VlsTsValidationTypeEnum.SALARIE, true,
                LocalDate.of(2026, 1, 10), TODAY);

        assertThat(r.statut()).isEqualTo(VlsTsValidationStatut.VALIDE);
        assertThat(r.risqueIrregularite()).isFalse();
        assertThat(r.procedureRecours()).isNull();
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void compute_dateEntreeFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> VlsTsValidationCalculator.compute(
                TODAY.plusDays(1), VlsTsValidationTypeEnum.ETUDIANT, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_dateEntreePlusDe24Mois_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> VlsTsValidationCalculator.compute(
                TODAY.minusMonths(25), VlsTsValidationTypeEnum.ETUDIANT, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("24 mois");
    }

    @Test
    void compute_dateValidationAvantEntree_lanceIllegalArgumentException() {
        LocalDate entree = LocalDate.of(2026, 4, 29);
        assertThatThrownBy(() -> VlsTsValidationCalculator.compute(
                entree, VlsTsValidationTypeEnum.ETUDIANT, true,
                LocalDate.of(2026, 4, 1), TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateValidationOFII");
    }

    @Test
    void compute_dateEntreeNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> VlsTsValidationCalculator.compute(
                null, VlsTsValidationTypeEnum.ETUDIANT, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateEntreeFrance");
    }

    @Test
    void compute_typeNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> VlsTsValidationCalculator.compute(
                LocalDate.of(2026, 4, 29), null, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeVlsTs");
    }
}
