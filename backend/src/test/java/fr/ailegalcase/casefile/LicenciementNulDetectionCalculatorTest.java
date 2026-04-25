package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.LicenciementNulDetectionCalculator.Protection;
import fr.ailegalcase.casefile.LicenciementNulDetectionCalculator.VerdictProbabilite;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicenciementNulDetectionCalculatorTest {

    private static final LocalDate NOTIF = LocalDate.of(2026, 4, 15);
    private static final BigDecimal SALAIRE = new BigDecimal("2500.00");

    private LicenciementNulDetectionInput.Builder baseBuilder() {
        return new LicenciementNulDetectionInput.Builder().notif(NOTIF).salaire(SALAIRE);
    }

    // --- Détection des 7 protections individuellement ---

    @Test
    void compute_salarieEnceinte_detectsMaternite() {
        var input = baseBuilder().enceinte(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).containsExactly(Protection.MATERNITE);
        assertThat(r.scoreNullite()).isEqualTo(35);
        assertThat(r.verdictProbabiliteNullite()).isEqualTo(VerdictProbabilite.MOYENNE);
    }

    @Test
    void compute_salarieAccidentTravail_detectsAT() {
        var input = baseBuilder().accidentTravail(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).containsExactly(Protection.ACCIDENT_TRAVAIL);
        assertThat(r.nulliteProbable()).isTrue();
    }

    @Test
    void compute_harcelementAvere_detectsHarcelement() {
        var input = baseBuilder().harcelementAvere(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).containsExactly(Protection.HARCELEMENT);
        assertThat(r.baseJuridique()).contains("L.1152-3");
    }

    @Test
    void compute_discriminationAlleguee_detectsDiscrimination() {
        var input = baseBuilder().discriminationAlleguee(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).containsExactly(Protection.DISCRIMINATION);
        assertThat(r.baseJuridique()).contains("L.1132-4");
    }

    @Test
    void compute_lanceurAlerte_detectsLanceurAlerte() {
        var input = baseBuilder().motifLanceurAlerte(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).containsExactly(Protection.LANCEUR_ALERTE);
        assertThat(r.baseJuridique()).contains("L.1132-3-3");
    }

    @Test
    void compute_mandatRepresentant_detectsMandatSyndical() {
        var input = baseBuilder().mandatRepresentant(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).containsExactly(Protection.MANDAT_SYNDICAL);
        assertThat(r.baseJuridique()).contains("L.2411-1");
    }

    @Test
    void compute_actionJustice_detectsActionJustice() {
        var input = baseBuilder().actionJustice(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).containsExactly(Protection.ACTION_JUSTICE);
    }

    // --- Dérivés date ---

    @Test
    void compute_dateAccouchementDansFenetre10Semaines_detectsMaternite() {
        // Notif le 2026-04-15, accouchement 5 semaines avant -> dans la fenêtre 10 semaines
        var input = baseBuilder().dateAccouchement(NOTIF.minusWeeks(5)).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).contains(Protection.MATERNITE);
    }

    @Test
    void compute_dateAccouchementHorsFenetre_neDeclenchePasMaternite() {
        // Accouchement il y a 12 semaines -> hors fenêtre
        var input = baseBuilder().dateAccouchement(NOTIF.minusWeeks(12)).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).doesNotContain(Protection.MATERNITE);
    }

    @Test
    void compute_dateConsolidationATDansFenetre6Mois_detectsAT() {
        var input = baseBuilder().dateConsolidationAT(NOTIF.minusMonths(3)).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).contains(Protection.ACCIDENT_TRAVAIL);
    }

    @Test
    void compute_dateConsolidationATHorsFenetre_neDeclenchePasAT() {
        var input = baseBuilder().dateConsolidationAT(NOTIF.minusMonths(8)).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).doesNotContain(Protection.ACCIDENT_TRAVAIL);
    }

    // --- Combinaisons + scoring ---

    @Test
    void compute_aucuneProtection_returnsScore0FaibleNotNull() {
        var input = baseBuilder().build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees()).isEmpty();
        assertThat(r.nombreProtectionsActives()).isZero();
        assertThat(r.nulliteProbable()).isFalse();
        assertThat(r.scoreNullite()).isZero();
        assertThat(r.verdictProbabiliteNullite()).isEqualTo(VerdictProbabilite.FAIBLE);
        assertThat(r.reintegrationOuverte()).isFalse();
    }

    @Test
    void compute_2protections_returnsScore70Elevee() {
        var input = baseBuilder().accidentTravail(true).discriminationAlleguee(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.protectionsDetectees())
                .containsExactly(Protection.ACCIDENT_TRAVAIL, Protection.DISCRIMINATION);
        assertThat(r.nombreProtectionsActives()).isEqualTo(2);
        assertThat(r.scoreNullite()).isEqualTo(70);
        assertThat(r.verdictProbabiliteNullite()).isEqualTo(VerdictProbabilite.ELEVEE);
        assertThat(r.indemniteMinimumNuliteEur()).isEqualByComparingTo("15000.00");
        assertThat(r.indemniteMinimumMois()).isEqualTo(6);
        assertThat(r.reintegrationOuverte()).isTrue();
    }

    @Test
    void compute_3protections_returnsScore100Elevee() {
        var input = baseBuilder()
                .harcelementAvere(true).discriminationAlleguee(true).motifLanceurAlerte(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.scoreNullite()).isEqualTo(100);
        assertThat(r.verdictProbabiliteNullite()).isEqualTo(VerdictProbabilite.ELEVEE);
    }

    @Test
    void compute_5protections_scorePlafonneA100() {
        var input = baseBuilder()
                .enceinte(true).accidentTravail(true).harcelementAvere(true)
                .discriminationAlleguee(true).motifLanceurAlerte(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.nombreProtectionsActives()).isEqualTo(5);
        assertThat(r.scoreNullite()).isEqualTo(100);
    }

    @Test
    void compute_baseJuridique_concatProtectionsArticles() {
        var input = baseBuilder().accidentTravail(true).discriminationAlleguee(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.baseJuridique())
                .contains("L.1235-3-1")
                .contains("L.1226-9")
                .contains("L.1132-4");
    }

    @Test
    void compute_formule_includesProtectionsCount() {
        var input = baseBuilder().harcelementAvere(true).discriminationAlleguee(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.formule())
                .contains("Plancher 6 mois")
                .contains("2500,00")
                .contains("15000,00")
                .contains("2 protections détectées")
                .contains("ELEVEE");
    }

    @Test
    void compute_indemniteEqualsSixTimesSalaire() {
        var input = baseBuilder().salaire(new BigDecimal("3450.75")).harcelementAvere(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.indemniteMinimumNuliteEur()).isEqualByComparingTo("20704.50");
    }

    // --- Cas d'erreur ---

    @Test
    void compute_salaireZero_throwsIllegalArgument() {
        var input = baseBuilder().salaire(BigDecimal.ZERO).build();
        assertThatThrownBy(() -> LicenciementNulDetectionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    void compute_salaireNegatif_throwsIllegalArgument() {
        var input = baseBuilder().salaire(new BigDecimal("-100")).build();
        assertThatThrownBy(() -> LicenciementNulDetectionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_salaireNull_throwsIllegalArgument() {
        var input = baseBuilder().salaire(null).build();
        assertThatThrownBy(() -> LicenciementNulDetectionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateNotificationNull_throwsIllegalArgument() {
        var input = baseBuilder().notif(null).build();
        assertThatThrownBy(() -> LicenciementNulDetectionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("notification");
    }

    @Test
    void compute_ancienneteNegative_throwsIllegalArgument() {
        var input = baseBuilder().anciennete(-1).build();
        assertThatThrownBy(() -> LicenciementNulDetectionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ancienneté");
    }

    @Test
    void compute_paysBELGIQUE_throwsIllegalArgument() {
        var input = baseBuilder().harcelementAvere(true).build();
        assertThatThrownBy(() -> LicenciementNulDetectionCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void compute_paysNull_throwsIllegalArgument() {
        var input = baseBuilder().build();
        assertThatThrownBy(() -> LicenciementNulDetectionCalculator.compute(input, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> LicenciementNulDetectionCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_messageMandatSyndical_includesNonCumulInfo() {
        var input = baseBuilder().mandatRepresentant(true).build();
        var r = LicenciementNulDetectionCalculator.compute(input, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("Salarié protégé") && m.contains("pas cumulable"));
    }
}
