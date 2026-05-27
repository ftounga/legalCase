package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-213-05 : tests unitaires du
 * {@link LicenciementBeProtectionGrossesseValidator}.
 *
 * <p>Vérifie la matrice de verdict (4 états), le calcul de
 * {@code dateFinProtection} selon les inputs disponibles, la fenêtre de
 * présomption des 10 semaines, le calcul de l'indemnité forfaitaire
 * 6 mois (art. 40 al. 3 Loi 16/03/1971), l'inversion de charge de la
 * preuve et la validation des inputs.</p>
 */
class LicenciementBeProtectionGrossesseValidatorTest {

    private static LicenciementBeProtectionGrossesseRequest req(
            LocalDate dateDebutGrossesse,
            LocalDate dateAccouchement,
            LocalDate dateCongeMaterniteDebut,
            LocalDate dateCongeMaterniteFinale,
            LocalDate dateLicenciement,
            boolean grossesseNotifieeParEcrit,
            BigDecimal remunerationMensuelleBrute,
            String motifInvoqueParEmployeur) {
        return new LicenciementBeProtectionGrossesseRequest(
                dateDebutGrossesse,
                dateAccouchement,
                dateCongeMaterniteDebut,
                dateCongeMaterniteFinale,
                dateLicenciement,
                grossesseNotifieeParEcrit,
                remunerationMensuelleBrute,
                motifInvoqueParEmployeur);
    }

    // ── Cas 1 : PROTECTION_APPLICABLE_NON_NOTIFIEE ─────────────────────────

