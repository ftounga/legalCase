package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-213-03 : tests unitaires du {@link LicenciementBeStatutUniquePreavisCalculator}.
 *
 * <p>Vérifie le barème statut unique côté employeur (Loi 26/12/2013), le calcul
 * de l'indemnité compensatoire (salaire hebdo × semaines), la date de fin
 * de préavis, l'avertissement pré-2014 et la validation des inputs.</p>
 */
class LicenciementBeStatutUniquePreavisCalculatorTest {

    private static LicenciementBeStatutUniquePreavisRequest req(
            Integer annees, Integer moisSupp, BigDecimal salaireHebdo,
            LocalDate dateNotif, Boolean statutUniqueSeulement) {
        return new LicenciementBeStatutUniquePreavisRequest(
                annees, moisSupp, salaireHebdo, dateNotif, statutUniqueSeulement);
    }

    // ---- Barème — tranches limites ----

    @Test
    void calculate_anciennete0AnneeMoisInferieurA3_returns2Semaines() {
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(0, 2, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.dureePreavisEnSemaines()).isEqualTo(2);
        assertThat(r.indemniteCompensatoire()).isEqualByComparingTo("1000.00");
    }

    @Test
    void calculate_anciennete0AnneeMoisAuMoins3_returns4Semaines() {
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(0, 6, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.dureePreavisEnSemaines()).isEqualTo(4);
    }

    @Test
    void calculate_anciennete1An_returns6Semaines() {
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(1, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.dureePreavisEnSemaines()).isEqualTo(6);
    }

    @Test
    void calculate_anciennete9Ans_returns27Semaines() {
        // Cas explicite mini-spec : 9 ans → 27 semaines
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(9, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.dureePreavisEnSemaines()).isEqualTo(27);
    }

    @Test
    void calculate_anciennete9Ans6Mois_resteSurTranche9_27Semaines() {
        // Mois supplémentaires ne changent pas la tranche (barème indexé années
        // complètes) — 9 ans 6 mois reste tranche 9-10 ans = 27 semaines.
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(9, 6, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.dureePreavisEnSemaines()).isEqualTo(27);
        assertThat(r.formuleCalcul()).contains("9 ans 6 mois").contains("27 semaines");
    }

    @Test
    void calculate_anciennete10Ans_returns30Semaines() {
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(10, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.dureePreavisEnSemaines()).isEqualTo(30);
    }

    @Test
    void calculate_anciennete19Ans_returns57Semaines() {
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(19, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.dureePreavisEnSemaines()).isEqualTo(57);
    }

    @Test
    void calculate_anciennete20Ans_returns62SemainesPlafond() {
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(20, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.dureePreavisEnSemaines()).isEqualTo(62);
    }

    @Test
    void calculate_anciennete30Ans_resteAuPlafond62Semaines() {
        // Au-delà de 20 ans, la durée reste plafonnée à 62 semaines (loi 26/12/2013).
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(30, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.dureePreavisEnSemaines()).isEqualTo(62);
    }

    // ---- Indemnité compensatoire ----

    @Test
    void calculate_indemniteCompensatoire_500EurosX27Semaines_returns13500() {
        // Cas mini-spec : salaire hebdo 500 € × 27 semaines = 13 500 €
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(9, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.indemniteCompensatoire()).isEqualByComparingTo("13500.00");
        assertThat(r.formuleCalcul()).contains("500.00").contains("27").contains("13500.00");
    }

    @Test
    void calculate_indemniteCompensatoire_arrondi2Decimales() {
        // 500.555 € × 27 = 13 514.985 → arrondi HALF_UP à 13 514.99
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(9, 0, new BigDecimal("500.555"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.indemniteCompensatoire().scale()).isEqualTo(2);
    }

    // ---- Date de fin de préavis ----

    @Test
    void calculate_dateFinPreavis_addsNSemainesEnJours() {
        // 9 ans → 27 semaines → 189 jours après notification
        LocalDate notif = LocalDate.of(2025, 5, 19);
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(9, 0, new BigDecimal("500.00"), notif, true));

        assertThat(r.dateFinPreavis()).isEqualTo(notif.plusDays(27L * 7));
        assertThat(r.dateFinPreavis()).isEqualTo(LocalDate.of(2025, 11, 24));
    }

    // ---- Avertissement pré-2014 ----

    @Test
    void calculate_partieStatutUniqueSeulement_avertissementParDefaut() {
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(9, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.avertissement()).contains("statut unique").doesNotContain("Formule Claeys");
        assertThat(r.partieStatutUniqueSeulement()).isTrue();
    }

    @Test
    void calculate_contratPre2014_avertissementClaeys() {
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(9, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), false));

        assertThat(r.avertissement()).contains("Formule Claeys").contains("01/01/2014");
        assertThat(r.partieStatutUniqueSeulement()).isFalse();
    }

    @Test
    void calculate_statutUniqueSeulementNull_defaultTrue() {
        // null → traité comme true (post-2014)
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(9, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), null));

        assertThat(r.partieStatutUniqueSeulement()).isTrue();
        assertThat(r.avertissement()).doesNotContain("Formule Claeys");
    }

    // ---- Validation des inputs ----

    @Test
    void calculate_requestNull_throws() {
        assertThatThrownBy(() -> LicenciementBeStatutUniquePreavisCalculator.calculate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculate_ancienneteNegative_throws() {
        assertThatThrownBy(() -> LicenciementBeStatutUniquePreavisCalculator.calculate(
                req(-1, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteAnnees");
    }

    @Test
    void calculate_salaireZero_throws() {
        assertThatThrownBy(() -> LicenciementBeStatutUniquePreavisCalculator.calculate(
                req(9, 0, BigDecimal.ZERO, LocalDate.of(2025, 6, 1), true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireHebdomadaireBrut");
    }

    @Test
    void calculate_salaireNegatif_throws() {
        assertThatThrownBy(() -> LicenciementBeStatutUniquePreavisCalculator.calculate(
                req(9, 0, new BigDecimal("-1"), LocalDate.of(2025, 6, 1), true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculate_dateNotificationNull_throws() {
        assertThatThrownBy(() -> LicenciementBeStatutUniquePreavisCalculator.calculate(
                req(9, 0, new BigDecimal("500.00"), null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationLicenciement");
    }

    @Test
    void calculate_moisSupplementaireHorsBornes_throws() {
        assertThatThrownBy(() -> LicenciementBeStatutUniquePreavisCalculator.calculate(
                req(9, 12, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteMoisSupplementaires");
    }

    // ---- Base juridique + formule ----

    @Test
    void calculate_baseJuridique_referenceLoi26122013() {
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(9, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.baseJuridique()).contains("Loi 26/12/2013").contains("statut unique");
    }

    @Test
    void calculate_formuleCalcul_includesAllValues() {
        LicenciementBeStatutUniquePreavisResult r =
                LicenciementBeStatutUniquePreavisCalculator.calculate(
                        req(9, 0, new BigDecimal("500.00"), LocalDate.of(2025, 6, 1), true));

        assertThat(r.formuleCalcul())
                .contains("9 ans")
                .contains("27 semaines")
                .contains("500.00")
                .contains("13500.00")
                .contains("loi 26/12/2013");
    }
}
