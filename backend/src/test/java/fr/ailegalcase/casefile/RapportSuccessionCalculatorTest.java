package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.RapportSuccessionCalculator.ModeRapport;
import fr.ailegalcase.casefile.RapportSuccessionCalculator.QualiteHeritier;
import fr.ailegalcase.casefile.RapportSuccessionCalculator.VerdictObligation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RapportSuccessionCalculatorTest {

    private static final String FR = "FRANCE";

    private static BigDecimal eur(String s) {
        return new BigDecimal(s);
    }

    private static LocalDate yearsAgo(int years) {
        return LocalDate.now().minusYears(years);
    }

    // ============ Verdict d'obligation ============

    @Test
    void dispenseExpresseArt919_dispense() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                true, false, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.verdictObligation()).isEqualTo(VerdictObligation.DISPENSÉ);
        assertThat(r.montantRapportable()).isEqualByComparingTo("0.00");
        assertThat(r.modeRapportRecommande()).isEqualTo(ModeRapport.NON_APPLICABLE);
    }

    @Test
    void naturePresumeeNonRapportableArt852_exempt() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("10000"), yearsAgo(2), eur("12000"),
                false, true, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.verdictObligation()).isEqualTo(VerdictObligation.EXEMPT);
        assertThat(r.montantRapportable()).isEqualByComparingTo("0.00");
        assertThat(r.modeRapportRecommande()).isEqualTo(ModeRapport.NON_APPLICABLE);
    }

    @Test
    void descendantDonationClassique_rapportable() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("80000"), yearsAgo(5), eur("100000"),
                false, false, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.verdictObligation()).isEqualTo(VerdictObligation.RAPPORTABLE);
        assertThat(r.modeRapportRecommande()).isEqualTo(ModeRapport.RAPPORT_EN_VALEUR);
        assertThat(r.montantRapportable()).isEqualByComparingTo("100000.00");
    }

    @Test
    void conjointSurvivantDonationClassique_rapportable() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("60000"), yearsAgo(4), eur("75000"),
                false, false, QualiteHeritier.CONJOINT_SURVIVANT, FR);
        assertThat(r.verdictObligation()).isEqualTo(VerdictObligation.RAPPORTABLE);
        assertThat(r.modeRapportRecommande()).isEqualTo(ModeRapport.RAPPORT_EN_VALEUR);
        assertThat(r.montantRapportable()).isEqualByComparingTo("75000.00");
    }

    // ============ Évaluation au jour du partage (art. 860) ============

    @Test
    void evaluationJourPartageRetenue_pasNominal() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(10), eur("120000"),
                false, false, QualiteHeritier.DESCENDANT, FR);
        // art. 860 : valeur au jour du partage retenue
        assertThat(r.montantRapportable()).isEqualByComparingTo("120000.00");
        assertThat(r.donationsRecuesEur()).isEqualByComparingTo("50000.00");
    }

    @Test
    void plusValueDepuisDonation_jourPartageSuperieur() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("100000"), yearsAgo(8), eur("180000"),
                false, false, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.montantRapportable()).isEqualByComparingTo("180000.00");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("Plus-value"));
    }

    @Test
    void moinsValueDepuisDonation_jourPartageInferieur() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("100000"), yearsAgo(6), eur("60000"),
                false, false, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.montantRapportable()).isEqualByComparingTo("60000.00");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("Moins-value"));
    }

    // ============ Cumul flags ============

    @Test
    void cumulDispenseEtNonRapportable_dispensePrime() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                true, true, QualiteHeritier.DESCENDANT, FR);
        // Dispense art. 919 prime sur exemption art. 852 (priorité de la déclaration explicite)
        assertThat(r.verdictObligation()).isEqualTo(VerdictObligation.DISPENSÉ);
    }

    // ============ Validations ============

    @Test
    void validation_donationsZero_throws() {
        assertThatThrownBy(() -> RapportSuccessionCalculator.compute(
                BigDecimal.ZERO, yearsAgo(3), eur("60000"),
                false, false, QualiteHeritier.DESCENDANT, FR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("donationsRecuesEur");
    }

    @Test
    void validation_donationsNegatif_throws() {
        assertThatThrownBy(() -> RapportSuccessionCalculator.compute(
                eur("-100"), yearsAgo(3), eur("60000"),
                false, false, QualiteHeritier.DESCENDANT, FR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_valeurJourPartageZero_throws() {
        assertThatThrownBy(() -> RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), BigDecimal.ZERO,
                false, false, QualiteHeritier.DESCENDANT, FR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valeurAuJourPartage");
    }

    @Test
    void validation_dateDonationFuture_throws() {
        assertThatThrownBy(() -> RapportSuccessionCalculator.compute(
                eur("50000"), LocalDate.now().plusDays(2), eur("60000"),
                false, false, QualiteHeritier.DESCENDANT, FR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void validation_qualiteNull_throws() {
        assertThatThrownBy(() -> RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                false, false, null, FR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_paysBE_throws() {
        assertThatThrownBy(() -> RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                false, false, QualiteHeritier.DESCENDANT, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void validation_paysNull_throws() {
        assertThatThrownBy(() -> RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                false, false, QualiteHeritier.DESCENDANT, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============ Métadonnées ============

    @Test
    void baseJuridiqueContient843Et919() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                false, false, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.baseJuridique()).contains("843").contains("919");
    }

    @Test
    void delaiPrescription5Ans_art924_1() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                false, false, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.delaiPrescriptionAns()).isEqualTo(5);
    }

    @Test
    void score_rapportableSuperieurAuxAutres() {
        RapportSuccessionResult rappor = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                false, false, QualiteHeritier.DESCENDANT, FR);
        RapportSuccessionResult exempt = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                false, true, QualiteHeritier.DESCENDANT, FR);
        RapportSuccessionResult dispense = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                true, false, QualiteHeritier.DESCENDANT, FR);

        assertThat(rappor.scoreEligibilite()).isEqualTo(100);
        assertThat(exempt.scoreEligibilite()).isLessThan(100).isPositive();
        assertThat(dispense.scoreEligibilite()).isLessThan(100).isPositive();
    }

    @Test
    void formuleEtMessagesNonVides() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                false, false, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.formule()).isNotBlank().contains("DESCENDANT").contains("RAPPORTABLE");
        assertThat(r.messages()).isNotEmpty();
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void messages_dispenseRefereArt919() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                true, false, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.messages()).anyMatch(m -> m.contains("919"));
    }

    @Test
    void messages_exemptionRefereArt852() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("60000"),
                false, true, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.messages()).anyMatch(m -> m.contains("852"));
    }

    // ============ Égalité valeur/nominal ============

    @Test
    void valeurEgaleNominal_pasMessageDePlusOuMoinsValue() {
        RapportSuccessionResult r = RapportSuccessionCalculator.compute(
                eur("50000"), yearsAgo(3), eur("50000"),
                false, false, QualiteHeritier.DESCENDANT, FR);
        assertThat(r.montantRapportable()).isEqualByComparingTo("50000.00");
        assertThat(r.messages()).noneMatch(m -> m.contains("Plus-value"));
        assertThat(r.messages()).noneMatch(m -> m.contains("Moins-value"));
    }
}
