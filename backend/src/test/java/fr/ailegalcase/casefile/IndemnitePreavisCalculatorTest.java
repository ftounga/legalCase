package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-DT-25-01 : tests unitaires du calculateur d'indemnité compensatrice de préavis FR
 * (art. L.1234-1 Code du travail + CCN).
 */
class IndemnitePreavisCalculatorTest {

    private static final LocalDate DATE_RUPTURE = LocalDate.of(2026, 4, 25);
    private static final BigDecimal SALAIRE_2500 = new BigDecimal("2500.00");

    @Test
    void compute_anciennete5ans_emp_idcc2120_returnsCcn3Mois() {
        // CCN Banque EMPLOYE 24+ mois → 3 mois (plus favorable que légale 2 mois)
        IndemnitePreavisResult r = IndemnitePreavisCalculator.compute(
                60, IndemnitePreavisFonction.EMPLOYE, "IDCC_2120", 3, "CCN Banque art. 27",
                SALAIRE_2500, true, DATE_RUPTURE);
        assertThat(r.dureePreavisMois()).isEqualTo(3);
        assertThat(r.sourceDuree()).isEqualTo(IndemnitePreavisSourceDuree.CCN);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("7500.00");
        assertThat(r.exemptionRetenue()).isTrue();
        assertThat(r.baseJuridique()).contains("L.1234-1").contains("IDCC_2120").contains("CCN Banque art. 27");
        assertThat(r.formule()).contains("3 mois").contains("2500,00").contains("7500,00");
    }

    @Test
    void compute_anciennete5ans_emp_sansCcn_returnsLegale2Mois() {
        IndemnitePreavisResult r = IndemnitePreavisCalculator.compute(
                60, IndemnitePreavisFonction.EMPLOYE, null, null, null,
                SALAIRE_2500, true, DATE_RUPTURE);
        assertThat(r.dureePreavisMois()).isEqualTo(2);
        assertThat(r.sourceDuree()).isEqualTo(IndemnitePreavisSourceDuree.LEGALE);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("5000.00");
        assertThat(r.baseJuridique()).isEqualTo("Art. L.1234-1 Code du travail");
    }

    @Test
    void compute_anciennete12mois_returnsLegale1Mois() {
        IndemnitePreavisResult r = IndemnitePreavisCalculator.compute(
                12, IndemnitePreavisFonction.EMPLOYE, null, null, null,
                SALAIRE_2500, true, DATE_RUPTURE);
        assertThat(r.dureePreavisMois()).isEqualTo(1);
        assertThat(r.sourceDuree()).isEqualTo(IndemnitePreavisSourceDuree.LEGALE);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("2500.00");
    }

    @Test
    void compute_anciennete3mois_returnsUsage0Mois_avecMessage() {
        IndemnitePreavisResult r = IndemnitePreavisCalculator.compute(
                3, IndemnitePreavisFonction.EMPLOYE, null, null, null,
                SALAIRE_2500, true, DATE_RUPTURE);
        assertThat(r.dureePreavisMois()).isEqualTo(0);
        assertThat(r.sourceDuree()).isEqualTo(IndemnitePreavisSourceDuree.USAGE);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("0.00");
        assertThat(r.messages()).anyMatch(m -> m.contains("usage"));
        assertThat(r.baseJuridique()).contains("usage");
    }

    @Test
    void compute_ccnInferieureALegale_priorisationLegale() {
        // Légale = 2 mois (60 mois) ; CCN = 1 mois → on retient la légale (principe de faveur).
        IndemnitePreavisResult r = IndemnitePreavisCalculator.compute(
                60, IndemnitePreavisFonction.OUVRIER, "IDCC_3248", 1, "CCN Métallurgie art. 30",
                SALAIRE_2500, true, DATE_RUPTURE);
        assertThat(r.dureePreavisMois()).isEqualTo(2);
        assertThat(r.sourceDuree()).isEqualTo(IndemnitePreavisSourceDuree.LEGALE);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("5000.00");
        assertThat(r.messages()).anyMatch(m -> m.contains("principe de faveur"));
    }

