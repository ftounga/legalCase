package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-213-04 : tests unitaires du {@link LicenciementBeFormuleClaeysCalculator}.
 *
 * <p>Vérifie la formule Claeys jurisprudentielle (0.8 × (ancienneté + 0.04 ×
 * rém. K€)), la conversion mois → semaines (× 4.333 arrondi), la clause de
 * sauvegarde optionnelle (cumul avec barème statut unique partagé via
 * {@link LicenciementBeBaremeStatutUnique}), l'avertissement obligatoire et
 * la validation des inputs.</p>
 */
class LicenciementBeFormuleClaeysCalculatorTest {

    private static LicenciementBeFormuleClaeysRequest req(
            Integer anneesPre, Integer moisPre, BigDecimal remKEur,
            Boolean appliquerSauvegarde, Integer anneesPost, BigDecimal salaireHebdo) {
        return new LicenciementBeFormuleClaeysRequest(
                anneesPre, moisPre, remKEur, appliquerSauvegarde, anneesPost, salaireHebdo);
    }

    // ---- Formule Claeys — calculs nominaux ----

    @Test
    void calculate_12ans_60KEur_sansSauvegarde_returns50Semaines() {
        // 0.8 × (12 + 0.04 × 60) = 0.8 × 14.4 = 11.52 mois
        // 11.52 × 4.333 = 49.92 → arrondi 50 semaines
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), false, null, null));

        assertThat(r.preavisClaeysMois()).isEqualByComparingTo("11.52");
        assertThat(r.preavisClaeysSemaines()).isEqualTo(50);
        assertThat(r.preavisStatutUniquesSemaines()).isZero();
        assertThat(r.preavisTotalSemaines()).isEqualTo(50);
    }

    @Test
    void calculate_20ans_200KEur_sansSauvegarde_returns97Semaines() {
        // 0.8 × (20 + 0.04 × 200) = 0.8 × 28 = 22.40 mois
        // 22.40 × 4.333 = 97.06 → arrondi 97 semaines
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(20, 0, new BigDecimal("200"), false, null, null));

        assertThat(r.preavisClaeysMois()).isEqualByComparingTo("22.40");
        assertThat(r.preavisClaeysSemaines()).isEqualTo(97);
    }

    @Test
    void calculate_avecMoisSupplementaires_inclueDansFormule() {
        // 10 ans 6 mois, 50 K€ : 0.8 × (10.5 + 2) = 0.8 × 12.5 = 10.00 mois
        // 10.00 × 4.333 = 43.33 → arrondi 43 semaines
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(10, 6, new BigDecimal("50"), false, null, null));

        assertThat(r.preavisClaeysMois()).isEqualByComparingTo("10.00");
        assertThat(r.preavisClaeysSemaines()).isEqualTo(43);
    }

    @Test
    void calculate_remunerationTresElevee_extrapole() {
        // 5 ans, 500 K€ : 0.8 × (5 + 20) = 20.00 mois → 20 × 4.333 = 86.66 → 87 sem
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(5, 0, new BigDecimal("500"), false, null, null));

        assertThat(r.preavisClaeysMois()).isEqualByComparingTo("20.00");
        assertThat(r.preavisClaeysSemaines()).isEqualTo(87);
    }

    @Test
    void calculate_remunerationFaible_calculeFormule() {
        // 3 ans, 20 K€ : 0.8 × (3 + 0.8) = 0.8 × 3.8 = 3.04 mois
        // 3.04 × 4.333 = 13.17 → 13 sem
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(3, 0, new BigDecimal("20"), false, null, null));

        assertThat(r.preavisClaeysMois()).isEqualByComparingTo("3.04");
        assertThat(r.preavisClaeysSemaines()).isEqualTo(13);
    }

    // ---- Clause de sauvegarde (art. 67 loi 26/12/2013) ----

    @Test
    void calculate_clauseSauvegarde_cumuleClaeysEtStatutUnique() {
        // Claeys : 12 ans, 60 K€ → 50 sem
        // Statut unique : 8 ans → 24 sem (barème partagé via LicenciementBeBaremeStatutUnique)
        // Total : 74 sem
        // Indemnité Claeys : 1000 € × 50 = 50 000 €
        // Indemnité totale : 1000 € × 74 = 74 000 €
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), true, 8, new BigDecimal("1000.00")));

        assertThat(r.preavisClaeysSemaines()).isEqualTo(50);
        assertThat(r.preavisStatutUniquesSemaines()).isEqualTo(24);
        assertThat(r.preavisTotalSemaines()).isEqualTo(74);
        assertThat(r.indemniteClaeysBrute()).isEqualByComparingTo("50000.00");
        assertThat(r.indemniteTotaleBrute()).isEqualByComparingTo("74000.00");
        assertThat(r.appliquerClauseSauvegarde()).isTrue();
    }

    @Test
    void calculate_clauseSauvegarde_avec20AnsPost_plafonne62Sem() {
        // Claeys 5 ans 30 K€ : 0.8 × (5 + 1.2) = 4.96 mois → 4.96 × 4.333 = 21.49 → 21 sem
        // Statut unique 20 ans → 62 sem plafond
        // Total : 21 + 62 = 83 sem
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(5, 0, new BigDecimal("30"), true, 20, new BigDecimal("500.00")));

        assertThat(r.preavisClaeysSemaines()).isEqualTo(21);
        assertThat(r.preavisStatutUniquesSemaines()).isEqualTo(62);
        assertThat(r.preavisTotalSemaines()).isEqualTo(83);
    }

    @Test
    void calculate_sansClauseSauvegarde_renvoieUniquementClaeys() {
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), false, null, null));

        assertThat(r.appliquerClauseSauvegarde()).isFalse();
        assertThat(r.preavisStatutUniquesSemaines()).isZero();
        assertThat(r.preavisTotalSemaines()).isEqualTo(r.preavisClaeysSemaines());
        assertThat(r.indemniteClaeysBrute()).isNull();
        assertThat(r.indemniteTotaleBrute()).isNull();
        assertThat(r.salaireHebdomadaireBrut()).isNull();
        assertThat(r.ancienneteAnneesPostStatutUnique()).isNull();
    }

    @Test
    void calculate_appliquerSauvegardeNull_defaultTrue() {
        // null → traité comme true → exige les champs post-2014
        assertThatThrownBy(() -> LicenciementBeFormuleClaeysCalculator.calculate(
                req(12, 0, new BigDecimal("60"), null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteAnneesPostStatutUnique");
    }

    @Test
    void calculate_appliquerSauvegardeNull_avecChamps_calculeNormalement() {
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), null, 8, new BigDecimal("1000.00")));

        assertThat(r.appliquerClauseSauvegarde()).isTrue();
        assertThat(r.preavisTotalSemaines()).isEqualTo(74);
    }

    // ---- Validation des inputs ----

    @Test
    void calculate_requestNull_throws() {
        assertThatThrownBy(() -> LicenciementBeFormuleClaeysCalculator.calculate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculate_ancienneteNegative_throws() {
        assertThatThrownBy(() -> LicenciementBeFormuleClaeysCalculator.calculate(
                req(-1, 0, new BigDecimal("60"), false, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteAnneesPreStatutUnique");
    }

    @Test
    void calculate_remunerationZero_throws() {
        assertThatThrownBy(() -> LicenciementBeFormuleClaeysCalculator.calculate(
                req(12, 0, BigDecimal.ZERO, false, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationAnnuelleBruteEnMilliers");
    }

    @Test
    void calculate_remunerationNegative_throws() {
        assertThatThrownBy(() -> LicenciementBeFormuleClaeysCalculator.calculate(
                req(12, 0, new BigDecimal("-1"), false, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationAnnuelleBruteEnMilliers");
    }

    @Test
    void calculate_moisHorsBornes_throws() {
        assertThatThrownBy(() -> LicenciementBeFormuleClaeysCalculator.calculate(
                req(12, 12, new BigDecimal("60"), false, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteMoisPreStatutUnique");
    }

    @Test
    void calculate_sauvegardeTrue_sansAnneesPost_throws() {
        assertThatThrownBy(() -> LicenciementBeFormuleClaeysCalculator.calculate(
                req(12, 0, new BigDecimal("60"), true, null, new BigDecimal("1000.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteAnneesPostStatutUnique");
    }

    @Test
    void calculate_sauvegardeTrue_sansSalaireHebdo_throws() {
        assertThatThrownBy(() -> LicenciementBeFormuleClaeysCalculator.calculate(
                req(12, 0, new BigDecimal("60"), true, 8, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireHebdomadaireBrut");
    }

    @Test
    void calculate_sauvegardeTrue_salaireNegatif_throws() {
        assertThatThrownBy(() -> LicenciementBeFormuleClaeysCalculator.calculate(
                req(12, 0, new BigDecimal("60"), true, 8, BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireHebdomadaireBrut");
    }

    // ---- Avertissement obligatoire ----

    @Test
    void calculate_avertissement_toujoursNonNull_avecSauvegarde() {
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), true, 8, new BigDecimal("1000.00")));

        assertThat(r.avertissement()).isNotNull().isNotBlank()
                .contains("Formule Claeys")
                .contains("jurisprudence");
    }

    @Test
    void calculate_avertissement_toujoursNonNull_sansSauvegarde() {
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), false, null, null));

        assertThat(r.avertissement()).isNotNull().isNotBlank()
                .contains("Formule Claeys");
    }

    // ---- Base juridique + formule textuelle ----

    @Test
    void calculate_baseJuridique_referenceLois_avecSauvegarde() {
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), true, 8, new BigDecimal("1000.00")));

        assertThat(r.baseJuridique())
                .contains("Loi 03/07/1978")
                .contains("art. 82")
                .contains("Loi 26/12/2013")
                .contains("art. 67");
    }

    @Test
    void calculate_baseJuridique_referenceLoi1978_sansSauvegarde() {
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), false, null, null));

        assertThat(r.baseJuridique())
                .contains("Loi 03/07/1978")
                .contains("art. 82");
    }

    @Test
    void calculate_formuleClaeys_inclueValeursEtFormule() {
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), false, null, null));

        assertThat(r.formuleClaeys())
                .contains("0.8")
                .contains("0.04")
                .contains("12")
                .contains("60")
                .contains("50 semaines");
    }

    @Test
    void calculate_formuleClaeys_inclueMoisSiNonZero() {
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(10, 6, new BigDecimal("50"), false, null, null));

        assertThat(r.formuleClaeys()).contains("10 ans 6 mois");
    }

    // ---- Cohérence indemnités ----

    @Test
    void calculate_indemniteTotale_egaleSalaireHebdoMultPreavisTotal() {
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), true, 8, new BigDecimal("1500.00")));

        BigDecimal expectedTotal = new BigDecimal("1500.00")
                .multiply(BigDecimal.valueOf(r.preavisTotalSemaines()));
        assertThat(r.indemniteTotaleBrute()).isEqualByComparingTo(expectedTotal);

        BigDecimal expectedClaeys = new BigDecimal("1500.00")
                .multiply(BigDecimal.valueOf(r.preavisClaeysSemaines()));
        assertThat(r.indemniteClaeysBrute()).isEqualByComparingTo(expectedClaeys);
    }

    @Test
    void calculate_indemnite_arrondi2Decimales() {
        LicenciementBeFormuleClaeysResult r =
                LicenciementBeFormuleClaeysCalculator.calculate(
                        req(12, 0, new BigDecimal("60"), true, 8, new BigDecimal("1234.567")));

        assertThat(r.indemniteClaeysBrute().scale()).isEqualTo(2);
        assertThat(r.indemniteTotaleBrute().scale()).isEqualTo(2);
    }
}
