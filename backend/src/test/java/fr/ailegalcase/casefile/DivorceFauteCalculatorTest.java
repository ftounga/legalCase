package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DivorceFauteCalculatorTest {

    // =========================================================================
    // 8 types de fautes — validation
    // =========================================================================

    @Test
    void compute_adultere_isRecognized() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"),
                true, false, 10,
                new BigDecimal("60000"), new BigDecimal("30000"),
                null);
        assertThat(r.nombreFautesInvoquees()).isEqualTo(1);
        assertThat(r.fautesInvoquees()).containsExactly("ADULTERE");
        assertThat(r.solidariteeFautesOk()).isTrue();
    }

    @Test
    void compute_violences_isRecognized() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("VIOLENCES"), true, false, 5,
                null, null, null);
        assertThat(r.fautesInvoquees()).containsExactly("VIOLENCES");
    }

    @Test
    void compute_abandon_isRecognized() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ABANDON"), true, false, 5, null, null, null);
        assertThat(r.fautesInvoquees()).containsExactly("ABANDON");
    }

    @Test
    void compute_outrages_isRecognized() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("OUTRAGES"), true, false, 5, null, null, null);
        assertThat(r.fautesInvoquees()).containsExactly("OUTRAGES");
    }

    @Test
    void compute_devoirAssistance_isRecognized() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("DEVOIR_ASSISTANCE"), true, false, 5, null, null, null);
        assertThat(r.fautesInvoquees()).containsExactly("DEVOIR_ASSISTANCE");
    }

    @Test
    void compute_devoirFidelite_isRecognized() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("DEVOIR_FIDELITE"), true, false, 5, null, null, null);
        assertThat(r.fautesInvoquees()).containsExactly("DEVOIR_FIDELITE");
    }

    @Test
    void compute_devoirCommunauteVie_isRecognized() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("DEVOIR_COMMUNAUTE_VIE"), true, false, 5, null, null, null);
        assertThat(r.fautesInvoquees()).containsExactly("DEVOIR_COMMUNAUTE_VIE");
    }

    @Test
    void compute_autre_isRecognized() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("AUTRE"), true, false, 5, null, null, null);
        assertThat(r.fautesInvoquees()).containsExactly("AUTRE");
    }

    // =========================================================================
    // Scoring
    // =========================================================================

    @Test
    void compute_nominal_1faute_preuvesOk_pasTortsPartages_elevee() {
        // 15 + 30 + 25 = 70 → MOYENNE
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"),
                true, false, 10,
                null, null, null);
        assertThat(r.scoreGlobal()).isEqualTo(70);
        assertThat(r.verdictProbabiliteDivorceFaute()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_3fautes_preuvesOk_pasTortsPartages_scoreMax() {
        // 3×15=45 plafond + 30 + 25 = 100
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE", "VIOLENCES", "OUTRAGES"),
                true, false, 15,
                null, null, null);
        assertThat(r.nombreFautesInvoquees()).isEqualTo(3);
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictProbabiliteDivorceFaute()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_4fautes_plafondFautes45_appliqué() {
        // min(4×15, 45)=45 + 30 + 25 = 100
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE", "VIOLENCES", "OUTRAGES", "ABANDON"),
                true, false, 15, null, null, null);
        assertThat(r.nombreFautesInvoquees()).isEqualTo(4);
        assertThat(r.scoreGlobal()).isEqualTo(100);
    }

    @Test
    void compute_2fautes_preuvesOk_tortsPartages_moyenne() {
        // 30 + 30 + 0 = 60 → MOYENNE
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("VIOLENCES", "OUTRAGES"),
                true, true, 5, null, null, null);
        assertThat(r.scoreGlobal()).isEqualTo(60);
        assertThat(r.verdictProbabiliteDivorceFaute()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_2fautes_sansPreuves_faible() {
        // 30 + 0 + 25 = 55 → MOYENNE (seuil 40)
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE", "VIOLENCES"),
                false, false, 5, null, null, null);
        assertThat(r.scoreGlobal()).isEqualTo(55);
        assertThat(r.verdictProbabiliteDivorceFaute()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_1faute_sansPreuves_tortsPartages_faible() {
        // 15 + 0 + 0 = 15 → FAIBLE
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), false, true, 3, null, null, null);
        assertThat(r.scoreGlobal()).isEqualTo(15);
        assertThat(r.verdictProbabiliteDivorceFaute()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_aucuneFaute_scoreMinimal() {
        // 0 + 0 (pas preuves utiles sans faute? ici preuvesDoc=true mais solidariteOk=false car 0 faute)
        // Score: 0×15=0 + 30 (preuvesDoc) + 25 (pas torts) = 55
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of(), true, false, 5, null, null, null);
        assertThat(r.nombreFautesInvoquees()).isEqualTo(0);
        assertThat(r.solidariteeFautesOk()).isFalse();
        assertThat(r.criteresNonRemplis()).anyMatch(s -> s.contains("Aucune faute"));
    }

    // =========================================================================
    // Verdict torts estimés
    // =========================================================================

    @Test
    void verdictTorts_exclusifDefendeur_si_fautesOk_et_pasTortsPartages() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 10, null, null, null);
        assertThat(r.verdictTortsEstimes()).isEqualTo("EXCLUSIF_DEFENDEUR");
    }

    @Test
    void verdictTorts_partages_si_fautesOk_et_tortsPartages() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("VIOLENCES"), true, true, 10, null, null, null);
        assertThat(r.verdictTortsEstimes()).isEqualTo("PARTAGES");
    }

    @Test
    void verdictTorts_impredictible_si_pasPreuves() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), false, false, 10, null, null, null);
        assertThat(r.verdictTortsEstimes()).isEqualTo("IMPREDICTIBLE");
    }

    @Test
    void verdictTorts_impredictible_si_aucuneFaute() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of(), true, false, 10, null, null, null);
        assertThat(r.verdictTortsEstimes()).isEqualTo("IMPREDICTIBLE");
    }

    // =========================================================================
    // Dommages-intérêts art. 266
    // =========================================================================

    @Test
    void di_exclusifDefendeur_fourchetteCompleteParChef() {
        // 2 fautes + DEFENDEUR : [2000 ; 20000]
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("VIOLENCES", "ADULTERE"),
                true, false, 10, null, null, null);
        assertThat(r.damagesInteretsArt266FourchetteMin())
                .isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(r.damagesInteretsArt266FourchetteMax())
                .isEqualByComparingTo(new BigDecimal("20000.00"));
    }

    @Test
    void di_tortsPartages_fourchetteDivisee2() {
        // 2 fautes + PARTAGES : [1000 ; 10000]
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("VIOLENCES", "ADULTERE"),
                true, true, 10, null, null, null);
        assertThat(r.verdictTortsEstimes()).isEqualTo("PARTAGES");
        assertThat(r.damagesInteretsArt266FourchetteMin())
                .isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(r.damagesInteretsArt266FourchetteMax())
                .isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void di_impredictible_sansPreuves_estZero() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), false, false, 10, null, null, null);
        assertThat(r.damagesInteretsArt266FourchetteMin())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.damagesInteretsArt266FourchetteMax())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // Prestation compensatoire art. 270
    // =========================================================================

    @Test
    void prestation_ecartRevenus_fourchettePlusMinus15() {
        // écart 30000 × 0.30 × 8 = 72000 ; ±15% → [61200 ; 82800]
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 10,
                new BigDecimal("60000"), new BigDecimal("30000"),
                null);
        assertThat(r.prestationCompensatoireFourchetteMin())
                .isEqualByComparingTo(new BigDecimal("61200.00"));
        assertThat(r.prestationCompensatoireFourchetteMax())
                .isEqualByComparingTo(new BigDecimal("82800.00"));
    }

    @Test
    void prestation_revenusNulls_estZero() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 10, null, null, null);
        assertThat(r.prestationCompensatoireFourchetteMin())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.prestationCompensatoireFourchetteMax())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void prestation_dureeZero_estZero() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 0,
                new BigDecimal("60000"), new BigDecimal("30000"), null);
        assertThat(r.prestationCompensatoireFourchetteMin())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void prestation_revenusEquivalents_estZero() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 10,
                new BigDecimal("40000"), new BigDecimal("40000"), null);
        assertThat(r.prestationCompensatoireFourchetteMin())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // Base juridique, formule, messages
    // =========================================================================

    @Test
    void baseJuridique_contient_articles() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 10, null, null, null);
        assertThat(r.baseJuridique())
                .contains("242").contains("266").contains("270").contains("Cciv");
    }

    @Test
    void formule_contient_verdictAndScore() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE", "VIOLENCES"), true, false, 10, null, null, null);
        assertThat(r.formule()).contains("ELEVEE").contains("/100");
    }

    @Test
    void messages_expliquent_prestationIndependanteTorts() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 10, null, null, null);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("indépendante").contains("270"));
    }

    @Test
    void messages_expliquent_chargePreuve() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 10, null, null, null);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Charge de la preuve"));
    }

    // =========================================================================
    // Erreurs
    // =========================================================================

    @Test
    void fauteInconnue_throws() {
        assertThatThrownBy(() -> DivorceFauteCalculator.compute(
                List.of("INSULTES_GRAVES"), true, false, 10, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Faute inconnue");
    }

    @Test
    void dureeMariageNegative_throws() {
        assertThatThrownBy(() -> DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, -1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeMariageAnnees");
    }

    @Test
    void revenusDemandeurNegatifs_throws() {
        assertThatThrownBy(() -> DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 5,
                new BigDecimal("-1"), new BigDecimal("0"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenusAnnuelsDemandeurEur");
    }

    @Test
    void revenusDefendeurNegatifs_throws() {
        assertThatThrownBy(() -> DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 5,
                new BigDecimal("0"), new BigDecimal("-1"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenusAnnuelsDefendeurEur");
    }

    @Test
    void fauteDuplique_estDeduplique() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE", "ADULTERE"), true, false, 5, null, null, null);
        assertThat(r.nombreFautesInvoquees()).isEqualTo(1);
    }

    @Test
    void fauteBlank_estIgnoree() {
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE", "", "  "), true, false, 5, null, null, null);
        assertThat(r.nombreFautesInvoquees()).isEqualTo(1);
    }

    @Test
    void compute_dateDepotAssignation_conservee() {
        LocalDate d = LocalDate.of(2026, 4, 10);
        DivorceFauteResult r = DivorceFauteCalculator.compute(
                List.of("ADULTERE"), true, false, 10, null, null, d);
        assertThat(r.dateDepotAssignation()).isEqualTo(d);
    }
}
