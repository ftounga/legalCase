package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.ReserveHereditaireCalculator.QualiteDemandeur;
import fr.ailegalcase.casefile.ReserveHereditaireCalculator.VerdictRecevabilite;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReserveHereditaireCalculatorTest {

    private static final String FR = "FRANCE";

    private static BigDecimal eur(String s) {
        return new BigDecimal(s);
    }

    private static LocalDate yearsAgo(int years) {
        return LocalDate.now().minusYears(years).plusDays(30); // marge tampon
    }

    // ============ Barème art. 913 + 914-1 ============

    @Test
    void bareme_0enfantConjoint_QD75_reserve25() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                0, true, eur("100000"), eur("0"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.CONJOINT_SURVIVANT, FR);
        assertThat(r.quotiteDisponiblePct()).isEqualTo(75.0);
        assertThat(r.reservePct()).isEqualTo(25.0);
        assertThat(r.montantQuotiteDispoEur()).isEqualByComparingTo("75000.00");
        assertThat(r.montantReserveEur()).isEqualByComparingTo("25000.00");
    }

    @Test
    void bareme_0enfantSansConjoint_QD100_reserve0() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                0, false, eur("100000"), eur("0"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.quotiteDisponiblePct()).isEqualTo(100.0);
        assertThat(r.reservePct()).isEqualTo(0.0);
    }

    @Test
    void bareme_1enfant_QD50_reserve50() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                1, false, eur("200000"), eur("0"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.quotiteDisponiblePct()).isEqualTo(50.0);
        assertThat(r.reservePct()).isEqualTo(50.0);
        assertThat(r.montantReserveEur()).isEqualByComparingTo("100000.00");
        assertThat(r.montantQuotiteDispoEur()).isEqualByComparingTo("100000.00");
    }

    @Test
    void bareme_2enfants_QD33_reserve67() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                2, false, eur("300000"), eur("0"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.quotiteDisponiblePct()).isEqualTo(33.33);
        assertThat(r.reservePct()).isEqualTo(66.67);
        // QD = 100 000 (300 000 / 3) — réserve = 200 000
        assertThat(r.montantQuotiteDispoEur()).isEqualByComparingTo("100000.00");
        assertThat(r.montantReserveEur()).isEqualByComparingTo("200000.00");
    }

    @Test
    void bareme_3enfants_QD25_reserve75() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                3, false, eur("400000"), eur("0"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.quotiteDisponiblePct()).isEqualTo(25.0);
        assertThat(r.reservePct()).isEqualTo(75.0);
        assertThat(r.montantQuotiteDispoEur()).isEqualByComparingTo("100000.00");
        assertThat(r.montantReserveEur()).isEqualByComparingTo("300000.00");
    }

    @Test
    void bareme_4enfants_meme75pct() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                4, false, eur("400000"), eur("0"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.quotiteDisponiblePct()).isEqualTo(25.0);
        assertThat(r.reservePct()).isEqualTo(75.0);
    }

    // ============ Excédent et action en réduction ============

    @Test
    void libsInferieuresQD_pasExcedent_actionNonRecevable() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                1, false, eur("200000"), eur("80000"), // QD = 100 000, libs 80 000
                LocalDate.now().minusMonths(6), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.excedentReductibleEur()).isEqualByComparingTo("0.00");
        assertThat(r.actionReductionRecevable()).isFalse();
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.NON_RECEVABLE_PAS_EXCEDENT);
    }

    @Test
    void libsSuperieuresQD_excedent_actionRecevable() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                1, false, eur("200000"), eur("150000"), // QD = 100 000, libs 150 000
                LocalDate.now().minusMonths(6), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.excedentReductibleEur()).isEqualByComparingTo("50000.00");
        assertThat(r.actionReductionRecevable()).isTrue();
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.RECEVABLE);
    }

    @Test
    void prescriptionEcoulee_actionNonRecevable() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                1, false, eur("200000"), eur("150000"),
                LocalDate.now().minusYears(6), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.actionReductionRecevable()).isFalse();
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.NON_RECEVABLE_PRESCRIPTION);
        assertThat(r.delaiPrescriptionRestantMois()).isZero();
    }

    @Test
    void prescriptionPresqueAtteinte_actionRecevable() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                1, false, eur("200000"), eur("150000"),
                yearsAgo(4), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        // ~4 ans → ~12 mois restants
        assertThat(r.delaiPrescriptionRestantMois()).isPositive();
        assertThat(r.actionReductionRecevable()).isTrue();
    }

    // ============ Qualité du demandeur (914-1) ============

    @Test
    void conjointDemandeurSansDescendants_qualiteValide() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                0, true, eur("200000"), eur("180000"),
                LocalDate.now().minusMonths(6), QualiteDemandeur.CONJOINT_SURVIVANT, FR);
        // QD = 150 000, libs = 180 000 → excédent = 30 000
        assertThat(r.excedentReductibleEur()).isEqualByComparingTo("30000.00");
        assertThat(r.actionReductionRecevable()).isTrue();
    }

    @Test
    void conjointDemandeurAvecDescendants_qualiteInvalide() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                2, true, eur("300000"), eur("250000"),
                LocalDate.now().minusMonths(6), QualiteDemandeur.CONJOINT_SURVIVANT, FR);
        assertThat(r.actionReductionRecevable()).isFalse();
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.NON_RECEVABLE_QUALITE);
    }

    @Test
    void descendantDemandeurSansEnfants_qualiteInvalide() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                0, true, eur("200000"), eur("180000"),
                LocalDate.now().minusMonths(6), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.actionReductionRecevable()).isFalse();
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.NON_RECEVABLE_QUALITE);
    }

    // ============ Calculs montants ============

    @Test
    void montantReserveEur_2enfants_3enfants_calculesCorrectement() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                2, false, eur("600000"), eur("0"),
                LocalDate.now().minusMonths(6), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        // 600 000 × 66.6666...% = 400 000 (arrondi 2 décimales)
        assertThat(r.montantReserveEur()).isEqualByComparingTo("400000.00");
    }

    @Test
    void delaiPrescriptionRestantMois_2ansEcoules_environ36mois() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                1, false, eur("200000"), eur("150000"),
                LocalDate.now().minusYears(2), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        // ~2 ans → ~36 mois restants
        assertThat(r.delaiPrescriptionRestantMois()).isBetween(34L, 38L);
    }

    // ============ Validations ============

    @Test
    void validation_nombreEnfantsNegatif_throws() {
        assertThatThrownBy(() -> ReserveHereditaireCalculator.compute(
                -1, false, eur("100000"), eur("0"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_montantSuccessionZero_throws() {
        assertThatThrownBy(() -> ReserveHereditaireCalculator.compute(
                1, false, BigDecimal.ZERO, eur("0"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_montantSuccessionNegatif_throws() {
        assertThatThrownBy(() -> ReserveHereditaireCalculator.compute(
                1, false, eur("-100"), eur("0"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_montantLibsNegatif_throws() {
        assertThatThrownBy(() -> ReserveHereditaireCalculator.compute(
                1, false, eur("100000"), eur("-1"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_dateFuture_throws() {
        assertThatThrownBy(() -> ReserveHereditaireCalculator.compute(
                1, false, eur("100000"), eur("0"),
                LocalDate.now().plusDays(2), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_paysBE_throws() {
        assertThatThrownBy(() -> ReserveHereditaireCalculator.compute(
                1, false, eur("100000"), eur("0"),
                LocalDate.now().minusMonths(1), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void validation_qualiteNull_throws() {
        assertThatThrownBy(() -> ReserveHereditaireCalculator.compute(
                1, false, eur("100000"), eur("0"),
                LocalDate.now().minusMonths(1), null, FR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============ Score d'éligibilité ============

    @Test
    void score_recevable_max100() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                1, false, eur("200000"), eur("150000"),
                LocalDate.now().minusMonths(6), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.scoreEligibilite()).isEqualTo(100);
    }

    @Test
    void score_qualiteInvalide_inferieur() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                0, true, eur("200000"), eur("150000"),
                LocalDate.now().minusMonths(6), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.scoreEligibilite()).isLessThan(100);
    }

    // ============ Résultat structuré ============

    @Test
    void result_baseJuridiqueEtFormulePresentes() {
        ReserveHereditaireResult r = ReserveHereditaireCalculator.compute(
                1, false, eur("200000"), eur("150000"),
                LocalDate.now().minusMonths(6), QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT, FR);
        assertThat(r.baseJuridique()).contains("913").contains("920");
        assertThat(r.formule()).isNotBlank();
        assertThat(r.messages()).isNotEmpty();
        assertThat(r.country()).isEqualTo("FRANCE");
    }
}
