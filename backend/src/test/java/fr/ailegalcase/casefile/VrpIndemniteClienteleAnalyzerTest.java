package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-11 : tests unitaires de {@link VrpIndemniteClienteleAnalyzer}. Couvre le
 * préavis VRP 1/2/3 mois (art. L.7313-9 CT), l'éligibilité à l'indemnité de
 * clientèle (DUE / NON_DUE faute grave / démission / clientèle non développée —
 * art. L.7313-13 CT), la fourchette indicative 1 à 2 années de commissions,
 * l'indemnité légale comparée (art. R.1234-2 CT), l'option la plus favorable
 * (non-cumul) et la validation.
 */
class VrpIndemniteClienteleAnalyzerTest {

    private static final BigDecimal COMMISSIONS = new BigDecimal("60000.00");
    private static final BigDecimal SALAIRE = new BigDecimal("4000.00");

    private VrpIndemniteClienteleResult analyze(LocalDate entree, LocalDate rupture,
                                                VrpCauseRupture cause, boolean clientele) {
        return VrpIndemniteClienteleAnalyzer.analyze(
                entree, rupture, cause, VrpTypeVrp.EXCLUSIF, COMMISSIONS, SALAIRE, clientele);
    }

    // ── Préavis VRP (art. L.7313-9 CT) ──────────────────────────────────

    @Test
    void preavis_moins1An_6mois_retourne1Mois() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 7, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, true);
        assertThat(r.dureePreavisMois()).isEqualTo(1);
    }

    @Test
    void preavis_18mois_retourne2Mois() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 7, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, true);
        assertThat(r.dureePreavisMois()).isEqualTo(2);
    }

    @Test
    void preavis_plus2Ans_3ans_retourne3Mois() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, true);
        assertThat(r.dureePreavisMois()).isEqualTo(3);
    }

    // ── Éligibilité indemnité de clientèle (art. L.7313-13 CT) ──────────

    @Test
    void eligibilite_licenciementClienteleDeveloppee_DUE_avecFourchette() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, true);
        assertThat(r.eligibiliteClientele()).isEqualTo(VrpEligibiliteClientele.DUE);
        assertThat(r.motifNonDue()).isNull();
        // borne basse = 1 × commissions ; borne haute = 2 × commissions
        assertThat(r.indemniteClienteleMin()).isEqualByComparingTo("60000.00");
        assertThat(r.indemniteClienteleMax()).isEqualByComparingTo("120000.00");
    }

    @Test
    void eligibilite_fauteGrave_NON_DUE_avecMotif() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.FAUTE_GRAVE, true);
        assertThat(r.eligibiliteClientele()).isEqualTo(VrpEligibiliteClientele.NON_DUE);
        assertThat(r.motifNonDue()).contains("faute grave");
        assertThat(r.indemniteClienteleMin()).isEqualByComparingTo("0.00");
        assertThat(r.indemniteClienteleMax()).isEqualByComparingTo("0.00");
    }

    @Test
    void eligibilite_fauteLourde_NON_DUE() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.FAUTE_LOURDE, true);
        assertThat(r.eligibiliteClientele()).isEqualTo(VrpEligibiliteClientele.NON_DUE);
        assertThat(r.motifNonDue()).contains("faute lourde");
    }

    @Test
    void eligibilite_demission_NON_DUE() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.DEMISSION, true);
        assertThat(r.eligibiliteClientele()).isEqualTo(VrpEligibiliteClientele.NON_DUE);
        assertThat(r.motifNonDue()).contains("Démission");
    }

    @Test
    void eligibilite_clienteleNonDeveloppee_NON_DUE() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, false);
        assertThat(r.eligibiliteClientele()).isEqualTo(VrpEligibiliteClientele.NON_DUE);
        assertThat(r.motifNonDue()).contains("Clientèle non développée");
    }

    @Test
    void eligibilite_departRetraite_clienteleDeveloppee_DUE() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2010, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.DEPART_RETRAITE, true);
        assertThat(r.eligibiliteClientele()).isEqualTo(VrpEligibiliteClientele.DUE);
    }

    // ── Indemnité légale comparée (art. R.1234-2 CT) + option ───────────

    @Test
    void indemniteLegale_3ans_quartDeMoisParAn() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, true);
        // ~3 ans × 1/4 × 4000 = ~3000 €
        assertThat(r.indemniteLegaleLicenciement()).isBetween(
                new BigDecimal("2950.00"), new BigDecimal("3050.00"));
    }

    @Test
    void indemniteLegale_15ans_quartPuisTiersAuDela() {
        VrpIndemniteClienteleAnalyzer.analyze(
                LocalDate.of(2010, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, VrpTypeVrp.EXCLUSIF,
                COMMISSIONS, SALAIRE, true);
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2010, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, true);
        // 10 ans × 1/4 + 5 ans × 1/3 = 2.5 + 1.6667 = ~4.1667 mois × 4000 = ~16667 €
        assertThat(r.indemniteLegaleLicenciement()).isBetween(
                new BigDecimal("16500.00"), new BigDecimal("16800.00"));
    }

    @Test
    void option_clienteleMaxSuperieure_recommandeIndemniteClientele() {
        VrpIndemniteClienteleResult r = analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, true);
        // clientèle max 120000 >> légale ~3000 → INDEMNITE_CLIENTELE
        assertThat(r.optionRecommandee()).isEqualTo(VrpOptionRecommandee.INDEMNITE_CLIENTELE);
    }

    @Test
    void option_indemniteLegaleSuperieure_recommandeIndemniteLegale() {
        // commissions faibles, salaire élevé, ancienneté longue → légale gagne
        VrpIndemniteClienteleResult r = VrpIndemniteClienteleAnalyzer.analyze(
                LocalDate.of(2000, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, VrpTypeVrp.EXCLUSIF,
                new BigDecimal("1000.00"), new BigDecimal("5000.00"), true);
        // clientèle max = 2000 ; légale = 25 ans (10×1/4 + 15×1/3) × 5000 = ~37500 €
        assertThat(r.optionRecommandee()).isEqualTo(VrpOptionRecommandee.INDEMNITE_LEGALE);
    }

    // ── Validation ──────────────────────────────────────────────────────

    @Test
    void validate_dateRuptureAvantDateEntree_throws() {
        assertThatThrownBy(() -> analyze(
                LocalDate.of(2025, 1, 1), LocalDate.of(2024, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRupture");
    }

    @Test
    void validate_commissionsNegatives_throws() {
        assertThatThrownBy(() -> VrpIndemniteClienteleAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, VrpTypeVrp.EXCLUSIF,
                new BigDecimal("-1"), SALAIRE, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commissionsAnnuellesMoyennes");
    }

    @Test
    void validate_salaireNegatif_throws() {
        assertThatThrownBy(() -> VrpIndemniteClienteleAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, VrpTypeVrp.EXCLUSIF,
                COMMISSIONS, new BigDecimal("-1"), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireMensuelMoyen");
    }

    @Test
    void validate_causeRuptureNull_throws() {
        assertThatThrownBy(() -> analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1), null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("causeRupture");
    }

    @Test
    void typeVrpNull_defaultExclusif() {
        VrpIndemniteClienteleResult r = VrpIndemniteClienteleAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                VrpCauseRupture.LICENCIEMENT_CAUSE_REELLE, null,
                COMMISSIONS, SALAIRE, true);
        assertThat(r.typeVrp()).isEqualTo(VrpTypeVrp.EXCLUSIF);
    }
}