    @Test
    void validate_licenciement_2j_apres_debut_grossesse_sans_notification_returns_NON_NOTIFIEE() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate licenciement = LocalDate.of(2026, 1, 17); // J+2

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeProtectionGrossesseVerdict.PROTECTION_APPLICABLE_NON_NOTIFIEE);
        assertThat(r.licenciementDansLaPeriodeProtegee()).isTrue();
        assertThat(r.indemniteForfaitaire()).isEqualByComparingTo("18000.00");
        assertThat(r.chargePreuveEmployeur()).isTrue();
    }

    // ── Cas 2 : PROTECTION_PRESUMEE (charge inversée renforcée) ────────────

    @Test
    void validate_licenciement_9sem_apres_debut_grossesse_avec_notification_returns_PRESUMEE() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate licenciement = debutGrossesse.plusWeeks(9); // dans fenêtre 10 sem

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                true, new BigDecimal("3000.00"), "Restructuration"));

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeProtectionGrossesseVerdict.PROTECTION_PRESUMEE);
        assertThat(r.licenciementDansLaPeriodeProtegee()).isTrue();
        assertThat(r.chargePreuveEmployeur()).isTrue();
        assertThat(r.indemniteForfaitaire()).isEqualByComparingTo("18000.00");
    }

    @Test
    void validate_licenciement_exactement_10sem_avec_notification_returns_PRESUMEE() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate licenciement = debutGrossesse.plusWeeks(10); // exactement 10 sem (borne incluse)

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                true, new BigDecimal("3000.00"), null));

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeProtectionGrossesseVerdict.PROTECTION_PRESUMEE);
    }

    @Test
    void validate_licenciement_11sem_avec_notification_returns_APPLICABLE() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate licenciement = debutGrossesse.plusWeeks(11); // hors fenêtre 10 sem

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                true, new BigDecimal("3000.00"), null));

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeProtectionGrossesseVerdict.PROTECTION_APPLICABLE);
        assertThat(r.chargePreuveEmployeur()).isTrue();
        assertThat(r.indemniteForfaitaire()).isEqualByComparingTo("18000.00");
    }

    // ── Cas 3 : HORS_PERIODE_PROTECTION ────────────────────────────────────

    @Test
    void validate_licenciement_1mois_pile_apres_fin_conge_maternite_returns_APPLICABLE() {
        // Fin congé maternité = 2026-02-04 → fin protection = 2026-03-04
        // Licenciement = 2026-03-04 (borne incluse).
        LocalDate debutGrossesse = LocalDate.of(2025, 4, 22);
        LocalDate finConge = LocalDate.of(2026, 2, 4);
        LocalDate licenciement = LocalDate.of(2026, 3, 4);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, null, null, finConge, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeProtectionGrossesseVerdict.PROTECTION_APPLICABLE_NON_NOTIFIEE);
        assertThat(r.licenciementDansLaPeriodeProtegee()).isTrue();
        assertThat(r.dateFinProtection()).isEqualTo(LocalDate.of(2026, 3, 4));
    }

    @Test
    void validate_licenciement_1mois_1jour_apres_fin_conge_maternite_returns_HORS_PERIODE() {
        // Fin congé maternité = 2026-02-04 → fin protection = 2026-03-04.
        // Licenciement = 2026-03-05 → 1 jour après fin protection → HORS.
        LocalDate debutGrossesse = LocalDate.of(2025, 4, 22);
        LocalDate finConge = LocalDate.of(2026, 2, 4);
        LocalDate licenciement = LocalDate.of(2026, 3, 5);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, null, null, finConge, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeProtectionGrossesseVerdict.HORS_PERIODE_PROTECTION);
        assertThat(r.licenciementDansLaPeriodeProtegee()).isFalse();
        assertThat(r.indemniteForfaitaire()).isNull();
        assertThat(r.chargePreuveEmployeur()).isFalse();
    }

    // ── Indemnité forfaitaire — calculs ────────────────────────────────────

    @Test
    void validate_indemnite_6mois_3000eur_egale_18000eur() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate licenciement = LocalDate.of(2026, 5, 10);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.indemniteForfaitaire()).isEqualByComparingTo("18000.00");
        assertThat(r.indemniteForfaitaire().scale()).isEqualTo(2);
    }

    @Test
    void validate_indemnite_arrondi_2_decimales_half_up() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate licenciement = LocalDate.of(2026, 5, 10);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                false, new BigDecimal("1234.567"), null));

        // 1234.567 × 6 = 7407.402 → HALF_UP 2 déc = 7407.40
        assertThat(r.indemniteForfaitaire()).isEqualByComparingTo("7407.40");
        assertThat(r.indemniteForfaitaire().scale()).isEqualTo(2);
    }

    // ── Calcul dateFinProtection ───────────────────────────────────────────

    @Test
    void validate_dateFinProtection_egale_finConge_plus_1_mois_si_finConge_renseignee() {
        LocalDate debutGrossesse = LocalDate.of(2025, 4, 22);
        LocalDate accouchement = LocalDate.of(2026, 1, 5);
        LocalDate finConge = LocalDate.of(2026, 4, 20);
        LocalDate licenciement = LocalDate.of(2026, 5, 1);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, finConge, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.dateFinProtection()).isEqualTo(LocalDate.of(2026, 5, 20));
    }

    @Test
    void validate_dateFinProtection_egale_accouchement_plus_3mois_si_finConge_absente() {
        LocalDate debutGrossesse = LocalDate.of(2025, 4, 22);
        LocalDate accouchement = LocalDate.of(2026, 1, 15);
        LocalDate licenciement = LocalDate.of(2026, 2, 1);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.dateFinProtection()).isEqualTo(LocalDate.of(2026, 4, 15));
    }

    @Test
    void validate_dateFinProtection_null_si_ni_accouchement_ni_finConge() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate licenciement = LocalDate.of(2026, 3, 1);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, null, null, null, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.dateFinProtection()).isNull();
        assertThat(r.avertissement()).contains("Date fin protection indéterminée");
        // Présomption ouverte côté droite : licenciement ≥ début grossesse → protégé.
        assertThat(r.licenciementDansLaPeriodeProtegee()).isTrue();
        assertThat(r.verdict())
                .isEqualTo(LicenciementBeProtectionGrossesseVerdict.PROTECTION_APPLICABLE_NON_NOTIFIEE);
    }

    @Test
    void validate_avertissement_approximation_si_accouchement_seul() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate licenciement = LocalDate.of(2026, 3, 1);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.avertissement())
                .contains("approximation")
                .contains("dateCongeMaterniteFinale");
    }

    // ── Validation des inputs ──────────────────────────────────────────────

    @Test
    void validate_requestNull_throws() {
        assertThatThrownBy(() -> LicenciementBeProtectionGrossesseValidator.validate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void validate_dateLicenciement_avant_dateDebutGrossesse_throws() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate licenciement = LocalDate.of(2026, 1, 14); // 1 jour avant

        assertThatThrownBy(() -> LicenciementBeProtectionGrossesseValidator.validate(
                req(debutGrossesse, null, null, null, licenciement,
                        false, new BigDecimal("3000.00"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date incohérente")
                .hasMessageContaining("dateLicenciement");
    }

    @Test
    void validate_remunerationZero_throws() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate licenciement = LocalDate.of(2026, 3, 1);

        assertThatThrownBy(() -> LicenciementBeProtectionGrossesseValidator.validate(
                req(debutGrossesse, null, null, null, licenciement,
                        false, BigDecimal.ZERO, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationMensuelleBrute");
    }

    @Test
    void validate_remunerationNegative_throws() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate licenciement = LocalDate.of(2026, 3, 1);

        assertThatThrownBy(() -> LicenciementBeProtectionGrossesseValidator.validate(
                req(debutGrossesse, null, null, null, licenciement,
                        false, new BigDecimal("-1"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationMensuelleBrute");
    }

    @Test
    void validate_dateDebutGrossesse_null_throws() {
        LocalDate licenciement = LocalDate.of(2026, 3, 1);

        assertThatThrownBy(() -> LicenciementBeProtectionGrossesseValidator.validate(
                req(null, null, null, null, licenciement,
                        false, new BigDecimal("3000.00"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDebutGrossesse");
    }

    @Test
    void validate_dateLicenciement_null_throws() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);

        assertThatThrownBy(() -> LicenciementBeProtectionGrossesseValidator.validate(
                req(debutGrossesse, null, null, null, null,
                        false, new BigDecimal("3000.00"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateLicenciement");
    }

    @Test
    void validate_dateAccouchement_avant_dateDebutGrossesse_throws() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchementInvalide = LocalDate.of(2025, 12, 1);
        LocalDate licenciement = LocalDate.of(2026, 3, 1);

        assertThatThrownBy(() -> LicenciementBeProtectionGrossesseValidator.validate(
                req(debutGrossesse, accouchementInvalide, null, null, licenciement,
                        false, new BigDecimal("3000.00"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAccouchement");
    }

    @Test
    void validate_finConge_avant_debutConge_throws() {
        LocalDate debutGrossesse = LocalDate.of(2025, 4, 22);
        LocalDate debutConge = LocalDate.of(2026, 1, 10);
        LocalDate finCongeInvalide = LocalDate.of(2026, 1, 5);
        LocalDate licenciement = LocalDate.of(2026, 5, 1);

        assertThatThrownBy(() -> LicenciementBeProtectionGrossesseValidator.validate(
                req(debutGrossesse, null, debutConge, finCongeInvalide, licenciement,
                        false, new BigDecimal("3000.00"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateCongeMaterniteFinale");
    }

    @Test
    void validate_motifInvoque_blank_throws() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate licenciement = LocalDate.of(2026, 3, 1);

        assertThatThrownBy(() -> LicenciementBeProtectionGrossesseValidator.validate(
                req(debutGrossesse, null, null, null, licenciement,
                        false, new BigDecimal("3000.00"), "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifInvoqueParEmployeur");
    }

    // ── Base juridique + formule textuelle ─────────────────────────────────

    @Test
    void validate_baseJuridique_reference_loi_16_03_1971() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate licenciement = LocalDate.of(2026, 3, 1);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.baseJuridique())
                .contains("Loi 16/03/1971")
                .contains("art. 40");
    }

    @Test
    void validate_formuleCalcul_inclut_montants_si_protection() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate licenciement = LocalDate.of(2026, 3, 1);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.formuleCalcul())
                .contains("3000")
                .contains("6 mois")
                .contains("18000")
                .contains("art. 40");
    }

    @Test
    void validate_formuleCalcul_aucune_indemnite_si_HORS_PERIODE() {
        LocalDate debutGrossesse = LocalDate.of(2025, 4, 22);
        LocalDate finConge = LocalDate.of(2026, 2, 4);
        LocalDate licenciement = LocalDate.of(2026, 3, 5); // hors

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, null, null, finConge, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.formuleCalcul())
                .contains("Aucune indemnité")
                .contains("hors période");
    }

    @Test
    void validate_avertissement_inclut_dommages_cumulables_si_protection() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate licenciement = LocalDate.of(2026, 3, 1);

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, null, null, licenciement,
                                false, new BigDecimal("3000.00"), null));

        assertThat(r.avertissement())
                .contains("dommages")
                .contains("cumulable");
    }

    // ── Snapshot inputs/outputs ────────────────────────────────────────────

    @Test
    void validate_snapshot_inclut_tous_les_inputs() {
        LocalDate debutGrossesse = LocalDate.of(2026, 1, 15);
        LocalDate accouchement = LocalDate.of(2026, 10, 22);
        LocalDate debutConge = LocalDate.of(2026, 9, 1);
        LocalDate finConge = LocalDate.of(2027, 1, 15);
        LocalDate licenciement = LocalDate.of(2026, 5, 10);
        String motif = "Restructuration économique";

        LicenciementBeProtectionGrossesseResult r =
                LicenciementBeProtectionGrossesseValidator.validate(
                        req(debutGrossesse, accouchement, debutConge, finConge, licenciement,
                                true, new BigDecimal("3500.00"), motif));

        assertThat(r.dateDebutGrossesse()).isEqualTo(debutGrossesse);
        assertThat(r.dateAccouchement()).isEqualTo(accouchement);
        assertThat(r.dateCongeMaterniteDebut()).isEqualTo(debutConge);
        assertThat(r.dateCongeMaterniteFinale()).isEqualTo(finConge);
        assertThat(r.dateLicenciement()).isEqualTo(licenciement);
        assertThat(r.grossesseNotifieeParEcrit()).isTrue();
        assertThat(r.remunerationMensuelleBrute()).isEqualByComparingTo("3500.00");
        assertThat(r.motifInvoqueParEmployeur()).isEqualTo(motif);
    }
}