    @Test
    void compute_exemptionFalse_returnsZero_avecMessage() {
        IndemnitePreavisResult r = IndemnitePreavisCalculator.compute(
                60, IndemnitePreavisFonction.EMPLOYE, null, null, null,
                SALAIRE_2500, false, DATE_RUPTURE);
        assertThat(r.dureePreavisMois()).isEqualTo(2);
        assertThat(r.sourceDuree()).isEqualTo(IndemnitePreavisSourceDuree.LEGALE);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("0.00");
        assertThat(r.exemptionRetenue()).isFalse();
        assertThat(r.messages()).anyMatch(m -> m.contains("n'a pas dispensé du préavis"));
    }

    @Test
    void compute_anciennete0_returnsZero_avecMessage() {
        IndemnitePreavisResult r = IndemnitePreavisCalculator.compute(
                0, IndemnitePreavisFonction.OUVRIER, null, null, null,
                SALAIRE_2500, true, DATE_RUPTURE);
        assertThat(r.dureePreavisMois()).isEqualTo(0);
        assertThat(r.sourceDuree()).isEqualTo(IndemnitePreavisSourceDuree.USAGE);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_salaireZero_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePreavisCalculator.compute(
                12, IndemnitePreavisFonction.EMPLOYE, null, null, null,
                BigDecimal.ZERO, true, DATE_RUPTURE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positif");
    }

    @Test
    void compute_salaireNegatif_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePreavisCalculator.compute(
                12, IndemnitePreavisFonction.EMPLOYE, null, null, null,
                new BigDecimal("-100.00"), true, DATE_RUPTURE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_ancienneteNegative_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePreavisCalculator.compute(
                -1, IndemnitePreavisFonction.EMPLOYE, null, null, null,
                SALAIRE_2500, true, DATE_RUPTURE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void compute_fonctionNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePreavisCalculator.compute(
                12, null, null, null, null,
                SALAIRE_2500, true, DATE_RUPTURE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fonction");
    }

    @Test
    void compute_dateRuptureNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePreavisCalculator.compute(
                12, IndemnitePreavisFonction.EMPLOYE, null, null, null,
                SALAIRE_2500, true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date de rupture");
    }

    @Test
    void compute_ccnDureeNegative_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePreavisCalculator.compute(
                12, IndemnitePreavisFonction.EMPLOYE, "IDCC_1486", -1, "art. 19",
                SALAIRE_2500, true, DATE_RUPTURE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CCN");
    }

    @Test
    void compute_cadre_idccSyntec_returns3Mois() {
        // CCN Syntec CADRE → 3 mois dès l'embauche (ancienneté 3 mois ici)
        IndemnitePreavisResult r = IndemnitePreavisCalculator.compute(
                3, IndemnitePreavisFonction.CADRE, "IDCC_1486", 3, "CCN Syntec art. 19",
                new BigDecimal("5000.00"), true, DATE_RUPTURE);
        assertThat(r.dureePreavisMois()).isEqualTo(3);
        assertThat(r.sourceDuree()).isEqualTo(IndemnitePreavisSourceDuree.CCN);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("15000.00");
    }

    @Test
    void compute_formule_arrondiCentime_HALF_UP() {
        // 1 × 2833.335 → 2833.34 (HALF_UP à la 2e décimale)
        IndemnitePreavisResult r = IndemnitePreavisCalculator.compute(
                12, IndemnitePreavisFonction.EMPLOYE, null, null, null,
                new BigDecimal("2833.335"), true, DATE_RUPTURE);
        assertThat(r.dureePreavisMois()).isEqualTo(1);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("2833.34");
    }

    @Test
    void compute_ccnEgaleALegale_returnsLegale() {
        // CCN = 2 mois, légale = 2 mois → sourceDuree = LEGALE (strictement > requis pour CCN).
        IndemnitePreavisResult r = IndemnitePreavisCalculator.compute(
                60, IndemnitePreavisFonction.EMPLOYE, "IDCC_3248", 2, "art. 30",
                SALAIRE_2500, true, DATE_RUPTURE);
        assertThat(r.dureePreavisMois()).isEqualTo(2);
        assertThat(r.sourceDuree()).isEqualTo(IndemnitePreavisSourceDuree.LEGALE);
    }
}
